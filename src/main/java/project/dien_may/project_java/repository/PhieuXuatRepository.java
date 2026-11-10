package project.dien_may.project_java.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import project.dien_may.project_java.entity.PhieuXuat;

import java.util.Optional;

public interface PhieuXuatRepository extends JpaRepository<PhieuXuat, Integer> {

    boolean existsByDonHangMaDonHang(Integer maDonHang);

    @EntityGraph(attributePaths = "taiKhoan")
    Optional<PhieuXuat> findByDonHangMaDonHang(Integer maDonHang);
}
