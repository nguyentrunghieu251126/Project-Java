/* =====================================================================
   TV3 – Phương thức thanh toán mới (ZaloPay, Sacombank, Nam A Bank JCB, Home PayLater, Kredivo)
   và ưu đãi lần đầu theo phương thức. Mỗi tài khoản dùng ưu đãi của một phương thức 1 lần;
   đơn dùng ưu đãi bị hủy thì ưu đãi được trả lại (đơn đã hủy không tính).
   Chạy trên database QLCuaHangDienMay đã có dữ liệu: KHÔNG xóa dữ liệu, chạy lại nhiều lần vẫn an toàn.
   ===================================================================== */
USE QLCuaHangDienMay;
GO
IF COL_LENGTH(N'dbo.ThanhToan', N'UuDaiThanhToan') IS NULL
    ALTER TABLE dbo.ThanhToan ADD UuDaiThanhToan NVARCHAR (200) NULL; -- ưu đãi lần đầu của phương thức đã áp dụng
GO
IF COL_LENGTH(N'dbo.ThanhToan', N'TienGiamThanhToan') IS NULL
    ALTER TABLE dbo.ThanhToan ADD TienGiamThanhToan DECIMAL (18, 0)
        CONSTRAINT DF_ThanhToan_TienGiamThanhToan DEFAULT 0 NOT NULL
        CONSTRAINT CK_ThanhToan_TienGiamThanhToan CHECK (TienGiamThanhToan >= 0);
GO
-- Thêm các phương thức mới vào ràng buộc
IF OBJECT_ID(N'dbo.CK_ThanhToan_PhuongThuc', N'C') IS NOT NULL
    ALTER TABLE dbo.ThanhToan DROP CONSTRAINT CK_ThanhToan_PhuongThuc;
GO
ALTER TABLE dbo.ThanhToan ADD CONSTRAINT CK_ThanhToan_PhuongThuc CHECK (PhuongThucThanhToan IN
    ('COD', 'CHUYEN_KHOAN', 'THE', 'ZALOPAY', 'SACOMBANK', 'NAMA_JCB', 'HOME_PAYLATER', 'KREDIVO'));
GO
-- Tìm nhanh ưu đãi lần đầu khách đã dùng
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'IX_ThanhToan_UuDai' AND object_id = OBJECT_ID(N'dbo.ThanhToan'))
    CREATE INDEX IX_ThanhToan_UuDai ON dbo.ThanhToan (PhuongThucThanhToan, MaDonHang) WHERE TienGiamThanhToan > 0;
GO
SELECT TOP 5 * FROM dbo.ThanhToan ORDER BY MaThanhToan DESC;
