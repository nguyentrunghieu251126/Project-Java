
USE QLCuaHangDienMay;


GO
IF NOT EXISTS (SELECT 1 FROM dbo.Quyen WHERE MaChucNang = 'TONGQUAN_XEM')
    INSERT INTO dbo.Quyen (MaChucNang, TenQuyen, NhomChucNang, MoTa)
    VALUES ('TONGQUAN_XEM', N'Xem trang tổng quan', N'Tổng quan', N'QT08');
-- Bản cập nhật trước xếp quyền này vào nhóm Báo cáo, chuyển sang nhóm riêng cho khớp với menu
UPDATE dbo.Quyen SET NhomChucNang = N'Tổng quan' WHERE MaChucNang = 'TONGQUAN_XEM';


GO
IF OBJECT_ID(N'dbo.TaiKhoan_Quyen', N'U') IS NULL
    CREATE TABLE dbo.TaiKhoan_Quyen (
        -- phần chỉnh riêng cho một nhân viên so với quyền của vai trò
        MaTaiKhoanQuyen INT IDENTITY (1, 1) NOT NULL,
        MaTaiKhoan      INT NOT NULL,
        MaQuyen         INT NOT NULL,
        DuocCap         BIT NOT NULL, -- 1 = cấp thêm, 0 = thu hồi
        CONSTRAINT PK_TaiKhoan_Quyen PRIMARY KEY (MaTaiKhoanQuyen),
        CONSTRAINT UQ_TaiKhoan_Quyen UNIQUE (MaTaiKhoan, MaQuyen),
        CONSTRAINT FK_TKQ_TaiKhoan FOREIGN KEY (MaTaiKhoan) REFERENCES dbo.TaiKhoan (MaTaiKhoan) ON DELETE CASCADE,
        CONSTRAINT FK_TKQ_Quyen FOREIGN KEY (MaQuyen) REFERENCES dbo.Quyen (MaQuyen) ON DELETE CASCADE
    );
GO


GO
-- Ảnh sản phẩm: trỏ về file thật trong static/images/sanpham/<nhóm>/ thay cho đường dẫn .jpg chưa có file
UPDATE dbo.SanPham SET HinhAnh = N'/images/sanpham/dien-thoai/dien-thoai-samsung-galaxy-s24-ultra.png' WHERE MaModel = N'SM-S928B';
UPDATE dbo.SanPham SET HinhAnh = N'/images/sanpham/dien-thoai/dien-thoai-iphone-15-pro-max.png' WHERE MaModel = N'MU793VN-A';
UPDATE dbo.SanPham SET HinhAnh = N'/images/sanpham/dien-thoai/dien-thoai-xiaomi-redmi-note-13.png' WHERE MaModel = N'23129RAA4G';
UPDATE dbo.SanPham SET HinhAnh = N'/images/sanpham/dien-thoai/may-tinh-bang-ipad-air-m2-11.png' WHERE MaModel = N'MUWD3ZA-A';
UPDATE dbo.SanPham SET HinhAnh = N'/images/sanpham/dien-thoai/may-tinh-bang-samsung-tab-s9-fe.png' WHERE MaModel = N'SM-X510';
UPDATE dbo.SanPham SET HinhAnh = N'/images/sanpham/laptop/laptop-macbook-air-m3-13.png' WHERE MaModel = N'MRXN3SA-A';
UPDATE dbo.SanPham SET HinhAnh = N'/images/sanpham/laptop/laptop-dell-inspiron-15-3530.png' WHERE MaModel = N'INS3530-I5';
UPDATE dbo.SanPham SET HinhAnh = N'/images/sanpham/laptop/laptop-asus-vivobook-15.png' WHERE MaModel = N'A1505VA-L1114W';
UPDATE dbo.SanPham SET HinhAnh = N'/images/sanpham/tivi/tivi-samsung-ua55.png' WHERE MaModel = N'UA55AU7700';
UPDATE dbo.SanPham SET HinhAnh = N'/images/sanpham/tivi/tivi-lg-oled-evo-55.png' WHERE MaModel = N'OLED55C4PSA';
UPDATE dbo.SanPham SET HinhAnh = N'/images/sanpham/tivi/tivi-sony-4k-50.png' WHERE MaModel = N'K-50S30';
UPDATE dbo.SanPham SET HinhAnh = N'/images/sanpham/tu-lanh/tu-lanh-panasonic-nr-bc360qkvn.png' WHERE MaModel = N'NR-BC360QKVN';
UPDATE dbo.SanPham SET HinhAnh = N'/images/sanpham/tu-lanh/tu-lanh-samsung-inverter-236-lit.png' WHERE MaModel = N'RT22M4032BY';
UPDATE dbo.SanPham SET HinhAnh = N'/images/sanpham/tu-lanh/tu-lanh-lg-inverter-335-lit.png' WHERE MaModel = N'GN-D332BL';
UPDATE dbo.SanPham SET HinhAnh = N'/images/sanpham/may-giat/may-giat-lg-fv1410s4p.png' WHERE MaModel = N'FV1410S4P';
UPDATE dbo.SanPham SET HinhAnh = N'/images/sanpham/may-giat/may-giat-electrolux-inverter-9kg.png' WHERE MaModel = N'EWF9024P5WB';
UPDATE dbo.SanPham SET HinhAnh = N'/images/sanpham/dieu-hoa/dieu-hoa-daikin-ftkb25wavmv.png' WHERE MaModel = N'FTKB25WAVMV';
UPDATE dbo.SanPham SET HinhAnh = N'/images/sanpham/dieu-hoa/dieu-hoa-panasonic-inverter-1-5hp.png' WHERE MaModel = N'CU-PU12AKH-8';
UPDATE dbo.SanPham SET HinhAnh = N'/images/sanpham/gia-dung/may-loc-nuoc-kangaroo-ro-10-loi.png' WHERE MaModel = N'KG10A3';
UPDATE dbo.SanPham SET HinhAnh = N'/images/sanpham/gia-dung/may-loc-nuoc-karofi-ro-9-loi.png' WHERE MaModel = N'KAQ-U95';
UPDATE dbo.SanPham SET HinhAnh = N'/images/sanpham/gia-dung/noi-com-dien-toshiba-1-8-lit.png' WHERE MaModel = N'RC-18NMFVN';
UPDATE dbo.SanPham SET HinhAnh = N'/images/sanpham/gia-dung/noi-com-dien-cuckoo-1-8-lit.png' WHERE MaModel = N'CR-1020F';
UPDATE dbo.SanPham SET HinhAnh = N'/images/sanpham/gia-dung/lo-vi-song-sharp-20-lit.png' WHERE MaModel = N'R-G225VN-BK';
UPDATE dbo.SanPham SET HinhAnh = N'/images/sanpham/gia-dung/lo-vi-song-electrolux-23-lit.png' WHERE MaModel = N'EMG23D22B';
UPDATE dbo.SanPham SET HinhAnh = N'/images/sanpham/gia-dung/robot-hut-bui-xiaomi-s10.png' WHERE MaModel = N'BHR5988EU';
UPDATE dbo.SanPham SET HinhAnh = N'/images/sanpham/gia-dung/may-hut-bui-dyson-v8-slim.png' WHERE MaModel = N'SV10K';
UPDATE dbo.SanPham SET HinhAnh = N'/images/sanpham/gia-dung/quat-dung-panasonic-f-409k.png' WHERE MaModel = N'F-409KB';
UPDATE dbo.SanPham SET HinhAnh = N'/images/sanpham/gia-dung/quat-dung-toshiba-f-lsa10.png' WHERE MaModel = N'F-LSA10-H';
UPDATE dbo.SanPham SET HinhAnh = N'/images/sanpham/loa/tai-nghe-airpods-pro-2-usbc.png' WHERE MaModel = N'MTJV3';
UPDATE dbo.SanPham SET HinhAnh = N'/images/sanpham/loa/tai-nghe-sony-wh-1000xm5.png' WHERE MaModel = N'WH-1000XM5';
UPDATE dbo.SanPham SET HinhAnh = N'/images/sanpham/loa/loa-jbl-charge-5.png' WHERE MaModel = N'JBLCHARGE5';
UPDATE dbo.SanPham SET HinhAnh = N'/images/sanpham/loa/loa-marshall-emberton-ii.png' WHERE MaModel = N'EMBERTON-II';
UPDATE dbo.SanPham SET HinhAnh = N'/images/sanpham/phu-kien/dong-ho-apple-watch-series-9-41mm.png' WHERE MaModel = N'MR8W3';
UPDATE dbo.SanPham SET HinhAnh = N'/images/sanpham/phu-kien/dong-ho-samsung-galaxy-watch6-40mm.png' WHERE MaModel = N'SM-R930';
UPDATE dbo.SanPham SET HinhAnh = N'/images/sanpham/phu-kien/may-anh-canon-eos-r50-kit.png' WHERE MaModel = N'EOSR50-KIT';
UPDATE dbo.SanPham SET HinhAnh = N'/images/sanpham/phu-kien/may-anh-sony-zv-e10-kit.png' WHERE MaModel = N'ILCE-ZVE10L';
UPDATE dbo.SanPham SET HinhAnh = N'/images/sanpham/phu-kien/man-hinh-lg-ultragear-27.png' WHERE MaModel = N'27GN800-B';
UPDATE dbo.SanPham SET HinhAnh = N'/images/sanpham/phu-kien/man-hinh-dell-ultrasharp-24.png' WHERE MaModel = N'U2424H';
UPDATE dbo.SanPham SET HinhAnh = N'/images/sanpham/phu-kien/may-in-canon-lbp2900.png' WHERE MaModel = N'LBP2900';
UPDATE dbo.SanPham SET HinhAnh = N'/images/sanpham/phu-kien/may-in-hp-m211dw.png' WHERE MaModel = N'9YF83A';
UPDATE dbo.SanPham SET HinhAnh = N'/images/sanpham/phu-kien/ban-phim-logitech-mx-keys-s.png' WHERE MaModel = N'920-011558';
UPDATE dbo.SanPham SET HinhAnh = N'/images/sanpham/phu-kien/chuot-logitech-mx-master-3s.png' WHERE MaModel = N'910-006561';
UPDATE dbo.SanPham SET HinhAnh = N'/images/sanpham/phu-kien/pin-sac-du-phong-anker-10000mah.png' WHERE MaModel = N'A1263';
UPDATE dbo.SanPham SET HinhAnh = N'/images/sanpham/phu-kien/cu-sac-nhanh-samsung-25w.png' WHERE MaModel = N'EP-TA800N';
GO
