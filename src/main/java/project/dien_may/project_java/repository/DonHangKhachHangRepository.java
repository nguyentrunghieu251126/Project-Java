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

/**
 * TV3 – KH16 – KH19: đơn hàng nhìn từ phía khách hàng.
 * Tách khỏi DonHangRepository (khu quản trị) để mỗi bên tự quản lý câu truy vấn của mình.
 */
public interface DonHangKhachHangRepository extends JpaRepository<DonHang, Integer> {

    /** dsTrangThai luôn khác rỗng (xem "Tất cả" thì truyền mọi trạng thái); maDon chỉ khác null khi từ khóa là số. */
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

    /** Chỉ hủy được khi đơn còn chờ xác nhận; trả về 0 nếu kho vừa xác nhận đơn trước đó. */
    @Modifying(flushAutomatically = true)
    @Query("""
            UPDATE DonHang d SET d.trangThai = :daHuy
            WHERE d.maDonHang = :maDon AND d.trangThai = :choXacNhan
            """)
    int huyNeuDangCho(@Param("maDon") Integer maDon,
                      @Param("choXacNhan") TrangThaiDonHang choXacNhan,
                      @Param("daHuy") TrangThaiDonHang daHuy);
}
