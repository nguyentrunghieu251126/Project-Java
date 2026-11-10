package project.dien_may.project_java.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import project.dien_may.project_java.entity.YeuCauHoTro;

import java.time.LocalDateTime;
import java.util.List;

/** TV3 – KH23. */
public interface YeuCauHoTroRepository extends JpaRepository<YeuCauHoTro, Integer> {

    @Query("""
            SELECT y FROM YeuCauHoTro y
            WHERE y.taiKhoan.email = :email
            ORDER BY y.ngayGui DESC, y.maYeuCauHoTro DESC
            """)
    List<YeuCauHoTro> cuaKhach(@Param("email") String email);

    @Query("SELECT COUNT(y) FROM YeuCauHoTro y WHERE y.taiKhoan.email = :email AND y.ngayGui >= :tu")
    long demTuLuc(@Param("email") String email, @Param("tu") LocalDateTime tu);
}
