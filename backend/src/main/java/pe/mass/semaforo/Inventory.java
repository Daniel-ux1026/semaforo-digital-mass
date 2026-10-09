package pe.mass.semaforo;

import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController @RequestMapping("/api")
class Inventory {
 final InventoryService service;
 Inventory(InventoryService service){this.service=service;}
 record ProductInput(@NotBlank @Size(max=40) String sku,@Size(max=13) String ean,@NotBlank @Size(max=120) String name,
  @NotBlank @Size(max=80) String category,@Pattern(regexp="UNIDAD|KG|LITRO") @NotNull String unit,
  @Min(1) @Max(3650) int warningDays,@Min(0) int criticalDays,boolean active){}
 record LotInput(@NotNull Long productId,@NotBlank @Size(max=60) String code,@NotNull LocalDate received,
  @NotNull LocalDate expiry,@NotNull @Digits(integer=11,fraction=3) BigDecimal quantity,
  @DecimalMin("0") @Digits(integer=10,fraction=4) BigDecimal cost){}
 record Action(@Pattern(regexp="PROMOCION|VENTA|MERMA|AJUSTE|REVERSO") @NotNull String type,
  @Pattern(regexp="NORMAL|PROMO") @NotNull String source,@NotNull @Digits(integer=11,fraction=3) BigDecimal quantity,
  @NotBlank @Size(max=500) String reason,Long reversalOf){}
 record Cost(@NotNull @DecimalMin("0") @Digits(integer=10,fraction=4) BigDecimal cost,@NotBlank @Size(max=500) String reason){}
 @GetMapping("/products") Object products(@RequestParam(defaultValue="") String q,@RequestParam(defaultValue="0") int page){return service.products(q,page);}
 @GetMapping("/products/barcode/{ean}") Object barcode(@PathVariable String ean){return service.barcode(ean);}
 @PostMapping("/admin/products") Object product(@AuthenticationPrincipal Security.Actor a,@Valid @RequestBody ProductInput p){return service.product(a,null,p);}
 @PutMapping("/admin/products/{id}") Object product(@AuthenticationPrincipal Security.Actor a,@PathVariable long id,@Valid @RequestBody ProductInput p){return service.product(a,id,p);}
 @DeleteMapping("/admin/products/{id}") Object removeProduct(@AuthenticationPrincipal Security.Actor a,@PathVariable long id){return service.removeProduct(a,id);}
 @GetMapping("/lots") Object lots(@AuthenticationPrincipal Security.Actor a,@RequestParam(defaultValue="") String q,@RequestParam(defaultValue="") String state,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="false") boolean exhausted){return service.lots(a,q,state,page,exhausted);}
 @GetMapping("/alerts") Object alerts(@AuthenticationPrincipal Security.Actor a,@RequestParam(defaultValue="") String q,@RequestParam(defaultValue="") String state,@RequestParam(defaultValue="0") int page){return service.alerts(a,q,state,page);}
 @PostMapping("/lots") Object lot(@AuthenticationPrincipal Security.Actor a,@RequestHeader("Idempotency-Key") String key,@Valid @RequestBody LotInput in){return service.receive(a,key,in);}
 @PostMapping("/lots/{id}/actions") Object action(@AuthenticationPrincipal Security.Actor a,@PathVariable long id,@RequestHeader("Idempotency-Key") String key,@Valid @RequestBody Action in){return service.action(a,id,key,in);}
 @PutMapping("/admin/lots/{id}/cost") Object cost(@AuthenticationPrincipal Security.Actor a,@PathVariable long id,@Valid @RequestBody Cost input){return service.cost(a,id,input);}
 @GetMapping("/lots/{id}/history") Object history(@AuthenticationPrincipal Security.Actor a,@PathVariable long id){return service.history(a,id);}
 @GetMapping("/lots/{id}/history/page") Object historyPage(@AuthenticationPrincipal Security.Actor a,@PathVariable long id,@RequestParam(defaultValue="0") int page){return service.historyPage(a,id,page);}
 @GetMapping("/dashboard") Object dashboard(){return service.dashboard();}
}

@Service
class InventoryService {
 @PersistenceContext EntityManager em;
 final JdbcTemplate jdbc;final Clock clock;
 InventoryService(JdbcTemplate jdbc,Clock clock){this.jdbc=jdbc;this.clock=clock;}
 static void admin(Security.Actor a){if(!"ADMIN".equals(a.role()))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Solo administrador.");}
 static void manager(Security.Actor a){if(!List.of("ADMIN","SUPERVISOR").contains(a.role()))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Solo supervisor o programador.");}
 static int offset(int page){if(page<0||page>10000)throw new IllegalArgumentException("Página inválida.");return page*25;}
 static String search(String q){if(q.length()>120)throw new IllegalArgumentException("Búsqueda demasiado larga.");return "%"+q+"%";}
 void audit(Security.Actor actor,String action,String entity,String detail){jdbc.update("INSERT INTO audit(actor_id,action,entity,detail,created_at) VALUES(?,?,?,?,?)",actor==null?null:actor.id(),action,entity,detail,java.sql.Timestamp.from(clock.instant()));}
 Product product(long id){Product p=em.find(Product.class,id);if(p==null)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Producto no encontrado.");return p;}
 Lot lot(long id){Lot l=em.find(Lot.class,id,LockModeType.PESSIMISTIC_WRITE);if(l==null)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Lote no encontrado.");return l;}
 Map<String,Object> productView(Product p){var v=new LinkedHashMap<String,Object>();v.put("id",p.id);v.put("sku",p.sku);v.put("ean",p.ean);v.put("name",p.name);v.put("category",p.category);v.put("unit",p.unit);v.put("active",p.active);v.put("warningDays",p.warningDays);v.put("criticalDays",p.criticalDays);return v;}
 @Transactional(readOnly=true) public Object products(String q,int page){
  String term=search(q);var list=em.createQuery("from Product p where p.name like :q or p.sku like :q or p.ean like :q order by p.name,p.id",Product.class).setParameter("q",term).setFirstResult(offset(page)).setMaxResults(26).getResultList();
  boolean more=list.size()>25;
  return Map.of("items",(more?list.subList(0,25):list).stream().map(this::productView).toList(),"page",page,"hasNext",more);
 }
 @Transactional(readOnly=true) public Object barcode(String ean){
  if(!Rules.ean(ean)||ean.length()!=13)throw new IllegalArgumentException("EAN-13 inválido.");
  var list=em.createQuery("from Product where ean=:ean",Product.class).setParameter("ean",ean).getResultList();
  if(list.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Código desconocido. Solicite el alta del producto al administrador.");
  return productView(list.getFirst());
 }
 @Transactional public Object product(Security.Actor a,Long id,Inventory.ProductInput in){
  manager(a);if(in.criticalDays()>=in.warningDays()||!Rules.ean(in.ean()))throw new IllegalArgumentException("Umbrales o checksum EAN-13 inválidos.");
  Product p=id==null?new Product():em.find(Product.class,id,LockModeType.PESSIMISTIC_WRITE);
  if(p==null)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Producto no encontrado.");
  if("SUPERVISOR".equals(a.role())&&!in.active())throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Solo programador puede dar de baja productos.");
  String before=id==null?"nuevo":productView(p).toString();
  if(id!=null && !p.unit.equals(in.unit()) && em.createQuery("select count(l) from Lot l where l.product.id=:id",Long.class).setParameter("id",id).getSingleResult()>0)throw new IllegalArgumentException("No puede cambiar la unidad de un producto con lotes.");
  p.sku=in.sku().trim();p.ean=in.ean()==null||in.ean().isBlank()?null:in.ean();p.name=in.name().trim();p.category=in.category().trim();p.unit=in.unit();p.warningDays=in.warningDays();p.criticalDays=in.criticalDays();p.active=in.active();
  if(id==null)em.persist(p);em.flush();audit(a,"PRODUCTO","producto:"+p.id,before+" → "+productView(p));
  evaluateProduct(p.id);return productView(p);
 }
 @Transactional public Object removeProduct(Security.Actor a,long id){admin(a);Product p=product(id);p.active=false;audit(a,"BAJA_PRODUCTO","producto:"+id,"Baja lógica; historial conservado");return Map.of("id",id);}
 Long previous(Security.Actor a,String key,Object input){
  try{UUID.fromString(key);}catch(Exception e){throw new IllegalArgumentException("Idempotency-Key debe ser UUID.");}
  em.find(User.class,a.id(),LockModeType.PESSIMISTIC_WRITE);
  var rows=jdbc.queryForList("SELECT fingerprint,result_id FROM idempotency WHERE actor_id=? AND request_key=?",a.id(),key);
  if(rows.isEmpty())return null;
  if(!rows.getFirst().get("fingerprint").equals(AuthService.hash(input.toString())))throw new ResponseStatusException(HttpStatus.CONFLICT,"Clave de operación reutilizada con otros datos.");
  return ((Number)rows.getFirst().get("result_id")).longValue();
 }
 void remember(Security.Actor a,String key,Object input,long id){jdbc.update("INSERT INTO idempotency(actor_id,request_key,fingerprint,result_id) VALUES(?,?,?,?)",a.id(),key,AuthService.hash(input.toString()),id);}
 @Transactional public Object receive(Security.Actor a,String key,Inventory.LotInput in){
  Long prior=previous(a,key,in);if(prior!=null)return Map.of("id",prior,"replayed",true);
  Product p=product(in.productId());if(!p.active)throw new IllegalArgumentException("Producto inactivo.");
  Rules.quantity(in.quantity(),p.unit);
  if(in.received().isAfter(LocalDate.now(clock.withZone(Rules.LIMA)))||in.expiry().isBefore(in.received()))throw new IllegalArgumentException("Fechas de recepción y vencimiento inválidas.");
  if(in.cost()!=null)admin(a);
  Lot l=new Lot();l.product=p;l.code=in.code().trim();l.received=in.received();l.expiry=in.expiry();l.initialQuantity=in.quantity();l.normal=in.quantity();l.promo=BigDecimal.ZERO;l.cost=in.cost();l.createdBy=a.id();l.createdAt=clock.instant();em.persist(l);em.flush();
  movement(a,l,"INGRESO","NORMAL",in.quantity(),in.quantity(),BigDecimal.ZERO,"Recepción de lote",null);
  audit(a,"INGRESO","lote:"+l.id,"Cantidad: "+in.quantity()+" "+p.unit);remember(a,key,in,l.id);evaluateLot(l);return Map.of("id",l.id);
 }
 Movement movement(Security.Actor a,Lot l,String type,String source,BigDecimal quantity,BigDecimal nd,BigDecimal pd,String reason,Long reversal){
  Movement m=new Movement();m.lotId=l.id;m.actorId=a.id();m.type=type;m.source=source;m.quantity=quantity;m.normalDelta=nd;m.promoDelta=pd;m.cost=l.cost;m.reason=reason;m.reversalOf=reversal;m.createdAt=clock.instant();em.persist(m);em.flush();return m;
 }
 @Transactional public Object action(Security.Actor a,long id,String key,Inventory.Action in){
  String fingerprint=id+":"+in;
  if(!in.type().equals("REVERSO")&&in.reversalOf()!=null)throw new IllegalArgumentException("Solo una reversión puede referenciar otro movimiento.");
  Long prior=previous(a,key,fingerprint);if(prior!=null)return Map.of("id",prior,"replayed",true);
  Lot l=lot(id);BigDecimal q=in.quantity(),nd=BigDecimal.ZERO,pd=BigDecimal.ZERO;
  if(in.type().equals("AJUSTE")){manager(a);Rules.quantity(q.abs(),l.product.unit);}else Rules.quantity(q,l.product.unit);
  boolean expired=Rules.days(l.expiry,clock)<0;
  if((in.type().equals("VENTA")||in.type().equals("PROMOCION"))&&(expired||!l.product.active))throw new IllegalArgumentException("No se permite vender/promocionar un lote vencido o producto inactivo.");
  switch(in.type()){
   case "PROMOCION" -> {if(!in.source().equals("NORMAL"))throw new IllegalArgumentException("Promoción se asigna desde saldo normal.");nd=q.negate();pd=q;}
   case "VENTA","MERMA" -> {if(in.source().equals("NORMAL"))nd=q.negate();else pd=q.negate();}
   case "AJUSTE" -> {if(in.source().equals("NORMAL"))nd=q;else pd=q;if(pd.signum()>0&&expired)throw new IllegalArgumentException("No se puede añadir promoción a un lote vencido.");}
   case "REVERSO" -> {
    manager(a);if(in.reversalOf()==null)throw new IllegalArgumentException("Indique movimiento original.");
    Movement original=em.find(Movement.class,in.reversalOf());
    if(original==null||!original.lotId.equals(id)||original.type.equals("REVERSO")||original.type.equals("INGRESO"))throw new IllegalArgumentException("Movimiento no reversible; corrija ingresos mediante ajuste.");
    if(q.compareTo(original.quantity.abs())!=0||!in.source().equals(original.source))throw new IllegalArgumentException("La reversión debe coincidir con la cantidad y saldo original.");
    if(Objects.requireNonNull(jdbc.queryForObject("SELECT COUNT(*) FROM movement WHERE reversal_of=?",Long.class,original.id))>0)throw new IllegalArgumentException("Movimiento ya revertido.");
    nd=original.normalDelta.negate();pd=original.promoDelta.negate();
   }
   default -> throw new IllegalArgumentException("Acción no permitida.");
  }
  var balance=Rules.apply(l.normal,l.promo,nd,pd);l.normal=balance.normal();l.promo=balance.promo();
  Movement m=movement(a,l,in.type(),in.source(),q,nd,pd,in.reason(),in.reversalOf());
  audit(a,in.type(),"lote:"+id,"normal Δ="+nd+", promoción Δ="+pd+"; "+in.reason());
  evaluateLot(l);
  jdbc.update("UPDATE alert SET attended_at=?,attended_by=? WHERE lot_id=? AND state=?",java.sql.Timestamp.from(clock.instant()),a.id(),id,Rules.state(Rules.days(l.expiry,clock),l.product.criticalDays,l.product.warningDays));
  remember(a,key,fingerprint,m.id);em.flush();evaluateLot(l);return Map.of("id",m.id,"normal",l.normal,"promo",l.promo,"total",l.normal.add(l.promo));
 }
 @Transactional public Object cost(Security.Actor a,long id,Inventory.Cost input){
  admin(a);Lot l=lot(id);if(l.cost!=null)throw new IllegalArgumentException("Costo histórico ya fijado. No se sobrescribe.");
  l.cost=input.cost();audit(a,"VALORIZACION","lote:"+id,"Costo PEN "+input.cost()+"; "+input.reason());return Map.of("id",id);
 }
 Map<String,Object> lotView(Lot l,boolean money){
  var v=new LinkedHashMap<String,Object>();long days=Rules.days(l.expiry,clock);
  v.put("id",l.id);v.put("productId",l.product.id);v.put("productActive",l.product.active);v.put("createdBy",l.createdBy);v.put("createdAt",l.createdAt);v.put("product",l.product.name);v.put("sku",l.product.sku);v.put("unit",l.product.unit);v.put("code",l.code);v.put("received",l.received);v.put("expiry",l.expiry);v.put("days",days);
  v.put("state",Rules.state(days,l.product.criticalDays,l.product.warningDays));v.put("normal",l.normal);v.put("promo",l.promo);v.put("total",l.normal.add(l.promo));if(money)v.put("cost",l.cost);return v;
 }
 @Transactional(readOnly=true) public Object lots(Security.Actor a,String q,String state,int page,boolean exhausted){
  return listLots(a,q,state,page,exhausted,false);
 }
 @Transactional(readOnly=true) public Object alerts(Security.Actor a,String q,String state,int page){
  return listLots(a,q,state,page,false,true);
 }
 private Object listLots(Security.Actor a,String q,String state,int page,boolean exhausted,boolean alerts){
  // State remains current even when the scheduler is stopped. Filtering is performed in SQL before pagination.
  var params=new ArrayList<Object>();params.add(LocalDate.now(clock.withZone(Rules.LIMA)));params.add(search(q));params.add(search(q));
  String sql="SELECT l.id FROM lot l JOIN product p ON p.id=l.product_id CROSS JOIN (SELECT ? today) d WHERE (p.name LIKE ? OR l.code LIKE ?) "+(exhausted?"":"AND l.normal+l.promo>0 ");
  if(alerts)sql+=" AND DATEDIFF(l.expiry,d.today)<=p.warning_days ";
  if(!state.isBlank()){
   if(!List.of("VERDE","AMARILLO","ROJO","VENCIDO").contains(state))throw new IllegalArgumentException("Estado inválido.");
   sql+=" AND (CASE WHEN DATEDIFF(l.expiry,d.today)<0 THEN 'VENCIDO' WHEN DATEDIFF(l.expiry,d.today)<=p.critical_days THEN 'ROJO' WHEN DATEDIFF(l.expiry,d.today)<=p.warning_days THEN 'AMARILLO' ELSE 'VERDE' END)=?";params.add(state);
  }
  sql+=" ORDER BY l.expiry,l.id LIMIT 26 OFFSET ?";params.add(offset(page));
  var ids=jdbc.queryForList(sql,Long.class,params.toArray());boolean next=ids.size()>25;if(next)ids=ids.subList(0,25);
  if(ids.isEmpty())return Map.of("items",List.of(),"page",page,"hasNext",false);
  var rows=em.createQuery("select l from Lot l join fetch l.product where l.id in :ids order by l.expiry,l.id",Lot.class).setParameter("ids",ids).getResultList();
  var views=rows.stream().map(l->lotView(l,"ADMIN".equals(a.role()))).toList();
  if(alerts){
   String placeholders=String.join(",",Collections.nCopies(ids.size(),"?"));
   var attention=jdbc.queryForList("SELECT a.lot_id,a.state,a.attended_at,u.name actor FROM alert a LEFT JOIN app_user u ON u.id=a.attended_by WHERE a.lot_id IN ("+placeholders+")",ids.toArray());
   for(var v:views){v.put("attention","PENDIENTE");for(var r:attention)if(Objects.equals(v.get("id"),((Number)r.get("lot_id")).longValue())&&Objects.equals(v.get("state"),r.get("state"))&&r.get("attended_at")!=null){v.put("attention","ATENDIDA_CON_SALDO");v.put("attendedAt",r.get("attended_at"));v.put("attendedBy",r.get("actor"));}}
  }
  return Map.of("items",views,"page",page,"hasNext",next);
 }
 @Transactional(readOnly=true) public Object history(Security.Actor a,long id){
  String cols="m.id,m.type,m.source,m.quantity,m.normal_delta,m.promo_delta,m.reason,m.created_at,m.reversal_of,u.name actor";
  if(a.role().equals("ADMIN"))cols+=",m.cost";
  return jdbc.queryForList("SELECT "+cols+" FROM movement m JOIN app_user u ON u.id=m.actor_id WHERE m.lot_id=? ORDER BY m.id DESC LIMIT 100",id);
 }
 @Transactional(readOnly=true) public Object historyPage(Security.Actor a,long id,int page){
  if(em.find(Lot.class,id)==null)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Lote no encontrado.");
  String cols="m.id,m.type,m.source,m.quantity,m.normal_delta,m.promo_delta,m.reason,m.created_at,m.reversal_of,m.actor_id,u.name actor";
  if(a.role().equals("ADMIN"))cols+=",m.cost";
  var rows=jdbc.queryForList("SELECT "+cols+" FROM movement m JOIN app_user u ON u.id=m.actor_id WHERE m.lot_id=? ORDER BY m.id DESC LIMIT 26 OFFSET ?",id,offset(page));
  boolean more=rows.size()>25;return Map.of("items",more?rows.subList(0,25):rows,"page",page,"hasNext",more);
 }
 public Object dashboard(){
  var rows=jdbc.queryForList("SELECT CASE WHEN DATEDIFF(l.expiry,?)<0 THEN 'VENCIDO' WHEN DATEDIFF(l.expiry,?)<=p.critical_days THEN 'ROJO' WHEN DATEDIFF(l.expiry,?)<=p.warning_days THEN 'AMARILLO' ELSE 'VERDE' END state,COUNT(*) lots FROM lot l JOIN product p ON p.id=l.product_id WHERE normal+promo>0 GROUP BY state",today(),today(),today());
  return Map.of("states",rows,"lastEvaluation",jdbc.queryForList("SELECT * FROM evaluation ORDER BY id DESC LIMIT 1"));
 }
 LocalDate today(){return LocalDate.now(clock.withZone(Rules.LIMA));}
 void evaluateProduct(long productId){for(Lot l:em.createQuery("select l from Lot l join fetch l.product where l.product.id=:p and l.normal+l.promo>0",Lot.class).setParameter("p",productId).getResultList())evaluateLot(l);}
 void evaluateLot(Lot l){
  if(l.normal.add(l.promo).signum()==0)return;
  String state=Rules.state(Rules.days(l.expiry,clock),l.product.criticalDays,l.product.warningDays);if(state.equals("VERDE"))return;
  jdbc.update("INSERT IGNORE INTO alert(lot_id,state,created_at) VALUES(?,?,?)",l.id,state,java.sql.Timestamp.from(clock.instant()));
  if(state.equals("ROJO")||state.equals("VENCIDO"))jdbc.update("INSERT IGNORE INTO outbox(id,alert_id,next_attempt) SELECT ?,id,? FROM alert WHERE lot_id=? AND state=?",UUID.randomUUID().toString(),java.sql.Timestamp.from(clock.instant()),l.id,state);
 }
}

