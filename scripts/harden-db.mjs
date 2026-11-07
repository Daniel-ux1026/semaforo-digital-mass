import {readFileSync,appendFileSync} from 'node:fs';
import {randomBytes} from 'node:crypto';
import {execFileSync} from 'node:child_process';
const env=Object.fromEntries(readFileSync('infra/.env','utf8').split(/\r?\n/).filter(Boolean).map(l=>{const i=l.indexOf('=');return[l.slice(0,i),l.slice(i+1)];}));
const migration=env.MIGRATION_PASSWORD||randomBytes(36).toString('base64url');
if(!/^[A-Za-z0-9_\-+/=]+$/.test(migration))throw new Error('Formato de secreto migrador inválido.');
const container=`${process.env.COMPOSE_PROJECT_NAME || 'semaforo'}-mysql-1`;
function mysql(sql){
 return execFileSync('docker',['exec','-i',container,'sh','-c','MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot -NB'],{input:sql,encoding:'utf8',stdio:['pipe','pipe','pipe']}).trim();
}
if(process.argv.includes('--prepare')){
 if(!env.MIGRATION_PASSWORD)appendFileSync('infra/.env',`\nMIGRATION_USER=migrator\nMIGRATION_PASSWORD=${migration}\n`);
 mysql(`CREATE USER IF NOT EXISTS 'migrator'@'%' IDENTIFIED BY '${migration}';\nALTER USER 'migrator'@'%' IDENTIFIED BY '${migration}';\nGRANT ALL PRIVILEGES ON semaforo.* TO 'migrator'@'%';\n`);
 console.log('Usuario migrador preparado.');
 process.exit(0);
}
let ready=false;
for(let attempt=0;attempt<45;attempt++){
 try{if(mysql("SELECT COUNT(*) FROM semaforo.flyway_schema_history WHERE version=8 AND success=1;")==='1'){ready=true;break;}}catch{}
 await new Promise(resolve=>setTimeout(resolve,1000));
}
if(!ready)throw new Error('La migración V8 aún no está aplicada; revise los logs de la API.');
if(mysql("SELECT COUNT(*) FROM information_schema.TRIGGERS WHERE TRIGGER_SCHEMA='semaforo' AND TRIGGER_NAME IN ('perfiles_crear','perfiles_cambiar') AND DEFINER='migrator@%';")!=='2')throw new Error('Los disparadores de perfiles deben pertenecer al migrador antes de limitar los permisos.');
let sql=`REVOKE ALL PRIVILEGES ON semaforo.* FROM 'semaforo'@'%';\nGRANT SELECT ON semaforo.* TO 'semaforo'@'%';\n`;
for(const table of ['app_user','auth_session','refresh_token','product','lot','alert','evaluation','outbox','password_reset','chat_session','usuarios','sesiones','renovaciones_sesion','productos','lotes','alertas','evaluaciones','notificaciones','recuperaciones_clave','sesiones_chat'])sql+=`GRANT INSERT,UPDATE ON semaforo.${table} TO 'semaforo'@'%';\n`;
for(const table of ['movement','audit','idempotency','service_receipt','movimientos','auditoria','operaciones_idempotentes','recepciones_servicio'])sql+=`GRANT INSERT ON semaforo.${table} TO 'semaforo'@'%';\n`;
sql+='GRANT UPDATE ON semaforo.bloqueo_tienda TO \'semaforo\'@\'%\';\n';
sql+="GRANT INSERT,UPDATE ON semaforo.solicitudes_clave TO 'semaforo'@'%';\n";
sql+="GRANT UPDATE ON semaforo.empleado TO 'semaforo'@'%';\n";
sql+="GRANT INSERT,UPDATE ON semaforo.avisos_correo TO 'semaforo'@'%';\n";
sql+='GRANT UPDATE ON semaforo.store_lock TO \'semaforo\'@\'%\';\n';
mysql(sql);
console.log('Privilegios separados: aplicación sin DELETE, movimientos/auditoría sin UPDATE. Migrador configurado en infra/.env.');
