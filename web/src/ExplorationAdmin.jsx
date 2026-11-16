import React, { useEffect, useState } from 'react';
import { ArrowRight, CreditCard, Flag, Map, RefreshCw, Trophy, Stamp } from 'lucide-react';
import { api, date, number } from './api';
import { Badge, Empty, ErrorBox, Loading } from './components';
import './exploration-admin.css';

const eventLabels = { DISCOVERY: 'Khám phá địa danh mới', FIRST_PLACE: 'Khám phá lần đầu (lịch sử)', JOURNEY: 'Hoàn thành hành trình (lịch sử)', QUEST: 'Hoàn thành nhiệm vụ' };
const conditionLabels = { STEP_COUNT: 'bước chân', DISCOVER_PLACE: 'địa danh', COMPLETE_JOURNEY: 'hành trình', PHOTO_APPROVED: 'ảnh được duyệt', MEDIA_APPROVED: 'nội dung được duyệt' };
const tabs = [['cards', 'Thẻ đặc trưng'], ['passport', 'Hộ chiếu vùng'], ['quests', 'Thử thách tuần'], ['ranking', 'Xếp hạng & điểm']];

export default function ExplorationAdmin({ navigate, browse, edit, revision }) {
  const [overview, setOverview] = useState(null);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(true);
  const [reload, setReload] = useState(0);
  const [tab, setTab] = useState('cards');
  const [week, setWeek] = useState('');
  const [region, setRegion] = useState('');
  const [eventType, setEventType] = useState('');
  const [filters, setFilters] = useState(null);
  const [page, setPage] = useState(1);
  const [ranking, setRanking] = useState(null);
  const [events, setEvents] = useState(null);
  const [rankingError, setRankingError] = useState('');
  const [rankingBusy, setRankingBusy] = useState(false);

  useEffect(() => {
    let active = true;
    setBusy(true); setError('');
    api('/exploration/overview').then(data => {
      if (!active) return;
      setOverview(data);
      setWeek(value => value || data.currentWeek);
      setFilters(value => value || { week: data.currentWeek, region: '', eventType: '' });
    }).catch(e => { if (active) setError(e.message); }).finally(() => { if (active) setBusy(false); });
    return () => { active = false; };
  }, [revision, reload]);

  useEffect(() => {
    if (!filters || tab !== 'ranking') return;
    let active = true;
    setRankingBusy(true); setRankingError('');
    const params = new URLSearchParams({ week: filters.week });
    if (filters.region) params.set('regionId', filters.region);
    const eventParams = new URLSearchParams(params);
    if (filters.eventType) eventParams.set('eventType', filters.eventType);
    eventParams.set('page', String(page)); eventParams.set('pageSize', '20');
    Promise.all([api(`/exploration/leaderboard?${params}&limit=20`), api(`/exploration/events?${eventParams}`)])
      .then(([board, ledger]) => { if (active) { setRanking(board); setEvents(ledger); } })
      .catch(e => { if (active) setRankingError(e.message); })
      .finally(() => { if (active) setRankingBusy(false); });
    return () => { active = false; };
  }, [filters, page, tab, revision, reload]);

  const manage = (resource, query = '') => browse(resource, query);
  return <>
    <div className="page-title"><div><span className="eyebrow">TRÒ CHƠI HÓA</span><h1>Khám phá & phần thưởng</h1><p>Theo dõi thẻ đặc trưng, hộ chiếu, thử thách tuần và thành tích khám phá.</p></div><button className="button secondary" disabled={busy} onClick={() => setReload(value => value + 1)}><RefreshCw size={16}/> Tải lại</button></div>
    {error ? <ErrorBox message={error} retry={() => setReload(value => value + 1)}/> : busy ? <Loading/> : overview && <>
      <div className="exploration-source"><span className="workspace-dot"/> Dữ liệu đang kết nối: <strong>{overview.database}</strong><span>Tuần hiện tại: {overview.currentWeek}</span></div>
      <div className="exploration-metrics">
        {[[CreditCard, overview.counts.distinctiveCards, 'Thẻ đặt tên riêng', 'cards'], [Stamp, overview.counts.passportStamps, 'Dấu khám phá đã ghi', 'passport'], [Flag, overview.counts.weeklyQuests, 'Thử thách tuần đang mở', 'quests'], [Trophy, overview.counts.weeklyParticipants, 'Người có điểm tuần này', 'ranking']].map(([Icon, value, label, key]) => <button className={`panel exploration-metric ${tab === key ? 'selected' : ''}`} key={key} onClick={() => setTab(key)}><Icon size={22}/><strong>{number(value)}</strong><span>{label}</span></button>)}
      </div>
      <div className="exploration-shortcuts">
        <button className="button secondary small" onClick={() => manage('cards')}>Quản lý toàn bộ thẻ <ArrowRight size={14}/></button>
        <button className="button secondary small" onClick={() => manage('collections')}>Quản lý bộ sưu tập <ArrowRight size={14}/></button>
        <button className="button secondary small" onClick={() => manage('quests')}>Cấu hình nhiệm vụ <ArrowRight size={14}/></button>
        <button className="button secondary small" onClick={() => manage('items')}>Quản lý vật phẩm <ArrowRight size={14}/></button>
      </div>
      <section className="panel exploration-panel">
        <div className="exploration-tabs" role="tablist" aria-label="Theo dõi khám phá">{tabs.map(([key, title]) => <button key={key} id={`exploration-tab-${key}`} role="tab" aria-selected={tab === key} aria-controls={`exploration-${key}`} className={tab === key ? 'active' : ''} onClick={() => setTab(key)}>{title}</button>)}</div>
        <div className="exploration-tab-content" id={`exploration-${tab}`} role="tabpanel" aria-labelledby={`exploration-tab-${tab}`}>
          {tab === 'cards' && <>
            <div className="exploration-heading"><div><h2>Thẻ đặc trưng địa danh</h2><p>Các thẻ có tên riêng khác tên địa danh, như Đèn lồng Hội An hay Thuyền buồm Hạ Long. Chỉnh tên, mô tả, ảnh và trạng thái ngay trong trình quản lý thẻ hiện có.</p></div><button className="button primary" onClick={() => manage('cards')}>Mở quản lý thẻ</button></div>
            <div className="table-scroll"><table className="resource-table"><thead><tr><th>Thẻ</th><th>Địa danh / vùng</th><th>Ảnh</th><th>Đã cấp</th><th>Trạng thái</th><th>Quản lý</th></tr></thead><tbody>{overview.cards.map(card => <tr key={card.cardId}><td><strong>{card.name}</strong><small className="exploration-description">{card.description}</small></td><td>{card.placeName}<small className="exploration-description">{card.regionName}</small></td><td>{card.hasImage ? 'Đã cấu hình' : 'Chưa có ảnh'}</td><td>{number(card.collectedCount)}</td><td><Badge value={card.status}/></td><td><button className="button secondary small" onClick={() => edit('cards', card.cardId)}>Chỉnh sửa thẻ</button></td></tr>)}</tbody></table></div>
            {!overview.cards.length && <Empty title="Chưa có thẻ đặt tên riêng" text="Mở quản lý thẻ để cấu hình thẻ đặc trưng cho từng địa danh."/>}
            <p className="exploration-note">Hệ thống có {number(overview.counts.activeCards)} thẻ đang hoạt động. Danh sách này hiển thị tối đa 50 thẻ đặt tên riêng; mở quản lý thẻ để xem toàn bộ. Thẻ chỉ cấp theo nghiệp vụ khám phá hiện có.</p>
          </>}
          {tab === 'passport' && <>
            <div className="exploration-heading"><div><h2>Hộ chiếu theo vùng địa danh</h2><p>Tổng hợp dấu mộc và số người hoàn thành vùng. Huy hiệu được suy ra từ số địa danh đang hoạt động, không cần tạo hoặc cấp thủ công.</p></div><button className="button secondary" onClick={() => navigate('regions')}><Map size={16}/> Quản lý khu vực</button></div>
            <div className="table-scroll"><table className="resource-table"><thead><tr><th>Vùng</th><th>Địa danh cần khám phá</th><th>Tổng dấu mộc</th><th>Hộ chiếu hoàn thành</th><th>Cấu hình</th></tr></thead><tbody>{overview.regions.map(row => <tr key={row.regionId}><td>{row.name}</td><td>{number(row.total)}</td><td>{number(row.stamps)}</td><td>{number(row.completedPassports)}</td><td><button className="button secondary small" onClick={() => edit('regions', row.regionId)}>Chỉnh sửa vùng</button></td></tr>)}</tbody></table></div>
            {!overview.regions.length && <Empty title="Chưa có khu vực hoạt động"/>}
          </>}
          {tab === 'quests' && <>
            <div className="exploration-heading"><div><h2>Thử thách tuần {overview.currentWeek}</h2><p>Tiến độ tổng hợp trong chu kỳ hiện tại. Dùng màn nhiệm vụ để tạo thử thách, đổi mục tiêu, chọn vật phẩm thưởng hoặc tạm ngưng.</p></div><button className="button primary" onClick={() => manage('quests')}>Tạo / quản lý nhiệm vụ</button></div>
            <div className="table-scroll"><table className="resource-table"><thead><tr><th>Thử thách</th><th>Mục tiêu</th><th>Phần thưởng</th><th>Tham gia / đạt</th><th>Trạng thái</th><th>Quản lý</th></tr></thead><tbody>{overview.quests.map(row => <tr key={row.questId}><td>{row.name}</td><td>{number(row.targetValue)} {conditionLabels[row.conditionType] || row.conditionType}</td><td>{row.rewardName ? `${row.rewardQuantity} ${row.rewardName}` : 'Không có vật phẩm'}</td><td>{number(row.participants)} / {number(row.completed)}</td><td><Badge value={row.status}/></td><td><button className="button secondary small" onClick={() => edit('quests', row.questId)}>Chỉnh sửa thử thách</button></td></tr>)}</tbody></table></div>
            {!overview.quests.length && <Empty title="Chưa có thử thách tuần" text="Tạo nhiệm vụ với chu kỳ Hằng tuần trong trình quản lý nhiệm vụ."/>}
          </>}
          {tab === 'ranking' && <>
            <div className="exploration-heading"><div><h2>Bảng xếp hạng & lịch sử điểm</h2><p>Chọn tuần và vùng của địa danh để đối soát thành tích. Điểm được ghi từ sự kiện hợp lệ, không cộng hoặc sửa trực tiếp tại đây.</p></div></div>
            <form className="exploration-filters" onSubmit={event => { event.preventDefault(); setPage(1); setFilters({ week, region, eventType }); }}>
              <label>Tuần<input type="week" aria-label="Tuần xếp hạng" required value={week} onChange={event => setWeek(event.target.value)}/></label>
              <label>Vùng địa danh<select aria-label="Vùng xếp hạng" value={region} onChange={event => setRegion(event.target.value)}><option value="">Toàn hệ thống</option>{overview.regions.map(row => <option key={row.regionId} value={row.regionId}>{row.name}</option>)}</select></label>
              <label>Sự kiện trong lịch sử<select aria-label="Loại sự kiện điểm" value={eventType} onChange={event => setEventType(event.target.value)}><option value="">Tất cả sự kiện</option>{Object.entries(eventLabels).map(([key, title]) => <option value={key} key={key}>{title}</option>)}</select></label>
              <button className="button primary" disabled={rankingBusy}>Áp dụng</button>
            </form>
            {rankingError ? <ErrorBox message={rankingError} retry={() => setReload(value => value + 1)}/> : rankingBusy ? <Loading/> : ranking && events && <>
              <p className="exploration-note">Kỳ {ranking.week}: {date(ranking.startsAt)} → {date(ranking.endsAt)}. Bộ lọc loại sự kiện chỉ áp dụng cho lịch sử điểm.</p>
              <div className="table-scroll"><table className="resource-table"><thead><tr><th>Hạng</th><th>Nhà khám phá</th><th>Điểm</th></tr></thead><tbody>{ranking.top.map(row => <tr key={row.userId}><td>{row.rank}</td><td>{row.displayName}</td><td><strong>{number(row.points)}</strong></td></tr>)}</tbody></table></div>
              {!ranking.top.length && <Empty title="Chưa có điểm trong kỳ này" text="Kiểm tra tuần và vùng đã chọn. Hệ thống không tính ngược điểm từ lịch sử cũ."/>}
              <h3 className="exploration-ledger-title">Lịch sử ghi điểm</h3>
              <div className="table-scroll"><table className="resource-table"><thead><tr><th>Thời điểm</th><th>Người dùng</th><th>Sự kiện</th><th>Địa danh / vùng</th><th>Điểm</th></tr></thead><tbody>{events.items.map(row => <tr key={row.eventId}><td>{date(row.occurredAt)}</td><td>Nhà khám phá {row.userId}</td><td>{eventLabels[row.eventType] || row.eventType}<small className="exploration-description">Nguồn {row.sourceKey} · {row.ruleVersion}</small></td><td>{row.placeName || row.questName || 'Nhiệm vụ toàn hệ thống'}<small className="exploration-description">{row.regionName || 'Không gán vùng'}</small></td><td>+{number(row.points)}</td></tr>)}</tbody></table></div>
              {!events.items.length && <Empty title="Không có sự kiện khớp bộ lọc"/>}
              <div className="pagination"><span>{number(events.total)} sự kiện · Trang {page}</span><div><button className="button secondary small" disabled={page <= 1} onClick={() => setPage(value => value - 1)}>Trang trước</button><button className="button secondary small" disabled={page * 20 >= events.total} onClick={() => setPage(value => value + 1)}>Trang sau</button></div></div>
            </>}
            <p className="exploration-note">Quy tắc đã duyệt: khám phá lần đầu bằng đi bộ +100; hành trình đi bộ đầu tiên tới mỗi đích +50; hoàn thành nhiệm vụ mới +20. Vé máy bay không sinh điểm đi bộ. Không công khai email hoặc tên thật trên bảng xếp hạng.</p>
          </>}
        </div>
      </section>
    </>}
  </>;
}
