package project.dien_may.project_java.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import project.dien_may.project_java.dto.baocao.TonKhoDanhMuc;
import project.dien_may.project_java.entity.SanPham;

import java.util.List;

public interface SanPhamRepository extends JpaRepository<SanPham, Integer> {

    /** Bộ lọc nào truyền null thì bỏ qua. tinhTrang nhận tên của TinhTrangTonKho. */
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

    /** Sản phẩm đã có trong chứng từ thì không xóa cứng được (khóa ngoại không cascade). */
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
}
