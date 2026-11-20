package project.dien_may.project_java.entity;

import project.dien_may.project_java.dto.muahang.ThoiGianGiaoHang;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "SoDiaChi", schema = "dbo")
@Getter
@Setter
public class SoDiaChi {

    public static final String NHA_RIENG = "Nhà riêng";
    public static final String VAN_PHONG = "Văn phòng";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaDiaChi")
    private Integer maDiaChi;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaTaiKhoan", nullable = false)
    private TaiKhoan taiKhoan;

    @Column(name = "TenNguoiNhan", nullable = false, length = 100)
    private String tenNguoiNhan;

    @Column(name = "SoDienThoai", nullable = false, length = 15)
    private String soDienThoai;

    @Column(name = "KhuVuc", nullable = false, length = 50)
    private String khuVuc;

    @Column(name = "DiaChiChiTiet", nullable = false, length = 200)
    private String diaChiChiTiet;

    @Column(name = "LoaiDiaChi", nullable = false, length = 20)
    private String loaiDiaChi = NHA_RIENG;

    @Column(name = "LaMacDinh", nullable = false)
    private boolean laMacDinh;

    @Column(name = "NgayTao", nullable = false)
    private LocalDateTime ngayTao;

    public String getDiaChiDayDu() {
        return diaChiChiTiet + ", " + khuVuc;
    }

    public ThoiGianGiaoHang getGiaoHang() {
        return ThoiGianGiaoHang.theoKhuVuc(khuVuc);
    }
}
