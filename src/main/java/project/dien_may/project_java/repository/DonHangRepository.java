package project.dien_may.project_java.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import project.dien_may.project_java.dto.baocao.DemTrangThaiDon;
import project.dien_may.project_java.dto.baocao.DoanhThuNgay;
import project.dien_may.project_java.entity.DonHang;
import project.dien_may.project_java.entity.TaiKhoan;
import project.dien_may.project_java.entity.TrangThaiDonHang;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface DonHangRepository extends JpaRepository<DonHang, Integer> {
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

        @Modifying(flushAutomatically = true, clearAutomatically = true)
        @Query("""
                        UPDATE DonHang d SET d.trangThai = :trangThaiMoi
                        WHERE d.maDonHang = :ma AND d.trangThai = :trangThaiCu
                        """)
        int doiTrangThai(@Param("ma") Integer maDonHang,
                        @Param("trangThaiCu") TrangThaiDonHang trangThaiCu,
                        @Param("trangThaiMoi") TrangThaiDonHang trangThaiMoi);

        // ===== TV4: nhân viên giao hàng (NVGH01 – NVGH04) =====

        /** Đơn đã xuất kho, chưa có nhân viên giao hàng nào nhận. */
        @Query("""
                        SELECT d FROM DonHang d
                        WHERE d.trangThai = project.dien_may.project_java.entity.TrangThaiDonHang.DA_XUAT_KHO
                          AND d.nhanVienGiao IS NULL
                        """)
        Page<DonHang> donChoNhanGiao(Pageable pageable);

        @Query("""
                        SELECT d FROM DonHang d
                        WHERE d.nhanVienGiao = :nhanVien AND d.trangThai IN :dsTrangThai
                        """)
        Page<DonHang> donCuaNhanVienGiao(@Param("nhanVien") TaiKhoan nhanVien,
                        @Param("dsTrangThai") Collection<TrangThaiDonHang> dsTrangThai,
                        Pageable pageable);

        long countByTrangThaiAndNhanVienGiaoIsNull(TrangThaiDonHang trangThai);

        long countByNhanVienGiaoAndTrangThai(TaiKhoan nhanVien, TrangThaiDonHang trangThai);

        /** Nhận đơn: chỉ thành công khi đơn vẫn "Đã xuất kho" và chưa ai nhận (hai người bấm cùng lúc thì một người được). */
        @Modifying(flushAutomatically = true, clearAutomatically = true)
        @Query("""
                        UPDATE DonHang d SET d.trangThai = project.dien_may.project_java.entity.TrangThaiDonHang.DANG_GIAO,
                                             d.nhanVienGiao = :nhanVien
                        WHERE d.maDonHang = :ma
                          AND d.trangThai = project.dien_may.project_java.entity.TrangThaiDonHang.DA_XUAT_KHO
                          AND d.nhanVienGiao IS NULL
                        """)
        int nhanDonGiao(@Param("ma") Integer maDonHang, @Param("nhanVien") TaiKhoan nhanVien);

        /** Đã giao: chỉ người đang giao đơn đó mới xác nhận được; ghi ngày giao làm mốc bảo hành, đổi trả. */
        @Modifying(flushAutomatically = true, clearAutomatically = true)
        @Query("""
                        UPDATE DonHang d SET d.trangThai = project.dien_may.project_java.entity.TrangThaiDonHang.DA_GIAO,
                                             d.ngayGiao = :ngayGiao
                        WHERE d.maDonHang = :ma
                          AND d.trangThai = project.dien_may.project_java.entity.TrangThaiDonHang.DANG_GIAO
                          AND d.nhanVienGiao = :nhanVien
                        """)
        int xacNhanDaGiao(@Param("ma") Integer maDonHang, @Param("nhanVien") TaiKhoan nhanVien,
                        @Param("ngayGiao") LocalDateTime ngayGiao);

        /** Giao thất bại: đơn chuyển sang "Đã hủy" kèm lý do; hàng được hoàn kho ở tầng service. */
        @Modifying(flushAutomatically = true, clearAutomatically = true)
        @Query("""
                        UPDATE DonHang d SET d.trangThai = project.dien_may.project_java.entity.TrangThaiDonHang.DA_HUY,
                                             d.lyDoGiaoThatBai = :lyDo
                        WHERE d.maDonHang = :ma
                          AND d.trangThai = project.dien_may.project_java.entity.TrangThaiDonHang.DANG_GIAO
                          AND d.nhanVienGiao = :nhanVien
                        """)
        int baoGiaoThatBai(@Param("ma") Integer maDonHang, @Param("nhanVien") TaiKhoan nhanVien,
                        @Param("lyDo") String lyDo);
}
