package project.dien_may.project_java.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import project.dien_may.project_java.entity.ChiTietDonHang;
import project.dien_may.project_java.entity.DonHang;
import project.dien_may.project_java.entity.TrangThaiDonHang;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DonHangKhachHangRepository extends JpaRepository<DonHang, Integer> {
        @Query("""
                        SELECT d FROM DonHang d
                        WHERE d.taiKhoan.email = :email
                          AND d.trangThai IN :dsTrangThai
                          AND (:tuKhoa IS NULL OR d.maDonHang = :maDon
                               OR EXISTS (SELECT 1 FROM ChiTietDonHang ct
                                          WHERE ct.donHang = d AND ct.sanPham.tenSanPham LIKE CONCAT('%', :tuKhoa, '%')))
                        ORDER BY d.ngayDat DESC, d.maDonHang DESC
                        """)
        Page<DonHang> timCuaKhach(@Param("email") String email,
                        @Param("dsTrangThai") Collection<TrangThaiDonHang> dsTrangThai,
                        @Param("tuKhoa") String tuKhoa,
                        @Param("maDon") Integer maDon,
                        Pageable pageable);

        @Query("SELECT d FROM DonHang d WHERE d.taiKhoan.email = :email ORDER BY d.ngayDat DESC, d.maDonHang DESC")
        List<DonHang> ganDayCuaKhach(@Param("email") String email, Pageable pageable);

        @Query("SELECT d.trangThai, COUNT(d) FROM DonHang d WHERE d.taiKhoan.email = :email GROUP BY d.trangThai")
        List<Object[]> demTheoTrangThai(@Param("email") String email);

        @Query("""
                        SELECT d FROM DonHang d LEFT JOIN FETCH d.khuyenMai
                        WHERE d.maDonHang = :maDon AND d.taiKhoan.email = :email
                        """)
        Optional<DonHang> timCuaKhach(@Param("maDon") Integer maDon, @Param("email") String email);

        @Query("""
                        SELECT ct FROM ChiTietDonHang ct JOIN FETCH ct.sanPham
                        WHERE ct.donHang.maDonHang IN :dsMaDon
                        ORDER BY ct.maChiTietDonHang
                        """)
        List<ChiTietDonHang> chiTietCuaCacDon(@Param("dsMaDon") Collection<Integer> dsMaDon);

        @Modifying(flushAutomatically = true)
        @Query("""
                        UPDATE DonHang d SET d.trangThai = :daHuy
                        WHERE d.maDonHang = :maDon AND d.trangThai = :choXacNhan
                        """)
        int huyNeuDangCho(@Param("maDon") Integer maDon,
                        @Param("choXacNhan") TrangThaiDonHang choXacNhan,
                        @Param("daHuy") TrangThaiDonHang daHuy);

        @Query("""
                        SELECT ct FROM ChiTietDonHang ct JOIN FETCH ct.sanPham JOIN FETCH ct.donHang d
                        WHERE d.taiKhoan.email = :email AND d.trangThai = :daGiao
                        ORDER BY d.ngayGiao DESC, d.maDonHang DESC
                        """)
        List<ChiTietDonHang> daGiaoCuaKhach(@Param("email") String email, @Param("daGiao") TrangThaiDonHang daGiao,
                        Pageable pageable);
}
