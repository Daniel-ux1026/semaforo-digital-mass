-- Recreate profile triggers under Flyway's migration account.
-- Existing users and profile records remain intact.
DROP TRIGGER IF EXISTS perfiles_crear;
DROP TRIGGER IF EXISTS perfiles_cambiar;

DELIMITER $$
CREATE TRIGGER perfiles_crear AFTER INSERT ON usuarios FOR EACH ROW
BEGIN
 IF NEW.role='ADMIN' THEN INSERT INTO programador VALUES(NEW.id);
 ELSEIF NEW.role='SUPERVISOR' THEN INSERT INTO supervisor VALUES(NEW.id);
 ELSE INSERT INTO empleado(usuario_id) VALUES(NEW.id); END IF;
END$$
CREATE TRIGGER perfiles_cambiar AFTER UPDATE ON usuarios FOR EACH ROW
BEGIN
 IF OLD.role<>NEW.role THEN
  IF OLD.role='ADMIN' THEN DELETE FROM programador WHERE usuario_id=OLD.id;
  ELSEIF OLD.role='SUPERVISOR' THEN DELETE FROM supervisor WHERE usuario_id=OLD.id;
  ELSE DELETE FROM empleado WHERE usuario_id=OLD.id; END IF;
  IF NEW.role='ADMIN' THEN INSERT INTO programador VALUES(NEW.id);
  ELSEIF NEW.role='SUPERVISOR' THEN INSERT INTO supervisor VALUES(NEW.id);
  ELSE INSERT INTO empleado(usuario_id) VALUES(NEW.id); END IF;
 END IF;
END$$
DELIMITER ;
