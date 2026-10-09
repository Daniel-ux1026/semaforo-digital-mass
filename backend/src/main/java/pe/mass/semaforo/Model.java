package pe.mass.semaforo;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;

// Persistence types are package-private and never serialized as API responses.
@Entity @Table(name="app_user")
class User {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id;
 String dni, name, password, role, email;
 boolean active = true, mustChange = true;
 @Version long version;
}
@Entity @Table(name="product")
class Product {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id;
 String sku, ean, name, category, unit;
 boolean active = true;
 int warningDays = 30, criticalDays = 15;
 @Version long version;
}
@Entity @Table(name="lot")
class Lot {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="product_id") Product product;
 String code;
 LocalDate received, expiry;
 @Column(precision=14,scale=3) BigDecimal initialQuantity, normal, promo;
 @Column(precision=14,scale=4) BigDecimal cost;
 Long createdBy;
 Instant createdAt;
 @Version long version;
}
@Entity @Table(name="movement")
class Movement {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id;
 Long lotId, actorId, reversalOf;
 String type, source, reason;
 @Column(precision=14,scale=3) BigDecimal quantity, normalDelta, promoDelta;
 @Column(precision=14,scale=4) BigDecimal cost;
 Instant createdAt;
}
