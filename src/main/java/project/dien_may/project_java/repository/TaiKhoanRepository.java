
package project.dien_may.project_java.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import project.dien_may.project_java.entity.TaiKhoan;

import java.util.Optional;

public interface TaiKhoanRepository
        extends JpaRepository<TaiKhoan, Integer> {

    // Tim tai khoan theo email, khong phan biet chu hoa/thuong
    Optional<TaiKhoan> findByEmailIgnoreCase(String email);

    // Kiem tra email da ton tai khi dang ky tai khoan.
    @Query("""
            SELECT CASE WHEN COUNT(t) > 0
                        THEN true ELSE false END
            FROM TaiKhoan t
            WHERE LOWER(t.email) = LOWER(:email)
            """)
    boolean daTonTaiEmail(@Param("email") String email);
}
