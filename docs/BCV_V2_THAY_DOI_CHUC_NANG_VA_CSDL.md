# Thay đổi chức năng và cơ sở dữ liệu BCV v2

Tài liệu tổng hợp các bổ sung nghiệp vụ trong app Android, API và web quản trị, cùng thay đổi dữ liệu cần triển khai. Phạm vi bao gồm cơ chế khám phá, điểm thưởng, nhiệm vụ, bộ sưu tập, thông báo và kiểm duyệt ảnh. Những thay đổi thuần về bố cục, màu sắc và giao diện không được trình bày.

BCV v2 giữ mô hình dữ liệu gốc 25 bảng và bổ sung 8 bảng nghiệp vụ, tổng cộng 33 bảng; không tính spatial_ref_sys của PostGIS. Bộ lệnh trong sql/ là nguồn triển khai hiện tại. Các tài liệu snapshot cũ trong docs/technical/ và migration trong db/ được giữ để tra cứu lịch sử.

## Khám phá và lựa chọn điểm đến

Bộ lọc khởi tạo theo sở thích đã lưu, cho phép người dùng chọn lại loại địa danh và điều kiện tìm kiếm. Có 12 nhóm sở thích chuẩn tương ứng loại địa danh 101–112. Các nhóm sở thích cũ 201–206 và dữ liệu lựa chọn cũ được giữ để tương thích, không thay đổi mô hình K-Means 35 đặc trưng.

Danh sách chọn điểm đến loại các địa danh đã khám phá và điểm trùng vị trí hiện tại trước khi phân trang. Ảnh cover được phép xem trước, kể cả địa danh chưa mở khóa; trạng thái khóa không cấp quyền sở hữu thẻ hay truy cập toàn bộ nội dung đã khóa.

Backend từ chối bắt đầu hành trình tới địa danh đã khám phá. Khi hoàn thành, backend kiểm tra lại việc mở khóa trong cùng transaction; nếu hành trình cũ không còn hợp lệ thì rollback, không phát thêm thẻ hoặc điểm.

## Hành trình và đồng bộ bước

Bước đồng bộ là tổng tích lũy theo nguồn của hành trình; chỉ phần tăng mới được ghi vào tiến độ và nhiệm vụ. App cập nhật định kỳ khi màn hình hoạt động, giữ trạng thái khi đổi tab và đọc lại dữ liệu từ server. Đồng bộ lại cùng dữ liệu không được cộng thưởng lần hai.

Kết quả hành trình là biên nhận chỉ đọc theo chủ sở hữu, gồm bước đã ghi nhận, bước yêu cầu, điểm khám phá thực nhận và thẻ đã cấp. Có thể mở lại từ lịch sử; thao tác xem lại không thực thi cơ chế phát thưởng.

# Điểm thưởng nhiệm vụ và xếp hạng

## Quy tắc phát điểm mới

Đi bộ hoàn thành khám phá một địa danh mới phát một sự kiện DISCOVERY, mặc định 150 điểm. Cấu hình này thay cho cách cộng riêng 50 điểm hành trình và 100 điểm lần đầu. Địa danh đã mở khóa không được dùng để tạo lượt thưởng mới. Vé bay không cộng điểm xếp hạng, kể cả điểm nhiệm vụ khám phá phát sinh từ vé bay.

Sổ explorer_score_event giữ nguyên sự kiện FIRST_PLACE, JOURNEY và QUEST cũ. Nâng cấp không cộng bù, không phát lại và không tính lại điểm đã lưu. Khóa duy nhất theo user_id, event_type, source_key ngăn ghi trùng một nguồn thưởng.

## Điểm nhiệm vụ độc lập

Mỗi nhiệm vụ có reward_points riêng, từ 0 đến 100000; phần thưởng vật phẩm tiếp tục độc lập. Một hoạt động có thể hoàn thành nhiều nhiệm vụ hợp lệ và mỗi tiến độ hoàn thành chỉ được chốt một lần trong chu kỳ ngày, tuần ISO hoặc một lần.

quest_progress lưu awarded_points và points_settled để giữ mức điểm tại thời điểm hoàn thành, kể cả mức 0. Sửa cấu hình sau đó chỉ ảnh hưởng lần hoàn thành mới. Quản trị viên được sửa tên, mô tả, trạng thái và điểm; điều kiện, mục tiêu, chu kỳ và phần thưởng của nhiệm vụ đã có tiến độ được bảo vệ để tránh làm sai lịch sử.

## Cấu hình quản trị và ví dụ

Quản trị viên thay đổi điểm khám phá trong explorer_reward_config và điểm từng nhiệm vụ trong quest. API cấu hình yêu cầu revision để phát hiện cập nhật đồng thời. Các cột quest_points, journey_points, first_place_points cũ được giữ cho tương thích dữ liệu; không tiếp tục dùng làm ba mức thưởng mới.

Ví dụ: người dùng có 50 điểm, hoàn thành khám phá mới nhận 150 điểm, đồng thời đạt nhiệm vụ bước 20 điểm và nhiệm vụ hành trình 30 điểm thì tổng là 250 điểm. Các mức này là dữ liệu minh họa; điểm thực tế lấy theo cấu hình được chốt khi hoàn thành.

## Thứ hạng và thông báo hoàn thành

Bảng xếp hạng dùng tổng điểm theo tuần ở múi giờ Việt Nam và hỗ trợ lọc vùng. Đồng điểm dùng dense_rank, nghĩa là cùng điểm có cùng hạng. Chỉ tài khoản ACTIVE tham gia; vị trí của người đang đăng nhập được trả riêng, kể cả nằm ngoài danh sách dẫn đầu.

Tên hiển thị lấy từ full_name của hồ sơ; không ghép mã người dùng. Thông báo hoàn thành so sánh thứ hạng toàn quốc tuần hiện tại trước hoạt động và sau khi cộng xong điểm bước, khám phá và nhiệm vụ hoàn thành hành trình. Nội dung phân biệt tăng hạng, giảm hạng, giữ hạng, mới vào bảng và chưa có hạng. Đây là ảnh chụp tại lúc hoàn thành, không tự cập nhật khi người khác phát sinh điểm về sau.

# Các bảng nghiệp vụ được bổ sung

Tám bảng sau được bổ sung so với schema logic gốc 25 bảng. Các khóa ngoại liên kết với tài khoản, địa danh, cấu hình và sở thích hiện có; không tạo bản sao danh tính hoặc bản sao quyền sở hữu thẻ.

| Bảng mới | Trường và ràng buộc chính | Vai trò |
| --- | --- | --- |
| interest_group | interest_group_id; code UNIQUE; name | Danh mục nhóm sở thích chuẩn và nhóm cũ được bảo toàn. |
| user_preference_interest | PK gồm user_preference_id và interest_group_id; khóa ngoại tới hai bảng tương ứng | Lưu các nhóm sở thích được người dùng lựa chọn. |
| recommendation_rating_snapshot | PK gồm recommendation_config_id và place_id; rating; rating_count | Đóng băng đánh giá theo phiên bản mô hình, tránh thay đổi gợi ý khi sửa dữ liệu đánh giá trực tiếp. |
| place_type_feature | PK gồm place_type_id và code | Khai báo đặc trưng mặc định cho địa danh kế thừa từ loại. |
| community_media | media_id; user_id; place_id; mime_type; media_data; status; reviewed_by; các mốc duyệt và dọn file | Lưu ảnh đóng góp và vòng đời kiểm duyệt. Giữ metadata khi file nhị phân được dọn. |
| media_report | report_id; media_id; reporter_id; reason; status; resolved_by; UNIQUE media_id và reporter_id | Theo dõi báo cáo ảnh và kết quả xử lý, ngăn báo cáo trùng từ cùng người. |
| explorer_score_event | score_event_id; user_id; event_type; source_key; points; rule_version; occurred_at; place_id; region_id | Sổ phát điểm có nguồn gốc và thời gian. UNIQUE user_id, event_type, source_key; points phải dương. |
| explorer_reward_config | singleton làm khóa chính; discovery_points; revision; updated_at; updated_by; giữ các mức cũ | Một cấu hình phát điểm khám phá dùng chung, có phiên bản chống ghi đè đồng thời. |

explorer_score_event có index theo thời gian và người dùng, cùng index vùng và thời gian phục vụ bảng xếp hạng. community_media có index tra cứu ảnh theo địa danh/trạng thái và hàng chờ duyệt; media_report có index theo media/trạng thái.

Không thêm bảng riêng cho hộ chiếu, huy hiệu, vị trí xếp hạng hoặc kết quả hoàn thành hành trình. Các chức năng này đọc/tổng hợp từ dữ liệu nghiệp vụ đã có để tránh lưu trùng và lệch trạng thái.

# Các bảng hiện có được sửa đổi

| Bảng | Thay đổi | Ý nghĩa nghiệp vụ |
| --- | --- | --- |
| app_user | last_login_at timestamptz; media_upload_blocked boolean NOT NULL DEFAULT false | Theo dõi đăng nhập và quyền đóng góp ảnh. |
| user_card | seen_at timestamptz, cho phép NULL | NULL biểu thị thẻ mới chưa xem. Đánh dấu xem không thay quyền sở hữu. |
| notification | hidden_at timestamptz, cho phép NULL | Ẩn thông báo khỏi danh sách thay vì xóa bản ghi sự kiện. |
| place | feature_source varchar(10), mặc định CUSTOM, nhận TYPE hoặc CUSTOM | Chọn dùng đặc trưng theo loại hay đặc trưng riêng của địa danh. |
| recommendation_config | settings jsonb NOT NULL; index UNIQUE cho bản ghi ACTIVE | Lưu metadata, ánh xạ sở thích, ngưỡng vận động; chỉ một mô hình hoạt động. |
| quest | reward_points integer, mặc định 20, giới hạn 0–100000; description text | Quản trị điểm riêng và mô tả cho từng nhiệm vụ. |
| quest_progress | awarded_points integer NOT NULL DEFAULT 0; points_settled boolean NOT NULL DEFAULT false | Chốt điểm đã thưởng của mỗi tiến độ, không tính lại khi sửa cấu hình. |

## Các thay đổi bổ sung trong bảng mới

explorer_reward_config bổ sung discovery_points, mặc định 150, giới hạn 0–200000. explorer_score_event mở rộng tập loại sự kiện với DISCOVERY và giữ các loại lịch sử. Thay đổi cấu hình không sửa các sự kiện điểm đã ghi nhận.

community_media bổ sung first_approved_at, file_deleted_at và delete_after; media_data cho phép NULL sau dọn file. Trạng thái gồm PENDING, APPROVED, REJECTED, FLAGGED, REMOVED, EXPIRED. Bộ SQL triển khai giới hạn MIME ở image/jpeg, image/png, image/webp. Báo cáo ảnh nhận OPEN, RESOLVED, ACCEPTED và DISMISSED.

## Danh mục và dữ liệu được giữ

Danh mục thẻ, bộ sưu tập, địa danh, ảnh và cấu hình gợi ý được cập nhật bằng dữ liệu tham chiếu. Seed cài mới chỉ gồm danh mục và nhiệm vụ; loại bản ghi QA, không chứa tài khoản hay lịch sử người dùng. Nâng cấp không nạp lại seed, không tự kích hoạt toàn bộ nội dung và không đổi điểm cũ.

Tính năng đóng góp video đã được gỡ khỏi phiên bản hiện tại. Cài mới không tạo place_video. Nâng cấp giữ bảng video cũ nếu có và dừng nếu community_media còn định dạng không hỗ trợ, để dữ liệu được xem xét trước khi chuyển đổi; không có lệnh tự xóa video trong bộ nâng cấp.

# Bộ sưu tập ảnh và chức năng quản trị

## Xem trước thẻ và giữ lịch sử sở hữu

Danh mục cho phép xem trước ảnh thẻ chưa sở hữu, nhưng cờ Owned vẫn dựa trên dữ liệu thẻ hoặc khám phá đã ghi nhận. Thẻ mới được đếm và đánh dấu đã xem qua seen_at; xem trước không cấp thẻ.

Nếu địa danh hoặc vùng ngừng hoạt động, thẻ ACTIVE đã sở hữu vẫn được giữ trong bộ sưu tập; thuộc tính PlaceAvailable=false ngăn điều hướng tới địa danh không khả dụng. Thẻ chưa sở hữu gắn với địa danh không khả dụng được loại khỏi danh mục. Thẻ INACTIVE vẫn theo chính sách ẩn hiện hành, không xóa quyền sở hữu.

## Hộ chiếu khám phá

Bộ sưu tập theo dõi những thẻ người dùng đã nhận. Hộ chiếu tổng hợp các địa danh, vùng và dấu mốc khám phá từ lịch sử có thật, giúp theo dõi phạm vi trải nghiệm. Hộ chiếu không cấp thêm một bộ thẻ hoặc phát điểm lần hai.

## Ảnh địa danh và tốc độ tải

Bổ sung nguồn ảnh địa danh và ghi công trong manifest. Ảnh công khai trên Android dùng bộ nhớ đệm đĩa 48 MiB, thời hạn 24 giờ, gộp yêu cầu trùng URL và giới hạn số lượt tải song song. Tài nguyên cần xác thực không sử dụng bộ nhớ đệm công khai này. Ảnh cover chưa mở khóa có thể xem mờ kèm trạng thái khóa; quyền mở khóa nội dung vẫn do dữ liệu server quyết định.

## Đóng góp ảnh và quản trị nội dung

Người dùng gửi ảnh địa danh để xét duyệt, có thể báo cáo nội dung không phù hợp. Quản trị viên duyệt, từ chối, gỡ, xử lý báo cáo và chặn quyền tải ảnh của tài khoản. Chỉ lần duyệt hợp lệ đầu tiên được dùng làm nguồn tiến độ nhiệm vụ ảnh; khôi phục/duyệt lại không phát thưởng lặp.

Quản trị nhiệm vụ hỗ trợ tạo, sửa, đổi trạng thái và cấu hình điểm riêng. Các kiểm tra liên kết dữ liệu bảo vệ việc xóa danh mục có bản ghi phụ thuộc; dùng ngừng hoạt động khi cần giữ lịch sử.

## Các hợp đồng và nhóm API đáng chú ý

Biên nhận hoàn thành dùng GET /api/journeys/{id}/completion và kiểm tra quyền sở hữu. Hợp đồng CollectionCard bổ sung PlaceAvailable, giữ Owned và IsNew riêng biệt. Nhóm API xếp hạng trả danh sách dẫn đầu cùng bản ghi Me; nhóm cấu hình điểm yêu cầu DiscoveryPoints và Revision. Nhóm thông báo đọc bản ghi đã phát trong transaction và lọc các bản ghi đã ẩn.

Mã xử lý nghiệp vụ nằm trong các module Journey, Discovery, PlaceCollection, QuestItem, StatsNotify và Admin của backend/Bcv.Api. Danh sách endpoint chi tiết và các ghi chú kỹ thuật được lưu tại docs/technical/api_changes.md và api_inventory.md.

# Triển khai và kiểm tra bàn giao

## Bộ SQL triển khai

Thư mục sql/ phân biệt rõ cài mới và nâng cấp. install.sql từ chối DB đã có bảng ứng dụng, tạo schema cuối và nạp danh mục không chứa dữ liệu cá nhân. upgrade.sql yêu cầu schema BCV, 12 loại chuẩn và một mô hình đã được duyệt ACTIVE; chạy migration trong transaction, không nạp lại danh mục.

Thứ tự nâng cấp là 018, 019, 020, 021, 022, 023, 024, 026, 027, 028, 029 và 030. Migration 026 trong sql/ tạo sổ điểm, không phải migration video 026 lịch sử trong db/. verify.sql kiểm tra 33 bảng nghiệp vụ, các cột mới, PostGIS, sở thích, mô hình và loại sự kiện DISCOVERY trước commit.

Database có mô hình tùy biến thiếu metadata hoặc media định dạng không hỗ trợ sẽ bị dừng để xem xét, thay vì đoán cấu hình hay xóa dữ liệu. Chạy backup trước nâng cấp. Không có script downgrade phá dữ liệu; khôi phục backup vào DB đích riêng nếu cần quay lại phiên bản cũ.

## Tài liệu và vận hành

README.md được viết lại theo nhánh bcv_v2 với hướng dẫn DB, backend, web, Android, tạo quản trị và cấu hình. sql/README.md có lệnh thực thi và điều kiện từng đường triển khai. db/README.md được ghi rõ là lịch sử. DEPLOY.md trỏ tới bộ SQL mới và phân biệt push Git với triển khai service/database.

Profile local dùng API 5082, web 5173 và DB vietnam_explorer_dev_v2. Khi triển khai, đặt ConnectionStrings__Bcv và Database__ApplyStartupMigrations=false. Credential, session, log, backup và APK không được đưa vào repository. Máy chủ đang theo dõi nhánh khác không tự chuyển sang bcv_v2 sau push.

## Phạm vi xác minh

Cài mới đã được thực thi trên database PostgreSQL thử riêng, tạo danh mục và không có tài khoản người dùng. Đường nâng cấp đã được kiểm tra từ schema gốc cùng dữ liệu mô hình chuẩn và 12 loại địa danh, sau đó chạy lại để kiểm tra tính lặp. Các kết quả build và kiểm tra ứng dụng cuối cùng được ghi trong docs/testing/DEPLOYMENT_2026_11_16.md.

Kiểm tra cho thay đổi xếp hạng bao gồm tên hồ sơ không ghép ID, điểm khám phá cùng điểm nhiệm vụ bước và hoàn thành, chuyển hạng 3 lên 1 và đồng bộ lặp không thưởng lại. Nội dung thông báo mới áp dụng cho lần hoàn thành sau khi backend được cập nhật; thông báo lịch sử không được tính lại.

Không triển khai migration lên DB cloud trong lần bàn giao mã nguồn. Kiểm tra cảm biến, Health Connect và ảnh qua mạng thực tế trên thiết bị vật lý vẫn là bước nghiệm thu riêng. Chỉ push Git không làm phiên API đang chạy trên máy tự nạp lại mã mới; cần khởi động lại backend theo hướng dẫn sau bàn giao.
