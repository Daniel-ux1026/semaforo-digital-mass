package pe.mass.semaforo;

import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import jakarta.servlet.http.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController @RequestMapping("/api/auth")
class Auth {
 final AuthService service;
 final boolean secure;
 Auth(AuthService service,@Value("${app.secure-cookie}") boolean secure){this.service=service;this.secure=secure;}
 record Login(@Pattern(regexp="[0-9]{8}") @NotNull String dni,@NotNull @Size(min=1,max=72) String password){}
 record Password(@NotBlank String current,@Size(min=12,max=72) @NotNull String next){}
 @GetMapping("/csrf") Map<String,String> csrf(CsrfToken token){return Map.of("token",token.getToken());}
 @PostMapping("/login") Object login(@Valid @RequestBody Login input,HttpServletResponse response){return output(service.login(input),response);}
 @PostMapping("/refresh") Object refresh(@CookieValue(name="refresh",defaultValue="") String token,HttpServletResponse response){return output(service.refresh(token),response);}
 @GetMapping("/me") Object me(@AuthenticationPrincipal Security.Actor actor){return Map.of("id",actor.id(),"name",actor.name(),"role",actor.role(),"mustChange",actor.mustChange());}
 @PostMapping("/password") Object password(@AuthenticationPrincipal Security.Actor actor,@Valid @RequestBody Password input,HttpServletResponse response){service.password(actor,input);clear(response);return Map.of("message","Contraseña actualizada. Inicie sesión nuevamente.");}
 @PostMapping("/logout") Object logout(@AuthenticationPrincipal Security.Actor actor,HttpServletResponse response){service.revoke(actor.session());clear(response);return Map.of("message","Sesión cerrada.");}
 Object output(AuthService.Tokens tokens,HttpServletResponse res){
  if(tokens==null){clear(res);throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Credenciales inválidas o sesión vencida.");}
  res.addHeader("Set-Cookie",ResponseCookie.from("refresh",tokens.refresh()).httpOnly(true).secure(secure).sameSite("Strict").path("/api/auth").maxAge(Duration.ofHours(8)).build().toString());
  return Map.of("accessToken",tokens.access(),"expiresIn",300,"user",tokens.user());
 }
 void clear(HttpServletResponse res){res.addHeader("Set-Cookie",ResponseCookie.from("refresh","").httpOnly(true).secure(secure).sameSite("Strict").path("/api/auth").maxAge(0).build().toString());}
}

@Service
class AuthService {
 private static final SecureRandom RANDOM=new SecureRandom();
 private record Attempt(long until,int count){}
 private final Map<String,Attempt> attempts=new java.util.concurrent.ConcurrentHashMap<>();
 @PersistenceContext EntityManager em;
 final JdbcTemplate jdbc; final PasswordEncoder encoder; final JwtEncoder jwt; final Clock clock;
 final String bootstrapDni,bootstrapPassword; final String dummyHash;
 record Tokens(String access,String refresh,Map<String,Object> user){}
 AuthService(JdbcTemplate jdbc,PasswordEncoder encoder,JwtEncoder jwt,Clock clock,@Value("${app.bootstrap-dni}") String dni,@Value("${app.bootstrap-password}") String password){
  this.jdbc=jdbc;this.encoder=encoder;this.jwt=jwt;this.clock=clock;bootstrapDni=dni;bootstrapPassword=password;dummyHash=encoder.encode(UUID.randomUUID().toString());
 }
 static String hash(String value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
 static String random(){byte[] bytes=new byte[32];RANDOM.nextBytes(bytes);return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);}
 @EventListener(ApplicationReadyEvent.class) @Transactional
 public void bootstrap(){
  jdbc.queryForList("SELECT id FROM store_lock WHERE id=1 FOR UPDATE");
  if(Objects.requireNonNull(jdbc.queryForObject("SELECT COUNT(*) FROM app_user",Long.class))==0 && !bootstrapDni.isBlank()){
   if(!bootstrapDni.matches("[0-9]{8}") || bootstrapPassword.length()<12 || bootstrapPassword.length()>72) throw new IllegalArgumentException("Credenciales iniciales inválidas.");
   User u=new User();u.dni=bootstrapDni;u.name="Administrador de tienda";u.role="ADMIN";u.password=encoder.encode(bootstrapPassword);em.persist(u);
  }
 }
 @Transactional public Tokens login(Auth.Login in){
  long now=clock.millis();String account=hash(in.dni());
  if(attempts.size()>10000)attempts.entrySet().removeIf(e->e.getValue().until()<now);
  Attempt attempt=attempts.compute(account,(k,v)->v==null||v.until()<now?new Attempt(now+60000,1):new Attempt(v.until(),v.count()+1));
  if(attempt.count()>8)throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Demasiados intentos; espere un minuto.");
  var users=em.createQuery("from User where dni=:dni",User.class).setParameter("dni",in.dni()).getResultList();
  User u=users.isEmpty()?null:users.getFirst();
  boolean match=encoder.matches(in.password(),u==null?dummyHash:u.password);
  if(!match||u==null||!u.active)return null;
  attempts.remove(account);
  String sid=UUID.randomUUID().toString();
  jdbc.update("INSERT INTO auth_session(id,user_id,expires_at) VALUES(?,?,?)",sid,u.id,java.sql.Timestamp.from(clock.instant().plus(Duration.ofHours(8))));
  return issue(u,sid);
 }
 @Transactional public Tokens refresh(String raw){
  if(raw.length()>200||raw.isEmpty())return null;
  var rows=jdbc.queryForList("SELECT r.used,r.session_id,s.user_id,s.revoked,s.expires_at FROM refresh_token r JOIN auth_session s ON s.id=r.session_id WHERE r.hash=? FOR UPDATE",hash(raw));
  if(rows.isEmpty())return null;
  var row=rows.getFirst();String sid=(String)row.get("session_id");
  if(Boolean.TRUE.equals(row.get("used")) || Boolean.TRUE.equals(row.get("revoked")) || ((java.sql.Timestamp)row.get("expires_at")).toInstant().isBefore(clock.instant())){revoke(sid);return null;}
  User u=em.find(User.class,((Number)row.get("user_id")).longValue());
  if(!u.active){revoke(sid);return null;}
  jdbc.update("UPDATE refresh_token SET used=true WHERE hash=?",hash(raw));
  return issue(u,sid);
 }
 Tokens issue(User u,String sid){
  String raw=random();jdbc.update("INSERT INTO refresh_token(hash,session_id) VALUES(?,?)",hash(raw),sid);
  var claims=JwtClaimsSet.builder().issuer("semaforo-api").audience(List.of("semaforo-ui")).subject(u.id.toString()).issuedAt(clock.instant()).expiresAt(clock.instant().plusSeconds(300)).claim("sid",sid).build();
  String token=jwt.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(),claims)).getTokenValue();
  return new Tokens(token,raw,Map.of("id",u.id,"name",u.name,"role",u.role,"mustChange",u.mustChange));
 }
 @Transactional public void password(Security.Actor actor,Auth.Password in){
  InventoryService.admin(actor);
  User u=em.find(User.class,actor.id(),LockModeType.PESSIMISTIC_WRITE);
  if(!encoder.matches(in.current(),u.password)||in.current().equals(in.next()))throw new IllegalArgumentException("Contraseña actual incorrecta o nueva contraseña repetida.");
  u.password=encoder.encode(in.next());u.mustChange=false;
  jdbc.update("UPDATE auth_session SET revoked=true WHERE user_id=?",u.id);
  jdbc.update("INSERT INTO audit(actor_id,action,entity,detail,created_at) VALUES(?,?,?,?,?)",u.id,"CAMBIO_PASSWORD","usuario:"+u.id,"Sesiones revocadas",java.sql.Timestamp.from(clock.instant()));
 }
 @Transactional public void revoke(String sid){jdbc.update("UPDATE auth_session SET revoked=true WHERE id=?",sid);}
}

