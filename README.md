# Bước Chân Việt

Bước Chân Việt chuyển bước chân thật thành hành trình khám phá các địa danh Việt Nam. Repository gồm app Android, ASP.NET Core API, web quản trị và dữ liệu mô hình gợi ý ngoại tuyến. Tài liệu này dành cho nhánh **bcv_v2**, cập nhật ngày 16/11/2026.

## Chức năng

- Chọn sở thích từ 12 nhóm loại địa danh; lọc và tìm kiếm điểm đến theo khoảng cách, số ngày dự kiến và loại địa danh. Gợi ý điểm đến mới dựa trên sở thích và mức vận động.
- Đồng bộ bước qua Health Connect để hoàn thành hành trình ảo. Địa danh đã khám phá không được chọn làm hành trình mới; backend kiểm tra cả khi bắt đầu và khi hoàn thành.
- Khám phá lần đầu bằng đi bộ nhận điểm xếp hạng theo cấu hình; mặc định **150 điểm**. Vé bay không cộng điểm xếp hạng. Nhiệm vụ có điểm thưởng riêng, được chốt một lần theo chu kỳ.
- Bảng xếp hạng tuần theo giờ Việt Nam, lọc vùng và xác định vị trí của bạn; tên hiển thị lấy từ hồ sơ, không ghép mã người dùng. Thông báo hoàn thành ghi nhận thay đổi thứ hạng toàn quốc trong tuần.
- Xem trước danh mục thẻ, nhận và đánh dấu thẻ mới đã xem. Thẻ đã sở hữu vẫn còn trong lịch sử khi địa danh ngừng hoạt động. Hộ chiếu tổng hợp dấu khám phá và vùng đã đi từ lịch sử, không tạo thêm quyền sở hữu thẻ.
- Xem lại kết quả hành trình từ dữ liệu đã ghi nhận; mở lại không phát thưởng. Đóng góp ảnh địa danh, kiểm duyệt và báo cáo ảnh; lưu metadata khi file hết thời hạn lưu trữ.
- Quản trị danh mục, nhiệm vụ, điểm thưởng và kiểm duyệt nội dung. Điểm đã chốt trong sổ điểm không bị tính lại khi thay cấu hình.

## Cấu trúc

| Thư mục | Nội dung |
| --- | --- |
| `android/` | Android Kotlin, Health Connect, bản đồ và bộ nhớ đệm ảnh công khai |
| `backend/Bcv.Api/` | API, quy tắc nghiệp vụ, truy cập PostgreSQL |
| `web/` | Web quản trị React 19 và Vite 7 |
| `sql/` | **Bộ SQL triển khai hiện tại**, cài mới/nâng cấp/kiểm tra |
| `db/` | Migration và seed lịch sử trong quá trình phát triển |
| `recommendation/` | Dataset, huấn luyện và đánh giá mô hình K-Means ngoại tuyến |
| `docs/` | Báo cáo chức năng, thay đổi CSDL và tài liệu kỹ thuật |

App/web gọi API; chỉ backend kết nối database. Backend đọc nhãn cụm, đặc trưng và cấu hình đã lưu, không cần chạy dịch vụ Python khi phục vụ yêu cầu. Mô hình gốc dùng 311 địa danh, 35 đặc trưng và 15 cụm; danh mục triển khai có thể có thêm địa danh do quản trị viên tạo.

## Yêu cầu

- .NET SDK 10.
- PostgreSQL 18, PostGIS 3.6 và bộ công cụ `psql`, `pg_dump`, `createdb` tương thích.
- Node.js 22.12 trở lên để chạy web; dùng `npm ci` theo lockfile.
- Android Studio với SDK 37, JDK tương thích AGP 9.4.1/Gradle 9.6. Dự án dùng Gradle wrapper; app yêu cầu Android 8.1/API 27 trở lên. Health Connect cần thiết bị và quyền đọc bước phù hợp.

## Chuẩn bị database

Đọc [sql/README.md](sql/README.md) trước khi chạy. Database mới:

```powershell
createdb -h localhost -U postgres -T template0 -E UTF8 vietnam_explorer_dev_v2
psql -X -h localhost -U postgres -d vietnam_explorer_dev_v2 -v ON_ERROR_STOP=1 -f sql/install.sql
```

Database đang có dữ liệu: backup trước, kiểm tra điều kiện rồi chạy `sql/upgrade.sql`. Không chạy lại schema/seed cài mới trên database đang sử dụng. Bộ seed không chứa tài khoản, mật khẩu, lịch sử hay dữ liệu cá nhân. Schema cuối có **33 bảng nghiệp vụ**, không tính bảng của PostGIS.

## Chạy local v2

Các cổng của profile local v2 là API **5082**, web **5173**; database là **vietnam_explorer_dev_v2**. Các script cũ `run-local.ps1`/`create-admin.ps1` dùng cấu hình riêng và có thể trỏ đến DB/cổng cũ; không trộn với profile v2.

### Backend

Tại thư mục gốc:

```powershell
Copy-Item .env.example .env.local
# Sửa DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASSWORD trong .env.local.
dotnet restore backend/Bcv.Api/Bcv.Api.csproj
.\backend\run-dev-v2.ps1
```

`.env.local` đã được Git bỏ qua. Launcher chỉ chấp nhận DB v2 trên máy local, tắt startup migration, SMTP, R2 và tác vụ dọn media trong profile thử nghiệm. Kiểm tra [API health local](http://127.0.0.1:5082/health).

### Web quản trị

Mở terminal khác:

```powershell
cd web
npm ci
$env:BCV_API_TARGET='http://127.0.0.1:5082'
$env:WEB_PORT='5173'
npm run dev
```

Mở [web local](http://127.0.0.1:5173). Cần đặt `BCV_API_TARGET` vì cấu hình mặc định của Vite vẫn hỗ trợ profile cũ ở cổng 5081. Có thể lưu hai biến này trong `web/.env.local` (không commit).

### Tạo tài khoản quản trị

Không có tài khoản/mật khẩu mặc định trong SQL. Cấu hình chuỗi kết nối **đúng database đích**, rồi chạy CLI:

```powershell
$env:ConnectionStrings__Bcv='Host=localhost;Port=5432;Database=vietnam_explorer_dev_v2;Username=<user>;Password=<password>'
$pw=Read-Host 'Mat khau admin moi (12-128 ky tu, co chu va so)' -AsSecureString
$env:BCV_ADMIN_PASSWORD=[System.Net.NetworkCredential]::new('', $pw).Password
try {
    dotnet run --project backend/Bcv.Api -- --create-admin admin@example.com
} finally {
    Remove-Item Env:BCV_ADMIN_PASSWORD -ErrorAction SilentlyContinue
    Remove-Item Env:ConnectionStrings__Bcv -ErrorAction SilentlyContinue
}
```

Thay email/giá trị giữ chỗ trước khi chạy. Không ghi credential vào file được theo dõi bởi Git.

### Android

```powershell
.\backend\build-android-dev-v2.ps1 -Api http://127.0.0.1:5082/
```

Helper sử dụng JDK Android Studio và dependency đã cache; xem tham số trong script nếu cài SDK/JDK ở vị trí khác. Hoặc mở `android/` bằng Android Studio và build với Gradle property `bcvApiBaseUrl`. Mặc định build Android trỏ máy chủ Test dùng chung, nên cần truyền URL local khi kiểm thử local.

Với emulator/USB device, ánh xạ cổng bằng ADB (launcher v2 duy trì mapping cho emulator):

```powershell
adb devices
adb reverse tcp:5082 tcp:5082
```

Nếu dùng IP LAN, truyền `-Api http://<IP-PC>:5082/`; điện thoại và PC phải liên lạc được. Cấp quyền Health Connect để đồng bộ bước; kết nối API thành công không đồng nghĩa thiết bị đã cấp quyền cảm biến/bước.

## Cấu hình và triển khai

- `ConnectionStrings__Bcv`: chuỗi Npgsql, cấu hình tại môi trường chạy backend. Cloud cần SSL phù hợp nhà cung cấp.
- `Database__ApplyStartupMigrations=false`: dùng bộ SQL quản lý schema thay vì migration tự chạy lúc startup.
- `Sessions__FilePath`: nơi lưu hash token, cần ổ đĩa bền vững nếu muốn giữ phiên qua redeploy.
- `Smtp__*`, `R2__*`, `Goong__ApiKey`: cấu hình riêng cho email, kho media và geocoding. Khóa Maptiles dùng phía client khác REST key phía server.
- `VITE_GOONG_MAPTILES_KEY`: khóa bản đồ công khai dành cho web; không dùng nó để chứa bí mật backend.

Xem [DEPLOY.md](DEPLOY.md) cho Docker/cloud và cấu hình máy chủ Test. Lần cập nhật này push mã lên **bcv_v2**; cấu hình service đang theo dõi nhánh `Web`/`Deploy_web` không tự chuyển nhánh hoặc database. Không chạy migration cloud chỉ bằng thao tác push Git.

## Kiểm tra

```powershell
dotnet build backend/Bcv.Api/Bcv.Api.csproj
dotnet run --project backend/Bcv.Checks
dotnet run --project backend/Bcv.StepChecks
npm --prefix web run build
```

Các kiểm tra integration cần PostgreSQL và kết nối riêng; đọc script trước khi chạy, ưu tiên database thử nghiệm. Kiểm tra Android qua helper [build-android-dev-v2.ps1](backend/build-android-dev-v2.ps1) và `testDebugUnitTest`. Kiểm tra thao tác thực tế gồm đăng nhập, chọn điểm chưa khám phá, hoàn thành, điểm nhiệm vụ, thông báo và thứ hạng. Thiết bị vật lý/Health Connect phải được kiểm tra riêng.

## Tài liệu

- [Danh sách lệnh triển khai DB](sql/README.md).
- [Báo cáo thay đổi chức năng và CSDL](docs/BCV_V2_THAY_DOI_CHUC_NANG_VA_CSDL.docx).
- [Tài liệu API thay đổi](docs/technical/api_changes.md).
- [Quy tắc điểm và nhiệm vụ](docs/points/POINT_SYSTEM_DESIGN.md).
- [Nguồn và ghi công ảnh](backend/Bcv.Api/Modules/Discovery/Domain/place-photo-credits.json).
- [Backend](backend/README.md), [Android](android/README.md), [mô hình gợi ý](recommendation/README.md).

Không commit `.env.local`, `dev.properties`, session file, log, backup DB hoặc output kiểm thử. Quyền sử dụng ảnh và tài nguyên theo từng nguồn được ghi trong manifest/tài liệu tương ứng.
