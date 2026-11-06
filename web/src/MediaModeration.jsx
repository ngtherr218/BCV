import React, { useEffect, useState } from 'react';
import { api, session, date } from './api';
import { ErrorBox, Loading, Empty } from './components';

function Preview({ item }) {
  const [url, setUrl] = useState('');
  const [error, setError] = useState('');
  useEffect(() => {
    const controller = new AbortController();
    let objectUrl;
    fetch(item.contentUrl, { signal: controller.signal, headers: { Authorization: `Bearer ${session.get()?.token}` } })
      .then(async response => { if (!response.ok) throw new Error('Chưa tải được tệp xem trước.'); return response.blob(); })
      .then(blob => { if (controller.signal.aborted) return; objectUrl = URL.createObjectURL(blob); setUrl(objectUrl); })
      .catch(error => { if (error.name !== 'AbortError') setError(error.message); });
    return () => { controller.abort(); if (objectUrl) URL.revokeObjectURL(objectUrl); };
  }, [item.mediaId, item.contentUrl]);
  if (error) return <ErrorBox message={error}/>;
  if (!url) return <Loading/>;
  return item.mimeType.startsWith('video/')
    ? <video controls preload="metadata" src={url} style={{width:'100%',maxHeight:300}}/>
    : <img src={url} alt={item.caption || item.placeName} style={{width:'100%',maxHeight:300,objectFit:'contain'}}/>;
}

function Submission({ item, reviewed }) {
  const [note, setNote] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const [preview, setPreview] = useState(false);
  async function review(status) {
    setBusy(true); setError('');
    try {
      await api(`/community-media/${item.mediaId}/review`, {method:'POST',body:JSON.stringify({status,note})});
      reviewed();
    } catch (error) { setError(error.message); }
    finally { setBusy(false); }
  }
  const labels = {PENDING:'Chờ duyệt',APPROVED:'Đã duyệt',REJECTED:'Từ chối'};
  return <article className="panel" style={{padding:20,marginBottom:16}}>
    <h2>{item.placeName} · {item.mimeType.startsWith('video/') ? 'Video' : 'Ảnh'}</h2>
    <p>{item.author} · {date(item.createdAt)} · {labels[item.status]}</p>
    <p style={{whiteSpace:'pre-wrap'}}>{item.caption}</p>
    <button className="button secondary" onClick={()=>setPreview(!preview)}>{preview?'Đóng xem trước':'Xem nội dung'}</button>
    {preview && <Preview item={item}/>}
    {item.reviewNote && <p>Ghi chú: {item.reviewNote}</p>}
    {item.status==='PENDING' && <div style={{marginTop:16}}>
      <label>Ghi chú cho người gửi<textarea maxLength={500} value={note} onChange={e=>setNote(e.target.value)} style={{display:'block',width:'100%',marginBottom:12}}/></label>
      <button disabled={busy || !preview} title="Mở xem nội dung trước khi duyệt" className="button primary" onClick={()=>review('APPROVED')}>Duyệt công khai</button>{' '}
      <button disabled={busy} className="button danger" onClick={()=>review('REJECTED')}>Từ chối</button>
    </div>}
    {error && <ErrorBox message={error}/>}
  </article>;
}

export default function MediaModeration() {
  const [status,setStatus]=useState('PENDING');
  const [items,setItems]=useState(null);
  const [error,setError]=useState('');
  const [revision,setRevision]=useState(0);
  useEffect(()=>{
    let alive=true; setItems(null); setError('');
    api(`/community-media?status=${status}`).then(data=>{if(alive)setItems(data.items);})
      .catch(error=>{if(alive)setError(error.message);});
    return ()=>{alive=false;};
  },[status,revision]);
  return <><div className="page-title"><div><h1>Duyệt ảnh và video</h1><p>Nội dung người dùng gửi chỉ công khai sau khi được duyệt. Hiển thị tối đa 100 nội dung mới nhất theo trạng thái.</p></div>
    <button className="button secondary" onClick={()=>setRevision(v=>v+1)}>Tải lại</button></div>
    <select aria-label="Trạng thái kiểm duyệt" value={status} onChange={e=>setStatus(e.target.value)}>
      <option value="PENDING">Chờ duyệt</option><option value="APPROVED">Đã duyệt</option><option value="REJECTED">Từ chối</option>
    </select>
    {error?<ErrorBox message={error} retry={()=>setRevision(v=>v+1)}/>:!items?<Loading/>:!items.length?<Empty title="Không có nội dung ở trạng thái này"/>:
      items.map(item=><Submission key={item.mediaId} item={item} reviewed={()=>setRevision(v=>v+1)}/>)}</>;
}
