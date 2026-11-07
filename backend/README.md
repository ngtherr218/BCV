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

Chi tiết bộ sưu tập trả `placeAvailable` cho mỗi thẻ. Giá trị này chỉ đúng khi địa danh và khu vực của nó đang hoạt động; app không mở trang địa danh khi giá trị là `false`. Thẻ đang hoạt động đã sở hữu vẫn được giữ như lịch sử khi địa danh ngừng hoạt động. Thẻ chưa sở hữu thuộc địa danh không còn khả dụng được bỏ khỏi danh sách và tổng số thẻ có thể thu thập; tổng quan, từng bộ và chi tiết dùng cùng quy tắc. Chính sách ẩn thẻ ngừng hoạt động được giữ nguyên, không xóa dữ liệu sở hữu. API địa danh trả `PLACE_NOT_FOUND` cho địa danh không khả dụng và `NO_STARTING_POINT` khi địa danh còn hoạt động nhưng người dùng chưa có vị trí ảo.

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

- Android: Mở khóa địa danh → Chi tiết → Ảnh/video cộng đồng → chọn tệp, nhập mô tả và gửi. Ảnh/video mới ở trạng thái `PENDING`.
- Web quản trị: Duyệt ảnh/video → mở xem trước → Duyệt công khai hoặc Từ chối, kèm ghi chú.
- Chỉ người dùng đã đăng nhập và mở khóa địa danh đang `ACTIVE` trong khu vực `ACTIVE` được xem/gửi nội dung cộng đồng. Nội dung `APPROVED` hiện với những người dùng này; người gửi xem được nội dung và kết quả duyệt của mình. Quản trị viên đang hoạt động có thể xem tệp để kiểm duyệt, kể cả khi địa danh đã bị ẩn. Tệp không truy cập vô danh, các yêu cầu ảnh/video đều cần Bearer token.
- JPEG/PNG/WebP tối đa 10 MB; video MP4/WebM tối đa 100 MiB, 60 giây và 1080p; mô tả tối đa 500 ký tự. Nội dung đã lưu vẫn được xem theo cùng quyền truy cập. Không tự nén hoặc chuyển mã tệp.
- Migration db/022_community_media.sql được nhúng và chạy idempotent khi API khởi động. Tệp hiện lưu BYTEA trong PostgreSQL; không phụ thuộc ổ đĩa container.
- Danh sách trả tối đa 100 mục mới nhất theo trạng thái.
- API: GET/POST /api/places/{id}/media, GET /api/community-media/{id}/content, GET /api/admin/community-media, POST /api/admin/community-media/{id}/review.
- Kiểm tra HTTP không dùng DB thật: dotnet run --project backend/Bcv.MediaChecks.
- Cần deploy backend/web và cài APK mới cùng nhau; backend cũ chưa có các API này.

## Đặc trưng theo loại địa danh

Migration `db/023_place_type_features.sql` được nhúng trong API và tự chạy khi khởi động, sau migration gợi ý. Migration tạo ánh xạ mặc định cho 12 loại chuẩn; không ghi đè đặc trưng của địa danh cũ (được giữ ở chế độ `CUSTOM`).

- Thêm/sửa loại địa danh: gửi `feature_codes` gồm ít nhất một trong 35 mã nội dung. Ánh xạ được lưu trong `place_type_feature`.
- Thêm/sửa địa danh: `feature_source: "TYPE"` để backend lấy ánh xạ từ loại, không cần gửi `feature_codes`. `feature_source: "CUSTOM"` cùng `feature_codes` để tùy chỉnh. Loại chưa cấu hình sẽ báo lỗi rõ ràng.
- Sửa ánh xạ loại cập nhật mọi địa danh `TYPE`, phân cụm lại trong một phiên bản mô hình mới và một transaction. Địa danh `CUSTOM` giữ nguyên. Không tạo lại phiên bản nếu ánh xạ không thay đổi.
- Chỉ có đặc trưng chưa đảm bảo xuất hiện trong Top 3: các điều kiện trạng thái, khoảng cách, lịch sử khám phá và điểm xếp hạng vẫn áp dụng.

Kiểm thử local: `./backend/test-admin-delete.ps1` (đã bao gồm kế thừa đặc trưng, cập nhật nhiều địa danh, tùy chỉnh và rollback). Test web: `node --test web/tests/place-feature-defaults.test.mjs`.

## Kiểm duyệt nội dung cộng đồng

Trang **Kiểm duyệt nội dung** (`#media`) có các bộ lọc trạng thái, ảnh/video, địa danh/người gửi/chú thích, báo cáo chưa xử lý và phân trang. Các chỉ số thể hiện số chờ duyệt, đã duyệt, có báo cáo và dung lượng tệp đang lưu (không phải phần trăm hạn mức lưu trữ).

- Duyệt sau khi tải được bản xem trước. Từ chối/gỡ/khôi phục/khóa quyền gửi đều có dialog xác nhận; lý do là bắt buộc, trừ lần duyệt mới.
- `PENDING → APPROVED/REJECTED`; `APPROVED → REMOVED`; `FLAGGED → APPROVED/REMOVED`. `REMOVED` là trạng thái kết thúc sau khi xóa tệp. Nội dung APPROVED có báo cáo mở có thể giữ công khai và đóng báo cáo sau khi kiểm tra.
- Gỡ là ngừng công khai và xóa tệp vĩnh viễn; chỉ nội dung đang tạm ẩn do báo cáo mới có thể được khôi phục. Tệp bị từ chối được dọn sau 7 ngày.
- Kết quả kiểm duyệt và thay đổi quyền gửi tạo thông báo cho người gửi trong cùng transaction. Điều kiện **Ảnh được duyệt** (`PHOTO_APPROVED`) chỉ tăng khi ảnh được duyệt lần đầu; video không tăng điều kiện này. Điều kiện cũ `MEDIA_APPROVED` vẫn tính cả ảnh/video để giữ các nhiệm vụ đã cấu hình. Duyệt lại/khôi phục không cộng lần hai; tiến độ được tính vào chu kỳ ngày/tuần tại thời điểm phê duyệt.
- Quản trị tạo nhiệm vụ ở **Nhiệm vụ → Điều kiện hoàn thành → Ảnh được duyệt**, đặt mục tiêu (ví dụ 1 ảnh), chu kỳ, phần thưởng và trạng thái `ACTIVE`. Không tự thêm hay đổi phần thưởng của các nhiệm vụ đang sử dụng.
- Khóa quyền gửi không khóa toàn bộ tài khoản; API upload kiểm tra quyền này trực tiếp tại backend.
- API `POST /api/community-media/{id}/reports` nhận `{ reason }`: người xem có quyền mở khóa có thể báo cáo nội dung đã duyệt của người khác; không tự báo cáo hoặc báo cáo trùng. Android có nút **Báo cáo** và ô nhập lý do. Mặc định 3 báo cáo mở từ các tài khoản khác nhau sẽ tạm ẩn. Đổi ngưỡng bằng `CommunityMedia__ReportThreshold` (1–100).
- Các API quản trị: `GET /api/admin/community-media?status=&q=&kind=image|video&reported=true&page=1&pageSize=12`; `GET /api/admin/community-media/{id}/reports`; `POST /api/admin/community-media/{id}/review` với `{status,note,expectedStatus}`; `POST /api/admin/community-media/users/{id}/upload-access` với `{blocked,note}`. Tất cả yêu cầu tài khoản ADMIN đang hoạt động.
- Migration `db/024_media_moderation.sql` được nhúng và tự chạy khi backend khởi động. Tạo thêm bảng `media_report`, cột `app_user.media_upload_blocked`, `community_media.first_approved_at` và trạng thái FLAGGED/REMOVED. Sau migration 024 có 31 bảng; migration 025 thêm `place_video`, tổng cộng 32 bảng.
- Kiểm thử bằng `./backend/test-admin-delete.ps1`: bao gồm HTTP authentication/authorization, từ chối thiếu lý do, báo cáo/ẩn/khôi phục, quyền gửi, thông báo và chống cộng lặp nhiệm vụ; sử dụng schema PostgreSQL local riêng và tự dọn.

## Video địa danh và quy tắc theo phụ lục chức năng

- Migration 025 tự chạy cùng khởi tạo module media. Thêm bảng place_video, cho phép dọn dữ liệu tệp cộng đồng nhưng giữ metadata và kết quả báo cáo ACCEPTED/DISMISSED. Migration 024 cũng chấp nhận EXPIRED để khởi động lại an toàn sau khi có dữ liệu hết hạn.
- Web thêm/sửa địa danh cho tải video MP4, tối đa 100 MB, 60 giây, 1080p. Cả trình duyệt và backend kiểm tra thông tin video. Backend đọc cấu trúc MP4; tệp không có metadata/thời lượng hoặc luồng video hợp lệ bị từ chối. Chưa có tính năng tự chuyển mã/nén video hoặc hỗ trợ mọi biến thể MP4 phân mảnh.
- Video tải trước được giữ tạm theo tài khoản quản trị; video_ids chỉ được gắn khi lưu địa danh trong cùng transaction. Tệp chưa được gắn sau 24 giờ được dọn. Video đã gắn bị loại khỏi biểu mẫu sẽ bị xóa khi lưu thành công.
- Tổng video chính thức + video cộng đồng đang APPROVED của một địa danh không vượt 5. Khóa hàng địa danh đồng bộ kiểm tra này giữa duyệt và sửa địa danh.
- Android chỉ nhận danh sách và phát video sau khi đã khám phá địa danh; API kiểm tra quyền và hỗ trợ Range/seek. Video chính thức và video cộng đồng đều phát trong app với Bearer token. Nút cộng đồng chỉ hiện tại địa danh đã mở khóa.
- MediaMaintenance chạy khi khởi động và mỗi giờ: dọn tệp từ chối đến hạn 7 ngày, chuyển nội dung chờ quá 30 ngày sang EXPIRED, dọn video quản trị chưa gắn sau 24 giờ.
- Báo cáo sau khi gỡ: ACCEPTED; báo cáo bị bác/khôi phục: DISMISSED. Không thể khóa quyền gửi của ADMIN. Có thể quản lý quyền gửi từ chi tiết người dùng.
- Bản đồ web và Android chỉ hiện ảnh từ zoom 11; phía dưới dùng chấm. Marker Android giữ dấu phân biệt đã/chưa khám phá kể cả khi có ảnh thu nhỏ. Web không tải thumbnail trước ngưỡng này.
- Danh sách địa danh khi thêm thẻ loại bỏ mọi địa danh đã có thẻ, kể cả thẻ DRAFT/INACTIVE. Khi sửa thẻ, vẫn giữ lựa chọn địa danh hiện tại; backend và ràng buộc duy nhất tiếp tục chặn tạo trùng.

## Giới hạn video 100 MB / 1080p / 60 giây

- Migration 026 cập nhật constraint thời lượng của video đã lưu lên 60 giây; không xóa dữ liệu và không thêm bảng.
- Web quản trị nhận MP4 tối đa 100 MiB (100×1024×1024 byte), 60 giây, 1920×1080 hoặc 1080×1920. Video cộng đồng MP4/WebM dùng cùng giới hạn kích thước/thời lượng/độ phân giải; ảnh vẫn tối đa 10 MB.
- Backend đọc metadata MP4/WebM, từ chối tệp không xác định được thời lượng/luồng video. WebM phải có Cluster chứa dữ liệu khung hình thuộc đúng track video; chỉ có metadata, block rỗng hoặc dữ liệu audio không đủ để được nhận. Đây là kiểm tra cấu trúc, không giải mã mọi frame để bảo đảm tệp phát được. Không tự nâng chất lượng, kéo dài hoặc chuyển mã tệp.
- Các endpoint raw upload nâng giới hạn request body tương ứng để không bị chặn tại giới hạn mặc định 30 MB của Kestrel. Web dùng timeout upload 5 phút. Android gửi theo luồng, không nạp toàn bộ tệp lớn vào RAM.
- Bộ test local có upload HTTP 31 MB, biên 60 giây/1080p (ngang/dọc), chặn trên 100 MB, 61 giây và 4K, cùng kiểm tra migration lặp lại khi đã có video 60 giây.
