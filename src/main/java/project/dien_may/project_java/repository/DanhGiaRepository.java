package project.dien_may.project_java.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import project.dien_may.project_java.entity.DanhGia;
import java.util.List;

/** Chỉ đọc đánh giá; không tạo API thêm/sửa/xóa của TV3. */
public interface DanhGiaRepository extends JpaRepository<DanhGia, Integer> {
    @Query("SELECT dg FROM DanhGia dg JOIN FETCH dg.taiKhoan " +
            "WHERE dg.sanPham.maSanPham = :maSanPham " +
            "ORDER BY dg.ngayDanhGia DESC, dg.maDanhGia DESC")
    List<DanhGia> layDanhGiaSanPham(@Param("maSanPham") Integer maSanPham);

    //  chi doc diem trung binh va so luot danh gia cho cac the san pham trang chu.

    @Query("SELECT dg.sanPham.maSanPham, COUNT(dg), AVG(dg.soSao) " + "FROM DanhGia dg WHERE dg.sanPham.maSanPham IN :maSanPham " + "GROUP BY dg.sanPham.maSanPham")
    List<Object[]> thongKeDanhGiaTheoSanPham(@Param("maSanPham") List<Integer> maSanPham);
}
