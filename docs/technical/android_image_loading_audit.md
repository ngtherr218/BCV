# Kiểm tra tải ảnh Android – 13/11/2026

## Nguyên nhân xác nhận từ source

RemoteImages trước đây chỉ có LruCache bitmap 16 MB trong RAM; cache key chứa sample. Cùng URL dùng sample 8 (preview), 4 (album/thumbnail), 2 (chi tiết) có thể tải HTTP lại; hai request đồng thời chưa có cơ chế gom theo URL. Đóng tiến trình mất toàn bộ cache. sample chỉ giảm kích thước giải mã bitmap, không giảm dung lượng ảnh tải từ máy chủ.

Album tạo các thẻ và gửi yêu cầu cho toàn bộ ảnh ngay khi render. Semaphore giới hạn 4 request đồng thời nên ảnh cuối phải đợi; URL trùng vẫn có thể chiếm thêm lượt ở bản cũ. Bản sửa lần này giảm tải trùng và bổ sung cache, chưa chuyển album sang RecyclerView/lazy loading.

## Dữ liệu và đo đạc

- Local v2 có 41 thẻ ACTIVE; 10 ảnh data URI, tổng chuỗi ảnh thẻ khoảng 577189 ký tự. Ảnh inline đi cùng JSON, không có thêm request ảnh nhưng vẫn làm response nặng hơn URL đơn thuần.
- 68 ảnh địa danh: 62 URL thumb.wikimedia.org, 6 URL upload.wikimedia.org. Thời gian tải đầu còn phụ thuộc máy chủ ngoài/mạng; không kết luận mọi ảnh đều chậm.
- Hai URL mẫu trên máy tính HTTP 200: 66935 byte / 0,869 giây và 139363 byte / 0,367 giây.
- Một URL thật trên emulator: lần đầu 912 ms, decode kích thước khác từ cùng cache 3 ms, memory hit 0 ms. Đây là đo mẫu, không phải benchmark toàn bộ album hay mọi thiết bị/mạng.

## Thay đổi

- BcvApplication khởi tạo cache ảnh công khai trong cacheDir/public-images-v1.
- Lưu bytes gốc đã decode thành công trên thiết bị: tối đa 48 MiB, TTL 24 giờ; Android có thể dọn cache khi thiếu chỗ. Không ghi cache lúc startup trên main thread.
- Khóa theo URL (64 mutex chia nhóm) ngăn tải trùng đồng thời; các kích thước dùng chung bytes trên đĩa, giữ bitmap cache theo sample để đúng kích thước.
- Chỉ ghi file khi mới tải, không ghi lại mỗi lần đọc. File tạm và rename tránh đọc tệp đang ghi, dọn tệp cũ để giới hạn dung lượng.
- Ảnh cần xác thực giữ luồng kiểm tra token và không được đưa vào cache công khai. Không thay đổi quyền xem media, URL hoặc dữ liệu ảnh trong DB.

## Kiểm chứng

Build APK thành công; 54 unit tests (gồm 2 test cache mới) đạt. Instrumentation: 1 probe ảnh thật + 2 test giải mã JPEG/PNG và dữ liệu lỗi, OK (3 tests).

Log: output/dev-v2/verification/image-cache-android.txt; thời gian: image-cache-probe.txt cùng thư mục. Kiểm tra dữ liệu dùng image-load-audit.sql và image-load-probes.csv. APK output/dev-v2/bcv-local-v2-debug.apk đã được cài lên emulator-5554.

Các hướng cần đo tiếp nếu lần tải đầu vẫn lâu: ảnh gốc lớn trong 6 URL upload.wikimedia.org, độ trễ API trả JSON chứa data URI, chuyển album sang tải theo vùng đang nhìn thấy, và cung cấp thumbnail theo kích thước từ nguồn ảnh. Không khẳng định đã kiểm chứng cold-start của mọi URL hoặc toàn bộ luồng màn hình trong lần kiểm tra này.
