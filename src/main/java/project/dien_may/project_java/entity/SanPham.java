
package project.dien_may.project_java.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "SanPham", schema = "dbo")
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

    @Column(name = "PhienBan", nullable = false)
    private Integer phienBan = 0;

    public SanPham() {
    }

    public Integer getMaSanPham() {
        return maSanPham;
    }

    public void setMaSanPham(Integer maSanPham) {
        this.maSanPham = maSanPham;
    }

    public String getMaModel() {
        return maModel;
    }

    public void setMaModel(String maModel) {
        this.maModel = maModel;
    }

    public String getTenSanPham() {
        return tenSanPham;
    }

    public void setTenSanPham(String tenSanPham) {
        this.tenSanPham = tenSanPham;
    }

    public String getThuongHieu() {
        return thuongHieu;
    }

    public void setThuongHieu(String thuongHieu) {
        this.thuongHieu = thuongHieu;
    }

    public BigDecimal getDonGia() {
        return donGia;
    }

    public void setDonGia(BigDecimal donGia) {
        this.donGia = donGia;
    }

    public Integer getSoLuong() {
        return soLuong;
    }

    public void setSoLuong(Integer soLuong) {
        this.soLuong = soLuong;
    }

    public String getMoTa() {
        return moTa;
    }

    public void setMoTa(String moTa) {
        this.moTa = moTa;
    }

    public String getHinhAnh() {
        return hinhAnh;
    }

    public void setHinhAnh(String hinhAnh) {
        this.hinhAnh = hinhAnh;
    }

    public Integer getThoiGianBaoHanh() {
        return thoiGianBaoHanh;
    }

    public void setThoiGianBaoHanh(Integer thoiGianBaoHanh) {
        this.thoiGianBaoHanh = thoiGianBaoHanh;
    }

    public DanhMuc getDanhMuc() {
        return danhMuc;
    }

    public void setDanhMuc(DanhMuc danhMuc) {
        this.danhMuc = danhMuc;
    }

    public Integer getMucTonToiThieu() {
        return mucTonToiThieu;
    }

    public void setMucTonToiThieu(Integer mucTonToiThieu) {
        this.mucTonToiThieu = mucTonToiThieu;
    }

    public boolean isDangKinhDoanh() {
        return dangKinhDoanh;
    }

    public void setDangKinhDoanh(boolean dangKinhDoanh) {
        this.dangKinhDoanh = dangKinhDoanh;
    }

    public Integer getPhienBan() {
        return phienBan;
    }

    public void setPhienBan(Integer phienBan) {
        this.phienBan = phienBan;
    }
}
