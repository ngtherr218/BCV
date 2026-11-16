# Khảo sát hệ thống — 11/11/2026

## Phạm vi khảo sát

Đã kiểm kê 595 file mã nguồn/config, Android resource, SQL và test; danh sách, số dòng và SHA256 ở [repository_inventory.md](repository_inventory.md). Đã phân tích các luồng account/onboarding, discovery/recommendation, journey/step sync, card/unlock, quest, leaderboard, admin và cấu hình chạy. Kiểm kê tĩnh không đồng nghĩa mọi nhánh đã được kiểm thử động.

## Công nghệ và kiến trúc

| Thành phần | Công nghệ / trách nhiệm | Cách chạy local |
|---|---|---|
| Backend | ASP.NET Core .NET 10, modular monolith; Npgsql 10, SQL tham số hóa; không EF/ORM migration | backend/run-dev-v2.ps1; cổng 5082 |
| Database | PostgreSQL 18 + PostGIS; baseline/migration SQL thủ công và các EnsureAsync cũ | localhost:5432/vietnam_explorer_dev_v2 |
| Android | Kotlin, Activity/ViewModel/LiveData, SDK 37/minSdk 27 | JDK Android Studio; build-android-dev-v2.ps1 |
| Admin | React 19/Vite 7, API proxy | cd web; npm run dev; cổng 5173 |
| Recommendation | Python/scikit-learn K-Means offline, 35 đặc trưng; backend P/C/A/R/N | Backend dùng cấu hình/cụm đã nhập, không cần gọi cloud/Python service khi gợi ý |

Goong và Health Connect/cảm biến là tích hợp ngoài/thiết bị theo kiến trúc có sẵn. API, database và admin do nhóm chạy local. Profile v2 tắt startup migrations, SMTP, R2 và media maintenance; session file riêng. Không deploy production.

## Database: bằng chứng live

Nguồn local bcv có 26 public tables (25 nghiệp vụ + spatial_ref_sys); target v2 có 34. Rà soát READ ONLY/REPEATABLE READ xác minh current_database và transaction_read_only trước khi đọc. Snapshot ban đầu không bị ghi đè; source_recheck và after được chụp mới. Schema và số bản ghi từng bảng nguồn khớp baseline.

Delta v2 đã tồn tại gồm 8 bảng, 6 cột, CHECK của cột mới và partial UNIQUE index trên recommendation_config. Các hồ sơ ngày 10/11 ghi nhận hai phương án đã phê duyệt/thực thi; xem database_changes_proposal.md, local_schema_compatibility_proposal.md, migrations_executed.md. Không chạy migration mới trong lượt này.

Bảng/cột/PK/FK/NULL/default/index cũ được bảo toàn theo verifier, trừ biểu diễn CHECK journey tương đương do PostgreSQL deparser sau restore; giữ nguyên hai snapshot để đối chiếu, không ALTER sửa nguồn. Đây là ngoại lệ biểu diễn được báo cáo, không tuyên bố schema dump byte-for-byte giống nhau.

V2 đã có dữ liệu nhập theo yêu cầu trước đây; không nhập thêm user thật trong lượt này. Không tạo lại database, không DROP/TRUNCATE/restore/import. Các thao tác ghi kiểm thử chỉ trên v2, dùng tài khoản example.invalid và bản nháp QA riêng.

## Xác thực, phân quyền và giao dịch

- Account dùng Bearer token qua ISessionStore; FileSessionStore lưu hash token. Đăng ký công khai không cấp ADMIN. Admin filter kiểm tra user ACTIVE/role trong DB; khóa tài khoản thu hồi phiên.
- Discovery/passport/ranking đòi đăng nhập; không nhận user ID/điểm tùy ý từ client. Leaderboard trả alias, không email/tên thật/tọa độ cá nhân. Route inventory ở api_inventory.md.
- Journey khóa dữ liệu và đồng bộ tổng tích lũy trong transaction; bài test gửi lặp/hoàn thành đồng thời kiểm tra chống cộng trùng. Không sửa reconciliation/nguồn sensor.
- PlaceUnlock dùng discovery/user_card uniqueness; thưởng thẻ và passport chỉ khi hoàn thành hợp lệ. Preview không ghi thưởng.
- Quest dùng QuestCycles/QuestProgress cũ và UNIQUE(user,quest,cycle); GET quests có thể chi thưởng, không mô tả là API đọc thuần.
- ExplorerScoreEvents ghi transaction, uniqueness theo user/type/source; không tính ngược lịch sử. Vé bay giữ mở khóa/thẻ cũ nhưng không sinh điểm đi bộ.
- Admin CRUD tái sử dụng catalog cũ; trang Khám phá & phần thưởng hiện có chỉ theo dõi/đối soát và mở editor cũ, không tạo editor thứ hai.

## Các thành phần tái sử dụng và ảnh hưởng

| Chức năng | Backend/database tái sử dụng | Android/admin |
|---|---|---|
| P0.1 | PlaceSearchRepository, place/region/type/features/profile | Tìm nâng cao, paging, kết quả bản đồ và chọn đích; dùng catalog cũ |
| P0.2 | Model/score P/C/A/R/N giữ nguyên, selector 2+1 | Đọc reasons hiện có; sở thích không khóa lựa chọn |
| P0.3/P0.4 | Explore detail, StepConversion, activity settings | Preview/steps/days/difficulty; tách quyền gallery/thưởng |
| P0.5/P0.6 | Card/UserCard/discovery/collection | Bộ sưu tập/hộ chiếu; badge suy ra, không ownership mới |
| P1.1/P1.2 | Ledger đã có, Quest/QuestProgress/chu kỳ ISO | Ranking/weekly quests; admin cấu hình quest cũ |
| P1.3 | Journey confirmed progress + ảnh preview | Mở 4 mảnh ảnh ở 25/50/75/100; không phát vật phẩm |

## Phát hiện và quyết định

1. Hướng dẫn cũ ghi kappa=0,1 hoặc 0,15 nhưng code StepConversion là 1. Đã sửa mô tả theo code; không đổi công thức hoặc requiredSteps đã lưu.
2. Recommendation README ghi 1–6 sở thích nhưng validation thực là 1–3. Đã sửa tài liệu; không đổi validation.
3. Android Gradle đặt build directory quá muộn, dẫn tới ADB lỗi đường dẫn Unicode và BUILD SUCCESSFUL không chứng minh cài/test. Helper đã chuyển sang beforeProject và lấy APK từ output ASCII.
4. Emulator 10.0.2.2:5082 timeout trên môi trường đã kiểm tra; localhost qua adb reverse hoạt động. APK demo hiện dùng 127.0.0.1:5082, cần reverse mỗi khi kết nối lại.
5. P1.3 trước chỉ có phần trăm; đã bổ sung ghép ảnh preview theo confirmed progress. Bitmap/render test kiểm tra mở đúng tile, gửi lặp và reset không lộ ảnh.
6. Báo cáo AGP biểu diễn AssumptionViolatedException thành failure. Giữ XML nguyên trạng và đối chiếu runner trực tiếp; không dựa riêng vào exit code Gradle.
7. Hai test framework cũ Bcv.StepChecks/Bcv.AdminDeleteChecks tự tạo/đổi schema. Không chạy chúng trên v2 theo yêu cầu không thêm schema; dùng API fixture trong schema hiện có.

## Giới hạn chưa khép

Chưa đo bước thật/Health Connect/GPS trên điện thoại vật lý; chưa kiểm thử toàn bộ media moderation/upload; chưa chạy Python suite do chưa tìm được interpreter Python thực (PATH chỉ có WindowsApps alias). 10 card đã có minh họa biểu tượng văn hóa hình học tự tạo bằng script local và được lưu/read-back qua API admin; cấu hình cũ được sao lưu. P2 chưa bắt đầu. Xem implementation_report.md cho trạng thái nghiệm thu và bằng chứng thực thi.

## Artwork và guard local bổ sung

backend/configure-cultural-card-art-dev-v2.ps1 tạo 10 JPEG bằng mã vẽ hình học trên máy; -Apply kiểm tra identity v2, sao lưu cấu hình cũ và gọi API quản trị hiện có. Không cấp ownership, không dùng cloud hoặc thêm schema. Đã kiểm tra ảnh trong editor web. Health ở Development trả thêm database từ current_database() để script xác minh target trước login/register/fixture write; ngoài Development giữ phản hồi cũ.

> Cập nhật sau yêu cầu gỡ video 11/11/2026: đã xoá place_video và dữ liệu video cộng đồng trên v2, giữ ảnh/kiểm duyệt ảnh. V2 hiện 33 bảng; không còn upload/player/duyệt video trên API, Android hoặc web. Các phần mô tả video trước cập nhật này là lịch sử. Xem [báo cáo gỡ video](video_removal_report.md).
