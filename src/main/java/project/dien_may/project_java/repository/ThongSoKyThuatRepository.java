package project.dien_may.project_java.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project.dien_may.project_java.entity.ThongSoKyThuat;
import java.util.List;

public interface ThongSoKyThuatRepository extends JpaRepository<ThongSoKyThuat, Integer> {

    List<ThongSoKyThuat> findBySanPham_MaSanPhamOrderByThuTuAscMaThongSoAsc(Integer maSanPham);
}
