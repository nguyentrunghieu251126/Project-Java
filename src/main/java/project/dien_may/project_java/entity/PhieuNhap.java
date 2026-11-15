package project.dien_may.project_java.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** NVK10: Phiếu nhập kho, lập cho yêu cầu nhập hàng đã được quản trị viên duyệt. */
@Entity
@Table(name = "PhieuNhap", schema = "dbo")
@Getter
@Setter
public class PhieuNhap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaPhieuNhap")
    private Integer maPhieuNhap;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaYeuCauNhap", nullable = false)
    private YeuCauNhapHang yeuCau;

    /** Nhân viên kho lập phiếu. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaTaiKhoan", nullable = false)
    private TaiKhoan taiKhoan;

    @Column(name = "NgayNhap", nullable = false)
    private LocalDateTime ngayNhap;

    @Column(name = "TongTienNhap", nullable = false, precision = 18)
    private BigDecimal tongTienNhap;

    @Column(name = "GhiChu", length = 500)
    private String ghiChu;

    /** Quan hệ một – nhiều: một phiếu nhập có nhiều dòng sản phẩm. */
    @OneToMany(mappedBy = "phieuNhap", cascade = CascadeType.ALL)
    private List<ChiTietPhieuNhap> chiTiet = new ArrayList<>();
}
