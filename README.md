# Bước Chân Việt (*BCV-Web*)

Khám phá địa danh Việt Nam bằng bước chân thật: app Android, Backend API, web quản trị và mô hình gợi ý điểm đến.

Người dùng đi bộ ngoài đời thực; app đọc số bước qua Health Connect và quy đổi thành tiến độ của một hành trình ảo tới các địa danh. Hoàn thành hành trình sẽ mở khóa ảnh, câu chuyện, thẻ sưu tập, kèm nhiệm vụ và vật phẩm. Mô hình K-Means phân cụm 311 địa danh theo 35 đặc trưng để gợi ý điểm đến hợp sở thích và mức vận động. Đây là đồ án tốt nghiệp nhóm **ĐAHK1262703**, Khoa Công nghệ Thông tin, Trường Đại học Mở Hà Nội. Thư mục kho mã có tên `BCV-Web`.

## Mục lục

- [Bối cảnh](#bối-cảnh)
- [Cài đặt](#cài-đặt)
- [Sử dụng](#sử-dụng)
- [Cấu hình](#cấu-hình)
- [Xử lý sự cố](#xử-lý-sự-cố)
- [API](#api)
- [Người duy trì](#người-duy-trì)
- [Đóng góp](#đóng-góp)
- [Giấy phép](#giấy-phép)

## Bối cảnh

Đặc tả đầy đủ nằm trong [tài liệu đồ án](https://docs.google.com/document/d/1GAQdAqCcwr_IivfHzInilF0LhbWyniN6AONmzqEHWjo/edit?tab=t.0).

### Kiến trúc

```text
App Android ─┐
             ├─► Backend API (ASP.NET Core, cổng 5081) ─► PostgreSQL + PostGIS
Web quản trị ┘                                              ▲
                                                            │ nhãn cụm, 35 đặc trưng
                              Mô hình gợi ý (Python, K-Means, huấn luyện ngoại tuyến)
```

App và web **không** kết nối thẳng vào database; mọi dữ liệu đi qua Backend API. Nhãn cụm của mô hình được lưu sẵn trong database, nên Backend chấm điểm gợi ý mà không cần gọi dịch vụ Python.

| Thư mục | Thành phần | Công nghệ | Hướng dẫn riêng |
| --- | --- | --- | --- |
| [`android/`](android/) | App cho người dùng | Kotlin, minSdk 27, Health Connect, MapLibre + Goong | [android/README.md](android/README.md) |
| [`backend/`](backend/) | Backend API dạng modular monolith | C#, ASP.NET Core .NET 10, Npgsql | [backend/README.md](backend/README.md) |
| [`web/`](web/) | Web quản trị nội dung | React 19, Vite 7 | [web/README.md](web/README.md) |
| [`db/`](db/) | Schema 25 bảng, dữ liệu mẫu, migration | PostgreSQL 18 + PostGIS 3.6 | [db/README.md](db/README.md) |
| [`recommendation/`](recommendation/) | Mô hình phân cụm địa danh | Python 3.12, scikit-learn, FastAPI | [recommendation/README.md](recommendation/README.md) |

### Dịch vụ bên ngoài

| Dịch vụ | Dùng cho | Bắt buộc |
| --- | --- | --- |
| Goong Maps | Bản đồ (Maptiles key), tra địa chỉ và chỉ đường (REST API key) | Có, để hiện bản đồ |
| Health Connect | Đọc số bước trên điện thoại | Có, trên thiết bị |
| Cloudflare R2 | Lưu ảnh tải lên; không có thì lưu trong database | Không |
| Gmail SMTP | Gửi mã đặt lại mật khẩu; không có thì mã in ra log | Không |

## Cài đặt

Các lệnh dưới đây chạy bằng **PowerShell** tại thư mục gốc của kho mã, trên Windows. macOS/Linux dùng PowerShell 7 (`pwsh`) cho các script `.ps1`.

### Yêu cầu

| Công cụ | Phiên bản | Dùng cho |
| --- | --- | --- |
| [.NET SDK](https://dotnet.microsoft.com/download) | 10.0 | Backend |
| [PostgreSQL](https://www.postgresql.org/download/) + PostGIS | 18 + 3.6 | Database |
| [Node.js](https://nodejs.org/) | 22.12 trở lên | Web quản trị |
| [Android Studio](https://developer.android.com/studio) | bản mới, kèm JDK | App Android |
| [Python](https://www.python.org/) | 3.12 | Chỉ khi chạy hoặc kiểm thử mô hình gợi ý |

### 1. Tạo database

Tạo database trống tên `bcv` (bằng pgAdmin hoặc `createdb`), bật PostGIS, rồi nạp schema và dữ liệu theo thứ tự trong [db/README.md](db/README.md#tạo-database-mới). Sau khi nạp, kích hoạt nội dung mẫu (mục [Trạng thái nội dung](db/README.md#trạng-thái-nội-dung)); nếu không, app sẽ không có địa danh, bộ sưu tập hay nhiệm vụ nào.

### 2. Cấu hình backend

```powershell
Copy-Item backend/local.settings.example.json backend/local.settings.json
.\backend\set-local-secrets.ps1
```

Sửa `backend/local.settings.json` cho đúng máy của bạn, quan trọng nhất là `DbHost`, `DbPort`, `DbName`, `DbUser`. Script `set-local-secrets.ps1` lưu mật khẩu PostgreSQL (và tùy chọn SMTP, R2) vào biến môi trường của tài khoản Windows, không ghi vào file nào trong kho mã. Chi tiết: [backend/README.md](backend/README.md#cấu-hình).

### 3. Chạy backend

```powershell
.\backend\run-local.ps1
```

Kiểm tra: http://127.0.0.1:5081/health trả về `{"status":"ok"}`.

### 4. Tạo tài khoản quản trị và chạy web

```powershell
.\backend\create-admin.ps1 -Email admin@bcv.local
cd web
npm ci
npm run dev
```

Mở http://127.0.0.1:5173 và đăng nhập bằng tài khoản vừa tạo. Nếu `create-admin.ps1` báo lỗi build vì backend đang chạy, dừng backend (Ctrl+C), chạy script rồi khởi động lại backend.

### 5. Chạy app Android

Tạo `android/dev.properties` (Git bỏ qua file này):

```properties
bcvApiBaseUrl=http://<IP-Wi-Fi-của-máy-tính>:5081/
bcvGoongMaptilesKey=<Goong Maptiles Key>
bcvGoongApiKey=<Goong REST API Key>
```

Rồi build và cài lên thiết bị:

```powershell
cd android
.\gradlew.bat installDebug
```

Hoặc mở thư mục `android/` bằng Android Studio và bấm **Run**. Mặc định app dùng API deploy `https://bcv-admin-test.onrender.com/`, không cần backend local hay cáp USB. Để thử backend local, cấu hình rõ `bcvApiBaseUrl=http://10.0.2.2:5081/` cho emulator hoặc IP Wi-Fi của máy tính cho điện thoại thật. Chi tiết: [android/README.md](android/README.md).

### 6. (Tùy chọn) Kiểm thử mô hình gợi ý

```powershell
cd recommendation
py -3.12 -m venv .venv
.venv\Scripts\python -m pip install -r requirements-test.txt
.venv\Scripts\python -m unittest discover -s tests -v
```

## Sử dụng

### App Android

1. **Đăng ký** bằng email và mật khẩu (hoặc đăng nhập). Phiên được giữ 30 ngày nên lần sau mở app sẽ vào thẳng.
2. **Thiết lập lần đầu:** chọn 1–3 loại địa danh yêu thích, mức vận động, cấp quyền đọc số bước, chọn điểm xuất phát ảo.
3. **Khám phá:** bấm **Gợi ý điểm đến** để nhận 3 địa danh hợp với bạn, chọn một để bắt đầu hành trình.
4. **Đi bộ thật**, rồi bấm **Đồng bộ** để cập nhật tiến độ. Đủ bước thì hành trình hoàn thành, địa danh được mở khóa và vị trí ảo chuyển tới đó.
5. **Bộ sưu tập, Nhiệm vụ, Hồ sơ** ở các tab còn lại.

Khi trình diễn, nút icon người đi bộ có dấu "+" cạnh chữ **Khám phá** ghi thêm 2.000 bước mẫu vào Health Connect để thử đồng bộ.

### Web quản trị

Quản lý người dùng, khu vực, địa danh (kèm bản đồ chọn tọa độ), loại địa danh, thẻ, bộ sưu tập, nhiệm vụ và vật phẩm. Nội dung ở trạng thái `DRAFT` chỉ hiện trong quản trị; chuyển sang `ACTIVE` thì app mới thấy. Xem [web/README.md](web/README.md).

### Quy tắc nghiệp vụ chính

| Quy tắc | Giá trị | Nơi mô tả |
| --- | --- | --- |
| Quy đổi khoảng cách thành bước | S = max(1, ⌈1000 · D · κ / L⌉), κ = 1, L = 0,75 m | [backend/README.md](backend/README.md#quy-đổi-khoảng-cách-thành-số-bước) |
| Điểm gợi ý | S = 0,35P + 0,15C + 0,3A + 0,1R + 0,1N, trả tối đa 3 địa danh | [backend/README.md](backend/README.md#gợi-ý-điểm-đến) |
| Loại địa danh | 12 loại chuẩn, mỗi loại ánh xạ sang nhóm đặc trưng của mô hình | [db/README.md](db/README.md#loại-địa-danh) |
| Phạm vi điểm xuất phát | Địa danh `ACTIVE` gần nhất trong bán kính 75 km | [android/README.md](android/README.md#luồng-sử-dụng) |

## Cấu hình

Các file và biến sau là **riêng của từng máy**, đã được Git bỏ qua; không commit và không chép sang máy khác.

| File / biến | Nội dung |
| --- | --- |
| `backend/local.settings.json` | Địa chỉ database, cổng API, tài khoản SMTP |
| Biến môi trường `BCV_DB_PASSWORD`, `BCV_SMTP_PASSWORD`, `BCV_R2_*` | Mật khẩu và khóa dịch vụ của backend |
| `%LOCALAPPDATA%\BCV\sessions.json` | Phiên đăng nhập đã lưu (chỉ chứa mã băm token) |
| `web/.env.local` | Cổng web, địa chỉ backend, Maptiles key (đều tùy chọn) |
| `android/dev.properties` | URL backend và hai khóa Goong cho app |

## Xử lý sự cố

| Hiện tượng | Cách xử lý |
| --- | --- |
| `/health` báo `Database unavailable` | PostgreSQL chưa chạy, sai `DbPort` trong `local.settings.json`, hoặc sai `BCV_DB_PASSWORD` |
| App báo "Chưa thể kết nối" | Điện thoại khác mạng, sai IP trong `dev.properties`, hoặc Windows Firewall chặn cổng 5081. Xem [android/README.md](android/README.md#xử-lý-sự-cố) |
| Web báo lỗi mạng | Backend chưa chạy hoặc `BCV_API_TARGET` trong `web/.env.local` sai cổng |
| Không có điểm đề xuất, bộ sưu tập, nhiệm vụ trống | Nội dung còn `DRAFT`; kích hoạt theo [db/README.md](db/README.md#trạng-thái-nội-dung) |
| Bản đồ trống, không có đường đi | Thiếu hoặc sai khóa Goong trong `dev.properties`; build lại app sau khi sửa |
| Cài app lên điện thoại báo `INSTALL_FAILED_ABORTED` | Điện thoại (ví dụ vivo) hỏi xác nhận cài qua USB; bấm **Cài đặt** rồi chạy lại |

## API

Toàn bộ endpoint của Backend (tài khoản, khám phá, hành trình, gợi ý, bộ sưu tập, nhiệm vụ, vật phẩm, thống kê, thông báo) liệt kê tại [backend/README.md](backend/README.md#api); API quản trị tại [web/README.md](web/README.md#api-quản-trị). Mọi API (trừ đăng ký, đăng nhập, đặt lại mật khẩu, `/health`) yêu cầu header `Authorization: Bearer <token>`.

## Người duy trì

Nhóm đồ án ĐAHK1262703, chuyên ngành Công nghệ phần mềm, Trường Đại học Mở Hà Nội:

- Lê Đức Quang
- Nguyễn Thị Thơ
- Phạm Công Đạt
- Nguyễn Thị Minh Thúy

## Đóng góp

Đây là kho mã đồ án của nhóm; thay đổi do các thành viên thực hiện. Câu hỏi và góp ý gửi qua mục Issues của kho mã hoặc trực tiếp cho nhóm.

Khi đóng góp:

- Không commit file cấu hình riêng của máy, mật khẩu hay khóa dịch vụ (xem [Cấu hình](#cấu-hình)).
- Thay đổi database viết thành script mới đánh số tiếp theo trong `db/`, chạy trong một giao dịch, có kiểm tra trạng thái và chạy lại được an toàn.
- Cập nhật README của thành phần liên quan khi đổi cách cài, chạy hoặc quy tắc nghiệp vụ.

## Giấy phép

UNLICENSED © 2026 Nhóm ĐAHK1262703, Trường Đại học Mở Hà Nội. Mã nguồn phục vụ đồ án tốt nghiệp, chưa cấp phép sử dụng lại.

Thành phần bên thứ ba giữ giấy phép riêng: biểu tượng Material Symbols (Apache 2.0, xem [android/THIRD_PARTY_NOTICES.md](android/THIRD_PARTY_NOTICES.md)), ảnh địa danh từ Wikimedia Commons (giấy phép theo từng tệp, xem [db/README.md](db/README.md#ghi-công)).
