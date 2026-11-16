# Chạy và trình diễn local v2

**Cập nhật kiểm thử 11/11/2026:** đã build source hiện tại, 53 unit test Android đạt; runner emulator báo `OK (41 tests)` gồm hai assumption không thực thi. Web có 11 unit test đạt, build thành công và đã smoke test bằng Playwright. Xem [báo cáo và quy trình](android_web_verification_2026-11-11.md). APK hiện dùng `http://127.0.0.1:5082/` qua `adb reverse`; chưa kiểm thử cảm biến trên điện thoại thật.

## Trạng thái trên máy hiện tại

- Database: localhost:5432/vietnam_explorer_dev_v2, user postgres. Mật khẩu trong root .env.local bị Git bỏ qua.
- Backend: http://127.0.0.1:5082; /health trả status=ok.
- Web: http://127.0.0.1:5173; proxy /api và /health trỏ về 5082.
- Android debug đã xác minh: http://127.0.0.1:5082/ qua `adb reverse tcp:5082 tcp:5082`. APK ở output/dev-v2/bcv-local-v2-debug.apk. Đường `10.0.2.2:5082` hiện timeout trên máy này.
- Admin demo: xem output/dev-v2/admin-credentials.json trên máy. Không commit/chia sẻ file này; tài khoản chỉ tồn tại trên v2.

Database đã có danh mục tham chiếu từ repository, 10 thẻ văn hóa, nhiệm vụ tuần và các tài khoản example.invalid phục vụ kiểm thử. Điểm/bước của tài khoản test không phải vận động thật. Sau yêu cầu nhập dữ liệu của chủ dự án, user/bước/hành trình nguồn đã được nhập có kiểm soát; xem data_import_report.md.

## Khởi động lại

Tại root repository trong PowerShell:

```powershell
.\backend\run-dev-v2.ps1
```

Giữ terminal chạy. Mở terminal khác:

```powershell
cd web
npm run dev
```

Mở web local, dùng thông tin admin từ file output nêu trên. Không dùng create-admin.ps1/run-local.ps1 mặc định vì chúng thuộc cấu hình database cũ. Script v2 kiểm tra host loopback/tên database, tắt startup migration/cloud SMTP/R2, dùng session file riêng. Media maintenance tắt trong profile này; không có cleanup nền trên database cũ.

## Android

JDK tìm thấy tại E:/AndroidStudio/jbr. Gradle wrapper phải được gọi bằng classpath tương đối vì đường dẫn repository có ký tự Unicode. Helper có output ASCII để unit runner nạp lớp đúng:

```powershell
.\backend\build-android-dev-v2.ps1
```

Helper mặc định dùng thư mục temp ASCII cho build; không thay mã nguồn hoặc công thức bước. Offline mode cần dependency/SDK đã được cache như trên máy hiện tại.

Emulator: cài APK rồi đăng ký một tài khoản mới trên backend local. Điện thoại thật: cùng Wi-Fi với PC, build với IP LAN của PC:

```powershell
.\backend\build-android-dev-v2.ps1 -Api http://192.168.x.x:5082/
```

Thay x.x bằng IP thực; không dùng nguyên ví dụ. Hoặc adb reverse tcp:5082 tcp:5082 rồi build -Api http://127.0.0.1:5082/. Không mở firewall tự động; chỉ cấu hình nếu điện thoại thực sự bị chặn. Goong keys đã có được giữ nguyên trong dev.properties. Goong/Health Connect là tích hợp ngoài theo kiến trúc cũ; backend, database và admin do nhóm chạy local.

Đã cài APK và chạy instrumentation trên emulator Medium_Phone_API_37.0. Kết nối API qua ADB reverse đạt; cảm biến/Health Connect/GPS trên điện thoại vật lý vẫn chưa được kiểm chứng.

## Kịch bản demo

1. Đăng ký trên APK local, chọn 1–3 nhóm sở thích, mức vận động và vị trí ảo.
2. Khám phá → Tìm kiếm và bộ lọc: tên/khoảng cách/ngày/loại/sở thích; bỏ sở thích để chọn địa danh bất kỳ hợp lệ. Danh sách có phân trang; nút bản đồ chỉ hiển thị trang kết quả đang chọn, Xóa lọc khôi phục bản đồ.
3. Xem preview cover/nội dung/ước tính; chọn điểm đến tạo hành trình. Xem trước không nhận thưởng.
4. Vận động và đồng bộ bước thật trên thiết bị; 25/50/75/100 là mốc hiển thị, không phát vật phẩm trung gian.
5. Màn Hành trình mở lần lượt bốn mảnh ảnh preview ở 25/50/75/100% từ tiến độ server; lỗi tải ảnh không ảnh hưởng bước hoặc thưởng. Hoàn thành nhận thẻ, mở Bộ sưu tập → Hộ chiếu, sau đó Bảng xếp hạng tuần/vùng.
6. Nhiệm vụ dùng màn cũ; admin chỉnh thẻ/bộ sưu tập/quest bằng các module cũ. Có sẵn 10 thẻ văn hóa ở seed_dev_v2_cultural_cards.sql.

## Kiểm thử và bằng chứng

```powershell
.\backend\test-dev-v2.ps1
.\backend\test-dev-v2.ps1 -ExerciseAwards
dotnet run --project backend/Bcv.RecommendationChecks --no-restore
dotnet run --project backend/Bcv.DatabaseAudit -- --target
dotnet run --project backend/Bcv.DatabaseAudit -- --recheck-source
.\backend\verify-dev-v2-schema.ps1
```

ExerciseAwards gửi bước fixture qua API thật và tạo tài khoản example.invalid trên v2. Không bật nó để trình bày dữ liệu cảm biến thật. Các fixture được giữ để kiểm tra, không tự xóa dữ liệu hoặc reset database. Kết quả trong local_api_test_results.md/schema_comparison_results.md/local_admin_test_results.md.

## Database và phê duyệt

Không chạy lại CREATE/restore/migration trên database đã có. prepare-dev-v2.ps1 từ chối target tồn tại. Hai SQL đã chạy sau phê duyệt: local_schema_compatibility_proposed.sql và explorer_score_event_approved.sql trong docs/technical. Mọi thay đổi mới vẫn phải đề xuất/duyệt trước. Snapshot nguồn và schema-only dump được giữ nguyên; xem schema comparison trước khi phát triển thêm.

Giới hạn còn lại: Health Connect/sensor/GPS trên điện thoại vật lý; browser CRUD/media moderation đầy đủ; rà soát mỹ thuật artwork; Python suite và P2. Ảnh ghép milestone đã có render test trên emulator. Chưa tuyên bố sẵn sàng nghiệm thu cuối cùng.

## Dữ liệu đã nhập và quản trị mới

Dữ liệu bcv đã được nhập theo yêu cầu sau cùng, gồm 6 tài khoản nguồn và các lịch sử liên quan; admin@gmail.com giữ nguyên thông tin đăng nhập đã yêu cầu. Không cần chạy import lại. Xem data_import_report.md trước mọi lần nhập thêm; tool mặc định chỉ lập kế hoạch. Web: mở http://127.0.0.1:5173/#exploration hoặc menu Trò chơi hóa → Khám phá & phần thưởng. Dùng Ctrl+F5 nếu tab cũ chưa nạp menu mới.

## Tạo lại bộ ảnh thẻ văn hóa

Chạy ./backend/configure-cultural-card-art-dev-v2.ps1 để tạo 10 JPEG trong output/dev-v2/card-art. Chỉ dùng -Apply khi cần cập nhật lại ảnh trên API v2 đang chạy; script sao lưu cấu hình ảnh cũ trước khi PUT. Giữ file before-*.json nếu cần khôi phục bằng editor thẻ/API. Bộ ảnh được vẽ bằng mã trên máy, không tải từ dịch vụ cloud. Đã kiểm thử cấp thẻ sau đổi ảnh. Helper Android mặc định localhost:5082; chạy adb reverse trước khi mở app hoặc Run từ Android Studio.

> Cập nhật sau yêu cầu gỡ video 11/11/2026: đã xoá place_video và dữ liệu video cộng đồng trên v2, giữ ảnh/kiểm duyệt ảnh. V2 hiện 33 bảng; không còn upload/player/duyệt video trên API, Android hoặc web. Các phần mô tả video trước cập nhật này là lịch sử. Xem [báo cáo gỡ video](video_removal_report.md).
