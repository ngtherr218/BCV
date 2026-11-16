--
-- PostgreSQL database dump
--

\restrict U5Q5vpc9ixOqfI4ZzvOk93iYNVJJHkws6EpCiMgrCv34CoTTEHcyV7xz2Q5M8EF

-- Dumped from database version 18.6
-- Dumped by pg_dump version 18.6

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Data for Name: place_type; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.place_type (place_type_id, name) FROM stdin;
1	Thiên nhiên
2	Di tích lịch sử
3	Tôn giáo và tín ngưỡng
4	Bảo tàng
5	Công trình và không gian xây dựng
6	Chợ
7	Làng nghề và thủ công
8	Cảnh quan nông nghiệp
9	Làng, bản và phố cổ
10	Khu vui chơi và nghỉ dưỡng
101	Di tích và bảo tàng
102	Đền, chùa và nhà thờ
103	Công trình kiến trúc
104	Phố cổ và làng nghề
105	Chợ và ẩm thực
106	Vui chơi và nghỉ dưỡng
107	Đồng quê và nông nghiệp
108	Biển và đảo
109	Sông, hồ và thác
110	Núi và đèo
111	Hang động
112	Rừng và khu bảo tồn
\.


--
-- Data for Name: region; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.region (region_id, name, description, status) FROM stdin;
1	An Giang	An Giang kết hợp cảnh quan rừng tràm Trà Sư với dấu ấn tín ngưỡng tại Miếu Bà Chúa Xứ và các điểm tham quan tâm linh.	ACTIVE
2	Bà Rịa - Vũng Tàu	Bà Rịa - Vũng Tàu có Bãi Sau, núi Dinh và Côn Đảo, phù hợp hành trình khám phá biển, núi và di tích.	ACTIVE
3	Bình Dương	Bình Dương có khu du lịch Đại Nam cùng các điểm văn hóa, tín ngưỡng như chùa Châu Thới và chùa Tây Tạng.	ACTIVE
4	Bình Phước	Bình Phước gợi mở hành trình giữa thiên nhiên Bù Gia Mập, núi Bà Rá và khu di tích lịch sử Tà Thiết.	ACTIVE
5	Bình Thuận	Bình Thuận nổi bật với làng chài Mũi Né, đồi cát Bàu Trắng và hải đăng Kê Gà trên tuyến khám phá ven biển.	ACTIVE
6	Bình Định	Bình Định có biển Kỳ Co, bãi tắm Quy Nhơn và Hòn Khô, mang đến nhiều điểm dừng chân bên biển.	ACTIVE
7	Bạc Liêu	Bạc Liêu lưu giữ câu chuyện địa phương qua Nhà Công tử Bạc Liêu, đồng thời có các điểm tín ngưỡng như chùa Quan Đế.	ACTIVE
8	Bắc Giang	Bắc Giang kết hợp không gian chùa Bổ Đà với cảnh quan hồ Cấm Sơn và những điểm tham quan thiên nhiên.	ACTIVE
9	Bắc Kạn	Bắc Kạn hấp dẫn với hồ Ba Bể, động Puông và thác Đầu Đẳng trong hành trình khám phá núi rừng.	ACTIVE
10	Bắc Ninh	Bắc Ninh có đền Bà Chúa Kho, chùa Phật Tích và chùa Bút Tháp, thích hợp cho hành trình tìm hiểu di sản tín ngưỡng.	ACTIVE
11	Bến Tre	Bến Tre có Cồn Phụng và khu lăng mộ Nguyễn Đình Chiểu, kết hợp trải nghiệm miệt vườn với dấu ấn văn hóa.	ACTIVE
12	Cao Bằng	Cao Bằng nổi bật với thác Bản Giốc, động Ngườm Ngao và cảnh quan núi Mắt Thần.	ACTIVE
13	Cà Mau	Cà Mau có Vườn quốc gia U Minh Hạ, đầm Thị Tường và Hòn Đá Bạc cho hành trình khám phá sông nước, rừng và biển.	ACTIVE
14	Cần Thơ	Cần Thơ gắn với chợ nổi Cái Răng, Chùa Ông và các điểm du lịch sinh thái ven sông.	ACTIVE
15	Gia Lai	Gia Lai có Biển Hồ, chùa Minh Thành và quảng trường Đại Đoàn Kết, kết hợp cảnh quan cao nguyên với nhịp sống đô thị.	ACTIVE
16	Hà Giang	Hà Giang nổi bật với cột cờ Lũng Cú, phố cổ Đồng Văn và đèo Mã Pí Lèng trên các cung đường núi.	ACTIVE
17	Hà Nam	Hà Nam có chùa Tam Chúc, đền Trần Thương và đền Lảnh Giang trong tuyến tham quan văn hóa, tín ngưỡng.	ACTIVE
18	Hà Nội	Hà Nội quy tụ Hồ Gươm, Văn Miếu - Quốc Tử Giám và Hoàng thành Thăng Long, nơi du khách tìm hiểu lịch sử và đời sống thủ đô.	ACTIVE
19	Hà Tĩnh	Hà Tĩnh kết hợp điểm đến lịch sử Ngã ba Đồng Lộc với chùa Hương Tích và biển Thiên Cầm.	ACTIVE
20	Hòa Bình	Hòa Bình có động Đá Bạc, Cửu thác Tú Sơn và các điểm tham quan núi rừng phù hợp cho chuyến đi thiên nhiên.	ACTIVE
21	Hưng Yên	Hưng Yên có chùa Chuông, đền Phù Ủng và đình làng Nôm, gợi hành trình khám phá kiến trúc và văn hóa làng.	ACTIVE
22	Hải Dương	Hải Dương có đền Kiếp Bạc, chùa Côn Sơn và Đảo Cò Chi Lăng Nam, kết hợp di sản với cảnh quan tự nhiên.	ACTIVE
23	Hải Phòng	Hải Phòng có Cát Bà, Đồ Sơn và những điểm tham quan đô thị như cầu Hoàng Văn Thụ.	ACTIVE
24	Hậu Giang	Hậu Giang gắn với chợ nổi Ngã Bảy, sông Cái Lớn và các trải nghiệm du lịch sông nước.	ACTIVE
25	Khánh Hòa	Khánh Hòa có Tháp Bà Ponagar, VinWonders Nha Trang và Bãi Dài Cam Ranh, kết hợp di sản Chăm với vui chơi ven biển.	ACTIVE
26	Kiên Giang	Kiên Giang có đảo Phú Quốc, biển Mũi Nai và các điểm gắn với lịch sử địa phương như Mộ chị Sứ.	ACTIVE
27	Kon Tum	Kon Tum có Nhà Thờ Gỗ, cầu treo Kon Klor và Vườn quốc gia Chư Mom Ray, kết hợp kiến trúc với cảnh quan Tây Nguyên.	ACTIVE
28	Lai Châu	Lai Châu có đèo Ô Quy Hồ, cánh đồng Mường Than và chợ phiên Sìn Hồ giữa không gian núi cao.	ACTIVE
29	Long An	Long An có làng nổi Tân Lập, nhà cổ Trăm Cột và các điểm tham quan văn hóa, thiên nhiên vùng sông nước.	ACTIVE
30	Lào Cai	Lào Cai gắn với đỉnh Fansipan, thác Tình Yêu và những tuyến khám phá cảnh quan vùng núi.	ACTIVE
31	Lâm Đồng	Lâm Đồng có Hồ Xuân Hương, chợ đêm Đà Lạt và Thiền viện Trúc Lâm, phù hợp chuyến đi ngắm cảnh và dạo phố.	ACTIVE
32	Lạng Sơn	Lạng Sơn có chợ Đông Kinh, chùa Tiên và cửa khẩu Hữu Nghị trong hành trình tìm hiểu vùng biên.	ACTIVE
33	Nam Định	Nam Định có đền Trần, nhà thờ Phú Nhai và chợ Viềng, thể hiện nhiều nét sinh hoạt văn hóa, tín ngưỡng.	ACTIVE
34	Nghệ An	Nghệ An có Làng Sen, biển Cửa Lò và khu di tích Truông Bồn cho hành trình kết hợp lịch sử với nghỉ biển.	ACTIVE
35	Ninh Bình	Ninh Bình nổi bật với Tràng An, Tam Cốc và cố đô Hoa Lư, nơi cảnh quan núi đá vôi đi cùng di tích lịch sử.	ACTIVE
36	Ninh Thuận	Ninh Thuận có Vườn quốc gia Núi Chúa, vịnh Vĩnh Hy và các vườn nho trong tuyến khám phá thiên nhiên ven biển.	ACTIVE
37	Phú Thọ	Phú Thọ có Đền Hùng và Vườn quốc gia Xuân Sơn, kết hợp điểm đến cội nguồn với trải nghiệm núi rừng.	ACTIVE
38	Phú Yên	Phú Yên có Gành Đá Đĩa, hải đăng Đại Lãnh và Tháp Nhạn, nổi bật với cảnh quan biển và dấu ấn kiến trúc.	ACTIVE
39	Quảng Bình	Quảng Bình có động Phong Nha cùng các điểm tham quan lịch sử, văn hóa trong hành trình khám phá miền Trung.	ACTIVE
40	Quảng Nam	Quảng Nam kết hợp phố cổ Hội An, thánh địa Mỹ Sơn và biển An Bàng trong một hành trình di sản và nghỉ biển.	ACTIVE
41	Quảng Ngãi	Quảng Ngãi có biển Mỹ Khê, thác Trắng và đèo Vi Ô Lắc, mang đến nhiều lựa chọn khám phá biển và núi.	ACTIVE
42	Quảng Ninh	Quảng Ninh nổi bật với vịnh Hạ Long, biển Bãi Cháy và các điểm vui chơi quanh thành phố Hạ Long.	ACTIVE
43	Quảng Trị	Quảng Trị có địa đạo Vịnh Mốc, nghĩa trang Trường Sơn và biển Cửa Tùng, kết hợp ký ức lịch sử với cảnh biển.	ACTIVE
44	Sóc Trăng	Sóc Trăng có chùa Dơi, chùa Som Rong và các điểm tham quan gắn với đời sống văn hóa địa phương.	ACTIVE
45	Sơn La	Sơn La có đỉnh Tà Xùa, thác Dải Yếm và rừng thông bản Áng, phù hợp hành trình khám phá cao nguyên.	ACTIVE
46	Thanh Hóa	Thanh Hóa có Vườn quốc gia Bến En, suối cá Cẩm Thủy và động Từ Thức cho chuyến đi thiên nhiên.	ACTIVE
47	Thành phố Hồ Chí Minh	Thành phố Hồ Chí Minh có chợ Bến Thành, Dinh Độc Lập và Nhà thờ Đức Bà Sài Gòn, nổi bật với nhịp sống đô thị và di tích.	ACTIVE
48	Thái Bình	Thái Bình có chùa Keo, biển Đồng Châu và các điểm kiến trúc tôn giáo gắn với vùng đồng bằng.	ACTIVE
49	Thái Nguyên	Thái Nguyên có hồ Núi Cốc, thác Đát Đắng và hồ Vai Miếu, phù hợp các chuyến đi ngắm cảnh, dã ngoại.	ACTIVE
50	Thừa Thiên Huế	Thừa Thiên Huế có Đại Nội, chùa Thiên Mụ và lăng Khải Định, tạo nên hành trình khám phá di sản cố đô.	ACTIVE
51	Tiền Giang	Tiền Giang có biển Tân Thành, Nhà thờ chính tòa Mỹ Tho và các điểm tham quan ven sông ở Cái Bè.	ACTIVE
52	Trà Vinh	Trà Vinh có Ao Bà Om, chùa Hang và biển Ba Động, kết hợp cảnh quan sông nước với kiến trúc tín ngưỡng.	ACTIVE
53	Tuyên Quang	Tuyên Quang có đền Hạ, hồ thủy điện và thác Bản Ba trong hành trình khám phá văn hóa, thiên nhiên.	ACTIVE
54	Tây Ninh	Tây Ninh có núi Bà Đen, Tòa thánh Tây Ninh và hồ Dầu Tiếng, kết hợp cảnh núi với điểm đến tín ngưỡng.	ACTIVE
55	Vĩnh Long	Vĩnh Long có khu du lịch Vinh Sang, Văn Thánh Miếu và chùa Tiên Châu, phù hợp hành trình tìm hiểu văn hóa sông nước.	ACTIVE
56	Vĩnh Phúc	Vĩnh Phúc có hồ Đại Lải, đầm Vạc và các khu du lịch sinh thái cho chuyến nghỉ ngơi gần thiên nhiên.	ACTIVE
57	Yên Bái	Yên Bái nổi bật với ruộng bậc thang Mù Cang Chải, hồ Thác Bà và các tuyến khám phá vùng núi.	ACTIVE
58	Điện Biên	Điện Biên có đồi A1, hầm De Castries và tượng đài chiến thắng Điện Biên Phủ, phù hợp hành trình tìm hiểu lịch sử.	ACTIVE
59	Đà Nẵng	Đà Nẵng có Bà Nà Hills, Ngũ Hành Sơn và cầu Rồng, kết hợp vui chơi, cảnh quan và điểm nhấn đô thị.	ACTIVE
60	Đắk Lắk	Đắk Lắk có thác Dray Nur, Bảo tàng cà phê Thế giới và các điểm du lịch gắn với văn hóa Tây Nguyên.	ACTIVE
61	Đắk Nông	Đắk Nông có khu bảo tồn Nâm Nung, hang động Chư Bluk và thác Dray Sap, nổi bật với cảnh quan địa chất, rừng và thác.	ACTIVE
62	Đồng Nai	Đồng Nai có Vườn quốc gia Cát Tiên, khu du lịch Bửu Long và thác Giang Điền cho những chuyến khám phá thiên nhiên.	ACTIVE
63	Đồng Tháp	Đồng Tháp có làng hoa Sa Đéc, Vườn quốc gia Tràm Chim và các điểm tham quan đậm nét sông nước.	ACTIVE
\.


--
-- Data for Name: place; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.place (place_id, region_id, place_type_id, name, latitude, longitude, introduction, highlights, rating, rating_count, status, feature_source) FROM stdin;
320	18	101	Test	21.025975	105.851600	Test	Test	5.00	99999	ACTIVE	CUSTOM
37	29	101	Nhà cổ Trăm Cột	10.482500	106.691389	Là một ngôi nhà cổ độc đáo với 120 cột gỗ quý được chạm khắc tinh xảo.	Bạn có biết? Nhà cổ Trăm Cột là một ngôi nhà cổ độc đáo với 120 cột gỗ quý được chạm khắc tinh xảo.	4.10	621	ACTIVE	CUSTOM
67	38	108	Hòn Yến	13.225716	109.308121	Là một hòn đảo nhỏ với cảnh quan hoang sơ, nổi bật với những rạn san hô tuyệt đẹp.	Bạn có biết? Hòn Yến là một hòn đảo nhỏ với cảnh quan hoang sơ, nổi bật với những rạn san hô tuyệt đẹp.	4.70	307	ACTIVE	CUSTOM
66	38	108	Gành Đá Đĩa	13.354066	109.293787	Một kỳ quan thiên nhiên độc đáo, Gành Đá Đĩa nổi bật với những cột đá bazan xếp chồng lên nhau như những chiếc đĩa.	Bạn có biết? Các cột đá bazan ở Gành Đá Đĩa xếp chồng như những chiếc đĩa.	4.50	8296	ACTIVE	CUSTOM
65	37	109	Suối nước nóng Thanh Thủy	21.154899	105.279180	Là khu vực có các dịch vụ nghỉ dưỡng và trị liệu hiện đại.	Bạn có biết? Suối nước nóng Thanh Thủy là khu vực có các dịch vụ nghỉ dưỡng và trị liệu hiện đại.	\N	0	ACTIVE	CUSTOM
64	37	112	Vườn quốc gia Xuân Sơn	21.150000	104.933000	Là một trong những khu bảo tồn thiên nhiên lớn nhất miền Bắc, nổi bật với hệ sinh thái rừng nhiệt đới và các loài động, thực vật phong phú	Bạn có biết? Vườn quốc gia Xuân Sơn là một trong những khu bảo tồn thiên nhiên lớn nhất miền Bắc, nổi bật với hệ sinh thái rừng nhiệt đới và các loài động, thực vật phong phú.	4.40	2340	ACTIVE	CUSTOM
57	36	110	Vườn quốc gia Núi Chúa	11.700278	109.152778	Là khu bảo tồn thiên nhiên với hệ sinh thái rừng khô hạn độc đáo, được UNESCO công nhận là Khu dự trữ sinh quyển thế giới.	Bạn có biết? Vườn quốc gia Núi Chúa là khu bảo tồn thiên nhiên với hệ sinh thái rừng khô hạn độc đáo, được UNESCO công nhận là Khu dự trữ sinh quyển thế giới.	4.30	2978	ACTIVE	CUSTOM
56	36	108	Vịnh Vĩnh Hy	11.716667	109.202778	Một trong bốn vịnh đẹp nhất Việt Nam, Vĩnh Hy gây ấn tượng bởi nước biển trong xanh, bãi cát trắng mịn, và các rạn san hô rực rỡ.	Bạn có biết? Vịnh Vĩnh Hy là một trong bốn vịnh đẹp nhất Việt Nam, Vĩnh Hy gây ấn tượng bởi nước biển trong xanh, bãi cát trắng mịn, và các rạn san hô rực rỡ.	4.50	1395	ACTIVE	CUSTOM
55	35	109	Hồ Đồng Chương	20.229306	105.797917	Hồ Đồng Chương là một hồ nước tự nhiên nằm giữa rừng thông, rất yên bình và thơ mộng.	Bạn có biết? Hồ Đồng Chương là một hồ nước tự nhiên nằm giữa rừng thông, rất yên bình và thơ mộng.	3.90	315	ACTIVE	CUSTOM
54	35	111	Vườn quốc gia Cúc Phương	20.316667	105.608333	Là vườn quốc gia đầu tiên của Việt Nam với hệ động thực vật phong phú, những cây cổ thụ nghìn năm tuổi và các hang động tiền sử.	Bạn có biết? Vườn quốc gia Cúc Phương là vườn quốc gia đầu tiên của Việt Nam với hệ động thực vật phong phú, những cây cổ thụ nghìn năm tuổi và các hang động tiền sử.	4.50	5206	ACTIVE	CUSTOM
52	35	111	Tam Cốc	20.218515	105.914698	Được mệnh danh là "vịnh Hạ Long trên cạn", với cảnh sắc thiên nhiên tuyệt đẹp của núi đá vôi, cánh đồng lúa, và dòng sông Ngô Đồng.	Bạn có biết? Tam Cốc được mệnh danh là vịnh Hạ Long trên cạn.	4.50	18465	ACTIVE	CUSTOM
51	35	111	Tràng An	20.256667	105.896389	Là Di sản Văn hóa và Thiên nhiên thế giới, Tràng An nổi bật với cảnh núi non hùng vĩ và hệ thống hang động độc đáo.	Bạn có biết? Tràng An là di sản thế giới kết hợp giá trị văn hóa và cảnh quan thiên nhiên.	4.60	15397	ACTIVE	CUSTOM
50	34	111	Hang Thẩm Ồm	19.650204	105.074146	Là một hang động lớn, với nhiều thạch nhũ kỳ ảo, hình thù độc đáo.	Bạn có biết? Hang Thẩm Ồm là một hang động lớn, với nhiều thạch nhũ kỳ ảo, hình thù độc đáo.	4.70	13	ACTIVE	CUSTOM
47	34	108	Bãi biển Cửa Lò	18.778470	105.748687	Một trong những bãi biển đẹp nhất miền Trung, Cửa Lò thu hút du khách với cát trắng mịn, nước biển trong xanh và hải sản tươi ngon.	Bạn có biết? Bãi biển Cửa Lò là một trong những bãi biển đẹp nhất miền Trung, Cửa Lò thu hút du khách với cát trắng mịn, nước biển trong xanh và hải sản tươi ngon.	4.30	1657	ACTIVE	CUSTOM
45	33	109	Hồ Vị Xuyên	20.431095	106.182181	Hồ nằm trong thành phố Nam Định, là một địa điểm lý tưởng để thư giãn, đi dạo, thưởng thức không gian xanh và yên bình.	Bạn có biết? Hồ Vị Xuyên — Hồ nằm trong thành phố Nam Định, là một địa điểm lý tưởng để thư giãn, đi dạo, thưởng thức không gian xanh và yên bình.	4.70	234	ACTIVE	CUSTOM
43	33	108	Vườn quốc gia Xuân Thủy	20.230000	106.517000	Đây là khu bảo tồn thiên nhiên nằm ở huyện Giao Thủy, thuộc vùng hạ lưu sông Hồng.	Bạn có biết? Vườn quốc gia Xuân Thủy — Đây là khu bảo tồn thiên nhiên nằm ở huyện Giao Thủy, thuộc vùng hạ lưu sông Hồng.	4.30	970	ACTIVE	CUSTOM
39	29	109	Sông Vàm Cỏ	10.490556	106.727778	Hai dòng sông lớn chảy qua Long An tạo nên vẻ đẹp sông nước đặc trưng.	Bạn có biết? Sông Vàm Cỏ — Hai dòng sông lớn chảy qua Long An tạo nên vẻ đẹp sông nước đặc trưng.	\N	0	ACTIVE	CUSTOM
36	29	112	Làng nổi Tân Lập	10.713907	105.986087	Là khu rừng tràm ngập nước rộng lớn, nổi tiếng với con đường xuyên rừng dài gần 5km.	Bạn có biết? Làng nổi Tân Lập là khu rừng tràm ngập nước rộng lớn, nổi tiếng với con đường xuyên rừng dài gần 5km.	4.00	2441	ACTIVE	CUSTOM
34	30	109	Thác Tình Yêu	22.347336	103.772429	Là một điểm đến lãng mạn với dòng nước trong xanh chảy qua rừng nguyên sinh.	Bạn có biết? Thác Tình Yêu là một điểm đến lãng mạn với dòng nước trong xanh chảy qua rừng nguyên sinh.	4.50	1206	ACTIVE	CUSTOM
32	30	110	Cổng Trời Ô Quy Hồ	22.352752	103.765305	Đèo Ô Quy Hồ là một trong "Tứ đại đỉnh đèo" của Tây Bắc, nối liền Lào Cai và Lai Châu.	Bạn có biết? Cổng Trời Ô Quy Hồ — Đèo Ô Quy Hồ là một trong "Tứ đại đỉnh đèo" của Tây Bắc, nối liền Lào Cai và Lai Châu.	4.40	702	ACTIVE	CUSTOM
31	30	110	Đỉnh Núi Fansipan	22.304276	103.777063	Fansipan được mệnh danh là “Nóc nhà Đông Dương” với độ cao 3.147m.	Bạn có biết? Fansipan được mệnh danh là Nóc nhà Đông Dương.	4.80	1763	ACTIVE	CUSTOM
29	32	110	Đèo Bông Lau	22.342784	106.446721	Là một cung đèo đẹp với những khúc cua uốn lượn và khung cảnh thiên nhiên ngoạn mục.	Bạn có biết? Đèo Bông Lau là một cung đèo đẹp với những khúc cua uốn lượn và khung cảnh thiên nhiên ngoạn mục.	4.60	27	ACTIVE	CUSTOM
26	32	111	Động Tam Thanh	21.855567	106.746799	Được mệnh danh là "Đệ nhất bát cảnh Xứ Lạng"các nhũ đá kỳ thú và nhiều di tích cổ như tượng Phật A Di Đà cao 2m được tạc từ thế kỷ 15.	Bạn có biết? Trong động Tam Thanh có tượng Phật A Di Đà được tạc từ thế kỷ 15.	\N	0	ACTIVE	CUSTOM
25	31	109	Thác Voi	11.823840	108.336735	Tọa lạc ở Nam Ban, thác Voi hùng vĩ và hoang sơ, là nơi lý tưởng cho những ai yêu thích khám phá thiên nhiên.	Bạn có biết? Thác Voi — Tọa lạc ở Nam Ban, thác Voi hùng vĩ và hoang sơ, là nơi lý tưởng cho những ai yêu thích khám phá thiên nhiên.	4.00	141	ACTIVE	CUSTOM
21	31	109	Hồ Xuân Hương	11.941845	108.446835	Biểu tượng của Đà Lạt, hồ nằm ngay trung tâm thành phố, bao quanh bởi rừng thông và những vườn hoa.	Bạn có biết? Hồ Xuân Hương nằm ngay trung tâm Đà Lạt, giữa rừng thông và những vườn hoa.	4.70	407	ACTIVE	CUSTOM
19	28	109	Hồ Thủy Điện Lai Châu	22.139444	102.985278	Hồ nước nhân tạo khổng lồ được tạo nên từ công trình Thủy điện Lai Châu, nằm giữa khung cảnh núi non thơ mộng.	Bạn có biết? Hồ Thủy Điện Lai Châu — Hồ nước nhân tạo khổng lồ được tạo nên từ công trình Thủy điện Lai Châu, nằm giữa khung cảnh núi non thơ mộng.	\N	0	ACTIVE	CUSTOM
17	28	110	Đỉnh Pu Ta Leng	22.422778	103.603333	Đỉnh núi cao thứ hai Việt Nam (3.049m), thuộc dãy Hoàng Liên Sơn, được mệnh danh là "nóc nhà thứ hai" của Đông Dương.	Bạn có biết? Đỉnh Pu Ta Leng — Đỉnh núi cao thứ hai Việt Nam (3.049m), thuộc dãy Hoàng Liên Sơn, được mệnh danh là "nóc nhà thứ hai" của Đông Dương.	4.90	9	ACTIVE	CUSTOM
289	19	101	Khu di tích Nguyễn Du	18.664999	105.766810	Khu di tích Nguyễn Du (Hà Tĩnh) là nơi tưởng niệm đại thi hào Nguyễn Du, tác giả của "Truyện Kiều". Di tích này giữ gìn những giá trị văn hóa, lịch sử về cuộc đời và sự nghiệp của ông.	Bạn có biết? Khu di tích Nguyễn Du (Hà Tĩnh) là nơi tưởng niệm đại thi hào Nguyễn Du, tác giả của "Truyện Kiều".	4.70	646	ACTIVE	CUSTOM
286	19	101	Ngã ba Đồng Lộc	18.399563	105.738785	Ngã ba Đồng Lộc (Hà Tĩnh) là di tích lịch sử nổi tiếng gắn liền với cuộc kháng chiến chống Mỹ. Nơi đây là nơi tưởng niệm các liệt sĩ hy sinh trong chiến tranh.	Bạn có biết? Ngã ba Đồng Lộc (Hà Tĩnh) là di tích lịch sử nổi tiếng gắn liền với cuộc kháng chiến chống Mỹ.	4.70	1538	ACTIVE	CUSTOM
283	18	101	Văn Miếu - Quốc Tử Giám	21.028611	105.835556	Văn Miếu - Quốc Tử Giám (Hà Nội) là một di tích lịch sử và văn hóa, nơi thờ Khổng Tử và là trường đại học đầu tiên của Việt Nam. Đây là điểm tham quan giáo dục và văn hóa.	Bạn có biết? Văn Miếu - Quốc Tử Giám (Hà Nội) là một di tích lịch sử và văn hóa, nơi thờ Khổng Tử và là trường đại học đầu tiên của Việt Nam.	4.60	25999	ACTIVE	CUSTOM
281	18	101	Lăng Chủ tịch Hồ Chí Minh	21.036667	105.834722	Lăng Chủ tịch Hồ Chí Minh (Hà Nội) là nơi lưu giữ thi hài của Chủ tịch Hồ Chí Minh, trở thành biểu tượng của lòng tôn kính và biết ơn của nhân dân Việt Nam.	Bạn có biết? Lăng Chủ tịch Hồ Chí Minh (Hà Nội) là nơi lưu giữ thi hài của Chủ tịch Hồ Chí Minh, trở thành biểu tượng của lòng tôn kính và biết ơn của nhân dân Việt Nam.	4.50	21480	ACTIVE	CUSTOM
275	16	101	Dinh Vua Mèo	23.256492	105.262146	Dinh Vua Mèo (Hà Giang) là dinh thự cổ của dòng họ Vương, với kiến trúc kết hợp độc đáo giữa Trung Hoa, Pháp và văn hóa dân tộc H'Mông, thu hút nhiều du khách tham quan.	Bạn có biết? Dinh Vua Mèo (Hà Giang) là dinh thự cổ của dòng họ Vương, với kiến trúc kết hợp độc đáo giữa Trung Hoa, Pháp và văn hóa dân tộc H'Mông, thu hút nhiều du khách tham quan.	4.60	181	ACTIVE	CUSTOM
264	63	101	Lăng cụ Nguyễn Sinh Sắc	10.446651	105.631059	Nơi an nghỉ của cụ Nguyễn Sinh Sắc, thân sinh Chủ tịch Hồ Chí Minh. Lăng được thiết kế hài hòa giữa kiến trúc và cảnh quan thiên nhiên.	Bạn có biết? Lăng cụ Nguyễn Sinh Sắc là nơi an nghỉ của cụ Nguyễn Sinh Sắc, thân sinh Chủ tịch Hồ Chí Minh.	4.70	925	ACTIVE	CUSTOM
263	63	101	Khu di tích Xẻo Quýt	10.399444	105.801667	Một khu di tích lịch sử kết hợp du lịch sinh thái tại Đồng Tháp, nơi tái hiện các căn cứ cách mạng trong rừng tràm thời kháng chiến.	Bạn có biết? Khu di tích Xẻo Quýt là một khu di tích lịch sử kết hợp du lịch sinh thái tại Đồng Tháp, nơi tái hiện các căn cứ cách mạng trong rừng tràm thời kháng chiến.	4.50	948	ACTIVE	CUSTOM
255	58	101	Tượng đài chiến thắng Điện Biên Phủ	21.392174	103.018938	Tượng đài nằm trên đồi D1, là biểu tượng của chiến thắng lịch sử Điện Biên Phủ. Đây là nơi tôn vinh sự hy sinh và chiến công của các chiến sĩ Việt Nam, đồng thời cũng là điểm ngắm toàn cảnh thành phố Điện Biên.	Bạn có biết? Tượng đài chiến thắng Điện Biên Phủ — Tượng đài nằm trên đồi D1, là biểu tượng của chiến thắng lịch sử Điện Biên Phủ.	4.60	1393	ACTIVE	CUSTOM
148	2	108	Hồ Tràm	10.535094	107.399238	Là một trong những bãi biển đẹp nhất khu vực miền Nam	Bạn có biết? Hồ Tràm là một trong những bãi biển đẹp nhất khu vực miền Nam.	4.40	431	ACTIVE	CUSTOM
252	58	101	Hầm De Castries	21.384944	103.010675	Đây là nơi ở và chỉ huy của tướng De Castries trong chiến dịch Điện Biên Phủ. Hầm được xây dựng kiên cố và hiện nay là di tích lịch sử được bảo tồn để tái hiện lại chiến trường năm xưa.	Bạn có biết? Hầm De Castries — Đây là nơi ở và chỉ huy của tướng De Castries trong chiến dịch Điện Biên Phủ.	4.30	2559	ACTIVE	CUSTOM
251	58	101	Đồi A1	21.382932	103.017673	Đồi A1 là một trong những di tích lịch sử quan trọng trong chiến dịch Điện Biên Phủ, nơi từng diễn ra các trận đánh khốc liệt. Hiện nay, nơi đây trở thành điểm tham quan với hầm chiến đấu, hệ thống giao thông hào và bảo tồn nhiều hiện vật lịch sử.	Bạn có biết? Đồi A1 là một địa điểm gắn với những trận đánh trong chiến dịch Điện Biên Phủ.	4.50	2836	ACTIVE	CUSTOM
209	12	101	Di tích rừng Trần Hưng Đạo	22.591606	106.040721	Nơi thành lập Đội Việt Nam Tuyên Truyền Giải Phóng Quân năm 1944, khởi nguồn của Quân đội Nhân dân Việt Nam. Khu di tích có giá trị lịch sử to lớn.	Bạn có biết? Di tích rừng Trần Hưng Đạo là nơi thành lập Đội Việt Nam Tuyên Truyền Giải Phóng Quân năm 1944, khởi nguồn của Quân đội Nhân dân Việt Nam.	4.50	138	ACTIVE	CUSTOM
188	4	101	Khu di tích Tà Thiết	11.747788	106.471953	Nơi lưu giữ nhiều dấu ấn lịch sử của chiến tranh Việt Nam, với các di tích như hầm chỉ huy và kho quân khí	Bạn có biết? Khu di tích Tà Thiết là nơi lưu giữ nhiều dấu ấn lịch sử của chiến tranh Việt Nam, với các di tích như hầm chỉ huy và kho quân khí.	4.40	505	ACTIVE	CUSTOM
164	7	101	Nhà Công tử Bạc Liêu	9.284084	105.723806	Ngôi nhà này từng là nơi sinh sống của công tử Bạc Liêu, một nhân vật nổi tiếng trong lịch sử.	Bạn có biết? Nhà Công tử Bạc Liêu — Ngôi nhà này từng là nơi sinh sống của công tử Bạc Liêu, một nhân vật nổi tiếng trong lịch sử.	4.40	4103	ACTIVE	CUSTOM
191	5	101	Tháp Chàm Poshanư	10.935947	108.146173	Công trình kiến trúc cổ của người Chăm, mang giá trị lịch sử và văn hóa đặc sắc	Bạn có biết? Tháp Chàm Poshanư — Công trình kiến trúc cổ của người Chăm, mang giá trị lịch sử và văn hóa đặc sắc.	4.40	1174	ACTIVE	CUSTOM
7	26	108	Biển Mũi Nai	10.380867	104.445337	Bãi biển hiền hòa, nơi lý tưởng để thư giãn và ngắm cảnh hoàng hôn	Bạn có biết? Biển Mũi Nai — Bãi biển hiền hòa, nơi lý tưởng để thư giãn và ngắm cảnh hoàng hôn.	4.00	3687	ACTIVE	CUSTOM
140	47	101	Địa đạo Củ Chi	11.061000	106.526000	Nơi đây là di tích quốc gia đặc biệt, gắn liền với lịch sử chiến tranh Việt Nam, từng là căn cứ địa quan trọng và cũng là nơi sinh hoạt chung bao gồm bệnh xá, nhà bếp, nơi làm việc,… của quân đội ta trong cuộc kháng chiến chống quân xâm lược, giành lại toàn vẹn lãnh thổ. Bên cạnh đó, bên trong địa đạo Củ Chi còn tái hiện tại cuộc sống của họ qua những qua những mô hình người và vật rất chân thật, ấn tượng. Nơi đây là một trong những điểm đến hấp dẫn của những tour Hồ Chí Minh mà bạn không nên bỏ qua trong chuyến đi của mình.	Bạn có biết? Địa đạo Củ Chi là nơi đây là di tích quốc gia đặc biệt, gắn liền với lịch sử chiến tranh Việt Nam, từng là căn cứ địa quan trọng và cũng là nơi sinh hoạt chung bao gồm bệnh xá, nhà bếp, nơi làm việc,… của quân đội ta trong cuộc kháng chiến chống quân xâm lược, giành lại toàn vẹn lãnh thổ.	4.60	18468	ACTIVE	CUSTOM
137	47	101	Dinh Độc Lập	10.777108	106.695441	Nơi đây đã được công nhận là 1 trong 10 di tích quốc gia đặc biệt của Việt Nam vào năm 2009. Dinh Độc Lập là nơi trưng bày và lưu giữ những hình ảnh, hiện vật giá trị từ những năm của thế kỷ 19 theo từng chủ đề khác nhau. Đặc biệt nơi đây vẫn còn giữ nguyên nội thất của những căn phòng quan trọng như phòng khánh tiết, phòng nội các,....	Bạn có biết? Dinh Độc Lập là nơi đây đã được công nhận là 1 trong 10 di tích quốc gia đặc biệt của Việt Nam vào năm 2009.	4.50	48922	ACTIVE	CUSTOM
129	50	101	Lăng Minh Mạng	16.387778	107.567778	Lăng Minh Mạng còn gọi là Hiếu Lăng, nằm trên núi Cẩm Khê, gần ngã ba Bằng Lãng là nơi hội lưu của hai dòng Hữu Trạch và Tả Trạch hợp thành sông Hương, cách cố đô Huế 12 km. Lăng Minh Mạng được xây dựng từ năm 1840 đến năm 1843 thì hoàn thành, huy động tới mười nghìn thợ và lính.	Bạn có biết? Lăng Minh Mạng còn gọi là Hiếu Lăng, nằm trên núi Cẩm Khê, gần ngã ba Bằng Lãng là nơi hội lưu của hai dòng Hữu Trạch và Tả Trạch hợp thành sông Hương, cách cố đô Huế 12 km.	4.50	6317	ACTIVE	CUSTOM
128	50	101	Lăng Khải Định	16.398889	107.590278	Nhắc đến Huế người ta không khỏi đề cập đến lăng tẩm thời Nguyễn. Tour du lịch của bạn sẽ thú vị hơn, nếu đừng chân ghé thăm điểm đến Lăng Khải Định. Du khách sẽ được chiêm ngưỡng đỉnh cao nghệ thuật tạo hình sành sứ và thủy tinh rất tinh xảo của bàn tay nghệ nhân thời xưa qua, bởi lẽ. Có thể nói lăng Khải Định là một công trình kết hợp độc đáo giữa kiến trúc Đông – Tây tinh xảo.	Bạn có biết? Lăng Khải Định kết hợp kiến trúc Đông - Tây với nghệ thuật trang trí sành sứ, thủy tinh.	4.50	17196	ACTIVE	CUSTOM
126	50	101	Đại Nội Huế	16.470231	107.577251	Nằm ở bờ Bắc của dòng sông Hương thơ mộng trữ tình, Đại Nội Huế với kiến trúc nghệ thuật cung đình và vườn hào độc đáo. Cũng là trung tâm hành chính, chính trị của triều đình nhà Nguyễn và nơi sinh hoạt của nhà vua hoàng gia ngày xưa.	Bạn có biết? Đại Nội Huế từng là trung tâm hành chính và nơi sinh hoạt của hoàng gia triều Nguyễn.	4.60	24549	ACTIVE	CUSTOM
172	11	101	Khu lăng mộ cụ Nguyễn Đình Chiểu	10.037083	106.582305	Khu lăng mộ Nguyễn Đình Chiểu ở Bến Tre là nơi yên nghỉ của nhà thơ, nhà văn nổi tiếng.	Bạn có biết? Khu lăng mộ cụ Nguyễn Đình Chiểu — Khu lăng mộ Nguyễn Đình Chiểu ở Bến Tre là nơi yên nghỉ của nhà thơ, nhà văn nổi tiếng.	4.60	1016	ACTIVE	CUSTOM
109	54	101	Tháp Chót Mạt	11.463680	106.010233	Tòa tháp này có kiến trúc rất đặc biệt đó chính là được xây dựng nhỏ dần lên phía trên nhưng lại không hề có đỉnh chóp. Người Óc Eo rất khéo léo và thông minh khi xây dựng công trình, họ không hề chút xi măng nào làm vật liệu kết dính. Khi đến đây tham quan, du khách có thể thoải mái check in tại một không gian vô cùng yên bình và mang nét hoài cổ của địa điểm du lịch Tây Ninh này.	Bạn có biết? Tháp Chót Mạt — Tòa tháp này có kiến trúc rất đặc biệt đó chính là được xây dựng nhỏ dần lên phía trên nhưng lại không hề có đỉnh chóp.	4.40	196	ACTIVE	CUSTOM
92	43	101	Địa đạo Vịnh Mốc	17.073000	107.111000	Công trình này ở sâu lòng đất, được người dân đào trong kháng chiến chống Mỹ. Địa đạo gồm có nhiều khu vực như trường học, kho hậu cần, bệnh viện, khu quân sự và khu gia đình.	Bạn có biết? Bên trong địa đạo Vịnh Mốc từng có cả trường học, bệnh viện và khu sinh hoạt gia đình.	4.60	3218	ACTIVE	CUSTOM
91	43	101	Nghĩa trang Liệt sĩ Quốc gia Trường Sơn	16.958621	106.954859	Địa điểm ghi dấu ấn về những mất mát, minh chứng cho những gì tàn bạo nhất của giặc ngoại xâm. Nơi này đã đi vào lịch sử, biểu tượng của sự bi tráng và lòng anh dũng của những con người yêu nước, đặt sự độc lập và vận mệnh dân tộc lên hàng đầu.	Bạn có biết? Nghĩa trang Liệt sĩ Quốc gia Trường Sơn — Địa điểm ghi dấu ấn về những mất mát, minh chứng cho những gì tàn bạo nhất của giặc ngoại xâm.	4.80	611	ACTIVE	CUSTOM
80	40	101	Thánh Địa Mỹ Sơn	15.766667	108.116667	Một thung lũng có đường kính 2km, Nơi đây bao quanh là núi đồi, vô cùng ẩn mình. Địa điểm này từng là nơi tổ chức cúng tế vương triều Champa. Khám quá nhiều di sản quý báu khi đến nới đây.	Bạn có biết? Thánh Địa Mỹ Sơn là một thung lũng có đường kính 2km, Nơi đây bao quanh là núi đồi, vô cùng ẩn mình.	4.50	13044	ACTIVE	CUSTOM
78	40	101	Tháp Chiên Đàn	15.615809	108.442489	Hiểu hơn về văn hóa Champa, sở hữu một nhà trưng bày nhưng tác phẩm điêu khắc tiêu biểu, vô vàn những món đồ quý hiếm	Bạn có biết? Tháp Chiên Đàn — Hiểu hơn về văn hóa Champa, sở hữu một nhà trưng bày nhưng tác phẩm điêu khắc tiêu biểu, vô vàn những món đồ quý hiếm.	4.40	163	ACTIVE	CUSTOM
74	39	101	Nhà lưu niệm Đại tướng Võ Nguyên Giáp	17.243777	106.749846	Là nơi lưu giữ những kỷ vật gắn bó với tuổi thơ của Đại tướng Võ Nguyên Giáp.	Bạn có biết? Nhà lưu niệm Đại tướng Võ Nguyên Giáp là nơi lưu giữ những kỷ vật gắn bó với tuổi thơ của Đại tướng Võ Nguyên Giáp.	4.90	122	ACTIVE	CUSTOM
70	38	101	Tháp Nhạn	13.077778	109.297222	Tháp Nhạn nằm trên núi Nhạn ở thành phố Tuy Hòa, là một công trình kiến trúc Chăm cổ kính và linh thiêng.	Bạn có biết? Tháp Nhạn nằm trên núi Nhạn ở thành phố Tuy Hòa, là một công trình kiến trúc Chăm cổ kính và linh thiêng.	4.70	953	ACTIVE	CUSTOM
58	36	101	Tháp Po Klong Garai	11.601831	108.944294	Đây là nơi thờ vua Po Klong Garai, thể hiện kiến trúc và nghệ thuật điêu khắc tinh xảo của người Chăm.	Bạn có biết? Tháp Po Klong Garai — Đây là nơi thờ vua Po Klong Garai, thể hiện kiến trúc và nghệ thuật điêu khắc tinh xảo của người Chăm.	4.80	28	ACTIVE	CUSTOM
53	35	101	Cố đô Hoa Lư	20.284315	105.908361	Là kinh đô của nước Đại Cồ Việt dưới triều Đinh và Tiền Lê, Hoa Lư lưu giữ nhiều di tích lịch sử quan trọng	Bạn có biết? Hoa Lư từng là kinh đô Đại Cồ Việt dưới triều Đinh và Tiền Lê.	4.30	12531	ACTIVE	CUSTOM
48	34	101	Khu di tích Truông Bồn	18.786300	105.411068	Là một di tích lịch sử quan trọng, nơi tưởng nhớ các anh hùng liệt sĩ đã hy sinh trong cuộc kháng chiến chống Mỹ.	Bạn có biết? Khu di tích Truông Bồn là một di tích lịch sử quan trọng, nơi tưởng nhớ các anh hùng liệt sĩ đã hy sinh trong cuộc kháng chiến chống Mỹ.	4.50	1172	ACTIVE	CUSTOM
46	34	101	Làng Sen	18.679861	105.552000	Là một ngôi nhà tranh đơn sơ, giếng Cốc, và những kỷ vật gắn liền với thời thơ ấu của Bác.	Bạn có biết? Làng Sen là một ngôi nhà tranh đơn sơ, giếng Cốc, và những kỷ vật gắn liền với thời thơ ấu của Bác.	4.60	8952	ACTIVE	CUSTOM
24	31	101	Dinh III Bảo Đại (Đà Lạt)	11.930172	108.429661	Một trong ba dinh thự của vua Bảo Đại tại Đà Lạt, mang phong cách kiến trúc Pháp tinh tế.	Bạn có biết? Dinh III Bảo Đại (Đà Lạt) là một trong ba dinh thự của vua Bảo Đại tại Đà Lạt, mang phong cách kiến trúc Pháp tinh tế.	4.20	9671	ACTIVE	CUSTOM
253	58	101	Bảo tàng chiến thắng Điện Biên Phủ	21.380757	103.015258	Bảo tàng lưu giữ các hiện vật, tư liệu và hình ảnh về chiến dịch Điện Biên Phủ, tái hiện lịch sử hào hùng của dân tộc Việt Nam. Đây là điểm đến không thể bỏ qua khi ghé thăm Điện Biên.	Bạn có biết? Bảo tàng chiến thắng Điện Biên Phủ — Bảo tàng lưu giữ các hiện vật, tư liệu và hình ảnh về chiến dịch Điện Biên Phủ, tái hiện lịch sử hào hùng của dân tộc Việt Nam.	4.70	1271	ACTIVE	CUSTOM
244	60	101	Bảo tàng cà phê Thế giới	12.690858	108.044520	Bảo tàng Thế giới Cà phê, tọa lạc tại Buôn Ma Thuột, Đắk Lắk, là một điểm đến hấp dẫn cho những ai yêu thích cà phê và muốn tìm hiểu về văn hóa cà phê toàn cầu	Bạn có biết? Bảo tàng cà phê Thế giới — Bảo tàng Thế giới Cà phê, tọa lạc tại Buôn Ma Thuột, Đắk Lắk, là một điểm đến hấp dẫn cho những ai yêu thích cà phê và muốn tìm hiểu về văn hóa cà phê toàn cầu.	4.20	7558	ACTIVE	CUSTOM
139	47	101	Bảo tàng Lịch sử Thành phố Hồ Chí Minh	10.784840	106.707590	Bảo tàng lịch sử Việt Nam được xây dựng và hoạt động từ những năm đầu thế kỷ 20, là nơi lưu giữ và bảo tồn những hình ảnh, cổ vật từ thuở sơ khai đến nay. Bảo tàng lịch sử Việt Nam thu hút phần lớn những du khách yêu lịch sử và kiến trúc pha trộn giữa 2 phong cách Á – Âu độc đáo.	Bạn có biết? Bảo tàng Lịch sử Thành phố Hồ Chí Minh — Bảo tàng lịch sử Việt Nam được xây dựng và hoạt động từ những năm đầu thế kỷ 20, là nơi lưu giữ và bảo tồn những hình ảnh, cổ vật từ thuở sơ khai đến nay.	4.50	4230	ACTIVE	CUSTOM
310	20	102	Đền Thác Bờ	20.768266	105.175825	Đền Thác Bờ (Hòa Bình) là một di tích lịch sử, thờ nữ tướng Cao Thị Lịch. Đền nằm gần thác Bờ, có khung cảnh huyền bí và là nơi linh thiêng, thu hút du khách đến tham quan và cầu nguyện.	Bạn có biết? Đền Thác Bờ (Hòa Bình) là một di tích lịch sử, thờ nữ tướng Cao Thị Lịch.	4.50	1071	ACTIVE	CUSTOM
294	22	102	Đền Kiếp Bạc	21.151053	106.327589	Đền Kiếp Bạc (Hải Dương) là nơi thờ Hưng Đạo Đại Vương Trần Quốc Tuấn, người có công lớn trong chiến tranh chống Nguyên. Đền mang đậm giá trị lịch sử và văn hóa.	Bạn có biết? Đền Kiếp Bạc (Hải Dương) là nơi thờ Hưng Đạo Đại Vương Trần Quốc Tuấn, người có công lớn trong chiến tranh chống Nguyên.	4.60	3120	ACTIVE	CUSTOM
292	22	102	Chùa Côn Sơn	21.149837	106.384008	Chùa Côn Sơn (Hải Dương) là di tích lịch sử và văn hóa, nổi bật với kiến trúc cổ kính và không gian thanh tịnh. Nơi đây là một trong những địa điểm du lịch tâm linh nổi tiếng ở miền Bắc.	Bạn có biết? Chùa Côn Sơn (Hải Dương) là di tích lịch sử và văn hóa, nổi bật với kiến trúc cổ kính và không gian thanh tịnh.	4.70	704	ACTIVE	CUSTOM
312	21	102	Chùa Chuông	20.655800	106.050000	Chùa Chuông là ngôi chùa thuộc quần thể di tích Phố Hiến ở Hưng Yên, gắn với kiến trúc Phật giáo và sinh hoạt tín ngưỡng của địa phương.	Bạn có biết? Chùa Chuông là ngôi chùa thuộc quần thể di tích Phố Hiến ở Hưng Yên, gắn với kiến trúc Phật giáo và sinh hoạt tín ngưỡng của địa phương.	4.70	1226	ACTIVE	CUSTOM
287	19	102	Chùa Hương Tích (Hà Tĩnh)	18.534142	105.789192	Chùa Hương Tích nằm trên dãy Hồng Lĩnh, khu vực Thiên Lộc, Can Lộc, Hà Tĩnh. Đây là di tích Phật giáo và điểm hành hương gắn với cảnh quan núi rừng.	Bạn có biết? Chùa Hương Tích (Hà Tĩnh) — Chùa Hương Tích nằm trên dãy Hồng Lĩnh, khu vực Thiên Lộc, Can Lộc, Hà Tĩnh.	4.60	1287	ACTIVE	CUSTOM
279	17	102	Đền Lảnh Giang	20.668960	106.025385	Đền Lảnh Giang tại khu vực Mộc Nam, Duy Tiên, Hà Nam, là di tích tín ngưỡng thờ các vị thủy thần gắn với truyền thuyết thời Hùng Vương.	Bạn có biết? Đền Lảnh Giang tại khu vực Mộc Nam, Duy Tiên, Hà Nam, là di tích tín ngưỡng thờ các vị thủy thần gắn với truyền thuyết thời Hùng Vương.	4.70	886	ACTIVE	CUSTOM
277	17	102	Đền Trần Thương	20.576157	106.103748	Đền Trần Thương (Hà Nam) thờ các vị vua Trần, nổi bật với kiến trúc cổ kính và không gian thanh tịnh. Đền là điểm đến văn hóa, tâm linh, thu hút du khách vào mùa lễ hội.	Bạn có biết? Đền Trần Thương (Hà Nam) thờ các vị vua Trần, nổi bật với kiến trúc cổ kính và không gian thanh tịnh.	4.70	531	ACTIVE	CUSTOM
276	17	102	Chùa Tam Chúc	20.553243	105.796146	Chùa Tam Chúc (Hà Nam) là một trong những quần thể chùa lớn nhất Việt Nam, nổi bật với kiến trúc hoành tráng và cảnh quan thiên nhiên tươi đẹp. Được bao quanh bởi những dãy núi, hồ nước trong xanh, đây là nơi thu hút tín đồ Phật giáo và du khách tham quan.	Bạn có biết? Chùa Tam Chúc (Hà Nam) là một trong những quần thể chùa lớn nhất Việt Nam, nổi bật với kiến trúc hoành tráng và cảnh quan thiên nhiên tươi đẹp.	4.40	358	ACTIVE	CUSTOM
268	15	102	Chùa Minh Thành	13.966258	108.006356	Ngôi chùa nổi tiếng ở Gia Lai, được xây dựng theo phong cách Phật giáo Nhật Bản, với kiến trúc độc đáo và không gian thanh tịnh.	Bạn có biết? Chùa Minh Thành — Ngôi chùa nổi tiếng ở Gia Lai, được xây dựng theo phong cách Phật giáo Nhật Bản, với kiến trúc độc đáo và không gian thanh tịnh.	4.70	3436	ACTIVE	CUSTOM
230	55	102	Văn Thánh Miếu	10.242755	105.984611	Văn Thánh Miếu là một công trình cổ kính, mang ý nghĩa tôn vinh Nho học và các bậc hiền tài. Khuôn viên miếu có nhiều cây xanh, tạo không gian yên tĩnh, thanh bình.	Bạn có biết? Văn Thánh Miếu là một công trình cổ kính, mang ý nghĩa tôn vinh Nho học và các bậc hiền tài.	4.40	451	ACTIVE	CUSTOM
227	55	102	Chùa Tiên Châu	10.262997	105.969111	Chùa Tiên Châu nằm trên cù lao An Bình, là một ngôi chùa cổ kính mang đậm nét kiến trúc Phật giáo. Ngôi chùa có không gian yên tĩnh, thanh bình, thu hút nhiều Phật tử và du khách đến chiêm bái.	Bạn có biết? Chùa Tiên Châu nằm trên cù lao An Bình, là một ngôi chùa cổ kính mang đậm nét kiến trúc Phật giáo.	4.60	141	ACTIVE	CUSTOM
223	53	102	Đền Hạ (Tuyên Quang)	21.813077	105.215191	Đền Hạ, còn gọi là đền Hiệp Thuận hoặc đền Tam Kỳ, là di tích thờ Mẫu tại khu vực trung tâm Tuyên Quang. Đền có liên hệ lễ hội với đền Thượng và đền Mẫu Ỷ La.	Bạn có biết? Đền Hạ (Tuyên Quang) — Đền Hạ, còn gọi là đền Hiệp Thuận hoặc đền Tam Kỳ, là di tích thờ Mẫu tại khu vực trung tâm Tuyên Quang.	4.60	241	ACTIVE	CUSTOM
220	52	102	Chùa Vàm Rây	9.655278	106.274725	Là ngôi chùa Khmer lớn nhất Việt Nam, chùa Vàm Rây gây ấn tượng bởi kiến trúc vàng rực rỡ, tượng Phật nằm khổng lồ và không gian tĩnh lặng.	Bạn có biết? Chùa Vàm Rây là ngôi chùa Khmer lớn nhất Việt Nam, chùa Vàm Rây gây ấn tượng bởi kiến trúc vàng rực rỡ, tượng Phật nằm khổng lồ và không gian tĩnh lặng.	4.60	557	ACTIVE	CUSTOM
217	52	102	Chùa Hang	9.887058	106.345022	Chùa Hang nổi tiếng không chỉ vì kiến trúc độc đáo mà còn vì những tác phẩm điêu khắc gỗ tinh xảo của các nghệ nhân Khmer. Ngôi chùa này là điểm dừng chân lý tưởng cho những ai muốn khám phá nghệ thuật và văn hóa Khmer.	Bạn có biết? Chùa Hang nổi tiếng không chỉ vì kiến trúc độc đáo mà còn vì những tác phẩm điêu khắc gỗ tinh xảo của các nghệ nhân Khmer.	4.50	846	ACTIVE	CUSTOM
204	14	102	Chùa Ông	10.034178	105.788458	Ngôi chùa cổ mang kiến trúc Trung Hoa, nổi tiếng linh thiêng và là nơi thờ Quan Công. Đây là địa điểm tham quan văn hóa độc đáo và check-in đẹp.	Bạn có biết? Chùa Ông — Ngôi chùa cổ mang kiến trúc Trung Hoa, nổi tiếng linh thiêng và là nơi thờ Quan Công.	4.60	1116	ACTIVE	CUSTOM
200	13	102	Đền Thờ Bác Hồ	9.183985	105.134884	Địa điểm tâm linh quan trọng, nơi người dân Cà Mau bày tỏ lòng thành kính với Chủ tịch Hồ Chí Minh	Bạn có biết? Đền Thờ Bác Hồ — Địa điểm tâm linh quan trọng, nơi người dân Cà Mau bày tỏ lòng thành kính với Chủ tịch Hồ Chí Minh.	\N	0	ACTIVE	CUSTOM
185	3	102	Chùa Châu Thới	10.914830	106.804556	Chùa Châu Thới là một ngôi chùa nổi tiếng ở Bình Dương, mang đậm nét văn hóa Phật giáo và có khung cảnh thanh tịnh, phù hợp cho các hoạt động tham quan tâm linh.	Bạn có biết? Chùa Châu Thới là một ngôi chùa nổi tiếng ở Bình Dương, mang đậm nét văn hóa Phật giáo và có khung cảnh thanh tịnh, phù hợp cho các hoạt động tham quan tâm linh.	4.60	6251	ACTIVE	CUSTOM
181	3	102	Chùa Tây Tạng	10.977526	106.661757	Chùa Tây Tạng là một ngôi chùa nổi tiếng ở Bình Dương, thu hút du khách bởi kiến trúc độc đáo và không gian thanh tịnh.	Bạn có biết? Chùa Tây Tạng là một ngôi chùa nổi tiếng ở Bình Dương, thu hút du khách bởi kiến trúc độc đáo và không gian thanh tịnh.	4.70	4303	ACTIVE	CUSTOM
174	11	102	Nhà thờ La Mã	10.099430	106.477188	Nhà thờ La Mã tại khu vực Hưng Nhượng, Giồng Trôm, Bến Tre, là địa điểm hành hương Công giáo gắn với Đức Mẹ La Mã Bến Tre.	Bạn có biết? Nhà thờ La Mã tại khu vực Hưng Nhượng, Giồng Trôm, Bến Tre, là địa điểm hành hương Công giáo gắn với Đức Mẹ La Mã Bến Tre.	4.80	1319	ACTIVE	CUSTOM
173	11	102	Chùa Tuyên Linh	10.051152	106.411692	Chùa Tuyên Linh tại khu vực Minh Đức, Mỏ Cày Nam, Bến Tre, là cơ sở Phật giáo gắn với lịch sử địa phương và khu lưu niệm Nguyễn Sinh Sắc.	Bạn có biết? Chùa Tuyên Linh tại khu vực Minh Đức, Mỏ Cày Nam, Bến Tre, là cơ sở Phật giáo gắn với lịch sử địa phương và khu lưu niệm Nguyễn Sinh Sắc.	4.70	49	ACTIVE	CUSTOM
169	10	102	Đền bà Chúa Kho	21.205233	106.085056	Đền Bà Chúa Kho ở Bắc Ninh là nơi thờ bà Chúa Kho, một trong những vị thần được thờ cúng linh thiêng nhất ở Việt Nam	Bạn có biết? Đền Bà Chúa Kho ở Bắc Ninh là nơi thờ bà Chúa Kho, một trong những vị thần được thờ cúng linh thiêng nhất ở Việt Nam.	4.60	5859	ACTIVE	CUSTOM
166	10	102	Chùa Phật Tích	21.106667	106.012500	Đây là một trong những ngôi chùa cổ, gắn liền với lịch sử Phật giáo Việt Nam.	Bạn có biết? Chùa Phật Tích — Đây là một trong những ngôi chùa cổ, gắn liền với lịch sử Phật giáo Việt Nam.	4.60	3026	ACTIVE	CUSTOM
161	7	102	Chùa Quan Đế	9.280318	105.723163	Là một ngôi chùa cổ kính, thờ Quan Công – một vị tướng nổi tiếng trong lịch sử Trung Quốc	Bạn có biết? Chùa Quan Đế là một ngôi chùa cổ kính, thờ Quan Công – một vị tướng nổi tiếng trong lịch sử Trung Quốc.	4.60	130	ACTIVE	CUSTOM
154	8	102	Đền Trung	21.267171	106.484545	Đây là một trong ba ngôi đền chính thờ các vua Hùng và gắn liền với truyền thuyết dựng nước của dân tộc Việt Nam	Bạn có biết? Đền Trung — Đây là một trong ba ngôi đền chính thờ các vua Hùng và gắn liền với truyền thuyết dựng nước của dân tộc Việt Nam.	4.50	10	ACTIVE	CUSTOM
151	8	102	Chùa Bổ Đà	21.242500	106.052222	Là một trong những ngôi chùa cổ nổi tiếng và đặc sắc nhất vùng Kinh Bắc	Bạn có biết? Chùa Bổ Đà là một trong những ngôi chùa cổ nổi tiếng và đặc sắc nhất vùng Kinh Bắc.	4.70	2160	ACTIVE	CUSTOM
145	1	102	Thiền viện Trúc Lâm An Giang	10.253680	105.270967	Là một điểm đến tâm linh và du lịch vô cùng hấp dẫn ở miền Tây	Bạn có biết? Thiền viện Trúc Lâm An Giang là một điểm đến tâm linh và du lịch vô cùng hấp dẫn ở miền Tây.	4.60	1040	ACTIVE	CUSTOM
144	1	102	Miếu Bà Chúa Xứ	10.682254	105.080132	Miếu Bà Chúa Xứ là một trong những địa điểm tâm linh nổi tiếng nhất An Giang	Bạn có biết? Miếu Bà Chúa Xứ là một trong những địa điểm tâm linh nổi tiếng nhất An Giang.	4.70	5790	ACTIVE	CUSTOM
143	1	102	Chùa Lầu	10.608125	104.960173	Là một ngôi chùa đã có lịch sử hơn 130 năm	Bạn có biết? Chùa Lầu là một ngôi chùa đã có lịch sử hơn 130 năm.	4.60	854	ACTIVE	CUSTOM
138	47	102	Nhà thờ Đức Bà Sài Gòn	10.779722	106.699167	Cũng nằm ngay trung tâm quận 1, nhà thờ Đức Bà là địa điểm du lịch thành phố Hồ Chí Minh mà bạn nhất định phải ghé đến. Nhà thờ Đức Bà là một công trình kiến trúc độc đáo mang đậm phong cách Châu Âu, là nơi sinh hoạt và tổ chức các buổi Thánh lễ cho những người theo đạo Công giáo ở Sài Thành.	Bạn có biết? Nhà thờ Đức Bà Sài Gòn nằm ở trung tâm Quận 1 và mang dấu ấn kiến trúc châu Âu.	4.40	29222	ACTIVE	CUSTOM
134	51	102	Chùa Linh Thứu	10.374163	106.302690	Đây là một trong những ngôi chùa có lịch sử lâu đời nhất tại tỉnh Tiền Giang. Đặc biệt hơn, nơi này gắn liền với câu chuyện thoát chết của vua Gia Long được lưu truyền qua nhiều thế hệ. Đây là một trong những ngôi chùa có lịch sử lâu đời nhất tại tỉnh Tiền Giang. Đặc biệt hơn, nơi này gắn liền với câu chuyện thoát chết của vua Gia Long được lưu truyền qua nhiều thế hệ.	Bạn có biết? Chùa Linh Thứu — Đây là một trong những ngôi chùa có lịch sử lâu đời nhất tại tỉnh Tiền Giang.	4.60	206	ACTIVE	CUSTOM
133	51	102	Nhà Thờ Cái Bè	10.337919	106.035271	Sở hữu nét kiến trúc Roman đặc trưng, nhà thờ Cái Bè không những đóng vai trò quan trọng trong đời sống tôn giáo của người dân nơi đây mà còn là địa điểm tham quan hấp dẫn ở Tiền Giang. Điểm nổi bật nhất của nhà thờ Cái Bè là tháp chuông được xem là cao nhất khu vực Đồng bằng Sông Cửu Long. Ngoài ra, chính diện nhà thờ hướng ra ngã ba sông Cái Bè tựa như đang ôm trọn đất trời. Khuôn viên được bố trí nhiều cây xanh, mang đến cảnh quan đẹp mắt	Bạn có biết? Nhà Thờ Cái Bè — Sở hữu nét kiến trúc Roman đặc trưng, nhà thờ Cái Bè không những đóng vai trò quan trọng trong đời sống tôn giáo của người dân nơi đây mà còn là địa điểm tham quan hấp dẫn ở Tiền Giang.	4.60	231	ACTIVE	CUSTOM
127	50	102	Chùa Thiên Mụ	16.453720	107.544581	Ngôi chùa này là điểm tham quan Huế nổi tiếng và không thể thiếu trong mọi hành trình đến khám phá Huế của du khách.Cách trung tâm thành phố Huế khoảng 5km về phía Tây, với một tên gọi khác là chùa Linh Mụ, chùa nằm trên đồi Hà Khê, nhìn xuống dòng Hương thơ mộng. Ngôi chùa mang vẻ đẹp cổ kính lâu đời với những khuông viên rộng rãi,khung cảnh thanh tịnh và đặc biệt hình tháp Phước Duyên 7 tầng tại sân trước của chùa được xem là biểu tượng đặc trưng cho thành phố Huế	Bạn có biết? Tháp Phước Duyên bảy tầng đứng ở sân trước chùa Thiên Mụ.	4.60	20332	ACTIVE	CUSTOM
167	10	102	Chùa Bút Tháp	21.060830	106.022200	Chùa Bút Tháp nằm ở Bắc Ninh, là một ngôi chùa nổi tiếng với kiến trúc độc đáo và tượng Phật Bà Quan Âm.	Bạn có biết? Chùa Bút Tháp nằm ở Bắc Ninh, là một ngôi chùa nổi tiếng với kiến trúc độc đáo và tượng Phật Bà Quan Âm.	4.70	502	ACTIVE	CUSTOM
114	48	102	Nhà thờ Bác Trạch	20.365399	106.477269	Đây chính là một trong những nhà thờ lớn nhất Việt Nam hiện nay với hoa văn trang trí công phu theo lối kiến trúc Roman. Với vẻ đẹp này chắc chắn nhà thờ Bác Trạch là một địa điểm check-in Thái Bình độc đáo dành cho bạn.	Bạn có biết? Nhà thờ Bác Trạch — Đây chính là một trong những nhà thờ lớn nhất Việt Nam hiện nay với hoa văn trang trí công phu theo lối kiến trúc Roman.	4.70	786	ACTIVE	CUSTOM
135	51	102	Nhà thờ chính tòa Mỹ Tho	10.361440	106.364926	Nhà thờ chính tòa của Giáo phận Mỹ Tho, tại số 32 Hùng Vương, khu vực Mỹ Tho, Tiền Giang. Công trình có giá trị kiến trúc và sinh hoạt tôn giáo của cộng đồng Công giáo.	Bạn có biết? Nhà thờ chính tòa Mỹ Tho — Nhà thờ chính tòa của Giáo phận Mỹ Tho, tại số 32 Hùng Vương, khu vực Mỹ Tho, Tiền Giang.	4.70	450	ACTIVE	CUSTOM
111	48	102	Chùa Keo	20.360556	106.297222	Chùa Keo ở Thái Bình được xếp hạng là ngôi chùa cổ đẹp nhất tại Việt Nam. Chùa được xây dựng theo kiến trúc cổ kín từ thời nhà Lê. Chính vì lẽ đó mà chùa Keo được xem là một trong những địa điểm check-in Thái Bình mà bạn nhất định phải đến tham quan.	Bạn có biết? Chùa Keo ở Thái Bình được xếp hạng là ngôi chùa cổ đẹp nhất tại Việt Nam.	4.60	3669	ACTIVE	CUSTOM
107	54	102	Tòa thánh Tây Ninh	11.303746	106.133717	Tòa thánh chính là địa điểm du lịch Tây Ninh dành cho những dân phượt muốn tìm hiểu về nét văn hóa và nét kiến trúc tiêu biểu tại nơi đây, Phong cách thiết kế của công trình vô cùng độc đáo, các chi tiết được trạm trổ hoa văn kỳ công và mang nhiều nét biểu tượng cho văn hóa mảnh đất này.	Bạn có biết? Tòa thánh Tây Ninh — Tòa thánh chính là địa điểm du lịch Tây Ninh dành cho những dân phượt muốn tìm hiểu về nét văn hóa và nét kiến trúc tiêu biểu tại nơi đây, Phong cách thiết kế của công trình vô cùng độc đáo, các chi tiết được trạm trổ hoa văn kỳ công và mang nhiều nét biểu tượng cho văn hóa mảnh đất này.	4.70	5687	ACTIVE	CUSTOM
100	44	102	Chùa Som Rong	9.623636	105.981946	Là một nơi đặc biệt quan trọng không chỉ do tín ngưỡng tôn giáo, mà còn do ngôi chùa này có một thư viện lớn với hơn 1500 đầu sách, phục vụ cho các bạn thiếu nhi, bà con tại nơi này. Ngôi bảo tháp được phủ một màu xám huyền bí thay vì màu vàng thường gặp; Pho tượng phật nằm dài hơn 63 mét, cao hơn mặt đất khoảng 28 mét, cộng thêm chiều cao 22 mét khiến cho pho tượng vô cùng uy nghi; chùa Som Rong thường được nhiều khách du lịch lựa chọn ghé thăm	Bạn có biết? Chùa Som Rong là một nơi đặc biệt quan trọng không chỉ do tín ngưỡng tôn giáo, mà còn do ngôi chùa này có một thư viện lớn với hơn 1500 đầu sách, phục vụ cho các bạn thiếu nhi, bà con tại nơi này.	4.80	878	ACTIVE	CUSTOM
97	44	102	Chùa Dơi	9.578940	105.972204	Kiến trúc của ngôi chùa này được xem là tiêu biểu của Phật giáo Nam tông Khmer, chùa được xây dựng vào thế kỷ 16 nhưng cho đến nay toàn bộ công trình còn giữ được tình trạng tốt và vẫn là nơi sinh hoạt tôn giáo mang ý nghĩa quan trọng của người dân trong vùng. Điều Đặc biệt khi đi đây là du khách sẽ thấy hàng nghìn con dơi khá lớn treo mình trên cây trong khuôn viên chùa giống như chúng đã quen thuộc với nơi này vậy	Bạn có biết? Chùa Dơi — Kiến trúc của ngôi chùa này được xem là tiêu biểu của Phật giáo Nam tông Khmer, chùa được xây dựng vào thế kỷ 16 nhưng cho đến nay toàn bộ công trình còn giữ được tình trạng tốt và vẫn là nơi sinh hoạt tôn giáo mang ý nghĩa quan trọng của người dân trong vùng.	4.20	4042	ACTIVE	CUSTOM
73	39	102	Đền Thánh Mẫu Liễu Hạnh	17.953503	106.469148	Là nơi linh thiêng thờ vị thánh trong tín ngưỡng dân gian Việt Nam.	Bạn có biết? Đền Thánh Mẫu Liễu Hạnh là nơi linh thiêng thờ vị thánh trong tín ngưỡng dân gian Việt Nam.	4.80	119	ACTIVE	CUSTOM
61	37	102	Đền Hùng	21.368224	105.321442	Là khu di tích lịch sử quốc gia đặc biệt, nơi thờ các vua Hùng – những người dựng nước đầu tiên của dân tộc Việt Nam.	Bạn có biết? Đền Hùng là khu di tích lịch sử quốc gia đặc biệt, nơi thờ các vua Hùng – những người dựng nước đầu tiên của dân tộc Việt Nam.	4.70	6210	ACTIVE	CUSTOM
42	33	102	Đền Trần	20.456854	106.167306	Nằm tại phường Lộc Vượng, thành phố Nam Định, đền Trần thờ các vua nhà Trần, gắn liền với lịch sử hào hùng của triều đại này.	Bạn có biết? Đền Trần — Nằm tại phường Lộc Vượng, thành phố Nam Định, đền Trần thờ các vua nhà Trần, gắn liền với lịch sử hào hùng của triều đại này.	4.70	1823	ACTIVE	CUSTOM
41	33	102	Nhà thờ Phú Nhai	20.307651	106.329848	Là một trong những nhà thờ lớn và cổ kính nhất Việt Nam, mang phong cách kiến trúc Gothic đặc sắc.	Bạn có biết? Nhà thờ Phú Nhai là một trong những nhà thờ lớn và cổ kính nhất Việt Nam, mang phong cách kiến trúc Gothic đặc sắc.	4.70	1282	ACTIVE	CUSTOM
38	29	102	Chùa Tôn Thạnh	10.605429	106.646288	Ngôi chùa này là nơi cụ Nguyễn Đình Chiểu từng ở và sáng tác bài văn tế "Văn tế nghĩa sĩ Cần Giuộc".	Bạn có biết? Chùa Tôn Thạnh — Ngôi chùa này là nơi cụ Nguyễn Đình Chiểu từng ở và sáng tác bài văn tế "Văn tế nghĩa sĩ Cần Giuộc".	4.70	729	ACTIVE	CUSTOM
35	30	102	Đền Thượng	22.509608	103.968162	Là nơi thờ Trần Hưng Đạo, vị tướng tài ba trong lịch sử Việt Nam.	Bạn có biết? Đền Thượng là nơi thờ Trần Hưng Đạo, vị tướng tài ba trong lịch sử Việt Nam.	4.70	867	ACTIVE	CUSTOM
30	32	102	Chùa Tiên (Lạng Sơn)	21.836328	106.756965	Chùa Tiên nằm trên núi Đại Tượng, gắn liền với nhiều câu chuyện huyền thoại và mang đậm dấu ấn Phật giáo.	Bạn có biết? Chùa Tiên (Lạng Sơn) — Chùa Tiên nằm trên núi Đại Tượng, gắn liền với nhiều câu chuyện huyền thoại và mang đậm dấu ấn Phật giáo.	4.40	329	ACTIVE	CUSTOM
22	31	102	Thiền viện Trúc Lâm Đà Lạt	11.904158	108.434787	Một thiền viện nổi tiếng tọa lạc gần Hồ Tuyền Lâm, mang kiến trúc thanh tịnh và không gian yên bình.	Bạn có biết? Thiền viện Trúc Lâm Đà Lạt là một thiền viện nổi tiếng tọa lạc gần Hồ Tuyền Lâm, mang kiến trúc thanh tịnh và không gian yên bình.	4.60	3958	ACTIVE	CUSTOM
300	23	103	Cầu Hoàng Văn Thụ	20.867265	106.681336	Cầu Hoàng Văn Thụ (Hải Phòng) là một cây cầu huyết mạch nối liền các khu vực trong thành phố. Cầu nổi bật với kiến trúc hiện đại và là điểm đến tham quan chính của du khách.	Bạn có biết? Cầu Hoàng Văn Thụ (Hải Phòng) là một cây cầu huyết mạch nối liền các khu vực trong thành phố.	4.80	193	ACTIVE	CUSTOM
299	23	103	Hải đăng Hòn Dấu	20.666424	106.816392	Hải đăng Hòn Dấu (Hải Phòng) là ngọn hải đăng cổ kính, nằm trên đảo Hòn Dấu. Đây là một trong những ngọn hải đăng quan trọng ở khu vực vịnh Bắc Bộ.	Bạn có biết? Hải đăng Hòn Dấu (Hải Phòng) là ngọn hải đăng cổ kính, nằm trên đảo Hòn Dấu.	\N	0	ACTIVE	CUSTOM
285	18	103	Cầu Long Biên	21.043333	105.858889	Cầu Long Biên (Hà Nội) là một trong những cây cầu lâu đời nhất, mang đậm giá trị lịch sử. Cầu nối liền hai bờ sông Hồng và là biểu tượng của thủ đô.	Bạn có biết? Cầu Long Biên (Hà Nội) là một trong những cây cầu lâu đời nhất, mang đậm giá trị lịch sử.	4.60	114	ACTIVE	CUSTOM
271	16	103	Cột cờ Lũng Cú	23.367944	105.315633	Cột cờ Lũng Cú, nằm ở cực Bắc Việt Nam (Hà Giang), là biểu tượng thiêng liêng của chủ quyền quốc gia. Du khách chinh phục gần 800 bậc thang để chiêm ngưỡng lá cờ đỏ sao vàng tung bay trên đỉnh núi hùng vĩ.	Bạn có biết? Cột cờ Lũng Cú, nằm ở cực Bắc Việt Nam (Hà Giang), là biểu tượng thiêng liêng của chủ quyền quốc gia.	4.60	9276	ACTIVE	CUSTOM
269	15	103	Quảng trường Đại Đoàn Kết	13.983124	108.005777	Quảng trường lớn nhất Tây Nguyên, nằm tại Pleiku, Gia Lai. Đây là nơi tổ chức các sự kiện lớn và cũng là biểu tượng của đoàn kết dân tộc.	Bạn có biết? Quảng trường Đại Đoàn Kết — Quảng trường lớn nhất Tây Nguyên, nằm tại Pleiku, Gia Lai.	4.80	749	ACTIVE	CUSTOM
215	59	103	Cầu Tình Yêu	16.063280	108.229604	Cây cầu nhỏ bên sông Hàn với thiết kế lãng mạn, nơi các cặp đôi thường treo móc khóa tình yêu. Từ đây, bạn có thể ngắm nhìn tượng Cá Chép Hóa Rồng.	Bạn có biết? Cầu Tình Yêu — Cây cầu nhỏ bên sông Hàn với thiết kế lãng mạn, nơi các cặp đôi thường treo móc khóa tình yêu.	4.60	772	ACTIVE	CUSTOM
212	59	103	Cầu Rồng	16.061111	108.226667	Biểu tượng của thành phố Đà Nẵng, cây cầu có thiết kế hình rồng phun lửa và nước vào cuối tuần. Đây là điểm check-in và ngắm cảnh sông Hàn tuyệt đẹp.	Bạn có biết? Cầu Rồng — Biểu tượng của thành phố Đà Nẵng, cây cầu có thiết kế hình rồng phun lửa và nước vào cuối tuần.	4.50	9758	ACTIVE	CUSTOM
202	14	103	Cầu Cần Thơ	10.031653	105.808639	Cây cầu dây văng lớn nhất Đông Nam Á, nối liền Cần Thơ và Vĩnh Long. Đây là điểm check-in nổi tiếng và mang tính biểu tượng của vùng đất Tây Nam Bộ.	Bạn có biết? Cầu Cần Thơ — Cây cầu dây văng lớn nhất Đông Nam Á, nối liền Cần Thơ và Vĩnh Long.	4.80	246	ACTIVE	CUSTOM
165	7	103	Đồng hồ Thái Dương (Bạc Liêu)	9.285647	105.722716	Đồng hồ đá do nhà bác vật Lưu Văn Lang chế tạo, nằm tại khu vực trung tâm Bạc Liêu. Công trình xác định giờ dựa trên ánh sáng mặt trời và được bảo tồn như một di tích.	Bạn có biết? Đồng hồ Thái Dương (Bạc Liêu) — Đồng hồ đá do nhà bác vật Lưu Văn Lang chế tạo, nằm tại khu vực trung tâm Bạc Liêu.	4.70	23	ACTIVE	CUSTOM
132	51	103	Cầu Mỹ Thuận	10.277556	105.909972	Cầu dây văng Mỹ Thuận bắc qua sông Tiền, nối khu vực Tiền Giang và Vĩnh Long, khánh thành năm 2000.	Bạn có biết? Cầu Mỹ Thuận — Cầu dây văng Mỹ Thuận bắc qua sông Tiền, nối khu vực Tiền Giang và Vĩnh Long, khánh thành năm 2000.	4.90	54	ACTIVE	CUSTOM
130	50	103	Cầu Tràng Tiền	16.468889	107.588611	Một trong những chiếc cầu lâu đời tại Huế và trở thành biểu tượng du lịch của Huế không ai khác chính là cầu Tràng Tiền bắc qua sông Hương thơ mộng cũng là một “nàng thơ” kiều diễm của không ít văn nghệ sĩ trong nước.	Bạn có biết? Cầu Tràng Tiền là một trong những chiếc cầu lâu đời tại Huế và trở thành biểu tượng du lịch của Huế không ai khác chính là cầu Tràng Tiền bắc qua sông Hương thơ mộng cũng là một “nàng thơ” kiều diễm của không ít văn nghệ sĩ trong nước.	4.70	744	ACTIVE	CUSTOM
192	5	103	Hải đăng Kê Gà	10.695960	107.991340	Một trong những ngọn hải đăng lâu đời nhất Việt Nam, nằm trên một hòn đảo nhỏ giữa biển	Bạn có biết? Hải đăng Kê Gà là một trong những ngọn hải đăng lâu đời nhất Việt Nam, nằm trên một hòn đảo nhỏ giữa biển.	4.50	2386	ACTIVE	CUSTOM
68	38	103	Hải đăng Đại Lãnh	12.896568	109.457139	Là điểm cực Đông của Việt Nam, nơi đón ánh bình minh đầu tiên trên đất liền	Bạn có biết? Hải đăng Đại Lãnh là điểm cực Đông của Việt Nam, nơi đón ánh bình minh đầu tiên trên đất liền.	4.60	2406	ACTIVE	CUSTOM
28	32	103	Cửa khẩu Hữu Nghị	21.971761	106.711039	Đây là cửa khẩu quốc tế lớn giữa Việt Nam và Trung Quốc, nằm trên tuyến quốc lộ 1A.	Bạn có biết? Cửa khẩu Hữu Nghị — Đây là cửa khẩu quốc tế lớn giữa Việt Nam và Trung Quốc, nằm trên tuyến quốc lộ 1A.	4.50	769	ACTIVE	CUSTOM
12	27	103	Cầu Treo Kon Klor	14.346989	108.035526	Cây cầu treo lớn và đẹp nhất ở Kon Tum, bắc qua sông Đăk Bla, được bao quanh bởi những cánh đồng lúa và núi rừng xanh mát	Bạn có biết? Cầu Treo Kon Klor — Cây cầu treo lớn và đẹp nhất ở Kon Tum, bắc qua sông Đăk Bla, được bao quanh bởi những cánh đồng lúa và núi rừng xanh mát.	4.70	32	ACTIVE	CUSTOM
311	21	104	Phố Hiến	20.659734	106.063532	Phố Hiến (Hưng Yên) là một khu phố cổ nổi tiếng với những công trình kiến trúc mang đậm dấu ấn lịch sử. Đây là một điểm đến hấp dẫn cho du khách muốn tìm hiểu về lịch sử, văn hóa và thưởng thức ẩm thực địa phương.	Bạn có biết? Phố Hiến (Hưng Yên) là một khu phố cổ nổi tiếng với những công trình kiến trúc mang đậm dấu ấn lịch sử.	\N	0	ACTIVE	CUSTOM
307	20	104	Mai Châu	20.664779	105.083326	Mai Châu (Hòa Bình) là một thung lũng xinh đẹp, nổi tiếng với những ngôi nhà sàn và cảnh sắc thiên nhiên hoang sơ. Mai Châu thu hút du khách bởi không khí trong lành, cộng với nền văn hóa đặc sắc của đồng bào dân tộc Thái.	Bạn có biết? Mai Châu (Hòa Bình) là một thung lũng xinh đẹp, nổi tiếng với những ngôi nhà sàn và cảnh sắc thiên nhiên hoang sơ.	\N	0	ACTIVE	CUSTOM
293	22	104	Làng rối nước Thanh Hải	20.887744	106.401585	Làng rối nước Thanh Hải (Hải Dương) là nơi bảo tồn và phát triển nghệ thuật rối nước truyền thống. Du khách có thể thưởng thức những màn rối đặc sắc trong không gian văn hóa dân gian đậm đà.	Bạn có biết? Làng rối nước Thanh Hải (Hải Dương) là nơi bảo tồn và phát triển nghệ thuật rối nước truyền thống.	4.60	142	ACTIVE	CUSTOM
280	17	104	Làng Vũ Đại	20.482341	106.166573	Làng Vũ Đại (Hà Nam) nổi tiếng nhờ sự gắn kết với tác phẩm "Chí Phèo" của nhà văn Nam Cao. Nơi đây có những ngôi nhà cổ, là điểm du lịch văn hóa, nơi lưu giữ nét đẹp văn hóa truyền thống.	Bạn có biết? Làng Vũ Đại (Hà Nam) nổi tiếng nhờ sự gắn kết với tác phẩm "Chí Phèo" của nhà văn Nam Cao.	\N	0	ACTIVE	CUSTOM
273	16	104	Phố cổ Đồng Văn	23.281778	105.359894	Phố cổ Đồng Văn (Hà Giang) lưu giữ nét kiến trúc cổ kính với những ngôi nhà tường đất, mái ngói âm dương. Phiên chợ cuối tuần nhộn nhịp là điểm nhấn văn hóa hấp dẫn.	Bạn có biết? Phố cổ Đồng Văn (Hà Giang) lưu giữ nét kiến trúc cổ kính với những ngôi nhà tường đất, mái ngói âm dương.	4.60	3175	ACTIVE	CUSTOM
241	60	104	Buôn Đôn	12.886402	107.783269	Buôn Đôn, nằm tại tỉnh Đắk Lắk, Việt Nam, là một vùng đất nổi tiếng với văn hóa đặc sắc và khung cảnh thiên nhiên hùng vĩ. Đây là vùng đất của người dân tộc Ê-đê và Mnông, nơi bạn có thể khám phá những nét văn hóa độc đáo và các hoạt động thú vị	Bạn có biết? Buôn Đôn, nằm tại tỉnh Đắk Lắk, Việt Nam, là một vùng đất nổi tiếng với văn hóa đặc sắc và khung cảnh thiên nhiên hùng vĩ.	\N	0	ACTIVE	CUSTOM
239	57	104	Bản Lìm Mông	21.778884	104.269393	Bản Lìm Mông là một ngôi làng nhỏ nằm giữa những ngọn núi và ruộng bậc thang tuyệt đẹp. Đây là nơi du khách có thể trải nghiệm cuộc sống của người H'Mông, tận hưởng sự yên bình và không khí trong lành.	Bạn có biết? Bản Lìm Mông là một ngôi làng nhỏ nằm giữa những ngọn núi và ruộng bậc thang tuyệt đẹp.	\N	0	ACTIVE	CUSTOM
194	5	104	Làng Chài Mũi Né	10.941171	108.278776	Khu vực làng chài truyền thống ven biển, nơi bạn có thể ngắm cảnh thuyền ghe tấp nập và trải nghiệm mua hải sản tươi sống trực tiếp từ ngư dân.	Bạn có biết? Làng Chài Mũi Né — Khu vực làng chài truyền thống ven biển, nơi bạn có thể ngắm cảnh thuyền ghe tấp nập và trải nghiệm mua hải sản tươi sống trực tiếp từ ngư dân.	4.00	3494	ACTIVE	CUSTOM
159	9	104	Bản Pác Ngòi	22.386412	105.639878	Đây là một bản làng của người Tày, nổi bật với nét văn hóa truyền thống	Bạn có biết? Bản Pác Ngòi — Đây là một bản làng của người Tày, nổi bật với nét văn hóa truyền thống.	4.00	2	ACTIVE	CUSTOM
89	42	104	Làng chài Cửa Vạn	20.807242	107.117945	Không chỉ được tận hưởng không gian yên tĩnh, thanh bình nơi đây, du khách còn có thêm những hiểu biết về đời sống văn hóa, thường nhật của các ngư dân. Ngư dân ở đây hết sức thân thiện và mến khách. Hành khách có thể chèo thuyền thúng, cắt cá, câu mực… để hiểu hơn cuộc sống dân chài.	Bạn có biết? Làng chài Cửa Vạn — Không chỉ được tận hưởng không gian yên tĩnh, thanh bình nơi đây, du khách còn có thêm những hiểu biết về đời sống văn hóa, thường nhật của các ngư dân.	4.80	9	ACTIVE	CUSTOM
76	40	104	Phố cổ Hội An	15.883333	108.333333	Những ngôi nhà cổ kính và có kiến trúc độc đáo như: những ngôi nhà lợp ngối đá, nhưng bức tường vàng mang lại vẻ đẹp yên bình. Đặt biệt bạn có thể thư dãn dạo bước với những đường nhỏ hẹp quanh sông Hoài	Bạn có biết? Phố cổ Hội An nổi bật với những bức tường vàng và các ngôi nhà cổ bên sông Hoài.	4.80	802	ACTIVE	CUSTOM
62	37	104	Làng cổ Hùng Lô	21.372406	105.385909	Nổi tiếng với các ngôi nhà cổ hàng trăm năm tuổi, cùng các nghề truyền thống như làm bánh chưng, bánh giầy.	Bạn có biết? Làng cổ Hùng Lô — Nổi tiếng với các ngôi nhà cổ hàng trăm năm tuổi, cùng các nghề truyền thống như làm bánh chưng, bánh giầy.	\N	0	ACTIVE	CUSTOM
313	21	104	Làng nghề đúc đồng Lộng Thượng	20.992358	106.062886	Làng nghề đúc đồng Lộng Thượng tại khu vực Đại Đồng, Văn Lâm, Hưng Yên, nổi tiếng với đồ thờ cúng và các sản phẩm đồng thủ công.	Bạn có biết? Làng nghề đúc đồng Lộng Thượng tại khu vực Đại Đồng, Văn Lâm, Hưng Yên, nổi tiếng với đồ thờ cúng và các sản phẩm đồng thủ công.	\N	0	ACTIVE	CUSTOM
233	56	104	Làng gốm Hương Canh	21.275271	105.648926	Làng gốm Hương Canh là một làng nghề truyền thống nổi tiếng với các sản phẩm gốm thủ công tinh xảo, đặc biệt là các sản phẩm gốm gia dụng và nghệ thuật. Du khách có thể tham quan và trải nghiệm làm gốm tại đây.	Bạn có biết? Làng gốm Hương Canh là một làng nghề truyền thống nổi tiếng với các sản phẩm gốm thủ công tinh xảo, đặc biệt là các sản phẩm gốm gia dụng và nghệ thuật.	\N	0	ACTIVE	CUSTOM
210	12	104	Làng rèn Phúc Sen	22.683432	106.408745	Làng nghề truyền thống nổi tiếng với sản phẩm dao kéo chất lượng cao. Du khách có thể tham quan quy trình rèn và mua sắm các sản phẩm thủ công.	Bạn có biết? Làng rèn Phúc Sen — Làng nghề truyền thống nổi tiếng với sản phẩm dao kéo chất lượng cao.	4.20	118	ACTIVE	CUSTOM
168	10	104	Làng tranh Đông Hồ	21.068806	106.075408	Làng tranh Đông Hồ ở Bắc Ninh là nơi sản xuất tranh dân gian nổi tiếng, với những bức tranh phản ánh đời sống nông thôn Việt Nam.	Bạn có biết? Làng tranh Đông Hồ ở Bắc Ninh là nơi sản xuất tranh dân gian nổi tiếng, với những bức tranh phản ánh đời sống nông thôn Việt Nam.	\N	0	ACTIVE	CUSTOM
115	48	104	Làng dệt chiếu Hới	20.646111	106.143333	Nổi tiếng với những làng nghề truyền thống gần hàng trăm năm tuổi. bạn còn được hiểu thêm về cuộc sống giản dị của người dân trong làng. Và bạn sẽ được người dân giới thiệu về quy trình để tạo ra được một một chiếc chiếu truyền thống, từ thu hoạch chế biến cói cho tới khâu thành phẩm. Những trải nghiệm này sẽ là một kỷ niệm khó quên cho chuyến đi của bạn.	Bạn có biết? Làng dệt chiếu Hới — Nổi tiếng với những làng nghề truyền thống gần hàng trăm năm tuổi.	\N	0	ACTIVE	CUSTOM
302	24	105	Chợ nổi Ngã Bảy	9.814644	105.824952	Chợ nổi Ngã Bảy (Hậu Giang) là một trong những chợ nổi nổi tiếng ở miền Tây. Du khách có thể trải nghiệm việc mua bán trên sông và khám phá nền văn hóa chợ nổi đặc sắc.	Bạn có biết? Chợ nổi Ngã Bảy (Hậu Giang) là một trong những chợ nổi nổi tiếng ở miền Tây.	4.30	137	ACTIVE	CUSTOM
295	22	105	Chợ Đông Ngô Quyền	20.933590	106.306108	Chợ Đông Ngô Quyền là chợ tại khu vực thành phố Hải Dương theo địa giới cũ, phục vụ giao thương và nhu cầu mua sắm của người dân địa phương.	Bạn có biết? Chợ Đông Ngô Quyền là điểm mua sắm và giao thương quen thuộc của người dân địa phương.	3.00	1	ACTIVE	CUSTOM
265	63	105	Chợ Cao Lãnh	10.454663	105.637197	Một khu chợ sầm uất tại thành phố Cao Lãnh, nơi du khách có thể khám phá văn hóa ẩm thực và đời sống người dân địa phương.	Bạn có biết? Chợ Cao Lãnh là một khu chợ sầm uất tại thành phố Cao Lãnh, nơi du khách có thể khám phá văn hóa ẩm thực và đời sống người dân địa phương.	4.30	2211	ACTIVE	CUSTOM
201	14	105	Chợ Nổi Cái Răng	10.005000	105.746000	Chợ nổi lớn và nổi tiếng nhất ở miền Tây Nam Bộ, nơi buôn bán nhộn nhịp trên sông. Du khách có thể trải nghiệm văn hóa sông nước, mua sắm đặc sản và thưởng thức ẩm thực trên thuyền.	Bạn có biết? Ở chợ nổi Cái Răng, hoạt động mua bán diễn ra ngay trên thuyền.	4.10	8158	ACTIVE	CUSTOM
5	25	109	Suối Ba Hồ	12.391428	109.138913	Gồm ba hồ nước trong xanh nằm trên núi, thích hợp cho trekking và tắm suối	Bạn có biết? Suối Ba Hồ có ba hồ nước trong xanh nằm trên núi.	4.50	279	ACTIVE	CUSTOM
170	18	105	Chợ Ninh Hiệp	21.078611	105.947500	Chợ Ninh Hiệp là một trong những khu chợ nổi tiếng nhất miền Bắc, chuyên buôn bán các mặt hàng vải vóc, quần áo.	Bạn có biết? Chợ Ninh Hiệp là một trong những khu chợ nổi tiếng nhất miền Bắc, chuyên buôn bán các mặt hàng vải vóc, quần áo.	4.50	1388	ACTIVE	CUSTOM
136	47	105	Chợ Bến Thành	10.772521	106.698019	Đây là một trong những địa điểm du lịch Thành phố Hồ Chí Minh nổi tiếng nhất nằm ngay trung tâm thành phố và luôn tấp nập người ra vào mua bán, du khách check in mỗi ngày. Chợ Bến Thành buôn bán tất tần tật những mặt hàng từ thực phẩm, vải vóc, quần áo, đồ gia dụng, đặc sản Sài Gòn,… và quan trọng là khu ẩm thực cực kỳ hấp dẫn ở chợ và những góc check in ấn tượng sẽ làm bạn luôn muốn lưu lại nơi đây thật lâu.	Bạn có biết? Chợ Bến Thành — Đây là một trong những địa điểm du lịch Thành phố Hồ Chí Minh nổi tiếng nhất nằm ngay trung tâm thành phố và luôn tấp nập người ra vào mua bán, du khách check in mỗi ngày.	4.00	87779	ACTIVE	CUSTOM
96	44	105	Chợ nổi Ngã Năm	9.566397	105.597209	bày bán đủ các loại mặt hàng từ thực phẩm cho đến nhu yếu phầm. Đặc biệt hơn hết, không thể không nhắc đến các ghe/ xuồng đầy ắp các loại trái cây nhiệt đới đặc trưng tùy mùa.	Bạn có biết? Chợ nổi Ngã Năm — bày bán đủ các loại mặt hàng từ thực phẩm cho đến nhu yếu phầm.	4.40	78	ACTIVE	CUSTOM
44	33	105	Chợ Viềng	20.336947	106.180757	Là phiên chợ đặc biệt chỉ diễn ra một lần trong năm vào đêm mùng 7, rạng sáng mùng 8 tháng Giêng âm lịch.	Bạn có biết? Chợ Viềng là phiên chợ đặc biệt chỉ diễn ra một lần trong năm vào đêm mùng 7, rạng sáng mùng 8 tháng Giêng âm lịch.	4.50	1043	ACTIVE	CUSTOM
40	29	105	Chợ đêm Tân An	10.531375	106.416600	Là nơi lý tưởng để thưởng thức ẩm thực địa phương và mua sắm các đặc sản Long An	Bạn có biết? Chợ đêm Tân An là nơi lý tưởng để thưởng thức ẩm thực địa phương và mua sắm các đặc sản Long An.	4.40	108	ACTIVE	CUSTOM
33	30	105	Chợ phiên Bắc Hà	22.535599	104.291803	Chợ phiên Bắc Hà nổi tiếng với các mặt hàng thổ cẩm, nông sản và các món ăn đặc trưng của người dân tộc H’Mông, Dao.	Bạn có biết? Chợ phiên Bắc Hà nổi tiếng với các mặt hàng thổ cẩm, nông sản và các món ăn đặc trưng của người dân tộc H’Mông, Dao.	4.80	44	ACTIVE	CUSTOM
27	32	105	Chợ Đông Kinh	21.851345	106.761271	Là trung tâm thương mại lớn nhất ở Lạng Sơn, nổi tiếng với các mặt hàng phong phú	Bạn có biết? Chợ Đông Kinh là trung tâm thương mại lớn nhất ở Lạng Sơn, nổi tiếng với các mặt hàng phong phú.	3.90	4070	ACTIVE	CUSTOM
23	31	105	Chợ Đêm Đà Lạt	11.941496	108.437272	Chợ đêm Đà Lạt là nơi bạn có thể thưởng thức đặc sản như bánh tráng nướng, sữa đậu nành và mua quà lưu niệm.	Bạn có biết? Chợ đêm Đà Lạt là nơi bạn có thể thưởng thức đặc sản như bánh tráng nướng, sữa đậu nành và mua quà lưu niệm.	4.20	19220	ACTIVE	CUSTOM
20	28	105	Chợ phiên Sìn Hồ	22.359652	103.249134	Nơi diễn ra các hoạt động mua bán nhộn nhịp của các dân tộc Thái, H’Mông, Dao, Lự...	Bạn có biết? Chợ phiên Sìn Hồ là nơi diễn ra các hoạt động mua bán nhộn nhịp của các dân tộc Thái, H’Mông, Dao, Lự.	4.40	42	ACTIVE	CUSTOM
303	24	106	Công viên giải trí Ti & Ni (Kittyd & Minnied)	9.958681	105.734812	Công viên giải trí trong khuôn viên Đại học Võ Trường Toản, khu vực Tân Phú Thạnh, Châu Thành A, Hậu Giang, có các khu vui chơi và công viên nước.	Bạn có biết? Công viên giải trí Ti & Ni (Kittyd & Minnied) — Công viên giải trí trong khuôn viên Đại học Võ Trường Toản, khu vực Tân Phú Thạnh, Châu Thành A, Hậu Giang, có các khu vui chơi và công viên nước.	3.30	117	ACTIVE	CUSTOM
257	62	106	Khu du lịch Bửu Long	10.962984	106.793691	Được ví như “Vịnh Hạ Long thu nhỏ” ở Đồng Nai, Bửu Long nổi bật với hồ nước trong xanh, núi đá và cảnh quan thơ mộng. Đây là nơi lý tưởng cho các hoạt động dã ngoại, cắm trại và chụp ảnh.	Bạn có biết? Khu du lịch Bửu Long được ví như “Vịnh Hạ Long thu nhỏ” ở Đồng Nai, Bửu Long nổi bật với hồ nước trong xanh, núi đá và cảnh quan thơ mộng.	4.20	11157	ACTIVE	CUSTOM
245	60	106	Khu du lịch Ko Tam	12.692419	108.115325	Khu du lịch Ko Tam, tọa lạc tại Buôn Ma Thuột, Đắk Lắk, là một điểm đến sinh thái và văn hóa nổi bật của Tây Nguyên. Với diện tích khoảng 13 hecta, Ko Tam mang đến cho du khách một không gian thiên nhiên hoang sơ và yên bình, cùng với những nét văn hóa đặc sắc của người dân tộc Ê-đê	Bạn có biết? Khu du lịch Ko Tam, tọa lạc tại Buôn Ma Thuột, Đắk Lắk, là một điểm đến sinh thái và văn hóa nổi bật của Tây Nguyên.	4.40	5121	ACTIVE	CUSTOM
235	56	106	Khu du lịch sinh thái Đầm Vạc	21.297778	105.598333	Đầm Vạc là một trong những đầm tự nhiên lớn nhất tại Vĩnh Phúc, có hệ sinh thái đa dạng. Du khách có thể tham gia các hoạt động như chèo thuyền, ngắm cảnh, câu cá, hoặc nghỉ ngơi tại các resort ven đầm.	Bạn có biết? Khu du lịch sinh thái Đầm Vạc — Đầm Vạc là một trong những đầm tự nhiên lớn nhất tại Vĩnh Phúc, có hệ sinh thái đa dạng.	4.30	172	ACTIVE	CUSTOM
234	56	106	Khu du lịch FLC Vĩnh Thịnh	21.181559	105.489533	FLC Vĩnh Thịnh là khu nghỉ dưỡng cao cấp với không gian sang trọng, tiện nghi. Nơi đây có các dịch vụ đa dạng như bể bơi, spa, sân golf, và nhà hàng, phù hợp cho những chuyến nghỉ dưỡng cùng gia đình.	Bạn có biết? Khu du lịch FLC Vĩnh Thịnh — FLC Vĩnh Thịnh là khu nghỉ dưỡng cao cấp với không gian sang trọng, tiện nghi.	4.10	1286	ACTIVE	CUSTOM
231	56	106	Tam Đảo	21.455998	105.644466	Tam Đảo là khu nghỉ dưỡng nổi tiếng với khí hậu mát mẻ quanh năm, tọa lạc trên dãy núi Tam Đảo. Nơi đây có nhiều điểm tham quan như Tháp Truyền Hình, Nhà Thờ Đá, và chợ đêm Tam Đảo. Du khách có thể tận hưởng cảnh đẹp núi non và ẩm thực địa phương.	Bạn có biết? Tam Đảo là khu nghỉ dưỡng nổi tiếng với khí hậu mát mẻ quanh năm, tọa lạc trên dãy núi Tam Đảo.	\N	0	ACTIVE	CUSTOM
229	55	106	Khu du lịch Vinh Sang	10.270408	105.953657	Khu du lịch Vinh Sang nằm trên cù lao An Bình, là nơi lý tưởng để trải nghiệm các trò chơi dân gian, tham gia câu cá sấu, chèo xuồng, hoặc khám phá các vườn trái cây.	Bạn có biết? Khu du lịch Vinh Sang nằm trên cù lao An Bình, là nơi lý tưởng để trải nghiệm các trò chơi dân gian, tham gia câu cá sấu, chèo xuồng, hoặc khám phá các vườn trái cây.	4.30	2211	ACTIVE	CUSTOM
211	59	106	Bà Nà Hills	15.995556	107.994167	Khu nghỉ dưỡng trên đỉnh núi với khí hậu mát mẻ, nổi tiếng với cây Cầu Vàng độc đáo, làng Pháp cổ điển và khu vui chơi trong nhà Fantasy Park.	Bạn có biết? Bà Nà Hills có Cầu Vàng và khu vui chơi trong nhà Fantasy Park.	4.40	99229	ACTIVE	CUSTOM
203	14	106	Làng Du Lịch Sinh Thái Ông Đề	9.992319	105.707532	Điểm đến hấp dẫn với cảnh quan xanh mát, các trò chơi dân gian, chèo thuyền, và thưởng thức các món ăn đặc sản miền Tây.	Bạn có biết? Làng Du Lịch Sinh Thái Ông Đề — Điểm đến hấp dẫn với cảnh quan xanh mát, các trò chơi dân gian, chèo thuyền, và thưởng thức các món ăn đặc sản miền Tây.	3.90	1783	ACTIVE	CUSTOM
184	3	106	Công viên Thành Phố mới Bình Dương	11.052409	106.678694	Công viên Thành Phố mới Bình Dương là một không gian xanh, là nơi lý tưởng cho các hoạt động thể thao, dạo bộ và thư giãn.	Bạn có biết? Công viên Thành Phố mới Bình Dương là một không gian xanh, là nơi lý tưởng cho các hoạt động thể thao, dạo bộ và thư giãn.	4.60	75	ACTIVE	CUSTOM
182	3	106	Khu du lịch Đại Nam	11.043333	106.632500	Khu du lịch Đại Nam là một khu phức hợp bao gồm công viên, khu vui chơi, và các điểm tham quan khác, nổi bật tại Bình Dương, thu hút nhiều du khách đến tham quan và giải trí.	Bạn có biết? Khu du lịch Đại Nam là một khu phức hợp bao gồm công viên, khu vui chơi, và các điểm tham quan khác, nổi bật tại Bình Dương, thu hút nhiều du khách đến tham quan và giải trí.	4.40	17840	ACTIVE	CUSTOM
175	11	106	Khu du lịch sinh thái vườn Ba Ngói	10.258321	106.077304	Là khu du lịch sinh thái nổi bật ở Bến Tre, thu hút du khách với không gian xanh mát, thiên nhiên hoang sơ và các hoạt động vui chơi giải trí hấp dẫn.	Bạn có biết? Khu du lịch sinh thái vườn Ba Ngói là khu du lịch sinh thái nổi bật ở Bến Tre, thu hút du khách với không gian xanh mát, thiên nhiên hoang sơ và các hoạt động vui chơi giải trí hấp dẫn.	\N	0	ACTIVE	CUSTOM
171	11	106	khu du lịch Cồn Phụng	10.333306	106.356500	Cồn Phụng nằm ở Bến Tre, nổi bật với phong cảnh sông nước miệt vườn, vườn dừa và các hoạt động du lịch sinh thái.	Bạn có biết? khu du lịch Cồn Phụng — Cồn Phụng nằm ở Bến Tre, nổi bật với phong cảnh sông nước miệt vườn, vườn dừa và các hoạt động du lịch sinh thái.	4.20	642	ACTIVE	CUSTOM
88	42	106	Sun World Hạ Long	20.953975	107.050122	Du lịch Hạ Long thì chắc chắn không nên bỏ qua Sun World Halong Park - khu phức hợp giải trí nổi tiếng dành cho mọi lứa tuổi. Nếu bạn đã từng đến VinWonders thì công viên Hạ Long cũng là một thiên đường tương tự như vậy, nhưng sẽ náo nhiệt hơn nhiều nhé.	Bạn có biết? Sun World Hạ Long — Du lịch Hạ Long thì chắc chắn không nên bỏ qua Sun World Halong Park - khu phức hợp giải trí nổi tiếng dành cho mọi lứa tuổi.	4.40	3995	ACTIVE	CUSTOM
63	37	106	Đảo Ngọc Xanh	21.165961	105.284595	Là khu du lịch sinh thái kết hợp nghỉ dưỡng, với cảnh quan thiên nhiên trong lành, công viên nước, trò chơi cảm giác mạnh và khu suối nước nóng	Bạn có biết? Đảo Ngọc Xanh là khu du lịch sinh thái kết hợp nghỉ dưỡng, với cảnh quan thiên nhiên trong lành, công viên nước, trò chơi cảm giác mạnh và khu suối nước nóng.	3.90	267	ACTIVE	CUSTOM
4	25	106	VinWonders Nha Trang	12.217396	109.241218	Công viên giải trí trên đảo Hòn Tre, nổi bật với nhiều trò chơi và hoạt động hấp dẫn.	Bạn có biết? VinWonders Nha Trang nằm trên đảo Hòn Tre.	4.70	20880	ACTIVE	CUSTOM
270	15	107	Đồi chè Bàu Cạn	13.840386	107.935231	Đồi chè Bàu Cạn ở Gia Lai là một trong những đồi chè lâu đời nhất Việt Nam, nổi bật với những luống chè xanh mướt trải dài, mang vẻ đẹp yên bình. Đây là điểm đến lý tưởng để thư giãn và chụp ảnh giữa không gian thiên nhiên tươi mát.	Bạn có biết? Đồi chè Bàu Cạn ở Gia Lai là một trong những đồi chè lâu đời nhất Việt Nam, nổi bật với những luống chè xanh mướt trải dài, mang vẻ đẹp yên bình.	\N	0	ACTIVE	CUSTOM
254	58	107	Cánh đồng Mường Thanh	21.365967	103.008684	Cánh đồng lúa lớn nhất vùng Tây Bắc, nổi tiếng với vẻ đẹp thơ mộng và vai trò quan trọng trong lịch sử chiến dịch Điện Biên Phủ. Cảnh quan ở đây thay đổi theo mùa vụ, thu hút nhiều du khách đến chiêm ngưỡng.	Bạn có biết? Cánh đồng Mường Thanh — Cánh đồng lúa lớn nhất vùng Tây Bắc, nổi tiếng với vẻ đẹp thơ mộng và vai trò quan trọng trong lịch sử chiến dịch Điện Biên Phủ.	4.70	104	ACTIVE	CUSTOM
238	57	107	Suối Giàng	21.616150	104.593051	Suối Giàng nổi tiếng với những cây chè Shan tuyết cổ thụ hàng trăm năm tuổi và khí hậu mát mẻ quanh năm. Đây là nơi lý tưởng để thưởng trà, tận hưởng không khí trong lành và tìm hiểu văn hóa của đồng bào người Mông.	Bạn có biết? Suối Giàng nổi tiếng với những cây chè Shan tuyết cổ thụ hàng trăm năm tuổi và khí hậu mát mẻ quanh năm.	\N	0	ACTIVE	CUSTOM
236	57	107	Ruộng bậc thang Mù Cang Chải	21.815753	104.128340	bậc thang Mù Cang Chải là kiệt tác thiên nhiên và lao động của đồng bào dân tộc H'Mông, nổi tiếng với vẻ đẹp hùng vĩ, đặc biệt vào mùa lúa chín (tháng 9 - tháng 10). Đây là Di sản Quốc gia và một trong những điểm đến hấp dẫn nhất Việt Nam.	Bạn có biết? Ruộng bậc thang Mù Cang Chải — bậc thang Mù Cang Chải là kiệt tác thiên nhiên và lao động của đồng bào dân tộc H'Mông, nổi tiếng với vẻ đẹp hùng vĩ, đặc biệt vào mùa lúa chín (tháng 9 - tháng 10).	4.70	214	ACTIVE	CUSTOM
122	46	107	Ruộng bậc thang Pù Luông	20.449342	105.157945	Đến Thanh Hóa mà không đặt chân đến các đến các ruộng bậc thang Pù Luông để check-in thì thật là một thiếu sót to lớn. Ruộng bậc thang Pù Luông vừa là niềm tự hào của nhân dân ta, vừa là địa điểm check-in Thanh Hóa siêu hot	Bạn có biết? Ruộng bậc thang Pù Luông — Đến Thanh Hóa mà không đặt chân đến các đến các ruộng bậc thang Pù Luông để check-in thì thật là một thiếu sót to lớn.	\N	0	ACTIVE	CUSTOM
117	49	107	Đồi chè Tân Cương	21.544137	105.759957	Một trong những địa điểm du lịch Thái Nguyên nổi tiếng chắc chắn không thể quên kể đến đó chính là đồi chè Tân Cương. Đây là một địa điểm thu hút sự quan tâm từ đông đảo khách du lịch nhờ vào cảnh quan thiên nhiên tuyệt đẹp, được bao phủ bởi một sắc xanh mát lạnh. Bất cứ ai đến đồi chè Tân Cương du lịch cũng đều có thể cảm nhận được sự tươi mới từ sâu trong tâm hồn, giúp bạn cảm thấy dễ chịu hơn bao giờ hết.	Bạn có biết? Đồi chè Tân Cương là một trong những địa điểm du lịch Thái Nguyên nổi tiếng chắc chắn không thể quên kể đến đó chính là đồi chè Tân Cương.	\N	0	ACTIVE	CUSTOM
94	43	107	Đồi hoa Miền Viên Thảo	16.645749	106.725938	Nổi bật với cánh đồng hoa rực rỡ và nở rộ quanh năm. Thời tiết ở đây cực kỳ mát mẻ nên việc trồng hoa khá thuận lợi. Địa điểm du lịch Quảng Trị có đủ hoa hồng, cúc họa mi, hoa cải vàng. Đặc biệt, nếu bạn yêu thích hoa tam giác mạch thì không thể bỏ hoa Miền Viên Thảo.	Bạn có biết? Đồi hoa Miền Viên Thảo — Nổi bật với cánh đồng hoa rực rỡ và nở rộ quanh năm.	4.10	47	ACTIVE	CUSTOM
83	41	107	Đồng muối Sa Huỳnh	14.688583	109.063682	Vựa muối quan trọng nhất miền Trung thân yêu của chúng ta, những bãi muối trắng phao, đến nơi đây bạn sẽ được trãi nghiệm các hoạt động hằng ngày của bà con nơi đây, hứa hẹn sẽ mang lại cho bạn những trãi nghiệm rất khác so với các hoạt động du lịch như lên rừng hay xuống biển.	Bạn có biết? Đồng muối Sa Huỳnh — Vựa muối quan trọng nhất miền Trung thân yêu của chúng ta, những bãi muối trắng phao, đến nơi đây bạn sẽ được trãi nghiệm các hoạt động hằng ngày của bà con nơi đây, hứa hẹn sẽ mang lại cho bạn những trãi nghiệm rất khác so với các hoạt động du lịch như lên rừng hay xuống biển.	4.30	276	ACTIVE	CUSTOM
59	36	107	Vườn nho Thái An Quán	11.665269	109.172418	Vườn nho tại thôn Thái An, xã Vĩnh Hải, khu vực Ninh Thuận, phục vụ tham quan và tìm hiểu hoạt động trồng nho.	Bạn có biết? Vườn nho Thái An Quán — Vườn nho tại thôn Thái An, xã Vĩnh Hải, khu vực Ninh Thuận, phục vụ tham quan và tìm hiểu hoạt động trồng nho.	4.80	46	ACTIVE	CUSTOM
274	16	109	Hẻm Tu Sản	23.228399	105.424618	Hẻm Tu Sản, nơi sâu nhất Đông Nam Á, nổi bật với vách đá cao 800m và dòng sông Nho Quế xanh biếc. Du khách có thể chèo thuyền khám phá khung cảnh kỳ vĩ này.	Bạn có biết? Hẻm Tu Sản, nơi sâu nhất Đông Nam Á, nổi bật với vách đá cao 800m và dòng sông Nho Quế xanh biếc.	5.00	11	ACTIVE	CUSTOM
16	28	110	Đèo Ô Quy Hồ	22.353200	103.764530	Một trong "Tứ đại đỉnh đèo" của Tây Bắc, Đèo Ô Quy Hồ nối liền Lào Cai và Lai Châu, nổi tiếng với cảnh sắc tuyệt đẹp và độ cao hùng vĩ	Bạn có biết? Đèo Ô Quy Hồ nối Lào Cai và Lai Châu, thuộc nhóm Tứ đại đỉnh đèo của Tây Bắc.	4.70	789	ACTIVE	CUSTOM
49	34	107	Đồi chè Thanh Chương	18.731346	105.281245	Được mệnh danh là "đảo chè", đồi chè Thanh Chương là địa điểm lý tưởng để chụp ảnh và tận hưởng không khí trong lành.	Bạn có biết? Đồi chè Thanh Chương được mệnh danh là "đảo chè", đồi chè Thanh Chương là địa điểm lý tưởng để chụp ảnh và tận hưởng không khí trong lành.	4.70	562	ACTIVE	CUSTOM
18	28	107	Cánh đồng Mường Than	21.988162	103.912812	Vào mùa lúa chín, cánh đồng Mường Than rực rỡ sắc vàng, tạo nên một bức tranh thiên nhiên thơ mộng	Bạn có biết? Cánh đồng Mường Than — Vào mùa lúa chín, cánh đồng Mường Than rực rỡ sắc vàng, tạo nên một bức tranh thiên nhiên thơ mộng.	4.80	56	ACTIVE	CUSTOM
309	20	109	Cửu thác Tú Sơn	20.709201	105.417094	Cửu thác Tú Sơn là khu cảnh quan thác, suối và rừng tại khu vực Tú Sơn, Kim Bôi, Hòa Bình, gắn với không gian văn hóa Mường.	Bạn có biết? Cửu thác Tú Sơn là khu cảnh quan thác, suối và rừng tại khu vực Tú Sơn, Kim Bôi, Hòa Bình, gắn với không gian văn hóa Mường.	3.60	334	ACTIVE	CUSTOM
308	20	111	Động Đá Bạc	20.822444	105.605195	Động Đá Bạc là hang động đá vôi tại khu vực Lương Sơn, Hòa Bình, có các khối thạch nhũ và cảnh quan hang tự nhiên.	Bạn có biết? Động Đá Bạc là hang động đá vôi tại khu vực Lương Sơn, Hòa Bình, có các khối thạch nhũ và cảnh quan hang tự nhiên.	4.70	70	ACTIVE	CUSTOM
306	20	109	Thung Nai	20.729115	105.313505	Thung Nai (Hòa Bình) là khu du lịch nổi tiếng với cảnh sắc thiên nhiên hùng vĩ, bao gồm hồ nước rộng lớn và các dãy núi bao quanh. Nơi đây là địa điểm lý tưởng cho du khách yêu thích du lịch sinh thái và tham gia các hoạt động ngoài trời như chèo thuyền.	Bạn có biết? Thung Nai (Hòa Bình) là khu du lịch nổi tiếng với cảnh sắc thiên nhiên hùng vĩ, bao gồm hồ nước rộng lớn và các dãy núi bao quanh.	\N	0	ACTIVE	CUSTOM
305	24	109	Sông Cái Lớn	9.923611	105.093056	Sông Cái Lớn là tuyến sông thuộc vùng Tây Nam Bộ, liên quan đến khu vực Hậu Giang và Kiên Giang theo địa giới gốc của dataset. Dòng sông gắn với cảnh quan sông nước và sinh hoạt ven sông.	Bạn có biết? Sông Cái Lớn gắn với cảnh quan sông nước và đời sống ven sông ở Tây Nam Bộ.	4.30	479	ACTIVE	CUSTOM
301	24	112	Khu du lịch Lung Ngọc Hoàng	9.722750	105.699917	Khu du lịch Lung Ngọc Hoàng (Hậu Giang) là khu bảo tồn thiên nhiên nổi bật với hệ sinh thái rừng ngập mặn. Đây là điểm đến lý tưởng cho những ai yêu thích du lịch sinh thái và nghiên cứu động thực vật.	Bạn có biết? Khu du lịch Lung Ngọc Hoàng (Hậu Giang) là khu bảo tồn thiên nhiên nổi bật với hệ sinh thái rừng ngập mặn.	5.00	1	ACTIVE	CUSTOM
297	23	108	Cát Bà	20.800000	106.999722	Cát Bà (Hải Phòng) là hòn đảo nổi tiếng với bãi biển tuyệt đẹp và hệ sinh thái đa dạng. Nơi đây là một điểm đến lý tưởng cho những ai yêu thích du lịch sinh thái và biển.	Bạn có biết? Cát Bà (Hải Phòng) là hòn đảo nổi tiếng với bãi biển tuyệt đẹp và hệ sinh thái đa dạng.	4.40	1372	ACTIVE	CUSTOM
296	23	108	Đồ Sơn	20.687277	106.795479	Đồ Sơn (Hải Phòng) là một bãi biển đẹp nổi tiếng với những dãy núi và biển xanh. Đây là khu du lịch biển với các hoạt động thể thao nước và thư giãn dưới ánh nắng.	Bạn có biết? Đồ Sơn (Hải Phòng) là một bãi biển đẹp nổi tiếng với những dãy núi và biển xanh.	\N	0	ACTIVE	CUSTOM
291	22	108	Đảo Cò Chi Lăng Nam	20.713610	106.227922	Đảo Cò Chi Lăng Nam (Hải Dương) là một khu vực bảo tồn thiên nhiên nổi bật với hàng nghìn con cò sinh sống. Đây là địa điểm du lịch sinh thái lý tưởng, với không gian yên bình và cảnh quan tuyệt đẹp.	Bạn có biết? Đảo Cò Chi Lăng Nam (Hải Dương) là một khu vực bảo tồn thiên nhiên nổi bật với hàng nghìn con cò sinh sống.	4.30	336	ACTIVE	CUSTOM
290	19	110	Đèo Ngang	17.956111	106.467778	Đèo Ngang (Quảng Bình) là một trong những con đèo đẹp nhất Việt Nam, nối liền hai tỉnh Quảng Bình và Hà Tĩnh. Đèo có cảnh quan hùng vĩ và thơ mộng.	Bạn có biết? Đèo Ngang (Quảng Bình) là một trong những con đèo đẹp nhất Việt Nam, nối liền hai tỉnh Quảng Bình và Hà Tĩnh.	4.40	372	ACTIVE	CUSTOM
288	19	108	Biển Thiên Cầm	18.273541	106.104252	Biển Thiên Cầm (Hà Tĩnh) là bãi biển xinh đẹp với bãi cát dài và nước biển trong xanh, nơi du khách có thể thư giãn và tham gia các hoạt động thể thao biển.	Bạn có biết? Biển Thiên Cầm (Hà Tĩnh) là bãi biển xinh đẹp với bãi cát dài và nước biển trong xanh, nơi du khách có thể thư giãn và tham gia các hoạt động thể thao biển.	4.30	1070	ACTIVE	CUSTOM
282	18	109	Hồ Gươm	21.028889	105.852500	Hồ Gươm (Hà Nội) là trung tâm của thủ đô, nổi bật với Tháp Rùa và cầu Thê Húc. Đây là điểm du lịch hấp dẫn, nơi người dân thư giãn và là biểu tượng của Hà Nội.	Bạn có biết? Hồ Gươm gắn với hai hình ảnh quen thuộc của Hà Nội: Tháp Rùa và cầu Thê Húc.	4.70	13004	ACTIVE	CUSTOM
278	17	111	Hang Luồn – Ao Dong (Hà Nam)	20.540313	105.857641	Hang Luồn trong khu Ao Dong, khu vực Liên Sơn, Kim Bảng, Hà Nam, là hang xuyên thủy giữa cảnh quan núi đá vôi và mặt nước.	Bạn có biết? Hang Luồn – Ao Dong (Hà Nam) — Hang Luồn trong khu Ao Dong, khu vực Liên Sơn, Kim Bảng, Hà Nam, là hang xuyên thủy giữa cảnh quan núi đá vôi và mặt nước.	4.10	89	ACTIVE	CUSTOM
10	26	101	Mộ chị Sứ	10.106083	104.893974	Khu di tích lịch sử nổi tiếng, gắn liền với cuộc kháng chiến chống Pháp.	Bạn có biết? Mộ chị Sứ — Khu di tích lịch sử nổi tiếng, gắn liền với cuộc kháng chiến chống Pháp.	4.40	1004	ACTIVE	CUSTOM
11	27	102	Nhà Thờ Gỗ Kon Tum	14.345037	108.013158	Một biểu tượng của Kon Tum mang kiến trúc độc đáo pha trộn giữa phong cách Roman và nét văn hóa Tây Nguyên	Bạn có biết? Nhà Thờ Gỗ Kon Tum kết hợp phong cách Roman với nét văn hóa Tây Nguyên.	4.70	4300	ACTIVE	CUSTOM
3	25	101	Thành cổ Diên Khánh	12.254820	109.088725	Di tích lịch sử với kiến trúc thành cổ độc đáo, được xây dựng từ thời Nguyễn.	Bạn có biết? Thành cổ Diên Khánh — Di tích lịch sử với kiến trúc thành cổ độc đáo, được xây dựng từ thời Nguyễn.	\N	0	ACTIVE	CUSTOM
2	25	101	Tháp Bà Ponagar	12.265278	109.195556	Công trình kiến trúc Chăm cổ, nơi du khách có thể tìm hiểu về lịch sử và văn hóa Chăm	Bạn có biết? Tháp Bà Ponagar là công trình Chăm cổ lưu giữ dấu ấn văn hóa Chăm ở Nha Trang.	4.50	32654	ACTIVE	CUSTOM
1	25	108	Bãi Dài Cam Ranh	12.050013	109.208276	Bãi biển hoang sơ với vẻ đẹp tự nhiên, lý tưởng để thư giãn và tắm biển	Bạn có biết? Bãi Dài Cam Ranh — Bãi biển hoang sơ với vẻ đẹp tự nhiên, lý tưởng để thư giãn và tắm biển.	4.50	845	ACTIVE	CUSTOM
15	27	109	Thác Đăk Sing	14.697229	107.869070	Thác Đăk Sing tại khu vực Kon Đào, Đắk Tô, Kon Tum theo địa giới gốc của dataset. Đây là điểm thác tự nhiên được chọn để thay tên chung Thác Đắk Tô chưa xác định được địa điểm cụ thể.	Bạn có biết? Thác Đăk Sing là điểm thác tự nhiên ở khu vực Kon Đào, Đắk Tô.	4.50	14	ACTIVE	CUSTOM
14	27	110	Đỉnh Ngọc Linh	15.087541	107.926369	Ngọn núi cao nhất ở Tây Nguyên, thuộc dãy Trường Sơn, là nơi sinh sống của nhiều loài động thực vật quý hiếm.	Bạn có biết? Đỉnh Ngọc Linh — Ngọn núi cao nhất ở Tây Nguyên, thuộc dãy Trường Sơn, là nơi sinh sống của nhiều loài động thực vật quý hiếm.	\N	0	ACTIVE	CUSTOM
13	27	112	Vườn Quốc Gia Chư Mom Ray	14.466667	107.633333	Là khu bảo tồn thiên nhiên lớn nhất Việt Nam, nổi bật với hệ sinh thái phong phú, những cánh rừng nguyên sinh và động vật quý hiếm	Bạn có biết? Vườn Quốc Gia Chư Mom Ray là khu bảo tồn thiên nhiên lớn nhất Việt Nam, nổi bật với hệ sinh thái phong phú, những cánh rừng nguyên sinh và động vật quý hiếm.	4.70	90	ACTIVE	CUSTOM
9	26	112	Vườn quốc gia U Minh Thượng	9.583330	105.083000	Rừng ngập mặn với hệ sinh thái phong phú, là nơi lý tưởng để khám phá thiên nhiên hoang dã và tham quan bằng thuyền	Bạn có biết? Vườn quốc gia U Minh Thượng — Rừng ngập mặn với hệ sinh thái phong phú, là nơi lý tưởng để khám phá thiên nhiên hoang dã và tham quan bằng thuyền.	4.30	16	ACTIVE	CUSTOM
6	26	108	Đảo Phú Quốc	10.216081	103.959391	Là điểm du lịch nổi tiếng nhất Kiên Giang, được mệnh danh là "Đảo Ngọc" với nhiều bãi biển đẹp và dịch vụ cao cấp	Bạn có biết? Phú Quốc còn được gọi là Đảo Ngọc.	4.40	6319	ACTIVE	CUSTOM
8	26	102	Chùa Láng Cát	9.999427	105.094487	Ngôi chùa cổ kính mang đậm dấu ấn Phật giáo.	Bạn có biết? Chùa Láng Cát — Ngôi chùa cổ kính mang đậm dấu ấn Phật giáo.	4.30	105	ACTIVE	CUSTOM
186	4	110	Núi Bà Rá	11.816906	107.000906	Ngọn núi cao nhất Bình Phước, có cáp treo dẫn lên đỉnh	Bạn có biết? Núi Bà Rá — Ngọn núi cao nhất Bình Phước, có cáp treo dẫn lên đỉnh.	4.30	344	ACTIVE	CUSTOM
298	23	101	Bến K15 (Đồ Sơn)	20.677364	106.802932	Bến K15 tại Đồ Sơn, Hải Phòng, là di tích gắn với các chuyến tàu không số và tuyến vận tải Hồ Chí Minh trên biển. Bến Nghiêng là một di tích khác ở gần đó.	Bạn có biết? Bến K15 (Đồ Sơn) — Bến K15 tại Đồ Sơn, Hải Phòng, là di tích gắn với các chuyến tàu không số và tuyến vận tải Hồ Chí Minh trên biển.	4.70	112	ACTIVE	CUSTOM
272	16	110	Đèo Mã Pí Lèng	23.240808	105.410942	Đèo Mã Pí Lèng là cung đường hùng vĩ tại Hà Giang, với sông Nho Quế xanh ngọc chảy uốn lượn phía dưới. Đây là điểm thu hút phượt thủ và du khách yêu vẻ đẹp thiên nhiên hoang sơ.	Bạn có biết? Đèo Mã Pí Lèng là cung đường hùng vĩ tại Hà Giang, với sông Nho Quế xanh ngọc chảy uốn lượn phía dưới.	4.80	812	ACTIVE	CUSTOM
267	15	109	Thác Phú Cường	13.662655	108.135356	Một thác nước đẹp tại Gia Lai, nằm giữa rừng nguyên sinh. Thác nổi bật với dòng nước trắng xóa, đổ từ độ cao hơn 45m.	Bạn có biết? Thác Phú Cường là một thác nước đẹp tại Gia Lai, nằm giữa rừng nguyên sinh.	4.10	359	ACTIVE	CUSTOM
266	15	109	Biển Hồ (T'Nưng)	14.045357	107.998066	Biển Hồ, còn gọi là hồ T'Nưng, là hồ nước ngọt tự nhiên ở khu vực Pleiku, Gia Lai, hình thành trên địa hình núi lửa cổ và nổi bật với cảnh quan mặt nước giữa đồi núi.	Bạn có biết? Biển Hồ (T'Nưng) — Biển Hồ, còn gọi là hồ T'Nưng, là hồ nước ngọt tự nhiên ở khu vực Pleiku, Gia Lai, hình thành trên địa hình núi lửa cổ và nổi bật với cảnh quan mặt nước giữa đồi núi.	4.40	660	ACTIVE	CUSTOM
261	63	112	Vườn quốc gia Tràm Chim	10.725000	105.517000	Vườn quốc gia ở Đồng Tháp, nơi bảo tồn hệ sinh thái đất ngập nước đặc trưng Đồng bằng sông Cửu Long. Tràm Chim là nhà của nhiều loài chim quý hiếm, đặc biệt là sếu đầu đỏ.	Bạn có biết? Vườn quốc gia Tràm Chim là nơi sinh sống của sếu đầu đỏ.	4.10	3521	ACTIVE	CUSTOM
260	62	109	Thác Giang Điền	10.921673	106.983357	Một thác nước đẹp ở Đồng Nai, được bao quanh bởi rừng cây xanh mát. Thác Giang Điền là địa điểm du lịch sinh thái, hấp dẫn cho các hoạt động nghỉ dưỡng, picnic và chụp ảnh kỷ niệm.	Bạn có biết? Thác Giang Điền là một thác nước đẹp ở Đồng Nai, được bao quanh bởi rừng cây xanh mát.	4.40	2692	ACTIVE	CUSTOM
259	62	108	Đảo Ó	11.108076	107.102078	Một hòn đảo nằm giữa lòng hồ Trị An, sở hữu không gian xanh mát và trong lành. Đảo Ó là điểm đến hấp dẫn cho các hoạt động dã ngoại, cắm trại, và thưởng ngoạn thiên nhiên.	Bạn có biết? Đảo Ó là một hòn đảo nằm giữa lòng hồ Trị An, sở hữu không gian xanh mát và trong lành.	4.00	345	ACTIVE	CUSTOM
258	62	108	Hồ Trị An	11.131390	107.134440	Hồ Trị An với trên 70 đảo lớn, nhỏ và nhiều đảo trong số này có cư dân sinh sống, cộng thêm vài hòn đảo có diện tích lớn được khai thác du lịch gần chục năm trở lại đây đã trở thành điểm nhấn của sông nước Trị An	Bạn có biết? Hồ Trị An với trên 70 đảo lớn, nhỏ và nhiều đảo trong số này có cư dân sinh sống, cộng thêm vài hòn đảo có diện tích lớn được khai thác du lịch gần chục năm trở lại đây đã trở thành điểm nhấn của sông nước Trị An.	4.50	1545	ACTIVE	CUSTOM
256	62	112	Vườn quốc gia Cát Tiên	11.347222	107.151389	Một trong những vườn quốc gia nổi tiếng nhất Việt Nam, nằm ở tỉnh Đồng Nai. Nơi đây sở hữu hệ sinh thái phong phú với nhiều loài động thực vật quý hiếm, cùng các hoạt động như khám phá rừng, ngắm chim và dã ngoại.	Bạn có biết? Vườn quốc gia Cát Tiên là một trong những vườn quốc gia nổi tiếng nhất Việt Nam, nằm ở tỉnh Đồng Nai.	4.70	1716	ACTIVE	CUSTOM
250	61	109	Khu bảo tồn thiên nhiên Nâm Nung	12.254589	107.810994	Khu bảo tồn nằm ở tỉnh Đắk Nông, nổi tiếng với hệ sinh thái đa dạng gồm rừng nguyên sinh, thác nước hùng vĩ và nhiều loài động thực vật quý hiếm. Đây là địa điểm lý tưởng cho các hoạt động khám phá thiên nhiên, trekking, và nghiên cứu sinh thái.	Bạn có biết? Khu bảo tồn thiên nhiên Nâm Nung — Khu bảo tồn nằm ở tỉnh Đắk Nông, nổi tiếng với hệ sinh thái đa dạng gồm rừng nguyên sinh, thác nước hùng vĩ và nhiều loài động thực vật quý hiếm.	4.10	647	ACTIVE	CUSTOM
249	61	109	Hồ Ea Snô	12.438378	107.912447	Hồ Ea Snô được bao quanh bởi hệ sinh thái thực vật đa dạng và phong phú, là nơi sinh sống của nhiều cộng đồng dân tộc bản địa. Khung cảnh nơi đây mang lại cảm giác bình yên và thư thá	Bạn có biết? Hồ Ea Snô được bao quanh bởi hệ sinh thái thực vật đa dạng và phong phú, là nơi sinh sống của nhiều cộng đồng dân tộc bản địa.	4.30	15	ACTIVE	CUSTOM
248	61	111	Hang động Chư Bluk	12.475833	107.944999	Hang động Chư Bluk, nằm ở huyện Krông Nô, tỉnh Đắk Nông, là hệ thống hang động núi lửa dài nhất Đông Nam Á. Hang động Chư Bluk nổi bật với vẻ đẹp hoang sơ và kỳ bí, với những cấu trúc đá bazan độc đáo và hệ sinh thái phong phú. Đây là điểm đến lý tưởng cho những ai yêu thích khám phá và mạo hiểm, bởi việc tiếp cận hang động không hề dễ dàng và đòi hỏi sự hướng dẫn của người dân địa phương	Bạn có biết? Hang động Chư Bluk, nằm ở huyện Krông Nô, tỉnh Đắk Nông, là hệ thống hang động núi lửa dài nhất Đông Nam Á.	4.70	161	ACTIVE	CUSTOM
247	61	109	Thác Dray Sap	12.538567	107.889975	Thác Dray Sap, còn được gọi là Thác Khói, là một trong những thác nước đẹp và hùng vĩ nhất của Tây Nguyên	Bạn có biết? Thác Dray Sap, còn được gọi là Thác Khói, là một trong những thác nước đẹp và hùng vĩ nhất của Tây Nguyên.	4.60	57	ACTIVE	CUSTOM
246	61	111	Công viên địa chất Đắk Nông	12.573249	107.887621	Công viên Địa chất Đắk Nông nổi tiếng với hệ thống hang động núi lửa độc đáo, bao gồm gần 50 hang động với tổng chiều dài hơn 10.000 mét. Công viên Địa chất Đắk Nông không chỉ là nơi bảo tồn các giá trị địa chất, mà còn là nơi lưu giữ và giới thiệu văn hóa của các dân tộc bản địa như người Mạ, người Mnông và người Ê-đê	Bạn có biết? Công viên Địa chất Đắk Nông nổi tiếng với hệ thống hang động núi lửa độc đáo, bao gồm gần 50 hang động với tổng chiều dài hơn 10.000 mét.	\N	0	ACTIVE	CUSTOM
243	60	109	Thác Dray Nur	12.540426	107.889884	Thác Dray Nur, nằm ở tỉnh Đắk Lắk, là một trong những thác nước đẹp và hùng vĩ nhất của Tây Nguyên. Thác này nằm bên dòng sông Sêrêpôk, nổi tiếng với vẻ đẹp hoang sơ và huyền bí	Bạn có biết? Thác Dray Nur, nằm ở tỉnh Đắk Lắk, là một trong những thác nước đẹp và hùng vĩ nhất của Tây Nguyên.	4.50	8281	ACTIVE	CUSTOM
242	60	109	Hồ Lắk	12.422500	108.180000	Hồ Lắk là hồ nước ngọt tự nhiên thuộc khu vực Lắk, Đắk Lắk, gắn với cảnh quan hồ, đồi núi và các buôn của cộng đồng M'nông.	Bạn có biết? Hồ Lắk là hồ nước ngọt tự nhiên thuộc khu vực Lắk, Đắk Lắk, gắn với cảnh quan hồ, đồi núi và các buôn của cộng đồng M'nông.	4.20	743	ACTIVE	CUSTOM
237	57	108	Hồ Thác Bà	21.749029	105.023108	Hồ Thác Bà là một trong những hồ nước nhân tạo lớn nhất Việt Nam, được bao quanh bởi hàng trăm hòn đảo nhỏ và cảnh quan núi non tuyệt đẹp. Du khách có thể tham gia các hoạt động như chèo thuyền, cắm trại, hoặc khám phá hang động quanh hồ.	Bạn có biết? Hồ Thác Bà là một trong những hồ nước nhân tạo lớn nhất Việt Nam, được bao quanh bởi hàng trăm hòn đảo nhỏ và cảnh quan núi non tuyệt đẹp.	4.50	266	ACTIVE	CUSTOM
232	56	109	Hồ Đại Lải	21.321944	105.714444	Hồ Đại Lải là một hồ nước nhân tạo rộng lớn, được bao quanh bởi rừng thông và núi đồi. Đây là điểm đến lý tưởng để tham gia các hoạt động như chèo thuyền, cắm trại, câu cá hoặc nghỉ dưỡng tại các khu resort ven hồ.	Bạn có biết? Hồ Đại Lải là một hồ nước nhân tạo rộng lớn, được bao quanh bởi rừng thông và núi đồi.	4.40	1071	ACTIVE	CUSTOM
225	53	109	Thác Khuổi Nhi	22.495480	105.361580	hác Khuổi Nhi thuộc khu vực Na Hang, là một trong những thác nước đẹp nhất ở Tuyên Quang. Nước thác trắng xóa đổ xuống từ độ cao lớn, hòa quyện với thiên nhiên hùng vĩ tạo nên cảnh sắc thơ mộng.	Bạn có biết? Thác Khuổi Nhi — hác Khuổi Nhi thuộc khu vực Na Hang, là một trong những thác nước đẹp nhất ở Tuyên Quang.	\N	0	ACTIVE	CUSTOM
224	53	109	Suối khoáng Mỹ Lâm	21.765885	105.123625	Suối khoáng Mỹ Lâm nổi tiếng với nguồn nước khoáng nóng tự nhiên, có tác dụng chữa bệnh và thư giãn. Đây là khu du lịch nghỉ dưỡng hấp dẫn, kết hợp giữa trị liệu sức khỏe và cảnh sắc thiên nhiên tươi đẹp.	Bạn có biết? Suối khoáng Mỹ Lâm nổi tiếng với nguồn nước khoáng nóng tự nhiên, có tác dụng chữa bệnh và thư giãn.	\N	0	ACTIVE	CUSTOM
222	53	108	Hồ Thủy Điện Tuyên Quang	22.360472	105.401028	Hồ Na Hang được ví như "Hạ Long trên núi", nổi bật với mặt hồ trong xanh, các đảo núi đá vôi trập trùng, và rừng nguyên sinh bao quanh. Du khách có thể khám phá các địa danh như thác Khuổi Nhi, đền Pác Tạ, và tham gia du thuyền ngắm cảnh.	Bạn có biết? Hồ Thủy Điện Tuyên Quang — Hồ Na Hang được ví như "Hạ Long trên núi", nổi bật với mặt hồ trong xanh, các đảo núi đá vôi trập trùng, và rừng nguyên sinh bao quanh.	4.60	212	ACTIVE	CUSTOM
221	53	109	Thác Bản Ba	22.337427	105.067892	Thác Bản Ba thuộc xã Trung Hà, huyện Chiêm Hóa, là một cụm thác với 3 tầng chính đổ xuống từ độ cao khoảng 40 m. Thác được bao quanh bởi rừng núi hoang sơ, nước trong xanh và khí hậu mát mẻ quanh năm. Đây là điểm đến lý tưởng để hòa mình vào thiên nhiên.	Bạn có biết? Thác Bản Ba thuộc xã Trung Hà, huyện Chiêm Hóa, là một cụm thác với 3 tầng chính đổ xuống từ độ cao khoảng 40 m.	4.50	133	ACTIVE	CUSTOM
218	52	108	Bãi biển Ba Động	9.623475	106.560817	bãi biển hoang sơ với cát mịn và sóng nhẹ, Ba Động là nơi thích hợp để thư giãn và thưởng thức các món hải sản tươi ngon. Đây là một trong những bãi biển hiếm hoi ở miền Tây Nam Bộ.	Bạn có biết? Bãi biển Ba Động — bãi biển hoang sơ với cát mịn và sóng nhẹ, Ba Động là nơi thích hợp để thư giãn và thưởng thức các món hải sản tươi ngon.	4.60	1180	ACTIVE	CUSTOM
216	52	109	Ao bà om	9.917608	106.304092	Ao Bà Om là một hồ nước tự nhiên, được bao quanh bởi hàng cây cổ thụ hàng trăm năm tuổi với rễ cây nổi lên thành những hình thù kỳ thú. Đây là một địa điểm du lịch yên bình, gắn liền với truyền thuyết và văn hóa Khmer.	Bạn có biết? Ao Bà Om là một hồ nước tự nhiên, được bao quanh bởi hàng cây cổ thụ hàng trăm năm tuổi với rễ cây nổi lên thành những hình thù kỳ thú.	4.50	1190	ACTIVE	CUSTOM
214	59	108	Bãi biển Mỹ Khê (Đà Nẵng)	16.070400	108.246100	Một trong những bãi biển đẹp nhất hành tinh, nổi tiếng với bãi cát trắng mịn, nước biển trong xanh, là nơi lý tưởng để tắm biển và các hoạt động thể thao dưới nước.	Bạn có biết? Bãi biển Mỹ Khê (Đà Nẵng) là một trong những bãi biển đẹp nhất hành tinh, nổi tiếng với bãi cát trắng mịn, nước biển trong xanh, là nơi lý tưởng để tắm biển và các hoạt động thể thao dưới nước.	4.70	4707	ACTIVE	CUSTOM
213	59	111	Ngũ Hành Sơn	16.004020	108.262775	Cụm núi đá vôi gồm 5 ngọn núi với hang động, chùa chiền, và khung cảnh thiên nhiên tuyệt đẹp. Nổi bật là động Huyền Không và chùa Tam Thai.	Bạn có biết? Ngũ Hành Sơn — Cụm núi đá vôi gồm 5 ngọn núi với hang động, chùa chiền, và khung cảnh thiên nhiên tuyệt đẹp.	4.40	43219	ACTIVE	CUSTOM
240	57	111	Động Thiên Cung Suối Giàng	21.603643	104.586016	Hang động tại khu vực Suối Giàng, Văn Chấn, Yên Bái, có cảnh quan đá vôi và thạch nhũ. Địa danh khác với động Thiên Cung ở vịnh Hạ Long.	Bạn có biết? Động Thiên Cung Suối Giàng ở Yên Bái khác với động Thiên Cung tại vịnh Hạ Long.	4.40	76	ACTIVE	CUSTOM
193	5	108	Bàu Trắng	11.066667	108.416667	Hồ nước ngọt lớn giữa vùng cát trắng, còn gọi là "tiểu sa mạc Sahara" của Việt Nam.	Bạn có biết? Bàu Trắng — Hồ nước ngọt lớn giữa vùng cát trắng, còn gọi là "tiểu sa mạc Sahara" của Việt Nam.	4.50	1766	ACTIVE	CUSTOM
208	12	110	Núi Mắt Thần	22.778215	106.311286	Ngọn núi độc đáo với một lỗ lớn ở giữa, được gọi là "Mắt Thần." Đây là điểm check-in nổi tiếng với cảnh quan núi non tuyệt đẹp.	Bạn có biết? Núi Mắt Thần — Ngọn núi độc đáo với một lỗ lớn ở giữa, được gọi là "Mắt Thần." Đây là điểm check-in nổi tiếng với cảnh quan núi non tuyệt đẹp.	4.80	1501	ACTIVE	CUSTOM
207	12	111	Động Ngườm Ngao	22.845328	106.706170	Hang động dài hơn 2.1km, nổi bật với hệ thống thạch nhũ độc đáo và cảnh quan kỳ ảo. Động nằm gần thác Bản Giốc, là điểm đến lý tưởng để khám phá.	Bạn có biết? Động Ngườm Ngao — Hang động dài hơn 2.1km, nổi bật với hệ thống thạch nhũ độc đáo và cảnh quan kỳ ảo.	4.70	1324	ACTIVE	CUSTOM
206	12	109	Thác Bản Giốc	22.855897	106.722501	Một trong những thác nước đẹp nhất và lớn nhất Đông Nam Á, nằm trên biên giới Việt Nam - Trung Quốc. Thác được chia thành nhiều tầng với dòng nước trong xanh, tạo nên khung cảnh hùng vĩ.	Bạn có biết? Thác Bản Giốc nằm trên biên giới Việt Nam - Trung Quốc và được chia thành nhiều tầng.	4.60	11529	ACTIVE	CUSTOM
205	14	108	Cồn Sơn	10.070018	105.762777	Khu sinh thái sông nước nổi tiếng, nơi du khách có thể tham quan làng cá bè, học làm bánh dân gian và trải nghiệm lối sống miệt vườn.	Bạn có biết? Cồn Sơn — Khu sinh thái sông nước nổi tiếng, nơi du khách có thể tham quan làng cá bè, học làm bánh dân gian và trải nghiệm lối sống miệt vườn.	4.30	556	ACTIVE	CUSTOM
199	13	108	Đầm Thị Tường	8.999872	104.935748	Được mệnh danh là “biển hồ giữa đồng bằng”, đây là đầm nước tự nhiên lớn nhất miền Tây	Bạn có biết? Đầm Thị Tường được mệnh danh là “biển hồ giữa đồng bằng”, đây là đầm nước tự nhiên lớn nhất miền Tây.	4.30	206	ACTIVE	CUSTOM
198	13	108	Hòn Đá Bạc	9.179166	104.800278	Cụm đảo nằm sát bờ biển, nổi bật với những phiến đá xếp chồng tự nhiên và hệ sinh thái biển phong phú	Bạn có biết? Hòn Đá Bạc — Cụm đảo nằm sát bờ biển, nổi bật với những phiến đá xếp chồng tự nhiên và hệ sinh thái biển phong phú.	4.20	552	ACTIVE	CUSTOM
197	13	112	Vườn Quốc Gia U Minh Hạ	9.251389	104.945278	Khu rừng tràm ngập nước nổi tiếng với hệ sinh thái đa dạng. Đây là nơi sinh sống của nhiều loài động, thực vật quý hiếm	Bạn có biết? Vườn Quốc Gia U Minh Hạ — Khu rừng tràm ngập nước nổi tiếng với hệ sinh thái đa dạng.	4.20	846	ACTIVE	CUSTOM
196	13	108	Mũi Cà Mau	8.631667	104.713889	Điểm cực Nam của Việt Nam, nơi có biểu tượng cột mốc quốc gia và hệ sinh thái rừng ngập mặn độc đáo	Bạn có biết? Mũi Cà Mau — Điểm cực Nam của Việt Nam, nơi có biểu tượng cột mốc quốc gia và hệ sinh thái rừng ngập mặn độc đáo.	4.80	57	ACTIVE	CUSTOM
195	5	108	Hòn Rơm	10.954907	108.335259	Bãi biển nổi tiếng với làn nước trong xanh, bờ cát dài mịn và không khí yên bình	Bạn có biết? Hòn Rơm — Bãi biển nổi tiếng với làn nước trong xanh, bờ cát dài mịn và không khí yên bình.	4.40	539	ACTIVE	CUSTOM
190	4	112	Vườn quốc gia Bù Gia Mập	12.087199	107.156406	Vườn quốc gia tại khu vực Bù Gia Mập, Bình Phước, bảo tồn hệ sinh thái rừng và đa dạng sinh học. Nội dung khám phá phù hợp gồm rừng, cảnh quan thiên nhiên và tìm hiểu bảo tồn.	Bạn có biết? Vườn quốc gia Bù Gia Mập — Vườn quốc gia tại khu vực Bù Gia Mập, Bình Phước, bảo tồn hệ sinh thái rừng và đa dạng sinh học.	4.30	2687	ACTIVE	CUSTOM
189	4	109	Hồ Thác Mơ	11.857778	107.019167	Hồ nhân tạo lớn với cảnh quan thiên nhiên đẹp mắt	Bạn có biết? Hồ Thác Mơ — Hồ nhân tạo lớn với cảnh quan thiên nhiên đẹp mắt.	4.30	331	ACTIVE	CUSTOM
187	4	109	Trảng cỏ Bù Lạch	11.808898	107.345534	Một cánh đồng cỏ rộng lớn, xanh mướt, được bao quanh bởi những khu rừng nguyên sinh và hồ nước	Bạn có biết? Trảng cỏ Bù Lạch là một cánh đồng cỏ rộng lớn, xanh mướt, được bao quanh bởi những khu rừng nguyên sinh và hồ nước.	4.40	174	ACTIVE	CUSTOM
180	6	108	Hòn Khô	13.764402	109.299314	Hòn Khô là một hòn đảo nhỏ nằm gần Quy Nhơn, nổi tiếng với các bãi biển đẹp và làn nước trong vắt, thu hút du khách đến tham quan và tắm biển.	Bạn có biết? Hòn Khô là một hòn đảo nhỏ nằm gần Quy Nhơn, nổi tiếng với các bãi biển đẹp và làn nước trong vắt, thu hút du khách đến tham quan và tắm biển.	4.50	754	ACTIVE	CUSTOM
179	6	108	Đầm Thị Nại	13.825000	109.241667	Đầm Thị Nại ở Bình Định là một trong những đầm lớn của miền Trung, nổi bật với khung cảnh thiên nhiên tĩnh lặng	Bạn có biết? Đầm Thị Nại ở Bình Định là một trong những đầm lớn của miền Trung, nổi bật với khung cảnh thiên nhiên tĩnh lặng.	4.40	70	ACTIVE	CUSTOM
178	6	108	Cù Lao Xanh	13.615833	109.350556	Cù Lao Xanh là một hòn đảo đẹp và hoang sơ tại Bình Định, với làn nước trong xanh và thiên nhiên chưa bị khai thác nhiều.	Bạn có biết? Cù Lao Xanh là một hòn đảo đẹp và hoang sơ tại Bình Định, với làn nước trong xanh và thiên nhiên chưa bị khai thác nhiều.	4.70	141	ACTIVE	CUSTOM
177	6	108	Biển Kỳ Co	13.850019	109.292631	Biển Kỳ Co thuộc Quy Nhơn, nổi tiếng với bãi biển hoang sơ, nước trong xanh và các rạn san hô đẹp mắt	Bạn có biết? Biển Kỳ Co thuộc Quy Nhơn, nổi tiếng với bãi biển hoang sơ, nước trong xanh và các rạn san hô đẹp mắt.	4.40	4193	ACTIVE	CUSTOM
176	6	108	Bãi tắm Quy Nhơn	13.764367	109.226586	Bãi tắm Quy Nhơn là một trong những bãi biển đẹp của miền Trung, nổi tiếng với cát trắng mịn và nước biển trong xanh.	Bạn có biết? Bãi tắm Quy Nhơn là một trong những bãi biển đẹp của miền Trung, nổi tiếng với cát trắng mịn và nước biển trong xanh.	4.60	1206	ACTIVE	CUSTOM
163	7	112	Cây Xoài Cổ Thụ	9.247911	105.781431	Đây là cây xoài lớn tuổi, được xem là biểu tượng của sự trường thọ và thịnh vượng.	Bạn có biết? Cây Xoài Cổ Thụ — Đây là cây xoài lớn tuổi, được xem là biểu tượng của sự trường thọ và thịnh vượng.	4.40	248	ACTIVE	CUSTOM
162	7	108	Biển Bạc Liêu	9.203671	105.743470	Biển Bạc Liêu có bờ biển dài, là một điểm đến du lịch nghỉ dưỡng tuyệt vời tại miền Tây Nam Bộ	Bạn có biết? Biển Bạc Liêu có bờ biển dài, là một điểm đến du lịch nghỉ dưỡng tuyệt vời tại miền Tây Nam Bộ.	\N	0	ACTIVE	CUSTOM
160	9	111	Động Nàng Tiên	22.241549	106.153245	Đây là một hang động kỳ vĩ với thạch nhũ hình dạng đa dạng, gắn liền với truyền thuyết về nàng tiên xinh đẹp.	Bạn có biết? Động Nàng Tiên — Đây là một hang động kỳ vĩ với thạch nhũ hình dạng đa dạng, gắn liền với truyền thuyết về nàng tiên xinh đẹp.	\N	0	ACTIVE	CUSTOM
158	9	109	Thác Đầu Đẳng	22.452600	105.570412	Đây là địa điểm du lịch sinh thái lý tưởng.	Bạn có biết? Thác Đầu Đẳng — Đây là địa điểm du lịch sinh thái lý tưởng.	4.30	356	ACTIVE	CUSTOM
157	9	111	Động Puông	22.457500	105.657778	Là một hang động đẹp với những thạch nhũ kỳ vĩ	Bạn có biết? Động Puông là một hang động đẹp với những thạch nhũ kỳ vĩ.	4.50	223	ACTIVE	CUSTOM
156	9	109	Hồ Ba Bể	22.405278	105.615278	Hồ nước ngọt tự nhiên thuộc Vườn quốc gia Ba Bể, khu vực Bắc Kạn, bao quanh bởi núi đá vôi và rừng. Địa danh gắn với cảnh quan hồ, hoạt động đi thuyền và bản làng ven hồ.	Bạn có biết? Hồ Ba Bể là hồ nước ngọt tự nhiên được bao quanh bởi núi đá vôi và rừng.	4.50	1289	ACTIVE	CUSTOM
155	8	109	Hồ Khuôn Thần	21.455889	106.520608	Là một điểm đến lý tưởng với vẻ đẹp thiên nhiên thơ mộng và không khí trong lành	Bạn có biết? Hồ Khuôn Thần là một điểm đến lý tưởng với vẻ đẹp thiên nhiên thơ mộng và không khí trong lành.	4.50	70	ACTIVE	CUSTOM
153	8	109	Hồ Cấm Sơn	21.534781	106.565012	Là một trong những hồ nước ngọt lớn nhất miền Bắc Việt Nam	Bạn có biết? Hồ Cấm Sơn là một trong những hồ nước ngọt lớn nhất miền Bắc Việt Nam.	4.30	294	ACTIVE	CUSTOM
152	8	109	Thác Thùm Thùm	21.244672	106.474343	Là một điểm đến hoang sơ và tuyệt đẹp giữa lòng thiên nhiên	Bạn có biết? Thác Thùm Thùm là một điểm đến hoang sơ và tuyệt đẹp giữa lòng thiên nhiên.	4.20	179	ACTIVE	CUSTOM
150	2	108	Côn Đảo Vũng Tàu	8.682374	106.607208	Là một quần đảo nổi tiếng với vẻ đẹp thiên nhiên hoang sơ, mà còn mang đậm dấu ấn lịch sử với các di tích gắn liền thời kỳ chiến tranh.	Bạn có biết? Côn Đảo Vũng Tàu là một quần đảo nổi tiếng với vẻ đẹp thiên nhiên hoang sơ, mà còn mang đậm dấu ấn lịch sử với các di tích gắn liền thời kỳ chiến tranh.	4.60	721	ACTIVE	CUSTOM
149	2	108	Suối Ồ	10.567356	107.567003	Nơi đây nổi tiếng với vẻ đẹp hiếm có, vừa có suối nước ngọt, vừa có bãi biển nước mặn giao hòa trong cùng một khu vực.	Bạn có biết? Suối Ồ là nơi đây nổi tiếng với vẻ đẹp hiếm có, vừa có suối nước ngọt, vừa có bãi biển nước mặn giao hòa trong cùng một khu vực.	3.50	410	ACTIVE	CUSTOM
147	2	108	Bãi Sau	10.327500	107.088056	Bãi Sau được biết đến là một trong những bãi biển đẹp và lớn nhất Việt Nam	Bạn có biết? Bãi Sau được biết đến là một trong những bãi biển đẹp và lớn nhất Việt Nam.	4.40	13547	ACTIVE	CUSTOM
146	2	110	Núi Dinh Vũng Tàu	10.537500	107.129167	Núi Dinh nổi bật với những vách đá dựng đứng, tạo nên một bức tranh thiên nhiên hùng vĩ	Bạn có biết? Núi Dinh Vũng Tàu — Núi Dinh nổi bật với những vách đá dựng đứng, tạo nên một bức tranh thiên nhiên hùng vĩ.	4.60	838	ACTIVE	CUSTOM
142	1	109	Hồ Soài So	10.391981	105.000634	Hồ nước nhân tạo lớn nhất An Giang	Bạn có biết? Hồ Soài So — Hồ nước nhân tạo lớn nhất An Giang.	4.40	186	ACTIVE	CUSTOM
141	1	112	Rừng Tràm Trà Sư	10.585556	105.058611	Khu rừng tràm đẹp nhất và nổi tiếng nhất Việt Nam	Bạn có biết? Rừng Tràm Trà Sư — Khu rừng tràm đẹp nhất và nổi tiếng nhất Việt Nam.	4.40	7661	ACTIVE	CUSTOM
131	51	108	Biển Tân Thành	10.290282	106.782074	với làn nước trong xanh và bãi cát trắng, biển Tân Thành sở hữu vẻ đẹp rất riêng với bãi cát đen dài hơn 7km. Dọc theo bãi biển là cầu cảng màu xanh lam đặc trưng và các khu lưới người dân dựng lên để đánh bắt cá, nghêu…	Bạn có biết? Biển Tân Thành — với làn nước trong xanh và bãi cát trắng, biển Tân Thành sở hữu vẻ đẹp rất riêng với bãi cát đen dài hơn 7km.	4.40	1496	ACTIVE	CUSTOM
125	46	111	Động Từ Thức	20.043056	105.968056	Đào và được tạo thành từ muôn vàn hình dạng nhũ đá. Sâu bên trong động còn có một hồ nước nhỏ tạo nên một bức tranh vô cùng thơ mộng.	Bạn có biết? Động Từ Thức — Đào và được tạo thành từ muôn vàn hình dạng nhũ đá.	4.40	750	ACTIVE	CUSTOM
124	46	111	Vườn quốc gia Bến En	19.616667	105.525000	là nơi sinh sống của nhiều loài động thực vật quý hiếm có thể kể đến lim xanh, bù hương, chò chỉ… gấu ngựa, phượng hoàng đất…Vườn Quốc gia Bến En được ví như lá phổi xanh xanh của Thanh Hóa bởi những cánh rừng xanh bất tận. Và xen lẫn vào màu xanh ấy là những hang động, dải núi đá cao gập ghềnh đã tạo nên những địa điểm check-in Thanh Hóa độc đáo và đĩnh đạc nhất.	Bạn có biết? Vườn quốc gia Bến En là nơi sinh sống của nhiều loài động thực vật quý hiếm có thể kể đến lim xanh, bù hương, chò chỉ… gấu ngựa, phượng hoàng đất…Vườn Quốc gia Bến En được ví như lá phổi xanh xanh của Thanh Hóa bởi những cánh rừng xanh bất tận.	4.20	129	ACTIVE	CUSTOM
226	55	108	Cù Lao An Bình	10.291776	105.979790	Cù Lao An Bình là một khu vực miệt vườn đặc trưng của miền Tây Nam Bộ, nổi tiếng với vườn cây ăn trái như chôm chôm, sầu riêng, măng cụt. Du khách có thể tham gia chèo xuồng, tham quan các làng nghề truyền thống và thưởng thức ẩm thực dân dã.	Bạn có biết? Cù Lao An Bình là một khu vực miệt vườn đặc trưng của miền Tây Nam Bộ, nổi tiếng với vườn cây ăn trái như chôm chôm, sầu riêng, măng cụt.	\N	0	ACTIVE	CUSTOM
105	45	110	Đỉnh Tà Xùa	21.433772	104.303889	Từ trên đỉnh, bạn có thể phóng tầm mắt chiêm ngưỡng khung cảnh hùng vĩ xung quanh với những dải ruộng bậc thang xanh mướt, thấp thoáng xa xa là mái lá khói bếp, tay có thể chạm đến mây trời.	Bạn có biết? Đỉnh Tà Xùa — Từ trên đỉnh, bạn có thể phóng tầm mắt chiêm ngưỡng khung cảnh hùng vĩ xung quanh với những dải ruộng bậc thang xanh mướt, thấp thoáng xa xa là mái lá khói bếp, tay có thể chạm đến mây trời.	4.80	225	ACTIVE	CUSTOM
123	46	109	Suối cá Thần Cẩm Thủy	20.251958	105.390441	Suối cá Thần là địa điểm gắn liền với tín ngưỡng của người dân tộc Mường, bởi nơi đây có lượng sinh sống đặc biệt dày đặc. Hiện nay, suối cá cũng đã trở thành một trong những địa điểm check-in Thanh Hóa độc đáo và nổi tiếng mà bạn có thể thêm vào lịch trình của mình nhé.	Bạn có biết? Suối cá Thần Cẩm Thủy — Suối cá Thần là địa điểm gắn liền với tín ngưỡng của người dân tộc Mường, bởi nơi đây có lượng sinh sống đặc biệt dày đặc.	4.30	1325	ACTIVE	CUSTOM
121	46	109	Suối Hiêu	20.466886	105.222092	Nằm giữa khu rừng xanh mướt và những cánh đồng lúa bình yên, suối Hiêu cứ như thế mà yên ả chảy ríu rít ngày ngày để mang đến một địa điểm check-in Thanh Hóa vô cùng thơ mộng. Như một đóa hồng có gai, suối mang vẻ đẹp quyến rũ đến nao lòng nhưng lại có khả năng hóa đá tạm thời những gì rơi xuống bởi hàm lượng vôi rất cao.	Bạn có biết? Suối Hiêu — Nằm giữa khu rừng xanh mướt và những cánh đồng lúa bình yên, suối Hiêu cứ như thế mà yên ả chảy ríu rít ngày ngày để mang đến một địa điểm check-in Thanh Hóa vô cùng thơ mộng.	5.00	2	ACTIVE	CUSTOM
120	49	109	Thác Đát Đắng	21.642690	105.519884	bạn sẽ có cảm giác hệt như đang lạc bước vào một không gian mới mẻ, được tránh xa hoàn toàn sự xô bồ và náo nhiệt nơi phố thị. Cảnh núi rừng hùng vĩ, dòng nước xanh trong mát lành chính là những thứ ấn tượng khiến bạn có cảm giác thật sự thư giãn và mong muốn được tận hưởng sự yên bình hiếm thấy này mãi không thôi.	Bạn có biết? Thác Đát Đắng — bạn sẽ có cảm giác hệt như đang lạc bước vào một không gian mới mẻ, được tránh xa hoàn toàn sự xô bồ và náo nhiệt nơi phố thị.	4.30	44	ACTIVE	CUSTOM
119	49	111	Động Linh Sơn	21.627870	105.867057	Động Linh Sơn tỉnh Thái Nguyên có diện tích rộng rãi, vào mùa hè rất mát mẻ và mùa đông sẽ ấm áp vô cùng. Động nằm tại khu vực lưng chừng núi đá rộng lớn nên đã được người dân nơi đây lựa chọn xây dựng chùa thờ Phật. Cứ vào các ngày rằm, mùng một, người dân từ Thái Nguyên và nhiều tỉnh thành lân cận khác đều sẽ đến đây dâng hương lễ Phật rất náo nhiệt.	Bạn có biết? Động Linh Sơn tỉnh Thái Nguyên có diện tích rộng rãi, vào mùa hè rất mát mẻ và mùa đông sẽ ấm áp vô cùng.	4.50	6	ACTIVE	CUSTOM
118	49	109	Hồ Vai Miếu	21.530775	105.645416	Hồ Vai Miếu nằm dưới chân dãy Tam Đảo, thuộc khu vực Ký Phú, Đại Từ, Thái Nguyên. Hồ có cảnh quan mặt nước giữa đồi núi, phù hợp với nội dung khám phá thiên nhiên.	Bạn có biết? Hồ Vai Miếu nằm dưới chân dãy Tam Đảo, thuộc khu vực Ký Phú, Đại Từ, Thái Nguyên.	4.70	17	ACTIVE	CUSTOM
116	49	109	Hồ Núi Cốc	21.579444	105.693889	Du khách có thể trải nghiệm ngồi thuyền thúng, khám phá động Thế giới Cổ tích và Âm phủ, động Ba Cây Thông, tham quan quần thể Thuyết Nhân Quả - Chùa Thiêng Thác Vàng, ngôi chùa nằm trong lòng Phật lớn nhất miền Bắc được ghi nhận kỷ lục Guniess.	Bạn có biết? Hồ Núi Cốc — Du khách có thể trải nghiệm ngồi thuyền thúng, khám phá động Thế giới Cổ tích và Âm phủ, động Ba Cây Thông, tham quan quần thể Thuyết Nhân Quả - Chùa Thiêng Thác Vàng, ngôi chùa nằm trong lòng Phật lớn nhất miền Bắc được ghi nhận kỷ lục Guniess.	4.20	1723	ACTIVE	CUSTOM
113	48	108	Bãi biển Cồn Đen	20.487412	106.603850	hình ảnh đẹp nhất của biển và sự hoang sơ của khu rừng nguyên sinh. Đặc biệt, vào thời điểm bình minh và hoàng hôn thì vẻ đẹp nơi đây lại trở nên quyến rũ đến kỳ lạ, lúc này cũng là khoảnh khắc được nhiều người săn đón nhất. Với vẻ đẹp trong lành từ không khí cho đến mặt nước biển trong xanh cùng bãi cát trắng thì đây đích thị là một background cực xịn	Bạn có biết? Bãi biển Cồn Đen — hình ảnh đẹp nhất của biển và sự hoang sơ của khu rừng nguyên sinh.	3.50	366	ACTIVE	CUSTOM
112	48	108	Bãi biển Đồng Châu	20.400278	106.576111	Còn giữ nguyên vẻ đẹp hoang sơ thì hãy đến với bãi biển Đồng Châu. Với bờ cát trải dài 5km ôm lấy mặt biển mênh mông, xanh mướt đã vẻ nên một địa điểm du lịch độc đáo tại Bắc Trung Bộ. hững chiếc chòi canh nhấp nhô trên mặt nước tạo nên cảnh sắt thật là hữu tình.	Bạn có biết? Bãi biển Đồng Châu — Còn giữ nguyên vẻ đẹp hoang sơ thì hãy đến với bãi biển Đồng Châu.	4.20	1299	ACTIVE	CUSTOM
110	54	112	Vườn quốc gia Lò Gò - Xa Mát	11.590950	105.885937	tới đây, du khách có thể ngắm nhìn các loài động, thực vật quý hiếm mà bạn khó có cơ hội được nhìn ngắm ngoài đời. Có rất nhiều những loài động vật quý hiếm được liệt kê vào sách đỏ. Ngoài ra, du khách cũng có thể trải nghiệm rất nhiều những hoạt động như đạp xe xuyên rừng, khám phá rừng bằng đường sông,…	Bạn có biết? Vườn quốc gia Lò Gò - Xa Mát — tới đây, du khách có thể ngắm nhìn các loài động, thực vật quý hiếm mà bạn khó có cơ hội được nhìn ngắm ngoài đời.	4.30	332	ACTIVE	CUSTOM
108	54	109	Hồ Dầu Tiếng	11.384170	106.363610	Hồ chứa nước Dầu Tiếng trên lưu vực sông Sài Gòn, trải trên địa bàn Tây Ninh, Bình Dương và Bình Phước theo địa giới của bộ dữ liệu gốc. Hồ phục vụ thủy lợi và có cảnh quan mặt nước rộng.	Bạn có biết? Hồ Dầu Tiếng là hồ chứa nước trên lưu vực sông Sài Gòn và phục vụ thủy lợi.	4.50	1245	ACTIVE	CUSTOM
106	54	110	Núi Bà Đen	11.381431	106.171175	núi Bà Đen còn được biết đến như “nóc nhà” của Nam Bộ với đỉnh cao nhất là 986 m, diện tích 24 km², Nếu yêu thích vẻ đẹp bao la của đất trời, muốn chiêm ngưỡng cảnh đẹp từ trên cao thì có thể lựa chọn di chuyển bằng cáp treo từ chân núi lên đỉnh núi.	Bạn có biết? núi Bà Đen còn được biết đến như “nóc nhà” của Nam Bộ với đỉnh cao nhất là 986 m, diện tích 24 km², Nếu yêu thích vẻ đẹp bao la của đất trời, muốn chiêm ngưỡng cảnh đẹp từ trên cao thì có thể lựa chọn di chuyển bằng cáp treo từ chân núi lên đỉnh núi.	4.60	7951	ACTIVE	CUSTOM
104	45	109	Thác Dải Yếm	20.820206	104.591076	Thác cao hơn 100m, chia thành 2 tầng chính. Trong đó tầng thứ nhất có tổng cộng 9 bậc, tầng thứ hai có 5 bậc. Vào độ từ tháng 4 đến tháng 10 hàng năm, khi lượng nước đổ về nhiều sẽ tạo nên những dòng chạy mạnh mẽ, tung bọt trắng xóa rất đẹp mắt	Bạn có biết? Thác Dải Yếm — Thác cao hơn 100m, chia thành 2 tầng chính.	4.20	1916	ACTIVE	CUSTOM
103	45	112	Rừng thông bản Áng	20.828378	104.628097	Khu rừng có tổng diện tích lên đến 43ha với bầu không khí trong lành và khung cảnh thông xanh trập trùng trông rất mát mắt. Bạn có thể trải nghiệm cắm trại qua đêm tại rừng thông bản Áng để thỏa sức hít thở khí trời se lạnh và đón mình bình.	Bạn có biết? Rừng thông bản Áng — Khu rừng có tổng diện tích lên đến 43ha với bầu không khí trong lành và khung cảnh thông xanh trập trùng trông rất mát mắt.	4.20	610	ACTIVE	CUSTOM
102	45	111	Ngũ động Bản Ôn	20.873159	104.711746	Đây là hệ thống gồm 5 hang động được người dân địa phương phát hiện lần đầu tiên vào năm 2006. Vì tại đây chưa có nhiều dịch vụ du lịch nên những hang động vẫn giữ được vẻ đẹp hoang sơ, huyền bí. Bên trong hang được điểm xuyến bởi những khối đá nhũ thạch khổng lồ với hình thù độc đáo.	Bạn có biết? Ngũ động Bản Ôn — Đây là hệ thống gồm 5 hang động được người dân địa phương phát hiện lần đầu tiên vào năm 2006.	4.20	206	ACTIVE	CUSTOM
101	45	112	Mộc Châu	20.844986	104.637836	với đặc sản là những mùa hoa ban, hoa anh đào, hoa mận, hoa cải vàng… tràn ngập hương sắc. Mộc Châu cũng là cái tên kinh điển nhất khi nhắc đến du lịch vùng Tây Bắc với cung đường phượt đẹp như tranh vẽ	Bạn có biết? Mộc Châu — với đặc sản là những mùa hoa ban, hoa anh đào, hoa mận, hoa cải vàng… tràn ngập hương sắc.	\N	0	ACTIVE	CUSTOM
99	44	108	Biển Hồ Bể	9.353386	106.160846	Khu vực này được hình thành do sự bãi bồi và quá trình xâm lấn của nước biển, làm nên một vùng hồ trũng rộng lớn. Bãi biển hồ này còn được thiên nhiên ưu ái bằng một bờ cát trắng dài hàng cây số. Do chưa đưa vào khai thác du lịch mạnh, bãi biển vẫn giữ được nét hoang sơ, tự nhiên vốn có mang lại sự yên bình, tự do cho du khách	Bạn có biết? Biển Hồ Bể — Khu vực này được hình thành do sự bãi bồi và quá trình xâm lấn của nước biển, làm nên một vùng hồ trũng rộng lớn.	3.80	939	ACTIVE	CUSTOM
98	44	112	Vườn cò Tân Long	9.539533	105.644355	Vườn cò Tân Long tại khu vực Ngã Năm, Sóc Trăng, là điểm tham quan có nhiều chim cò trú ngụ trong vườn cây.	Bạn có biết? Vườn cò Tân Long tại khu vực Ngã Năm, Sóc Trăng, là điểm tham quan có nhiều chim cò trú ngụ trong vườn cây.	3.90	118	ACTIVE	CUSTOM
95	43	110	Đỉnh Cu Vơ	16.701520	106.656808	Rất thích hợp cho nhóm bạn đi phượt. Cực kì thu hút các bạn trẻ đến để săn mây. Vào buổi sáng sớm, đứng ở ngọn núi, mở ra trước mắt bạn là những tầng mây trắng. Khoảnh khắc ánh nắng mặt trời rọi xuống thực sự đẹp biết bao.	Bạn có biết? Đỉnh Cu Vơ — Rất thích hợp cho nhóm bạn đi phượt.	4.80	102	ACTIVE	CUSTOM
93	43	108	Bãi biển Cửa Tùng	17.022827	107.110430	Mùa hè đến bãi biển này, bạn sẽ thấy được nét đẹp bình yên của biển xanh, cát trắng. Khung cảnh tĩnh lặng vào buổi chiều hoàng hôn khiến bao trái tim bị mê hoặc.	Bạn có biết? Bãi biển Cửa Tùng — Mùa hè đến bãi biển này, bạn sẽ thấy được nét đẹp bình yên của biển xanh, cát trắng.	4.30	815	ACTIVE	CUSTOM
90	42	111	Động Thiên Cung (Hạ Long)	20.873300	107.075000	Hang động trong khu vực vịnh Hạ Long, Quảng Ninh, có nhiều khối thạch nhũ và không gian hang đá vôi. Địa danh khác với động Thiên Cung tại Suối Giàng.	Bạn có biết? Động Thiên Cung tại vịnh Hạ Long khác với động cùng tên ở Suối Giàng.	\N	0	ACTIVE	CUSTOM
87	42	108	Biển Bãi Cháy	20.943056	107.029141	Bãi Cháy luôn thu hút rất nhiều du khách đến tham quan trải nghiệm và tắm biển. Bãi biển cùng đã có thêm những hoạt động thể thao trên biển như cano, dù bay hay nhà phao để du khách có thể tận hưởng những dịch vụ cao cấp hơn.	Bạn có biết? Biển Bãi Cháy — Bãi Cháy luôn thu hút rất nhiều du khách đến tham quan trải nghiệm và tắm biển.	4.30	2332	ACTIVE	CUSTOM
86	42	111	Vịnh Hạ Long	20.900000	107.200000	Là một trong những di sản thiên nhiên thế giới được UNESCO công nhận. Với diện tích 1553km2, gần 2000 hòn đảo lớn nhỏ, vịnh Hạ Long được thiên nhiên ưu ái có hệ thống hang động và đảo đá vừa đa dạng lại tuyệt đẹp.	Bạn có biết? Vịnh Hạ Long có gần 2.000 đảo lớn nhỏ và nhiều hang động đá vôi.	4.60	8478	ACTIVE	CUSTOM
85	41	110	Đèo Vi Ô Lắc	14.762611	108.520991	Nằm cao hơn 1300m so với mực nước biển, sự quanh co uốn lượn của các cung đường cùng với không khí thơ mộng luôn mang đến nhưng vẻ đẹp tuyệt mỹ cùng hàng cỏ xanh tươi mọc hai bên đường khi du khách đi lên đèo là nhưng nét ấn tượng nhất, thú vị nhất	Bạn có biết? Đèo Vi Ô Lắc — Nằm cao hơn 1300m so với mực nước biển, sự quanh co uốn lượn của các cung đường cùng với không khí thơ mộng luôn mang đến nhưng vẻ đẹp tuyệt mỹ cùng hàng cỏ xanh tươi mọc hai bên đường khi du khách đi lên đèo là nhưng nét ấn tượng nhất, thú vị nhất.	4.70	279	ACTIVE	CUSTOM
84	41	109	Thác Trắng	14.906573	108.658744	Khùng cảnh ở đây ấn tượng với rừng núi hoang sơ, trong lành, dòng thác đổ xuống giữa khung cảnh bao quanh là biết bao cây cối sông suối tung bọt trắng xóa	Bạn có biết? Thác Trắng — Khùng cảnh ở đây ấn tượng với rừng núi hoang sơ, trong lành, dòng thác đổ xuống giữa khung cảnh bao quanh là biết bao cây cối sông suối tung bọt trắng xóa.	4.10	854	ACTIVE	CUSTOM
82	41	108	Bãi biển Mỹ Khê (Quảng Ngãi)	15.183864	108.894533	Bãi biển Mỹ Khê thuộc khu vực Sơn Tịnh, Quảng Ngãi, có bãi cát dài ven biển và cảnh quan làng chài. Đây là địa danh khác với bãi biển Mỹ Khê tại Đà Nẵng.	Bạn có biết? Bãi biển Mỹ Khê ở Quảng Ngãi khác với bãi biển cùng tên tại Đà Nẵng.	4.50	565	ACTIVE	CUSTOM
81	41	110	Núi Thiên Ấn	15.149357	108.818196	Là một ngọn núi từ lâu đã rất nổi tiếng với khung cảnh hùng vĩ và mang lại một giá trị lịch sử vô cùng lớn lao, với vẻ đẹp thơ mộng và dịu dàng mà nó mang lại là điểm đặc biệt hu hút khách du lịch hàng năm.	Bạn có biết? Núi Thiên Ấn là một ngọn núi từ lâu đã rất nổi tiếng với khung cảnh hùng vĩ và mang lại một giá trị lịch sử vô cùng lớn lao, với vẻ đẹp thơ mộng và dịu dàng mà nó mang lại là điểm đặc biệt hu hút khách du lịch hàng năm.	\N	0	ACTIVE	CUSTOM
79	40	110	Đỉnh Quế	15.836445	107.382082	Đỉnh Quế thuộc khu vực Tây Giang, Quảng Nam, nổi bật với cảnh quan núi Trường Sơn và hoạt động ngắm mây. Khu vực xung quanh gắn với đời sống văn hóa của cộng đồng Cơ Tu.	Bạn có biết? Đỉnh Quế thuộc khu vực Tây Giang, Quảng Nam, nổi bật với cảnh quan núi Trường Sơn và hoạt động ngắm mây.	4.40	610	ACTIVE	CUSTOM
77	40	108	Bãi biển An Bàng	15.913607	108.340649	Sở hữu vẻ đẹp yên bình, hoang sơ với những bãi các vàng, biển xanh, khí hậu vô cùng mát mẻ và trong lành.	Bạn có biết? Bãi biển An Bàng — Sở hữu vẻ đẹp yên bình, hoang sơ với những bãi các vàng, biển xanh, khí hậu vô cùng mát mẻ và trong lành.	4.30	20559	ACTIVE	CUSTOM
75	39	111	Sông Chày	17.574140	106.252054	Là điểm đến lý tưởng cho những người yêu thích phiêu lưu, Sông Chày kết hợp giữa vẻ đẹp sông nước và hang động.	Bạn có biết? Sông Chày là điểm đến lý tưởng cho những người yêu thích phiêu lưu, Sông Chày kết hợp giữa vẻ đẹp sông nước và hang động.	4.30	64	ACTIVE	CUSTOM
72	39	108	Bãi Đá Nhảy	17.660749	106.515920	Là nơi nổi bật với những khối đá có hình thù độc đáo bên bờ biển.	Bạn có biết? Bãi Đá Nhảy là nơi nổi bật với những khối đá có hình thù độc đáo bên bờ biển.	4.70	24	ACTIVE	CUSTOM
71	39	111	Động Phong Nha	17.581325	106.283125	Phong Nha - Kẻ Bàng, được UNESCO công nhận là Di sản Thiên nhiên Thế giới.	Bạn có biết? Động Phong Nha thuộc khu vực Phong Nha - Kẻ Bàng, Di sản Thiên nhiên Thế giới.	4.70	2960	ACTIVE	CUSTOM
69	38	109	Suối nước nóng Triêm Đức	13.353243	109.057657	Là một điểm đến thú vị để thư giãn và ngâm mình trong làn nước khoáng ấm áp.	Bạn có biết? Suối nước nóng Triêm Đức là một điểm đến thú vị để thư giãn và ngâm mình trong làn nước khoáng ấm áp.	4.40	10	ACTIVE	CUSTOM
219	52	101	Bảo tàng Văn hóa Khmer	9.916186	106.305017	Bảo tàng trưng bày các hiện vật, tư liệu về đời sống, văn hóa, nghệ thuật và lịch sử của người Khmer Nam Bộ, giúp du khách hiểu thêm về cộng đồng dân tộc này.	Bạn có biết? Bảo tàng Văn hóa Khmer — Bảo tàng trưng bày các hiện vật, tư liệu về đời sống, văn hóa, nghệ thuật và lịch sử của người Khmer Nam Bộ, giúp du khách hiểu thêm về cộng đồng dân tộc này.	4.50	402	ACTIVE	CUSTOM
262	63	107	Làng hoa Sa Đéc	10.315355	105.749957	Làng nghề truyền thống nổi tiếng ở Đồng Tháp, nơi trồng hàng trăm loài hoa đẹp và quý. Làng hoa thu hút du khách với những vườn hoa rực rỡ quanh năm.	Bạn có biết? Làng hoa Sa Đéc trồng hàng trăm loài hoa và cây cảnh.	4.40	4091	ACTIVE	CUSTOM
284	18	101	Hoàng thành Thăng Long	21.032500	105.839720	Hoàng Thành Thăng Long (Hà Nội) là di tích lịch sử quan trọng của Việt Nam, từng là trung tâm quyền lực của nhiều triều đại. Di tích này mang đậm dấu ấn lịch sử và văn hóa dân tộc.	Bạn có biết? Hoàng thành Thăng Long từng là trung tâm quyền lực qua nhiều triều đại.	4.40	19575	ACTIVE	CUSTOM
315	21	102	Đình làng Nôm	20.992804	106.081201	Đình làng Nôm (Hưng Yên) là một di tích văn hóa đặc sắc, nổi bật với kiến trúc cổ truyền và các hoạt động lễ hội dân gian. Đây là điểm đến thu hút du khách khám phá những giá trị văn hóa truyền thống.	Bạn có biết? Đình làng Nôm (Hưng Yên) là một di tích văn hóa đặc sắc, nổi bật với kiến trúc cổ truyền và các hoạt động lễ hội dân gian.	4.70	53	ACTIVE	CUSTOM
314	21	102	Đền Phù Ủng	20.884782	106.143608	Đền Phù Ủng (Hưng Yên) là di tích lịch sử, thờ Đức thánh Phù Ủng, người có công giúp vua Hùng trong các cuộc chiến. Đền nằm trong khuôn viên rộng lớn, với không gian thanh tịnh và linh thiêng.	Bạn có biết? Đền Phù Ủng (Hưng Yên) là di tích lịch sử, thờ Đức thánh Phù Ủng, người có công giúp vua Hùng trong các cuộc chiến.	4.60	145	ACTIVE	CUSTOM
\.


--
-- Data for Name: card; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.card (card_id, place_id, name, image_key, description, status) FROM stdin;
24	54	Vườn quốc gia Cúc Phương	https://thumb.wikimedia.org/wikipedia/commons/thumb/9/98/Forest_in_Cuc_Phuong_National_Park_%2815706323528%29.jpg/960px-Forest_in_Cuc_Phuong_National_Park_%2815706323528%29.jpg	Là vườn quốc gia đầu tiên của Việt Nam với hệ động thực vật phong phú, những cây cổ thụ nghìn năm tuổi và các hang động tiền sử.	ACTIVE
22	52	Tam Cốc	https://thumb.wikimedia.org/wikipedia/commons/thumb/e/ef/Tam_Coc_%283%29.jpg/960px-Tam_Coc_%283%29.jpg	Được mệnh danh là "vịnh Hạ Long trên cạn", với cảnh sắc thiên nhiên tuyệt đẹp của núi đá vôi, cánh đồng lúa, và dòng sông Ngô Đồng.	ACTIVE
2	17	Đỉnh Pu Ta Leng	\N	Đỉnh núi cao thứ hai Việt Nam (3.049m), thuộc dãy Hoàng Liên Sơn, được mệnh danh là "nóc nhà thứ hai" của Đông Dương.	DRAFT
122	312	Chùa Chuông	\N	Chùa Chuông là ngôi chùa thuộc quần thể di tích Phố Hiến ở Hưng Yên, gắn với kiến trúc Phật giáo và sinh hoạt tín ngưỡng của địa phương.	DRAFT
106	291	Đảo Cò Chi Lăng Nam	\N	Đảo Cò Chi Lăng Nam (Hải Dương) là một khu vực bảo tồn thiên nhiên nổi bật với hàng nghìn con cò sinh sống. Đây là địa điểm du lịch sinh thái lý tưởng, với không gian yên bình và cảnh quan tuyệt đẹp.	DRAFT
107	292	Chùa Côn Sơn	\N	Chùa Côn Sơn (Hải Dương) là di tích lịch sử và văn hóa, nổi bật với kiến trúc cổ kính và không gian thanh tịnh. Nơi đây là một trong những địa điểm du lịch tâm linh nổi tiếng ở miền Bắc.	DRAFT
108	293	Làng rối nước Thanh Hải	\N	Làng rối nước Thanh Hải (Hải Dương) là nơi bảo tồn và phát triển nghệ thuật rối nước truyền thống. Du khách có thể thưởng thức những màn rối đặc sắc trong không gian văn hóa dân gian đậm đà.	DRAFT
110	295	Chợ Đông Ngô Quyền	\N	Chợ Đông Ngô Quyền là chợ tại khu vực thành phố Hải Dương theo địa giới cũ, phục vụ giao thương và nhu cầu mua sắm của người dân địa phương.	DRAFT
113	298	Bến K15 (Đồ Sơn)	\N	Bến K15 tại Đồ Sơn, Hải Phòng, là di tích gắn với các chuyến tàu không số và tuyến vận tải Hồ Chí Minh trên biển. Bến Nghiêng là một di tích khác ở gần đó.	DRAFT
114	299	Hải đăng Hòn Dấu	\N	Hải đăng Hòn Dấu (Hải Phòng) là ngọn hải đăng cổ kính, nằm trên đảo Hòn Dấu. Đây là một trong những ngọn hải đăng quan trọng ở khu vực vịnh Bắc Bộ.	DRAFT
117	307	Mai Châu	\N	Mai Châu (Hòa Bình) là một thung lũng xinh đẹp, nổi tiếng với những ngôi nhà sàn và cảnh sắc thiên nhiên hoang sơ. Mai Châu thu hút du khách bởi không khí trong lành, cộng với nền văn hóa đặc sắc của đồng bào dân tộc Thái.	DRAFT
118	308	Động Đá Bạc	\N	Động Đá Bạc là hang động đá vôi tại khu vực Lương Sơn, Hòa Bình, có các khối thạch nhũ và cảnh quan hang tự nhiên.	DRAFT
119	309	Cửu thác Tú Sơn	\N	Cửu thác Tú Sơn là khu cảnh quan thác, suối và rừng tại khu vực Tú Sơn, Kim Bôi, Hòa Bình, gắn với không gian văn hóa Mường.	DRAFT
120	310	Đền Thác Bờ	\N	Đền Thác Bờ (Hòa Bình) là một di tích lịch sử, thờ nữ tướng Cao Thị Lịch. Đền nằm gần thác Bờ, có khung cảnh huyền bí và là nơi linh thiêng, thu hút du khách đến tham quan và cầu nguyện.	DRAFT
121	311	Phố Hiến	\N	Phố Hiến (Hưng Yên) là một khu phố cổ nổi tiếng với những công trình kiến trúc mang đậm dấu ấn lịch sử. Đây là một điểm đến hấp dẫn cho du khách muốn tìm hiểu về lịch sử, văn hóa và thưởng thức ẩm thực địa phương.	DRAFT
123	313	Làng nghề đúc đồng Lộng Thượng	\N	Làng nghề đúc đồng Lộng Thượng tại khu vực Đại Đồng, Văn Lâm, Hưng Yên, nổi tiếng với đồ thờ cúng và các sản phẩm đồng thủ công.	DRAFT
124	314	Đền Phù Ủng	\N	Đền Phù Ủng (Hưng Yên) là di tích lịch sử, thờ Đức thánh Phù Ủng, người có công giúp vua Hùng trong các cuộc chiến. Đền nằm trong khuôn viên rộng lớn, với không gian thanh tịnh và linh thiêng.	DRAFT
125	315	Đình làng Nôm	\N	Đình làng Nôm (Hưng Yên) là một di tích văn hóa đặc sắc, nổi bật với kiến trúc cổ truyền và các hoạt động lễ hội dân gian. Đây là điểm đến thu hút du khách khám phá những giá trị văn hóa truyền thống.	DRAFT
21	51	Thuyền khám phá Tràng An	data:image/jpeg;base64,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oAwPiD/yIXiT/sGXP/opqwPgR/ySnQ/+2/8A6Pkrf+IP/IheJP8AsGXP/opqwPgR/wAkp0P/ALb/APo+SvMf+/x/wP8A9KR3r/c3/iX5M76iiivTOAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigDA+IP/IheJP+wZc/+imrA+BH/JKdD/7b/wDo+St/4g/8iF4k/wCwZc/+imrA+BH/ACSnQ/8Atv8A+j5K8x/7/H/A/wD0pHev9zf+JfkzvqKKK9M4AooooAwPiD/yIXiT/sGXP/opqwPgR/ySnQ/+2/8A6Pkrf+IP/IheJP8AsGXP/opqwPgR/wAkp0P/ALb/APo+SvMf+/x/wP8A9KR3r/c3/iX5M76iiivTOAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigDA+IP/IheJP+wZc/+imrA+BH/JKdD/7b/wDo+St/4g/8iF4k/wCwZc/+imrA+BH/ACSnQ/8Atv8A+j5K8x/7/H/A/wD0pHev9zf+JfkzvqKKK9M4AooooAwPiD/yIXiT/sGXP/opqwPgR/ySnQ/+2/8A6Pkrf+IP/IheJP8AsGXP/opqwPgR/wAkp0P/ALb/APo+SvMf+/x/wP8A9KR3r/c3/iX5M76iiivTOAKKKKACiiigAooooAKKK8L+MP7XXhn4MeMW8OarpGrXt2sEdx5tmsRj2vnA+Zwc8elVGEpu0Uc9fEUsNDnrSsj3SivlT/h4l4H/AOhc8Qf98Qf/AByuk8Hftn+HviDZeLDouhanHd6FoN3rhS/MaRyrAF/d7lZiCSw5x0zWjoVFq0cMc1wU3yxqJv5n0PRXhP7NH7S8/wC0Bd6/DNoEeiDS0gcGO6M3meYXHdFxjZ79a92rOUXB8stztw+Ip4qm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	Thuyền khám phá vùng núi đá vôi và di sản Tràng An.	ACTIVE
31	86	Thuyền buồm Hạ Long	data:image/jpeg;base64,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	Thẻ văn hóa biển và cảnh quan vịnh Hạ Long.	ACTIVE
103	283	Khuê Văn Các	data:image/jpeg;base64,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+Crfw3ekX1qlotpIxXZvAXG4DJweMjng1maV8OIrbwnNouoahNfvJdPeC/UeVMkpfesinJwwbnPf0wcVxLDVYxjTS0t9z0uvR/5+R1vEU5Sc29b/AIa2fqv66mzr2gWF/wCHLjT5LeOK1SI+WEUL5JAyrJj7pUgEEdMVx/hbU7nWtb8E312c3U+hXDyNj7zFoMt+PX8a2W0rWNamvdG1DVJf7PjRBJNDYeQ9yjg5US+YwPAw21FI3DBGa1v+EZij17TNQgdYIbCzls0tVj42sYyMHPAAjxjHf2rplCVWSnGNkrdukl+Wv3mEZRpxcZO+/fqv1NmiiivSOEKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAwPiD/yIXiT/sGXP/opqwPgR/ySnQ/+2/8A6Pkrf+IP/IheJP8AsGXP/opqwPgR/wAkp0P/ALb/APo+SvMf+/x/wP8A9KR3r/c3/iX5M76iiivTOAKKKKAMD4g/8iF4k/7Blz/6KasD4Ef8kp0P/tv/AOj5K3/iD/yIXiT/ALBlz/6KasD4Ef8AJKdD/wC2/wD6PkrzH/v8f8D/APSkd6/3N/4l+TO+ooor0zgCiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAwPiD/yIXiT/sGXP/opqwPgR/ySnQ/+2/8A6Pkrf+IP/IheJP8AsGXP/opqwPgR/wAkp0P/ALb/APo+SvMf+/x/wP8A9KR3r/c3/iX5M76iiivTOAKKKKAMD4g/8iF4k/7Blz/6KasD4Ef8kp0P/tv/AOj5K3/iD/yIXiT/ALBlz/6KasD4Ef8AJKdD/wC2/wD6PkrzH/v8f8D/APSkd6/3N/4l+TO+ooor0zgCiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKK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c/8AopqwPgR/ySnQ/wDtv/6PkrzH/v8AH/A//Skd6/3N/wCJfkzvqKKK9M4AooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKgubpbcY6uego3E2krsfNOkC5Y/QdzWXcXL3DZbgDoBTJJGlYsxyTTa1UbHFOo5adAoooqzIKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAfFM0LblOPb1rUt7pbgccP3WsilVijAg4I71LVzSE3E3KKrWl4Jvlbh/51ZrFqx2pqSugooooKCiiigAooooAKKKKACiiigAooooAKKKKACiiigDA+IP8AyIXiT/sGXP8A6KasD4Ef8kp0P/tv/wCj5K3/AIg/8iF4k/7Blz/6KasD4Ef8kp0P/tv/AOj5K8x/7/H/AAP/ANKR3r/c3/iX5M76iiivTOAKKKKAMD4g/wDIheJP+wZc/wDopqwPgR/ySnQ/+2//AKPkrf8AiD/yIXiT/sGXP/opqwPgR/ySnQ/+2/8A6PkrzH/v8f8AA/8A0pHev9zf+JfkzvqKKK9M4AooooAKKKKACiiigAooooAKKKKACiiigAooqC6uRbp6uego3E2krsS6uhbrgcueg9Ky2YuxZjknvQzF2LMck96StkrHDObmwoooqjMKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigABwcjrWlZ3nm4Rz8/Y+tZtAODkdalq5cZOLujdoqrZ3fnDY5+cfrVqsWrHdFqSugooooKCiiigAooooAKKKKACiiigAooooAKKKKAMD4g/8iF4k/wCwZc/+imrA+BH/ACSnQ/8Atv8A+j5K3/iD/wAiF4k/7Blz/wCimrA+BH/JKdD/AO2//o+SvMf+/wAf8D/9KR3r/c3/AIl+TO+ooor0zgCiiigDA+IP/IheJP8AsGXP/opqwPgR/wAkp0P/ALb/APo+St/4g/8AIheJP+wZc/8AopqwPgR/ySnQ/wDtv/6PkrzH/v8AH/A//Skd6/3N/wCJfkzvqKKK9M4AooooAKKKKACiiigAooooAKKKKACiikZgilicAUAMnmWCMsfwHrWRJI0rlmOSafczm4kyeAOgqKtY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	Dấu ấn truyền thống hiếu học tại Văn Miếu – Quốc Tử Giám.	ACTIVE
101	281	Lăng Chủ tịch Hồ Chí Minh	https://thumb.wikimedia.org/wikipedia/commons/thumb/2/28/Ho_Chi_Minh_Mausoleum%2C_Hanoi_%283%29_%2824622750848%29.jpg/960px-Ho_Chi_Minh_Mausoleum%2C_Hanoi_%283%29_%2824622750848%29.jpg	Lăng Chủ tịch Hồ Chí Minh (Hà Nội) là nơi lưu giữ thi hài của Chủ tịch Hồ Chí Minh, trở thành biểu tượng của lòng tôn kính và biết ơn của nhân dân Việt Nam.	ACTIVE
89	253	Bảo tàng chiến thắng Điện Biên Phủ	https://upload.wikimedia.org/wikipedia/commons/3/39/Captured_French_artillery_at_the_Dien_Bien_Phu_Museum.jpg	Bảo tàng lưu giữ các hiện vật, tư liệu và hình ảnh về chiến dịch Điện Biên Phủ, tái hiện lịch sử hào hùng của dân tộc Việt Nam. Đây là điểm đến không thể bỏ qua khi ghé thăm Điện Biên.	ACTIVE
109	294	Đền Kiếp Bạc	https://thumb.wikimedia.org/wikipedia/commons/thumb/e/ea/Ki%E1%BA%BFp_B%E1%BA%A1c_temple_in_1904.jpg/960px-Ki%E1%BA%BFp_B%E1%BA%A1c_temple_in_1904.jpg	Đền Kiếp Bạc (Hải Dương) là nơi thờ Hưng Đạo Đại Vương Trần Quốc Tuấn, người có công lớn trong chiến tranh chống Nguyên. Đền mang đậm giá trị lịch sử và văn hóa.	ACTIVE
96	276	Chùa Tam Chúc	https://thumb.wikimedia.org/wikipedia/commons/thumb/0/06/Tam_Chuc_Pagoda_-_Ha_Nam.jpg/960px-Tam_Chuc_Pagoda_-_Ha_Nam.jpg	Chùa Tam Chúc (Hà Nam) là một trong những quần thể chùa lớn nhất Việt Nam, nổi bật với kiến trúc hoành tráng và cảnh quan thiên nhiên tươi đẹp. Được bao quanh bởi những dãy núi, hồ nước trong xanh, đây là nơi thu hút tín đồ Phật giáo và du khách tham quan.	ACTIVE
51	151	Chùa Bổ Đà	https://thumb.wikimedia.org/wikipedia/commons/thumb/d/d7/NNU_Ch%C3%B9ab%E1%BB%95%C4%91%C3%A0.jpg/960px-NNU_Ch%C3%B9ab%E1%BB%95%C4%91%C3%A0.jpg	Là một trong những ngôi chùa cổ nổi tiếng và đặc sắc nhất vùng Kinh Bắc	ACTIVE
143	138	Nhà thờ Đức Bà Sài Gòn	https://thumb.wikimedia.org/wikipedia/commons/thumb/8/88/Saigon_Notre-Dame_Basilica.jpg/960px-Saigon_Notre-Dame_Basilica.jpg	Cũng nằm ngay trung tâm quận 1, nhà thờ Đức Bà là địa điểm du lịch thành phố Hồ Chí Minh mà bạn nhất định phải ghé đến. Nhà thờ Đức Bà là một công trình kiến trúc độc đáo mang đậm phong cách Châu Âu, là nơi sinh hoạt và tổ chức các buổi Thánh lễ cho những người theo đạo Công giáo ở Sài Thành.	ACTIVE
115	300	Cầu Hoàng Văn Thụ	https://thumb.wikimedia.org/wikipedia/commons/thumb/8/88/Hoang_Van_Thu_Bridge.jpg/960px-Hoang_Van_Thu_Bridge.jpg	Cầu Hoàng Văn Thụ (Hải Phòng) là một cây cầu huyết mạch nối liền các khu vực trong thành phố. Cầu nổi bật với kiến trúc hiện đại và là điểm đến tham quan chính của du khách.	ACTIVE
105	285	Cầu Long Biên	https://thumb.wikimedia.org/wikipedia/commons/thumb/c/cf/LONG_BIEN_BRIDGE_HANOI_VIETNAM_FEB_2012_%287011980509%29.jpg/960px-LONG_BIEN_BRIDGE_HANOI_VIETNAM_FEB_2012_%287011980509%29.jpg	Cầu Long Biên (Hà Nội) là một trong những cây cầu lâu đời nhất, mang đậm giá trị lịch sử. Cầu nối liền hai bờ sông Hồng và là biểu tượng của thủ đô.	ACTIVE
17	42	Đền Trần	\N	Nằm tại phường Lộc Vượng, thành phố Nam Định, đền Trần thờ các vua nhà Trần, gắn liền với lịch sử hào hùng của triều đại này.	DRAFT
18	43	Vườn quốc gia Xuân Thủy	\N	Đây là khu bảo tồn thiên nhiên nằm ở huyện Giao Thủy, thuộc vùng hạ lưu sông Hồng.	DRAFT
19	44	Chợ Viềng	\N	Là phiên chợ đặc biệt chỉ diễn ra một lần trong năm vào đêm mùng 7, rạng sáng mùng 8 tháng Giêng âm lịch.	DRAFT
20	45	Hồ Vị Xuyên	\N	Hồ nằm trong thành phố Nam Định, là một địa điểm lý tưởng để thư giãn, đi dạo, thưởng thức không gian xanh và yên bình.	DRAFT
23	53	Cố đô Hoa Lư	\N	Là kinh đô của nước Đại Cồ Việt dưới triều Đinh và Tiền Lê, Hoa Lư lưu giữ nhiều di tích lịch sử quan trọng	DRAFT
25	55	Hồ Đồng Chương	\N	Hồ Đồng Chương là một hồ nước tự nhiên nằm giữa rừng thông, rất yên bình và thơ mộng.	DRAFT
26	61	Đền Hùng	\N	Là khu di tích lịch sử quốc gia đặc biệt, nơi thờ các vua Hùng – những người dựng nước đầu tiên của dân tộc Việt Nam.	DRAFT
27	62	Làng cổ Hùng Lô	\N	Nổi tiếng với các ngôi nhà cổ hàng trăm năm tuổi, cùng các nghề truyền thống như làm bánh chưng, bánh giầy.	DRAFT
28	63	Đảo Ngọc Xanh	\N	Là khu du lịch sinh thái kết hợp nghỉ dưỡng, với cảnh quan thiên nhiên trong lành, công viên nước, trò chơi cảm giác mạnh và khu suối nước nóng	DRAFT
29	64	Vườn quốc gia Xuân Sơn	\N	Là một trong những khu bảo tồn thiên nhiên lớn nhất miền Bắc, nổi bật với hệ sinh thái rừng nhiệt đới và các loài động, thực vật phong phú	DRAFT
30	65	Suối nước nóng Thanh Thủy	\N	Là khu vực có các dịch vụ nghỉ dưỡng và trị liệu hiện đại.	DRAFT
32	87	Biển Bãi Cháy	\N	Bãi Cháy luôn thu hút rất nhiều du khách đến tham quan trải nghiệm và tắm biển. Bãi biển cùng đã có thêm những hoạt động thể thao trên biển như cano, dù bay hay nhà phao để du khách có thể tận hưởng những dịch vụ cao cấp hơn.	DRAFT
33	88	Sun World Hạ Long	\N	Du lịch Hạ Long thì chắc chắn không nên bỏ qua Sun World Halong Park - khu phức hợp giải trí nổi tiếng dành cho mọi lứa tuổi. Nếu bạn đã từng đến VinWonders thì công viên Hạ Long cũng là một thiên đường tương tự như vậy, nhưng sẽ náo nhiệt hơn nhiều nhé.	DRAFT
34	89	Làng chài Cửa Vạn	\N	Không chỉ được tận hưởng không gian yên tĩnh, thanh bình nơi đây, du khách còn có thêm những hiểu biết về đời sống văn hóa, thường nhật của các ngư dân. Ngư dân ở đây hết sức thân thiện và mến khách. Hành khách có thể chèo thuyền thúng, cắt cá, câu mực… để hiểu hơn cuộc sống dân chài.	DRAFT
35	90	Động Thiên Cung (Hạ Long)	\N	Hang động trong khu vực vịnh Hạ Long, Quảng Ninh, có nhiều khối thạch nhũ và không gian hang đá vôi. Địa danh khác với động Thiên Cung tại Suối Giàng.	DRAFT
37	102	Ngũ động Bản Ôn	\N	Đây là hệ thống gồm 5 hang động được người dân địa phương phát hiện lần đầu tiên vào năm 2006. Vì tại đây chưa có nhiều dịch vụ du lịch nên những hang động vẫn giữ được vẻ đẹp hoang sơ, huyền bí. Bên trong hang được điểm xuyến bởi những khối đá nhũ thạch khổng lồ với hình thù độc đáo.	DRAFT
38	103	Rừng thông bản Áng	\N	Khu rừng có tổng diện tích lên đến 43ha với bầu không khí trong lành và khung cảnh thông xanh trập trùng trông rất mát mắt. Bạn có thể trải nghiệm cắm trại qua đêm tại rừng thông bản Áng để thỏa sức hít thở khí trời se lạnh và đón mình bình.	DRAFT
39	104	Thác Dải Yếm	\N	Thác cao hơn 100m, chia thành 2 tầng chính. Trong đó tầng thứ nhất có tổng cộng 9 bậc, tầng thứ hai có 5 bậc. Vào độ từ tháng 4 đến tháng 10 hàng năm, khi lượng nước đổ về nhiều sẽ tạo nên những dòng chạy mạnh mẽ, tung bọt trắng xóa rất đẹp mắt	DRAFT
40	105	Đỉnh Tà Xùa	\N	Từ trên đỉnh, bạn có thể phóng tầm mắt chiêm ngưỡng khung cảnh hùng vĩ xung quanh với những dải ruộng bậc thang xanh mướt, thấp thoáng xa xa là mái lá khói bếp, tay có thể chạm đến mây trời.	DRAFT
61	166	Chùa Phật Tích	\N	Đây là một trong những ngôi chùa cổ, gắn liền với lịch sử Phật giáo Việt Nam.	DRAFT
41	111	Chùa Keo	\N	Chùa Keo ở Thái Bình được xếp hạng là ngôi chùa cổ đẹp nhất tại Việt Nam. Chùa được xây dựng theo kiến trúc cổ kín từ thời nhà Lê. Chính vì lẽ đó mà chùa Keo được xem là một trong những địa điểm check-in Thái Bình mà bạn nhất định phải đến tham quan.	DRAFT
42	112	Bãi biển Đồng Châu	\N	Còn giữ nguyên vẻ đẹp hoang sơ thì hãy đến với bãi biển Đồng Châu. Với bờ cát trải dài 5km ôm lấy mặt biển mênh mông, xanh mướt đã vẻ nên một địa điểm du lịch độc đáo tại Bắc Trung Bộ. hững chiếc chòi canh nhấp nhô trên mặt nước tạo nên cảnh sắt thật là hữu tình.	DRAFT
43	113	Bãi biển Cồn Đen	\N	hình ảnh đẹp nhất của biển và sự hoang sơ của khu rừng nguyên sinh. Đặc biệt, vào thời điểm bình minh và hoàng hôn thì vẻ đẹp nơi đây lại trở nên quyến rũ đến kỳ lạ, lúc này cũng là khoảnh khắc được nhiều người săn đón nhất. Với vẻ đẹp trong lành từ không khí cho đến mặt nước biển trong xanh cùng bãi cát trắng thì đây đích thị là một background cực xịn	DRAFT
44	114	Nhà thờ Bác Trạch	\N	Đây chính là một trong những nhà thờ lớn nhất Việt Nam hiện nay với hoa văn trang trí công phu theo lối kiến trúc Roman. Với vẻ đẹp này chắc chắn nhà thờ Bác Trạch là một địa điểm check-in Thái Bình độc đáo dành cho bạn.	DRAFT
45	115	Làng dệt chiếu Hới	\N	Nổi tiếng với những làng nghề truyền thống gần hàng trăm năm tuổi. bạn còn được hiểu thêm về cuộc sống giản dị của người dân trong làng. Và bạn sẽ được người dân giới thiệu về quy trình để tạo ra được một một chiếc chiếu truyền thống, từ thu hoạch chế biến cói cho tới khâu thành phẩm. Những trải nghiệm này sẽ là một kỷ niệm khó quên cho chuyến đi của bạn.	DRAFT
48	118	Hồ Vai Miếu	\N	Hồ Vai Miếu nằm dưới chân dãy Tam Đảo, thuộc khu vực Ký Phú, Đại Từ, Thái Nguyên. Hồ có cảnh quan mặt nước giữa đồi núi, phù hợp với nội dung khám phá thiên nhiên.	DRAFT
49	119	Động Linh Sơn	\N	Động Linh Sơn tỉnh Thái Nguyên có diện tích rộng rãi, vào mùa hè rất mát mẻ và mùa đông sẽ ấm áp vô cùng. Động nằm tại khu vực lưng chừng núi đá rộng lớn nên đã được người dân nơi đây lựa chọn xây dựng chùa thờ Phật. Cứ vào các ngày rằm, mùng một, người dân từ Thái Nguyên và nhiều tỉnh thành lân cận khác đều sẽ đến đây dâng hương lễ Phật rất náo nhiệt.	DRAFT
50	120	Thác Đát Đắng	\N	bạn sẽ có cảm giác hệt như đang lạc bước vào một không gian mới mẻ, được tránh xa hoàn toàn sự xô bồ và náo nhiệt nơi phố thị. Cảnh núi rừng hùng vĩ, dòng nước xanh trong mát lành chính là những thứ ấn tượng khiến bạn có cảm giác thật sự thư giãn và mong muốn được tận hưởng sự yên bình hiếm thấy này mãi không thôi.	DRAFT
62	167	Chùa Bút Tháp	\N	Chùa Bút Tháp nằm ở Bắc Ninh, là một ngôi chùa nổi tiếng với kiến trúc độc đáo và tượng Phật Bà Quan Âm.	DRAFT
63	168	Làng tranh Đông Hồ	\N	Làng tranh Đông Hồ ở Bắc Ninh là nơi sản xuất tranh dân gian nổi tiếng, với những bức tranh phản ánh đời sống nông thôn Việt Nam.	DRAFT
52	152	Thác Thùm Thùm	\N	Là một điểm đến hoang sơ và tuyệt đẹp giữa lòng thiên nhiên	DRAFT
53	153	Hồ Cấm Sơn	\N	Là một trong những hồ nước ngọt lớn nhất miền Bắc Việt Nam	DRAFT
54	154	Đền Trung	\N	Đây là một trong ba ngôi đền chính thờ các vua Hùng và gắn liền với truyền thuyết dựng nước của dân tộc Việt Nam	DRAFT
55	155	Hồ Khuôn Thần	\N	Là một điểm đến lý tưởng với vẻ đẹp thiên nhiên thơ mộng và không khí trong lành	DRAFT
59	159	Bản Pác Ngòi	\N	Đây là một bản làng của người Tày, nổi bật với nét văn hóa truyền thống	DRAFT
60	160	Động Nàng Tiên	\N	Đây là một hang động kỳ vĩ với thạch nhũ hình dạng đa dạng, gắn liền với truyền thuyết về nàng tiên xinh đẹp.	DRAFT
64	169	Đền bà Chúa Kho	\N	Đền Bà Chúa Kho ở Bắc Ninh là nơi thờ bà Chúa Kho, một trong những vị thần được thờ cúng linh thiêng nhất ở Việt Nam	DRAFT
65	170	Chợ Ninh Hiệp	\N	Chợ Ninh Hiệp là một trong những khu chợ nổi tiếng nhất miền Bắc, chuyên buôn bán các mặt hàng vải vóc, quần áo.	DRAFT
68	208	Núi Mắt Thần	\N	Ngọn núi độc đáo với một lỗ lớn ở giữa, được gọi là "Mắt Thần." Đây là điểm check-in nổi tiếng với cảnh quan núi non tuyệt đẹp.	DRAFT
69	209	Di tích rừng Trần Hưng Đạo	\N	Nơi thành lập Đội Việt Nam Tuyên Truyền Giải Phóng Quân năm 1944, khởi nguồn của Quân đội Nhân dân Việt Nam. Khu di tích có giá trị lịch sử to lớn.	DRAFT
70	210	Làng rèn Phúc Sen	\N	Làng nghề truyền thống nổi tiếng với sản phẩm dao kéo chất lượng cao. Du khách có thể tham quan quy trình rèn và mua sắm các sản phẩm thủ công.	DRAFT
71	221	Thác Bản Ba	\N	Thác Bản Ba thuộc xã Trung Hà, huyện Chiêm Hóa, là một cụm thác với 3 tầng chính đổ xuống từ độ cao khoảng 40 m. Thác được bao quanh bởi rừng núi hoang sơ, nước trong xanh và khí hậu mát mẻ quanh năm. Đây là điểm đến lý tưởng để hòa mình vào thiên nhiên.	DRAFT
72	222	Hồ Thủy Điện Tuyên Quang	\N	Hồ Na Hang được ví như "Hạ Long trên núi", nổi bật với mặt hồ trong xanh, các đảo núi đá vôi trập trùng, và rừng nguyên sinh bao quanh. Du khách có thể khám phá các địa danh như thác Khuổi Nhi, đền Pác Tạ, và tham gia du thuyền ngắm cảnh.	DRAFT
73	223	Đền Hạ (Tuyên Quang)	\N	Đền Hạ, còn gọi là đền Hiệp Thuận hoặc đền Tam Kỳ, là di tích thờ Mẫu tại khu vực trung tâm Tuyên Quang. Đền có liên hệ lễ hội với đền Thượng và đền Mẫu Ỷ La.	DRAFT
74	224	Suối khoáng Mỹ Lâm	\N	Suối khoáng Mỹ Lâm nổi tiếng với nguồn nước khoáng nóng tự nhiên, có tác dụng chữa bệnh và thư giãn. Đây là khu du lịch nghỉ dưỡng hấp dẫn, kết hợp giữa trị liệu sức khỏe và cảnh sắc thiên nhiên tươi đẹp.	DRAFT
75	225	Thác Khuổi Nhi	\N	hác Khuổi Nhi thuộc khu vực Na Hang, là một trong những thác nước đẹp nhất ở Tuyên Quang. Nước thác trắng xóa đổ xuống từ độ cao lớn, hòa quyện với thiên nhiên hùng vĩ tạo nên cảnh sắc thơ mộng.	DRAFT
77	231	Tam Đảo	\N	Tam Đảo là khu nghỉ dưỡng nổi tiếng với khí hậu mát mẻ quanh năm, tọa lạc trên dãy núi Tam Đảo. Nơi đây có nhiều điểm tham quan như Tháp Truyền Hình, Nhà Thờ Đá, và chợ đêm Tam Đảo. Du khách có thể tận hưởng cảnh đẹp núi non và ẩm thực địa phương.	DRAFT
78	232	Hồ Đại Lải	\N	Hồ Đại Lải là một hồ nước nhân tạo rộng lớn, được bao quanh bởi rừng thông và núi đồi. Đây là điểm đến lý tưởng để tham gia các hoạt động như chèo thuyền, cắm trại, câu cá hoặc nghỉ dưỡng tại các khu resort ven hồ.	DRAFT
79	233	Làng gốm Hương Canh	\N	Làng gốm Hương Canh là một làng nghề truyền thống nổi tiếng với các sản phẩm gốm thủ công tinh xảo, đặc biệt là các sản phẩm gốm gia dụng và nghệ thuật. Du khách có thể tham quan và trải nghiệm làm gốm tại đây.	DRAFT
80	234	Khu du lịch FLC Vĩnh Thịnh	\N	FLC Vĩnh Thịnh là khu nghỉ dưỡng cao cấp với không gian sang trọng, tiện nghi. Nơi đây có các dịch vụ đa dạng như bể bơi, spa, sân golf, và nhà hàng, phù hợp cho những chuyến nghỉ dưỡng cùng gia đình.	DRAFT
81	235	Khu du lịch sinh thái Đầm Vạc	\N	Đầm Vạc là một trong những đầm tự nhiên lớn nhất tại Vĩnh Phúc, có hệ sinh thái đa dạng. Du khách có thể tham gia các hoạt động như chèo thuyền, ngắm cảnh, câu cá, hoặc nghỉ ngơi tại các resort ven đầm.	DRAFT
82	236	Ruộng bậc thang Mù Cang Chải	\N	bậc thang Mù Cang Chải là kiệt tác thiên nhiên và lao động của đồng bào dân tộc H'Mông, nổi tiếng với vẻ đẹp hùng vĩ, đặc biệt vào mùa lúa chín (tháng 9 - tháng 10). Đây là Di sản Quốc gia và một trong những điểm đến hấp dẫn nhất Việt Nam.	DRAFT
84	238	Suối Giàng	\N	Suối Giàng nổi tiếng với những cây chè Shan tuyết cổ thụ hàng trăm năm tuổi và khí hậu mát mẻ quanh năm. Đây là nơi lý tưởng để thưởng trà, tận hưởng không khí trong lành và tìm hiểu văn hóa của đồng bào người Mông.	DRAFT
86	240	Động Thiên Cung Suối Giàng	\N	Hang động tại khu vực Suối Giàng, Văn Chấn, Yên Bái, có cảnh quan đá vôi và thạch nhũ. Địa danh khác với động Thiên Cung ở vịnh Hạ Long.	DRAFT
87	251	Đồi A1	\N	Đồi A1 là một trong những di tích lịch sử quan trọng trong chiến dịch Điện Biên Phủ, nơi từng diễn ra các trận đánh khốc liệt. Hiện nay, nơi đây trở thành điểm tham quan với hầm chiến đấu, hệ thống giao thông hào và bảo tồn nhiều hiện vật lịch sử.	DRAFT
88	252	Hầm De Castries	\N	Đây là nơi ở và chỉ huy của tướng De Castries trong chiến dịch Điện Biên Phủ. Hầm được xây dựng kiên cố và hiện nay là di tích lịch sử được bảo tồn để tái hiện lại chiến trường năm xưa.	DRAFT
11	31	Cờ trên mây Fansipan	data:image/jpeg;base64,/9j/4AAQSkZJRgABAQEAYABgAAD/2wBDAAMCAgMCAgMDAwMEAwMEBQgFBQQEBQoHBwYIDAoMDAsKCwsNDhIQDQ4RDgsLEBYQERMUFRUVDA8XGBYUGBIUFRT/2wBDAQMEBAUEBQkFBQkUDQsNFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBT/wAARCAMgAoADASIAAhEBAxEB/8QAHwAAAQUBAQEBAQEAAAAAAAAAAAECAwQFBgcICQoL/8QAtRAAAgEDAwIEAwUFBAQAAAF9AQIDAAQRBRIhMUEGE1FhByJxFDKBkaEII0KxwRVS0fAkM2JyggkKFhcYGRolJicoKSo0NTY3ODk6Q0RFRkdISUpTVFVWV1hZWmNkZWZnaGlqc3R1dnd4eXqDhIWGh4iJipKTlJWWl5iZmqKjpKWmp6ipqrKztLW2t7i5usLDxMXGx8jJytLT1NXW19jZ2uHi4+Tl5ufo6erx8vP09fb3+Pn6/8QAHwEAAwEBAQEBAQEBAQAAAAAAAAECAwQFBgcICQoL/8QAtREAAgECBAQDBAcFBAQAAQJ3AAECAxEEBSExBhJBUQdhcRMiMoEIFEKRobHBCSMzUvAVYnLRChYkNOEl8RcYGRomJygpKjU2Nzg5OkNERUZHSElKU1RVVldYWVpjZGVmZ2hpanN0dXZ3eHl6goOEhYaHiImKkpOUlZaXmJmaoqOkpaanqKmqsrO0tba3uLm6wsPExcbHyMnK0tPU1dbX2Nna4uPk5ebn6Onq8vP09fb3+Pn6/9oADAMBAAIRAxEAPwD9LaKKK8o6wooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAjuLiK0t5Z55UhgiUvJJIwVUUDJJJ4AA71if8LB8Lf9DJpH/gfF/wDFUfEH/kQvEn/YMuf/AEU1eZfCb4TeFPE3w/0rUtS0r7Tez+b5kv2iVN22V1HCuB0AHSvNr166rqjRSbab1b7pdE+53UqNJ0nVqt720t2v1PTf+Fg+Fv8AoZNI/wDA+L/4qj/hYPhb/oZNI/8AA+L/AOKrA/4UR4G/6Af/AJNz/wDxdH/CiPA3/QD/APJuf/4ulfH/AMsPvl/kO2D7y+5f5m//AMLB8Lf9DJpH/gfF/wDFUf8ACwfC3/QyaR/4Hxf/ABVYH/CiPA3/AEA//Juf/wCLo/4UR4G/6Af/AJNz/wDxdF8f/LD75f5BbB95fcv8zf8A+Fg+Fv8AoZNI/wDA+L/4qj/hYPhb/oZNI/8AA+L/AOKrA/4UR4G/6Af/AJNz/wDxdH/CiPA3/QD/APJuf/4ui+P/AJYffL/ILYPvL7l/mb//AAsHwt/0Mmkf+B8X/wAVR/wsHwt/0Mmkf+B8X/xVYH/CiPA3/QD/APJuf/4uj/hRHgb/AKAf/k3P/wDF0Xx/8sPvl/kFsH3l9y/zN/8A4WD4W/6GTSP/AAPi/wDiqP8AhYPhb/oZNI/8D4v/AIqsD/hRHgb/AKAf/k3P/wDF0f8ACiPA3/QD/wDJuf8A+Lovj/5YffL/ACC2D7y+5f5m/wD8LB8Lf9DJpH/gfF/8VR/wsHwt/wBDJpH/AIHxf/FVgf8ACiPA3/QD/wDJuf8A+Lo/4UR4G/6Af/k3P/8AF0Xx/wDLD75f5BbB95fcv8zf/wCFg+Fv+hk0j/wPi/8AiqP+Fg+Fv+hk0j/wPi/+KrA/4UR4G/6Af/k3P/8AF0f8KI8Df9AP/wAm5/8A4ui+P/lh98v8gtg+8vuX+Zv/APCwfC3/AEMmkf8AgfF/8VR/wsHwt/0Mmkf+B8X/AMVWB/wojwN/0A//ACbn/wDi6P8AhRHgb/oB/wDk3P8A/F0Xx/8ALD75f5BbB95fcv8AM3/+Fg+Fv+hk0j/wPi/+Ko/4WD4W/wChk0j/AMD4v/iqwP8AhRHgb/oB/wDk3P8A/F0f8KI8Df8AQD/8m5//AIui+P8A5YffL/ILYPvL7l/mb/8AwsHwt/0Mmkf+B8X/AMVR/wALB8Lf9DJpH/gfF/8AFVgf8KI8Df8AQD/8m5//AIuj/hRHgb/oB/8Ak3P/APF0Xx/8sPvl/kFsH3l9y/zN/wD4WD4W/wChk0j/AMD4v/iqP+Fg+Fv+hk0j/wAD4v8A4qsD/hRHgb/oB/8Ak3P/APF0f8KI8Df9AP8A8m5//i6L4/8Alh98v8gtg+8vuX+Zv/8ACwfC3/QyaR/4Hxf/ABVH/CwfC3/QyaR/4Hxf/FVgf8KI8Df9AP8A8m5//i6P+FEeBv8AoB/+Tc//AMXRfH/yw++X+QWwfeX3L/M3/wDhYPhb/oZNI/8AA+L/AOKo/wCFg+Fv+hk0j/wPi/8AiqwP+FEeBv8AoB/+Tc//AMXR/wAKI8Df9AP/AMm5/wD4ui+P/lh98v8AILYPvL7l/mb/APwsHwt/0Mmkf+B8X/xVH/CwfC3/AEMmkf8AgfF/8VWB/wAKI8Df9AP/AMm5/wD4uj/hRHgb/oB/+Tc//wAXRfH/AMsPvl/kFsH3l9y/zN//AIWD4W/6GTSP/A+L/wCKo/4WD4W/6GTSP/A+L/4qsD/hRHgb/oB/+Tc//wAXR/wojwN/0A//ACbn/wDi6L4/+WH3y/yC2D7y+5f5m/8A8LB8Lf8AQyaR/wCB8X/xVH/CwfC3/QyaR/4Hxf8AxVYH/CiPA3/QD/8AJuf/AOLo/wCFEeBv+gH/AOTc/wD8XRfH/wAsPvl/kFsH3l9y/wAzf/4WD4W/6GTSP/A+L/4qj/hYPhb/AKGTSP8AwPi/+KrA/wCFEeBv+gH/AOTc/wD8XR/wojwN/wBAP/ybn/8Ai6L4/wDlh98v8gtg+8vuX+Zv/wDCwfC3/QyaR/4Hxf8AxVH/AAsHwt/0Mmkf+B8X/wAVWB/wojwN/wBAP/ybn/8Ai6P+FEeBv+gH/wCTc/8A8XRfH/yw++X+QWwfeX3L/M3/APhYPhb/AKGTSP8AwPi/+Ko/4WD4W/6GTSP/AAPi/wDiqwP+FEeBv+gH/wCTc/8A8XR/wojwN/0A/wDybn/+Lovj/wCWH3y/yC2D7y+5f5m//wALB8Lf9DJpH/gfF/8AFUf8LB8Lf9DJpH/gfF/8VWB/wojwN/0A/wDybn/+Lo/4UR4G/wCgH/5Nz/8AxdF8f/LD75f5BbB95fcv8zf/AOFg+Fv+hk0j/wAD4v8A4qj/AIWD4W/6GTSP/A+L/wCKrA/4UR4G/wCgH/5Nz/8AxdH/AAojwN/0A/8Aybn/APi6L4/+WH3y/wAgtg+8vuX+Zv8A/CwfC3/QyaR/4Hxf/FUf8LB8Lf8AQyaR/wCB8X/xVYH/AAojwN/0A/8Aybn/APi6P+FEeBv+gH/5Nz//ABdF8f8Ayw++X+QWwfeX3L/M3/8AhYPhb/oZNI/8D4v/AIqj/hYPhb/oZNI/8D4v/iqwP+FEeBv+gH/5Nz//ABdH/CiPA3/QD/8AJuf/AOLovj/5YffL/ILYPvL7l/mb/wDwsHwt/wBDJpH/AIHxf/FUf8LB8Lf9DJpH/gfF/wDFVgf8KI8Df9AP/wAm5/8A4uj/AIUR4G/6Af8A5Nz/APxdF8f/ACw++X+QWwfeX3L/ADN//hYPhb/oZNI/8D4v/iqP+Fg+Fv8AoZNI/wDA+L/4qsD/AIUR4G/6Af8A5Nz/APxdH/CiPA3/AEA//Juf/wCLovj/AOWH3y/yC2D7y+5f5m//AMLB8Lf9DJpH/gfF/wDFUf8ACwfC3/QyaR/4Hxf/ABVYH/CiPA3/AEA//Juf/wCLo/4UR4G/6Af/AJNz/wDxdF8f/LD75f5BbB95fcv8zf8A+Fg+Fv8AoZNI/wDA+L/4qj/hYPhb/oZNI/8AA+L/AOKrA/4UR4G/6Af/AJNz/wDxdH/CiPA3/QD/APJuf/4ui+P/AJYffL/ILYPvL7l/mb//AAsHwt/0Mmkf+B8X/wAVR/wsHwt/0Mmkf+B8X/xVYH/CiPA3/QD/APJuf/4uj/hRHgb/AKAf/k3P/wDF0Xx/8sPvl/kFsH3l9y/zN/8A4WD4W/6GTSP/AAPi/wDiqP8AhYPhb/oZNI/8D4v/AIqsD/hRHgb/AKAf/k3P/wDF0f8ACiPA3/QD/wDJuf8A+Lovj/5YffL/ACC2D7y+5f5m/wD8LB8Lf9DJpH/gfF/8VR/wsHwt/wBDJpH/AIHxf/FVgf8ACiPA3/QD/wDJuf8A+Lo/4UR4G/6Af/k3P/8AF0Xx/wDLD75f5BbB95fcv8zf/wCFg+Fv+hk0j/wPi/8AiqP+Fg+Fv+hk0j/wPi/+KrA/4UR4G/6Af/k3P/8AF0f8KI8Df9AP/wAm5/8A4ui+P/lh98v8gtg+8vuX+Zv/APCwfC3/AEMmkf8AgfF/8VR/wsHwt/0Mmkf+B8X/AMVWB/wojwN/0A//ACbn/wDi6P8AhRHgb/oB/wDk3P8A/F0Xx/8ALD75f5BbB95fcv8AM3/+Fg+Fv+hk0j/wPi/+Ko/4WD4W/wChk0j/AMD4v/iqwP8AhRHgb/oB/wDk3P8A/F0f8KI8Df8AQD/8m5//AIui+P8A5YffL/ILYPvL7l/mb/8AwsHwt/0Mmkf+B8X/AMVR/wALB8Lf9DJpH/gfF/8AFVgf8KI8Df8AQD/8m5//AIuj/hRHgb/oB/8Ak3P/APF0Xx/8sPvl/kFsH3l9y/zN/wD4WD4W/wChk0j/AMD4v/iqP+Fg+Fv+hk0j/wAD4v8A4qsD/hRHgb/oB/8Ak3P/APF0f8KI8Df9AP8A8m5//i6L4/8Alh98v8gtg+8vuX+Zv/8ACwfC3/QyaR/4Hxf/ABVH/CwfC3/QyaR/4Hxf/FVgf8KI8Df9AP8A8m5//i6P+FEeBv8AoB/+Tc//AMXRfH/yw++X+QWwfeX3L/M3/wDhYPhb/oZNI/8AA+L/AOKrYsNQtdVtI7qyuYby2kzsmgkDo2CQcMODyCPwrzHxh8F/BuleEdbvbXR/Kubaxnmif7VMdrrGxU4L4PIHWtf4Ef8AJKdD/wC2/wD6PkopV6/t/Y1ktm9G+6XVLuFSjR9j7Wk3vbW3Zvod9RRRXpnAFFFFAGB8Qf8AkQvEn/YMuf8A0U1YHwI/5JTof/bf/wBHyVv/ABB/5ELxJ/2DLn/0U1YHwI/5JTof/bf/ANHyV5j/AN/j/gf/AKUjvX+5v/EvyZ31FFFemcAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFAGB8Qf8AkQvEn/YMuf8A0U1YHwI/5JTof/bf/wBHyVv/ABB/5ELxJ/2DLn/0U1YHwI/5JTof/bf/ANHyV5j/AN/j/gf/AKUjvX+5v/EvyZ31FFFemcAUUUUAYHxB/wCRC8Sf9gy5/wDRTVgfAj/klOh/9t//AEfJW/8AEH/kQvEn/YMuf/RTVgfAj/klOh/9t/8A0fJXmP8A3+P+B/8ApSO9f7m/8S/JnfUUUV6ZwBRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUVBd39rp4iN1cw2wlkWGMzOE3yMcKgz1YngDqaBN23J6Kzdf8Raf4Yso7vUpzBDJNHbRhI2keSWRgqIqICzEkjgA9z0BrSoFdN26hRRRQUFFFFABRRWLJ400WHxXF4be+Ua1LD56W2xsFeTjfjaGIVmCE7iqsQMAmi1yXJR3djaooooKCiiigAoorE07xpoureI9Q0G0vlm1WwUNcQBGAXpnaxG1yu5NwUkrvXdjcMlrkuSjZN7m3RRRQUFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFAGB8Qf+RC8Sf8AYMuf/RTVgfAj/klOh/8Abf8A9HyVv/EH/kQvEn/YMuf/AEU1YHwI/wCSU6H/ANt//R8leY/9/j/gf/pSO9f7m/8AEvyZ31FFFemcAUUUUAYHxB/5ELxJ/wBgy5/9FNWB8CP+SU6H/wBt/wD0fJW/8Qf+RC8Sf9gy5/8ARTVgfAj/AJJTof8A23/9HyV5j/3+P+B/+lI71/ub/wAS/JnfUUUV6ZwBRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFAFfUZWg0+5kQ7XSJmU+hAOK8B0Lxn4v0zwP8MfE8vi681/UfEtxYQ3Oh3tpaCOVJwDK0PkwxyKY13PkswCqdw7j33VEaTTbtVUszQuAoGSTtNfLPh7wjaj4Q+GNM8JeC9S0H4pRW1kjasPD9xppt5VaPznnunijWRMB9ybn39AD1G9NJrU8jGynGS5X0fVrW62XV9kz27w94q1S++NnjHQJ7rfpOn6Zp9xbW/lqPLklM3mHcBuOdi8EkDHGKwPGusKfi5fprmoXC+FvDHh2DxN9giVAhuEmuQZXO3e21YlIXcFyMkGrmow3PgL4x6v4nuNN1C/0LW9KtbV7nTLSS7ktriCST5WhiVpCrLKCGVTgq2ccZqL4Pufil4g8d6pdW15oujaz4dj8N2T3cBhuJEP2hpZzC4DIAbgBQ4BO1sqBgkSSd+lhSc5R9nvLmenlq1fy28uhc8P+H/FXxC0bSPEur+IZfD19KovrLTLCwtJY7FXQ7Vd54XkaTYxV2Ro/vMBgc1xul/Evxbp9zrlxrF5a6j4rttXTwjp+i2kMltYSzybZo7uTMkhOYiXJG0qEdRnIau28HeP9Q8M+F9O0fxV4c1qLxDYwLav/ZemT3ttdlBtEsU0SFFVwAwEhQrnBAxXJ3nwv8U3GjXfi2GyiTxa/iiLxRFojzIN0MUP2dbRpeVEhgLncDtEjAZ2jdVLdqWxjPmcYyotuW737dv5uy/A9J03wf4mt7yzvL3x7qF26Or3Nkmn2cdnKMfMqr5RlQHnH71iPU1ofEPxnB8PvBmqa/PA92LOMeXbRnDTyuwSKMHsWdkXPbNUNN+J1nqd5a2S6H4kt72Z1R4rjQ7mOOAnqXnKeSQO5Vz7Zo+L3g288ffDvVtF024jtdSk8q4tJZv9WJ4ZkmjD8H5S8ag4HQmsvtLmPRbSpTdB3dnbVvW2m5Ss/CPjXULSC81Lx1dabqbBXksdKsLQ2MZzkx/vYnlYAfKW8xScZAXOBNpHiTUbr4y+JNCluN2lWej2F1Bb7FGyWSW5V23Y3HIjTgnAxxjJqLT/AIsg2lvFqnhbxLp+tsqibT4tHuLlEk7hbmNDAy5zht44646Vg3Pw20rxb8c/Ed74j8LWes6cNE0+O0uNU09J4RIJboyLGzqRuAKZA55XPaq78xi5L3PYtvXW7fZ772+46T/hI9R/4XZ/YH2j/iU/8I99u+z7F/132nZu3Y3fd4xnHtXD3Pi+Zfiu+oeVaN4KTW4tCmU2cXOqGDi980jeWVzFaZyMHI5xw3U/DVv8KfiNruu+FPBSw2sPhGQx22iaXsjursXGUixEnzOcL6kDnoKii/Z61m5+D7eHZ/HOtJf3FsbuW38mx+zjUGbzy+77N5u0XB3Z8zdxwwqkorU5pyrzvFJtpt6P7lrbR6l741+PfGHhDx94ZHhwm90230691XVNGSFHkv4IXgVljYjcrqsruoUjcVAOc10PjH4gSSQ/DXUPDuoo+l+INcggklRFYXFq9rcSbfmBK5ZEORgjGPUVh+F7vWvFvj74deI9R0S/02VvDN/HqCXNpJELa6aW1zG24DbkpIVzjcoyOK5nX/AuveDvib4P0TSNKuL7wPN4kGuwS20TOukSeTcrcQvjIWJ3mWROgBMg9KaUdE90KVSqnKpFtxk499Ph/B6p+fzPVPjJquu+Hvh7qWteHZSt/pRjv5IBGr/abeJw08PzA43RhwCMHOMEVn/FPx/cWHw5s7zwvdJ/bHiKW1sNEn2K4824I2y7SCCEjLSHIIwhzxmvQ5YknieORQ8bgqysMgg9Qa+f/hN4M16Hx9Z6BrGn3kHh74erdJpN7cowjvzcOwt2Rjw/k226M4JwX5565ws1d9DrxDqRnywv76t6Pv8Ac2/kjv7LxNqy/E/xVokt809jp2gWN3ArRICJne5V3JCgnd5ScdBjgDJrC+CPhnUNc8GeDvFl/wCKNQuLq6s11CSzWw02K38ydd820paLIoZmJJDgk9Sa1YNJvh8ZPGt6bK4FnceHbCCG4MTeXJIsl2WRWxgsAy5A5G4eorjPgJpPgrQPDHgyC48CXGmeM4LKGGe+l8G3UMsdx5e1y10bYKM5YFy+OTzV/ZdvL8jC79rHnel5btr7St6+SN3xTYeKNK+InhDRbf4h+IFstaN6Z82umFo/KiDoEP2Pjk4O7PH516xplpLYWEFvPez6jNGoVru5WMSSn+8wjVUz/uqB7VxHjPTLy6+LPw5vIbSea0tf7R+0Txxlo4d0Chd7AYXJ4Gepr0CspO6R20I8s6mr30u2+i7+YUUUVB2BRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFAGB8Qf+RC8Sf8AYMuf/RTVgfAj/klOh/8Abf8A9HyVv/EH/kQvEn/YMuf/AEU1YHwI/wCSU6H/ANt//R8leY/9/j/gf/pSO9f7m/8AEvyZ31FFFemcAUUUUAYHxB/5ELxJ/wBgy5/9FNWB8CP+SU6H/wBt/wD0fJW/8Qf+RC8Sf9gy5/8ARTVgfAj/AJJTof8A23/9HyV5j/3+P+B/+lI71/ub/wAS/JnfUUUV6ZwBRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFZtt4c0608QX2tw2+zU76CG2uJ97HfHEXMa7SdowZH5ABOec4FaVFBLSdr9AooooKCiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigDA+IP/IheJP+wZc/+imrA+BH/JKdD/7b/wDo+St/4g/8iF4k/wCwZc/+imrA+BH/ACSnQ/8Atv8A+j5K8x/7/H/A/wD0pHev9zf+JfkzvqKKK9M4AooooAwPiD/yIXiT/sGXP/opqwPgR/ySnQ/+2/8A6Pkrf+IP/IheJP8AsGXP/opqwPgR/wAkp0P/ALb/APo+SvMf+/x/wP8A9KR3r/c3/iX5M76iiivTOAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAoooZgoJJAA5JPagAoqO2uob23juLeVJ4JVDpLEwZXU9CCOCKzbrVZrLxHZ2cr2v2S8icRAyBJxKnJwpb51Kn+EZXHOQciHNRSZSi27GtRSSSLEjO7BEUZLMcAD1NZ+neJdI1id4bDVbK+mTlo7a4SRl+oBOKblFNJvViUW1dI0aKKzNT8UaNoswi1DVrGwlYZCXNykbEfRiKJSjFXk7AouTslc06Kie7gjtWuXmjW2VPMMzMAgTGd2emMc5oguoLq2juYZo5reRBIk0bBkZSMhgRwRjvTutgsyWio7a6hvbeO4t5UnglUOksTBldT0II4IqShO+qFsFFFFMAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKAMD4g/8iF4k/7Blz/6KasD4Ef8kp0P/tv/AOj5K3/iD/yIXiT/ALBlz/6KasD4Ef8AJKdD/wC2/wD6PkrzH/v8f8D/APSkd6/3N/4l+TO+ooor0zgCiiigDA+IP/IheJP+wZc/+imrA+BH/JKdD/7b/wDo+St/4g/8iF4k/wCwZc/+imrA+BH/ACSnQ/8Atv8A+j5K8x/7/H/A/wD0pHev9zf+JfkzvqKKK9M4AooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACob3/AI85/wDrm38qmpk0fnQyR5xuUrn0zSewLc534af8k98Of9eEP/oAriPFPiPTNQ1XxDqB1fT7fUNFMcOm281zGkjSRESzYBOcOcRf8ANdl4e8N6/4f0qx01NZ06W0tIVhUnTJBIVUYBJ+0Yz+FavhbQF8MaBZ6asxuWhU+ZOwwZZGJZ3IycZYk9T1rzPZVKtOFJrlstb2etrLZ/P1SPQ9pTpzlUTvd6fffqvl8zmtbv7bxbrPg213iXR9Rjm1BoT0n8tEMasO4Bk3EeqjNXvibaRJ4Mv79AsN5pkRu7SdQA0UiDI2n0ONpHcEimW3w6it9HgsVvpInsbyS6025gQI9orMSI+SQ64ZlIIwVIGBgGrVz4Xv9bVINb1SG8sFdXa1tLQwCbByBIS7kjIBwu0HHPHFHJVlCalHWXppol+Du9O/cXPTjKLjLSP463/Faf8AALmu6xPpvg7UdVRAtzb2El0qN0DrGWwfxFQ+DNFtdM8N2YjVZ5bmFZrm5f5nuZGUFndv4ifftgdBW7LEk8TxyKHjcFWVhkEHqDXM6f4W1XQLRLDSNaij06P5YYr+0NxJCnZFcSJ8o6DcGIGOTXTKMlUU7XVrdP17mEZRcHG9tf6+4p/EfVrCxs9K0W6urXT7fU7hYpWuHWONbaP55BzgYYAR/wDbSofAuuWVzb+INHs72C+h0+Z3t5beVZFMEoLqAVP8JLpjsFFb9n4cki8StrF1efapRZJZxJ5ewJ826RuuCXO3oBjaBzRf+GvtXiW21iG5Fu62klnPH5e7zo2IZOcjBVsnofvEcZrF06rqe1Xpbrb1vbfX8DVTpqHs/nfz+6+2hV+Gn/JPfDn/AF4Q/wDoArpazfDOjf8ACO+HtO0vzvtH2O3SDzdu3ftGM4ycfTNaVddGLhSjF7pI5qrUqkpLq2FFFFbGYUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFAGB8Qf+RC8Sf8AYMuf/RTVgfAj/klOh/8Abf8A9HyVv/EH/kQvEn/YMuf/AEU1YHwI/wCSU6H/ANt//R8leY/9/j/gf/pSO9f7m/8AEvyZ31FFFemcAUUUUAYHxB/5ELxJ/wBgy5/9FNWB8CP+SU6H/wBt/wD0fJW/8Qf+RC8Sf9gy5/8ARTVgfAj/AJJTof8A23/9HyV5j/3+P+B/+lI71/ub/wAS/JnfUUUV6ZwBRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAYHxB/5ELxJ/wBgy5/9FNWB8CP+SU6H/wBt/wD0fJW/8Qf+RC8Sf9gy5/8ARTVgfAj/AJJTof8A23/9HyV5j/3+P+B/+lI71/ub/wAS/JnfUUUV6ZwBRRRQBgfEH/kQvEn/AGDLn/0U1YHwI/5JTof/AG3/APR8lb/xB/5ELxJ/2DLn/wBFNWB8CP8AklOh/wDbf/0fJXmP/f4/4H/6UjvX+5v/ABL8md9RRRXpnAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQBgfEH/kQvEn/AGDLn/0U1YHwI/5JTof/AG3/APR8lb/xB/5ELxJ/2DLn/wBFNWB8CP8AklOh/wDbf/0fJXmP/f4/4H/6UjvX+5v/ABL8md9RRRXpnAFFFFAGB8Qf+RC8Sf8AYMuf/RTVgfAj/klOh/8Abf8A9HyVv/EH/kQvEn/YMuf/AEU1YHwI/wCSU6H/ANt//R8leY/9/j/gf/pSO9f7m/8AEvyZ31FFFemcAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFAGB8Qf+RC8Sf8AYMuf/RTVgfAj/klOh/8Abf8A9HyVv/EH/kQvEn/YMuf/AEU1YHwI/wCSU6H/ANt//R8leY/9/j/gf/pSO9f7m/8AEvyZ31FFFemcAUUUUAYHxB/5ELxJ/wBgy5/9FNWB8CP+SU6H/wBt/wD0fJW/8Qf+RC8Sf9gy5/8ARTVgfAj/AJJTof8A23/9HyV5j/3+P+B/+lI71/ub/wAS/JnfUUUV6ZwBRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRTXkWMZZgv1oEOoqrJqMa/dBf8ASq76lIfuqF/Wq5WZupFGlSM6r94gfU1jvcyv1kb8Dioyc9afIZut2RsNdwr1kX8OaYdQhHcn6Csqiq5UR7aRpnUoh2Y/hSf2nH/das2ijlQvayNL+04/7rUDUov7rD8KzaKOVB7WRqDUIT3I+oqRbuFukg/HiseijlQ/bSNxZFf7rBvoaWsKpFnkT7rsPxqeQtVu6NmisxNRlXrhvqKsR6kjfeUr+tLlZoqsWW6KZHMkv3WBp9SabhRRRQMKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAwPiD/AMiF4k/7Blz/AOimrA+BH/JKdD/7b/8Ao+St/wCIP/IheJP+wZc/+imrA+BH/JKdD/7b/wDo+SvMf+/x/wAD/wDSkd6/3N/4l+TO+ooor0zgCiiigDA+IP8AyIXiT/sGXP8A6KasD4Ef8kp0P/tv/wCj5K3/AIg/8iF4k/7Blz/6KasD4Ef8kp0P/tv/AOj5K8x/7/H/AAP/ANKR3r/c3/iX5M76iiivTOAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACigkAZJwKqT6gicJ859e1CVyXJR3LZOBk1Wlv4o+Ad59ulZ8txJMfmYkenao60Ue5zyrfylmW/lk6HYPaq5JY5JyfU0lFXaxzuTe4UUUUxBRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAdKnivZY/4tw9GqCila402tjTi1GN+HBQ/mKtBgwyCCPUVhU+OZ4jlGIqHHsbxrNbm1RVKHUgeJBg+o6VcVg4ypBHqKhpo6IyUthaKKKRYUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAYHxB/5ELxJ/wBgy5/9FNWB8CP+SU6H/wBt/wD0fJW/8Qf+RC8Sf9gy5/8ARTVgfAj/AJJTof8A23/9HyV5j/3+P+B/+lI71/ub/wAS/JnfUUUV6ZwBRRRQBgfEH/kQvEn/AGDLn/0U1YHwI/5JTof/AG3/APR8lb/xB/5ELxJ/2DLn/wBFNWB8CP8AklOh/wDbf/0fJXmP/f4/4H/6UjvX+5v/ABL8md9RRRXpnAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUjMEUljgDuaAFqC4vEg4+83oKq3N+Xysfyr/e7mqdWo9zmnVtpElmuZJz8xwv90dKioorQ5m29WFFFFMQUUUUAFFFFABRRRQAUUV8t6v+3roGkate2L+FtSke1neBnWePDFWK5H5VcYSn8KOWviqOFSdaVrn1JRXmNh+0F4buMLcw3tm/8RaMOo+hU5/SujsPir4T1HHla3bIT/z33Rf+hgV5sMfhZu0aiv6nrywWJgrum7eh1dFVbLVbLUlzaXlvdDrmGVX/AJGrVdqakrpnG007MKKKKoQUUUUAFFFFABRRRQAUUUUAFPineE5Q49u1MopDTtsalvfJLgN8jfoas1hVZt75ocBvmT9RUOPY6IVekjUopsciyruU5FOrM6QooooGFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQBgfEH/AJELxJ/2DLn/ANFNWB8CP+SU6H/23/8AR8lb/wAQf+RC8Sf9gy5/9FNWB8CP+SU6H/23/wDR8leY/wDf4/4H/wClI71/ub/xL8md9RRRXpnAFFFFAGB8Qf8AkQvEn/YMuf8A0U1YHwI/5JTof/bf/wBHyVv/ABB/5ELxJ/2DLn/0U1YHwI/5JTof/bf/ANHyV5j/AN/j/gf/AKUjvX+5v/EvyZ31FFFemcAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFQXN0tuMdXPQUbibSV2PmnSBcsfoO5rLuLl7hstwB0ApkkjSsWY5JptaqNjinUctOgUUUVZkFFFFABRRRQAUUUUAFFFFABRRRQAV+RHjX/AJHLXv8Ar/n/APRjV+u9fkR41/5HLXv+v+f/ANGNXbhd2fH8RfDS+f6H08/32+tNpz/fb602vwip8b9T+kaX8OPohVYowZSQwOQR1FbFh4z17TMfZdZvoVH8C3DbfyzisaiiM5wd4Ow5QjNWkrnd2Hxt8XWWA2oJdKP4Z4EP6gA/rXR2H7R2qx4+2aTaXHr5LtFn891eQ0V308zxlP4ar+ev53OGeW4Sp8VNfLT8j6AsP2jNIlwLzS7y3J6mJlkA/MrXRWHxq8I32AdSa2c/wzwuv6gEfrXy7RXo08/xkPis/Vf5WPPnkWEl8N16P/O59iWHi/Q9Tx9k1ixnY/wpcKW/LOa1wQQCDkGviSrljrF/phBs765tCOQYJmT+Rr0afEj/AOXlL7n/AMA8+fDq/wCXdT71/wAE+z6K+XPD/wARPHM9wsGm6he6hL/zzMQuCR75BP417L4Rn+IV3sbWItLtLc9fNRmmx7BG2/nXuYTNqeLdoU5fdp99zxcVlc8KrzqR+/X7rHfUUDOBnrRXunihRRRQAUUUUAPimaFtynHt61qW90twOOH7rWRSqxRgQcEd6lq5pCbiblFVrS8E3ytw/wDOrNYtWO1NSV0FFFFBQUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAYHxB/5ELxJ/wBgy5/9FNWB8CP+SU6H/wBt/wD0fJW/8Qf+RC8Sf9gy5/8ARTVgfAj/AJJTof8A23/9HyV5j/3+P+B/+lI71/ub/wAS/JnfUUUV6ZwBRRRQBgfEH/kQvEn/AGDLn/0U1YHwI/5JTof/AG3/APR8lb/xB/5ELxJ/2DLn/wBFNWB8CP8AklOh/wDbf/0fJXmP/f4/4H/6UjvX+5v/ABL8md9RRRXpnAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFQXVyLdPVz0FG4m0ldiXV0LdcDlz0HpWWzF2LMck96GYuxZjknvSVslY4Zzc2FFFFUZhRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABX5EeNf8Akcte/wCv+f8A9GNX671+RHjX/kcte/6/5/8A0Y1duF3Z8fxF8NL5/ofTz/fb602nP99vrTa/CKnxv1P6Rpfw4+iCiiiszQKKlt7aa7mWKCJ5pW4VI1LMfoBXdeH/AII+Jtb2vNbppcB533bYb/vgZOfriumjh62IdqUW/QwrYilQV6skjgKnsrG51GdYLS3lupm6RwoXY/gK+hPD/wCz/oWm7X1KafVZR1Unyo/yBz/49XommaNYaLB5NhZwWcX92CMKD9cda+jw/D1eetaSivvf+R89iM/ow0oxcn9y/wAz528P/AnxHq+17tYtJgPObhtz49lX+pFel+H/AIC+HdJ2vfGbVph/z2OyPPsq/wBSa9Kor6bD5Ng8Pry8z89fw2/A+cr5vi6+nNyry0/Hcr2Gm2ml24gs7aG0hHSOBAi/kKsUUV7aSirI8Ztt3YUUUUxBRRRQAUUUUAFFFFAADg5HWtKzvPNwjn5+x9azaAcHI61LVy4ycXdG7RVWzu/OGxz84/WrVYtWO6LUldBRRRQUFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAYHxB/wCRC8Sf9gy5/wDRTVgfAj/klOh/9t//AEfJW/8AEH/kQvEn/YMuf/RTVgfAj/klOh/9t/8A0fJXmP8A3+P+B/8ApSO9f7m/8S/JnfUUUV6ZwBRRRQBgfEH/AJELxJ/2DLn/ANFNWB8CP+SU6H/23/8AR8lb/wAQf+RC8Sf9gy5/9FNWB8CP+SU6H/23/wDR8leY/wDf4/4H/wClI71/ub/xL8md9RRRXpnAFFFFABRRRQAUUUUAFFFFABRRRQAUUUjMEUsTgCgBk8ywRlj+A9ayJJGlcsxyTT7mc3EmTwB0FRVrFWOGpPmemwUUUVZkFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABX5EeNf+Ry17/r/AJ//AEY1frvX5EeNf+Ry17/r/n/9GNXbhd2fH8RfDS+f6H08/wB9vrSxRPNIscaNI7HAVRkk+wr3DwX8B9MvtMstT1S9nuhdQpOLeEeWqhgGwTyT17Yr1PRPCukeG49mmadb2nGC6J85Hux5P4mvzClw/iKsnKq1Ffe/8vxP2uefYenBKknJ29F/XyPnLw/8GvE+vbXNkNOgP/LS9Pl/+O8t+lel+H/2etIsdsmq3c2pSDrGn7qP9DuP5ivV6K+iw+SYShrJcz8/8tjwa+c4utpF8q8v89zP0fw9pnh+HytOsILJMYPkxgFvqep/GtCiivejGMFyxVkeJKTm+aTuwoooqiQooooAKKKKACiiigAooooAKKKKACiiigAooooAVWKsCDgjvWta3IuE54cdRWRT4pWhkDL1FS1c0hPlZtUU2KUTRh16GnVid24UUUUDCiiigAooooAKKKKACiiigAooooAwPiD/AMiF4k/7Blz/AOimrA+BH/JKdD/7b/8Ao+St/wCIP/IheJP+wZc/+imrA+BH/JKdD/7b/wDo+SvMf+/x/wAD/wDSkd6/3N/4l+TO+ooor0zgCiiigDA+IP8AyIXiT/sGXP8A6KasD4Ef8kp0P/tv/wCj5K3/AIg/8iF4k/7Blz/6KasD4Ef8kp0P/tv/AOj5K8x/7/H/AAP/ANKR3r/c3/iX5M76iiivTOAKKKKACiiigAooooAKKKKACiiigArOv7ne3lqflHX3NWry48iLj7zcCsmriupzVZ291BRRRWpyhRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFfkR41/5HLXv+v+f/ANGNX671+RHjX/kcte/6/wCf/wBGNXbhd2fH8RfDS+f6H6veDP8AkT9C/wCvCD/0WtbNY3gz/kT9C/68IP8A0WtbNcT3PrYfAvQKKKKCwooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAsWdx5EmD9xuvtWrWFWlp9x5ieWx+ZenuKzkup00p/ZZbooorM6gooooAKKKKACiiigAooooAKKKKAMD4g/8iF4k/7Blz/6KasD4Ef8kp0P/tv/AOj5K3/iD/yIXiT/ALBlz/6KasD4Ef8AJKdD/wC2/wD6PkrzH/v8f8D/APSkd6/3N/4l+TO+ooor0zgCiiigDA+IP/IheJP+wZc/+imrA+BH/JKdD/7b/wDo+St/4g/8iF4k/wCwZc/+imrA+BH/ACSnQ/8Atv8A+j5K8x/7/H/A/wD0pHev9zf+JfkzvqKKK9M4AooooAKKKKACiiigAooooAKCQASeAKKqajNsjCDq3X6UJXZMnyq5SuZjPKW7dB9Kioorc89u7uwooopiCiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAK/Ijxr/yOWvf9f8//AKMav13r8iPGv/I5a9/1/wA//oxq7cLuz4/iL4aXz/Q/V7wZ/wAifoX/AF4Qf+i1rZrG8Gf8ifoX/XhB/wCi1rZrie59bD4F6BRRRQWFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABTopDFIrDqDTaKA2NuNxIgZehp1UNNmwTGe/Iq/WDVmehCXMrhRRRSLCiiigAooooAKKKKACiiigDA+IP/IheJP+wZc/+imrA+BH/JKdD/7b/wDo+St/4g/8iF4k/wCwZc/+imrA+BH/ACSnQ/8Atv8A+j5K8x/7/H/A/wD0pHev9zf+JfkzvqKKK9M4AooooAwPiD/yIXiT/sGXP/opqwPgR/ySnQ/+2/8A6Pkrf+IP/IheJP8AsGXP/opqwPgR/wAkp0P/ALb/APo+SvMf+/x/wP8A9KR3r/c3/iX5M76iiivTOAKKKKACiiigAooooAKKKKAAnArGuJfOlZu3b6VoX8vlwYB5bj8Ky60iupy1pa8oUUUVocwUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAV+Zviv9nf4kXvirV5oPCGoyRTXk0kbBV+ZS7EEc+lfpfPMtvE0j52jsBkk9gB3J6Ad6u2mkn7E3nhRdSkOzDnYf4VB9B07Zyx43GtIVXS1R52My2GY8sZNrl7GD4VtpbLwxo9vOhjmis4Y3RuqsEAIP41qUrIyMVYbWHUUlZ7noJcqsFFFFAwooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKAFRyjhh1BzW3G4kRWHQjNYdaGmy5Voz25FRJaXN6UrOxdooorI7AooooAKKKKACiiigAooooAwPiD/yIXiT/sGXP/opqwPgR/ySnQ/+2/8A6Pkrf+IP/IheJP8AsGXP/opqwPgR/wAkp0P/ALb/APo+SvMf+/x/wP8A9KR3r/c3/iX5M76iiivTOAKKKKAMD4g/8iF4k/7Blz/6KasD4Ef8kp0P/tv/AOj5K3/iD/yIXiT/ALBlz/6KasD4Ef8AJKdD/wC2/wD6PkrzH/v8f8D/APSkd6/3N/4l+TO+ooor0zgCiiigAooooAKKKKACiimyOI42Y9hmgRm38vmTkdl4qtSkliSeSaSt1oec3d3CiiimIKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAoorP1O4meW306zfZfXeQj4B8mMY3ykH+6CAOxZkB4OaBMt6XF/a2oNMebO0cqp7SzDgn0ITp3+bPQpXRVDZWcOn2kNtAmyGJQiLkk4HqTyT7nrU1YN3Z6FOHIrFO/tt6mRR8w6+9Z1btZV7b+TLkD5G5FXF9DGrD7SK9FFFaHMFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAVLbS+TMrds4NRUUhp2dzdoqK0k82BD3HBqWsD0U7q4UUUUDCiiigAooooAKKKKAMD4g/wDIheJP+wZc/wDopqwPgR/ySnQ/+2//AKPkrf8AiD/yIXiT/sGXP/opqwPgR/ySnQ/+2/8A6PkrzH/v8f8AA/8A0pHev9zf+JfkzvqKKK9M4AooooAwPiD/AMiF4k/7Blz/AOimrA+BH/JKdD/7b/8Ao+St/wCIP/IheJP+wZc/+imrA+BH/JKdD/7b/wDo+SvMf+/x/wAD/wDSkd6/3N/4l+TO+ooor0zgCiiigAooooAKKKKACquovtgC/wB41arN1J90wX+6KqO5lUdosqUUUVscIUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQBBf39vpdjPeXUohtoEMkkjdFUDJNP8IaZOkU2q38TQ6jfhWML9beEZ8uHjuMkt1+ZmwSAuMW3i/4TDxQbcDdoujSq9we1xeDDJH7rF8rt/tmMZ+VxXd1E3bQ1ox5nzvZbf1+H3hRRRWR3BTJ4RPGVP4H0NPooE1fQw3UoxUjBHFJWhqFvuHmqOR96s+tk7nBKPK7BRRRVEBRRRQAUUUUAFFFFABRRUN7e2+m2c13dzx2trAhklmmcKiKBksxPAAHegNiaijrRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQBf0yT76fiKvVk2T7LlPQ8VrVjLc7aTvEKKKKk2CiiigAooooAKKKKAMD4g/wDIheJP+wZc/wDopqwPgR/ySnQ/+2//AKPkrf8AiD/yIXiT/sGXP/opqwPgR/ySnQ/+2/8A6PkrzH/v8f8AA/8A0pHev9zf+JfkzvqKKK9M4AooooAwPiD/AMiF4k/7Blz/AOimrA+BH/JKdD/7b/8Ao+St/wCIP/IheJP+wZc/+imrA+BH/JKdD/7b/wDo+SvMf+/x/wAD/wDSkd6/3N/4l+TO+ooor0zgCiiigAooooAKKKKACsa5ffO5962HbYjN6DNYZOTVwOas9kFFFFanKFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAVz/i/W7mwhtNN0va+varIbeyVxuWM4y8zj+5GuWPqdq9WFbN/f2+l2NxeXcyW1pbxtNNNIcLGijLMT2AAJr5t8H/ALYvw3g1/VvEOtXWof2lcM1pZwx2bOtrZK3yqDn70hHmOR/sLz5YNXGLeqVzkr4ilSahUmo37u2nU+pPDegW3hfRLXTLTe0MCnMkh3PK5JZ5HPdmYsxPcsTWlXzz/wAN3fCr/n71T/wAb/Gj/hu74Vf8/eqf+ADf41l7Ko/ss6lmWBiklVjb1R9DUV88/wDDd3wq/wCfvVP/AAAb/Gj/AIbu+FX/AD96p/4AN/jS9jU/lY/7TwX/AD+j96Poaivnn/hu74Vf8/eqf+ADf40f8N3fCr/n71T/AMAG/wAaPY1P5WH9p4L/AJ/R+9H0MRkYPIrIuoDBKR/CeQa8Q/4bu+FX/P3qn/gA3+Nb/gj9qn4efFLxDDoGkX90mpzIzwJd2xiEhUZKgnvgE49jVKnUjq4kSx2Dq2jGrFvpqj0qiiimWFFFFABRRRQAUUUUAFfFP7a3x6/tG6k+HuhXP+iwOG1eeM8SSA5WDPopwW/2gB/Cc+3/ALT/AMc4/g74LMNhKp8TaorRWMfUwjo05HoueM9WI6gGvzZnnkuZpJppGllkYu8jsSzMTkkk9Sa7cPTv77Pks7x/JH6rTer39O3z/L1P0s/ZU+Jn/CyfhFpr3EvmarpX/Evu8n5mKAbHP+8m3n1DV7DX52fsY/Ez/hBvitHpN1Ls0zxAos3DH5VnBJhb65JT/tpX6J1hWhyTPVyrFfWsNFvdaP8Ar0CiiisT2AooooAKKKKACiiigAooooAKKKKAFU7WBHUHNbgO4Ajoawq2LVt9vGfbFZzOmi9WiWiiiszqCiiigAooooAKKKKAMD4g/wDIheJP+wZc/wDopqwPgR/ySnQ/+2//AKPkrf8AiD/yIXiT/sGXP/opqwPgR/ySnQ/+2/8A6PkrzH/v8f8AA/8A0pHev9zf+JfkzvqKKK9M4AooooAwPiD/AMiF4k/7Blz/AOimrA+BH/JKdD/7b/8Ao+St/wCIP/IheJP+wZc/+imrA+BH/JKdD/7b/wDo+SvMf+/x/wAD/wDSkd6/3N/4l+TO+ooor0zgCiiigAooooAKKKKAIrttttIfbFY9amoHFsfcisutY7HHW+IKKKKswCiiigAooooAKKKKACiiigAooooAKKKKACiiigAoorzv47/Fy0+DfgC71mTZLqUv+j6fasf9bOQcEj+6v3j7DHUimk5OyM6lSNKDqTdkjwD9t/44+TEPh5o1x+8cLNq8sbfdXqkH48M3ttHcivjKrWq6rd65qd3qN/O91e3crTzzSHLO7HLE/UmqtexTgoRsj8rxmKljKzqy+XkgooorQ4gooooAKKKKACrmi6xeeHtXs9T0+drW+s5lngmTqjqcg/mKp0UAm07o/Vr4OfE2z+LfgDTfENrtjmlXyru3U58i4UDen06EeoYHvXbV+dX7IPxl/wCFafEBdI1GfZ4f1xlglLn5YJ84jl9hk7W9iCfuiv0VryKsPZyt0P1DLcYsZQUn8S0f9eYUUUVieqFFFFABWF448Z6X8PfCuo+INYm8iwsozI2PvOeioo7sxIAHqa3SQBk8Cvzw/a3+PP8AwtDxV/YOj3G/wxpMhCuh+W7nGQ0vuo5VfbJ/i41pU3UlY8zMMbHBUXP7T2Xn/wAA8p+KHxH1P4q+NL/xFqrYluG2wwA5W3iH3I19gPzJJ6muUoor10klZH5fOcqknOTu2SW9xJaTxzwyNFNGwdJEOCrA5BB9c1+qnwU+IkfxT+GmieIAy/apovLu0X+C4T5ZBjtkjI9mFflPX1T+wf8AEz+x/Feo+DLuXFrqqm6swx4Fwi/MB/vIM/8AbMVz4iHNG/Y97JMV7DEezltPT59P8vmfc9FFFeWfooUUUUAFFFFABRRRQAUUUUAFFFFABWppzZt8ehIrLrQ0w/I496iWxtS+Iu0UUVkdoUUUUAFFFFABRRRQBgfEH/kQvEn/AGDLn/0U1YHwI/5JTof/AG3/APR8lb/xB/5ELxJ/2DLn/wBFNWB8CP8AklOh/wDbf/0fJXmP/f4/4H/6UjvX+5v/ABL8md9RRRXpnAFFFFAGB8Qf+RC8Sf8AYMuf/RTVgfAj/klOh/8Abf8A9HyVv/EH/kQvEn/YMuf/AEU1YHwI/wCSU6H/ANt//R8leY/9/j/gf/pSO9f7m/8AEvyZ31FFFemcAUUUUAFFFFABRRRQBU1I/uF/3qza0dT/ANUn+9WdWsdjiq/EFFFFWYhRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQBFd3cFhaTXVzKkFtAjSSyyNtVFAyWJ7AAZr8yP2jPjNP8ZfH897E7roVlut9NgbjEeeZCP7zkZPoNo7V9B/tvfHH+z7L/AIV7o1xi5uVWXVpYzykZ5SHPq3DN/s4HIY18UV6GHp2XOz4bPMd7SX1am9Fv69vl+foFFFFdp8mFFFFABRRRQAUUUUAFFFFABX6Pfsl/GX/haXw9Sx1CfzPEOihba6LnLTR4/dy++QME/wB5Se4r84a7z4JfFK6+EHxC07X4d8loD5F7bof9dbsRvX6jAYe6isatP2kbdT1ssxn1OupP4Xo/8/kfqpRVXS9Ttda0201CxnS5srqJZ4JozlXRgCrD2IIq1XkH6cnfVBRRXB/Gr4s6f8HPAt3rl3smuz+5sbMtg3E5Hyr/ALo6sewB74ppNuyIqVI0oOc3ZI8e/bL+PX/CGaE3gvRLjbrmpxZvJo25tbY/w57O/T2XJ7qa+DK0fEfiHUPFmu32sarcNd6heytNNM38TH+QHQDoAAKzq9enBU42Py7HYyWNrOo9ui7IKKKK1PPCtLw14hvPCfiHTda0+Tyr2wuEuYW7blIIB9jjBHoazaKBpuLuj9dvBXiuz8deEtJ1+wObTUbdJ0GclCRyp91OQfcGtqvkX9gr4mfa9L1bwPeS/vbUm/sAx6xsQJUH0Yq3/A29K+uq8apHkk0fq+CxCxVCNXq9/XqFFFFZnaFFFFABRRRQAUUUUAFFFFABV7SzzIPpVGrumfff6VMtjSn8aNCiiisTvCiiigAooooAKKKKAMD4g/8AIheJP+wZc/8AopqwPgR/ySnQ/wDtv/6Pkrf+IP8AyIXiT/sGXP8A6KasD4Ef8kp0P/tv/wCj5K8x/wC/x/wP/wBKR3r/AHN/4l+TO+ooor0zgCiiigDA+IP/ACIXiT/sGXP/AKKasD4Ef8kp0P8A7b/+j5K3/iD/AMiF4k/7Blz/AOimrA+BH/JKdD/7b/8Ao+SvMf8Av8f8D/8ASkd6/wBzf+JfkzvqKKK9M4AooooAKKKKACiiigCnqf8Aqk/3qzq0tSH7hf8Ae/pWbWsdjiq/EFFFFWYhRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABXA/G74r2Xwd8AXuuXGyW9P7ixtWP+vnYHaPoMFj7A98V3VxcRWkEk80iQwxqXeR2CqqgZJJPQAV+Z37Svxpk+Mnj6We2dx4f07db6dEcjcufmlI9XIB9gFHat6NP2kvI8fM8asFRvH4nov8AP5HmOt6ze+ItXvNU1G4e7v7yVp55pDku7HJNUqKK9Y/Mm23dhRRRQAUUUUAFFFFABRRRQAUUUUAFFFFAH2r+wz8Zft1jP8PtUn/f2wa50tnPLx5zJEPdSSwHoW7LX13X5BeGPEd/4Q8Q6frWlzG31CxmWeGTsGBzgjuD0I7gkV+qfwv+IVh8UfA2l+I9PIWO7j/ew5yYZRw8Z+jZ+owe9ebiKfK+ZdT77JMb7al7Cb96O3p/wP8AI6HUtStdG065v76dLWzto2mmmkOFjRRlmJ9ABX5k/tC/Gi6+NHjqa+UvFodnug022bjbHnl2H99yMn0GB/DXtP7a3x6/tG6k+HuhXP8AosDhtXnjPEkgOVgz6KcFv9oAfwnPyLW2Hp8q52eVnWP9rL6tTfurfzf/AAPzCiiiuw+WCiiigAooooA6z4U+Pbj4ZfEHRPEdvuIs5wZo1P8ArIT8sifipOPfBr9XNPv7fVbC2vbSVZ7W5iWaGVejowBVh7EEGvx3r9Av2IfiZ/wlvw1l8OXUu7UfD7iNAx5a2fJjP/ASGX2AX1rixMLrmR9ZkGK5KksPLZ6r1/4b8j6Oooorzz7kKKKKACiiigAooooAKKKKACrumfff6VSq9pY5kP0qZbGlP4kX6KKKxO8KKKKACiiigAooooAwPiD/AMiF4k/7Blz/AOimrA+BH/JKdD/7b/8Ao+St/wCIP/IheJP+wZc/+imrA+BH/JKdD/7b/wDo+SvMf+/x/wAD/wDSkd6/3N/4l+TO+ooor0zgCiiigDA+IP8AyIXiT/sGXP8A6KasD4Ef8kp0P/tv/wCj5K3/AIg/8iF4k/7Blz/6KasD4Ef8kp0P/tv/AOj5K8x/7/H/AAP/ANKR3r/c3/iX5M76iiivTOAKKKKACiiigAooooAragM2x9iKy617xd1tIPbNZFax2OOsveCiiirMAooooAKKKKACiiigAooooAKKKKACiiuH+MvxTsPhB4DvtfvNss6jyrO1Jwbicg7E+nBJPYA00m3ZGdScaUXObskeD/tt/HD+xNKHgDRrjF9fIH1SSM8xQHlYvYv1P+zx0avh+tDxBr1/4p1u+1fU7hrrUL2Zp55n6sxOT9B6DsOKz69inBU42Py3HYuWMrOq9ui7IKKKK0OAKKKKACiiigAooooAKKKKACiiigAooooAK9U+D37QmvfBzQfEmmaYBNHqkP8Ao5c8WlzwvnAd/kzx3Kp2GD5XRUuKkrM1pVZ0Zc9N2Y+eeS5mkmmkaWWRi7yOxLMxOSST1JplFFUZBRRRQAUUUUAFFFFABXqP7NnxM/4Vd8WdJ1GeXy9Lu2+w32ThRDIQNx/3WCt/wE15dRSaUlZmtKpKjUjUjunc/ZDrRXj37KnxM/4WT8ItNe4l8zVdK/4l93k/MxQDY5/3k28+oavYa8SScW0z9ao1Y16casdmrhRRRSNgooooAKKKKACiiigArQ0wfJIfcVn1p6cuLcn1Y1Etjal8RaooorI7QooooAKKKKACiiigDA+IP/IheJP+wZc/+imrA+BH/JKdD/7b/wDo+St/4g/8iF4k/wCwZc/+imrA+BH/ACSnQ/8Atv8A+j5K8x/7/H/A/wD0pHev9zf+JfkzvqKKK9M4AooooAwPiD/yIXiT/sGXP/opqwPgR/ySnQ/+2/8A6Pkrf+IP/IheJP8AsGXP/opqwPgR/wAkp0P/ALb/APo+SvMf+/x/wP8A9KR3r/c3/iX5M76iiivTOAKKKKACiiigAooooAbIu+Nl9QRWJW7WLOuyZ19Ca0gc1ZbMZRRRWhyhRRRQAUUUUAFFFFABRRRQAUUUUANllSCJ5JHWONAWZ2OAoHUk9hX5qftPfGx/jD48kFlK3/COaWWg09Ogk5+eYj1cgY9FC9819DftsfHH/hG9D/4QTR7jbqepR7tRkjPMNsekfsX7/wCzn+8K+Fq9DD07e+z4jPMdzy+q03ot/Xt8v62Ciiiu0+RCiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigD339jH4mf8IN8Vo9JupdmmeIFFm4Y/Ks4JMLfXJKf9tK/ROvxzt7iS0njnhkaKaNg6SIcFWByCD65r9VPgp8RI/in8NNE8QBl+1TReXdov8ABcJ8sgx2yRkezCvPxMLNSR9vkGK5oSw8umq9Ov4/mdxRRRXEfXBRRRQAUUUUAFFFFABWvZrttkHtmsgDJAHWtxF2oo9Bis5nRRWrYtFFFZnWFFFFABRRRQAUUUUAYHxB/wCRC8Sf9gy5/wDRTVgfAj/klOh/9t//AEfJW/8AEH/kQvEn/YMuf/RTVgfAj/klOh/9t/8A0fJXmP8A3+P+B/8ApSO9f7m/8S/JnfUUUV6ZwBRRRQBgfEH/AJELxJ/2DLn/ANFNWB8CP+SU6H/23/8AR8lb/wAQf+RC8Sf9gy5/9FNWB8CP+SU6H/23/wDR8leY/wDf4/4H/wClI71/ub/xL8md9RRRXpnAFFFFABRRRQAUUUUAFZmoptuM/wB4ZrTqnqUe6JX/ALpqo7mVVXiZ1FFFbHCFFFFABRRRQAUUUUAFFFFABXG/Fz4maf8ACTwJqHiK/wASNCvl21tuwbids7Ix9epPYAntXYu6xozuwVVGSxOABX5t/tT/ABub4u+O2t9PmLeGtJLQWQU/LO2fnn/4ERgf7IHQk1tSp+0l5Hk5ljVgqLkviei/z+R5P4n8Sah4w8QahrWqzm51C+maaaQ92PYDsBwAOwAFZlFFevsfmLbk7vcKKKKBBRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAV9U/sH/Ez+x/Feo+DLuXFrqqm6swx4Fwi/MB/vIM/9sxXytWl4a8Q3nhPxDputafJ5V7YXCXMLdtykEA+xxgj0NROPPFxOzB4h4WvGqun5dT9fqKxfBXiuz8deEtJ1+wObTUbdJ0GclCRyp91OQfcGtqvF2P1eMlJKUdmFFFFBQUUUUAFFFFAE1onmXCDsDmtes/TI8u7+gwK0Kxk9TspK0bhRRRUm4UUUUAFFFFABRRRQBgfEH/kQvEn/AGDLn/0U1YHwI/5JTof/AG3/APR8lb/xB/5ELxJ/2DLn/wBFNWB8CP8AklOh/wDbf/0fJXmP/f4/4H/6UjvX+5v/ABL8md9RRRXpnAFFFFAGB8Qf+RC8Sf8AYMuf/RTVgfAj/klOh/8Abf8A9HyVv/EH/kQvEn/YMuf/AEU1YHwI/wCSU6H/ANt//R8leY/9/j/gf/pSO9f7m/8AEvyZ31FFFemcAUUUUAFFFFABRRRQAUyePzYmX1HFPooE9TC6UVPexeXcN6HkVBW55zVnYKKKKYgooooAKKKKACiiuS+KfxH074U+CNR8RakQyW67YIM4aeY/cjH1PX0AJ7UJNuyInONOLnJ2SPDv20/jj/wiPh3/AIQrSJ8axq0Wb2RDzb2pyCvs0nI/3c+oNfBta/i3xVqPjfxLqOu6tObjUL6YzSv2yeigdlAwAOwAFZFexTh7ONj8tx+MljazqPbovIKKKK1PPCiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKAPtb9gr4mfa9L1bwPeS/vbUm/sAx6xsQJUH0Yq3/AANvSvrqvyb+FPj24+GXxB0TxHb7iLOcGaNT/rIT8sifipOPfBr9XNPv7fVbC2vbSVZ7W5iWaGVejowBVh7EEGvMxEOWV+5+hZHivbYf2Ut4fl0/yLFFFFcp9GFFFFABRRT4YzLKqDuaQbmnZR+Xbr6tzU9AGBgdKKwep6KVlYKKKKCgooooAKKKKACiiigDA+IP/IheJP8AsGXP/opqwPgR/wAkp0P/ALb/APo+St/4g/8AIheJP+wZc/8AopqwPgR/ySnQ/wDtv/6PkrzH/v8AH/A//Skd6/3N/wCJfkzvqKKK9M4AooooAwPiD/yIXiT/ALBlz/6KasD4Ef8AJKdD/wC2/wD6Pkrf+IP/ACIXiT/sGXP/AKKasD4Ef8kp0P8A7b/+j5K8x/7/AB/wP/0pHev9zf8AiX5M76iiivTOAKKKKACiiigAooooAKKKKAKmoxb4g46r/Ks2txlDKQeQeDWNLGYpGQ9jWkX0OStGzuMooorQ5wooooAKKKKAEJCgknAHUmvzj/aw+N5+K/jc6fps5bw1o7NFbbT8txL0eb3Bxhf9kZ43Gvoj9s344f8ACD+Fx4R0i42a5rER+0SRt81tang/Rn5UewY8cV8CV34en9tnxWeY67+q036/5BRRRXcfHhRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAV+gX7EPxM/wCEt+Gsvhy6l3aj4fcRoGPLWz5MZ/4CQy+wC+tfn7XqP7NnxM/4Vd8WdJ1GeXy9Lu2+w32ThRDIQNx/3WCt/wABNY1oc8Gj1csxX1XExk9no/mfp/RR1oryD9QCiiigAq9psWS0h7cCqSgsQByTxWzDEIYlQdqiT0NqUbu4+iiisjtCiiigAooooAKKKKACiiigDA+IP/IheJP+wZc/+imrA+BH/JKdD/7b/wDo+St/4g/8iF4k/wCwZc/+imrA+BH/ACSnQ/8Atv8A+j5K8x/7/H/A/wD0pHev9zf+JfkzvqKKK9M4AooooAwPiD/yIXiT/sGXP/opqwPgR/ySnQ/+2/8A6Pkrf+IP/IheJP8AsGXP/opqwPgR/wAkp0P/ALb/APo+SvMf+/x/wP8A9KR3r/c3/iX5M76iiivTOAKKKKACiiigAooooAKKKKACqWpQ5AkHbg1dpHUOpU8g8U07MiUeZWMOinzRGGQoe3emVsee1bQKKKKYBXL/ABL+IOm/C/wXqXiPVGzBaJ8kIOGnkPCRr7sfyGT0BrqOlfnX+1z8cD8UPGn9jaXcb/DWjSNHEyH5bmfo8vuByq+2SPvVrSp+0lboeXmONWCouf2novX/AIB49418Yal4+8U6l4g1aXzr++lMshHRR0VF9FUAKB6AViUUV7Gx+Xyk5NyluwooooEFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQB+l/wCyp8TP+Fk/CLTXuJfM1XSv+Jfd5PzMUA2Of95NvPqGr2Gvzs/Yx+Jn/CDfFaPSbqXZpniBRZuGPyrOCTC31ySn/bSv0TryK0OSZ+m5VivrWGi3utH/AF6BRRSqpdgoGSeKxPYLenQ7nMh6L0+taNMhiEMaoO1PrBu7O+EeVWCiiikaBRRRQAUUUUAFFFFABRRRQBgfEH/kQvEn/YMuf/RTVgfAj/klOh/9t/8A0fJW/wDEH/kQvEn/AGDLn/0U1YHwI/5JTof/AG3/APR8leY/9/j/AIH/AOlI71/ub/xL8md9RRRXpnAFFFFAGB8Qf+RC8Sf9gy5/9FNWB8CP+SU6H/23/wDR8lb/AMQf+RC8Sf8AYMuf/RTVgfAj/klOh/8Abf8A9HyV5j/3+P8Agf8A6UjvX+5v/EvyZ31FFFemcAUUUUAFFFFABRRRQAUUUUAFFFFAFW/t/Nj3gfMv6isyt2su9t/Jk3KPkb9DWkX0OWrD7SK1FFc58Q/Hem/DXwdqXiLVX22tlGWEYOGlc8JGv+0xwB9cngVolfRHJKShFyk7JHi37Y/xx/4V94S/4RfSbjZ4g1mIiR0PzW1qeGb2Z+VHtuPBAr8+63/HnjbU/iL4t1LxDq8vmXt7KXIH3Y16Ki/7KgAD6VgV7FKHs42Py7MMY8bWc+i0XoFFFFanmhRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFAElvcSWk8c8MjRTRsHSRDgqwOQQfXNfqp8FPiJH8U/hponiAMv2qaLy7tF/guE+WQY7ZIyPZhX5T19U/sH/Ez+x/Feo+DLuXFrqqm6swx4Fwi/MB/vIM/wDbMVzYiHNG/Y+gyTFewxHs5bT0+fT/AC+Z9z1f063x+9b6LVa2gNxJj+Eck1rgBQABgDtXjyfQ/TaULvmYUUUVmdYUUUUAFFFFABRRRQAUUUUAFFFFAGB8Qf8AkQvEn/YMuf8A0U1YHwI/5JTof/bf/wBHyVv/ABB/5ELxJ/2DLn/0U1YHwI/5JTof/bf/ANHyV5j/AN/j/gf/AKUjvX+5v/EvyZ31FFFemcAUUUUAYHxB/wCRC8Sf9gy5/wDRTVgfAj/klOh/9t//AEfJW/8AEH/kQvEn/YMuf/RTVgfAj/klOh/9t/8A0fJXmP8A3+P+B/8ApSO9f7m/8S/JnfUUUV6ZwBRRRQAUUUUAFFFFABRRRQAUUUUAFNljEqFW6GnUUC3MWaJoZCrdu/rXC/FH4P6D8X7GysvELXr2dpIZUgtrgxIzkY3MB1IGQPTJ9a9OurYXCejDoayWUqSCMEdq3jJ7rc86vRjJOE1eLPAf+GIvhh/z6an/AOBzf4Uf8MRfDD/n01P/AMDm/wAK99orX2s+5539n4T/AJ9L7jwL/hiL4Yf8+mp/+Bzf4Uf8MRfDD/n01P8A8Dm/wr32ij2s+4f2fhP+fS+48C/4Yi+GH/Ppqf8A4HN/hR/wxF8MP+fTU/8AwOb/AAr32ij2s+4f2fhP+fS+48C/4Yi+GH/Ppqf/AIHN/hR/wxF8MP8An01P/wADm/wr32ij2s+4f2fhP+fS+48C/wCGIvhh/wA+mp/+Bzf4Uf8ADEXww/59NT/8Dm/wr32ij2s+4f2fhP8An0vuPAv+GIvhh/z6an/4HN/hR/wxF8MP+fTU/wDwOb/CvfaKPaz7h/Z+E/59L7jwL/hiL4Yf8+mp/wDgc3+FH/DEXww/59NT/wDA5v8ACvfaKPaz7h/Z+E/59L7jwL/hiL4Yf8+mp/8Agc3+FH/DEXww/wCfTU//AAOb/CvfaKPaz7h/Z+E/59L7jwL/AIYi+GH/AD6an/4HN/hR/wAMRfDD/n01P/wOb/CvfaKPaz7h/Z+E/wCfS+48C/4Yi+GH/Ppqf/gc3+FH/DEXww/59NT/APA5v8K99oo9rPuH9n4T/n0vuPAv+GIvhh/z6an/AOBzf4Uf8MRfDD/n01P/AMDm/wAK99oo9rPuH9n4T/n0vuPAv+GIvhh/z6an/wCBzf4Uf8MRfDD/AJ9NT/8AA5v8K99oo9rPuH9n4T/n0vuPAv8AhiL4Yf8APpqf/gc3+FH/AAxF8MP+fTU//A5v8K99oo9rPuH9n4T/AJ9L7jwL/hiL4Yf8+mp/+Bzf4Uf8MRfDD/n01P8A8Dm/wr32ij2s+4f2fhP+fS+48C/4Yi+GH/Ppqf8A4HN/hR/wxF8MP+fTU/8AwOb/AAr32ij2s+4f2fhP+fS+48C/4Yi+GH/Ppqf/AIHN/hR/wxF8MP8An01P/wADm/wr32ij2s+4f2fhP+fS+48C/wCGIvhh/wA+mp/+Bzf4Uf8ADEXww/59NT/8Dm/wr32ij2s+4f2fhP8An0vuPAv+GIvhh/z6an/4HN/hR/wxF8MP+fTU/wDwOb/CvfaKPaz7h/Z+E/59L7jwL/hiL4Yf8+mp/wDgc3+FH/DEXww/59NT/wDA5v8ACvfaKPaz7h/Z+E/59L7jwL/hiL4Yf8+mp/8Agc3+FH/DEXww/wCfTU//AAOb/CvfaKPaz7h/Z+E/59L7jwL/AIYi+GH/AD6an/4HN/hWl4b/AGPvh/4Z1/T9W0uLVINRsp0ngkF6xw6nIyMcj1HcV7XWnZWvkrvYfOf0qXVmlua08uwrkmqa08ia3gEEYUde5qSiiuU91KysgooooGFFFFABRRRQAUUUUAFFFFABRRRQBgfEH/kQvEn/AGDLn/0U1YHwI/5JTof/AG3/APR8lb/xB/5ELxJ/2DLn/wBFNWB8CP8AklOh/wDbf/0fJXmP/f4/4H/6UjvX+5v/ABL8md9RRRXpnAFFFFAGB8Qf+RC8Sf8AYMuf/RTVgfAj/klOh/8Abf8A9HyVv/EH/kQvEn/YMuf/AEU1YHwI/wCSU6H/ANt//R8leY/9/j/gf/pSO9f7m/8AEvyZ31FFFemcAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAVVvbTzhvUfOP1q1RQnYmSUlZmF0orSvLPzMug+fuPWs3pWydzhlFxdmFFFFUQFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUVes7PJEkg47KaTdioxcnZC2NpjEjjn+Ef1q9RRWLdzujFRVkFFFFIsKKKKACiiigAooooAKKKKACiiigAooooAwPiD/yIXiT/ALBlz/6KasD4Ef8AJKdD/wC2/wD6Pkrf+IP/ACIXiT/sGXP/AKKasD4Ef8kp0P8A7b/+j5K8x/7/AB/wP/0pHev9zf8AiX5M76iiivTOAKKKKAMD4g/8iF4k/wCwZc/+imrA+BH/ACSnQ/8Atv8A+j5K3/iD/wAiF4k/7Blz/wCimrA+BH/JKdD/AO2//o+SvMf+/wAf8D/9KR3r/c3/AIl+TO+ooor0zgCiiigAooooAKKKKACiiigAooooAKKKKACiiigAqrd2Qly6cP3HrVqihOxMoqSszDIKkgjBHY0la1zaLcDP3XHQ1mSRNC21hg1snc4pwcBlFFFUZhRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRSqpdgqjJPYVpWtkIcM/zP+gqW7Fxg5PQjtLHGHkHPZavUUVk3c7YxUVZBRRRSLCiiigAooooAKKKKACiiigAooooAKKKKACiiigDA+IP/ACIXiT/sGXP/AKKasD4Ef8kp0P8A7b/+j5K3/iD/AMiF4k/7Blz/AOimrA+BH/JKdD/7b/8Ao+SvMf8Av8f8D/8ASkd6/wBzf+JfkzvqKKK9M4AooooAwPiD/wAiF4k/7Blz/wCimrA+BH/JKdD/AO2//o+St/4g/wDIheJP+wZc/wDopqwPgR/ySnQ/+2//AKPkrzH/AL/H/A//AEpHev8Ac3/iX5M76iiivTOAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigApksKTLtcZ/pT6KBbmVcWbwcj5k9ar1u1UuNPV8tHhW9O1aKXc5p0usTNop0kbRNh1Kmm1ocwUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUqqXYBQST2FACVLBbPcHgYXuxq1b6d/FL/3yKvABQABgDsKzcux0QpN6yIoLdLdcKOe5PU1LRRWZ1JJaIKKKKBhRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAYHxB/5ELxJ/wBgy5/9FNWB8CP+SU6H/wBt/wD0fJW/8Qf+RC8Sf9gy5/8ARTVgfAj/AJJTof8A23/9HyV5j/3+P+B/+lI71/ub/wAS/JnfUUUV6ZwBRRRQBgfEH/kQvEn/AGDLn/0U1YHwI/5JTof/AG3/APR8lb/xB/5ELxJ/2DLn/wBFNWB8CP8AklOh/wDbf/0fJXmP/f4/4H/6UjvX+5v/ABL8md9RRRXpnAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFADXjWRdrAMPeqU2mkcxnP+yav0U02iJQUtzDdGRsMCD6GkrbeNZBhlDD3qpLpoPMbY9jWikjmlSa2M+ipJLeSL7ykD17VHVGLTW4UUUUxBRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUU+OF5T8ilqQWvsMpQCxwASfQVdi009ZGx7LVyOFIRhFA96lyRtGk3uUYdOZ+ZDtHp3q9FCkIwi496fRWbbZ0xgo7BRRRSNAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAwPiD/AMiF4k/7Blz/AOimrA+BH/JKdD/7b/8Ao+St/wCIP/IheJP+wZc/+imrA+BH/JKdD/7b/wDo+SvMf+/x/wAD/wDSkd6/3N/4l+TO+ooor0zgCiiigDA+IP8AyIXiT/sGXP8A6KasD4Ef8kp0P/tv/wCj5K3/AIg/8iF4k/7Blz/6KasD4Ef8kp0P/tv/AOj5K8x/7/H/AAP/ANKR3r/c3/iX5M76iiivTOAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKhktIpOqAH1HFTUUCaT3KEmmf3H/BqgezmT+An6c1rUVXMzJ0oswyCpwRg+9JW6VDDBAP1qJrSFusY/Diq5zJ0X0Zj0Vptp0R6bl+hph0xe0hH1FPmRPspGfRV46Wf+en/jtJ/Zj/AN9afMifZy7FKirv9mP/AH1pRpZ7yD8qOZB7OXYo0VoDSx3kP5U9dNiHUsfxpcyH7KRmUAZPHNa62cKdEB+vNSqip91QPoKXOWqL6syUtJn6IR9eKnTTGP33A9hzWhRU8zNFSityCOyij/h3H1bmpwMdKKKm9zVJLYKKKKCgooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigDA+IP/IheJP8AsGXP/opqwPgR/wAkp0P/ALb/APo+St/4g/8AIheJP+wZc/8AopqwPgR/ySnQ/wDtv/6PkrzH/v8AH/A//Skd6/3N/wCJfkzvqKKK9M4AooooAwPiD/yIXiT/ALBlz/6KasD4Ef8AJKdD/wC2/wD6Pkrf+IP/ACIXiT/sGXP/AKKasD4Ef8kp0P8A7b/+j5K8x/7/AB/wP/0pHev9zf8AiX5M76iiivTOAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigDA+IP/IheJP8AsGXP/opqwPgR/wAkp0P/ALb/APo+St/4g/8AIheJP+wZc/8AopqwPgR/ySnQ/wDtv/6PkrzH/v8AH/A//Skd6/3N/wCJfkzvqKKK9M4AooooAwPiD/yIXiT/ALBlz/6KasD4Ef8AJKdD/wC2/wD6Pkrf+IP/ACIXiT/sGXP/AKKasD4Ef8kp0P8A7b/+j5K8x/7/AB/wP/0pHev9zf8AiX5M76iiivTOAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACi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	Kỷ niệm hành trình khám phá đỉnh Fansipan.	ACTIVE
3	18	Cánh đồng Mường Than	\N	Vào mùa lúa chín, cánh đồng Mường Than rực rỡ sắc vàng, tạo nên một bức tranh thiên nhiên thơ mộng	DRAFT
4	19	Hồ Thủy Điện Lai Châu	\N	Hồ nước nhân tạo khổng lồ được tạo nên từ công trình Thủy điện Lai Châu, nằm giữa khung cảnh núi non thơ mộng.	DRAFT
5	20	Chợ phiên Sìn Hồ	\N	Nơi diễn ra các hoạt động mua bán nhộn nhịp của các dân tộc Thái, H’Mông, Dao, Lự...	DRAFT
6	26	Động Tam Thanh	\N	Được mệnh danh là "Đệ nhất bát cảnh Xứ Lạng"các nhũ đá kỳ thú và nhiều di tích cổ như tượng Phật A Di Đà cao 2m được tạc từ thế kỷ 15.	DRAFT
7	27	Chợ Đông Kinh	\N	Là trung tâm thương mại lớn nhất ở Lạng Sơn, nổi tiếng với các mặt hàng phong phú	DRAFT
8	28	Cửa khẩu Hữu Nghị	\N	Đây là cửa khẩu quốc tế lớn giữa Việt Nam và Trung Quốc, nằm trên tuyến quốc lộ 1A.	DRAFT
9	29	Đèo Bông Lau	\N	Là một cung đèo đẹp với những khúc cua uốn lượn và khung cảnh thiên nhiên ngoạn mục.	DRAFT
10	30	Chùa Tiên (Lạng Sơn)	\N	Chùa Tiên nằm trên núi Đại Tượng, gắn liền với nhiều câu chuyện huyền thoại và mang đậm dấu ấn Phật giáo.	DRAFT
12	32	Cổng Trời Ô Quy Hồ	\N	Đèo Ô Quy Hồ là một trong "Tứ đại đỉnh đèo" của Tây Bắc, nối liền Lào Cai và Lai Châu.	DRAFT
91	255	Tượng đài chiến thắng Điện Biên Phủ	\N	Tượng đài nằm trên đồi D1, là biểu tượng của chiến thắng lịch sử Điện Biên Phủ. Đây là nơi tôn vinh sự hy sinh và chiến công của các chiến sĩ Việt Nam, đồng thời cũng là điểm ngắm toàn cảnh thành phố Điện Biên.	DRAFT
76	271	Cột cờ Lũng Cú	\N	Cột cờ Lũng Cú, nằm ở cực Bắc Việt Nam (Hà Giang), là biểu tượng thiêng liêng của chủ quyền quốc gia. Du khách chinh phục gần 800 bậc thang để chiêm ngưỡng lá cờ đỏ sao vàng tung bay trên đỉnh núi hùng vĩ.	DRAFT
92	272	Đèo Mã Pí Lèng	\N	Đèo Mã Pí Lèng là cung đường hùng vĩ tại Hà Giang, với sông Nho Quế xanh ngọc chảy uốn lượn phía dưới. Đây là điểm thu hút phượt thủ và du khách yêu vẻ đẹp thiên nhiên hoang sơ.	DRAFT
94	274	Hẻm Tu Sản	\N	Hẻm Tu Sản, nơi sâu nhất Đông Nam Á, nổi bật với vách đá cao 800m và dòng sông Nho Quế xanh biếc. Du khách có thể chèo thuyền khám phá khung cảnh kỳ vĩ này.	DRAFT
100	280	Làng Vũ Đại	https://thumb.wikimedia.org/wikipedia/commons/thumb/4/4c/%C4%90%C3%ACnh_l%C3%A0ng_V%C5%A9_%C4%90%E1%BA%A1i.JPG/960px-%C4%90%C3%ACnh_l%C3%A0ng_V%C5%A9_%C4%90%E1%BA%A1i.JPG	Làng Vũ Đại (Hà Nam) nổi tiếng nhờ sự gắn kết với tác phẩm "Chí Phèo" của nhà văn Nam Cao. Nơi đây có những ngôi nhà cổ, là điểm du lịch văn hóa, nơi lưu giữ nét đẹp văn hóa truyền thống.	ACTIVE
93	273	Phố cổ Đồng Văn	https://thumb.wikimedia.org/wikipedia/commons/thumb/5/55/Dong_Van_old_town.jpg/960px-Dong_Van_old_town.jpg	Phố cổ Đồng Văn (Hà Giang) lưu giữ nét kiến trúc cổ kính với những ngôi nhà tường đất, mái ngói âm dương. Phiên chợ cuối tuần nhộn nhịp là điểm nhấn văn hóa hấp dẫn.	ACTIVE
150	211	Bà Nà Hills	https://thumb.wikimedia.org/wikipedia/commons/thumb/a/a5/The_Golden_Bridge%2C_Ba_Na_Hills%2C_Vietnam.jpg/960px-The_Golden_Bridge%2C_Ba_Na_Hills%2C_Vietnam.jpg	Khu nghỉ dưỡng trên đỉnh núi với khí hậu mát mẻ, nổi tiếng với cây Cầu Vàng độc đáo, làng Pháp cổ điển và khu vui chơi trong nhà Fantasy Park.	ACTIVE
128	4	VinWonders Nha Trang	https://upload.wikimedia.org/wikipedia/commons/a/ad/Vinpearl_Land_Nha_Trang_Ferris_wheel.jpg	Thẻ sưu tầm dữ liệu mẫu cho địa danh VinWonders Nha Trang	ACTIVE
90	254	Cánh đồng Mường Thanh	https://thumb.wikimedia.org/wikipedia/commons/thumb/a/a1/Panorama_of_Ricefields_in_Muong_Thanh_Valley_-_Dien_Bien_Phu_-_Vietnam_-_02_%2848178461326%29.jpg/960px-Panorama_of_Ricefields_in_Muong_Thanh_Valley_-_Dien_Bien_Phu_-_Vietnam_-_02_%2848178461326%29.jpg	Cánh đồng lúa lớn nhất vùng Tây Bắc, nổi tiếng với vẻ đẹp thơ mộng và vai trò quan trọng trong lịch sử chiến dịch Điện Biên Phủ. Cảnh quan ở đây thay đổi theo mùa vụ, thu hút nhiều du khách đến chiêm ngưỡng.	ACTIVE
1	16	Đèo Ô Quy Hồ	https://thumb.wikimedia.org/wikipedia/commons/thumb/a/a0/Sunset_on_O_Quy_Ho_pass.jpg/960px-Sunset_on_O_Quy_Ho_pass.jpg	Một trong "Tứ đại đỉnh đèo" của Tây Bắc, Đèo Ô Quy Hồ nối liền Lào Cai và Lai Châu, nổi tiếng với cảnh sắc tuyệt đẹp và độ cao hùng vĩ	ACTIVE
47	117	Đồi chè Tân Cương	https://thumb.wikimedia.org/wikipedia/commons/thumb/a/a4/Tan_Cuong_Tea_culture.jpg/960px-Tan_Cuong_Tea_culture.jpg	Một trong những địa điểm du lịch Thái Nguyên nổi tiếng chắc chắn không thể quên kể đến đó chính là đồi chè Tân Cương. Đây là một địa điểm thu hút sự quan tâm từ đông đảo khách du lịch nhờ vào cảnh quan thiên nhiên tuyệt đẹp, được bao phủ bởi một sắc xanh mát lạnh. Bất cứ ai đến đồi chè Tân Cương du lịch cũng đều có thể cảm nhận được sự tươi mới từ sâu trong tâm hồn, giúp bạn cảm thấy dễ chịu hơn bao giờ hết.	ACTIVE
116	306	Thung Nai	https://thumb.wikimedia.org/wikipedia/commons/thumb/a/a4/Thung_Nai%2C_Cao_Phong%2C_H%C3%B2a_B%C3%ACnh%2C_Vietnam_-_panoramio.jpg/960px-Thung_Nai%2C_Cao_Phong%2C_H%C3%B2a_B%C3%ACnh%2C_Vietnam_-_panoramio.jpg	Thung Nai (Hòa Bình) là khu du lịch nổi tiếng với cảnh sắc thiên nhiên hùng vĩ, bao gồm hồ nước rộng lớn và các dãy núi bao quanh. Nơi đây là địa điểm lý tưởng cho du khách yêu thích du lịch sinh thái và tham gia các hoạt động ngoài trời như chèo thuyền.	ACTIVE
112	297	Cát Bà	https://thumb.wikimedia.org/wikipedia/commons/thumb/2/27/Cat_Ba_Island.jpg/960px-Cat_Ba_Island.jpg	Cát Bà (Hải Phòng) là hòn đảo nổi tiếng với bãi biển tuyệt đẹp và hệ sinh thái đa dạng. Nơi đây là một điểm đến lý tưởng cho những ai yêu thích du lịch sinh thái và biển.	ACTIVE
111	296	Đồ Sơn	https://thumb.wikimedia.org/wikipedia/commons/thumb/c/cb/%C4%90%E1%BB%93_S%C6%A1n_beach_-_panoramio.jpg/960px-%C4%90%E1%BB%93_S%C6%A1n_beach_-_panoramio.jpg	Đồ Sơn (Hải Phòng) là một bãi biển đẹp nổi tiếng với những dãy núi và biển xanh. Đây là khu du lịch biển với các hoạt động thể thao nước và thư giãn dưới ánh nắng.	ACTIVE
83	237	Hồ Thác Bà	https://thumb.wikimedia.org/wikipedia/commons/thumb/0/0d/Th%C3%A1c_B%C3%A0_lake_-_panorama.jpg/960px-Th%C3%A1c_B%C3%A0_lake_-_panorama.jpg	Hồ Thác Bà là một trong những hồ nước nhân tạo lớn nhất Việt Nam, được bao quanh bởi hàng trăm hòn đảo nhỏ và cảnh quan núi non tuyệt đẹp. Du khách có thể tham gia các hoạt động như chèo thuyền, cắm trại, hoặc khám phá hang động quanh hồ.	ACTIVE
67	207	Động Ngườm Ngao	https://thumb.wikimedia.org/wikipedia/commons/thumb/6/6b/Nguom_Ngao_Cave_-_Cao_Bang_Province_-_Vietnam_-_03_%2848119821128%29.jpg/960px-Nguom_Ngao_Cave_-_Cao_Bang_Province_-_Vietnam_-_03_%2848119821128%29.jpg	Hang động dài hơn 2.1km, nổi bật với hệ thống thạch nhũ độc đáo và cảnh quan kỳ ảo. Động nằm gần thác Bản Giốc, là điểm đến lý tưởng để khám phá.	ACTIVE
66	206	Thác Bản Giốc	https://thumb.wikimedia.org/wikipedia/commons/thumb/1/13/Ban_Gioc.jpg/960px-Ban_Gioc.jpg	Một trong những thác nước đẹp nhất và lớn nhất Đông Nam Á, nằm trên biên giới Việt Nam - Trung Quốc. Thác được chia thành nhiều tầng với dòng nước trong xanh, tạo nên khung cảnh hùng vĩ.	ACTIVE
58	158	Thác Đầu Đẳng	https://thumb.wikimedia.org/wikipedia/commons/thumb/7/7d/Panorama_of_Dau_Dang_Waterfall_-_Near_Ba_Be_Lake_-_Ba_Be_National_Park_-_Vietnam_%2848102307023%29.jpg/960px-Panorama_of_Dau_Dang_Waterfall_-_Near_Ba_Be_Lake_-_Ba_Be_National_Park_-_Vietnam_%2848102307023%29.jpg	Đây là địa điểm du lịch sinh thái lý tưởng.	ACTIVE
57	157	Động Puông	https://thumb.wikimedia.org/wikipedia/commons/thumb/7/79/Hang_Puong_Cave_-_Ba_Be_Lake_-_Ba_Be_National_Park_-_Vietnam_-_01_%2848102406852%29.jpg/960px-Hang_Puong_Cave_-_Ba_Be_Lake_-_Ba_Be_National_Park_-_Vietnam_-_01_%2848102406852%29.jpg	Là một hang động đẹp với những thạch nhũ kỳ vĩ	ACTIVE
56	156	Hồ Ba Bể	https://thumb.wikimedia.org/wikipedia/commons/thumb/7/71/Ba_Be_Lake_5.jpg/960px-Ba_Be_Lake_5.jpg	Hồ nước ngọt tự nhiên thuộc Vườn quốc gia Ba Bể, khu vực Bắc Kạn, bao quanh bởi núi đá vôi và rừng. Địa danh gắn với cảnh quan hồ, hoạt động đi thuyền và bản làng ven hồ.	ACTIVE
46	116	Hồ Núi Cốc	https://thumb.wikimedia.org/wikipedia/commons/thumb/4/4b/Nui_Coc_Lake_25.jpg/960px-Nui_Coc_Lake_25.jpg	Du khách có thể trải nghiệm ngồi thuyền thúng, khám phá động Thế giới Cổ tích và Âm phủ, động Ba Cây Thông, tham quan quần thể Thuyết Nhân Quả - Chùa Thiêng Thác Vàng, ngôi chùa nằm trong lòng Phật lớn nhất miền Bắc được ghi nhận kỷ lục Guniess.	ACTIVE
36	101	Mộc Châu	https://thumb.wikimedia.org/wikipedia/commons/thumb/6/67/Nha_san_Moc_Chau.jpg/960px-Nha_san_Moc_Chau.jpg	với đặc sản là những mùa hoa ban, hoa anh đào, hoa mận, hoa cải vàng… tràn ngập hương sắc. Mộc Châu cũng là cái tên kinh điển nhất khi nhắc đến du lịch vùng Tây Bắc với cung đường phượt đẹp như tranh vẽ	ACTIVE
104	284	Hoàng thành Thăng Long	https://thumb.wikimedia.org/wikipedia/commons/thumb/9/9f/Central_Sector_of_the_Imperial_Citadel_of_Thang_Long_-_Hanoi_%2826246667851%29.jpg/960px-Central_Sector_of_the_Imperial_Citadel_of_Thang_Long_-_Hanoi_%2826246667851%29.jpg	Hoàng Thành Thăng Long (Hà Nội) là di tích lịch sử quan trọng của Việt Nam, từng là trung tâm quyền lực của nhiều triều đại. Di tích này mang đậm dấu ấn lịch sử và văn hóa dân tộc.	ACTIVE
13	33	Chợ phiên Bắc Hà	\N	Chợ phiên Bắc Hà nổi tiếng với các mặt hàng thổ cẩm, nông sản và các món ăn đặc trưng của người dân tộc H’Mông, Dao.	DRAFT
14	34	Thác Tình Yêu	\N	Là một điểm đến lãng mạn với dòng nước trong xanh chảy qua rừng nguyên sinh.	DRAFT
15	35	Đền Thượng	\N	Là nơi thờ Trần Hưng Đạo, vị tướng tài ba trong lịch sử Việt Nam.	DRAFT
16	41	Nhà thờ Phú Nhai	\N	Là một trong những nhà thờ lớn và cổ kính nhất Việt Nam, mang phong cách kiến trúc Gothic đặc sắc.	DRAFT
102	282	Hồ Gươm	https://thumb.wikimedia.org/wikipedia/commons/thumb/8/83/Ho_Hoan_Kiem.jpg/960px-Ho_Hoan_Kiem.jpg	Hồ Gươm (Hà Nội) là trung tâm của thủ đô, nổi bật với Tháp Rùa và cầu Thê Húc. Đây là điểm du lịch hấp dẫn, nơi người dân thư giãn và là biểu tượng của Hà Nội.	ACTIVE
85	239	Bản Lìm Mông	\N	Bản Lìm Mông là một ngôi làng nhỏ nằm giữa những ngọn núi và ruộng bậc thang tuyệt đẹp. Đây là nơi du khách có thể trải nghiệm cuộc sống của người H'Mông, tận hưởng sự yên bình và không khí trong lành.	DRAFT
127	2	Tháp Chăm Ponagar	data:image/jpeg;base64,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	Dấu ấn kiến trúc Chăm tại Tháp Bà Ponagar.	ACTIVE
95	275	Dinh Vua Mèo	\N	Dinh Vua Mèo (Hà Giang) là dinh thự cổ của dòng họ Vương, với kiến trúc kết hợp độc đáo giữa Trung Hoa, Pháp và văn hóa dân tộc H'Mông, thu hút nhiều du khách tham quan.	DRAFT
97	277	Đền Trần Thương	\N	Đền Trần Thương (Hà Nam) thờ các vị vua Trần, nổi bật với kiến trúc cổ kính và không gian thanh tịnh. Đền là điểm đến văn hóa, tâm linh, thu hút du khách vào mùa lễ hội.	DRAFT
98	278	Hang Luồn – Ao Dong (Hà Nam)	\N	Hang Luồn trong khu Ao Dong, khu vực Liên Sơn, Kim Bảng, Hà Nam, là hang xuyên thủy giữa cảnh quan núi đá vôi và mặt nước.	DRAFT
99	279	Đền Lảnh Giang	\N	Đền Lảnh Giang tại khu vực Mộc Nam, Duy Tiên, Hà Nam, là di tích tín ngưỡng thờ các vị thủy thần gắn với truyền thuyết thời Hùng Vương.	DRAFT
129	6	Ngọc đảo Phú Quốc	data:image/jpeg;base64,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cDkEAj0eilKrOTUm9jSll2FpUpUYw0lvfW/zPibTf2GPiFo6yabYfExbHRXJ3R2z3EYYHqTEGC/+PV9keGtGHh3w5pWkiX7QLC0itfNK7d+xAu7GTjOM4zWjRROrKp8Q8LgKGDbdFPXzb/MKKKKyPRCiiigAooooAKKKKACiiigAooooAKKKKACiiigDA+IP/IheJP8AsGXP/opqwPgR/wAkp0P/ALb/APo+St/4g/8AIheJP+wZc/8AopqwPgR/ySnQ/wDtv/6PkrzH/v8AH/A//Skd6/3N/wCJfkzvqKKK9M4AooooAwPiD/yIXiT/ALBlz/6KasD4Ef8AJKdD/wC2/wD6Pkrf+IP/ACIXiT/sGXP/AKKasD4Ef8kp0P8A7b/+j5K8x/7/AB/wP/0pHev9zf8AiX5M76iiivTOAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigDA+IP/IheJP8AsGXP/opqwPgR/wAkp0P/ALb/APo+St/4g/8AIheJP+wZc/8AopqwPgR/ySnQ/wDtv/6PkrzH/v8AH/A//Skd6/3N/wCJfkzvqKKK9M4AooooAwPiD/yIXiT/ALBlz/6KasD4Ef8AJKdD/wC2/wD6Pkrf+IP/ACIXiT/sGXP/AKKasD4Ef8kp0P8A7b/+j5K8x/7/AB/wP/0pHev9zf8AiX5M76iiivTOAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKrTanaQMivcRhnlECqDkmQjO3A745+nNJtLcEm9izRRRTAKKRmCKWYhVAySegqCPUbaa5FvHOkkxjEwVDn5CcBuOx5x64PpSbS3CzZYooopgFFFFABRTFnjbbiRTuJVcMOSOoH5H8qfSAKKKKYBRRRQAUVBY39vqVstxayrPAxIWRPutg4OD35HWp6Saaugaa0YUUUUwCiiigAoqrDq1ncahPYx3MT3kChpIFb5kBxjI/EfmPUVapJp7Daa3CiiimIKKa8iR43sFyQoycZPYU6kAUUUUwCioru7hsLWW5uJVh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	Thẻ khám phá đảo ngọc và văn hóa biển Phú Quốc.	ACTIVE
130	11	Nhà gỗ Tây Nguyên	data:image/jpeg;base64,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	Dấu ấn Nhà Thờ Gỗ Kon Tum và kiến trúc gỗ địa phương.	ACTIVE
136	71	Thạch nhũ Phong Nha	data:image/jpeg;base64,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	Dấu ấn hang động và cảnh quan địa chất Phong Nha.	ACTIVE
137	76	Đèn lồng Hội An	data:image/jpeg;base64,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KKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKAMD4g/wDIheJP+wZc/wDopqwPgR/ySnQ/+2//AKPkrf8AiD/yIXiT/sGXP/opqwPgR/ySnQ/+2/8A6PkrzH/v8f8AA/8A0pHev9zf+JfkzvqKKK9M4AooooAwPiD/AMiF4k/7Blz/AOimrA+BH/JKdD/7b/8Ao+St/wCIP/IheJP+wZc/+imrA+BH/JKdD/7b/wDo+SvMf+/x/wAD/wDSkd6/3N/4l+TO+ooor0zgCiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAwPiD/AMiF4k/7Blz/AOimrA+BH/JKdD/7b/8Ao+St/wCIP/IheJP+wZc/+imrA+BH/JKdD/7b/wDo+SvMf+/x/wAD/wDSkd6/3N/4l+TO+ooor0zgCiiigDA+IP8AyIXiT/sGXP8A6KasD4Ef8kp0P/tv/wCj5K3/AIg/8iF4k/7Blz/6KasD4Ef8kp0P/tv/AOj5K8x/7/H/AAP/ANKR3r/c3/iX5M76iiivTOAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigDA+IP8AyIXiT/sGXP8A6KasD4Ef8kp0P/tv/wCj5K3/AIg/8iF4k/7Blz/6KasD4Ef8kp0P/tv/AOj5K8x/7/H/AAP/ANKR3r/c3/iX5M76iiivTOAKKKKAMD4g/wDIheJP+wZc/wDopqwPgR/ySnQ/+2//AKPkrf8AiD/yIXiT/sGXP/opqwPgR/ySnQ/+2/8A6PkrzH/v8f8AA/8A0pHev9zf+JfkzvqKKK9M4AooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiii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8D/9KR3r/c3/AIl+TO+ooor0zgCiiigDA+IP/IheJP8AsGXP/opqwPgR/wAkp0P/ALb/APo+St/4g/8AIheJP+wZc/8AopqwPgR/ySnQ/wDtv/6PkrzH/v8AH/A//Skd6/3N/wCJfkzvqKKK9M4AooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKAMD4g/8iF4k/wCwZc/+imrA+BH/ACSnQ/8Atv8A+j5K3/iD/wAiF4k/7Blz/wCimrA+BH/JKdD/AO2//o+SvMf+/wAf8D/9KR3r/c3/AIl+TO+ooor0zgCiiigDA+IP/IheJP8AsGXP/opqwPgR/wAkp0P/ALb/APo+St/4g/8AIheJP+wZc/8AopqwPgR/ySnQ/wDtv/6PkrzH/v8AH/A//Skd6/3N/wCJfkzvqKKK9M4AooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKAMD4g/8iF4k/wCwZc/+imrA+BH/ACSnQ/8Atv8A+j5K3/iD/wAiF4k/7Blz/wCimrA+BH/JKdD/AO2//o+SvMf+/wAf8D/9KR3r/c3/AIl+TO+ooor0zgCiiigDA+IP/IheJP8AsGXP/opqwPgR/wAkp0P/ALb/APo+St/4g/8AIheJP+wZc/8AopqwPgR/ySnQ/wDtv/6PkrzH/v8AH/A//Skd6/3N/wCJfkzvqKKK9M4AooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAoryD44/tL6B8B9S0uz1nTNSv5NQieaNrFYyFCkAg7mX1rzWL/goj4ELgSeHvESp3KxQMR+Hmj+daxo1JK6R5lXMsJQm6dSok0fVNFcF8Lvjn4M+MUEx8M6utzdQLumsZ0MVxGOOSjdV5A3LkZOM5rqPFN3q9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	Biểu tượng đèn lồng gắn với không gian văn hóa phố cổ Hội An.	ACTIVE
131	21	Sắc hoa Đà Lạt	data:image/jpeg;base64,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/wDwsHwt/wBDJpH/AIHxf/FUf8LB8Lf9DJpH/gfF/wDFVgf8KI8Df9AP/wAm5/8A4uj/AIUR4G/6Af8A5Nz/APxdF8f/ACw++X+QWwfeX3L/ADN//hYPhb/oZNI/8D4v/iqP+Fg+Fv8AoZNI/wDA+L/4qsD/AIUR4G/6Af8A5Nz/APxdH/CiPA3/AEA//Juf/wCLovj/AOWH3y/yC2D7y+5f5m//AMLB8Lf9DJpH/gfF/wDFUf8ACwfC3/QyaR/4Hxf/ABVYH/CiPA3/AEA//Juf/wCLo/4UR4G/6Af/AJNz/wDxdF8f/LD75f5BbB95fcv8yTxx448OXfgrxBBB4g0uaeXT7hI4472NmdjGwAADZJJ7VH8CP+SU6H/23/8AR8lH/CiPA3/QD/8AJuf/AOLrr9B0Gx8M6VBpumwfZrKDd5cW9n27mLHliT1JPWppUsQ8R7atypKLWjfdPql2HUqUFQ9lSvvfVLs10Zfooor1TzwooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooA/9k=	Kỷ niệm khám phá Hồ Xuân Hương và cảnh quan thành phố Đà Lạt.	ACTIVE
142	126	Đại Nội Huế	https://thumb.wikimedia.org/wikipedia/commons/thumb/d/d7/Imperial_City%2C_Hue%2C_Vietnam_%2849575145062%29.jpg/960px-Imperial_City%2C_Hue%2C_Vietnam_%2849575145062%29.jpg	Nằm ở bờ Bắc của dòng sông Hương thơ mộng trữ tình, Đại Nội Huế với kiến trúc nghệ thuật cung đình và vườn hào độc đáo. Cũng là trung tâm hành chính, chính trị của triều đình nhà Nguyễn và nơi sinh hoạt của nhà vua hoàng gia ngày xưa.	ACTIVE
\.


--
-- Data for Name: collection; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.collection (collection_id, name, description, status) FROM stdin;
900001	Di sản và kiến trúc mẫu	Bộ sưu tập thử nghiệm các điểm văn hóa và kiến trúc.	ACTIVE
900002	Vui chơi và khám phá mẫu	Bộ sưu tập thử nghiệm các điểm tham quan và giải trí.	ACTIVE
1	Biển đảo Bắc Bộ	Các địa danh ở Hải Phòng và Quảng Ninh.	ACTIVE
2	Đồng bằng Bắc Bộ	Các địa danh ở Hà Nam, Hưng Yên, Hải Dương, Nam Định, Ninh Bình và Thái Bình.	ACTIVE
3	Di sản Hà Nội và Kinh Bắc	Các địa danh ở Hà Nội, Bắc Ninh, Bắc Giang và Vĩnh Phúc.	ACTIVE
4	Hành trình Tây Bắc	Các địa danh ở Hòa Bình, Lai Châu, Lào Cai, Phú Thọ, Sơn La, Yên Bái và Điện Biên.	ACTIVE
5	Núi rừng Đông Bắc	Các địa danh ở Bắc Kạn, Cao Bằng, Hà Giang, Lạng Sơn, Thái Nguyên và Tuyên Quang.	ACTIVE
\.


--
-- Data for Name: collection_card; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.collection_card (collection_card_id, collection_id, card_id) FROM stdin;
1	4	1
2	4	2
3	4	3
4	4	4
5	4	5
6	5	6
7	5	7
8	5	8
9	5	9
10	5	10
11	4	11
12	4	12
13	4	13
14	4	14
15	4	15
16	2	16
17	2	17
18	2	18
19	2	19
20	2	20
21	2	21
22	2	22
23	2	23
24	2	24
25	2	25
26	4	26
27	4	27
28	4	28
29	4	29
30	4	30
31	1	31
32	1	32
33	1	33
34	1	34
35	1	35
36	4	36
37	4	37
38	4	38
39	4	39
40	4	40
41	2	41
42	2	42
43	2	43
44	2	44
45	2	45
46	5	46
47	5	47
48	5	48
49	5	49
50	5	50
51	3	51
52	3	52
53	3	53
54	3	54
55	3	55
56	5	56
57	5	57
58	5	58
59	5	59
60	5	60
61	3	61
62	3	62
63	3	63
64	3	64
65	3	65
66	5	66
67	5	67
68	5	68
69	5	69
70	5	70
71	5	71
72	5	72
73	5	73
74	5	74
75	5	75
76	5	76
77	3	77
78	3	78
79	3	79
80	3	80
81	3	81
82	4	82
83	4	83
84	4	84
85	4	85
86	4	86
87	4	87
88	4	88
89	4	89
90	4	90
91	4	91
92	5	92
93	5	93
94	5	94
95	5	95
96	2	96
97	2	97
98	2	98
99	2	99
100	2	100
101	3	101
102	3	102
103	3	103
104	3	104
105	3	105
106	2	106
107	2	107
108	2	108
109	2	109
110	2	110
111	1	111
112	1	112
113	1	113
114	1	114
115	1	115
116	4	116
117	4	117
118	4	118
119	4	119
120	4	120
121	2	121
122	2	122
123	2	123
124	2	124
125	2	125
900001	900001	127
900002	900001	130
900003	900002	128
900004	900001	137
900005	900002	31
900006	900002	66
900007	900001	102
900008	900002	11
900009	900001	143
900010	900002	21
900011	900002	129
900012	900002	131
900013	900002	150
900014	900002	136
900015	900001	142
\.


--
-- Data for Name: interest_group; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.interest_group (interest_group_id, code, name) FROM stdin;
201	PREF_HERITAGE	Di sản
202	PREF_NATURE	Thiên nhiên
203	PREF_CUISINE	Ẩm thực
204	PREF_COAST_ISLAND	Biển đảo
205	PREF_HISTORY	Lịch sử
206	PREF_ARCHITECTURE	Kiến trúc
101	PLACE_TYPE_101	Di tích và bảo tàng
102	PLACE_TYPE_102	Đền, chùa và nhà thờ
103	PLACE_TYPE_103	Công trình kiến trúc
104	PLACE_TYPE_104	Phố cổ và làng nghề
105	PLACE_TYPE_105	Chợ và ẩm thực
106	PLACE_TYPE_106	Vui chơi và nghỉ dưỡng
107	PLACE_TYPE_107	Đồng quê và nông nghiệp
108	PLACE_TYPE_108	Biển và đảo
109	PLACE_TYPE_109	Sông, hồ và thác
110	PLACE_TYPE_110	Núi và đèo
111	PLACE_TYPE_111	Hang động
112	PLACE_TYPE_112	Rừng và khu bảo tồn
\.


--
-- Data for Name: item; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.item (item_id, name, item_type, description, effect_value, effective_range, usage_limit, status) FROM stdin;
900001	Giày tăng tốc	SPEED_SHOES	Nhân đôi số bước tính vào tiến độ hành trình trong tối đa 5.000 bước thực tế sau khi kích hoạt.	2.00	5000	1	ACTIVE
900002	Vé máy bay	FLIGHT_TICKET	Chuyển vị trí ảo đến địa danh thuộc vùng hợp lệ; chỉ trừ vé khi cập nhật vị trí thành công.	\N	\N	1	ACTIVE
\.


--
-- Data for Name: recommendation_config; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.recommendation_config (recommendation_config_id, version, distance_limit, cluster_count, status, updated_at, settings) FROM stdin;
1	rank_20261020_draft_six_v1	500000.00	15	ACTIVE	2026-10-29 02:13:26.434142	{"modelSha256": "0f79c0880d04dca5d056354689a33b1ec93cf203a11335cefc550ce1cc81f3d3", "modelVersion": "kmeans-k15-seed42", "datasetVersion": "canonical-311-20261020", "mappingVersion": "six-interests-v1", "interestFeatures": {"201": ["kind__heritage"], "202": ["kind__nature"], "203": ["theme__cuisine"], "204": ["landform__coast", "landform__island"], "205": ["theme__history"], "206": ["theme__architecture"]}, "activityThresholds": {"LOW": 3000, "HIGH": 10000, "MEDIUM": 6000}}
\.


--
-- Data for Name: place_feature; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.place_feature (place_feature_id, place_id, recommendation_config_id, cluster_number, updated_at) FROM stdin;
1	1	1	0	2026-10-25 18:27:27.169686
2	2	1	1	2026-10-25 18:27:27.169686
3	3	1	2	2026-10-25 18:27:27.169686
4	4	1	3	2026-10-25 18:27:27.169686
5	5	1	4	2026-10-25 18:27:27.169686
6	6	1	0	2026-10-25 18:27:27.169686
7	7	1	0	2026-10-25 18:27:27.169686
8	8	1	1	2026-10-25 18:27:27.169686
9	9	1	5	2026-10-25 18:27:27.169686
10	10	1	2	2026-10-25 18:27:27.169686
11	11	1	1	2026-10-25 18:27:27.169686
12	12	1	6	2026-10-25 18:27:27.169686
13	13	1	5	2026-10-25 18:27:27.169686
14	14	1	7	2026-10-25 18:27:27.169686
15	15	1	8	2026-10-25 18:27:27.169686
16	16	1	7	2026-10-25 18:27:27.169686
17	17	1	7	2026-10-25 18:27:27.169686
18	18	1	9	2026-10-25 18:27:27.169686
19	19	1	4	2026-10-25 18:27:27.169686
20	20	1	10	2026-10-25 18:27:27.169686
21	21	1	4	2026-10-25 18:27:27.169686
22	22	1	1	2026-10-25 18:27:27.169686
23	23	1	10	2026-10-25 18:27:27.169686
24	24	1	2	2026-10-25 18:27:27.169686
25	25	1	8	2026-10-25 18:27:27.169686
26	26	1	11	2026-10-25 18:27:27.169686
27	27	1	10	2026-10-25 18:27:27.169686
28	28	1	6	2026-10-25 18:27:27.169686
29	29	1	7	2026-10-25 18:27:27.169686
30	30	1	1	2026-10-25 18:27:27.169686
31	31	1	7	2026-10-25 18:27:27.169686
32	32	1	7	2026-10-25 18:27:27.169686
33	33	1	10	2026-10-25 18:27:27.169686
34	34	1	8	2026-10-25 18:27:27.169686
35	35	1	1	2026-10-25 18:27:27.169686
36	36	1	5	2026-10-25 18:27:27.169686
37	37	1	2	2026-10-25 18:27:27.169686
38	38	1	1	2026-10-25 18:27:27.169686
39	39	1	8	2026-10-25 18:27:27.169686
40	40	1	10	2026-10-25 18:27:27.169686
41	41	1	1	2026-10-25 18:27:27.169686
42	42	1	1	2026-10-25 18:27:27.169686
43	43	1	5	2026-10-25 18:27:27.169686
44	44	1	10	2026-10-25 18:27:27.169686
45	45	1	4	2026-10-25 18:27:27.169686
46	46	1	2	2026-10-25 18:27:27.169686
47	47	1	0	2026-10-25 18:27:27.169686
48	48	1	2	2026-10-25 18:27:27.169686
49	49	1	9	2026-10-25 18:27:27.169686
50	50	1	11	2026-10-25 18:27:27.169686
51	51	1	11	2026-10-25 18:27:27.169686
52	52	1	9	2026-10-25 18:27:27.169686
53	53	1	2	2026-10-25 18:27:27.169686
54	54	1	11	2026-10-25 18:27:27.169686
55	55	1	5	2026-10-25 18:27:27.169686
56	56	1	0	2026-10-25 18:27:27.169686
57	57	1	5	2026-10-25 18:27:27.169686
58	58	1	1	2026-10-25 18:27:27.169686
59	59	1	9	2026-10-25 18:27:27.169686
61	61	1	1	2026-10-25 18:27:27.169686
62	62	1	12	2026-10-25 18:27:27.169686
63	63	1	3	2026-10-25 18:27:27.169686
64	64	1	5	2026-10-25 18:27:27.169686
65	65	1	0	2026-10-25 18:27:27.169686
66	66	1	11	2026-10-25 18:27:27.169686
67	67	1	13	2026-10-25 18:27:27.169686
68	68	1	6	2026-10-25 18:27:27.169686
69	69	1	0	2026-10-25 18:27:27.169686
70	70	1	1	2026-10-25 18:27:27.169686
71	71	1	11	2026-10-25 18:27:27.169686
72	72	1	0	2026-10-25 18:27:27.169686
73	73	1	1	2026-10-25 18:27:27.169686
74	74	1	2	2026-10-25 18:27:27.169686
75	75	1	11	2026-10-25 18:27:27.169686
76	76	1	2	2026-10-25 18:27:27.169686
77	77	1	0	2026-10-25 18:27:27.169686
78	78	1	2	2026-10-25 18:27:27.169686
79	79	1	7	2026-10-25 18:27:27.169686
80	80	1	1	2026-10-25 18:27:27.169686
81	81	1	7	2026-10-25 18:27:27.169686
82	82	1	0	2026-10-25 18:27:27.169686
83	83	1	9	2026-10-25 18:27:27.169686
84	84	1	8	2026-10-25 18:27:27.169686
85	85	1	7	2026-10-25 18:27:27.169686
86	86	1	11	2026-10-25 18:27:27.169686
87	87	1	0	2026-10-25 18:27:27.169686
88	88	1	3	2026-10-25 18:27:27.169686
89	89	1	14	2026-10-25 18:27:27.169686
90	90	1	11	2026-10-25 18:27:27.169686
91	91	1	2	2026-10-25 18:27:27.169686
92	92	1	2	2026-10-25 18:27:27.169686
93	93	1	0	2026-10-25 18:27:27.169686
94	94	1	9	2026-10-25 18:27:27.169686
95	95	1	7	2026-10-25 18:27:27.169686
96	96	1	10	2026-10-25 18:27:27.169686
97	97	1	1	2026-10-25 18:27:27.169686
98	98	1	5	2026-10-25 18:27:27.169686
99	99	1	0	2026-10-25 18:27:27.169686
100	100	1	1	2026-10-25 18:27:27.169686
101	101	1	14	2026-10-25 18:27:27.169686
102	102	1	11	2026-10-25 18:27:27.169686
103	103	1	5	2026-10-25 18:27:27.169686
104	104	1	8	2026-10-25 18:27:27.169686
105	105	1	7	2026-10-25 18:27:27.169686
106	106	1	7	2026-10-25 18:27:27.169686
107	107	1	1	2026-10-25 18:27:27.169686
108	108	1	4	2026-10-25 18:27:27.169686
109	109	1	2	2026-10-25 18:27:27.169686
110	110	1	5	2026-10-25 18:27:27.169686
111	111	1	1	2026-10-25 18:27:27.169686
112	112	1	0	2026-10-25 18:27:27.169686
113	113	1	5	2026-10-25 18:27:27.169686
114	114	1	1	2026-10-25 18:27:27.169686
115	115	1	12	2026-10-25 18:27:27.169686
116	116	1	4	2026-10-25 18:27:27.169686
117	117	1	9	2026-10-25 18:27:27.169686
118	118	1	4	2026-10-25 18:27:27.169686
119	119	1	11	2026-10-25 18:27:27.169686
120	120	1	5	2026-10-25 18:27:27.169686
121	121	1	5	2026-10-25 18:27:27.169686
122	122	1	9	2026-10-25 18:27:27.169686
123	123	1	13	2026-10-25 18:27:27.169686
124	124	1	5	2026-10-25 18:27:27.169686
125	125	1	11	2026-10-25 18:27:27.169686
126	126	1	2	2026-10-25 18:27:27.169686
127	127	1	1	2026-10-25 18:27:27.169686
128	128	1	2	2026-10-25 18:27:27.169686
129	129	1	2	2026-10-25 18:27:27.169686
130	130	1	6	2026-10-25 18:27:27.169686
131	131	1	0	2026-10-25 18:27:27.169686
132	132	1	6	2026-10-25 18:27:27.169686
133	133	1	1	2026-10-25 18:27:27.169686
134	134	1	1	2026-10-25 18:27:27.169686
135	135	1	1	2026-10-25 18:27:27.169686
136	136	1	10	2026-10-25 18:27:27.169686
137	137	1	2	2026-10-25 18:27:27.169686
138	138	1	1	2026-10-25 18:27:27.169686
139	139	1	2	2026-10-25 18:27:27.169686
140	140	1	2	2026-10-25 18:27:27.169686
141	141	1	5	2026-10-25 18:27:27.169686
142	142	1	4	2026-10-25 18:27:27.169686
143	143	1	1	2026-10-25 18:27:27.169686
144	144	1	1	2026-10-25 18:27:27.169686
145	145	1	1	2026-10-25 18:27:27.169686
146	146	1	7	2026-10-25 18:27:27.169686
147	147	1	0	2026-10-25 18:27:27.169686
148	148	1	0	2026-10-25 18:27:27.169686
149	149	1	0	2026-10-25 18:27:27.169686
150	150	1	0	2026-10-25 18:27:27.169686
151	151	1	1	2026-10-25 18:27:27.169686
152	152	1	8	2026-10-25 18:27:27.169686
153	153	1	4	2026-10-25 18:27:27.169686
154	154	1	1	2026-10-25 18:27:27.169686
155	155	1	4	2026-10-25 18:27:27.169686
156	156	1	5	2026-10-25 18:27:27.169686
157	157	1	11	2026-10-25 18:27:27.169686
158	158	1	8	2026-10-25 18:27:27.169686
159	159	1	14	2026-10-25 18:27:27.169686
160	160	1	11	2026-10-25 18:27:27.169686
161	161	1	1	2026-10-25 18:27:27.169686
162	162	1	0	2026-10-25 18:27:27.169686
163	163	1	14	2026-10-25 18:27:27.169686
164	164	1	2	2026-10-25 18:27:27.169686
165	165	1	2	2026-10-25 18:27:27.169686
166	166	1	1	2026-10-25 18:27:27.169686
167	167	1	1	2026-10-25 18:27:27.169686
168	168	1	12	2026-10-25 18:27:27.169686
169	169	1	1	2026-10-25 18:27:27.169686
170	170	1	10	2026-10-25 18:27:27.169686
171	171	1	13	2026-10-25 18:27:27.169686
172	172	1	2	2026-10-25 18:27:27.169686
173	173	1	1	2026-10-25 18:27:27.169686
174	174	1	1	2026-10-25 18:27:27.169686
175	175	1	3	2026-10-25 18:27:27.169686
176	176	1	0	2026-10-25 18:27:27.169686
177	177	1	0	2026-10-25 18:27:27.169686
178	178	1	0	2026-10-25 18:27:27.169686
179	179	1	0	2026-10-25 18:27:27.169686
180	180	1	0	2026-10-25 18:27:27.169686
181	181	1	1	2026-10-25 18:27:27.169686
182	182	1	3	2026-10-25 18:27:27.169686
184	184	1	3	2026-10-25 18:27:27.169686
185	185	1	1	2026-10-25 18:27:27.169686
186	186	1	7	2026-10-25 18:27:27.169686
187	187	1	5	2026-10-25 18:27:27.169686
188	188	1	2	2026-10-25 18:27:27.169686
189	189	1	4	2026-10-25 18:27:27.169686
190	190	1	5	2026-10-25 18:27:27.169686
191	191	1	2	2026-10-25 18:27:27.169686
192	192	1	6	2026-10-25 18:27:27.169686
193	193	1	11	2026-10-25 18:27:27.169686
194	194	1	14	2026-10-25 18:27:27.169686
195	195	1	0	2026-10-25 18:27:27.169686
196	196	1	5	2026-10-25 18:27:27.169686
197	197	1	5	2026-10-25 18:27:27.169686
198	198	1	13	2026-10-25 18:27:27.169686
199	199	1	8	2026-10-25 18:27:27.169686
200	200	1	1	2026-10-25 18:27:27.169686
201	201	1	10	2026-10-25 18:27:27.169686
202	202	1	6	2026-10-25 18:27:27.169686
203	203	1	3	2026-10-25 18:27:27.169686
204	204	1	1	2026-10-25 18:27:27.169686
205	205	1	13	2026-10-25 18:27:27.169686
206	206	1	8	2026-10-25 18:27:27.169686
207	207	1	11	2026-10-25 18:27:27.169686
208	208	1	7	2026-10-25 18:27:27.169686
209	209	1	2	2026-10-25 18:27:27.169686
210	210	1	12	2026-10-25 18:27:27.169686
211	211	1	3	2026-10-25 18:27:27.169686
212	212	1	6	2026-10-25 18:27:27.169686
213	213	1	11	2026-10-25 18:27:27.169686
214	214	1	0	2026-10-25 18:27:27.169686
215	215	1	6	2026-10-25 18:27:27.169686
216	216	1	4	2026-10-25 18:27:27.169686
217	217	1	1	2026-10-25 18:27:27.169686
218	218	1	0	2026-10-25 18:27:27.169686
219	219	1	2	2026-10-25 18:27:27.169686
220	220	1	1	2026-10-25 18:27:27.169686
221	221	1	5	2026-10-25 18:27:27.169686
222	222	1	5	2026-10-25 18:27:27.169686
223	223	1	1	2026-10-25 18:27:27.169686
224	224	1	0	2026-10-25 18:27:27.169686
225	225	1	8	2026-10-25 18:27:27.169686
226	226	1	13	2026-10-25 18:27:27.169686
227	227	1	1	2026-10-25 18:27:27.169686
229	229	1	3	2026-10-25 18:27:27.169686
230	230	1	1	2026-10-25 18:27:27.169686
231	231	1	14	2026-10-25 18:27:27.169686
232	232	1	5	2026-10-25 18:27:27.169686
233	233	1	12	2026-10-25 18:27:27.169686
234	234	1	3	2026-10-25 18:27:27.169686
235	235	1	4	2026-10-25 18:27:27.169686
236	236	1	9	2026-10-25 18:27:27.169686
237	237	1	4	2026-10-25 18:27:27.169686
238	238	1	9	2026-10-25 18:27:27.169686
239	239	1	9	2026-10-25 18:27:27.169686
240	240	1	11	2026-10-25 18:27:27.169686
241	241	1	14	2026-10-25 18:27:27.169686
242	242	1	14	2026-10-25 18:27:27.169686
243	243	1	8	2026-10-25 18:27:27.169686
244	244	1	10	2026-10-25 18:27:27.169686
245	245	1	3	2026-10-25 18:27:27.169686
246	246	1	11	2026-10-25 18:27:27.169686
247	247	1	8	2026-10-25 18:27:27.169686
248	248	1	11	2026-10-25 18:27:27.169686
249	249	1	4	2026-10-25 18:27:27.169686
250	250	1	5	2026-10-25 18:27:27.169686
251	251	1	2	2026-10-25 18:27:27.169686
252	252	1	2	2026-10-25 18:27:27.169686
253	253	1	2	2026-10-25 18:27:27.169686
254	254	1	9	2026-10-25 18:27:27.169686
255	255	1	2	2026-10-25 18:27:27.169686
256	256	1	5	2026-10-25 18:27:27.169686
257	257	1	3	2026-10-25 18:27:27.169686
258	258	1	4	2026-10-25 18:27:27.169686
259	259	1	4	2026-10-25 18:27:27.169686
260	260	1	5	2026-10-25 18:27:27.169686
261	261	1	5	2026-10-25 18:27:27.169686
262	262	1	9	2026-10-25 18:27:27.169686
263	263	1	2	2026-10-25 18:27:27.169686
264	264	1	2	2026-10-25 18:27:27.169686
265	265	1	10	2026-10-25 18:27:27.169686
266	266	1	11	2026-10-25 18:27:27.169686
267	267	1	8	2026-10-25 18:27:27.169686
268	268	1	1	2026-10-25 18:27:27.169686
269	269	1	6	2026-10-25 18:27:27.169686
270	270	1	9	2026-10-25 18:27:27.169686
271	271	1	6	2026-10-25 18:27:27.169686
272	272	1	7	2026-10-25 18:27:27.169686
273	273	1	2	2026-10-25 18:27:27.169686
274	274	1	11	2026-10-25 18:27:27.169686
275	275	1	2	2026-10-25 18:27:27.169686
276	276	1	1	2026-10-25 18:27:27.169686
277	277	1	1	2026-10-25 18:27:27.169686
278	278	1	11	2026-10-25 18:27:27.169686
279	279	1	1	2026-10-25 18:27:27.169686
280	280	1	14	2026-10-25 18:27:27.169686
281	281	1	2	2026-10-25 18:27:27.169686
282	282	1	4	2026-10-25 18:27:27.169686
283	283	1	1	2026-10-25 18:27:27.169686
284	284	1	2	2026-10-25 18:27:27.169686
285	285	1	6	2026-10-25 18:27:27.169686
286	286	1	2	2026-10-25 18:27:27.169686
287	287	1	1	2026-10-25 18:27:27.169686
288	288	1	0	2026-10-25 18:27:27.169686
289	289	1	2	2026-10-25 18:27:27.169686
290	290	1	7	2026-10-25 18:27:27.169686
291	291	1	13	2026-10-25 18:27:27.169686
292	292	1	1	2026-10-25 18:27:27.169686
293	293	1	14	2026-10-25 18:27:27.169686
294	294	1	1	2026-10-25 18:27:27.169686
295	295	1	10	2026-10-25 18:27:27.169686
296	296	1	0	2026-10-25 18:27:27.169686
297	297	1	0	2026-10-25 18:27:27.169686
298	298	1	2	2026-10-25 18:27:27.169686
299	299	1	6	2026-10-25 18:27:27.169686
300	300	1	6	2026-10-25 18:27:27.169686
301	301	1	5	2026-10-25 18:27:27.169686
302	302	1	10	2026-10-25 18:27:27.169686
303	303	1	3	2026-10-25 18:27:27.169686
305	305	1	14	2026-10-25 18:27:27.169686
306	306	1	4	2026-10-25 18:27:27.169686
307	307	1	14	2026-10-25 18:27:27.169686
308	308	1	11	2026-10-25 18:27:27.169686
309	309	1	8	2026-10-25 18:27:27.169686
310	310	1	1	2026-10-25 18:27:27.169686
311	311	1	2	2026-10-25 18:27:27.169686
312	312	1	1	2026-10-25 18:27:27.169686
313	313	1	12	2026-10-25 18:27:27.169686
314	314	1	1	2026-10-25 18:27:27.169686
315	315	1	1	2026-10-25 18:27:27.169686
322	320	1	2	2026-10-29 10:23:17.781914
\.


--
-- Data for Name: recommendation_criterion; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.recommendation_criterion (criterion_id, code, name, description, status) FROM stdin;
1	kind__nature	Thiên nhiên	Cảnh quan hoặc điểm bảo tồn tự nhiên.	ACTIVE
2	kind__heritage	Di tích, công trình lịch sử	Di tích, nơi lưu niệm hoặc công trình có giá trị lịch sử được mô tả.	ACTIVE
3	kind__religious	Tín ngưỡng, tôn giáo	Chùa, đền, đình, miếu, nhà thờ hoặc thánh địa; không suy từ sản phẩm làng nghề.	ACTIVE
4	kind__museum	Bảo tàng	Cơ sở trưng bày được xác định là bảo tàng.	ACTIVE
5	kind__structure	Công trình, không gian xây dựng	Cầu, hải đăng, quảng trường, cột cờ, công trình quan sát; không gán cho mọi tòa nhà.	ACTIVE
6	kind__market	Chợ	Chợ hoặc chợ nổi; không tự suy ra ẩm thực.	ACTIVE
7	kind__craft	Nghề thủ công	Điểm sản xuất, trưng bày hoặc trải nghiệm nghề thủ công; không gồm ruộng, vườn trồng cây.	ACTIVE
8	kind__agriculture	Cảnh quan, sản xuất nông nghiệp	Ruộng, vườn nho/hoa/quả, đồi chè, đồng muối; đồng muối được xếp nhóm sản xuất cảnh quan.	ACTIVE
9	kind__community	Khu dân cư, văn hóa cộng đồng	Làng, bản, phố cổ hoặc khu vực cư trú là đối tượng tham quan.	ACTIVE
10	kind__recreation	Khu vui chơi, nghỉ dưỡng	Công viên, khu du lịch, tổ hợp nghỉ dưỡng; không mặc định có trò chơi.	ACTIVE
11	landform__coast	Biển, ven biển	Bãi biển, vịnh biển, mũi biển hoặc cảnh quan bờ biển thuộc điểm đến.	ACTIVE
12	landform__island	Đảo, cù lao	Đảo biển hoặc đảo sông/hồ có bằng chứng; không suy từ tên thương hiệu.	ACTIVE
13	landform__mountain	Núi, đồi	Núi, đồi, đèo, vách núi trong phạm vi điểm đến.	ACTIVE
14	landform__cave	Hang động tự nhiên	Hang, động tự nhiên; loại trừ địa đạo, hầm và hang nhân tạo.	ACTIVE
15	landform__river	Sông	Sông, chợ trên sông hoặc cầu qua sông; loại trừ tên sông chỉ nhắc trong vùng lân cận.	ACTIVE
16	landform__lake	Hồ, ao	Mặt nước hồ, ao hoặc hồ chứa; Hồ Tràm không thuộc nhãn này.	ACTIVE
17	landform__waterfall	Thác	Thác thực tế; không suy từ Hồ Thác Mơ hoặc Đền Thác Bờ.	ACTIVE
18	landform__stream	Suối, nguồn khoáng	Suối hoặc nguồn khoáng; Suối Giàng là tên riêng nên không tự gán.	ACTIVE
19	landform__lagoon	Đầm ven biển	Đầm ven biển, khác với hồ nội địa; Đầm Vạc được xếp hồ nội địa.	ACTIVE
20	landform__dunes	Cồn, đồi cát	Địa hình cồn cát tự nhiên được mô tả; giữ dù ít xuất hiện.	ACTIVE
21	landform__valley	Thung lũng	Thung lũng được xác định trong mô tả, không suy từ tên địa danh.	ACTIVE
22	ecosystem__forest	Rừng	Có rừng thuộc điểm đến; không đồng nhất cây trong vườn hoặc danh xưng vườn quốc gia với rừng.	ACTIVE
23	ecosystem__flooded_habitat	Sinh cảnh ngập nước	Rừng ngập nước, vùng trũng ngập nước hoặc đầm lầy được xác nhận. Định nghĩa hẹp dùng cho dataset, không phải toàn bộ phân loại Ramsar (vốn bao gồm cả hồ, sông...).	ACTIVE
24	ecosystem__grassland	Trảng, đồng cỏ	Trảng cỏ tự nhiên; không đồng nhất với ruộng canh tác.	ACTIVE
25	theme__history	Lịch sử	Di tích, nhân vật hoặc sự kiện lịch sử có trong nội dung; không mặc định mọi chùa đều có giá trị lịch sử.	ACTIVE
26	theme__culture	Văn hóa	Văn hóa cộng đồng, lễ hội, phong tục, nghệ thuật hoặc đời sống địa phương.	ACTIVE
27	theme__architecture	Kiến trúc	Công trình hoặc giá trị kiến trúc được xác định trong mô tả.	ACTIVE
28	theme__spirituality	Tâm linh	Nội dung tôn giáo, tín ngưỡng hoặc hành hương.	ACTIVE
29	theme__ecology	Sinh thái	Bảo tồn, động thực vật, hệ sinh thái hoặc du lịch sinh thái được nêu; không suy từ mọi cảnh quan xanh.	ACTIVE
30	theme__geology	Địa chất	Hang động tự nhiên, đá vôi, núi lửa hoặc cấu tạo địa chất được nêu.	ACTIVE
31	theme__craft	Thủ công	Nội dung nghề thủ công, sản phẩm hoặc trải nghiệm chế tác.	ACTIVE
32	theme__cuisine	Ẩm thực	Món ăn, đồ uống, đặc sản hoặc văn hóa ẩm thực được nêu.	ACTIVE
33	theme__games	Vui chơi	Trò chơi, giải trí chủ động hoặc công viên giải trí được nêu.	ACTIVE
34	theme__relaxation	Nghỉ dưỡng	Nghỉ dưỡng, thư giãn, resort hoặc tắm khoáng được nêu.	ACTIVE
35	theme__agriculture	Nông nghiệp	Nội dung trồng trọt, đồng muối hoặc trải nghiệm sản xuất nông nghiệp.	ACTIVE
36	PREFERENCE	Khớp sở thích	P là tỷ lệ nhóm sở thích khớp tất cả loại địa danh.	ACTIVE
37	CLUSTER	Khớp cụm	C là trung bình P trên địa danh chuẩn trong cụm.	ACTIVE
38	ACTIVITY	Phù hợp mức vận động	A = min(1, T(u) / required_steps).	ACTIVE
39	RATING	Điểm đánh giá	R = rating / 5 khi có rating.	ACTIVE
40	POPULARITY	Mức phổ biến	N = ln(1 + rating_count) / ln(1 + Nmax) khi tính được.	ACTIVE
\.


--
-- Data for Name: place_feature_value; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.place_feature_value (place_feature_id, criterion_id, feature_value) FROM stdin;
1	1	1
1	2	0
1	3	0
1	4	0
1	5	0
1	6	0
1	7	0
1	8	0
1	9	0
1	10	0
1	11	1
1	12	0
1	13	0
1	14	0
1	15	0
1	16	0
1	17	0
1	18	0
1	19	0
1	20	0
1	21	0
1	22	0
1	23	0
1	24	0
1	25	0
1	26	0
1	27	0
1	28	0
1	29	0
1	30	0
1	31	0
1	32	0
1	33	0
1	34	1
1	35	0
2	1	0
2	2	1
2	3	1
2	4	0
2	5	0
2	6	0
2	7	0
2	8	0
2	9	0
2	10	0
2	11	0
2	12	0
2	13	0
2	14	0
2	15	0
2	16	0
2	17	0
2	18	0
2	19	0
2	20	0
2	21	0
2	22	0
2	23	0
2	24	0
2	25	1
2	26	1
2	27	1
2	28	1
2	29	0
2	30	0
2	31	0
2	32	0
2	33	0
2	34	0
2	35	0
3	1	0
3	2	1
3	3	0
3	4	0
3	5	0
3	6	0
3	7	0
3	8	0
3	9	0
3	10	0
3	11	0
3	12	0
3	13	0
3	14	0
3	15	0
3	16	0
3	17	0
3	18	0
3	19	0
3	20	0
3	21	0
3	22	0
3	23	0
3	24	0
3	25	1
3	26	0
3	27	1
3	28	0
3	29	0
3	30	0
3	31	0
3	32	0
3	33	0
3	34	0
3	35	0
4	1	0
4	2	0
4	3	0
4	4	0
4	5	0
4	6	0
4	7	0
4	8	0
4	9	0
4	10	1
4	11	0
4	12	1
4	13	0
4	14	0
4	15	0
4	16	0
4	17	0
4	18	0
4	19	0
4	20	0
4	21	0
4	22	0
4	23	0
4	24	0
4	25	0
4	26	0
4	27	0
4	28	0
4	29	0
4	30	0
4	31	0
4	32	0
4	33	1
4	34	0
4	35	0
5	1	1
5	2	0
5	3	0
5	4	0
5	5	0
5	6	0
5	7	0
5	8	0
5	9	0
5	10	0
5	11	0
5	12	0
5	13	1
5	14	0
5	15	0
5	16	1
5	17	0
5	18	1
5	19	0
5	20	0
5	21	0
5	22	0
5	23	0
5	24	0
5	25	0
5	26	0
5	27	0
5	28	0
5	29	0
5	30	0
5	31	0
5	32	0
5	33	0
5	34	1
5	35	0
6	1	1
6	2	0
6	3	0
6	4	0
6	5	0
6	6	0
6	7	0
6	8	0
6	9	0
6	10	0
6	11	1
6	12	1
6	13	0
6	14	0
6	15	0
6	16	0
6	17	0
6	18	0
6	19	0
6	20	0
6	21	0
6	22	0
6	23	0
6	24	0
6	25	0
6	26	0
6	27	0
6	28	0
6	29	0
6	30	0
6	31	0
6	32	0
6	33	0
6	34	1
6	35	0
7	1	1
7	2	0
7	3	0
7	4	0
7	5	0
7	6	0
7	7	0
7	8	0
7	9	0
7	10	0
7	11	1
7	12	0
7	13	0
7	14	0
7	15	0
7	16	0
7	17	0
7	18	0
7	19	0
7	20	0
7	21	0
7	22	0
7	23	0
7	24	0
7	25	0
7	26	0
7	27	0
7	28	0
7	29	0
7	30	0
7	31	0
7	32	0
7	33	0
7	34	1
7	35	0
8	1	0
8	2	0
8	3	1
8	4	0
8	5	0
8	6	0
8	7	0
8	8	0
8	9	0
8	10	0
8	11	0
8	12	0
8	13	0
8	14	0
8	15	0
8	16	0
8	17	0
8	18	0
8	19	0
8	20	0
8	21	0
8	22	0
8	23	0
8	24	0
8	25	0
8	26	1
8	27	1
8	28	1
8	29	0
8	30	0
8	31	0
8	32	0
8	33	0
8	34	0
8	35	0
9	1	1
9	2	0
9	3	0
9	4	0
9	5	0
9	6	0
9	7	0
9	8	0
9	9	0
9	10	0
9	11	0
9	12	0
9	13	0
9	14	0
9	15	0
9	16	0
9	17	0
9	18	0
9	19	0
9	20	0
9	21	0
9	22	1
9	23	1
9	24	0
9	25	0
9	26	0
9	27	0
9	28	0
9	29	1
9	30	0
9	31	0
9	32	0
9	33	0
9	34	0
9	35	0
10	1	0
10	2	1
10	3	0
10	4	0
10	5	0
10	6	0
10	7	0
10	8	0
10	9	0
10	10	0
10	11	0
10	12	0
10	13	0
10	14	0
10	15	0
10	16	0
10	17	0
10	18	0
10	19	0
10	20	0
10	21	0
10	22	0
10	23	0
10	24	0
10	25	1
10	26	0
10	27	0
10	28	0
10	29	0
10	30	0
10	31	0
10	32	0
10	33	0
10	34	0
10	35	0
11	1	0
11	2	0
11	3	1
11	4	0
11	5	0
11	6	0
11	7	0
11	8	0
11	9	0
11	10	0
11	11	0
11	12	0
11	13	0
11	14	0
11	15	0
11	16	0
11	17	0
11	18	0
11	19	0
11	20	0
11	21	0
11	22	0
11	23	0
11	24	0
11	25	0
11	26	1
11	27	1
11	28	1
11	29	0
11	30	0
11	31	0
11	32	0
11	33	0
11	34	0
11	35	0
12	1	0
12	2	0
12	3	0
12	4	0
12	5	1
12	6	0
12	7	0
12	8	0
12	9	0
12	10	0
12	11	0
12	12	0
12	13	0
12	14	0
12	15	1
12	16	0
12	17	0
12	18	0
12	19	0
12	20	0
12	21	0
12	22	0
12	23	0
12	24	0
12	25	0
12	26	0
12	27	1
12	28	0
12	29	0
12	30	0
12	31	0
12	32	0
12	33	0
12	34	0
12	35	0
13	1	1
13	2	0
13	3	0
13	4	0
13	5	0
13	6	0
13	7	0
13	8	0
13	9	0
13	10	0
13	11	0
13	12	0
13	13	0
13	14	0
13	15	0
13	16	0
13	17	0
13	18	0
13	19	0
13	20	0
13	21	0
13	22	1
13	23	0
13	24	0
13	25	0
13	26	0
13	27	0
13	28	0
13	29	1
13	30	0
13	31	0
13	32	0
13	33	0
13	34	0
13	35	0
14	1	1
14	2	0
14	3	0
14	4	0
14	5	0
14	6	0
14	7	0
14	8	0
14	9	0
14	10	0
14	11	0
14	12	0
14	13	1
14	14	0
14	15	0
14	16	0
14	17	0
14	18	0
14	19	0
14	20	0
14	21	0
14	22	0
14	23	0
14	24	0
14	25	0
14	26	0
14	27	0
14	28	0
14	29	1
14	30	0
14	31	0
14	32	0
14	33	0
14	34	0
14	35	0
15	1	1
15	2	0
15	3	0
15	4	0
15	5	0
15	6	0
15	7	0
15	8	0
15	9	0
15	10	0
15	11	0
15	12	0
15	13	0
15	14	0
15	15	0
15	16	0
15	17	1
15	18	0
15	19	0
15	20	0
15	21	0
15	22	0
15	23	0
15	24	0
15	25	0
15	26	0
15	27	0
15	28	0
15	29	0
15	30	0
15	31	0
15	32	0
15	33	0
15	34	0
15	35	0
16	1	1
16	2	0
16	3	0
16	4	0
16	5	0
16	6	0
16	7	0
16	8	0
16	9	0
16	10	0
16	11	0
16	12	0
16	13	1
16	14	0
16	15	0
16	16	0
16	17	0
16	18	0
16	19	0
16	20	0
16	21	0
16	22	0
16	23	0
16	24	0
16	25	0
16	26	0
16	27	0
16	28	0
16	29	0
16	30	0
16	31	0
16	32	0
16	33	0
16	34	0
16	35	0
17	1	1
17	2	0
17	3	0
17	4	0
17	5	0
17	6	0
17	7	0
17	8	0
17	9	0
17	10	0
17	11	0
17	12	0
17	13	1
17	14	0
17	15	0
17	16	0
17	17	0
17	18	0
17	19	0
17	20	0
17	21	0
17	22	0
17	23	0
17	24	0
17	25	0
17	26	0
17	27	0
17	28	0
17	29	0
17	30	0
17	31	0
17	32	0
17	33	0
17	34	0
17	35	0
18	1	0
18	2	0
18	3	0
18	4	0
18	5	0
18	6	0
18	7	0
18	8	1
18	9	0
18	10	0
18	11	0
18	12	0
18	13	0
18	14	0
18	15	0
18	16	0
18	17	0
18	18	0
18	19	0
18	20	0
18	21	0
18	22	0
18	23	0
18	24	0
18	25	0
18	26	0
18	27	0
18	28	0
18	29	0
18	30	0
18	31	0
18	32	0
18	33	0
18	34	0
18	35	1
19	1	1
19	2	0
19	3	0
19	4	0
19	5	0
19	6	0
19	7	0
19	8	0
19	9	0
19	10	0
19	11	0
19	12	0
19	13	0
19	14	0
19	15	0
19	16	1
19	17	0
19	18	0
19	19	0
19	20	0
19	21	0
19	22	0
19	23	0
19	24	0
19	25	0
19	26	0
19	27	0
19	28	0
19	29	0
19	30	0
19	31	0
19	32	0
19	33	0
19	34	0
19	35	0
20	1	0
20	2	0
20	3	0
20	4	0
20	5	0
20	6	1
20	7	0
20	8	0
20	9	0
20	10	0
20	11	0
20	12	0
20	13	0
20	14	0
20	15	0
20	16	0
20	17	0
20	18	0
20	19	0
20	20	0
20	21	0
20	22	0
20	23	0
20	24	0
20	25	0
20	26	1
20	27	0
20	28	0
20	29	0
20	30	0
20	31	0
20	32	1
20	33	0
20	34	0
20	35	0
21	1	1
21	2	0
21	3	0
21	4	0
21	5	0
21	6	0
21	7	0
21	8	0
21	9	0
21	10	0
21	11	0
21	12	0
21	13	0
21	14	0
21	15	0
21	16	1
21	17	0
21	18	0
21	19	0
21	20	0
21	21	0
21	22	0
21	23	0
21	24	0
21	25	0
21	26	0
21	27	0
21	28	0
21	29	0
21	30	0
21	31	0
21	32	0
21	33	0
21	34	1
21	35	0
22	1	0
22	2	0
22	3	1
22	4	0
22	5	0
22	6	0
22	7	0
22	8	0
22	9	0
22	10	0
22	11	0
22	12	0
22	13	0
22	14	0
22	15	0
22	16	0
22	17	0
22	18	0
22	19	0
22	20	0
22	21	0
22	22	0
22	23	0
22	24	0
22	25	0
22	26	0
22	27	1
22	28	1
22	29	0
22	30	0
22	31	0
22	32	0
22	33	0
22	34	0
22	35	0
23	1	0
23	2	0
23	3	0
23	4	0
23	5	0
23	6	1
23	7	0
23	8	0
23	9	0
23	10	0
23	11	0
23	12	0
23	13	0
23	14	0
23	15	0
23	16	0
23	17	0
23	18	0
23	19	0
23	20	0
23	21	0
23	22	0
23	23	0
23	24	0
23	25	0
23	26	0
23	27	0
23	28	0
23	29	0
23	30	0
23	31	0
23	32	1
23	33	0
23	34	0
23	35	0
24	1	0
24	2	1
24	3	0
24	4	0
24	5	0
24	6	0
24	7	0
24	8	0
24	9	0
24	10	0
24	11	0
24	12	0
24	13	0
24	14	0
24	15	0
24	16	0
24	17	0
24	18	0
24	19	0
24	20	0
24	21	0
24	22	0
24	23	0
24	24	0
24	25	1
24	26	0
24	27	1
24	28	0
24	29	0
24	30	0
24	31	0
24	32	0
24	33	0
24	34	0
24	35	0
25	1	1
25	2	0
25	3	0
25	4	0
25	5	0
25	6	0
25	7	0
25	8	0
25	9	0
25	10	0
25	11	0
25	12	0
25	13	0
25	14	0
25	15	0
25	16	0
25	17	1
25	18	0
25	19	0
25	20	0
25	21	0
25	22	0
25	23	0
25	24	0
25	25	0
25	26	0
25	27	0
25	28	0
25	29	0
25	30	0
25	31	0
25	32	0
25	33	0
25	34	0
25	35	0
26	1	1
26	2	1
26	3	1
26	4	0
26	5	0
26	6	0
26	7	0
26	8	0
26	9	0
26	10	0
26	11	0
26	12	0
26	13	0
26	14	1
26	15	0
26	16	0
26	17	0
26	18	0
26	19	0
26	20	0
26	21	0
26	22	0
26	23	0
26	24	0
26	25	1
26	26	0
26	27	1
26	28	1
26	29	0
26	30	1
26	31	0
26	32	0
26	33	0
26	34	0
26	35	0
27	1	0
27	2	0
27	3	0
27	4	0
27	5	0
27	6	1
27	7	0
27	8	0
27	9	0
27	10	0
27	11	0
27	12	0
27	13	0
27	14	0
27	15	0
27	16	0
27	17	0
27	18	0
27	19	0
27	20	0
27	21	0
27	22	0
27	23	0
27	24	0
27	25	0
27	26	1
27	27	0
27	28	0
27	29	0
27	30	0
27	31	0
27	32	0
27	33	0
27	34	0
27	35	0
28	1	0
28	2	0
28	3	0
28	4	0
28	5	1
28	6	0
28	7	0
28	8	0
28	9	0
28	10	0
28	11	0
28	12	0
28	13	0
28	14	0
28	15	0
28	16	0
28	17	0
28	18	0
28	19	0
28	20	0
28	21	0
28	22	0
28	23	0
28	24	0
28	25	0
28	26	0
28	27	1
28	28	0
28	29	0
28	30	0
28	31	0
28	32	0
28	33	0
28	34	0
28	35	0
29	1	1
29	2	0
29	3	0
29	4	0
29	5	0
29	6	0
29	7	0
29	8	0
29	9	0
29	10	0
29	11	0
29	12	0
29	13	1
29	14	0
29	15	0
29	16	0
29	17	0
29	18	0
29	19	0
29	20	0
29	21	0
29	22	0
29	23	0
29	24	0
29	25	0
29	26	0
29	27	0
29	28	0
29	29	0
29	30	0
29	31	0
29	32	0
29	33	0
29	34	0
29	35	0
30	1	0
30	2	0
30	3	1
30	4	0
30	5	0
30	6	0
30	7	0
30	8	0
30	9	0
30	10	0
30	11	0
30	12	0
30	13	1
30	14	0
30	15	0
30	16	0
30	17	0
30	18	0
30	19	0
30	20	0
30	21	0
30	22	0
30	23	0
30	24	0
30	25	0
30	26	0
30	27	1
30	28	1
30	29	0
30	30	0
30	31	0
30	32	0
30	33	0
30	34	0
30	35	0
31	1	1
31	2	0
31	3	0
31	4	0
31	5	0
31	6	0
31	7	0
31	8	0
31	9	0
31	10	0
31	11	0
31	12	0
31	13	1
31	14	0
31	15	0
31	16	0
31	17	0
31	18	0
31	19	0
31	20	0
31	21	0
31	22	0
31	23	0
31	24	0
31	25	0
31	26	0
31	27	0
31	28	0
31	29	0
31	30	0
31	31	0
31	32	0
31	33	0
31	34	0
31	35	0
32	1	1
32	2	0
32	3	0
32	4	0
32	5	0
32	6	0
32	7	0
32	8	0
32	9	0
32	10	0
32	11	0
32	12	0
32	13	1
32	14	0
32	15	0
32	16	0
32	17	0
32	18	0
32	19	0
32	20	0
32	21	0
32	22	0
32	23	0
32	24	0
32	25	0
32	26	0
32	27	0
32	28	0
32	29	0
32	30	0
32	31	0
32	32	0
32	33	0
32	34	0
32	35	0
33	1	0
33	2	0
33	3	0
33	4	0
33	5	0
33	6	1
33	7	0
33	8	0
33	9	0
33	10	0
33	11	0
33	12	0
33	13	0
33	14	0
33	15	0
33	16	0
33	17	0
33	18	0
33	19	0
33	20	0
33	21	0
33	22	0
33	23	0
33	24	0
33	25	0
33	26	1
33	27	0
33	28	0
33	29	0
33	30	0
33	31	0
33	32	1
33	33	0
33	34	0
33	35	0
34	1	1
34	2	0
34	3	0
34	4	0
34	5	0
34	6	0
34	7	0
34	8	0
34	9	0
34	10	0
34	11	0
34	12	0
34	13	0
34	14	0
34	15	0
34	16	0
34	17	1
34	18	0
34	19	0
34	20	0
34	21	0
34	22	1
34	23	0
34	24	0
34	25	0
34	26	0
34	27	0
34	28	0
34	29	0
34	30	0
34	31	0
34	32	0
34	33	0
34	34	0
34	35	0
35	1	0
35	2	0
35	3	1
35	4	0
35	5	0
35	6	0
35	7	0
35	8	0
35	9	0
35	10	0
35	11	0
35	12	0
35	13	0
35	14	0
35	15	0
35	16	0
35	17	0
35	18	0
35	19	0
35	20	0
35	21	0
35	22	0
35	23	0
35	24	0
35	25	1
35	26	1
35	27	0
35	28	1
35	29	0
35	30	0
35	31	0
35	32	0
35	33	0
35	34	0
35	35	0
36	1	1
36	2	0
36	3	0
36	4	0
36	5	0
36	6	0
36	7	0
36	8	0
36	9	0
36	10	0
36	11	0
36	12	0
36	13	0
36	14	0
36	15	0
36	16	0
36	17	0
36	18	0
36	19	0
36	20	0
36	21	0
36	22	1
36	23	1
36	24	0
36	25	0
36	26	0
36	27	0
36	28	0
36	29	1
36	30	0
36	31	0
36	32	0
36	33	0
36	34	0
36	35	0
37	1	0
37	2	1
37	3	0
37	4	0
37	5	0
37	6	0
37	7	0
37	8	0
37	9	0
37	10	0
37	11	0
37	12	0
37	13	0
37	14	0
37	15	0
37	16	0
37	17	0
37	18	0
37	19	0
37	20	0
37	21	0
37	22	0
37	23	0
37	24	0
37	25	1
37	26	0
37	27	1
37	28	0
37	29	0
37	30	0
37	31	0
37	32	0
37	33	0
37	34	0
37	35	0
38	1	0
38	2	0
38	3	1
38	4	0
38	5	0
38	6	0
38	7	0
38	8	0
38	9	0
38	10	0
38	11	0
38	12	0
38	13	0
38	14	0
38	15	0
38	16	0
38	17	0
38	18	0
38	19	0
38	20	0
38	21	0
38	22	0
38	23	0
38	24	0
38	25	1
38	26	0
38	27	1
38	28	1
38	29	0
38	30	0
38	31	0
38	32	0
38	33	0
38	34	0
38	35	0
39	1	1
39	2	0
39	3	0
39	4	0
39	5	0
39	6	0
39	7	0
39	8	0
39	9	0
39	10	0
39	11	0
39	12	0
39	13	0
39	14	0
39	15	1
39	16	0
39	17	0
39	18	0
39	19	0
39	20	0
39	21	0
39	22	0
39	23	0
39	24	0
39	25	0
39	26	0
39	27	0
39	28	0
39	29	0
39	30	0
39	31	0
39	32	0
39	33	0
39	34	0
39	35	0
40	1	0
40	2	0
40	3	0
40	4	0
40	5	0
40	6	1
40	7	0
40	8	0
40	9	0
40	10	0
40	11	0
40	12	0
40	13	0
40	14	0
40	15	0
40	16	0
40	17	0
40	18	0
40	19	0
40	20	0
40	21	0
40	22	0
40	23	0
40	24	0
40	25	0
40	26	0
40	27	0
40	28	0
40	29	0
40	30	0
40	31	0
40	32	1
40	33	0
40	34	0
40	35	0
41	1	0
41	2	0
41	3	1
41	4	0
41	5	0
41	6	0
41	7	0
41	8	0
41	9	0
41	10	0
41	11	0
41	12	0
41	13	0
41	14	0
41	15	0
41	16	0
41	17	0
41	18	0
41	19	0
41	20	0
41	21	0
41	22	0
41	23	0
41	24	0
41	25	0
41	26	0
41	27	1
41	28	1
41	29	0
41	30	0
41	31	0
41	32	0
41	33	0
41	34	0
41	35	0
42	1	0
42	2	0
42	3	1
42	4	0
42	5	0
42	6	0
42	7	0
42	8	0
42	9	0
42	10	0
42	11	0
42	12	0
42	13	0
42	14	0
42	15	0
42	16	0
42	17	0
42	18	0
42	19	0
42	20	0
42	21	0
42	22	0
42	23	0
42	24	0
42	25	1
42	26	1
42	27	0
42	28	1
42	29	0
42	30	0
42	31	0
42	32	0
42	33	0
42	34	0
42	35	0
43	1	1
43	2	0
43	3	0
43	4	0
43	5	0
43	6	0
43	7	0
43	8	0
43	9	0
43	10	0
43	11	1
43	12	0
43	13	0
43	14	0
43	15	0
43	16	0
43	17	0
43	18	0
43	19	0
43	20	0
43	21	0
43	22	1
43	23	1
43	24	0
43	25	0
43	26	0
43	27	0
43	28	0
43	29	1
43	30	0
43	31	0
43	32	0
43	33	0
43	34	0
43	35	0
44	1	0
44	2	0
44	3	0
44	4	0
44	5	0
44	6	1
44	7	0
44	8	0
44	9	0
44	10	0
44	11	0
44	12	0
44	13	0
44	14	0
44	15	0
44	16	0
44	17	0
44	18	0
44	19	0
44	20	0
44	21	0
44	22	0
44	23	0
44	24	0
44	25	0
44	26	1
44	27	0
44	28	1
44	29	0
44	30	0
44	31	0
44	32	0
44	33	0
44	34	0
44	35	0
45	1	1
45	2	0
45	3	0
45	4	0
45	5	0
45	6	0
45	7	0
45	8	0
45	9	0
45	10	0
45	11	0
45	12	0
45	13	0
45	14	0
45	15	0
45	16	1
45	17	0
45	18	0
45	19	0
45	20	0
45	21	0
45	22	0
45	23	0
45	24	0
45	25	0
45	26	0
45	27	0
45	28	0
45	29	0
45	30	0
45	31	0
45	32	0
45	33	0
45	34	1
45	35	0
46	1	0
46	2	1
46	3	0
46	4	0
46	5	0
46	6	0
46	7	0
46	8	0
46	9	0
46	10	0
46	11	0
46	12	0
46	13	0
46	14	0
46	15	0
46	16	0
46	17	0
46	18	0
46	19	0
46	20	0
46	21	0
46	22	0
46	23	0
46	24	0
46	25	1
46	26	1
46	27	0
46	28	0
46	29	0
46	30	0
46	31	0
46	32	0
46	33	0
46	34	0
46	35	0
47	1	1
47	2	0
47	3	0
47	4	0
47	5	0
47	6	0
47	7	0
47	8	0
47	9	0
47	10	0
47	11	1
47	12	0
47	13	0
47	14	0
47	15	0
47	16	0
47	17	0
47	18	0
47	19	0
47	20	0
47	21	0
47	22	0
47	23	0
47	24	0
47	25	0
47	26	0
47	27	0
47	28	0
47	29	0
47	30	0
47	31	0
47	32	0
47	33	0
47	34	1
47	35	0
48	1	0
48	2	1
48	3	0
48	4	0
48	5	0
48	6	0
48	7	0
48	8	0
48	9	0
48	10	0
48	11	0
48	12	0
48	13	0
48	14	0
48	15	0
48	16	0
48	17	0
48	18	0
48	19	0
48	20	0
48	21	0
48	22	0
48	23	0
48	24	0
48	25	1
48	26	0
48	27	0
48	28	0
48	29	0
48	30	0
48	31	0
48	32	0
48	33	0
48	34	0
48	35	0
49	1	0
49	2	0
49	3	0
49	4	0
49	5	0
49	6	0
49	7	0
49	8	1
49	9	0
49	10	0
49	11	0
49	12	0
49	13	0
49	14	0
49	15	0
49	16	0
49	17	0
49	18	0
49	19	0
49	20	0
49	21	0
49	22	0
49	23	0
49	24	0
49	25	0
49	26	0
49	27	0
49	28	0
49	29	0
49	30	0
49	31	0
49	32	0
49	33	0
49	34	0
49	35	1
50	1	1
50	2	0
50	3	0
50	4	0
50	5	0
50	6	0
50	7	0
50	8	0
50	9	0
50	10	0
50	11	0
50	12	0
50	13	0
50	14	1
50	15	0
50	16	0
50	17	0
50	18	0
50	19	0
50	20	0
50	21	0
50	22	0
50	23	0
50	24	0
50	25	0
50	26	0
50	27	0
50	28	0
50	29	0
50	30	1
50	31	0
50	32	0
50	33	0
50	34	0
50	35	0
51	1	1
51	2	1
51	3	0
51	4	0
51	5	0
51	6	0
51	7	0
51	8	0
51	9	0
51	10	0
51	11	0
51	12	0
51	13	1
51	14	1
51	15	0
51	16	0
51	17	0
51	18	0
51	19	0
51	20	0
51	21	0
51	22	0
51	23	0
51	24	0
51	25	1
51	26	1
51	27	0
51	28	0
51	29	0
51	30	1
51	31	0
51	32	0
51	33	0
51	34	0
51	35	0
52	1	1
52	2	0
52	3	0
52	4	0
52	5	0
52	6	0
52	7	0
52	8	1
52	9	0
52	10	0
52	11	0
52	12	0
52	13	1
52	14	1
52	15	1
52	16	0
52	17	0
52	18	0
52	19	0
52	20	0
52	21	0
52	22	0
52	23	0
52	24	0
52	25	0
52	26	0
52	27	0
52	28	0
52	29	0
52	30	0
52	31	0
52	32	0
52	33	0
52	34	0
52	35	1
53	1	0
53	2	1
53	3	0
53	4	0
53	5	0
53	6	0
53	7	0
53	8	0
53	9	0
53	10	0
53	11	0
53	12	0
53	13	0
53	14	0
53	15	0
53	16	0
53	17	0
53	18	0
53	19	0
53	20	0
53	21	0
53	22	0
53	23	0
53	24	0
53	25	1
53	26	1
53	27	1
53	28	0
53	29	0
53	30	0
53	31	0
53	32	0
53	33	0
53	34	0
53	35	0
54	1	1
54	2	0
54	3	0
54	4	0
54	5	0
54	6	0
54	7	0
54	8	0
54	9	0
54	10	0
54	11	0
54	12	0
54	13	0
54	14	1
54	15	0
54	16	0
54	17	0
54	18	0
54	19	0
54	20	0
54	21	0
54	22	1
54	23	0
54	24	0
54	25	1
54	26	0
54	27	0
54	28	0
54	29	1
54	30	1
54	31	0
54	32	0
54	33	0
54	34	0
54	35	0
55	1	1
55	2	0
55	3	0
55	4	0
55	5	0
55	6	0
55	7	0
55	8	0
55	9	0
55	10	0
55	11	0
55	12	0
55	13	0
55	14	0
55	15	0
55	16	1
55	17	0
55	18	0
55	19	0
55	20	0
55	21	0
55	22	1
55	23	0
55	24	0
55	25	0
55	26	0
55	27	0
55	28	0
55	29	1
55	30	0
55	31	0
55	32	0
55	33	0
55	34	1
55	35	0
56	1	1
56	2	0
56	3	0
56	4	0
56	5	0
56	6	0
56	7	0
56	8	0
56	9	0
56	10	0
56	11	1
56	12	0
56	13	0
56	14	0
56	15	0
56	16	0
56	17	0
56	18	0
56	19	0
56	20	0
56	21	0
56	22	0
56	23	0
56	24	0
56	25	0
56	26	0
56	27	0
56	28	0
56	29	1
56	30	0
56	31	0
56	32	0
56	33	0
56	34	1
56	35	0
57	1	1
57	2	0
57	3	0
57	4	0
57	5	0
57	6	0
57	7	0
57	8	0
57	9	0
57	10	0
57	11	0
57	12	0
57	13	1
57	14	0
57	15	0
57	16	0
57	17	0
57	18	0
57	19	0
57	20	0
57	21	0
57	22	1
57	23	0
57	24	0
57	25	0
57	26	0
57	27	0
57	28	0
57	29	1
57	30	0
57	31	0
57	32	0
57	33	0
57	34	0
57	35	0
58	1	0
58	2	1
58	3	1
58	4	0
58	5	0
58	6	0
58	7	0
58	8	0
58	9	0
58	10	0
58	11	0
58	12	0
58	13	0
58	14	0
58	15	0
58	16	0
58	17	0
58	18	0
58	19	0
58	20	0
58	21	0
58	22	0
58	23	0
58	24	0
58	25	1
58	26	1
58	27	1
58	28	1
58	29	0
58	30	0
58	31	0
58	32	0
58	33	0
58	34	0
58	35	0
59	1	0
59	2	0
59	3	0
59	4	0
59	5	0
59	6	0
59	7	0
59	8	1
59	9	0
59	10	0
59	11	0
59	12	0
59	13	0
59	14	0
59	15	0
59	16	0
59	17	0
59	18	0
59	19	0
59	20	0
59	21	0
59	22	0
59	23	0
59	24	0
59	25	0
59	26	0
59	27	0
59	28	0
59	29	0
59	30	0
59	31	0
59	32	0
59	33	0
59	34	0
59	35	1
61	1	0
61	2	0
61	3	1
61	4	0
61	5	0
61	6	0
61	7	0
61	8	0
61	9	0
61	10	0
61	11	0
61	12	0
61	13	0
61	14	0
61	15	0
61	16	0
61	17	0
61	18	0
61	19	0
61	20	0
61	21	0
61	22	0
61	23	0
61	24	0
61	25	1
61	26	1
61	27	0
61	28	1
61	29	0
61	30	0
61	31	0
61	32	0
61	33	0
61	34	0
61	35	0
62	1	0
62	2	1
62	3	0
62	4	0
62	5	0
62	6	0
62	7	1
62	8	0
62	9	1
62	10	0
62	11	0
62	12	0
62	13	0
62	14	0
62	15	0
62	16	0
62	17	0
62	18	0
62	19	0
62	20	0
62	21	0
62	22	0
62	23	0
62	24	0
62	25	1
62	26	1
62	27	1
62	28	0
62	29	0
62	30	0
62	31	1
62	32	1
62	33	0
62	34	0
62	35	0
63	1	0
63	2	0
63	3	0
63	4	0
63	5	0
63	6	0
63	7	0
63	8	0
63	9	0
63	10	1
63	11	0
63	12	0
63	13	0
63	14	0
63	15	0
63	16	0
63	17	0
63	18	0
63	19	0
63	20	0
63	21	0
63	22	0
63	23	0
63	24	0
63	25	0
63	26	0
63	27	0
63	28	0
63	29	0
63	30	0
63	31	0
63	32	0
63	33	1
63	34	1
63	35	0
64	1	1
64	2	0
64	3	0
64	4	0
64	5	0
64	6	0
64	7	0
64	8	0
64	9	0
64	10	0
64	11	0
64	12	0
64	13	0
64	14	0
64	15	0
64	16	0
64	17	0
64	18	0
64	19	0
64	20	0
64	21	0
64	22	1
64	23	0
64	24	0
64	25	0
64	26	0
64	27	0
64	28	0
64	29	1
64	30	0
64	31	0
64	32	0
64	33	0
64	34	0
64	35	0
65	1	1
65	2	0
65	3	0
65	4	0
65	5	0
65	6	0
65	7	0
65	8	0
65	9	0
65	10	0
65	11	0
65	12	0
65	13	0
65	14	0
65	15	0
65	16	0
65	17	0
65	18	1
65	19	0
65	20	0
65	21	0
65	22	0
65	23	0
65	24	0
65	25	0
65	26	0
65	27	0
65	28	0
65	29	0
65	30	0
65	31	0
65	32	0
65	33	0
65	34	1
65	35	0
66	1	1
66	2	0
66	3	0
66	4	0
66	5	0
66	6	0
66	7	0
66	8	0
66	9	0
66	10	0
66	11	1
66	12	0
66	13	0
66	14	0
66	15	0
66	16	0
66	17	0
66	18	0
66	19	0
66	20	0
66	21	0
66	22	0
66	23	0
66	24	0
66	25	0
66	26	0
66	27	0
66	28	0
66	29	0
66	30	1
66	31	0
66	32	0
66	33	0
66	34	0
66	35	0
67	1	1
67	2	0
67	3	0
67	4	0
67	5	0
67	6	0
67	7	0
67	8	0
67	9	0
67	10	0
67	11	1
67	12	1
67	13	0
67	14	0
67	15	0
67	16	0
67	17	0
67	18	0
67	19	0
67	20	0
67	21	0
67	22	0
67	23	0
67	24	0
67	25	0
67	26	0
67	27	0
67	28	0
67	29	1
67	30	0
67	31	0
67	32	0
67	33	0
67	34	0
67	35	0
68	1	0
68	2	0
68	3	0
68	4	0
68	5	1
68	6	0
68	7	0
68	8	0
68	9	0
68	10	0
68	11	1
68	12	0
68	13	0
68	14	0
68	15	0
68	16	0
68	17	0
68	18	0
68	19	0
68	20	0
68	21	0
68	22	0
68	23	0
68	24	0
68	25	0
68	26	0
68	27	1
68	28	0
68	29	0
68	30	0
68	31	0
68	32	0
68	33	0
68	34	0
68	35	0
69	1	1
69	2	0
69	3	0
69	4	0
69	5	0
69	6	0
69	7	0
69	8	0
69	9	0
69	10	0
69	11	0
69	12	0
69	13	0
69	14	0
69	15	0
69	16	0
69	17	0
69	18	1
69	19	0
69	20	0
69	21	0
69	22	0
69	23	0
69	24	0
69	25	0
69	26	0
69	27	0
69	28	0
69	29	0
69	30	0
69	31	0
69	32	0
69	33	0
69	34	1
69	35	0
70	1	0
70	2	1
70	3	1
70	4	0
70	5	0
70	6	0
70	7	0
70	8	0
70	9	0
70	10	0
70	11	0
70	12	0
70	13	0
70	14	0
70	15	0
70	16	0
70	17	0
70	18	0
70	19	0
70	20	0
70	21	0
70	22	0
70	23	0
70	24	0
70	25	1
70	26	1
70	27	1
70	28	1
70	29	0
70	30	0
70	31	0
70	32	0
70	33	0
70	34	0
70	35	0
71	1	1
71	2	0
71	3	0
71	4	0
71	5	0
71	6	0
71	7	0
71	8	0
71	9	0
71	10	0
71	11	0
71	12	0
71	13	0
71	14	1
71	15	0
71	16	0
71	17	0
71	18	0
71	19	0
71	20	0
71	21	0
71	22	0
71	23	0
71	24	0
71	25	0
71	26	0
71	27	0
71	28	0
71	29	0
71	30	1
71	31	0
71	32	0
71	33	0
71	34	0
71	35	0
72	1	1
72	2	0
72	3	0
72	4	0
72	5	0
72	6	0
72	7	0
72	8	0
72	9	0
72	10	0
72	11	1
72	12	0
72	13	0
72	14	0
72	15	0
72	16	0
72	17	0
72	18	0
72	19	0
72	20	0
72	21	0
72	22	0
72	23	0
72	24	0
72	25	0
72	26	0
72	27	0
72	28	0
72	29	0
72	30	0
72	31	0
72	32	0
72	33	0
72	34	0
72	35	0
73	1	0
73	2	0
73	3	1
73	4	0
73	5	0
73	6	0
73	7	0
73	8	0
73	9	0
73	10	0
73	11	0
73	12	0
73	13	0
73	14	0
73	15	0
73	16	0
73	17	0
73	18	0
73	19	0
73	20	0
73	21	0
73	22	0
73	23	0
73	24	0
73	25	0
73	26	1
73	27	0
73	28	1
73	29	0
73	30	0
73	31	0
73	32	0
73	33	0
73	34	0
73	35	0
74	1	0
74	2	1
74	3	0
74	4	0
74	5	0
74	6	0
74	7	0
74	8	0
74	9	0
74	10	0
74	11	0
74	12	0
74	13	0
74	14	0
74	15	0
74	16	0
74	17	0
74	18	0
74	19	0
74	20	0
74	21	0
74	22	0
74	23	0
74	24	0
74	25	1
74	26	0
74	27	0
74	28	0
74	29	0
74	30	0
74	31	0
74	32	0
74	33	0
74	34	0
74	35	0
75	1	1
75	2	0
75	3	0
75	4	0
75	5	0
75	6	0
75	7	0
75	8	0
75	9	0
75	10	0
75	11	0
75	12	0
75	13	0
75	14	1
75	15	1
75	16	0
75	17	0
75	18	0
75	19	0
75	20	0
75	21	0
75	22	0
75	23	0
75	24	0
75	25	0
75	26	0
75	27	0
75	28	0
75	29	0
75	30	1
75	31	0
75	32	0
75	33	0
75	34	1
75	35	0
76	1	0
76	2	1
76	3	0
76	4	0
76	5	0
76	6	0
76	7	0
76	8	0
76	9	1
76	10	0
76	11	0
76	12	0
76	13	0
76	14	0
76	15	0
76	16	0
76	17	0
76	18	0
76	19	0
76	20	0
76	21	0
76	22	0
76	23	0
76	24	0
76	25	1
76	26	1
76	27	1
76	28	0
76	29	0
76	30	0
76	31	0
76	32	1
76	33	0
76	34	0
76	35	0
77	1	1
77	2	0
77	3	0
77	4	0
77	5	0
77	6	0
77	7	0
77	8	0
77	9	0
77	10	0
77	11	1
77	12	0
77	13	0
77	14	0
77	15	0
77	16	0
77	17	0
77	18	0
77	19	0
77	20	0
77	21	0
77	22	0
77	23	0
77	24	0
77	25	0
77	26	0
77	27	0
77	28	0
77	29	0
77	30	0
77	31	0
77	32	0
77	33	0
77	34	1
77	35	0
78	1	0
78	2	1
78	3	0
78	4	0
78	5	0
78	6	0
78	7	0
78	8	0
78	9	0
78	10	0
78	11	0
78	12	0
78	13	0
78	14	0
78	15	0
78	16	0
78	17	0
78	18	0
78	19	0
78	20	0
78	21	0
78	22	0
78	23	0
78	24	0
78	25	1
78	26	1
78	27	1
78	28	0
78	29	0
78	30	0
78	31	0
78	32	0
78	33	0
78	34	0
78	35	0
79	1	1
79	2	0
79	3	0
79	4	0
79	5	0
79	6	0
79	7	0
79	8	0
79	9	0
79	10	0
79	11	0
79	12	0
79	13	1
79	14	0
79	15	0
79	16	0
79	17	0
79	18	0
79	19	0
79	20	0
79	21	0
79	22	0
79	23	0
79	24	0
79	25	0
79	26	0
79	27	0
79	28	0
79	29	0
79	30	0
79	31	0
79	32	0
79	33	0
79	34	0
79	35	0
80	1	0
80	2	1
80	3	1
80	4	0
80	5	0
80	6	0
80	7	0
80	8	0
80	9	0
80	10	0
80	11	0
80	12	0
80	13	1
80	14	0
80	15	0
80	16	0
80	17	0
80	18	0
80	19	0
80	20	0
80	21	1
80	22	0
80	23	0
80	24	0
80	25	1
80	26	1
80	27	1
80	28	1
80	29	0
80	30	0
80	31	0
80	32	0
80	33	0
80	34	0
80	35	0
81	1	1
81	2	0
81	3	0
81	4	0
81	5	0
81	6	0
81	7	0
81	8	0
81	9	0
81	10	0
81	11	0
81	12	0
81	13	1
81	14	0
81	15	0
81	16	0
81	17	0
81	18	0
81	19	0
81	20	0
81	21	0
81	22	0
81	23	0
81	24	0
81	25	1
81	26	0
81	27	0
81	28	0
81	29	0
81	30	0
81	31	0
81	32	0
81	33	0
81	34	0
81	35	0
82	1	1
82	2	0
82	3	0
82	4	0
82	5	0
82	6	0
82	7	0
82	8	0
82	9	0
82	10	0
82	11	1
82	12	0
82	13	0
82	14	0
82	15	0
82	16	0
82	17	0
82	18	0
82	19	0
82	20	0
82	21	0
82	22	0
82	23	0
82	24	0
82	25	0
82	26	0
82	27	0
82	28	0
82	29	0
82	30	0
82	31	0
82	32	0
82	33	0
82	34	1
82	35	0
83	1	0
83	2	0
83	3	0
83	4	0
83	5	0
83	6	0
83	7	0
83	8	1
83	9	0
83	10	0
83	11	0
83	12	0
83	13	0
83	14	0
83	15	0
83	16	0
83	17	0
83	18	0
83	19	0
83	20	0
83	21	0
83	22	0
83	23	0
83	24	0
83	25	0
83	26	1
83	27	0
83	28	0
83	29	0
83	30	0
83	31	0
83	32	0
83	33	0
83	34	0
83	35	1
84	1	1
84	2	0
84	3	0
84	4	0
84	5	0
84	6	0
84	7	0
84	8	0
84	9	0
84	10	0
84	11	0
84	12	0
84	13	0
84	14	0
84	15	0
84	16	0
84	17	1
84	18	0
84	19	0
84	20	0
84	21	0
84	22	0
84	23	0
84	24	0
84	25	0
84	26	0
84	27	0
84	28	0
84	29	0
84	30	0
84	31	0
84	32	0
84	33	0
84	34	0
84	35	0
85	1	1
85	2	0
85	3	0
85	4	0
85	5	0
85	6	0
85	7	0
85	8	0
85	9	0
85	10	0
85	11	0
85	12	0
85	13	1
85	14	0
85	15	0
85	16	0
85	17	0
85	18	0
85	19	0
85	20	0
85	21	0
85	22	0
85	23	0
85	24	0
85	25	0
85	26	0
85	27	0
85	28	0
85	29	0
85	30	0
85	31	0
85	32	0
85	33	0
85	34	0
85	35	0
86	1	1
86	2	0
86	3	0
86	4	0
86	5	0
86	6	0
86	7	0
86	8	0
86	9	0
86	10	0
86	11	1
86	12	1
86	13	0
86	14	1
86	15	0
86	16	0
86	17	0
86	18	0
86	19	0
86	20	0
86	21	0
86	22	0
86	23	0
86	24	0
86	25	0
86	26	0
86	27	0
86	28	0
86	29	0
86	30	1
86	31	0
86	32	0
86	33	0
86	34	0
86	35	0
87	1	1
87	2	0
87	3	0
87	4	0
87	5	0
87	6	0
87	7	0
87	8	0
87	9	0
87	10	0
87	11	1
87	12	0
87	13	0
87	14	0
87	15	0
87	16	0
87	17	0
87	18	0
87	19	0
87	20	0
87	21	0
87	22	0
87	23	0
87	24	0
87	25	0
87	26	0
87	27	0
87	28	0
87	29	0
87	30	0
87	31	0
87	32	0
87	33	0
87	34	1
87	35	0
88	1	0
88	2	0
88	3	0
88	4	0
88	5	0
88	6	0
88	7	0
88	8	0
88	9	0
88	10	1
88	11	0
88	12	0
88	13	0
88	14	0
88	15	0
88	16	0
88	17	0
88	18	0
88	19	0
88	20	0
88	21	0
88	22	0
88	23	0
88	24	0
88	25	0
88	26	0
88	27	0
88	28	0
88	29	0
88	30	0
88	31	0
88	32	0
88	33	1
88	34	0
88	35	0
89	1	0
89	2	0
89	3	0
89	4	0
89	5	0
89	6	0
89	7	0
89	8	0
89	9	1
89	10	0
89	11	1
89	12	0
89	13	0
89	14	0
89	15	0
89	16	0
89	17	0
89	18	0
89	19	0
89	20	0
89	21	0
89	22	0
89	23	0
89	24	0
89	25	0
89	26	1
89	27	0
89	28	0
89	29	0
89	30	0
89	31	0
89	32	1
89	33	0
89	34	0
89	35	0
90	1	1
90	2	0
90	3	0
90	4	0
90	5	0
90	6	0
90	7	0
90	8	0
90	9	0
90	10	0
90	11	0
90	12	0
90	13	0
90	14	1
90	15	0
90	16	0
90	17	0
90	18	0
90	19	0
90	20	0
90	21	0
90	22	0
90	23	0
90	24	0
90	25	0
90	26	0
90	27	0
90	28	0
90	29	0
90	30	1
90	31	0
90	32	0
90	33	0
90	34	0
90	35	0
91	1	0
91	2	1
91	3	0
91	4	0
91	5	0
91	6	0
91	7	0
91	8	0
91	9	0
91	10	0
91	11	0
91	12	0
91	13	0
91	14	0
91	15	0
91	16	0
91	17	0
91	18	0
91	19	0
91	20	0
91	21	0
91	22	0
91	23	0
91	24	0
91	25	1
91	26	0
91	27	0
91	28	0
91	29	0
91	30	0
91	31	0
91	32	0
91	33	0
91	34	0
91	35	0
92	1	0
92	2	1
92	3	0
92	4	0
92	5	0
92	6	0
92	7	0
92	8	0
92	9	0
92	10	0
92	11	0
92	12	0
92	13	0
92	14	0
92	15	0
92	16	0
92	17	0
92	18	0
92	19	0
92	20	0
92	21	0
92	22	0
92	23	0
92	24	0
92	25	1
92	26	0
92	27	0
92	28	0
92	29	0
92	30	0
92	31	0
92	32	0
92	33	0
92	34	0
92	35	0
93	1	1
93	2	0
93	3	0
93	4	0
93	5	0
93	6	0
93	7	0
93	8	0
93	9	0
93	10	0
93	11	1
93	12	0
93	13	0
93	14	0
93	15	0
93	16	0
93	17	0
93	18	0
93	19	0
93	20	0
93	21	0
93	22	0
93	23	0
93	24	0
93	25	0
93	26	0
93	27	0
93	28	0
93	29	0
93	30	0
93	31	0
93	32	0
93	33	0
93	34	1
93	35	0
94	1	0
94	2	0
94	3	0
94	4	0
94	5	0
94	6	0
94	7	0
94	8	1
94	9	0
94	10	0
94	11	0
94	12	0
94	13	0
94	14	0
94	15	0
94	16	0
94	17	0
94	18	0
94	19	0
94	20	0
94	21	0
94	22	0
94	23	0
94	24	0
94	25	0
94	26	0
94	27	0
94	28	0
94	29	0
94	30	0
94	31	0
94	32	0
94	33	0
94	34	1
94	35	1
95	1	1
95	2	0
95	3	0
95	4	0
95	5	0
95	6	0
95	7	0
95	8	0
95	9	0
95	10	0
95	11	0
95	12	0
95	13	1
95	14	0
95	15	0
95	16	0
95	17	0
95	18	0
95	19	0
95	20	0
95	21	0
95	22	0
95	23	0
95	24	0
95	25	0
95	26	0
95	27	0
95	28	0
95	29	0
95	30	0
95	31	0
95	32	0
95	33	0
95	34	0
95	35	0
96	1	0
96	2	0
96	3	0
96	4	0
96	5	0
96	6	1
96	7	0
96	8	0
96	9	0
96	10	0
96	11	0
96	12	0
96	13	0
96	14	0
96	15	1
96	16	0
96	17	0
96	18	0
96	19	0
96	20	0
96	21	0
96	22	0
96	23	0
96	24	0
96	25	0
96	26	1
96	27	0
96	28	0
96	29	0
96	30	0
96	31	0
96	32	1
96	33	0
96	34	0
96	35	0
97	1	0
97	2	0
97	3	1
97	4	0
97	5	0
97	6	0
97	7	0
97	8	0
97	9	0
97	10	0
97	11	0
97	12	0
97	13	0
97	14	0
97	15	0
97	16	0
97	17	0
97	18	0
97	19	0
97	20	0
97	21	0
97	22	0
97	23	0
97	24	0
97	25	0
97	26	1
97	27	1
97	28	1
97	29	0
97	30	0
97	31	0
97	32	0
97	33	0
97	34	0
97	35	0
98	1	1
98	2	0
98	3	0
98	4	0
98	5	0
98	6	0
98	7	0
98	8	0
98	9	0
98	10	0
98	11	0
98	12	0
98	13	0
98	14	0
98	15	0
98	16	0
98	17	0
98	18	0
98	19	0
98	20	0
98	21	0
98	22	0
98	23	0
98	24	0
98	25	0
98	26	0
98	27	0
98	28	0
98	29	1
98	30	0
98	31	0
98	32	0
98	33	0
98	34	0
98	35	0
99	1	1
99	2	0
99	3	0
99	4	0
99	5	0
99	6	0
99	7	0
99	8	0
99	9	0
99	10	0
99	11	1
99	12	0
99	13	0
99	14	0
99	15	0
99	16	0
99	17	0
99	18	0
99	19	0
99	20	0
99	21	0
99	22	0
99	23	0
99	24	0
99	25	0
99	26	0
99	27	0
99	28	0
99	29	0
99	30	0
99	31	0
99	32	0
99	33	0
99	34	1
99	35	0
100	1	0
100	2	0
100	3	1
100	4	0
100	5	0
100	6	0
100	7	0
100	8	0
100	9	0
100	10	0
100	11	0
100	12	0
100	13	0
100	14	0
100	15	0
100	16	0
100	17	0
100	18	0
100	19	0
100	20	0
100	21	0
100	22	0
100	23	0
100	24	0
100	25	0
100	26	1
100	27	1
100	28	1
100	29	0
100	30	0
100	31	0
100	32	0
100	33	0
100	34	0
100	35	0
101	1	1
101	2	0
101	3	0
101	4	0
101	5	0
101	6	0
101	7	0
101	8	0
101	9	1
101	10	0
101	11	0
101	12	0
101	13	0
101	14	0
101	15	0
101	16	0
101	17	0
101	18	0
101	19	0
101	20	0
101	21	0
101	22	0
101	23	0
101	24	0
101	25	0
101	26	1
101	27	0
101	28	0
101	29	0
101	30	0
101	31	0
101	32	0
101	33	0
101	34	1
101	35	0
102	1	1
102	2	0
102	3	0
102	4	0
102	5	0
102	6	0
102	7	0
102	8	0
102	9	0
102	10	0
102	11	0
102	12	0
102	13	0
102	14	1
102	15	0
102	16	0
102	17	0
102	18	0
102	19	0
102	20	0
102	21	0
102	22	0
102	23	0
102	24	0
102	25	0
102	26	0
102	27	0
102	28	0
102	29	0
102	30	1
102	31	0
102	32	0
102	33	0
102	34	0
102	35	0
103	1	1
103	2	0
103	3	0
103	4	0
103	5	0
103	6	0
103	7	0
103	8	0
103	9	0
103	10	0
103	11	0
103	12	0
103	13	0
103	14	0
103	15	0
103	16	0
103	17	0
103	18	0
103	19	0
103	20	0
103	21	0
103	22	1
103	23	0
103	24	0
103	25	0
103	26	0
103	27	0
103	28	0
103	29	1
103	30	0
103	31	0
103	32	0
103	33	0
103	34	1
103	35	0
104	1	1
104	2	0
104	3	0
104	4	0
104	5	0
104	6	0
104	7	0
104	8	0
104	9	0
104	10	0
104	11	0
104	12	0
104	13	0
104	14	0
104	15	0
104	16	0
104	17	1
104	18	0
104	19	0
104	20	0
104	21	0
104	22	0
104	23	0
104	24	0
104	25	0
104	26	0
104	27	0
104	28	0
104	29	0
104	30	0
104	31	0
104	32	0
104	33	0
104	34	0
104	35	0
105	1	1
105	2	0
105	3	0
105	4	0
105	5	0
105	6	0
105	7	0
105	8	0
105	9	0
105	10	0
105	11	0
105	12	0
105	13	1
105	14	0
105	15	0
105	16	0
105	17	0
105	18	0
105	19	0
105	20	0
105	21	0
105	22	0
105	23	0
105	24	0
105	25	0
105	26	0
105	27	0
105	28	0
105	29	0
105	30	0
105	31	0
105	32	0
105	33	0
105	34	0
105	35	0
106	1	1
106	2	0
106	3	0
106	4	0
106	5	0
106	6	0
106	7	0
106	8	0
106	9	0
106	10	0
106	11	0
106	12	0
106	13	1
106	14	0
106	15	0
106	16	0
106	17	0
106	18	0
106	19	0
106	20	0
106	21	0
106	22	0
106	23	0
106	24	0
106	25	0
106	26	0
106	27	0
106	28	0
106	29	0
106	30	0
106	31	0
106	32	0
106	33	0
106	34	0
106	35	0
107	1	0
107	2	0
107	3	1
107	4	0
107	5	0
107	6	0
107	7	0
107	8	0
107	9	0
107	10	0
107	11	0
107	12	0
107	13	0
107	14	0
107	15	0
107	16	0
107	17	0
107	18	0
107	19	0
107	20	0
107	21	0
107	22	0
107	23	0
107	24	0
107	25	0
107	26	1
107	27	1
107	28	1
107	29	0
107	30	0
107	31	0
107	32	0
107	33	0
107	34	0
107	35	0
108	1	1
108	2	0
108	3	0
108	4	0
108	5	0
108	6	0
108	7	0
108	8	0
108	9	0
108	10	0
108	11	0
108	12	0
108	13	0
108	14	0
108	15	0
108	16	1
108	17	0
108	18	0
108	19	0
108	20	0
108	21	0
108	22	0
108	23	0
108	24	0
108	25	0
108	26	0
108	27	0
108	28	0
108	29	0
108	30	0
108	31	0
108	32	0
108	33	0
108	34	1
108	35	0
109	1	0
109	2	1
109	3	0
109	4	0
109	5	0
109	6	0
109	7	0
109	8	0
109	9	0
109	10	0
109	11	0
109	12	0
109	13	0
109	14	0
109	15	0
109	16	0
109	17	0
109	18	0
109	19	0
109	20	0
109	21	0
109	22	0
109	23	0
109	24	0
109	25	1
109	26	1
109	27	1
109	28	0
109	29	0
109	30	0
109	31	0
109	32	0
109	33	0
109	34	0
109	35	0
110	1	1
110	2	0
110	3	0
110	4	0
110	5	0
110	6	0
110	7	0
110	8	0
110	9	0
110	10	0
110	11	0
110	12	0
110	13	0
110	14	0
110	15	0
110	16	0
110	17	0
110	18	0
110	19	0
110	20	0
110	21	0
110	22	1
110	23	0
110	24	0
110	25	0
110	26	0
110	27	0
110	28	0
110	29	1
110	30	0
110	31	0
110	32	0
110	33	0
110	34	0
110	35	0
111	1	0
111	2	0
111	3	1
111	4	0
111	5	0
111	6	0
111	7	0
111	8	0
111	9	0
111	10	0
111	11	0
111	12	0
111	13	0
111	14	0
111	15	0
111	16	0
111	17	0
111	18	0
111	19	0
111	20	0
111	21	0
111	22	0
111	23	0
111	24	0
111	25	1
111	26	0
111	27	1
111	28	1
111	29	0
111	30	0
111	31	0
111	32	0
111	33	0
111	34	0
111	35	0
112	1	1
112	2	0
112	3	0
112	4	0
112	5	0
112	6	0
112	7	0
112	8	0
112	9	0
112	10	0
112	11	1
112	12	0
112	13	0
112	14	0
112	15	0
112	16	0
112	17	0
112	18	0
112	19	0
112	20	0
112	21	0
112	22	0
112	23	0
112	24	0
112	25	0
112	26	0
112	27	0
112	28	0
112	29	0
112	30	0
112	31	0
112	32	0
112	33	0
112	34	1
112	35	0
113	1	1
113	2	0
113	3	0
113	4	0
113	5	0
113	6	0
113	7	0
113	8	0
113	9	0
113	10	0
113	11	1
113	12	0
113	13	0
113	14	0
113	15	0
113	16	0
113	17	0
113	18	0
113	19	0
113	20	0
113	21	0
113	22	1
113	23	0
113	24	0
113	25	0
113	26	0
113	27	0
113	28	0
113	29	1
113	30	0
113	31	0
113	32	0
113	33	0
113	34	1
113	35	0
114	1	0
114	2	0
114	3	1
114	4	0
114	5	0
114	6	0
114	7	0
114	8	0
114	9	0
114	10	0
114	11	0
114	12	0
114	13	0
114	14	0
114	15	0
114	16	0
114	17	0
114	18	0
114	19	0
114	20	0
114	21	0
114	22	0
114	23	0
114	24	0
114	25	0
114	26	0
114	27	1
114	28	1
114	29	0
114	30	0
114	31	0
114	32	0
114	33	0
114	34	0
114	35	0
115	1	0
115	2	0
115	3	0
115	4	0
115	5	0
115	6	0
115	7	1
115	8	0
115	9	0
115	10	0
115	11	0
115	12	0
115	13	0
115	14	0
115	15	0
115	16	0
115	17	0
115	18	0
115	19	0
115	20	0
115	21	0
115	22	0
115	23	0
115	24	0
115	25	0
115	26	1
115	27	0
115	28	0
115	29	0
115	30	0
115	31	1
115	32	0
115	33	0
115	34	0
115	35	0
116	1	1
116	2	0
116	3	0
116	4	0
116	5	0
116	6	0
116	7	0
116	8	0
116	9	0
116	10	1
116	11	0
116	12	0
116	13	0
116	14	0
116	15	0
116	16	1
116	17	0
116	18	0
116	19	0
116	20	0
116	21	0
116	22	0
116	23	0
116	24	0
116	25	0
116	26	0
116	27	0
116	28	0
116	29	0
116	30	0
116	31	0
116	32	0
116	33	1
116	34	1
116	35	0
117	1	0
117	2	0
117	3	0
117	4	0
117	5	0
117	6	0
117	7	0
117	8	1
117	9	0
117	10	0
117	11	0
117	12	0
117	13	0
117	14	0
117	15	0
117	16	0
117	17	0
117	18	0
117	19	0
117	20	0
117	21	0
117	22	0
117	23	0
117	24	0
117	25	0
117	26	0
117	27	0
117	28	0
117	29	0
117	30	0
117	31	0
117	32	1
117	33	0
117	34	0
117	35	1
118	1	1
118	2	0
118	3	0
118	4	0
118	5	0
118	6	0
118	7	0
118	8	0
118	9	0
118	10	0
118	11	0
118	12	0
118	13	1
118	14	0
118	15	0
118	16	1
118	17	0
118	18	0
118	19	0
118	20	0
118	21	0
118	22	0
118	23	0
118	24	0
118	25	0
118	26	0
118	27	0
118	28	0
118	29	0
118	30	0
118	31	0
118	32	0
118	33	0
118	34	1
118	35	0
119	1	1
119	2	0
119	3	1
119	4	0
119	5	0
119	6	0
119	7	0
119	8	0
119	9	0
119	10	0
119	11	0
119	12	0
119	13	1
119	14	1
119	15	0
119	16	0
119	17	0
119	18	0
119	19	0
119	20	0
119	21	0
119	22	0
119	23	0
119	24	0
119	25	0
119	26	0
119	27	0
119	28	1
119	29	0
119	30	1
119	31	0
119	32	0
119	33	0
119	34	0
119	35	0
120	1	1
120	2	0
120	3	0
120	4	0
120	5	0
120	6	0
120	7	0
120	8	0
120	9	0
120	10	0
120	11	0
120	12	0
120	13	1
120	14	0
120	15	0
120	16	0
120	17	1
120	18	0
120	19	0
120	20	0
120	21	0
120	22	1
120	23	0
120	24	0
120	25	0
120	26	0
120	27	0
120	28	0
120	29	1
120	30	0
120	31	0
120	32	0
120	33	0
120	34	0
120	35	0
121	1	1
121	2	0
121	3	0
121	4	0
121	5	0
121	6	0
121	7	0
121	8	0
121	9	0
121	10	0
121	11	0
121	12	0
121	13	0
121	14	0
121	15	0
121	16	0
121	17	0
121	18	1
121	19	0
121	20	0
121	21	0
121	22	1
121	23	0
121	24	0
121	25	0
121	26	0
121	27	0
121	28	0
121	29	1
121	30	0
121	31	0
121	32	0
121	33	0
121	34	0
121	35	0
122	1	0
122	2	0
122	3	0
122	4	0
122	5	0
122	6	0
122	7	0
122	8	1
122	9	0
122	10	0
122	11	0
122	12	0
122	13	1
122	14	0
122	15	0
122	16	0
122	17	0
122	18	0
122	19	0
122	20	0
122	21	0
122	22	0
122	23	0
122	24	0
122	25	0
122	26	1
122	27	0
122	28	0
122	29	0
122	30	0
122	31	0
122	32	0
122	33	0
122	34	0
122	35	1
123	1	1
123	2	0
123	3	0
123	4	0
123	5	0
123	6	0
123	7	0
123	8	0
123	9	0
123	10	0
123	11	0
123	12	0
123	13	0
123	14	0
123	15	0
123	16	0
123	17	0
123	18	1
123	19	0
123	20	0
123	21	0
123	22	0
123	23	0
123	24	0
123	25	0
123	26	1
123	27	0
123	28	1
123	29	1
123	30	0
123	31	0
123	32	0
123	33	0
123	34	0
123	35	0
124	1	1
124	2	0
124	3	0
124	4	0
124	5	0
124	6	0
124	7	0
124	8	0
124	9	0
124	10	0
124	11	0
124	12	0
124	13	1
124	14	1
124	15	0
124	16	0
124	17	0
124	18	0
124	19	0
124	20	0
124	21	0
124	22	1
124	23	0
124	24	0
124	25	0
124	26	0
124	27	0
124	28	0
124	29	1
124	30	1
124	31	0
124	32	0
124	33	0
124	34	0
124	35	0
125	1	1
125	2	0
125	3	0
125	4	0
125	5	0
125	6	0
125	7	0
125	8	0
125	9	0
125	10	0
125	11	0
125	12	0
125	13	0
125	14	1
125	15	0
125	16	0
125	17	0
125	18	0
125	19	0
125	20	0
125	21	0
125	22	0
125	23	0
125	24	0
125	25	0
125	26	0
125	27	0
125	28	0
125	29	0
125	30	1
125	31	0
125	32	0
125	33	0
125	34	0
125	35	0
126	1	0
126	2	1
126	3	0
126	4	0
126	5	0
126	6	0
126	7	0
126	8	0
126	9	0
126	10	0
126	11	0
126	12	0
126	13	0
126	14	0
126	15	0
126	16	0
126	17	0
126	18	0
126	19	0
126	20	0
126	21	0
126	22	0
126	23	0
126	24	0
126	25	1
126	26	1
126	27	1
126	28	0
126	29	0
126	30	0
126	31	0
126	32	0
126	33	0
126	34	0
126	35	0
127	1	0
127	2	0
127	3	1
127	4	0
127	5	0
127	6	0
127	7	0
127	8	0
127	9	0
127	10	0
127	11	0
127	12	0
127	13	0
127	14	0
127	15	0
127	16	0
127	17	0
127	18	0
127	19	0
127	20	0
127	21	0
127	22	0
127	23	0
127	24	0
127	25	1
127	26	0
127	27	1
127	28	1
127	29	0
127	30	0
127	31	0
127	32	0
127	33	0
127	34	0
127	35	0
128	1	0
128	2	1
128	3	0
128	4	0
128	5	0
128	6	0
128	7	0
128	8	0
128	9	0
128	10	0
128	11	0
128	12	0
128	13	0
128	14	0
128	15	0
128	16	0
128	17	0
128	18	0
128	19	0
128	20	0
128	21	0
128	22	0
128	23	0
128	24	0
128	25	1
128	26	1
128	27	1
128	28	0
128	29	0
128	30	0
128	31	0
128	32	0
128	33	0
128	34	0
128	35	0
129	1	0
129	2	1
129	3	0
129	4	0
129	5	0
129	6	0
129	7	0
129	8	0
129	9	0
129	10	0
129	11	0
129	12	0
129	13	0
129	14	0
129	15	0
129	16	0
129	17	0
129	18	0
129	19	0
129	20	0
129	21	0
129	22	0
129	23	0
129	24	0
129	25	1
129	26	1
129	27	1
129	28	0
129	29	0
129	30	0
129	31	0
129	32	0
129	33	0
129	34	0
129	35	0
130	1	0
130	2	0
130	3	0
130	4	0
130	5	1
130	6	0
130	7	0
130	8	0
130	9	0
130	10	0
130	11	0
130	12	0
130	13	0
130	14	0
130	15	1
130	16	0
130	17	0
130	18	0
130	19	0
130	20	0
130	21	0
130	22	0
130	23	0
130	24	0
130	25	1
130	26	0
130	27	1
130	28	0
130	29	0
130	30	0
130	31	0
130	32	0
130	33	0
130	34	0
130	35	0
131	1	1
131	2	0
131	3	0
131	4	0
131	5	0
131	6	0
131	7	0
131	8	0
131	9	0
131	10	0
131	11	1
131	12	0
131	13	0
131	14	0
131	15	0
131	16	0
131	17	0
131	18	0
131	19	0
131	20	0
131	21	0
131	22	0
131	23	0
131	24	0
131	25	0
131	26	0
131	27	0
131	28	0
131	29	0
131	30	0
131	31	0
131	32	0
131	33	0
131	34	1
131	35	0
132	1	0
132	2	0
132	3	0
132	4	0
132	5	1
132	6	0
132	7	0
132	8	0
132	9	0
132	10	0
132	11	0
132	12	0
132	13	0
132	14	0
132	15	1
132	16	0
132	17	0
132	18	0
132	19	0
132	20	0
132	21	0
132	22	0
132	23	0
132	24	0
132	25	0
132	26	0
132	27	1
132	28	0
132	29	0
132	30	0
132	31	0
132	32	0
132	33	0
132	34	0
132	35	0
133	1	0
133	2	0
133	3	1
133	4	0
133	5	0
133	6	0
133	7	0
133	8	0
133	9	0
133	10	0
133	11	0
133	12	0
133	13	0
133	14	0
133	15	0
133	16	0
133	17	0
133	18	0
133	19	0
133	20	0
133	21	0
133	22	0
133	23	0
133	24	0
133	25	0
133	26	0
133	27	1
133	28	1
133	29	0
133	30	0
133	31	0
133	32	0
133	33	0
133	34	0
133	35	0
134	1	0
134	2	0
134	3	1
134	4	0
134	5	0
134	6	0
134	7	0
134	8	0
134	9	0
134	10	0
134	11	0
134	12	0
134	13	0
134	14	0
134	15	0
134	16	0
134	17	0
134	18	0
134	19	0
134	20	0
134	21	0
134	22	0
134	23	0
134	24	0
134	25	1
134	26	0
134	27	1
134	28	1
134	29	0
134	30	0
134	31	0
134	32	0
134	33	0
134	34	0
134	35	0
135	1	0
135	2	0
135	3	1
135	4	0
135	5	0
135	6	0
135	7	0
135	8	0
135	9	0
135	10	0
135	11	0
135	12	0
135	13	0
135	14	0
135	15	0
135	16	0
135	17	0
135	18	0
135	19	0
135	20	0
135	21	0
135	22	0
135	23	0
135	24	0
135	25	0
135	26	0
135	27	1
135	28	1
135	29	0
135	30	0
135	31	0
135	32	0
135	33	0
135	34	0
135	35	0
136	1	0
136	2	0
136	3	0
136	4	0
136	5	0
136	6	1
136	7	0
136	8	0
136	9	0
136	10	0
136	11	0
136	12	0
136	13	0
136	14	0
136	15	0
136	16	0
136	17	0
136	18	0
136	19	0
136	20	0
136	21	0
136	22	0
136	23	0
136	24	0
136	25	0
136	26	1
136	27	0
136	28	0
136	29	0
136	30	0
136	31	0
136	32	1
136	33	0
136	34	0
136	35	0
137	1	0
137	2	1
137	3	0
137	4	0
137	5	0
137	6	0
137	7	0
137	8	0
137	9	0
137	10	0
137	11	0
137	12	0
137	13	0
137	14	0
137	15	0
137	16	0
137	17	0
137	18	0
137	19	0
137	20	0
137	21	0
137	22	0
137	23	0
137	24	0
137	25	1
137	26	0
137	27	1
137	28	0
137	29	0
137	30	0
137	31	0
137	32	0
137	33	0
137	34	0
137	35	0
138	1	0
138	2	0
138	3	1
138	4	0
138	5	0
138	6	0
138	7	0
138	8	0
138	9	0
138	10	0
138	11	0
138	12	0
138	13	0
138	14	0
138	15	0
138	16	0
138	17	0
138	18	0
138	19	0
138	20	0
138	21	0
138	22	0
138	23	0
138	24	0
138	25	1
138	26	0
138	27	1
138	28	1
138	29	0
138	30	0
138	31	0
138	32	0
138	33	0
138	34	0
138	35	0
139	1	0
139	2	0
139	3	0
139	4	1
139	5	0
139	6	0
139	7	0
139	8	0
139	9	0
139	10	0
139	11	0
139	12	0
139	13	0
139	14	0
139	15	0
139	16	0
139	17	0
139	18	0
139	19	0
139	20	0
139	21	0
139	22	0
139	23	0
139	24	0
139	25	1
139	26	1
139	27	1
139	28	0
139	29	0
139	30	0
139	31	0
139	32	0
139	33	0
139	34	0
139	35	0
140	1	0
140	2	1
140	3	0
140	4	0
140	5	0
140	6	0
140	7	0
140	8	0
140	9	0
140	10	0
140	11	0
140	12	0
140	13	0
140	14	0
140	15	0
140	16	0
140	17	0
140	18	0
140	19	0
140	20	0
140	21	0
140	22	0
140	23	0
140	24	0
140	25	1
140	26	0
140	27	0
140	28	0
140	29	0
140	30	0
140	31	0
140	32	0
140	33	0
140	34	0
140	35	0
141	1	1
141	2	0
141	3	0
141	4	0
141	5	0
141	6	0
141	7	0
141	8	0
141	9	0
141	10	0
141	11	0
141	12	0
141	13	0
141	14	0
141	15	0
141	16	0
141	17	0
141	18	0
141	19	0
141	20	0
141	21	0
141	22	1
141	23	1
141	24	0
141	25	0
141	26	0
141	27	0
141	28	0
141	29	1
141	30	0
141	31	0
141	32	0
141	33	0
141	34	0
141	35	0
142	1	1
142	2	0
142	3	0
142	4	0
142	5	0
142	6	0
142	7	0
142	8	0
142	9	0
142	10	0
142	11	0
142	12	0
142	13	0
142	14	0
142	15	0
142	16	1
142	17	0
142	18	0
142	19	0
142	20	0
142	21	0
142	22	0
142	23	0
142	24	0
142	25	0
142	26	0
142	27	0
142	28	0
142	29	0
142	30	0
142	31	0
142	32	0
142	33	0
142	34	1
142	35	0
143	1	0
143	2	0
143	3	1
143	4	0
143	5	0
143	6	0
143	7	0
143	8	0
143	9	0
143	10	0
143	11	0
143	12	0
143	13	0
143	14	0
143	15	0
143	16	0
143	17	0
143	18	0
143	19	0
143	20	0
143	21	0
143	22	0
143	23	0
143	24	0
143	25	0
143	26	0
143	27	1
143	28	1
143	29	0
143	30	0
143	31	0
143	32	0
143	33	0
143	34	0
143	35	0
144	1	0
144	2	0
144	3	1
144	4	0
144	5	0
144	6	0
144	7	0
144	8	0
144	9	0
144	10	0
144	11	0
144	12	0
144	13	0
144	14	0
144	15	0
144	16	0
144	17	0
144	18	0
144	19	0
144	20	0
144	21	0
144	22	0
144	23	0
144	24	0
144	25	0
144	26	1
144	27	0
144	28	1
144	29	0
144	30	0
144	31	0
144	32	0
144	33	0
144	34	0
144	35	0
145	1	0
145	2	0
145	3	1
145	4	0
145	5	0
145	6	0
145	7	0
145	8	0
145	9	0
145	10	0
145	11	0
145	12	0
145	13	0
145	14	0
145	15	0
145	16	0
145	17	0
145	18	0
145	19	0
145	20	0
145	21	0
145	22	0
145	23	0
145	24	0
145	25	0
145	26	0
145	27	1
145	28	1
145	29	0
145	30	0
145	31	0
145	32	0
145	33	0
145	34	0
145	35	0
146	1	1
146	2	0
146	3	0
146	4	0
146	5	0
146	6	0
146	7	0
146	8	0
146	9	0
146	10	0
146	11	0
146	12	0
146	13	1
146	14	0
146	15	0
146	16	0
146	17	0
146	18	0
146	19	0
146	20	0
146	21	0
146	22	0
146	23	0
146	24	0
146	25	0
146	26	0
146	27	0
146	28	0
146	29	0
146	30	0
146	31	0
146	32	0
146	33	0
146	34	0
146	35	0
147	1	1
147	2	0
147	3	0
147	4	0
147	5	0
147	6	0
147	7	0
147	8	0
147	9	0
147	10	0
147	11	1
147	12	0
147	13	0
147	14	0
147	15	0
147	16	0
147	17	0
147	18	0
147	19	0
147	20	0
147	21	0
147	22	0
147	23	0
147	24	0
147	25	0
147	26	0
147	27	0
147	28	0
147	29	0
147	30	0
147	31	0
147	32	0
147	33	0
147	34	1
147	35	0
148	1	1
148	2	0
148	3	0
148	4	0
148	5	0
148	6	0
148	7	0
148	8	0
148	9	0
148	10	0
148	11	1
148	12	0
148	13	0
148	14	0
148	15	0
148	16	0
148	17	0
148	18	0
148	19	0
148	20	0
148	21	0
148	22	0
148	23	0
148	24	0
148	25	0
148	26	0
148	27	0
148	28	0
148	29	0
148	30	0
148	31	0
148	32	0
148	33	0
148	34	1
148	35	0
149	1	1
149	2	0
149	3	0
149	4	0
149	5	0
149	6	0
149	7	0
149	8	0
149	9	0
149	10	0
149	11	1
149	12	0
149	13	0
149	14	0
149	15	0
149	16	0
149	17	0
149	18	1
149	19	0
149	20	0
149	21	0
149	22	0
149	23	0
149	24	0
149	25	0
149	26	0
149	27	0
149	28	0
149	29	0
149	30	0
149	31	0
149	32	0
149	33	0
149	34	1
149	35	0
150	1	1
150	2	1
150	3	0
150	4	0
150	5	0
150	6	0
150	7	0
150	8	0
150	9	0
150	10	0
150	11	1
150	12	1
150	13	0
150	14	0
150	15	0
150	16	0
150	17	0
150	18	0
150	19	0
150	20	0
150	21	0
150	22	0
150	23	0
150	24	0
150	25	1
150	26	0
150	27	0
150	28	0
150	29	0
150	30	0
150	31	0
150	32	0
150	33	0
150	34	1
150	35	0
151	1	0
151	2	0
151	3	1
151	4	0
151	5	0
151	6	0
151	7	0
151	8	0
151	9	0
151	10	0
151	11	0
151	12	0
151	13	0
151	14	0
151	15	0
151	16	0
151	17	0
151	18	0
151	19	0
151	20	0
151	21	0
151	22	0
151	23	0
151	24	0
151	25	1
151	26	0
151	27	1
151	28	1
151	29	0
151	30	0
151	31	0
151	32	0
151	33	0
151	34	0
151	35	0
152	1	1
152	2	0
152	3	0
152	4	0
152	5	0
152	6	0
152	7	0
152	8	0
152	9	0
152	10	0
152	11	0
152	12	0
152	13	0
152	14	0
152	15	0
152	16	0
152	17	1
152	18	0
152	19	0
152	20	0
152	21	0
152	22	0
152	23	0
152	24	0
152	25	0
152	26	0
152	27	0
152	28	0
152	29	0
152	30	0
152	31	0
152	32	0
152	33	0
152	34	0
152	35	0
153	1	1
153	2	0
153	3	0
153	4	0
153	5	0
153	6	0
153	7	0
153	8	0
153	9	0
153	10	0
153	11	0
153	12	0
153	13	0
153	14	0
153	15	0
153	16	1
153	17	0
153	18	0
153	19	0
153	20	0
153	21	0
153	22	0
153	23	0
153	24	0
153	25	0
153	26	0
153	27	0
153	28	0
153	29	0
153	30	0
153	31	0
153	32	0
153	33	0
153	34	0
153	35	0
154	1	0
154	2	0
154	3	1
154	4	0
154	5	0
154	6	0
154	7	0
154	8	0
154	9	0
154	10	0
154	11	0
154	12	0
154	13	0
154	14	0
154	15	0
154	16	0
154	17	0
154	18	0
154	19	0
154	20	0
154	21	0
154	22	0
154	23	0
154	24	0
154	25	0
154	26	0
154	27	0
154	28	1
154	29	0
154	30	0
154	31	0
154	32	0
154	33	0
154	34	0
154	35	0
155	1	1
155	2	0
155	3	0
155	4	0
155	5	0
155	6	0
155	7	0
155	8	0
155	9	0
155	10	0
155	11	0
155	12	0
155	13	0
155	14	0
155	15	0
155	16	1
155	17	0
155	18	0
155	19	0
155	20	0
155	21	0
155	22	0
155	23	0
155	24	0
155	25	0
155	26	0
155	27	0
155	28	0
155	29	0
155	30	0
155	31	0
155	32	0
155	33	0
155	34	1
155	35	0
156	1	1
156	2	0
156	3	0
156	4	0
156	5	0
156	6	0
156	7	0
156	8	0
156	9	0
156	10	0
156	11	0
156	12	0
156	13	1
156	14	0
156	15	0
156	16	1
156	17	0
156	18	0
156	19	0
156	20	0
156	21	0
156	22	1
156	23	0
156	24	0
156	25	0
156	26	1
156	27	0
156	28	0
156	29	1
156	30	0
156	31	0
156	32	0
156	33	0
156	34	0
156	35	0
157	1	1
157	2	0
157	3	0
157	4	0
157	5	0
157	6	0
157	7	0
157	8	0
157	9	0
157	10	0
157	11	0
157	12	0
157	13	0
157	14	1
157	15	0
157	16	0
157	17	0
157	18	0
157	19	0
157	20	0
157	21	0
157	22	0
157	23	0
157	24	0
157	25	0
157	26	0
157	27	0
157	28	0
157	29	0
157	30	1
157	31	0
157	32	0
157	33	0
157	34	0
157	35	0
158	1	1
158	2	0
158	3	0
158	4	0
158	5	0
158	6	0
158	7	0
158	8	0
158	9	0
158	10	0
158	11	0
158	12	0
158	13	0
158	14	0
158	15	0
158	16	0
158	17	1
158	18	0
158	19	0
158	20	0
158	21	0
158	22	0
158	23	0
158	24	0
158	25	0
158	26	0
158	27	0
158	28	0
158	29	0
158	30	0
158	31	0
158	32	0
158	33	0
158	34	0
158	35	0
159	1	0
159	2	0
159	3	0
159	4	0
159	5	0
159	6	0
159	7	0
159	8	0
159	9	1
159	10	0
159	11	0
159	12	0
159	13	0
159	14	0
159	15	0
159	16	0
159	17	0
159	18	0
159	19	0
159	20	0
159	21	0
159	22	0
159	23	0
159	24	0
159	25	0
159	26	1
159	27	0
159	28	0
159	29	0
159	30	0
159	31	0
159	32	0
159	33	0
159	34	0
159	35	0
160	1	1
160	2	0
160	3	0
160	4	0
160	5	0
160	6	0
160	7	0
160	8	0
160	9	0
160	10	0
160	11	0
160	12	0
160	13	0
160	14	1
160	15	0
160	16	0
160	17	0
160	18	0
160	19	0
160	20	0
160	21	0
160	22	0
160	23	0
160	24	0
160	25	0
160	26	1
160	27	0
160	28	0
160	29	0
160	30	1
160	31	0
160	32	0
160	33	0
160	34	0
160	35	0
161	1	0
161	2	0
161	3	1
161	4	0
161	5	0
161	6	0
161	7	0
161	8	0
161	9	0
161	10	0
161	11	0
161	12	0
161	13	0
161	14	0
161	15	0
161	16	0
161	17	0
161	18	0
161	19	0
161	20	0
161	21	0
161	22	0
161	23	0
161	24	0
161	25	1
161	26	1
161	27	1
161	28	1
161	29	0
161	30	0
161	31	0
161	32	0
161	33	0
161	34	0
161	35	0
162	1	1
162	2	0
162	3	0
162	4	0
162	5	0
162	6	0
162	7	0
162	8	0
162	9	0
162	10	0
162	11	1
162	12	0
162	13	0
162	14	0
162	15	0
162	16	0
162	17	0
162	18	0
162	19	0
162	20	0
162	21	0
162	22	0
162	23	0
162	24	0
162	25	0
162	26	0
162	27	0
162	28	0
162	29	0
162	30	0
162	31	0
162	32	0
162	33	0
162	34	1
162	35	0
163	1	1
163	2	0
163	3	0
163	4	0
163	5	0
163	6	0
163	7	0
163	8	0
163	9	0
163	10	0
163	11	0
163	12	0
163	13	0
163	14	0
163	15	0
163	16	0
163	17	0
163	18	0
163	19	0
163	20	0
163	21	0
163	22	0
163	23	0
163	24	0
163	25	0
163	26	1
163	27	0
163	28	0
163	29	0
163	30	0
163	31	0
163	32	0
163	33	0
163	34	0
163	35	0
164	1	0
164	2	1
164	3	0
164	4	0
164	5	0
164	6	0
164	7	0
164	8	0
164	9	0
164	10	0
164	11	0
164	12	0
164	13	0
164	14	0
164	15	0
164	16	0
164	17	0
164	18	0
164	19	0
164	20	0
164	21	0
164	22	0
164	23	0
164	24	0
164	25	1
164	26	1
164	27	1
164	28	0
164	29	0
164	30	0
164	31	0
164	32	0
164	33	0
164	34	0
164	35	0
165	1	0
165	2	1
165	3	0
165	4	0
165	5	1
165	6	0
165	7	0
165	8	0
165	9	0
165	10	0
165	11	0
165	12	0
165	13	0
165	14	0
165	15	0
165	16	0
165	17	0
165	18	0
165	19	0
165	20	0
165	21	0
165	22	0
165	23	0
165	24	0
165	25	1
165	26	0
165	27	1
165	28	0
165	29	0
165	30	0
165	31	0
165	32	0
165	33	0
165	34	0
165	35	0
166	1	0
166	2	0
166	3	1
166	4	0
166	5	0
166	6	0
166	7	0
166	8	0
166	9	0
166	10	0
166	11	0
166	12	0
166	13	0
166	14	0
166	15	0
166	16	0
166	17	0
166	18	0
166	19	0
166	20	0
166	21	0
166	22	0
166	23	0
166	24	0
166	25	1
166	26	0
166	27	1
166	28	1
166	29	0
166	30	0
166	31	0
166	32	0
166	33	0
166	34	0
166	35	0
167	1	0
167	2	0
167	3	1
167	4	0
167	5	0
167	6	0
167	7	0
167	8	0
167	9	0
167	10	0
167	11	0
167	12	0
167	13	0
167	14	0
167	15	0
167	16	0
167	17	0
167	18	0
167	19	0
167	20	0
167	21	0
167	22	0
167	23	0
167	24	0
167	25	1
167	26	0
167	27	1
167	28	1
167	29	0
167	30	0
167	31	0
167	32	0
167	33	0
167	34	0
167	35	0
168	1	0
168	2	0
168	3	0
168	4	0
168	5	0
168	6	0
168	7	1
168	8	0
168	9	0
168	10	0
168	11	0
168	12	0
168	13	0
168	14	0
168	15	0
168	16	0
168	17	0
168	18	0
168	19	0
168	20	0
168	21	0
168	22	0
168	23	0
168	24	0
168	25	0
168	26	1
168	27	0
168	28	0
168	29	0
168	30	0
168	31	1
168	32	0
168	33	0
168	34	0
168	35	0
169	1	0
169	2	0
169	3	1
169	4	0
169	5	0
169	6	0
169	7	0
169	8	0
169	9	0
169	10	0
169	11	0
169	12	0
169	13	0
169	14	0
169	15	0
169	16	0
169	17	0
169	18	0
169	19	0
169	20	0
169	21	0
169	22	0
169	23	0
169	24	0
169	25	0
169	26	1
169	27	0
169	28	1
169	29	0
169	30	0
169	31	0
169	32	0
169	33	0
169	34	0
169	35	0
170	1	0
170	2	0
170	3	0
170	4	0
170	5	0
170	6	1
170	7	0
170	8	0
170	9	0
170	10	0
170	11	0
170	12	0
170	13	0
170	14	0
170	15	0
170	16	0
170	17	0
170	18	0
170	19	0
170	20	0
170	21	0
170	22	0
170	23	0
170	24	0
170	25	0
170	26	1
170	27	0
170	28	0
170	29	0
170	30	0
170	31	0
170	32	0
170	33	0
170	34	0
170	35	0
171	1	1
171	2	0
171	3	0
171	4	0
171	5	0
171	6	0
171	7	0
171	8	1
171	9	0
171	10	1
171	11	0
171	12	1
171	13	0
171	14	0
171	15	1
171	16	0
171	17	0
171	18	0
171	19	0
171	20	0
171	21	0
171	22	0
171	23	0
171	24	0
171	25	0
171	26	0
171	27	0
171	28	0
171	29	1
171	30	0
171	31	0
171	32	0
171	33	0
171	34	0
171	35	1
172	1	0
172	2	1
172	3	0
172	4	0
172	5	0
172	6	0
172	7	0
172	8	0
172	9	0
172	10	0
172	11	0
172	12	0
172	13	0
172	14	0
172	15	0
172	16	0
172	17	0
172	18	0
172	19	0
172	20	0
172	21	0
172	22	0
172	23	0
172	24	0
172	25	1
172	26	1
172	27	0
172	28	0
172	29	0
172	30	0
172	31	0
172	32	0
172	33	0
172	34	0
172	35	0
173	1	0
173	2	0
173	3	1
173	4	0
173	5	0
173	6	0
173	7	0
173	8	0
173	9	0
173	10	0
173	11	0
173	12	0
173	13	0
173	14	0
173	15	0
173	16	0
173	17	0
173	18	0
173	19	0
173	20	0
173	21	0
173	22	0
173	23	0
173	24	0
173	25	1
173	26	1
173	27	0
173	28	1
173	29	0
173	30	0
173	31	0
173	32	0
173	33	0
173	34	0
173	35	0
174	1	0
174	2	0
174	3	1
174	4	0
174	5	0
174	6	0
174	7	0
174	8	0
174	9	0
174	10	0
174	11	0
174	12	0
174	13	0
174	14	0
174	15	0
174	16	0
174	17	0
174	18	0
174	19	0
174	20	0
174	21	0
174	22	0
174	23	0
174	24	0
174	25	0
174	26	0
174	27	0
174	28	1
174	29	0
174	30	0
174	31	0
174	32	0
174	33	0
174	34	0
174	35	0
175	1	0
175	2	0
175	3	0
175	4	0
175	5	0
175	6	0
175	7	0
175	8	0
175	9	0
175	10	1
175	11	0
175	12	0
175	13	0
175	14	0
175	15	0
175	16	0
175	17	0
175	18	0
175	19	0
175	20	0
175	21	0
175	22	0
175	23	0
175	24	0
175	25	0
175	26	0
175	27	0
175	28	0
175	29	1
175	30	0
175	31	0
175	32	0
175	33	1
175	34	0
175	35	0
176	1	1
176	2	0
176	3	0
176	4	0
176	5	0
176	6	0
176	7	0
176	8	0
176	9	0
176	10	0
176	11	1
176	12	0
176	13	0
176	14	0
176	15	0
176	16	0
176	17	0
176	18	0
176	19	0
176	20	0
176	21	0
176	22	0
176	23	0
176	24	0
176	25	0
176	26	0
176	27	0
176	28	0
176	29	0
176	30	0
176	31	0
176	32	0
176	33	0
176	34	1
176	35	0
177	1	1
177	2	0
177	3	0
177	4	0
177	5	0
177	6	0
177	7	0
177	8	0
177	9	0
177	10	0
177	11	1
177	12	0
177	13	0
177	14	0
177	15	0
177	16	0
177	17	0
177	18	0
177	19	0
177	20	0
177	21	0
177	22	0
177	23	0
177	24	0
177	25	0
177	26	0
177	27	0
177	28	0
177	29	1
177	30	0
177	31	0
177	32	0
177	33	0
177	34	1
177	35	0
178	1	1
178	2	0
178	3	0
178	4	0
178	5	0
178	6	0
178	7	0
178	8	0
178	9	0
178	10	0
178	11	1
178	12	1
178	13	0
178	14	0
178	15	0
178	16	0
178	17	0
178	18	0
178	19	0
178	20	0
178	21	0
178	22	0
178	23	0
178	24	0
178	25	0
178	26	0
178	27	0
178	28	0
178	29	0
178	30	0
178	31	0
178	32	0
178	33	0
178	34	1
178	35	0
179	1	1
179	2	0
179	3	0
179	4	0
179	5	0
179	6	0
179	7	0
179	8	0
179	9	0
179	10	0
179	11	0
179	12	0
179	13	0
179	14	0
179	15	0
179	16	0
179	17	0
179	18	0
179	19	1
179	20	0
179	21	0
179	22	0
179	23	0
179	24	0
179	25	0
179	26	0
179	27	0
179	28	0
179	29	0
179	30	0
179	31	0
179	32	0
179	33	0
179	34	1
179	35	0
180	1	1
180	2	0
180	3	0
180	4	0
180	5	0
180	6	0
180	7	0
180	8	0
180	9	0
180	10	0
180	11	1
180	12	1
180	13	0
180	14	0
180	15	0
180	16	0
180	17	0
180	18	0
180	19	0
180	20	0
180	21	0
180	22	0
180	23	0
180	24	0
180	25	0
180	26	0
180	27	0
180	28	0
180	29	0
180	30	0
180	31	0
180	32	0
180	33	0
180	34	1
180	35	0
181	1	0
181	2	0
181	3	1
181	4	0
181	5	0
181	6	0
181	7	0
181	8	0
181	9	0
181	10	0
181	11	0
181	12	0
181	13	0
181	14	0
181	15	0
181	16	0
181	17	0
181	18	0
181	19	0
181	20	0
181	21	0
181	22	0
181	23	0
181	24	0
181	25	0
181	26	1
181	27	1
181	28	1
181	29	0
181	30	0
181	31	0
181	32	0
181	33	0
181	34	0
181	35	0
182	1	0
182	2	0
182	3	0
182	4	0
182	5	0
182	6	0
182	7	0
182	8	0
182	9	0
182	10	1
182	11	0
182	12	0
182	13	0
182	14	0
182	15	0
182	16	0
182	17	0
182	18	0
182	19	0
182	20	0
182	21	0
182	22	0
182	23	0
182	24	0
182	25	0
182	26	0
182	27	0
182	28	0
182	29	0
182	30	0
182	31	0
182	32	0
182	33	1
182	34	0
182	35	0
184	1	0
184	2	0
184	3	0
184	4	0
184	5	0
184	6	0
184	7	0
184	8	0
184	9	0
184	10	1
184	11	0
184	12	0
184	13	0
184	14	0
184	15	0
184	16	0
184	17	0
184	18	0
184	19	0
184	20	0
184	21	0
184	22	0
184	23	0
184	24	0
184	25	0
184	26	0
184	27	0
184	28	0
184	29	0
184	30	0
184	31	0
184	32	0
184	33	0
184	34	1
184	35	0
185	1	0
185	2	0
185	3	1
185	4	0
185	5	0
185	6	0
185	7	0
185	8	0
185	9	0
185	10	0
185	11	0
185	12	0
185	13	0
185	14	0
185	15	0
185	16	0
185	17	0
185	18	0
185	19	0
185	20	0
185	21	0
185	22	0
185	23	0
185	24	0
185	25	0
185	26	0
185	27	1
185	28	1
185	29	0
185	30	0
185	31	0
185	32	0
185	33	0
185	34	0
185	35	0
186	1	1
186	2	0
186	3	0
186	4	0
186	5	0
186	6	0
186	7	0
186	8	0
186	9	0
186	10	0
186	11	0
186	12	0
186	13	1
186	14	0
186	15	0
186	16	0
186	17	0
186	18	0
186	19	0
186	20	0
186	21	0
186	22	0
186	23	0
186	24	0
186	25	0
186	26	0
186	27	0
186	28	0
186	29	0
186	30	0
186	31	0
186	32	0
186	33	0
186	34	0
186	35	0
187	1	1
187	2	0
187	3	0
187	4	0
187	5	0
187	6	0
187	7	0
187	8	0
187	9	0
187	10	0
187	11	0
187	12	0
187	13	0
187	14	0
187	15	0
187	16	1
187	17	0
187	18	0
187	19	0
187	20	0
187	21	0
187	22	1
187	23	0
187	24	1
187	25	0
187	26	0
187	27	0
187	28	0
187	29	1
187	30	0
187	31	0
187	32	0
187	33	0
187	34	0
187	35	0
188	1	0
188	2	1
188	3	0
188	4	0
188	5	0
188	6	0
188	7	0
188	8	0
188	9	0
188	10	0
188	11	0
188	12	0
188	13	0
188	14	0
188	15	0
188	16	0
188	17	0
188	18	0
188	19	0
188	20	0
188	21	0
188	22	0
188	23	0
188	24	0
188	25	1
188	26	0
188	27	0
188	28	0
188	29	0
188	30	0
188	31	0
188	32	0
188	33	0
188	34	0
188	35	0
189	1	1
189	2	0
189	3	0
189	4	0
189	5	0
189	6	0
189	7	0
189	8	0
189	9	0
189	10	0
189	11	0
189	12	0
189	13	0
189	14	0
189	15	0
189	16	1
189	17	0
189	18	0
189	19	0
189	20	0
189	21	0
189	22	0
189	23	0
189	24	0
189	25	0
189	26	0
189	27	0
189	28	0
189	29	0
189	30	0
189	31	0
189	32	0
189	33	0
189	34	1
189	35	0
190	1	1
190	2	0
190	3	0
190	4	0
190	5	0
190	6	0
190	7	0
190	8	0
190	9	0
190	10	0
190	11	0
190	12	0
190	13	0
190	14	0
190	15	0
190	16	0
190	17	0
190	18	0
190	19	0
190	20	0
190	21	0
190	22	1
190	23	0
190	24	0
190	25	0
190	26	0
190	27	0
190	28	0
190	29	1
190	30	0
190	31	0
190	32	0
190	33	0
190	34	0
190	35	0
191	1	0
191	2	1
191	3	0
191	4	0
191	5	0
191	6	0
191	7	0
191	8	0
191	9	0
191	10	0
191	11	0
191	12	0
191	13	0
191	14	0
191	15	0
191	16	0
191	17	0
191	18	0
191	19	0
191	20	0
191	21	0
191	22	0
191	23	0
191	24	0
191	25	1
191	26	1
191	27	1
191	28	0
191	29	0
191	30	0
191	31	0
191	32	0
191	33	0
191	34	0
191	35	0
192	1	0
192	2	0
192	3	0
192	4	0
192	5	1
192	6	0
192	7	0
192	8	0
192	9	0
192	10	0
192	11	1
192	12	1
192	13	0
192	14	0
192	15	0
192	16	0
192	17	0
192	18	0
192	19	0
192	20	0
192	21	0
192	22	0
192	23	0
192	24	0
192	25	0
192	26	0
192	27	1
192	28	0
192	29	0
192	30	0
192	31	0
192	32	0
192	33	0
192	34	0
192	35	0
193	1	1
193	2	0
193	3	0
193	4	0
193	5	0
193	6	0
193	7	0
193	8	0
193	9	0
193	10	0
193	11	0
193	12	0
193	13	0
193	14	0
193	15	0
193	16	1
193	17	0
193	18	0
193	19	0
193	20	1
193	21	0
193	22	0
193	23	0
193	24	0
193	25	0
193	26	0
193	27	0
193	28	0
193	29	0
193	30	1
193	31	0
193	32	0
193	33	0
193	34	0
193	35	0
194	1	0
194	2	0
194	3	0
194	4	0
194	5	0
194	6	0
194	7	0
194	8	0
194	9	1
194	10	0
194	11	1
194	12	0
194	13	0
194	14	0
194	15	0
194	16	0
194	17	0
194	18	0
194	19	0
194	20	0
194	21	0
194	22	0
194	23	0
194	24	0
194	25	0
194	26	1
194	27	0
194	28	0
194	29	0
194	30	0
194	31	0
194	32	1
194	33	0
194	34	0
194	35	0
195	1	1
195	2	0
195	3	0
195	4	0
195	5	0
195	6	0
195	7	0
195	8	0
195	9	0
195	10	0
195	11	1
195	12	0
195	13	0
195	14	0
195	15	0
195	16	0
195	17	0
195	18	0
195	19	0
195	20	0
195	21	0
195	22	0
195	23	0
195	24	0
195	25	0
195	26	0
195	27	0
195	28	0
195	29	0
195	30	0
195	31	0
195	32	0
195	33	0
195	34	1
195	35	0
196	1	1
196	2	0
196	3	0
196	4	0
196	5	0
196	6	0
196	7	0
196	8	0
196	9	0
196	10	0
196	11	1
196	12	0
196	13	0
196	14	0
196	15	0
196	16	0
196	17	0
196	18	0
196	19	0
196	20	0
196	21	0
196	22	1
196	23	1
196	24	0
196	25	0
196	26	0
196	27	0
196	28	0
196	29	1
196	30	0
196	31	0
196	32	0
196	33	0
196	34	0
196	35	0
197	1	1
197	2	0
197	3	0
197	4	0
197	5	0
197	6	0
197	7	0
197	8	0
197	9	0
197	10	0
197	11	0
197	12	0
197	13	0
197	14	0
197	15	0
197	16	0
197	17	0
197	18	0
197	19	0
197	20	0
197	21	0
197	22	1
197	23	1
197	24	0
197	25	0
197	26	0
197	27	0
197	28	0
197	29	1
197	30	0
197	31	0
197	32	0
197	33	0
197	34	0
197	35	0
198	1	1
198	2	0
198	3	0
198	4	0
198	5	0
198	6	0
198	7	0
198	8	0
198	9	0
198	10	0
198	11	1
198	12	1
198	13	0
198	14	0
198	15	0
198	16	0
198	17	0
198	18	0
198	19	0
198	20	0
198	21	0
198	22	0
198	23	0
198	24	0
198	25	0
198	26	0
198	27	0
198	28	0
198	29	1
198	30	1
198	31	0
198	32	0
198	33	0
198	34	0
198	35	0
199	1	1
199	2	0
199	3	0
199	4	0
199	5	0
199	6	0
199	7	0
199	8	0
199	9	0
199	10	0
199	11	0
199	12	0
199	13	0
199	14	0
199	15	0
199	16	0
199	17	0
199	18	0
199	19	1
199	20	0
199	21	0
199	22	0
199	23	0
199	24	0
199	25	0
199	26	0
199	27	0
199	28	0
199	29	0
199	30	0
199	31	0
199	32	0
199	33	0
199	34	0
199	35	0
200	1	0
200	2	1
200	3	1
200	4	0
200	5	0
200	6	0
200	7	0
200	8	0
200	9	0
200	10	0
200	11	0
200	12	0
200	13	0
200	14	0
200	15	0
200	16	0
200	17	0
200	18	0
200	19	0
200	20	0
200	21	0
200	22	0
200	23	0
200	24	0
200	25	1
200	26	0
200	27	0
200	28	1
200	29	0
200	30	0
200	31	0
200	32	0
200	33	0
200	34	0
200	35	0
201	1	0
201	2	0
201	3	0
201	4	0
201	5	0
201	6	1
201	7	0
201	8	0
201	9	0
201	10	0
201	11	0
201	12	0
201	13	0
201	14	0
201	15	1
201	16	0
201	17	0
201	18	0
201	19	0
201	20	0
201	21	0
201	22	0
201	23	0
201	24	0
201	25	0
201	26	1
201	27	0
201	28	0
201	29	0
201	30	0
201	31	0
201	32	1
201	33	0
201	34	0
201	35	0
202	1	0
202	2	0
202	3	0
202	4	0
202	5	1
202	6	0
202	7	0
202	8	0
202	9	0
202	10	0
202	11	0
202	12	0
202	13	0
202	14	0
202	15	1
202	16	0
202	17	0
202	18	0
202	19	0
202	20	0
202	21	0
202	22	0
202	23	0
202	24	0
202	25	0
202	26	0
202	27	1
202	28	0
202	29	0
202	30	0
202	31	0
202	32	0
202	33	0
202	34	0
202	35	0
203	1	0
203	2	0
203	3	0
203	4	0
203	5	0
203	6	0
203	7	0
203	8	0
203	9	0
203	10	1
203	11	0
203	12	0
203	13	0
203	14	0
203	15	0
203	16	0
203	17	0
203	18	0
203	19	0
203	20	0
203	21	0
203	22	0
203	23	0
203	24	0
203	25	0
203	26	1
203	27	0
203	28	0
203	29	1
203	30	0
203	31	0
203	32	1
203	33	1
203	34	0
203	35	0
204	1	0
204	2	0
204	3	1
204	4	0
204	5	0
204	6	0
204	7	0
204	8	0
204	9	0
204	10	0
204	11	0
204	12	0
204	13	0
204	14	0
204	15	0
204	16	0
204	17	0
204	18	0
204	19	0
204	20	0
204	21	0
204	22	0
204	23	0
204	24	0
204	25	0
204	26	1
204	27	1
204	28	1
204	29	0
204	30	0
204	31	0
204	32	0
204	33	0
204	34	0
204	35	0
205	1	1
205	2	0
205	3	0
205	4	0
205	5	0
205	6	0
205	7	0
205	8	1
205	9	0
205	10	0
205	11	0
205	12	1
205	13	0
205	14	0
205	15	1
205	16	0
205	17	0
205	18	0
205	19	0
205	20	0
205	21	0
205	22	0
205	23	0
205	24	0
205	25	0
205	26	1
205	27	0
205	28	0
205	29	1
205	30	0
205	31	1
205	32	1
205	33	0
205	34	0
205	35	1
206	1	1
206	2	0
206	3	0
206	4	0
206	5	0
206	6	0
206	7	0
206	8	0
206	9	0
206	10	0
206	11	0
206	12	0
206	13	0
206	14	0
206	15	0
206	16	0
206	17	1
206	18	0
206	19	0
206	20	0
206	21	0
206	22	0
206	23	0
206	24	0
206	25	0
206	26	0
206	27	0
206	28	0
206	29	0
206	30	0
206	31	0
206	32	0
206	33	0
206	34	0
206	35	0
207	1	1
207	2	0
207	3	0
207	4	0
207	5	0
207	6	0
207	7	0
207	8	0
207	9	0
207	10	0
207	11	0
207	12	0
207	13	0
207	14	1
207	15	0
207	16	0
207	17	0
207	18	0
207	19	0
207	20	0
207	21	0
207	22	0
207	23	0
207	24	0
207	25	0
207	26	0
207	27	0
207	28	0
207	29	0
207	30	1
207	31	0
207	32	0
207	33	0
207	34	0
207	35	0
208	1	1
208	2	0
208	3	0
208	4	0
208	5	0
208	6	0
208	7	0
208	8	0
208	9	0
208	10	0
208	11	0
208	12	0
208	13	1
208	14	0
208	15	0
208	16	0
208	17	0
208	18	0
208	19	0
208	20	0
208	21	0
208	22	0
208	23	0
208	24	0
208	25	0
208	26	0
208	27	0
208	28	0
208	29	0
208	30	1
208	31	0
208	32	0
208	33	0
208	34	0
208	35	0
209	1	0
209	2	1
209	3	0
209	4	0
209	5	0
209	6	0
209	7	0
209	8	0
209	9	0
209	10	0
209	11	0
209	12	0
209	13	0
209	14	0
209	15	0
209	16	0
209	17	0
209	18	0
209	19	0
209	20	0
209	21	0
209	22	1
209	23	0
209	24	0
209	25	1
209	26	0
209	27	0
209	28	0
209	29	0
209	30	0
209	31	0
209	32	0
209	33	0
209	34	0
209	35	0
210	1	0
210	2	0
210	3	0
210	4	0
210	5	0
210	6	0
210	7	1
210	8	0
210	9	0
210	10	0
210	11	0
210	12	0
210	13	0
210	14	0
210	15	0
210	16	0
210	17	0
210	18	0
210	19	0
210	20	0
210	21	0
210	22	0
210	23	0
210	24	0
210	25	0
210	26	1
210	27	0
210	28	0
210	29	0
210	30	0
210	31	1
210	32	0
210	33	0
210	34	0
210	35	0
211	1	0
211	2	0
211	3	0
211	4	0
211	5	0
211	6	0
211	7	0
211	8	0
211	9	0
211	10	1
211	11	0
211	12	0
211	13	1
211	14	0
211	15	0
211	16	0
211	17	0
211	18	0
211	19	0
211	20	0
211	21	0
211	22	0
211	23	0
211	24	0
211	25	0
211	26	0
211	27	1
211	28	0
211	29	0
211	30	0
211	31	0
211	32	0
211	33	1
211	34	1
211	35	0
212	1	0
212	2	0
212	3	0
212	4	0
212	5	1
212	6	0
212	7	0
212	8	0
212	9	0
212	10	0
212	11	0
212	12	0
212	13	0
212	14	0
212	15	1
212	16	0
212	17	0
212	18	0
212	19	0
212	20	0
212	21	0
212	22	0
212	23	0
212	24	0
212	25	0
212	26	0
212	27	1
212	28	0
212	29	0
212	30	0
212	31	0
212	32	0
212	33	0
212	34	0
212	35	0
213	1	1
213	2	0
213	3	1
213	4	0
213	5	0
213	6	0
213	7	0
213	8	0
213	9	0
213	10	0
213	11	0
213	12	0
213	13	1
213	14	1
213	15	0
213	16	0
213	17	0
213	18	0
213	19	0
213	20	0
213	21	0
213	22	0
213	23	0
213	24	0
213	25	0
213	26	0
213	27	0
213	28	1
213	29	0
213	30	1
213	31	0
213	32	0
213	33	0
213	34	0
213	35	0
214	1	1
214	2	0
214	3	0
214	4	0
214	5	0
214	6	0
214	7	0
214	8	0
214	9	0
214	10	0
214	11	1
214	12	0
214	13	0
214	14	0
214	15	0
214	16	0
214	17	0
214	18	0
214	19	0
214	20	0
214	21	0
214	22	0
214	23	0
214	24	0
214	25	0
214	26	0
214	27	0
214	28	0
214	29	0
214	30	0
214	31	0
214	32	0
214	33	0
214	34	1
214	35	0
215	1	0
215	2	0
215	3	0
215	4	0
215	5	1
215	6	0
215	7	0
215	8	0
215	9	0
215	10	0
215	11	0
215	12	0
215	13	0
215	14	0
215	15	1
215	16	0
215	17	0
215	18	0
215	19	0
215	20	0
215	21	0
215	22	0
215	23	0
215	24	0
215	25	0
215	26	0
215	27	1
215	28	0
215	29	0
215	30	0
215	31	0
215	32	0
215	33	0
215	34	0
215	35	0
216	1	1
216	2	0
216	3	0
216	4	0
216	5	0
216	6	0
216	7	0
216	8	0
216	9	0
216	10	0
216	11	0
216	12	0
216	13	0
216	14	0
216	15	0
216	16	1
216	17	0
216	18	0
216	19	0
216	20	0
216	21	0
216	22	0
216	23	0
216	24	0
216	25	0
216	26	1
216	27	0
216	28	0
216	29	0
216	30	0
216	31	0
216	32	0
216	33	0
216	34	0
216	35	0
217	1	0
217	2	0
217	3	1
217	4	0
217	5	0
217	6	0
217	7	0
217	8	0
217	9	0
217	10	0
217	11	0
217	12	0
217	13	0
217	14	0
217	15	0
217	16	0
217	17	0
217	18	0
217	19	0
217	20	0
217	21	0
217	22	0
217	23	0
217	24	0
217	25	0
217	26	1
217	27	1
217	28	1
217	29	0
217	30	0
217	31	1
217	32	0
217	33	0
217	34	0
217	35	0
218	1	1
218	2	0
218	3	0
218	4	0
218	5	0
218	6	0
218	7	0
218	8	0
218	9	0
218	10	0
218	11	1
218	12	0
218	13	0
218	14	0
218	15	0
218	16	0
218	17	0
218	18	0
218	19	0
218	20	0
218	21	0
218	22	0
218	23	0
218	24	0
218	25	0
218	26	0
218	27	0
218	28	0
218	29	0
218	30	0
218	31	0
218	32	1
218	33	0
218	34	1
218	35	0
219	1	0
219	2	0
219	3	0
219	4	1
219	5	0
219	6	0
219	7	0
219	8	0
219	9	0
219	10	0
219	11	0
219	12	0
219	13	0
219	14	0
219	15	0
219	16	0
219	17	0
219	18	0
219	19	0
219	20	0
219	21	0
219	22	0
219	23	0
219	24	0
219	25	1
219	26	1
219	27	0
219	28	0
219	29	0
219	30	0
219	31	0
219	32	0
219	33	0
219	34	0
219	35	0
220	1	0
220	2	0
220	3	1
220	4	0
220	5	0
220	6	0
220	7	0
220	8	0
220	9	0
220	10	0
220	11	0
220	12	0
220	13	0
220	14	0
220	15	0
220	16	0
220	17	0
220	18	0
220	19	0
220	20	0
220	21	0
220	22	0
220	23	0
220	24	0
220	25	0
220	26	1
220	27	1
220	28	1
220	29	0
220	30	0
220	31	0
220	32	0
220	33	0
220	34	0
220	35	0
221	1	1
221	2	0
221	3	0
221	4	0
221	5	0
221	6	0
221	7	0
221	8	0
221	9	0
221	10	0
221	11	0
221	12	0
221	13	1
221	14	0
221	15	0
221	16	0
221	17	1
221	18	0
221	19	0
221	20	0
221	21	0
221	22	1
221	23	0
221	24	0
221	25	0
221	26	0
221	27	0
221	28	0
221	29	1
221	30	0
221	31	0
221	32	0
221	33	0
221	34	0
221	35	0
222	1	1
222	2	0
222	3	0
222	4	0
222	5	0
222	6	0
222	7	0
222	8	0
222	9	0
222	10	0
222	11	0
222	12	1
222	13	1
222	14	0
222	15	0
222	16	1
222	17	0
222	18	0
222	19	0
222	20	0
222	21	0
222	22	1
222	23	0
222	24	0
222	25	0
222	26	0
222	27	0
222	28	0
222	29	1
222	30	0
222	31	0
222	32	0
222	33	0
222	34	1
222	35	0
223	1	0
223	2	0
223	3	1
223	4	0
223	5	0
223	6	0
223	7	0
223	8	0
223	9	0
223	10	0
223	11	0
223	12	0
223	13	0
223	14	0
223	15	0
223	16	0
223	17	0
223	18	0
223	19	0
223	20	0
223	21	0
223	22	0
223	23	0
223	24	0
223	25	0
223	26	1
223	27	0
223	28	1
223	29	0
223	30	0
223	31	0
223	32	0
223	33	0
223	34	0
223	35	0
224	1	1
224	2	0
224	3	0
224	4	0
224	5	0
224	6	0
224	7	0
224	8	0
224	9	0
224	10	0
224	11	0
224	12	0
224	13	0
224	14	0
224	15	0
224	16	0
224	17	0
224	18	1
224	19	0
224	20	0
224	21	0
224	22	0
224	23	0
224	24	0
224	25	0
224	26	0
224	27	0
224	28	0
224	29	0
224	30	0
224	31	0
224	32	0
224	33	0
224	34	1
224	35	0
225	1	1
225	2	0
225	3	0
225	4	0
225	5	0
225	6	0
225	7	0
225	8	0
225	9	0
225	10	0
225	11	0
225	12	0
225	13	0
225	14	0
225	15	0
225	16	0
225	17	1
225	18	0
225	19	0
225	20	0
225	21	0
225	22	0
225	23	0
225	24	0
225	25	0
225	26	0
225	27	0
225	28	0
225	29	0
225	30	0
225	31	0
225	32	0
225	33	0
225	34	0
225	35	0
226	1	1
226	2	0
226	3	0
226	4	0
226	5	0
226	6	0
226	7	0
226	8	1
226	9	1
226	10	0
226	11	0
226	12	1
226	13	0
226	14	0
226	15	1
226	16	0
226	17	0
226	18	0
226	19	0
226	20	0
226	21	0
226	22	0
226	23	0
226	24	0
226	25	0
226	26	1
226	27	0
226	28	0
226	29	1
226	30	0
226	31	1
226	32	1
226	33	0
226	34	0
226	35	1
227	1	0
227	2	0
227	3	1
227	4	0
227	5	0
227	6	0
227	7	0
227	8	0
227	9	0
227	10	0
227	11	0
227	12	1
227	13	0
227	14	0
227	15	0
227	16	0
227	17	0
227	18	0
227	19	0
227	20	0
227	21	0
227	22	0
227	23	0
227	24	0
227	25	0
227	26	1
227	27	1
227	28	1
227	29	0
227	30	0
227	31	0
227	32	0
227	33	0
227	34	0
227	35	0
229	1	0
229	2	0
229	3	0
229	4	0
229	5	0
229	6	0
229	7	0
229	8	1
229	9	0
229	10	1
229	11	0
229	12	1
229	13	0
229	14	0
229	15	0
229	16	0
229	17	0
229	18	0
229	19	0
229	20	0
229	21	0
229	22	0
229	23	0
229	24	0
229	25	0
229	26	0
229	27	0
229	28	0
229	29	0
229	30	0
229	31	0
229	32	0
229	33	1
229	34	0
229	35	1
230	1	0
230	2	0
230	3	1
230	4	0
230	5	0
230	6	0
230	7	0
230	8	0
230	9	0
230	10	0
230	11	0
230	12	0
230	13	0
230	14	0
230	15	0
230	16	0
230	17	0
230	18	0
230	19	0
230	20	0
230	21	0
230	22	0
230	23	0
230	24	0
230	25	0
230	26	1
230	27	1
230	28	1
230	29	0
230	30	0
230	31	0
230	32	0
230	33	0
230	34	0
230	35	0
231	1	0
231	2	0
231	3	0
231	4	0
231	5	0
231	6	0
231	7	0
231	8	0
231	9	1
231	10	1
231	11	0
231	12	0
231	13	1
231	14	0
231	15	0
231	16	0
231	17	0
231	18	0
231	19	0
231	20	0
231	21	0
231	22	0
231	23	0
231	24	0
231	25	0
231	26	1
231	27	0
231	28	0
231	29	0
231	30	0
231	31	0
231	32	0
231	33	0
231	34	1
231	35	0
232	1	1
232	2	0
232	3	0
232	4	0
232	5	0
232	6	0
232	7	0
232	8	0
232	9	0
232	10	0
232	11	0
232	12	0
232	13	1
232	14	0
232	15	0
232	16	1
232	17	0
232	18	0
232	19	0
232	20	0
232	21	0
232	22	1
232	23	0
232	24	0
232	25	0
232	26	0
232	27	0
232	28	0
232	29	1
232	30	0
232	31	0
232	32	0
232	33	0
232	34	1
232	35	0
233	1	0
233	2	0
233	3	0
233	4	0
233	5	0
233	6	0
233	7	1
233	8	0
233	9	0
233	10	0
233	11	0
233	12	0
233	13	0
233	14	0
233	15	0
233	16	0
233	17	0
233	18	0
233	19	0
233	20	0
233	21	0
233	22	0
233	23	0
233	24	0
233	25	0
233	26	1
233	27	0
233	28	0
233	29	0
233	30	0
233	31	1
233	32	0
233	33	0
233	34	0
233	35	0
234	1	0
234	2	0
234	3	0
234	4	0
234	5	0
234	6	0
234	7	0
234	8	0
234	9	0
234	10	1
234	11	0
234	12	0
234	13	0
234	14	0
234	15	0
234	16	0
234	17	0
234	18	0
234	19	0
234	20	0
234	21	0
234	22	0
234	23	0
234	24	0
234	25	0
234	26	0
234	27	0
234	28	0
234	29	0
234	30	0
234	31	0
234	32	0
234	33	0
234	34	1
234	35	0
235	1	1
235	2	0
235	3	0
235	4	0
235	5	0
235	6	0
235	7	0
235	8	0
235	9	0
235	10	1
235	11	0
235	12	0
235	13	0
235	14	0
235	15	0
235	16	1
235	17	0
235	18	0
235	19	0
235	20	0
235	21	0
235	22	0
235	23	0
235	24	0
235	25	0
235	26	0
235	27	0
235	28	0
235	29	1
235	30	0
235	31	0
235	32	0
235	33	0
235	34	1
235	35	0
236	1	0
236	2	0
236	3	0
236	4	0
236	5	0
236	6	0
236	7	0
236	8	1
236	9	0
236	10	0
236	11	0
236	12	0
236	13	1
236	14	0
236	15	0
236	16	0
236	17	0
236	18	0
236	19	0
236	20	0
236	21	0
236	22	0
236	23	0
236	24	0
236	25	0
236	26	1
236	27	0
236	28	0
236	29	0
236	30	0
236	31	0
236	32	0
236	33	0
236	34	0
236	35	1
237	1	1
237	2	0
237	3	0
237	4	0
237	5	0
237	6	0
237	7	0
237	8	0
237	9	0
237	10	0
237	11	0
237	12	1
237	13	1
237	14	0
237	15	0
237	16	1
237	17	0
237	18	0
237	19	0
237	20	0
237	21	0
237	22	0
237	23	0
237	24	0
237	25	0
237	26	0
237	27	0
237	28	0
237	29	0
237	30	0
237	31	0
237	32	0
237	33	0
237	34	1
237	35	0
238	1	0
238	2	0
238	3	0
238	4	0
238	5	0
238	6	0
238	7	0
238	8	1
238	9	1
238	10	0
238	11	0
238	12	0
238	13	0
238	14	0
238	15	0
238	16	0
238	17	0
238	18	0
238	19	0
238	20	0
238	21	0
238	22	0
238	23	0
238	24	0
238	25	0
238	26	1
238	27	0
238	28	0
238	29	0
238	30	0
238	31	0
238	32	1
238	33	0
238	34	0
238	35	1
239	1	0
239	2	0
239	3	0
239	4	0
239	5	0
239	6	0
239	7	0
239	8	1
239	9	1
239	10	0
239	11	0
239	12	0
239	13	1
239	14	0
239	15	0
239	16	0
239	17	0
239	18	0
239	19	0
239	20	0
239	21	0
239	22	0
239	23	0
239	24	0
239	25	0
239	26	1
239	27	0
239	28	0
239	29	0
239	30	0
239	31	0
239	32	0
239	33	0
239	34	0
239	35	1
240	1	1
240	2	0
240	3	0
240	4	0
240	5	0
240	6	0
240	7	0
240	8	0
240	9	0
240	10	0
240	11	0
240	12	0
240	13	0
240	14	1
240	15	0
240	16	0
240	17	0
240	18	0
240	19	0
240	20	0
240	21	0
240	22	0
240	23	0
240	24	0
240	25	0
240	26	0
240	27	0
240	28	0
240	29	0
240	30	1
240	31	0
240	32	0
240	33	0
240	34	0
240	35	0
241	1	0
241	2	0
241	3	0
241	4	0
241	5	0
241	6	0
241	7	0
241	8	0
241	9	1
241	10	0
241	11	0
241	12	0
241	13	0
241	14	0
241	15	0
241	16	0
241	17	0
241	18	0
241	19	0
241	20	0
241	21	0
241	22	0
241	23	0
241	24	0
241	25	0
241	26	1
241	27	0
241	28	0
241	29	0
241	30	0
241	31	0
241	32	0
241	33	0
241	34	0
241	35	0
242	1	1
242	2	0
242	3	0
242	4	0
242	5	0
242	6	0
242	7	0
242	8	0
242	9	1
242	10	0
242	11	0
242	12	0
242	13	1
242	14	0
242	15	0
242	16	1
242	17	0
242	18	0
242	19	0
242	20	0
242	21	0
242	22	0
242	23	0
242	24	0
242	25	0
242	26	1
242	27	0
242	28	0
242	29	0
242	30	0
242	31	0
242	32	0
242	33	0
242	34	0
242	35	0
243	1	1
243	2	0
243	3	0
243	4	0
243	5	0
243	6	0
243	7	0
243	8	0
243	9	0
243	10	0
243	11	0
243	12	0
243	13	0
243	14	0
243	15	1
243	16	0
243	17	1
243	18	0
243	19	0
243	20	0
243	21	0
243	22	0
243	23	0
243	24	0
243	25	0
243	26	0
243	27	0
243	28	0
243	29	0
243	30	0
243	31	0
243	32	0
243	33	0
243	34	0
243	35	0
244	1	0
244	2	0
244	3	0
244	4	1
244	5	0
244	6	0
244	7	0
244	8	0
244	9	0
244	10	0
244	11	0
244	12	0
244	13	0
244	14	0
244	15	0
244	16	0
244	17	0
244	18	0
244	19	0
244	20	0
244	21	0
244	22	0
244	23	0
244	24	0
244	25	0
244	26	1
244	27	0
244	28	0
244	29	0
244	30	0
244	31	0
244	32	1
244	33	0
244	34	0
244	35	0
245	1	0
245	2	0
245	3	0
245	4	0
245	5	0
245	6	0
245	7	0
245	8	0
245	9	0
245	10	1
245	11	0
245	12	0
245	13	0
245	14	0
245	15	0
245	16	0
245	17	0
245	18	0
245	19	0
245	20	0
245	21	0
245	22	0
245	23	0
245	24	0
245	25	0
245	26	1
245	27	0
245	28	0
245	29	1
245	30	0
245	31	0
245	32	1
245	33	0
245	34	0
245	35	0
246	1	1
246	2	0
246	3	0
246	4	0
246	5	0
246	6	0
246	7	0
246	8	0
246	9	0
246	10	0
246	11	0
246	12	0
246	13	0
246	14	1
246	15	0
246	16	0
246	17	0
246	18	0
246	19	0
246	20	0
246	21	0
246	22	0
246	23	0
246	24	0
246	25	0
246	26	1
246	27	0
246	28	0
246	29	0
246	30	1
246	31	0
246	32	0
246	33	0
246	34	0
246	35	0
247	1	1
247	2	0
247	3	0
247	4	0
247	5	0
247	6	0
247	7	0
247	8	0
247	9	0
247	10	0
247	11	0
247	12	0
247	13	0
247	14	0
247	15	0
247	16	0
247	17	1
247	18	0
247	19	0
247	20	0
247	21	0
247	22	0
247	23	0
247	24	0
247	25	0
247	26	0
247	27	0
247	28	0
247	29	0
247	30	0
247	31	0
247	32	0
247	33	0
247	34	0
247	35	0
248	1	1
248	2	0
248	3	0
248	4	0
248	5	0
248	6	0
248	7	0
248	8	0
248	9	0
248	10	0
248	11	0
248	12	0
248	13	0
248	14	1
248	15	0
248	16	0
248	17	0
248	18	0
248	19	0
248	20	0
248	21	0
248	22	0
248	23	0
248	24	0
248	25	0
248	26	0
248	27	0
248	28	0
248	29	1
248	30	1
248	31	0
248	32	0
248	33	0
248	34	0
248	35	0
249	1	1
249	2	0
249	3	0
249	4	0
249	5	0
249	6	0
249	7	0
249	8	0
249	9	0
249	10	0
249	11	0
249	12	0
249	13	0
249	14	0
249	15	0
249	16	1
249	17	0
249	18	0
249	19	0
249	20	0
249	21	0
249	22	0
249	23	0
249	24	0
249	25	0
249	26	1
249	27	0
249	28	0
249	29	1
249	30	0
249	31	0
249	32	0
249	33	0
249	34	0
249	35	0
250	1	1
250	2	0
250	3	0
250	4	0
250	5	0
250	6	0
250	7	0
250	8	0
250	9	0
250	10	0
250	11	0
250	12	0
250	13	0
250	14	0
250	15	0
250	16	0
250	17	1
250	18	0
250	19	0
250	20	0
250	21	0
250	22	1
250	23	0
250	24	0
250	25	0
250	26	0
250	27	0
250	28	0
250	29	1
250	30	0
250	31	0
250	32	0
250	33	0
250	34	0
250	35	0
251	1	0
251	2	1
251	3	0
251	4	0
251	5	0
251	6	0
251	7	0
251	8	0
251	9	0
251	10	0
251	11	0
251	12	0
251	13	1
251	14	0
251	15	0
251	16	0
251	17	0
251	18	0
251	19	0
251	20	0
251	21	0
251	22	0
251	23	0
251	24	0
251	25	1
251	26	0
251	27	0
251	28	0
251	29	0
251	30	0
251	31	0
251	32	0
251	33	0
251	34	0
251	35	0
252	1	0
252	2	1
252	3	0
252	4	0
252	5	0
252	6	0
252	7	0
252	8	0
252	9	0
252	10	0
252	11	0
252	12	0
252	13	0
252	14	0
252	15	0
252	16	0
252	17	0
252	18	0
252	19	0
252	20	0
252	21	0
252	22	0
252	23	0
252	24	0
252	25	1
252	26	0
252	27	0
252	28	0
252	29	0
252	30	0
252	31	0
252	32	0
252	33	0
252	34	0
252	35	0
253	1	0
253	2	0
253	3	0
253	4	1
253	5	0
253	6	0
253	7	0
253	8	0
253	9	0
253	10	0
253	11	0
253	12	0
253	13	0
253	14	0
253	15	0
253	16	0
253	17	0
253	18	0
253	19	0
253	20	0
253	21	0
253	22	0
253	23	0
253	24	0
253	25	1
253	26	1
253	27	0
253	28	0
253	29	0
253	30	0
253	31	0
253	32	0
253	33	0
253	34	0
253	35	0
254	1	0
254	2	0
254	3	0
254	4	0
254	5	0
254	6	0
254	7	0
254	8	1
254	9	0
254	10	0
254	11	0
254	12	0
254	13	0
254	14	0
254	15	0
254	16	0
254	17	0
254	18	0
254	19	0
254	20	0
254	21	0
254	22	0
254	23	0
254	24	0
254	25	1
254	26	0
254	27	0
254	28	0
254	29	0
254	30	0
254	31	0
254	32	0
254	33	0
254	34	0
254	35	1
255	1	0
255	2	1
255	3	0
255	4	0
255	5	1
255	6	0
255	7	0
255	8	0
255	9	0
255	10	0
255	11	0
255	12	0
255	13	1
255	14	0
255	15	0
255	16	0
255	17	0
255	18	0
255	19	0
255	20	0
255	21	0
255	22	0
255	23	0
255	24	0
255	25	1
255	26	0
255	27	1
255	28	0
255	29	0
255	30	0
255	31	0
255	32	0
255	33	0
255	34	0
255	35	0
256	1	1
256	2	0
256	3	0
256	4	0
256	5	0
256	6	0
256	7	0
256	8	0
256	9	0
256	10	0
256	11	0
256	12	0
256	13	0
256	14	0
256	15	0
256	16	0
256	17	0
256	18	0
256	19	0
256	20	0
256	21	0
256	22	1
256	23	0
256	24	0
256	25	0
256	26	0
256	27	0
256	28	0
256	29	1
256	30	0
256	31	0
256	32	0
256	33	0
256	34	0
256	35	0
257	1	0
257	2	0
257	3	0
257	4	0
257	5	0
257	6	0
257	7	0
257	8	0
257	9	0
257	10	1
257	11	0
257	12	0
257	13	1
257	14	0
257	15	0
257	16	1
257	17	0
257	18	0
257	19	0
257	20	0
257	21	0
257	22	0
257	23	0
257	24	0
257	25	0
257	26	0
257	27	0
257	28	0
257	29	0
257	30	0
257	31	0
257	32	0
257	33	0
257	34	1
257	35	0
258	1	1
258	2	0
258	3	0
258	4	0
258	5	0
258	6	0
258	7	0
258	8	0
258	9	0
258	10	0
258	11	0
258	12	1
258	13	0
258	14	0
258	15	0
258	16	1
258	17	0
258	18	0
258	19	0
258	20	0
258	21	0
258	22	0
258	23	0
258	24	0
258	25	0
258	26	0
258	27	0
258	28	0
258	29	0
258	30	0
258	31	0
258	32	0
258	33	0
258	34	0
258	35	0
259	1	1
259	2	0
259	3	0
259	4	0
259	5	0
259	6	0
259	7	0
259	8	0
259	9	0
259	10	0
259	11	0
259	12	1
259	13	0
259	14	0
259	15	0
259	16	1
259	17	0
259	18	0
259	19	0
259	20	0
259	21	0
259	22	0
259	23	0
259	24	0
259	25	0
259	26	0
259	27	0
259	28	0
259	29	0
259	30	0
259	31	0
259	32	0
259	33	0
259	34	1
259	35	0
260	1	1
260	2	0
260	3	0
260	4	0
260	5	0
260	6	0
260	7	0
260	8	0
260	9	0
260	10	0
260	11	0
260	12	0
260	13	0
260	14	0
260	15	0
260	16	0
260	17	1
260	18	0
260	19	0
260	20	0
260	21	0
260	22	1
260	23	0
260	24	0
260	25	0
260	26	0
260	27	0
260	28	0
260	29	1
260	30	0
260	31	0
260	32	0
260	33	0
260	34	1
260	35	0
261	1	1
261	2	0
261	3	0
261	4	0
261	5	0
261	6	0
261	7	0
261	8	0
261	9	0
261	10	0
261	11	0
261	12	0
261	13	0
261	14	0
261	15	0
261	16	0
261	17	0
261	18	0
261	19	0
261	20	0
261	21	0
261	22	0
261	23	1
261	24	0
261	25	0
261	26	0
261	27	0
261	28	0
261	29	1
261	30	0
261	31	0
261	32	0
261	33	0
261	34	0
261	35	0
262	1	0
262	2	0
262	3	0
262	4	0
262	5	0
262	6	0
262	7	0
262	8	1
262	9	0
262	10	0
262	11	0
262	12	0
262	13	0
262	14	0
262	15	0
262	16	0
262	17	0
262	18	0
262	19	0
262	20	0
262	21	0
262	22	0
262	23	0
262	24	0
262	25	0
262	26	1
262	27	0
262	28	0
262	29	0
262	30	0
262	31	0
262	32	0
262	33	0
262	34	0
262	35	1
263	1	0
263	2	1
263	3	0
263	4	0
263	5	0
263	6	0
263	7	0
263	8	0
263	9	0
263	10	0
263	11	0
263	12	0
263	13	0
263	14	0
263	15	0
263	16	0
263	17	0
263	18	0
263	19	0
263	20	0
263	21	0
263	22	1
263	23	0
263	24	0
263	25	1
263	26	0
263	27	0
263	28	0
263	29	1
263	30	0
263	31	0
263	32	0
263	33	0
263	34	0
263	35	0
264	1	0
264	2	1
264	3	0
264	4	0
264	5	0
264	6	0
264	7	0
264	8	0
264	9	0
264	10	0
264	11	0
264	12	0
264	13	0
264	14	0
264	15	0
264	16	0
264	17	0
264	18	0
264	19	0
264	20	0
264	21	0
264	22	0
264	23	0
264	24	0
264	25	1
264	26	0
264	27	1
264	28	0
264	29	0
264	30	0
264	31	0
264	32	0
264	33	0
264	34	0
264	35	0
265	1	0
265	2	0
265	3	0
265	4	0
265	5	0
265	6	1
265	7	0
265	8	0
265	9	0
265	10	0
265	11	0
265	12	0
265	13	0
265	14	0
265	15	0
265	16	0
265	17	0
265	18	0
265	19	0
265	20	0
265	21	0
265	22	0
265	23	0
265	24	0
265	25	0
265	26	1
265	27	0
265	28	0
265	29	0
265	30	0
265	31	0
265	32	1
265	33	0
265	34	0
265	35	0
266	1	1
266	2	0
266	3	0
266	4	0
266	5	0
266	6	0
266	7	0
266	8	0
266	9	0
266	10	0
266	11	0
266	12	0
266	13	1
266	14	0
266	15	0
266	16	1
266	17	0
266	18	0
266	19	0
266	20	0
266	21	0
266	22	0
266	23	0
266	24	0
266	25	0
266	26	0
266	27	0
266	28	0
266	29	0
266	30	1
266	31	0
266	32	0
266	33	0
266	34	0
266	35	0
267	1	1
267	2	0
267	3	0
267	4	0
267	5	0
267	6	0
267	7	0
267	8	0
267	9	0
267	10	0
267	11	0
267	12	0
267	13	0
267	14	0
267	15	0
267	16	0
267	17	1
267	18	0
267	19	0
267	20	0
267	21	0
267	22	1
267	23	0
267	24	0
267	25	0
267	26	0
267	27	0
267	28	0
267	29	0
267	30	0
267	31	0
267	32	0
267	33	0
267	34	0
267	35	0
268	1	0
268	2	0
268	3	1
268	4	0
268	5	0
268	6	0
268	7	0
268	8	0
268	9	0
268	10	0
268	11	0
268	12	0
268	13	0
268	14	0
268	15	0
268	16	0
268	17	0
268	18	0
268	19	0
268	20	0
268	21	0
268	22	0
268	23	0
268	24	0
268	25	0
268	26	1
268	27	1
268	28	1
268	29	0
268	30	0
268	31	0
268	32	0
268	33	0
268	34	0
268	35	0
269	1	0
269	2	0
269	3	0
269	4	0
269	5	1
269	6	0
269	7	0
269	8	0
269	9	0
269	10	0
269	11	0
269	12	0
269	13	0
269	14	0
269	15	0
269	16	0
269	17	0
269	18	0
269	19	0
269	20	0
269	21	0
269	22	0
269	23	0
269	24	0
269	25	0
269	26	1
269	27	1
269	28	0
269	29	0
269	30	0
269	31	0
269	32	0
269	33	0
269	34	0
269	35	0
270	1	0
270	2	0
270	3	0
270	4	0
270	5	0
270	6	0
270	7	0
270	8	1
270	9	0
270	10	0
270	11	0
270	12	0
270	13	1
270	14	0
270	15	0
270	16	0
270	17	0
270	18	0
270	19	0
270	20	0
270	21	0
270	22	0
270	23	0
270	24	0
270	25	0
270	26	0
270	27	0
270	28	0
270	29	0
270	30	0
270	31	0
270	32	0
270	33	0
270	34	1
270	35	1
271	1	0
271	2	0
271	3	0
271	4	0
271	5	1
271	6	0
271	7	0
271	8	0
271	9	0
271	10	0
271	11	0
271	12	0
271	13	1
271	14	0
271	15	0
271	16	0
271	17	0
271	18	0
271	19	0
271	20	0
271	21	0
271	22	0
271	23	0
271	24	0
271	25	0
271	26	1
271	27	1
271	28	0
271	29	0
271	30	0
271	31	0
271	32	0
271	33	0
271	34	0
271	35	0
272	1	1
272	2	0
272	3	0
272	4	0
272	5	0
272	6	0
272	7	0
272	8	0
272	9	0
272	10	0
272	11	0
272	12	0
272	13	1
272	14	0
272	15	0
272	16	0
272	17	0
272	18	0
272	19	0
272	20	0
272	21	0
272	22	0
272	23	0
272	24	0
272	25	0
272	26	0
272	27	0
272	28	0
272	29	0
272	30	0
272	31	0
272	32	0
272	33	0
272	34	0
272	35	0
273	1	0
273	2	1
273	3	0
273	4	0
273	5	0
273	6	0
273	7	0
273	8	0
273	9	1
273	10	0
273	11	0
273	12	0
273	13	0
273	14	0
273	15	0
273	16	0
273	17	0
273	18	0
273	19	0
273	20	0
273	21	0
273	22	0
273	23	0
273	24	0
273	25	1
273	26	1
273	27	1
273	28	0
273	29	0
273	30	0
273	31	0
273	32	0
273	33	0
273	34	0
273	35	0
274	1	1
274	2	0
274	3	0
274	4	0
274	5	0
274	6	0
274	7	0
274	8	0
274	9	0
274	10	0
274	11	0
274	12	0
274	13	1
274	14	0
274	15	1
274	16	0
274	17	0
274	18	0
274	19	0
274	20	0
274	21	0
274	22	0
274	23	0
274	24	0
274	25	0
274	26	0
274	27	0
274	28	0
274	29	0
274	30	1
274	31	0
274	32	0
274	33	0
274	34	0
274	35	0
275	1	0
275	2	1
275	3	0
275	4	0
275	5	0
275	6	0
275	7	0
275	8	0
275	9	0
275	10	0
275	11	0
275	12	0
275	13	0
275	14	0
275	15	0
275	16	0
275	17	0
275	18	0
275	19	0
275	20	0
275	21	0
275	22	0
275	23	0
275	24	0
275	25	1
275	26	1
275	27	1
275	28	0
275	29	0
275	30	0
275	31	0
275	32	0
275	33	0
275	34	0
275	35	0
276	1	0
276	2	0
276	3	1
276	4	0
276	5	0
276	6	0
276	7	0
276	8	0
276	9	0
276	10	0
276	11	0
276	12	0
276	13	1
276	14	0
276	15	0
276	16	1
276	17	0
276	18	0
276	19	0
276	20	0
276	21	0
276	22	0
276	23	0
276	24	0
276	25	0
276	26	0
276	27	1
276	28	1
276	29	0
276	30	0
276	31	0
276	32	0
276	33	0
276	34	0
276	35	0
277	1	0
277	2	0
277	3	1
277	4	0
277	5	0
277	6	0
277	7	0
277	8	0
277	9	0
277	10	0
277	11	0
277	12	0
277	13	0
277	14	0
277	15	0
277	16	0
277	17	0
277	18	0
277	19	0
277	20	0
277	21	0
277	22	0
277	23	0
277	24	0
277	25	0
277	26	1
277	27	1
277	28	1
277	29	0
277	30	0
277	31	0
277	32	0
277	33	0
277	34	0
277	35	0
278	1	1
278	2	0
278	3	0
278	4	0
278	5	0
278	6	0
278	7	0
278	8	0
278	9	0
278	10	0
278	11	0
278	12	0
278	13	1
278	14	1
278	15	0
278	16	0
278	17	0
278	18	0
278	19	0
278	20	0
278	21	0
278	22	0
278	23	0
278	24	0
278	25	0
278	26	0
278	27	0
278	28	0
278	29	0
278	30	1
278	31	0
278	32	0
278	33	0
278	34	0
278	35	0
279	1	0
279	2	0
279	3	1
279	4	0
279	5	0
279	6	0
279	7	0
279	8	0
279	9	0
279	10	0
279	11	0
279	12	0
279	13	0
279	14	0
279	15	0
279	16	0
279	17	0
279	18	0
279	19	0
279	20	0
279	21	0
279	22	0
279	23	0
279	24	0
279	25	0
279	26	1
279	27	0
279	28	1
279	29	0
279	30	0
279	31	0
279	32	0
279	33	0
279	34	0
279	35	0
280	1	0
280	2	0
280	3	0
280	4	0
280	5	0
280	6	0
280	7	0
280	8	0
280	9	1
280	10	0
280	11	0
280	12	0
280	13	0
280	14	0
280	15	0
280	16	0
280	17	0
280	18	0
280	19	0
280	20	0
280	21	0
280	22	0
280	23	0
280	24	0
280	25	0
280	26	1
280	27	1
280	28	0
280	29	0
280	30	0
280	31	0
280	32	0
280	33	0
280	34	0
280	35	0
281	1	0
281	2	1
281	3	0
281	4	0
281	5	0
281	6	0
281	7	0
281	8	0
281	9	0
281	10	0
281	11	0
281	12	0
281	13	0
281	14	0
281	15	0
281	16	0
281	17	0
281	18	0
281	19	0
281	20	0
281	21	0
281	22	0
281	23	0
281	24	0
281	25	1
281	26	0
281	27	0
281	28	0
281	29	0
281	30	0
281	31	0
281	32	0
281	33	0
281	34	0
281	35	0
282	1	1
282	2	0
282	3	0
282	4	0
282	5	0
282	6	0
282	7	0
282	8	0
282	9	0
282	10	0
282	11	0
282	12	0
282	13	0
282	14	0
282	15	0
282	16	1
282	17	0
282	18	0
282	19	0
282	20	0
282	21	0
282	22	0
282	23	0
282	24	0
282	25	0
282	26	0
282	27	1
282	28	0
282	29	0
282	30	0
282	31	0
282	32	0
282	33	0
282	34	1
282	35	0
283	1	0
283	2	1
283	3	1
283	4	0
283	5	0
283	6	0
283	7	0
283	8	0
283	9	0
283	10	0
283	11	0
283	12	0
283	13	0
283	14	0
283	15	0
283	16	0
283	17	0
283	18	0
283	19	0
283	20	0
283	21	0
283	22	0
283	23	0
283	24	0
283	25	1
283	26	1
283	27	0
283	28	1
283	29	0
283	30	0
283	31	0
283	32	0
283	33	0
283	34	0
283	35	0
284	1	0
284	2	1
284	3	0
284	4	0
284	5	0
284	6	0
284	7	0
284	8	0
284	9	0
284	10	0
284	11	0
284	12	0
284	13	0
284	14	0
284	15	0
284	16	0
284	17	0
284	18	0
284	19	0
284	20	0
284	21	0
284	22	0
284	23	0
284	24	0
284	25	1
284	26	1
284	27	0
284	28	0
284	29	0
284	30	0
284	31	0
284	32	0
284	33	0
284	34	0
284	35	0
285	1	0
285	2	1
285	3	0
285	4	0
285	5	1
285	6	0
285	7	0
285	8	0
285	9	0
285	10	0
285	11	0
285	12	0
285	13	0
285	14	0
285	15	1
285	16	0
285	17	0
285	18	0
285	19	0
285	20	0
285	21	0
285	22	0
285	23	0
285	24	0
285	25	1
285	26	0
285	27	1
285	28	0
285	29	0
285	30	0
285	31	0
285	32	0
285	33	0
285	34	0
285	35	0
286	1	0
286	2	1
286	3	0
286	4	0
286	5	0
286	6	0
286	7	0
286	8	0
286	9	0
286	10	0
286	11	0
286	12	0
286	13	0
286	14	0
286	15	0
286	16	0
286	17	0
286	18	0
286	19	0
286	20	0
286	21	0
286	22	0
286	23	0
286	24	0
286	25	1
286	26	0
286	27	0
286	28	0
286	29	0
286	30	0
286	31	0
286	32	0
286	33	0
286	34	0
286	35	0
287	1	0
287	2	0
287	3	1
287	4	0
287	5	0
287	6	0
287	7	0
287	8	0
287	9	0
287	10	0
287	11	0
287	12	0
287	13	1
287	14	0
287	15	0
287	16	0
287	17	0
287	18	0
287	19	0
287	20	0
287	21	0
287	22	1
287	23	0
287	24	0
287	25	0
287	26	0
287	27	0
287	28	1
287	29	0
287	30	0
287	31	0
287	32	0
287	33	0
287	34	0
287	35	0
288	1	1
288	2	0
288	3	0
288	4	0
288	5	0
288	6	0
288	7	0
288	8	0
288	9	0
288	10	0
288	11	1
288	12	0
288	13	0
288	14	0
288	15	0
288	16	0
288	17	0
288	18	0
288	19	0
288	20	0
288	21	0
288	22	0
288	23	0
288	24	0
288	25	0
288	26	0
288	27	0
288	28	0
288	29	0
288	30	0
288	31	0
288	32	0
288	33	0
288	34	1
288	35	0
289	1	0
289	2	1
289	3	0
289	4	0
289	5	0
289	6	0
289	7	0
289	8	0
289	9	0
289	10	0
289	11	0
289	12	0
289	13	0
289	14	0
289	15	0
289	16	0
289	17	0
289	18	0
289	19	0
289	20	0
289	21	0
289	22	0
289	23	0
289	24	0
289	25	1
289	26	1
289	27	0
289	28	0
289	29	0
289	30	0
289	31	0
289	32	0
289	33	0
289	34	0
289	35	0
290	1	1
290	2	0
290	3	0
290	4	0
290	5	0
290	6	0
290	7	0
290	8	0
290	9	0
290	10	0
290	11	0
290	12	0
290	13	1
290	14	0
290	15	0
290	16	0
290	17	0
290	18	0
290	19	0
290	20	0
290	21	0
290	22	0
290	23	0
290	24	0
290	25	0
290	26	0
290	27	0
290	28	0
290	29	0
290	30	0
290	31	0
290	32	0
290	33	0
290	34	0
290	35	0
291	1	1
291	2	0
291	3	0
291	4	0
291	5	0
291	6	0
291	7	0
291	8	0
291	9	0
291	10	0
291	11	0
291	12	1
291	13	0
291	14	0
291	15	0
291	16	0
291	17	0
291	18	0
291	19	0
291	20	0
291	21	0
291	22	0
291	23	0
291	24	0
291	25	0
291	26	0
291	27	0
291	28	0
291	29	1
291	30	0
291	31	0
291	32	0
291	33	0
291	34	0
291	35	0
292	1	0
292	2	0
292	3	1
292	4	0
292	5	0
292	6	0
292	7	0
292	8	0
292	9	0
292	10	0
292	11	0
292	12	0
292	13	0
292	14	0
292	15	0
292	16	0
292	17	0
292	18	0
292	19	0
292	20	0
292	21	0
292	22	0
292	23	0
292	24	0
292	25	1
292	26	1
292	27	1
292	28	1
292	29	0
292	30	0
292	31	0
292	32	0
292	33	0
292	34	0
292	35	0
293	1	0
293	2	0
293	3	0
293	4	0
293	5	0
293	6	0
293	7	0
293	8	0
293	9	1
293	10	0
293	11	0
293	12	0
293	13	0
293	14	0
293	15	0
293	16	0
293	17	0
293	18	0
293	19	0
293	20	0
293	21	0
293	22	0
293	23	0
293	24	0
293	25	0
293	26	1
293	27	0
293	28	0
293	29	0
293	30	0
293	31	0
293	32	0
293	33	0
293	34	0
293	35	0
294	1	0
294	2	0
294	3	1
294	4	0
294	5	0
294	6	0
294	7	0
294	8	0
294	9	0
294	10	0
294	11	0
294	12	0
294	13	0
294	14	0
294	15	0
294	16	0
294	17	0
294	18	0
294	19	0
294	20	0
294	21	0
294	22	0
294	23	0
294	24	0
294	25	1
294	26	1
294	27	0
294	28	1
294	29	0
294	30	0
294	31	0
294	32	0
294	33	0
294	34	0
294	35	0
295	1	0
295	2	0
295	3	0
295	4	0
295	5	0
295	6	1
295	7	0
295	8	0
295	9	0
295	10	0
295	11	0
295	12	0
295	13	0
295	14	0
295	15	0
295	16	0
295	17	0
295	18	0
295	19	0
295	20	0
295	21	0
295	22	0
295	23	0
295	24	0
295	25	0
295	26	1
295	27	0
295	28	0
295	29	0
295	30	0
295	31	0
295	32	0
295	33	0
295	34	0
295	35	0
296	1	1
296	2	0
296	3	0
296	4	0
296	5	0
296	6	0
296	7	0
296	8	0
296	9	0
296	10	0
296	11	1
296	12	0
296	13	1
296	14	0
296	15	0
296	16	0
296	17	0
296	18	0
296	19	0
296	20	0
296	21	0
296	22	0
296	23	0
296	24	0
296	25	0
296	26	0
296	27	0
296	28	0
296	29	0
296	30	0
296	31	0
296	32	0
296	33	0
296	34	1
296	35	0
297	1	1
297	2	0
297	3	0
297	4	0
297	5	0
297	6	0
297	7	0
297	8	0
297	9	0
297	10	0
297	11	1
297	12	1
297	13	0
297	14	0
297	15	0
297	16	0
297	17	0
297	18	0
297	19	0
297	20	0
297	21	0
297	22	0
297	23	0
297	24	0
297	25	0
297	26	0
297	27	0
297	28	0
297	29	1
297	30	0
297	31	0
297	32	0
297	33	0
297	34	1
297	35	0
298	1	0
298	2	1
298	3	0
298	4	0
298	5	0
298	6	0
298	7	0
298	8	0
298	9	0
298	10	0
298	11	1
298	12	0
298	13	0
298	14	0
298	15	0
298	16	0
298	17	0
298	18	0
298	19	0
298	20	0
298	21	0
298	22	0
298	23	0
298	24	0
298	25	1
298	26	0
298	27	0
298	28	0
298	29	0
298	30	0
298	31	0
298	32	0
298	33	0
298	34	0
298	35	0
299	1	0
299	2	0
299	3	0
299	4	0
299	5	1
299	6	0
299	7	0
299	8	0
299	9	0
299	10	0
299	11	0
299	12	1
299	13	0
299	14	0
299	15	0
299	16	0
299	17	0
299	18	0
299	19	0
299	20	0
299	21	0
299	22	0
299	23	0
299	24	0
299	25	1
299	26	0
299	27	1
299	28	0
299	29	0
299	30	0
299	31	0
299	32	0
299	33	0
299	34	0
299	35	0
300	1	0
300	2	0
300	3	0
300	4	0
300	5	1
300	6	0
300	7	0
300	8	0
300	9	0
300	10	0
300	11	0
300	12	0
300	13	0
300	14	0
300	15	0
300	16	0
300	17	0
300	18	0
300	19	0
300	20	0
300	21	0
300	22	0
300	23	0
300	24	0
300	25	0
300	26	0
300	27	1
300	28	0
300	29	0
300	30	0
300	31	0
300	32	0
300	33	0
300	34	0
300	35	0
301	1	1
301	2	0
301	3	0
301	4	0
301	5	0
301	6	0
301	7	0
301	8	0
301	9	0
301	10	0
301	11	0
301	12	0
301	13	0
301	14	0
301	15	0
301	16	0
301	17	0
301	18	0
301	19	0
301	20	0
301	21	0
301	22	1
301	23	1
301	24	0
301	25	0
301	26	0
301	27	0
301	28	0
301	29	1
301	30	0
301	31	0
301	32	0
301	33	0
301	34	0
301	35	0
302	1	0
302	2	0
302	3	0
302	4	0
302	5	0
302	6	1
302	7	0
302	8	0
302	9	0
302	10	0
302	11	0
302	12	0
302	13	0
302	14	0
302	15	1
302	16	0
302	17	0
302	18	0
302	19	0
302	20	0
302	21	0
302	22	0
302	23	0
302	24	0
302	25	0
302	26	1
302	27	0
302	28	0
302	29	0
302	30	0
302	31	0
302	32	0
302	33	0
302	34	0
302	35	0
303	1	0
303	2	0
303	3	0
303	4	0
303	5	0
303	6	0
303	7	0
303	8	0
303	9	0
303	10	1
303	11	0
303	12	0
303	13	0
303	14	0
303	15	0
303	16	0
303	17	0
303	18	0
303	19	0
303	20	0
303	21	0
303	22	0
303	23	0
303	24	0
303	25	0
303	26	0
303	27	0
303	28	0
303	29	0
303	30	0
303	31	0
303	32	0
303	33	1
303	34	0
303	35	0
305	1	1
305	2	0
305	3	0
305	4	0
305	5	0
305	6	0
305	7	0
305	8	0
305	9	0
305	10	0
305	11	0
305	12	0
305	13	0
305	14	0
305	15	1
305	16	0
305	17	0
305	18	0
305	19	0
305	20	0
305	21	0
305	22	0
305	23	0
305	24	0
305	25	0
305	26	1
305	27	0
305	28	0
305	29	0
305	30	0
305	31	0
305	32	0
305	33	0
305	34	0
305	35	0
306	1	1
306	2	0
306	3	0
306	4	0
306	5	0
306	6	0
306	7	0
306	8	0
306	9	0
306	10	0
306	11	0
306	12	0
306	13	1
306	14	0
306	15	0
306	16	1
306	17	0
306	18	0
306	19	0
306	20	0
306	21	0
306	22	0
306	23	0
306	24	0
306	25	0
306	26	0
306	27	0
306	28	0
306	29	0
306	30	0
306	31	0
306	32	0
306	33	0
306	34	1
306	35	0
307	1	0
307	2	0
307	3	0
307	4	0
307	5	0
307	6	0
307	7	0
307	8	0
307	9	1
307	10	0
307	11	0
307	12	0
307	13	0
307	14	0
307	15	0
307	16	0
307	17	0
307	18	0
307	19	0
307	20	0
307	21	1
307	22	0
307	23	0
307	24	0
307	25	0
307	26	1
307	27	0
307	28	0
307	29	0
307	30	0
307	31	0
307	32	0
307	33	0
307	34	0
307	35	0
308	1	1
308	2	0
308	3	0
308	4	0
308	5	0
308	6	0
308	7	0
308	8	0
308	9	0
308	10	0
308	11	0
308	12	0
308	13	0
308	14	1
308	15	0
308	16	0
308	17	0
308	18	0
308	19	0
308	20	0
308	21	0
308	22	0
308	23	0
308	24	0
308	25	0
308	26	0
308	27	0
308	28	0
308	29	0
308	30	1
308	31	0
308	32	0
308	33	0
308	34	0
308	35	0
309	1	1
309	2	0
309	3	0
309	4	0
309	5	0
309	6	0
309	7	0
309	8	0
309	9	0
309	10	0
309	11	0
309	12	0
309	13	0
309	14	0
309	15	0
309	16	0
309	17	1
309	18	1
309	19	0
309	20	0
309	21	0
309	22	1
309	23	0
309	24	0
309	25	0
309	26	1
309	27	0
309	28	0
309	29	0
309	30	0
309	31	0
309	32	0
309	33	0
309	34	0
309	35	0
310	1	0
310	2	0
310	3	1
310	4	0
310	5	0
310	6	0
310	7	0
310	8	0
310	9	0
310	10	0
310	11	0
310	12	0
310	13	0
310	14	0
310	15	0
310	16	0
310	17	0
310	18	0
310	19	0
310	20	0
310	21	0
310	22	0
310	23	0
310	24	0
310	25	1
310	26	0
310	27	0
310	28	1
310	29	0
310	30	0
310	31	0
310	32	0
310	33	0
310	34	0
310	35	0
311	1	0
311	2	1
311	3	0
311	4	0
311	5	0
311	6	0
311	7	0
311	8	0
311	9	1
311	10	0
311	11	0
311	12	0
311	13	0
311	14	0
311	15	0
311	16	0
311	17	0
311	18	0
311	19	0
311	20	0
311	21	0
311	22	0
311	23	0
311	24	0
311	25	1
311	26	1
311	27	1
311	28	0
311	29	0
311	30	0
311	31	0
311	32	1
311	33	0
311	34	0
311	35	0
312	1	0
312	2	0
312	3	1
312	4	0
312	5	0
312	6	0
312	7	0
312	8	0
312	9	0
312	10	0
312	11	0
312	12	0
312	13	0
312	14	0
312	15	0
312	16	0
312	17	0
312	18	0
312	19	0
312	20	0
312	21	0
312	22	0
312	23	0
312	24	0
312	25	0
312	26	1
312	27	1
312	28	1
312	29	0
312	30	0
312	31	0
312	32	0
312	33	0
312	34	0
312	35	0
313	1	0
313	2	0
313	3	0
313	4	0
313	5	0
313	6	0
313	7	1
313	8	0
313	9	0
313	10	0
313	11	0
313	12	0
313	13	0
313	14	0
313	15	0
313	16	0
313	17	0
313	18	0
313	19	0
313	20	0
313	21	0
313	22	0
313	23	0
313	24	0
313	25	0
313	26	1
313	27	0
313	28	0
313	29	0
313	30	0
313	31	1
313	32	0
313	33	0
313	34	0
313	35	0
314	1	0
314	2	0
314	3	1
314	4	0
314	5	0
314	6	0
314	7	0
314	8	0
314	9	0
314	10	0
314	11	0
314	12	0
314	13	0
314	14	0
314	15	0
314	16	0
314	17	0
314	18	0
314	19	0
314	20	0
314	21	0
314	22	0
314	23	0
314	24	0
314	25	1
314	26	1
314	27	0
314	28	1
314	29	0
314	30	0
314	31	0
314	32	0
314	33	0
314	34	0
314	35	0
315	1	0
315	2	0
315	3	1
315	4	0
315	5	0
315	6	0
315	7	0
315	8	0
315	9	0
315	10	0
315	11	0
315	12	0
315	13	0
315	14	0
315	15	0
315	16	0
315	17	0
315	18	0
315	19	0
315	20	0
315	21	0
315	22	0
315	23	0
315	24	0
315	25	0
315	26	1
315	27	1
315	28	1
315	29	0
315	30	0
315	31	0
315	32	0
315	33	0
315	34	0
315	35	0
322	1	0
322	2	1
322	3	0
322	4	0
322	5	0
322	6	0
322	7	0
322	8	0
322	9	0
322	10	0
322	11	0
322	12	0
322	13	0
322	14	0
322	15	0
322	16	0
322	17	0
322	18	0
322	19	0
322	20	0
322	21	0
322	22	0
322	23	0
322	24	0
322	25	1
322	26	1
322	27	0
322	28	0
322	29	0
322	30	0
322	31	0
322	32	0
322	33	0
322	34	0
322	35	0
\.


--
-- Data for Name: place_image; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.place_image (place_image_id, place_id, image_key) FROM stdin;
1	16	https://thumb.wikimedia.org/wikipedia/commons/thumb/a/a0/Sunset_on_O_Quy_Ho_pass.jpg/960px-Sunset_on_O_Quy_Ho_pass.jpg
2	16	https://thumb.wikimedia.org/wikipedia/commons/thumb/1/19/O_Quy_Ho_pass.jpg/960px-O_Quy_Ho_pass.jpg
3	31	https://thumb.wikimedia.org/wikipedia/commons/thumb/1/1d/Fansipan_summit_2_in_sunrise.jpg/960px-Fansipan_summit_2_in_sunrise.jpg
4	31	https://thumb.wikimedia.org/wikipedia/commons/thumb/1/19/Fansipan_Summit_Mark.jpg/960px-Fansipan_Summit_Mark.jpg
5	51	https://thumb.wikimedia.org/wikipedia/commons/thumb/f/fd/Trang_An%2C_Ninh_Binh_%2824089563857%29.jpg/960px-Trang_An%2C_Ninh_Binh_%2824089563857%29.jpg
6	51	https://thumb.wikimedia.org/wikipedia/commons/thumb/a/a5/Trang_An%2C_Ninh_Binh.jpg/960px-Trang_An%2C_Ninh_Binh.jpg
7	52	https://thumb.wikimedia.org/wikipedia/commons/thumb/e/ef/Tam_Coc_%283%29.jpg/960px-Tam_Coc_%283%29.jpg
8	52	https://thumb.wikimedia.org/wikipedia/commons/thumb/0/0e/Tam_Coc-Paysage_karstique.jpg/960px-Tam_Coc-Paysage_karstique.jpg
9	54	https://thumb.wikimedia.org/wikipedia/commons/thumb/9/98/Forest_in_Cuc_Phuong_National_Park_%2815706323528%29.jpg/960px-Forest_in_Cuc_Phuong_National_Park_%2815706323528%29.jpg
10	54	https://upload.wikimedia.org/wikipedia/commons/3/3c/Cuc_Phuong_National_Park_-_Vietnam.JPG
11	101	https://thumb.wikimedia.org/wikipedia/commons/thumb/6/67/Nha_san_Moc_Chau.jpg/960px-Nha_san_Moc_Chau.jpg
12	101	https://upload.wikimedia.org/wikipedia/commons/8/86/M%E1%BB%99c_Ch%C3%A2u.jpg
13	116	https://thumb.wikimedia.org/wikipedia/commons/thumb/4/4b/Nui_Coc_Lake_25.jpg/960px-Nui_Coc_Lake_25.jpg
14	117	https://thumb.wikimedia.org/wikipedia/commons/thumb/a/a4/Tan_Cuong_Tea_culture.jpg/960px-Tan_Cuong_Tea_culture.jpg
15	151	https://thumb.wikimedia.org/wikipedia/commons/thumb/d/d7/NNU_Ch%C3%B9ab%E1%BB%95%C4%91%C3%A0.jpg/960px-NNU_Ch%C3%B9ab%E1%BB%95%C4%91%C3%A0.jpg
16	156	https://thumb.wikimedia.org/wikipedia/commons/thumb/7/71/Ba_Be_Lake_5.jpg/960px-Ba_Be_Lake_5.jpg
17	156	https://thumb.wikimedia.org/wikipedia/commons/thumb/b/b2/Ba_Be_Lake_2014.jpg/960px-Ba_Be_Lake_2014.jpg
18	157	https://thumb.wikimedia.org/wikipedia/commons/thumb/7/79/Hang_Puong_Cave_-_Ba_Be_Lake_-_Ba_Be_National_Park_-_Vietnam_-_01_%2848102406852%29.jpg/960px-Hang_Puong_Cave_-_Ba_Be_Lake_-_Ba_Be_National_Park_-_Vietnam_-_01_%2848102406852%29.jpg
19	157	https://thumb.wikimedia.org/wikipedia/commons/thumb/b/b6/Ba_Be_Lake_-_Puong_cave.JPG/960px-Ba_Be_Lake_-_Puong_cave.JPG
20	158	https://thumb.wikimedia.org/wikipedia/commons/thumb/7/7d/Panorama_of_Dau_Dang_Waterfall_-_Near_Ba_Be_Lake_-_Ba_Be_National_Park_-_Vietnam_%2848102307023%29.jpg/960px-Panorama_of_Dau_Dang_Waterfall_-_Near_Ba_Be_Lake_-_Ba_Be_National_Park_-_Vietnam_%2848102307023%29.jpg
21	158	https://thumb.wikimedia.org/wikipedia/commons/thumb/5/53/Dau_Dang_Waterfall_-_Near_Ba_Be_Lake_-_Ba_Be_National_Park_-_Vietnam_%2848102296396%29.jpg/960px-Dau_Dang_Waterfall_-_Near_Ba_Be_Lake_-_Ba_Be_National_Park_-_Vietnam_%2848102296396%29.jpg
22	206	https://thumb.wikimedia.org/wikipedia/commons/thumb/3/3c/Ban_Gioc_Waterfall_-_Trung_Kanh_District_-_Cao_Bang_Province_-_Vietnam_-_06_%2848119780846%29.jpg/960px-Ban_Gioc_Waterfall_-_Trung_Kanh_District_-_Cao_Bang_Province_-_Vietnam_-_06_%2848119780846%29.jpg
23	206	https://thumb.wikimedia.org/wikipedia/commons/thumb/7/71/Ban_Gioc_Waterfall_-_Trung_Kanh_District_-_Cao_Bang_Province_-_Vietnam_-_05_%2848119871617%29.jpg/960px-Ban_Gioc_Waterfall_-_Trung_Kanh_District_-_Cao_Bang_Province_-_Vietnam_-_05_%2848119871617%29.jpg
24	207	https://thumb.wikimedia.org/wikipedia/commons/thumb/6/6b/Nguom_Ngao_Cave_-_Cao_Bang_Province_-_Vietnam_-_03_%2848119821128%29.jpg/960px-Nguom_Ngao_Cave_-_Cao_Bang_Province_-_Vietnam_-_03_%2848119821128%29.jpg
25	207	https://thumb.wikimedia.org/wikipedia/commons/thumb/1/18/Nguom_Ngao_Cave_-_Cao_Bang_Province_-_Vietnam_-_02_%2848119821703%29.jpg/960px-Nguom_Ngao_Cave_-_Cao_Bang_Province_-_Vietnam_-_02_%2848119821703%29.jpg
26	237	https://thumb.wikimedia.org/wikipedia/commons/thumb/0/0d/Th%C3%A1c_B%C3%A0_lake_-_panorama.jpg/960px-Th%C3%A1c_B%C3%A0_lake_-_panorama.jpg
27	237	https://thumb.wikimedia.org/wikipedia/commons/thumb/f/fd/Th%C3%A1c_B%C3%A0_Lake.jpg/960px-Th%C3%A1c_B%C3%A0_Lake.jpg
28	253	https://upload.wikimedia.org/wikipedia/commons/3/39/Captured_French_artillery_at_the_Dien_Bien_Phu_Museum.jpg
29	254	https://thumb.wikimedia.org/wikipedia/commons/thumb/a/a1/Panorama_of_Ricefields_in_Muong_Thanh_Valley_-_Dien_Bien_Phu_-_Vietnam_-_02_%2848178461326%29.jpg/960px-Panorama_of_Ricefields_in_Muong_Thanh_Valley_-_Dien_Bien_Phu_-_Vietnam_-_02_%2848178461326%29.jpg
30	273	https://thumb.wikimedia.org/wikipedia/commons/thumb/5/55/Dong_Van_old_town.jpg/960px-Dong_Van_old_town.jpg
31	276	https://thumb.wikimedia.org/wikipedia/commons/thumb/0/06/Tam_Chuc_Pagoda_-_Ha_Nam.jpg/960px-Tam_Chuc_Pagoda_-_Ha_Nam.jpg
32	280	https://thumb.wikimedia.org/wikipedia/commons/thumb/4/4c/%C4%90%C3%ACnh_l%C3%A0ng_V%C5%A9_%C4%90%E1%BA%A1i.JPG/960px-%C4%90%C3%ACnh_l%C3%A0ng_V%C5%A9_%C4%90%E1%BA%A1i.JPG
33	281	https://thumb.wikimedia.org/wikipedia/commons/thumb/2/28/Ho_Chi_Minh_Mausoleum%2C_Hanoi_%283%29_%2824622750848%29.jpg/960px-Ho_Chi_Minh_Mausoleum%2C_Hanoi_%283%29_%2824622750848%29.jpg
34	281	https://thumb.wikimedia.org/wikipedia/commons/thumb/3/33/2012_Ho_Chi_Minh_Mausoleum.jpg/960px-2012_Ho_Chi_Minh_Mausoleum.jpg
35	283	https://thumb.wikimedia.org/wikipedia/commons/thumb/d/db/Dai_Trung_gate%2C_Temple_of_Literature%2C_Hanoi%2C_Vietnam%2C_20240123_0928_3065.jpg/960px-Dai_Trung_gate%2C_Temple_of_Literature%2C_Hanoi%2C_Vietnam%2C_20240123_0928_3065.jpg
36	283	https://thumb.wikimedia.org/wikipedia/commons/thumb/7/75/Constellation_of_Literature_pavilion%2C_Temple_of_Literature%2C_Hanoi%2C_Vietnam%2C_20240123_0939_3103.jpg/960px-Constellation_of_Literature_pavilion%2C_Temple_of_Literature%2C_Hanoi%2C_Vietnam%2C_20240123_0939_3103.jpg
37	284	https://thumb.wikimedia.org/wikipedia/commons/thumb/9/9f/Central_Sector_of_the_Imperial_Citadel_of_Thang_Long_-_Hanoi_%2826246667851%29.jpg/960px-Central_Sector_of_the_Imperial_Citadel_of_Thang_Long_-_Hanoi_%2826246667851%29.jpg
38	284	https://thumb.wikimedia.org/wikipedia/commons/thumb/0/0f/Central_Sector_of_the_Imperial_Citadel_of_Thang_Long_-_Hanoi.jpg/960px-Central_Sector_of_the_Imperial_Citadel_of_Thang_Long_-_Hanoi.jpg
39	285	https://thumb.wikimedia.org/wikipedia/commons/thumb/c/cf/LONG_BIEN_BRIDGE_HANOI_VIETNAM_FEB_2012_%287011980509%29.jpg/960px-LONG_BIEN_BRIDGE_HANOI_VIETNAM_FEB_2012_%287011980509%29.jpg
40	285	https://thumb.wikimedia.org/wikipedia/commons/thumb/c/c9/LONG_BIEN_BRIDGE_HANOI_VIETNAM_FEB_2012.jpg/960px-LONG_BIEN_BRIDGE_HANOI_VIETNAM_FEB_2012.jpg
41	294	https://thumb.wikimedia.org/wikipedia/commons/thumb/e/ea/Ki%E1%BA%BFp_B%E1%BA%A1c_temple_in_1904.jpg/960px-Ki%E1%BA%BFp_B%E1%BA%A1c_temple_in_1904.jpg
42	296	https://thumb.wikimedia.org/wikipedia/commons/thumb/c/cb/%C4%90%E1%BB%93_S%C6%A1n_beach_-_panoramio.jpg/960px-%C4%90%E1%BB%93_S%C6%A1n_beach_-_panoramio.jpg
43	297	https://thumb.wikimedia.org/wikipedia/commons/thumb/2/27/Cat_Ba_Island.jpg/960px-Cat_Ba_Island.jpg
44	297	https://thumb.wikimedia.org/wikipedia/commons/thumb/a/a5/Cat_Ba_Island%2C_Vietnam%2C_20240131_1431_4699.jpg/960px-Cat_Ba_Island%2C_Vietnam%2C_20240131_1431_4699.jpg
45	300	https://thumb.wikimedia.org/wikipedia/commons/thumb/8/88/Hoang_Van_Thu_Bridge.jpg/960px-Hoang_Van_Thu_Bridge.jpg
46	306	https://thumb.wikimedia.org/wikipedia/commons/thumb/a/a4/Thung_Nai%2C_Cao_Phong%2C_H%C3%B2a_B%C3%ACnh%2C_Vietnam_-_panoramio.jpg/960px-Thung_Nai%2C_Cao_Phong%2C_H%C3%B2a_B%C3%ACnh%2C_Vietnam_-_panoramio.jpg
900001	2	https://upload.wikimedia.org/wikipedia/commons/b/b5/Po_Nagar_Cham_Towers.jpeg
900002	4	https://upload.wikimedia.org/wikipedia/commons/a/ad/Vinpearl_Land_Nha_Trang_Ferris_wheel.jpg
900003	11	https://upload.wikimedia.org/wikipedia/commons/a/ac/Kontum_wooden_catholic_church.jpg
900004	76	https://thumb.wikimedia.org/wikipedia/commons/thumb/0/06/Hoi_An_Ancient_Town.jpg/960px-Hoi_An_Ancient_Town.jpg
900005	86	https://thumb.wikimedia.org/wikipedia/commons/thumb/2/2d/Halong_Bay_in_Vietnam.jpg/960px-Halong_Bay_in_Vietnam.jpg
900006	206	https://thumb.wikimedia.org/wikipedia/commons/thumb/1/13/Ban_Gioc.jpg/960px-Ban_Gioc.jpg
900007	282	https://thumb.wikimedia.org/wikipedia/commons/thumb/8/83/Ho_Hoan_Kiem.jpg/960px-Ho_Hoan_Kiem.jpg
900008	31	https://thumb.wikimedia.org/wikipedia/commons/thumb/5/59/Fansipan_summit_new.jpg/960px-Fansipan_summit_new.jpg
900009	138	https://thumb.wikimedia.org/wikipedia/commons/thumb/8/88/Saigon_Notre-Dame_Basilica.jpg/960px-Saigon_Notre-Dame_Basilica.jpg
900010	51	https://thumb.wikimedia.org/wikipedia/commons/thumb/1/11/Trang_An_Landscape_Complex%2C_Ninh_Binh_Province%2C_Vietnam%2C_20240202_1524_5364.jpg/960px-Trang_An_Landscape_Complex%2C_Ninh_Binh_Province%2C_Vietnam%2C_20240202_1524_5364.jpg
900011	6	https://thumb.wikimedia.org/wikipedia/commons/thumb/b/bf/Phu_Quoc_Beach.jpg/960px-Phu_Quoc_Beach.jpg
900012	21	https://thumb.wikimedia.org/wikipedia/commons/thumb/3/3a/Xuan_Huong_Lake_in_Da_Lat_%2828219543381%29.jpg/960px-Xuan_Huong_Lake_in_Da_Lat_%2828219543381%29.jpg
900013	211	https://thumb.wikimedia.org/wikipedia/commons/thumb/a/a5/The_Golden_Bridge%2C_Ba_Na_Hills%2C_Vietnam.jpg/960px-The_Golden_Bridge%2C_Ba_Na_Hills%2C_Vietnam.jpg
900014	71	https://thumb.wikimedia.org/wikipedia/commons/thumb/b/bd/Phong_Nha_cave_entrance.jpg/960px-Phong_Nha_cave_entrance.jpg
900015	126	https://thumb.wikimedia.org/wikipedia/commons/thumb/d/d7/Imperial_City%2C_Hue%2C_Vietnam_%2849575145062%29.jpg/960px-Imperial_City%2C_Hue%2C_Vietnam_%2849575145062%29.jpg
900016	80	https://thumb.wikimedia.org/wikipedia/commons/thumb/9/94/A_far_view_of_the_ruins_at_My_Son_%2830992152933%29.jpg/960px-A_far_view_of_the_ruins_at_My_Son_%2830992152933%29.jpg
900017	80	https://thumb.wikimedia.org/wikipedia/commons/thumb/7/7d/My_Son_Sanctuary_Vietnam_06.jpg/960px-My_Son_Sanctuary_Vietnam_06.jpg
900018	3	https://thumb.wikimedia.org/wikipedia/commons/thumb/2/2d/Dien_Khanh_eastern_gate.JPG/960px-Dien_Khanh_eastern_gate.JPG
900019	3	https://thumb.wikimedia.org/wikipedia/commons/thumb/f/fb/Dien_Khanh_front_gate.JPG/960px-Dien_Khanh_front_gate.JPG
900020	1	https://thumb.wikimedia.org/wikipedia/commons/thumb/6/6b/Long_Beach%2C_Cam_Ranh.jpg/960px-Long_Beach%2C_Cam_Ranh.jpg
900021	9	https://thumb.wikimedia.org/wikipedia/commons/thumb/e/ee/RungUMinh3.jpg/960px-RungUMinh3.jpg
900022	9	https://thumb.wikimedia.org/wikipedia/commons/thumb/5/5e/RungUMinh1.jpg/960px-RungUMinh1.jpg
\.


--
-- Data for Name: place_type_feature; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.place_type_feature (place_type_id, code) FROM stdin;
\.


--
-- Data for Name: recommendation_rating_snapshot; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.recommendation_rating_snapshot (recommendation_config_id, place_id, rating, rating_count) FROM stdin;
1	1	4.50	845
1	2	4.50	32654
1	3	\N	0
1	4	4.70	20880
1	5	4.50	279
1	6	4.40	6319
1	7	4.00	3687
1	8	4.30	105
1	9	4.30	16
1	10	4.40	1004
1	11	4.70	4300
1	12	4.70	32
1	13	4.70	90
1	14	\N	0
1	15	4.50	14
1	16	4.70	789
1	17	4.90	9
1	18	4.80	56
1	19	\N	0
1	20	4.40	42
1	21	4.70	407
1	22	4.60	3958
1	23	4.20	19220
1	24	4.20	9671
1	25	4.00	141
1	26	\N	0
1	27	3.90	4070
1	28	4.50	769
1	29	4.60	27
1	30	4.40	329
1	31	4.80	1763
1	32	4.40	702
1	33	4.80	44
1	34	4.50	1206
1	35	4.70	867
1	36	4.00	2441
1	37	4.10	621
1	38	4.70	729
1	39	\N	0
1	40	4.40	108
1	41	4.70	1282
1	42	4.70	1823
1	43	4.30	970
1	44	4.50	1043
1	45	4.70	234
1	46	4.60	8952
1	47	4.30	1657
1	48	4.50	1172
1	49	4.70	562
1	50	4.70	13
1	51	4.60	15397
1	52	4.50	18465
1	53	4.30	12531
1	54	4.50	5206
1	55	3.90	315
1	56	4.50	1395
1	57	4.30	2978
1	58	4.80	28
1	59	4.80	46
1	61	4.70	6210
1	62	\N	0
1	63	3.90	267
1	64	4.40	2340
1	65	\N	0
1	66	4.50	8296
1	67	4.70	307
1	68	4.60	2406
1	69	4.40	10
1	70	4.70	953
1	71	4.70	2960
1	72	4.70	24
1	73	4.80	119
1	74	4.90	122
1	75	4.30	64
1	76	4.80	802
1	77	4.30	20559
1	78	4.40	163
1	79	4.40	610
1	80	4.50	13044
1	81	\N	0
1	82	4.50	565
1	83	4.30	276
1	84	4.10	854
1	85	4.70	279
1	86	4.60	8478
1	87	4.30	2332
1	88	4.40	3995
1	89	4.80	9
1	90	\N	0
1	91	4.80	611
1	92	4.60	3218
1	93	4.30	815
1	94	4.10	47
1	95	4.80	102
1	96	4.40	78
1	97	4.20	4042
1	98	3.90	118
1	99	3.80	939
1	100	4.80	878
1	101	\N	0
1	102	4.20	206
1	103	4.20	610
1	104	4.20	1916
1	105	4.80	225
1	106	4.60	7951
1	107	4.70	5687
1	108	4.50	1245
1	109	4.40	196
1	110	4.30	332
1	111	4.60	3669
1	112	4.20	1299
1	113	3.50	366
1	114	4.70	786
1	115	\N	0
1	116	4.20	1723
1	117	\N	0
1	118	4.70	17
1	119	4.50	6
1	120	4.30	44
1	121	5.00	2
1	122	\N	0
1	123	4.30	1325
1	124	4.20	129
1	125	4.40	750
1	126	4.60	24549
1	127	4.60	20332
1	128	4.50	17196
1	129	4.50	6317
1	130	4.70	744
1	131	4.40	1496
1	132	4.90	54
1	133	4.60	231
1	134	4.60	206
1	135	4.70	450
1	136	4.00	87779
1	137	4.50	48922
1	138	4.40	29222
1	139	4.50	4230
1	140	4.60	18468
1	141	4.40	7661
1	142	4.40	186
1	143	4.60	854
1	144	4.70	5790
1	145	4.60	1040
1	146	4.60	838
1	147	4.40	13547
1	148	4.40	431
1	149	3.50	410
1	150	4.60	721
1	151	4.70	2160
1	152	4.20	179
1	153	4.30	294
1	154	4.50	10
1	155	4.50	70
1	156	4.50	1289
1	157	4.50	223
1	158	4.30	356
1	159	4.00	2
1	160	\N	0
1	161	4.60	130
1	162	\N	0
1	163	4.40	248
1	164	4.40	4103
1	165	4.70	23
1	166	4.60	3026
1	167	4.70	502
1	168	\N	0
1	169	4.60	5859
1	170	4.50	1388
1	171	4.20	642
1	172	4.60	1016
1	173	4.70	49
1	174	4.80	1319
1	175	\N	0
1	176	4.60	1206
1	177	4.40	4193
1	178	4.70	141
1	179	4.40	70
1	180	4.50	754
1	181	4.70	4303
1	182	4.40	17840
1	184	4.60	75
1	185	4.60	6251
1	186	4.30	344
1	187	4.40	174
1	188	4.40	505
1	189	4.30	331
1	190	4.30	2687
1	191	4.40	1174
1	192	4.50	2386
1	193	4.50	1766
1	194	4.00	3494
1	195	4.40	539
1	196	4.80	57
1	197	4.20	846
1	198	4.20	552
1	199	4.30	206
1	200	\N	0
1	201	4.10	8158
1	202	4.80	246
1	203	3.90	1783
1	204	4.60	1116
1	205	4.30	556
1	206	4.60	11529
1	207	4.70	1324
1	208	4.80	1501
1	209	4.50	138
1	210	4.20	118
1	211	4.40	99229
1	212	4.50	9758
1	213	4.40	43219
1	214	4.70	4707
1	215	4.60	772
1	216	4.50	1190
1	217	4.50	846
1	218	4.60	1180
1	219	4.50	402
1	220	4.60	557
1	221	4.50	133
1	222	4.60	212
1	223	4.60	241
1	224	\N	0
1	225	\N	0
1	226	\N	0
1	227	4.60	141
1	229	4.30	2211
1	230	4.40	451
1	231	\N	0
1	232	4.40	1071
1	233	\N	0
1	234	4.10	1286
1	235	4.30	172
1	236	4.70	214
1	237	4.50	266
1	238	\N	0
1	239	\N	0
1	240	4.40	76
1	241	\N	0
1	242	4.20	743
1	243	4.50	8281
1	244	4.20	7558
1	245	4.40	5121
1	246	\N	0
1	247	4.60	57
1	248	4.70	161
1	249	4.30	15
1	250	4.10	647
1	251	4.50	2836
1	252	4.30	2559
1	253	4.70	1271
1	254	4.70	104
1	255	4.60	1393
1	256	4.70	1716
1	257	4.20	11157
1	258	4.50	1545
1	259	4.00	345
1	260	4.40	2692
1	261	4.10	3521
1	262	4.40	4091
1	263	4.50	948
1	264	4.70	925
1	265	4.30	2211
1	266	4.40	660
1	267	4.10	359
1	268	4.70	3436
1	269	4.80	749
1	270	\N	0
1	271	4.60	9276
1	272	4.80	812
1	273	4.60	3175
1	274	5.00	11
1	275	4.60	181
1	276	4.40	358
1	277	4.70	531
1	278	4.10	89
1	279	4.70	886
1	280	\N	0
1	281	4.50	21480
1	282	4.70	13004
1	283	4.60	25999
1	284	4.40	19575
1	285	4.60	114
1	286	4.70	1538
1	287	4.60	1287
1	288	4.30	1070
1	289	4.70	646
1	290	4.40	372
1	291	4.30	336
1	292	4.70	704
1	293	4.60	142
1	294	4.60	3120
1	295	3.00	1
1	296	\N	0
1	297	4.40	1372
1	298	4.70	112
1	299	\N	0
1	300	4.80	193
1	301	5.00	1
1	302	4.30	137
1	303	3.30	117
1	305	4.30	479
1	306	\N	0
1	307	\N	0
1	308	4.70	70
1	309	3.60	334
1	310	4.50	1071
1	311	\N	0
1	312	4.70	1226
1	313	\N	0
1	314	4.60	145
1	315	4.70	53
1	320	5.00	99999
\.


--
-- Data for Name: recommendation_weight; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.recommendation_weight (recommendation_config_id, criterion_id, weight_value) FROM stdin;
1	36	0.4000
1	37	0.2000
1	38	0.2000
1	39	0.1000
1	40	0.1000
\.


--
-- Name: card_card_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.card_card_id_seq', 166, true);


--
-- Name: collection_card_collection_card_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.collection_card_collection_card_id_seq', 900015, true);


--
-- Name: collection_collection_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.collection_collection_id_seq', 900006, true);


--
-- Name: item_item_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.item_item_id_seq', 900002, true);


--
-- Name: place_feature_place_feature_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.place_feature_place_feature_id_seq', 322, true);


--
-- Name: place_image_place_image_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.place_image_place_image_id_seq', 900022, true);


--
-- Name: place_place_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.place_place_id_seq', 320, true);


--
-- Name: place_type_place_type_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.place_type_place_type_id_seq', 112, true);


--
-- Name: recommendation_config_recommendation_config_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.recommendation_config_recommendation_config_id_seq', 2, true);


--
-- Name: recommendation_criterion_criterion_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.recommendation_criterion_criterion_id_seq', 40, true);


--
-- Name: region_region_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.region_region_id_seq', 68, true);


--
-- PostgreSQL database dump complete
--

\unrestrict U5Q5vpc9ixOqfI4ZzvOk93iYNVJJHkws6EpCiMgrCv34CoTTEHcyV7xz2Q5M8EF
