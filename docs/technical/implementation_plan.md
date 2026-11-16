# Kế hoạch local trong 14 ngày

**Cập nhật 11/11/2026:** đã rà soát lại source/database; không cần schema mới cho P0/P1. Ghép ảnh milestone đã được bổ sung và kiểm thử render, Android emulator/API đã chạy, CRUD API bản nháp region/collection/quest đạt. Phần còn lại là cảm biến/GPS điện thoại vật lý, browser CRUD/media đầy đủ, rà soát mỹ thuật thẻ và Python suite. P2 chưa bắt đầu. Lịch bên dưới là khung ưu tiên 14 ngày, không phải cam kết mọi hạng mục đã nghiệm thu.

| Ngày | Công việc | Điều kiện hoàn thành |
|---|---|---|
|1–2|Khảo sát, live snapshot chỉ đọc; tạo DB mới bằng schema-only dump sau khi xác định source local|Snapshot đầy đủ, current_database() đúng, không copy user thật|
|3–4|P0.1 tìm kiếm kết hợp + Android filter/map/list; P0.3 preview|Test ngoài sở thích, phân trang, ACTIVE, không cấp thưởng khi xem|
|5|P0.4 estimate từ activity/config; P0.2 2+1|Không thay StepConversion; fallback thiếu ứng viên; test cấu hình|
|6–7|P0.5 cấu hình 10 card và ảnh được phép; P0.6 passport Android/API|Transaction cấp một lần, badge suy ra, không vùng rỗng tự hoàn thành|
|8–9|P1.2 kiểm thử Quest tuần và UI; P1.3 milestones|Ranh giới tuần, thưởng đồng thời một lần, không thưởng ở 25/50/75|
|10–11|P1.1 theo phương án được duyệt|Điểm tuần/vùng đúng; vé và lặp không farm; top/me bảo vệ riêng tư|
|12–13|Regression backend/Android/web với DB v2; xử lý lỗi|Bằng chứng thực thi và so sánh schema, không endpoint production|
|14|Demo local, tài liệu đồ án, danh sách giới hạn|Chạy lại từ hướng dẫn, seed được phép, hồ sơ nghiệm thu|

P2 chỉ bắt đầu nếu P0/P1 ổn định. Không hứa toàn bộ P2 trong 14 ngày. Phần chờ schema không ngăn việc phát triển read-only API/UI sau khi khảo sát đủ contract.

## Cổng an toàn database

1. Chủ dự án điền .env.local từ .env.example, DB_SOURCE_NAME chỉ database local cũ; DB_NAME cố định vietnam_explorer_dev_v2.
2. Chạy `dotnet run --project backend/Bcv.DatabaseAudit` tại root. Công cụ chỉ loopback, REPEATABLE READ + READ ONLY, không khởi động API, không xuất dữ liệu người dùng.
3. Lấy schema-only dump từ source bằng pg_dump đã cài, gồm schema/constraint/index/sequence/extension; không chạy lại mù quáng 001–026 vì một số migration đổi constraint/type/default của hệ cũ.
4. Xác minh source local và database target khác nhau; CREATE DATABASE chỉ tên v2, không TEMPLATE từ database có người dùng. Nếu v2 đã tồn tại, dừng để kiểm tra, không xóa/ghi đè.
5. Khôi phục schema-only vào v2 sau kiểm tra current_database(); so sánh định nghĩa chuẩn hóa bỏ owner và metadata dump. Seed danh mục được phép tách biệt user/session/steps/journey.
6. Chặn automatic EnsureAsync và cloud side effects trong profile v2 trước khi chạy API, dùng session file riêng.
7. Chỉ migration mới đã được phê duyệt; lưu log tên migration và kết quả so sánh. Không dùng DROP/TRUNCATE trên DB cũ.

## Chạy local hiện tại

Backend dùng `backend/run-dev-v2.ps1`, cổng 5082. Web `cd web`, `npm run dev`, proxy đã cấu hình 5082. Android dùng `backend/build-android-dev-v2.ps1 -Api http://127.0.0.1:5082/` cùng `adb reverse tcp:5082 tcp:5082`; điện thoại có thể dùng IP LAN. Đường 10.0.2.2 hiện timeout trên máy đã test. Xem `local_v2_runbook.md`; không dùng launcher cũ trỏ bcv.

## Cổng hoàn tất còn lại

| Ưu tiên | Việc tiếp theo | Bằng chứng bắt buộc |
|---|---|---|
| P0/P1 | Đi bộ thực, Health Connect/cảm biến/GPS trên thiết bị | Tổng bước trước/sau, reconnect/offline/nền và tiến độ server |
| P0/P1 | Browser CRUD place/type/card và media moderation | Fixture riêng, trạng thái trước/sau, phân quyền, cleanup |
| P0/P1 | Rà soát bộ 10 artwork tự tạo và demo Goong | 10 ảnh đã lưu local; kiểm tra mạng/thiết bị demo thực |
| Regression | Python suite và kiểm thử dài ngày | Log thực thi; không tự công bố chỉ số đánh giá người dùng |
| Tài liệu | ERD/DFD/đặc tả chính thức | Đối chiếu tám báo cáo Markdown và schema snapshot |
| P2 | Chỉ mở sau các cổng trên | Phạm vi được chọn; mọi DDL mới phải có đề xuất/phê duyệt riêng |
