package pe.mass.semaforo;

import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.*;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController @RequestMapping("/api/admin")
class Admin {
 @PersistenceContext EntityManager em;
 final JdbcTemplate jdbc; final PasswordEncoder encoder;final InventoryService inventory;
 Admin(JdbcTemplate jdbc,PasswordEncoder encoder,InventoryService inventory){this.jdbc=jdbc;this.encoder=encoder;this.inventory=inventory;}
 record Person(@NotNull @Pattern(regexp="[0-9]{8}") String dni,@NotBlank @Size(max=100) String name,
  @NotNull @Pattern(regexp="ADMIN|SUPERVISOR|WORKER") String role,@Size(min=12,max=72) String password,boolean active,@Email @Size(max=254) String email){}
 record Reset(@NotNull @Size(min=12,max=72) String password){}
 @GetMapping("/users") Object users(@RequestParam(defaultValue="0") int page){return jdbc.queryForList("SELECT u.id,u.dni,u.name,u.email,u.role,u.active,u.must_change,e.supervisor_id FROM app_user u LEFT JOIN empleado e ON e.usuario_id=u.id ORDER BY u.id LIMIT 25 OFFSET ?",InventoryService.offset(page));}
 record Assignment(@NotNull Long supervisorId){}
 @PutMapping("/users/{id}/supervisor") @Transactional Object assign(@AuthenticationPrincipal Security.Actor a,@PathVariable long id,@Valid @RequestBody Assignment in){
  jdbc.queryForList("SELECT id FROM store_lock WHERE id=1 FOR UPDATE");
  if(jdbc.queryForObject("SELECT COUNT(*) FROM app_user WHERE id=? AND role='SUPERVISOR' AND active=true",Integer.class,in.supervisorId())!=1)throw new IllegalArgumentException("Seleccione un supervisor activo.");
  if(jdbc.update("UPDATE empleado SET supervisor_id=? WHERE usuario_id=?",in.supervisorId(),id)!=1)throw new IllegalArgumentException("La cuenta debe ser empleado.");
  inventory.audit(a,"ASIGNAR_SUPERVISOR","usuario:"+id,"Supervisor:"+in.supervisorId());return Map.of("id",id);
 }
 @DeleteMapping("/users/{id}") @Transactional Object remove(@AuthenticationPrincipal Security.Actor a,@PathVariable long id){User u=em.find(User.class,id);if(u==null)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Usuario no encontrado.");return save(a,id,new Person(u.dni,u.name,u.role,null,false,u.email));}
 @PostMapping("/users") @Transactional Object create(@AuthenticationPrincipal Security.Actor a,@Valid @RequestBody Person in){return save(a,null,in);}
 @PutMapping("/users/{id}") @Transactional Object update(@AuthenticationPrincipal Security.Actor a,@PathVariable long id,@Valid @RequestBody Person in){return save(a,id,in);}
 Object save(Security.Actor a,Long id,Person in){
  jdbc.queryForList("SELECT id FROM store_lock WHERE id=1 FOR UPDATE");
  User u=id==null?new User():em.find(User.class,id,LockModeType.PESSIMISTIC_WRITE);
  if(u==null)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Usuario no encontrado.");
  if(id!=null&&u.active&&u.role.equals("ADMIN")&&(!in.active()||!in.role().equals("ADMIN"))&&Objects.requireNonNull(jdbc.queryForObject("SELECT COUNT(*) FROM app_user WHERE active=true AND role='ADMIN'",Long.class))<=1)throw new IllegalArgumentException("Debe conservar un administrador activo.");
  if(id==null&&in.password()==null)throw new IllegalArgumentException("Contraseña inicial obligatoria.");
  String before=id==null?"nuevo":u.name+"/"+u.role+"/activo="+u.active;
  String email=in.email()==null||in.email().isBlank()?null:in.email().strip().toLowerCase(Locale.ROOT);
  boolean emailChanged=!Objects.equals(u.email,email);
  u.dni=in.dni();u.name=in.name();u.role=in.role();u.active=in.active();u.email=email;
  if(!u.role.equals("ADMIN"))u.mustChange=false;
  if(id==null){u.password=encoder.encode(in.password());em.persist(u);}else if(in.password()!=null)throw new IllegalArgumentException("Use el restablecimiento de contraseña.");
  em.flush();jdbc.update("UPDATE auth_session SET revoked=true WHERE user_id=?",u.id);
  inventory.audit(a,"PERSONAL","usuario:"+u.id,before+" → "+u.name+"/"+u.role+"/activo="+u.active+"; correo modificado="+emailChanged);return Map.of("id",u.id);
 }
 @PostMapping("/users/{id}/reset") @Transactional Object reset(@AuthenticationPrincipal Security.Actor a,@PathVariable long id,@Valid @RequestBody Reset in){
  User u=em.find(User.class,id,LockModeType.PESSIMISTIC_WRITE);if(u==null)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Usuario no encontrado.");
  if(in.password().getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72)throw new IllegalArgumentException("La contraseña supera 72 bytes UTF-8.");
  jdbc.update("UPDATE solicitudes_clave SET estado='RESUELTA',resuelta_el=UTC_TIMESTAMP(6),resuelta_por=? WHERE usuario_id=? AND estado='PENDIENTE'",a.id(),id);
  u.password=encoder.encode(in.password());u.mustChange=u.role.equals("ADMIN");jdbc.update("UPDATE auth_session SET revoked=true WHERE user_id=?",id);inventory.audit(a,"RESTABLECER_PASSWORD","usuario:"+id,"Clave asignada por programador; sesiones revocadas");return Map.of("id",id);
 }
 @GetMapping("/audit") Object audit(@RequestParam(defaultValue="0") int page){return jdbc.queryForList("SELECT a.id,u.name actor,a.action,a.entity,a.detail,a.created_at FROM audit a LEFT JOIN app_user u ON u.id=a.actor_id ORDER BY a.id DESC LIMIT 25 OFFSET ?",InventoryService.offset(page));}
 @GetMapping("/reports") Object reports(@RequestParam LocalDate from,@RequestParam LocalDate to,@RequestParam(required=false) Long productId,@RequestParam(defaultValue="") String type,@RequestParam(defaultValue="0") int page){return report(from,to,productId,type,page,false);}
 @GetMapping("/reports/summary") Object summary(@RequestParam LocalDate from,@RequestParam LocalDate to,@RequestParam(required=false) Long productId,@RequestParam(defaultValue="") String type){
  var groups=new LinkedHashMap<String,Map<String,Object>>();
  for(var row:report(from,to,productId,type,0,true)){
   String unit=row.get("unit").toString();var group=groups.computeIfAbsent(unit,k->{var g=new LinkedHashMap<String,Object>();g.put("unit",unit);g.put("wasteQuantity",BigDecimal.ZERO);g.put("valuedLoss",BigDecimal.ZERO);g.put("pendingQuantity",BigDecimal.ZERO);g.put("movements",0);return g;});
   group.put("movements",((Integer)group.get("movements"))+1);
   BigDecimal waste=(BigDecimal)row.get("waste_quantity");
   group.put("wasteQuantity",((BigDecimal)group.get("wasteQuantity")).add(waste));
   if(row.get("loss")!=null)group.put("valuedLoss",((BigDecimal)group.get("valuedLoss")).add((BigDecimal)row.get("loss")));
   if("PENDIENTE".equals(row.get("valuation")))group.put("pendingQuantity",((BigDecimal)group.get("pendingQuantity")).add(waste));
  }
  return groups.values();
 }
 @GetMapping(value="/reports.csv",produces="text/csv;charset=UTF-8") ResponseEntity<String> csv(@RequestParam LocalDate from,@RequestParam LocalDate to,@RequestParam(required=false) Long productId,@RequestParam(defaultValue="") String type){
  var rows=report(from,to,productId,type,0,true);
  StringBuilder text=new StringBuilder("\ufeffFecha Lima;Producto;Lote;Unidad;Acción;Cantidad;Responsable;Motivo;Pérdida PEN;Valorización\r\n");
  for(var r:rows){StringJoiner line=new StringJoiner(";");for(String field:List.of("created_at","product","code","unit","type","quantity","actor","reason","loss","valuation")){Object value=r.get(field);if(field.equals("created_at")&&value instanceof java.sql.Timestamp stamp)value=stamp.toInstant().atZone(Rules.LIMA).format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/uuuu HH:mm:ss"));line.add(csvCell(value));}text.append(line).append("\r\n");}
  return ResponseEntity.ok().header("Content-Disposition","attachment; filename=movimientos.csv").body(text.toString());
 }
 static String csvCell(Object value){String s=value==null?"":value.toString();if(s.stripLeading().matches("^[=+@\\-].*")||s.startsWith("\t")||s.startsWith("\r"))s="'"+s;return "\""+s.replace("\"","\"\"")+"\"";}
 List<Map<String,Object>> report(LocalDate from,LocalDate to,Long productId,String type,int page,boolean export){
  if(to.isBefore(from)||from.plusYears(1).isBefore(to))throw new IllegalArgumentException("Seleccione un rango de hasta un año.");
  if(!type.isEmpty()&&!List.of("INGRESO","PROMOCION","VENTA","MERMA","AJUSTE","REVERSO").contains(type))throw new IllegalArgumentException("Acción inválida.");
  var args=new ArrayList<Object>();args.add(java.sql.Timestamp.from(from.atStartOfDay(Rules.LIMA).toInstant()));args.add(java.sql.Timestamp.from(to.plusDays(1).atStartOfDay(Rules.LIMA).toInstant()));
  String sql="SELECT m.id,m.created_at,p.name product,l.code,p.unit,m.type,m.quantity,u.name actor,m.reason, CASE WHEN m.type='MERMA' THEN m.quantity*COALESCE(m.cost,l.cost) WHEN m.type='REVERSO' AND original.type='MERMA' THEN -original.quantity*COALESCE(original.cost,l.cost) ELSE NULL END loss, CASE WHEN (m.type='MERMA' OR original.type='MERMA') AND COALESCE(m.cost,original.cost,l.cost) IS NULL THEN 'PENDIENTE' ELSE 'VALORIZADO' END valuation FROM movement m JOIN lot l ON l.id=m.lot_id JOIN product p ON p.id=l.product_id JOIN app_user u ON u.id=m.actor_id LEFT JOIN movement original ON original.id=m.reversal_of WHERE m.created_at>=? AND m.created_at<?";
  sql=sql.replace("SELECT m.id,","SELECT CASE WHEN m.type='MERMA' THEN m.quantity WHEN m.type='REVERSO' AND original.type='MERMA' THEN -original.quantity ELSE 0 END waste_quantity,m.id,");
  if(productId!=null){sql+=" AND p.id=?";args.add(productId);}if(!type.isEmpty()){sql+=" AND m.type=?";args.add(type);}
  sql+=" ORDER BY m.id DESC LIMIT ? OFFSET ?";args.add(export?10001:25);args.add(export?0:InventoryService.offset(page));
  var rows=jdbc.queryForList(sql,args.toArray());if(export&&rows.size()>10000)throw new IllegalArgumentException("Exportación supera 10000 movimientos; reduzca el rango.");return rows;
 }
}

