package project.dien_may.project_java.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import project.dien_may.project_java.dto.baocao.TonKhoDanhMuc;
import project.dien_may.project_java.entity.SanPham;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface SanPhamRepository
                extends JpaRepository<SanPham, Integer>, JpaSpecificationExecutor<SanPham> {
        List<SanPham> findByDangKinhDoanhTrue();

        Page<SanPham> findByDangKinhDoanhTrue(Pageable pageable);

        List<SanPham> findByDanhMuc_MaDanhMucAndDangKinhDoanhTrue(
                        Integer maDanhMuc);

        List<SanPham> findByTenSanPhamContainingIgnoreCaseAndDangKinhDoanhTrue(
                        String tenSanPham);

        Page<SanPham> findByDanhMuc_MaDanhMucAndDangKinhDoanhTrue(
                        Integer maDanhMuc,
                        Pageable pageable);

        List<SanPham> findTop6ByDanhMuc_MaDanhMucAndDangKinhDoanhTrueAndMaSanPhamNotOrderByMaSanPhamAsc(
                        Integer maDanhMuc, Integer maSanPham);

        List<SanPham> findTop5ByDangKinhDoanhTrueOrderByMaSanPhamDesc();

        List<SanPham> findTop4ByDangKinhDoanhTrueOrderByMaSanPhamDesc();

        List<SanPham> findTop10ByDangKinhDoanhTrueOrderByMaSanPhamDesc();

        @Query(value = "SELECT TOP (10) ct.MaSanPham "
                        + "FROM dbo.ChiTietDonHang ct "
                        + "JOIN dbo.DonHang dh ON dh.MaDonHang = ct.MaDonHang "
                        + "JOIN dbo.SanPham sp ON sp.MaSanPham = ct.MaSanPham "
                        + "WHERE dh.TrangThaiDonHang = 'DA_GIAO' AND sp.DangKinhDoanh = 1 "
                        + "GROUP BY ct.MaSanPham "
                        + "ORDER BY SUM(ct.SoLuong) DESC, ct.MaSanPham ASC", nativeQuery = true)
        List<Integer> layMaSanPhamBanChay();

        @Query("SELECT DISTINCT sp.thuongHieu FROM SanPham sp "
                        + "WHERE sp.dangKinhDoanh = true ORDER BY sp.thuongHieu")
        List<String> layDanhSachThuongHieu();

        @Query("SELECT DISTINCT sp.thuongHieu FROM SanPham sp "
                        + "WHERE sp.dangKinhDoanh = true AND sp.danhMuc.maDanhMuc = :maDanhMuc "
                        + "ORDER BY sp.thuongHieu")
        List<String> layDanhSachThuongHieuTheoDanhMuc(@Param("maDanhMuc") Integer maDanhMuc);

        @Query("SELECT MIN(sp.donGia) FROM SanPham sp WHERE sp.dangKinhDoanh = true")
        BigDecimal layGiaThapNhat();

        @Query("SELECT MAX(sp.donGia) FROM SanPham sp WHERE sp.dangKinhDoanh = true")
        BigDecimal layGiaCaoNhat();

        @EntityGraph(attributePaths = "danhMuc")
        @Query("""
                        SELECT s FROM SanPham s
                        WHERE (:tuKhoa IS NULL OR s.tenSanPham LIKE CONCAT('%', :tuKhoa, '%')
                                               OR s.maModel LIKE CONCAT('%', :tuKhoa, '%'))
                          AND (:maDanhMuc IS NULL OR s.danhMuc.maDanhMuc = :maDanhMuc)
                          AND (:thuongHieu IS NULL OR s.thuongHieu = :thuongHieu)
                          AND (:dangKinhDoanh IS NULL OR s.dangKinhDoanh = :dangKinhDoanh)
                          AND (:tinhTrang IS NULL
                               OR (:tinhTrang = 'HET' AND s.soLuong = 0)
                               OR (:tinhTrang = 'SAP_HET' AND s.soLuong > 0 AND s.soLuong < s.mucTonToiThieu)
                               OR (:tinhTrang = 'DU' AND s.soLuong > 0 AND s.soLuong >= s.mucTonToiThieu))
                        """)
        Page<SanPham> timKiem(@Param("tuKhoa") String tuKhoa,
                        @Param("maDanhMuc") Integer maDanhMuc,
                        @Param("thuongHieu") String thuongHieu,
                        @Param("dangKinhDoanh") Boolean dangKinhDoanh,
                        @Param("tinhTrang") String tinhTrang,
                        Pageable pageable);

        @Query("SELECT DISTINCT s.thuongHieu FROM SanPham s ORDER BY s.thuongHieu")
        List<String> danhSachThuongHieu();

        boolean existsByMaModelIgnoreCase(String maModel);

        boolean existsByMaModelIgnoreCaseAndMaSanPhamNot(String maModel, Integer maSanPham);

        @Query(value = """
                        SELECT CASE WHEN EXISTS (SELECT 1 FROM dbo.ChiTietDonHang WHERE MaSanPham = :ma)
                                      OR EXISTS (SELECT 1 FROM dbo.ChiTietYeuCauNhapHang WHERE MaSanPham = :ma)
                                      OR EXISTS (SELECT 1 FROM dbo.ChiTietPhieuNhap WHERE MaSanPham = :ma)
                                      OR EXISTS (SELECT 1 FROM dbo.ChiTietPhieuXuat WHERE MaSanPham = :ma)
                                      OR EXISTS (SELECT 1 FROM dbo.DanhGia WHERE MaSanPham = :ma)
                                    THEN 1 ELSE 0 END
                        """, nativeQuery = true)
        int daPhatSinhChungTu(@Param("ma") Integer maSanPham);

        @Query("""
                        SELECT new project.dien_may.project_java.dto.baocao.TonKhoDanhMuc(
                                   d.tenDanhMuc, COUNT(s), SUM(s.soLuong), SUM(s.soLuong * s.donGia),
                                   SUM(CASE WHEN s.soLuong < s.mucTonToiThieu THEN 1L ELSE 0L END))
                        FROM SanPham s JOIN s.danhMuc d
                        WHERE s.dangKinhDoanh = true
                        GROUP BY d.tenDanhMuc
                        ORDER BY SUM(s.soLuong * s.donGia) DESC
                        """)
        List<TonKhoDanhMuc> tonKhoTheoDanhMuc();

        @EntityGraph(attributePaths = "danhMuc")
        @Query("""
                        SELECT s FROM SanPham s
                        WHERE s.dangKinhDoanh = true AND s.soLuong < s.mucTonToiThieu
                        ORDER BY s.soLuong ASC, s.tenSanPham ASC
                        """)
        List<SanPham> danhSachCanNhap();

        /** TV4: mọi sản phẩm đang kinh doanh, kèm danh mục, cho trang lập yêu cầu nhập. */
        @EntityGraph(attributePaths = "danhMuc")
        @Query("SELECT s FROM SanPham s WHERE s.dangKinhDoanh = true ORDER BY s.tenSanPham ASC")
        List<SanPham> danhSachDangKinhDoanh();

        @Modifying(flushAutomatically = true, clearAutomatically = true)
        @Query("UPDATE SanPham s SET s.soLuong = s.soLuong + :soLuong WHERE s.maSanPham = :maSanPham")
        int congTonKho(@Param("maSanPham") Integer maSanPham, @Param("soLuong") int soLuong);

        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Query("SELECT s FROM SanPham s WHERE s.maSanPham = :maSanPham")
        Optional<SanPham> khoaTheoMa(@Param("maSanPham") Integer maSanPham);

        Optional<SanPham> findByMaModelIgnoreCase(String maModel);
}
