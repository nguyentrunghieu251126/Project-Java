package project.dien_may.project_java.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import project.dien_may.project_java.entity.PhieuBaoHanh;
import project.dien_may.project_java.entity.TrangThaiBaoHanh;

import java.util.Collection;
import java.util.List;

public interface PhieuBaoHanhRepository extends JpaRepository<PhieuBaoHanh, Integer> {
        @EntityGraph(attributePaths = { "chiTietDonHang", "chiTietDonHang.sanPham", "chiTietDonHang.donHang",
                        "chiTietDonHang.donHang.taiKhoan", "nhanVien" })
        @Query("SELECT p FROM PhieuBaoHanh p WHERE :trangThai IS NULL OR p.trangThai = :trangThai")
        Page<PhieuBaoHanh> timKiem(@Param("trangThai") TrangThaiBaoHanh trangThai, Pageable pageable);

        long countByTrangThai(TrangThaiBaoHanh trangThai);

        boolean existsByChiTietDonHangMaChiTietDonHangAndTrangThaiIn(Integer maChiTietDonHang,
                        Collection<TrangThaiBaoHanh> dsTrangThai);

        @EntityGraph(attributePaths = { "chiTietDonHang", "chiTietDonHang.sanPham" })
        List<PhieuBaoHanh> findByChiTietDonHangDonHangTaiKhoanEmailIgnoreCaseOrderByNgayYeuCauDesc(String email);
}
