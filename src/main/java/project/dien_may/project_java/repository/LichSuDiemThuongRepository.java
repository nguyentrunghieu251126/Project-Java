package project.dien_may.project_java.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import project.dien_may.project_java.entity.LichSuDiemThuong;
import project.dien_may.project_java.entity.LoaiGiaoDichDiem;

import java.util.List;

/** TV3 – điểm thưởng. */
public interface LichSuDiemThuongRepository extends JpaRepository<LichSuDiemThuong, Integer> {

    /** Số điểm hiện có = tổng cộng − tổng trừ. */
    @Query("""
            SELECT COALESCE(SUM(CASE WHEN l.loaiGiaoDich = project.dien_may.project_java.entity.LoaiGiaoDichDiem.CONG
                                     THEN l.soDiem ELSE -l.soDiem END), 0)
            FROM LichSuDiemThuong l WHERE l.taiKhoan.email = :email
            """)
    long tongDiem(@Param("email") String email);

    /** loai null = tất cả; CONG / TRU = chỉ cộng / chỉ trừ. */
    @Query(value = """
            SELECT l FROM LichSuDiemThuong l LEFT JOIN FETCH l.donHang
            WHERE l.taiKhoan.email = :email AND (:loai IS NULL OR l.loaiGiaoDich = :loai)
            ORDER BY l.ngayGiaoDich DESC, l.maLichSu DESC
            """,
            countQuery = """
            SELECT COUNT(l) FROM LichSuDiemThuong l
            WHERE l.taiKhoan.email = :email AND (:loai IS NULL OR l.loaiGiaoDich = :loai)
            """)
    Page<LichSuDiemThuong> cuaKhach(@Param("email") String email, @Param("loai") LoaiGiaoDichDiem loai,
                                    Pageable pageable);

    /** Mỗi dòng: [LoaiGiaoDichDiem, tổng số điểm, số lần]. */
    @Query("""
            SELECT l.loaiGiaoDich, COALESCE(SUM(l.soDiem), 0), COUNT(l) FROM LichSuDiemThuong l
            WHERE l.taiKhoan.email = :email GROUP BY l.loaiGiaoDich
            """)
    List<Object[]> thongKe(@Param("email") String email);

    @Query("SELECT l FROM LichSuDiemThuong l WHERE l.donHang.maDonHang = :maDon ORDER BY l.maLichSu")
    List<LichSuDiemThuong> theoDon(@Param("maDon") Integer maDon);

    /**
     * Khóa dòng tài khoản đến hết giao dịch: hai đơn đặt cùng lúc không dùng trùng một số điểm.
     */
    @Query(value = "SELECT MaTaiKhoan FROM dbo.TaiKhoan WITH (UPDLOCK, ROWLOCK) WHERE MaTaiKhoan = :ma", nativeQuery = true)
    Integer khoaTaiKhoan(@Param("ma") Integer maTaiKhoan);
}
