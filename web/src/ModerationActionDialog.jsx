import React,{useEffect,useRef,useState} from 'react';
import {api} from './api';
import {ErrorBox} from './components';

export default function ConfirmAction({action,close,done}){
  const [note,setNote]=useState('');const [busy,setBusy]=useState(false);const [error,setError]=useState('');const panel=useRef(null);
  const {item,status,blocked}=action;const access=blocked!==undefined;
  const title=access?(blocked?'Khóa quyền gửi nội dung':'Mở lại quyền gửi nội dung'):status==='APPROVED'?(item.status==='PENDING'?'Duyệt công khai':item.status==='APPROVED'?'Giữ nội dung công khai':'Khôi phục nội dung'):status==='REJECTED'?'Từ chối nội dung':'Gỡ nội dung';
  const required=access||status!=='APPROVED'||item.status!=='PENDING';
  useEffect(()=>{
    const previous=document.activeElement;const old=document.body.style.overflow;document.body.style.overflow='hidden';panel.current?.querySelector('textarea')?.focus();
    function key(e){if(e.key==='Escape'){e.preventDefault();e.stopImmediatePropagation();if(!busy)close();}if(e.key==='Tab'){e.stopImmediatePropagation();const fields=panel.current?.querySelectorAll('button:not(:disabled),textarea');const first=fields?.[0],last=fields?.[fields.length-1];if(e.shiftKey&&document.activeElement===first){e.preventDefault();last.focus();}else if(!e.shiftKey&&document.activeElement===last){e.preventDefault();first.focus();}}}
    document.addEventListener('keydown',key,true);return()=>{document.body.style.overflow=old;document.removeEventListener('keydown',key,true);if(previous?.isConnected)previous.focus();};
  },[busy,close]);
  async function submit(e){e.preventDefault();if(busy)return;setBusy(true);setError('');try{
    await api(access?`/community-media/users/${item.userId}/upload-access`:`/community-media/${item.mediaId}/review`,{method:'POST',body:JSON.stringify(access?{blocked,note}:{status,note,expectedStatus:item.status})});done(title+' thành công.');
  }catch(e){setError(e.message);setBusy(false);}}
  return <div className="overlay delete-overlay" onMouseDown={e=>{if(e.target===e.currentTarget&&!busy)close();}}><section className="delete-dialog moderation-confirm" role="dialog" aria-modal="true" aria-labelledby="moderation-title" ref={panel}>
    <h2 id="moderation-title">{title}?</h2><p>{access?`${item.author} (#${item.userId})`: `${item.placeName} · Nội dung #${item.mediaId}`}</p>
    <p>{access?'Chỉ thay đổi quyền gửi ảnh/video; các chức năng khác của tài khoản vẫn sử dụng được.':status==='REMOVED'?'Nội dung sẽ ngừng công khai và tệp sẽ bị xóa vĩnh viễn. Thao tác này không thể khôi phục.':status==='APPROVED'?'Nội dung sẽ được hiển thị công khai tại địa danh. Người gửi nhận thông báo kết quả.':'Nội dung sẽ không được công khai. Người gửi nhận lý do từ chối.'}</p>
    <form onSubmit={submit}><label htmlFor="moderation-note">{required?'Lý do (bắt buộc)':'Ghi chú cho người gửi (không bắt buộc)'}</label><textarea id="moderation-note" value={note} maxLength={500} required={required} disabled={busy} onChange={e=>setNote(e.target.value)} rows={4}/><small>{note.length}/500 ký tự</small>
      {error&&<ErrorBox message={error}/>}<div className="delete-dialog-actions"><button type="button" className="button secondary" disabled={busy} onClick={close}>Hủy</button><button className={`button ${status==='REJECTED'||status==='REMOVED'||blocked?'danger':'primary'}`} disabled={busy||(required&&!note.trim())}>{busy?'Đang xử lý…':title}</button></div></form>
  </section></div>;
}
