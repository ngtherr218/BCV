# Kết quả kiểm thử hệ thống điểm v3

Ngày kiểm tra: 12/11/2026 (giờ Việt Nam). Môi trường: backend local 5084, web 5174, PostgreSQL vietnam_explorer_dev_v2, Chrome và Android emulator-5554. Không triển khai production.

## A. Chức năng đã sửa

- Một điểm khám phá DISCOVERY thay cho JOURNEY + FIRST_PLACE trong các lần ghi mới; mặc định 150, lấy từ cấu hình, chỉ khi khám phá lần đầu bằng hoàn thành hành trình đi bộ hợp lệ.
- Mỗi quest có reward_points riêng; hỗ trợ mức 0, chỉ vật phẩm, chỉ điểm, cả hai hoặc không phần thưởng. Admin sửa điểm sau khi có tiến độ mà không đổi điểm lịch sử.
- Snapshot awarded_points/points_settled lưu cả 0 tại thời điểm hoàn thành. Ghi điểm và thay đổi hành trình/khám phá/nhiệm vụ trong transaction, unique key chống trả lặp.
- Migration 028 và seed 7 nhiệm vụ đã chạy trên target local, có kiểm tra chạy lại an toàn. Giữ nguyên ledger, tổng lịch sử và nguồn bcv.
- API danh sách/chi tiết/tạo/sửa nhiệm vụ có reward_points; app có mức hiện tại và thực nhận; bảng xếp hạng vẫn SUM cùng ledger. Lịch sử admin thêm nguồn/phiên bản/tên nhiệm vụ.

## B. Giao diện

- Web: form điểm khám phá + khối liên kết quản lý nhiệm vụ; bỏ ô điểm nhiệm vụ chung và hai ô khám phá cũ.
- Tạo/sửa nhiệm vụ: 3 nhóm, điểm riêng, gợi ý nhanh, mục tiêu theo đơn vị, vật phẩm tùy chọn, tóm tắt và khóa gửi lặp.
- Danh sách: cột điểm, lọc chu kỳ cùng tìm kiếm/trạng thái/phân trang.
- Android: điểm, vật phẩm và trạng thái nhận tách rõ; thẻ có nhãn ngày/tuần, tên dài xuống dòng, điểm thực nhận được giữ nguyên.
- Thành quả khám phá: một dòng +N điểm khám phá theo API và thẻ đủ điều kiện. Kiểm thử cuối chọn địa danh có thẻ ACTIVE, xác nhận backend cấp thẻ và popup thật.

## C. Kết quả cuối

| Thành phần | PASS | FAIL | BLOCKED | NOT RUN |
|---|---:|---:|---:|---:|
| Backend | 120 | 0 | 0 | 0 |
| Database | 5 | 0 | 0 | 0 |
| Web Admin | 14 | 0 | 0 | 0 |
| Android | 57 | 0 | 0 | 1 |
| E2E | 19 | 0 | 0 | 0 |

Đơn vị tính là ca/nhóm xác nhận đã chạy, không cộng tất cả hàng thành tổng vì E2E là tập con của Web/Android. Backend gồm 43 kiểm tra cơ chế v3 + 52 regression khám phá + 22 admin + 3 nhóm checks logic hiện có. Database gồm 2 nhóm SQL toàn vẹn/idempotency + 3 nhóm đối chiếu schema/nguồn. Android gồm 52 unit tests + 5 instrumentation tests; E2E gồm 14 kiểm tra trình duyệt và 5 instrumentation. Android NOT RUN 1 là luồng bước chân cảm biến/Health Connect vật lý, không được thay bằng kết quả API synthetic. Bộ StepChecks yêu cầu DB riêng không được chạy trên v2; hành vi điểm/vật phẩm tương ứng đã được kiểm tra qua API local.

Bằng chứng:

| Bộ kiểm tra | Tệp kết quả |
|---|---|
| Cơ chế v3 | output/dev-v2/verification/points-v3-api.txt |
| Regression khám phá/thẻ/quest/ranking | output/dev-v2/verification/points-v3-regression.txt |
| Quản trị | output/dev-v2/verification/points-v3-admin.txt |
| Logic backend | output/dev-v2/verification/points-v3-backend-checks.txt |
| SQL hash/snapshot/unique/seed | output/dev-v2/verification/points-v3-database.txt |
| Source/schema audit | docs/technical/schema_comparison_results.md |
| Chrome | output/playwright/points-v3-results.json |
| Android | output/dev-v2/verification/points-v3-android.txt, OK (5 tests) |
| Unit Android | C:/Users/Admin/AppData/Local/Temp/bcv-dev-v2-android-build/app/test-results/testDebugUnitTest |

Backend, Vite production và Android APK build đều thành công. APK output/dev-v2/bcv-local-v2-debug.apk đã cài lên máy ảo. git diff --check không có lỗi whitespace tại thời điểm kiểm tra cuối.

## D. Lỗi phát hiện và xử lý

- Hai nguồn điểm luôn cùng phát sinh: thay bằng DISCOVERY duy nhất cho dữ liệu mới.
- Điểm nhiệm vụ chung: thay bằng reward_points và snapshot thực nhận; không còn runtime đọc mức chung.
- Chỉ thưởng điểm nhưng reward_status vẫn chưa xử lý vì không có vật phẩm: chốt trạng thái mà không insert item null, giữ cơ chế item trong transaction.
- Giá trị 0 có thể bị hiểu là chưa từng trả điểm rồi trả ngược sau khi sửa: dùng points_settled để phân biệt rõ.
- Payload config cũ có thể bị bỏ qua và rơi về default: JsonRequired bắt buộc discoveryPoints/revision, test 400.
- Harness ban đầu dùng alias Start của PowerShell và không đổi đích khi mô phỏng từ bỏ: sửa fixture NewQaJourney và loại đích đang đi. Không phải lỗi nghiệp vụ của app.
- Browser test ban đầu không truy cập Vite trong sandbox: chạy dịch vụ kiểm thử phù hợp, Chrome chạy lại đạt. Chụp ảnh sau animation để tránh ảnh giữa chuyển cảnh.
- Espresso ban đầu chọn cửa sổ activity sau dialog mất focus: dùng isDialog đúng cửa sổ. Sau restart backend có một lượt mất ADB reverse/timeout: khôi phục mapping và chạy lại. Kết quả cuối 5/5 đạt, không dùng lượt lỗi làm PASS.
- Console phát hiện favicon.ico 404: bổ sung favicon từ icon Footprints đang dùng và giấy phép sẵn có; kiểm tra lại không có page error hoặc console error ngoài lỗi 503 cố ý mô phỏng. Kiểm thử bấm lưu hai lần xác nhận chỉ một POST tạo nhiệm vụ; lọc trạng thái cũng đạt.
- Không còn lỗi chức năng chưa sửa trong phạm vi đã kiểm tra. Còn giới hạn môi trường: NuGet NU1900 không tải được feed kiểm tra lỗ hổng; build vẫn thành công, chưa thể khẳng định kết quả vulnerability audit. Chưa kiểm tra cảm biến thật, production hoặc mọi thiết bị Android.

Tài khoản fixture dùng example.invalid. Cấu hình khám phá được khôi phục về trước test, các nhiệm vụ QA được chuyển INACTIVE; 7 nhiệm vụ mẫu giữ ACTIVE. Nhiệm vụ và ledger cũ không bị xóa/đổi điểm. Backend/web kiểm thử dùng cổng riêng; phiên VS Code cổng thường dùng cần restart backend mới sau khi nâng cấp.

## E. Cập nhật tài liệu đồ án

Không tìm thấy bản Phụ lục A gốc dạng Word/PDF trong repository; bảng sau đối chiếu mã F do yêu cầu cung cấp với hành vi source đã khảo sát. Đây là nội dung đề xuất để cập nhật đặc tả chính thức, không phải trích nguyên văn tài liệu chưa được cung cấp.

| Mã | Hành vi cũ trong phạm vi khảo sát | Nội dung cần bổ sung/cập nhật |
|---|---|---|
| F14 | Tạo hành trình đến địa danh hợp lệ; chặn địa danh đã khám phá | Không cộng điểm khi tạo; kiểm tra trước thay hành trình; server từ chối chọn lại cả đích mở bằng vé bay |
| F15 | Tiến độ từ bước đã xác nhận, mốc trung gian không cấp thẻ | Chưa đủ tiến độ không cộng điểm khám phá; request lặp không nhân tiến độ/điểm |
| F16 | Hoàn thành đi bộ có hai sự kiện điểm 50 + 100 trong bản mở rộng cũ | Một DISCOVERY từ cấu hình, cùng transaction với hoàn thành/khám phá/thẻ/nhiệm vụ; trả đúng số điểm thực nhận |
| F18 | Mở khóa bằng đi bộ hoặc vé bay | Chỉ lần đầu bằng hoàn thành đi bộ có điểm khám phá; vé bay giữ mở khóa nhưng không cấp điểm này |
| F20 | Cấp thẻ tương ứng nếu ACTIVE và chưa sở hữu | Cấp thẻ độc lập với mức điểm 0; giữ unique ownership và rollback khi giao dịch hoàn thành thất bại |
| F22 | Xem nhiệm vụ ngày/tuần và phần thưởng vật phẩm; bản mở rộng từng hiện mức điểm chung | Hiện reward_points riêng, điểm thực nhận khi hoàn thành, phân biệt điểm/vật phẩm/tiến độ/chu kỳ |
| F23 | Tiến độ tích lũy theo điều kiện và chu kỳ | Chốt điểm khi chuyển hoàn thành, snapshot kể cả 0, khóa cấu hình khi đọc; unique chu kỳ |
| F24 | Cấp vật phẩm theo reward_status | Điểm và vật phẩm độc lập; điểm ghi tại hoàn thành một lần/chu kỳ, item vẫn cấp idempotent; sửa cấu hình không sửa quá khứ |
| F43 | Tạo quest với chu kỳ, điều kiện, mục tiêu và vật phẩm tùy chọn | Thêm description và reward_points 0..100000, validation số nguyên; biểu mẫu 3 nhóm và tóm tắt |
| F44 | Quest có tiến độ chỉ sửa tên/trạng thái | Cho sửa thêm mô tả/điểm cho lần hoàn thành tiếp theo; giữ điều kiện/chu kỳ/vật phẩm khi đã có tiến độ |

ERD thêm các cột ở POINT_MIGRATION.md, không tạo ledger mới. DFD bổ sung lưu snapshot điểm và truy vấn sổ điểm hiện có. Bảng xếp hạng là phần mở rộng, giữ cơ chế tuần/vùng và dữ liệu riêng tư hiện tại. Không thay recommendation model, chức năng ảnh/kiểm duyệt ảnh hoặc module video trong lần sửa này.
