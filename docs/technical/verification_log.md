# Bằng chứng kiểm thử local v2 — 10/11/2026

**Lần cuối chưa thực thi:** Sau build APK và 53 unit test thành công, source có thêm giữ giá trị form tìm kiếm khi tái tạo Activity và dọn assertion lặp/đổi tên test milestone. Lệnh build lại và lệnh HTTP xác minh 401/no-store cuối bị automatic approval review từ chối vì tài khoản chạm usage limit; không phải đánh giá unsafe. Không chạy lại/bypass. APK ở output là bản thành công trước hai chỉnh nhỏ này. Chủ dự án đã chọn chưa kiểm thử thiết bị lúc này.

| Lệnh/kiểm tra | Kết quả thực thi |
|---|---|
|dotnet --version|10.0.402|
|Bcv.DatabaseAudit source + recheck|26 public tables; READ ONLY/REPEATABLE READ; không xuất dữ liệu người dùng|
|Bcv.DatabaseAudit --target|34 public tables sau hai schema được duyệt|
|backend/verify-dev-v2-schema.ps1|PASS bảo toàn nguồn + bảng/cột/constraint/index cũ, chỉ delta đã duyệt; có 1 khác biệt deparse tương đương đã ghi rõ|
|backend/test-dev-v2.ps1 -ExerciseAwards|32 PASS HTTP integration; bao gồm completion đồng thời, thưởng/điểm trùng, flight và chống farm|
|dotnet run --project backend/Bcv.RecommendationChecks --no-restore|25/25 PASS|
|dotnet run --project backend/Bcv.Checks --no-restore|3 nhóm PASS ở khảo sát: sở thích/geocoding policy/classifier 315 labels|
|Android testDebugUnitTest|53 tests, 0 failures/errors; dùng JDK E:/AndroidStudio/jbr, output ASCII|
|Android assembleDebug và helper build-android-dev-v2.ps1|PASS; APK output/dev-v2/bcv-local-v2-debug.apk; URL đóng trong BuildConfig http://10.0.2.2:5082/|
|web: node --test tests/*.test.mjs|11/11 PASS ở khảo sát|
|web: npm run build|PASS Vite 7.3.7 ở khảo sát|
|Admin demo API login/catalog/lookup|7 catalog và lookup PASS; chỉ đọc, không test toàn bộ CRUD|
|HTTP local 5173 + proxy /health|200, status ok|
|adb devices trên host|Không có device/emulator|
|git check-ignore config/secrets|.env.local, web/.env.local, android/dev.properties, output/dev-v2/admin-credentials.json đều bị ignore|

## Các lỗi ban đầu đã điều tra

Thiếu psql/Java trong PATH được xử lý bằng công cụ đã cài trên ổ E, không cài cloud. Kết nối localhost trong sandbox bị chặn, sau khi được auto-review cho phép đã chạy trên host. pg_dump gặp đường dẫn Unicode, sửa filename tương đối. Restore đổi biểu diễn một CHECK tương đương; không ALTER nguồn. Ba test recommendation ban đầu không khớp kappa=1; chỉ sửa fixture/expected của test, không sửa StepConversion. Android ClassNotFound được xử lý bằng output ASCII. Seed gặp lỗi đã rollback transaction trước khi sửa và chạy lại. Query binding mặc định đã sửa và được HTTP test xác nhận.

## Giới hạn

Dữ liệu bước trong ExerciseAwards là fixture synthetic trên tài khoản test example.invalid/v2, không phải Health Connect/cảm biến thật. Không có thiết bị nên chưa chạy connectedAndroidTest, thao tác UI, Goong hoặc đồng bộ ngoài đời. Chưa kiểm thử media moderation/browser CRUD toàn diện, Python suite, thời gian chạy dài ngày hoặc mọi thao tác admin xóa entity có ledger. Không suy diễn kết quả build/unit/API thành nghiệm thu đầy đủ.

## Sau nhập dữ liệu và thêm web quản trị

Đã re-run 32 HTTP checks sau nhập: PASS. Build backend/admin và LocalDataImport PASS; RecommendationChecks 25/25 qua output riêng trong workspace; web build và 11/11 tests PASS. Playwright đã kiểm tra 4 tab, editor thẻ mở/cancel, vùng/loại sự kiện/empty state, nút sang #cards, desktop/mobile. Source recheck/schema verifier PASS; import plan sau cùng 0 INSERT/0 UPDATE, coverage user/history khớp và điểm của user nhập bằng 0. Xem data_import_report.md và admin_management_report.md. Không có schema mới trong lần nhập hoặc bổ sung màn admin này.
