package project.dien_may.project_java.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project.dien_may.project_java.entity.GioHang;

import java.util.Optional;

/** TV3 – KH11. */
public interface GioHangRepository extends JpaRepository<GioHang, Integer> {

    Optional<GioHang> findByTaiKhoanMaTaiKhoan(Integer maTaiKhoan);
}
