# Backend API Bước Chân Việt (*backend*)

ASP.NET Core API (.NET 10) dùng chung cho app Android và web quản trị, kết nối PostgreSQL/PostGIS.

Mã nguồn tổ chức dạng modular monolith: mỗi module trong `Bcv.Api/Modules/` có các lớp `Api`, `Application`, `Domain`, `Infrastructure`.

| Module | Phụ trách |
| --- | --- |
| `Account` | Đăng ký, đăng nhập, phiên, hồ sơ, đặt lại mật khẩu, sở thích, mức vận động |
| `StepLocation` | Điểm xuất phát (vị trí ảo), quy đổi khoảng cách thành bước |
| `Discovery` | Màn Khám phá, chi tiết địa danh, gợi ý điểm đến |
| `Journey` | Tạo hành trình, đồng bộ số bước, lịch sử |
| `PlaceCollection` | Thẻ và bộ sưu tập |
| `QuestItem` | Nhiệm vụ, túi đồ, Giày tăng tốc, Vé máy bay |
| `StatsNotify` | Thống kê cá nhân, thông báo |
| `Admin` | API cho web quản trị (`/api/admin`) |
| `Shared` | Định dạng lỗi API, hàm SQL không gian dùng chung |

## Mục lục

- [Cài đặt](#cài-đặt)
- [Sử dụng](#sử-dụng)
- [Cấu hình](#cấu-hình)
- [Quy đổi khoảng cách thành số bước](#quy-đổi-khoảng-cách-thành-số-bước)
- [Gợi ý điểm đến](#gợi-ý-điểm-đến)
- [Phiên đăng nhập và bảo mật](#phiên-đăng-nhập-và-bảo-mật)
- [API](#api)
- [Đóng góp](#đóng-góp)
- [Giấy phép](#giấy-phép)

## Cài đặt

Cần [.NET SDK 10](https://dotnet.microsoft.com/download) và database `bcv` đã nạp dữ liệu ([db/README.md](../db/README.md)). Từ thư mục gốc kho mã:

```powershell
Copy-Item backend/local.settings.example.json backend/local.settings.json
.\backend\set-local-secrets.ps1
```

Sửa `backend/local.settings.json` theo máy (xem [Cấu hình](#cấu-hình)). `set-local-secrets.ps1` hỏi lần lượt mật khẩu PostgreSQL, SMTP và khóa R2; nhấn Enter để bỏ qua mục tùy chọn.

## Sử dụng

```powershell
.\backend\run-local.ps1
```

API lắng nghe ở `http://0.0.0.0:5081` (nhận cả kết nối từ điện thoại trong cùng mạng). Kiểm tra: http://127.0.0.1:5081/health trả về `{"status":"ok"}`.

| Script (chạy từ thư mục gốc) | Công dụng |
| --- | --- |
| `run-local.ps1` | Build và chạy API với cấu hình của máy |
| `set-local-secrets.ps1` | Lưu mật khẩu database, SMTP và khóa R2 vào biến môi trường của tài khoản Windows. Thêm `-R2` để chỉ đổi khóa R2 |
| `create-admin.ps1` | Tạo tài khoản quản trị web (mặc định `admin@bcv.local`, đổi bằng `-Email`) |
| `local-config.ps1` | Hàm dùng chung để đọc cấu hình; các script khác tự gọi |

`dotnet run` **không** tự đọc cấu hình máy; nếu chạy trực tiếp, phải tự đặt biến `ConnectionStrings__Bcv`.

## Cấu hình

### `local.settings.json`

| Trường | Ý nghĩa | Mặc định |
| --- | --- | --- |
| `DbHost` | Máy chạy PostgreSQL | `127.0.0.1` |
| `DbPort` | Cổng PostgreSQL | `5432` |
| `DbName` | Tên database | `bcv` |
| `DbUser` | Tài khoản PostgreSQL | `postgres` |
| `ApiUrls` | Địa chỉ API lắng nghe | `http://0.0.0.0:5081` |
| `SmtpHost`, `SmtpPort`, `SmtpEnableSsl` | Máy chủ gửi mail | Gmail, `587`, `true` |
| `SmtpUser` | Email gửi mã đặt lại mật khẩu; trống thì không gửi mail, mã in ra log | trống |
| `SmtpFrom` | Địa chỉ người gửi; trống thì dùng `SmtpUser` | trống |
| `PsqlPath` | Đường dẫn `psql.exe` nếu không có trong PATH | trống |

Mỗi trường có thể ghi đè tạm bằng biến môi trường cùng tên dạng `BCV_DB_PORT`, `BCV_API_URLS`, `BCV_SMTP_USER`… Thứ tự ưu tiên: biến môi trường của terminal → biến môi trường của tài khoản → `local.settings.json` → mặc định.

### Mật khẩu và khóa dịch vụ

Không ghi vào file nào trong kho mã; lưu bằng `set-local-secrets.ps1`. Nếu chưa lưu, `run-local.ps1` sẽ hỏi khi chạy.

| Biến môi trường | Bắt buộc | Dùng cho |
| --- | --- | --- |
| `BCV_DB_PASSWORD` | Có | Mật khẩu PostgreSQL |
| `BCV_SMTP_PASSWORD` | Không | Gmail App Password (khi đã điền `SmtpUser`) |
| `BCV_R2_ACCOUNT_ID`, `BCV_R2_BUCKET`, `BCV_R2_PUBLIC_URL`, `BCV_R2_ACCESS_KEY_ID`, `BCV_R2_SECRET_ACCESS_KEY` | Không | Lưu ảnh lên Cloudflare R2; phải đủ cả 5, thiếu thì ảnh lưu trong database |

`BCV_R2_PUBLIC_URL` phải là URL công khai của bucket (`https://pub-….r2.dev` hoặc tên miền riêng), không phải endpoint `….r2.cloudflarestorage.com`.

### Thư mục ảnh trên R2

Ảnh được chia thư mục theo mục đích. Mã nằm ở `Modules/Shared/Infrastructure/ImageStorage.cs`. Mỗi lần tải lên tạo một tên file mới, nên ứng dụng không bao giờ hiển thị ảnh cũ còn trong bộ nhớ đệm.

| Thư mục | Ảnh | Tải lên từ |
| --- | --- | --- |
| `avatars/{userId}/` | Ảnh đại diện người dùng; ảnh cũ bị xóa khi đổi ảnh mới | App, màn Hồ sơ |
| `places/` | Ảnh địa danh | Web quản trị, `POST /api/admin/images?purpose=place` |
| `cards/` | Ảnh thẻ sưu tập | Web quản trị, `POST /api/admin/images?purpose=card` |

Khi chưa cấu hình R2, ảnh được lưu thẳng trong cột ảnh của database dưới dạng data URI.

## Quy đổi khoảng cách thành số bước

Theo mục 3.5.1.2 của tài liệu: **S = max(1, ⌈1000 · D · κ / L⌉)**, với D là khoảng cách (km) từ vị trí ảo tới điểm đến, L = 0,75 m/bước, **κ = 0,15**. Ví dụ 10 km cần 2.000 bước, 50 km cần 10.000 bước.

κ là hệ số tỉ lệ (0 < κ ≤ 1). κ = 1 giữ nguyên quãng đường thật; κ nhỏ hơn thu nhỏ hành trình (tài liệu ví dụ κ = 0,01). Mã: `Modules/StepLocation/Domain/StepConversion.cs`. Số bước được lưu khi tạo hành trình, nên đổi κ chỉ áp dụng cho hành trình mới.

## Gợi ý điểm đến

`GET /api/explore/recommendations` trả tối đa 3 địa danh, theo mục 3.5.4.6 của tài liệu. Phần tính điểm nằm ở `Modules/Discovery/Domain/RecommendationScoring.cs`.

1. **Ứng viên:** địa danh `ACTIVE`, chưa khám phá, cách vị trí ảo trong `distance_limit` của cấu hình gợi ý (500 km).
2. **Điểm thành phần:**
   - **P** (sở thích): tỷ lệ nhóm sở thích người dùng chọn mà địa danh khớp. Sáu nhóm ID 201–206 độc lập với 12 loại địa danh; mapping nằm trong `recommendation_config.settings.interestFeatures`. Khớp khi có ít nhất một đặc trưng trong nhóm.
   - **C** (cụm): trung bình P của các địa danh cùng cụm K-Means (`place_feature.cluster_number`).
   - **A** (vận động): `min(1, T / số bước cần đi)`, với T = 3.000 / 6.000 / 10.000 cho mức Ít / Trung bình / Năng động.
   - **R** (đánh giá): `rating / 5`.
   - **N** (độ phổ biến): `ln(1 + số lượt đánh giá) / ln(1 + N_max)`.
3. **Điểm tổng** theo `recommendation_weight`: **P 0,35 · C 0,15 · A 0,3 · R 0,1 · N 0,1**. Tiêu chí thiếu dữ liệu bị bỏ và trọng số còn lại được chuẩn hóa lại.
4. Sắp xếp giảm dần, hòa điểm thì xét mã địa danh, lấy 3 kết quả đầu.

Trọng số trong `015` là cấu hình thử nghiệm do nhóm chọn, chưa phải kết quả học hoặc đánh giá Precision@3/NDCG@3. Bộ kiểm tra xếp hạng cũng dùng được cấu hình khởi tạo của tài liệu (0,4 / 0,2 / 0,2 / 0,1 / 0,1). Khi thay trọng số, cần đủ năm mã tiêu chí, từng giá trị từ 0 đến 1 và tổng bằng 1.

Chỉ cấu hình `ACTIVE` được sử dụng. Nếu chưa có cấu hình hoạt động hoặc cấu hình không hợp lệ, API trả lỗi rõ ràng. Người dùng phải có sở thích và mức vận động hợp lệ trước khi yêu cầu gợi ý. Véc-tơ rỗng hoặc có mã đặc trưng lạ bị loại; mỗi mã địa danh chỉ xuất hiện một lần. C và N_max được tính từ tập tham chiếu hợp lệ trước khi lọc theo lịch sử và khoảng cách của từng người dùng.

Chạy `db/016_preserve_missing_rating_counts.sql` trước khi dùng bản API này. Rating/count thiếu là NULL; count = 0 là số liệu thật, không đồng nghĩa thiếu. Kết quả kèm `criteria`, `usedCriteria`, `missingCriteria`, `effectiveWeights`, `reasons` và `configVersion` để truy vết. Trọng số bằng 0 không được ghi là đã dùng. App hiển thị tối đa hai lý do, với nội dung đầy đủ cho trình đọc màn hình. Cụm và đặc trưng đã lưu trong database, nên API không cần gọi dịch vụ Python trong [`recommendation/`](../recommendation/).

Backend tự chạy migration nhúng `021_versioned_recommendations.sql` khi khởi động nếu chưa có schema mới, bằng kết nối DB đã cấu hình. Transaction và advisory lock bảo vệ khi hai service khởi động cùng lúc; migration lỗi sẽ chặn khởi động bản mới. Có thể chạy SQL thủ công trước. API vẫn giữ tên trường `placeTypes`/`placeTypeIds`, nhưng giá trị là ID nhóm 201–206. Người dùng cũ cần chọn lại nhóm; lựa chọn cũ được bảo toàn. Ngưỡng A lấy từ settings; R/N lấy từ snapshot. Nhập mô hình hoặc sửa địa danh trên web tạo phiên bản mới, giữ snapshot cũ để quay lui. `configuration` trong kết quả ghi metadata để truy vết.

API quản trị bổ sung: `GET /api/admin/recommendations/dataset`, `POST /api/admin/recommendations/models` (tạo DRAFT) và `POST /api/admin/recommendations/models/{id}/activate`. Tất cả yêu cầu tài khoản ADMIN đang hoạt động. Xem [quy trình mô hình](../recommendation/README.md#đánh-giá-người-dùng-và-trao-đổi-mô-hình) và [mẫu chấm](../recommendation/evaluation/README.md).

Kiểm tra 20 tình huống về thay đổi sở thích/mức vận động, lọc ứng viên, loại trùng, dữ liệu thiếu, số 0 thật, trọng số, lý do và hợp đồng 35 đặc trưng. Chạy từ thư mục gốc với .NET SDK 10, không cần database hoặc thư viện kiểm thử bổ sung:

```powershell
dotnet run --project backend/Bcv.RecommendationChecks --artifacts-path .runtime.local/ai-checks -p:UseAppHost=false
```

## Phiên đăng nhập và bảo mật

- Phiên có hạn 30 ngày và tự gia hạn khi còn sử dụng. Phiên được lưu ra `%LOCALAPPDATA%\BCV\sessions.json` (đổi bằng cấu hình `Sessions__FilePath`), nên khởi động lại API không làm mất đăng nhập. File chỉ chứa mã băm SHA-256 của token.
- Đăng xuất, đổi hoặc đặt lại mật khẩu, và khóa tài khoản hủy phiên ngay.
- `GET /api/account/me` trả người dùng kèm cờ thiết lập còn thiếu, để app tự vào đúng màn khi mở lại.
- Mật khẩu được băm bằng PBKDF2-SHA256 có salt.
- API cho app chỉ trả nội dung `ACTIVE`.
- Khi triển khai ngoài máy phát triển, đặt API sau HTTPS.

## API

Mọi API (trừ đăng ký, đăng nhập, đặt lại mật khẩu, `/health`) yêu cầu header `Authorization: Bearer <token>`.

| Nhóm | Endpoint |
| --- | --- |
| Hệ thống | `GET /health` |
| Tài khoản | `POST /api/account/register`, `/login`, `/logout`, `/change-password`, `/delete`; `GET /api/account/me`; `GET/PUT /api/account/profile`; `POST /api/account/password-reset/request`, `/verify`, `/complete` |
| Thiết lập ban đầu | `GET/PUT /api/interests`, `GET/PUT /api/activity-level` |
| Điểm xuất phát | `GET/PUT /api/starting-point`, `POST /api/starting-point/resolve-current` |
| Khám phá, hành trình | `GET /api/explore`, `GET /api/explore/places/{placeId}`, `GET /api/explore/recommendations`, `POST /api/explore/journeys`, `POST /api/explore/sync-steps`, `GET /api/journeys/history` |
| Bộ sưu tập | `GET /api/collections`, `GET /api/collections/{collectionId}` |
| Nhiệm vụ, vật phẩm | `GET /api/quests`, `GET /api/inventory`, `GET/POST /api/items/speed-shoes[/use]`, `GET/POST /api/items/flight-ticket[/use]`, `GET /api/items/flight-ticket/regions/{regionId}/places` |
| Thống kê, thông báo | `GET /api/profile/stats`, `GET /api/notifications`, `POST /api/notifications/{id}/read` |
| Quản trị | Xem [web/README.md](../web/README.md#api-quản-trị) |

## Đóng góp

Xem [Đóng góp](../README.md#đóng-góp) ở README gốc.

## Giấy phép

UNLICENSED © 2026 Nhóm ĐAHK1262703. Xem [Giấy phép](../README.md#giấy-phép) ở README gốc.

## Lưu đặc trưng địa danh từ web quản trị

`GET /api/admin/lookups` trả thêm `features`; chi tiết địa danh trả `feature_codes`. Khi tạo/sửa địa danh, gửi `feature_codes` là danh sách mã đặc trưng được chọn. Mã phải thuộc bộ 35 đặc trưng của mô hình và danh sách không được rỗng. Nếu cập nhật mà không gửi trường này, giữ đặc trưng hiện có của cấu hình được chọn.

Backend chọn cấu hình ACTIVE trước, nếu chưa có thì dùng bản mới nhất để lưu nội dung quản trị; chỉ hỗ trợ cấu hình 15 cụm. Lưu địa danh, vector 35 giá trị và cụm trong một giao dịch; dữ liệu không hợp lệ làm hoàn tác toàn bộ. Xóa địa danh sẽ xóa các hàng đặc trưng sở hữu trong cùng giao dịch, nhưng vẫn tôn trọng ràng buộc lịch sử khác.

`PlaceFeatureClassifier` dùng tài nguyên nhúng `recommendation-centroids.json`, chuẩn hóa bốn nhóm đặc trưng và tìm tâm cụm gần nhất; không huấn luyện lại khi chỉnh sửa địa danh. Script `recommendation/export_centroids.py` tái tạo tâm từ snapshot nhãn và xác minh 315 dòng nguồn trước khi xuất.

Kiểm tra phân cụm, không cần database:

```powershell
dotnet run --project backend/Bcv.Checks/Bcv.Checks.csproj --artifacts-path backend/checks-build-out -p:UseAppHost=false
```

Để kiểm thử tạo/sửa/đọc lại, 35 giá trị, rollback và xóa dữ liệu thử, đặt biến môi trường tiến trình `BCV_CHECK_CONNECTION` bằng chuỗi kết nối cục bộ trước khi chạy. Không ghi chuỗi kết nối vào repo. Bộ kiểm tra tạo bản ghi tạm và tự xóa sau khi chạy.

### Tự xác định khu vực từ ghim bản đồ quản trị

`GET /api/admin/map/region?latitude=21.0272&longitude=105.8355` yêu cầu phiên quản trị và trả về tỉnh hiện tại, tỉnh trước sáp nhập (nếu có) và địa chỉ từ Goong Geocode V2. Web đối chiếu tên với khu vực trong database trước khi điền biểu mẫu tạo địa danh; tọa độ giữ nguyên điểm người dùng chọn.

Cấu hình Goong REST API Key bằng `Goong__ApiKey`. Script `run-local.ps1` cũng hỗ trợ `BCV_GOONG_API_KEY` và khóa `bcvGoongApiKey` trong `android/dev.properties` cho môi trường phát triển. Không dùng Maptiles Key thay cho REST key. REST key không được trả về trình duyệt. Khi thiếu khóa, lỗi dịch vụ hoặc không có khu vực tương ứng, người dùng vẫn có thể điền khu vực thủ công trong biểu mẫu.

## Ảnh/video cộng đồng và kiểm duyệt

- Android: Chi tiết địa danh → Ảnh/video cộng đồng → chọn tệp và gửi.
- Web quản trị: Duyệt ảnh/video → mở xem trước → Duyệt công khai hoặc Từ chối, kèm ghi chú.
- Chỉ nội dung APPROVED được xem công khai. Người gửi xem được trạng thái nội dung của mình.
- JPEG/PNG/WebP tối đa 10 MB; MP4/WebM tối đa 30 MB; mô tả tối đa 500 ký tự.
- Migration db/022_community_media.sql được nhúng và chạy idempotent khi API khởi động. Tệp hiện lưu BYTEA trong PostgreSQL; không phụ thuộc ổ đĩa container.
- Danh sách trả tối đa 100 mục mới nhất theo trạng thái.
- API: GET/POST /api/places/{id}/media, GET /api/community-media/{id}/content, GET /api/admin/community-media, POST /api/admin/community-media/{id}/review.
- Kiểm tra HTTP không dùng DB thật: dotnet run --project backend/Bcv.MediaChecks.
- Cần deploy backend/web và cài APK mới cùng nhau; backend cũ chưa có các API này.
