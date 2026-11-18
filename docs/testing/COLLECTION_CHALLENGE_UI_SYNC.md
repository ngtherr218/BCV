# Đồng bộ Bộ sưu tập và Thử thách với BCV-ther

Ngày kiểm tra: 17/11/2026.

## Phạm vi

- Bộ sưu tập dùng ba mục Thẻ / Hộ chiếu / Nhật ký, thông báo thẻ mới ngay trong nội dung và huy hiệu số thẻ chưa xem theo bộ.
- Hộ chiếu dùng bìa xanh, thống kê, dấu đồng/bạc/vàng và danh sách địa danh theo bố cục BCV-ther. Dữ liệu dựa trên khu vực và lịch sử khám phá hiện có của v2, không tự đổi khu vực thành hệ tỉnh hoặc coi khám phá là xác thực GPS.
- Nhật ký có dòng thời gian, ghi chú riêng, ảnh riêng và chia sẻ. Chỉ chủ tài khoản được đọc hoặc sửa; địa danh phải thuộc lịch sử khám phá của người đó. Bộ nhớ đệm giao diện tách theo phiên đăng nhập.
- Thử thách có chuỗi ngày, lịch bảy ngày, tạm nghỉ, nhiệm vụ ngày/tuần và 13 huy hiệu từ tiến độ thật. Điểm thưởng nhiệm vụ vẫn theo cấu hình và sổ điểm v2.
- Bước hàng ngày lấy giá trị lớn nhất giữa tổng Health Connect và tổng bước thô của hành trình, không cộng hai nguồn hoặc hệ số vật phẩm. Huy hiệu không phát thêm điểm.
- Migration 032 chỉ thêm bốn bảng vào `vietnam_explorer_dev_v2`. Không chạy trên database `bcv`.

## Xác minh

- Backend build thành công; 45 kiểm tra API đi qua, gồm quyền riêng tư, tải ảnh đồng thời tối đa 12 ảnh/địa danh, đơn điệu số bước, tạm nghỉ đồng thời và bảo toàn sổ điểm.
- Android unit tests và assembleDebug thành công.
- `CollectionChallengeScreensTest`: bốn kiểm tra trên emulator API 37 với schema và tài khoản tạm, bao phủ Thẻ, Hộ chiếu, Nhật ký, chuỗi ngày và huy hiệu. Fixture được dọn sau kiểm thử.
- Xem ảnh thực tế và sửa đường phân cách Hộ chiếu bị giãn chiều cao; đặt chiều cao lưới dấu/huy hiệu theo nội dung.
- Ảnh: `output/dev-v2/collection-ui/*.png`; kết quả thiết bị: `output/dev-v2/collection-ui-device-results.txt`.
- APK: `output/dev-v2/bcv-local-v2-debug.apk`, API local cổng 5082. Chưa xác minh cảm biến bước trên điện thoại thật.

## Chạy lại

```powershell
./backend/test-culture-quiz-dev-v2.ps1
./backend/build-android-dev-v2.ps1
```

Chế độ `-UiPreview` của script API cung cấp fixture cô lập trên 5085 và chờ cờ `output/dev-v2/collection-ui-finish.flag`, tối đa tám phút. Kiểm thử thiết bị nhận token fixture qua tham số instrumentation; không ghi token vào tài liệu hoặc log kết quả. Tránh runtime khác ghi đè `adb reverse` trong lúc dùng fixture, rồi phục hồi cổng 5082 sau kiểm thử.
