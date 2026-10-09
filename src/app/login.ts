import {Component,inject,signal} from '@angular/core';
import {ReactiveFormsModule,FormBuilder,Validators} from '@angular/forms';
import {Api} from './core';
@Component({standalone:true,imports:[ReactiveFormsModule],template:`
 <div class="login-layout">
  <section class="login-brand"><div class="wordmark">SEMÁFORO<span>DIGITAL</span></div><div class="brand-copy"><div class="eyebrow">CONTROL DE INVENTARIO · UNA TIENDA</div><h1>Cada producto<br>cuenta.<br><span>Cada día también.</span></h1><p>Anticípate al vencimiento. Cuida tus productos y toma decisiones a tiempo.</p><div class="traffic-art" aria-hidden="true"><i></i><i></i><i></i></div></div><small>Proyecto académico UTP · Tiendas Mass</small></section>
  <main class="login-form"><div class="login-card"><span class="eyebrow">BIENVENIDO A TU TIENDA</span><h2>Inicia sesión</h2><p class="muted">Ingresa tus credenciales para continuar.</p>
  @if(api.expired()){<div class="notice" role="status">Tu sesión venció. Vuelve a iniciar sesión.</div>}
  @if(!api.online()){<div class="error" role="alert">Sin conexión. Necesitas acceso al servidor para iniciar sesión.</div>}
  @if(error()){<div class="error" role="alert">{{error()}}</div>}
  <form [formGroup]="form" (ngSubmit)="login()"><label for="dni">DNI</label><input id="dni" formControlName="dni" inputmode="numeric" maxlength="8" autocomplete="username" placeholder="Tu DNI de 8 dígitos"><label for="password">Contraseña</label><input id="password" type="password" formControlName="password" autocomplete="current-password" placeholder="Tu contraseña"><button class="primary wide" [disabled]="busy()||form.invalid||!api.online()">{{busy()?'Validando…':'Ingresar a mi tienda →'}}</button></form>
  <p class="login-help">¿Olvidaste tu contraseña?<br><a href="#solicitud" (click)="requestDemo($event)">Solicitar un restablecimiento al administrador</a></p><p class="muted">Solicitud en modo de prueba; no envía mensajes reales.</p><div class="secure-note">Acceso exclusivo para personal autorizado</div></div></main>
 </div>`})
export class Login {
 api=inject(Api);fb=inject(FormBuilder);busy=signal(false);error=signal('');
 form=this.fb.nonNullable.group({dni:['',[Validators.required,Validators.pattern(/^[0-9]{8}$/)]],password:['',[Validators.required,Validators.maxLength(72)]]});
 requestDemo(event:Event){event.preventDefault();window.alert('Mensaje enviado a tu administrador.\n\nModo de prueba: no se ha enviado un mensaje real.');}
 async login(){if(this.form.invalid||this.busy())return;this.busy.set(true);this.error.set('');try{const v=this.form.getRawValue();await this.api.login(v.dni,v.password);await this.api.router.navigateByUrl('/');}catch(e){this.error.set(this.api.error(e));}finally{this.busy.set(false);}}
}
