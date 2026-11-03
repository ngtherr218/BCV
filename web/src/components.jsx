import React, { useEffect, useRef, useState } from 'react';
import { AlertCircle, CheckCircle2, X, LoaderCircle, ImagePlus, Upload, Search, Check, ImageOff, ExternalLink } from 'lucide-react';
import { api, number, date } from './api';
import { catalog, statusLabels } from './catalog';
import { useAnimatedDismiss } from './motion';
import CoordinatePicker from './CoordinatePicker';
import { matchPinRegion } from './pinRegion';
import PlaceFeatures from './PlaceFeatures';
import { suggestedFeatures, changePlaceType } from './placeFeatureDefaults';

export function Badge({ value }) { return <span className={`badge badge-${value?.toLowerCase()}`}><i/>{statusLabels[value] || value || '—'}</span>; }
export function ErrorBox({ message, retry }) { return <div className="error-box" role="alert"><AlertCircle size={20}/><div>{message}</div>{retry && <button className="button secondary small" onClick={retry}>Thử lại</button>}</div>; }
export function Loading() { return <div className="loading" role="status"><LoaderCircle className="spin" size={24}/> Đang tải dữ liệu…</div>; }
export function Empty({ title = 'Chưa có dữ liệu', text = 'Thử thay đổi từ khóa hoặc điều kiện lọc.' }) { return <div className="empty"><Search size={30}/><h3>{title}</h3><p>{text}</p></div>; }
export function Thumbnail({ src, name }) { const [failed,setFailed] = useState(false); useEffect(() => setFailed(false),[src]); return src && !failed ? <img className="thumbnail" src={src} alt={name || ''} loading="lazy" onError={() => setFailed(true)}/> : <span className="thumbnail placeholder"><ImageOff size={18}/></span>; }
export function Toast({ message, close }) { useEffect(() => { const timer=setTimeout(close,4500); return () => clearTimeout(timer); },[message,close]); return message && <div className="toast" role="status"><CheckCircle2 size={20}/>{message}<button className="icon-button" onClick={close} aria-label="Đóng thông báo"><X size={16}/></button></div>; }

// Storage folder on the server: place photos and card art are kept apart (places/, cards/).
const uploadPurpose = { places: 'place', cards: 'card' };

async function uploadImage(file, purpose) {
  if (!['image/jpeg','image/png','image/webp'].includes(file.type)) throw new Error('Chọn ảnh JPEG, PNG hoặc WebP.');
  if (file.size > 12 * 1024 * 1024) throw new Error('Ảnh gốc không được quá 12 MB.');
  const bitmap = await createImageBitmap(file);
  const scale = Math.min(1,1600 / Math.max(bitmap.width,bitmap.height));
  const canvas = document.createElement('canvas'); canvas.width=Math.round(bitmap.width*scale); canvas.height=Math.round(bitmap.height*scale);
  const context=canvas.getContext('2d'); context.fillStyle='#fff'; context.fillRect(0,0,canvas.width,canvas.height); context.drawImage(bitmap,0,0,canvas.width,canvas.height); bitmap.close();
  const blob = await new Promise(resolve => canvas.toBlob(resolve,'image/jpeg',0.85));
  if (!blob) throw new Error('Không thể xử lý ảnh này.');
  return (await api(`/images?purpose=${purpose}`,{method:'POST',headers:{'Content-Type':'image/jpeg'},body:blob})).url;
}

function ImagesField({ field, value, change, purpose }) {
  const multiple=field.type==='images'; const images=multiple ? value || [] : value ? [value] : [];
  const [url,setUrl]=useState(''); const [busy,setBusy]=useState(false); const [error,setError]=useState('');
  const add = next => change(multiple ? [...images,...next].filter((v,i,a) => a.indexOf(v)===i).slice(0,30) : next[0] || '');
  async function upload(event) { setBusy(true); setError(''); try { const next=[]; for(const file of event.target.files) next.push(await uploadImage(file, purpose)); add(next); } catch(e){setError(e.message);} finally {setBusy(false);event.target.value='';} }
  return <div className="images-field"><div className="image-previews">{images.map((image,index) => <div className="image-preview" key={index}><img src={image} alt={`Ảnh ${index+1}`} onError={e => { e.currentTarget.style.opacity='.25'; }}/><button type="button" className="image-remove" aria-label={`Xóa ảnh ${index+1}`} onClick={() => change(multiple ? images.filter((_,i)=>i!==index) : '')}><X size={14}/></button></div>)}</div>
    <div className="url-input"><input aria-label="URL ảnh HTTPS" type="url" placeholder="Dán đường dẫn ảnh https://…" value={url} onChange={e=>setUrl(e.target.value)}/><button type="button" className="button secondary" disabled={busy || !url.trim()} onClick={()=>{ if(!/^https:\/\//i.test(url.trim())) {setError('Đường dẫn phải bắt đầu bằng https://');return;} add([url.trim()]);setUrl('');setError(''); }}>Thêm ảnh</button></div>
    <label className={`upload-button ${busy?'disabled':''}`}><input type="file" accept="image/jpeg,image/png,image/webp" multiple={multiple} disabled={busy} onChange={upload}/>{busy?<LoaderCircle className="spin" size={18}/>:<Upload size={18}/>} {busy?'Đang tải ảnh…':'Hoặc tải ảnh từ máy tính'}</label><small>JPEG, PNG, WebP · tối đa 12 MB/ảnh gốc · tự tối ưu trước khi tải lên.</small>{error&&<p className="field-error" role="alert">{error}</p>}</div>;
}

function CardPicker({ value = [], change, cards = [] }) {
  const [q,setQ]=useState(''); const filtered=cards.filter(c=>c.name.toLocaleLowerCase('vi').includes(q.toLocaleLowerCase('vi')));
  return <div className="card-picker"><div className="picker-head"><span>{number(value.length)} thẻ đã chọn</span><button className="text-button" type="button" onClick={()=>change([])}>Bỏ chọn tất cả</button></div><div className="search-box"><Search size={16}/><input value={q} onChange={e=>setQ(e.target.value)} placeholder="Tìm thẻ trong danh sách…" aria-label="Tìm thẻ"/></div><div className="picker-list">{filtered.map(card=><label key={card.id}><input type="checkbox" checked={value.includes(card.id)} onChange={e=>change(e.target.checked?[...value,card.id]:value.filter(id=>id!==card.id))}/><span>{card.name}</span><Badge value={card.status}/></label>)}{!filtered.length&&<p>Không tìm thấy thẻ phù hợp.</p>}</div></div>;
}

export function Drawer({ resource, recordId, initial, lookups, close, saved }) {
  const { closing, dismiss }=useAnimatedDismiss(close);
  const config=catalog[resource]; const [data,setData]=useState(null); const [loading,setLoading]=useState(true); const [busy,setBusy]=useState(false); const [error,setError]=useState(''); const [confirm,setConfirm]=useState(false);
  const panel=useRef(null); const original=useRef(null); const [coordinateOpen,setCoordinateOpen]=useState(false); const [preferRegion,setPreferRegion]=useState(false);
  const [coordinateRequest,setCoordinateRequest]=useState(null); const [coordinateBusy,setCoordinateBusy]=useState(false); const [coordinateMessage,setCoordinateMessage]=useState('');
  useEffect(()=>{
    if(!coordinateRequest)return;
    let alive=true;
    const timer=setTimeout(()=>{
      api(`/map/region?latitude=${coordinateRequest.latitude}&longitude=${coordinateRequest.longitude}`).then(address=>{
        if(!alive)return;
        const region=matchPinRegion(address,lookups.regions||[]);
        if(region)setData(previous=>({...previous,region_id:region.id}));
        setCoordinateMessage(region?`Khu vực: ${region.name}`:'Không tìm thấy khu vực tương ứng. Hãy chọn khu vực trong biểu mẫu.');
      }).catch(error=>{if(alive)setCoordinateMessage(error.message);}).finally(()=>{if(alive)setCoordinateBusy(false);});
    },350);
    return()=>{alive=false;clearTimeout(timer);};
  },[coordinateRequest,lookups.regions]);
  function chooseCoordinates(point){
    setPreferRegion(false);setError('');setConfirm(false);setCoordinateMessage('');
    setData(previous=>({...previous,...point,region_id:''}));
    const valid=point.latitude!==''&&point.longitude!==''&&point.latitude!=null&&point.longitude!=null&&
      Number.isFinite(Number(point.latitude))&&Number.isFinite(Number(point.longitude))&&
      Math.abs(Number(point.latitude))<=90&&Math.abs(Number(point.longitude))<=180;
    setCoordinateBusy(valid);setCoordinateRequest(valid?point:null);
  }
  useEffect(()=>{let alive=true;setLoading(true);setError('');(async()=>{try{ const record=recordId ? await api(`/data/${resource}/${recordId}`) : Object.fromEntries(config.fields.map(f=>[f.key,f.default??(f.type==='images'||f.type==='cards'?[]:'')])); if(alive){ const next={...record,...initial}; setData(next);original.current=record; }}catch(e){if(alive)setError(e.message);}finally{if(alive)setLoading(false);}})();return()=>{alive=false;};},[resource,recordId]);
  useEffect(()=>{ const previous=document.activeElement; const old=document.body.style.overflow;document.body.style.overflow='hidden';panel.current?.focus();function key(e){if(e.key==='Escape'&&!busy)dismiss();if(e.key==='Tab'){const items=panel.current?.querySelectorAll('button:not(:disabled),input:not(:disabled),select:not(:disabled),textarea:not(:disabled),a[href]');if(!items?.length)return;const first=items[0],last=items[items.length-1];if(e.shiftKey&&document.activeElement===first){e.preventDefault();last.focus();}else if(!e.shiftKey&&document.activeElement===last){e.preventDefault();first.focus();}}}document.addEventListener('keydown',key);return()=>{document.body.style.overflow=old;document.removeEventListener('keydown',key);previous?.focus();};},[dismiss,busy]);
  useEffect(()=>{if(confirm){const box=panel.current?.querySelector('.confirm-box');box?.scrollIntoView({block:'nearest'});box?.querySelector('button')?.focus();}else if(error){panel.current?.querySelector('[role="alert"]')?.scrollIntoView({block:'nearest'});}},[confirm,error]);
  function change(key,value){
    if(resource==='places'&&key==='region_id'){
      setPreferRegion(true);setCoordinateRequest(null);setCoordinateBusy(false);setCoordinateMessage('');
    }
    if(resource==='places'&&['latitude','longitude'].includes(key)){
      chooseCoordinates({latitude:data.latitude,longitude:data.longitude,[key]:value});return;
    }
    if(resource==='places'&&key==='place_type_id'){
      const nextAutomatic=suggestedFeatures(value,lookups.types,lookups.features);
      setData(previous=>changePlaceType(previous,value,nextAutomatic));
    }else{setData(previous=>({...previous,[key]:value,...(key==='quest_type'?{cycle_type:value}:{})}));}
    setError('');setConfirm(false);
  }
  async function save(event){event?.preventDefault(); if(coordinateBusy){setError('Đang xác định khu vực theo tọa độ. Vui lòng chờ.');return;} if(resource==='places'&&!data.feature_codes?.length){setError('Hãy chọn ít nhất một đặc trưng nội dung cho địa danh.');return;} if(config.fields.some(f=>f.key==='status') && data.status!==original.current.status && !confirm){setConfirm(true);return;}setBusy(true);setError('');try{ const payload={...data};config.fields.filter(f=>f.type==='number'||f.type==='reference').forEach(f=>{payload[f.key]=payload[f.key]===''||payload[f.key]==null?null:Number(payload[f.key]);});await api(`/data/${resource}${recordId?`/${recordId}`:''}`,{method:recordId?'PUT':'POST',body:JSON.stringify(payload)});saved(recordId?'Đã lưu thay đổi.':`Đã tạo ${config.singular} mới.`);}catch(e){setError(e.message);}finally{setBusy(false);}}
  return <div className={`overlay ${closing?'is-closing':''}`} onMouseDown={e=>{if(e.target===e.currentTarget&&!busy)dismiss();}}><section className="drawer" role="dialog" aria-modal="true" aria-labelledby="drawer-title" tabIndex={-1} ref={panel}><div className="drawer-heading"><div><span className="eyebrow">{config.section}</span><h2 id="drawer-title">{recordId ? resource==='users'?'Thông tin người dùng':`Chỉnh sửa ${config.singular}`:`Thêm ${config.singular}`}</h2></div><button className="icon-button" onClick={dismiss} disabled={busy} aria-label="Đóng"><X/></button></div>
    {loading?<Loading/>:!data?<div className="drawer-body"><ErrorBox message={error}/></div>:<form onSubmit={save} className="drawer-form"><div className="drawer-body">
      {resource==='users'&&<><div className="user-detail"><span className="avatar large">{data.full_name?.[0]||'U'}</span><div><h3>{data.full_name}</h3><p>{data.email}</p><small>Tham gia {date(data.created_at)}</small></div></div><div className="mini-metrics">{[['journey_count','Hành trình'],['discovered_count','Đã khám phá'],['card_count','Thẻ sở hữu'],['total_steps','Bước chân']].map(([key,label])=><div key={key}><strong>{number(data[key])}</strong><span>{label}</span></div>)}</div>{data.role==='ADMIN'&&<p className="form-hint">Tài khoản quản trị: không thay đổi trạng thái tại chức năng quản lý người dùng.</p>}</>}
      {resource==='quests'&&recordId&&<p className="form-hint">Nếu nhiệm vụ đã phát sinh tiến độ, hệ thống chỉ cho sửa tên và trạng thái. Tạo nhiệm vụ mới khi cần đổi điều kiện hoặc phần thưởng.</p>}
      {resource==='items'&&<p className="form-hint">{data.item_type==='SPEED_SHOES'?'Giày tăng tốc: hệ số phải lớn hơn 1, số bước hiệu lực lớn hơn 0.':'Vé máy bay: không áp dụng hệ số hoặc phạm vi số bước.'} Cấu hình mới áp dụng cho các lần sử dụng sau.</p>}

      <div className="form-grid">{config.fields.filter(f=>!(resource==='items'&&data.item_type==='FLIGHT_TICKET'&&['effect_value','effective_range'].includes(f.key))).map(f=><React.Fragment key={f.key}><div className={`form-field ${f.wide?'wide':''}`} key={f.key}><label htmlFor={`field-${f.key}`}>{f.label}{f.required&&<span className="required"> *</span>}</label>
      {['images','image'].includes(f.type)?<ImagesField field={f} value={data[f.key]} change={value=>change(f.key,value)} purpose={uploadPurpose[resource]}/>:f.type==='cards'?<CardPicker value={data[f.key]} change={value=>change(f.key,value)} cards={lookups.cards}/>:f.type==='textarea'?<textarea id={`field-${f.key}`} rows={4} maxLength={f.maxLength} value={data[f.key]||''} onChange={e=>change(f.key,e.target.value)}/>:['select','reference'].includes(f.type)?<select id={`field-${f.key}`} required={f.required} value={data[f.key]??''} disabled={(resource==='users'&&data.role==='ADMIN')||(resource==='items'&&!!recordId&&f.key==='item_type')} onChange={e=>change(f.key,e.target.value)}><option value="">{f.required?'Chọn '+f.label.toLowerCase():'Không có'}</option>{(f.type==='reference'?(lookups[f.lookup]||[]).map(v=>[v.id,f.lookup==='regions'?`#${v.id} · ${v.name}`:v.name]):f.options).map(([value,label])=><option key={value} value={value}>{label}</option>)}</select>:<input id={`field-${f.key}`} type={f.type} required={f.required} min={f.min} max={f.max??(f.type==='number'?2147483647:undefined)} step={f.step||1} maxLength={f.maxLength} value={data[f.key]??''} onFocus={resource==='places'&&['latitude','longitude'].includes(f.key)?()=>setCoordinateOpen(true):undefined} onClick={resource==='places'&&['latitude','longitude'].includes(f.key)?()=>setCoordinateOpen(true):undefined} aria-expanded={resource==='places'&&['latitude','longitude'].includes(f.key)?coordinateOpen:undefined} aria-controls={resource==='places'&&['latitude','longitude'].includes(f.key)?'place-coordinate-map':undefined} onChange={e=>change(f.key,e.target.value)}/>}
      </div>{resource==='places'&&f.key==='longitude'&&coordinateOpen&&<div className="form-field wide" id="place-coordinate-map"><div className="coordinate-picker-toolbar"><strong>Chọn tọa độ</strong><button className="button secondary small" type="button" onClick={()=>setCoordinateOpen(false)}>Xong / Đóng bản đồ</button></div><CoordinatePicker latitude={data.latitude} longitude={data.longitude} regionId={data.region_id} places={lookups.places} preferRegion={preferRegion} change={chooseCoordinates}/></div>}</React.Fragment>)}</div>{resource==='places'&&<PlaceFeatures typeId={data.place_type_id} value={data.feature_codes||[]} options={lookups.features||[]} change={value=>change('feature_codes',value)}/>} {resource==='places'&&<p className="form-hint">Khi kích hoạt, khu vực liên kết phải đang hoạt động. Đặc trưng và cụm gợi ý được lưu cùng địa danh.</p>}
      {resource==='places'&&(coordinateBusy||coordinateMessage)&&<p className="form-hint" role="status" aria-live="polite">{coordinateBusy?'Đang xác định khu vực theo tọa độ…':coordinateMessage}</p>}{error&&<ErrorBox message={error}/>} {confirm&&<div className="confirm-box" role="alert"><strong>Xác nhận chuyển sang “{statusLabels[data.status]}”?</strong><p>{resource==='users'?(data.status==='ACTIVE'?'Người dùng sẽ được phép đăng nhập trở lại.':'Các phiên đăng nhập hiện tại của người dùng sẽ bị hủy.'):(data.status==='ACTIVE'?'Nội dung đủ điều kiện sẽ được sử dụng trên ứng dụng Android.':'Nội dung sẽ không còn được cung cấp cho các hoạt động mới trên ứng dụng.')}</p><button type="button" className="button primary" onClick={()=>save()} disabled={busy}>Xác nhận và lưu</button></div>}
    </div><footer className="drawer-footer"><span>{recordId?`Mã #${recordId}`:'Thông tin mới'}</span><button type="button" className="button secondary" onClick={dismiss} disabled={busy}>Hủy</button><button className="button primary" type="submit" disabled={busy||coordinateBusy||(resource==='users'&&data.role==='ADMIN')}>{busy?<LoaderCircle className="spin" size={16}/>:<Check size={16}/>} {busy?'Đang lưu…':'Lưu thay đổi'}</button></footer></form>}
  </section></div>;
}
