# Gỡ chức năng video — 11/11/2026

Thực hiện theo yêu cầu trực tiếp: “xoá các bảng, và chức năng liên quan đến đăng tải và kiểm duyệt video”. Đây là phê duyệt xoá phạm vi video trên local v2, thay cho yêu cầu bảo toàn bảng video trước đó; không áp dụng lên database cũ.

## Database

- Đã sao lưu đầy đủ v2 bằng pg_dump -Fc và xác minh pg_restore --list đọc được: output/dev-v2/before-video-removal-20261111-081037/v2.dump.
- Đã chạy transaction docs/technical/remove_video_approved.sql, có guard current_database() = vietnam_explorer_dev_v2.
- Đã DROP bảng riêng place_video, không dùng DROP CASCADE.
- Đã DELETE community_media có mime_type LIKE video/%. Các báo cáo liên quan được xoá theo FK ON DELETE CASCADE hiện có.
- Giữ community_media và media_report để phục vụ ảnh; không xoá ảnh, thẻ, hành trình, điểm hoặc lịch sử thưởng đã cấp.
- Không thêm bảng/cột/constraint. V2 hiện 33 public tables (32 nghiệp vụ + spatial_ref_sys), database cũ vẫn 26 bảng; verifier schema/counts nguồn đạt.

## Source

- Backend: gỡ route upload/read video chính thức, parser MP4/WebM, gắn video vào địa danh, quota duyệt video, cleanup video và startup migration video. API community upload chỉ nhận JPEG/PNG/WebP tối đa 10 MiB. Admin list/review chỉ phục vụ ảnh.
- Web: gỡ field/upload/player video chính thức, lựa chọn lọc video, player video cộng đồng; đổi nhãn thành ảnh.
- Android: xoá PlaceVideoActivity và khai báo manifest, DTO/parser video, nút video chính thức/cộng đồng, chọn tệp và kiểm tra metadata video. Picker/policy chỉ nhận ảnh. Route video không còn nằm trong whitelist gửi token.
- Giữ tên condition MEDIA_APPROVED để tương thích quest cũ, nhưng từ nay chỉ phát sinh từ ảnh được duyệt; không tính lại hoặc thu hồi thưởng lịch sử.
- SQL migration video cũ được giữ làm hồ sơ lịch sử, đã bỏ embedded resource và lời gọi khởi tạo nên backend không tự tạo lại place_video.

## Kiểm thử đã chạy

- Backend API build: đạt.
- Android APK + unit test: đạt (52 test, giảm một ca sau khi thay ba test policy video bằng hai test policy ảnh/chặn video).
- Web production build và 11 unit test: đạt.
- Bcv.MediaChecks: đạt kiểm tra header, từ chối video/định dạng sai và phân quyền; không dùng database thật.
- API local integration 32/32 đạt; admin regression 22/22 đạt.
- Source database read-only recheck và verifier: đạt; target 33 bảng.
- Chưa chạy lại toàn bộ instrumentation emulator hoặc thao tác duyệt ảnh trên browser trong lượt gỡ video này; không dùng kết quả cũ làm bằng chứng cho hai mục đó.

APK đã cập nhật: output/dev-v2/bcv-local-v2-debug.apk; cần cài lại để giao diện mới có hiệu lực. Backend local 5082 đã khởi động lại với source không có video.

## Kiểm tra ảnh sau khi gỡ video

Đã chạy backend/test-photo-only-dev-v2.ps1 trên fixture example.invalid riêng: ảnh JPEG upload ở trạng thái PENDING, duyệt sang APPROVED, tải nội dung trả 200; MP4/WebM bị chặn 400; hai endpoint video cũ trả 404. Đã gỡ ảnh fixture thành công; tài khoản/hành trình/bước synthetic được giữ làm bằng chứng test. Một lượt test ban đầu phát hiện thiếu tham số $1 trong truy vấn duyệt ảnh; đã sửa và chạy lại trọn luồng đạt. Bằng chứng: output/dev-v2/verification/photo-only-media.txt. Có một ảnh PENDING của lượt test lỗi trước được giữ cùng fixture, không phải ảnh người dùng thật.
