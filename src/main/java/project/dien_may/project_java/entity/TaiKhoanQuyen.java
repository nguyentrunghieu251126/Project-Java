package project.dien_may.project_java.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Phần chỉnh riêng cho một nhân viên so với quyền của vai trò: cấp thêm hoặc thu hồi một quyền. */
@Entity
@Table(name = "TaiKhoan_Quyen", schema = "dbo")
@Getter
@Setter
@NoArgsConstructor
public class TaiKhoanQuyen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaTaiKhoanQuyen")
    private Integer maTaiKhoanQuyen;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaTaiKhoan", nullable = false)
    private TaiKhoan taiKhoan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaQuyen", nullable = false)
    private Quyen quyen;

    /** true là cấp thêm, false là thu hồi. */
    @Column(name = "DuocCap", nullable = false)
    private boolean duocCap;

    public TaiKhoanQuyen(TaiKhoan taiKhoan, Quyen quyen, boolean duocCap) {
        this.taiKhoan = taiKhoan;
        this.quyen = quyen;
        this.duocCap = duocCap;
    }
}
