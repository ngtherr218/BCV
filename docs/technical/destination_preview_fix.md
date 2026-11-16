# Sửa chọn điểm đến và ảnh xem trước – 13/11/2026

Màn Gợi ý dùng API tìm kiếm khi đã khởi tạo bộ lọc sở thích. API này trước đây trả cả địa danh đã mở khóa, còn showCards/renderSaving luôn gán nút Chọn điểm đến; trạng thái đã mở khóa không được phản ánh ở hành động. RecommendationScoring chính đã loại discovered nên lỗi xuất hiện chủ yếu ở nhánh search/filter.

- Thêm onlyUndiscovered vào PlaceSearchQuery, mặc định false để giữ tìm kiếm chung. Chế độ chọn điểm đến trên Android gửi true; SQL lọc discovered và điểm 0 bước trước count/phân trang.
- Fallback với dữ liệu cũ/stale: địa danh đã mở khóa chỉ có nút Xem địa danh, không gọi createJourney. Bỏ icon khóa và blur ở địa danh đã mở khóa. Điểm 0 bước không thể chọn.
- Chưa mở khóa: ảnh thật alpha 0.65, blur nhẹ, giữ icon khóa; thiếu/lỗi ảnh có placeholder thay ô trống. Khi ảnh tải xong xóa tint và padding/contentPadding, dùng centerCrop để phủ đúng khung.
- Ba địa danh trong screenshot có 2 ảnh/địa danh. URL ảnh đầu của Văn Miếu, Lăng Chủ tịch Hồ Chí Minh, Hoàng thành Thăng Long đều HTTP 200 trên máy tính, lần lượt khoảng 0.47/0.32/0.41 giây. Test không có ảnh trong DB; không tạo ảnh giả cho địa danh này.

Build thành công, 62 unit test, 62 kiểm tra API regression (bao gồm 2 kiểm tra lọc mới) và 2 instrumentation test ảnh/trạng thái đạt. UI test dùng danh sách fixture để hiển thị đồng thời trạng thái khóa/mở khóa và URL ảnh thật của Văn Miếu sau khi tải dữ liệu API thật; không sửa quyền khám phá trong DB để tạo hình ảnh minh họa. Native probe xác nhận cùng URL decode được trên Android.

Bằng chứng: output/dev-v2/verification/destination-preview-api.txt, destination-preview-android.txt, destination-photo-lock.png. Ảnh screenshot là fixture trình bày trạng thái, không phải thành quả thật của tài khoản người dùng. APK output/dev-v2/bcv-local-v2-debug.apk đã cài emulator-5554. Backend local kiểm thử 5084; khi bàn giao trả mapping app về 5082 và dừng dịch vụ kiểm thử. Cần restart backend VS Code để áp dụng tham số lọc mới.
