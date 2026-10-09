// Configure only this project's local n8n. Never sends a WhatsApp message.
import {readFileSync,writeFileSync,mkdirSync} from 'node:fs';
import {randomBytes} from 'node:crypto';
import {execFileSync} from 'node:child_process';
const env=Object.fromEntries(readFileSync('infra/.env','utf8').split(/\r?\n/).filter(Boolean).map(l=>{const i=l.indexOf('=');return[l.slice(0,i),l.slice(i+1)];}));
mkdirSync('.local',{recursive:true});
const credentialFile='.local/n8n-credentials.json';
writeFileSync(credentialFile,JSON.stringify([
 {id:'semaforo-webhook',name:'Semáforo webhook',type:'httpHeaderAuth',data:{name:'Authorization',value:'Bearer '+env.N8N_TOKEN}},
 {id:'semaforo-service',name:'Semáforo servicio',type:'httpHeaderAuth',data:{name:'Authorization',value:'Bearer '+env.SERVICE_TOKEN}}
]));
const owner={email:'admin@semaforo.local',firstName:'Administrador',lastName:'Local',password:'Sd!'+randomBytes(20).toString('base64url')};
const settings=await (await fetch('http://localhost:5678/rest/settings')).json();
if(settings.data?.userManagement?.showSetupOnFirstLoad){
 const result=await fetch('http://localhost:5678/rest/owner/setup',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(owner)});
 if(!result.ok)throw new Error('Configuración de propietario rechazada: '+result.status);
 writeFileSync('.local/n8n-owner.json',JSON.stringify(owner));
}
function docker(args){execFileSync('docker',['exec','semaforo-n8n-1',...args],{stdio:['ignore','pipe','pipe']});}
execFileSync('docker',['cp',credentialFile,'semaforo-n8n-1:/tmp/semaforo-credentials.json']);
docker(['n8n','import:credentials','--input=/tmp/semaforo-credentials.json']);
for(const [file,id] of [['critical-events','semaforo-events'],['chatbot','semaforo-chat']]){
 execFileSync('docker',['cp','n8n/'+file+'.json','semaforo-n8n-1:/tmp/'+file+'.json']);
 docker(['n8n','import:workflow','--input=/tmp/'+file+'.json']);
 docker(['n8n','publish:workflow','--id='+id]);
}
execFileSync('docker',['exec','--user','root','semaforo-n8n-1','rm','-f','/tmp/semaforo-credentials.json']);
// Remove only the generated cleartext credential transport file, not n8n storage.
const {unlinkSync}=await import('node:fs');unlinkSync(credentialFile);
execFileSync('docker',['restart','semaforo-n8n-1'],{stdio:'ignore'});
console.log('Flujos y credenciales locales importados. Credenciales del editor en .local/n8n-owner.json si se creó el propietario.');

