package project.dien_may.project_java.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import project.dien_may.project_java.entity.KhuyenMai;

import java.time.LocalDate;

public interface KhuyenMaiRepository extends JpaRepository<KhuyenMai, Integer> {

    /** tinhTrang nhận tên của TinhTrangKhuyenMai; điều kiện phải khớp KhuyenMai.getTinhTrang(). */
    @Query("""
            SELECT k FROM KhuyenMai k
            WHERE (:tuKhoa IS NULL OR k.tenKhuyenMai LIKE CONCAT('%', :tuKhoa, '%')
                                   OR k.maGiamGia LIKE CONCAT('%', :tuKhoa, '%'))
              AND (:tinhTrang IS NULL
                   OR (:tinhTrang = 'TAM_DUNG' AND k.trangThai = false)
                   OR (:tinhTrang = 'SAP_DIEN_RA' AND k.trangThai = true AND k.ngayBatDau > :homNay)
                   OR (:tinhTrang = 'HET_HAN' AND k.trangThai = true AND k.ngayBatDau <= :homNay
                       AND k.ngayKetThuc < :homNay)
                   OR (:tinhTrang = 'HET_LUOT' AND k.trangThai = true AND k.ngayBatDau <= :homNay
                       AND k.ngayKetThuc >= :homNay
                       AND k.soLuotToiDa IS NOT NULL AND k.soLuotDaDung >= k.soLuotToiDa)
                   OR (:tinhTrang = 'DANG_CHAY' AND k.trangThai = true AND k.ngayBatDau <= :homNay
                       AND k.ngayKetThuc >= :homNay
                       AND (k.soLuotToiDa IS NULL OR k.soLuotDaDung < k.soLuotToiDa)))
            """)
    Page<KhuyenMai> timKiem(@Param("tuKhoa") String tuKhoa,
                            @Param("tinhTrang") String tinhTrang,
                            @Param("homNay") LocalDate homNay,
                            Pageable pageable);

    boolean existsByMaGiamGiaIgnoreCase(String maGiamGia);

    boolean existsByMaGiamGiaIgnoreCaseAndMaKhuyenMaiNot(String maGiamGia, Integer maKhuyenMai);
}
