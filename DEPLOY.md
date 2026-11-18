# Đưa Bước Chân Việt lên Internet

Bộ cấu hình này phục vụ web quản trị và API trên cùng một tên miền HTTPS. Máy người dùng chỉ cần trình duyệt và tài khoản được cấp quyền; máy phát triển không cần bật sau khi triển khai xong. Database phải được đưa lên dịch vụ PostgreSQL có PostGIS trước khi ứng dụng chạy.

## Phương án dùng thử: Render + Neon

- Render chạy image Docker của web và backend, cấp địa chỉ dạng `https://<ten-dich-vu>.onrender.com`.
- Neon lưu PostgreSQL và hỗ trợ extension PostGIS. Chọn gói phù hợp dung lượng và hạn mức thực tế của nhóm; kiểm tra điều kiện gói trên trang nhà cung cấp khi đăng ký.
- Gói Render Free có thể ngủ sau 15 phút không có truy cập, lần mở tiếp theo mất thời gian khởi động. Dữ liệu trên ổ đĩa container không bền vững, nên phiên đăng nhập dạng file có thể mất sau restart/redeploy; đăng nhập lại để tiếp tục. Dữ liệu nghiệp vụ trong Neon vẫn ở database riêng.
- Gói Render Free chặn cổng SMTP 25/465/587. Backend dùng Brevo (ưu tiên) hoặc Resend qua HTTPS để gửi mã đặt lại mật khẩu. Trên Render, thiếu cấu hình email sẽ trả lỗi gửi email và không ghi mã vào log.
- Không dùng Render Postgres Free cho dữ liệu cần giữ lâu: theo tài liệu hiện tại database miễn phí này hết hạn sau 30 ngày.

Tài liệu: [Render Free](https://render.com/docs/free), [PostGIS trên Neon](https://neon.com/docs/extensions/postgis), [Docker trên Render](https://render.com/docs/docker).

## Email khôi phục mật khẩu trên Render bằng Brevo

Sender đã được xác minh cho dự án: `quanggm7a1@gmail.com` (BuocChanViet).

1. Brevo → SMTP & API → API Keys: tạo API key Restricted, Transactional email = All.
   Dùng API key, không dùng SMTP key. Nếu key từng xuất hiện trong ảnh hoặc chat, thu hồi và tạo key mới.
2. Render → service `bcv-admin-test` → Environment, thêm:

   | Key | Value |
   | --- | --- |
   | `Brevo__ApiKey` | API key mới, nhập riêng trên Render |
   | `Brevo__FromEmail` | `quanggm7a1@gmail.com` |
   | `Brevo__FromName` | `Bước Chân Việt` |
   | `Email__RequireDelivery` | `true` |

3. Giữ biến database/map/R2. Xóa các biến `Resend__*` nếu chuyển hoàn toàn sang Brevo.
4. Đưa code có `BrevoPasswordResetMailer` lên GitHub nhánh `ther`, kiểm tra Render đang deploy
   nhánh đó rồi Manual Deploy → Deploy latest commit. Thêm biến vào backend cũ chưa đủ.
5. Hoàn tất xác minh điện thoại/kích hoạt gửi transactional theo yêu cầu tài khoản Brevo.
6. Trên app dùng API Render, dùng email đã đăng ký BCV ở trạng thái ACTIVE:
   Quên mật khẩu → nhận mã 6 số → Xác minh → nhập mật khẩu mới → đăng nhập lại.
   App debug local cần backend local được cấu hình các biến `Brevo__*` tương tự.
7. Kiểm tra Brevo → Transactional → Logs nếu email chưa tới, và kiểm tra Spam.
   API chấp nhận gửi không bảo đảm email đã tới inbox; Brevo có thể từ chối do key, quyền, sender,
   trạng thái tài khoản, địa chỉ bị chặn hoặc hạn mức. Backend chỉ log HTTP status, không log key/mã.

Không cần tạo template Brevo: backend gửi nội dung mã trực tiếp bằng `textContent`.
Mã hết hạn sau 10 phút, gửi lại sau 60 giây; restart backend làm mất mã đang chờ.
Tài liệu: [Brevo transactional email API](https://developers.brevo.com/docs/send-a-transactional-email).

## Phương án khác: Resend

### Chạy thử khi chưa có tên miền riêng

Đăng ký Resend bằng email bạn muốn nhận mã thử, tạo API key, rồi đặt
`Resend__From=BCV <onboarding@resend.dev>` và `Resend__ApiKey` trong Render Environment.
Đăng ký tài khoản BCV bằng đúng email đó. Cấu hình này chỉ phục vụ kiểm thử;
Resend từ chối gửi tới người nhận khác cho đến khi bạn xác minh domain riêng.
Không cần thay đổi luồng giao diện để chạy thử.

### Gửi cho tất cả người dùng

1. Tạo tài khoản [Resend](https://resend.com), thêm domain gửi email trong Domains và xác minh DNS.
   Địa chỉ `onboarding@resend.dev` chỉ gửi thử tới email chủ tài khoản Resend; muốn gửi cho người dùng
   khác phải dùng domain đã xác minh. Xem [giới hạn địa chỉ thử](https://resend.com/docs/knowledge-base/403-error-resend-dev-domain).
2. Tạo API key có quyền gửi email cho domain đó. Không đưa key vào GitHub, APK hoặc biến `VITE_*`.
3. Render Dashboard → service backend → Settings: chọn repository `ngtherr218/BCV`, branch `ther`.
   Với service đã tồn tại, sửa trực tiếp trên Dashboard; sửa Blueprint không tự đổi service thủ công.
4. Render → Environment, thêm các biến sau rồi Save, rebuild, and deploy:

   | Key | Value |
   | --- | --- |
   | `Resend__ApiKey` | API key tạo trong Resend |
   | `Resend__From` | `Bước Chân Việt <no-reply@your-verified-domain>` (thay bằng domain thực tế) |
   | `Email__RequireDelivery` | `true` |

   Giữ nguyên `ConnectionStrings__Bcv` và các biến đang dùng. SMTP không được sử dụng trên Render.
   `render.yaml` đã khai báo hai biến Resend là secret nhập riêng (`sync: false`).
5. Triển khai bản code có `ResendPasswordResetMailer`. Chỉ thêm biến vào bản backend cũ chưa đủ.
6. App dùng API Render (`https://bcv-admin-test.onrender.com/` hoặc URL thực tế của service).
   Bản debug đang trỏ localhost sẽ gọi backend local; build bản online bằng tham số
   `-PbcvApiBaseUrl=https://bcv-admin-test.onrender.com/`.
7. Dùng một tài khoản ACTIVE có email thật: Quên mật khẩu → Gửi mã → nhập mã 6 số trong email
   → Xác minh → màn nhập mật khẩu mới → Đăng nhập. Mã có hạn 10 phút, chờ 60 giây để gửi lại;
   mã và token đổi mật khẩu chỉ dùng một lần. Đổi mật khẩu xong phải đăng nhập lại.

Nếu Resend trả lỗi, xem trạng thái gửi trong Resend Dashboard và HTTP status trong Render Logs;
kiểm tra domain, quyền API key và hạn mức. Không ghi mã xác minh vào log Render.
Mã đang lưu trong RAM của một instance; restart/redeploy làm mất mã đang chờ, cần yêu cầu mã mới.

## 1. Tạo database online

1. Đăng ký [Neon](https://console.neon.tech/), tạo project và database. Chọn phiên bản PostgreSQL tương thích nguồn dữ liệu hiện có (dự án dùng PostgreSQL 18).
2. Bật PostGIS trong database đích:

   ```sql
   CREATE EXTENSION IF NOT EXISTS postgis;
   ```

3. Chọn **một** cách đưa dữ liệu lên:
   - **Giữ dữ liệu đang sử dụng:** dùng pgAdmin Backup dạng Custom, bỏ ownership/privileges khi restore vào database cloud trống. Kết nối cloud bằng thông tin Neon cung cấp, bật SSL; dùng `pg_dump`/`pg_restore` phiên bản tương thích nguồn. Không chạy lại bộ SQL khởi tạo sau khi restore.
   - **Tạo bản demo mới:** nạp lần lượt các file SQL theo danh sách trong [db/README.md](db/README.md#tạo-database-mới), rồi kích hoạt nội dung cần sử dụng. Không nạp lại `001_schema.sql` lên database đã có bảng.
4. Kiểm tra database có đủ các bảng ứng dụng và PostGIS:

   ```sql
   SELECT postgis_version();
   SELECT count(*) FROM public.place;
   SELECT count(*) FROM public.region;
   ```

Thông tin kết nối backend phải ở định dạng **Npgsql**, không dán nguyên URI `postgresql://...` vào `ConnectionStrings__Bcv`:

```text
Host=<host-neon>;Port=5432;Database=<database>;Username=<username>;Password=<password>;SSL Mode=VerifyFull
```

Lấy host/user/database/password từ mục Connect của Neon; mật khẩu chứa dấu chấm phẩy hoặc dấu nháy cần được quote theo định dạng chuỗi kết nối Npgsql. Giữ thông tin này trong mục Environment của hosting, không đưa vào Git hay gửi trong chat.

## 2. Có tài khoản quản trị

Nếu restore database có tài khoản ADMIN, tiếp tục dùng tài khoản đó. Nếu database mới, tạo tài khoản bằng CLI sẵn có từ máy phát triển, kết nối tới database cloud qua biến môi trường chỉ trong tiến trình terminal:

```powershell
$cloudConnection = Read-Host 'Npgsql connection string' -AsSecureString
$adminPassword = Read-Host 'Admin password (12-128 characters, letters and digits)' -AsSecureString
try {
    $env:ConnectionStrings__Bcv = [System.Net.NetworkCredential]::new('', $cloudConnection).Password
    $env:BCV_ADMIN_PASSWORD = [System.Net.NetworkCredential]::new('', $adminPassword).Password
    dotnet run --project backend/Bcv.Api -- --create-admin admin@example.com
    if ($LASTEXITCODE -ne 0) { throw 'Create admin failed; check the error before continuing.' }
} finally {
    Remove-Item Env:ConnectionStrings__Bcv, Env:BCV_ADMIN_PASSWORD -ErrorAction SilentlyContinue
}
```

Thay email mẫu bằng email thật. Lệnh không tự nâng quyền hay đổi mật khẩu email đã tồn tại. Người dùng thông thường không thể đăng nhập web quản trị; chỉ cấp ADMIN cho thành viên cần quản lý nội dung.

## 3. Triển khai Render từ nhánh Web

1. Đưa các file `Dockerfile`, `.dockerignore`, `render.yaml` và thay đổi backend lên nhánh `Web` trên GitHub.
2. Đăng ký/đăng nhập [Render](https://dashboard.render.com/), kết nối repository `Lee-Quang/BCV_Sv_Test`.
3. Chọn **New → Blueprint**, chọn repository và nhánh **Web**; dùng `render.yaml` tại gốc repo. Kiểm tra plan là **Free** trước khi xác nhận.
4. Điền các giá trị riêng tư khi Render yêu cầu:

   | Biến | Giá trị |
   | --- | --- |
   | `ConnectionStrings__Bcv` | Chuỗi Npgsql kết nối database cloud đã nạp dữ liệu |
   | `Goong__ApiKey` | Goong REST API Key, dùng ở backend để xác định khu vực |
   | `VITE_GOONG_MAPTILES_KEY` | Goong Maptiles Key, dùng khi build web |

5. Tạo Blueprint và chờ build/deploy. Backend nhận cổng `PORT` do Render cấp; web tĩnh và `/api` nằm cùng service, không cần `BCV_API_TARGET` hoặc Vite dev server.
6. Kiểm tra `https://<ten-dich-vu>.onrender.com/health` trả `{"status":"ok"}`, sau đó mở trang gốc để đăng nhập.
7. Cập nhật giới hạn domain/IP trong tài khoản Goong cho tên miền hosting và nguồn gọi API thực tế nếu khóa của bạn có giới hạn. Thử bản đồ và thao tác chọn vị trí để kiểm tra cả Maptiles Key lẫn REST Key.

Blueprint tắt tự động deploy: sau mỗi lần push `Web`, dùng **Manual Deploy → Deploy latest commit** trong Render. Sửa Maptiles Key cần rebuild web; sửa REST key/chuỗi kết nối cần restart/redeploy backend. Kiểm tra cả `/health` và đăng nhập sau khi cập nhật.

### Máy chủ dùng chung và đồng bộ nhánh

Nhóm thống nhất dùng `https://bcv-admin-test.onrender.com/` cho cả app và web. Web gọi `/api` trên chính tên miền đang mở; APK dùng `bcvApiBaseUrl`, nên mở web Test và giữ URL Test khi build app. Dữ liệu trên `bcv-admin.onrender.com` không tự chuyển sang Test.

`Web` là nhánh nguồn của service Test. Giữ `Deploy_web` cùng commit với `Web` sau khi kiểm thử; khi nhánh đích chưa có commit riêng, dùng fast-forward, không cần force push:

```powershell
git fetch origin
git switch Deploy_web
git merge --ff-only origin/Web
git push origin Deploy_web
git switch Web
```

Hai nhánh giống code không đồng nghĩa hai service dùng cùng database: `ConnectionStrings__Bcv` được đặt riêng trong Environment của từng service Render. Theo cấu hình dùng chung hiện tại, dùng bản Test để đọc và ghi dữ liệu. Nếu vẫn vận hành hai service, cần cấu hình cùng database ở Render và triển khai cùng commit; thao tác Git không gộp dữ liệu giữa hai database.

Kho mã nhóm đang đồng bộ là `ngtherr218/BCV`. Nếu service Render vẫn lấy nguồn từ fork `Lee-Quang/BCV_Sv_Test`, cập nhật fork đến cùng commit trước khi chọn Manual Deploy; kiểm tra mục Settings của service để xác nhận repository và nhánh thực tế.

## 4. Kiểm tra trước khi chia sẻ link

- Truy cập link HTTPS từ điện thoại dùng 4G hoặc máy khác mạng Wi-Fi.
- Đăng nhập, xem danh sách, tạo địa danh thử bằng ghim; khu vực và tọa độ được điền đúng.
- Tải ảnh và đọc lại sau khi reload. Nếu chưa cấu hình R2, ảnh đang được lưu trong database; không dùng thư mục container làm nơi lưu ảnh lâu dài.
- Với gói Free, chấp nhận đăng nhập lại sau restart và chờ lần mở đầu khi service ngủ.
- Các thành viên cùng dùng **một database cloud**; dữ liệu local không tự đồng bộ lên cloud sau bước import. Thực hiện sao lưu cloud định kỳ.

## Docker trên máy chủ khác

Từ thư mục gốc, khi Docker Engine đã chạy:

```powershell
docker build --build-arg VITE_GOONG_MAPTILES_KEY=<maptiles-key> -t bcv-web .
```

Truyền `ConnectionStrings__Bcv`, `Goong__ApiKey` khi chạy container; không đặt chúng vào Dockerfile. Container lắng nghe cổng 8080 khi không có `PORT`. Mount volume có quyền ghi cho UID của user `app` vào `/app/data` nếu muốn giữ phiên đăng nhập dạng file. Trên VPS, đặt HTTPS reverse proxy trước container và giữ database ở mạng riêng.

Đã build và chạy image thành công trên Render ngày 29/10/2026 tại https://bcv-admin-test.onrender.com. Service hiện tại tên `bcv-admin-test`, dùng gói Free, vùng Singapore và nhánh Web; auto-deploy đã tắt. Service cũ `bcv-sv-test` đã tạm dừng sau khi chuyển sang link mới; tài khoản và database Neon được giữ nguyên. Database đã được chuyển từ bản hiện tại lên Neon với đủ 25 bảng ứng dụng, 312 địa danh, 63 khu vực, 6 tài khoản và 135 thẻ; giữ nguyên tài khoản quản trị. `/health` đã trả HTTP 200. Mọi thay đổi sau deploy ghi vào database Neon, không tự đồng bộ về database local. Bản backup trước khi chuyển nằm ở `output/cloud-deploy/bcv-before-neon.dump` trên máy phát triển, không đưa vào Git. Không lưu mật khẩu hoặc khóa API vào tài liệu này.
