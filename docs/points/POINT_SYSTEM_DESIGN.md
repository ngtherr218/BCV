# Thiết kế hệ thống điểm xếp hạng v3

## Nguồn điểm

| Nguồn | Sự kiện ghi sổ mới | Cấu hình | Điều kiện |
|---|---|---|---|
| Khám phá địa danh mới | DISCOVERY, source_key = place_id | discovery_points, mặc định 150 | Hoàn thành hành trình đi bộ hợp lệ và INSERT khám phá đầu tiên thành công |
| Hoàn thành nhiệm vụ | QUEST, source_key = quest_progress_id | quest.reward_points | Chuyển sang hoàn thành trong chu kỳ hiện tại, từ sự kiện đủ điều kiện |

Không còn phát sinh hai giao dịch JOURNEY + FIRST_PLACE trong source mới. Các loại đó chỉ được giữ để đọc lịch sử. Tổng điểm và xếp hạng được SUM từ explorer_score_event; không có bộ đếm tổng điểm thứ hai để bị lệch. Điểm nhiệm vụ không gán vùng; bộ lọc vùng chỉ lấy điểm khám phá ở vùng đó và các sự kiện lịch sử vốn có vùng.

## Tính hợp lệ và chống lặp

- Địa danh đã khám phá bằng đi bộ hoặc vé bay không thể chọn lại làm hành trình. Validate trước khi thay thế hành trình đang đi.
- Chỉ tạo hành trình, xem nội dung, tiến độ chưa đủ hoặc hành trình ABANDONED không có điểm khám phá.
- Vé bay giữ hành vi mở khóa/thẻ/tiến độ hiện có nhưng không có điểm khám phá; nhiệm vụ hoàn thành do sự kiện vé bay chốt điểm thực nhận bằng 0, không được trả lại điểm đó ở request đi bộ sau.
- Khóa journey khi đồng bộ, INSERT khám phá có UNIQUE(user_id,place_id), sổ điểm có UNIQUE(user_id,event_type,source_key). Cả thao tác hoàn thành, vị trí ảo, khám phá, thẻ, nhiệm vụ và ghi sổ thuộc một transaction. Lỗi ghi sổ rollback toàn bộ.
- Cấu hình khám phá được giữ FOR SHARE đến hết transaction; revision chặn admin ghi đè thay đổi mới hơn. Điểm khám phá từ 0 đến 200000 (giữ được tổng tối đa của hai ô cũ); điểm nhiệm vụ từ 0 đến 100000. Số nguyên bắt buộc, 0 tắt điểm cho nguồn đó.
- Không backfill khám phá/hành trình cũ, không viết lại sổ điểm hoặc tính lại hạng từ mức cấu hình hiện tại.

## API

GET/PUT /api/admin/exploration/reward-settings chỉ có discoveryPoints và revision (kèm ruleVersion khi đọc). PUT thiếu trường mới, số âm, thập phân, quá lớn hoặc revision cũ bị từ chối. Payload ba ô cũ không được lặng lẽ coi là mức 150.

API admin nhiệm vụ dùng reward_points theo convention snake_case của catalog. API Android giữ rankingPoints (mức hiện tại) và awardedRankingPoints (thực nhận). Journey trả rankingPoints là một khoản khám phá thực nhận; Android không tự cộng hay hardcode 150.

Lịch sử quản trị vẫn cùng endpoint /api/admin/exploration/events, thêm DISCOVERY và sourceKey/ruleVersion/questName để truy vết. Quyền ADMIN ACTIVE và bảo vệ dữ liệu riêng tư ở leaderboard giữ nguyên.
