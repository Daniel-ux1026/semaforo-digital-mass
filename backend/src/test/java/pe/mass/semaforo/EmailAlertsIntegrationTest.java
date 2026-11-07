package pe.mass.semaforo;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.*;
import java.net.URI;
import java.net.http.*;
import java.time.*;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest @Testcontainers
class EmailAlertsIntegrationTest {
 @Container static MySQLContainer<?> db=new MySQLContainer<>("mysql:8.4.8").withCommand("--log-bin-trust-function-creators=1");
 @Container static GenericContainer<?> smtp=new GenericContainer<>("axllent/mailpit:v1.27.8").withExposedPorts(1025,8025);
 @DynamicPropertySource static void properties(DynamicPropertyRegistry r){
  r.add("spring.datasource.url",db::getJdbcUrl);r.add("spring.datasource.username",db::getUsername);r.add("spring.datasource.password",db::getPassword);
  r.add("spring.flyway.user",db::getUsername);r.add("spring.flyway.password",db::getPassword);
  r.add("app.jwt-secret",()->"test-only-"+"x".repeat(40));r.add("app.bootstrap-dni",()->"00000001");r.add("app.bootstrap-password",()->"Test-only-password-123");
 }
 @Autowired JdbcTemplate jdbc;@Autowired PlatformTransactionManager manager;
 @Test void smtpContentDeduplicationStateChangeAndUncertainDelivery() throws Exception {
  LocalDate day=LocalDate.now(Rules.LIMA);
  jdbc.update("INSERT INTO product(sku,name,category,unit) VALUES('EMAIL-DEMO','Yogur de prueba','Pruebas','UNIDAD')");
  long product=jdbc.queryForObject("SELECT id FROM product WHERE sku='EMAIL-DEMO'",Long.class);
  long user=jdbc.queryForObject("SELECT id FROM app_user WHERE dni='00000001'",Long.class);
  jdbc.update("INSERT INTO lot(product_id,code,received,expiry,initial_quantity,normal,created_by,created_at) VALUES(?,'DEMO-CORREO',?,?,3,3,?,UTC_TIMESTAMP(6))",product,day.minusDays(10),day.plusDays(1),user);
  long lot=jdbc.queryForObject("SELECT id FROM lot WHERE code='DEMO-CORREO'",Long.class);
  ResetMailer mailer=new ResetMailer();mailer.host=smtp.getHost();mailer.port=smtp.getMappedPort(1025);
  mailer.username="";mailer.password="";mailer.from="tienda@example.invalid";mailer.startTls=false;mailer.allowLocalPlaintext=true;
  var alerts=new EmailAlerts(jdbc,mailer,Clock.systemUTC(),manager,true,"destino@example.invalid");
  alerts.enqueue();alerts.enqueue();
  assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM avisos_correo",Integer.class));
  alerts.deliver();alerts.enqueue();alerts.deliver();
  assertEquals("ACCEPTED",jdbc.queryForObject("SELECT estado FROM avisos_correo WHERE lote_id=?",String.class,lot));
  var client=HttpClient.newHttpClient();var json=new ObjectMapper();
  String base="http://"+smtp.getHost()+":"+smtp.getMappedPort(8025);
  var messages=json.readTree(client.send(HttpRequest.newBuilder(URI.create(base+"/api/v1/messages")).build(),HttpResponse.BodyHandlers.ofString()).body());
  assertEquals(1,messages.path("total").asInt());
  String messageId=messages.path("messages").get(0).path("ID").asText();
  var message=json.readTree(client.send(HttpRequest.newBuilder(URI.create(base+"/api/v1/message/"+messageId)).build(),HttpResponse.BodyHandlers.ofString()).body());
  String body=message.path("Text").asText();
  assertTrue(body.contains("DEMO-CORREO"));assertTrue(body.contains("Vence mañana"));assertTrue(body.contains("Yogur de prueba"));assertTrue(body.contains("3.000 UNIDAD"));
  assertEquals("Alerta de vencimiento · Semáforo Digital Mass",message.path("Subject").asText());
  jdbc.update("UPDATE lot SET expiry=? WHERE id=?",day.minusDays(1),lot);alerts.enqueue();
  assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM avisos_correo",Integer.class));
  var failing=org.mockito.Mockito.spy(mailer);
  org.mockito.Mockito.doThrow(new java.net.SocketTimeoutException("test")).when(failing).sendMessage(org.mockito.ArgumentMatchers.anyString(),org.mockito.ArgumentMatchers.anyString(),org.mockito.ArgumentMatchers.anyString());
  var uncertain=new EmailAlerts(jdbc,failing,Clock.systemUTC(),manager,true,"destino@example.invalid");
  uncertain.deliver();uncertain.deliver();
  assertEquals("UNCERTAIN",jdbc.queryForObject("SELECT estado FROM avisos_correo WHERE estado_lote='VENCIDO'",String.class));
  org.mockito.Mockito.verify(failing,org.mockito.Mockito.times(1)).sendMessage(org.mockito.ArgumentMatchers.anyString(),org.mockito.ArgumentMatchers.anyString(),org.mockito.ArgumentMatchers.anyString());
  jdbc.update("UPDATE avisos_correo SET estado='PENDING',siguiente_intento=UTC_TIMESTAMP(6) WHERE estado_lote='VENCIDO'");
  jdbc.update("UPDATE lot SET normal=0 WHERE id=?",lot);uncertain.deliver();
  assertEquals("OBSOLETE",jdbc.queryForObject("SELECT estado FROM avisos_correo WHERE estado_lote='VENCIDO'",String.class));
  assertEquals("AUTH_RECHAZADA",EmailAlerts.failure(new jakarta.mail.AuthenticationFailedException()));
  jdbc.update("INSERT INTO lot(product_id,code,received,expiry,initial_quantity,normal,created_by,created_at) VALUES(?,'DEMO-BASELINE',?,?,2,2,?,UTC_TIMESTAMP(6))",product,day,day.plusDays(1),user);
  var disabled=new EmailAlerts(jdbc,mailer,Clock.systemUTC(),manager,false,"destino@example.invalid");
  var admin=new EmailAlertsAdmin(disabled,jdbc,org.mockito.Mockito.mock(InventoryService.class));
  admin.baseline(null);disabled.deliver();
  assertEquals("BASELINE",jdbc.queryForObject("SELECT c.estado FROM avisos_correo c JOIN lot l ON l.id=c.lote_id WHERE l.code='DEMO-BASELINE'",String.class));
  assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM avisos_correo WHERE estado='ACCEPTED'",Integer.class));
 }
}
