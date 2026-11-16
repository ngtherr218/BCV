# API đã thay đổi và bổ sung — local v2

**Rà soát 11/11/2026:** không thêm route mới cho ghép ảnh milestone. Android dùng cover từ preview đã có và progress từ journey server. `QuestCycles.At` chỉ giúp kiểm thử boundary, không đổi request/response hay công thức bước. Tìm kiếm/preview/passport/ranking đã được kiểm thử qua API thật; admin regression kiểm tra bảy catalog và CRUD draft region/collection/quest. Mười ảnh card văn hóa được cấu hình qua POST /api/admin/images?purpose=card và PUT /api/admin/data/cards/{id} hiện có. Bằng chứng tại implementation_report.md.

## GET /health — nhận diện local Development

Ở Development, response bổ sung database lấy trực tiếp từ current_database(): status=ok, database=vietnam_explorer_dev_v2. Script kiểm thử xác minh giá trị này trước login/register hoặc ghi fixture. Không trả connection string/password/user data. Ngoài Development giữ response status=ok như cũ. Không có DDL; kiểm tra kết nối vẫn chỉ đọc.

Giữ các route cũ. Các route mới/cá nhân đều yêu cầu Bearer; không nhận user ID hoặc điểm do client tự cấp. Danh sách route nguồn ở api_inventory.md; bằng chứng HTTP ở local_api_test_results.md.

## GET /api/explore/places

Query: q (<=120 ký tự), minDistanceKm/maxDistanceKm (>=0, min<=max), maxEstimatedDays (>0), placeTypeId, interestGroupId, page (mặc định 1, tối đa 100000), pageSize (mặc định 20, 1..50), sort (distance/name/estimatedDays/interests). Các điều kiện kết hợp AND; sở thích chỉ lọc khi người dùng chọn interestGroupId. Không tự giới hạn địa danh theo hồ sơ.

Response: page/pageSize/total/places. Mỗi kết quả có ID, tên, loại, vùng, lat/lng, ảnh cover, isDiscovered, distanceKm, requiredSteps, dailyStepTarget, estimatedDays, relativeDifficulty, matchesInterests. Trạng thái place và region phải ACTIVE. EXISTS tránh trùng do features; order ổn định bằng ID; total giữ nguyên cả khi trang không có kết quả. SQL tham số hóa, sort theo whitelist, transaction read-only/repeatable-read. Tìm tên dùng lower/substring; chưa hỗ trợ tìm không dấu ở API nâng cao (ô tìm nhanh bản đồ cũ vẫn có normalize dấu).

## GET /api/explore/filters

Trả placeTypes/interests/regions từ DB để Android tạo bộ lọc; không có danh mục giả trong UI.

## GET /api/explore/places/{placeId}

Mở highlights cho preview; cover không bị làm mờ trên Android. Giữ introduction/name/type/region/distance/steps; thêm latitude/longitude/dailyStepTarget/estimatedDays/relativeDifficulty. Gallery/video/community vẫn theo quyền khám phá cũ. GET preview không ghi discovery/card/quest/điểm.

## GET /api/explore/recommendations

Response tương thích cũ; selection dùng hai ứng viên sở thích và một ngoài sở thích trong một daily target khi có đủ ứng viên. Giữ score/model/weights và tính hợp lệ; không nới giới hạn khoảng cách hoặc đưa lại place đã khám phá. Thiếu ứng viên phù hợp thì fallback ranking cũ, không tạo kết quả giả. Thêm lý do khám phá ngoài sở thích trong reasons, Android hiện có đọc được.

## GET /api/passport

Response regions gồm regionId/name/discovered/total/progressPercent/hasBadge/stamps. Dấu mộc dùng user_discovered_place; mẫu số là place ACTIVE trong region ACTIVE. Vùng rỗng không có huy hiệu. Badge là trạng thái suy ra, có thể thay đổi nếu admin thay danh mục hoạt động; không là vật phẩm sở hữu vĩnh viễn và không tạo bảng ownership mới.

## GET /api/leaderboard

Query week (YYYY-Www, mặc định tuần hiện tại), regionId tùy chọn, limit 1..100 (mặc định 20). Response week/startsAt/endsAt/regionId/top/me; thời gian có offset +07:00; khoảng tuần [thứ Hai 00:00, thứ Hai kế tiếp). DENSE_RANK theo điểm, tie hiển thị theo user ID. Chỉ hiển thị user ACTIVE. me.rank=null khi chưa có điểm. Tên hiển thị là alias Nhà khám phá {id}; không email, tên thật hoặc tọa độ cá nhân.

Điểm FIRST_PLACE=100 và JOURNEY=50 chỉ từ đi bộ hợp lệ; mỗi user/đích nhận một lần. QUEST=20 mỗi quest_progress hoàn thành mới. Quest không có địa danh không gán vùng. Ghi cùng transaction, UNIQUE(user_id,event_type,source_key), không backfill. Vé không ghi điểm; API cũ của vé/thẻ giữ nguyên.

## Quy tắc và tương thích

Không đổi StepConversion, sync-steps hoặc dữ liệu cột cũ. Quest API giữ route/chi thưởng cũ; ledger chỉ bổ sung khi có bảng đã duyệt. Database legacy không có ledger tiếp tục luồng thưởng cũ. API v2 dùng launcher tắt auto-migration và session file riêng. Những chức năng API mới cần schema tương thích/model active/profile/vị trí ảo; lỗi trả NO_ACTIVITY_CONFIG/NO_VIRTUAL_LOCATION/INVALID_FILTER/INVALID_WEEK rõ ràng.
## API quản trị bổ sung

GET /api/admin/exploration/overview: dữ liệu catalog, thẻ đặt tên riêng, hộ chiếu và thử thách tuần; trả database đang kết nối. GET /api/admin/exploration/leaderboard: tái sử dụng query tuần/vùng/limit hiện có. GET /api/admin/exploration/events: week/regionId/eventType/page/pageSize; trả total/items. Cả ba kế thừa ADMIN filter, trả no-store và chỉ đọc. Không có endpoint nhập database công khai hoặc sửa điểm.

> Cập nhật sau yêu cầu gỡ video 11/11/2026: đã xoá place_video và dữ liệu video cộng đồng trên v2, giữ ảnh/kiểm duyệt ảnh. V2 hiện 33 bảng; không còn upload/player/duyệt video trên API, Android hoặc web. Các phần mô tả video trước cập nhật này là lịch sử. Xem [báo cáo gỡ video](video_removal_report.md).

## Sở thích12loại (11/11/2026)

GET /api/interests và /api/explore/filters trả12loại chuẩn101–112. GET interests thêm requiresReselection cho lựa chọn6nhóm cũ; không tự chuyển sở thích. PUT giữ contract placeTypeIds và giới hạn3. Gợi ý/bộlọc hỗ trợ mapping12loại từ014, vẫn hỗ trợ6nhóm cũ. Không đổi schema. Xem twelve_interests_fix.md.


## Lọc nhiều loại trên trang Khám phá tiếp

GET /api/explore/places thêm interestGroupIds=108,111 (CSV, tối đa12ID101–112). Các loại kết hợp OR, các điều kiện khác AND; sort=interests ưu tiên matchesInterests rồi khoảng cách. Giữ query đơn cũ. Không đổi schema. Xem inline_discovery_search.md.
