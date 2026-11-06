package project.dien_may.project_java.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.dien_may.project_java.dto.SanPhamNoiBat;
import project.dien_may.project_java.entity.SanPham;
import project.dien_may.project_java.repository.SanPhamRepository;
import project.dien_may.project_java.repository.DanhGiaRepository;

import java.util.*;

@Service
public class SanPhamNoiBatService {
    private final SanPhamRepository sanPhamRepository;
    private final ChiTietSanPhamService chiTietSanPhamService;
    private final DanhGiaRepository danhGiaRepository;

    private record ThongKeDanhGia(long soLuot, String diem) {}

    public SanPhamNoiBatService(SanPhamRepository sanPhamRepository, ChiTietSanPhamService chiTietSanPhamService, DanhGiaRepository danhGiaRepository) {
        this.sanPhamRepository = sanPhamRepository;
        this.chiTietSanPhamService = chiTietSanPhamService;
        this.danhGiaRepository = danhGiaRepository;
    }

    // Danh sách đề xuất: 5 sản phẩm đang kinh doanh có mã mới nhất.
    @Transactional(readOnly = true)
    public List<SanPhamNoiBat> laySanPhamNoiBat() {
        List<SanPham> danhSach = sanPhamRepository.findTop5ByDangKinhDoanhTrueOrderByMaSanPhamDesc();
        Map<Integer, ThongKeDanhGia> thongKe = layThongKeDanhGia(danhSach);
        return danhSach.stream().map(sp -> chuyenThanhThe(sp, false, thongKe)).toList();
    }

    // Bán chạy thật: xếp theo tổng số lượng trong đơn đã giao.
    @Transactional(readOnly = true)
    public List<SanPhamNoiBat> laySanPhamBanChay() {
        List<Integer> maBanChay = sanPhamRepository.layMaSanPhamBanChay();
        Map<Integer, SanPham> theoMa = new HashMap<>();
        if (!maBanChay.isEmpty()) {
            sanPhamRepository.findAllById(maBanChay)
                    .forEach(sp -> theoMa.put(sp.getMaSanPham(), sp));
        }
        List<SanPham> ketQua = new ArrayList<>();
        Set<Integer> daThem = new HashSet<>();
        Set<Integer> banChayThucTe = new HashSet<>();
        for (Integer ma : maBanChay) {
            SanPham sp = theoMa.get(ma);
            if (sp != null && sp.isDangKinhDoanh() && daThem.add(ma)) {
                ketQua.add(sp);
                banChayThucTe.add(ma);
            }
        }
        if (ketQua.size() < 4) {
            for (SanPham sp : sanPhamRepository.findTop4ByDangKinhDoanhTrueOrderByMaSanPhamDesc()) {
                if (daThem.add(sp.getMaSanPham())) ketQua.add(sp);
                if (ketQua.size() == 4) break;
            }
        }
        Map<Integer, ThongKeDanhGia> thongKe = layThongKeDanhGia(ketQua);
        return ketQua.stream().map(sp -> chuyenThanhThe(
                sp, banChayThucTe.contains(sp.getMaSanPham()), thongKe)).toList();
    }

    private Map<Integer, ThongKeDanhGia> layThongKeDanhGia(List<SanPham> sanPham) {
        if (sanPham.isEmpty()) return Collections.emptyMap();
        List<Integer> maSanPham = sanPham.stream().map(SanPham::getMaSanPham).toList();
        Map<Integer, ThongKeDanhGia> ketQua = new HashMap<>();
        for (Object[] hang : danhGiaRepository.thongKeDanhGiaTheoSanPham(maSanPham)) {
            if (hang[0] == null || hang[1] == null || hang[2] == null) continue;
            int ma = ((Number) hang[0]).intValue();
            long soLuot = ((Number) hang[1]).longValue();
            double diem = ((Number) hang[2]).doubleValue();
            if (soLuot > 0) {
                ketQua.put(ma, new ThongKeDanhGia(soLuot, String.format(Locale.US, "%.1f", diem)));
            }
        }
        return ketQua;
    }

    private SanPhamNoiBat chuyenThanhThe(SanPham sanPham, boolean banChay, Map<Integer, ThongKeDanhGia> thongKe) {

        Integer maDanhMuc = sanPham.getDanhMuc() == null ? null : sanPham.getDanhMuc().getMaDanhMuc();
        String nhom = maDanhMuc != null && maDanhMuc >= 5 && maDanhMuc <= 12 ? "gia-dung" : "cong-nghe";
        ThongKeDanhGia danhGia = thongKe.get(sanPham.getMaSanPham());
        return new SanPhamNoiBat(sanPham.getMaSanPham(), sanPham.getTenSanPham(),
                sanPham.getThuongHieu(), sanPham.getDonGia(), sanPham.getSoLuong(),
                chiTietSanPhamService.layAnhDaiDien(sanPham), nhom, banChay,
                danhGia == null ? 0 : danhGia.soLuot(),
                danhGia == null ? null : danhGia.diem(),
                null, null);
    }
}
