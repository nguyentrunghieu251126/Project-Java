
package project.dien_may.project_java.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "VaiTro", schema = "dbo")
public class VaiTro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaVaiTro")
    private Integer maVaiTro;

    @Column(name = "TenVaiTro", nullable = false, length = 30)
    private String tenVaiTro;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "VaiTro_Quyen", schema = "dbo",
            joinColumns = @JoinColumn(name = "MaVaiTro"),
            inverseJoinColumns = @JoinColumn(name = "MaQuyen"))
    private Set<Quyen> dsQuyen = new HashSet<>();

    public VaiTro() {
    }

    public Integer getMaVaiTro() {
        return maVaiTro;
    }

    public void setMaVaiTro(Integer maVaiTro) {
        this.maVaiTro = maVaiTro;
    }

    public String getTenHienThi() {
        return switch (tenVaiTro) {
            case "QUAN_TRI" -> "Quản trị viên";
            case "NHAN_VIEN_KHO" -> "Nhân viên kho";
            case "KHACH_HANG" -> "Khách hàng";
            default -> tenVaiTro;
        };
    }

    public Set<Quyen> getDsQuyen() {
        return dsQuyen;
    }

    public String getTenVaiTro() {
        return tenVaiTro;
    }

    public void setTenVaiTro(String tenVaiTro) {
        this.tenVaiTro = tenVaiTro;
    }
}
