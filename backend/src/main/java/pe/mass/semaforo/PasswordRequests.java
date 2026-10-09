package pe.mass.semaforo;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
@RestController @RequestMapping("/api")
class PasswordRequests {
 final JdbcTemplate jdbc;final InventoryService inventory;
 PasswordRequests(JdbcTemplate jdbc,InventoryService inventory){this.jdbc=jdbc;this.inventory=inventory;}
 @PostMapping("/password-requests") @Transactional Object request(@AuthenticationPrincipal Security.Actor actor){
  if(!List.of("WORKER","SUPERVISOR").contains(actor.role()))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Solo empleado o supervisor.");
  jdbc.queryForList("SELECT id FROM app_user WHERE id=? FOR UPDATE",actor.id());
  int n=jdbc.update("INSERT IGNORE INTO solicitudes_clave(usuario_id,creada_el) VALUES(?,UTC_TIMESTAMP(6))",actor.id());
  if(n>0)inventory.audit(actor,"SOLICITAR_CLAVE","usuario:"+actor.id(),"Solicitud enviada al programador");
  return Map.of("message",n>0?"Solicitud enviada al programador.":"Ya tienes una solicitud pendiente.");
 }
 @GetMapping("/password-requests") Object own(@AuthenticationPrincipal Security.Actor a){return jdbc.queryForList("SELECT id,estado,creada_el,resuelta_el FROM solicitudes_clave WHERE usuario_id=? ORDER BY id DESC LIMIT 10",a.id());}
 @GetMapping("/admin/password-requests") Object pending(@RequestParam(defaultValue="0") int page){
  var rows=jdbc.queryForList("SELECT u.id,u.name,u.dni,u.role,u.active,r.id solicitud_id,r.creada_el FROM solicitudes_clave r JOIN app_user u ON u.id=r.usuario_id WHERE r.estado='PENDIENTE' ORDER BY r.id LIMIT 26 OFFSET ?",InventoryService.offset(page));
  return Map.of("items",rows.size()>25?rows.subList(0,25):rows,"hasNext",rows.size()>25,"total",Objects.requireNonNull(jdbc.queryForObject("SELECT COUNT(*) FROM solicitudes_clave WHERE estado='PENDIENTE'",Long.class)));
 }
}
