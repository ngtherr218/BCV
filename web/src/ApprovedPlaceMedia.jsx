import React,{useEffect,useState} from 'react';
import { RefreshCw } from 'lucide-react';
import { api,date,session } from './api';
import { ErrorBox,Loading } from './components';

function Media({item}){
  const image=item.mimeType.startsWith('image/');
  const [requested,setRequested]=useState(false),[url,setUrl]=useState(''),[error,setError]=useState(''),[revision,setRevision]=useState(0);
  useEffect(()=>{
    setUrl('');setError('');
    // A page can contain twelve 100 MB videos: load video bytes only when requested.
    if(!image&&!requested)return;
    const controller=new AbortController();let objectUrl;
    async function load(){
      try{
        const token=session.get()?.token;
        if(!token)throw new Error('Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.');
        const endpoint=new URL(item.contentUrl,window.location.origin);
        if(endpoint.origin!==window.location.origin)throw new Error('Địa chỉ nội dung không hợp lệ.');
        const response=await fetch(endpoint.href,{signal:controller.signal,redirect:'error',headers:{Authorization:`Bearer ${token}`}});
        if(!response.ok)throw new Error('Không tải được nội dung. Tệp có thể đã bị gỡ hoặc phiên đăng nhập đã hết hạn.');
        const blob=await response.blob();
        if(controller.signal.aborted)return;
        objectUrl=URL.createObjectURL(blob);setUrl(objectUrl);
      }catch(failure){if(!controller.signal.aborted)setError(failure.message||'Không tải được nội dung.');}
    }
    load();
    return()=>{controller.abort();if(objectUrl)URL.revokeObjectURL(objectUrl);};
  },[item.contentUrl,image,requested,revision]);
  return <figure className="approved-media-card">
    {error?<ErrorBox message={error} retry={()=>setRevision(value=>value+1)}/>:!image&&!requested?
      <button type="button" className="button secondary" onClick={()=>setRequested(true)}>Xem video</button>:!url?<p role="status">Đang tải nội dung…</p>:image?
      <a href={url} target="_blank" rel="noopener noreferrer" aria-label={`Xem ảnh đầy đủ của ${item.author}`}><img src={url} alt={item.caption||`Ảnh tại ${item.placeName}`} loading="lazy" onError={()=>setError('Không hiển thị được ảnh. Hãy thử tải lại.')}/></a>:
      <video src={url} controls preload="metadata" aria-label={`Video của ${item.author}`} onError={()=>setError('Không phát được video. Hãy thử tải lại.')}/>}
    <figcaption><strong>{item.author}</strong><small>Duyệt lúc {date(item.reviewedAt)}</small>{item.caption&&<p>{item.caption}</p>}</figcaption>
  </figure>;
}
export default function ApprovedPlaceMedia({placeId}){
  const [data,setData]=useState(null),[page,setPage]=useState(1),[revision,setRevision]=useState(0),[busy,setBusy]=useState(true),[error,setError]=useState('');
  useEffect(()=>{let alive=true;setBusy(true);setError('');api(`/community-media?${new URLSearchParams({status:'APPROVED',placeId,page,pageSize:12})}`).then(value=>{if(alive){setData(value);const last=Math.max(1,Math.ceil(value.total/12));if(page>last)setPage(last);}}).catch(e=>{if(alive)setError(e.message);}).finally(()=>{if(alive)setBusy(false);});return()=>{alive=false;};},[placeId,page,revision]);
  return <section className="detail-section approved-place-media" aria-label="Nội dung cộng đồng đã duyệt"><div className="approved-media-heading"><h3>Ảnh/video cộng đồng đã duyệt {data&&<span>{data.total}</span>}</h3><button type="button" className="icon-button" aria-label="Tải lại nội dung cộng đồng" title="Tải lại nội dung cộng đồng" disabled={busy} onClick={()=>setRevision(v=>v+1)}><RefreshCw size={16}/></button></div><p className="detail-empty">Nội dung người dùng đóng góp đã được duyệt cho địa danh này.</p>
    {error?<ErrorBox message={error} retry={()=>setRevision(v=>v+1)}/>:busy?<Loading/>:!data?.items.length?<p className="detail-empty">Chưa có ảnh/video cộng đồng được duyệt.</p>:<><div className="approved-media-grid">{data.items.map(item=><Media key={item.mediaId} item={item}/>)}</div>{data.total>12&&<div className="pagination"><button type="button" className="button secondary small" disabled={page===1} onClick={()=>setPage(v=>v-1)}>Trước</button><span>{page}/{Math.ceil(data.total/12)}</span><button type="button" className="button secondary small" disabled={page*12>=data.total} onClick={()=>setPage(v=>v+1)}>Sau</button></div>}</>}
  </section>;
}
