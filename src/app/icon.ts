import {Component, input} from '@angular/core';

const paths:Record<string,string>={
 brand:'M7 3h10a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2z M12 7h.01 M12 12h.01 M12 17h.01',
 Resumen:'M5 3h4a2 2 0 0 1 2 2v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2z M15 3h4a2 2 0 0 1 2 2v4a2 2 0 0 1-2 2h-4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2z M5 13h4a2 2 0 0 1 2 2v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4a2 2 0 0 1 2-2z M15 13h4a2 2 0 0 1 2 2v4a2 2 0 0 1-2 2h-4a2 2 0 0 1-2-2v-4a2 2 0 0 1 2-2z',
 Alertas:'M12 3a9 9 0 1 0 9 9 9 9 0 0 0-9-9z M12 8v5 M12 16h.01',
 Productos:'M4 8l8-4 8 4v8l-8 4-8-4V8z M4 8l8 4 8-4 M12 12v8 M8 6l8 4v3',
 Lotes:'M5 5h14a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V7a2 2 0 0 1 2-2z M3 10h18 M8 14h4',
 Reportes:'M4 19h16 M7 15v-4 M12 15V5 M17 15V8',
 Personal:'M16 20v-2a4 4 0 0 0-4-4H7a4 4 0 0 0-4 4v2 M9.5 11a3.5 3.5 0 1 0 0-7 3.5 3.5 0 0 0 0 7 M21 20v-2a4 4 0 0 0-3-3.8 M16 4.3a3.5 3.5 0 0 1 0 6.4',
 Auditoría:'M12 3l7 3v5c0 4.8-3 7.5-7 10-4-2.5-7-5.2-7-10V6l7-3z M9 12l2 2 4-4',
 Automatización:'M13 2L4 14h7l-1 8 10-12h-7l1-8z',
 'Base de datos':'M20 6c0 2-4 3-8 3S4 8 4 6s4-3 8-3 8 1 8 3z M4 6v12c0 2 4 3 8 3s8-1 8-3V6 M4 12c0 2 4 3 8 3s8-1 8-3',
 'Mi cuenta':'M12 3a9 9 0 1 0 9 9 9 9 0 0 0-9-9z M8 18v-1a4 4 0 0 1 8 0v1 M12 7a2.5 2.5 0 1 0 0 5 2.5 2.5 0 0 0 0-5z',
 bell:'M18 9a6 6 0 0 0-12 0c0 4-1.1 5.8-2.5 7.5h17C19.1 14.8 18 13 18 9z M10 20a2.2 2.2 0 0 0 4 0',
 plus:'M12 5v14 M5 12h14',
 refresh:'M20 7v5h-5 M4 17v-5h5 M6 6a8 8 0 0 1 14 6 M18 18A8 8 0 0 1 4 12',
 logout:'M9 5H5a1 1 0 0 0-1 1v12a1 1 0 0 0 1 1h4 M9 12h11 M16 8l4 4-4 4',
 menu:'M4 6h16 M4 12h16 M4 18h16',
 close:'M6 6l12 12 M18 6 6 18',
 settings:'M12 8a4 4 0 1 1 0 8 4 4 0 0 1 0-8',
 'arrow-right':'M5 12h14 M13 6l6 6-6 6',
 chevron:'M9 6l6 6-6 6',
 calendar:'M5 5h14a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V7a2 2 0 0 1 2-2z M7 3v4 M17 3v4 M3 10h18 M8 15h2 M14 15h2',
 lock:'M6 11h12a2 2 0 0 1 2 2v6a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2v-6a2 2 0 0 1 2-2z M8 11V7a4 4 0 1 1 8 0v4 M12 15v2',
 eye:'M2 12s3.6-6 10-6 10 6 10 6-3.6 6-10 6S2 12 2 12z M12 9a3 3 0 1 0 0 6 3 3 0 0 0 0-6z',
 'eye-off':'M3 3l18 18 M9.8 6.3A10 10 0 0 1 12 6c6.4 0 10 6 10 6a16 16 0 0 1-2.2 2.8 M6.2 7.9A16 16 0 0 0 2 12s3.6 6 10 6a10 10 0 0 0 4-.8',
 clock:'M12 3a9 9 0 1 0 9 9 9 9 0 0 0-9-9z M12 7v5l3 2',
 scan:'M8 3H5a2 2 0 0 0-2 2v3 M16 3h3a2 2 0 0 1 2 2v3 M3 16v3a2 2 0 0 0 2 2h3 M21 16v3a2 2 0 0 1-2 2h-3 M3 12h18',
 download:'M12 3v12 M7 10l5 5 5-5 M4 17v3h16v-3',
 spark:'M12 2l2.2 6.8L21 11l-6.8 2.2L12 20l-2.2-6.8L3 11l6.8-2.2L12 2z',
};
@Component({
 selector:'mass-icon',standalone:true,
 template:'<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true" focusable="false"><path [attr.d]="path()"/></svg>',
 styles:[':host{display:inline-flex;width:20px;height:20px;flex:none;vertical-align:middle}svg{width:100%;height:100%}'],
})
export class Icon{
 readonly name=input('settings');
 path(){return paths[this.name()]??paths['settings'];}
}
