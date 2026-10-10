import {execFileSync} from 'node:child_process';
import {writeFileSync} from 'node:fs';
import {fileURLToPath} from 'node:url';
import {resolve} from 'node:path';

const container='semaforo-mysql-1';
const schema='semaforo';

/** Exporta definiciones de MySQL sin filas ni credenciales. */
export function exportarEstructura(){
 const metadata=execFileSync('docker',['exec','-i',container,'sh','-c','MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot -N -B'],{
  input:`SELECT TABLE_NAME,TABLE_TYPE FROM information_schema.TABLES WHERE TABLE_SCHEMA='${schema}' ORDER BY TABLE_NAME;\n`,
  encoding:'utf8',stdio:['pipe','pipe','pipe']
 }).trim().split(/\r?\n/).filter(Boolean).map(row=>row.split('\t'));
 const tables=metadata.filter(([,type])=>type==='BASE TABLE').map(([name])=>name);
 const views=metadata.filter(([,type])=>type==='VIEW').map(([name])=>name);
 for(const required of ['usuarios','empleado','supervisor','programador','solicitudes_clave']){
  if(!tables.includes(required))throw new Error(`Falta ${required} en la base activa. Inicie el proyecto y aplique sus migraciones antes de exportar.`);
 }
 let dump=execFileSync('docker',['exec',container,'sh','-c',
  'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysqldump -uroot --no-data --triggers --skip-add-drop-table --skip-add-drop-trigger --no-tablespaces --set-gtid-purged=OFF --skip-comments semaforo'
 ],{encoding:'utf8',maxBuffer:20*1024*1024,stdio:['pipe','pipe','pipe']});
 // El usuario que importe la estructura será el propietario de sus triggers.
 dump=dump.replace(/\sDEFINER\s*=\s*`[^`]*`@`[^`]*`/g,'');
 // Los contadores locales no forman parte de la definición del esquema.
 dump=dump.replace(/\sAUTO_INCREMENT=\d+/g,'');
 dump=dump.replace(/[ \t]+$/gm,'').trimEnd()+'\n';
 if(/^\s*(INSERT|REPLACE)\s+INTO\b/im.test(dump))throw new Error('La exportación debe contener solo estructura.');
 const business=tables.filter(name=>name!=='flyway_schema_history');
 const header=[
  '-- ESTRUCTURA ACTUAL DE SEMÁFORO DIGITAL MASS',
  `-- Generada: ${new Date().toISOString()}`,
  `-- ${business.length} tablas de negocio, ${tables.length-business.length} tabla técnica y ${views.length} vistas de compatibilidad.`,
  '-- Incluye claves, índices, restricciones y triggers. No incluye registros ni cuentas.',
  '-- Para visualizarla: cree una base vacía en Workbench, selecciónela con USE y ejecute este archivo.',
  '-- Para instalar la aplicación: ejecute INICIAR_PROYECTO.bat. Flyway aplica V1 a V6.',
  '-- No importe esta copia en la base operativa ni la use para actualizar una instalación existente.',
  '-- La tabla técnica se exporta vacía. Flyway gestiona su historial al iniciar el proyecto.',
  '-- Tablas: '+tables.join(', '),
  '-- Los CREATE TABLE temporales con nombres ingleses preparan las vistas de compatibilidad.',
  '',
 ].join('\n');
 writeFileSync(new URL('./estructura_mysql.sql',import.meta.url),header+'\n'+dump,'utf8');
 console.log(`Estructura actualizada: ${business.length} tablas de negocio, ${tables.length-business.length} técnica y ${views.length} vistas, sin datos.`);
 return {tablas:tables,vistas:views};
}

if(process.argv[1]&&resolve(process.argv[1])===fileURLToPath(import.meta.url))exportarEstructura();
