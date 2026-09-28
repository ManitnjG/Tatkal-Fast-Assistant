import {FIELDS,validate,trusted,readiness,parseWithAI} from './core.js';
import {pageAssistant} from './page.js';
const $=id=>document.getElementById(id);
const today=()=>new Intl.DateTimeFormat('en-CA',{timeZone:'Asia/Kolkata',year:'numeric',month:'2-digit',day:'2-digit'}).format(new Date());
let preview=null;
const status=text=>{$('status').textContent=text;};
const raw=()=>Object.fromEntries(FIELDS.map(k=>[k,$('draft').elements.namedItem(k).value]));
function invalidate(){preview=null;$('preview').replaceChildren();$('apply').disabled=true;}
function showDraft(d){FIELDS.forEach(k=>$('draft').elements.namedItem(k).value=d[k]||'');invalidate();showReadiness();}
function showReadiness(){
 $('readiness').replaceChildren();
 let notes;try{notes=readiness(validate(raw()),today());}catch(e){notes=[e.message];}
 for(const note of notes){const li=document.createElement('li');li.textContent=note;$('readiness').append(li);}
}
async function rememberDraft(){await chrome.storage.session.set({draft:raw()});}
async function connection(){const s=await chrome.storage.session.get(['key','model']);$('connection').textContent=s.key?'AI configured • connection tested when you create a draft':'AI not configured • manual mode available';$('model').value=s.model||'gemini-2.5-flash';}
function action(id,fn){$(id).addEventListener('click',async()=>{const b=$(id);b.disabled=true;try{await fn();}catch(e){status(e.name==='TimeoutError'?'AI timed out. No automatic retry; please try again.':e.message||'Action failed.');}finally{b.disabled=id==='apply'?!preview:false;}});}
$('draft').addEventListener('submit',e=>e.preventDefault());
$('draft').addEventListener('input',()=>{invalidate();showReadiness();rememberDraft().catch(e=>status(e.message));});
action('connect',async()=>{
 const key=$('key').value.trim();if(!key)throw Error('Enter a Gemini key.');
 const model=$('model').value.trim();if(!/^gemini-[a-z0-9.-]{1,70}$/.test(model))throw Error('Enter a valid Gemini model ID.');
 if(!await chrome.permissions.request({origins:['https://generativelanguage.googleapis.com/*']}))throw Error('AI permission was not granted. Manual mode still works.');
 await chrome.storage.session.set({key,model});$('key').value='';await connection();status('AI configured. Enter journey instructions and consent to send them.');
});
action('forgetKey',async()=>{await chrome.storage.session.remove(['key','model']);await chrome.permissions.remove({origins:['https://generativelanguage.googleapis.com/*']});$('key').value='';await connection();status('API key forgotten and AI site permission removed.');});
action('parse',async()=>{
 if(!$('consent').checked)throw Error('Confirm sending only the journey text to Gemini.');
 const {key,model}=await chrome.storage.session.get(['key','model']);
 const before=JSON.stringify(raw());const prompt=$('prompt').value;
 status('Asking Gemini to extract your journey…');
 const result=await parseWithAI({key,model:model||'gemini-2.5-flash',prompt,today:today()});
 if(before!==JSON.stringify(raw()) || prompt!==$('prompt').value)throw Error('You edited the draft while AI was running. Result discarded to preserve your changes.');
 const previous=raw();for(const k of ['mobile','passengerName','passengerAge'])result[k]=previous[k];
 showDraft(result);await rememberDraft();status('AI draft ready. Verify every value, especially dates and station names. Nothing has been filled on IRCTC.');
});
action('save',async()=>{const saved=validate(raw());await chrome.storage.local.set({saved});status('Saved on this Chrome profile. Passenger details are not encrypted.');});
action('load',async()=>{const {saved}=await chrome.storage.local.get('saved');if(!saved)throw Error('No saved draft.');showDraft(validate(saved));await rememberDraft();status('Saved draft loaded. Check the journey date.');});
action('clear',async()=>{await chrome.storage.local.remove('saved');await chrome.storage.session.remove('draft');showDraft({});$('prompt').value='';$('consent').checked=false;status('Saved details and current draft cleared.');});
action('open',async()=>{await chrome.tabs.create({url:'https://www.irctc.co.in/eticket/train-search'});});
action('scan',async()=>{
 invalidate();const draft=validate(raw());
 if(draft.date && draft.date<today())throw Error('Journey date is in the past. Update the draft before filling.');
 const [tab]=await chrome.tabs.query({active:true,currentWindow:true});
 if(!tab?.id || !trusted(tab.url))throw Error('Open the official IRCTC page, then reopen this extension there.');
 const [injection]=await chrome.scripting.executeScript({target:{tabId:tab.id},func:pageAssistant,args:['scan']});
 const result=injection?.result;if(!result)throw Error('Could not inspect this page.');
 const fills=[];
 for(const f of result.fields){
  let value=draft[f.field];if(!value)continue;
  // Text date widgets have site-specific formats; leave these manual.
  if(f.field==='date' && f.type!=='date')continue;
  if(f.type==='select'){
   const options=f.options.filter(o=>o.value===value || o.label.trim().toUpperCase()===value.toUpperCase());
   if(options.length!==1)continue;value=options[0].value;
  }
  const label=document.createElement('label');label.className='check';const check=document.createElement('input');check.type='checkbox';check.checked=true;
  const span=document.createElement('span');span.textContent=`${f.field}: ${draft[f.field]}`;label.append(check,span);$('preview').append(label);
  fills.push({id:f.id,value,check});
 }
 if(!fills.length){status('No supported empty fields match your draft. Fields may already contain values, use custom widgets, or have ambiguous labels. Enter these manually.');return;}
 preview={tabId:tab.id,token:result.token,fills,snapshot:JSON.stringify(raw())};$('apply').disabled=false;
 status(`${fills.length} field(s) ready for review. ${result.ambiguous.length?'Repeated fields skipped: '+result.ambiguous.join(', ')+'.':''} This does not select station suggestions or submit the form.`);
});
action('apply',async()=>{
 const p=preview;if(!p || p.snapshot!==JSON.stringify(raw()))throw Error('Draft changed. Preview again.');
 const fills=p.fills.filter(f=>f.check.checked).map(({id,value})=>({id,value}));if(!fills.length)throw Error('Select at least one field.');
 const [tab]=await chrome.tabs.query({active:true,currentWindow:true});if(tab?.id!==p.tabId || !trusted(tab.url))throw Error('Active page changed. Preview again.');
 invalidate();const [result]=await chrome.scripting.executeScript({target:{tabId:p.tabId},func:pageAssistant,args:['apply',{token:p.token,fills}]});
 if(!result?.result?.results)throw Error('The page could not apply the fill. Preview again or enter manually.');
 status(result.result.results.map(r=>`${r.field}: ${r.status}`).join('\n')+'\nVerify on IRCTC. Select station suggestions manually.');
});
await chrome.storage.local.setAccessLevel({accessLevel:'TRUSTED_CONTEXTS'});
await connection();const {draft}=await chrome.storage.session.get('draft');if(draft)showDraft(validate(draft));else showReadiness();
