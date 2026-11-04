package project.dien_may.project_java.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import project.dien_may.project_java.entity.TaiKhoan;
import project.dien_may.project_java.entity.TrangThaiYeuCauNhap;
import project.dien_may.project_java.entity.YeuCauNhapHang;

import java.time.LocalDateTime;

public interface YeuCauNhapHangRepository extends JpaRepository<YeuCauNhapHang, Integer> {

    @EntityGraph(attributePaths = {"taiKhoan", "nguoiDuyet"})
    @Query("SELECT y FROM YeuCauNhapHang y WHERE :trangThai IS NULL OR y.trangThai = :trangThai")
    Page<YeuCauNhapHang> timKiem(@Param("trangThai") TrangThaiYeuCauNhap trangThai, Pageable pageable);

    long countByTrangThai(TrangThaiYeuCauNhap trangThai);

    /**
     * Chỉ cập nhật khi yêu cầu còn chờ duyệt; trả về 0 nghĩa là đã có người xử lý trước,
     * nhờ vậy hai quản trị viên thao tác cùng lúc không ghi đè kết quả của nhau.
     */
    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE YeuCauNhapHang y
            SET y.trangThai = :trangThai, y.nguoiDuyet = :nguoiDuyet,
                y.ngayDuyet = :ngayDuyet, y.lyDoTuChoi = :lyDoTuChoi
            WHERE y.maYeuCauNhap = :ma
              AND y.trangThai = project.dien_may.project_java.entity.TrangThaiYeuCauNhap.CHO_DUYET
            """)
    int xuLy(@Param("ma") Integer maYeuCauNhap,
             @Param("trangThai") TrangThaiYeuCauNhap trangThai,
             @Param("nguoiDuyet") TaiKhoan nguoiDuyet,
             @Param("ngayDuyet") LocalDateTime ngayDuyet,
             @Param("lyDoTuChoi") String lyDoTuChoi);
}
