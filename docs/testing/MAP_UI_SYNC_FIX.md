# Sửa hiển thị điểm và công cụ bản đồ theo BCV-ther

- `GoongExploreMap` và `PlacePhotoMarker` dùng cách vẽ từ BCV-ther: ghim ảnh có mũi neo ở tọa độ, ảnh xám/khóa khi chưa khám phá, ảnh màu/viền xanh/dấu tích khi đã khám phá; chấm xám/xanh khi chưa có ảnh; tên theo mức zoom và ưu tiên hiển thị địa danh đã khám phá.
- Giữ sửa lỗi v2 về cập nhật tiến độ khi thiết bị tắt animation.
- Bỏ cụm zoom +/− được tạo chồng lên các nút bên phải ở Khám phá; vẫn phóng/thu bằng thao tác bản đồ.
- Bỏ nút test ở tiêu đề Khám phá; chỉ giữ một nút test 48dp trên bản đồ trong bản debug.
- Công cụ bản đồ nằm trong một cụm, có khoảng cách; tự chuyển thành hàng ngang khi bản đồ còn ít chiều cao. Khi bảng hành trình che gần hết bản đồ, cụm tạm ẩn và hiện lại khi thu gọn bảng. Thẻ vị trí và nhãn bộ lọc cũng tránh vùng nút.
- Kết nối các trạm câu hỏi với tuyến hành trình; chạm trạm mở đúng câu hỏi tương ứng.
- Thêm nút Quay lại ở đầu màn Hành trình, dùng cùng xử lý back của hệ thống.
- Sửa thứ tự khởi tạo MapLibre trước khi tạo MapView ở Hành trình để không phụ thuộc màn Khám phá đã chạy trước đó.

Kiểm tra: build debug và unit tests; kiểm thử thiết bị về màu/trạng thái marker, nút back, bố cục 360×740, 740×360, 320×480 và mở/thu gọn bảng che bản đồ. Các kiểm thử được chọn không tạo tài khoản và không sửa database.

APK: `output/dev-v2/bcv-local-v2-debug.apk`.

Kết quả xác nhận cuối: build debug và unit tests thành công; cả 7 kiểm thử thiết bị đều PASS. Báo cáo: output/dev-v2/map-ui-device-results.txt. Không tạo tài khoản và không thay đổi database trong đợt sửa này.
