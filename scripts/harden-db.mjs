import {readFileSync,appendFileSync} from 'node:fs';
import {randomBytes} from 'node:crypto';
import {execFileSync} from 'node:child_process';
const env=Object.fromEntries(readFileSync('infra/.env','utf8').split(/\r?\n/).filter(Boolean).map(l=>{const i=l.indexOf('=');return[l.slice(0,i),l.slice(i+1)];}));
const migration=env.MIGRATION_PASSWORD||randomBytes(36).toString('base64url');
if(!/^[A-Za-z0-9_\-+/=]+$/.test(migration))throw new Error('Formato de secreto migrador inválido.');
let ready=false;
for(let attempt=0;attempt<45;attempt++){
 try{const result=execFileSync('docker',['exec','-i','semaforo-mysql-1','sh','-c','MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot -Nse "SELECT COUNT(*) FROM semaforo.flyway_schema_history WHERE version=6 AND success=1"'],{encoding:'utf8',stdio:['ignore','pipe','pipe']});if(result.trim()==='1'){ready=true;break;}}catch{}
 await new Promise(resolve=>setTimeout(resolve,1000));
}
if(!ready)throw new Error('La migración V6 aún no está aplicada; revise los logs de la API.');
let sql=`CREATE USER IF NOT EXISTS 'migrator'@'%' IDENTIFIED BY '${migration}';\nGRANT ALL PRIVILEGES ON semaforo.* TO 'migrator'@'%';\nREVOKE ALL PRIVILEGES ON semaforo.* FROM 'semaforo'@'%';\nGRANT SELECT ON semaforo.* TO 'semaforo'@'%';\n`;
for(const table of ['app_user','auth_session','refresh_token','product','lot','alert','evaluation','outbox','password_reset','chat_session','usuarios','sesiones','renovaciones_sesion','productos','lotes','alertas','evaluaciones','notificaciones','recuperaciones_clave','sesiones_chat'])sql+=`GRANT INSERT,UPDATE ON semaforo.${table} TO 'semaforo'@'%';\n`;
for(const table of ['movement','audit','idempotency','service_receipt','movimientos','auditoria','operaciones_idempotentes','recepciones_servicio'])sql+=`GRANT INSERT ON semaforo.${table} TO 'semaforo'@'%';\n`;
sql+='GRANT UPDATE ON semaforo.bloqueo_tienda TO \'semaforo\'@\'%\';\n';
sql+="GRANT INSERT,UPDATE ON semaforo.solicitudes_clave TO 'semaforo'@'%';\n";
sql+="GRANT UPDATE ON semaforo.empleado TO 'semaforo'@'%';\n";
sql+='GRANT UPDATE ON semaforo.store_lock TO \'semaforo\'@\'%\';\n';
execFileSync('docker',['exec','-i','semaforo-mysql-1','sh','-c','MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot'],{input:sql,stdio:['pipe','pipe','pipe']});
if(!env.MIGRATION_PASSWORD)appendFileSync('infra/.env',`\nMIGRATION_USER=migrator\nMIGRATION_PASSWORD=${migration}\n`);
console.log('Privilegios separados: aplicación sin DELETE, movimientos/auditoría sin UPDATE. Migrador configurado en infra/.env.');
