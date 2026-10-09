import {execFileSync} from 'node:child_process';
import {readFileSync} from 'node:fs';
const files=execFileSync('git',['ls-files','-z'],{encoding:'utf8'}).split('\0').filter(Boolean);
const patterns=[/-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----/,/gh[pousr]_[A-Za-z0-9]{30,}/,/github_pat_[A-Za-z0-9_]{30,}/,/sk-(?:proj-|svcacct-)?[A-Za-z0-9_-]{24,}/,/AKIA[0-9A-Z]{16}/];
const blocked=/^(?:\.local\/|node_modules\/|dist\/|backend\/target\/|base_de_datos\/datos\/)|(?:^|\/)\.env(?:\.(?!example$).*)?$|\.(?:pem|key|p12)$/i;
let failures=0;
for(const file of files){
 if(blocked.test(file)){console.error('Archivo privado o generado incluido: '+file);failures++;continue;}
 if(file==='scripts/check-tracked-secrets.mjs')continue;
 const data=readFileSync(file);if(data.includes(0))continue;
 if(patterns.some(p=>p.test(data.toString('utf8')))){console.error('Posible secreto en '+file);failures++;}
}
console.log(`${files.length} archivos versionados revisados; ${failures} alertas.`);
if(failures)process.exitCode=1;
