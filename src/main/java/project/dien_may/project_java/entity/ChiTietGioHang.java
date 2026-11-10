package project.dien_may.project_java.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/** TV3 – KH11: một dòng trong giỏ hàng; mỗi sản phẩm chỉ có một dòng (UQ_ChiTietGioHang). */
@Entity
@Table(name = "ChiTietGioHang", schema = "dbo")
@Getter
@Setter
public class ChiTietGioHang {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaChiTietGioHang")
    private Integer maChiTietGioHang;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaGioHang", nullable = false)
    private GioHang gioHang;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaSanPham", nullable = false)
    private SanPham sanPham;

    @Column(name = "SoLuong", nullable = false)
    private int soLuong;

    /** Giá hiện tại của sản phẩm nhân số lượng; giá chốt chỉ lưu khi đặt hàng. */
    public BigDecimal getThanhTien() {
        return sanPham.getDonGia().multiply(BigDecimal.valueOf(soLuong));
    }
}
