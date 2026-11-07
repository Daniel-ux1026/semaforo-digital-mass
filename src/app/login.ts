import {Component, inject, signal} from '@angular/core';
import {FormBuilder, ReactiveFormsModule, Validators} from '@angular/forms';
import {Api} from './core';
import {Icon} from './icon';

@Component({
 standalone:true, imports:[ReactiveFormsModule,Icon], template:`
 <div class="login-layout">
  <aside class="login-brand">
   <div class="wordmark"><span class="brand-symbol"><mass-icon name="brand"/></span><span>Semáforo <b>Digital</b></span></div>
   <div class="brand-copy">
    <h1>Vencimientos<br> bajo control</h1>
    <p>Registra lotes, revisa las fechas y atiende lo más urgente.</p>
    <div class="traffic-art" aria-hidden="true">
     <span><i></i>En orden</span><span><i></i>Próximo</span><span><i></i>Crítico</span>
    </div>
   </div>
  </aside>
  <main class="login-form">
   <div class="login-card">
    <div class="login-mobile-brand"><mass-icon name="brand"/> Semáforo Digital</div>
    <div class="login-emblem" aria-hidden="true"><mass-icon name="lock"/></div>
    <h2>Inicia sesión</h2>
    <p class="muted">Ingresa tu DNI y contraseña.</p>
    @if(api.expired() && !error() && api.online()){
     <div class="notice" role="status">Tu sesión venció. Vuelve a ingresar.</div>
    }
    @if(!api.online()){
     <div class="error" role="alert">Sin conexión. Intenta de nuevo cuando se restablezca.</div>
    } @else if(error()){
     <div class="error" role="alert">{{error()}}</div>
    }
    <form class="access-form" [formGroup]="form" (ngSubmit)="login()">
     <label for="dni">DNI</label>
     <input id="dni" formControlName="dni" inputmode="numeric" maxlength="8" autocomplete="username" placeholder="8 dígitos">
     <label for="password">Contraseña</label>
     <div class="password-field">
      <input id="password" [type]="showPassword() ? 'text' : 'password'" formControlName="password" autocomplete="current-password" maxlength="72" placeholder="Tu contraseña">
      <button type="button" class="icon-button password-toggle" (click)="showPassword.set(!showPassword())" [attr.aria-label]="showPassword() ? 'Ocultar contraseña' : 'Mostrar contraseña'">
       <mass-icon [name]="showPassword() ? 'eye-off' : 'eye'"/>
      </button>
     </div>
     <button type="submit" class="primary wide" [disabled]="busy()||form.invalid||!api.online()">
      {{busy() ? 'Ingresando…' : 'Ingresar a mi tienda'}} <mass-icon name="arrow-right"/>
     </button>
    </form>
   </div>
  </main>
 </div>`
})
export class Login {
 readonly api=inject(Api);
 private readonly fb=inject(FormBuilder);
 readonly busy=signal(false);
 readonly error=signal('');
 readonly showPassword=signal(false);
 readonly form=this.fb.nonNullable.group({
  dni:['',[Validators.required,Validators.pattern(/^[0-9]{8}$/)]],
  password:['',[Validators.required,Validators.maxLength(72)]],
 });
 async login(){
  if(this.form.invalid||this.busy()||!this.api.online())return;
  this.busy.set(true);
  this.error.set('');
  this.showPassword.set(false);
  try{
   const {dni,password}=this.form.getRawValue();
   await this.api.login(dni,password);
   await this.api.router.navigateByUrl('/');
  }catch(e){this.error.set(this.api.error(e));}
  finally{this.busy.set(false);}
 }
}
