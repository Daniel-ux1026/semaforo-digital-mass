import {test,expect,APIRequestContext} from '@playwright/test';
import {readFileSync,existsSync,mkdirSync,writeFileSync} from 'node:fs';
import {randomBytes,randomUUID,createHmac} from 'node:crypto';
const env=Object.fromEntries(readFileSync('infra/.env','utf8').split(/\r?\n/).filter(Boolean).map(l=>{const i=l.indexOf('=');return [l.slice(0,i),l.slice(i+1)];}));
const credentialsPath='.local/demo-user.json';
mkdirSync('.local',{recursive:true});mkdirSync('docs/evidence',{recursive:true});
function credentials(){return existsSync(credentialsPath)?JSON.parse(readFileSync(credentialsPath,'utf8')):{dni:env['BOOTSTRAP_DNI'],password:env['BOOTSTRAP_PASSWORD']};}
async function api(ctx:APIRequestContext,method:string,path:string,body?:unknown,token?:string,key?:string){
 const csrf=await ctx.get('/api/auth/csrf');expect(csrf.ok()).toBeTruthy();const xsrf=(await csrf.json()).token;
 return ctx.fetch('/api'+path,{method,data:body,headers:{'X-XSRF-TOKEN':xsrf,...(token?{Authorization:'Bearer '+token}:{}),...(key?{'Idempotency-Key':key}:{})}});
}
test('recorrido real: acceso, catálogo, lotes, atención y móvil',async({page,request})=>{
 const browserErrors:string[]=[];page.on('pageerror',e=>browserErrors.push(e.message));
 await page.setViewportSize({width:1440,height:1000});await page.goto('/login');
 await expect(page.getByRole('heading',{name:'Inicia sesión'})).toBeVisible();await page.screenshot({path:'docs/evidence/login-desktop.png',fullPage:true});
 const user=credentials();await page.getByLabel('DNI',{exact:true}).fill(user.dni);await page.getByLabel('Contraseña',{exact:true}).fill(user.password);await page.getByRole('button',{name:'Ingresar a mi tienda'}).click();
 await expect(page.getByRole('complementary',{name:'Menú principal'}).getByRole('button',{name:'Cerrar sesión'})).toBeVisible();
 if(await page.getByText('Debes cambiar tu contraseña temporal').isVisible()){
  const password=randomBytes(20).toString('base64url');await page.getByLabel('Contraseña actual').fill(user.password);await page.getByLabel('Nueva contraseña').fill(password);await page.getByRole('button',{name:'Actualizar contraseña'}).click();
  await expect(page.getByRole('heading',{name:'Inicia sesión'})).toBeVisible();user.password=password;writeFileSync(credentialsPath,JSON.stringify(user));await page.getByLabel('DNI',{exact:true}).fill(user.dni);await page.getByLabel('Contraseña',{exact:true}).fill(user.password);await page.getByRole('button',{name:'Ingresar a mi tienda'}).click();
 }
 await expect(page.getByRole('heading',{name:/Tu tienda, bajo control|Administración de tienda/})).toBeVisible();
 // Explicit demonstration records, never presented as corporate inventory.
 const login=await api(request,'POST','/auth/login',user);expect(login.ok()).toBeTruthy();const token=(await login.json()).accessToken;
 const tag=Date.now().toString();const names=['Leche entera DEMO','Yogur natural DEMO','Arroz extra DEMO','Galletas DEMO'];
 for(let i=0;i<4;i++){
  const p=await api(request,'POST','/admin/products',{sku:'DEMO-'+tag+'-'+i,ean:null,name:names[i],category:i<2?'Lácteos':'Abarrotes',unit:'UNIDAD',warningDays:30,criticalDays:15,active:true},token);expect(p.ok()).toBeTruthy();const id=(await p.json()).id;
  const today=new Date();const expiry=new Date(today);expiry.setUTCDate(expiry.getUTCDate()+[45,22,8,-1][i]!);const received=new Date(today);received.setUTCDate(received.getUTCDate()-10);
  const l=await api(request,'POST','/lots',{productId:id,code:'DEMO-'+tag+'-'+i,received:received.toISOString().slice(0,10),expiry:expiry.toISOString().slice(0,10),quantity:20,cost:i===3?null:2.5},token,randomUUID());expect(l.ok()).toBeTruthy();
 }
 await page.getByRole('button',{name:'Actualizar',exact:false}).first().click();await expect(page.getByText('Yogur natural DEMO').first()).toBeVisible();
 await page.screenshot({path:'docs/evidence/dashboard-desktop.png',fullPage:true});
 const row=page.getByRole('row').filter({hasText:'Arroz extra DEMO'}).last();await row.getByRole('button',{name:'Atender'}).click();
 await expect(page.getByRole('dialog')).toBeVisible();expect(await page.locator('.content-shell').evaluate(e=>getComputedStyle(e).filter)).toContain('blur');expect(await page.locator('.sidebar').evaluate(e=>getComputedStyle(e).filter)).toBe('none');
 await page.getByLabel('Cantidad',{exact:true}).fill('10');await page.getByLabel('Observación / motivo').fill('Demostración de promoción parcial');await page.screenshot({path:'docs/evidence/attention-modal.png',fullPage:true});
 page.once('dialog',d=>d.dismiss());await page.getByRole('dialog').getByRole('button',{name:'Promoción',exact:true}).click();await expect(page.getByRole('dialog')).toBeVisible();
 page.once('dialog',d=>d.accept());await page.getByRole('dialog').getByRole('button',{name:'Promoción',exact:true}).click();await expect(page.getByRole('dialog')).not.toBeVisible();await expect(page.getByText('Acción registrada.')).toBeVisible();
 await page.getByRole('button',{name:'Productos',exact:false}).first().click();await expect(page.getByRole('heading',{name:'Catálogo de productos'})).toBeVisible();
 await page.getByRole('button',{name:'Reportes',exact:false}).first().click();await expect(page.getByRole('button',{name:'Exportar CSV'})).toBeVisible();
 await page.getByLabel('Buscar producto por nombre o SKU').fill('Arroz extra DEMO');await page.getByRole('button',{name:'Buscar productos',exact:true}).click();await expect(page.getByLabel('Producto',{exact:true}).locator('option')).not.toHaveCount(1);
 const downloadPromise=page.waitForEvent('download');await page.getByRole('button',{name:'Exportar CSV'}).click();expect((await downloadPromise).suggestedFilename()).toBe('movimientos.csv');
 await page.getByRole('button',{name:'Personal',exact:false}).first().click();await page.getByRole('button',{name:'Restablecer',exact:true}).first().click();await expect(page.getByLabel('Nueva contraseña asignada',{exact:true})).toHaveAttribute('type','password');await expect(page.getByLabel('Confirmar contraseña asignada')).toHaveAttribute('type','password');await page.getByRole('button',{name:'Cerrar restablecimiento'}).click();
 await page.getByRole('button',{name:'Resumen',exact:false}).first().click();await page.setViewportSize({width:390,height:844});
 await expect(page.getByRole('button',{name:'Abrir menú'})).toBeVisible();expect(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBeTruthy();await page.screenshot({path:'docs/evidence/dashboard-mobile.png',fullPage:true});
 await page.getByRole('button',{name:'Abrir menú'}).click();await expect(page.getByRole('button',{name:'✦ Asistente'})).not.toBeVisible();await page.screenshot({path:'docs/evidence/menu-mobile.png',fullPage:true});await page.keyboard.press('Escape');await expect(page.getByRole('button',{name:'Abrir menú'})).toBeFocused();
 await page.getByRole('button',{name:'✦ Asistente'}).click();await page.getByText('Consultas locales sin GPT',{exact:true}).click();await page.getByRole('button',{name:'ayuda',exact:true}).click();await expect(page.locator('.chat-panel pre')).toContainText('estructuradas');
 expect(browserErrors).toEqual([]);
});
test('API: trabajador sin privilegios, sesión revocada y DNI con ceros',async({request,page})=>{
 const user=credentials();const login=await api(request,'POST','/auth/login',user);expect(login.ok()).toBeTruthy();const token=(await login.json()).accessToken;
 const dni='0'+Math.floor(1000000+Math.random()*8999999);const password=randomBytes(18).toString('base64url');
 const created=await api(request,'POST','/admin/users',{dni,name:'Trabajador DEMO',role:'WORKER',password,active:true},token);expect(created.ok()).toBeTruthy();const id=(await created.json()).id;
 const workerLogin=await api(request,'POST','/auth/login',{dni,password});expect(workerLogin.ok()).toBeTruthy();let workerToken=(await workerLogin.json()).accessToken;
 expect((await api(request,'GET','/lots',undefined,workerToken)).status()).toBe(200);
 const next=randomBytes(18).toString('base64url');expect((await api(request,'POST','/auth/password',{current:password,next},workerToken)).status()).toBe(403);expect((await api(request,'POST','/admin/users/'+id+'/reset',{password:next},token)).ok()).toBeTruthy();
 expect((await api(request,'GET','/auth/me',undefined,workerToken)).status()).toBe(401);
 workerToken=(await (await api(request,'POST','/auth/login',{dni,password:next})).json()).accessToken;
 expect((await api(request,'GET','/admin/users',undefined,workerToken)).status()).toBe(403);
 expect((await api(request,'GET','/admin/database',undefined,workerToken)).status()).toBe(403);
 await page.goto('/administracion');await expect(page).toHaveURL(/login/);await page.getByLabel('DNI',{exact:true}).fill(dni);await page.getByLabel('Contraseña',{exact:true}).fill(next);await page.getByRole('button',{name:'Ingresar a mi tienda'}).click();await expect(page).toHaveURL('http://localhost:4200/');await page.goto('/administracion');await expect(page).toHaveURL('http://localhost:4200/');await expect(page.getByRole('button',{name:'Base de datos',exact:true})).not.toBeVisible();
 await page.screenshot({path:'docs/evidence/empleado-desktop.png',fullPage:true});
 await page.getByRole('button',{name:'Lotes',exact:false}).first().click();await page.getByText('＋ Ingresar lote y escanear producto',{exact:true}).click();await page.getByLabel('Buscar producto por nombre',{exact:true}).fill('Arroz');await page.getByRole('button',{name:'Buscar catálogo',exact:true}).click();await expect(page.getByRole('textbox',{name:'Buscar',exact:true})).toHaveValue('');await expect(page.getByLabel('Producto y unidad base').locator('option')).not.toHaveCount(1);
 await page.setViewportSize({width:390,height:844});expect(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBeTruthy();await page.screenshot({path:'docs/evidence/empleado-mobile.png',fullPage:true});
 expect((await api(request,'POST','/admin/chat',{query:'ayuda'},workerToken)).status()).toBe(403);
 const lots=await api(request,'GET','/lots',undefined,workerToken);expect(lots.ok()).toBeTruthy();expect(JSON.stringify(await lots.json())).not.toContain('"cost"');
 expect((await api(request,'PUT','/admin/users/'+id,{dni,name:'Trabajador DEMO',role:'WORKER',password:null,active:false},token)).ok()).toBeTruthy();
 expect((await api(request,'GET','/auth/me',undefined,workerToken)).status()).toBe(401);
 expect((await api(request,'POST','/auth/login',{dni,password:next})).status()).toBe(401);
});
test('escáner manual, código desconocido, permiso denegado y offline',async({page,context,request})=>{
 await page.addInitScript(()=>{navigator.mediaDevices.getUserMedia=async()=>{throw new DOMException('Permiso denegado','NotAllowedError');};});
 const user=credentials();await page.goto('/login');await page.getByLabel('DNI',{exact:true}).fill(user.dni);await page.getByLabel('Contraseña',{exact:true}).fill(user.password);await page.getByRole('button',{name:'Ingresar a mi tienda'}).click();
 await page.getByRole('button',{name:'Lotes',exact:false}).first().click();await page.getByText('＋ Ingresar lote y escanear producto',{exact:true}).click();
 await page.getByLabel('Código EAN-13',{exact:true}).fill('7751271000016');await page.getByRole('button',{name:'Buscar código'}).click();await expect(page.getByRole('alert')).toContainText('checksum');
 await page.getByLabel('Código EAN-13',{exact:true}).fill('9999999999994');await page.getByRole('button',{name:'Buscar código'}).click();await expect(page.getByRole('alert')).toContainText('desconocido');
 const login=await api(request,'POST','/auth/login',user);const token=(await login.json()).accessToken;
 const known=await api(request,'GET','/products/barcode/0000000000000',undefined,token);
 if(known.status()===404){expect((await api(request,'POST','/admin/products',{sku:'EAN-DEMO-'+Date.now(),ean:'0000000000000',name:'Código conocido DEMO',category:'Pruebas',unit:'UNIDAD',warningDays:30,criticalDays:15,active:true},token)).ok()).toBeTruthy();}
 await page.getByLabel('Código EAN-13',{exact:true}).fill('0000000000000');await page.getByRole('button',{name:'Buscar código'}).click();await expect(page.getByText('Producto encontrado: Código conocido DEMO',{exact:true})).toBeVisible();
 await page.getByRole('button',{name:'Abrir cámara'}).click();await expect(page.getByRole('alert')).toContainText('No se pudo abrir la cámara');await expect(page.locator('video')).not.toBeVisible();
 await context.setOffline(true);await expect(page.getByRole('alert').first()).toContainText('Sin conexión');await expect(page.getByRole('button',{name:'Confirmar ingreso'})).toBeDisabled();await context.setOffline(false);
});
test('JWT alterado/vencido, último administrador y límite de login',async({request})=>{
 const user=credentials();const login=await api(request,'POST','/auth/login',user);expect(login.ok()).toBeTruthy();const session=await login.json();
 const parts=session.accessToken.split('.');const payload=JSON.parse(Buffer.from(parts[1],'base64url').toString());payload.exp=Math.floor(Date.now()/1000)-3600;
 const encoded=Buffer.from(JSON.stringify(payload)).toString('base64url');const signature=createHmac('sha256',env['JWT_SECRET']).update(parts[0]+'.'+encoded).digest('base64url');
 expect((await request.get('/api/auth/me',{headers:{Authorization:'Bearer '+parts[0]+'.'+encoded+'.'+signature}})).status()).toBe(401);
 expect((await request.get('/api/auth/me',{headers:{Authorization:'Bearer '+parts[0]+'.'+encoded+'.invalid'}})).status()).toBe(401);
 expect((await api(request,'PUT','/admin/users/'+session.user.id,{dni:user.dni,name:session.user.name,role:'WORKER',password:null,active:false},session.accessToken)).status()).toBe(400);
 const csrf=(await (await request.get('/api/auth/csrf')).json()).token;let limited=false;
 for(let i=0;i<13;i++){const response=await request.post('/api/auth/login',{data:{dni:'00009999',password:'deliberately-invalid'},headers:{'X-XSRF-TOKEN':csrf}});if(response.status()===429){limited=true;break;}expect(response.status()).toBe(401);}
 expect(limited).toBeTruthy();expect((await request.get('/api/dashboard',{headers:{Authorization:'Bearer '+session.accessToken}})).status()).toBe(200);
});
