import {test,expect} from '@playwright/test';
import {readFileSync} from 'node:fs';

test('GPT consulta el inventario desde la aplicación pública',async({page})=>{
 test.skip(process.env['E2E_GPT_LIVE']!=='true','Prueba optativa: genera consumo en la API de OpenAI.');
 const user=JSON.parse(readFileSync(process.env['E2E_CREDENTIALS_PATH']||'.local/demo-user.json','utf8'));
 await page.setViewportSize({width:1440,height:1000});
 await page.goto('/login');
 await page.getByLabel('DNI',{exact:true}).fill(user.dni);
 await page.getByLabel('Contraseña',{exact:true}).fill(user.password);
 await page.getByRole('button',{name:'Ingresar a mi tienda'}).click();
 await expect(page.getByRole('heading',{name:'Resumen',exact:true})).toBeVisible();
 const close=page.getByRole('button',{name:'Cerrar aviso de vencimiento'});
 if(await close.isVisible())await close.click();
 await page.getByRole('button',{name:'Asistente',exact:true}).click();
 const frame=page.frameLocator('iframe[title="Asistente GPT de la tienda"]');
 const prompt=frame.getByPlaceholder('Pregunta por alertas, vencimientos o mermas');
 await expect(prompt).toBeVisible({timeout:45000});
 await prompt.fill('¿Qué lotes requieren atención por vencimiento? Menciona como máximo dos códigos registrados y su fecha. Responde brevemente.');
 await prompt.press('Enter');
 const messages=frame.getByTestId('stChatMessage');
 await expect(messages).toHaveCount(2,{timeout:45000});
 await expect(messages.last()).toContainText(/DEMO|lote|venc/i);
 await expect(frame.getByText('No se pudo consultar GPT.',{exact:false})).toHaveCount(0);
 await page.getByRole('region',{name:'Asistente de consulta'}).screenshot({path:'docs/evidence/railway-gpt-answer.png'});
});
