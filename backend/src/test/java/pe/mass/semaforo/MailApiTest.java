package pe.mass.semaforo;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.net.http.*;
import org.mockito.ArgumentCaptor;

class MailApiTest {
 @SuppressWarnings("unchecked")
 @Test void sendsJsonOverHttpsAndRequiresProviderConfirmation() throws Exception {
  var mailer=new ResetMailer();mailer.provider="resend";mailer.resendKey="test-private-key";
  mailer.mailClient=mock(HttpClient.class);var response=(HttpResponse<String>)mock(HttpResponse.class);
  when(response.statusCode()).thenReturn(200);when(response.body()).thenReturn("{\"id\":\"test-message-id\"}");
  doReturn(response).when(mailer.mailClient).send(any(HttpRequest.class),any(HttpResponse.BodyHandler.class));
  assertTrue(mailer.configured());mailer.sendMessage("owner@example.invalid","Aviso de vencimiento","Lote DEMO vence mañana");
  var request=ArgumentCaptor.forClass(HttpRequest.class);
  verify(mailer.mailClient).send(request.capture(),any(HttpResponse.BodyHandler.class));
  assertEquals("https://api.resend.com/emails",request.getValue().uri().toString());
  assertEquals("Bearer test-private-key",request.getValue().headers().firstValue("Authorization").orElseThrow());
  when(response.body()).thenReturn("{}");
  assertThrows(IllegalStateException.class,()->mailer.sendMessage("owner@example.invalid","Aviso","Lote DEMO"));
 }
 @SuppressWarnings("unchecked")
 @Test void rejectsInvalidCredentialsWithoutLoggingProviderBody() throws Exception {
  var mailer=new ResetMailer();mailer.provider="resend";mailer.resendKey="test-private-key";mailer.mailClient=mock(HttpClient.class);
  var response=(HttpResponse<String>)mock(HttpResponse.class);when(response.statusCode()).thenReturn(401);
  doReturn(response).when(mailer.mailClient).send(any(HttpRequest.class),any(HttpResponse.BodyHandler.class));
  var error=assertThrows(ResetMailer.MailApiRejected.class,()->mailer.sendMessage("owner@example.invalid","Aviso","Lote DEMO"));
  assertEquals("AUTH_RECHAZADA",EmailAlerts.failure(error));
  assertFalse(error.getMessage().contains(mailer.resendKey));
  assertEquals("RECHAZO_PROVEEDOR",EmailAlerts.failure(new ResetMailer.MailApiRejected(429)));
 }
 @Test void doesNotTreatAnUnknownOutcomeAsASafeRetry(){
  assertEquals("ENVIO_INCIERTO",EmailAlerts.failure(new HttpTimeoutException("test")));
  var mailer=new ResetMailer();mailer.provider="resend";assertFalse(mailer.configured());
 }
}
