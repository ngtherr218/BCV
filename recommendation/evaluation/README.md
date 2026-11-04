# Chấm chất lượng gợi ý Top 3

**Trạng thái: chưa thu thập đánh giá người dùng.** Không có kết quả Precision@3/NDCG@3 thực tế. `profiles.example.json` chỉ là tình huống minh họa để chạy thử công cụ, không phải người tham gia khảo sát.

Đã có sẵn `judgments.example.csv` để xem bố cục phiếu, tạo từ tình huống minh họa Hà Nội và dữ liệu local. Mọi ô điểm đều trống. Khi khảo sát thật, tạo lại phiếu theo hồ sơ người tham gia và bộ dữ liệu xuất tại thời điểm khảo sát.

## Chuẩn bị tình huống

Mỗi người tham gia cung cấp mã ẩn danh, điểm xuất phát, sở thích, mức vận động và danh sách địa điểm đã khám phá. Sao chép cấu trúc trong `profiles.example.json` thành `profiles.json`; mỗi tình huống có `caseId` duy nhất. Không ghi tên, email hay vị trí nhà riêng; có thể chọn một điểm xuất phát công cộng.

| ID sở thích | Nhóm |
| --- | --- |
| 201 | Di sản |
| 202 | Thiên nhiên |
| 203 | Ẩm thực |
| 204 | Biển đảo |
| 205 | Lịch sử |
| 206 | Kiến trúc |

Chọn 1–6 nhóm. Mức vận động là `LOW`, `MEDIUM` hoặc `HIGH`. Ngưỡng vận động cụ thể lấy từ phiên bản cấu hình đã xuất, không sửa riêng khi so sánh thuật toán.

## Tạo phiếu

Backend phải đã chạy migration `021_versioned_recommendations.sql` và phiên bản API tương ứng. Từ thư mục `recommendation`, xuất dữ liệu bằng tài khoản quản trị; mật khẩu được hỏi kín:

```powershell
python -m service.exchange export --url https://bcv-admin.onrender.com --output evaluation/dataset.json
dotnet run --project ../backend/Bcv.RecommendationEvaluation -- evaluation/dataset.json evaluation/profiles.json evaluation/rankings.json
python -m service.evaluation template --rankings evaluation/rankings.json --output evaluation/judgments.csv
```

Lệnh xuất chỉ đọc dữ liệu. Giữ nguyên bộ `dataset.json`, `profiles.json`, `rankings.json` trong suốt đợt chấm. Có thể thử công cụ với `profiles.example.json`, nhưng không đưa kết quả minh họa vào báo cáo thực nghiệm người dùng.

## Hướng dẫn người chấm

Mở `judgments.csv` bằng Excel. Với từng tình huống, đánh giá mức phù hợp của địa điểm với sở thích, mức vận động và điểm xuất phát đã cung cấp. Chỉ điền cột `relevance` và tùy chọn `comment`; giữ nguyên các cột mã và dữ liệu địa điểm.

| Điểm | Ý nghĩa |
| --- | --- |
| 0 | Không phù hợp, không muốn chọn |
| 1 | Ít phù hợp |
| 2 | Phù hợp, có thể chọn |
| 3 | Rất phù hợp, muốn chọn |

Nếu chưa đủ thông tin để đánh giá, để trống và ghi lý do; **không dùng 0 thay cho “chưa biết”**. Người tổ chức bổ sung cùng một nguồn mô tả cho mọi người chấm rồi hoàn tất các ô còn thiếu. Phiếu không tiết lộ thuật toán, điểm hệ thống hoặc thứ hạng. Thứ tự địa điểm được xáo trộn xác định theo mã tình huống.

Mỗi người chấm toàn bộ tập địa điểm đủ điều kiện của tình huống mình cung cấp, không chỉ ba gợi ý của một thuật toán. Điều này cần thiết để tính thứ hạng lý tưởng IDCG trên cùng tập ứng viên. Nếu tập quá lớn, thiết kế trước một nghiên cứu nhỏ hơn; không tự bỏ hàng trong phiếu hoặc thay đổi giới hạn khoảng cách giữa các phương pháp.

## Tính và báo cáo

Lưu CSV UTF-8 rồi chạy:

```powershell
python -m service.evaluation score --rankings evaluation/rankings.json --judgments evaluation/judgments.csv --output evaluation/metrics.json
```

Công cụ từ chối ô trống, điểm ngoài 0–3, hàng trùng, mã sai hoặc thiếu ứng viên. Precision@3 coi điểm từ 2 là phù hợp và luôn chia cho 3. NDCG@3 dùng gain `2^điểm − 1`, IDCG lấy từ toàn bộ ứng viên được chấm; trường hợp IDCG bằng 0 được ghi cờ và tính 0. Tình huống không có ứng viên bị loại và được liệt kê. Trung bình tính theo tình huống, không theo người; cần báo cáo số tình huống mỗi người để tránh hiểu sai trọng số khảo sát.

So sánh năm phương pháp trên cùng tập ứng viên: chỉ P; P+C với tỷ lệ 2:1; đầy đủ trọng số cấu hình; bỏ C và chuẩn hóa lại; trọng số ban đầu của tài liệu (0,4/0,2/0,2/0,1/0,1). Các phương pháp dùng chung quy tắc xử lý dữ liệu thiếu. Khoảng cách offline dùng Haversine; khoảng cách API dùng PostGIS, nên không khẳng định hai môi trường luôn cho thứ hạng giống hệt ở sát ranh giới khoảng cách.

Báo cáo phải nêu số người thực tế, số tình huống, cách tuyển người, thời gian thu thập, thang chấm, phiên bản dữ liệu/cấu hình, bảng điểm từng phương pháp và giới hạn nghiên cứu. Silhouette của K-Means không thay thế các chỉ số này. Không dùng điểm do AI tự sinh làm đánh giá người dùng.
