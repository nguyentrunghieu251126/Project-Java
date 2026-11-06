package project.dien_may.project_java.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "ThongSoKyThuat", schema = "dbo")
public class ThongSoKyThuat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaThongSo")
    private Integer maThongSo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaSanPham", nullable = false)
    private SanPham sanPham;

    @Column(name = "TenThongSo", nullable = false, length = 100)
    private String tenThongSo;

    @Column(name = "GiaTri", nullable = false, length = 255)
    private String giaTri;

    @Column(name = "ThuTu", nullable = false)
    private Integer thuTu;

    public Integer getMaThongSo() { return maThongSo; }
    public SanPham getSanPham() { return sanPham; }
    public String getTenThongSo() { return tenThongSo; }
    public String getGiaTri() { return giaTri; }
    public Integer getThuTu() { return thuTu; }
}
