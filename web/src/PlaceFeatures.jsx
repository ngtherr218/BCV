import React from 'react';

const groups = [['kind__', 'Loại nội dung'], ['landform__', 'Địa hình'], ['ecosystem__', 'Hệ sinh thái'], ['theme__', 'Chủ đề']];
export default function PlaceFeatures({ value = [], options = [], change, description }) {
  const toggle = (code, checked) => change(checked ? [...new Set([...value, code])] : value.filter(item => item !== code));
  return <section className="place-features">
    <h3>Đặc trưng nội dung cho gợi ý</h3>
    <p>{description || "Chọn các đặc trưng có thật. Cần ít nhất một đặc trưng để tính gợi ý."}</p>
    {groups.map(([prefix, label]) => <fieldset key={prefix}><legend>{label} · {options.filter(option => option.code.startsWith(prefix) && value.includes(option.code)).length} đã chọn</legend>
      <div className="feature-options">{options.filter(option => option.code.startsWith(prefix)).map(option =>
        <label key={option.code} title={option.description}><input type="checkbox" checked={value.includes(option.code)}
          onChange={event => toggle(option.code, event.target.checked)} />
          <span>{option.name}</span></label>)}</div>
    </fieldset>)}
    {!options.length && <p role="alert">Chưa tải được danh sách đặc trưng. Hãy đóng biểu mẫu và thử lại.</p>}
    <small aria-live="polite">Đã chọn {value.length}/35 đặc trưng. Cần ít nhất một đặc trưng để lưu.</small>
  </section>;
}
