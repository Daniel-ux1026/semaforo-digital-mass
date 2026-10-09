import {provideZonelessChangeDetection} from '@angular/core';
import {TestBed} from '@angular/core/testing';
import {provideHttpClient,HttpErrorResponse} from '@angular/common/http';
import {provideHttpClientTesting} from '@angular/common/http/testing';
import {provideRouter} from '@angular/router';
import {Api} from './core';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

describe('Recuperación de sesión',()=>{
 let api:Api;
 beforeEach(()=>{TestBed.configureTestingModule({providers:[provideZonelessChangeDetection(),provideHttpClient(),provideHttpClientTesting(),provideRouter([])]});api=TestBed.inject(Api);api.apply({accessToken:'test-memory-only',expiresIn:300,user:{id:1,name:'Admin',role:'ADMIN',mustChange:false}},false);});
 afterEach(()=>{api.clear(false);vi.restoreAllMocks();});
 it('conserva la sesión y avisa ante una interrupción de red',async()=>{
  vi.spyOn(api,'post').mockRejectedValue(new HttpErrorResponse({status:0}));
  expect(await api.restore()).toBe(false);expect(api.user()?.id).toBe(1);expect(api.connectionIssue()).toBe(true);
 });
 it('invalida una sesión rechazada por el servidor',async()=>{
  vi.spyOn(api.router,'navigateByUrl').mockResolvedValue(true);vi.spyOn(api,'post').mockRejectedValue(new HttpErrorResponse({status:401}));
  expect(await api.restore()).toBe(false);expect(api.user()).toBeNull();expect(api.expired()).toBe(true);
 });
 it('comparte una sola renovación entre solicitudes simultáneas de la pestaña',async()=>{
  const post=vi.spyOn(api,'post').mockResolvedValue({accessToken:'renewed',expiresIn:300,user:{id:1,name:'Admin',role:'ADMIN',mustChange:false}});
  expect(await Promise.all([api.restore(),api.restore()])).toEqual([true,true]);expect(post).toHaveBeenCalledTimes(1);
 });
});
