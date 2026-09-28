import test from 'node:test';import assert from 'node:assert/strict';import {pageAssistant} from '../page.js';
// Small DOM fixture, not a claim of live IRCTC compatibility.
class Input {
 constructor(attrs={}){this.attrs=attrs;this.type=attrs.type||'text';this.tagName='INPUT';this.isConnected=true;this.labels=[];this.events=[];this._value='';}
 get value(){return this._value;}set value(v){this._value=v;}
 getAttribute(k){return this.attrs[k]||null;}getClientRects(){return this.hidden?[]:[{}];}closest(){return false;}dispatchEvent(e){this.events.push(e.type);}
}
class Select extends Input {constructor(attrs,options){super(attrs);this.tagName='SELECT';this.options=options;}get value(){return this._value;}set value(v){this._value=v;}}
function setup(inputs,url='https://www.irctc.co.in/eticket/train-search'){
 globalThis.location={href:url};globalThis.document={querySelectorAll:()=>inputs,getElementById:()=>null};globalThis.getComputedStyle=()=>({visibility:'visible',display:'block',opacity:'1'});globalThis.HTMLInputElement=Input;globalThis.HTMLSelectElement=Select;delete globalThis.__tatkalReviewedFillV1;
}
test('preview then apply writes only selected fields and dispatches events',()=>{
 const from=new Input({placeholder:'From *'}),to=new Input({'aria-label':'To'}),captcha=new Input({name:'captcha',placeholder:'Passenger Name'});
 setup([from,to,captcha]);const p=pageAssistant('scan');assert.equal(p.fields.length,2);assert.equal(from.value,'');
 const result=pageAssistant('apply',{token:p.token,fills:[{id:p.fields.find(f=>f.field==='from').id,value:'TJ'}]});
 assert.equal(from.value,'TJ');assert.equal(to.value,'');assert.equal(captcha.value,'');assert.deepEqual(from.events,['input','change']);assert.equal(result.results[0].status,'filled — verify on page');
 assert.throws(()=>pageAssistant('apply',{token:p.token,fills:[]}),/expired/);
});
test('never fill security fields, hidden fields or repeated passenger rows',()=>{
 const nodes=['captcha','OTP','password','UPI PIN','card number','Aadhaar'].map(name=>new Input({name,placeholder:'Passenger Name'}));
 const hidden=new Input({placeholder:'From'});hidden.hidden=true;
 setup([...nodes,hidden,new Input({name:'passengerName'}),new Input({name:'passengerName'})]);const result=pageAssistant('scan');assert.equal(result.fields.length,0);assert.deepEqual(result.ambiguous,['passengerName']);
});
test('changed metadata or values stop the entire pending fill',()=>{
 const from=new Input({name:'from'}),to=new Input({name:'to'});setup([from,to]);const p=pageAssistant('scan');to.value='already chosen';
 assert.throws(()=>pageAssistant('apply',{token:p.token,fills:p.fields.map(f=>({id:f.id,value:'TJ'}))}),/changed/);assert.equal(from.value,'');
});
test('navigation and token substitution are rejected',()=>{
 setup([new Input({name:'from'})]);let p=pageAssistant('scan');assert.throws(()=>pageAssistant('apply',{token:'fake',fills:[]}),/expired/);
 p=pageAssistant('scan');location.href='https://www.irctc.co.in/eticket/payment';assert.throws(()=>pageAssistant('apply',{token:p.token,fills:[]}),/changed/);
});
test('origin checks apply again at execution',()=>{setup([],'https://irctc.co.in.evil.test/');assert.throws(()=>pageAssistant('scan'),/official/);});
test('native select accepts only available options',()=>{
 const quota=new Select({name:'quota'},[{value:'TQ',textContent:'TATKAL'}]);setup([quota]);let p=pageAssistant('scan');assert.throws(()=>pageAssistant('apply',{token:p.token,fills:[{id:'0',value:'EVIL'}]}),/unavailable/);
 p=pageAssistant('scan');pageAssistant('apply',{token:p.token,fills:[{id:'0',value:'TQ'}]});assert.equal(quota.value,'TQ');
});
test('non-empty fields are not offered',()=>{const from=new Input({name:'from'});from.value='TJ';setup([from]);assert.equal(pageAssistant('scan').fields.length,0);});
test('expired previews and duplicate operations fail closed',()=>{setup([new Input({name:'from'})]);let p=pageAssistant('scan');globalThis.__tatkalReviewedFillV1.expires=0;assert.throws(()=>pageAssistant('apply',{token:p.token,fills:[]}),/expired/);p=pageAssistant('scan');assert.throws(()=>pageAssistant('apply',{token:p.token,fills:[{id:'0',value:'TJ'},{id:'0',value:'MS'}]}),/Invalid/);});
