import http from 'k6/http';
import {check,sleep} from 'k6';
import {SharedArray} from 'k6/data';
import {Counter,Rate,Trend} from 'k6/metrics';
const tokens=new SharedArray('private tokens',()=>JSON.parse(open('/private/tokens.json')));
const successful=new Trend('successful_duration',true);
const failed=new Rate('operation_failed');
const ok=new Counter('responses_200'),limited=new Counter('responses_429'),other=new Counter('responses_other');
export const options={
 scenarios:{alerts:{executor:'constant-vus',vus:Number(__ENV.VUS),duration:'45s',gracefulStop:'5s'}},
 thresholds:{operation_failed:['rate<0.01'],successful_duration:[Number(__ENV.VUS)>=10?'p(95)<1000':'p(95)<500'],checks:['rate>0.99']},
 summaryTrendStats:['avg','min','med','max','p(90)','p(95)','p(99)'],
};
export default function(){
 const response=http.get('http://web/api/alerts?state=ROJO&page=0',{headers:{Authorization:'Bearer '+tokens[(__VU-1)%tokens.length]},tags:{name:'GET critical alerts'},timeout:'10s'});
 let valid=false;
 if(response.status===200){
  ok.add(1);successful.add(response.timings.duration);
  try{const body=response.json();valid=body.page===0&&Array.isArray(body.items)&&body.items.length>0&&body.items.length<=25&&body.items.every(l=>l.state==='ROJO'&&l.total>0&&!('cost' in l));}catch{}
 }else if(response.status===429)limited.add(1);else other.add(1);
 failed.add(!valid);
 check(response,{'HTTP 200 y contenido correcto':()=>valid});
 sleep(1);
}
export function handleSummary(data){return {['/results/'+__ENV.RUN_NAME+'.json']:JSON.stringify(data,null,2)};}
