package project.dien_may.project_java.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "DonHang", schema = "dbo")
@Getter
@Setter
public class DonHang {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaDonHang")
    private Integer maDonHang;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaTaiKhoan", nullable = false)
    private TaiKhoan taiKhoan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaKhuyenMai")
    private KhuyenMai khuyenMai;

    @Column(name = "NgayDat", nullable = false)
    private LocalDateTime ngayDat;

    @Column(name = "HoTenNguoiNhan", nullable = false, length = 100)
    private String hoTenNguoiNhan;

    @Column(name = "SoDienThoaiNguoiNhan", nullable = false, length = 15)
    private String soDienThoaiNguoiNhan;

    @Column(name = "DiaChiGiaoHang", nullable = false, length = 255)
    private String diaChiGiaoHang;

    @Column(name = "TienGiam", nullable = false, precision = 18)
    private BigDecimal tienGiam;

    /** Số tiền phải trả sau giảm giá. */
    @Column(name = "TongTien", nullable = false, precision = 18)
    private BigDecimal tongTien;

    @Enumerated(EnumType.STRING)
    @Column(name = "TrangThaiDonHang", nullable = false, length = 20)
    private TrangThaiDonHang trangThai;

    @Column(name = "NgayGiao")
    private LocalDateTime ngayGiao;
}
