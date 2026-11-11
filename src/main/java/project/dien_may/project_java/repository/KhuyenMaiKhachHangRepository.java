package project.dien_may.project_java.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import project.dien_may.project_java.entity.KhuyenMai;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** TV3 – KH15: khách áp dụng mã giảm giá. Điều kiện "dùng được" phải khớp KhuyenMai.getTinhTrang(). */
public interface KhuyenMaiKhachHangRepository extends JpaRepository<KhuyenMai, Integer> {

    Optional<KhuyenMai> findByMaGiamGiaIgnoreCase(String maGiamGia);

    /** Mã đang chạy: đang bật, trong thời hạn, còn lượt. */
    @Query("""
            SELECT k FROM KhuyenMai k
            WHERE k.trangThai = true AND k.ngayBatDau <= :homNay AND k.ngayKetThuc >= :homNay
              AND (k.soLuotToiDa IS NULL OR k.soLuotDaDung < k.soLuotToiDa)
            ORDER BY k.ngayKetThuc ASC
            """)
    List<KhuyenMai> dangChay(@Param("homNay") LocalDate homNay);

    /** Mã đang bật nhưng chưa tới ngày, hiển thị mờ để khách biết sắp có. */
    @Query("""
            SELECT k FROM KhuyenMai k
            WHERE k.trangThai = true AND k.ngayBatDau > :homNay AND k.ngayBatDau <= :denNgay
            ORDER BY k.ngayBatDau ASC
            """)
    List<KhuyenMai> sapDienRa(@Param("homNay") LocalDate homNay, @Param("denNgay") LocalDate denNgay);

    /** Mã đang bật nhưng đã hết hạn (trong khoảng gần đây) hoặc đã dùng hết lượt: hiển thị mờ ở cuối danh sách. */
    @Query("""
            SELECT k FROM KhuyenMai k
            WHERE k.trangThai = true
              AND ((k.ngayKetThuc < :homNay AND k.ngayKetThuc >= :tuNgay)
                OR (k.ngayBatDau <= :homNay AND k.ngayKetThuc >= :homNay
                    AND k.soLuotToiDa IS NOT NULL AND k.soLuotDaDung >= k.soLuotToiDa))
            ORDER BY k.ngayKetThuc DESC
            """)
    List<KhuyenMai> hetHanHoacHetLuot(@Param("homNay") LocalDate homNay, @Param("tuNgay") LocalDate tuNgay);

    /** Trừ lượt có điều kiện ngay trên CSDL: hai đơn cùng lúc không dùng quá số lượt tối đa. */
    @Modifying(flushAutomatically = true)
    @Query("""
            UPDATE KhuyenMai k SET k.soLuotDaDung = k.soLuotDaDung + 1
            WHERE k.maKhuyenMai = :ma AND k.trangThai = true
              AND k.ngayBatDau <= :homNay AND k.ngayKetThuc >= :homNay
              AND (k.soLuotToiDa IS NULL OR k.soLuotDaDung < k.soLuotToiDa)
            """)
    int dungMotLuot(@Param("ma") Integer maKhuyenMai, @Param("homNay") LocalDate homNay);

    /** Hoàn lượt khi khách hủy đơn. */
    @Modifying(flushAutomatically = true)
    @Query("UPDATE KhuyenMai k SET k.soLuotDaDung = k.soLuotDaDung - 1 WHERE k.maKhuyenMai = :ma AND k.soLuotDaDung > 0")
    int hoanMotLuot(@Param("ma") Integer maKhuyenMai);
}
