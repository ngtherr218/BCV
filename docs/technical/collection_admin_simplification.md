# Làm gọn Bộ sưu tập và thiết kế lại web quản trị

Ngày: 11/11/2026.

## Android

- Trang Bộ sưu tập là trang tổng quan: tiến độ, lối vào Album thẻ/Thẻ mới, danh sách bộ theo chủ đề, Hộ chiếu và Xếp hạng.
- Xóa bảng thẻ mới chưa xem và lưới toàn bộ thẻ khỏi trang chính. Không tự đánh dấu đã xem khi chỉ mở trang tổng quan.
- Mục Thẻ mới mở trang riêng, chỉ chứa thẻ vừa nhận chưa xem. Chỉ khi mở chi tiết thẻ mới dùng cơ chế markSeen cũ. Có empty state khi đã xem hết.
- Album thẻ có lựa chọn Tất cả/Đã sở hữu/Chưa sở hữu. Lựa chọn được giữ khi Activity tạo lại.
- Không đổi quyền xem trước, điều kiện cấp thẻ hoặc schema dữ liệu.

## Web quản trị

- Sidebar xanh đậm, nhóm điều hướng rõ hơn; font và khoảng cách được thống nhất.
- Dashboard thay banner trang trí bằng ba tác vụ nội dung, bỏ phần thông tin lặp lại và bảng xếp theo ID khu vực.
- Trang danh mục có bộ lọc trạng thái dạng nút, tìm kiếm và đặt lại bộ lọc. Không hiển thị thêm select trạng thái trùng chức năng.
- Bảng dữ liệu, biểu mẫu và trang chi tiết dùng cùng nền, viền và khoảng cách. Giữ điều hướng mobile và cơ chế xác nhận/lưu/xóa cũ.

## Xác minh

Web build thành công. Script output/playwright/verify-admin-redesign.cjs thực hiện đăng nhập admin thật vào API local v2, kiểm tra dashboard ở 320/390/760/1024/1440px, lọc ACTIVE, tìm kiếm empty, mở editor mà không lưu, điều hướng mobile. 11 kiểm tra đạt, không có lỗi JavaScript; kết quả và ảnh admin-redesign-* trong output/playwright. Không dùng mock để thay thế kiểm tra API thật.

Android build và 52/52 unit test đạt. LocalExplorerScreensTest trên emulator-5554 đạt: trang chính không có banner/lưới thẻ, mở mục Thẻ mới và empty state, album vẫn tải ảnh xem trước; đồng thời hồi quy tìm kiếm/bộ lọc/hộ chiếu/xếp hạng. Bằng chứng: output/dev-v2/verification/collection-inbox-emulator.txt; ảnh redesign-collections.png, redesign-new-card-inbox.png, redesign-collection-preview.png trong cùng thư mục. Hai lượt đầu bị hộp thoại System UI ANR của emulator chặn focus; đã kiểm tra ảnh và đóng hộp thoại, lượt sau đạt. APK output/dev-v2/bcv-local-v2-debug.apk đã cập nhật/cài. Chưa kiểm tra trên điện thoại vật lý.
