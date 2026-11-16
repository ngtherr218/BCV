# Gỡ kiểm duyệt khỏi web quản trị — 11/11/2026

Thực hiện yêu cầu “Loại bỏ chức năng kiểm duyệt ở web quản trị”.

- Gỡ mục Kiểm duyệt nội dung khỏi menu, tiêu đề và nhánh route #media. Hash cũ dùng cơ chế fallback hiện có về dashboard.
- Xoá MediaModeration.jsx và ModerationActionDialog.jsx cùng CSS riêng của màn kiểm duyệt.
- Gỡ nút duyệt/từ chối/gỡ/khôi phục, xử lý báo cáo và khoá/mở quyền gửi ảnh trong chi tiết người dùng.
- Chi tiết địa danh vẫn có gallery ảnh cộng đồng đã công khai ở chế độ xem; không còn thao tác kiểm duyệt hoặc thay đổi quyền gửi trên web.
- Không thay API, database, dữ liệu ảnh hoặc Android trong lượt này. API kiểm duyệt backend vẫn tồn tại; yêu cầu này chỉ gỡ chức năng ở web.

Kiểm tra: Vite production build thành công; 11/11 web unit test đạt; rà soát source không còn import/route MediaModeration hoặc lời gọi review/upload-access từ giao diện web; git diff --check đạt. Đã kiểm thử tương tác bằng Playwright trong lượt tiếp tục: đăng nhập admin local, menu không còn mục kiểm duyệt, hash #media hiển thị dashboard, chi tiết tài khoản test không còn nút khoá/mở quyền gửi hoặc thao tác kiểm duyệt. Ảnh bằng chứng ở output/playwright/user-detail-without-moderation.png. Browser chỉ ghi nhận lỗi favicon.ico 404 đã có từ trước; không ghi nhận lỗi JavaScript ứng dụng trong các màn đã đi qua.
