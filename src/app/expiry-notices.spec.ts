import {provideZonelessChangeDetection} from '@angular/core';
import {TestBed} from '@angular/core/testing';
import {provideHttpClient} from '@angular/common/http';
import {provideRouter} from '@angular/router';
import {afterEach,beforeEach,describe,expect,it,vi} from 'vitest';
import {Api,Lot} from './core';
import {Workspace} from './workspace';

describe('Avisos de vencimiento',()=>{
 let workspace:Workspace,api:Api;
 const lot=(id:number,state='ROJO')=>({id,state,total:4,productActive:true,product:'Yogur',code:'DEMO-'+id,days:1,expiry:'2026-11-08'} as Lot);
 beforeEach(()=>{
  TestBed.configureTestingModule({providers:[provideZonelessChangeDetection(),provideHttpClient(),provideRouter([])]});
  api=TestBed.inject(Api);workspace=TestBed.runInInjectionContext(()=>new Workspace());vi.spyOn(workspace,'stopCamera').mockImplementation(()=>{});
 });
 afterEach(()=>{workspace.ngOnDestroy();api.clear(false);vi.restoreAllMocks();TestBed.resetTestingModule();});
 it('recorre las páginas y no repite avisos cerrados; avisa ante un cambio de estado',async()=>{
  let expired=false;
  vi.spyOn(api,'get').mockImplementation(async(path:string)=>{
   if(path.includes('VENCIDO'))return {items:expired?[lot(1,'VENCIDO')]:[],hasNext:false};
   if(path.endsWith('page=0'))return {items:expired?[]:[lot(1)],hasNext:!expired};
   return {items:[lot(2)],hasNext:false};
  });
  await workspace.checkCriticalAlerts();
  expect(workspace.criticalNotices().map(l=>l.id)).toEqual([1,2]);
  workspace.criticalNotices.set([]);
  await workspace.checkCriticalAlerts();expect(workspace.criticalNotices()).toEqual([]);
  expired=true;await workspace.checkCriticalAlerts();
  expect(workspace.criticalNotices().map(l=>l.state)).toEqual(['VENCIDO']);
 });
 it('excluye agotados e inactivos y conserva el aviso si falla la consulta',async()=>{
  const get=vi.spyOn(api,'get').mockResolvedValue({items:[lot(1),{...lot(2),total:0},{...lot(3),productActive:false}],hasNext:false});
  await workspace.checkCriticalAlerts();
  expect(workspace.criticalNotices().every(l=>l.id===1)).toBe(true);
  get.mockRejectedValue(new Error('sin conexión'));
  await workspace.checkCriticalAlerts();expect(workspace.criticalNotices().length).toBeGreaterThan(0);
 });
});
