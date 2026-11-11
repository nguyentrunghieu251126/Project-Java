package project.dien_may.project_java.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** TV4 – Phiếu đổi trả của khách cho một sản phẩm trong đơn đã giao (KH21, NVK12, NVK14). */
@Entity
@Table(name = "PhieuDoiTra", schema = "dbo")
@Getter
@Setter
public class PhieuDoiTra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaPhieuDoiTra")
    private Integer maPhieuDoiTra;

    /** Sản phẩm cụ thể trong đơn hàng mà khách gửi yêu cầu. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaChiTietDonHang", nullable = false)
    private ChiTietDonHang chiTietDonHang;

    /** Nhân viên kho tiếp nhận; null khi chưa tiếp nhận. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaTaiKhoan")
    private TaiKhoan nhanVien;

    @Column(name = "NgayYeuCau", nullable = false)
    private LocalDateTime ngayYeuCau;

    @Column(name = "NgayTiepNhan")
    private LocalDateTime ngayTiepNhan;

    @Column(name = "LyDoDoiTra", nullable = false, length = 1000)
    private String lyDoDoiTra;

    @Enumerated(EnumType.STRING)
    @Column(name = "TrangThaiDoiTra", nullable = false, length = 20)
    private TrangThaiDoiTra trangThai;

    @Column(name = "KetQuaXuLy", length = 1000)
    private String ketQuaXuLy;
}
