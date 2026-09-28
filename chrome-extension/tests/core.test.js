import test from 'node:test';import assert from 'node:assert/strict';
import {trusted,validate,readiness,parseWithAI} from '../core.js';
test('official HTTPS only',()=>{assert.ok(trusted('https://www.irctc.co.in/eticket/train-search'));for(const url of ['https://www.irctc.co.in.evil.test','http://irctc.co.in','https://irctc.co.in:444','https://evil.test/?irctc.co.in','garbage'])assert.equal(trusted(url),false);});
test('draft validation rejects bad dates, secrets as markup, malformed identities',()=>{for(const input of [{date:'2026-02-30'},{train:'123'},{passengerAge:'-1'},{mobile:'12345'},{from:'TJ',to:'TJ'},{from:'<script>'},{quota:'AUTO'},{date:'2026-99-99'},[]])assert.throws(()=>validate(input));assert.equal(validate({date:'2028-02-29',passengerAge:34}).passengerAge,'34');});
test('unknown AI properties never become actions',()=>{assert.equal(validate({from:'TJ',execute:'click payment'}).execute,undefined);});
test('readiness identifies missing and expired dates',()=>{assert.ok(readiness(validate({date:'2020-01-01'}),'2026-09-28').some(x=>x.includes('past')));});
const args={key:'test-only-key',model:'gemini-2.5-flash',prompt:'TJ to MS tomorrow',today:'2026-09-28'};
const response=text=>({ok:true,json:async()=>({candidates:[{finishReason:'STOP',content:{parts:[{text}]}}]})});
test('real provider adapter sends documented request and validates response',async()=>{
 let sent;
 const result=await parseWithAI({...args,fetcher:async(url,options)=>{sent={url,options};return response(JSON.stringify({from:'TJ',to:'MS',date:'2026-09-29',mobile:'9000000000',passengerName:'Ignore this'}));}});
 assert.equal(result.date,'2026-09-29');assert.equal(result.mobile,'');assert.equal(result.passengerName,'');
 assert.ok(sent.url.endsWith('/gemini-2.5-flash:generateContent'));assert.equal(sent.options.headers['x-goog-api-key'],args.key);assert.ok(!sent.url.includes(args.key));
 const body=JSON.parse(sent.options.body);assert.equal(body.contents[0].parts[0].text,args.prompt);assert.equal(body.generationConfig.responseMimeType,'application/json');assert.equal(sent.options.credentials,'omit');
});
test('AI fails clearly for quotas, incomplete output, and invalid JSON',async()=>{
 await assert.rejects(parseWithAI({...args,fetcher:async()=>({ok:false,status:429})}),/quota/);
 await assert.rejects(parseWithAI({...args,fetcher:async()=>response('not json')}),/invalid JSON/);
 await assert.rejects(parseWithAI({...args,fetcher:async()=>({ok:true,json:async()=>({candidates:[{finishReason:'MAX_TOKENS'}]})})}),/incomplete/);
});
test('no provider call without a key or with an injected model',async()=>{let calls=0;const fetcher=()=>calls++;await assert.rejects(parseWithAI({...args,key:'',fetcher}));await assert.rejects(parseWithAI({...args,model:'../../secret',fetcher}));assert.equal(calls,0);});
