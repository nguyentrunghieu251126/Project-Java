package project.dien_may.project_java.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** TV4 – Phiếu bảo hành của khách cho một sản phẩm trong đơn đã giao (KH20, NVK11, NVK13). */
@Entity
@Table(name = "PhieuBaoHanh", schema = "dbo")
@Getter
@Setter
public class PhieuBaoHanh {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaPhieuBaoHanh")
    private Integer maPhieuBaoHanh;

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

    @Column(name = "MoTaLoi", nullable = false, length = 1000)
    private String moTaLoi;

    @Enumerated(EnumType.STRING)
    @Column(name = "TrangThaiBaoHanh", nullable = false, length = 20)
    private TrangThaiBaoHanh trangThai;

    @Column(name = "KetQuaXuLy", length = 1000)
    private String ketQuaXuLy;
}
