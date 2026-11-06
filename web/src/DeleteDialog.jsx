import React, { useEffect, useRef, useState } from 'react';
import { Trash2, LoaderCircle } from 'lucide-react';
import { api } from './api';
import { catalog } from './catalog';
import { ErrorBox } from './components';

export default function DeleteDialog({ resource, record, close, deleted }) {
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const panel = useRef(null);
  const cancel = useRef(null);
  const config = catalog[resource];
  const id = record[config.id];
  const name = record.name || record.full_name || `#${id}`;
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
    if (busy || config.delete === false) return;
    setBusy(true); setError('');
    try { await api(`/data/${resource}/${id}`, { method: 'DELETE' }); deleted(`Đã xóa ${config.singular} “${name}”.`); }
    catch (error) { setError(error.message); setBusy(false); }
  }
  if (config.delete === false) return null;
  return <div className="overlay delete-overlay" onMouseDown={event => { if (event.target === event.currentTarget && !busy) close(); }}>
    <section className="delete-dialog" role="alertdialog" aria-modal="true" aria-labelledby="delete-title" aria-describedby="delete-description" aria-busy={busy} ref={panel}>
      <span className="delete-symbol"><Trash2 size={25}/></span>
      <h2 id="delete-title">Xóa {config.singular}?</h2>
      <p id="delete-description">Bạn có chắc muốn xóa <strong>{name}</strong> (#{id})? Thao tác này không thể hoàn tác. Bản ghi có liên kết hoặc lịch sử sử dụng sẽ được bảo vệ.</p>
      {error && <ErrorBox message={error}/>}
      <div className="delete-dialog-actions"><button ref={cancel} type="button" className="button secondary" onClick={close} disabled={busy}>Hủy</button><button type="button" className="button danger" onClick={confirm} disabled={busy}>{busy ? <LoaderCircle size={17} className="spin"/> : <Trash2 size={17}/>} {busy ? 'Đang xóa…' : 'Xác nhận xóa'}</button></div>
    </section>
  </div>;
}
