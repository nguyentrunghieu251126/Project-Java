package project.dien_may.project_java.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "PhieuXuat", schema = "dbo")
@Getter
@Setter
public class PhieuXuat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaPhieuXuat")
    private Integer maPhieuXuat;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaDonHang", nullable = false, unique = true)
    private DonHang donHang;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaTaiKhoan", nullable = false)
    private TaiKhoan taiKhoan;

    @Column(name = "NgayXuat", nullable = false)
    private LocalDateTime ngayXuat;

    @Column(name = "GhiChu", length = 500)
    private String ghiChu;

    @OneToMany(mappedBy = "phieuXuat", cascade = CascadeType.ALL)
    private List<ChiTietPhieuXuat> chiTiet = new ArrayList<>();
}
