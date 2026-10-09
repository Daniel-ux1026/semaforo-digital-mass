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
    expect(compiled.querySelector('button')?.disabled).toBe(true);
  });
  it('valida checksum EAN y rechaza códigos incorrectos',()=>{expect(validEan('7751271000017')).toBe(true);expect(validEan('7751271000016')).toBe(false);expect(validEan('123')).toBe(false);});
  it('admite EAN con ceros iniciales',()=>expect(validEan('0000000000000')).toBe(true));
});
