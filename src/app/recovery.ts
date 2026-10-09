import {Component,inject,signal} from '@angular/core';
import {FormBuilder,ReactiveFormsModule,Validators} from '@angular/forms';
import {RouterLink} from '@angular/router';
import {Api} from './core';

@Component({standalone:true,imports:[ReactiveFormsModule,RouterLink],template:`
 <main class="recovery-page"><section class="panel account"><a routerLink="/login">← Inicio de sesión</a>
 <h1>{{reset?'Crea tu nueva contraseña':'Recupera tu acceso'}}</h1>
 @if(error()){<div class="error" role="alert">{{error()}}</div>}
 @if(message()){<div class="notice" role="status">{{message()}}</div>}
 @if(done()){<button class="primary" routerLink="/login">Ir al inicio de sesión</button>}
 @else if(reset){<p>El enlace es personal, vence en 20 minutos y funciona una sola vez.</p><form [formGroup]="passwordForm" (ngSubmit)="save()">
 <label>Nueva contraseña<input type="password" formControlName="password" autocomplete="new-password" maxlength="72"></label>
 <label>Repite la contraseña<input type="password" formControlName="confirm" autocomplete="new-password" maxlength="72"></label>
 <p class="muted">Entre 12 y 72 caracteres; máximo 72 bytes UTF-8.</p><button class="primary" [disabled]="busy()||passwordForm.invalid||!api.online()">Guardar nueva contraseña</button></form>}
 @else{<p>Escribe el correo registrado por el administrador en tu cuenta de la tienda.</p><form [formGroup]="emailForm" (ngSubmit)="send()">
 <label>Correo electrónico<input type="email" formControlName="email" autocomplete="email" maxlength="254"></label>
 <button class="primary" [disabled]="busy()||emailForm.invalid||!api.online()">Enviar enlace de recuperación</button></form>}
 </section></main>`})
export class Recovery {
 api=inject(Api);fb=inject(FormBuilder);reset=location.pathname==='/restablecer';
 private token=new URLSearchParams(location.hash.slice(1)).get('token')??'';
 error=signal('');message=signal('');busy=signal(false);done=signal(false);
 emailForm=this.fb.nonNullable.group({email:['',[Validators.required,Validators.email,Validators.maxLength(254)]]});
 passwordForm=this.fb.nonNullable.group({password:['',[Validators.required,Validators.minLength(12),Validators.maxLength(72)]],confirm:['',Validators.required]});
 constructor(){if(this.reset){history.replaceState(null,'',location.pathname);if(!/^[A-Za-z0-9_-]{43}$/.test(this.token))this.error.set('El enlace está incompleto. Abre el enlace del correo o solicita uno nuevo.');}}
 async send(){if(this.emailForm.invalid)return;await this.run(async()=>{const r=await this.api.post<{message:string}>('/auth/forgot-password',this.emailForm.getRawValue());this.message.set(r.message);});}
 async save(){if(this.passwordForm.invalid)return;await this.run(async()=>{const v=this.passwordForm.getRawValue();if(v.password!==v.confirm)throw new Error('Las contraseñas no coinciden.');if(new TextEncoder().encode(v.password).length>72)throw new Error('La contraseña supera 72 bytes UTF-8.');const r=await this.api.post<{message:string}>('/auth/reset-password',{token:this.token,password:v.password});this.api.clear();this.token='';this.passwordForm.reset();this.message.set(r.message);this.done.set(true);});}
 async run(action:()=>Promise<void>){if(this.busy())return;this.busy.set(true);this.error.set('');try{await action();}catch(e){this.error.set(this.api.error(e));}finally{this.busy.set(false);}}
}
