import React,{useEffect,useState} from 'react';
import { Upload, X, Video } from 'lucide-react';
import { api,session } from './api';

function metadata(file){return new Promise((resolve,reject)=>{
  const video=document.createElement('video'),url=URL.createObjectURL(file);
  let done=false;
  const finish=(error,value)=>{if(done)return;done=true;video.onloadedmetadata=null;video.onerror=null;clearTimeout(timer);video.removeAttribute('src');video.load();URL.revokeObjectURL(url);error?reject(error):resolve(value);};
  const timer=setTimeout(()=>finish(new Error('Không đọc được thông tin video.')),15000);
  video.onloadedmetadata=()=>{const duration=video.duration,width=video.videoWidth,height=video.videoHeight;
    if(!Number.isFinite(duration)||duration<=0||duration>30)return finish(new Error('Video tối đa 30 giây.'));
    if(width<=0||height<=0||Math.min(width,height)>720||Math.max(width,height)>1280)return finish(new Error('Video tối đa 720p (1280×720 hoặc 720×1280).'));
    finish(null,{duration,width,height});};video.onerror=()=>finish(new Error('Không phát được tệp MP4 này.'));video.preload='metadata';video.src=url;
});}
function Preview({video}){
  const [open,setOpen]=useState(false),[url,setUrl]=useState(''),[error,setError]=useState('');
  useEffect(()=>{if(!open)return;const controller=new AbortController();let local;
    fetch(video.url,{signal:controller.signal,headers:{Authorization:`Bearer ${session.get()?.token}`}}).then(async r=>{if(!r.ok)throw Error('Không tải được video.');return r.blob();}).then(blob=>{if(!controller.signal.aborted){local=URL.createObjectURL(blob);setUrl(local);}}).catch(e=>{if(e.name!=='AbortError')setError(e.message);});
    return()=>{controller.abort();if(local)URL.revokeObjectURL(local);};
  },[open,video.url]);
  return <><button type="button" className="button secondary small" onClick={()=>setOpen(!open)}><Video size={15}/>{open?'Đóng video':'Xem video'}</button>{open&&(url?<video className="place-video-preview" controls preload="metadata" src={url}/>:<p role="status">{error||'Đang tải video…'}</p>)}</>;
}
export default function PlaceVideosField({value=[],change,readOnly=false,onBusy}){
  const [busy,setBusy]=useState(false),[error,setError]=useState('');
  async function upload(event){const file=event.target.files[0];if(!file)return;setBusy(true);onBusy?.(true);setError('');try{
    if(file.type!=='video/mp4'||file.size>15*1024*1024)throw Error('Chọn video MP4 không quá 15 MB.');
    await metadata(file);const video=await api('/place-videos',{method:'POST',headers:{'Content-Type':'video/mp4'},body:file});change([...value,video]);
  }catch(e){setError(e.message);}finally{setBusy(false);onBusy?.(false);event.target.value='';}}
  return <div className="place-videos-field">{!readOnly&&<p className="form-hint">MP4 · tối đa 15 MB, 30 giây, 720p. Mỗi địa danh tối đa 5 video công khai, tính cả video cộng đồng đã duyệt. Video được gắn khi lưu địa danh.</p>}
    {value.map((video,index)=><article className="place-video-entry" key={video.videoId}><div className="place-video-heading"><strong>Video {index+1} · {Math.round(video.durationSeconds)} giây · {video.width}×{video.height}</strong>{!readOnly&&<button type="button" className="icon-button" disabled={busy} aria-label={`Bỏ video ${index+1}`} onClick={()=>change(value.filter(v=>v.videoId!==video.videoId))}><X size={16}/></button>}</div><Preview video={video}/></article>)}
    {!value.length&&readOnly&&<p>Chưa có video.</p>}{!readOnly&&value.length<5&&<label className={`upload-button ${busy?'disabled':''}`}><input type="file" accept="video/mp4" disabled={busy} onChange={upload}/><Upload size={17}/>{busy?'Đang tải video…':'Tải video từ máy tính'}</label>}{error&&<p className="field-error" role="alert">{error}</p>}
  </div>;
}
