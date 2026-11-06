package project.dien_may.project_java.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project.dien_may.project_java.entity.DanhMuc;

import java.util.List;

public interface DanhMucRepository extends JpaRepository<DanhMuc, Integer> {

    List<DanhMuc> findAllByOrderByTenDanhMucAsc();
}
