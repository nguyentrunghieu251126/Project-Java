package project.dien_may.project_java.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import project.dien_may.project_java.entity.ChiTietYeuCauNhapHang;
import project.dien_may.project_java.entity.TrangThaiYeuCauNhap;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface ChiTietYeuCauNhapHangRepository extends JpaRepository<ChiTietYeuCauNhapHang, Integer> {

    boolean existsBySanPhamMaSanPhamAndYeuCauTrangThai(Integer maSanPham, TrangThaiYeuCauNhap trangThai);

    @EntityGraph(attributePaths = "sanPham")
    List<ChiTietYeuCauNhapHang> findByYeuCauMaYeuCauNhap(Integer maYeuCauNhap);

    @Query("SELECT DISTINCT c.sanPham.maSanPham FROM ChiTietYeuCauNhapHang c WHERE c.yeuCau.trangThai = :trangThai")
    Set<Integer> maSanPhamTheoTrangThai(@Param("trangThai") TrangThaiYeuCauNhap trangThai);

    /** TV4: cặp [mã sản phẩm, mã yêu cầu] của các yêu cầu ở những trạng thái cho trước. */
    @Query("""
            SELECT c.sanPham.maSanPham, c.yeuCau.maYeuCauNhap FROM ChiTietYeuCauNhapHang c
            WHERE c.yeuCau.trangThai IN :dsTrangThai
            ORDER BY c.yeuCau.maYeuCauNhap
            """)
    List<Object[]> sanPhamVaYeuCauTheoTrangThai(@Param("dsTrangThai") Collection<TrangThaiYeuCauNhap> dsTrangThai);
}
