package project.dien_may.project_java.service.admin;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import project.dien_may.project_java.entity.Quyen;
import project.dien_may.project_java.entity.TaiKhoan;
import project.dien_may.project_java.entity.TaiKhoanQuyen;
import project.dien_may.project_java.entity.VaiTro;
import project.dien_may.project_java.repository.QuyenRepository;
import project.dien_may.project_java.repository.TaiKhoanQuyenRepository;
import project.dien_may.project_java.repository.TaiKhoanRepository;
import project.dien_may.project_java.repository.VaiTroRepository;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PhanQuyenService {
    private static final String VAI_TRO_QUAN_TRI = "QUAN_TRI";
    public static final String VAI_TRO_NHAN_VIEN_KHO = "NHAN_VIEN_KHO";
    public static final String VAI_TRO_NHAN_VIEN_BAO_HANH = "NHAN_VIEN_BAO_HANH";
    /** Các vai trò nhân viên được phân quyền theo chức năng. */
    public static final List<String> DS_VAI_TRO_NHAN_VIEN = List.of(VAI_TRO_NHAN_VIEN_KHO,
            VAI_TRO_NHAN_VIEN_BAO_HANH);
    private static final String QUYEN_TONG_QUAN = "TONGQUAN_XEM";

    private final VaiTroRepository vaiTroRepository;
    private final QuyenRepository quyenRepository;
    private final TaiKhoanRepository taiKhoanRepository;
    private final TaiKhoanQuyenRepository taiKhoanQuyenRepository;

    public PhanQuyenService(VaiTroRepository vaiTroRepository, QuyenRepository quyenRepository,
            TaiKhoanRepository taiKhoanRepository, TaiKhoanQuyenRepository taiKhoanQuyenRepository) {
        this.vaiTroRepository = vaiTroRepository;
        this.quyenRepository = quyenRepository;
        this.taiKhoanRepository = taiKhoanRepository;
        this.taiKhoanQuyenRepository = taiKhoanQuyenRepository;
    }

    @Transactional(readOnly = true)
    public Map<String, List<Quyen>> quyenTheoNhom() {
        return quyenRepository.findAll(Sort.by("maQuyen")).stream()
                .sorted(Comparator.comparing((Quyen q) -> !QUYEN_TONG_QUAN.equals(q.getMaChucNang())))
                .collect(Collectors.groupingBy(Quyen::getNhomChucNang, LinkedHashMap::new, Collectors.toList()));
    }

    // Lấy danh sách tất cả các quyển có hiệu lực
    @Transactional(readOnly = true)
    public Set<String> quyenHieuLuc(TaiKhoan taiKhoan) {
        String tenVaiTro = taiKhoan.getVaiTro().getTenVaiTro();
        if (VAI_TRO_QUAN_TRI.equals(tenVaiTro)) {
            return maChucNang(quyenRepository.findAll());
        }
        Set<String> ketQua = maChucNang(taiKhoan.getVaiTro().getDsQuyen());
        if (DS_VAI_TRO_NHAN_VIEN.contains(tenVaiTro)) {
            for (TaiKhoanQuyen rieng : taiKhoanQuyenRepository.findByTaiKhoanMaTaiKhoan(taiKhoan.getMaTaiKhoan())) {
                if (rieng.isDuocCap()) {
                    ketQua.add(rieng.getQuyen().getMaChucNang());
                } else {
                    ketQua.remove(rieng.getQuyen().getMaChucNang());
                }
            }
        }
        return ketQua;
    }

    /** Danh sách vai trò nhân viên để chọn ở tab Toàn thể nhân viên. */
    @Transactional(readOnly = true)
    public List<VaiTro> dsVaiTroNhanVien() {
        return vaiTroRepository.findAll().stream()
                .filter(v -> DS_VAI_TRO_NHAN_VIEN.contains(v.getTenVaiTro()))
                .sorted(Comparator.comparing(VaiTro::getMaVaiTro))
                .toList();
    }

    /** Tên vai trò hợp lệ; rỗng hoặc sai thì lấy nhân viên kho. */
    public static String chuanHoaVaiTro(String tenVaiTro) {
        // List.of(...).contains(null) ném NullPointerException nên phải kiểm tra null trước
        return tenVaiTro != null && DS_VAI_TRO_NHAN_VIEN.contains(tenVaiTro) ? tenVaiTro : VAI_TRO_NHAN_VIEN_KHO;
    }

    @Transactional(readOnly = true)
    public Set<String> quyenToanTheNhanVien(String tenVaiTro) {
        return maChucNang(vaiTroNhanVien(tenVaiTro).getDsQuyen());
    }

    // Cập nhật lại quyền cho toàn thể nhân viên của một vai trò
    @Transactional
    public void capNhatToanTheNhanVien(String tenVaiTro, Collection<Integer> dsMaQuyen) {
        Set<Quyen> moi = layQuyen(dsMaQuyen);
        Set<Quyen> dsQuyen = vaiTroNhanVien(tenVaiTro).getDsQuyen();
        dsQuyen.retainAll(moi);
        dsQuyen.addAll(moi);
    }

    @Transactional(readOnly = true)
    public List<TaiKhoan> dsNhanVien() {
        return taiKhoanRepository.findByVaiTroTenVaiTroInOrderByHoTenAsc(DS_VAI_TRO_NHAN_VIEN);
    }

    @Transactional(readOnly = true)
    public TaiKhoan nhanVien(Integer maTaiKhoan) {
        return taiKhoanRepository.findById(maTaiKhoan)
                .filter(t -> DS_VAI_TRO_NHAN_VIEN.contains(t.getVaiTro().getTenVaiTro()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    /** Mã chức năng đang khác với quyền của toàn thể nhân viên. */
    @Transactional(readOnly = true)
    public Set<String> quyenChinhRieng(Integer maTaiKhoan) {
        return taiKhoanQuyenRepository.findByTaiKhoanMaTaiKhoan(maTaiKhoan).stream()
                .map(rieng -> rieng.getQuyen().getMaChucNang())
                .collect(Collectors.toSet());
    }

    @Transactional
    public void capNhatNhanVien(Integer maTaiKhoan, Collection<Integer> dsMaQuyen) {
        TaiKhoan nhanVien = nhanVien(maTaiKhoan);
        Set<Quyen> duocChon = layQuyen(dsMaQuyen);
        Set<String> theoVaiTro = maChucNang(nhanVien.getVaiTro().getDsQuyen());
        Set<String> daChon = maChucNang(duocChon);

        taiKhoanQuyenRepository.xoaTheoTaiKhoan(maTaiKhoan);
        for (Quyen quyen : quyenRepository.findAll()) {
            boolean chon = daChon.contains(quyen.getMaChucNang());
            if (chon != theoVaiTro.contains(quyen.getMaChucNang())) {
                taiKhoanQuyenRepository.save(new TaiKhoanQuyen(nhanVien, quyen, chon));
            }
        }
    }

    private VaiTro vaiTroNhanVien(String tenVaiTro) {
        String ten = chuanHoaVaiTro(tenVaiTro);
        return vaiTroRepository.findByTenVaiTro(ten)
                .orElseThrow(() -> new NghiepVuException("Chưa có vai trò " + ten + " trong cơ sở dữ liệu."));
    }

    private Set<Quyen> layQuyen(Collection<Integer> dsMaQuyen) {
        Map<Integer, Quyen> quyenTheoMa = quyenRepository.findAll().stream()
                .collect(Collectors.toMap(Quyen::getMaQuyen, Function.identity()));
        Set<Quyen> ketQua = new HashSet<>();
        for (Integer ma : dsMaQuyen) {
            Quyen quyen = quyenTheoMa.get(ma);
            if (quyen == null) {
                throw new NghiepVuException("Dữ liệu phân quyền không hợp lệ.");
            }
            ketQua.add(quyen);
        }
        return ketQua;
    }

    private Set<String> maChucNang(Collection<Quyen> dsQuyen) {
        return dsQuyen.stream().map(Quyen::getMaChucNang).collect(Collectors.toCollection(HashSet::new));
    }
}
