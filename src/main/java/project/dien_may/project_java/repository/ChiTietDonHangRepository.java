package project.dien_may.project_java.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import project.dien_may.project_java.dto.baocao.SanPhamBanChay;
import project.dien_may.project_java.entity.ChiTietDonHang;
import project.dien_may.project_java.entity.TrangThaiDonHang;

import java.time.LocalDateTime;
import java.util.List;

public interface ChiTietDonHangRepository extends JpaRepository<ChiTietDonHang, Integer> {

    @EntityGraph(attributePaths = "sanPham")
    List<ChiTietDonHang> findByDonHangMaDonHang(Integer maDonHang);

    @Query("""
            SELECT new project.dien_may.project_java.dto.baocao.SanPhamBanChay(
                       s.maSanPham, s.tenSanPham, s.maModel, dm.tenDanhMuc,
                       SUM(ct.soLuong), SUM(ct.thanhTien))
            FROM ChiTietDonHang ct JOIN ct.sanPham s JOIN s.danhMuc dm JOIN ct.donHang d
            WHERE d.trangThai = :trangThai AND d.ngayDat >= :tu AND d.ngayDat < :den
              AND (:maDanhMuc IS NULL OR dm.maDanhMuc = :maDanhMuc)
            GROUP BY s.maSanPham, s.tenSanPham, s.maModel, dm.tenDanhMuc
            ORDER BY SUM(ct.soLuong) DESC, SUM(ct.thanhTien) DESC
            """)
    List<SanPhamBanChay> banChay(@Param("trangThai") TrangThaiDonHang trangThai,
                                 @Param("tu") LocalDateTime tu,
                                 @Param("den") LocalDateTime den,
                                 @Param("maDanhMuc") Integer maDanhMuc,
                                 Pageable pageable);

    /** TV4 (KH20 – KH21): các sản phẩm khách đã nhận, tức thuộc đơn đã giao, mới nhất trước. */
    @Query("""
            SELECT ct FROM ChiTietDonHang ct JOIN FETCH ct.sanPham JOIN FETCH ct.donHang d
            WHERE LOWER(d.taiKhoan.email) = LOWER(:email)
              AND d.trangThai = project.dien_may.project_java.entity.TrangThaiDonHang.DA_GIAO
            ORDER BY d.ngayGiao DESC, ct.maChiTietDonHang ASC
            """)
    List<ChiTietDonHang> sanPhamDaNhan(@Param("email") String email);
}
