package project.dien_may.project_java.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "SanPham", schema = "dbo")
@Getter
@Setter
public class SanPham {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaSanPham")
    private Integer maSanPham;

    @Column(name = "MaModel", nullable = false, unique = true, length = 50)
    private String maModel;

    @Column(name = "TenSanPham", nullable = false, length = 200)
    private String tenSanPham;

    @Column(name = "ThuongHieu", nullable = false, length = 50)
    private String thuongHieu;

    @Column(name = "DonGia", nullable = false, precision = 18, scale = 0)
    private BigDecimal donGia;

    @Column(name = "SoLuong", nullable = false)
    private Integer soLuong = 0;

    @Column(name = "MoTa", columnDefinition = "nvarchar(max)")
    private String moTa;

    @Column(name = "HinhAnh", length = 255)
    private String hinhAnh;

    @Column(name = "ThoiGianBaoHanh", nullable = false)
    private Integer thoiGianBaoHanh = 12;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaDanhMuc", nullable = false)
    private DanhMuc danhMuc;

    @Column(name = "MucTonToiThieu", nullable = false)
    private Integer mucTonToiThieu = 5;

    @Column(name = "DangKinhDoanh", nullable = false)
    private boolean dangKinhDoanh = true;

    // Để null khi tạo mới: Spring Data dựa vào version null để nhận biết bản ghi mới.
    @Version
    @Column(name = "PhienBan", nullable = false)
    private Integer phienBan;

    public BigDecimal getGiaTriTon() {
        return donGia.multiply(BigDecimal.valueOf(soLuong));
    }

    public TinhTrangTonKho getTinhTrangTonKho() {
        if (soLuong == 0) {
            return TinhTrangTonKho.HET;
        }
        return soLuong < mucTonToiThieu ? TinhTrangTonKho.SAP_HET : TinhTrangTonKho.DU;
    }
}
