package pe.mass.semaforo;

import jakarta.mail.AuthenticationFailedException;
import jakarta.mail.internet.InternetAddress;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.*;

@Service
class EmailAlerts {
 final JdbcTemplate jdbc;final ResetMailer mailer;final Clock clock;final TransactionTemplate tx;
 final boolean enabled;final String recipient;
 EmailAlerts(JdbcTemplate jdbc,ResetMailer mailer,Clock clock,PlatformTransactionManager manager,
   @Value("${ALERT_EMAIL_ENABLED:false}") boolean enabled,@Value("${ALERT_EMAIL_TO:}") String recipient){
  this.jdbc=jdbc;this.mailer=mailer;this.clock=clock;this.tx=new TransactionTemplate(manager);
  this.enabled=enabled;this.recipient=recipient.strip().toLowerCase(Locale.ROOT);
  if(enabled||!this.recipient.isEmpty()){
   try{var address=new InternetAddress(this.recipient,true);address.validate();if(!address.getAddress().equals(this.recipient)||this.recipient.indexOf('@')<1)throw new IllegalArgumentException();}
   catch(Exception e){throw new IllegalArgumentException("Configura un único destinatario válido en ALERT_EMAIL_TO.");}
   if(enabled&&!mailer.configured())throw new IllegalArgumentException("Configura SMTP antes de activar alertas por correo.");
  }
 }
 // Se ejecuta en el servidor, independientemente de las sesiones del navegador.
 @Scheduled(fixedDelayString="${ALERT_EMAIL_INTERVAL_MS:60000}",initialDelay=15000)
 public void tick(){
  if(!enabled)return;
  enqueue();deliver();
 }
 void enqueue(){
  LocalDate day=LocalDate.now(clock.withZone(Rules.LIMA));
  jdbc.update("""
   INSERT IGNORE INTO avisos_correo(lote_id,estado_lote,destinatario)
   SELECT l.id,CASE WHEN l.expiry<? THEN 'VENCIDO' ELSE 'ROJO' END,?
   FROM lot l JOIN product p ON p.id=l.product_id
   WHERE p.active=true AND l.normal+l.promo>0 AND DATEDIFF(l.expiry,?)<=p.critical_days
   """,day,recipient,day);
 }
 void deliver(){
  deliver(null);
 }
 void deliver(Long onlyId){
  // SMTP no garantiza idempotencia. Tras una caída durante un envío, no reenviar a ciegas.
  jdbc.update("UPDATE avisos_correo SET estado='UNCERTAIN',ultimo_error='ENVIO_INTERRUMPIDO' WHERE estado='PROCESSING' AND siguiente_intento<UTC_TIMESTAMP(6)");
  var rows=tx.execute(s->{
   var pending=jdbc.queryForList("SELECT id,lote_id,estado_lote,intentos FROM avisos_correo WHERE destinatario=? AND estado='PENDING' AND siguiente_intento<=UTC_TIMESTAMP(6) AND (? IS NULL OR id=?) ORDER BY id LIMIT 5 FOR UPDATE SKIP LOCKED",recipient,onlyId,onlyId);
   for(var row:pending)jdbc.update("UPDATE avisos_correo SET estado='PROCESSING',siguiente_intento=TIMESTAMPADD(MINUTE,5,UTC_TIMESTAMP(6)) WHERE id=?",row.get("id"));
   return pending;
  });
  for(var row:Objects.requireNonNull(rows)){
   Object id=row.get("id");int attempts=((Number)row.get("intentos")).intValue()+1;
   try{
    LocalDate day=LocalDate.now(clock.withZone(Rules.LIMA));
    var lots=jdbc.queryForList("""
     SELECT l.code,l.expiry,p.name product,p.unit,l.normal+l.promo balance,DATEDIFF(l.expiry,?) days
     FROM lot l JOIN product p ON p.id=l.product_id
     WHERE l.id=? AND p.active=true AND l.normal+l.promo>0
      AND ?=CASE WHEN l.expiry<? THEN 'VENCIDO' WHEN DATEDIFF(l.expiry,?)<=p.critical_days THEN 'ROJO' ELSE 'NONE' END
     """,day,row.get("lote_id"),row.get("estado_lote"),day,day);
    if(lots.isEmpty()){jdbc.update("UPDATE avisos_correo SET estado='OBSOLETE' WHERE id=?",id);continue;}
    mailer.sendMessage(recipient,"Alerta de vencimiento · Semáforo Digital Mass",body(lots.getFirst(),row.get("estado_lote").toString()));
    jdbc.update("UPDATE avisos_correo SET estado='ACCEPTED',intentos=?,aceptado_en=UTC_TIMESTAMP(6),ultimo_error=NULL WHERE id=?",attempts,id);
   }catch(Exception error){
    String code=failure(error);
    String status=code.equals("CONEXION")&&attempts<3?"PENDING":code.equals("ENVIO_INCIERTO")?"UNCERTAIN":"FAILED";
    jdbc.update("UPDATE avisos_correo SET estado=?,intentos=?,ultimo_error=?,siguiente_intento=TIMESTAMPADD(MINUTE,5,UTC_TIMESTAMP(6)) WHERE id=?",status,attempts,code,id);
    // Una credencial rechazada afectaría a todos: detener el lote de envíos.
    if(code.equals("AUTH_RECHAZADA")){
     jdbc.update("UPDATE avisos_correo SET estado='PENDING',siguiente_intento=TIMESTAMPADD(MINUTE,5,UTC_TIMESTAMP(6)) WHERE destinatario=? AND estado='PROCESSING'",recipient);
     break;
    }
   }
  }
 }
 static String body(Map<String,Object> lot,String state){
  int days=((Number)lot.get("days")).intValue();
  String when=days<0?"Venció hace "+(-days)+" día(s)":days==0?"Vence hoy":days==1?"Vence mañana":"Vence en "+days+" días";
  return String.join("\n","Semáforo Digital Mass · Aviso de vencimiento","",
   "Producto: "+lot.get("product"),"Lote: "+lot.get("code"),"Fecha de vencimiento: "+lot.get("expiry"),
   "Aviso: "+when,"Estado: "+state,"Stock disponible: "+lot.get("balance")+" "+lot.get("unit"),"",
   "Revisa la sección Alertas de la aplicación para atender este lote.",
   "Este mensaje no realiza ventas, promociones ni retiros automáticamente.");
 }
 static String failure(Exception error){
  for(Throwable cause=error;cause!=null;cause=cause.getCause()){
   if(cause instanceof AuthenticationFailedException)return "AUTH_RECHAZADA";
   if(cause instanceof ResetMailer.MailApiRejected rejected)return rejected.status==401||rejected.status==403?"AUTH_RECHAZADA":"RECHAZO_PROVEEDOR";
   if(cause instanceof ConnectException||cause instanceof UnknownHostException)return "CONEXION";
  }
  return "ENVIO_INCIERTO";
 }
}

@RestController @RequestMapping("/api/admin/email-alerts")
class EmailAlertsAdmin {
 final EmailAlerts alerts;final JdbcTemplate jdbc;final InventoryService inventory;
 EmailAlertsAdmin(EmailAlerts alerts,JdbcTemplate jdbc,InventoryService inventory){this.alerts=alerts;this.jdbc=jdbc;this.inventory=inventory;}
 @GetMapping Object status(){return Map.of("enabled",alerts.enabled,"smtpConfigured",alerts.mailer.configured(),
  "recipient",alerts.recipient,"items",jdbc.queryForList("SELECT c.id,c.lote_id,c.estado_lote,c.estado,c.intentos,c.aceptado_en,c.ultimo_error,l.code,p.name product FROM avisos_correo c JOIN lot l ON l.id=c.lote_id JOIN product p ON p.id=l.product_id ORDER BY c.id DESC LIMIT 50"));}
 @PostMapping("/test/{lotId}") Object test(@PathVariable long lotId,@org.springframework.security.core.annotation.AuthenticationPrincipal Security.Actor actor){
  if(!alerts.mailer.configured()||alerts.recipient.isEmpty())throw new IllegalArgumentException("Configura SMTP y el destinatario antes de probar.");
  LocalDate day=LocalDate.now(alerts.clock.withZone(Rules.LIMA));
  var lots=jdbc.queryForList("SELECT CASE WHEN l.expiry<? THEN 'VENCIDO' ELSE 'ROJO' END state FROM lot l JOIN product p ON p.id=l.product_id WHERE l.id=? AND p.active=true AND l.normal+l.promo>0 AND DATEDIFF(l.expiry,?)<=p.critical_days",day,lotId,day);
  if(lots.isEmpty())throw new IllegalArgumentException("Elige un lote crítico o vencido con stock.");
  String state=lots.getFirst().get("state").toString();
  jdbc.update("INSERT IGNORE INTO avisos_correo(lote_id,estado_lote,destinatario) VALUES(?,?,?)",lotId,state,alerts.recipient);
  Long id=jdbc.queryForObject("SELECT id FROM avisos_correo WHERE lote_id=? AND estado_lote=? AND destinatario=?",Long.class,lotId,state,alerts.recipient);
  inventory.audit(actor,"PRUEBA_CORREO","correo:"+id,"Prueba de un solo lote al destinatario configurado");
  alerts.deliver(id);
  return jdbc.queryForMap("SELECT id,estado,intentos,ultimo_error FROM avisos_correo WHERE id=?",id);
 }
 @PostMapping("/baseline") @org.springframework.transaction.annotation.Transactional Object baseline(@org.springframework.security.core.annotation.AuthenticationPrincipal Security.Actor actor){
  if(alerts.enabled)throw new IllegalArgumentException("La línea inicial solo se registra con el envío automático desactivado.");
  if(alerts.recipient.isEmpty())throw new IllegalArgumentException("Configura el destinatario.");
  alerts.enqueue();
  int count=jdbc.update("UPDATE avisos_correo SET estado='BASELINE' WHERE destinatario=? AND estado='PENDING'",alerts.recipient);
  inventory.audit(actor,"INICIO_CORREO","tienda","Se reconocieron "+count+" avisos existentes sin enviar correo; los avisos nuevos se procesarán al activar.");
  return Map.of("recognized",count);
 }
 @PostMapping("/{id}/retry") Object retry(@PathVariable long id,@org.springframework.security.core.annotation.AuthenticationPrincipal Security.Actor actor){
  if(!alerts.enabled)throw new IllegalArgumentException("Activa la configuración de correo antes de reintentar.");
  if(jdbc.update("UPDATE avisos_correo SET estado='PENDING',intentos=0,siguiente_intento=UTC_TIMESTAMP(6) WHERE id=? AND estado='FAILED' AND destinatario=?",id,alerts.recipient)!=1)throw new IllegalArgumentException("Solo se pueden reintentar avisos fallidos del destinatario actual.");
  inventory.audit(actor,"REINTENTO_CORREO","correo:"+id,"Reintento manual de aviso fallido");
  return Map.of("status","PENDING");
 }
}
