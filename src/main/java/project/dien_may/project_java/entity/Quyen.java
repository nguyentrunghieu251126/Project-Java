package project.dien_may.project_java.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "Quyen", schema = "dbo")
@Getter
@Setter
public class Quyen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaQuyen")
    private Integer maQuyen;

    /** Mã dùng làm authority trong Spring Security, ví dụ SANPHAM_THEM. */
    @Column(name = "MaChucNang", nullable = false, length = 50)
    private String maChucNang;

    @Column(name = "TenQuyen", nullable = false, length = 100)
    private String tenQuyen;

    @Column(name = "NhomChucNang", nullable = false, length = 50)
    private String nhomChucNang;

    @Column(name = "MoTa", length = 255)
    private String moTa;
}
