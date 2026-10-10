-- DATOS DE DEMOSTRACIÓN DE SEMÁFORO DIGITAL MASS
-- MySQL 8.4. Inicie antes la aplicación para aplicar las migraciones V1 a V6
-- y crear su programador con BOOTSTRAP_DNI y BOOTSTRAP_PASSWORD locales.
-- No contiene cuentas, contraseñas, tokens ni datos personales.
-- Crea tres productos y tres lotes: verde, amarillo y rojo al importarlos.
-- Puede ejecutarse varias veces: no duplica ni reinicia los lotes existentes.
-- Ejecute este archivo en su instalación local de prueba, con la API detenida.
-- Se necesita permiso temporal para crear y eliminar el procedimiento de carga.

USE `semaforo`;
SET NAMES utf8mb4;

DELIMITER $$
CREATE PROCEDURE cargar_demo_mass_v1()
BEGIN
 DECLARE fin BOOLEAN DEFAULT FALSE;
 DECLARE responsable BIGINT;
 DECLARE candado INT;
 DECLARE producto BIGINT;
 DECLARE lote BIGINT;
 DECLARE creados INT DEFAULT 0;
 DECLARE sku_demo VARCHAR(40);
 DECLARE nombre_demo VARCHAR(120);
 DECLARE dias INT;
 DECLARE cantidad DECIMAL(14,3);
 DECLARE costo DECIMAL(14,4);
 DECLARE ejemplos CURSOR FOR
  SELECT 'DEMO-MASS-ARROZ', 'Arroz de demostración', 70, 50.000, 3.5000
  UNION ALL SELECT 'DEMO-MASS-LECHE', 'Leche de demostración', 20, 40.000, 4.2000
  UNION ALL SELECT 'DEMO-MASS-YOGUR', 'Yogur de demostración', 5, 30.000, 2.8000;
 DECLARE CONTINUE HANDLER FOR NOT FOUND SET fin=TRUE;
 DECLARE EXIT HANDLER FOR SQLEXCEPTION
 BEGIN
  ROLLBACK;
  RESIGNAL;
 END;

 START TRANSACTION;
 SELECT MIN(id) INTO responsable FROM usuarios WHERE role='ADMIN' AND active=TRUE;
 IF responsable IS NULL THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Inicie la aplicación para crear el programador antes de cargar los datos de ejemplo.';
 END IF;
 SELECT MAX(id) INTO candado FROM bloqueo_tienda WHERE id=1;
 IF candado IS NULL THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Falta el registro de control. Aplique las migraciones desde INICIAR_PROYECTO.bat.';
 END IF;
 SELECT id INTO candado FROM bloqueo_tienda WHERE id=1 FOR UPDATE;

 OPEN ejemplos;
 cargar: LOOP
  FETCH ejemplos INTO sku_demo,nombre_demo,dias,cantidad,costo;
  IF fin THEN LEAVE cargar; END IF;

  IF EXISTS(SELECT 1 FROM productos WHERE sku=sku_demo AND
   (name<>nombre_demo OR category<>'Demostración' OR unit<>'UNIDAD' OR
    active<>TRUE OR warning_days<>30 OR critical_days<>15)) THEN
   SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Un SKU de ejemplo ya pertenece a un producto diferente. No se modificaron los datos.';
  END IF;
  INSERT INTO productos(sku,ean,name,category,unit,active,warning_days,critical_days)
   SELECT sku_demo,NULL,nombre_demo,'Demostración','UNIDAD',TRUE,30,15
   WHERE NOT EXISTS(SELECT 1 FROM productos WHERE sku=sku_demo);
  SELECT id INTO producto FROM productos WHERE sku=sku_demo;

  IF NOT EXISTS(SELECT 1 FROM lotes WHERE product_id=producto AND code='LOTE-DEMO-MASS-01') THEN
   INSERT INTO lotes(product_id,code,received,expiry,initial_quantity,normal,promo,cost,created_by,created_at)
    VALUES(producto,'LOTE-DEMO-MASS-01',CURRENT_DATE,DATE_ADD(CURRENT_DATE,INTERVAL dias DAY),cantidad,cantidad,0,costo,responsable,UTC_TIMESTAMP(6));
   SET lote=LAST_INSERT_ID();
   INSERT INTO movimientos(lot_id,actor_id,type,source,quantity,normal_delta,promo_delta,cost,reason,created_at)
    VALUES(lote,responsable,'INGRESO','NORMAL',cantidad,cantidad,0,costo,'Carga de datos ficticios de demostración',UTC_TIMESTAMP(6));
   SET creados=creados+1;
  END IF;
 END LOOP;
 CLOSE ejemplos;

 IF creados>0 THEN
  INSERT INTO auditoria(actor_id,action,entity,detail,created_at)
   VALUES(responsable,'CARGA_DEMO','inventario',CONCAT('Lotes ficticios creados: ',creados),UTC_TIMESTAMP(6));
 END IF;
 COMMIT;
 SELECT creados AS lotes_creados, 'Carga de demostración completada' AS resultado;
END$$
DELIMITER ;

CALL cargar_demo_mass_v1();
DROP PROCEDURE cargar_demo_mass_v1;

-- Si una carga rechazada dejó el procedimiento, ejecute antes de reintentarlo:
-- DROP PROCEDURE IF EXISTS cargar_demo_mass_v1;
-- La transacción revierte los cambios si se encuentra un error.
-- Las alertas y notificaciones las genera la aplicación al evaluar los lotes.
-- Las sesiones, tokens y recuperaciones se crean solamente al usar sus funciones.
