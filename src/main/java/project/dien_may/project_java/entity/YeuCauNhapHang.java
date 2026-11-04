package project.dien_may.project_java.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "YeuCauNhapHang", schema = "dbo")
@Getter
@Setter
public class YeuCauNhapHang {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaYeuCauNhap")
    private Integer maYeuCauNhap;

    /** Nhân viên kho gửi yêu cầu. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaTaiKhoan", nullable = false)
    private TaiKhoan taiKhoan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaNguoiDuyet")
    private TaiKhoan nguoiDuyet;

    @Column(name = "NgayYeuCau", nullable = false)
    private LocalDateTime ngayYeuCau;

    @Enumerated(EnumType.STRING)
    @Column(name = "TrangThai", nullable = false, length = 20)
    private TrangThaiYeuCauNhap trangThai;

    @Column(name = "NgayDuyet")
    private LocalDateTime ngayDuyet;

    @Column(name = "LyDoTuChoi", length = 500)
    private String lyDoTuChoi;

    @OneToMany(mappedBy = "yeuCau")
    private List<ChiTietYeuCauNhapHang> chiTiet = new ArrayList<>();

    public int getTongSoLuong() {
        return chiTiet.stream().mapToInt(ChiTietYeuCauNhapHang::getSoLuongYeuCau).sum();
    }
}
