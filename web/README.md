# Web quản trị Bước Chân Việt (*bcv-admin*)

Website quản trị nội dung Bước Chân Việt (React 19 + Vite), lấy dữ liệu từ Backend qua `/api/admin`.

Phạm vi theo tài liệu đồ án, mục 2.3.8–2.3.11 (chức năng F33–F50). Node.js và Vite chỉ dùng khi phát triển và build; sau khi build, web là các file tĩnh, còn nghiệp vụ vẫn do Backend ASP.NET Core xử lý. Tên gói trong `package.json` là `bcv-admin`.

## Mục lục

- [Cài đặt](#cài-đặt)
- [Sử dụng](#sử-dụng)
- [Chức năng](#chức-năng)
- [Build và triển khai](#build-và-triển-khai)
- [Kiểm thử tích hợp](#kiểm-thử-tích-hợp)
- [Xử lý sự cố](#xử-lý-sự-cố)
- [API quản trị](#api-quản-trị)
- [Đóng góp](#đóng-góp)
- [Giấy phép](#giấy-phép)

## Cài đặt

Cần Node.js 22.12 trở lên và Backend đang chạy ([backend/README.md](../backend/README.md#cài-đặt)).

```powershell
cd web
npm ci
```

### Cấu hình (tùy chọn)

Chỉ cần khi khác mặc định. Tạo `web/.env.local` (Git bỏ qua file này):

```powershell
Copy-Item web/.env.example web/.env.local
```

| Biến | Ý nghĩa | Mặc định |
| --- | --- | --- |
| `BCV_API_TARGET` | Địa chỉ Backend | `http://127.0.0.1:5081` |
| `WEB_HOST` | Địa chỉ web lắng nghe; `0.0.0.0` để máy khác trong mạng truy cập | `127.0.0.1` |
| `WEB_PORT` | Cổng web | `5173` |
| `VITE_GOONG_MAPTILES_KEY` | Khóa bản đồ Goong | Lấy `bcvGoongMaptilesKey` từ `android/dev.properties` |

Sửa `.env.local` xong phải chạy lại `npm run dev`. Không đặt mật khẩu hay khóa bí mật vào biến `VITE_*`, vì chúng được gửi xuống trình duyệt.

### Tài khoản quản trị

Chỉ tài khoản vai trò `ADMIN` đăng nhập được; tài khoản đăng ký từ app là `USER` và bị từ chối. Tạo admin (một lần cho mỗi database), từ thư mục gốc:

```powershell
.\backend\create-admin.ps1 -Email admin@bcv.local
```

Script hỏi mật khẩu (ít nhất 12 ký tự, có cả chữ và số). Email đã tồn tại sẽ bị từ chối; script không đổi mật khẩu hay quyền của tài khoản cũ. Nếu báo lỗi build vì backend đang chạy, dừng backend rồi chạy lại.

## Sử dụng

Web dùng chung trên máy tính, tablet và điện thoại. Trên màn hình từ 760 px trở xuống, mở menu bằng nút ba gạch; danh sách hiển thị dạng thẻ có nhãn thay cho bảng rộng, biểu mẫu dùng một cột và các nút thao tác chính có vùng chạm tối thiểu 44 px. Bấm chọn điểm hoặc kéo ghim vẫn tự điền khu vực và tọa độ. Khung thông tin vị trí nằm dưới bản đồ để không che vùng chọn trên điện thoại. Dùng cùng link HTTPS sau khi [triển khai hosting](../DEPLOY.md), không cần cài thêm ứng dụng.

```powershell
cd web
npm run dev
```

Mở http://127.0.0.1:5173 và đăng nhập bằng tài khoản quản trị. Vite tự chuyển tiếp `/api` và `/health` sang Backend. Nhấn Ctrl+C để dừng.

## Chức năng

| Màn hình | Chức năng |
| --- | --- |
| Bảng điều khiển | Số người dùng, hành trình, lượt khám phá, thẻ thu thập; biểu đồ 7/30/90 ngày; nội dung thiếu ảnh |
| Người dùng | Tìm kiếm, xem thống kê cá nhân, khóa/mở tài khoản |
| Khu vực | Tạo, sửa tên, mô tả, trạng thái |
| Địa danh | Nội dung, fun fact, tọa độ, loại hình, khu vực, đánh giá, trạng thái, ảnh (URL hoặc tải lên) |
| Bản đồ địa danh | Xem ghim trên bản đồ Goong, kéo ghim để chọn tọa độ, tạo địa danh từ ghim |
| Loại địa danh | 12 loại chuẩn ([danh sách và ánh xạ](../db/README.md#loại-địa-danh)) |
| Thẻ | Ảnh, nội dung, địa danh liên kết (mỗi địa danh tối đa một thẻ) |
| Bộ sưu tập | Chọn các thẻ thuộc bộ sưu tập |
| Nhiệm vụ | Nhiệm vụ ngày/tuần, điều kiện, mục tiêu, phần thưởng |
| Vật phẩm | Cấu hình Giày tăng tốc và Vé máy bay |
| Thông tin hệ thống | Kiểm tra kết nối Backend |

Mọi danh sách có tìm kiếm, lọc trạng thái, phân trang và xuất CSV. Bấm một dòng để xem chi tiết; từ đó có thể chỉnh sửa, xóa hoặc mở đối tượng liên quan.

### Quy tắc nội dung

- **Trạng thái:** `DRAFT` (bản nháp, chỉ thấy trong quản trị), `ACTIVE` (app hiển thị), `INACTIVE` (ngừng dùng).
- **Thứ tự kích hoạt:** địa danh trước, rồi thẻ và bộ sưu tập. Thẻ chỉ kích hoạt được khi có ảnh và địa danh đang hoạt động.
- **Xóa:** cần xác nhận tên và mã. Không xóa được tài khoản quản trị và bản ghi đã có liên kết hoặc lịch sử; khi đó chuyển sang `INACTIVE`.
- **Nhiệm vụ đã có người làm:** chỉ đổi được tên và trạng thái; muốn đổi điều kiện hay phần thưởng thì tạo nhiệm vụ mới.
- **Khóa người dùng** đăng xuất người đó ngay; không thể tự khóa chính mình.
- **Ảnh** được nén thành JPEG (cạnh dài tối đa 1600 px, tối đa 5 MB). Nếu Backend đã cấu hình Cloudflare R2, ảnh địa danh vào thư mục `places/`, ảnh thẻ vào `cards/`, tách khỏi ảnh đại diện người dùng ở `avatars/`. Nếu chưa cấu hình, ảnh được lưu trong database.
- **Địa danh mới thêm** được lưu cùng 35 giá trị đặc trưng và cụm của mô hình hiện có, có thể tham gia gợi ý khi đáp ứng trạng thái và các điều kiện xếp hạng. **Loại địa danh mới** chỉ được tính điểm sở thích khi được thêm vào bảng ánh xạ ([db/README.md](../db/README.md#loại-địa-danh)).

### Chọn tọa độ và đặc trưng địa danh

Trong form thêm/sửa địa danh, bấm ô vĩ độ hoặc kinh độ để mở bản đồ. Bấm lên bản đồ hoặc kéo ghim để cập nhật cả hai tọa độ; có thể đóng bản đồ và tiếp tục nhập tay.

Khi chọn điểm, kéo ghim hoặc nhập một cặp tọa độ hợp lệ, biểu mẫu tự tra khu vực qua Goong và điền vào ô **Khu vực**. Tọa độ mới xóa khu vực cũ trong lúc tra cứu để tránh lưu nhầm; nút lưu chờ tra cứu hoàn tất. Đóng bản đồ vẫn tiếp tục tra cứu. Nếu không nhận diện được, chọn khu vực thủ công trước khi lưu. Chọn khu vực thủ công sẽ hủy kết quả tra cứu đang chờ, tránh ghi đè lựa chọn của bạn. Mở bản ghi có sẵn hoặc chỉ đổi khung nhìn không tự tra lại khu vực.

- Chọn **Khu vực** trước khi mở bản đồ: khung nhìn được xác định từ các địa danh có tọa độ hợp lệ trong khu vực đó. Ví dụ, chọn Hà Nội rồi bấm ô vĩ độ/kinh độ sẽ mở bản đồ tại vùng các địa danh Hà Nội.
- Đổi khu vực khi bản đồ đang mở sẽ chuyển khung nhìn sang khu vực mới, giữ nguyên tọa độ đang nhập cho tới khi chọn điểm hoặc sửa tọa độ bằng tay.
- Khi mở bản ghi đã có tọa độ hợp lệ và chưa đổi khu vực, bản đồ ưu tiên vị trí đã lưu. Nếu khu vực chưa có địa danh với tọa độ hợp lệ, bản đồ hiển thị toàn cảnh Việt Nam kèm thông báo.
- Chuyển khung nhìn không tự lưu dữ liệu; cần bấm **Lưu thay đổi** trong biểu mẫu.

#### Bản đồ Goong và khu vực trước sáp nhập

Ở trang **Bản đồ địa danh**, bật **Chọn tọa độ trên bản đồ** rồi đặt ghim. Hệ thống tự tra khu vực theo tọa độ qua Goong; khi kéo ghim, khu vực được tra lại. Nút **Tạo địa danh tại đây** chờ tra cứu hoàn tất rồi mở biểu mẫu với khu vực, vĩ độ và kinh độ đã điền sẵn, không cần chọn khu vực trước. Database hiện dùng tên khu vực cũ nên ưu tiên đối chiếu tỉnh trước sáp nhập do Goong trả về. Nếu không tra được hoặc không có tên tương ứng trong database, biểu mẫu vẫn nhận tọa độ và cho chọn khu vực thủ công, không tự đoán theo địa danh gần nhất.

Backend cần Goong REST API Key qua `Goong__ApiKey` (khác Maptiles Key). Khi dùng `backend/run-local.ps1`, script đọc biến này, sau đó `BCV_GOONG_API_KEY`, hoặc khóa `bcvGoongApiKey` trong `android/dev.properties`. REST key chỉ dùng ở backend, không nhúng vào bản build web. Khởi động lại backend sau khi cập nhật mã hoặc cấu hình.

Web sử dụng bản đồ nền Goong hiện hành; chưa tích hợp lựa chọn bản đồ trước sáp nhập. Tên và ranh giới trên bản đồ nền có thể khác danh sách khu vực trong database. Điểm địa danh vẫn được đặt theo vĩ độ/kinh độ, còn khung nhìn khu vực được tính từ dữ liệu địa danh của ứng dụng, không phụ thuộc tên tỉnh hiển thị trên nền bản đồ.

Hướng đồng bộ dữ liệu đề xuất là dùng khu vực hành chính hiện tại và giữ tên khu vực cũ để tra cứu. Đây là hướng phát triển, chưa có chức năng tự chuyển đổi hay bảng đối chiếu trong thay đổi này; không đổi hàng loạt ID khu vực hoặc tọa độ chỉ vì sáp nhập.

Tính năng tra khu vực từ ghim dùng tham số `has_deprecated_administrative_unit` của API V2 để bổ sung thông tin địa giới trước sáp nhập. Tham số này phục vụ dữ liệu địa chỉ, không phải công tắc chuyển bản đồ nền về phiên bản cũ. Xem [hướng dẫn Geocode V2 của Goong](https://help.goong.io/kb/rest-api-v2/geocode-rest-api-v2/geocode-v2/).

Chọn loại địa danh để tự tích bộ đặc trưng theo ID chuẩn 101–112. Mỗi lần đổi loại, toàn bộ ô đã tích (kể cả lựa chọn thủ công) được thay bằng bộ mặc định của loại mới. Chọn lại mục “Chọn loại địa danh” sẽ bỏ hết tích. Sau khi chọn loại, có thể chỉnh các ô cho đúng thực tế rồi lưu.

Các đặc trưng chia thành bốn nhóm: loại nội dung, địa hình, hệ sinh thái và chủ đề. Loại gộp dùng một trường hợp mặc định, ví dụ sông trong “Sông, hồ và thác”; hãy đổi sang hồ/thác nếu phù hợp. Không tự tích mọi phương án trong một loại gộp. Rừng không tự kéo theo núi vì hệ sinh thái và địa hình là hai thuộc tính độc lập.

Cần ít nhất một đặc trưng để lưu. Backend kiểm tra mã, lưu đủ 35 giá trị 0/1 và tính lại cụm trong cùng giao dịch. Việc sửa đặc trưng không huấn luyện lại mô hình. Đổi tên loại chuẩn không làm mất bộ tự tích vì ánh xạ dùng ID; thêm ID mới vẫn cần cập nhật quy tắc trong code.

Nếu danh sách đặc trưng trống, kiểm tra backend đã chạy bản có trường `features` trong `/api/admin/lookups`, database đã nhập 35 tiêu chí, rồi tải lại trang. Database cũ cần migration `014` để đồng bộ mã loại/sở thích; gợi ý cho người dùng cần cấu hình `ACTIVE`.

## Build và triển khai

Để xuất bản cho các máy khác mạng truy cập, dùng [hướng dẫn Render + Neon](../DEPLOY.md). `Dockerfile` ở gốc repo build web và backend vào cùng image; backend phục vụ `wwwroot` và API trên một tên miền. `render.yaml` dùng nhánh `Web`, gói Free và database PostgreSQL/PostGIS bên ngoài.

```powershell
npm run build
```

Kết quả nằm trong `dist/`. Khi triển khai, phục vụ thư mục này bằng web server tĩnh (IIS, Nginx…) qua HTTPS và cấu hình reverse proxy `/api` tới Backend. Proxy trong `vite.config.js` và `npm run preview` chỉ dùng khi phát triển.

## Kiểm thử tích hợp

Khi Backend đang chạy và đã có tài khoản admin, từ thư mục gốc:

```powershell
.\web\scripts\verify-admin.ps1
```

Script hỏi mật khẩu admin và cần biến `BCV_DB_PASSWORD` để dọn dữ liệu thử; nó chỉ tạo và xóa bản ghi thử của chính nó. Tham số tùy chọn: `-ApiBase`, `-Email`, `-Psql`, `-ConfigPath`. Kết quả kiểm thử gần nhất: [TESTING.md](TESTING.md); lịch sử thay đổi: [CHANGELOG.md](CHANGELOG.md).

## Xử lý sự cố

| Hiện tượng | Cách xử lý |
| --- | --- |
| Lỗi mạng khi đăng nhập | Kiểm tra http://127.0.0.1:5081/health và `BCV_API_TARGET` |
| "Không đủ quyền" | Tài khoản không phải `ADMIN` |
| Đột nhiên bị đăng xuất | Phiên hết hạn (30 ngày không dùng) hoặc tài khoản bị khóa; đăng nhập lại |
| Bản đồ trống | Chưa có Maptiles key, hoặc vừa thêm key mà chưa chạy lại `npm run dev` |
| Báo trùng cổng 5173 | Một server web cũ còn chạy; dừng nó hoặc đổi `WEB_PORT` |

## API quản trị

Mọi API ngoài `login` yêu cầu Bearer token của tài khoản `ADMIN`.

```text
POST   /api/admin/login
GET    /api/admin/me
GET    /api/admin/dashboard?days=30
GET    /api/admin/lookups
GET    /api/admin/data/{resource}?q=&status=&page=1&pageSize=12
GET    /api/admin/data/{resource}/{id}
POST   /api/admin/data/{resource}
PUT    /api/admin/data/{resource}/{id}
DELETE /api/admin/data/{resource}/{id}
POST   /api/admin/images?purpose=place|card   (image/jpeg)
```

`resource` gồm `users`, `regions`, `place-types`, `places`, `cards`, `collections`, `quests`, `items`. Người dùng và vật phẩm chỉ cho cập nhật. Đăng xuất dùng chung `POST /api/account/logout`.

## Đóng góp

Xem [Đóng góp](../README.md#đóng-góp) ở README gốc.

## Giấy phép

UNLICENSED © 2026 Nhóm ĐAHK1262703. Xem [Giấy phép](../README.md#giấy-phép) ở README gốc.
