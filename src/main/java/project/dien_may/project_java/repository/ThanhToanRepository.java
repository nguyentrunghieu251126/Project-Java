package project.dien_may.project_java.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import project.dien_may.project_java.entity.PhuongThucThanhToan;
import project.dien_may.project_java.entity.ThanhToan;

import java.util.List;
import java.util.Optional;

/** TV3 – KH14. */
public interface ThanhToanRepository extends JpaRepository<ThanhToan, Integer> {

    Optional<ThanhToan> findFirstByDonHangMaDonHangOrderByMaThanhToanDesc(Integer maDonHang);

    /**
     * Phương thức khách đã dùng ưu đãi lần đầu. Đơn đã hủy không tính, nên hủy đơn là ưu đãi được trả lại.
     */
    @Query("""
            SELECT DISTINCT t.phuongThuc FROM ThanhToan t
            WHERE t.donHang.taiKhoan.email = :email AND t.tienGiamUuDai > 0
              AND t.donHang.trangThai <> project.dien_may.project_java.entity.TrangThaiDonHang.DA_HUY
            """)
    List<PhuongThucThanhToan> daDungUuDai(@Param("email") String email);
}
