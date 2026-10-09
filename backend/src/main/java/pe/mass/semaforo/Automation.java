package pe.mass.semaforo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.net.URI;
import java.net.http.*;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.bind.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController @RequestMapping("/api")
class Automation {
 @Value("${app.n8n-chat-url:}") String chatUrl;
 @Value("${app.n8n-token}") String n8nToken;
 final ObjectMapper mapper=new ObjectMapper().findAndRegisterModules();
 final EvaluationService evaluation;final JdbcTemplate jdbc;final InventoryService inventory;
 Automation(EvaluationService evaluation,JdbcTemplate jdbc,InventoryService inventory){this.evaluation=evaluation;this.jdbc=jdbc;this.inventory=inventory;}
 @PostMapping("/admin/evaluation") Object run(@AuthenticationPrincipal Security.Actor actor){inventory.audit(actor,"REVISION_MANUAL","tienda","Revisión solicitada");return evaluation.run(false);}
 @GetMapping("/admin/outbox") Object outbox(){return jdbc.queryForList("SELECT * FROM outbox ORDER BY next_attempt DESC LIMIT 100");}
 @GetMapping("/admin/outbox/page") Object outboxPage(@RequestParam(defaultValue="0") int page){
  var rows=jdbc.queryForList("SELECT * FROM outbox ORDER BY next_attempt DESC,id LIMIT 26 OFFSET ?",InventoryService.offset(page));
  boolean more=rows.size()>25;return Map.of("items",more?rows.subList(0,25):rows,"page",page,"hasNext",more);
 }
 @PostMapping("/admin/outbox/{id}/retry") @org.springframework.transaction.annotation.Transactional Object retry(@AuthenticationPrincipal Security.Actor a,@PathVariable String id){
  try{UUID.fromString(id);}catch(IllegalArgumentException e){throw new IllegalArgumentException("Evento inválido.");}
  int changed=jdbc.update("UPDATE outbox SET status='PENDING',attempts=0,next_attempt=UTC_TIMESTAMP(6) WHERE id=? AND status='FAILED'",id);
  if(changed!=1)throw new IllegalArgumentException("Solo se pueden reintentar eventos fallidos.");
  inventory.audit(a,"REINTENTO_NOTIFICACION","evento:"+id,"Nueva ronda de hasta cinco intentos; conserva identificador para deduplicación");
  return Map.of("id",id,"status","PENDING");
 }
 record Chat(@NotNull @Pattern(regexp="criticos|vencimientos|mermas|ayuda") String query){}
 @PostMapping("/admin/chat") Object chat(@AuthenticationPrincipal Security.Actor a,@Valid @RequestBody Chat in){
  Object data=switch(in.query()){
   case "criticos" -> inventory.lots(a,"","ROJO",0,false);
   case "vencimientos" -> inventory.lots(a,"","VENCIDO",0,false);
   case "mermas" -> wasteSummary();
   default -> "Registre productos y lotes. Atienda cantidades parciales desde Alertas. Los vencidos solo permiten retiro por merma. Reportes exporta CSV. Las cantidades se expresan en la unidad base del producto.";
  };
  if(!chatUrl.isBlank() && n8nToken.length()>=32){
   try{
    String response=WebhookClient.post(chatUrl,n8nToken,mapper.writeValueAsString(Map.of("query",in.query(),"data",data)));
    return Map.of("mode","Consultas estructuradas mediante n8n; no es IA generativa","data",mapper.readTree(response).path("data"));
   }catch(Exception e){if(e instanceof InterruptedException)Thread.currentThread().interrupt();}
  }
  return Map.of("mode","Consultas estructuradas locales (respaldo); no es IA generativa","data",data);
 }
 @PostMapping({"/admin/chat/session","/chat/session"}) Object chatSession(@AuthenticationPrincipal Security.Actor a,jakarta.servlet.http.HttpServletResponse res,@Value("${app.secure-cookie}") boolean secure){
  String token=AuthService.random();
  jdbc.update("INSERT INTO chat_session(hash,session_id,expires_at) VALUES(?,?,TIMESTAMPADD(HOUR,1,UTC_TIMESTAMP(6)))",AuthService.hash(token),a.session());
  res.addHeader("Set-Cookie",org.springframework.http.ResponseCookie.from("mass_chat",token).httpOnly(true).secure(secure).sameSite("Strict").path("/chat").maxAge(Duration.ofHours(1)).build().toString());
  return Map.of("ready",true);
 }
 @GetMapping("/chat/context") Object chatContext(@AuthenticationPrincipal Security.Actor actor){
  var lots=jdbc.queryForList("SELECT p.name product,p.unit,l.code,l.expiry,DATEDIFF(l.expiry,?) days,l.normal,l.promo,CASE WHEN DATEDIFF(l.expiry,?)<0 THEN 'VENCIDO' WHEN DATEDIFF(l.expiry,?)<=p.critical_days THEN 'ROJO' ELSE 'AMARILLO' END state FROM lot l JOIN product p ON p.id=l.product_id WHERE l.normal+l.promo>0 AND DATEDIFF(l.expiry,?)<=p.warning_days ORDER BY l.expiry,l.id LIMIT 51",inventory.today(),inventory.today(),inventory.today(),inventory.today());
  boolean more=lots.size()>50;
  return Map.of("dateLima",inventory.today().toString(),"alerts",more?lots.subList(0,50):lots,"moreAlerts",more,"waste",actor.role().equals("ADMIN")?wasteSummary():quantityWaste(),"scope","Una tienda; inventario manual. Alertas: primeros 50 lotes. Mermas: todos los movimientos netos de reversiones, por unidad. Sin POS/ERP ni capacidad de modificar datos.");
 }
 Object quantityWaste(){return jdbc.queryForList("SELECT p.unit,SUM(CASE WHEN m.type='MERMA' THEN m.quantity ELSE -o.quantity END) wasteQuantity FROM movement m JOIN lot l ON l.id=m.lot_id JOIN product p ON p.id=l.product_id LEFT JOIN movement o ON o.id=m.reversal_of WHERE m.type='MERMA' OR (m.type='REVERSO' AND o.type='MERMA') GROUP BY p.unit");}
 Object wasteSummary(){
  return jdbc.queryForList("SELECT unit,SUM(q) wasteQuantity,COALESCE(SUM(q*cost),0) valuedLoss,SUM(CASE WHEN cost IS NULL THEN q ELSE 0 END) pendingQuantity,CASE WHEN SUM(CASE WHEN cost IS NULL THEN q ELSE 0 END)<>0 THEN 'PARCIAL: HAY COSTOS PENDIENTES' ELSE 'COMPLETA' END valuation FROM (SELECT p.unit,CASE WHEN m.type='MERMA' THEN m.quantity ELSE -o.quantity END q,CASE WHEN m.type='MERMA' THEN COALESCE(m.cost,l.cost) ELSE COALESCE(o.cost,l.cost) END cost FROM movement m JOIN lot l ON l.id=m.lot_id JOIN product p ON p.id=l.product_id LEFT JOIN movement o ON o.id=m.reversal_of WHERE m.type='MERMA' OR (m.type='REVERSO' AND o.type='MERMA')) w GROUP BY unit");
 }
 // Narrow service read: no personnel, DNI or cost; token does not grant access to admin APIs.
 @GetMapping("/service/critical") Object critical(){return jdbc.queryForList("SELECT l.id,l.code,l.expiry,p.name product,p.unit,l.normal+l.promo balance FROM lot l JOIN product p ON p.id=l.product_id WHERE l.normal+l.promo>0 AND DATEDIFF(l.expiry,?)<=p.critical_days ORDER BY l.expiry LIMIT 100",inventory.today());}
 record Receipt(@NotNull @Pattern(regexp="[a-f0-9-]{36}") String eventId){}
 @PostMapping("/service/receipt") Object receipt(@Valid @RequestBody Receipt input){
  if(Objects.requireNonNull(jdbc.queryForObject("SELECT COUNT(*) FROM outbox WHERE id=?",Integer.class,input.eventId()))!=1)throw new IllegalArgumentException("Evento desconocido.");
  int inserted=jdbc.update("INSERT IGNORE INTO service_receipt(event_id,accepted_at) VALUES(?,UTC_TIMESTAMP(6))",input.eventId());
  return Map.of("accepted",true,"eventId",input.eventId(),"duplicate",inserted==0,"delivery","NOT_CONFIGURED");
 }
}

@Service
class EvaluationService {
 final JdbcTemplate jdbc;final Clock clock;final TransactionTemplate tx;
 EvaluationService(JdbcTemplate jdbc,Clock clock,PlatformTransactionManager manager){this.jdbc=jdbc;this.clock=clock;tx=new TransactionTemplate(manager);}
 @EventListener(ApplicationReadyEvent.class) public void startup(){run(true);}
 @Scheduled(cron="0 5 0 * * *",zone="America/Lima") public void daily(){run(false);}
 public Object run(boolean catchup){
  LocalDate day=LocalDate.now(clock.withZone(Rules.LIMA));Instant start=clock.instant();
  try{return tx.execute(s->{
   jdbc.queryForList("SELECT id FROM store_lock WHERE id=1 FOR UPDATE");
   if(catchup&&jdbc.queryForObject("SELECT COUNT(*) FROM evaluation WHERE day=? AND status='OK'",Long.class,day)>0)return Map.of("status","AL_DIA");
   jdbc.update("INSERT INTO evaluation(day,started_at,status) VALUES(?,?,'RUNNING')",day,java.sql.Timestamp.from(start));
   Long id=jdbc.queryForObject("SELECT LAST_INSERT_ID()",Long.class);
   int count=jdbc.queryForObject("SELECT COUNT(*) FROM lot WHERE normal+promo>0",Integer.class);
   jdbc.update("INSERT IGNORE INTO alert(lot_id,state,created_at) SELECT l.id,CASE WHEN DATEDIFF(l.expiry,?)<0 THEN 'VENCIDO' WHEN DATEDIFF(l.expiry,?)<=p.critical_days THEN 'ROJO' ELSE 'AMARILLO' END,? FROM lot l JOIN product p ON p.id=l.product_id WHERE normal+promo>0 AND DATEDIFF(l.expiry,?)<=p.warning_days",day,day,java.sql.Timestamp.from(start),day);
   jdbc.update("INSERT IGNORE INTO outbox(id,alert_id,next_attempt) SELECT UUID(),a.id,? FROM alert a JOIN lot l ON l.id=a.lot_id JOIN product p ON p.id=l.product_id WHERE l.normal+l.promo>0 AND a.state=CASE WHEN DATEDIFF(l.expiry,?)<0 THEN 'VENCIDO' WHEN DATEDIFF(l.expiry,?)<=p.critical_days THEN 'ROJO' ELSE 'NONE' END",java.sql.Timestamp.from(start),day,day);
   jdbc.update("UPDATE evaluation SET status='OK',lots=?,finished_at=? WHERE id=?",count,java.sql.Timestamp.from(clock.instant()),id);
   return Map.of("status","OK","lots",count,"milliseconds",Duration.between(start,clock.instant()).toMillis());
  });}catch(RuntimeException e){jdbc.update("INSERT INTO evaluation(day,started_at,finished_at,status,error) VALUES(?,?,?,'ERROR',?)",day,java.sql.Timestamp.from(start),java.sql.Timestamp.from(clock.instant()),e.getClass().getSimpleName());throw e;}
 }
}

@Service
class OutboxDelivery {
 final JdbcTemplate jdbc;final TransactionTemplate tx;final String url,token;final ObjectMapper json;final Clock clock;
 OutboxDelivery(JdbcTemplate jdbc,PlatformTransactionManager manager,ObjectMapper json,Clock clock,@Value("${app.n8n-url}") String url,@Value("${app.n8n-token}") String token){this.jdbc=jdbc;tx=new TransactionTemplate(manager);this.json=json;this.clock=clock;this.url=url;this.token=token;}
 @Scheduled(fixedDelay=30000) public void send(){
  if(url.isBlank()||token.length()<32)return;
  // Recover leases after a crash. n8n must deduplicate event IDs before any external send.
  jdbc.update("UPDATE outbox SET status='PENDING' WHERE status='PROCESSING' AND next_attempt<UTC_TIMESTAMP(6)");
  var rows=tx.execute(s->{
   var pending=jdbc.queryForList("SELECT o.id,o.alert_id,o.attempts FROM outbox o WHERE status='PENDING' AND next_attempt<=UTC_TIMESTAMP(6) ORDER BY next_attempt LIMIT 10 FOR UPDATE SKIP LOCKED");
   for(var row:pending)jdbc.update("UPDATE outbox SET status='PROCESSING',next_attempt=TIMESTAMPADD(MINUTE,5,UTC_TIMESTAMP(6)) WHERE id=?",row.get("id"));return pending;
  });
  for(var row:Objects.requireNonNull(rows)){
   String id=row.get("id").toString();int attempts=((Number)row.get("attempts")).intValue()+1;
   try{
    var payload=jdbc.queryForList("SELECT a.state,l.code,l.expiry,p.name product,p.unit,l.normal+l.promo balance FROM alert a JOIN lot l ON l.id=a.lot_id JOIN product p ON p.id=l.product_id WHERE a.id=? AND l.normal+l.promo>0 AND a.state=CASE WHEN DATEDIFF(l.expiry,?)<0 THEN 'VENCIDO' WHEN DATEDIFF(l.expiry,?)<=p.critical_days THEN 'ROJO' ELSE 'NONE' END",row.get("alert_id"),LocalDate.now(clock.withZone(Rules.LIMA)),LocalDate.now(clock.withZone(Rules.LIMA)));
    if(payload.isEmpty()){jdbc.update("UPDATE outbox SET status='OBSOLETE' WHERE id=?",id);continue;}
    String res=WebhookClient.post(url,token,json.writeValueAsString(Map.of("eventId",id,"data",payload.getFirst())));
    var response=json.readTree(res);if(!response.path("accepted").asBoolean()||!id.equals(response.path("eventId").asText()))throw new IllegalStateException("Respuesta n8n inválida");
    jdbc.update("UPDATE outbox SET status='ACCEPTED',attempts=?,accepted_at=UTC_TIMESTAMP(6),last_error=NULL WHERE id=?",attempts,id);
   }catch(Exception e){if(e instanceof InterruptedException)Thread.currentThread().interrupt();jdbc.update("UPDATE outbox SET status=?,attempts=?,next_attempt=?,last_error=? WHERE id=?",attempts>=5?"FAILED":"PENDING",attempts,java.sql.Timestamp.from(clock.instant().plusSeconds(30L*(1L<<attempts))),e.getClass().getSimpleName(),id);}
  }
 }
}

