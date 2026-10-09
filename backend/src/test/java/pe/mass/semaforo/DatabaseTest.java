package pe.mass.semaforo;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.*;
import org.springframework.jdbc.core.JdbcTemplate;
import java.time.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;

@SpringBootTest
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
@Testcontainers
class DatabaseTest {
 @Container static MySQLContainer<?> db=new MySQLContainer<>("mysql:8.4.8").withCommand("--log-bin-trust-function-creators=1");
 @DynamicPropertySource static void properties(DynamicPropertyRegistry r){r.add("spring.datasource.url",db::getJdbcUrl);r.add("spring.datasource.username",db::getUsername);r.add("spring.datasource.password",db::getPassword);r.add("spring.flyway.user",db::getUsername);r.add("spring.flyway.password",db::getPassword);r.add("app.jwt-secret",()->"test-only-"+"x".repeat(40));r.add("app.bootstrap-dni",()->"00000001");r.add("app.bootstrap-password",()->"Test-only-password-123");}
 @Autowired InventoryService inventory;@Autowired AuthService auth;@Autowired JdbcTemplate jdbc;@Autowired EvaluationService evaluation;
 @Autowired org.springframework.test.web.servlet.MockMvc mvc;
 @Autowired Admin reports;
 @Autowired RecoveryService recovery;
 @Autowired org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
 @Autowired Automation automation;
 @Autowired PasswordRequests requests;
 @Test void onlyProgrammerChangesStaffPasswords() throws Exception {
  for(String role:List.of("WORKER","SUPERVISOR")){
   String dni=role.equals("WORKER")?"00777771":"00777772";
   jdbc.update("INSERT INTO app_user(dni,name,password,role,must_change) VALUES(?,?,?,?,false)",dni,"Solicitud "+role,passwordEncoder.encode("Before-password-123"),role);
   long id=jdbc.queryForObject("SELECT id FROM app_user WHERE dni=?",Long.class,dni);
   var actor=new Security.Actor(id,"Solicitud",role,"",false);
   assertThrows(org.springframework.web.server.ResponseStatusException.class,()->auth.password(actor,new Auth.Password("Before-password-123","After-password-456")));
   requests.request(actor);requests.request(actor);
   assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM solicitudes_clave WHERE usuario_id=? AND estado='PENDIENTE'",Integer.class,id));
   var login=auth.login(new Auth.Login(dni,"Before-password-123"));
   reports.reset(admin(),id,new Admin.Reset("After-password-456"));
   assertNull(auth.refresh(login.refresh()));assertNull(auth.login(new Auth.Login(dni,"Before-password-123")));
   var next=auth.login(new Auth.Login(dni,"After-password-456"));assertEquals(false,next.user().get("mustChange"));
   assertEquals("RESUELTA",jdbc.queryForObject("SELECT estado FROM solicitudes_clave WHERE usuario_id=?",String.class,id));
   assertFalse(automation.chatContext(actor).toString().contains("valuedLoss"));
   mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/auth/password").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("staff").roles(role)).with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isForbidden());
  }
 }

 @Test void threeRolesAndProfiles() throws Exception {
  jdbc.update("INSERT INTO app_user(dni,name,password,role,must_change) VALUES('00888881','Supervisor',?,'SUPERVISOR',false)",passwordEncoder.encode("Test-password-123"));
  long sid=jdbc.queryForObject("SELECT id FROM app_user WHERE dni='00888881'",Long.class);
  var supervisor=new Security.Actor(sid,"Supervisor","SUPERVISOR","",false);
  assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM supervisor WHERE usuario_id=?",Integer.class,sid));
  assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM programador WHERE usuario_id=?",Integer.class,admin().id()));
  long lot=createLot(45,BigDecimal.TEN);inventory.action(supervisor,lot,key(),action("AJUSTE","NORMAL","1"));
  assertThrows(org.springframework.web.server.ResponseStatusException.class,()->inventory.removeProduct(supervisor,jdbc.queryForObject("SELECT product_id FROM lot WHERE id=?",Long.class,lot)));
  for(String role:List.of("SUPERVISOR","WORKER")){
   mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/admin/users/1").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("test").roles(role)).with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isForbidden());
  }
  mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/admin/products/1").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("worker").roles("WORKER")).with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isForbidden());
 }

 @SuppressWarnings("unchecked") @Test void paginatedHistoryKeepsAllMovementsAndWorkerPrivacy(){
  long id=createLot(45,new BigDecimal("40"));
  for(int i=0;i<26;i++)inventory.action(admin(),id,key(),action("VENTA","NORMAL","1"));
  var worker=new Security.Actor(admin().id(),"Operador","WORKER","",false);
  var first=(Map<String,Object>)inventory.historyPage(worker,id,0);
  var second=(Map<String,Object>)inventory.historyPage(worker,id,1);
  var rows=(List<Map<String,Object>>)first.get("items");var rest=(List<Map<String,Object>>)second.get("items");
  assertEquals(25,rows.size());assertEquals(2,rest.size());assertEquals(true,first.get("hasNext"));assertEquals(false,second.get("hasNext"));
  var ids=new HashSet<Object>();rows.forEach(r->ids.add(r.get("id")));rest.forEach(r->assertTrue(ids.add(r.get("id"))));
  assertFalse(first.toString().contains("cost="));assertTrue(inventory.historyPage(admin(),id,0).toString().contains("cost="));
  assertThrows(IllegalArgumentException.class,()->inventory.historyPage(worker,id,-1));
 }
 @SuppressWarnings("unchecked") @Test void failedNotificationsRetryOnceAndPageWithoutOverlap() throws Exception {
  for(int i=0;i<26;i++)createLot(5,BigDecimal.ONE);
  var first=(Map<String,Object>)automation.outboxPage(0);var second=(Map<String,Object>)automation.outboxPage(1);
  var rows=(List<Map<String,Object>>)first.get("items");var rest=(List<Map<String,Object>>)second.get("items");
  assertEquals(25,rows.size());assertEquals(true,first.get("hasNext"));assertFalse(rest.isEmpty());
  var ids=new HashSet<Object>();rows.forEach(r->ids.add(r.get("id")));rest.forEach(r->assertTrue(ids.add(r.get("id"))));
  String id=rows.getFirst().get("id").toString();
  jdbc.update("UPDATE outbox SET status='FAILED',attempts=5 WHERE id=?",id);
  automation.retry(admin(),id);
  assertEquals("PENDING",jdbc.queryForObject("SELECT status FROM outbox WHERE id=?",String.class,id));
  assertEquals(0,jdbc.queryForObject("SELECT attempts FROM outbox WHERE id=?",Integer.class,id));
  assertThrows(IllegalArgumentException.class,()->automation.retry(admin(),id));
  mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/admin/outbox/"+id+"/retry").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("worker").roles("WORKER")).with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isForbidden());
 }
 @org.springframework.test.context.bean.override.mockito.MockitoBean ResetMailer resetMailer;
 @Test void recoveryIsSingleUseAndRevokesSessions(){
  String dni="00990001",email="recovery-test@example.invalid";
  jdbc.update("INSERT INTO app_user(dni,name,password,role,email,must_change) VALUES(?,?,?,'ADMIN',?,false)",dni,"Recuperación",passwordEncoder.encode("Before-password-123"),email);
  var session=auth.login(new Auth.Login(dni,"Before-password-123"));assertNotNull(session);
  var link=new java.util.concurrent.atomic.AtomicReference<String>();
  try{org.mockito.Mockito.doAnswer(call->{link.set(call.getArgument(1));return null;}).when(resetMailer).send(org.mockito.ArgumentMatchers.eq(email),org.mockito.ArgumentMatchers.anyString());}catch(Exception e){throw new AssertionError(e);}
  recovery.deliver(email);assertNotNull(link.get());String token=link.get().split("#token=")[1];
  assertFalse(jdbc.queryForList("SELECT hash FROM password_reset").toString().contains(token));
  recovery.reset(token,"After-password-456");assertNull(auth.refresh(session.refresh()));
  assertThrows(IllegalArgumentException.class,()->recovery.reset(token,"Other-password-789"));
  assertNull(auth.login(new Auth.Login(dni,"Before-password-123")));assertNotNull(auth.login(new Auth.Login(dni,"After-password-456")));
 }
 @Test void recoveryRejectsExpiredAndChangedEmail(){
  String email="expired@example.invalid";
  jdbc.update("INSERT INTO app_user(dni,name,password,role,email) VALUES('00990002','Caducidad',?,'ADMIN',?)",passwordEncoder.encode("Before-password-123"),email);
  var link=new java.util.concurrent.atomic.AtomicReference<String>();
  try{org.mockito.Mockito.doAnswer(call->{link.set(call.getArgument(1));return null;}).when(resetMailer).send(org.mockito.ArgumentMatchers.eq(email),org.mockito.ArgumentMatchers.anyString());}catch(Exception e){throw new AssertionError(e);}
  recovery.deliver(email);String token=link.get().split("#token=")[1];
  jdbc.update("UPDATE password_reset SET expires_at=TIMESTAMPADD(MINUTE,-1,UTC_TIMESTAMP(6)) WHERE hash=?",AuthService.hash(token));
  assertThrows(IllegalArgumentException.class,()->recovery.reset(token,"After-password-456"));
  jdbc.update("UPDATE password_reset SET expires_at=TIMESTAMPADD(MINUTE,20,UTC_TIMESTAMP(6)) WHERE hash=?",AuthService.hash(token));
  jdbc.update("UPDATE app_user SET email='changed@example.invalid' WHERE email=?",email);
  assertThrows(IllegalArgumentException.class,()->recovery.reset(token,"After-password-456"));
 }
 @SuppressWarnings("unchecked") @Test void alertsShowPartialAttentionAndExcludeGreen(){
  long id=createLot(5,BigDecimal.TEN);long green=createLot(50,BigDecimal.ONE);
  inventory.action(admin(),id,key(),action("PROMOCION","NORMAL","2"));
  var result=(Map<String,Object>)inventory.alerts(admin(),"","",0);
  var items=(List<Map<String,Object>>)result.get("items");
  assertTrue(items.stream().anyMatch(r->r.get("id").equals(id)&&r.get("attention").equals("ATENDIDA_CON_SALDO")));
  assertFalse(items.stream().anyMatch(r->r.get("id").equals(green)));
 }
 @SuppressWarnings("unchecked") @Test void chatWasteIncludesUnvaluedQuantity(){
  long id=createLot(5,BigDecimal.TEN);jdbc.update("UPDATE lot SET cost=NULL WHERE id=?",id);inventory.action(admin(),id,key(),action("MERMA","NORMAL","2"));
  var rows=(List<Map<String,Object>>)automation.wasteSummary();
  assertTrue(rows.stream().anyMatch(r->((BigDecimal)r.get("pendingQuantity")).signum()>0&&r.get("valuation").toString().startsWith("PARCIAL")));
 }
 @Test void chatContextRequiresScopedAdministratorTicket() throws Exception {
  mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/chat/context")).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isUnauthorized());
  mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/admin/chat/session").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("worker").roles("WORKER")).with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isForbidden());
 }
 @Test void directApiDeniesWorkerAndCsrf() throws Exception {
  mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/admin/users").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("worker").roles("WORKER"))).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isForbidden());
  mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/auth/login").contentType("application/json").content("{\"dni\":\"00000001\",\"password\":\"invalid\"}")).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isForbidden());
  mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/lots")).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isUnauthorized());
  mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/service/critical").header("Authorization","Bearer invalid")).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isUnauthorized());
 }
 @Test void workerDtoOmitsMoney(){long id=createLot(5,BigDecimal.ONE);var worker=new Security.Actor(admin().id(),"Operador","WORKER","",false);String view=inventory.lots(worker,"","",0,true).toString();assertFalse(view.contains("cost="));assertFalse(inventory.history(worker,id).toString().contains("cost="));assertThrows(Exception.class,()->inventory.action(worker,id,key(),action("AJUSTE","NORMAL","1")));}
 Security.Actor admin(){return new Security.Actor(jdbc.queryForObject("select id from app_user where dni='00000001'",Long.class),"Test","ADMIN","test",false);}
 String key(){return UUID.randomUUID().toString();}
 @SuppressWarnings("unchecked") long createLot(int days,BigDecimal qty){var a=admin();var p=(Map<String,Object>)inventory.product(a,null,new Inventory.ProductInput("SKU-"+key(),null,"Prueba","Pruebas","UNIDAD",30,15,true));return ((Number)((Map<String,Object>)inventory.receive(a,key(),new Inventory.LotInput(((Number)p.get("id")).longValue(),key(),LocalDate.now(Rules.LIMA).minusDays(10),LocalDate.now(Rules.LIMA).plusDays(days),qty,new BigDecimal("2.50")))).get("id")).longValue();}
 Inventory.Action action(String type,String source,String q){return new Inventory.Action(type,source,new BigDecimal(q),"prueba",null);}
 @Test void partialIdempotencyAndRollback(){long id=createLot(5,new BigDecimal("20"));String once=key();inventory.action(admin(),id,once,action("PROMOCION","NORMAL","10"));inventory.action(admin(),id,once,action("PROMOCION","NORMAL","10"));inventory.action(admin(),id,key(),action("VENTA","PROMO","5"));inventory.action(admin(),id,key(),action("MERMA","NORMAL","5"));assertEquals(0,new BigDecimal("5").compareTo(jdbc.queryForObject("select normal from lot where id=?",BigDecimal.class,id)));assertEquals(0,new BigDecimal("5").compareTo(jdbc.queryForObject("select promo from lot where id=?",BigDecimal.class,id)));assertThrows(Exception.class,()->inventory.action(admin(),id,key(),action("MERMA","NORMAL","6")));assertEquals(4,jdbc.queryForObject("select count(*) from movement where lot_id=?",Integer.class,id));assertThrows(Exception.class,()->inventory.action(admin(),id,once,action("MERMA","NORMAL","1")));}
 @Test void concurrentWithdrawals() throws Exception {long id=createLot(10,new BigDecimal("10"));var a=admin();try(var executor=Executors.newFixedThreadPool(2)){var latch=new CountDownLatch(1);Callable<Boolean> task=()->{latch.await();try{inventory.action(a,id,key(),action("VENTA","NORMAL","7"));return true;}catch(IllegalArgumentException e){return false;}};var one=executor.submit(task);var two=executor.submit(task);latch.countDown();assertNotEquals(one.get(),two.get());}assertEquals(0,new BigDecimal("3").compareTo(jdbc.queryForObject("select normal from lot where id=?",BigDecimal.class,id)));}
 @Test void expiredAndEvaluation(){long id=createLot(-1,BigDecimal.TEN);assertThrows(IllegalArgumentException.class,()->inventory.action(admin(),id,key(),action("VENTA","NORMAL","1")));assertThrows(IllegalArgumentException.class,()->inventory.action(admin(),id,key(),action("PROMOCION","NORMAL","1")));evaluation.run(false);evaluation.run(false);assertEquals(1,jdbc.queryForObject("select count(*) from alert where lot_id=? and state='VENCIDO'",Integer.class,id));assertEquals(1,jdbc.queryForObject("select count(*) from outbox o join alert a on a.id=o.alert_id where a.lot_id=?",Integer.class,id));}
 @Test void refreshReplayRevokes(){var login=auth.login(new Auth.Login("00000001","Test-only-password-123"));assertNotNull(login);var next=auth.refresh(login.refresh());assertNotNull(next);assertNull(auth.refresh(login.refresh()));assertNull(auth.refresh(next.refresh()));}
 @SuppressWarnings("unchecked") @Test void reversalsRespectSubsequentBalances(){
  long id=createLot(10,BigDecimal.TEN);var a=admin();var promotion=(Map<String,Object>)inventory.action(a,id,key(),action("PROMOCION","NORMAL","10"));
  var sale=(Map<String,Object>)inventory.action(a,id,key(),action("VENTA","PROMO","5"));
  assertThrows(IllegalArgumentException.class,()->inventory.action(a,id,key(),new Inventory.Action("REVERSO","NORMAL",BigDecimal.TEN,"reversión",((Number)promotion.get("id")).longValue())));
  inventory.action(a,id,key(),new Inventory.Action("REVERSO","PROMO",new BigDecimal("5"),"reversión",((Number)sale.get("id")).longValue()));
  inventory.action(a,id,key(),new Inventory.Action("REVERSO","NORMAL",BigDecimal.TEN,"reversión",((Number)promotion.get("id")).longValue()));
  assertEquals(0,BigDecimal.TEN.compareTo(jdbc.queryForObject("select normal from lot where id=?",BigDecimal.class,id)));
  assertThrows(IllegalArgumentException.class,()->inventory.action(a,id,key(),new Inventory.Action("REVERSO","NORMAL",BigDecimal.TEN,"duplicado",((Number)promotion.get("id")).longValue())));
 }
 @Test void exhaustedRetainsHistoryAndCatchup(){long id=createLot(0,BigDecimal.ONE);inventory.action(admin(),id,key(),action("MERMA","NORMAL","1"));assertEquals(2,jdbc.queryForObject("select count(*) from movement where lot_id=?",Integer.class,id));jdbc.update("DELETE FROM evaluation");evaluation.run(true);int before=jdbc.queryForObject("select count(*) from evaluation",Integer.class);evaluation.run(true);assertEquals(before,jdbc.queryForObject("select count(*) from evaluation",Integer.class));}
 @SuppressWarnings("unchecked") @Test void valuedWasteAndPendingCost(){
  long id=createLot(10,BigDecimal.TEN);Long productId=jdbc.queryForObject("SELECT product_id FROM lot WHERE id=?",Long.class,id);var a=admin();
  var m=(Map<String,Object>)inventory.action(a,id,key(),action("MERMA","NORMAL","4"));
  LocalDate today=LocalDate.now(Rules.LIMA);var rows=reports.report(today,today,productId,"",0,false);
  var waste=rows.stream().filter(r->"MERMA".equals(r.get("type"))).findFirst().orElseThrow();assertEquals(0,new BigDecimal("10").compareTo((BigDecimal)waste.get("loss")));
  inventory.action(a,id,key(),new Inventory.Action("REVERSO","NORMAL",new BigDecimal("4"),"Error de registro",((Number)m.get("id")).longValue()));
  rows=reports.report(today,today,productId,"",0,false);BigDecimal net=rows.stream().filter(r->r.get("loss")!=null).map(r->(BigDecimal)r.get("loss")).reduce(BigDecimal.ZERO,BigDecimal::add);assertEquals(0,net.signum());
  jdbc.update("UPDATE lot SET cost=NULL WHERE id=?",id);inventory.action(a,id,key(),action("MERMA","NORMAL","2"));
  rows=reports.report(today,today,productId,"MERMA",0,false);var latest=rows.getFirst();assertNull(latest.get("loss"));assertEquals("PENDIENTE",latest.get("valuation"));
  inventory.cost(a,id,new Inventory.Cost(new BigDecimal("1.25"),"Costo confirmado"));rows=reports.report(today,today,productId,"MERMA",0,false);assertEquals(0,new BigDecimal("2.50").compareTo((BigDecimal)rows.getFirst().get("loss")));
 }
}
