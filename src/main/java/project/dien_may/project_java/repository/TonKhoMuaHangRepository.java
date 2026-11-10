package project.dien_may.project_java.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import project.dien_may.project_java.entity.SanPham;

/**
 * TV3 – trừ và hoàn tồn kho khi khách đặt / hủy đơn.
 * Trừ có điều kiện ngay trên CSDL (WHERE SoLuong >= :soLuong) nên không bán vượt tồn (NFR06).
 * TV5 có thể thay bằng truTonKho() khóa PESSIMISTIC_WRITE mà không đổi phía gọi.
 */
public interface TonKhoMuaHangRepository extends JpaRepository<SanPham, Integer> {

    @Modifying(flushAutomatically = true)
    @Query("""
            UPDATE SanPham s SET s.soLuong = s.soLuong - :soLuong
            WHERE s.maSanPham = :maSanPham AND s.dangKinhDoanh = true AND s.soLuong >= :soLuong
            """)
    int truTonKhoNeuDu(@Param("maSanPham") Integer maSanPham, @Param("soLuong") int soLuong);

    @Modifying(flushAutomatically = true)
    @Query("UPDATE SanPham s SET s.soLuong = s.soLuong + :soLuong WHERE s.maSanPham = :maSanPham")
    int hoanTonKho(@Param("maSanPham") Integer maSanPham, @Param("soLuong") int soLuong);
}
