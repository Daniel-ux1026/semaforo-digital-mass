-- ESTRUCTURA ACTUAL DE SEMÁFORO DIGITAL MASS
-- Generada: 2026-10-10T03:54:56.814Z
-- 19 tablas de negocio, 1 tabla técnica y 15 vistas de compatibilidad.
-- Incluye claves, índices, restricciones y triggers. No incluye registros ni cuentas.
-- Para visualizarla: cree una base vacía en Workbench, selecciónela con USE y ejecute este archivo.
-- Para instalar la aplicación: ejecute INICIAR_PROYECTO.bat. Flyway aplica V1 a V6.
-- No importe esta copia en la base operativa ni la use para actualizar una instalación existente.
-- La tabla técnica se exporta vacía. Flyway gestiona su historial al iniciar el proyecto.
-- Tablas: alertas, auditoria, bloqueo_tienda, empleado, evaluaciones, flyway_schema_history, lotes, movimientos, notificaciones, operaciones_idempotentes, productos, programador, recepciones_servicio, recuperaciones_clave, renovaciones_sesion, sesiones, sesiones_chat, solicitudes_clave, supervisor, usuarios
-- Los CREATE TABLE temporales con nombres ingleses preparan las vistas de compatibilidad.


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `alert` AS SELECT
 1 AS `id`,
 1 AS `lot_id`,
 1 AS `state`,
 1 AS `attended_at`,
 1 AS `created_at`,
 1 AS `attended_by`*/;
SET character_set_client = @saved_cs_client;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `alertas` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `lot_id` bigint NOT NULL,
  `state` varchar(12) NOT NULL,
  `attended_at` timestamp(6) NULL DEFAULT NULL,
  `created_at` timestamp(6) NOT NULL,
  `attended_by` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `lot_id` (`lot_id`,`state`),
  KEY `fk_alert_actor` (`attended_by`),
  CONSTRAINT `alertas_ibfk_1` FOREIGN KEY (`lot_id`) REFERENCES `lotes` (`id`),
  CONSTRAINT `fk_alert_actor` FOREIGN KEY (`attended_by`) REFERENCES `usuarios` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `app_user` AS SELECT
 1 AS `id`,
 1 AS `dni`,
 1 AS `name`,
 1 AS `password`,
 1 AS `role`,
 1 AS `active`,
 1 AS `must_change`,
 1 AS `version`,
 1 AS `email`*/;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `audit` AS SELECT
 1 AS `id`,
 1 AS `actor_id`,
 1 AS `action`,
 1 AS `entity`,
 1 AS `detail`,
 1 AS `created_at`*/;
SET character_set_client = @saved_cs_client;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `auditoria` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `actor_id` bigint DEFAULT NULL,
  `action` varchar(60) NOT NULL,
  `entity` varchar(80) NOT NULL,
  `detail` varchar(1500) NOT NULL,
  `created_at` timestamp(6) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `actor_id` (`actor_id`),
  KEY `ix_audit_date` (`created_at`),
  CONSTRAINT `auditoria_ibfk_1` FOREIGN KEY (`actor_id`) REFERENCES `usuarios` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `auth_session` AS SELECT
 1 AS `id`,
 1 AS `user_id`,
 1 AS `expires_at`,
 1 AS `revoked`*/;
SET character_set_client = @saved_cs_client;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `bloqueo_tienda` (
  `id` int NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `chat_session` AS SELECT
 1 AS `hash`,
 1 AS `session_id`,
 1 AS `expires_at`*/;
SET character_set_client = @saved_cs_client;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `empleado` (
  `usuario_id` bigint NOT NULL,
  `supervisor_id` bigint DEFAULT NULL,
  PRIMARY KEY (`usuario_id`),
  KEY `supervisor_id` (`supervisor_id`),
  CONSTRAINT `empleado_ibfk_1` FOREIGN KEY (`usuario_id`) REFERENCES `usuarios` (`id`),
  CONSTRAINT `empleado_ibfk_2` FOREIGN KEY (`supervisor_id`) REFERENCES `supervisor` (`usuario_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `evaluaciones` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `day` date NOT NULL,
  `started_at` timestamp(6) NOT NULL,
  `finished_at` timestamp(6) NULL DEFAULT NULL,
  `lots` int NOT NULL DEFAULT '0',
  `status` varchar(20) NOT NULL,
  `error` varchar(300) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `ix_evaluation_day` (`day`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `evaluation` AS SELECT
 1 AS `id`,
 1 AS `day`,
 1 AS `started_at`,
 1 AS `finished_at`,
 1 AS `lots`,
 1 AS `status`,
 1 AS `error`*/;
SET character_set_client = @saved_cs_client;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `flyway_schema_history` (
  `installed_rank` int NOT NULL,
  `version` varchar(50) DEFAULT NULL,
  `description` varchar(200) NOT NULL,
  `type` varchar(20) NOT NULL,
  `script` varchar(1000) NOT NULL,
  `checksum` int DEFAULT NULL,
  `installed_by` varchar(100) NOT NULL,
  `installed_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `execution_time` int NOT NULL,
  `success` tinyint(1) NOT NULL,
  PRIMARY KEY (`installed_rank`),
  KEY `flyway_schema_history_s_idx` (`success`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `idempotency` AS SELECT
 1 AS `actor_id`,
 1 AS `request_key`,
 1 AS `fingerprint`,
 1 AS `result_id`*/;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `lot` AS SELECT
 1 AS `id`,
 1 AS `product_id`,
 1 AS `code`,
 1 AS `received`,
 1 AS `expiry`,
 1 AS `initial_quantity`,
 1 AS `normal`,
 1 AS `promo`,
 1 AS `cost`,
 1 AS `created_by`,
 1 AS `created_at`,
 1 AS `version`*/;
SET character_set_client = @saved_cs_client;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `lotes` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `product_id` bigint NOT NULL,
  `code` varchar(60) NOT NULL,
  `received` date NOT NULL,
  `expiry` date NOT NULL,
  `initial_quantity` decimal(14,3) NOT NULL,
  `normal` decimal(14,3) NOT NULL,
  `promo` decimal(14,3) NOT NULL DEFAULT '0.000',
  `cost` decimal(14,4) DEFAULT NULL,
  `created_by` bigint NOT NULL,
  `created_at` timestamp(6) NOT NULL,
  `version` bigint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `product_id` (`product_id`,`code`,`expiry`),
  KEY `created_by` (`created_by`),
  KEY `ix_lot_expiry` (`expiry`,`id`),
  CONSTRAINT `lotes_ibfk_1` FOREIGN KEY (`product_id`) REFERENCES `productos` (`id`),
  CONSTRAINT `lotes_ibfk_2` FOREIGN KEY (`created_by`) REFERENCES `usuarios` (`id`),
  CONSTRAINT `ck_cost` CHECK (((`cost` is null) or (`cost` >= 0))),
  CONSTRAINT `ck_dates` CHECK ((`expiry` >= `received`)),
  CONSTRAINT `ck_lot_qty` CHECK (((`initial_quantity` > 0) and (`normal` >= 0) and (`promo` >= 0)))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `movement` AS SELECT
 1 AS `id`,
 1 AS `lot_id`,
 1 AS `actor_id`,
 1 AS `type`,
 1 AS `source`,
 1 AS `quantity`,
 1 AS `normal_delta`,
 1 AS `promo_delta`,
 1 AS `cost`,
 1 AS `reason`,
 1 AS `created_at`,
 1 AS `reversal_of`*/;
SET character_set_client = @saved_cs_client;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `movimientos` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `lot_id` bigint NOT NULL,
  `actor_id` bigint NOT NULL,
  `type` varchar(20) NOT NULL,
  `source` varchar(10) NOT NULL,
  `quantity` decimal(14,3) NOT NULL,
  `normal_delta` decimal(14,3) NOT NULL,
  `promo_delta` decimal(14,3) NOT NULL,
  `cost` decimal(14,4) DEFAULT NULL,
  `reason` varchar(500) NOT NULL,
  `created_at` timestamp(6) NOT NULL,
  `reversal_of` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `reversal_of` (`reversal_of`),
  KEY `lot_id` (`lot_id`),
  KEY `actor_id` (`actor_id`),
  KEY `ix_movement_date` (`created_at`,`lot_id`),
  CONSTRAINT `movimientos_ibfk_1` FOREIGN KEY (`lot_id`) REFERENCES `lotes` (`id`),
  CONSTRAINT `movimientos_ibfk_2` FOREIGN KEY (`actor_id`) REFERENCES `usuarios` (`id`),
  CONSTRAINT `movimientos_ibfk_3` FOREIGN KEY (`reversal_of`) REFERENCES `movimientos` (`id`),
  CONSTRAINT `ck_movement_type` CHECK ((`type` in (_utf8mb4'INGRESO',_utf8mb4'PROMOCION',_utf8mb4'VENTA',_utf8mb4'MERMA',_utf8mb4'AJUSTE',_utf8mb4'REVERSO')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `notificaciones` (
  `id` varchar(36) NOT NULL,
  `alert_id` bigint NOT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'PENDING',
  `attempts` int NOT NULL DEFAULT '0',
  `next_attempt` timestamp(6) NOT NULL,
  `last_error` varchar(300) DEFAULT NULL,
  `accepted_at` timestamp(6) NULL DEFAULT NULL,
  `delivered_at` timestamp(6) NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `alert_id` (`alert_id`),
  KEY `ix_outbox_due` (`status`,`next_attempt`),
  CONSTRAINT `notificaciones_ibfk_1` FOREIGN KEY (`alert_id`) REFERENCES `alertas` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `operaciones_idempotentes` (
  `actor_id` bigint NOT NULL,
  `request_key` varchar(36) NOT NULL,
  `fingerprint` varchar(64) NOT NULL,
  `result_id` bigint NOT NULL,
  PRIMARY KEY (`actor_id`,`request_key`),
  CONSTRAINT `operaciones_idempotentes_ibfk_1` FOREIGN KEY (`actor_id`) REFERENCES `usuarios` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `outbox` AS SELECT
 1 AS `id`,
 1 AS `alert_id`,
 1 AS `status`,
 1 AS `attempts`,
 1 AS `next_attempt`,
 1 AS `last_error`,
 1 AS `accepted_at`,
 1 AS `delivered_at`*/;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `password_reset` AS SELECT
 1 AS `hash`,
 1 AS `user_id`,
 1 AS `password_snapshot`,
 1 AS `email_snapshot`,
 1 AS `created_at`,
 1 AS `expires_at`,
 1 AS `used`,
 1 AS `delivery`*/;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `product` AS SELECT
 1 AS `id`,
 1 AS `sku`,
 1 AS `ean`,
 1 AS `name`,
 1 AS `category`,
 1 AS `unit`,
 1 AS `active`,
 1 AS `warning_days`,
 1 AS `critical_days`,
 1 AS `version`*/;
SET character_set_client = @saved_cs_client;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `productos` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `sku` varchar(40) NOT NULL,
  `ean` varchar(13) DEFAULT NULL,
  `name` varchar(120) NOT NULL,
  `category` varchar(80) NOT NULL,
  `unit` varchar(12) NOT NULL,
  `active` tinyint(1) NOT NULL DEFAULT '1',
  `warning_days` int NOT NULL DEFAULT '30',
  `critical_days` int NOT NULL DEFAULT '15',
  `version` bigint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `sku` (`sku`),
  UNIQUE KEY `ean` (`ean`),
  CONSTRAINT `ck_ean` CHECK (((`ean` is null) or regexp_like(`ean`,_utf8mb4'^[0-9]{13}$'))),
  CONSTRAINT `ck_threshold` CHECK (((`critical_days` >= 0) and (`warning_days` > `critical_days`) and (`warning_days` <= 3650))),
  CONSTRAINT `ck_unit` CHECK ((`unit` in (_utf8mb4'UNIDAD',_utf8mb4'KG',_utf8mb4'LITRO')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `programador` (
  `usuario_id` bigint NOT NULL,
  PRIMARY KEY (`usuario_id`),
  CONSTRAINT `programador_ibfk_1` FOREIGN KEY (`usuario_id`) REFERENCES `usuarios` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `recepciones_servicio` (
  `event_id` varchar(36) NOT NULL,
  `accepted_at` timestamp(6) NOT NULL,
  PRIMARY KEY (`event_id`),
  CONSTRAINT `recepciones_servicio_ibfk_1` FOREIGN KEY (`event_id`) REFERENCES `notificaciones` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `recuperaciones_clave` (
  `hash` char(64) NOT NULL,
  `user_id` bigint NOT NULL,
  `password_snapshot` varchar(100) NOT NULL,
  `email_snapshot` varchar(254) NOT NULL,
  `created_at` timestamp(6) NOT NULL,
  `expires_at` timestamp(6) NOT NULL,
  `used` tinyint(1) NOT NULL DEFAULT '0',
  `delivery` varchar(12) NOT NULL DEFAULT 'PENDING',
  PRIMARY KEY (`hash`),
  KEY `ix_reset_user` (`user_id`,`created_at`),
  CONSTRAINT `recuperaciones_clave_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `usuarios` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `refresh_token` AS SELECT
 1 AS `hash`,
 1 AS `session_id`,
 1 AS `used`*/;
SET character_set_client = @saved_cs_client;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `renovaciones_sesion` (
  `hash` varchar(64) NOT NULL,
  `session_id` varchar(36) NOT NULL,
  `used` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`hash`),
  KEY `session_id` (`session_id`),
  CONSTRAINT `renovaciones_sesion_ibfk_1` FOREIGN KEY (`session_id`) REFERENCES `sesiones` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `service_receipt` AS SELECT
 1 AS `event_id`,
 1 AS `accepted_at`*/;
SET character_set_client = @saved_cs_client;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sesiones` (
  `id` varchar(36) NOT NULL,
  `user_id` bigint NOT NULL,
  `expires_at` timestamp(6) NOT NULL,
  `revoked` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `sesiones_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `usuarios` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sesiones_chat` (
  `hash` char(64) NOT NULL,
  `session_id` varchar(36) NOT NULL,
  `expires_at` timestamp(6) NOT NULL,
  PRIMARY KEY (`hash`),
  KEY `session_id` (`session_id`),
  CONSTRAINT `sesiones_chat_ibfk_1` FOREIGN KEY (`session_id`) REFERENCES `sesiones` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `solicitudes_clave` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `usuario_id` bigint NOT NULL,
  `estado` varchar(20) NOT NULL DEFAULT 'PENDIENTE',
  `creada_el` timestamp(6) NOT NULL,
  `resuelta_el` timestamp(6) NULL DEFAULT NULL,
  `resuelta_por` bigint DEFAULT NULL,
  `pendiente_usuario` bigint GENERATED ALWAYS AS ((case when (`estado` = _utf8mb4'PENDIENTE') then `usuario_id` else NULL end)) STORED,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_solicitud_pendiente` (`pendiente_usuario`),
  KEY `usuario_id` (`usuario_id`),
  KEY `resuelta_por` (`resuelta_por`),
  CONSTRAINT `solicitudes_clave_ibfk_1` FOREIGN KEY (`usuario_id`) REFERENCES `usuarios` (`id`),
  CONSTRAINT `solicitudes_clave_ibfk_2` FOREIGN KEY (`resuelta_por`) REFERENCES `usuarios` (`id`),
  CONSTRAINT `solicitudes_clave_chk_1` CHECK ((`estado` in (_utf8mb4'PENDIENTE',_utf8mb4'RESUELTA')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `store_lock` AS SELECT
 1 AS `id`*/;
SET character_set_client = @saved_cs_client;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `supervisor` (
  `usuario_id` bigint NOT NULL,
  PRIMARY KEY (`usuario_id`),
  CONSTRAINT `supervisor_ibfk_1` FOREIGN KEY (`usuario_id`) REFERENCES `usuarios` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `usuarios` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `dni` varchar(8) NOT NULL,
  `name` varchar(100) NOT NULL,
  `password` varchar(100) NOT NULL,
  `role` varchar(20) NOT NULL,
  `active` tinyint(1) NOT NULL DEFAULT '1',
  `must_change` tinyint(1) NOT NULL DEFAULT '1',
  `version` bigint NOT NULL DEFAULT '0',
  `email` varchar(254) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `dni` (`dni`),
  UNIQUE KEY `email` (`email`),
  CONSTRAINT `ck_dni` CHECK (regexp_like(`dni`,_utf8mb4'^[0-9]{8}$')),
  CONSTRAINT `ck_role` CHECK ((`role` in (_utf8mb4'ADMIN',_utf8mb4'SUPERVISOR',_utf8mb4'WORKER')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017*/ /*!50003 TRIGGER `perfiles_crear` AFTER INSERT ON `usuarios` FOR EACH ROW BEGIN
 IF NEW.role='ADMIN' THEN INSERT INTO programador VALUES(NEW.id);
 ELSEIF NEW.role='SUPERVISOR' THEN INSERT INTO supervisor VALUES(NEW.id);
 ELSE INSERT INTO empleado(usuario_id) VALUES(NEW.id); END IF;
END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017*/ /*!50003 TRIGGER `perfiles_cambiar` AFTER UPDATE ON `usuarios` FOR EACH ROW BEGIN
 IF OLD.role<>NEW.role THEN
  IF OLD.role='ADMIN' THEN DELETE FROM programador WHERE usuario_id=OLD.id;
  ELSEIF OLD.role='SUPERVISOR' THEN DELETE FROM supervisor WHERE usuario_id=OLD.id;
  ELSE DELETE FROM empleado WHERE usuario_id=OLD.id; END IF;
  IF NEW.role='ADMIN' THEN INSERT INTO programador VALUES(NEW.id);
  ELSEIF NEW.role='SUPERVISOR' THEN INSERT INTO supervisor VALUES(NEW.id);
  ELSE INSERT INTO empleado(usuario_id) VALUES(NEW.id); END IF;
 END IF;
END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50001 DROP VIEW IF EXISTS `alert`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 SQL SECURITY INVOKER */
/*!50001 VIEW `alert` AS select `alertas`.`id` AS `id`,`alertas`.`lot_id` AS `lot_id`,`alertas`.`state` AS `state`,`alertas`.`attended_at` AS `attended_at`,`alertas`.`created_at` AS `created_at`,`alertas`.`attended_by` AS `attended_by` from `alertas` */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;
/*!50001 DROP VIEW IF EXISTS `app_user`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 SQL SECURITY INVOKER */
/*!50001 VIEW `app_user` AS select `usuarios`.`id` AS `id`,`usuarios`.`dni` AS `dni`,`usuarios`.`name` AS `name`,`usuarios`.`password` AS `password`,`usuarios`.`role` AS `role`,`usuarios`.`active` AS `active`,`usuarios`.`must_change` AS `must_change`,`usuarios`.`version` AS `version`,`usuarios`.`email` AS `email` from `usuarios` */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;
/*!50001 DROP VIEW IF EXISTS `audit`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 SQL SECURITY INVOKER */
/*!50001 VIEW `audit` AS select `auditoria`.`id` AS `id`,`auditoria`.`actor_id` AS `actor_id`,`auditoria`.`action` AS `action`,`auditoria`.`entity` AS `entity`,`auditoria`.`detail` AS `detail`,`auditoria`.`created_at` AS `created_at` from `auditoria` */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;
/*!50001 DROP VIEW IF EXISTS `auth_session`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 SQL SECURITY INVOKER */
/*!50001 VIEW `auth_session` AS select `sesiones`.`id` AS `id`,`sesiones`.`user_id` AS `user_id`,`sesiones`.`expires_at` AS `expires_at`,`sesiones`.`revoked` AS `revoked` from `sesiones` */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;
/*!50001 DROP VIEW IF EXISTS `chat_session`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 SQL SECURITY INVOKER */
/*!50001 VIEW `chat_session` AS select `sesiones_chat`.`hash` AS `hash`,`sesiones_chat`.`session_id` AS `session_id`,`sesiones_chat`.`expires_at` AS `expires_at` from `sesiones_chat` */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;
/*!50001 DROP VIEW IF EXISTS `evaluation`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 SQL SECURITY INVOKER */
/*!50001 VIEW `evaluation` AS select `evaluaciones`.`id` AS `id`,`evaluaciones`.`day` AS `day`,`evaluaciones`.`started_at` AS `started_at`,`evaluaciones`.`finished_at` AS `finished_at`,`evaluaciones`.`lots` AS `lots`,`evaluaciones`.`status` AS `status`,`evaluaciones`.`error` AS `error` from `evaluaciones` */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;
/*!50001 DROP VIEW IF EXISTS `idempotency`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 SQL SECURITY INVOKER */
/*!50001 VIEW `idempotency` AS select `operaciones_idempotentes`.`actor_id` AS `actor_id`,`operaciones_idempotentes`.`request_key` AS `request_key`,`operaciones_idempotentes`.`fingerprint` AS `fingerprint`,`operaciones_idempotentes`.`result_id` AS `result_id` from `operaciones_idempotentes` */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;
/*!50001 DROP VIEW IF EXISTS `lot`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 SQL SECURITY INVOKER */
/*!50001 VIEW `lot` AS select `lotes`.`id` AS `id`,`lotes`.`product_id` AS `product_id`,`lotes`.`code` AS `code`,`lotes`.`received` AS `received`,`lotes`.`expiry` AS `expiry`,`lotes`.`initial_quantity` AS `initial_quantity`,`lotes`.`normal` AS `normal`,`lotes`.`promo` AS `promo`,`lotes`.`cost` AS `cost`,`lotes`.`created_by` AS `created_by`,`lotes`.`created_at` AS `created_at`,`lotes`.`version` AS `version` from `lotes` */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;
/*!50001 DROP VIEW IF EXISTS `movement`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 SQL SECURITY INVOKER */
/*!50001 VIEW `movement` AS select `movimientos`.`id` AS `id`,`movimientos`.`lot_id` AS `lot_id`,`movimientos`.`actor_id` AS `actor_id`,`movimientos`.`type` AS `type`,`movimientos`.`source` AS `source`,`movimientos`.`quantity` AS `quantity`,`movimientos`.`normal_delta` AS `normal_delta`,`movimientos`.`promo_delta` AS `promo_delta`,`movimientos`.`cost` AS `cost`,`movimientos`.`reason` AS `reason`,`movimientos`.`created_at` AS `created_at`,`movimientos`.`reversal_of` AS `reversal_of` from `movimientos` */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;
/*!50001 DROP VIEW IF EXISTS `outbox`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 SQL SECURITY INVOKER */
/*!50001 VIEW `outbox` AS select `notificaciones`.`id` AS `id`,`notificaciones`.`alert_id` AS `alert_id`,`notificaciones`.`status` AS `status`,`notificaciones`.`attempts` AS `attempts`,`notificaciones`.`next_attempt` AS `next_attempt`,`notificaciones`.`last_error` AS `last_error`,`notificaciones`.`accepted_at` AS `accepted_at`,`notificaciones`.`delivered_at` AS `delivered_at` from `notificaciones` */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;
/*!50001 DROP VIEW IF EXISTS `password_reset`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 SQL SECURITY INVOKER */
/*!50001 VIEW `password_reset` AS select `recuperaciones_clave`.`hash` AS `hash`,`recuperaciones_clave`.`user_id` AS `user_id`,`recuperaciones_clave`.`password_snapshot` AS `password_snapshot`,`recuperaciones_clave`.`email_snapshot` AS `email_snapshot`,`recuperaciones_clave`.`created_at` AS `created_at`,`recuperaciones_clave`.`expires_at` AS `expires_at`,`recuperaciones_clave`.`used` AS `used`,`recuperaciones_clave`.`delivery` AS `delivery` from `recuperaciones_clave` */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;
/*!50001 DROP VIEW IF EXISTS `product`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 SQL SECURITY INVOKER */
/*!50001 VIEW `product` AS select `productos`.`id` AS `id`,`productos`.`sku` AS `sku`,`productos`.`ean` AS `ean`,`productos`.`name` AS `name`,`productos`.`category` AS `category`,`productos`.`unit` AS `unit`,`productos`.`active` AS `active`,`productos`.`warning_days` AS `warning_days`,`productos`.`critical_days` AS `critical_days`,`productos`.`version` AS `version` from `productos` */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;
/*!50001 DROP VIEW IF EXISTS `refresh_token`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 SQL SECURITY INVOKER */
/*!50001 VIEW `refresh_token` AS select `renovaciones_sesion`.`hash` AS `hash`,`renovaciones_sesion`.`session_id` AS `session_id`,`renovaciones_sesion`.`used` AS `used` from `renovaciones_sesion` */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;
/*!50001 DROP VIEW IF EXISTS `service_receipt`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 SQL SECURITY INVOKER */
/*!50001 VIEW `service_receipt` AS select `recepciones_servicio`.`event_id` AS `event_id`,`recepciones_servicio`.`accepted_at` AS `accepted_at` from `recepciones_servicio` */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;
/*!50001 DROP VIEW IF EXISTS `store_lock`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 SQL SECURITY INVOKER */
/*!50001 VIEW `store_lock` AS select `bloqueo_tienda`.`id` AS `id` from `bloqueo_tienda` */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
