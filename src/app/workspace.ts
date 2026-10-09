import {Component,ElementRef,ViewChild,inject,signal,OnInit,OnDestroy,HostListener} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormBuilder,ReactiveFormsModule,Validators} from '@angular/forms';
import {HttpErrorResponse} from '@angular/common/http';
import {firstValueFrom} from 'rxjs';
import {Api,Lot,Product,Page,RecordRow,validEan} from './core';
import {Icon} from './icon';
import type {IScannerControls} from '@zxing/browser';
type View='Resumen'|'Alertas'|'Productos'|'Lotes'|'Personal'|'Reportes'|'Auditoría'|'Automatización'|'Mi cuenta'|'Base de datos';
@Component({standalone:true,imports:[CommonModule,ReactiveFormsModule,Icon],templateUrl:'./workspace.html'})
export class Workspace implements OnInit,OnDestroy {
 api=inject(Api);fb=inject(FormBuilder);view=signal<View>('Resumen');menu=signal(false);busy=signal(false);loading=signal(false);error=signal('');notice=signal('');
 products=signal<Product[]>([]);lots=signal<Lot[]>([]);records=signal<RecordRow[]>([]);history=signal<RecordRow[]>([]);states=signal<RecordRow[]>([]);evaluation=signal<RecordRow[]>([]);
 reportSummary=signal<RecordRow[]>([]);
 page=signal(0);hasNext=signal(false);selected=signal<Lot|null>(null);editProduct=signal<number|null>(null);editUser=signal<number|null>(null);chatOpen=signal(false);chat=signal('');scanning=signal(false);
 @ViewChild('actionDialog') dialog!:ElementRef<HTMLDialogElement>;
 @ViewChild('passwordDialog') passwordDialog!:ElementRef<HTMLDialogElement>;
 @ViewChild('video') video?:ElementRef<HTMLVideoElement>;
 catalogProducts=signal<Product[]>([]);catalogQuery=this.fb.nonNullable.control('');catalogPage=0;catalogMore=signal(false);
 reportQuery=this.fb.nonNullable.control('');reportProducts=signal<Product[]>([]);
 unknownBarcode=signal(false);newProductOpen=signal(false);historyIndex=signal(0);historyMore=signal(false);historyLoading=signal(false);
 resetTarget=signal<RecordRow|null>(null);resetForm=this.fb.nonNullable.group({password:['',[Validators.required,Validators.minLength(12),Validators.maxLength(72)]],confirm:['',Validators.required]});
 requests=signal<RecordRow[]>([]);requestTotal=signal(0);requestPage=signal(0);requestMore=signal(false);notificationsOpen=signal(false);private notificationTimer?:ReturnType<typeof setInterval>;
 private historyRevision=0;private loadRevision=0;private catalogRevision=0;private menuOpener?:HTMLElement;
 private controls?:IScannerControls;private cameraGeneration=0;private key=crypto.randomUUID();private receiptKey=crypto.randomUUID();private opener?:HTMLElement;
 readonly filters=this.fb.nonNullable.group({q:'',state:'',exhausted:false});
 readonly productForm=this.fb.nonNullable.group({sku:['',Validators.required],ean:'',name:['',Validators.required],category:['',Validators.required],unit:'UNIDAD',warningDays:[30,Validators.min(1)],criticalDays:[15,Validators.min(0)],active:true});
 readonly lotForm=this.fb.group({productId:[null as number|null,Validators.required],code:['',Validators.required],received:[this.today(),Validators.required],expiry:['',Validators.required],quantity:[1,[Validators.required,Validators.min(0.001)]],cost:[null as number|null]});
 readonly actionForm=this.fb.nonNullable.group({source:'NORMAL',quantity:[1,Validators.required],reason:['',Validators.required],reversalOf:0});
 readonly personForm=this.fb.nonNullable.group({email:['',Validators.email],dni:['',[Validators.required,Validators.pattern(/^[0-9]{8}$/)]],name:['',Validators.required],role:'WORKER',password:'',active:true});
 readonly passwordForm=this.fb.nonNullable.group({current:['',Validators.required],next:['',[Validators.required,Validators.minLength(12),Validators.maxLength(72)]]});
 readonly reportForm=this.fb.nonNullable.group({from:this.today().slice(0,7)+'-01',to:this.today(),type:'',productId:''});
 databaseTables=signal<string[]>([]);databaseTable=signal('usuarios');databaseTotal=signal(0);
 readonly barcode=this.fb.nonNullable.control('');
 admin(){return this.api.user()?.role==='ADMIN';}
 manager(){return ['ADMIN','SUPERVISOR'].includes(this.api.user()?.role??'');}
 nav():View[]{return this.admin()?['Resumen','Alertas','Productos','Lotes','Reportes','Personal','Auditoría','Automatización','Base de datos','Mi cuenta']:this.manager()?['Resumen','Alertas','Productos','Lotes','Reportes','Mi cuenta']:['Resumen','Alertas','Productos','Lotes','Mi cuenta'];}
 today(){return new Intl.DateTimeFormat('en-CA',{timeZone:'America/Lima',year:'numeric',month:'2-digit',day:'2-digit'}).format(new Date());}
 ngOnInit(){if(this.api.user()?.mustChange)this.view.set('Mi cuenta');void this.load();if(this.admin()){void this.loadRequests();this.notificationTimer=setInterval(()=>{if(this.api.online())void this.loadRequests();},3000);}}
 ngOnDestroy(){clearInterval(this.notificationTimer);this.loadRevision++;this.catalogRevision++;this.stopCamera();}
 toggleMenu(open:boolean){if(open)this.menuOpener=document.activeElement as HTMLElement;this.menu.set(open);setTimeout(()=>{if(open)document.querySelector<HTMLButtonElement>('.menu-close')?.focus();else this.menuOpener?.focus();});}
 @HostListener('document:keydown',['$event']) menuKeys(event:KeyboardEvent){if(!this.menu())return;if(event.key==='Escape'){event.preventDefault();this.toggleMenu(false);}if(event.key==='Tab'){const controls=[...document.querySelectorAll<HTMLElement>('.sidebar button:not([disabled]),.sidebar a')].filter(e=>e.getClientRects().length);const first=controls[0],last=controls.at(-1);if(event.shiftKey&&document.activeElement===first){event.preventDefault();last?.focus();}else if(!event.shiftKey&&document.activeElement===last){event.preventDefault();first?.focus();}}}

 async go(view:View,state=''){this.stopCamera();if(this.busy())return;this.view.set(view);this.filters.reset({q:'',state:'',exhausted:false});if(state)this.filters.controls.state.setValue(state);if(view==='Alertas'){this.filters.controls.state.setValue(state);this.filters.controls.exhausted.setValue(false);}if(this.menu())this.toggleMenu(false);this.page.set(0);this.error.set('');this.notice.set('');this.records.set([]);this.lots.set([]);this.hasNext.set(false);await this.load();}
 async load(){
  if(this.api.user()?.mustChange&&this.view()!=='Mi cuenta'){this.view.set('Mi cuenta');return;}
  const stamp=++this.loadRevision;this.loading.set(true);this.error.set('');
  try{
   const v=this.view(),page=this.page();
   if(['Resumen','Alertas','Lotes'].includes(v)){
    const f=this.filters.getRawValue();const result=await this.api.get<Page<Lot>>(`${v==='Alertas'?'/alerts':'/lots'}?q=${encodeURIComponent(f.q)}&state=${f.state}&page=${page}&exhausted=${f.exhausted}`);if(stamp!==this.loadRevision)return;this.lots.set(result.items);this.hasNext.set(result.hasNext);
    if(v==='Resumen'){const d=await this.api.get<{states:RecordRow[];lastEvaluation:RecordRow[]}>('/dashboard');if(stamp!==this.loadRevision)return;this.states.set(d.states);this.evaluation.set(d.lastEvaluation);}
    if(v==='Lotes')await this.searchProducts();
   }else if(v==='Base de datos'){this.databaseTables.set(await this.api.get<string[]>('/admin/database'));const r=await this.api.get<Page<RecordRow>&{total:number}>('/admin/database/'+this.databaseTable()+'?page='+page);if(stamp!==this.loadRevision)return;this.records.set(r.items);this.hasNext.set(r.hasNext);this.databaseTotal.set(r.total);
   }else if(v==='Productos'){const p=await this.api.get<Page<Product>>(`/products?q=${encodeURIComponent(this.filters.controls.q.value)}&page=${page}`);if(stamp!==this.loadRevision)return;this.products.set(p.items);this.hasNext.set(p.hasNext);}
   else if(v==='Automatización'){const r=await this.api.get<Page<RecordRow>>('/admin/outbox/page?page='+page);if(stamp!==this.loadRevision)return;this.records.set(r.items);this.hasNext.set(r.hasNext);}
   else if(v==='Personal'||v==='Auditoría'||v==='Reportes'){
    const path=v==='Personal'?'/admin/users':v==='Auditoría'?'/admin/audit':'/admin/reports';
    const rows=await this.api.get<RecordRow[]>(path+'?page='+page+(v==='Reportes'?'&'+this.reportParams():''));if(stamp!==this.loadRevision)return;this.records.set(rows);this.hasNext.set(rows.length===25);
    if(v==='Reportes'){const summary=await this.api.get<RecordRow[]>('/admin/reports/summary?'+this.reportParams());if(stamp!==this.loadRevision)return;this.reportSummary.set(summary);}
   }
  }catch(e){if(stamp===this.loadRevision)this.error.set(this.api.error(e));}finally{if(stamp===this.loadRevision)this.loading.set(false);}
 }
 count(state:string){return this.states().find(s=>s['state']===state)?.['lots']??0;}
 async filter(){this.page.set(0);await this.load();}
 async paginate(delta:number){this.page.update(p=>p+delta);await this.load();}
 async run(work:()=>Promise<void>){if(this.busy())return;this.busy.set(true);this.error.set('');this.notice.set('');try{await work();}catch(e){this.error.set(this.api.error(e));}finally{this.busy.set(false);}}
 async deleteProduct(p:Product){await this.run(async()=>{if(!confirm('¿Dar de baja el producto? Se conserva su historial.'))return;await firstValueFrom(this.api.http.delete('/api/admin/products/'+p.id));await this.load();this.notice.set('Producto dado de baja.');});}
 async deletePerson(p:RecordRow){await this.run(async()=>{if(!confirm('¿Dar de baja esta cuenta y cerrar sus sesiones?'))return;await firstValueFrom(this.api.http.delete('/api/admin/users/'+p['id']));await this.load();this.notice.set('Cuenta dada de baja.');});}
 async assignSupervisor(p:RecordRow){const value=prompt('ID del supervisor activo (consúltalo en Personal)');if(value===null)return;if(!/^\d+$/.test(value)){this.error.set('ID inválido.');return;}await this.run(async()=>{await this.api.put('/admin/users/'+p['id']+'/supervisor',{supervisorId:Number(value)});await this.load();this.notice.set('Supervisor asignado.');});}
 async saveProduct(){if(this.productForm.invalid)return;await this.run(async()=>{const p=this.productForm.getRawValue();if(p.criticalDays>=p.warningDays)throw new Error('El plazo crítico debe ser menor que el plazo de aviso.');if(p.ean&&!validEan(p.ean))throw new Error('Checksum EAN-13 inválido.');if(this.editProduct())await this.api.put('/admin/products/'+this.editProduct(),p);else await this.api.post('/admin/products',p);this.resetProduct();await this.load();this.notice.set('Producto guardado.');});}
 setProduct(p:Product){this.editProduct.set(p.id);this.productForm.setValue({sku:p.sku,ean:p.ean??'',name:p.name,category:p.category,unit:p.unit,active:p.active,warningDays:p.warningDays,criticalDays:p.criticalDays});}
 resetProduct(){this.newProductOpen.set(false);this.editProduct.set(null);this.productForm.reset({sku:'',ean:'',name:'',category:'',unit:'UNIDAD',active:true,warningDays:30,criticalDays:15});}
 async searchProducts(append=false){const stamp=++this.catalogRevision;const page=append?this.catalogPage+1:0;try{const p=await this.api.get<Page<Product>>('/products?q='+encodeURIComponent(this.catalogQuery.value)+'&page='+page);if(stamp!==this.catalogRevision)return;const chosen=this.catalogProducts().find(p=>p.id===this.lotForm.controls.productId.value);const rows=append?[...this.catalogProducts(),...p.items]:p.items;if(chosen&&!rows.some(p=>p.id===chosen.id))rows.unshift(chosen);this.catalogProducts.set(rows);this.catalogPage=page;this.catalogMore.set(p.hasNext);}catch(e){this.error.set(this.api.error(e));}}
 async searchReportProducts(){await this.run(async()=>{const p=await this.api.get<Page<Product>>('/products?q='+encodeURIComponent(this.reportQuery.value));this.reportProducts.set(p.items);this.reportForm.controls.productId.setValue('');if(p.hasNext)this.notice.set('Se muestran 25 coincidencias; escribe un nombre o SKU más específico.');});}
 async receive(){if(this.lotForm.invalid)return;await this.run(async()=>{const inValue=this.lotForm.getRawValue();const body={...inValue,...(!this.admin()?{cost:null}:{})};if(!window.confirm('¿Confirmar ingreso del lote en la unidad base indicada?'))return;await this.api.post('/lots',body,this.receiptKey);this.receiptKey=crypto.randomUUID();this.lotForm.patchValue({code:'',quantity:1,cost:null});await this.load();this.notice.set('Ingreso guardado.');});}
 newReceiptAttempt(){this.receiptKey=crypto.randomUUID();}
 async lookup(){this.unknownBarcode.set(false);await this.run(async()=>{const code=this.barcode.value.trim();if(!validEan(code))throw new Error('Ingrese un EAN-13 con checksum válido.');let p:Product;try{p=await this.api.get<Product>('/products/barcode/'+code);}catch(e){if(e instanceof HttpErrorResponse&&e.status===404)this.unknownBarcode.set(true);throw e;}if(!p.active)throw new Error('El producto está inactivo.');this.catalogProducts.set([p]);this.newReceiptAttempt();this.lotForm.patchValue({productId:p.id});this.notice.set('Producto encontrado: '+p.name);});}
 async registerUnknown(){const ean=this.barcode.value.trim();this.resetProduct();this.productForm.controls.ean.setValue(ean);this.newProductOpen.set(true);await this.go('Productos');}
 async startCamera(){
  this.error.set('');this.scanning.set(true);const generation=++this.cameraGeneration;
  try{if(!window.isSecureContext)throw new Error('La cámara requiere HTTPS o localhost. Use entrada manual.');const {BrowserMultiFormatReader}=await import('@zxing/browser');await new Promise<void>(resolve=>setTimeout(resolve,0));
   if(generation!==this.cameraGeneration)return;
   const controls=await new BrowserMultiFormatReader().decodeFromConstraints({video:{facingMode:{ideal:'environment'}},audio:false},this.video!.nativeElement,(result,_error,control)=>{if(result&&validEan(result.getText())){control.stop();this.barcode.setValue(result.getText());this.stopCamera();void this.lookup();}});
   if(generation!==this.cameraGeneration)controls.stop();else this.controls=controls;
  }catch{this.stopCamera();this.error.set('No se pudo abrir la cámara. Revise permisos o ingrese el código manualmente.');}
 }
 stopCamera(){this.cameraGeneration++;this.controls?.stop();this.controls=undefined;const stream=this.video?.nativeElement.srcObject;if(stream instanceof MediaStream)stream.getTracks().forEach(t=>t.stop());this.scanning.set(false);}
 async open(l:Lot){this.opener=document.activeElement as HTMLElement;this.selected.set(l);this.key=crypto.randomUUID();this.actionForm.reset({source:l.normal>0?'NORMAL':'PROMO',quantity:1,reason:'',reversalOf:0});this.history.set([]);this.historyIndex.set(0);this.historyMore.set(false);this.error.set('');this.dialog.nativeElement.showModal();await this.loadHistory(0);}
 async loadHistory(page:number){const id=this.selected()?.id;if(!id)return;const revision=++this.historyRevision;this.historyLoading.set(true);try{const r=await this.api.get<Page<RecordRow>>('/lots/'+id+'/history/page?page='+page);if(this.selected()?.id!==id||revision!==this.historyRevision)return;this.history.set(r.items);this.historyIndex.set(page);this.historyMore.set(r.hasNext);}catch(e){this.error.set(this.api.error(e));}finally{if(revision===this.historyRevision)this.historyLoading.set(false);}}
 close(){if(this.busy())return;this.dialog.nativeElement.close();this.selected.set(null);this.opener?.focus();}
 onCancel(event:Event){event.preventDefault();this.close();}
 async act(type:string){if(this.actionForm.invalid||!this.selected())return;await this.run(async()=>{const lot=this.selected()!;const input=this.actionForm.getRawValue();if(!Number.isFinite(input.quantity)||input.quantity===0||(type!=='AJUSTE'&&input.quantity<0))throw new Error('Ingresa una cantidad positiva.');if(lot.unit==='UNIDAD'&&!Number.isInteger(input.quantity))throw new Error('Los productos por unidad requieren cantidades enteras.');if(['VENTA','MERMA','PROMOCION'].includes(type)&&input.quantity>(input.source==='NORMAL'?lot.normal:lot.promo))throw new Error('La cantidad supera el saldo de origen.');if(!window.confirm(`¿Confirmar ${type.toLowerCase()} por la cantidad indicada?`))return;const v=this.actionForm.getRawValue();await this.api.post('/lots/'+this.selected()!.id+'/actions',{...v,type,reversalOf:type==='REVERSO'?v.reversalOf:null},this.key);this.dialog.nativeElement.close();this.selected.set(null);await this.load();this.notice.set('Acción registrada. El saldo restante continúa en seguimiento.');this.opener?.focus();});}
 newActionAttempt(){this.key=crypto.randomUUID();}
 async cost(l:Lot){const cost=window.prompt('Costo histórico por unidad en S/ (hasta 4 decimales)');if(cost===null)return;if(!cost.trim()||!Number.isFinite(Number(cost))||Number(cost)<0){this.error.set('Ingresa un costo válido; vacío no equivale a cero.');return;}const reason=window.prompt('Motivo de valorización');if(!reason)return;await this.run(async()=>{await this.api.put('/admin/lots/'+l.id+'/cost',{cost:Number(cost),reason});await this.load();});}
 async savePerson(){if(this.personForm.invalid)return;await this.run(async()=>{const p=this.personForm.getRawValue();if(!this.editUser()&&(p.password.length<12||new TextEncoder().encode(p.password).length>72))throw new Error('La contraseña temporal requiere al menos 12 caracteres y hasta 72 bytes UTF-8.');const body={...p,password:p.password||null};if(this.editUser())await this.api.put('/admin/users/'+this.editUser(),body);else await this.api.post('/admin/users',body);this.editUser.set(null);this.personForm.reset({email:'',dni:'',name:'',role:'WORKER',password:'',active:true});await this.load();this.notice.set('Personal actualizado. Las sesiones anteriores fueron revocadas.');});}
 setPerson(p:RecordRow){this.editUser.set(Number(p['id']));this.personForm.setValue({email:String(p['email']??''),dni:String(p['dni']),name:String(p['name']),role:String(p['role']),password:'',active:!!p['active']});}
 resetPassword(row:RecordRow){this.opener=document.activeElement as HTMLElement;this.resetTarget.set(row);this.resetForm.reset();this.error.set('');this.passwordDialog.nativeElement.showModal();}
 closePassword(){if(this.busy())return;this.passwordDialog.nativeElement.close();this.resetTarget.set(null);this.resetForm.reset();this.opener?.focus();}
 async saveReset(){if(this.resetForm.invalid||!this.resetTarget())return;await this.run(async()=>{const v=this.resetForm.getRawValue();if(v.password!==v.confirm)throw new Error('Las contraseñas no coinciden.');if(new TextEncoder().encode(v.password).length>72)throw new Error('La contraseña supera 72 bytes UTF-8.');if(!confirm('¿Restablecer la contraseña y cerrar las sesiones de este usuario?'))return;await this.api.post('/admin/users/'+this.resetTarget()!['id']+'/reset',{password:v.password});this.passwordDialog.nativeElement.close();this.resetTarget.set(null);this.resetForm.reset();this.notice.set('Contraseña guardada. El usuario puede ingresar con la clave asignada.');void this.loadRequests();this.opener?.focus();});}
 async retryEvent(row:RecordRow){await this.run(async()=>{if(!confirm('¿Reintentar este evento fallido conservando su identificador?'))return;await this.api.post('/admin/outbox/'+row['id']+'/retry');await this.load();this.notice.set('Evento programado para reintento.');});}
 async loadRequests(){try{const r=await this.api.get<Page<RecordRow>&{total:number}>('/admin/password-requests?page='+this.requestPage());this.requests.set(r.items);this.requestTotal.set(r.total);this.requestMore.set(r.hasNext);}catch{} }
 async requestPassword(){await this.run(async()=>{const r=await this.api.post<{message:string}>('/password-requests');this.notice.set(r.message);});}
 async requestPaginate(delta:number){this.requestPage.update(p=>Math.max(0,p+delta));await this.loadRequests();}
 async changePassword(){if(this.passwordForm.invalid)return;await this.run(async()=>{await this.api.post('/auth/password',this.passwordForm.getRawValue());this.api.clear();await this.api.router.navigateByUrl('/login');});}
 reportParams(){const f=this.reportForm.getRawValue();return new URLSearchParams(Object.fromEntries(Object.entries(f).filter(([,v])=>v!==''))).toString();}
 async download(){await this.run(async()=>{const data=await firstValueFrom(this.api.http.get('/api/admin/reports.csv?'+this.reportParams(),{responseType:'blob'}));const url=URL.createObjectURL(data);const a=document.createElement('a');a.href=url;a.download='movimientos.csv';a.click();URL.revokeObjectURL(url);});}
 async evaluate(){await this.run(async()=>{const r=await this.api.post<{lots:number}>('/admin/evaluation');this.notice.set('Revisión finalizada: '+r.lots+' lotes evaluados.');await this.load();});}
 async ask(query:string){await this.run(async()=>{const r=await this.api.post<{mode:string;data:unknown}>('/admin/chat',{query});this.chat.set(r.mode+'\n\n'+(typeof r.data==='string'?r.data:JSON.stringify(r.data,null,2)));});}
 async toggleChat(){if(this.chatOpen()){this.chatOpen.set(false);return;}await this.run(async()=>{await this.api.post('/chat/session');this.chatOpen.set(true);});}
 async logout(){await this.run(()=>this.api.logout());}
 columns(){return this.records().length?Object.keys(this.records()[0]).filter(k=>!['password','hash'].includes(k)):[];}
 columnName(key:string){return ({supervisor_id:'Supervisor ID',email:'Correo',dni:'DNI',must_change:'Cambio de clave obligatorio',waste_quantity:'Merma neta',id:'ID',name:'Nombre',role:'Rol',active:'Activo',created_at:'Fecha (Lima)',action:'Acción',entity:'Registro',detail:'Detalle',actor:'Responsable',product:'Producto',code:'Lote',unit:'Unidad',type:'Acción',quantity:'Cantidad',reason:'Motivo',loss:'Pérdida S/',valuation:'Valorización',status:'Estado',attempts:'Intentos',next_attempt:'Próximo intento',last_error:'Último error',accepted_at:'Aceptado',delivered_at:'Entregado',alert_id:'Alerta',reverses:'Revierte'} as Record<string,string>)[key]??key;}
 dateTime(value:unknown){if(!value)return 'Sin registro';return new Intl.DateTimeFormat('es-PE',{timeZone:'America/Lima',dateStyle:'short',timeStyle:'short'}).format(new Date(String(value)));}
 cell(key:string,value:unknown){return (key.endsWith('_at')||key==='next_attempt')&&value?this.dateTime(value):this.display(value);}
 display(value:unknown){if(value===null)return 'Pendiente';if(typeof value==='boolean')return value?'Sí':'No';const labels:Record<string,string>={ADMIN:'Programador (administrador)',SUPERVISOR:'Supervisor',WORKER:'Empleado',PENDING:'Pendiente',FAILED:'Fallido',PROCESSING:'En proceso',ACCEPTED:'Aceptado por n8n',OBSOLETE:'Sin vigencia',PROMO:'Promoción',NORMAL:'Normal'};return labels[String(value)]??String(value);}
}
