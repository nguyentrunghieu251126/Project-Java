package project.dien_may.project_java.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import project.dien_may.project_java.entity.DanhGia;

import java.util.Collection;
import java.util.List;

public interface DanhGiaKhachHangRepository extends JpaRepository<DanhGia, Integer> {
        @Query("""
                        SELECT dg.sanPham.maSanPham, dg.soSao FROM DanhGia dg
                        WHERE dg.taiKhoan.maTaiKhoan = :maTaiKhoan AND dg.sanPham.maSanPham IN :dsMaSanPham
                        """)
        List<Object[]> soSaoDaDanhGia(@Param("maTaiKhoan") Integer maTaiKhoan,
                        @Param("dsMaSanPham") Collection<Integer> dsMaSanPham);

        boolean existsByTaiKhoanMaTaiKhoanAndSanPhamMaSanPham(Integer maTaiKhoan, Integer maSanPham);

        @Modifying
        @Query(value = """
                        INSERT INTO dbo.DanhGia (MaTaiKhoan, MaSanPham, SoSao, NoiDungDanhGia)
                        VALUES (:maTaiKhoan, :maSanPham, :soSao, NULLIF(:noiDung, N''))
                        """, nativeQuery = true)
        int themDanhGia(@Param("maTaiKhoan") Integer maTaiKhoan,
                        @Param("maSanPham") Integer maSanPham,
                        @Param("soSao") int soSao,
                        @Param("noiDung") String noiDung);
}
