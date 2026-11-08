package project.dien_may.project_java.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import project.dien_may.project_java.entity.ChiTietYeuCauNhapHang;
import project.dien_may.project_java.entity.TrangThaiYeuCauNhap;

import java.util.List;
import java.util.Set;

public interface ChiTietYeuCauNhapHangRepository extends JpaRepository<ChiTietYeuCauNhapHang, Integer> {

    boolean existsBySanPhamMaSanPhamAndYeuCauTrangThai(Integer maSanPham, TrangThaiYeuCauNhap trangThai);

    @EntityGraph(attributePaths = "sanPham")
    List<ChiTietYeuCauNhapHang> findByYeuCauMaYeuCauNhap(Integer maYeuCauNhap);

    @Query("SELECT DISTINCT c.sanPham.maSanPham FROM ChiTietYeuCauNhapHang c WHERE c.yeuCau.trangThai = :trangThai")
    Set<Integer> maSanPhamTheoTrangThai(@Param("trangThai") TrangThaiYeuCauNhap trangThai);
}
