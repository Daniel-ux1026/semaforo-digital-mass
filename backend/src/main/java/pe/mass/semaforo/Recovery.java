package pe.mass.semaforo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import org.apache.commons.mail2.jakarta.SimpleEmail;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController @RequestMapping("/api/auth")
class Recovery {
 final RecoveryService service;
 Recovery(RecoveryService service){this.service=service;}
 record Forgot(@NotBlank @Email @Size(max=254) String email){}
 record Reset(@NotNull @Pattern(regexp="[A-Za-z0-9_-]{43}") String token,
              @NotNull @Size(min=12,max=72) String password){}
 @PostMapping("/forgot-password") Object forgot(@Valid @RequestBody Forgot in){
  service.request(in.email());
  return Map.of("message","Si el correo corresponde a un usuario activo, recibirás un enlace para restablecer tu contraseña. Revisa también spam.");
 }
 @PostMapping("/reset-password") Object reset(@Valid @RequestBody Reset in){
  service.reset(in.token(),in.password());
  return Map.of("message","Contraseña guardada. Ya puedes volver al inicio de sesión.");
 }
}

@Configuration
class RecoveryConfiguration {
 @Bean ThreadPoolTaskExecutor recoveryExecutor(){
  var executor=new ThreadPoolTaskExecutor();executor.setCorePoolSize(1);executor.setMaxPoolSize(2);
  executor.setQueueCapacity(25);executor.setThreadNamePrefix("recovery-mail-");
  executor.setWaitForTasksToCompleteOnShutdown(true);executor.setAwaitTerminationSeconds(15);return executor;
 }
}

@Service
class ResetMailer {
 @Value("${SMTP_HOST:}") String host;
 @Value("${SMTP_PORT:587}") int port;
 @Value("${SMTP_USERNAME:}") String username;
 @Value("${SMTP_PASSWORD:}") String password;
 @Value("${SMTP_FROM:}") String from;
 @Value("${SMTP_SSL:false}") boolean ssl;
 @Value("${SMTP_STARTTLS:true}") boolean startTls;
 @Value("${SMTP_ALLOW_LOCAL_PLAINTEXT:false}") boolean allowLocalPlaintext;
 boolean configured(){return !host.isBlank()&&!from.isBlank();}
 void send(String recipient,String link) throws Exception {
  if(!configured())throw new IllegalStateException("SMTP no configurado");
  if(!ssl&&!startTls&&!(allowLocalPlaintext&&Set.of("mailpit","localhost","127.0.0.1").contains(host)))throw new IllegalStateException("SMTP requiere TLS");
  SimpleEmail email=new SimpleEmail();email.setHostName(host);email.setSmtpPort(port);
  email.setCharset(StandardCharsets.UTF_8.name());email.setSSLOnConnect(ssl);
  if(ssl)email.setSslSmtpPort(Integer.toString(port));
  email.setStartTLSEnabled(startTls);email.setStartTLSRequired(startTls);email.setSSLCheckServerIdentity(true);
  email.setSocketConnectionTimeout(Duration.ofSeconds(5));email.setSocketTimeout(Duration.ofSeconds(10));
  if(!username.isBlank())email.setAuthentication(username,password);
  email.setFrom(from,"Semáforo Digital · Tiendas Mass");email.addTo(recipient);
  email.setSubject("Restablece tu contraseña · Semáforo Digital Mass");
  email.setMsg("Hola,\n\nRecibimos una solicitud para restablecer la contraseña de tu usuario en Semáforo Digital de Tiendas Mass (proyecto académico).\n\nAbre este enlace para crear una nueva contraseña:\n"+link+"\n\nEl enlace vence en 20 minutos y puede utilizarse una sola vez. Si no solicitaste el cambio, ignora este correo: tu contraseña no cambiará.\n\nEquipo Semáforo Digital");
  email.send();
 }
}

@Service
class RecoveryService {
 final JdbcTemplate jdbc;final PasswordEncoder encoder;final Clock clock;final ResetMailer mailer;
 final ThreadPoolTaskExecutor executor;final TransactionTemplate tx;final String origin;
 RecoveryService(JdbcTemplate jdbc,PasswordEncoder encoder,Clock clock,ResetMailer mailer,
   ThreadPoolTaskExecutor recoveryExecutor,PlatformTransactionManager manager,@Value("${app.origin}") String origin){
  this.jdbc=jdbc;this.encoder=encoder;this.clock=clock;this.mailer=mailer;executor=recoveryExecutor;
  tx=new TransactionTemplate(manager);this.origin=origin;
 }
 void request(String email){
  if(!mailer.configured())throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"El envío de correo aún no está configurado. Contacta al administrador.");
  String normalized=email.strip().toLowerCase(Locale.ROOT);
  try{executor.execute(()->deliver(normalized));}
  catch(org.springframework.core.task.TaskRejectedException e){throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Espere un momento antes de solicitar otro correo.");}
 }
 void deliver(String email){
  String raw=AuthService.random(),hash=AuthService.hash(raw);
  try{
   Boolean created=tx.execute(s->{
    var users=jdbc.queryForList("SELECT id,password FROM app_user WHERE email=? AND active=true AND role='ADMIN' FOR UPDATE",email);
    if(users.isEmpty())return false;
    var user=users.getFirst();
    long recent=Objects.requireNonNull(jdbc.queryForObject("SELECT COUNT(*) FROM password_reset WHERE user_id=? AND created_at>?",Long.class,user.get("id"),java.sql.Timestamp.from(clock.instant().minusSeconds(60))));
    if(recent>0)return false;
    jdbc.update("INSERT INTO password_reset(hash,user_id,password_snapshot,email_snapshot,created_at,expires_at) VALUES(?,?,?,?,?,?)",hash,user.get("id"),user.get("password"),email,java.sql.Timestamp.from(clock.instant()),java.sql.Timestamp.from(clock.instant().plusSeconds(1200)));
    return true;
   });
   if(!Boolean.TRUE.equals(created))return;
   // Fragment avoids sending the bearer secret in URL queries, access logs or Referer.
   mailer.send(email,origin+"/restablecer#token="+raw);
   jdbc.update("UPDATE password_reset SET delivery='SENT' WHERE hash=?",hash);
  }catch(Exception e){
   jdbc.update("UPDATE password_reset SET delivery='FAILED',used=true WHERE hash=?",hash);
   org.slf4j.LoggerFactory.getLogger(RecoveryService.class).warn("No se pudo completar correo de recuperación: {}",e.getClass().getSimpleName());
  }
 }
 void reset(String raw,String password){
  if(password.getBytes(StandardCharsets.UTF_8).length>72)throw new IllegalArgumentException("La contraseña no puede superar 72 bytes UTF-8.");
  tx.executeWithoutResult(s->{
   String hash=AuthService.hash(raw);
   var lookup=jdbc.queryForList("SELECT user_id FROM password_reset WHERE hash=?",hash);
   if(lookup.isEmpty())throw invalid();
   Object userId=lookup.getFirst().get("user_id");
   var user=jdbc.queryForList("SELECT password,email,active,role FROM app_user WHERE id=? FOR UPDATE",userId).getFirst();
   var token=jdbc.queryForList("SELECT * FROM password_reset WHERE hash=? FOR UPDATE",hash).getFirst();
   if(!"ADMIN".equals(user.get("role"))||Boolean.TRUE.equals(token.get("used"))||!Boolean.TRUE.equals(user.get("active"))||
      !((java.sql.Timestamp)token.get("expires_at")).toInstant().isAfter(clock.instant())||
      !Objects.equals(user.get("password"),token.get("password_snapshot"))||
      !Objects.equals(user.get("email"),token.get("email_snapshot")))throw invalid();
   if(encoder.matches(password,user.get("password").toString()))throw new IllegalArgumentException("Elige una contraseña diferente a la anterior.");
   jdbc.update("UPDATE app_user SET password=?,must_change=false,version=version+1 WHERE id=?",encoder.encode(password),userId);
   jdbc.update("UPDATE password_reset SET used=true WHERE user_id=?",userId);
   jdbc.update("UPDATE auth_session SET revoked=true WHERE user_id=?",userId);
   jdbc.update("INSERT INTO audit(actor_id,action,entity,detail,created_at) VALUES(?,?,?,?,?)",userId,"RECUPERACION_PASSWORD","usuario:"+userId,"Enlace de correo consumido; sesiones revocadas",java.sql.Timestamp.from(clock.instant()));
  });
 }
 private IllegalArgumentException invalid(){return new IllegalArgumentException("El enlace es inválido, ya fue utilizado o venció. Solicita uno nuevo.");}
}
