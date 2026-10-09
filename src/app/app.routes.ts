import { Routes } from '@angular/router';

import {adminGuard,workerHomeGuard} from './core';
export const routes: Routes = [
 {path:'login',loadComponent:()=>import('./login').then(m=>m.Login)},
 {path:'recuperar',loadComponent:()=>import('./recovery').then(m=>m.Recovery)},
 {path:'restablecer',loadComponent:()=>import('./recovery').then(m=>m.Recovery)},
 {path:'administracion',canActivate:[adminGuard],loadComponent:()=>import('./workspace').then(m=>m.Workspace)},
 {path:'',canActivate:[workerHomeGuard],loadComponent:()=>import('./workspace').then(m=>m.Workspace)},
 {path:'**',redirectTo:''}
];
