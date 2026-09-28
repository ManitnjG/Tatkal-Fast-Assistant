// Serialized by chrome.scripting; keep this function self-contained.
export function pageAssistant(action, payload={}) {
 const u=new URL(location.href);
 if(u.protocol!=='https:' || !['www.irctc.co.in','irctc.co.in'].includes(u.hostname) || (u.port && u.port!=='443')) throw Error('Open an official HTTPS IRCTC page first.');
 const aliases={from:['from','fromstation','journeyfrom','fromstationcode'],to:['to','tostation','journeyto','tostationcode'],date:['journeydate','dateofjourney','departuredate'],train:['trainnumber','trainno'],travelClass:['journeyclass','travelclass','class'],quota:['quota','journeyquota'],mobile:['mobile','mobileno','mobilenumber','contactmobile','passengermobile'],passengerName:['passengername','psgnname','travellername'],passengerAge:['passengerage','psgnage','age']};
 const blocked=/captcha|otp|onetime|verification|password|passwd|pin|cvv|cvc|card|accountnumber|aadhaar|aadhar|wallet|payment|transaction|username|login/i;
 const norm=s=>String(s||'').toLowerCase().replace(/[^a-z0-9]/g,'');
 function describe(el) {
  const parts=['id','name','placeholder','aria-label','autocomplete','formcontrolname','title','type'].map(k=>el.getAttribute(k)||'');
  parts.push(...Array.from(el.labels||[]).map(l=>l.textContent||''));
  const labelled=el.getAttribute('aria-labelledby');
  if(labelled) parts.push(...labelled.split(/\s+/).map(id=>document.getElementById(id)?.textContent||''));
  return parts.map(s=>s.slice(0,200));
 }
 function kind(el) {
  if(el.disabled || el.readOnly || !el.isConnected || !el.getClientRects().length || el.closest('[inert],[aria-hidden="true"]')) return null;
  const style=getComputedStyle(el); if(style.visibility==='hidden' || style.display==='none' || style.opacity==='0') return null;
  if(el.tagName==='INPUT' && !['text','tel','number','date',''].includes(el.type)) return null;
  const tokens=describe(el).map(norm);
  if(tokens.some(s=>blocked.test(s))) return null;
  const keys=Object.keys(aliases).filter(k=>tokens.some(t=>aliases[k].includes(t)));
  return keys.length===1 ? keys[0] : null;
 }
 const key='__tatkalReviewedFillV1';
 if(action==='scan') {
  const els=Array.from(document.querySelectorAll('input,select')).slice(0,500);
  const entries=els.map(el=>({el,field:kind(el)})).filter(x=>x.field);
  const counts={};entries.forEach(x=>counts[x.field]=(counts[x.field]||0)+1);
  const token=crypto.randomUUID();const items=[];
  for(const entry of entries) {
   const {el,field}=entry;
   // Never infer which passenger row a repeated label belongs to.
   if(counts[field]!==1 || String(el.value||'').trim()) continue;
   items.push({el,field,signature:JSON.stringify(describe(el)),id:String(items.length)});
  }
  globalThis[key]={token,items,url:location.href,expires:Date.now()+120000};
  return {token,fields:items.map(({id,field,el})=>({id,field,type:el.tagName==='SELECT'?'select':el.type,options:el.tagName==='SELECT'?Array.from(el.options).map(o=>({value:o.value,label:o.textContent})):undefined})),ambiguous:Object.keys(counts).filter(k=>counts[k]>1)};
 }
 if(action!=='apply') throw Error('Unknown action.');
 const state=globalThis[key]; delete globalThis[key];
 if(!state || payload.token!==state.token || state.expires<Date.now() || state.url!==location.href) throw Error('Preview expired or page changed. Scan again.');
 if(!Array.isArray(payload.fills) || payload.fills.length>9 || new Set(payload.fills.map(f=>f.id)).size!==payload.fills.length) throw Error('Invalid fill request.');
 const operations=payload.fills.map(f=>{
  const item=state.items.find(i=>i.id===f.id);
  if(!item || kind(item.el)!==item.field || JSON.stringify(describe(item.el))!==item.signature || String(item.el.value||'').trim()) throw Error('A field changed since preview. Scan again.');
  if(typeof f.value!=='string' || !f.value || f.value.length>100 || /[<>\x00-\x1f]/.test(f.value)) throw Error('Invalid field value.');
  if(item.el.tagName==='SELECT' && !Array.from(item.el.options).some(o=>o.value===f.value && !o.disabled)) throw Error('Option unavailable.');
  return {...item,value:f.value};
 });
 const results=[];
 for(const {el,field,value} of operations) {
  // Prior input events can cause the application to replace later nodes.
  if(kind(el)!==field || String(el.value||'').trim()) {results.push({field,status:'page changed; skipped'});continue;}
  const proto=el.tagName==='SELECT'?HTMLSelectElement.prototype:HTMLInputElement.prototype;
  Object.getOwnPropertyDescriptor(proto,'value').set.call(el,value);
  el.dispatchEvent(new Event('input',{bubbles:true}));el.dispatchEvent(new Event('change',{bubbles:true}));
  results.push({field,status:el.value===value?'filled — verify on page':'not retained — enter manually'});
 }
 return {results};
}
