package project.dien_may.project_java.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import project.dien_may.project_java.entity.SanPham;

import java.math.BigDecimal;
import java.util.List;

public interface SanPhamRepository
        extends JpaRepository<SanPham, Integer>, JpaSpecificationExecutor<SanPham> {

    // Lay danh sach san pham dang kinh doanh
    List<SanPham> findByDangKinhDoanhTrue();
    // Lay san pham co phan trang va sap xep
    Page<SanPham> findByDangKinhDoanhTrue(Pageable pageable);
    // Lay san pham theo danh muc
    List<SanPham> findByDanhMuc_MaDanhMucAndDangKinhDoanhTrue(
            Integer maDanhMuc
    );
    // Tim kiem san pham theo ten
    List<SanPham> findByTenSanPhamContainingIgnoreCaseAndDangKinhDoanhTrue(
            String tenSanPham
    );
    // Lay san pham theo danh muc, co phan trang va sap xep
    Page<SanPham> findByDanhMuc_MaDanhMucAndDangKinhDoanhTrue(
            Integer maDanhMuc,
            Pageable pageable
    );
    // KH07: Goi y san pham cung danh muc, khong bao gom san pham dang xem.
    List<SanPham> findTop6ByDanhMuc_MaDanhMucAndDangKinhDoanhTrueAndMaSanPhamNotOrderByMaSanPhamAsc(Integer maDanhMuc, Integer maSanPham);
    // TV2: san pham moi dua vao danh sach (khong suy dien ngay ra mat tu ma san pham).
    List<SanPham> findTop5ByDangKinhDoanhTrueOrderByMaSanPhamDesc();
    // TV2: bo sung san pham de xuat neu doanh so thuc te chua du 4.
    List<SanPham> findTop4ByDangKinhDoanhTrueOrderByMaSanPhamDesc();
    // TV2: luot ban thuc tu cac don DA_GIAO, khong lay don huy/cho xac nhan.
    @Query(value = "SELECT TOP (4) ct.MaSanPham "
            + "FROM dbo.ChiTietDonHang ct "
            + "JOIN dbo.DonHang dh ON dh.MaDonHang = ct.MaDonHang "
            + "JOIN dbo.SanPham sp ON sp.MaSanPham = ct.MaSanPham "
            + "WHERE dh.TrangThaiDonHang = 'DA_GIAO' AND sp.DangKinhDoanh = 1 "
            + "GROUP BY ct.MaSanPham "
            + "ORDER BY SUM(ct.SoLuong) DESC, ct.MaSanPham ASC", nativeQuery = true)
    List<Integer> layMaSanPhamBanChay();

    // KH09 - Danh sach thuong hieu (khong trung) cua san pham dang kinh doanh
    @Query("SELECT DISTINCT sp.thuongHieu FROM SanPham sp " + "WHERE sp.dangKinhDoanh = true ORDER BY sp.thuongHieu")
    List<String> layDanhSachThuongHieu();

    // KH09 - Thuong hieu (khong trung) trong mot danh muc, dung khi dang chon danh muc
    @Query("SELECT DISTINCT sp.thuongHieu FROM SanPham sp "
            + "WHERE sp.dangKinhDoanh = true AND sp.danhMuc.maDanhMuc = :maDanhMuc "
            + "ORDER BY sp.thuongHieu")
    List<String> layDanhSachThuongHieuTheoDanhMuc(@Param("maDanhMuc") Integer maDanhMuc);

    // KH09 - Gia thap nhat va cao nhat thuc te, dung lam gioi han thanh keo gia
    @Query("SELECT MIN(sp.donGia) FROM SanPham sp WHERE sp.dangKinhDoanh = true")
    BigDecimal layGiaThapNhat();

    @Query("SELECT MAX(sp.donGia) FROM SanPham sp WHERE sp.dangKinhDoanh = true")
    BigDecimal layGiaCaoNhat();
}
