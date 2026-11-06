import React, { useEffect, useState } from 'react';
import { Search, RefreshCw, Image, Video, CheckCircle2, ShieldAlert, Ban, RotateCcw, LockKeyhole, UnlockKeyhole, X } from 'lucide-react';
import { api, session, date, number } from './api';
import { ErrorBox, Loading, Empty } from './components';
import ConfirmAction from './ModerationActionDialog';

const labels={PENDING:'Chờ duyệt',APPROVED:'Đã duyệt',REJECTED:'Từ chối',FLAGGED:'Tạm ẩn do báo cáo',REMOVED:'Đã gỡ',EXPIRED:'Hết hạn'};
export const fileSize=value=>value>=1048576?`${(value/1048576).toFixed(1)} MB`:`${Math.ceil((value||0)/1024)} KB`;
function Preview({item,ready}){
  const [url,setUrl]=useState('');const [error,setError]=useState('');
  useEffect(()=>{
    const controller=new AbortController();let objectUrl;
    fetch(item.contentUrl,{signal:controller.signal,headers:{Authorization:`Bearer ${session.get()?.token}`}})
      .then(async r=>{if(!r.ok)throw new Error('Không tải được tệp xem trước.');return r.blob();})
      .then(blob=>{if(controller.signal.aborted)return;objectUrl=URL.createObjectURL(blob);setUrl(objectUrl);})
      .catch(e=>{if(e.name!=='AbortError')setError(e.message);});
    return()=>{controller.abort();if(objectUrl)URL.revokeObjectURL(objectUrl);};
  },[item.contentUrl,item.mediaId]);
  if(error)return <ErrorBox message={error}/>;
  if(!url)return <Loading/>;
  return item.mimeType.startsWith('video/')?<video controls preload="auto" src={url} onLoadedData={ready} onError={()=>setError('Không phát được video.')} aria-label={`Video tại ${item.placeName}`}/>:<img src={url} alt={item.caption||item.placeName} onLoad={ready} onError={()=>setError('Không hiển thị được ảnh.')}/>;
}
function Submission({item,act}){
  const [preview,setPreview]=useState(false);const [ready,setReady]=useState(false);const [reports,setReports]=useState(null);const [reportError,setReportError]=useState('');const [reportBusy,setReportBusy]=useState(false);
  async function loadReports(){setReportBusy(true);setReportError('');try{setReports(await api(`/community-media/${item.mediaId}/reports`));}catch(e){setReportError(e.message);}finally{setReportBusy(false);}}
  return <article className="panel moderation-card"><div className="moderation-media">{item.contentAvailable===false?<p className="form-hint">Tệp đã được xóa theo chính sách lưu trữ.</p>:preview?<Preview item={item} ready={()=>setReady(true)}/>:<button type="button" className="moderation-open" onClick={()=>setPreview(true)}>{item.mimeType.startsWith('video/')?<Video size={34}/>:<Image size={34}/>}<span>Xem {item.mimeType.startsWith('video/')?'video':'ảnh'}</span></button>}</div>
    <div className="moderation-info"><div className="moderation-heading"><span className={`media-status media-status-${item.status.toLowerCase()}`}>{labels[item.status]}</span><small>#{item.mediaId} · {fileSize(item.bytes)}</small></div>
      <h2>{item.placeName}</h2><p className="moderation-author">{item.author} · #{item.userId}{item.uploadBlocked&&<span className="media-status media-status-removed">Đã khóa quyền gửi</span>}</p><small>Gửi lúc {date(item.createdAt)}</small>
      <p className="moderation-caption">{item.caption||'Không có chú thích.'}</p>
      {item.reviewedAt&&<p className="moderation-review">{item.reviewer||'Quản trị viên'} · {date(item.reviewedAt)}{item.reviewNote&&<><br/>{item.reviewNote}</>}</p>}
      <div className="moderation-actions">{item.status==='PENDING'&&<><button className="button primary" disabled={!ready} title={!ready?'Mở và tải nội dung trước khi duyệt':undefined} onClick={()=>act({item,status:'APPROVED'})}><CheckCircle2 size={15}/>Duyệt</button><button className="button danger" onClick={()=>act({item,status:'REJECTED'})}><X size={15}/>Từ chối</button></>}
        {['APPROVED','FLAGGED'].includes(item.status)&&<button className="button danger" onClick={()=>act({item,status:'REMOVED'})}><Ban size={15}/>Gỡ nội dung</button>}
        {item.status==='APPROVED'&&item.reportCount>0&&<button className="button primary" disabled={!ready} title={!ready?'Mở nội dung trước khi xử lý báo cáo':undefined} onClick={()=>act({item,status:'APPROVED'})}><CheckCircle2 size={15}/>Giữ công khai</button>}
        {item.status==='FLAGGED'&&<button className="button primary" disabled={!ready} title={!ready?'Mở nội dung trước khi khôi phục':undefined} onClick={()=>act({item,status:'APPROVED'})}><RotateCcw size={15}/>Khôi phục</button>}
        <button className="button secondary" onClick={()=>act({item,blocked:!item.uploadBlocked})}>{item.uploadBlocked?<UnlockKeyhole size={15}/>:<LockKeyhole size={15}/>} {item.uploadBlocked?'Mở quyền gửi':'Khóa quyền gửi'}</button>
        {item.totalReports>0&&<button className="button secondary" onClick={loadReports} disabled={reportBusy}><ShieldAlert size={15}/>{reportBusy?'Đang tải…':`${item.reportCount} báo cáo chưa xử lý · ${item.totalReports} tổng`}</button>}
      </div>
      {reportError&&<ErrorBox message={reportError}/>} {reports&&<section className="moderation-reports"><h3>Báo cáo vi phạm</h3>{reports.map(r=><div key={r.report_id}><strong>{r.reporter}</strong> · {({OPEN:'Chưa xử lý',ACCEPTED:'Chấp nhận báo cáo',DISMISSED:'Bác báo cáo',RESOLVED:'Đã xử lý'})[r.status]||r.status}<small>{date(r.created_at)}</small><p>{r.reason}</p></div>)}</section>}
    </div></article>;
}
export default function MediaModeration(){
  const [status,setStatus]=useState('PENDING');const [q,setQ]=useState('');const [query,setQuery]=useState('');const [kind,setKind]=useState('');const [reported,setReported]=useState(false);const [page,setPage]=useState(1);const [data,setData]=useState(null);const [busy,setBusy]=useState(true);const [error,setError]=useState('');const [revision,setRevision]=useState(0);const [action,setAction]=useState(null);const [message,setMessage]=useState('');
  useEffect(()=>{const timer=setTimeout(()=>{setQuery(q);setPage(1);},300);return()=>clearTimeout(timer);},[q]);
  useEffect(()=>{let alive=true;setBusy(true);setError('');api(`/community-media?${new URLSearchParams({status,q:query,kind,reported,page,pageSize:12})}`).then(value=>{if(alive){setData(value);const last=Math.max(1,Math.ceil(value.total/12));if(page>last)setPage(last);}}).catch(e=>{if(alive)setError(e.message);}).finally(()=>{if(alive)setBusy(false);});return()=>{alive=false;};},[status,query,kind,reported,page,revision]);
  const stats=data?.stats||{};const pages=Math.max(1,Math.ceil((data?.total||0)/12));
  return <><div className="page-title"><div><span className="eyebrow">NỘI DUNG CỘNG ĐỒNG</span><h1>Kiểm duyệt ảnh và video</h1><p>Xem nội dung, xử lý báo cáo và quản lý quyền gửi của người dùng.</p></div><button className="button secondary" disabled={busy} onClick={()=>setRevision(v=>v+1)}><RefreshCw size={16}/>Tải lại</button></div>
    <div className="moderation-metrics">{[['Chờ duyệt',number(stats.pending)],['Đã duyệt',number(stats.approved)],['Có báo cáo chưa xử lý',number(stats.reported)],['Dung lượng tệp đang lưu',fileSize(stats.bytes)]].map(([label,value])=><div className="panel" key={label}><small>{label}</small><strong>{value}</strong></div>)}</div>
    <section className="panel moderation-filters"><div className="moderation-tabs" aria-label="Lọc trạng thái">{[['PENDING','Chờ duyệt'],['','Tất cả'],['APPROVED','Đã duyệt'],['FLAGGED','Tạm ẩn'],['REMOVED','Đã gỡ'],['REJECTED','Từ chối'],['EXPIRED','Hết hạn']].map(([key,label])=><button type="button" key={key} className={status===key?'active':''} aria-pressed={status===key} onClick={()=>{setStatus(key);setPage(1);}}>{label}</button>)}</div>
      <div className="filter-bar"><div className="search-box"><Search size={17}/><input value={q} maxLength={200} aria-label="Tìm nội dung" placeholder="Tên địa danh, người gửi hoặc chú thích…" onChange={e=>setQ(e.target.value)}/></div><select value={kind} aria-label="Loại tệp" onChange={e=>{setKind(e.target.value);setPage(1);}}><option value="">Ảnh và video</option><option value="image">Chỉ ảnh</option><option value="video">Chỉ video</option></select><label className="moderation-report-filter"><input type="checkbox" checked={reported} onChange={e=>{setReported(e.target.checked);if(e.target.checked)setStatus('');setPage(1);}}/> Có báo cáo chưa xử lý</label></div>
    </section>
    {message&&<div className="moderation-success" role="status">{message}<button className="icon-button" aria-label="Đóng thông báo" onClick={()=>setMessage('')}><X size={16}/></button></div>}
    {error?<ErrorBox message={error} retry={()=>setRevision(v=>v+1)}/>:busy?<Loading/>:!data?.items.length?<Empty title="Không có nội dung phù hợp" text="Thử thay đổi bộ lọc hoặc tải lại danh sách."/>:<><p className="moderation-total">{number(data.total)} nội dung · Trang {page}/{pages}</p>{data.items.map(item=><Submission key={`${item.mediaId}:${revision}`} item={item} act={setAction}/>)}<div className="pagination"><span>Trang {page}/{pages}</span><div><button className="button secondary" disabled={page===1} onClick={()=>setPage(v=>v-1)}>Trước</button><button className="button secondary" disabled={page>=pages} onClick={()=>setPage(v=>v+1)}>Sau</button></div></div></>}
    {action&&<ConfirmAction action={action} close={()=>setAction(null)} done={text=>{setAction(null);setMessage(text);setRevision(v=>v+1);}}/>}
  </>;
}
