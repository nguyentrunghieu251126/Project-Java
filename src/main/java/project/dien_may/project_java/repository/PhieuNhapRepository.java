package project.dien_may.project_java.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import project.dien_may.project_java.entity.PhieuNhap;

import java.util.Optional;

public interface PhieuNhapRepository extends JpaRepository<PhieuNhap, Integer> {

    boolean existsByYeuCauMaYeuCauNhap(Integer maYeuCauNhap);

    @EntityGraph(attributePaths = {"taiKhoan", "chiTiet", "chiTiet.sanPham"})
    Optional<PhieuNhap> findFirstByYeuCauMaYeuCauNhap(Integer maYeuCauNhap);
}
