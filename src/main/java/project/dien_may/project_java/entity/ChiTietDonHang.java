package project.dien_may.project_java.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "ChiTietDonHang", schema = "dbo")
@Getter
@Setter
public class ChiTietDonHang {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaChiTietDonHang")
    private Integer maChiTietDonHang;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaDonHang", nullable = false)
    private DonHang donHang;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaSanPham", nullable = false)
    private SanPham sanPham;

    @Column(name = "SoLuong", nullable = false)
    private int soLuong;

    @Column(name = "DonGia", nullable = false, precision = 18)
    private BigDecimal donGia;

    @Column(name = "ThanhTien", nullable = false, precision = 18)
    private BigDecimal thanhTien;
}
