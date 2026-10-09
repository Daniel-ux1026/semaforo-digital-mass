package pe.mass.semaforo;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.*;
import org.springframework.web.cors.*;
import org.springframework.web.filter.OncePerRequestFilter;

@Configuration @EnableMethodSecurity
class Security {
 record Actor(long id, String name, String role, String session, boolean mustChange) {}
 @Bean PasswordEncoder encoder() { return new BCryptPasswordEncoder(12); }
 @Bean JwtEncoder jwtEncoder(@Value("${app.jwt-secret}") String secret) {
  if(secret.getBytes(StandardCharsets.UTF_8).length<32) throw new IllegalArgumentException("JWT_SECRET requiere al menos 32 bytes aleatorios.");
  return new NimbusJwtEncoder(new ImmutableSecret<>(secret.getBytes(StandardCharsets.UTF_8)));
 }
 @Bean JwtDecoder jwtDecoder(@Value("${app.jwt-secret}") String secret) {
  var decoder=NimbusJwtDecoder.withSecretKey(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8),"HmacSHA256")).macAlgorithm(MacAlgorithm.HS256).build();
  OAuth2TokenValidator<Jwt> audience = jwt -> jwt.getAudience().equals(List.of("semaforo-ui")) ? OAuth2TokenValidatorResult.success() : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token"));
  decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefaultWithIssuer("semaforo-api"),audience));
  return decoder;
 }
 @Bean SecurityFilterChain chain(HttpSecurity http, JwtDecoder decoder, JdbcTemplate jdbc,
   @Value("${app.origin}") String origin, @Value("${app.secure-cookie}") boolean secure,
   @Value("${app.service-token}") String serviceToken) throws Exception {
  var csrf=CookieCsrfTokenRepository.withHttpOnlyFalse();
  csrf.setCookieCustomizer(c -> c.path("/").sameSite("Strict").secure(secure));
  var handler=new CsrfTokenRequestAttributeHandler();
  var cors=new CorsConfiguration(); cors.setAllowedOrigins(List.of(origin)); cors.setAllowedMethods(List.of("GET","POST","PUT","DELETE","OPTIONS"));
  cors.setAllowedHeaders(List.of("Authorization","Content-Type","X-XSRF-TOKEN","Idempotency-Key")); cors.setAllowCredentials(true);
  var source=new UrlBasedCorsConfigurationSource(); source.registerCorsConfiguration("/**",cors);
  http.cors(c -> c.configurationSource(source)).sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
   .csrf(c -> c.csrfTokenRepository(csrf).csrfTokenRequestHandler(handler).ignoringRequestMatchers("/api/service/**"))
   .headers(h -> h.contentSecurityPolicy(c -> c.policyDirectives("default-src 'none'; frame-ancestors 'none'")))
   .authorizeHttpRequests(a -> a
    .requestMatchers("/api/auth/csrf","/api/auth/login","/api/auth/refresh","/api/auth/forgot-password","/api/auth/reset-password","/actuator/health").permitAll()
    .requestMatchers("/api/chat/context").hasRole("CHAT")
    .requestMatchers("/api/auth/password").hasRole("ADMIN")
    .requestMatchers(org.springframework.http.HttpMethod.POST,"/api/chat/session").hasAnyRole("ADMIN","SUPERVISOR","WORKER")
    .requestMatchers("/api/service/**").hasRole("SERVICE")
    .requestMatchers(org.springframework.http.HttpMethod.POST,"/api/admin/products").hasAnyRole("ADMIN","SUPERVISOR")
    .requestMatchers(org.springframework.http.HttpMethod.PUT,"/api/admin/products/*").hasAnyRole("ADMIN","SUPERVISOR")
    .requestMatchers(org.springframework.http.HttpMethod.GET,"/api/admin/reports","/api/admin/reports/summary","/api/admin/reports.csv").hasAnyRole("ADMIN","SUPERVISOR")
    .requestMatchers("/api/admin/**","/actuator/metrics/**").hasRole("ADMIN")
    .requestMatchers("/api/**").hasAnyRole("ADMIN","SUPERVISOR","WORKER").anyRequest().denyAll())
   .exceptionHandling(e -> e.authenticationEntryPoint((r,s,x)->json(s,401,"Sesión expirada o credenciales inválidas."))
    .accessDeniedHandler((r,s,x)->json(s,403,"No tiene permisos o falta protección CSRF.")))
   .addFilterBefore(new AccessFilter(decoder,jdbc,serviceToken),UsernamePasswordAuthenticationFilter.class);
  return http.build();
 }
 static void json(HttpServletResponse res,int status,String message) throws IOException {
  res.setStatus(status);res.setContentType("application/json;charset=UTF-8");res.getWriter().write("{\"message\":\""+message+"\"}");
 }
 static class AccessFilter extends OncePerRequestFilter {
  final JwtDecoder decoder; final JdbcTemplate jdbc; final String serviceToken;
  record Window(long minute, int count) {}
  final Map<String,Window> limits=new ConcurrentHashMap<>();
  AccessFilter(JwtDecoder decoder,JdbcTemplate jdbc,String serviceToken){this.decoder=decoder;this.jdbc=jdbc;this.serviceToken=serviceToken;}
  boolean limit(String key,int max) {
   long now=System.currentTimeMillis()/60000;
   if(limits.size()>10000) limits.entrySet().removeIf(e->e.getValue().minute<now);
   return limits.compute(key,(k,w)->new Window(now,w==null||w.minute!=now?1:w.count+1)).count>max;
  }
  @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain) throws ServletException,IOException {
   res.setHeader("Cache-Control","no-store");
   if(req.getContentLengthLong()>32768){json(res,413,"Solicitud demasiado grande.");return;}
   if(List.of("POST","PUT","PATCH").contains(req.getMethod())) {
    byte[] body=req.getInputStream().readNBytes(32769);
    if(body.length>32768){json(res,413,"Solicitud demasiado grande.");return;}
    req=new HttpServletRequestWrapper(req){
     @Override public ServletInputStream getInputStream(){
      var input=new java.io.ByteArrayInputStream(body);
      return new ServletInputStream(){public int read(){return input.read();}public boolean isFinished(){return input.available()==0;}public boolean isReady(){return true;}public void setReadListener(ReadListener listener){throw new UnsupportedOperationException();}};
     }
     @Override public java.io.BufferedReader getReader(){return new java.io.BufferedReader(new java.io.InputStreamReader(getInputStream(),StandardCharsets.UTF_8));}
    };
   }
   boolean login=List.of("/api/auth/login","/api/auth/forgot-password","/api/auth/reset-password").contains(req.getRequestURI());
   if(limit((login?"login:":"api:")+req.getRemoteAddr(),login?12:240)){res.setHeader("Retry-After","60");json(res,429,"Demasiados intentos; espere un minuto.");return;}
   String authorization=req.getHeader("Authorization");
   if(req.getRequestURI().equals("/api/chat/context")) {
    if(authorization!=null&&authorization.startsWith("Bearer ")&&authorization.length()<200){
     var actors=jdbc.query("SELECT u.id,u.name,u.role,u.must_change,s.id sid FROM chat_session c JOIN auth_session s ON s.id=c.session_id JOIN app_user u ON u.id=s.user_id WHERE c.hash=? AND c.expires_at>UTC_TIMESTAMP(6) AND s.expires_at>UTC_TIMESTAMP(6) AND s.revoked=false AND u.active=true AND u.must_change=false AND u.role IN ('ADMIN','SUPERVISOR','WORKER')",
      (rs,i)->new Actor(rs.getLong("id"),rs.getString("name"),rs.getString("role"),rs.getString("sid"),rs.getBoolean("must_change")),AuthService.hash(authorization.substring(7)));
     if(actors.size()==1){var actor=actors.getFirst();if(limit("chat:"+actor.id(),20)){json(res,429,"Límite del asistente alcanzado; espere un minuto.");return;}
      SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(actor,null,List.of(new SimpleGrantedAuthority("ROLE_CHAT"))));}
    }
   } else if(req.getRequestURI().startsWith("/api/service/")) {
    if(serviceToken.length()>=32 && authorization!=null && java.security.MessageDigest.isEqual(authorization.getBytes(StandardCharsets.UTF_8),("Bearer "+serviceToken).getBytes(StandardCharsets.UTF_8)))
     SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("n8n",null,List.of(new SimpleGrantedAuthority("ROLE_SERVICE"))));
   } else if(authorization!=null && authorization.startsWith("Bearer ")) {
    try {
     Jwt jwt=decoder.decode(authorization.substring(7));
     var actors=jdbc.query("SELECT u.id,u.name,u.role,u.must_change,s.id sid FROM auth_session s JOIN app_user u ON u.id=s.user_id WHERE s.id=? AND s.user_id=? AND s.revoked=false AND s.expires_at>UTC_TIMESTAMP(6) AND u.active=true",
       (rs,i)->new Actor(rs.getLong("id"),rs.getString("name"),rs.getString("role"),rs.getString("sid"),rs.getBoolean("must_change")),jwt.getClaimAsString("sid"),Long.valueOf(jwt.getSubject()));
     if(actors.size()!=1) {json(res,401,"Sesión revocada.");return;}
     Actor actor=actors.getFirst();
     if(actor.mustChange && !List.of("/api/auth/me","/api/auth/password","/api/auth/logout","/api/auth/csrf").contains(req.getRequestURI())) {json(res,403,"Debe cambiar su contraseña.");return;}
     if(limit("user:"+actor.id,180)){json(res,429,"Límite de solicitudes alcanzado.");return;}
     SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(actor,null,List.of(new SimpleGrantedAuthority("ROLE_"+actor.role))));
    } catch(JwtException | IllegalArgumentException e) {json(res,401,"Sesión expirada.");return;}
   }
   chain.doFilter(req,res);
  }
 }
}
