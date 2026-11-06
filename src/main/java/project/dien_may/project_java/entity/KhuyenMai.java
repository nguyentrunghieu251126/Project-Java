package project.dien_may.project_java.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "KhuyenMai", schema = "dbo")
@Getter
@Setter
public class KhuyenMai {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaKhuyenMai")
    private Integer maKhuyenMai;

    @Column(name = "TenKhuyenMai", nullable = false, length = 200)
    private String tenKhuyenMai;

    @Column(name = "MaGiamGia", nullable = false, length = 30)
    private String maGiamGia;

    @Enumerated(EnumType.STRING)
    @Column(name = "LoaiGiamGia", nullable = false, length = 20)
    private LoaiGiamGia loaiGiamGia;

    @Column(name = "GiaTriGiam", nullable = false, precision = 18)
    private BigDecimal giaTriGiam;

    @Column(name = "NgayBatDau", nullable = false)
    private LocalDate ngayBatDau;

    @Column(name = "NgayKetThuc", nullable = false)
    private LocalDate ngayKetThuc;

    /** NULL: không giới hạn số lượt. */
    @Column(name = "SoLuotToiDa")
    private Integer soLuotToiDa;

    @Column(name = "SoLuotDaDung", nullable = false)
    private int soLuotDaDung;

    /** false: quản trị viên đang tạm dừng mã. */
    @Column(name = "TrangThai", nullable = false)
    private boolean trangThai = true;

    public TinhTrangKhuyenMai getTinhTrang() {
        LocalDate homNay = LocalDate.now();
        if (!trangThai) {
            return TinhTrangKhuyenMai.TAM_DUNG;
        }
        if (ngayBatDau.isAfter(homNay)) {
            return TinhTrangKhuyenMai.SAP_DIEN_RA;
        }
        if (ngayKetThuc.isBefore(homNay)) {
            return TinhTrangKhuyenMai.HET_HAN;
        }
        if (soLuotToiDa != null && soLuotDaDung >= soLuotToiDa) {
            return TinhTrangKhuyenMai.HET_LUOT;
        }
        return TinhTrangKhuyenMai.DANG_CHAY;
    }
}
