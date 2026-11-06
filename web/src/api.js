const KEY = 'bcv-admin-session';
export const session = {
  get() { try { return JSON.parse(sessionStorage.getItem(KEY) || 'null'); } catch { return null; } },
  set(value) { value ? sessionStorage.setItem(KEY, JSON.stringify(value)) : sessionStorage.removeItem(KEY); },
};
export async function api(path, options = {}) {
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), 30000);
  const token = session.get()?.token;
  try {
    const response = await fetch(path.startsWith('/api/') ? path : `/api/admin${path}`, {
      ...options, signal: controller.signal,
      headers: { ...(options.body && typeof options.body === 'string' ? { 'Content-Type': 'application/json' } : {}), ...(token ? { Authorization: `Bearer ${token}` } : {}), ...options.headers },
    });
    const data = await response.json().catch(() => null);
    if (!response.ok) {
      if (response.status === 401 && path !== '/login') { session.set(null); window.dispatchEvent(new Event('bcv-session-expired')); }
      throw new Error(data?.message || (response.status === 503 || response.status === 500 ? 'Không kết nối được backend hoặc cơ sở dữ liệu. Kiểm tra server cổng 5081.' : `Yêu cầu không thành công (${response.status}).`));
    }
    return data;
  } catch (error) {
    if (error.name === 'AbortError') throw new Error('Máy chủ phản hồi quá lâu. Vui lòng thử lại.');
    if (error instanceof TypeError) throw new Error('Không kết nối được máy chủ. Kiểm tra backend đang chạy tại cổng 5081.');
    throw error;
  } finally { clearTimeout(timeout); }
}
export const number = value => new Intl.NumberFormat('vi-VN').format(value || 0);
export const date = value => value ? new Intl.DateTimeFormat('vi-VN', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value.endsWith?.('Z') ? value : `${value}Z`)) : '—';
export function downloadCSV(rows, columns, file) {
  const escape = value => { let text = String(value ?? ''); if (/^[=+@\-\t\r]/.test(text)) text = "'" + text; return `"${text.replaceAll('"', '""')}"`; };
  const csv = '\ufeff' + [columns.map(c => escape(c.label)), ...rows.map(row => columns.map(c => escape(row[c.key])))].map(row => row.join(',')).join('\r\n');
  const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv;charset=utf-8;' }));
  const link = document.createElement('a'); link.href = url; link.download = file; link.click(); setTimeout(() => URL.revokeObjectURL(url), 1000);
}
