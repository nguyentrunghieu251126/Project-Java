package project.dien_may.project_java.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import project.dien_may.project_java.entity.SoDiaChi;

import java.util.List;
import java.util.Optional;

public interface SoDiaChiRepository extends JpaRepository<SoDiaChi, Integer> {
        @Query("""
                        SELECT d FROM SoDiaChi d
                        WHERE d.taiKhoan.email = :email
                        ORDER BY d.laMacDinh DESC, d.maDiaChi ASC
                        """)
        List<SoDiaChi> cuaKhach(@Param("email") String email);

        @Query("SELECT d FROM SoDiaChi d WHERE d.maDiaChi = :ma AND d.taiKhoan.email = :email")
        Optional<SoDiaChi> timCuaKhach(@Param("ma") Integer maDiaChi, @Param("email") String email);

        @Query("SELECT COUNT(d) FROM SoDiaChi d WHERE d.taiKhoan.email = :email")
        long demCuaKhach(@Param("email") String email);

        @Modifying(flushAutomatically = true, clearAutomatically = true)
        @Query("""
                        UPDATE SoDiaChi d SET d.laMacDinh = false
                        WHERE d.taiKhoan.maTaiKhoan = :maTaiKhoan AND d.laMacDinh = true
                          AND (:giuLai IS NULL OR d.maDiaChi <> :giuLai)
                        """)
        int boMacDinh(@Param("maTaiKhoan") Integer maTaiKhoan, @Param("giuLai") Integer giuLai);
}
