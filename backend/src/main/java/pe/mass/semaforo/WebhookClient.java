package pe.mass.semaforo;
import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

final class WebhookClient {
 private WebhookClient(){}
 static String post(String url,String token,String body) throws IOException {
  HttpURLConnection c=(HttpURLConnection)URI.create(url).toURL().openConnection();
  try{
   c.setConnectTimeout(3000);c.setReadTimeout(8000);c.setInstanceFollowRedirects(false);c.setRequestMethod("POST");c.setDoOutput(true);
   c.setRequestProperty("Authorization","Bearer "+token);c.setRequestProperty("Content-Type","application/json");
   try(var out=c.getOutputStream()){out.write(body.getBytes(StandardCharsets.UTF_8));}
   if(c.getResponseCode()<200||c.getResponseCode()>=300)throw new IOException("Webhook HTTP "+c.getResponseCode());
   try(var in=c.getInputStream()){byte[] bytes=in.readNBytes(65537);if(bytes.length>65536)throw new IOException("Respuesta demasiado grande");return new String(bytes,StandardCharsets.UTF_8);}
  }finally{c.disconnect();}
 }
}
