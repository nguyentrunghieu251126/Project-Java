
USE QLCuaHangDienMay;


GO
IF OBJECT_ID(N'dbo.SoDiaChi', N'U') IS NULL
    CREATE TABLE dbo.SoDiaChi (
        MaDiaChi      INT            IDENTITY (1, 1) NOT NULL,
        MaTaiKhoan    INT            NOT NULL,
        TenNguoiNhan  NVARCHAR (100) NOT NULL,
        SoDienThoai   VARCHAR (15)   NOT NULL,
        KhuVuc        NVARCHAR (50)  NOT NULL, -- tỉnh / thành phố
        DiaChiChiTiet NVARCHAR (200) NOT NULL, -- số nhà, đường, phường/xã
        LoaiDiaChi    NVARCHAR (20)  CONSTRAINT DF_SoDiaChi_Loai DEFAULT N'Nhà riêng' NOT NULL,
        LaMacDinh     BIT            CONSTRAINT DF_SoDiaChi_MacDinh DEFAULT 0 NOT NULL,
        NgayTao       DATETIME2 (0)  CONSTRAINT DF_SoDiaChi_NgayTao DEFAULT SYSDATETIME() NOT NULL,
        CONSTRAINT PK_SoDiaChi PRIMARY KEY (MaDiaChi),
        -- Xóa tài khoản thì xóa luôn sổ địa chỉ (sổ địa chỉ không phải chứng từ)
        CONSTRAINT FK_SoDiaChi_TaiKhoan FOREIGN KEY (MaTaiKhoan) REFERENCES dbo.TaiKhoan (MaTaiKhoan) ON DELETE CASCADE,
        CONSTRAINT CK_SoDiaChi_Loai CHECK (LoaiDiaChi IN (N'Nhà riêng', N'Văn phòng'))
    );
GO

-- Mỗi tài khoản chỉ có tối đa một địa chỉ mặc định
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'UX_SoDiaChi_MacDinh')
    CREATE UNIQUE INDEX UX_SoDiaChi_MacDinh ON dbo.SoDiaChi (MaTaiKhoan) WHERE LaMacDinh = 1;
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'IX_SoDiaChi_MaTaiKhoan')
    CREATE INDEX IX_SoDiaChi_MaTaiKhoan ON dbo.SoDiaChi (MaTaiKhoan);
GO

/* Dữ liệu mẫu: chép địa chỉ trong hồ sơ của khách hàng (có đủ SĐT + địa chỉ) thành địa chỉ mặc định.
   "431 Nguyễn Trãi, Quận 10, TP. Hồ Chí Minh" -> chi tiết "431 Nguyễn Trãi, Quận 10", khu vực "TP. Hồ Chí Minh". */
INSERT INTO dbo.SoDiaChi (MaTaiKhoan, TenNguoiNhan, SoDienThoai, KhuVuc, DiaChiChiTiet, LoaiDiaChi, LaMacDinh)
SELECT t.MaTaiKhoan,
       t.HoTen,
       t.SoDienThoai,
       LTRIM(RIGHT(t.DiaChi, CHARINDEX(',', REVERSE(t.DiaChi)) - 1)),
       LTRIM(RTRIM(LEFT(t.DiaChi, LEN(t.DiaChi) - CHARINDEX(',', REVERSE(t.DiaChi))))),
       N'Nhà riêng',
       1
FROM dbo.TaiKhoan t
         JOIN dbo.VaiTro v ON v.MaVaiTro = t.MaVaiTro
WHERE v.TenVaiTro = 'KHACH_HANG'
  AND t.SoDienThoai IS NOT NULL
  AND t.DiaChi IS NOT NULL
  AND CHARINDEX(',', t.DiaChi) > 0
  AND NOT EXISTS (SELECT 1 FROM dbo.SoDiaChi s WHERE s.MaTaiKhoan = t.MaTaiKhoan);
GO

SELECT COUNT(*) AS SoDiaChiDaCo FROM dbo.SoDiaChi;
GO
