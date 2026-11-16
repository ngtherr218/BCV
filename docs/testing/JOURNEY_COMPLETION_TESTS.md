# Kiểm thử Journey Completion

Ngày: 13/11/2026. Môi trường local vietnam_explorer_dev_v2, backend 5084, emulator-5554 API 37; APK base URL 127.0.0.1:5082 được ADB reverse tới 5084 trong kiểm thử.

## Phạm vi và kết quả thực tế

| Bộ | PASS | FAIL cuối | BLOCKED | NOT RUN |
|---|---:|---:|---:|---:|
| Android unit | 62 | 0 | 0 | 0 |
| Android instrumentation chính | 16 | 0 | 0 | 0 |
| Màn hình nhỏ + font lớn (chạy riêng) | 1 | 0 | 0 | 0 |
| API khám phá/receipt/regression | 60 | 0 | 0 | 0 |
| API điểm/nhiệm vụ/vật phẩm | 43 | 0 | 0 | 0 |

62 unit gồm 8 test validation receipt mới. Instrumentation chính gồm 11 phương thức UI receipt có repository fixture có kiểm soát, 1 luồng API thật (đăng ký/đăng nhập QA, tạo/hoàn thành hành trình, thẻ/điểm thực, nhiệm vụ, leaderboard, thông báo) và 4 test thao tác thông báo. 16 phương thức không đồng nghĩa 16 ca E2E backend độc lập. Ca màn nhỏ lặp lại JC-15 nên không cộng thành 17 kịch bản độc lập.

UI dùng fixture để kiểm soát ổn định trường hợp 0 điểm, không thẻ/ảnh, mạng lỗi, font và vòng đời. Luồng API thật không dùng override repository, không có điểm/thẻ giả. Tài khoản dùng example.invalid và khôi phục session trước test; dữ liệu QA thuộc local v2. Bước chân synthetic; không kiểm tra Health Connect/cảm biến vật lý hoặc production trong lượt này.

## Ma trận yêu cầu

| ID | Trạng thái | Bằng chứng |
|---|---|---|
| JC-01 | PASS | UI receipt và controller mở sau COMPLETED; luồng API thật xác minh thành quả |
| JC-02 | PASS | Controller bỏ qua IN_PROGRESS; validation và API từ chối receipt chưa hoàn thành |
| JC-03 | PASS | UI fixture đúng tên/nhãn; API thật trả thẻ được nhận tại completed_at |
| JC-04 | PASS | card=null ẩn cardSection, không có thông báo thẻ giả |
| JC-05 | PASS | UI +375 fixture và số thực từ ledger API, không hardcode 150 |
| JC-06 | PASS | Fixture rankingPoints=0 hiển thị +0 |
| JC-07 | PASS | Thiếu ảnh có drawable placeholder; không crash |
| JC-08 | PASS | Monitor kiểm tra nút Xem địa danh mở PlaceDetailActivity; extras ID lấy trực tiếp receipt |
| JC-09 | PASS | Monitor kiểm tra Tiếp tục khám phá mở ExploreActivity |
| JC-10 | PASS | Hai lần performClick chỉ một điều hướng; controller nhận kết quả lặp chỉ một màn hình |
| JC-11 | PASS | recreate giữ ViewModel, repository chỉ load một lần; state đã xem giữ nguyên |
| JC-12 | PASS | Đóng rồi mở Activity theo ID tải receipt trở lại; marker prefs còn, API đọc lịch sử khi có hành trình khác |
| JC-13 | PASS | Retry receipt không đổi ledger; marker tồn tại sau đổi token cùng user ID, không mở lặp khi quay lại |
| JC-14 | PASS | animator_duration_scale=0, nội dung alpha=1 và nút hoạt động; khôi phục giá trị trong finally |
| JC-15 | PASS | Font hệ thống thực 1,6 lần, assert fontScale>=1,5; viewport 840×1680px ở 420dpi (~320×640dp), cuộn được tới nút cuối; khôi phục font và độ phân giải |
| JC-16 | PASS | IOException từ repository fixture: chỉ hiện lỗi/thử lại; retry thành công, chưa Ready thì không hiện thành quả |

JC-12 dùng đóng/mở Activity và kiểm tra prefs/đọc receipt bền vững, không force-stop tiến trình đang chạy instrumentation. UI mất mạng là lỗi mô phỏng ở repository, không ngắt mạng toàn thiết bị. Navigation monitor chặn mở activity đích trong test điều hướng để không ảnh hưởng dữ liệu; luồng sử dụng intent sản phẩm thật.

## Backend và regression

60 kiểm tra bao gồm: phiên đăng nhập, owner khác không đọc được, chưa hoàn thành không có receipt, bước/điểm/thẻ thực nhận, mở lại nhiều lần không tăng điểm, lịch sử vẫn đọc được khi có hành trình mới; đồng bộ bước lặp/đồng thời, vị trí/khám phá/passport, thẻ một lần, nhiệm vụ, vật phẩm, thông báo và leaderboard theo tuần/vùng. 43 kiểm tra điểm/vật phẩm bổ sung mức 0, thưởng từng quest, chuyển chu kỳ, snapshot lịch sử, rollback khi cố ý lỗi ledger, retry, vé bay và khôi phục cấu hình.

Endpoint mới chỉ SELECT. Không chạy migration, không thay schema hoặc logic thưởng. Điểm và trạng thái cấu hình được trả về trước test nhờ finally của harness. Không kiểm tra thao tác cảm biến thực, các thiết bị Android khác hoặc theme tối vì app hiện chỉ có theme sáng.

## Log và hình ảnh

- output/dev-v2/verification/journey-completion-ui.txt: OK (16 tests).
- output/dev-v2/verification/journey-completion-small-screen.txt: OK (1 test).
- output/dev-v2/verification/journey-completion-regression.txt: 60 PASS.
- output/dev-v2/verification/journey-completion-points-regression.txt: 43 PASS.
- Unit XML: C:/Users/Admin/AppData/Local/Temp/bcv-dev-v2-android-build/app/test-results/testDebugUnitTest.
- Ảnh API thật: output/dev-v2/verification/journey-completion-real.png.
- Ảnh fixture/nhánh: journey-completion-fixture-full.png, journey-completion-zero.png, journey-completion-error.png, journey-completion-small-font.png trong cùng thư mục.

## Lỗi đã sửa và giới hạn

- Dialog cũ thay bằng Activity; loại animation lặp cũ. Marker từng theo token dễ hiện lại sau đăng nhập: thêm scope backend/user ID cho presentation, giữ chuyển tiếp marker cũ.
- Placeholder vector ban đầu bị phóng to/crop: dùng centerInside/padding cho placeholder, centerCrop/fitCenter khi có bitmap và bỏ tint placeholder.
- Kiểm tra font ban đầu chỉ thay Resources chưa đủ bằng chứng hệ thống: đổi sang settings thật, assert fontScale và chạy thêm viewport nhỏ; tất cả được khôi phục.
- Không có FAIL/BLOCKED cuối trong các ca đã chạy. Cảnh báo NU1900 do không truy cập feed audit NuGet không ngăn backend build. Ảnh nguồn ngoài có thể chậm hoặc lỗi; màn hình có placeholder và vẫn dùng được nút, không chặn hiển thị thành quả khi ảnh chưa tải.

APK bàn giao: output/dev-v2/bcv-local-v2-debug.apk. Cần backend mới có endpoint receipt khi chạy từ VS Code. Màn hình lỗi có thử lại nếu server cũ chưa được restart; không rơi về thành công tự tính.
