package project.dien_may.project_java.service.admin;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

    public static final String VAI_TRO_QUAN_TRI = "QUAN_TRI";

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

    /** @param dsCap các cặp "maVaiTro:maQuyen" được tick trên màn hình */
    @Transactional
    public void capNhat(Collection<String> dsCap) {
        Map<Integer, Quyen> quyenTheoMa = quyenRepository.findAll().stream()
                .collect(Collectors.toMap(Quyen::getMaQuyen, Function.identity()));
        List<VaiTro> dsVaiTro = vaiTroRepository.findAll();

        Map<Integer, Set<Quyen>> moi = new HashMap<>();
        dsVaiTro.forEach(vt -> moi.put(vt.getMaVaiTro(), new HashSet<>()));
        for (String cap : dsCap) {
            String[] phan = cap.split(":");
            Set<Quyen> dich = phan.length == 2 ? moi.get(soNguyen(phan[0])) : null;
            Quyen quyen = phan.length == 2 ? quyenTheoMa.get(soNguyen(phan[1])) : null;
            if (dich == null || quyen == null) {
                throw new NghiepVuException("Dữ liệu phân quyền không hợp lệ.");
            }
            dich.add(quyen);
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
