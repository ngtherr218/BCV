import React from 'react';
import {HelpCircle, CheckCircle2, MapPin} from 'lucide-react';
export default function QuestionFields({data,change,places}) {
 const answer=Number(data.answer_index??0);
 return <div className="question-editor">
  <aside className="question-guide"><HelpCircle size={26}/><div><strong>Một câu hỏi, một câu chuyện</strong><p>Trạm mở theo bước chân. Mỗi câu đúng nhận 20 điểm; đúng toàn bộ trạm nhận thêm 50 điểm và dấu Am hiểu.</p></div></aside>
  <section><h3><MapPin size={18}/>Nội dung địa danh</h3><div className="form-grid">
   <div className="form-field wide"><label htmlFor="question-place">Địa danh *</label><select id="question-place" required value={data.place_id??''} onChange={e=>change('place_id',e.target.value)}><option value="">Chọn địa danh</option>{places.map(p=><option key={p.id} value={p.id}>{p.name}</option>)}</select></div>
   <div className="form-field wide"><label htmlFor="question-text">Câu hỏi *</label><textarea id="question-text" required rows={3} maxLength={300} value={data.question||''} onChange={e=>change('question',e.target.value)} placeholder="Điều gì làm nên nét riêng của địa danh này?"/><small>{(data.question||'').length}/300 ký tự</small></div>
  </div></section>
  <fieldset className="question-options"><legend>Bốn lựa chọn · Chọn đáp án đúng</legend>{['a','b','c','d'].map((letter,index)=><div className={answer===index?'question-option correct':'question-option'} key={letter}><label className="question-answer"><input type="radio" name="correct-answer" checked={answer===index} onChange={()=>change('answer_index',index)} aria-label={'Chọn đáp án đúng '+letter.toUpperCase()}/><span>{letter.toUpperCase()}</span></label><input required maxLength={150} aria-label={'Nội dung đáp án '+letter.toUpperCase()} value={data['option_'+letter]||''} onChange={e=>change('option_'+letter,e.target.value)} placeholder={'Nhập đáp án '+letter.toUpperCase()}/>{answer===index&&<CheckCircle2 size={18} aria-label="Đáp án đúng"/>}</div>)}</fieldset>
  <div className="form-field"><label htmlFor="question-explanation">Giải thích sau khi trả lời</label><textarea id="question-explanation" rows={3} maxLength={400} value={data.explanation||''} onChange={e=>change('explanation',e.target.value)} placeholder="Kể thêm một chi tiết văn hóa để người chơi ghi nhớ."/></div>
  <div className="form-grid"><div className="form-field"><label htmlFor="question-difficulty">Độ khó</label><select id="question-difficulty" value={data.difficulty??1} onChange={e=>change('difficulty',e.target.value)}><option value="1">Dễ</option><option value="2">Vừa</option><option value="3">Khó</option></select></div><div className="form-field"><label htmlFor="question-status">Trạng thái</label><select id="question-status" required value={data.status||'DRAFT'} onChange={e=>change('status',e.target.value)}><option value="DRAFT">Bản nháp</option><option value="ACTIVE">Đang hoạt động</option><option value="INACTIVE">Tạm ngưng</option></select></div></div>
  <p className="form-hint">Chỉ câu hỏi đang hoạt động xuất hiện trên hành trình. Người chơi chỉ trả lời một lần ở mỗi trạm.</p>
 </div>;
}
