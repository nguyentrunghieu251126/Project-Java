package project.dien_may.project_java.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** TV3 – KH23: khách gửi yêu cầu hỗ trợ bằng form web; nhân viên phản hồi vào cột PhanHoi. */
@Entity
@Table(name = "YeuCauHoTro", schema = "dbo")
@Getter
@Setter
public class YeuCauHoTro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaYeuCauHoTro")
    private Integer maYeuCauHoTro;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaTaiKhoan", nullable = false)
    private TaiKhoan taiKhoan;

    @Column(name = "TieuDe", nullable = false, length = 200)
    private String tieuDe;

    @Column(name = "NoiDung", nullable = false, length = 2000)
    private String noiDung;

    @Column(name = "NgayGui", nullable = false)
    private LocalDateTime ngayGui;

    @Enumerated(EnumType.STRING)
    @Column(name = "TrangThai", nullable = false, length = 20)
    private TrangThaiHoTro trangThai = TrangThaiHoTro.MOI;

    /** NULL khi cửa hàng chưa phản hồi. */
    @Column(name = "PhanHoi", length = 2000)
    private String phanHoi;
}
