package project.dien_may.project_java.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** KH07: Chỉ đọc đánh giá hiện có; TV3 quản lý nghiệp vụ gửi đánh giá (KH22). */
@Entity
@Table(name = "DanhGia", schema = "dbo")
public class DanhGia {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaDanhGia")
    private Integer maDanhGia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaTaiKhoan", nullable = false)
    private TaiKhoan taiKhoan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaSanPham", nullable = false)
    private SanPham sanPham;

    @Column(name = "SoSao", nullable = false)
    private Byte soSao;

    @Column(name = "NoiDungDanhGia", length = 1000)
    private String noiDungDanhGia;

    @Column(name = "NgayDanhGia", nullable = false, insertable = false, updatable = false)
    private LocalDateTime ngayDanhGia;

    protected DanhGia() { }

    public Integer getMaDanhGia() { return maDanhGia; }
    public TaiKhoan getTaiKhoan() { return taiKhoan; }
    public SanPham getSanPham() { return sanPham; }
    public Integer getSoSao() { return soSao == null ? null : soSao.intValue(); }
    public String getNoiDungDanhGia() { return noiDungDanhGia; }
    public LocalDateTime getNgayDanhGia() { return ngayDanhGia; }
}
