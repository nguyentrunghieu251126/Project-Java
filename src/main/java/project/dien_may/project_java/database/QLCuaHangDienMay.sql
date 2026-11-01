/* =====================================================================
   CƠ SỞ DỮ LIỆU: QLCuaHangDienMay
   Đồ án Công nghệ Java – Hệ thống quản lý cửa hàng điện máy
   Chạy trên SQL Server 2016 trở lên (SSMS: mở file, nhấn F5).
   CẢNH BÁO: script sẽ XÓA database QLCuaHangDienMay cũ (nếu có) rồi tạo lại.
   Mật khẩu của mọi tài khoản mẫu: 123456 (đã mã hóa BCrypt).
   ===================================================================== */
USE master;
GO
IF DB_ID(N'QLCuaHangDienMay') IS NOT NULL
BEGIN
    ALTER DATABASE QLCuaHangDienMay SET SINGLE_USER WITH ROLLBACK IMMEDIATE;
    DROP DATABASE QLCuaHangDienMay;
END
GO
CREATE DATABASE QLCuaHangDienMay COLLATE Vietnamese_CI_AS;
GO
USE QLCuaHangDienMay;
GO

/* ============================ 1. TÀI KHOẢN ============================ */
CREATE TABLE dbo.VaiTro (
    MaVaiTro    INT IDENTITY(1,1) NOT NULL,
    TenVaiTro   VARCHAR(30)       NOT NULL,
    CONSTRAINT PK_VaiTro PRIMARY KEY (MaVaiTro),
    CONSTRAINT UQ_VaiTro_TenVaiTro UNIQUE (TenVaiTro)
);

CREATE TABLE dbo.TaiKhoan (
    MaTaiKhoan  INT IDENTITY(1,1) NOT NULL,
    HoTen       NVARCHAR(100)     NOT NULL,
    Email       VARCHAR(100)      NOT NULL,
    MatKhau     VARCHAR(100)      NOT NULL,          -- chuỗi BCrypt (60 ký tự)
    SoDienThoai VARCHAR(15)       NULL,
    DiaChi      NVARCHAR(255)     NULL,
    MaVaiTro    INT               NOT NULL,
    TrangThai   BIT               NOT NULL CONSTRAINT DF_TaiKhoan_TrangThai DEFAULT 1,  -- 1: hoạt động, 0: bị khóa
    NgayTao     DATETIME2(0)      NOT NULL CONSTRAINT DF_TaiKhoan_NgayTao DEFAULT SYSDATETIME(),
    CONSTRAINT PK_TaiKhoan PRIMARY KEY (MaTaiKhoan),
    CONSTRAINT UQ_TaiKhoan_Email UNIQUE (Email),
    CONSTRAINT FK_TaiKhoan_VaiTro FOREIGN KEY (MaVaiTro) REFERENCES dbo.VaiTro(MaVaiTro)
);

/* ============================ 2. SẢN PHẨM ============================= */
CREATE TABLE dbo.DanhMuc (
    MaDanhMuc   INT IDENTITY(1,1) NOT NULL,
    TenDanhMuc  NVARCHAR(100)     NOT NULL,
    MoTa        NVARCHAR(500)     NULL,
    CONSTRAINT PK_DanhMuc PRIMARY KEY (MaDanhMuc),
    CONSTRAINT UQ_DanhMuc_TenDanhMuc UNIQUE (TenDanhMuc)
);

CREATE TABLE dbo.SanPham (
    MaSanPham       INT IDENTITY(1,1) NOT NULL,
    MaModel         VARCHAR(50)       NOT NULL,      -- mã model của hãng, không được trùng
    TenSanPham      NVARCHAR(200)     NOT NULL,
    ThuongHieu      NVARCHAR(50)      NOT NULL,
    DonGia          DECIMAL(18,0)     NOT NULL,
    SoLuong         INT               NOT NULL CONSTRAINT DF_SanPham_SoLuong DEFAULT 0,  -- số lượng tồn kho
    MoTa            NVARCHAR(MAX)     NULL,
    HinhAnh         VARCHAR(255)      NULL,
    ThoiGianBaoHanh INT               NOT NULL CONSTRAINT DF_SanPham_BaoHanh DEFAULT 12, -- số tháng
    MaDanhMuc       INT               NOT NULL,
    PhienBan        INT               NOT NULL CONSTRAINT DF_SanPham_PhienBan DEFAULT 0, -- dùng cho @Version (khóa lạc quan)
    CONSTRAINT PK_SanPham PRIMARY KEY (MaSanPham),
    CONSTRAINT UQ_SanPham_MaModel UNIQUE (MaModel),
    CONSTRAINT FK_SanPham_DanhMuc FOREIGN KEY (MaDanhMuc) REFERENCES dbo.DanhMuc(MaDanhMuc),
    CONSTRAINT CK_SanPham_DonGia CHECK (DonGia >= 0),
    CONSTRAINT CK_SanPham_SoLuong CHECK (SoLuong >= 0),             -- chặn tồn kho âm ở mức CSDL
    CONSTRAINT CK_SanPham_BaoHanh CHECK (ThoiGianBaoHanh >= 0)
);
CREATE INDEX IX_SanPham_TenSanPham ON dbo.SanPham(TenSanPham);
CREATE INDEX IX_SanPham_MaDanhMuc ON dbo.SanPham(MaDanhMuc);

/* ============================ 3. GIỎ HÀNG ============================= */
CREATE TABLE dbo.GioHang (
    MaGioHang   INT IDENTITY(1,1) NOT NULL,
    MaTaiKhoan  INT               NOT NULL,
    NgayTao     DATETIME2(0)      NOT NULL CONSTRAINT DF_GioHang_NgayTao DEFAULT SYSDATETIME(),
    CONSTRAINT PK_GioHang PRIMARY KEY (MaGioHang),
    CONSTRAINT UQ_GioHang_MaTaiKhoan UNIQUE (MaTaiKhoan),           -- mỗi khách hàng 1 giỏ
    CONSTRAINT FK_GioHang_TaiKhoan FOREIGN KEY (MaTaiKhoan) REFERENCES dbo.TaiKhoan(MaTaiKhoan)
);

CREATE TABLE dbo.ChiTietGioHang (
    MaChiTietGioHang INT IDENTITY(1,1) NOT NULL,
    MaGioHang        INT               NOT NULL,
    MaSanPham        INT               NOT NULL,
    SoLuong          INT               NOT NULL,
    CONSTRAINT PK_ChiTietGioHang PRIMARY KEY (MaChiTietGioHang),
    CONSTRAINT UQ_ChiTietGioHang UNIQUE (MaGioHang, MaSanPham),
    CONSTRAINT FK_CTGH_GioHang FOREIGN KEY (MaGioHang) REFERENCES dbo.GioHang(MaGioHang) ON DELETE CASCADE,
    CONSTRAINT FK_CTGH_SanPham FOREIGN KEY (MaSanPham) REFERENCES dbo.SanPham(MaSanPham),
    CONSTRAINT CK_CTGH_SoLuong CHECK (SoLuong > 0)
);

/* ============================ 4. KHUYẾN MÃI =========================== */
CREATE TABLE dbo.KhuyenMai (
    MaKhuyenMai   INT IDENTITY(1,1) NOT NULL,
    TenKhuyenMai  NVARCHAR(200)     NOT NULL,
    MaGiamGia     VARCHAR(30)       NOT NULL,        -- mã khách nhập khi mua hàng
    LoaiGiamGia   VARCHAR(20)       NOT NULL,        -- PHAN_TRAM | SO_TIEN
    GiaTriGiam    DECIMAL(18,0)     NOT NULL,
    NgayBatDau    DATE              NOT NULL,
    NgayKetThuc   DATE              NOT NULL,
    SoLuotToiDa   INT               NULL,            -- NULL: không giới hạn
    SoLuotDaDung  INT               NOT NULL CONSTRAINT DF_KhuyenMai_DaDung DEFAULT 0,
    TrangThai     BIT               NOT NULL CONSTRAINT DF_KhuyenMai_TrangThai DEFAULT 1,
    CONSTRAINT PK_KhuyenMai PRIMARY KEY (MaKhuyenMai),
    CONSTRAINT UQ_KhuyenMai_MaGiamGia UNIQUE (MaGiamGia),
    CONSTRAINT CK_KhuyenMai_Loai CHECK (LoaiGiamGia IN ('PHAN_TRAM','SO_TIEN')),
    CONSTRAINT CK_KhuyenMai_GiaTri CHECK (GiaTriGiam > 0 AND (LoaiGiamGia <> 'PHAN_TRAM' OR GiaTriGiam <= 100)),
    CONSTRAINT CK_KhuyenMai_Ngay CHECK (NgayKetThuc >= NgayBatDau),
    CONSTRAINT CK_KhuyenMai_SoLuot CHECK (SoLuotToiDa IS NULL OR SoLuotDaDung <= SoLuotToiDa)
);

/* ============================ 5. ĐƠN HÀNG ============================= */
CREATE TABLE dbo.DonHang (
    MaDonHang            INT IDENTITY(1,1) NOT NULL,
    MaTaiKhoan           INT               NOT NULL,
    MaKhuyenMai          INT               NULL,
    NgayDat              DATETIME2(0)      NOT NULL CONSTRAINT DF_DonHang_NgayDat DEFAULT SYSDATETIME(),
    HoTenNguoiNhan       NVARCHAR(100)     NOT NULL,
    SoDienThoaiNguoiNhan VARCHAR(15)       NOT NULL,
    DiaChiGiaoHang       NVARCHAR(255)     NOT NULL,
    TienGiam             DECIMAL(18,0)     NOT NULL CONSTRAINT DF_DonHang_TienGiam DEFAULT 0,
    TongTien             DECIMAL(18,0)     NOT NULL,  -- số tiền phải trả sau giảm giá
    TrangThaiDonHang     VARCHAR(20)       NOT NULL CONSTRAINT DF_DonHang_TrangThai DEFAULT 'CHO_XAC_NHAN',
    CONSTRAINT PK_DonHang PRIMARY KEY (MaDonHang),
    CONSTRAINT FK_DonHang_TaiKhoan FOREIGN KEY (MaTaiKhoan) REFERENCES dbo.TaiKhoan(MaTaiKhoan),
    CONSTRAINT FK_DonHang_KhuyenMai FOREIGN KEY (MaKhuyenMai) REFERENCES dbo.KhuyenMai(MaKhuyenMai),
    CONSTRAINT CK_DonHang_Tien CHECK (TongTien >= 0 AND TienGiam >= 0),
    CONSTRAINT CK_DonHang_TrangThai CHECK (TrangThaiDonHang IN
        ('CHO_XAC_NHAN','DA_XAC_NHAN','DA_XUAT_KHO','DANG_GIAO','DA_GIAO','DA_HUY'))
);
CREATE INDEX IX_DonHang_MaTaiKhoan ON dbo.DonHang(MaTaiKhoan);
CREATE INDEX IX_DonHang_TrangThai ON dbo.DonHang(TrangThaiDonHang);

CREATE TABLE dbo.ChiTietDonHang (
    MaChiTietDonHang INT IDENTITY(1,1) NOT NULL,
    MaDonHang        INT               NOT NULL,
    MaSanPham        INT               NOT NULL,
    SoLuong          INT               NOT NULL,
    DonGia           DECIMAL(18,0)     NOT NULL,      -- giá tại thời điểm đặt
    ThanhTien        DECIMAL(18,0)     NOT NULL,      -- SoLuong * DonGia
    CONSTRAINT PK_ChiTietDonHang PRIMARY KEY (MaChiTietDonHang),
    CONSTRAINT UQ_ChiTietDonHang UNIQUE (MaDonHang, MaSanPham),
    CONSTRAINT FK_CTDH_DonHang FOREIGN KEY (MaDonHang) REFERENCES dbo.DonHang(MaDonHang),
    CONSTRAINT FK_CTDH_SanPham FOREIGN KEY (MaSanPham) REFERENCES dbo.SanPham(MaSanPham),
    CONSTRAINT CK_CTDH_SoLuong CHECK (SoLuong > 0),
    CONSTRAINT CK_CTDH_Tien CHECK (DonGia >= 0 AND ThanhTien = SoLuong * DonGia)
);

CREATE TABLE dbo.ThanhToan (
    MaThanhToan          INT IDENTITY(1,1) NOT NULL,
    MaDonHang            INT               NOT NULL,
    PhuongThucThanhToan  VARCHAR(20)       NOT NULL,  -- COD | CHUYEN_KHOAN | THE
    SoTienThanhToan      DECIMAL(18,0)     NOT NULL,
    NgayThanhToan        DATETIME2(0)      NULL,      -- NULL khi chưa thanh toán
    TrangThaiThanhToan   VARCHAR(20)       NOT NULL CONSTRAINT DF_ThanhToan_TrangThai DEFAULT 'CHUA_THANH_TOAN',
    CONSTRAINT PK_ThanhToan PRIMARY KEY (MaThanhToan),
    CONSTRAINT FK_ThanhToan_DonHang FOREIGN KEY (MaDonHang) REFERENCES dbo.DonHang(MaDonHang),
    CONSTRAINT CK_ThanhToan_PhuongThuc CHECK (PhuongThucThanhToan IN ('COD','CHUYEN_KHOAN','THE')),
    CONSTRAINT CK_ThanhToan_TrangThai CHECK (TrangThaiThanhToan IN ('CHUA_THANH_TOAN','DA_THANH_TOAN','DA_HOAN_TIEN')),
    CONSTRAINT CK_ThanhToan_SoTien CHECK (SoTienThanhToan >= 0)
);

/* ======================= 6. NHẬP HÀNG – XUẤT HÀNG ===================== */
CREATE TABLE dbo.YeuCauNhapHang (
    MaYeuCauNhap  INT IDENTITY(1,1) NOT NULL,
    MaTaiKhoan    INT               NOT NULL,        -- nhân viên kho gửi yêu cầu
    MaNguoiDuyet  INT               NULL,            -- quản trị viên duyệt / từ chối
    NgayYeuCau    DATETIME2(0)      NOT NULL CONSTRAINT DF_YCNH_NgayYeuCau DEFAULT SYSDATETIME(),
    TrangThai     VARCHAR(20)       NOT NULL CONSTRAINT DF_YCNH_TrangThai DEFAULT 'CHO_DUYET',
    NgayDuyet     DATETIME2(0)      NULL,
    LyDoTuChoi    NVARCHAR(500)     NULL,
    CONSTRAINT PK_YeuCauNhapHang PRIMARY KEY (MaYeuCauNhap),
    CONSTRAINT FK_YCNH_NguoiYeuCau FOREIGN KEY (MaTaiKhoan) REFERENCES dbo.TaiKhoan(MaTaiKhoan),
    CONSTRAINT FK_YCNH_NguoiDuyet FOREIGN KEY (MaNguoiDuyet) REFERENCES dbo.TaiKhoan(MaTaiKhoan),
    CONSTRAINT CK_YCNH_TrangThai CHECK (TrangThai IN ('CHO_DUYET','DA_DUYET','TU_CHOI','DA_NHAP')),
    CONSTRAINT CK_YCNH_LyDo CHECK (TrangThai <> 'TU_CHOI' OR LyDoTuChoi IS NOT NULL)
);

CREATE TABLE dbo.ChiTietYeuCauNhapHang (
    MaChiTietYeuCauNhap INT IDENTITY(1,1) NOT NULL,
    MaYeuCauNhap        INT               NOT NULL,
    MaSanPham           INT               NOT NULL,
    SoLuongYeuCau       INT               NOT NULL,
    CONSTRAINT PK_ChiTietYeuCauNhapHang PRIMARY KEY (MaChiTietYeuCauNhap),
    CONSTRAINT UQ_ChiTietYeuCauNhapHang UNIQUE (MaYeuCauNhap, MaSanPham),
    CONSTRAINT FK_CTYC_YeuCau FOREIGN KEY (MaYeuCauNhap) REFERENCES dbo.YeuCauNhapHang(MaYeuCauNhap),
    CONSTRAINT FK_CTYC_SanPham FOREIGN KEY (MaSanPham) REFERENCES dbo.SanPham(MaSanPham),
    CONSTRAINT CK_CTYC_SoLuong CHECK (SoLuongYeuCau > 0)
);

CREATE TABLE dbo.PhieuNhap (
    MaPhieuNhap   INT IDENTITY(1,1) NOT NULL,
    MaYeuCauNhap  INT               NOT NULL,
    MaTaiKhoan    INT               NOT NULL,        -- nhân viên kho lập phiếu
    NgayNhap      DATETIME2(0)      NOT NULL CONSTRAINT DF_PhieuNhap_NgayNhap DEFAULT SYSDATETIME(),
    TongTienNhap  DECIMAL(18,0)     NOT NULL CONSTRAINT DF_PhieuNhap_TongTien DEFAULT 0,
    GhiChu        NVARCHAR(500)     NULL,
    CONSTRAINT PK_PhieuNhap PRIMARY KEY (MaPhieuNhap),
    CONSTRAINT FK_PhieuNhap_YeuCau FOREIGN KEY (MaYeuCauNhap) REFERENCES dbo.YeuCauNhapHang(MaYeuCauNhap),
    CONSTRAINT FK_PhieuNhap_TaiKhoan FOREIGN KEY (MaTaiKhoan) REFERENCES dbo.TaiKhoan(MaTaiKhoan),
    CONSTRAINT CK_PhieuNhap_TongTien CHECK (TongTienNhap >= 0)
);

CREATE TABLE dbo.ChiTietPhieuNhap (
    MaChiTietPhieuNhap INT IDENTITY(1,1) NOT NULL,
    MaPhieuNhap        INT               NOT NULL,
    MaSanPham          INT               NOT NULL,
    SoLuongNhap        INT               NOT NULL,
    DonGiaNhap         DECIMAL(18,0)     NOT NULL,
    ThanhTien          DECIMAL(18,0)     NOT NULL,
    CONSTRAINT PK_ChiTietPhieuNhap PRIMARY KEY (MaChiTietPhieuNhap),
    CONSTRAINT UQ_ChiTietPhieuNhap UNIQUE (MaPhieuNhap, MaSanPham),
    CONSTRAINT FK_CTPN_PhieuNhap FOREIGN KEY (MaPhieuNhap) REFERENCES dbo.PhieuNhap(MaPhieuNhap),
    CONSTRAINT FK_CTPN_SanPham FOREIGN KEY (MaSanPham) REFERENCES dbo.SanPham(MaSanPham),
    CONSTRAINT CK_CTPN_SoLuong CHECK (SoLuongNhap > 0),
    CONSTRAINT CK_CTPN_Tien CHECK (DonGiaNhap >= 0 AND ThanhTien = SoLuongNhap * DonGiaNhap)
);

CREATE TABLE dbo.PhieuXuat (
    MaPhieuXuat INT IDENTITY(1,1) NOT NULL,
    MaDonHang   INT               NOT NULL,
    MaTaiKhoan  INT               NOT NULL,          -- nhân viên kho lập phiếu
    NgayXuat    DATETIME2(0)      NOT NULL CONSTRAINT DF_PhieuXuat_NgayXuat DEFAULT SYSDATETIME(),
    GhiChu      NVARCHAR(500)     NULL,
    CONSTRAINT PK_PhieuXuat PRIMARY KEY (MaPhieuXuat),
    CONSTRAINT UQ_PhieuXuat_MaDonHang UNIQUE (MaDonHang),         -- mỗi đơn hàng xuất kho 1 lần
    CONSTRAINT FK_PhieuXuat_DonHang FOREIGN KEY (MaDonHang) REFERENCES dbo.DonHang(MaDonHang),
    CONSTRAINT FK_PhieuXuat_TaiKhoan FOREIGN KEY (MaTaiKhoan) REFERENCES dbo.TaiKhoan(MaTaiKhoan)
);

CREATE TABLE dbo.ChiTietPhieuXuat (
    MaChiTietPhieuXuat INT IDENTITY(1,1) NOT NULL,
    MaPhieuXuat        INT               NOT NULL,
    MaSanPham          INT               NOT NULL,
    SoLuongXuat        INT               NOT NULL,
    CONSTRAINT PK_ChiTietPhieuXuat PRIMARY KEY (MaChiTietPhieuXuat),
    CONSTRAINT UQ_ChiTietPhieuXuat UNIQUE (MaPhieuXuat, MaSanPham),
    CONSTRAINT FK_CTPX_PhieuXuat FOREIGN KEY (MaPhieuXuat) REFERENCES dbo.PhieuXuat(MaPhieuXuat),
    CONSTRAINT FK_CTPX_SanPham FOREIGN KEY (MaSanPham) REFERENCES dbo.SanPham(MaSanPham),
    CONSTRAINT CK_CTPX_SoLuong CHECK (SoLuongXuat > 0)
);

/* ================== 7. ĐÁNH GIÁ – HỖ TRỢ – BẢO HÀNH – ĐỔI TRẢ ================= */
CREATE TABLE dbo.DanhGia (
    MaDanhGia      INT IDENTITY(1,1) NOT NULL,
    MaTaiKhoan     INT               NOT NULL,
    MaSanPham      INT               NOT NULL,
    SoSao          TINYINT           NOT NULL,
    NoiDungDanhGia NVARCHAR(1000)    NULL,
    NgayDanhGia    DATETIME2(0)      NOT NULL CONSTRAINT DF_DanhGia_Ngay DEFAULT SYSDATETIME(),
    CONSTRAINT PK_DanhGia PRIMARY KEY (MaDanhGia),
    CONSTRAINT UQ_DanhGia UNIQUE (MaTaiKhoan, MaSanPham),         -- mỗi khách đánh giá 1 sản phẩm 1 lần
    CONSTRAINT FK_DanhGia_TaiKhoan FOREIGN KEY (MaTaiKhoan) REFERENCES dbo.TaiKhoan(MaTaiKhoan),
    CONSTRAINT FK_DanhGia_SanPham FOREIGN KEY (MaSanPham) REFERENCES dbo.SanPham(MaSanPham),
    CONSTRAINT CK_DanhGia_SoSao CHECK (SoSao BETWEEN 1 AND 5)
);

CREATE TABLE dbo.YeuCauHoTro (
    MaYeuCauHoTro INT IDENTITY(1,1) NOT NULL,
    MaTaiKhoan    INT               NOT NULL,
    TieuDe        NVARCHAR(200)     NOT NULL,
    NoiDung       NVARCHAR(2000)    NOT NULL,
    NgayGui       DATETIME2(0)      NOT NULL CONSTRAINT DF_YCHT_NgayGui DEFAULT SYSDATETIME(),
    TrangThai     VARCHAR(20)       NOT NULL CONSTRAINT DF_YCHT_TrangThai DEFAULT 'MOI',
    PhanHoi       NVARCHAR(2000)    NULL,
    CONSTRAINT PK_YeuCauHoTro PRIMARY KEY (MaYeuCauHoTro),
    CONSTRAINT FK_YCHT_TaiKhoan FOREIGN KEY (MaTaiKhoan) REFERENCES dbo.TaiKhoan(MaTaiKhoan),
    CONSTRAINT CK_YCHT_TrangThai CHECK (TrangThai IN ('MOI','DANG_XU_LY','DA_XU_LY'))
);

CREATE TABLE dbo.PhieuBaoHanh (
    MaPhieuBaoHanh   INT IDENTITY(1,1) NOT NULL,
    MaChiTietDonHang INT               NOT NULL,
    MaTaiKhoan       INT               NULL,          -- nhân viên kho tiếp nhận (NULL khi chưa tiếp nhận)
    NgayYeuCau       DATETIME2(0)      NOT NULL CONSTRAINT DF_PBH_NgayYeuCau DEFAULT SYSDATETIME(),
    NgayTiepNhan     DATETIME2(0)      NULL,
    MoTaLoi          NVARCHAR(1000)    NOT NULL,
    TrangThaiBaoHanh VARCHAR(20)       NOT NULL CONSTRAINT DF_PBH_TrangThai DEFAULT 'CHO_TIEP_NHAN',
    KetQuaXuLy       NVARCHAR(1000)    NULL,
    CONSTRAINT PK_PhieuBaoHanh PRIMARY KEY (MaPhieuBaoHanh),
    CONSTRAINT FK_PBH_ChiTietDonHang FOREIGN KEY (MaChiTietDonHang) REFERENCES dbo.ChiTietDonHang(MaChiTietDonHang),
    CONSTRAINT FK_PBH_TaiKhoan FOREIGN KEY (MaTaiKhoan) REFERENCES dbo.TaiKhoan(MaTaiKhoan),
    CONSTRAINT CK_PBH_TrangThai CHECK (TrangThaiBaoHanh IN ('CHO_TIEP_NHAN','DANG_BAO_HANH','HOAN_THANH','TU_CHOI'))
);

CREATE TABLE dbo.PhieuDoiTra (
    MaPhieuDoiTra    INT IDENTITY(1,1) NOT NULL,
    MaChiTietDonHang INT               NOT NULL,
    MaTaiKhoan       INT               NULL,          -- nhân viên kho tiếp nhận (NULL khi chưa tiếp nhận)
    NgayYeuCau       DATETIME2(0)      NOT NULL CONSTRAINT DF_PDT_NgayYeuCau DEFAULT SYSDATETIME(),
    NgayTiepNhan     DATETIME2(0)      NULL,
    LyDoDoiTra       NVARCHAR(1000)    NOT NULL,
    TrangThaiDoiTra  VARCHAR(20)       NOT NULL CONSTRAINT DF_PDT_TrangThai DEFAULT 'CHO_TIEP_NHAN',
    KetQuaXuLy       NVARCHAR(1000)    NULL,
    CONSTRAINT PK_PhieuDoiTra PRIMARY KEY (MaPhieuDoiTra),
    CONSTRAINT FK_PDT_ChiTietDonHang FOREIGN KEY (MaChiTietDonHang) REFERENCES dbo.ChiTietDonHang(MaChiTietDonHang),
    CONSTRAINT FK_PDT_TaiKhoan FOREIGN KEY (MaTaiKhoan) REFERENCES dbo.TaiKhoan(MaTaiKhoan),
    CONSTRAINT CK_PDT_TrangThai CHECK (TrangThaiDoiTra IN ('CHO_TIEP_NHAN','CHAP_NHAN','TU_CHOI','HOAN_THANH'))
);
GO

/* ============================ DỮ LIỆU MẪU ============================= */

-- VaiTro: 3 bản ghi
SET IDENTITY_INSERT dbo.VaiTro ON;
INSERT INTO dbo.VaiTro (MaVaiTro, TenVaiTro) VALUES
    (1, N'QUAN_TRI'),
    (2, N'NHAN_VIEN_KHO'),
    (3, N'KHACH_HANG');
SET IDENTITY_INSERT dbo.VaiTro OFF;
GO

-- TaiKhoan: 30 bản ghi
SET IDENTITY_INSERT dbo.TaiKhoan ON;
INSERT INTO dbo.TaiKhoan (MaTaiKhoan, HoTen, Email, MatKhau, SoDienThoai, DiaChi, MaVaiTro, TrangThai, NgayTao) VALUES
    (1, N'Huỳnh Minh Phúc', N'admin1@dienmay.vn', N'$2a$10$HTw0S0isa8xJcfibtVx.3.6yoywZl7JSLgkIf80GYO5XE/9ouWBKO', N'0901815908', N'110 Lê Lợi, Quận 3, TP. Hồ Chí Minh', 1, 1, '2026-04-25T14:00:00'),
    (2, N'Trần Ngọc Châu', N'admin2@dienmay.vn', N'$2a$10$HTw0S0isa8xJcfibtVx.3.6yoywZl7JSLgkIf80GYO5XE/9ouWBKO', N'0986091390', N'296 Lý Thường Kiệt, Quận Gò Vấp, TP. Hồ Chí Minh', 1, 1, '2026-03-07T11:00:00'),
    (3, N'Nguyễn Bảo Hưng', N'kho1@dienmay.vn', N'$2a$10$HTw0S0isa8xJcfibtVx.3.6yoywZl7JSLgkIf80GYO5XE/9ouWBKO', N'0924628194', N'287 Cách Mạng Tháng 8, Quận 3, TP. Hồ Chí Minh', 2, 1, '2026-05-14T17:00:00'),
    (4, N'Ngô Ngọc Nam', N'kho2@dienmay.vn', N'$2a$10$HTw0S0isa8xJcfibtVx.3.6yoywZl7JSLgkIf80GYO5XE/9ouWBKO', N'0918190937', N'349 Hoàng Văn Thụ, Quận Gò Vấp, TP. Hồ Chí Minh', 2, 1, '2026-06-08T13:00:00'),
    (5, N'Đặng Thu Tú', N'kho3@dienmay.vn', N'$2a$10$HTw0S0isa8xJcfibtVx.3.6yoywZl7JSLgkIf80GYO5XE/9ouWBKO', N'0975432319', N'154 Hoàng Văn Thụ, Quận Tân Bình, TP. Hồ Chí Minh', 2, 1, '2026-06-21T13:00:00'),
    (6, N'Dương Hoàng Long', N'kho4@dienmay.vn', N'$2a$10$HTw0S0isa8xJcfibtVx.3.6yoywZl7JSLgkIf80GYO5XE/9ouWBKO', N'0991186252', N'251 Nguyễn Văn Linh, Quận 1, TP. Hồ Chí Minh', 2, 1, '2026-05-25T09:00:00'),
    (7, N'Bùi Thu Lan', N'lan.bui7@gmail.com', N'$2a$10$HTw0S0isa8xJcfibtVx.3.6yoywZl7JSLgkIf80GYO5XE/9ouWBKO', N'0955597971', N'431 Nguyễn Trãi, Quận 10, TP. Hồ Chí Minh', 3, 1, '2026-04-30T19:00:00'),
    (8, N'Ngô Thị Bình', N'binh.ngo8@gmail.com', N'$2a$10$HTw0S0isa8xJcfibtVx.3.6yoywZl7JSLgkIf80GYO5XE/9ouWBKO', N'0949746507', N'182 Cách Mạng Tháng 8, TP. Thủ Đức, TP. Hồ Chí Minh', 3, 1, '2026-03-15T15:00:00'),
    (9, N'Nguyễn Ngọc Duy', N'duy.nguyen9@gmail.com', N'$2a$10$HTw0S0isa8xJcfibtVx.3.6yoywZl7JSLgkIf80GYO5XE/9ouWBKO', N'0942366712', N'230 Nguyễn Văn Linh, Quận Phú Nhuận, TP. Hồ Chí Minh', 3, 1, '2026-04-05T10:00:00'),
    (10, N'Võ Bảo Linh', N'linh.vo10@gmail.com', N'$2a$10$HTw0S0isa8xJcfibtVx.3.6yoywZl7JSLgkIf80GYO5XE/9ouWBKO', N'0965632122', N'119 Điện Biên Phủ, Quận 1, TP. Hồ Chí Minh', 3, 1, '2026-05-02T17:00:00'),
    (11, N'Lê Thanh Long', N'long.le11@gmail.com', N'$2a$10$HTw0S0isa8xJcfibtVx.3.6yoywZl7JSLgkIf80GYO5XE/9ouWBKO', N'0902685995', N'65 Hoàng Văn Thụ, TP. Thủ Đức, TP. Hồ Chí Minh', 3, 1, '2026-05-23T18:00:00'),
    (12, N'Dương Văn Sơn', N'son.duong12@gmail.com', N'$2a$10$HTw0S0isa8xJcfibtVx.3.6yoywZl7JSLgkIf80GYO5XE/9ouWBKO', N'0986666176', N'32 Điện Biên Phủ, Quận 3, TP. Hồ Chí Minh', 3, 1, '2026-03-27T15:00:00'),
    (13, N'Lê Thị Mai', N'mai.le13@gmail.com', N'$2a$10$HTw0S0isa8xJcfibtVx.3.6yoywZl7JSLgkIf80GYO5XE/9ouWBKO', N'0990109281', N'187 Lý Thường Kiệt, Quận 1, TP. Hồ Chí Minh', 3, 1, '2026-03-10T11:00:00'),
    (14, N'Đỗ Gia Giang', N'giang.do14@gmail.com', N'$2a$10$HTw0S0isa8xJcfibtVx.3.6yoywZl7JSLgkIf80GYO5XE/9ouWBKO', N'0945957117', N'239 Quang Trung, Quận Tân Bình, TP. Hồ Chí Minh', 3, 1, '2026-04-09T09:00:00'),
    (15, N'Lê Thị Khoa', N'khoa.le15@gmail.com', N'$2a$10$HTw0S0isa8xJcfibtVx.3.6yoywZl7JSLgkIf80GYO5XE/9ouWBKO', N'0954728038', N'186 Cách Mạng Tháng 8, Quận Phú Nhuận, TP. Hồ Chí Minh', 3, 1, '2026-06-26T08:00:00'),
    (16, N'Bùi Thanh Yến', N'yen.bui16@gmail.com', N'$2a$10$HTw0S0isa8xJcfibtVx.3.6yoywZl7JSLgkIf80GYO5XE/9ouWBKO', N'0914852538', N'278 Hoàng Văn Thụ, Quận Bình Thạnh, TP. Hồ Chí Minh', 3, 1, '2026-05-21T11:00:00'),
    (17, N'Đỗ Ngọc Lan', N'lan.do17@gmail.com', N'$2a$10$HTw0S0isa8xJcfibtVx.3.6yoywZl7JSLgkIf80GYO5XE/9ouWBKO', N'0936338750', N'15 Võ Văn Tần, Quận Tân Bình, TP. Hồ Chí Minh', 3, 1, '2026-04-03T11:00:00'),
    (18, N'Dương Thu Nam', N'nam.duong18@gmail.com', N'$2a$10$HTw0S0isa8xJcfibtVx.3.6yoywZl7JSLgkIf80GYO5XE/9ouWBKO', N'0975513137', N'101 Phan Xích Long, Quận 7, TP. Hồ Chí Minh', 3, 1, '2026-05-01T17:00:00'),
    (19, N'Đỗ Văn Tâm', N'tam.do19@gmail.com', N'$2a$10$HTw0S0isa8xJcfibtVx.3.6yoywZl7JSLgkIf80GYO5XE/9ouWBKO', N'0951163726', N'405 Phan Xích Long, Quận 3, TP. Hồ Chí Minh', 3, 1, '2026-06-11T19:00:00'),
    (20, N'Võ Hoàng Phúc', N'phuc.vo20@gmail.com', N'$2a$10$HTw0S0isa8xJcfibtVx.3.6yoywZl7JSLgkIf80GYO5XE/9ouWBKO', N'0912220297', N'413 Cách Mạng Tháng 8, TP. Thủ Đức, TP. Hồ Chí Minh', 3, 1, '2026-06-14T17:00:00'),
    (21, N'Đặng Đức Tú', N'tu.dang21@gmail.com', N'$2a$10$HTw0S0isa8xJcfibtVx.3.6yoywZl7JSLgkIf80GYO5XE/9ouWBKO', N'0952882001', N'270 Cách Mạng Tháng 8, Quận Gò Vấp, TP. Hồ Chí Minh', 3, 1, '2026-06-20T11:00:00'),
    (22, N'Phạm Văn Linh', N'linh.pham22@gmail.com', N'$2a$10$HTw0S0isa8xJcfibtVx.3.6yoywZl7JSLgkIf80GYO5XE/9ouWBKO', N'0934839548', N'215 Cách Mạng Tháng 8, Quận 1, TP. Hồ Chí Minh', 3, 1, '2026-06-25T19:00:00'),
    (23, N'Huỳnh Hoàng Hiếu', N'hieu.huynh23@gmail.com', N'$2a$10$HTw0S0isa8xJcfibtVx.3.6yoywZl7JSLgkIf80GYO5XE/9ouWBKO', N'0998682828', N'262 Lê Lợi, Quận Tân Bình, TP. Hồ Chí Minh', 3, 1, '2026-06-08T10:00:00'),
    (24, N'Đỗ Văn Duy', N'duy.do24@gmail.com', N'$2a$10$HTw0S0isa8xJcfibtVx.3.6yoywZl7JSLgkIf80GYO5XE/9ouWBKO', N'0922279180', N'167 Hoàng Văn Thụ, Quận Phú Nhuận, TP. Hồ Chí Minh', 3, 1, '2026-05-11T15:00:00'),
    (25, N'Trần Bảo Bình', N'binh.tran25@gmail.com', N'$2a$10$HTw0S0isa8xJcfibtVx.3.6yoywZl7JSLgkIf80GYO5XE/9ouWBKO', N'0933401878', N'15 Nguyễn Trãi, Quận Tân Bình, TP. Hồ Chí Minh', 3, 1, '2026-04-11T17:00:00'),
    (26, N'Bùi Thu Thảo', N'thao.bui26@gmail.com', N'$2a$10$HTw0S0isa8xJcfibtVx.3.6yoywZl7JSLgkIf80GYO5XE/9ouWBKO', N'0934788783', N'358 Hoàng Văn Thụ, Quận 10, TP. Hồ Chí Minh', 3, 1, '2026-06-27T16:00:00'),
    (27, N'Phạm Hoàng Giang', N'giang.pham27@gmail.com', N'$2a$10$HTw0S0isa8xJcfibtVx.3.6yoywZl7JSLgkIf80GYO5XE/9ouWBKO', N'0961675136', N'38 Điện Biên Phủ, Quận 10, TP. Hồ Chí Minh', 3, 1, '2026-06-09T09:00:00'),
    (28, N'Lê Anh Yến', N'yen.le28@gmail.com', N'$2a$10$HTw0S0isa8xJcfibtVx.3.6yoywZl7JSLgkIf80GYO5XE/9ouWBKO', N'0952427316', N'250 Cách Mạng Tháng 8, Quận 7, TP. Hồ Chí Minh', 3, 1, '2026-03-21T19:00:00'),
    (29, N'Võ Bảo Phúc', N'phuc.vo29@gmail.com', N'$2a$10$HTw0S0isa8xJcfibtVx.3.6yoywZl7JSLgkIf80GYO5XE/9ouWBKO', N'0956355150', N'174 Hoàng Văn Thụ, Quận Tân Bình, TP. Hồ Chí Minh', 3, 0, '2026-04-26T19:00:00'),
    (30, N'Nguyễn Gia Mai', N'mai.nguyen30@gmail.com', N'$2a$10$HTw0S0isa8xJcfibtVx.3.6yoywZl7JSLgkIf80GYO5XE/9ouWBKO', N'0989481131', N'44 Võ Văn Tần, Quận 10, TP. Hồ Chí Minh', 3, 1, '2026-03-06T20:00:00');
SET IDENTITY_INSERT dbo.TaiKhoan OFF;
GO

-- DanhMuc: 20 bản ghi
SET IDENTITY_INSERT dbo.DanhMuc ON;
INSERT INTO dbo.DanhMuc (MaDanhMuc, TenDanhMuc, MoTa) VALUES
    (1, N'Điện thoại', N'Các sản phẩm thuộc nhóm điện thoại chính hãng'),
    (2, N'Máy tính bảng', N'Các sản phẩm thuộc nhóm máy tính bảng chính hãng'),
    (3, N'Laptop', N'Các sản phẩm thuộc nhóm laptop chính hãng'),
    (4, N'Tivi', N'Các sản phẩm thuộc nhóm tivi chính hãng'),
    (5, N'Tủ lạnh', N'Các sản phẩm thuộc nhóm tủ lạnh chính hãng'),
    (6, N'Máy giặt', N'Các sản phẩm thuộc nhóm máy giặt chính hãng'),
    (7, N'Điều hòa', N'Các sản phẩm thuộc nhóm điều hòa chính hãng'),
    (8, N'Máy lọc nước', N'Các sản phẩm thuộc nhóm máy lọc nước chính hãng'),
    (9, N'Nồi cơm điện', N'Các sản phẩm thuộc nhóm nồi cơm điện chính hãng'),
    (10, N'Lò vi sóng', N'Các sản phẩm thuộc nhóm lò vi sóng chính hãng'),
    (11, N'Máy hút bụi', N'Các sản phẩm thuộc nhóm máy hút bụi chính hãng'),
    (12, N'Quạt điện', N'Các sản phẩm thuộc nhóm quạt điện chính hãng'),
    (13, N'Tai nghe', N'Các sản phẩm thuộc nhóm tai nghe chính hãng'),
    (14, N'Loa', N'Các sản phẩm thuộc nhóm loa chính hãng'),
    (15, N'Đồng hồ thông minh', N'Các sản phẩm thuộc nhóm đồng hồ thông minh chính hãng'),
    (16, N'Máy ảnh', N'Các sản phẩm thuộc nhóm máy ảnh chính hãng'),
    (17, N'Màn hình máy tính', N'Các sản phẩm thuộc nhóm màn hình máy tính chính hãng'),
    (18, N'Máy in', N'Các sản phẩm thuộc nhóm máy in chính hãng'),
    (19, N'Bàn phím - Chuột', N'Các sản phẩm thuộc nhóm bàn phím - chuột chính hãng'),
    (20, N'Phụ kiện sạc', N'Các sản phẩm thuộc nhóm phụ kiện sạc chính hãng');
SET IDENTITY_INSERT dbo.DanhMuc OFF;
GO

-- SanPham: 44 bản ghi
SET IDENTITY_INSERT dbo.SanPham ON;
INSERT INTO dbo.SanPham (MaSanPham, MaModel, TenSanPham, ThuongHieu, DonGia, SoLuong, MoTa, HinhAnh, ThoiGianBaoHanh, MaDanhMuc, PhienBan) VALUES
    (1, N'SM-S928B', N'Samsung Galaxy S24 Ultra 256GB', N'Samsung', 29990000, 8, N'Samsung Galaxy S24 Ultra 256GB chính hãng Samsung, bảo hành 12 tháng.', N'/images/sanpham/sm-s928b.jpg', 12, 1, 0),
    (2, N'MU793VN-A', N'iPhone 15 Pro Max 256GB', N'Apple', 30990000, 15, N'iPhone 15 Pro Max 256GB chính hãng Apple, bảo hành 12 tháng.', N'/images/sanpham/mu793vn-a.jpg', 12, 1, 0),
    (3, N'23129RAA4G', N'Xiaomi Redmi Note 13 8GB/128GB', N'Xiaomi', 4890000, 8, N'Xiaomi Redmi Note 13 8GB/128GB chính hãng Xiaomi, bảo hành 18 tháng.', N'/images/sanpham/23129raa4g.jpg', 18, 1, 0),
    (4, N'MUWD3ZA-A', N'iPad Air M2 11 inch WiFi 128GB', N'Apple', 16990000, 25, N'iPad Air M2 11 inch WiFi 128GB chính hãng Apple, bảo hành 12 tháng.', N'/images/sanpham/muwd3za-a.jpg', 12, 2, 0),
    (5, N'SM-X510', N'Samsung Galaxy Tab S9 FE WiFi', N'Samsung', 9490000, 15, N'Samsung Galaxy Tab S9 FE WiFi chính hãng Samsung, bảo hành 12 tháng.', N'/images/sanpham/sm-x510.jpg', 12, 2, 0),
    (6, N'MRXN3SA-A', N'MacBook Air M3 13 inch 8GB/256GB', N'Apple', 27990000, 25, N'MacBook Air M3 13 inch 8GB/256GB chính hãng Apple, bảo hành 12 tháng.', N'/images/sanpham/mrxn3sa-a.jpg', 12, 3, 0),
    (7, N'INS3530-I5', N'Dell Inspiron 15 3530 i5', N'Dell', 16490000, 8, N'Dell Inspiron 15 3530 i5 chính hãng Dell, bảo hành 12 tháng.', N'/images/sanpham/ins3530-i5.jpg', 12, 3, 0),
    (8, N'A1505VA-L1114W', N'ASUS Vivobook 15 OLED i5', N'ASUS', 17990000, 40, N'ASUS Vivobook 15 OLED i5 chính hãng ASUS, bảo hành 24 tháng.', N'/images/sanpham/a1505va-l1114w.jpg', 24, 3, 0),
    (9, N'UA55AU7700', N'Smart Tivi Samsung 4K 55 inch', N'Samsung', 10990000, 40, N'Smart Tivi Samsung 4K 55 inch chính hãng Samsung, bảo hành 24 tháng.', N'/images/sanpham/ua55au7700.jpg', 24, 4, 0),
    (10, N'OLED55C4PSA', N'Smart Tivi LG OLED evo 55 inch', N'LG', 32990000, 30, N'Smart Tivi LG OLED evo 55 inch chính hãng LG, bảo hành 24 tháng.', N'/images/sanpham/oled55c4psa.jpg', 24, 4, 0),
    (11, N'K-50S30', N'Google Tivi Sony 4K 50 inch', N'Sony', 13490000, 20, N'Google Tivi Sony 4K 50 inch chính hãng Sony, bảo hành 24 tháng.', N'/images/sanpham/k-50s30.jpg', 24, 4, 0),
    (12, N'NR-BC360QKVN', N'Tủ lạnh Panasonic Inverter 322 lít', N'Panasonic', 13990000, 5, N'Tủ lạnh Panasonic Inverter 322 lít chính hãng Panasonic, bảo hành 24 tháng.', N'/images/sanpham/nr-bc360qkvn.jpg', 24, 5, 0),
    (13, N'RT22M4032BY', N'Tủ lạnh Samsung Inverter 236 lít', N'Samsung', 6290000, 15, N'Tủ lạnh Samsung Inverter 236 lít chính hãng Samsung, bảo hành 24 tháng.', N'/images/sanpham/rt22m4032by.jpg', 24, 5, 0),
    (14, N'GN-D332BL', N'Tủ lạnh LG Inverter 335 lít', N'LG', 9990000, 3, N'Tủ lạnh LG Inverter 335 lít chính hãng LG, bảo hành 24 tháng.', N'/images/sanpham/gn-d332bl.jpg', 24, 5, 0),
    (15, N'FV1410S4P', N'Máy giặt LG Inverter 10 kg', N'LG', 9490000, 8, N'Máy giặt LG Inverter 10 kg chính hãng LG, bảo hành 24 tháng.', N'/images/sanpham/fv1410s4p.jpg', 24, 6, 0),
    (16, N'EWF9024P5WB', N'Máy giặt Electrolux Inverter 9 kg', N'Electrolux', 8290000, 25, N'Máy giặt Electrolux Inverter 9 kg chính hãng Electrolux, bảo hành 24 tháng.', N'/images/sanpham/ewf9024p5wb.jpg', 24, 6, 0),
    (17, N'FTKB25WAVMV', N'Điều hòa Daikin Inverter 1 HP', N'Daikin', 10490000, 5, N'Điều hòa Daikin Inverter 1 HP chính hãng Daikin, bảo hành 12 tháng.', N'/images/sanpham/ftkb25wavmv.jpg', 12, 7, 0),
    (18, N'CU-PU12AKH-8', N'Điều hòa Panasonic Inverter 1.5 HP', N'Panasonic', 12690000, 15, N'Điều hòa Panasonic Inverter 1.5 HP chính hãng Panasonic, bảo hành 12 tháng.', N'/images/sanpham/cu-pu12akh-8.jpg', 12, 7, 0),
    (19, N'KG10A3', N'Máy lọc nước Kangaroo RO 10 lõi', N'Kangaroo', 4990000, 3, N'Máy lọc nước Kangaroo RO 10 lõi chính hãng Kangaroo, bảo hành 12 tháng.', N'/images/sanpham/kg10a3.jpg', 12, 8, 0),
    (20, N'KAQ-U95', N'Máy lọc nước Karofi RO 9 lõi', N'Karofi', 6990000, 5, N'Máy lọc nước Karofi RO 9 lõi chính hãng Karofi, bảo hành 12 tháng.', N'/images/sanpham/kaq-u95.jpg', 12, 8, 0),
    (21, N'RC-18NMFVN', N'Nồi cơm điện Toshiba 1.8 lít', N'Toshiba', 1990000, 15, N'Nồi cơm điện Toshiba 1.8 lít chính hãng Toshiba, bảo hành 12 tháng.', N'/images/sanpham/rc-18nmfvn.jpg', 12, 9, 0),
    (22, N'CR-1020F', N'Nồi cơm điện Cuckoo 1.8 lít', N'Cuckoo', 2290000, 5, N'Nồi cơm điện Cuckoo 1.8 lít chính hãng Cuckoo, bảo hành 24 tháng.', N'/images/sanpham/cr-1020f.jpg', 24, 9, 0),
    (23, N'R-G225VN-BK', N'Lò vi sóng Sharp 20 lít', N'Sharp', 1690000, 12, N'Lò vi sóng Sharp 20 lít chính hãng Sharp, bảo hành 12 tháng.', N'/images/sanpham/r-g225vn-bk.jpg', 12, 10, 0),
    (24, N'EMG23D22B', N'Lò vi sóng Electrolux 23 lít', N'Electrolux', 2890000, 5, N'Lò vi sóng Electrolux 23 lít chính hãng Electrolux, bảo hành 24 tháng.', N'/images/sanpham/emg23d22b.jpg', 24, 10, 0),
    (25, N'BHR5988EU', N'Robot hút bụi Xiaomi S10', N'Xiaomi', 4990000, 15, N'Robot hút bụi Xiaomi S10 chính hãng Xiaomi, bảo hành 12 tháng.', N'/images/sanpham/bhr5988eu.jpg', 12, 11, 0),
    (26, N'SV10K', N'Máy hút bụi Dyson V8 Slim', N'Dyson', 9990000, 5, N'Máy hút bụi Dyson V8 Slim chính hãng Dyson, bảo hành 24 tháng.', N'/images/sanpham/sv10k.jpg', 24, 11, 0),
    (27, N'F-409KB', N'Quạt đứng Panasonic F-409K', N'Panasonic', 1590000, 30, N'Quạt đứng Panasonic F-409K chính hãng Panasonic, bảo hành 12 tháng.', N'/images/sanpham/f-409kb.jpg', 12, 12, 0),
    (28, N'F-LSA10-H', N'Quạt đứng Toshiba F-LSA10', N'Toshiba', 1890000, 3, N'Quạt đứng Toshiba F-LSA10 chính hãng Toshiba, bảo hành 12 tháng.', N'/images/sanpham/f-lsa10-h.jpg', 12, 12, 0),
    (29, N'MTJV3', N'Tai nghe AirPods Pro 2 USB-C', N'Apple', 5990000, 20, N'Tai nghe AirPods Pro 2 USB-C chính hãng Apple, bảo hành 12 tháng.', N'/images/sanpham/mtjv3.jpg', 12, 13, 0),
    (30, N'WH-1000XM5', N'Tai nghe Sony WH-1000XM5', N'Sony', 7490000, 40, N'Tai nghe Sony WH-1000XM5 chính hãng Sony, bảo hành 12 tháng.', N'/images/sanpham/wh-1000xm5.jpg', 12, 13, 0),
    (31, N'JBLCHARGE5', N'Loa Bluetooth JBL Charge 5', N'JBL', 3490000, 25, N'Loa Bluetooth JBL Charge 5 chính hãng JBL, bảo hành 12 tháng.', N'/images/sanpham/jblcharge5.jpg', 12, 14, 0),
    (32, N'EMBERTON-II', N'Loa Bluetooth Marshall Emberton II', N'Marshall', 4290000, 15, N'Loa Bluetooth Marshall Emberton II chính hãng Marshall, bảo hành 12 tháng.', N'/images/sanpham/emberton-ii.jpg', 12, 14, 0),
    (33, N'MR8W3', N'Apple Watch Series 9 GPS 41mm', N'Apple', 9490000, 8, N'Apple Watch Series 9 GPS 41mm chính hãng Apple, bảo hành 12 tháng.', N'/images/sanpham/mr8w3.jpg', 12, 15, 0),
    (34, N'SM-R930', N'Samsung Galaxy Watch6 40mm', N'Samsung', 5490000, 3, N'Samsung Galaxy Watch6 40mm chính hãng Samsung, bảo hành 12 tháng.', N'/images/sanpham/sm-r930.jpg', 12, 15, 0),
    (35, N'EOSR50-KIT', N'Máy ảnh Canon EOS R50 Kit 18-45mm', N'Canon', 18990000, 40, N'Máy ảnh Canon EOS R50 Kit 18-45mm chính hãng Canon, bảo hành 24 tháng.', N'/images/sanpham/eosr50-kit.jpg', 24, 16, 0),
    (36, N'ILCE-ZVE10L', N'Máy ảnh Sony ZV-E10 Kit 16-50mm', N'Sony', 16490000, 12, N'Máy ảnh Sony ZV-E10 Kit 16-50mm chính hãng Sony, bảo hành 24 tháng.', N'/images/sanpham/ilce-zve10l.jpg', 24, 16, 0),
    (37, N'27GN800-B', N'Màn hình LG UltraGear 27 inch 2K', N'LG', 6490000, 5, N'Màn hình LG UltraGear 27 inch 2K chính hãng LG, bảo hành 24 tháng.', N'/images/sanpham/27gn800-b.jpg', 24, 17, 0),
    (38, N'U2424H', N'Màn hình Dell UltraSharp 24 inch', N'Dell', 6990000, 8, N'Màn hình Dell UltraSharp 24 inch chính hãng Dell, bảo hành 36 tháng.', N'/images/sanpham/u2424h.jpg', 36, 17, 0),
    (39, N'LBP2900', N'Máy in laser Canon LBP2900', N'Canon', 3290000, 15, N'Máy in laser Canon LBP2900 chính hãng Canon, bảo hành 12 tháng.', N'/images/sanpham/lbp2900.jpg', 12, 18, 0),
    (40, N'9YF83A', N'Máy in laser HP M211dw', N'HP', 4590000, 3, N'Máy in laser HP M211dw chính hãng HP, bảo hành 12 tháng.', N'/images/sanpham/9yf83a.jpg', 12, 18, 0),
    (41, N'920-011558', N'Bàn phím Logitech MX Keys S', N'Logitech', 2990000, 8, N'Bàn phím Logitech MX Keys S chính hãng Logitech, bảo hành 12 tháng.', N'/images/sanpham/920-011558.jpg', 12, 19, 0),
    (42, N'910-006561', N'Chuột Logitech MX Master 3S', N'Logitech', 2490000, 12, N'Chuột Logitech MX Master 3S chính hãng Logitech, bảo hành 12 tháng.', N'/images/sanpham/910-006561.jpg', 12, 19, 0),
    (43, N'A1263', N'Pin sạc dự phòng Anker 10000mAh', N'Anker', 590000, 15, N'Pin sạc dự phòng Anker 10000mAh chính hãng Anker, bảo hành 18 tháng.', N'/images/sanpham/a1263.jpg', 18, 20, 0),
    (44, N'EP-TA800N', N'Củ sạc nhanh Samsung 25W', N'Samsung', 390000, 15, N'Củ sạc nhanh Samsung 25W chính hãng Samsung, bảo hành 12 tháng.', N'/images/sanpham/ep-ta800n.jpg', 12, 20, 0);
SET IDENTITY_INSERT dbo.SanPham OFF;
GO

-- KhuyenMai: 20 bản ghi
SET IDENTITY_INSERT dbo.KhuyenMai ON;
INSERT INTO dbo.KhuyenMai (MaKhuyenMai, TenKhuyenMai, MaGiamGia, LoaiGiamGia, GiaTriGiam, NgayBatDau, NgayKetThuc, SoLuotToiDa, SoLuotDaDung, TrangThai) VALUES
    (1, N'Chào bạn mới', N'CHAOBANMOI', N'SO_TIEN', 200000, '2026-07-01', '2026-07-21', 100, 0, 1),
    (2, N'Siêu sale tháng 8', N'SALE8', N'PHAN_TRAM', 5, '2026-08-01', '2026-09-15', 50, 1, 1),
    (3, N'Back to school', N'BACK2SCHOOL', N'PHAN_TRAM', 7, '2026-08-10', '2026-09-09', 100, 0, 1),
    (4, N'Mừng Quốc khánh 2/9', N'QUOCKHANH', N'PHAN_TRAM', 10, '2026-08-28', '2026-09-11', 100, 0, 1),
    (5, N'Giảm 500K đơn từ 10 triệu', N'GIAM500K', N'SO_TIEN', 500000, '2026-08-01', '2026-08-15', NULL, 0, 1),
    (6, N'Giảm 1 triệu laptop', N'LAPTOP1TR', N'SO_TIEN', 1000000, '2026-09-01', '2026-09-15', 50, 0, 1),
    (7, N'Trung thu sum vầy', N'TRUNGTHU', N'PHAN_TRAM', 8, '2026-09-10', '2026-10-25', 50, 1, 1),
    (8, N'Ngày Phụ nữ VN 20/10', N'PHUNU2010', N'PHAN_TRAM', 10, '2026-10-15', '2026-11-29', NULL, 1, 1),
    (9, N'Flash sale cuối tuần', N'FLASHWEEKEND', N'PHAN_TRAM', 12, '2026-10-01', '2026-11-15', 200, 3, 1),
    (10, N'Giảm 300K phụ kiện', N'PHUKIEN300', N'SO_TIEN', 300000, '2026-09-01', '2026-10-16', 100, 3, 1),
    (11, N'Thành viên thân thiết', N'THANTHIET', N'PHAN_TRAM', 5, '2026-06-01', '2026-12-31', 50, 6, 1),
    (12, N'Sinh nhật cửa hàng', N'SINHNHAT', N'PHAN_TRAM', 15, '2026-10-20', '2026-11-19', 50, 2, 1),
    (13, N'Halloween', N'HALLOWEEN', N'SO_TIEN', 250000, '2026-10-25', '2026-11-14', 200, 1, 1),
    (14, N'Black Friday', N'BLACKFRIDAY', N'PHAN_TRAM', 20, '2026-11-25', '2026-12-25', NULL, 0, 1),
    (15, N'Giảm 2 triệu tivi', N'TIVI2TR', N'SO_TIEN', 2000000, '2026-11-01', '2026-11-21', NULL, 0, 1),
    (16, N'Điện lạnh mùa nóng', N'DIENLANH', N'PHAN_TRAM', 6, '2026-06-01', '2026-08-31', 100, 0, 1),
    (17, N'Mua sắm 11/11', N'SALE1111', N'PHAN_TRAM', 11, '2026-11-08', '2026-12-23', 50, 0, 1),
    (18, N'Giáng sinh an lành', N'NOEL', N'SO_TIEN', 400000, '2026-12-15', '2026-12-29', NULL, 0, 1),
    (19, N'Đón năm mới 2027', N'NAMMOI2027', N'PHAN_TRAM', 10, '2026-12-26', '2027-02-09', 100, 0, 1),
    (20, N'Tết sum vầy', N'TET2027', N'SO_TIEN', 600000, '2027-01-10', '2027-01-30', 100, 0, 1);
SET IDENTITY_INSERT dbo.KhuyenMai OFF;
GO

-- GioHang: 22 bản ghi
SET IDENTITY_INSERT dbo.GioHang ON;
INSERT INTO dbo.GioHang (MaGioHang, MaTaiKhoan, NgayTao) VALUES
    (1, 7, '2026-04-30T19:00:00'),
    (2, 8, '2026-03-15T15:00:00'),
    (3, 9, '2026-04-05T10:00:00'),
    (4, 10, '2026-05-02T17:00:00'),
    (5, 11, '2026-05-23T18:00:00'),
    (6, 12, '2026-03-27T15:00:00'),
    (7, 13, '2026-03-10T11:00:00'),
    (8, 14, '2026-04-09T09:00:00'),
    (9, 15, '2026-06-26T08:00:00'),
    (10, 16, '2026-05-21T11:00:00'),
    (11, 17, '2026-04-03T11:00:00'),
    (12, 18, '2026-05-01T17:00:00'),
    (13, 19, '2026-06-11T19:00:00'),
    (14, 20, '2026-06-14T17:00:00'),
    (15, 21, '2026-06-20T11:00:00'),
    (16, 22, '2026-06-25T19:00:00'),
    (17, 23, '2026-06-08T10:00:00'),
    (18, 24, '2026-05-11T15:00:00'),
    (19, 25, '2026-04-11T17:00:00'),
    (20, 26, '2026-06-27T16:00:00'),
    (21, 27, '2026-06-09T09:00:00'),
    (22, 28, '2026-03-21T19:00:00');
SET IDENTITY_INSERT dbo.GioHang OFF;
GO

-- ChiTietGioHang: 34 bản ghi
SET IDENTITY_INSERT dbo.ChiTietGioHang ON;
INSERT INTO dbo.ChiTietGioHang (MaChiTietGioHang, MaGioHang, MaSanPham, SoLuong) VALUES
    (1, 1, 32, 1),
    (2, 1, 1, 2),
    (3, 1, 5, 1),
    (4, 2, 16, 1),
    (5, 2, 7, 1),
    (6, 3, 34, 2),
    (7, 5, 6, 1),
    (8, 5, 36, 1),
    (9, 6, 15, 2),
    (10, 8, 9, 1),
    (11, 8, 41, 2),
    (12, 9, 8, 1),
    (13, 9, 7, 1),
    (14, 10, 38, 1),
    (15, 10, 13, 1),
    (16, 10, 25, 2),
    (17, 13, 20, 1),
    (18, 13, 30, 2),
    (19, 13, 18, 1),
    (20, 14, 34, 2),
    (21, 14, 16, 1),
    (22, 16, 42, 1),
    (23, 16, 20, 1),
    (24, 17, 32, 2),
    (25, 18, 6, 1),
    (26, 18, 17, 2),
    (27, 19, 24, 1),
    (28, 19, 15, 1),
    (29, 20, 27, 2),
    (30, 20, 24, 1),
    (31, 21, 1, 1),
    (32, 22, 5, 1),
    (33, 22, 14, 1),
    (34, 22, 32, 1);
SET IDENTITY_INSERT dbo.ChiTietGioHang OFF;
GO

-- DonHang: 30 bản ghi
SET IDENTITY_INSERT dbo.DonHang ON;
INSERT INTO dbo.DonHang (MaDonHang, MaTaiKhoan, MaKhuyenMai, NgayDat, HoTenNguoiNhan, SoDienThoaiNguoiNhan, DiaChiGiaoHang, TienGiam, TongTien, TrangThaiDonHang) VALUES
    (1, 14, 11, '2026-08-07T15:11:00', N'Đỗ Gia Giang', N'0945957117', N'239 Quang Trung, Quận Tân Bình, TP. Hồ Chí Minh', 634000, 12056000, N'DA_GIAO'),
    (2, 21, 9, '2026-10-11T13:15:00', N'Đặng Đức Tú', N'0952882001', N'270 Cách Mạng Tháng 8, Quận Gò Vấp, TP. Hồ Chí Minh', 838000, 6152000, N'DA_GIAO'),
    (3, 28, NULL, '2026-08-12T15:17:00', N'Lê Anh Yến', N'0952427316', N'250 Cách Mạng Tháng 8, Quận 7, TP. Hồ Chí Minh', 0, 19560000, N'DA_GIAO'),
    (4, 11, 11, '2026-08-20T14:37:00', N'Lê Thanh Long', N'0902685995', N'65 Hoàng Văn Thụ, TP. Thủ Đức, TP. Hồ Chí Minh', 499000, 9491000, N'DA_GIAO'),
    (5, 18, NULL, '2026-09-20T20:20:00', N'Dương Thu Nam', N'0975513137', N'101 Phan Xích Long, Quận 7, TP. Hồ Chí Minh', 0, 42970000, N'DA_GIAO'),
    (6, 25, NULL, '2026-10-05T10:58:00', N'Trần Bảo Bình', N'0933401878', N'15 Nguyễn Trãi, Quận Tân Bình, TP. Hồ Chí Minh', 0, 46970000, N'DA_GIAO'),
    (7, 8, NULL, '2026-08-07T10:40:00', N'Ngô Thị Bình', N'0949746507', N'182 Cách Mạng Tháng 8, TP. Thủ Đức, TP. Hồ Chí Minh', 0, 21480000, N'DA_GIAO'),
    (8, 15, NULL, '2026-10-03T12:00:00', N'Lê Thị Khoa', N'0954728038', N'186 Cách Mạng Tháng 8, Quận Phú Nhuận, TP. Hồ Chí Minh', 0, 18980000, N'DA_GIAO'),
    (9, 22, 9, '2026-10-01T12:51:00', N'Phạm Văn Linh', N'0934839548', N'215 Cách Mạng Tháng 8, Quận 1, TP. Hồ Chí Minh', 1258000, 9232000, N'DA_GIAO'),
    (10, 29, 7, '2026-10-04T21:24:00', N'Võ Bảo Phúc', N'0956355150', N'174 Hoàng Văn Thụ, Quận Tân Bình, TP. Hồ Chí Minh', 558000, 6422000, N'DA_GIAO'),
    (11, 12, 2, '2026-09-13T12:41:00', N'Dương Văn Sơn', N'0986666176', N'32 Điện Biên Phủ, Quận 3, TP. Hồ Chí Minh', 554000, 10526000, N'DA_GIAO'),
    (12, 19, 11, '2026-08-14T19:13:00', N'Đỗ Văn Tâm', N'0951163726', N'405 Phan Xích Long, Quận 3, TP. Hồ Chí Minh', 524000, 9956000, N'DA_GIAO'),
    (13, 26, 10, '2026-09-10T09:59:00', N'Bùi Thu Thảo', N'0934788783', N'358 Hoàng Văn Thụ, Quận 10, TP. Hồ Chí Minh', 300000, 35680000, N'DA_GIAO'),
    (14, 9, NULL, '2026-09-20T11:58:00', N'Nguyễn Ngọc Duy', N'0942366712', N'230 Nguyễn Văn Linh, Quận Phú Nhuận, TP. Hồ Chí Minh', 0, 9490000, N'DA_GIAO'),
    (15, 16, 10, '2026-09-17T10:38:00', N'Bùi Thanh Yến', N'0914852538', N'278 Hoàng Văn Thụ, Quận Bình Thạnh, TP. Hồ Chí Minh', 300000, 33270000, N'DA_GIAO'),
    (16, 23, NULL, '2026-08-22T08:31:00', N'Huỳnh Hoàng Hiếu', N'0998682828', N'262 Lê Lợi, Quận Tân Bình, TP. Hồ Chí Minh', 0, 16980000, N'DA_GIAO'),
    (17, 30, NULL, '2026-09-19T13:07:00', N'Nguyễn Gia Mai', N'0989481131', N'44 Võ Văn Tần, Quận 10, TP. Hồ Chí Minh', 0, 31980000, N'DA_GIAO'),
    (18, 13, 10, '2026-09-08T12:23:00', N'Lê Thị Mai', N'0990109281', N'187 Lý Thường Kiệt, Quận 1, TP. Hồ Chí Minh', 300000, 9690000, N'DA_GIAO'),
    (19, 20, 11, '2026-10-26T12:00:00', N'Võ Hoàng Phúc', N'0912220297', N'413 Cách Mạng Tháng 8, TP. Thủ Đức, TP. Hồ Chí Minh', 634000, 12056000, N'DANG_GIAO'),
    (20, 27, NULL, '2026-10-22T14:00:00', N'Phạm Hoàng Giang', N'0961675136', N'38 Điện Biên Phủ, Quận 10, TP. Hồ Chí Minh', 0, 11170000, N'DANG_GIAO'),
    (21, 10, NULL, '2026-10-25T09:00:00', N'Võ Bảo Linh', N'0965632122', N'119 Điện Biên Phủ, Quận 1, TP. Hồ Chí Minh', 0, 1590000, N'DA_XUAT_KHO'),
    (22, 17, 9, '2026-10-23T08:00:00', N'Đỗ Ngọc Lan', N'0936338750', N'15 Võ Văn Tần, Quận Tân Bình, TP. Hồ Chí Minh', 3356000, 24614000, N'DA_XUAT_KHO'),
    (23, 24, 12, '2026-10-29T18:00:00', N'Đỗ Văn Duy', N'0922279180', N'167 Hoàng Văn Thụ, Quận Phú Nhuận, TP. Hồ Chí Minh', 1048000, 5942000, N'DA_XAC_NHAN'),
    (24, 7, 12, '2026-10-27T18:00:00', N'Bùi Thu Lan', N'0955597971', N'431 Nguyễn Trãi, Quận 10, TP. Hồ Chí Minh', 1423000, 8067000, N'DA_XAC_NHAN'),
    (25, 14, 8, '2026-10-29T13:00:00', N'Đỗ Gia Giang', N'0945957117', N'239 Quang Trung, Quận Tân Bình, TP. Hồ Chí Minh', 1477000, 13293000, N'DA_XAC_NHAN'),
    (26, 21, 13, '2026-10-28T16:00:00', N'Đặng Đức Tú', N'0952882001', N'270 Cách Mạng Tháng 8, Quận Gò Vấp, TP. Hồ Chí Minh', 250000, 1740000, N'CHO_XAC_NHAN'),
    (27, 28, 11, '2026-10-26T19:00:00', N'Lê Anh Yến', N'0952427316', N'250 Cách Mạng Tháng 8, Quận 7, TP. Hồ Chí Minh', 658000, 12502000, N'CHO_XAC_NHAN'),
    (28, 11, NULL, '2026-10-26T15:00:00', N'Lê Thanh Long', N'0902685995', N'65 Hoàng Văn Thụ, TP. Thủ Đức, TP. Hồ Chí Minh', 0, 9380000, N'CHO_XAC_NHAN'),
    (29, 18, NULL, '2026-08-13T12:57:00', N'Dương Thu Nam', N'0975513137', N'101 Phan Xích Long, Quận 7, TP. Hồ Chí Minh', 0, 4990000, N'DA_HUY'),
    (30, 25, 11, '2026-09-10T21:52:00', N'Trần Bảo Bình', N'0933401878', N'15 Nguyễn Trãi, Quận Tân Bình, TP. Hồ Chí Minh', 549000, 10441000, N'DA_HUY');
SET IDENTITY_INSERT dbo.DonHang OFF;
GO

-- ChiTietDonHang: 51 bản ghi
SET IDENTITY_INSERT dbo.ChiTietDonHang ON;
INSERT INTO dbo.ChiTietDonHang (MaChiTietDonHang, MaDonHang, MaSanPham, SoLuong, DonGia, ThanhTien) VALUES
    (1, 1, 18, 1, 12690000, 12690000),
    (2, 2, 20, 1, 6990000, 6990000),
    (3, 3, 42, 2, 2490000, 4980000),
    (4, 3, 13, 1, 6290000, 6290000),
    (5, 3, 16, 1, 8290000, 8290000),
    (6, 4, 26, 1, 9990000, 9990000),
    (7, 5, 10, 1, 32990000, 32990000),
    (8, 5, 19, 2, 4990000, 9980000),
    (9, 6, 33, 1, 9490000, 9490000),
    (10, 6, 37, 1, 6490000, 6490000),
    (11, 6, 2, 1, 30990000, 30990000),
    (12, 7, 7, 1, 16490000, 16490000),
    (13, 7, 25, 1, 4990000, 4990000),
    (14, 8, 5, 1, 9490000, 9490000),
    (15, 8, 33, 1, 9490000, 9490000),
    (16, 9, 17, 1, 10490000, 10490000),
    (17, 10, 31, 2, 3490000, 6980000),
    (18, 11, 40, 1, 4590000, 4590000),
    (19, 11, 37, 1, 6490000, 6490000),
    (20, 12, 19, 1, 4990000, 4990000),
    (21, 12, 34, 1, 5490000, 5490000),
    (22, 13, 2, 1, 30990000, 30990000),
    (23, 13, 19, 1, 4990000, 4990000),
    (24, 14, 5, 1, 9490000, 9490000),
    (25, 15, 18, 1, 12690000, 12690000),
    (26, 15, 8, 1, 17990000, 17990000),
    (27, 15, 24, 1, 2890000, 2890000),
    (28, 16, 26, 1, 9990000, 9990000),
    (29, 16, 20, 1, 6990000, 6990000),
    (30, 17, 1, 1, 29990000, 29990000),
    (31, 17, 21, 1, 1990000, 1990000),
    (32, 18, 26, 1, 9990000, 9990000),
    (33, 19, 18, 1, 12690000, 12690000),
    (34, 20, 21, 1, 1990000, 1990000),
    (35, 20, 13, 1, 6290000, 6290000),
    (36, 20, 24, 1, 2890000, 2890000),
    (37, 21, 27, 1, 1590000, 1590000),
    (38, 22, 9, 1, 10990000, 10990000),
    (39, 22, 11, 1, 13490000, 13490000),
    (40, 22, 31, 1, 3490000, 3490000),
    (41, 23, 20, 1, 6990000, 6990000),
    (42, 24, 5, 1, 9490000, 9490000),
    (43, 25, 28, 2, 1890000, 3780000),
    (44, 25, 9, 1, 10990000, 10990000),
    (45, 26, 21, 1, 1990000, 1990000),
    (46, 27, 25, 2, 4990000, 9980000),
    (47, 27, 27, 2, 1590000, 3180000),
    (48, 28, 37, 1, 6490000, 6490000),
    (49, 28, 24, 1, 2890000, 2890000),
    (50, 29, 25, 1, 4990000, 4990000);
INSERT INTO dbo.ChiTietDonHang (MaChiTietDonHang, MaDonHang, MaSanPham, SoLuong, DonGia, ThanhTien) VALUES
    (51, 30, 9, 1, 10990000, 10990000);
SET IDENTITY_INSERT dbo.ChiTietDonHang OFF;
GO

-- ThanhToan: 30 bản ghi
SET IDENTITY_INSERT dbo.ThanhToan ON;
INSERT INTO dbo.ThanhToan (MaThanhToan, MaDonHang, PhuongThucThanhToan, SoTienThanhToan, NgayThanhToan, TrangThaiThanhToan) VALUES
    (1, 1, N'CHUYEN_KHOAN', 12056000, '2026-08-07T15:16:00', N'DA_THANH_TOAN'),
    (2, 2, N'COD', 6152000, '2026-10-14T13:15:00', N'DA_THANH_TOAN'),
    (3, 3, N'COD', 19560000, '2026-08-15T15:17:00', N'DA_THANH_TOAN'),
    (4, 4, N'COD', 9491000, '2026-08-22T14:37:00', N'DA_THANH_TOAN'),
    (5, 5, N'COD', 42970000, '2026-09-22T20:20:00', N'DA_THANH_TOAN'),
    (6, 6, N'COD', 46970000, '2026-10-07T10:58:00', N'DA_THANH_TOAN'),
    (7, 7, N'COD', 21480000, '2026-08-11T10:40:00', N'DA_THANH_TOAN'),
    (8, 8, N'COD', 18980000, '2026-10-07T12:00:00', N'DA_THANH_TOAN'),
    (9, 9, N'COD', 9232000, '2026-10-05T12:51:00', N'DA_THANH_TOAN'),
    (10, 10, N'COD', 6422000, '2026-10-06T21:24:00', N'DA_THANH_TOAN'),
    (11, 11, N'THE', 10526000, '2026-09-13T12:46:00', N'DA_THANH_TOAN'),
    (12, 12, N'COD', 9956000, '2026-08-18T19:13:00', N'DA_THANH_TOAN'),
    (13, 13, N'THE', 35680000, '2026-09-10T10:04:00', N'DA_THANH_TOAN'),
    (14, 14, N'COD', 9490000, '2026-09-24T11:58:00', N'DA_THANH_TOAN'),
    (15, 15, N'THE', 33270000, '2026-09-17T10:43:00', N'DA_THANH_TOAN'),
    (16, 16, N'THE', 16980000, '2026-08-22T08:36:00', N'DA_THANH_TOAN'),
    (17, 17, N'COD', 31980000, '2026-09-21T13:07:00', N'DA_THANH_TOAN'),
    (18, 18, N'COD', 9690000, '2026-09-11T12:23:00', N'DA_THANH_TOAN'),
    (19, 19, N'COD', 12056000, NULL, N'CHUA_THANH_TOAN'),
    (20, 20, N'THE', 11170000, '2026-10-22T14:05:00', N'DA_THANH_TOAN'),
    (21, 21, N'COD', 1590000, NULL, N'CHUA_THANH_TOAN'),
    (22, 22, N'CHUYEN_KHOAN', 24614000, '2026-10-23T08:05:00', N'DA_THANH_TOAN'),
    (23, 23, N'COD', 5942000, NULL, N'CHUA_THANH_TOAN'),
    (24, 24, N'COD', 8067000, NULL, N'CHUA_THANH_TOAN'),
    (25, 25, N'COD', 13293000, NULL, N'CHUA_THANH_TOAN'),
    (26, 26, N'COD', 1740000, NULL, N'CHUA_THANH_TOAN'),
    (27, 27, N'CHUYEN_KHOAN', 12502000, '2026-10-26T19:05:00', N'DA_THANH_TOAN'),
    (28, 28, N'COD', 9380000, NULL, N'CHUA_THANH_TOAN'),
    (29, 29, N'THE', 4990000, '2026-08-13T13:02:00', N'DA_HOAN_TIEN'),
    (30, 30, N'THE', 10441000, '2026-09-10T21:57:00', N'DA_HOAN_TIEN');
SET IDENTITY_INSERT dbo.ThanhToan OFF;
GO

-- YeuCauNhapHang: 25 bản ghi
SET IDENTITY_INSERT dbo.YeuCauNhapHang ON;
INSERT INTO dbo.YeuCauNhapHang (MaYeuCauNhap, MaTaiKhoan, MaNguoiDuyet, NgayYeuCau, TrangThai, NgayDuyet, LyDoTuChoi) VALUES
    (1, 6, 1, '2026-06-05T09:00:00', N'DA_NHAP', '2026-06-05T19:00:00', NULL),
    (2, 3, 1, '2026-06-10T11:00:00', N'DA_NHAP', '2026-06-11T01:00:00', NULL),
    (3, 3, 1, '2026-06-15T08:00:00', N'DA_NHAP', '2026-06-15T22:00:00', NULL),
    (4, 6, 1, '2026-06-20T09:00:00', N'DA_NHAP', '2026-06-20T20:00:00', NULL),
    (5, 5, 1, '2026-06-25T08:00:00', N'DA_NHAP', '2026-06-25T21:00:00', NULL),
    (6, 3, 1, '2026-06-30T11:00:00', N'DA_NHAP', '2026-07-01T04:00:00', NULL),
    (7, 5, 2, '2026-07-05T10:00:00', N'DA_NHAP', '2026-07-05T12:00:00', NULL),
    (8, 3, 1, '2026-07-10T11:00:00', N'DA_NHAP', '2026-07-10T21:00:00', NULL),
    (9, 5, 1, '2026-07-15T10:00:00', N'DA_NHAP', '2026-07-15T20:00:00', NULL),
    (10, 6, 2, '2026-07-20T08:00:00', N'DA_NHAP', '2026-07-20T22:00:00', NULL),
    (11, 4, 1, '2026-07-25T10:00:00', N'DA_NHAP', '2026-07-25T22:00:00', NULL),
    (12, 4, 2, '2026-07-30T09:00:00', N'DA_NHAP', '2026-07-30T13:00:00', NULL),
    (13, 3, 1, '2026-08-04T11:00:00', N'DA_NHAP', '2026-08-04T21:00:00', NULL),
    (14, 4, 1, '2026-08-09T11:00:00', N'DA_NHAP', '2026-08-09T17:00:00', NULL),
    (15, 5, 2, '2026-08-14T10:00:00', N'DA_NHAP', '2026-08-14T23:00:00', NULL),
    (16, 4, 2, '2026-08-19T09:00:00', N'DA_NHAP', '2026-08-20T05:00:00', NULL),
    (17, 4, 1, '2026-08-24T09:00:00', N'DA_NHAP', '2026-08-25T01:00:00', NULL),
    (18, 5, 1, '2026-08-29T11:00:00', N'DA_NHAP', '2026-08-29T22:00:00', NULL),
    (19, 5, 1, '2026-09-03T08:00:00', N'DA_NHAP', '2026-09-04T00:00:00', NULL),
    (20, 5, 1, '2026-09-08T10:00:00', N'DA_NHAP', '2026-09-08T13:00:00', NULL),
    (21, 6, 2, '2026-10-27T09:00:00', N'DA_DUYET', '2026-10-27T16:00:00', NULL),
    (22, 3, NULL, '2026-10-28T09:00:00', N'CHO_DUYET', NULL, NULL),
    (23, 6, NULL, '2026-10-29T09:00:00', N'CHO_DUYET', NULL, NULL),
    (24, 6, 1, '2026-09-28T11:00:00', N'TU_CHOI', '2026-09-29T00:00:00', N'Tồn kho hiện tại còn đủ bán, chưa cần nhập thêm'),
    (25, 6, 1, '2026-10-03T09:00:00', N'TU_CHOI', '2026-10-03T13:00:00', N'Tồn kho hiện tại còn đủ bán, chưa cần nhập thêm');
SET IDENTITY_INSERT dbo.YeuCauNhapHang OFF;
GO

-- ChiTietYeuCauNhapHang: 65 bản ghi
SET IDENTITY_INSERT dbo.ChiTietYeuCauNhapHang ON;
INSERT INTO dbo.ChiTietYeuCauNhapHang (MaChiTietYeuCauNhap, MaYeuCauNhap, MaSanPham, SoLuongYeuCau) VALUES
    (1, 1, 7, 10),
    (2, 1, 40, 10),
    (3, 1, 32, 20),
    (4, 2, 14, 10),
    (5, 2, 2, 20),
    (6, 3, 21, 10),
    (7, 3, 8, 15),
    (8, 3, 6, 10),
    (9, 4, 24, 10),
    (10, 4, 22, 5),
    (11, 4, 29, 5),
    (12, 5, 8, 20),
    (13, 5, 36, 15),
    (14, 5, 14, 15),
    (15, 6, 24, 20),
    (16, 6, 35, 10),
    (17, 7, 16, 5),
    (18, 7, 41, 20),
    (19, 7, 26, 5),
    (20, 8, 5, 15),
    (21, 8, 39, 15),
    (22, 9, 18, 5),
    (23, 9, 20, 5),
    (24, 9, 1, 10),
    (25, 10, 28, 20),
    (26, 10, 32, 10),
    (27, 10, 9, 5),
    (28, 11, 30, 5),
    (29, 11, 24, 10),
    (30, 11, 39, 20),
    (31, 12, 31, 15),
    (32, 12, 36, 10),
    (33, 13, 14, 20),
    (34, 13, 7, 20),
    (35, 14, 30, 10),
    (36, 14, 40, 5),
    (37, 14, 44, 15),
    (38, 15, 17, 10),
    (39, 15, 13, 10),
    (40, 15, 29, 10),
    (41, 16, 21, 20),
    (42, 16, 5, 15),
    (43, 17, 7, 20),
    (44, 17, 1, 10),
    (45, 18, 8, 10),
    (46, 18, 4, 10),
    (47, 19, 43, 15),
    (48, 19, 1, 10),
    (49, 19, 7, 5),
    (50, 20, 17, 10);
INSERT INTO dbo.ChiTietYeuCauNhapHang (MaChiTietYeuCauNhap, MaYeuCauNhap, MaSanPham, SoLuongYeuCau) VALUES
    (51, 20, 3, 5),
    (52, 21, 5, 20),
    (53, 21, 14, 20),
    (54, 21, 3, 5),
    (55, 22, 43, 5),
    (56, 22, 36, 10),
    (57, 22, 10, 20),
    (58, 23, 43, 5),
    (59, 23, 20, 15),
    (60, 23, 27, 15),
    (61, 24, 26, 5),
    (62, 24, 14, 20),
    (63, 25, 37, 10),
    (64, 25, 24, 10),
    (65, 25, 30, 5);
SET IDENTITY_INSERT dbo.ChiTietYeuCauNhapHang OFF;
GO

-- PhieuNhap: 20 bản ghi
SET IDENTITY_INSERT dbo.PhieuNhap ON;
INSERT INTO dbo.PhieuNhap (MaPhieuNhap, MaYeuCauNhap, MaTaiKhoan, NgayNhap, TongTienNhap, GhiChu) VALUES
    (1, 1, 6, '2026-06-07T19:00:00', 252090000, N'Nhập hàng theo yêu cầu đã duyệt'),
    (2, 2, 3, '2026-06-13T01:00:00', 611730000, N'Nhập hàng theo yêu cầu đã duyệt'),
    (3, 3, 3, '2026-06-17T22:00:00', 484185000, N'Nhập hàng theo yêu cầu đã duyệt'),
    (4, 4, 6, '2026-06-22T20:00:00', 59745000, N'Nhập hàng theo yêu cầu đã duyệt'),
    (5, 5, 5, '2026-06-27T21:00:00', 643425000, N'Nhập hàng theo yêu cầu đã duyệt'),
    (6, 6, 3, '2026-07-03T04:00:00', 210530000, N'Nhập hàng theo yêu cầu đã duyệt'),
    (7, 7, 5, '2026-07-07T12:00:00', 128505000, N'Nhập hàng theo yêu cầu đã duyệt'),
    (8, 8, 3, '2026-07-12T21:00:00', 162930000, N'Nhập hàng theo yêu cầu đã duyệt'),
    (9, 9, 5, '2026-07-17T20:00:00', 338545000, N'Nhập hàng theo yêu cầu đã duyệt'),
    (10, 10, 6, '2026-07-22T22:00:00', 115285000, N'Nhập hàng theo yêu cầu đã duyệt'),
    (11, 11, 4, '2026-07-27T22:00:00', 112310000, N'Nhập hàng theo yêu cầu đã duyệt'),
    (12, 12, 4, '2026-08-01T13:00:00', 184650000, N'Nhập hàng theo yêu cầu đã duyệt'),
    (13, 13, 3, '2026-08-06T21:00:00', 450140000, N'Nhập hàng theo yêu cầu đã duyệt'),
    (14, 14, 4, '2026-08-11T17:00:00', 88130000, N'Nhập hàng theo yêu cầu đã duyệt'),
    (15, 15, 5, '2026-08-16T23:00:00', 193530000, N'Nhập hàng theo yêu cầu đã duyệt'),
    (16, 16, 4, '2026-08-22T05:00:00', 154810000, N'Nhập hàng theo yêu cầu đã duyệt'),
    (17, 17, 4, '2026-08-27T01:00:00', 535230000, N'Nhập hàng theo yêu cầu đã duyệt'),
    (18, 18, 5, '2026-08-31T22:00:00', 297320000, N'Nhập hàng theo yêu cầu đã duyệt'),
    (19, 19, 5, '2026-09-06T00:00:00', 332505000, N'Nhập hàng theo yêu cầu đã duyệt'),
    (20, 20, 5, '2026-09-10T13:00:00', 109940000, N'Nhập hàng theo yêu cầu đã duyệt');
SET IDENTITY_INSERT dbo.PhieuNhap OFF;
GO

-- ChiTietPhieuNhap: 51 bản ghi
SET IDENTITY_INSERT dbo.ChiTietPhieuNhap ON;
INSERT INTO dbo.ChiTietPhieuNhap (MaChiTietPhieuNhap, MaPhieuNhap, MaSanPham, SoLuongNhap, DonGiaNhap, ThanhTien) VALUES
    (1, 1, 7, 10, 14016000, 140160000),
    (2, 1, 40, 10, 3901000, 39010000),
    (3, 1, 32, 20, 3646000, 72920000),
    (4, 2, 14, 10, 8491000, 84910000),
    (5, 2, 2, 20, 26341000, 526820000),
    (6, 3, 21, 10, 1691000, 16910000),
    (7, 3, 8, 15, 15291000, 229365000),
    (8, 3, 6, 10, 23791000, 237910000),
    (9, 4, 24, 10, 2456000, 24560000),
    (10, 4, 22, 5, 1946000, 9730000),
    (11, 4, 29, 5, 5091000, 25455000),
    (12, 5, 8, 20, 15291000, 305820000),
    (13, 5, 36, 15, 14016000, 210240000),
    (14, 5, 14, 15, 8491000, 127365000),
    (15, 6, 24, 20, 2456000, 49120000),
    (16, 6, 35, 10, 16141000, 161410000),
    (17, 7, 16, 5, 7046000, 35230000),
    (18, 7, 41, 20, 2541000, 50820000),
    (19, 7, 26, 5, 8491000, 42455000),
    (20, 8, 5, 15, 8066000, 120990000),
    (21, 8, 39, 15, 2796000, 41940000),
    (22, 9, 18, 5, 10786000, 53930000),
    (23, 9, 20, 5, 5941000, 29705000),
    (24, 9, 1, 10, 25491000, 254910000),
    (25, 10, 28, 20, 1606000, 32120000),
    (26, 10, 32, 10, 3646000, 36460000),
    (27, 10, 9, 5, 9341000, 46705000),
    (28, 11, 30, 5, 6366000, 31830000),
    (29, 11, 24, 10, 2456000, 24560000),
    (30, 11, 39, 20, 2796000, 55920000),
    (31, 12, 31, 15, 2966000, 44490000),
    (32, 12, 36, 10, 14016000, 140160000),
    (33, 13, 14, 20, 8491000, 169820000),
    (34, 13, 7, 20, 14016000, 280320000),
    (35, 14, 30, 10, 6366000, 63660000),
    (36, 14, 40, 5, 3901000, 19505000),
    (37, 14, 44, 15, 331000, 4965000),
    (38, 15, 17, 10, 8916000, 89160000),
    (39, 15, 13, 10, 5346000, 53460000),
    (40, 15, 29, 10, 5091000, 50910000),
    (41, 16, 21, 20, 1691000, 33820000),
    (42, 16, 5, 15, 8066000, 120990000),
    (43, 17, 7, 20, 14016000, 280320000),
    (44, 17, 1, 10, 25491000, 254910000),
    (45, 18, 8, 10, 15291000, 152910000),
    (46, 18, 4, 10, 14441000, 144410000),
    (47, 19, 43, 15, 501000, 7515000),
    (48, 19, 1, 10, 25491000, 254910000),
    (49, 19, 7, 5, 14016000, 70080000),
    (50, 20, 17, 10, 8916000, 89160000);
INSERT INTO dbo.ChiTietPhieuNhap (MaChiTietPhieuNhap, MaPhieuNhap, MaSanPham, SoLuongNhap, DonGiaNhap, ThanhTien) VALUES
    (51, 20, 3, 5, 4156000, 20780000);
SET IDENTITY_INSERT dbo.ChiTietPhieuNhap OFF;
GO

-- PhieuXuat: 22 bản ghi
SET IDENTITY_INSERT dbo.PhieuXuat ON;
INSERT INTO dbo.PhieuXuat (MaPhieuXuat, MaDonHang, MaTaiKhoan, NgayXuat, GhiChu) VALUES
    (1, 1, 5, '2026-08-08T17:11:00', NULL),
    (2, 2, 6, '2026-10-12T15:15:00', NULL),
    (3, 3, 3, '2026-08-13T17:17:00', NULL),
    (4, 4, 4, '2026-08-21T16:37:00', NULL),
    (5, 5, 6, '2026-09-21T22:20:00', NULL),
    (6, 6, 3, '2026-10-06T12:58:00', NULL),
    (7, 7, 4, '2026-08-08T12:40:00', NULL),
    (8, 8, 3, '2026-10-04T14:00:00', NULL),
    (9, 9, 6, '2026-10-02T14:51:00', NULL),
    (10, 10, 4, '2026-10-05T23:24:00', NULL),
    (11, 11, 5, '2026-09-14T14:41:00', NULL),
    (12, 12, 4, '2026-08-15T21:13:00', NULL),
    (13, 13, 5, '2026-09-11T11:59:00', NULL),
    (14, 14, 5, '2026-09-21T13:58:00', NULL),
    (15, 15, 3, '2026-09-18T12:38:00', NULL),
    (16, 16, 5, '2026-08-23T10:31:00', NULL),
    (17, 17, 3, '2026-09-20T15:07:00', NULL),
    (18, 18, 6, '2026-09-09T14:23:00', NULL),
    (19, 19, 4, '2026-10-27T14:00:00', NULL),
    (20, 20, 4, '2026-10-23T16:00:00', NULL),
    (21, 21, 5, '2026-10-26T11:00:00', NULL),
    (22, 22, 5, '2026-10-24T10:00:00', NULL);
SET IDENTITY_INSERT dbo.PhieuXuat OFF;
GO

-- ChiTietPhieuXuat: 40 bản ghi
SET IDENTITY_INSERT dbo.ChiTietPhieuXuat ON;
INSERT INTO dbo.ChiTietPhieuXuat (MaChiTietPhieuXuat, MaPhieuXuat, MaSanPham, SoLuongXuat) VALUES
    (1, 1, 18, 1),
    (2, 2, 20, 1),
    (3, 3, 42, 2),
    (4, 3, 13, 1),
    (5, 3, 16, 1),
    (6, 4, 26, 1),
    (7, 5, 10, 1),
    (8, 5, 19, 2),
    (9, 6, 33, 1),
    (10, 6, 37, 1),
    (11, 6, 2, 1),
    (12, 7, 7, 1),
    (13, 7, 25, 1),
    (14, 8, 5, 1),
    (15, 8, 33, 1),
    (16, 9, 17, 1),
    (17, 10, 31, 2),
    (18, 11, 40, 1),
    (19, 11, 37, 1),
    (20, 12, 19, 1),
    (21, 12, 34, 1),
    (22, 13, 2, 1),
    (23, 13, 19, 1),
    (24, 14, 5, 1),
    (25, 15, 18, 1),
    (26, 15, 8, 1),
    (27, 15, 24, 1),
    (28, 16, 26, 1),
    (29, 16, 20, 1),
    (30, 17, 1, 1),
    (31, 17, 21, 1),
    (32, 18, 26, 1),
    (33, 19, 18, 1),
    (34, 20, 21, 1),
    (35, 20, 13, 1),
    (36, 20, 24, 1),
    (37, 21, 27, 1),
    (38, 22, 9, 1),
    (39, 22, 11, 1),
    (40, 22, 31, 1);
SET IDENTITY_INSERT dbo.ChiTietPhieuXuat OFF;
GO

-- DanhGia: 32 bản ghi
SET IDENTITY_INSERT dbo.DanhGia ON;
INSERT INTO dbo.DanhGia (MaDanhGia, MaTaiKhoan, MaSanPham, SoSao, NoiDungDanhGia, NgayDanhGia) VALUES
    (1, 14, 18, 5, N'Đóng gói cẩn thận, hàng chính hãng', '2026-08-12T15:11:00'),
    (2, 21, 20, 3, N'Tạm được, chưa như kỳ vọng', '2026-10-19T13:15:00'),
    (3, 28, 42, 4, N'Hài lòng, sẽ ủng hộ tiếp', '2026-08-20T15:17:00'),
    (4, 28, 13, 5, N'Sản phẩm rất tốt, giao hàng nhanh', '2026-08-18T15:17:00'),
    (5, 28, 16, 4, N'Dùng ổn, giá hợp lý', '2026-08-20T15:17:00'),
    (6, 11, 26, 5, N'Sản phẩm rất tốt, giao hàng nhanh', '2026-08-24T14:37:00'),
    (7, 18, 10, 3, N'Sản phẩm bình thường so với giá', '2026-09-30T20:20:00'),
    (8, 18, 19, 4, N'Sản phẩm tốt, giao hơi chậm một chút', '2026-09-25T20:20:00'),
    (9, 25, 33, 5, N'Chất lượng tuyệt vời, đáng tiền', '2026-10-11T10:58:00'),
    (10, 25, 37, 5, N'Đóng gói cẩn thận, hàng chính hãng', '2026-10-14T10:58:00'),
    (11, 25, 2, 3, N'Tạm được, chưa như kỳ vọng', '2026-10-14T10:58:00'),
    (12, 8, 7, 5, N'Đóng gói cẩn thận, hàng chính hãng', '2026-08-17T10:40:00'),
    (13, 8, 25, 4, N'Hài lòng, sẽ ủng hộ tiếp', '2026-08-14T10:40:00'),
    (14, 15, 5, 4, N'Sản phẩm tốt, giao hơi chậm một chút', '2026-10-08T12:00:00'),
    (15, 15, 33, 4, N'Dùng ổn, giá hợp lý', '2026-10-10T12:00:00'),
    (16, 22, 17, 5, N'Chất lượng tuyệt vời, đáng tiền', '2026-10-07T12:51:00'),
    (17, 29, 31, 5, N'Sản phẩm rất tốt, giao hàng nhanh', '2026-10-09T21:24:00'),
    (18, 12, 40, 4, N'Dùng ổn, giá hợp lý', '2026-09-21T12:41:00'),
    (19, 12, 37, 5, N'Đóng gói cẩn thận, hàng chính hãng', '2026-09-23T12:41:00'),
    (20, 19, 19, 4, N'Dùng ổn, giá hợp lý', '2026-08-21T19:13:00'),
    (21, 19, 34, 2, N'Giao hàng chậm, hộp bị móp', '2026-08-23T19:13:00'),
    (22, 26, 2, 3, N'Sản phẩm bình thường so với giá', '2026-09-18T09:59:00'),
    (23, 26, 19, 4, N'Sản phẩm tốt, giao hơi chậm một chút', '2026-09-17T09:59:00'),
    (24, 9, 5, 4, N'Sản phẩm tốt, giao hơi chậm một chút', '2026-09-28T11:58:00'),
    (25, 16, 18, 2, N'Giao hàng chậm, hộp bị móp', '2026-09-21T10:38:00'),
    (26, 16, 8, 5, N'Đóng gói cẩn thận, hàng chính hãng', '2026-09-24T10:38:00'),
    (27, 16, 24, 2, N'Giao hàng chậm, hộp bị móp', '2026-09-24T10:38:00'),
    (28, 23, 26, 2, N'Giao hàng chậm, hộp bị móp', '2026-09-01T08:31:00'),
    (29, 23, 20, 2, N'Giao hàng chậm, hộp bị móp', '2026-08-26T08:31:00'),
    (30, 30, 1, 5, N'Sản phẩm rất tốt, giao hàng nhanh', '2026-09-25T13:07:00'),
    (31, 30, 21, 3, N'Sản phẩm bình thường so với giá', '2026-09-23T13:07:00'),
    (32, 13, 26, 2, N'Giao hàng chậm, hộp bị móp', '2026-09-12T12:23:00');
SET IDENTITY_INSERT dbo.DanhGia OFF;
GO

-- YeuCauHoTro: 20 bản ghi
SET IDENTITY_INSERT dbo.YeuCauHoTro ON;
INSERT INTO dbo.YeuCauHoTro (MaYeuCauHoTro, MaTaiKhoan, TieuDe, NoiDung, NgayGui, TrangThai, PhanHoi) VALUES
    (1, 12, N'Hỏi về thời gian giao hàng', N'Đơn hàng của tôi bao lâu thì được giao?', '2026-08-09T18:00:00', N'DA_XU_LY', N'Cửa hàng đã liên hệ và hỗ trợ khách hàng qua điện thoại.'),
    (2, 17, N'Không áp dụng được mã giảm giá', N'Tôi nhập mã nhưng hệ thống báo không hợp lệ.', '2026-08-13T10:00:00', N'DA_XU_LY', N'Cửa hàng đã liên hệ và hỗ trợ khách hàng qua điện thoại.'),
    (3, 22, N'Đổi địa chỉ nhận hàng', N'Tôi muốn đổi địa chỉ nhận hàng cho đơn vừa đặt.', '2026-08-17T09:00:00', N'DA_XU_LY', N'Cửa hàng đã liên hệ và hỗ trợ khách hàng qua điện thoại.'),
    (4, 27, N'Hỏi chính sách trả góp', N'Cửa hàng có hỗ trợ trả góp 0% không?', '2026-08-21T19:00:00', N'DA_XU_LY', N'Cửa hàng đã liên hệ và hỗ trợ khách hàng qua điện thoại.'),
    (5, 8, N'Tư vấn chọn laptop', N'Tôi cần laptop cho sinh viên IT, ngân sách khoảng 17 triệu.', '2026-08-25T13:00:00', N'DA_XU_LY', N'Cửa hàng đã liên hệ và hỗ trợ khách hàng qua điện thoại.'),
    (6, 13, N'Hỏi lắp đặt điều hòa', N'Mua điều hòa có được lắp đặt miễn phí không?', '2026-08-29T20:00:00', N'DA_XU_LY', N'Cửa hàng đã liên hệ và hỗ trợ khách hàng qua điện thoại.'),
    (7, 18, N'Sản phẩm còn hàng không', N'Khi nào có hàng trở lại?', '2026-09-02T19:00:00', N'DA_XU_LY', N'Cửa hàng đã liên hệ và hỗ trợ khách hàng qua điện thoại.'),
    (8, 23, N'Xuất hóa đơn VAT', N'Tôi cần xuất hóa đơn công ty cho đơn hàng.', '2026-09-06T16:00:00', N'DA_XU_LY', N'Cửa hàng đã liên hệ và hỗ trợ khách hàng qua điện thoại.'),
    (9, 28, N'Quên mật khẩu', N'Tôi không đăng nhập được, cần lấy lại mật khẩu.', '2026-09-10T09:00:00', N'DA_XU_LY', N'Cửa hàng đã liên hệ và hỗ trợ khách hàng qua điện thoại.'),
    (10, 9, N'Thời gian bảo hành', N'Sản phẩm của tôi bảo hành bao lâu?', '2026-09-14T08:00:00', N'DA_XU_LY', N'Cửa hàng đã liên hệ và hỗ trợ khách hàng qua điện thoại.'),
    (11, 14, N'Hỏi về thời gian giao hàng', N'Đơn hàng của tôi bao lâu thì được giao?', '2026-09-18T20:00:00', N'DA_XU_LY', N'Cửa hàng đã liên hệ và hỗ trợ khách hàng qua điện thoại.'),
    (12, 19, N'Không áp dụng được mã giảm giá', N'Tôi nhập mã nhưng hệ thống báo không hợp lệ.', '2026-09-22T16:00:00', N'DA_XU_LY', N'Cửa hàng đã liên hệ và hỗ trợ khách hàng qua điện thoại.'),
    (13, 24, N'Đổi địa chỉ nhận hàng', N'Tôi muốn đổi địa chỉ nhận hàng cho đơn vừa đặt.', '2026-09-26T14:00:00', N'DA_XU_LY', N'Cửa hàng đã liên hệ và hỗ trợ khách hàng qua điện thoại.'),
    (14, 29, N'Hỏi chính sách trả góp', N'Cửa hàng có hỗ trợ trả góp 0% không?', '2026-09-30T18:00:00', N'DA_XU_LY', N'Cửa hàng đã liên hệ và hỗ trợ khách hàng qua điện thoại.'),
    (15, 10, N'Tư vấn chọn laptop', N'Tôi cần laptop cho sinh viên IT, ngân sách khoảng 17 triệu.', '2026-10-04T20:00:00', N'DANG_XU_LY', NULL),
    (16, 15, N'Hỏi lắp đặt điều hòa', N'Mua điều hòa có được lắp đặt miễn phí không?', '2026-10-08T10:00:00', N'DANG_XU_LY', NULL),
    (17, 20, N'Sản phẩm còn hàng không', N'Khi nào có hàng trở lại?', '2026-10-12T08:00:00', N'DANG_XU_LY', NULL),
    (18, 25, N'Xuất hóa đơn VAT', N'Tôi cần xuất hóa đơn công ty cho đơn hàng.', '2026-10-16T21:00:00', N'MOI', NULL),
    (19, 30, N'Quên mật khẩu', N'Tôi không đăng nhập được, cần lấy lại mật khẩu.', '2026-10-20T09:00:00', N'MOI', NULL),
    (20, 11, N'Thời gian bảo hành', N'Sản phẩm của tôi bảo hành bao lâu?', '2026-10-24T17:00:00', N'MOI', NULL);
SET IDENTITY_INSERT dbo.YeuCauHoTro OFF;
GO

-- PhieuBaoHanh: 20 bản ghi
SET IDENTITY_INSERT dbo.PhieuBaoHanh ON;
INSERT INTO dbo.PhieuBaoHanh (MaPhieuBaoHanh, MaChiTietDonHang, MaTaiKhoan, NgayYeuCau, NgayTiepNhan, MoTaLoi, TrangThaiBaoHanh, KetQuaXuLy) VALUES
    (1, 1, 4, '2026-08-18T15:11:00', '2026-08-19T15:11:00', N'Màn hình xuất hiện sọc', N'HOAN_THANH', N'Đã thay linh kiện, sản phẩm hoạt động bình thường'),
    (2, 18, 4, '2026-10-05T12:41:00', '2026-10-06T12:41:00', N'Không làm lạnh', N'HOAN_THANH', N'Đã thay linh kiện, sản phẩm hoạt động bình thường'),
    (3, 27, 5, '2026-10-26T10:38:00', '2026-10-27T10:38:00', N'Không làm lạnh', N'HOAN_THANH', N'Đã thay linh kiện, sản phẩm hoạt động bình thường'),
    (4, 2, 6, '2026-10-31T13:15:00', '2026-11-01T13:15:00', N'Màn hình xuất hiện sọc', N'HOAN_THANH', N'Đã thay linh kiện, sản phẩm hoạt động bình thường'),
    (5, 31, 5, '2026-10-24T13:07:00', '2026-10-25T13:07:00', N'Máy không lên nguồn', N'HOAN_THANH', N'Đã thay linh kiện, sản phẩm hoạt động bình thường'),
    (6, 21, 3, '2026-09-17T19:13:00', '2026-09-18T19:13:00', N'Không làm lạnh', N'HOAN_THANH', N'Đã thay linh kiện, sản phẩm hoạt động bình thường'),
    (7, 17, 5, '2026-11-10T21:24:00', '2026-11-11T21:24:00', N'Cảm ứng bị loạn', N'HOAN_THANH', N'Đã thay linh kiện, sản phẩm hoạt động bình thường'),
    (8, 4, 3, '2026-09-05T15:17:00', '2026-09-06T15:17:00', N'Pin hao nhanh bất thường', N'HOAN_THANH', N'Đã thay linh kiện, sản phẩm hoạt động bình thường'),
    (9, 13, 6, '2026-09-03T10:40:00', '2026-09-04T10:40:00', N'Không làm lạnh', N'HOAN_THANH', N'Đã thay linh kiện, sản phẩm hoạt động bình thường'),
    (10, 28, 5, '2026-09-26T08:31:00', '2026-09-27T08:31:00', N'Pin hao nhanh bất thường', N'HOAN_THANH', N'Đã thay linh kiện, sản phẩm hoạt động bình thường'),
    (11, 30, 5, '2026-10-11T13:07:00', '2026-10-12T13:07:00', N'Không kết nối được Bluetooth', N'HOAN_THANH', N'Đã thay linh kiện, sản phẩm hoạt động bình thường'),
    (12, 24, 5, '2026-10-04T11:58:00', '2026-10-05T11:58:00', N'Pin hao nhanh bất thường', N'HOAN_THANH', N'Đã thay linh kiện, sản phẩm hoạt động bình thường'),
    (13, 23, 3, '2026-10-14T09:59:00', '2026-10-15T09:59:00', N'Kêu to khi hoạt động', N'DANG_BAO_HANH', NULL),
    (14, 14, 4, '2026-10-20T12:00:00', '2026-10-21T12:00:00', N'Không kết nối được Bluetooth', N'DANG_BAO_HANH', NULL),
    (15, 15, 3, '2026-11-05T12:00:00', '2026-11-06T12:00:00', N'Pin hao nhanh bất thường', N'DANG_BAO_HANH', NULL),
    (16, 19, 5, '2026-10-19T12:41:00', '2026-10-20T12:41:00', N'Pin hao nhanh bất thường', N'DANG_BAO_HANH', NULL),
    (17, 11, NULL, '2026-11-04T10:58:00', NULL, N'Cảm ứng bị loạn', N'CHO_TIEP_NHAN', NULL),
    (18, 25, NULL, '2026-10-15T10:38:00', NULL, N'Không làm lạnh', N'CHO_TIEP_NHAN', NULL),
    (19, 9, 5, '2026-11-12T10:58:00', '2026-11-13T10:58:00', N'Không làm lạnh', N'TU_CHOI', N'Lỗi do người dùng làm rơi vỡ, không thuộc diện bảo hành'),
    (20, 20, 3, '2026-08-24T19:13:00', '2026-08-25T19:13:00', N'Màn hình xuất hiện sọc', N'TU_CHOI', N'Lỗi do người dùng làm rơi vỡ, không thuộc diện bảo hành');
SET IDENTITY_INSERT dbo.PhieuBaoHanh OFF;
GO

-- PhieuDoiTra: 20 bản ghi
SET IDENTITY_INSERT dbo.PhieuDoiTra ON;
INSERT INTO dbo.PhieuDoiTra (MaPhieuDoiTra, MaChiTietDonHang, MaTaiKhoan, NgayYeuCau, NgayTiepNhan, LyDoDoiTra, TrangThaiDoiTra, KetQuaXuLy) VALUES
    (1, 23, 5, '2026-09-14T09:59:00', '2026-09-15T09:59:00', N'Sản phẩm không đúng mô tả', N'HOAN_THANH', N'Đã đổi sản phẩm mới cho khách'),
    (2, 14, 6, '2026-10-09T12:00:00', '2026-10-10T12:00:00', N'Sản phẩm không đúng mô tả', N'HOAN_THANH', N'Đã đổi sản phẩm mới cho khách'),
    (3, 15, 3, '2026-10-08T12:00:00', '2026-10-09T12:00:00', N'Giao sai màu so với đơn đặt', N'HOAN_THANH', N'Đã đổi sản phẩm mới cho khách'),
    (4, 19, 4, '2026-09-19T12:41:00', '2026-09-20T12:41:00', N'Sản phẩm không đúng mô tả', N'HOAN_THANH', N'Đã đổi sản phẩm mới cho khách'),
    (5, 11, 3, '2026-10-08T10:58:00', '2026-10-09T10:58:00', N'Sản phẩm lỗi do nhà sản xuất', N'HOAN_THANH', N'Đã đổi sản phẩm mới cho khách'),
    (6, 25, 5, '2026-09-20T10:38:00', '2026-09-21T10:38:00', N'Hộp bị móp, thiếu phụ kiện', N'HOAN_THANH', N'Đã đổi sản phẩm mới cho khách'),
    (7, 9, 5, '2026-10-08T10:58:00', '2026-10-09T10:58:00', N'Sản phẩm không đúng mô tả', N'HOAN_THANH', N'Đã đổi sản phẩm mới cho khách'),
    (8, 20, 6, '2026-08-18T19:13:00', '2026-08-19T19:13:00', N'Sản phẩm không đúng mô tả', N'HOAN_THANH', N'Đã đổi sản phẩm mới cho khách'),
    (9, 12, 4, '2026-08-12T10:40:00', '2026-08-13T10:40:00', N'Giao sai màu so với đơn đặt', N'HOAN_THANH', N'Đã đổi sản phẩm mới cho khách'),
    (10, 3, 6, '2026-08-17T15:17:00', '2026-08-18T15:17:00', N'Giao sai màu so với đơn đặt', N'HOAN_THANH', N'Đã đổi sản phẩm mới cho khách'),
    (11, 32, 3, '2026-09-12T12:23:00', '2026-09-13T12:23:00', N'Giao sai màu so với đơn đặt', N'HOAN_THANH', N'Đã đổi sản phẩm mới cho khách'),
    (12, 22, 6, '2026-09-14T09:59:00', '2026-09-15T09:59:00', N'Sản phẩm lỗi do nhà sản xuất', N'CHAP_NHAN', N'Đã chấp nhận, chờ khách gửi hàng về'),
    (13, 6, 4, '2026-08-23T14:37:00', '2026-08-24T14:37:00', N'Hộp bị móp, thiếu phụ kiện', N'CHAP_NHAN', N'Đã chấp nhận, chờ khách gửi hàng về'),
    (14, 26, 5, '2026-09-23T10:38:00', '2026-09-24T10:38:00', N'Sản phẩm lỗi do nhà sản xuất', N'CHAP_NHAN', N'Đã chấp nhận, chờ khách gửi hàng về'),
    (15, 10, NULL, '2026-10-08T10:58:00', NULL, N'Sản phẩm không đúng mô tả', N'CHO_TIEP_NHAN', NULL),
    (16, 16, NULL, '2026-10-06T12:51:00', NULL, N'Sản phẩm không đúng mô tả', N'CHO_TIEP_NHAN', NULL),
    (17, 29, NULL, '2026-08-29T08:31:00', NULL, N'Muốn đổi sang model khác', N'CHO_TIEP_NHAN', NULL),
    (18, 5, 6, '2026-08-19T15:17:00', '2026-08-20T15:17:00', N'Giao sai màu so với đơn đặt', N'TU_CHOI', N'Quá thời hạn hoặc không đủ điều kiện đổi trả'),
    (19, 7, 3, '2026-09-24T20:20:00', '2026-09-25T20:20:00', N'Sản phẩm lỗi do nhà sản xuất', N'TU_CHOI', N'Quá thời hạn hoặc không đủ điều kiện đổi trả'),
    (20, 8, 3, '2026-09-23T20:20:00', '2026-09-24T20:20:00', N'Muốn đổi sang model khác', N'TU_CHOI', N'Quá thời hạn hoặc không đủ điều kiện đổi trả');
SET IDENTITY_INSERT dbo.PhieuDoiTra OFF;
GO

/* ===================== KIỂM TRA SỐ BẢN GHI ===================== */
SELECT N'VaiTro' AS Bang, COUNT(*) AS SoBanGhi FROM dbo.VaiTro
UNION ALL
SELECT N'TaiKhoan' AS Bang, COUNT(*) AS SoBanGhi FROM dbo.TaiKhoan
UNION ALL
SELECT N'DanhMuc' AS Bang, COUNT(*) AS SoBanGhi FROM dbo.DanhMuc
UNION ALL
SELECT N'SanPham' AS Bang, COUNT(*) AS SoBanGhi FROM dbo.SanPham
UNION ALL
SELECT N'GioHang' AS Bang, COUNT(*) AS SoBanGhi FROM dbo.GioHang
UNION ALL
SELECT N'ChiTietGioHang' AS Bang, COUNT(*) AS SoBanGhi FROM dbo.ChiTietGioHang
UNION ALL
SELECT N'KhuyenMai' AS Bang, COUNT(*) AS SoBanGhi FROM dbo.KhuyenMai
UNION ALL
SELECT N'DonHang' AS Bang, COUNT(*) AS SoBanGhi FROM dbo.DonHang
UNION ALL
SELECT N'ChiTietDonHang' AS Bang, COUNT(*) AS SoBanGhi FROM dbo.ChiTietDonHang
UNION ALL
SELECT N'ThanhToan' AS Bang, COUNT(*) AS SoBanGhi FROM dbo.ThanhToan
UNION ALL
SELECT N'YeuCauNhapHang' AS Bang, COUNT(*) AS SoBanGhi FROM dbo.YeuCauNhapHang
UNION ALL
SELECT N'ChiTietYeuCauNhapHang' AS Bang, COUNT(*) AS SoBanGhi FROM dbo.ChiTietYeuCauNhapHang
UNION ALL
SELECT N'PhieuNhap' AS Bang, COUNT(*) AS SoBanGhi FROM dbo.PhieuNhap
UNION ALL
SELECT N'ChiTietPhieuNhap' AS Bang, COUNT(*) AS SoBanGhi FROM dbo.ChiTietPhieuNhap
UNION ALL
SELECT N'PhieuXuat' AS Bang, COUNT(*) AS SoBanGhi FROM dbo.PhieuXuat
UNION ALL
SELECT N'ChiTietPhieuXuat' AS Bang, COUNT(*) AS SoBanGhi FROM dbo.ChiTietPhieuXuat
UNION ALL
SELECT N'DanhGia' AS Bang, COUNT(*) AS SoBanGhi FROM dbo.DanhGia
UNION ALL
SELECT N'YeuCauHoTro' AS Bang, COUNT(*) AS SoBanGhi FROM dbo.YeuCauHoTro
UNION ALL
SELECT N'PhieuBaoHanh' AS Bang, COUNT(*) AS SoBanGhi FROM dbo.PhieuBaoHanh
UNION ALL
SELECT N'PhieuDoiTra' AS Bang, COUNT(*) AS SoBanGhi FROM dbo.PhieuDoiTra;
GO