package pe.mass.semaforo;

import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/admin/database")
class DatabaseViewer {
 final JdbcTemplate jdbc;
 DatabaseViewer(JdbcTemplate jdbc){this.jdbc=jdbc;}
 static final Map<String,String> TABLES=new LinkedHashMap<>();
 static {
  TABLES.put("solicitudes_clave","id,usuario_id,estado,creada_el,resuelta_el,resuelta_por");
  TABLES.put("empleado","usuario_id id,supervisor_id");
  TABLES.put("supervisor","usuario_id id");
  TABLES.put("programador","usuario_id id");
  TABLES.put("usuarios","id,dni,name nombre,email correo,role rol,active activo,must_change cambio_clave_obligatorio");
  TABLES.put("productos","id,sku,ean,name nombre,category categoria,unit unidad,active activo,warning_days dias_aviso,critical_days dias_criticos");
  TABLES.put("lotes","id,product_id producto_id,code codigo,received recepcion,expiry vencimiento,initial_quantity cantidad_inicial,normal saldo_normal,promo saldo_promocion,cost costo_unitario,created_by creador_id,created_at creado_el");
  TABLES.put("movimientos","id,lot_id lote_id,actor_id responsable_id,type tipo,source origen,quantity cantidad,normal_delta cambio_normal,promo_delta cambio_promocion,cost costo_unitario,reason motivo,reversal_of revierte_movimiento_id,created_at creado_el");
  TABLES.put("alertas","id,lot_id lote_id,state estado,attended_at atendida_el,attended_by atendida_por,created_at creada_el");
  TABLES.put("auditoria","id,actor_id responsable_id,action accion,entity entidad,detail detalle,created_at creado_el");
  TABLES.put("evaluaciones","id,day fecha,started_at inicio,finished_at fin,lots lotes,status estado,error");
  TABLES.put("notificaciones","id,alert_id alerta_id,status estado,attempts intentos,next_attempt proximo_intento,last_error ultimo_error,accepted_at aceptada_el,delivered_at entregada_el");
  TABLES.put("sesiones","id,user_id usuario_id,expires_at vence_el,revoked revocada");
  TABLES.put("renovaciones_sesion","ROW_NUMBER() OVER (ORDER BY session_id,hash) id,session_id sesion_id,used utilizada");
  TABLES.put("recuperaciones_clave","ROW_NUMBER() OVER (ORDER BY created_at,hash) id,user_id usuario_id,created_at creada_el,expires_at vence_el,used utilizada,delivery estado_correo");
  TABLES.put("sesiones_chat","ROW_NUMBER() OVER (ORDER BY session_id,hash) id,session_id sesion_id,expires_at vence_el");
  TABLES.put("operaciones_idempotentes","ROW_NUMBER() OVER (ORDER BY actor_id,request_key) id,actor_id responsable_id,result_id resultado_id");
  TABLES.put("recepciones_servicio","event_id id,accepted_at aceptada_el");
  TABLES.put("bloqueo_tienda","id");
 }
 @GetMapping Object tables(){return TABLES.keySet();}
 @GetMapping("/{table}") Object rows(@PathVariable String table,@RequestParam(defaultValue="0") int page){
  if(!TABLES.containsKey(table))throw new IllegalArgumentException("Tabla no disponible.");
  var rows=jdbc.queryForList("SELECT "+TABLES.get(table)+" FROM "+table+" ORDER BY id LIMIT 26 OFFSET ?",InventoryService.offset(page));
  boolean more=rows.size()>25;
  return Map.of("items",more?rows.subList(0,25):rows,"page",page,"hasNext",more,"total",Objects.requireNonNull(jdbc.queryForObject("SELECT COUNT(*) FROM "+table,Long.class)));
 }
}
