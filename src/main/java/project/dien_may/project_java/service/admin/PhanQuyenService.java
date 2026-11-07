package project.dien_may.project_java.service.admin;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.dien_may.project_java.config.NhomQuyen;
import project.dien_may.project_java.entity.Quyen;
import project.dien_may.project_java.entity.VaiTro;
import project.dien_may.project_java.repository.QuyenRepository;
import project.dien_may.project_java.repository.VaiTroRepository;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PhanQuyenService {

    public static final String VAI_TRO_QUAN_TRI = NhomQuyen.QUAN_TRI;

    /** Quản trị viên luôn giữ quyền này, nếu không sẽ không ai cấp lại quyền được nữa. */
    public static final String QUYEN_KHONG_THE_GO = "PHANQUYEN_QUANLY";

    private final VaiTroRepository vaiTroRepository;
    private final QuyenRepository quyenRepository;

    public PhanQuyenService(VaiTroRepository vaiTroRepository, QuyenRepository quyenRepository) {
        this.vaiTroRepository = vaiTroRepository;
        this.quyenRepository = quyenRepository;
    }

    @Transactional(readOnly = true)
    public List<VaiTro> dsVaiTro() {
        return vaiTroRepository.findAll(Sort.by("maVaiTro"));
    }

    /** Quyền gom theo nhóm chức năng, giữ thứ tự xuất hiện theo MaQuyen. */
    @Transactional(readOnly = true)
    public Map<String, List<Quyen>> quyenTheoNhom() {
        return quyenRepository.findAll(Sort.by("maQuyen")).stream()
                .collect(Collectors.groupingBy(Quyen::getNhomChucNang, LinkedHashMap::new, Collectors.toList()));
    }

    /** Mã vai trò -> tập mã quyền đang được cấp. */
    @Transactional(readOnly = true)
    public Map<Integer, Set<Integer>> quyenDaCap() {
        Map<Integer, Set<Integer>> ketQua = new HashMap<>();
        for (VaiTro vaiTro : vaiTroRepository.findAll()) {
            ketQua.put(vaiTro.getMaVaiTro(),
                    vaiTro.getDsQuyen().stream().map(Quyen::getMaQuyen).collect(Collectors.toSet()));
        }
        return ketQua;
    }

    /** Mã vai trò -> (mã quyền không được gán -> lý do), để khóa ô tương ứng trên màn hình. */
    @Transactional(readOnly = true)
    public Map<Integer, Map<Integer, String>> oBiKhoa() {
        List<Quyen> dsQuyen = quyenRepository.findAll();
        Map<Integer, Map<Integer, String>> ketQua = new HashMap<>();
        for (VaiTro vaiTro : vaiTroRepository.findAll()) {
            Map<Integer, String> lyDoTheoQuyen = new HashMap<>();
            for (Quyen quyen : dsQuyen) {
                String lyDo = NhomQuyen.lyDoKhoa(vaiTro.getTenVaiTro(), quyen.getMaChucNang());
                if (lyDo != null) {
                    lyDoTheoQuyen.put(quyen.getMaQuyen(), lyDo);
                }
            }
            ketQua.put(vaiTro.getMaVaiTro(), lyDoTheoQuyen);
        }
        return ketQua;
    }

    /** @param dsCap các cặp "maVaiTro:maQuyen" được tick trên màn hình */
    @Transactional
    public void capNhat(Collection<String> dsCap) {
        Map<Integer, Quyen> quyenTheoMa = quyenRepository.findAll().stream()
                .collect(Collectors.toMap(Quyen::getMaQuyen, Function.identity()));
        List<VaiTro> dsVaiTro = vaiTroRepository.findAll();
        Map<Integer, VaiTro> vaiTroTheoMa = dsVaiTro.stream()
                .collect(Collectors.toMap(VaiTro::getMaVaiTro, Function.identity()));

        // Kiểm tra hết các cặp trước khi đổi dữ liệu, một cặp sai thì không lưu gì
        Map<Integer, Set<Quyen>> moi = new HashMap<>();
        dsVaiTro.forEach(vt -> moi.put(vt.getMaVaiTro(), new HashSet<>()));
        for (String cap : dsCap) {
            String[] phan = cap.split(":");
            VaiTro vaiTro = phan.length == 2 ? vaiTroTheoMa.get(soNguyen(phan[0])) : null;
            Quyen quyen = phan.length == 2 ? quyenTheoMa.get(soNguyen(phan[1])) : null;
            if (vaiTro == null || quyen == null) {
                throw new NghiepVuException("Dữ liệu phân quyền không hợp lệ.");
            }
            if (!NhomQuyen.choPhepGan(vaiTro.getTenVaiTro(), quyen.getMaChucNang())) {
                throw new NghiepVuException("Không thể cấp quyền " + quyen.getTenQuyen()
                        + " cho vai trò " + vaiTro.getTenHienThi() + ".");
            }
            moi.get(vaiTro.getMaVaiTro()).add(quyen);
        }

        for (VaiTro vaiTro : dsVaiTro) {
            Set<Quyen> quyenMoi = moi.get(vaiTro.getMaVaiTro());
            if (VAI_TRO_QUAN_TRI.equals(vaiTro.getTenVaiTro())) {
                quyenTheoMa.values().stream()
                        .filter(q -> QUYEN_KHONG_THE_GO.equals(q.getMaChucNang()))
                        .forEach(quyenMoi::add);
            }
            vaiTro.getDsQuyen().retainAll(quyenMoi);
            vaiTro.getDsQuyen().addAll(quyenMoi);
        }
    }

    private Integer soNguyen(String s) {
        try {
            return Integer.valueOf(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
