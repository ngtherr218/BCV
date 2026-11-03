import React from 'react';
import { typeFeatures } from './placeFeatureDefaults';

const groups = [['kind__', 'Loại nội dung'], ['landform__', 'Địa hình'], ['ecosystem__', 'Hệ sinh thái'], ['theme__', 'Chủ đề']];
export default function PlaceFeatures({ value = [], options = [], change, typeId }) {
  const relevant = typeFeatures[typeId] || [];
  const suggestions = options.filter(option => relevant.includes(option.code));
  const hasMatch = relevant.some(code => value.includes(code));
  const toggle = (code, checked) => change(checked ? [...new Set([...value, code])] : value.filter(item => item !== code));
  return <section className="place-features">
    <h3>Đặc trưng nội dung cho gợi ý</h3>
    <p>Chỉ chọn những đặc trưng có thật tại địa danh. Mỗi lần đổi loại, toàn bộ ô tích được thay bằng bộ gợi ý của loại mới, kể cả các ô bạn đã tự chọn. Chọn lại mục “Chọn loại địa danh” sẽ bỏ hết tích. Hãy bỏ mục không đúng và bổ sung đặc trưng thực tế. Khi lưu, hệ thống cập nhật đặc trưng và tính lại cụm.</p>
    {suggestions.length > 0 && <div className="feature-guidance">
      <strong>Đặc trưng phù hợp với loại đã chọn</strong>
      <p>Với loại gộp, mặc định chọn một trường hợp phổ biến: di tích, phố cổ, bờ biển, sông, núi hoặc rừng. Nếu là bảo tàng, làng nghề, đảo, hồ, thác hay hệ sinh thái khác, hãy đổi các ô tương ứng.</p>
      <div className="feature-options">{suggestions.map(option => <label key={option.code} title={option.description}>
        <input type="checkbox" checked={value.includes(option.code)} onChange={event => toggle(option.code, event.target.checked)} />
        <span>{option.name}</span>
      </label>)}</div>
      {!hasMatch && <p className="feature-notice" role="status">Chưa chọn đặc trưng khớp loại địa danh. Hãy chọn mục phù hợp ở trên hoặc kiểm tra lại loại đã chọn.</p>}
    </div>}
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
