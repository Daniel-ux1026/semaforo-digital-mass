ALTER TABLE usuarios DROP CHECK ck_role;
ALTER TABLE usuarios ADD CONSTRAINT ck_role CHECK (role IN ('ADMIN','SUPERVISOR','WORKER'));
CREATE TABLE supervisor (usuario_id BIGINT PRIMARY KEY, FOREIGN KEY(usuario_id) REFERENCES usuarios(id));
CREATE TABLE programador (usuario_id BIGINT PRIMARY KEY, FOREIGN KEY(usuario_id) REFERENCES usuarios(id));
CREATE TABLE empleado (usuario_id BIGINT PRIMARY KEY, supervisor_id BIGINT NULL, FOREIGN KEY(usuario_id) REFERENCES usuarios(id), FOREIGN KEY(supervisor_id) REFERENCES supervisor(usuario_id));
INSERT INTO supervisor SELECT id FROM usuarios WHERE role='SUPERVISOR';
INSERT INTO programador SELECT id FROM usuarios WHERE role='ADMIN';
INSERT INTO empleado(usuario_id) SELECT id FROM usuarios WHERE role='WORKER';
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
