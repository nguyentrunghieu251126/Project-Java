
package project.dien_may.project_java.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project.dien_may.project_java.entity.VaiTro;

import java.util.Optional;

public interface VaiTroRepository extends JpaRepository<VaiTro, Integer> {

    // Tìm vai trò theo tên
    Optional<VaiTro> findByTenVaiTro(String tenVaiTro);
}
