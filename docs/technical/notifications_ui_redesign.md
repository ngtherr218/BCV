# Giao diện thông báo Android

- Giữ tiêu đề và nút quay lại thống nhất với các màn hình chi tiết. Menu ba chấm chứa đánh dấu tất cả đã đọc, làm mới và ẩn tất cả.
- Bộ lọc Tất cả / Chưa đọc có số lượng chưa đọc; giữ lựa chọn khi xoay màn hình.
- Nhóm thông báo theo Hôm nay, Hôm qua và ngày cụ thể, mới nhất trước.
- Dùng nền xanh nhẹ và dấu chấm cho thông báo chưa đọc; loại sự kiện được phân biệt bằng biểu tượng. Giữ đầy đủ nội dung, thời gian và trạng thái đọc.
- Trạng thái trống riêng cho hộp thư chưa có tin và bộ lọc đã đọc hết; có nút trở lại tất cả. Lỗi làm mới giữ danh sách và cho thử lại.
- Thao tác ẩn dùng cơ chế local hiện có, có xác nhận và không thay đổi điểm/vật phẩm. Không thêm API hay thay schema.

Kiểm chứng: APK build thành công, 52 unit test đạt. Kiểm thử thiết bị với API local xác nhận danh sách thông báo thật, bộ lọc chưa đọc, menu đánh dấu tất cả, trạng thái đã đọc hết và xem lại danh sách đã đọc; `OK (1 test)`. Log: `output/dev-v2/verification/notifications-redesign-emulator.txt`. Ảnh chụp: `notifications-redesign-all.png` và `notifications-redesign-read.png` trong cùng thư mục. APK: `output/dev-v2/bcv-local-v2-debug.apk`, đã cài lên emulator-5554. Kiểm thử dùng tài khoản riêng `example.invalid` và khôi phục phiên người dùng sau khi chạy.
