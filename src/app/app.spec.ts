import { provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Login } from './login';
import { validEan } from './core';
import {provideHttpClient} from '@angular/common/http';
import {provideHttpClientTesting} from '@angular/common/http/testing';
import {provideRouter} from '@angular/router';
import {beforeEach, describe, expect, it} from 'vitest';

describe('Validación de acceso y códigos', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Login],
      providers: [provideZonelessChangeDetection(),provideHttpClient(),provideHttpClientTesting(),provideRouter([])]
    }).compileComponents();
  });

  it('conserva los ceros iniciales del DNI', () => {
    const login = TestBed.createComponent(Login).componentInstance;
    login.form.setValue({dni:'00123456',password:'password-largo'});
    expect(login.form.valid).toBe(true);
    expect(login.form.value.dni).toBe('00123456');
  });

  it('bloquea acceso con DNI incompleto o no numérico', () => {
    const fixture = TestBed.createComponent(Login);
    fixture.componentInstance.form.setValue({dni:'1234567a',password:'password-largo'});
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector<HTMLButtonElement>('button[type=submit]')?.disabled).toBe(true);
  });
  it('conserva solo ocho dígitos al escribir o pegar y mantiene el cursor', () => {
    const fixture = TestBed.createComponent(Login);
    fixture.detectChanges();
    const input = fixture.nativeElement.querySelector('#dni') as HTMLInputElement;
    input.value = 'ab00123456789$';
    input.setSelectionRange(5,5);
    input.dispatchEvent(new Event('input',{bubbles:true}));
    expect(input.value).toBe('00123456');
    expect(fixture.componentInstance.form.controls.dni.value).toBe('00123456');
    expect(input.selectionStart).toBe(3);
    expect(fixture.componentInstance.form.controls.dni.valid).toBe(true);
  });
  it('permite mostrar y ocultar la contraseña con un solo botón', () => {
    const fixture = TestBed.createComponent(Login);
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;
    const input = element.querySelector('#password') as HTMLInputElement;
    const toggle = element.querySelector('.password-toggle') as HTMLButtonElement;
    expect(element.querySelectorAll('.password-toggle')).toHaveLength(1);
    expect(toggle.type).toBe('button');
    expect(input.type).toBe('password');
    toggle.click();
    fixture.detectChanges();
    expect(input.type).toBe('text');
    expect(toggle.getAttribute('aria-label')).toBe('Ocultar contraseña');
    toggle.click();
    fixture.detectChanges();
    expect(input.type).toBe('password');
    expect(toggle.getAttribute('aria-label')).toBe('Mostrar contraseña');
  });
  it('valida checksum EAN y rechaza códigos incorrectos',()=>{expect(validEan('7751271000017')).toBe(true);expect(validEan('7751271000016')).toBe(false);expect(validEan('123')).toBe(false);});
  it('admite EAN con ceros iniciales',()=>expect(validEan('0000000000000')).toBe(true));
});
