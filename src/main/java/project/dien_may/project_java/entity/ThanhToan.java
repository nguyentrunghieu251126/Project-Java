package project.dien_may.project_java.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ThanhToan", schema = "dbo")
@Getter
@Setter
public class ThanhToan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaThanhToan")
    private Integer maThanhToan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaDonHang", nullable = false)
    private DonHang donHang;

    @Enumerated(EnumType.STRING)
    @Column(name = "PhuongThucThanhToan", nullable = false, length = 20)
    private PhuongThucThanhToan phuongThuc;

    @Column(name = "SoTienThanhToan", nullable = false, precision = 18)
    private BigDecimal soTien;

    @Column(name = "NgayThanhToan")
    private LocalDateTime ngayThanhToan;

    @Enumerated(EnumType.STRING)
    @Column(name = "TrangThaiThanhToan", nullable = false, length = 20)
    private TrangThaiThanhToan trangThai;

    @Column(name = "UuDaiThanhToan", length = 200)
    private String uuDai;

    @Column(name = "TienGiamThanhToan", nullable = false, precision = 18)
    private BigDecimal tienGiamUuDai = BigDecimal.ZERO;
}
