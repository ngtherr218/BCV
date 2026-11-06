import React, { useEffect, useRef, useState } from 'react';
import { Trash2, LoaderCircle } from 'lucide-react';
import { api } from './api';
import { catalog } from './catalog';
import { ErrorBox } from './components';

export default function DeleteDialog({ resource, record, close, deleted, edit }) {
  const [busy, setBusy] = useState(false);
  const [check, setCheck] = useState(null);
  const [checking, setChecking] = useState(true);
  const [error, setError] = useState('');
  const panel = useRef(null);
  const cancel = useRef(null);
  const config = catalog[resource];
  const id = record[config.id];
  const name = record.name || record.full_name || `#${id}`;
  useEffect(() => {
    let alive = true;
    setChecking(true); setCheck(null); setError('');
    api(`/data/${resource}/${id}/delete-check`).then(value => { if (alive) setCheck(value); })
      .catch(error => { if (alive) setError(error.message); })
      .finally(() => { if (alive) setChecking(false); });
    return () => { alive = false; };
  }, [resource, id]);
  useEffect(() => {
    const previous = document.activeElement;
    const overflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    cancel.current?.focus();
    function keydown(event) {
      if (event.key === 'Escape') { event.preventDefault(); if (!busy) close(); }
      if (event.key === 'Tab') {
        const buttons = panel.current?.querySelectorAll('button:not(:disabled)');
        if (!buttons?.length) { event.preventDefault(); return; }
        const first = buttons[0], last = buttons[buttons.length - 1];
        if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last.focus(); }
        else if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first.focus(); }
      }
    }
    document.addEventListener('keydown', keydown);
    return () => { document.body.style.overflow = overflow; document.removeEventListener('keydown', keydown); if (previous?.isConnected) previous.focus(); };
  }, [close, busy]);
  async function confirm() {
    if (busy || checking || !check?.canDelete || config.delete === false) return;
    setBusy(true); setError('');
    try { await api(`/data/${resource}/${id}`, { method: 'DELETE' }); deleted(`Đã xóa ${config.singular} “${name}”.`); }
    catch (error) { setError(error.message); setBusy(false); }
  }
  if (config.delete === false) return null;
  return <div className="overlay delete-overlay" onMouseDown={event => { if (event.target === event.currentTarget && !busy) close(); }}>
    <section className="delete-dialog" role="alertdialog" aria-modal="true" aria-labelledby="delete-title" aria-describedby="delete-description" aria-busy={busy} ref={panel}>
      <span className="delete-symbol"><Trash2 size={25}/></span>
      <h2 id="delete-title">{check && !check.canDelete ? `Không thể xóa ${config.singular}` : `Xóa ${config.singular}?`}</h2>
      <p id="delete-description"><strong>{name}</strong> (#{id}). {checking ? 'Đang kiểm tra dữ liệu liên kết…' : check?.message} {check?.canDelete ? 'Bạn có chắc muốn xóa?' : check && (check.canChangeStatus ? 'Bạn có thể chuyển trạng thái sang Tạm ngưng trong biểu mẫu chỉnh sửa.' : 'Nội dung này chưa hỗ trợ trạng thái. Hãy giữ lại khi còn liên kết.')}</p>
      {error && <ErrorBox message={error}/>}
      <div className="delete-dialog-actions"><button ref={cancel} type="button" className="button secondary" onClick={close} disabled={busy}>Hủy</button>{check && !check.canDelete ? check.canChangeStatus && <button type="button" className="button primary" onClick={()=>{close();edit(resource,id);}}>Đổi trạng thái</button> : <button type="button" className="button danger" onClick={confirm} disabled={busy || checking || !check?.canDelete}>{busy ? <LoaderCircle size={17} className="spin"/> : <Trash2 size={17}/>} {busy ? 'Đang xóa…' : 'Xác nhận xóa'}</button>}</div>
    </section>
  </div>;
}
