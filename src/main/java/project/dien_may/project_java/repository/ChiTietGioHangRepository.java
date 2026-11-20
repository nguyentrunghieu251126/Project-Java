package project.dien_may.project_java.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import project.dien_may.project_java.entity.ChiTietGioHang;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ChiTietGioHangRepository extends JpaRepository<ChiTietGioHang, Integer> {

        @EntityGraph(attributePaths = "sanPham")
        @Query("""
                        SELECT ct FROM ChiTietGioHang ct
                        WHERE ct.gioHang.taiKhoan.email = :email
                        ORDER BY ct.maChiTietGioHang DESC
                        """)
        List<ChiTietGioHang> timTheoEmail(@Param("email") String email);

        @Query("""
                        SELECT ct FROM ChiTietGioHang ct
                        WHERE ct.gioHang.taiKhoan.email = :email AND ct.sanPham.maSanPham = :maSanPham
                        """)
        Optional<ChiTietGioHang> timDong(@Param("email") String email, @Param("maSanPham") Integer maSanPham);

        @Query("SELECT COUNT(ct) FROM ChiTietGioHang ct WHERE ct.gioHang.taiKhoan.email = :email")
        long demDong(@Param("email") String email);

        @Query("SELECT COALESCE(SUM(ct.soLuong), 0) FROM ChiTietGioHang ct WHERE ct.gioHang.taiKhoan.email = :email")
        long tongSoLuong(@Param("email") String email);

        @Modifying(flushAutomatically = true)
        @Query("""
                        DELETE FROM ChiTietGioHang ct
                        WHERE ct.gioHang.maGioHang = :maGioHang AND ct.sanPham.maSanPham IN :dsMaSanPham
                        """)
        int xoaCacDong(@Param("maGioHang") Integer maGioHang, @Param("dsMaSanPham") Collection<Integer> dsMaSanPham);
}
