import {request} from '@playwright/test';
import {readFileSync,writeFileSync,mkdirSync} from 'node:fs';
import {exportarEstructura} from './exportar-estructura.mjs';
const folder=new URL('./datos/',import.meta.url);mkdirSync(folder,{recursive:true});
const user=JSON.parse(readFileSync(new URL('../.local/demo-user.json',import.meta.url),'utf8'));
const ctx=await request.newContext({baseURL:'http://localhost:4200'});
try{
 const csrf=await (await ctx.get('/api/auth/csrf')).json();
 const login=await ctx.post('/api/auth/login',{data:user,headers:{'X-XSRF-TOKEN':csrf.token}});
 if(!login.ok())throw new Error('No se pudo autenticar al administrador local.');
 const token=(await login.json()).accessToken;const headers={Authorization:'Bearer '+token};
 const tables=await (await ctx.get('/api/admin/database',{headers})).json();
 if(!Array.isArray(tables))throw new Error('No se pudo consultar el catálogo.');
 const all={};const stamp=new Date().toISOString();
 const db='semaforo_consulta_'+stamp.replace(/\D/g,'').slice(0,14);
 let sql=`-- Copia de consulta generada ${stamp}. No contiene contraseñas ni tokens.\n-- Las tablas y columnas de esta copia están en español. No sustituye un respaldo operativo.\nCREATE DATABASE \`${db}\` CHARACTER SET utf8mb4;\nUSE \`${db}\`;\nSET SQL_MODE='NO_BACKSLASH_ESCAPES';\n`;
 const quote=v=>v==null?'NULL':"'"+String(typeof v==='object'?JSON.stringify(v):v).replaceAll("'","''")+"'";
 for(const table of tables){
  let rows=[],page=0,more=true;
  while(more){const r=await ctx.get('/api/admin/database/'+table+'?page='+page++,{headers});if(!r.ok())throw new Error('Error de consulta en '+table);const d=await r.json();rows.push(...d.items);more=d.hasNext;}
  all[table]=rows;
  const keys=rows.length?Object.keys(rows[0]):['sin_registros'];
  sql+=`\nCREATE TABLE \`${table}\` (${keys.map(k=>'`'+k+'` LONGTEXT NULL').join(', ')});\n`;
  for(const row of rows)sql+=`INSERT INTO \`${table}\` VALUES (${keys.map(k=>quote(row[k])).join(',')});\n`;
 }
 writeFileSync(new URL('semaforo_consulta.sql',folder),sql);
 writeFileSync(new URL('datos.json',folder),JSON.stringify({generado:stamp,tablas:all},null,2));
 exportarEstructura();
 const count=Object.fromEntries(Object.entries(all).map(([k,v])=>[k,v.length]));
 writeFileSync(new URL('../inventario_exportacion.json',folder),JSON.stringify({fecha:stamp,tablas:count},null,2));
 console.log('Exportadas '+tables.length+' tablas españolas a base_de_datos/datos; estructura real guardada sin datos privados.');
}finally{await ctx.dispose();}
