# Triển khai PostgreSQL cho BCV v2

Bộ SQL ngày 16/11/2026 dành cho nhánh `bcv_v2`, PostgreSQL 18 và PostGIS. Có **33 bảng nghiệp vụ**, không tính `spatial_ref_sys` của PostGIS. Các file trong thư mục này là đường triển khai hiện tại; `db/` lưu lịch sử các bước phát triển.

## Chọn đúng đường triển khai

| Trường hợp | Lệnh đầu vào |
| --- | --- |
| Database mới, chưa có bảng ứng dụng | `install.sql` |
| Database BCV đã có schema gốc, 12 loại chuẩn và mô hình ACTIVE | `upgrade.sql` |
| Chỉ kiểm tra sau khi triển khai | `verify.sql` |

Dùng **psql 18**, chạy từ thư mục gốc repository. Các lệnh `\ir`, `COPY FROM stdin`, `\restrict` là cú pháp psql; không dán toàn bộ file vào Query Tool của pgAdmin. Có thể mở PSQL Tool trong pgAdmin. Mật khẩu được psql hỏi tương tác hoặc lấy từ `.pgpass`; không ghi vào SQL/Git.

## Database mới

```powershell
# Thay host/user/tên DB theo môi trường; thêm -p nếu không dùng 5432.
createdb -h localhost -U postgres -T template0 -E UTF8 bcv_v2
psql -X -h localhost -U postgres -d bcv_v2 -v ON_ERROR_STOP=1 -f sql/install.sql
```

`install.sql` kiểm tra database trống, lấy khóa triển khai rồi thực thi trong một transaction:

1. `schema/001_schema.sql`: schema cuối cùng, khóa ngoại, index, sequence và PostGIS.
2. `seed/002_reference.sql`: vùng, loại địa danh, địa danh, URL ảnh công khai, thẻ, bộ sưu tập, vật phẩm, mô hình gợi ý và sở thích.
3. `seed/003_rewards_quests.sql`: nhiệm vụ danh mục (loại bản ghi QA), điểm khám phá mặc định 150.
4. `migrations/030_photo_lifecycle.sql`: trạng thái duyệt ảnh, lưu metadata khi dọn file và ràng buộc chỉ nhận ảnh.
5. `verify.sql`: kiểm tra các bảng/cột, PostGIS, sở thích, mô hình và quy tắc điểm.

Không có tài khoản, password hash, phiên đăng nhập, lịch sử hành trình, điểm cá nhân, ảnh cộng đồng hay tiến độ nhiệm vụ trong dữ liệu seed. Không có tài khoản quản trị mặc định. Tạo quản trị bằng CLI sau khi cấu hình backend, xem [README](../README.md).

**Không chạy seed hoặc schema riêng trên DB đang sử dụng.** Seed chứa ID cố định và đặt sequence cho cài mới. `install.sql` cố ý từ chối chạy lần hai.

## Nâng cấp database đang có

Dừng ghi từ API trong thời gian nâng cấp. Backup trước, giữ backup ngoài Git:

```powershell
pg_dump -h localhost -U postgres -d bcv -Fc --no-owner --no-privileges -f /duong-dan-backup/BCV_truoc_nang_cap.dump
psql -X -h localhost -U postgres -d bcv -v ON_ERROR_STOP=1 -f sql/upgrade.sql
```

Đổi đường dẫn backup theo máy. Nếu chuyển server, restore backup vào database đích trước rồi chạy `upgrade.sql` trên đích. Không chạy `install.sql` sau restore.

Thứ tự migration: **018 → 019 → 020 → 021 → 022 → 023 → 024 → 026 → 027 → 028 → 029 → 030**. Các file trong `migrations/` là thành phần của `upgrade.sql`; không chạy bộ migration video 025/026 cũ trong `db/`.

- 018–020 thêm dấu thời gian đăng nhập, xem thẻ, ẩn thông báo.
- 021 thêm nhóm sở thích, metadata mô hình, snapshot đánh giá; giữ các lựa chọn cũ.
- 022–024 thêm ảnh cộng đồng, đặc trưng mặc định theo loại địa danh và báo cáo kiểm duyệt.
- 026 tạo sổ điểm; 027 cấu hình điểm; 028 hợp nhất điểm khám phá, thêm điểm riêng cho nhiệm vụ và snapshot điểm đã chốt.
- 029 thêm 12 sở thích theo loại địa danh 101–112, giữ nhóm cũ 201–206.
- 030 bổ sung vòng đời file ảnh và trạng thái xử lý báo cáo.

Nâng cấp không nạp lại danh mục, không đổi trạng thái hàng loạt thành ACTIVE, không tạo tài khoản, không cộng bù điểm và không xóa lịch sử. Điểm cũ FIRST_PLACE/JOURNEY/QUEST vẫn được giữ; điểm khám phá mới dùng DISCOVERY. Cấu hình đã có được giữ nguyên. Nhiệm vụ hoàn thành cũ được chốt snapshot bằng điểm đã có trong sổ điểm, không phát sinh thưởng mới.

**Các trường hợp cần xử lý trước:**

- Schema gốc thiếu bảng/cột hoặc thiếu 12 loại địa danh chuẩn: đối chiếu schema/backup trước. Database còn dùng 10 loại cũ cần đánh giá migration lịch sử `db/014_standard_place_types.sql` trước; migration đó có chuyển/xóa lựa chọn loại cũ nên không được tự động chạy ở đây. Database chưa có mô hình ACTIVE cần duyệt và kích hoạt mô hình trước (bản dữ liệu chuẩn có migration lịch sử 015). `upgrade.sql` không biến đổi một DB bất kỳ thành BCV.
- Mô hình tùy biến chưa có `settings`: migration 021 dừng để kiểm tra metadata; không tự gán dữ liệu mô hình khác.
- Có media dạng video/định dạng không được hỗ trợ: migration 030 dừng và rollback; cần phương án lưu trữ/chuyển dữ liệu được duyệt trước. Không tự xóa video. Bảng `place_video` cũ nếu có được giữ, ứng dụng v2 không sử dụng.

Có thể chạy lại `upgrade.sql` trên DB đã nâng cấp chuẩn. Toàn bộ migration nằm trong một transaction: lỗi ở bất kỳ bước nào sẽ rollback; `verify.sql` cũng chạy trước commit, nên lỗi kiểm tra sẽ rollback toàn bộ. Có thể gọi lại file này độc lập sau triển khai. Khôi phục bằng backup trên DB đích riêng nếu cần rollback phiên bản; không có script downgrade xóa dữ liệu tự động.

## Cấu hình ứng dụng sau SQL

Đặt `ConnectionStrings__Bcv` bằng chuỗi Npgsql của DB đích và `Database__ApplyStartupMigrations=false`. Backend không cần quyền tự thay đổi schema khi vận hành. Với cloud, cấu hình SSL phù hợp chứng chỉ của nhà cung cấp. Không thay DB đang chạy bằng DB thử nghiệm.

```powershell
psql -X -h localhost -U postgres -d bcv_v2 -v ON_ERROR_STOP=1 -f sql/verify.sql
```

Kiểm tra thêm API `/health`, đăng nhập, gợi ý, hành trình, nhiệm vụ, thông báo và bảng xếp hạng. URL ảnh seed cần Internet; không kèm file ảnh nhị phân trong SQL.
