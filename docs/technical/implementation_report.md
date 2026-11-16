# Báo cáo triển khai và kiểm thử — 11/11/2026

## Kết luận hiện tại

Bản local v2 có backend/database/admin và Android kết nối qua adb reverse. P0/P1 đã có luồng API và Android, bổ sung ghép ảnh milestone trong lượt này. Chưa tuyên bố hoàn thiện toàn bộ đồ án hoặc nghiệm thu cảm biến thực. Không có thay đổi schema mới; database cũ vẫn bảo toàn theo đối chiếu live.

## Trạng thái tính năng

| Hạng mục | Trạng thái | Bằng chứng / giới hạn |
|---|---|---|
| Database độc lập/bảo toàn cũ | Đã kiểm thử | 26 bảng nguồn/34 target; source schema và counts khớp baseline; delta khớp allowlist; ngoại lệ deparser CHECK được ghi rõ |
| P0.1 Search/filter | Đã kiểm thử | API kết hợp filter, ngoài sở thích, phân trang không trùng; Activity thật tải kết quả/empty; chọn đích API; API nâng cao chưa hỗ trợ bỏ dấu |
| P0.2 Gợi ý | Đã kiểm thử | 26 logic checks gồm 2+1, valid candidates/fallback/activity; API tối đa 3; chưa có đánh giá người dùng Precision@3/NDCG@3 |
| P0.3 Preview | Đã kiểm thử | API không ghi discovery/thẻ/passport; Android detail tích hợp; quyền gallery/video giữ riêng |
| P0.4 Vận động/effort | Đã kiểm thử | LOW/HIGH đổi ngày nhưng không đổi requiredSteps; difficulty theo cấu hình; không đổi StepConversion |
| P0.5 Card văn hóa | Đã kiểm thử | 10 minh họa biểu tượng tự tạo, lưu/read-back qua API local và hiển thị trong editor web; cấp một lần regression đạt; ảnh cũ có backup |
| P0.6 Passport | Đã kiểm thử | API discovery/stamp một lần/vùng rỗng; Activity thật tải danh sách; badge suy ra từ catalog active |
| P1.1 Ranking tuần/vùng | Đã kiểm thử | Tuần giờ Việt Nam/ISO năm, alias riêng tư, chống farm/vé/đọc lặp; Activity thật tải top/me; không backfill lịch sử |
| P1.2 Weekly quest | Đã kiểm thử | API chu kỳ/thưởng một lần; boundary logic khớp ranking; admin tạo/sửa/xóa draft tuần đạt; chưa chạy mô phỏng toàn pipeline qua nhiều tuần thật |
| P1.3 Milestone ảnh | Đã kiểm thử | 4 tile từ ảnh preview theo 25/50/75/100 confirmed progress; pixel test kiểm tra threshold/repeat/reset; không cấp thưởng giữa chặng |
| Admin cũ | Đã kiểm thử một phần | Đọc 7 catalog + lookup, CRUD draft region/collection/quest, cleanup fixture và no-store; chưa bao phủ mọi CRUD/media |
| P2 Quiz | Chuyển sang hướng phát triển tương lai | Chưa tạo bảng ngân hàng câu hỏi |
| P2 Streak | Chuyển sang hướng phát triển tương lai | Chưa tính chuỗi từ bước hợp lệ |
| P2 Nhật ký ảnh riêng | Chuyển sang hướng phát triển tương lai | Chưa triển khai; không công khai ảnh cá nhân |

## Các thay đổi thực hiện trong lượt này

- Thêm JourneyMilestoneImage và tích hợp vào JourneyActivity, dùng cover preview hiện có. Request phụ được hủy theo lifecycle/khi đổi journey, lỗi ảnh không làm mất tiến độ. Không thêm API/schema/quyền mở khóa.
- QuestCycles.At(instant) cho kiểm tra thời gian xác định; Current vẫn gọi UtcNow rồi tính bằng cùng logic cũ. Thêm boundary test khớp ExplorerWeek ở đầu tuần và đầu năm ISO.
- Thêm JourneyMilestoneImageTest; tái sử dụng LocalExplorerScreensTest qua API thật.
- Thêm test-admin-dev-v2.ps1: xác minh API đang kết nối vietnam_explorer_dev_v2 trước mọi fixture write, kiểm tra các catalog và CRUD bản nháp riêng, xóa đúng các fixture vừa tạo.
- Sửa tài liệu công thức kappa và giới hạn sở thích theo source, cập nhật đủ tám hồ sơ kỹ thuật.

## Kết quả đã thực thi

| Bộ kiểm tra | Kết quả và bằng chứng |
|---|---|
| Database audit/verifier | READ ONLY source/target; PASS schema+counts nguồn và allowlist delta; schema_comparison_results.md |
| API integration | 32/32 đạt; output/dev-v2/verification/local-api-latest.txt |
| Recommendation/tuần C# | 26/26 đạt; output/dev-v2/verification/recommendation-checks.txt |
| Android unit/build | 53/53 đạt; APK assembleDebug thành công; XML trong output/dev-v2/verification |
| Android instrumentation mới | 41 ca được liệt kê; 39 đạt + 2 assumption không thực thi; XML AGP ghi 2 assumption là failure, runner trực tiếp được đối chiếu |
| Admin API regression | 22/22 kiểm tra đạt, gồm dọn ba fixture; output/dev-v2/verification/admin-crud-latest.txt |
| Web trong phiên kiểm thử trước cùng ngày | 11/11 unit + build; Playwright login/dashboard/4 tab/filter Hà Nội/editor card/mobile; xem android_web_verification_2026-11-11.md |

Hai assumption là GPS cần đối số tọa độ kỳ vọng và network diagnostics chỉ cho 10.0.2.2 trong khi APK dùng localhost. Không xem hai ca này là đã kiểm thử thành công. Bằng chứng runner trực tiếp ở output/dev-v2/verification/android-runner.txt; XML gốc không sửa để che lỗi.

Bước trong integration test là synthetic trên tài khoản example.invalid, không phải Health Connect/cảm biến thực. Admin test chỉ dùng bản nháp QA riêng. Không dùng kết quả fixture để thay thế đánh giá hành vi người dùng hoặc tính chính xác cảm biến.

## Schema và dữ liệu

Không tạo database v2 lại vì nó đã tồn tại. Không chạy SQL migration/DDL/restore/import ở lượt này. Snapshot before nguyên gốc được giữ; source_recheck/after chụp live. Các phê duyệt ngày 10/11 được ghi trong hồ sơ lịch sử, không dùng như quyền chạy migration mới. Các tài khoản thật đã được nhập ở lượt trước theo yêu cầu trước đó; lượt này không nhập thêm.

## Hạn chế và công việc trước nghiệm thu hội đồng

1. Chạy điện thoại vật lý: cấp quyền Health Connect/cảm biến, đi bộ thật, offline/nền/mở lại app, đối chiếu bước và GPS.
2. Test đầy đủ CRUD place/type/card và upload/moderation media trên browser bằng fixture được phép; hiện mới kiểm tra editor card ở chế độ xem và CRUD API ba catalog.
3. Rà soát mỹ thuật của bộ 10 minh họa văn hóa với nhóm trước demo; bộ hình đã tạo/cấu hình qua API local, không còn dùng cover tham chiếu làm ảnh thẻ.
4. Chạy Python model suite khi có interpreter/dependency phù hợp; chưa công bố độ chính xác gợi ý cá nhân hóa nếu chưa có chấm thực tế.
5. Kiểm tra Goong trên thiết bị và mạng demo thực; không giả định dịch vụ ngoài chạy offline.
6. Còn favicon 404 trên web và cảnh báo build deprecated/NuGet vulnerability feed không truy cập được; không ảnh hưởng các bài đã chạy nhưng chưa có xác nhận vulnerability online.
7. P2 giữ ở tương lai để ưu tiên ổn định P0/P1. Chưa có thay đổi schema mới chờ duyệt ở cuối lượt này.

## Chạy và demo

Dùng local_v2_runbook.md. APK output/dev-v2/bcv-local-v2-debug.apk cần backend 5082 và adb reverse tcp:5082 tcp:5082. Thứ tự demo: đăng nhập/onboarding → search ngoài sở thích → preview → tạo journey → đồng bộ bước (phân biệt thật/fixture) → milestone → hoàn thành/thẻ → passport → weekly quest/ranking → admin đối soát. Không dùng launcher legacy trỏ database bcv.

## Artwork và xác minh database

Đã thêm configure-cultural-card-art-dev-v2.ps1 (mặc định chỉ tạo file, -Apply mới ghi API). Đã áp dụng ảnh cho đúng 10 thẻ hiện có theo place/name, giữ name/description/status/place link; backup trước thay đổi ở output/dev-v2/card-art/before-*.json. Không thêm ownership. 10/10 read-back image_key khớp, editor web hiển thị ảnh Fansipan (output/playwright/cultural-card-editor.png). Sau cập nhật, 32/32 integration checks vẫn đạt. Health Development bổ sung database thực tế để script/test kiểm tra target trước khi tạo fixture; production contract không đổi.

> Cập nhật sau yêu cầu gỡ video 11/11/2026: đã xoá place_video và dữ liệu video cộng đồng trên v2, giữ ảnh/kiểm duyệt ảnh. V2 hiện 33 bảng; không còn upload/player/duyệt video trên API, Android hoặc web. Các phần mô tả video trước cập nhật này là lịch sử. Xem [báo cáo gỡ video](video_removal_report.md).

> Cập nhật 11/11/2026: chức năng kiểm duyệt đã được gỡ khỏi web quản trị (menu, màn, dialog xử lý và nút khóa/mở quyền gửi). API backend được giữ nguyên. Xem [báo cáo gỡ kiểm duyệt web](web_moderation_removal_report.md).

> Cập nhật: sửa sở thích hồ sơ đủ12loại địa danh thay6nhóm, giữ giới hạn3 và tương thích dữ liệu cũ; xem [báo cáo sửa sở thích](twelve_interests_fix.md). API38/38 và logic27/27 đạt.


> Cập nhật: tìm kiếm/bộ lọc đã tích hợp vào Khám phá tiếp; checkbox ưu tiên sở thích mặc định bật. API41/41, Android52unit và test màn hình emulator đạt. Xem [chi tiết](inline_discovery_search.md).
