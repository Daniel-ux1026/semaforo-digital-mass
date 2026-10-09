package pe.mass.semaforo;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;
import java.time.*;
import java.math.BigDecimal;
class RulesTest {
 @ParameterizedTest @CsvSource({"31,VERDE","30,AMARILLO","16,AMARILLO","15,ROJO","1,ROJO","0,ROJO","-1,VENCIDO"})
 void boundaries(long days,String expected){assertEquals(expected,Rules.state(days,15,30));}
 @Test void limaMidnight(){LocalDate expiry=LocalDate.of(2026,9,28);assertEquals(1,Rules.days(expiry,Clock.fixed(Instant.parse("2026-09-28T04:59:59Z"),ZoneOffset.UTC)));assertEquals(0,Rules.days(expiry,Clock.fixed(Instant.parse("2026-09-28T05:00:00Z"),ZoneOffset.UTC)));}
 @Test void customThreshold(){assertEquals("ROJO",Rules.state(3,3,8));assertEquals("AMARILLO",Rules.state(8,3,8));assertEquals("VERDE",Rules.state(9,3,8));}
 @Test void eanAndLeadingZeros(){assertTrue(Rules.ean("0000000000000"));assertTrue(Rules.ean("7751271000017"));assertFalse(Rules.ean("7751271000016"));assertFalse(Rules.ean("123"));}
 @Test void partialBalances(){var b=Rules.apply(new BigDecimal("20"),BigDecimal.ZERO,new BigDecimal("-10"),new BigDecimal("10"));assertEquals(new BigDecimal("20"),b.normal().add(b.promo()));b=Rules.apply(b.normal(),b.promo(),BigDecimal.ZERO,new BigDecimal("-5"));b=Rules.apply(b.normal(),b.promo(),new BigDecimal("-5"),BigDecimal.ZERO);assertEquals(new BigDecimal("5"),b.normal());assertEquals(new BigDecimal("5"),b.promo());}
 @Test void negativeAndIndivisible(){assertThrows(IllegalArgumentException.class,()->Rules.apply(BigDecimal.ZERO,BigDecimal.ONE,BigDecimal.ONE.negate(),BigDecimal.ZERO));assertThrows(IllegalArgumentException.class,()->Rules.quantity(new BigDecimal("0.5"),"UNIDAD"));assertDoesNotThrow(()->Rules.quantity(new BigDecimal("0.125"),"KG"));}
 @Test void csvFormula(){assertEquals("\"'=HYPERLINK(1)\"",Admin.csvCell("=HYPERLINK(1)"));assertEquals("\"'  +2\"",Admin.csvCell("  +2"));assertEquals("\"a\"\"b\"",Admin.csvCell("a\"b"));}
}

