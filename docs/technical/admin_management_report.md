# Bổ sung quản trị khám phá và phần thưởng

## Nguyên nhân không thấy chức năng mới

Đợt đầu chỉ tích hợp backend/Android và tái sử dụng CRUD cũ; menu web chưa có nơi tập trung để nhìn thấy hộ chiếu, điểm tuần/vùng hay nhóm thẻ mới. Không phải dữ liệu mới có sẵn một trang CRUD riêng.

## Đã bổ sung

Menu **Trò chơi hóa → Khám phá & phần thưởng**, đường dẫn http://127.0.0.1:5173/#exploration.

- Thẻ đặc trưng: tên/mô tả, địa danh/vùng, ảnh đã cấu hình, số lượt cấp và trạng thái. Nút sửa mở Drawer thẻ cũ, không tạo CRUD trùng.
- Hộ chiếu vùng: 63 vùng hoạt động, tổng địa danh cần khám phá, dấu mộc và số người hoàn thành; chỉnh vùng bằng form cũ. Badge vẫn suy ra, không cấp tay.
- Thử thách tuần: 6 thử thách đang mở, mục tiêu, vật phẩm, số người có tiến độ/hoàn thành trong tuần hiện tại; nút sửa mở form quest cũ.
- Xếp hạng & điểm: top tuần/vùng, khoảng thời gian kỳ, lịch sử sự kiện có phân trang và bộ lọc loại sự kiện. Không có thao tác cộng/sửa điểm tùy ý.
- Hiển thị database đang kết nối để phân biệt bcv với v2. Các nút nhanh dẫn tới thẻ/bộ sưu tập/nhiệm vụ/vật phẩm hiện có.

Các API mới chỉ đọc, cùng ADMIN endpoint filter hiện có: GET /api/admin/exploration/overview, /leaderboard, /events. Query được parameterize, page/limit/week/event được validate, no-store cho dữ liệu admin. Không thêm bảng/cột/index hoặc công thức điểm mới.

## Kiểm thử thực thi

- Backend build thành công. Web Vite build thành công, 11/11 unit tests web đạt.
- HTTP admin login và ba API mới trả 200; anonymous overview trả 401; tuần không hợp lệ trả 400. API vẫn trỏ đúng vietnam_explorer_dev_v2.
- Playwright thực tế: đăng nhập; menu mới; 10 thẻ; mở đúng form Đèn lồng Hội An rồi hủy không lưu; xem 63 vùng; xem 6 thử thách; xem điểm/lịch sử; lọc vùng An Giang + QUEST và thấy empty state đúng; nút quản lý toàn bộ thẻ chuyển tới #cards.
- Đã xem ảnh desktop 1440×1000 và mobile 390×844. Bảng rộng dùng horizontal scroll, các chỉ số/điều khiển không tràn viewport.
- Console quan sát có một lỗi favicon.ico 404 có sẵn; không thấy lỗi tải API hoặc render trang mới trong các thao tác trên.
- Chưa sửa/lưu CRUD dữ liệu trong kiểm thử trình duyệt này; chỉ mở/cancel editor. Android/Health Connect vẫn theo giới hạn đã ghi trước đó.

Ảnh kiểm tra: output/playwright/admin-cards-after-import.png, admin-ranking-after-import.png, admin-cards-mobile.png. Browser là phiên QA riêng, không sử dụng tài khoản bên ngoài hoặc đăng dữ liệu lên cloud.