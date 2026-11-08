package project.dien_may.project_java.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import project.dien_may.project_java.entity.TaiKhoanQuyen;

import java.util.List;

public interface TaiKhoanQuyenRepository extends JpaRepository<TaiKhoanQuyen, Integer> {

    @EntityGraph(attributePaths = "quyen")
    List<TaiKhoanQuyen> findByTaiKhoanMaTaiKhoan(Integer maTaiKhoan);

    /** Xóa ngay bằng câu lệnh riêng, vì Hibernate ghi INSERT trước DELETE sẽ vi phạm UQ_TaiKhoan_Quyen. */
    @Modifying
    @Query("DELETE FROM TaiKhoanQuyen t WHERE t.taiKhoan.maTaiKhoan = :ma")
    void xoaTheoTaiKhoan(@Param("ma") Integer maTaiKhoan);
}
