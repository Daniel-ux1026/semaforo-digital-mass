import {inject, Injectable, signal, DestroyRef} from '@angular/core';
import {HttpClient, HttpErrorResponse, HttpInterceptorFn} from '@angular/common/http';
import {CanActivateFn, Router} from '@angular/router';
import {firstValueFrom, catchError, throwError, from, switchMap} from 'rxjs';
export interface User {id:number;name:string;role:'ADMIN'|'SUPERVISOR'|'WORKER';mustChange:boolean}
export interface Product {id:number;sku:string;ean:string|null;name:string;category:string;unit:string;active:boolean;warningDays:number;criticalDays:number}
export interface Lot {id:number;productId:number;product:string;sku:string;unit:string;code:string;received:string;expiry:string;days:number;state:string;normal:number;promo:number;total:number;cost?:number|null;productActive?:boolean;createdBy?:number;createdAt?:string;attention?:string;attendedAt?:string;attendedBy?:string}
export interface Page<T> {items:T[];page:number;hasNext:boolean}
export interface RecordRow {[key:string]:string|number|boolean|null}
interface Session {accessToken:string;expiresIn:number;user:User}
@Injectable({providedIn:'root'})
export class Api {
 readonly http=inject(HttpClient);readonly router=inject(Router);
 readonly user=signal<User|null>(null);readonly online=signal(navigator.onLine);readonly expired=signal(false);
 readonly connectionIssue=signal(false);
 token='';private tokenUntil=0;private timer:ReturnType<typeof setTimeout>|undefined;private renewal:Promise<boolean>|null=null;
 private channel=typeof BroadcastChannel==='undefined'?null:new BroadcastChannel('mass-auth');
 constructor(){window.addEventListener('online',()=>{this.online.set(true);if(this.user())void this.restore();});window.addEventListener('offline',()=>this.online.set(false));
  if(this.channel)this.channel.onmessage=event=>{if(event.data?.type==='session')this.apply(event.data.session,false);if(event.data?.type==='logout')this.invalidate(false);};
  inject(DestroyRef).onDestroy(()=>{clearTimeout(this.timer);this.channel?.close();});
 }
 get<T>(path:string){return firstValueFrom(this.http.get<T>('/api'+path));}
 async post<T>(path:string,body:unknown={},key?:string){if(!this.online())throw new Error('Sin conexión. No se guardó la operación.');await this.csrf();return firstValueFrom(this.http.post<T>('/api'+path,body,{headers:key?{'Idempotency-Key':key}:{}}));}
 async put<T>(path:string,body:unknown){if(!this.online())throw new Error('Sin conexión.');await this.csrf();return firstValueFrom(this.http.put<T>('/api'+path,body));}
 csrf(){return this.get<{token:string}>('/auth/csrf');}
 async login(dni:string,password:string){this.apply(await this.post<Session>('/auth/login',{dni,password}));}
 apply(session:Session,broadcast=true){this.token=session.accessToken;this.tokenUntil=Date.now()+session.expiresIn*1000;this.user.set(session.user);this.expired.set(false);this.connectionIssue.set(false);clearTimeout(this.timer);this.timer=setTimeout(()=>void this.restore(),Math.max(1000,(session.expiresIn-60)*1000));if(broadcast)this.channel?.postMessage({type:'session',session});}
 restore():Promise<boolean>{
  if(this.renewal)return this.renewal;
  const renew=async()=>{try{this.apply(await this.post<Session>('/auth/refresh'));return true;}catch(e){
   if(e instanceof HttpErrorResponse&&(e.status===401||e.status===403)){this.invalidate();}
   else{this.connectionIssue.set(true);clearTimeout(this.timer);if(this.user())this.timer=setTimeout(()=>void this.restore(),15000);}
   return false;
  }};
  // Web Locks serializes rotating-cookie requests across tabs; no credentials in storage.
  const pending=(async()=>navigator.locks?await navigator.locks.request('mass-refresh',renew):await renew())();
  this.renewal=pending.finally(()=>this.renewal=null);return this.renewal;
 }
 async ensureSession(){if(this.user()&&Date.now()>=this.tokenUntil-15000){if(!await this.restore())throw new Error('No se pudo renovar la sesión. Revisa la conexión e inténtalo nuevamente.');}}
 invalidate(broadcast=true){this.clear(broadcast);this.expired.set(true);void this.router.navigateByUrl('/login');}
 clear(broadcast=true){this.token='';this.tokenUntil=0;this.user.set(null);clearTimeout(this.timer);if(broadcast)this.channel?.postMessage({type:'logout'});}
 async logout(){await this.post('/auth/logout');this.clear();await this.router.navigateByUrl('/login');}
 error(e:unknown){return e instanceof HttpErrorResponse ? (e.error?.message || (e.status===0?'No se pudo conectar con el servidor.':'No se pudo completar la operación.')) : e instanceof Error?e.message:'Error inesperado.';}
}
export const authInterceptor:HttpInterceptorFn=(req,next)=>{
 const api=inject(Api);const publicAuth=['/refresh','/login','/csrf','/forgot-password','/reset-password'].some(path=>req.url.endsWith(path));
 const authenticated=!!api.token&&!publicAuth;
 return from(authenticated?api.ensureSession():Promise.resolve()).pipe(switchMap(()=>next(authenticated?req.clone({setHeaders:{Authorization:'Bearer '+api.token}}):req)),catchError((e:HttpErrorResponse)=>{
  if(e.status===401&&authenticated)api.invalidate();return throwError(()=>e);
 }));
};
export const authGuard:CanActivateFn=async()=>{const api=inject(Api);return api.user()||await api.restore()?true:api.router.parseUrl('/login');};
export function validEan(value:string){if(!/^[0-9]{13}$/.test(value))return false;const sum=[...value.slice(0,12)].reduce((s,n,i)=>s+Number(n)*(i%2?3:1),0);return (10-sum%10)%10===Number(value[12]);}

export const adminGuard:CanActivateFn=async()=>{const api=inject(Api);if(!api.user()&&!await api.restore())return api.router.parseUrl('/login');return api.user()?.role==='ADMIN'?true:api.router.parseUrl('/');};
export const workerHomeGuard:CanActivateFn=async()=>{const api=inject(Api);if(!api.user()&&!await api.restore())return api.router.parseUrl('/login');return api.user()?.role==='ADMIN'?api.router.parseUrl('/administracion'):true;};
