package project.dien_may.project_java.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "ChiTietPhieuXuat", schema = "dbo")
@Getter
@Setter
public class ChiTietPhieuXuat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaChiTietPhieuXuat")
    private Integer maChiTietPhieuXuat;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaPhieuXuat", nullable = false)
    private PhieuXuat phieuXuat;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaSanPham", nullable = false)
    private SanPham sanPham;

    @Column(name = "SoLuongXuat", nullable = false)
    private int soLuongXuat;
}
