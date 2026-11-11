package project.dien_may.project_java.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** TV3 – KH11: mỗi khách hàng có đúng một giỏ (UQ_GioHang_MaTaiKhoan). */
@Entity
@Table(name = "GioHang", schema = "dbo")
@Getter
@Setter
public class GioHang {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaGioHang")
    private Integer maGioHang;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaTaiKhoan", nullable = false, unique = true)
    private TaiKhoan taiKhoan;

    @Column(name = "NgayTao", nullable = false)
    private LocalDateTime ngayTao;
}
