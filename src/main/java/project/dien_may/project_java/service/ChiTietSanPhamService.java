package project.dien_may.project_java.service;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.dien_may.project_java.dto.DanhGiaHienThi;
import project.dien_may.project_java.entity.DanhGia;
import project.dien_may.project_java.entity.SanPham;
import project.dien_may.project_java.entity.ThongSoKyThuat;
import project.dien_may.project_java.repository.DanhGiaRepository;
import project.dien_may.project_java.repository.SanPhamRepository;
import project.dien_may.project_java.repository.ThongSoKyThuatRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class ChiTietSanPhamService {
    private final SanPhamService sanPhamService;
    private final SanPhamRepository sanPhamRepository;
    private final ThongSoKyThuatRepository thongSoKyThuatRepository;
    private final DanhGiaRepository danhGiaRepository;

    private static final Map<String, String> ANH_CO_SAN = Map.ofEntries(
            Map.entry("SM-S928B", "/images/sanpham/dien-thoai/dien-thoai-samsung-galaxy-s24-ultra.png"),
            Map.entry("MU793VN-A", "/images/sanpham/dien-thoai/dien-thoai-iphone-15-pro-max.png"),
            Map.entry("23129RAA4G", "/images/sanpham/dien-thoai/dien-thoai-xiaomi-redmi-note-13.png"),
            Map.entry("MUWD3ZA-A", "/images/sanpham/dien-thoai/may-tinh-bang-ipad-air-m2-11.png"),
            Map.entry("SM-X510", "/images/sanpham/dien-thoai/may-tinh-bang-samsung-tab-s9-fe.png"),
            Map.entry("MRXN3SA-A", "/images/sanpham/laptop/laptop-macbook-air-m3-13.png"),
            Map.entry("INS3530-I5", "/images/sanpham/laptop/laptop-dell-inspiron-15-3530.png"),
            Map.entry("A1505VA-L1114W", "/images/sanpham/laptop/laptop-asus-vivobook-15.png"),
            Map.entry("UA55AU7700", "/images/sanpham/tivi/tivi-samsung-ua55.png"),
            Map.entry("OLED55C4PSA", "/images/sanpham/tivi/tivi-lg-oled-evo-55.png"),
            Map.entry("K-50S30", "/images/sanpham/tivi/tivi-sony-4k-50.png"),
            Map.entry("NR-BC360QKVN", "/images/sanpham/tu-lanh/tu-lanh-panasonic-nr-bc360qkvn.png"),
            Map.entry("RT22M4032BY", "/images/sanpham/tu-lanh/tu-lanh-samsung-inverter-236-lit.png"),
            Map.entry("GN-D332BL", "/images/sanpham/tu-lanh/tu-lanh-lg-inverter-335-lit.png"),
            Map.entry("FV1410S4P", "/images/sanpham/may-giat/may-giat-lg-fv1410s4p.png"),
            Map.entry("EWF9024P5WB", "/images/sanpham/may-giat/may-giat-electrolux-inverter-9kg.png"),
            Map.entry("FTKB25WAVMV", "/images/sanpham/dieu-hoa/dieu-hoa-daikin-ftkb25wavmv.png"),
            Map.entry("CU-PU12AKH-8", "/images/sanpham/dieu-hoa/dieu-hoa-panasonic-inverter-1-5hp.png"),
            Map.entry("KG10A3", "/images/sanpham/gia-dung/may-loc-nuoc-kangaroo-ro-10-loi.png"),
            Map.entry("KAQ-U95", "/images/sanpham/gia-dung/may-loc-nuoc-karofi-ro-9-loi.png"),
            Map.entry("RC-18NMFVN", "/images/sanpham/gia-dung/noi-com-dien-toshiba-1-8-lit.png"),
            Map.entry("CR-1020F", "/images/sanpham/gia-dung/noi-com-dien-cuckoo-1-8-lit.png"),
            Map.entry("R-G225VN-BK", "/images/sanpham/gia-dung/lo-vi-song-sharp-20-lit.png"),
            Map.entry("EMG23D22B", "/images/sanpham/gia-dung/lo-vi-song-electrolux-23-lit.png"),
            Map.entry("BHR5988EU", "/images/sanpham/gia-dung/robot-hut-bui-xiaomi-s10.png"),
            Map.entry("SV10K", "/images/sanpham/gia-dung/may-hut-bui-dyson-v8-slim.png"),
            Map.entry("F-409KB", "/images/sanpham/gia-dung/quat-dung-panasonic-f-409k.png"),
            Map.entry("F-LSA10-H", "/images/sanpham/gia-dung/quat-dung-toshiba-f-lsa10.png"),
            Map.entry("MTJV3", "/images/sanpham/loa/tai-nghe-airpods-pro-2-usbc.png"),
            Map.entry("WH-1000XM5", "/images/sanpham/loa/tai-nghe-sony-wh-1000xm5.png"),
            Map.entry("JBLCHARGE5", "/images/sanpham/loa/loa-jbl-charge-5.png"),
            Map.entry("EMBERTON-II", "/images/sanpham/loa/loa-marshall-emberton-ii.png"),
            Map.entry("MR8W3", "/images/sanpham/phu-kien/dong-ho-apple-watch-series-9-41mm.png"),
            Map.entry("SM-R930", "/images/sanpham/phu-kien/dong-ho-samsung-galaxy-watch6-40mm.png"),
            Map.entry("EOSR50-KIT", "/images/sanpham/phu-kien/may-anh-canon-eos-r50-kit.png"),
            Map.entry("ILCE-ZVE10L", "/images/sanpham/phu-kien/may-anh-sony-zv-e10-kit.png"),
            Map.entry("27GN800-B", "/images/sanpham/phu-kien/man-hinh-lg-ultragear-27.png"),
            Map.entry("U2424H", "/images/sanpham/phu-kien/man-hinh-dell-ultrasharp-24.png"),
            Map.entry("LBP2900", "/images/sanpham/phu-kien/may-in-canon-lbp2900.png"),
            Map.entry("9YF83A", "/images/sanpham/phu-kien/may-in-hp-m211dw.png"),
            Map.entry("920-011558", "/images/sanpham/phu-kien/ban-phim-logitech-mx-keys-s.png"),
            Map.entry("910-006561", "/images/sanpham/phu-kien/chuot-logitech-mx-master-3s.png"),
            Map.entry("A1263", "/images/sanpham/phu-kien/pin-sac-du-phong-anker-10000mah.png"),
            Map.entry("EP-TA800N", "/images/sanpham/phu-kien/cu-sac-nhanh-samsung-25w.png")
    );

    public ChiTietSanPhamService(SanPhamService sanPhamService,
                                 SanPhamRepository sanPhamRepository,
                                 ThongSoKyThuatRepository thongSoKyThuatRepository,
                                 DanhGiaRepository danhGiaRepository) {
        this.sanPhamService = sanPhamService;
        this.sanPhamRepository = sanPhamRepository;
        this.thongSoKyThuatRepository = thongSoKyThuatRepository;
        this.danhGiaRepository = danhGiaRepository;
    }

    @Transactional(readOnly = true)
    public Optional<SanPham> laySanPham(Integer maSanPham) {
        Optional<SanPham> sanPham = sanPhamService.timSanPhamTheoMa(maSanPham);
        sanPham.ifPresent(sp -> sp.getDanhMuc().getTenDanhMuc());
        return sanPham;
    }

    @Transactional(readOnly = true)
    public List<ThongSoKyThuat> layThongSo(Integer maSanPham) {
        return thongSoKyThuatRepository.findBySanPham_MaSanPhamOrderByThuTuAscMaThongSoAsc(maSanPham);
    }

    @Transactional(readOnly = true)
    public List<SanPham> laySanPhamTuongTu(SanPham sanPham) {
        if (sanPham.getDanhMuc() == null) return List.of();
        return sanPhamRepository.findTop6ByDanhMuc_MaDanhMucAndDangKinhDoanhTrueAndMaSanPhamNotOrderByMaSanPhamAsc(
                sanPham.getDanhMuc().getMaDanhMuc(), sanPham.getMaSanPham());
    }

    @Transactional(readOnly = true)
    public List<DanhGiaHienThi> layDanhGia(Integer maSanPham) {
        return danhGiaRepository.layDanhGiaSanPham(maSanPham).stream()
                .map(dg -> new DanhGiaHienThi(
                        anTenNguoiDanhGia(dg.getTaiKhoan().getHoTen()),
                        dg.getSoSao(), dg.getNoiDungDanhGia(), dg.getNgayDanhGia()))
                .toList();
    }

    private String anTenNguoiDanhGia(String hoTen) {
        if (hoTen == null || hoTen.isBlank()) return "Khách hàng PDMJV";
        String[] phan = hoTen.trim().split("\\s+");
        if (phan.length == 1) return phan[0].substring(0, 1) + "***";
        return phan[0] + " " + phan[phan.length - 1].substring(0, 1) + "***";
    }


    public List<String> layDanhSachAnh(SanPham sanPham) {
        Set<String> danhSach = new LinkedHashSet<>();
        String maModel = sanPham.getMaModel();
        String anhChinh = ANH_CO_SAN.get(maModel);
        if (anhTonTai(anhChinh)) {
            danhSach.add(anhChinh);
            int viTriDuoi = anhChinh.lastIndexOf('.');
            String tenGoc = anhChinh.substring(0, viTriDuoi);
            String phanMoRong = anhChinh.substring(viTriDuoi);
            for (int i = 2; i <= 4; i++) {
                String anhPhu = tenGoc + "-" + i + phanMoRong;
                if (anhTonTai(anhPhu)) danhSach.add(anhPhu);
            }
        }
        if (maModel != null && maModel.matches("[a-zA-Z0-9_-]+")) {
            String thuMuc = "/images/sanpham/chi-tiet/" + maModel + "/";
            try {
                Resource[] resources = new PathMatchingResourcePatternResolver()
                        .getResources("classpath*:static" + thuMuc + "*");
                List<String> tenAnh = new ArrayList<>();
                for (Resource resource : resources) {
                    String ten = resource.getFilename();
                    if (resource.isReadable() && ten != null && laAnhHopLe(ten)) tenAnh.add(ten);
                }
                tenAnh.sort(String.CASE_INSENSITIVE_ORDER);
                for (String ten : tenAnh) danhSach.add(thuMuc + ten);
            } catch (IOException ignored) {
            }
        }

        if (danhSach.isEmpty() && anhTonTai(sanPham.getHinhAnh())) {
            danhSach.add(sanPham.getHinhAnh());
        }
        return List.copyOf(danhSach);
    }

    public String layAnhDaiDien(SanPham sanPham) {
        List<String> anh = layDanhSachAnh(sanPham);
        return anh.isEmpty() ? null : anh.get(0);
    }
//    lam moi anh
    @Transactional(readOnly = true)
    public Map<Integer, String> layAnhSanPhamDaXem(List<Integer> maSanPham) {
        Map<Integer, String> ketQua = new java.util.LinkedHashMap<>();
        if (maSanPham == null) return ketQua;
        Set<Integer> maHopLe = new LinkedHashSet<>();
        for (Integer ma : maSanPham) {
            if (ma != null && ma > 0 && maHopLe.size() < 4) maHopLe.add(ma);
        }
        for (SanPham sp : sanPhamRepository.findAllById(maHopLe)) {
            if (!sp.isDangKinhDoanh()) continue;
            String anh = layAnhDaiDien(sp);
            if (anh != null) ketQua.put(sp.getMaSanPham(), anh);
        }
        return ketQua;
    }
    private boolean anhTonTai(String duongDan) {
        return duongDan != null && duongDan.startsWith("/images/")
                && !duongDan.contains("..")
                && new ClassPathResource("static" + duongDan).exists();
    }

    private boolean laAnhHopLe(String ten) {
        String lower = ten.toLowerCase(Locale.ROOT);
        return lower.endsWith(".png") || lower.endsWith(".jpg")
                || lower.endsWith(".jpeg") || lower.endsWith(".webp");
    }
}
