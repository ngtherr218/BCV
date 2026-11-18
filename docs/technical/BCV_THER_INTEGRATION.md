# Tích hợp BCV-ther vào BCV v2

Đích triển khai: repository BCV-main_v2/BCV-main. BCV-ther được dùng làm nguồn tham khảo và nguồn bộ câu hỏi; không thay cấu trúc nghiệp vụ của v2 bằng schema BCV-ther.

## Database

Chỉ bổ sung vào database `vietnam_explorer_dev_v2`; không chạy script trên database `bcv`. Script `backend/invoke-dev-v2-sql.ps1` kiểm tra host local và tên database trước khi ghi.

```powershell
.\backend\invoke-dev-v2-sql.ps1 -SqlFile sql/upgrade-culture-quiz.sql
```

Migration 031 thêm `place_question`, `journey_quiz_answer`, `user_place_mastery`, cột tương thích `journey.bonus_steps`, loại sự kiện QUIZ/MASTERY trong sổ điểm v2 và nhiệm vụ QUIZ_CORRECT. Không nhập các migration xóa community media của BCV-ther. Seed chứa 794 câu hỏi cho 311 địa danh gốc; chỉ thêm câu chưa tồn tại, không ghi đè nội dung quản trị đã sửa. Cài mới và nâng cấp toàn bộ đã bao gồm migration/seed này.

Database local v2 đã được áp dụng migration thành công trong phiên triển khai này. Hãy sao lưu database trước khi nâng cấp môi trường khác.

## App và web

- Thanh điều hướng đồng bộ BCV-ther: Khám phá, Bộ sưu tập, Thử thách, Xếp hạng. Hành trình chuyển thành màn chi tiết mở từ Hồ sơ hoặc tiến độ trên bản đồ.
- Màn Hành trình có nút Trạm câu hỏi hiển thị số câu đang mở hoặc số câu đã trả lời. Màn câu hỏi dùng thiết kế BCV-ther: tuyến hành trình, các trạm khóa/mở, đáp án, giải thích và điểm nhận được.
- Mỗi câu đúng nhận 20 điểm; đúng toàn bộ trạm nhận thêm 50 điểm và ghi dấu Am hiểu một lần theo địa danh. Trang chi tiết thẻ hiển thị nhãn Am hiểu; quyền sở hữu thẻ vẫn theo hành trình khám phá của v2.
- Trạm mở tại `(i+1)/(N+1)` tổng bước yêu cầu, làm tròn lên. Câu hỏi không cộng bước đi. Mỗi trạm chỉ được trả lời một lần.
- Web có danh mục Câu hỏi văn hóa, bộ soạn bốn lựa chọn và đáp án đúng, độ khó, giải thích, trạng thái; nhiệm vụ hỗ trợ điều kiện trả lời đúng câu hỏi văn hóa. Dashboard có lối vào ngân hàng câu hỏi.
- Đồng bộ cỡ chữ trực tiếp từ BCV-ther cho 24 layout tương thích: đăng nhập, hồ sơ, thẻ, lịch sử, thống kê, Health Connect, vật phẩm và các trạng thái tải.
- Phong cách dùng xanh rừng, xanh sage và vàng của BCV-ther; giao diện quản trị và các cơ chế ảnh, xếp hạng, phần thưởng hiện có của v2 được giữ để tương thích dữ liệu.

## API

- `GET /api/journeys/current/quiz`: các trạm của hành trình đang đi; trạm khóa không lộ nội dung, câu chưa trả lời không lộ đáp án.
- `POST /api/journeys/current/quiz`: `{key, answer}`; kiểm tra phiên/tài khoản hoạt động, tiến độ bước, lựa chọn hợp lệ và khóa giao dịch để chống thưởng trùng.
- `GET /api/engagement/mastered-places`: danh sách địa danh đã Am hiểu của người dùng.
- CRUD quản trị: `/api/admin/data/questions`.

## Kiểm thử

```powershell
.\backend\test-culture-quiz-dev-v2.ps1
.\backend\build-android-dev-v2.ps1
cd web
npm run build
node --test tests/*.test.mjs
```

Bộ API checks chỉ chấp nhận database local `vietnam_explorer_dev_v2`. Dữ liệu hành trình/câu hỏi/điểm dùng schema tạm; tài khoản fixture được tạo qua API do repository tài khoản có tên bảng public cố định, và được dọn cùng thông báo sau kiểm thử. Không dùng database `bcv`.

Kết quả ban đầu: backend build thành công, 26 kiểm tra API thành công, Android unit tests và assembleDebug thành công, web build và 11 kiểm thử thành công. NuGet không lấy được dữ liệu kiểm tra lỗ hổng từ mạng trong môi trường này (NU1900), không gây lỗi biên dịch.

APK local: `output/dev-v2/bcv-local-v2-debug.apk`. Backend local đã được khởi động lại trên cổng 5082 bằng bản có bộ lọc câu hỏi mới. Kiểm thử thao tác và bố cục trên thiết bị Android thực tế là bước xác minh riêng với build/unit tests.

Kiểm tra giao diện web bằng Playwright với dữ liệu mô phỏng ở desktop và mobile 390px: biểu mẫu hiển thị và không tràn ngang. Ảnh kiểm tra nằm trong `output/culture-questions-desktop.png` và `output/culture-questions-mobile.png`.

## Hoàn thiện bộ lọc và giao diện (17/11/2026)

- Tìm câu hỏi bằng nhiều từ, có dấu hoặc không dấu, không phân biệt hoa thường; bao gồm tên địa danh, đáp án và giải thích. Ký tự % không được xem là wildcard.
- Lọc kết hợp khu vực → địa danh, độ khó, trạng thái; đổi khu vực xóa địa danh cũ. Sắp xếp mới/cũ, theo độ khó hoặc địa danh. Phân trang và CSV dùng chung bộ lọc.
- Lỗi lọc khu vực trả trống xảy ra khi web dùng bộ lọc mới nhưng backend trên 5082 vẫn chạy binary cũ. Đã build riêng và khởi động lại API đúng database v2. Kiểm tra chỉ đọc trên dữ liệu thật: vùng 25 có 10 câu, vùng 25 + địa danh 1 có 2 câu. Lệnh kiểm tra: `backend/test-culture-quiz-dev-v2.ps1 -ReadOnlyFilters`.
- Playwright xác nhận 8 tình huống giao diện lọc, gồm đổi vùng, lọc địa danh, reset, tìm kiếm/độ khó/sắp xếp, CSV và bố cục 390px; ảnh trong `output/playwright/question-filters-*.png`.
- App dùng bố cục bản đồ, cụm công cụ, header, bộ chuyển mục bộ sưu tập, màn chi tiết địa danh thống nhất và podium xếp hạng từ BCV-ther. Giữ ảnh đóng góp, dấu khám phá, quy tắc điểm và dữ liệu v2. Xếp hạng dùng bảng điểm v2 với chọn tuần, không thêm các loại điểm chưa tồn tại.
- Đã kiểm tra trên emulator các màn Khám phá, Bộ sưu tập, Thử thách và Xếp hạng; ảnh nằm tại `output/bcv-*.png`.

Xác minh cuối: APK debug build/unit tests thành công; APK cài và mở launcher trên AVD Medium_Phone_API_37.0, không có lỗi AndroidRuntime khi khởi động. Web local 5173 và API 5082 đều xác nhận database `vietnam_explorer_dev_v2`. Không kiểm thử cảm biến bước thực tế trong đợt chỉnh UI này.

## Bộ sưu tập và Thử thách hoàn chỉnh (17/11/2026)

Đã ghép ba mục Thẻ / Hộ chiếu / Nhật ký và bổ sung chuỗi ngày, tạm nghỉ, lịch bảy ngày, lưới huy hiệu theo BCV-ther. Hộ chiếu dùng khu vực và lịch sử khám phá v2; Nhật ký có ghi chú và ảnh riêng theo chủ tài khoản. Migration 032 thêm bốn bảng, không sửa database `bcv`. Bước hàng ngày không cộng trùng Health Connect và hành trình; điểm vẫn theo quy tắc v2.

Backend build, 45 kiểm tra API, Android unit tests và bốn kiểm tra giao diện emulator đều qua. Đã xem ảnh và sửa lỗi chiều cao đường phân cách Hộ chiếu. Dọn sạch fixture: còn 72 người dùng, không email đuôi invalid, không tài khoản/schema kiểm thử. Chi tiết: [COLLECTION_CHALLENGE_UI_SYNC.md](../testing/COLLECTION_CHALLENGE_UI_SYNC.md).
