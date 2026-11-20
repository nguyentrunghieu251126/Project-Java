package project.dien_may.project_java.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "LichSuDiemThuong", schema = "dbo")
@Getter
@Setter
public class LichSuDiemThuong {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaLichSu")
    private Integer maLichSu;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaTaiKhoan", nullable = false)
    private TaiKhoan taiKhoan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaDonHang")
    private DonHang donHang;

    @Enumerated(EnumType.STRING)
    @Column(name = "LoaiGiaoDich", nullable = false, length = 10)
    private LoaiGiaoDichDiem loaiGiaoDich;

    @Column(name = "SoDiem", nullable = false)
    private int soDiem;

    @Column(name = "MoTa", length = 255)
    private String moTa;

    @Column(name = "NgayGiaoDich", nullable = false)
    private LocalDateTime ngayGiaoDich;
}
