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

/**
 * Quản trị viên mặc nhiên có mọi quyền. Nhân viên kho nhận quyền của vai trò (áp dụng cho toàn thể nhân viên),
 * sau đó từng người có thể được cấp thêm hoặc thu hồi riêng. Quyền của khách hàng không chỉnh ở đây.
 */
@Service
public class PhanQuyenService {

    private static final String VAI_TRO_QUAN_TRI = "QUAN_TRI";
    private static final String VAI_TRO_NHAN_VIEN = "NHAN_VIEN_KHO";
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

    /** Quyền gom theo nhóm chức năng, theo thứ tự MaQuyen; Tổng quan đứng đầu như trên menu. */
    @Transactional(readOnly = true)
    public Map<String, List<Quyen>> quyenTheoNhom() {
        return quyenRepository.findAll(Sort.by("maQuyen")).stream()
                .sorted(Comparator.comparing((Quyen q) -> !QUYEN_TONG_QUAN.equals(q.getMaChucNang())))
                .collect(Collectors.groupingBy(Quyen::getNhomChucNang, LinkedHashMap::new, Collectors.toList()));
    }

    /** Mã chức năng tài khoản này thực sự được dùng, sau khi áp dụng phần chỉnh riêng. */
    @Transactional(readOnly = true)
    public Set<String> quyenHieuLuc(TaiKhoan taiKhoan) {
        String tenVaiTro = taiKhoan.getVaiTro().getTenVaiTro();
        if (VAI_TRO_QUAN_TRI.equals(tenVaiTro)) {
            return maChucNang(quyenRepository.findAll());
        }
        Set<String> ketQua = maChucNang(taiKhoan.getVaiTro().getDsQuyen());
        if (VAI_TRO_NHAN_VIEN.equals(tenVaiTro)) {
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

    @Transactional(readOnly = true)
    public Set<String> quyenToanTheNhanVien() {
        return maChucNang(vaiTroNhanVien().getDsQuyen());
    }

    /** @param dsMaQuyen các quyền được tick; thay đổi có hiệu lực với mọi nhân viên, trừ phần đã chỉnh riêng */
    @Transactional
    public void capNhatToanTheNhanVien(Collection<Integer> dsMaQuyen) {
        Set<Quyen> moi = layQuyen(dsMaQuyen);
        Set<Quyen> dsQuyen = vaiTroNhanVien().getDsQuyen();
        dsQuyen.retainAll(moi);
        dsQuyen.addAll(moi);
    }

    @Transactional(readOnly = true)
    public List<TaiKhoan> dsNhanVien() {
        return taiKhoanRepository.findByVaiTroTenVaiTroOrderByHoTenAsc(VAI_TRO_NHAN_VIEN);
    }

    /** Chỉ nhân viên kho mới có phân quyền riêng; tài khoản khác coi như không tồn tại ở màn hình này. */
    @Transactional(readOnly = true)
    public TaiKhoan nhanVien(Integer maTaiKhoan) {
        return taiKhoanRepository.findById(maTaiKhoan)
                .filter(t -> VAI_TRO_NHAN_VIEN.equals(t.getVaiTro().getTenVaiTro()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    /** Mã chức năng đang khác với quyền của toàn thể nhân viên. */
    @Transactional(readOnly = true)
    public Set<String> quyenChinhRieng(Integer maTaiKhoan) {
        return taiKhoanQuyenRepository.findByTaiKhoanMaTaiKhoan(maTaiKhoan).stream()
                .map(rieng -> rieng.getQuyen().getMaChucNang())
                .collect(Collectors.toSet());
    }

    /**
     * Chỉ lưu phần khác với quyền của vai trò, nên ô nào tick giống toàn thể thì người đó tiếp tục theo toàn thể.
     *
     * @param dsMaQuyen các quyền được tick cho nhân viên này
     */
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

    private VaiTro vaiTroNhanVien() {
        return vaiTroRepository.findAll().stream()
                .filter(v -> VAI_TRO_NHAN_VIEN.equals(v.getTenVaiTro()))
                .findFirst().orElseThrow();
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
