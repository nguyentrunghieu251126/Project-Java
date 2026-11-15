package project.dien_may.project_java.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/** Khớp ràng buộc CK_CTPN_Tien: ThanhTien = SoLuongNhap * DonGiaNhap. */
@Entity
@Table(name = "ChiTietPhieuNhap", schema = "dbo")
@Getter
@Setter
public class ChiTietPhieuNhap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaChiTietPhieuNhap")
    private Integer maChiTietPhieuNhap;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaPhieuNhap", nullable = false)
    private PhieuNhap phieuNhap;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaSanPham", nullable = false)
    private SanPham sanPham;

    @Column(name = "SoLuongNhap", nullable = false)
    private int soLuongNhap;

    @Column(name = "DonGiaNhap", nullable = false, precision = 18)
    private BigDecimal donGiaNhap;

    @Column(name = "ThanhTien", nullable = false, precision = 18)
    private BigDecimal thanhTien;
}
