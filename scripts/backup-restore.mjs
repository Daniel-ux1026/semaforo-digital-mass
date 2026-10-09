import {execFileSync} from 'node:child_process';
import {mkdirSync,writeFileSync} from 'node:fs';
mkdirSync('.local/backups',{recursive:true});
function mysql(sql){return execFileSync('docker',['exec','-i','semaforo-mysql-1','sh','-c','MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot -N -B'],{input:sql,encoding:'utf8',stdio:['pipe','pipe','pipe']}).trim();}
const dump=execFileSync('docker',['exec','semaforo-mysql-1','sh','-c','MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysqldump -uroot --single-transaction --no-tablespaces --set-gtid-purged=OFF semaforo'],{maxBuffer:32*1024*1024});
const file='.local/backups/semaforo-'+new Date().toISOString().replace(/[:.]/g,'-')+'.sql';writeFileSync(file,dump);
const restore='restore_check_'+Date.now();mysql(`CREATE DATABASE ${restore};`);
try{
 execFileSync('docker',['exec','-i','semaforo-mysql-1','sh','-c',`MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot ${restore}`],{input:dump,stdio:['pipe','pipe','pipe']});
 const tables=['solicitudes_clave','empleado','supervisor','programador','usuarios','productos','lotes','movimientos','auditoria','alertas','notificaciones','sesiones','renovaciones_sesion','recuperaciones_clave','sesiones_chat','operaciones_idempotentes','recepciones_servicio','evaluaciones','bloqueo_tienda'];const checks=[];
 for(const table of tables){const source=Number(mysql(`SELECT COUNT(*) FROM semaforo.${table};`));const restored=Number(mysql(`SELECT COUNT(*) FROM ${restore}.${table};`));if(source!==restored)throw new Error('Diferencia al restaurar '+table);checks.push({table,source,restored});}
 if(Number(mysql(`SELECT COUNT(*) FROM information_schema.VIEW_TABLE_USAGE WHERE VIEW_SCHEMA='${restore}' AND TABLE_SCHEMA<>'${restore}';`))!==0)throw new Error('Una vista restaurada referencia otra base.');
 writeFileSync('docs/backup-verification.json',JSON.stringify({time:new Date().toISOString(),file,checks,status:'passed',note:'Restauración en base desechable independiente; respaldo contiene datos privados y permanece en .local.'},null,2));
 console.log('Respaldo y restauración comprobados en una base temporal. Se conserva el respaldo en .local/backups.');
}finally{mysql(`DROP DATABASE ${restore};`);}
