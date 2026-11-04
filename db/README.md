# Database Bước Chân Việt (*db*)

Schema gốc 25 bảng; migration 021 thêm 3 bảng cho sở thích và snapshot đánh giá. Dữ liệu gồm 311 địa danh trong database `bcv` (PostgreSQL 18 + PostGIS 3.6).

Thiết kế theo mục 3.1.2 và 3.1.5 của [tài liệu đồ án](https://docs.google.com/document/d/1GAQdAqCcwr_IivfHzInilF0LhbWyniN6AONmzqEHWjo/edit?tab=t.0). Mỗi file SQL chạy trong một giao dịch, kiểm tra trạng thái trước và sau, và (trừ `001`) chạy lại được an toàn.

## Mục lục

- [Cài đặt](#cài-đặt)
- [Sử dụng](#sử-dụng)
- [Các file](#các-file)
- [Loại địa danh](#loại-địa-danh)
- [Trạng thái nội dung](#trạng-thái-nội-dung)
- [Dữ liệu mẫu](#dữ-liệu-mẫu)
- [Máy phát triển hiện tại](#máy-phát-triển-hiện-tại)
- [Ghi công](#ghi-công)
- [Đóng góp](#đóng-góp)
- [Giấy phép](#giấy-phép)

## Cài đặt

### Tạo database mới

1. Cài PostgreSQL 18 và PostGIS 3.6.
2. Tạo database trống tên `bcv` (bằng pgAdmin hoặc `createdb`).
3. Tạo `backend/local.settings.json` với host, cổng, tên database, user của máy ([backend/README.md](../backend/README.md#cấu-hình)).
4. Từ thư mục gốc kho mã, nạp các file theo thứ tự:

```powershell
. .\backend\local-config.ps1
$config = Get-BcvLocalConfig
$psql = Resolve-BcvPsql $config
$pw = Read-Host 'Mat khau PostgreSQL' -AsSecureString
$env:PGPASSWORD = [System.Net.NetworkCredential]::new('', $pw).Password
try {
    foreach ($file in @(
        '001_schema.sql',
        '002_seed_reference.sql',
        '003_import_places.sql',
        '004_seed_demo_remaining.sql',
        '009_describe_regions.sql',
        '010_place_fun_facts.sql',
        '012_northern_place_images.sql',
        '013_northern_cards_collections.sql',
        '014_standard_place_types.sql',
        '015_tune_recommendation_weights.sql',
        '016_preserve_missing_rating_counts.sql',
        '017_complete_place_cards.sql',
        '018_user_last_login.sql',
        '019_card_seen.sql',
        '020_notification_hidden.sql',
        '021_versioned_recommendations.sql'
    )) {
        & $psql -X -w -v ON_ERROR_STOP=1 -h $config.DbHost -p $config.DbPort -U $config.DbUser -d $config.DbName -f (Join-Path db $file)
        if ($LASTEXITCODE -ne 0) { throw "Import failed: $file" }
    }
} finally {
    Remove-Item Env:PGPASSWORD -ErrorAction SilentlyContinue
}
```

5. Kích hoạt nội dung mẫu ([Trạng thái nội dung](#trạng-thái-nội-dung)).
6. Tạo tài khoản quản trị: `.\backend\create-admin.ps1` ([web/README.md](../web/README.md#tài-khoản-quản-trị)).

Lưu ý:

- Không chạy lại `001` trên database đã có bảng.
- Nếu đang nạp mà bị lỗi, kiểm tra file nào đã xong rồi tiếp tục từ file kế tiếp.
- Nếu khôi phục từ bản backup đầy đủ thì bỏ qua các bước nạp.

## Sử dụng

Mở psql với cấu hình của máy:

```powershell
. .\backend\local-config.ps1
$config = Get-BcvLocalConfig
& (Resolve-BcvPsql $config) -X -W -h $config.DbHost -p $config.DbPort -U $config.DbUser -d $config.DbName
```

Thay đổi dữ liệu nghiệp vụ hằng ngày nên làm qua web quản trị. Thay đổi cấu trúc hoặc dữ liệu hàng loạt viết thành file SQL mới, đánh số tiếp theo.

## Các file

| File | Nội dung |
| --- | --- |
| `001_schema.sql` | 25 bảng, khóa, ràng buộc, chỉ mục (gồm 2 chỉ mục không gian PostGIS) |
| `002_seed_reference.sql` | 10 loại địa danh gốc (được `014` thay thế), 40 tiêu chí gợi ý |
| `003_import_places.sql` | 63 khu vực, 311 địa danh, đặc trưng và nhãn cụm của mô hình, cấu hình gợi ý |
| `004_seed_demo_remaining.sql` | Dữ liệu mẫu cho 17 bảng còn lại (người dùng giả, hành trình, 15 thẻ đầu, nhiệm vụ, vật phẩm…) |
| `009_describe_regions.sql` | Mô tả cho 63 khu vực |
| `010_place_fun_facts.sql` | Fun fact cho 311 địa danh |
| `012_northern_place_images.sql` | 46 ảnh cho 29 địa danh miền Bắc |
| `013_northern_cards_collections.sql` | 120 thẻ miền Bắc, gán vào 5 bộ sưu tập |
| `014_standard_place_types.sql` | Thay 10 loại gốc bằng 12 loại chuẩn (ID 101–112), gán lại loại cho 311 địa danh, chuyển sở thích đã lưu |
| `015_tune_recommendation_weights.sql` | Trọng số gợi ý sau khi chỉnh (P 0,35 · C 0,15 · A 0,3 · R 0,1 · N 0,1) và kích hoạt cấu hình gợi ý |
| `016_preserve_missing_rating_counts.sql` | Cho phép số lượt đánh giá thiếu là NULL; khôi phục 35 giá trị thiếu từ bản nhập gốc, giữ nguyên đánh giá đã bổ sung |
| `017_complete_place_cards.sql` | Tạo 176 thẻ còn thiếu và 5 bộ sưu tập khu vực; mỗi địa danh có một thẻ, giữ nguyên thẻ cũ và cấp thẻ mới cho người đã khám phá |
| `021_versioned_recommendations.sql` | Sáu nhóm sở thích độc lập, metadata cấu hình và snapshot rating/count; giữ nguyên lựa chọn cũ để đối chiếu |
| `012_northern_place_images_sources.md` | Nguồn của từng ảnh trong `012` |

Số thứ tự bị nhảy (`005`–`008`, `011`) vì các migration sửa dữ liệu cũ đã được gộp vào `004` và `010` rồi xóa.

Ghi chú về schema:

- Bảng `USER` trong tài liệu được đặt tên `app_user` để tránh trùng từ khóa SQL; tên cột dùng `snake_case`.
- Mục 4.1.3 của tài liệu ghi 28 bảng, nhưng mục 3.1.2 và 3.1.5 liệt kê 25 quan hệ; schema theo 25 quan hệ này.
- Chạy `016` cả trên database đang dùng. Sau migration, `place.rating_count` có thể NULL; 0 là số lượt đánh giá đã biết bằng 0. Web quản trị cho phép để trống trường này. Các bản ghi đã có điểm hoặc số lượt lớn hơn 0 không bị backfill thay đổi.

## Loại địa danh

12 loại chuẩn do `014` tạo. Mỗi loại ứng với một nhóm đặc trưng của mô hình gợi ý; địa danh **khớp** một loại khi có **ít nhất một** đặc trưng trong nhóm, nên có thể khớp nhiều loại. Cột `place.place_type_id` chỉ lưu loại chính.

| ID | Loại | Đặc trưng | Số địa danh (loại chính) |
| --- | --- | --- | --- |
| 101 | Di tích và bảo tàng | `kind__heritage`, `kind__museum` | 42 |
| 102 | Đền, chùa và nhà thờ | `kind__religious` | 51 |
| 103 | Công trình kiến trúc | `kind__structure` | 15 |
| 104 | Phố cổ và làng nghề | `kind__community`, `kind__craft` | 17 |
| 105 | Chợ và ẩm thực | `kind__market`, `theme__cuisine` | 13 |
| 106 | Vui chơi và nghỉ dưỡng | `kind__recreation` | 16 |
| 107 | Đồng quê và nông nghiệp | `kind__agriculture` | 12 |
| 108 | Biển và đảo | `landform__coast`, `landform__island`, `landform__lagoon`, `landform__dunes` | 44 |
| 109 | Sông, hồ và thác | `landform__river`, `landform__lake`, `landform__waterfall`, `landform__stream` | 46 |
| 110 | Núi và đèo | `landform__mountain`, `landform__valley` | 18 |
| 111 | Hang động | `landform__cave` | 22 |
| 112 | Rừng và khu bảo tồn | `ecosystem__forest`, `ecosystem__flooded_habitat`, `ecosystem__grassland` | 15 |

Đây là danh mục phân loại địa danh. Từ migration 021, màn chọn sở thích và điểm P dùng sáu nhóm riêng trong `interest_group`, liên kết qua `user_preference_interest`; mapping được lưu theo phiên bản cấu hình. Không dùng danh mục 12 loại trên làm sở thích nữa.

`014` tách 145 địa danh "Thiên nhiên" cũ theo địa hình (hang động → biển → sông/hồ/thác → núi → rừng). Sở thích "Thiên nhiên" đã lưu bị bỏ vì nay ứng với 5 loại trong khi mỗi người chọn tối đa 3; người dùng không còn sở thích nào sẽ được app hỏi chọn lại.

## Trạng thái nội dung

Các bảng `place`, `card`, `collection`, `quest`, `item`, `region`, `recommendation_config` có cột `status`: `DRAFT`, `ACTIVE` hoặc `INACTIVE`. **App chỉ dùng nội dung `ACTIVE`**; gợi ý điểm đến yêu cầu cấu hình `ACTIVE`, không tự dùng bản nháp. Migration 021 bảo đảm tối đa một cấu hình ACTIVE.

Dữ liệu mẫu được nhập ở trạng thái `DRAFT` (riêng cấu hình gợi ý được `015` kích hoạt). Khi còn `DRAFT`, app không có điểm xuất phát đề xuất, báo mọi vị trí "chưa nằm trong vùng hỗ trợ", và bộ sưu tập, nhiệm vụ đều trống.

Kích hoạt từng mục qua web quản trị (địa danh trước, rồi thẻ và bộ sưu tập), hoặc toàn bộ dữ liệu mẫu trong psql:

```sql
BEGIN;
UPDATE place      SET status = 'ACTIVE' WHERE status = 'DRAFT';
UPDATE card       SET status = 'ACTIVE' WHERE status = 'DRAFT';
UPDATE collection SET status = 'ACTIVE' WHERE status = 'DRAFT';
UPDATE quest      SET status = 'ACTIVE' WHERE status = 'DRAFT';
UPDATE item       SET status = 'ACTIVE' WHERE status = 'DRAFT';
COMMIT;
```

## Dữ liệu mẫu

- **Người dùng mẫu** (ID từ `900001`, email `example.invalid`): mật khẩu là hash bcrypt của chuỗi ngẫu nhiên đã bỏ, nên **không đăng nhập được**; hãy đăng ký tài khoản mới qua app.
- **Vật phẩm:** Giày tăng tốc (`SPEED_SHOES`, hệ số 2,00, hiệu lực 5.000 bước) và Vé máy bay (`FLIGHT_TICKET`).
- **Nhiệm vụ:** 11 nhiệm vụ ngày/tuần với điều kiện `STEP_COUNT`, `DISCOVER_PLACE`, `COMPLETE_JOURNEY`.
- **Thẻ và bộ sưu tập:** sau `017` có 311 thẻ cho 311 địa danh, thuộc 12 bộ sưu tập; 270 thẻ chưa có ảnh. Migration giữ nguyên 135 thẻ cũ, thêm 176 thẻ và gán vào 5 bộ mới: Hành trình Bắc Trung Bộ (28), Duyên hải Nam Trung Bộ (35), Khám phá Tây Nguyên (23), Dấu chân Đông Nam Bộ (28), Sắc màu miền Tây (62). Một số thẻ mẫu cũ thuộc nhiều bộ nên tổng số thẻ theo từng bộ có thể lớn hơn 311. Thẻ mới dùng ảnh địa danh nếu có; không tự tạo ảnh thay thế. Thẻ mới được kích hoạt khi địa danh và khu vực đều ACTIVE; các bộ mới ở trạng thái ACTIVE. Chạy lại không thêm bản trùng.
- **Ảnh:** 61 ảnh địa danh là URL trực tiếp từ Wikimedia Commons.
- **Hành trình mẫu cũ** giữ số bước đã lưu. Backend hiện dùng κ = 0,15 cho hành trình mới.
- Trường dữ liệu gốc không có cột tương ứng (URL nguồn, mã băm, metadata mô hình) không được nhập; `rating_count` bị thiếu đặt `0`, `rating` để `NULL`.

## Máy phát triển hiện tại

| Thông số | Giá trị |
| --- | --- |
| Host / cổng | `127.0.0.1:5433` |
| Database / user | `bcv` / `postgres` |
| Thư mục dữ liệu | `D:\PostgreSQL\BCV\data` |
| Khởi động | Scheduled Task `BCV PostgreSQL`, tự chạy khi đăng nhập Windows |

Cổng 5432 trên máy này là một cluster PostgreSQL **khác**, dự án không dùng; đặt `"DbPort": 5433` trong `backend/local.settings.json`. Máy khác dùng cổng và đường dẫn của riêng mình.

## Ghi công

Ảnh địa danh lấy từ Wikimedia Commons. Khi hiển thị hoặc phân phối, phải ghi tên tác giả và giấy phép theo từng tệp. Ảnh miền Bắc (`012`): xem [012_northern_place_images_sources.md](012_northern_place_images_sources.md). Ảnh trong `004`:

| Địa danh | Ảnh nguồn | Tác giả, giấy phép |
| --- | --- | --- |
| Tháp Bà Ponagar | [Po Nagar Cham Towers](https://commons.wikimedia.org/wiki/File:Po_Nagar_Cham_Towers.jpeg) | 4pf3154f7, CC BY-SA 3.0 |
| VinWonders Nha Trang | [Vinpearl Land Nha Trang Ferris wheel](https://commons.wikimedia.org/wiki/File:Vinpearl_Land_Nha_Trang_Ferris_wheel.jpg) | 유자차, CC BY-SA 4.0 |
| Nhà Thờ Gỗ Kon Tum | [Kontum wooden catholic church](https://commons.wikimedia.org/wiki/File:Kontum_wooden_catholic_church.jpg) | Rdavout, CC BY-SA 3.0 |
| Phố cổ Hội An | [Hoi An Ancient Town](https://commons.wikimedia.org/wiki/File:Hoi_An_Ancient_Town.jpg) | Andre Hospers, CC BY 4.0 |
| Vịnh Hạ Long | [Halong Bay in Vietnam](https://commons.wikimedia.org/wiki/File:Halong_Bay_in_Vietnam.jpg) | Thomas Hirsch / User:Ravn, CC BY-SA 3.0 |
| Thác Bản Giốc | [Ban Gioc](https://commons.wikimedia.org/wiki/File:Ban_Gioc.jpg) | Caobangpro, CC BY 3.0 |
| Hồ Gươm | [Ho Hoan Kiem](https://commons.wikimedia.org/wiki/File:Ho_Hoan_Kiem.jpg) | Trung geo, phạm vi công cộng |
| Đỉnh Núi Fansipan | [Fansipan summit new](https://commons.wikimedia.org/wiki/File:Fansipan_summit_new.jpg) | Thái Nhi, phạm vi công cộng |
| Nhà thờ Đức Bà Sài Gòn | [Saigon Notre-Dame Basilica](https://commons.wikimedia.org/wiki/File:Saigon_Notre-Dame_Basilica.jpg) | Ngô Trung, CC BY-SA 3.0 |
| Tràng An | [Trang An Landscape Complex](https://commons.wikimedia.org/wiki/File:Trang_An_Landscape_Complex,_Ninh_Binh_Province,_Vietnam,_20240202_1524_5364.jpg) | Jakub Hałun, CC BY 4.0 |
| Đảo Phú Quốc | [Phu Quoc Beach](https://commons.wikimedia.org/wiki/File:Phu_Quoc_Beach.jpg) | FrogsLegs71, CC BY-SA 3.0 |
| Hồ Xuân Hương | [Xuan Huong Lake in Da Lat](https://commons.wikimedia.org/wiki/File:Xuan_Huong_Lake_in_Da_Lat_(28219543381).jpg) | Le Anh Tuan, phạm vi công cộng |
| Bà Nà Hills | [The Golden Bridge](https://commons.wikimedia.org/wiki/File:The_Golden_Bridge,_Ba_Na_Hills,_Vietnam.jpg) | Ray in Manila, CC BY 2.0 |
| Động Phong Nha | [Phong Nha cave entrance](https://commons.wikimedia.org/wiki/File:Phong_Nha_cave_entrance.jpg) | Tycho, CC BY-SA 3.0 |
| Đại Nội Huế | [Imperial City, Hue](https://commons.wikimedia.org/wiki/File:Imperial_City,_Hue,_Vietnam_(49575145062).jpg) | flowcomm, CC BY 2.0 |

## Đóng góp

Xem [Đóng góp](../README.md#đóng-góp) ở README gốc. Migration mới: đánh số tiếp theo, chạy trong một giao dịch, kiểm tra trạng thái trước và sau, chạy lại được an toàn, và thêm vào danh sách nạp ở mục [Cài đặt](#cài-đặt).

## Giấy phép

UNLICENSED © 2026 Nhóm ĐAHK1262703 cho schema và script. Ảnh địa danh giữ giấy phép riêng theo từng tệp (xem [Ghi công](#ghi-công)).

## Thời gian đăng nhập cuối

`018_user_last_login.sql` thêm cột `app_user.last_login_at` kiểu TIMESTAMPTZ, cho phép NULL. Backend tự bảo đảm cột này tồn tại khi khởi động, nên bản triển khai hiện có được nâng cấp tự động. Không backfill thời gian cũ. Chỉ đăng nhập thành công bằng tài khoản/mật khẩu cập nhật cột này; khôi phục phiên và đăng nhập bị từ chối không cập nhật. Web hiển thị theo giờ Việt Nam (GMT+7).
