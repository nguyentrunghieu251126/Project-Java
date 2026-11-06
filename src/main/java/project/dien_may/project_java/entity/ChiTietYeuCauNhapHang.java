package project.dien_may.project_java.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "ChiTietYeuCauNhapHang", schema = "dbo")
@Getter
@Setter
public class ChiTietYeuCauNhapHang {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaChiTietYeuCauNhap")
    private Integer maChiTietYeuCauNhap;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaYeuCauNhap", nullable = false)
    private YeuCauNhapHang yeuCau;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaSanPham", nullable = false)
    private SanPham sanPham;

    @Column(name = "SoLuongYeuCau", nullable = false)
    private int soLuongYeuCau;
}
