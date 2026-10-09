package pe.mass.semaforo;

import java.math.BigDecimal;
import java.time.*;
import java.time.temporal.ChronoUnit;

public final class Rules {
 public static final ZoneId LIMA = ZoneId.of("America/Lima");
 private Rules() {}
 public static long days(LocalDate expiry, Clock clock) { return ChronoUnit.DAYS.between(LocalDate.now(clock.withZone(LIMA)), expiry); }
 public static String state(long days, int critical, int warning) {
  if (days < 0) return "VENCIDO";
  if (days <= critical) return "ROJO";
  if (days <= warning) return "AMARILLO";
  return "VERDE";
 }
 public static boolean ean(String value) {
  if (value == null || value.isBlank()) return true;
  if (!value.matches("[0-9]{13}")) return false;
  int sum = 0;
  for (int i=0;i<12;i++) sum += (value.charAt(i)-'0') * (i%2 == 0 ? 1 : 3);
  return (10-sum%10)%10 == value.charAt(12)-'0';
 }
 public static void quantity(BigDecimal q, String unit) {
  if(q == null || q.signum() <= 0 || q.scale()>3 || q.compareTo(new BigDecimal("99999999999.999"))>0)
   throw new IllegalArgumentException("Cantidad inválida; máximo tres decimales.");
  if ("UNIDAD".equals(unit) && q.stripTrailingZeros().scale()>0)
   throw new IllegalArgumentException("Las unidades deben ser enteras.");
 }
 public record Balance(BigDecimal normal, BigDecimal promo) {}
 public static Balance apply(BigDecimal normal, BigDecimal promo, BigDecimal nd, BigDecimal pd) {
  BigDecimal n=normal.add(nd), p=promo.add(pd);
  if(n.signum()<0 || p.signum()<0) throw new IllegalArgumentException("Saldo insuficiente para la operación o su reversión.");
  return new Balance(n,p);
 }
}
