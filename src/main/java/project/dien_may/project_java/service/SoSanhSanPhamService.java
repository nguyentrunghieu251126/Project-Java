
package project.dien_may.project_java.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.dien_may.project_java.entity.SanPham;
import project.dien_may.project_java.entity.ThongSoKyThuat;
import project.dien_may.project_java.repository.SanPhamRepository;
import project.dien_may.project_java.repository.ThongSoKyThuatRepository;

import java.util.*;

@Service
public class SoSanhSanPhamService {
    private final SanPhamRepository sanPhamRepository;
    private final ThongSoKyThuatRepository thongSoKyThuatRepository;
    private final ChiTietSanPhamService chiTietSanPhamService;

    public SoSanhSanPhamService(SanPhamRepository sanPhamRepository, ThongSoKyThuatRepository thongSoKyThuatRepository, ChiTietSanPhamService chiTietSanPhamService) {
        this.sanPhamRepository = sanPhamRepository;
        this.thongSoKyThuatRepository = thongSoKyThuatRepository;
        this.chiTietSanPhamService = chiTietSanPhamService;
    }

    public record KetQuaSoSanh(List<SanPham> sanPhamDaChon, List<SanPham> sanPhamDeXuat, List<String> tenThongSo, Map<Integer, Map<String, String>> thongSoTheoSanPham, Map<Integer, String> anhSanPham, String thongBao) {}
    @Transactional(readOnly = true)
    public KetQuaSoSanh layDuLieu(List<Integer> maSanPham) {
        Set<Integer> maHopLe = new LinkedHashSet<>();
        if (maSanPham != null) {
            for (Integer id : maSanPham) {
                if (id != null && id > 0) maHopLe.add(id);
            }
        }
        String thongBao = null;
        if (maHopLe.size() > 3) {
            thongBao = "Chỉ được so sánh tối đa 3 sản phẩm.";
        }
        List<SanPham> daChon = new ArrayList<>();
        Integer danhMuc = null;
        for (Integer id : maHopLe) {
            if (daChon.size() >= 3) break;
            SanPham sp = sanPhamRepository.findById(id).orElse(null);
            if (sp == null || !sp.isDangKinhDoanh() || sp.getDanhMuc() == null) {
                thongBao = "Một số sản phẩm không tồn tại hoặc đã ngừng kinh doanh.";
                continue;
            }
            sp.getDanhMuc().getTenDanhMuc();
            Integer maDanhMuc = sp.getDanhMuc().getMaDanhMuc();
            if (danhMuc == null) danhMuc = maDanhMuc;
            if (!danhMuc.equals(maDanhMuc)) {
                thongBao = "Chỉ được so sánh các sản phẩm cùng danh mục.";
                continue;
            }
            daChon.add(sp);
        }

        List<SanPham> deXuat = danhMuc == null ? List.of() : sanPhamRepository.findByDanhMuc_MaDanhMucAndDangKinhDoanhTrue(danhMuc).stream().filter(sp -> daChon.stream().noneMatch(chon -> chon.getMaSanPham().equals(sp.getMaSanPham()))).limit(12).toList();
        Map<Integer, Map<String, String>> duLieuChuan = new LinkedHashMap<>();
        Map<String, String> tenHienThi = new LinkedHashMap<>();
        Map<Integer, String> anh = new LinkedHashMap<>();
        List<SanPham> tatCa = new ArrayList<>(daChon);
        tatCa.addAll(deXuat);
        for (SanPham sp : tatCa) {
            anh.put(sp.getMaSanPham(), chiTietSanPhamService.layAnhDaiDien(sp));
        }
        for (SanPham sp : daChon) {
            Map<String, String> chiTiet = new LinkedHashMap<>();
            for (ThongSoKyThuat ts : thongSoKyThuatRepository
                    .findBySanPham_MaSanPhamOrderByThuTuAscMaThongSoAsc(sp.getMaSanPham())) {
                if (ts.getTenThongSo() == null || ts.getTenThongSo().isBlank()) continue;
                String tenGoc = ts.getTenThongSo().trim().replaceAll("\\s+", " ");
                String khoaChuan = chuanHoaTenThongSo(tenGoc);
                if (khoaChuan.isBlank()) continue;
                tenHienThi.putIfAbsent(khoaChuan, tenGoc);
                String giaTri = ts.getGiaTri();
                if (giaTri != null && !giaTri.isBlank()) {
                    chiTiet.putIfAbsent(khoaChuan, giaTri.trim());
                }
            }
            duLieuChuan.put(sp.getMaSanPham(), chiTiet);
        }
        Map<Integer, Map<String, String>> thongSo = new LinkedHashMap<>();
        for (SanPham sp : daChon) {
            Map<String, String> giaTriTheoTen = new LinkedHashMap<>();
            Map<String, String> chiTiet = duLieuChuan.get(sp.getMaSanPham());
            for (Map.Entry<String, String> ten : tenHienThi.entrySet()) {
                String giaTri = chiTiet.get(ten.getKey());
                if (giaTri != null) giaTriTheoTen.put(ten.getValue(), giaTri);
            }
            thongSo.put(sp.getMaSanPham(), giaTriTheoTen);
        }
        List<String> tenThongSo = List.copyOf(tenHienThi.values());
        return new KetQuaSoSanh(daChon, deXuat, tenThongSo, thongSo, anh, thongBao);
    }
    private String chuanHoaTenThongSo(String ten) {
        return ten.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }
}
