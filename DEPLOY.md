# Đưa Bước Chân Việt lên Internet

Bộ cấu hình này phục vụ web quản trị và API trên cùng một tên miền HTTPS. Máy người dùng chỉ cần trình duyệt và tài khoản được cấp quyền; máy phát triển không cần bật sau khi triển khai xong. Database phải được đưa lên dịch vụ PostgreSQL có PostGIS trước khi ứng dụng chạy.

## Phương án dùng thử: Render + Neon

- Render chạy image Docker của web và backend, cấp địa chỉ dạng `https://<ten-dich-vu>.onrender.com`.
- Neon lưu PostgreSQL và hỗ trợ extension PostGIS. Chọn gói phù hợp dung lượng và hạn mức thực tế của nhóm; kiểm tra điều kiện gói trên trang nhà cung cấp khi đăng ký.
- Gói Render Free có thể ngủ sau 15 phút không có truy cập, lần mở tiếp theo mất thời gian khởi động. Dữ liệu trên ổ đĩa container không bền vững, nên phiên đăng nhập dạng file có thể mất sau restart/redeploy; đăng nhập lại để tiếp tục. Dữ liệu nghiệp vụ trong Neon vẫn ở database riêng.
- Gói Render Free chặn cổng SMTP 25/465/587. Chưa cấu hình SMTP cho phương án này; email đặt lại mật khẩu qua Gmail SMTP không hoạt động. Không công khai log backend vì chế độ chưa cấu hình SMTP ghi mã đặt lại mật khẩu vào log.
- Không dùng Render Postgres Free cho dữ liệu cần giữ lâu: theo tài liệu hiện tại database miễn phí này hết hạn sau 30 ngày.

Tài liệu: [Render Free](https://render.com/docs/free), [PostGIS trên Neon](https://neon.com/docs/extensions/postgis), [Docker trên Render](https://render.com/docs/docker).

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
