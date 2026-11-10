package project.dien_may.project_java.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import project.dien_may.project_java.entity.PhieuDoiTra;
import project.dien_may.project_java.entity.TrangThaiDoiTra;

import java.util.Collection;
import java.util.List;

public interface PhieuDoiTraRepository extends JpaRepository<PhieuDoiTra, Integer> {

    /** Danh sách cho nhân viên kho; trangThai null thì lấy tất cả. */
    @EntityGraph(attributePaths = {"chiTietDonHang", "chiTietDonHang.sanPham", "chiTietDonHang.donHang",
            "chiTietDonHang.donHang.taiKhoan", "nhanVien"})
    @Query("SELECT p FROM PhieuDoiTra p WHERE :trangThai IS NULL OR p.trangThai = :trangThai")
    Page<PhieuDoiTra> timKiem(@Param("trangThai") TrangThaiDoiTra trangThai, Pageable pageable);

    long countByTrangThai(TrangThaiDoiTra trangThai);

    /** Sản phẩm đang có yêu cầu chưa xử lý xong thì không cho gửi thêm. */
    boolean existsByChiTietDonHangMaChiTietDonHangAndTrangThaiIn(Integer maChiTietDonHang,
                                                                Collection<TrangThaiDoiTra> dsTrangThai);

    /** Lịch sử yêu cầu của một khách hàng. */
    @EntityGraph(attributePaths = {"chiTietDonHang", "chiTietDonHang.sanPham"})
    List<PhieuDoiTra> findByChiTietDonHangDonHangTaiKhoanEmailIgnoreCaseOrderByNgayYeuCauDesc(String email);
}
