# Mô hình gợi ý Bước Chân Việt (*recommendation*)

Dịch vụ Python nạp mô hình K-Means (35 đặc trưng, K = 15) và trả các địa danh tương đồng về nội dung.

Ba tệp trong `service/data` là bản sao nguyên vẹn của mô hình (`model.joblib`), ma trận nhị phân 35 cột (`features.csv`) và thông tin 315 bản ghi địa danh đã rà soát (`places.csv`). Dịch vụ không huấn luyện trong request và không dùng trường vận động hay rating làm đầu vào K-Means. Nhãn cụm và 35 đặc trưng của 311 địa danh đã được nạp sẵn vào database (bảng `place_feature`, `place_feature_value`), nên Backend chấm điểm gợi ý mà **không cần dịch vụ này chạy**. Dịch vụ dùng để kiểm chứng mô hình và tra cứu địa danh tương đồng.

## Mục lục

- [Cài đặt](#cài-đặt)
- [Sử dụng](#sử-dụng)
- [Rà soát dữ liệu và kiểm chứng huấn luyện](#rà-soát-dữ-liệu-và-kiểm-chứng-huấn-luyện)
- [Mô hình](#mô-hình)
- [API](#api)
- [Đóng góp](#đóng-góp)
- [Giấy phép](#giấy-phép)

## Cài đặt

Cần Python 3.12. Phiên bản scikit-learn và NumPy được khóa theo môi trường đã tạo artifact; dịch vụ từ chối mô hình không tương thích hoặc CSV sai. Ví dụ trên Windows dùng `uv` và đặt môi trường trong thư mục tạm có đường dẫn ASCII, để thư viện native không gặp lỗi đường dẫn tiếng Việt khi huấn luyện:

```powershell
cd recommendation
$aiEnv = Join-Path $env:TEMP 'bcv-ai-py312'
uv venv --python 3.12 $aiEnv
$aiPython = Join-Path $aiEnv 'Scripts\python.exe'
uv pip install --python $aiPython -r requirements-test.txt
```

Trên Linux/macOS dùng `python3.12 -m venv .venv`, cài `requirements-test.txt` bằng pip và thay `$aiPython` bằng `.venv/bin/python` trong các lệnh dưới. Thư mục `.venv` không được commit.

## Sử dụng

Chạy 26 kiểm thử về dữ liệu, bản ghi trùng, dữ liệu thiếu, đặc trưng, tái huấn luyện, chuẩn hóa, lân cận và API:

```powershell
& $aiPython -m unittest discover -s tests -v
```

Chạy dịch vụ cục bộ:

```powershell
& $aiPython -m uvicorn service.app:app --host 127.0.0.1 --port 8090
```

Hoặc bằng Docker (bind ra loopback):

```powershell
docker build -t bcv-recommendation .
docker run --rm -p 127.0.0.1:8090:8090 bcv-recommendation
```

Biến `BV_MODEL_DATA_DIR` chọn thư mục dữ liệu tin cậy khác khi triển khai; chỉ người vận hành cấu hình đường dẫn này, API không nhận upload mô hình. Dịch vụ chỉ dành cho mạng nội bộ; app Android và web quản trị không gọi trực tiếp.

## Rà soát dữ liệu và kiểm chứng huấn luyện

Từ thư mục `recommendation`, chạy:

```powershell
& $aiPython -m service.review audit
& $aiPython -m service.review verify
```

`audit` đối chiếu CSV nguồn với đủ 35 cột, nhãn mô tả, tọa độ, mã chính/mã trùng, trọng số theo số bản ghi của mỗi địa danh và thông tin đánh giá. Bản xuất chỉ chứa 311 địa danh phân biệt; 35 địa danh thiếu điểm và số lượt đánh giá vẫn để trống. Nhãn 0 có nghĩa chưa được ghi nhận. Kiểm tra URL xác nhận trường nguồn có mặt và đúng dạng, không thay thế việc kiểm chứng lại từng trang nguồn hoặc gán nhãn thủ công.

`verify` chạy K = 2–15, mỗi K dùng ba seed 42, 7, 2026, với k-means++, `n_init=20`, `max_iter=500`, `tol=0.0001`, Lloyd. Huấn luyện dùng 315 bản ghi và trọng số 0,5 cho mỗi bản ghi trong cặp trùng. Silhouette và ARI được đánh giá trên **311 địa danh phân biệt**, tránh tính lặp địa danh. Chỉ xét cấu hình có ít nhất ba địa danh phân biệt trong mỗi cụm ở cả ba seed; ưu tiên Silhouette trung bình, rồi ARI trung bình, rồi K nhỏ hơn khi hòa. Trong K được chọn, giữ lần chạy có Silhouette cao nhất.

Kết quả kiểm chứng hiện tại: **K = 15, seed = 42**, Silhouette **0,418647**, Silhouette trung bình **0,413841**, ARI trung bình **0,909102**; nhãn tái huấn luyện có ARI = 1 với artifact và tâm cụm khớp hoàn toàn. Đây là kết quả phân cụm nội dung, chưa phải chất lượng gợi ý cá nhân hóa.

Các kết quả có thể xem và tạo lại bằng các lệnh trên:

| Tệp | Nội dung |
| --- | --- |
| [reports/data-audit.json](reports/data-audit.json) | Tình trạng dữ liệu, bốn cặp trùng, thiếu đánh giá/nhãn và SHA-256 nguồn |
| [reports/canonical-places.csv](reports/canonical-places.csv) | 311 địa danh, mã database, 35 đặc trưng và dữ liệu đánh giá giữ nguyên trạng thái thiếu |
| [reports/model-verification.json](reports/model-verification.json) | Đủ 42 lần huấn luyện, quy tắc chọn, phiên bản thư viện và đối chiếu artifact |
| [reports/model-search.csv](reports/model-search.csv) | Bảng số liệu các lần huấn luyện |
| [reports/clusters.json](reports/clusters.json) | Phân bố, đặc trưng thường gặp và danh sách địa danh trong 15 cụm |

Tham số `--data-dir` và `--output` chọn dữ liệu/đầu ra khác. `verify --model-output <đường-dẫn-model.joblib>` lưu thêm artifact sau khi mọi kiểm chứng đạt; không ghi đè artifact đã rà soát trong `service/data`. Mỗi lần phục vụ vẫn chỉ nạp mô hình có sẵn.

Sau migration `021_versioned_recommendations.sql`, sở thích là sáu nhóm ID 201–206 theo thiết kế: Di sản, Thiên nhiên, Ẩm thực, Biển đảo, Lịch sử, Kiến trúc. Danh mục này độc lập với 12 loại địa danh. Người dùng chọn lại 1–3 nhóm; lựa chọn cũ vẫn được giữ trong bảng cũ, không tự suy diễn sang nhóm mới. Mapping đặc trưng, ngưỡng vận động, mã dữ liệu và SHA-256 mô hình được lưu theo phiên bản cấu hình, cùng snapshot điểm/số lượt đánh giá.

## Đánh giá người dùng và trao đổi mô hình

Xem [mẫu chấm và quy trình đánh giá](evaluation/README.md). Chưa có dữ liệu chấm thực tế, vì vậy chưa công bố Precision@3/NDCG@3. Công cụ dùng chung hàm xếp hạng backend để so sánh P, P+C, đầy đủ, bỏ C và trọng số ban đầu trong tài liệu.

Backend cung cấp API quản trị để xuất dữ liệu, nhập phiên bản DRAFT và kích hoạt riêng. Từ thư mục `recommendation`, sau khi xuất `evaluation/dataset.json` theo hướng dẫn:

```powershell
python -m service.exchange prepare --input evaluation/dataset.json --output evaluation/model.json --version reviewed-k15-v2
python -m service.exchange import --url https://bcv-admin.onrender.com --input evaluation/model.json
# Thay 123 bằng configId thực tế do lệnh import trả về, chỉ kích hoạt sau khi rà soát.
python -m service.exchange activate --url https://bcv-admin.onrender.com --config-id 123
```

`prepare` đóng gói nhãn và 35 đặc trưng từ bộ dữ liệu đã kiểm chứng, không tự huấn luyện lại. API không nhận hoặc thực thi file joblib. Nhập mới không thay phiên bản đang hoạt động; kích hoạt kiểm tra độ phủ dữ liệu và thực hiện trong transaction. Có thể kích hoạt lại cấu hình trước để quay lui. Cần triển khai backend mới và migration 021 trước khi dùng các lệnh này trên server.

Tài liệu phương pháp: [K-Means và sample_weight](https://scikit-learn.org/stable/modules/generated/sklearn.cluster.KMeans.html), [Silhouette](https://scikit-learn.org/stable/modules/generated/sklearn.metrics.silhouette_score.html), [Adjusted Rand Index](https://scikit-learn.org/stable/modules/generated/sklearn.metrics.adjusted_rand_score.html).

## Mô hình

- **Dữ liệu:** 315 bản ghi nguồn, 311 địa danh phân biệt; 4 cặp bản ghi trùng được quy về mã chính (ví dụ `VN-0060` → `VN-0156`).
- **Đặc trưng:** 35 cột nhị phân, 4 nhóm: KindTags (10), LandformTags (11), EcosystemTags (3), ThemeTags (11).
- **Chuẩn hóa:** L2 riêng từng nhóm rồi nhân 0,5; nhóm không có nhãn giữ vector 0; không chuẩn hóa L2 toàn vector thêm lần nữa.
- **Huấn luyện (ngoại tuyến):** K-Means, k-means++, 20 lần khởi tạo, tối đa 500 vòng, ngưỡng 0,0001, Lloyd, K = 15, seed 42 (Silhouette 0,418647).
- **Suy luận:** khoảng cách tới tâm cụm đã fit, tính bằng NumPy, tương đương `predict` của K-Means. Khi khởi động, dịch vụ kiểm tra lại nhãn của đủ 315 bản ghi với artifact. Cách này tránh lỗi threadpoolctl đọc đường dẫn DLL bằng ANSI trên Windows khi dự án nằm trong thư mục có dấu tiếng Việt.

Kết quả ở đây là **độ tương đồng nội dung**, không phải điểm gợi ý cá nhân hóa P/C/A/R/N; phần đó do Backend tính ([backend/README.md](../backend/README.md#gợi-ý-điểm-đến)).

## API

| Endpoint | Trả về |
| --- | --- |
| `GET /health` | Mô hình đã nạp và số địa danh chuẩn (311) |
| `GET /model` | Thứ tự 35 cột, 4 nhóm, K = 15, seed = 42, phiên bản scikit-learn, SHA-256 của artifact |
| `GET /similar/{mã}?limit=5` | Địa danh cùng cụm, sắp theo khoảng cách nội dung tăng dần. `limit` từ 1 đến 20; mã không tồn tại trả 404; mã trùng được quy về mã chính; kết quả không gồm chính địa danh hỏi |

## Đóng góp

Xem [Đóng góp](../README.md#đóng-góp) ở README gốc. Huấn luyện lại mô hình phải giữ đúng thứ tự 35 cột và quy tắc chuẩn hóa, rồi nạp lại nhãn cụm vào `place_feature` của database.

## Giấy phép

UNLICENSED © 2026 Nhóm ĐAHK1262703. Xem [Giấy phép](../README.md#giấy-phép) ở README gốc.
