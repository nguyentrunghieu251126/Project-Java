package project.dien_may.project_java.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import project.dien_may.project_java.dto.baocao.DemTrangThaiDon;
import project.dien_may.project_java.dto.baocao.DoanhThuNgay;
import project.dien_may.project_java.entity.DonHang;
import project.dien_may.project_java.entity.TrangThaiDonHang;

import java.time.LocalDateTime;
import java.util.List;

public interface DonHangRepository extends JpaRepository<DonHang, Integer> {

    /** maDon chỉ khác null khi từ khóa là số, để tra cứu đúng theo mã đơn. */
    @Query("""
            SELECT d FROM DonHang d
            WHERE (:tuKhoa IS NULL OR d.hoTenNguoiNhan LIKE CONCAT('%', :tuKhoa, '%')
                                   OR d.soDienThoaiNguoiNhan LIKE CONCAT('%', :tuKhoa, '%')
                                   OR d.maDonHang = :maDon)
              AND (:trangThai IS NULL OR d.trangThai = :trangThai)
              AND (:tu IS NULL OR d.ngayDat >= :tu)
              AND (:den IS NULL OR d.ngayDat < :den)
            """)
    Page<DonHang> timKiem(@Param("tuKhoa") String tuKhoa,
                          @Param("maDon") Integer maDon,
                          @Param("trangThai") TrangThaiDonHang trangThai,
                          @Param("tu") LocalDateTime tu,
                          @Param("den") LocalDateTime den,
                          Pageable pageable);

    @Query("""
            SELECT new project.dien_may.project_java.dto.baocao.DemTrangThaiDon(d.trangThai, COUNT(d))
            FROM DonHang d GROUP BY d.trangThai
            """)
    List<DemTrangThaiDon> demTheoTrangThai();

    @Query("SELECT MIN(YEAR(d.ngayDat)) FROM DonHang d")
    Integer namDauTien();

    boolean existsByKhuyenMaiMaKhuyenMai(Integer maKhuyenMai);

    /** Doanh thu chỉ tính đơn đã giao; [tu, den) theo ngày đặt, TongTien đã trừ giảm giá. */
    @Query("""
            SELECT new project.dien_may.project_java.dto.baocao.DoanhThuNgay(
                       YEAR(d.ngayDat), MONTH(d.ngayDat), DAY(d.ngayDat),
                       COUNT(d), SUM(d.tienGiam), SUM(d.tongTien))
            FROM DonHang d
            WHERE d.trangThai = :trangThai AND d.ngayDat >= :tu AND d.ngayDat < :den
            GROUP BY YEAR(d.ngayDat), MONTH(d.ngayDat), DAY(d.ngayDat)
            """)
    List<DoanhThuNgay> doanhThuTheoNgay(@Param("trangThai") TrangThaiDonHang trangThai,
                                        @Param("tu") LocalDateTime tu,
                                        @Param("den") LocalDateTime den);
}
