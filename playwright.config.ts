import {defineConfig} from '@playwright/test';
export default defineConfig({testDir:'e2e',workers:1,timeout:90000,use:{baseURL:process.env['E2E_URL']||'http://localhost:4200',channel:'msedge',headless:true,reducedMotion:'reduce',screenshot:'only-on-failure',trace:'off'},reporter:[['list'],['json',{outputFile:'docs/e2e-results.json'}]]});
