
package project.dien_may.project_java.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import project.dien_may.project_java.entity.TaiKhoan;

import java.util.List;
import java.util.Optional;

public interface TaiKhoanRepository
        extends JpaRepository<TaiKhoan, Integer> {

    // Tim tai khoan theo email, khong phan biet chu hoa/thuong
    Optional<TaiKhoan> findByEmailIgnoreCase(String email);

    @EntityGraph(attributePaths = "vaiTro")
    List<TaiKhoan> findByVaiTroTenVaiTroOrderByHoTenAsc(String tenVaiTro);

    // Kiem tra email da ton tai khi dang ky tai khoan.
    @Query("""
            SELECT CASE WHEN COUNT(t) > 0
                        THEN true ELSE false END
            FROM TaiKhoan t
            WHERE LOWER(t.email) = LOWER(:email)
            """)
    boolean daTonTaiEmail(@Param("email") String email);

    boolean existsByEmailIgnoreCaseAndMaTaiKhoanNot(String email, Integer maTaiKhoan);

    @EntityGraph(attributePaths = "vaiTro")
    @Query("""
            SELECT t FROM TaiKhoan t
            WHERE (:tuKhoa IS NULL OR t.hoTen LIKE CONCAT('%', :tuKhoa, '%')
                                   OR t.email LIKE CONCAT('%', :tuKhoa, '%')
                                   OR t.soDienThoai LIKE CONCAT('%', :tuKhoa, '%'))
              AND (:maVaiTro IS NULL OR t.vaiTro.maVaiTro = :maVaiTro)
              AND (:trangThai IS NULL OR t.trangThai = :trangThai)
            """)
    Page<TaiKhoan> timKiem(@Param("tuKhoa") String tuKhoa,
                           @Param("maVaiTro") Integer maVaiTro,
                           @Param("trangThai") Boolean trangThai,
                           Pageable pageable);

    /** Tài khoản đã gắn với chứng từ thì không xóa được (khóa ngoại không cascade), chỉ khóa. */
    @Query(value = """
            SELECT CASE WHEN EXISTS (SELECT 1 FROM dbo.DonHang WHERE MaTaiKhoan = :ma)
                          OR EXISTS (SELECT 1 FROM dbo.YeuCauNhapHang WHERE MaTaiKhoan = :ma OR MaNguoiDuyet = :ma)
                          OR EXISTS (SELECT 1 FROM dbo.PhieuNhap WHERE MaTaiKhoan = :ma)
                          OR EXISTS (SELECT 1 FROM dbo.PhieuXuat WHERE MaTaiKhoan = :ma)
                          OR EXISTS (SELECT 1 FROM dbo.DanhGia WHERE MaTaiKhoan = :ma)
                          OR EXISTS (SELECT 1 FROM dbo.YeuCauHoTro WHERE MaTaiKhoan = :ma)
                          OR EXISTS (SELECT 1 FROM dbo.PhieuBaoHanh WHERE MaTaiKhoan = :ma)
                          OR EXISTS (SELECT 1 FROM dbo.PhieuDoiTra WHERE MaTaiKhoan = :ma)
                        THEN 1 ELSE 0 END
            """, nativeQuery = true)
    int daPhatSinhChungTu(@Param("ma") Integer maTaiKhoan);

    /** Giỏ hàng không phải chứng từ nên xóa cùng tài khoản; ChiTietGioHang tự cascade. */
    @Modifying
    @Query(value = "DELETE FROM dbo.GioHang WHERE MaTaiKhoan = :ma", nativeQuery = true)
    void xoaGioHang(@Param("ma") Integer maTaiKhoan);
}
