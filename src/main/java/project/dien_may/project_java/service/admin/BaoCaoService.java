package project.dien_may.project_java.service.admin;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.dien_may.project_java.dto.baocao.DoanhThuNgay;
import project.dien_may.project_java.dto.baocao.SanPhamBanChay;
import project.dien_may.project_java.dto.baocao.TonKhoDanhMuc;
import project.dien_may.project_java.entity.SanPham;
import project.dien_may.project_java.entity.TinhTrangTonKho;
import project.dien_may.project_java.entity.TrangThaiDonHang;
import project.dien_may.project_java.repository.ChiTietDonHangRepository;
import project.dien_may.project_java.repository.DonHangRepository;
import project.dien_may.project_java.repository.SanPhamRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Doanh thu và sản phẩm bán chạy chỉ tính đơn đã giao, theo ngày đặt hàng,
 * để các báo cáo khớp nhau trên cùng một khoảng thời gian.
 */
@Service
public class BaoCaoService {

    private static final int SO_NAM_XEM_TOI_THIEU = 5;

    private final DonHangRepository donHangRepository;
    private final ChiTietDonHangRepository chiTietDonHangRepository;
    private final SanPhamRepository sanPhamRepository;

    public BaoCaoService(DonHangRepository donHangRepository, ChiTietDonHangRepository chiTietDonHangRepository,
                         SanPhamRepository sanPhamRepository) {
        this.donHangRepository = donHangRepository;
        this.chiTietDonHangRepository = chiTietDonHangRepository;
        this.sanPhamRepository = sanPhamRepository;
    }

    /** chuaToi: tháng còn ở tương lai, chưa có số liệu chứ không phải doanh thu bằng 0. */
    public record DoanhThuThang(int thang, long soDon, BigDecimal tienGiam, BigDecimal doanhThu, boolean chuaToi) {
    }

    public record MocTruc(String nhan, double phanTram) {
    }

    /** phanTram là chiều cao của điểm so với đỉnh trục, chỉ có nghĩa khi tháng đã tới. */
    public record DiemDuong(int thang, BigDecimal doanhThu, long soDon, double phanTram, boolean chuaToi) {
    }

    /** duongCong và vungTo là dữ liệu path của SVG, hệ tọa độ 100 x 100. */
    public record BieuDoDuong(int nam, List<DiemDuong> diem, List<MocTruc> moc, String duongCong, String vungTo) {
    }

    public record BaoCaoNam(int nam, List<DoanhThuThang> dsThang, BieuDoDuong bieuDo, long soDon,
                            BigDecimal tongTienGiam, BigDecimal tongDoanhThu, BigDecimal giaTriDonTrungBinh) {
    }

    public record CotSanPham(SanPhamBanChay sanPham, double phanTram) {
    }

    public record BieuDoSanPham(List<CotSanPham> cot, List<MocTruc> moc) {
    }

    public record BaoCaoThang(int nam, int thang, long soDon, BigDecimal tienGiam, BigDecimal doanhThu,
                              long tongSoLuongBan, BieuDoSanPham bieuDo) {
    }

    public record DongBanChay(SanPhamBanChay sanPham, double phanTram) {
    }

    public record BaoCaoTonKho(List<TonKhoDanhMuc> theoDanhMuc, List<SanPham> dsCanNhap, long tongSanPham,
                               long tongSoLuong, BigDecimal tongGiaTri, long soHetHang, long soSapHet) {
    }

    /** Các năm có thể xem báo cáo, mới nhất trước: ít nhất 5 năm gần nhất, xa hơn nếu có đơn cũ hơn. */
    @Transactional(readOnly = true)
    public List<Integer> dsNam() {
        int namNay = Year.now().getValue();
        int namCuNhat = namNay - SO_NAM_XEM_TOI_THIEU + 1;
        Integer namDonDauTien = donHangRepository.namDauTien();
        if (namDonDauTien != null) {
            namCuNhat = Math.min(namCuNhat, namDonDauTien);
        }
        List<Integer> ds = new ArrayList<>();
        for (int nam = namNay; nam >= namCuNhat; nam--) {
            ds.add(nam);
        }
        return ds;
    }

    @Transactional(readOnly = true)
    public BigDecimal tongDoanhThu(LocalDate tuNgay, LocalDate denNgay) {
        return doanhThuTrongKhoang(tuNgay.atStartOfDay(), denNgay.plusDays(1).atStartOfDay()).stream()
                .map(DoanhThuNgay::doanhThu).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transactional(readOnly = true)
    public BaoCaoNam doanhThuTheoNam(int nam) {
        long[] soDon = new long[13];
        BigDecimal[] tienGiam = new BigDecimal[13];
        BigDecimal[] doanhThu = new BigDecimal[13];
        for (int t = 1; t <= 12; t++) {
            tienGiam[t] = BigDecimal.ZERO;
            doanhThu[t] = BigDecimal.ZERO;
        }
        for (DoanhThuNgay d : doanhThuTrongKhoang(LocalDate.of(nam, 1, 1).atStartOfDay(),
                LocalDate.of(nam + 1, 1, 1).atStartOfDay())) {
            soDon[d.thang()] += d.soDon();
            tienGiam[d.thang()] = tienGiam[d.thang()].add(d.tienGiam());
            doanhThu[d.thang()] = doanhThu[d.thang()].add(d.doanhThu());
        }

        YearMonth thangNay = YearMonth.now();
        List<DoanhThuThang> dsThang = new ArrayList<>();
        long tongDon = 0;
        BigDecimal tongGiam = BigDecimal.ZERO;
        BigDecimal tong = BigDecimal.ZERO;
        for (int t = 1; t <= 12; t++) {
            dsThang.add(new DoanhThuThang(t, soDon[t], tienGiam[t], doanhThu[t], YearMonth.of(nam, t).isAfter(thangNay)));
            tongDon += soDon[t];
            tongGiam = tongGiam.add(tienGiam[t]);
            tong = tong.add(doanhThu[t]);
        }

        BigDecimal trungBinh = tongDon == 0 ? BigDecimal.ZERO
                : tong.divide(BigDecimal.valueOf(tongDon), 0, RoundingMode.HALF_UP);
        return new BaoCaoNam(nam, dsThang, veBieuDoDuong(nam, dsThang), tongDon, tongGiam, tong, trungBinh);
    }

    @Transactional(readOnly = true)
    public BaoCaoThang doanhThuTheoThang(int nam, int thang) {
        YearMonth ky = YearMonth.of(nam, thang);
        LocalDateTime tu = ky.atDay(1).atStartOfDay();
        LocalDateTime den = ky.plusMonths(1).atDay(1).atStartOfDay();

        long soDon = 0;
        BigDecimal tienGiam = BigDecimal.ZERO;
        BigDecimal doanhThu = BigDecimal.ZERO;
        for (DoanhThuNgay d : doanhThuTrongKhoang(tu, den)) {
            soDon += d.soDon();
            tienGiam = tienGiam.add(d.tienGiam());
            doanhThu = doanhThu.add(d.doanhThu());
        }

        List<SanPhamBanChay> dsSanPham = chiTietDonHangRepository.banChay(TrangThaiDonHang.DA_GIAO, tu, den, null,
                Pageable.unpaged());
        long tongSoLuong = dsSanPham.stream().mapToLong(SanPhamBanChay::soLuongBan).sum();
        return new BaoCaoThang(nam, thang, soDon, tienGiam, doanhThu, tongSoLuong, veBieuDoSanPham(dsSanPham));
    }

    @Transactional(readOnly = true)
    public List<DongBanChay> banChay(LocalDate tuNgay, LocalDate denNgay, Integer maDanhMuc, int soLuong) {
        List<SanPhamBanChay> ds = chiTietDonHangRepository.banChay(TrangThaiDonHang.DA_GIAO,
                tuNgay.atStartOfDay(), denNgay.plusDays(1).atStartOfDay(), maDanhMuc, PageRequest.of(0, soLuong));
        long lonNhat = ds.isEmpty() ? 1 : ds.get(0).soLuongBan();
        return ds.stream()
                .map(sp -> new DongBanChay(sp, sp.soLuongBan() * 100.0 / lonNhat))
                .toList();
    }

    @Transactional(readOnly = true)
    public BaoCaoTonKho tonKho() {
        List<TonKhoDanhMuc> theoDanhMuc = sanPhamRepository.tonKhoTheoDanhMuc();
        List<SanPham> dsCanNhap = sanPhamRepository.danhSachCanNhap();
        long tongSanPham = theoDanhMuc.stream().mapToLong(TonKhoDanhMuc::soSanPham).sum();
        long tongSoLuong = theoDanhMuc.stream().mapToLong(TonKhoDanhMuc::soLuong).sum();
        BigDecimal tongGiaTri = theoDanhMuc.stream().map(TonKhoDanhMuc::giaTri).reduce(BigDecimal.ZERO, BigDecimal::add);
        long soHet = dsCanNhap.stream().filter(s -> s.getTinhTrangTonKho() == TinhTrangTonKho.HET).count();
        return new BaoCaoTonKho(theoDanhMuc, dsCanNhap, tongSanPham, tongSoLuong, tongGiaTri, soHet,
                dsCanNhap.size() - soHet);
    }

    private List<DoanhThuNgay> doanhThuTrongKhoang(LocalDateTime tu, LocalDateTime den) {
        return donHangRepository.doanhThuTheoNgay(TrangThaiDonHang.DA_GIAO, tu, den);
    }

    private BieuDoDuong veBieuDoDuong(int nam, List<DoanhThuThang> dsThang) {
        BigDecimal lonNhat = dsThang.stream().map(DoanhThuThang::doanhThu)
                .max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        double dinhTruc = lamTronDinhTruc(lonNhat.doubleValue());

        List<DiemDuong> dsDiem = new ArrayList<>();
        List<double[]> toaDo = new ArrayList<>();
        for (DoanhThuThang t : dsThang) {
            double phanTram = t.doanhThu().doubleValue() * 100 / dinhTruc;
            dsDiem.add(new DiemDuong(t.thang(), t.doanhThu(), t.soDon(), phanTram, t.chuaToi()));
            if (!t.chuaToi()) {
                // Điểm nằm giữa dải của tháng, khớp với cột nhãn dàn đều bên dưới
                toaDo.add(new double[]{(t.thang() - 0.5) * 100 / 12, 100 - phanTram});
            }
        }

        List<MocTruc> dsMoc = new ArrayList<>();
        for (int i = 0; i <= 4; i++) {
            dsMoc.add(new MocTruc(rutGonTien(dinhTruc * i / 4), i * 25.0));
        }

        String duongCong = duongCongDonDieu(toaDo);
        String vungTo = toaDo.size() < 2 ? "" : duongCong
                + " L" + so(toaDo.get(toaDo.size() - 1)[0]) + " 100 L" + so(toaDo.get(0)[0]) + " 100 Z";
        return new BieuDoDuong(nam, dsDiem, dsMoc, duongCong, vungTo);
    }

    /**
     * Đường cong đơn điệu (Fritsch–Butland): mượt nhưng không lượn quá điểm dữ liệu,
     * nên không vẽ ra doanh thu âm hay đỉnh giả giữa hai tháng.
     */
    private String duongCongDonDieu(List<double[]> diem) {
        int n = diem.size();
        if (n < 2) {
            return "";
        }
        double[] doDoc = new double[n - 1];
        for (int i = 0; i < n - 1; i++) {
            doDoc[i] = (diem.get(i + 1)[1] - diem.get(i)[1]) / (diem.get(i + 1)[0] - diem.get(i)[0]);
        }
        double[] tiepTuyen = new double[n];
        tiepTuyen[0] = doDoc[0];
        tiepTuyen[n - 1] = doDoc[n - 2];
        for (int i = 1; i < n - 1; i++) {
            tiepTuyen[i] = doDoc[i - 1] * doDoc[i] <= 0 ? 0
                    : 2 * doDoc[i - 1] * doDoc[i] / (doDoc[i - 1] + doDoc[i]);
        }

        StringBuilder path = new StringBuilder("M" + so(diem.get(0)[0]) + " " + so(diem.get(0)[1]));
        for (int i = 0; i < n - 1; i++) {
            double[] a = diem.get(i);
            double[] b = diem.get(i + 1);
            double doan = (b[0] - a[0]) / 3;
            path.append(" C").append(so(a[0] + doan)).append(' ').append(so(a[1] + tiepTuyen[i] * doan))
                    .append(' ').append(so(b[0] - doan)).append(' ').append(so(b[1] - tiepTuyen[i + 1] * doan))
                    .append(' ').append(so(b[0])).append(' ').append(so(b[1]));
        }
        return path.toString();
    }

    private BieuDoSanPham veBieuDoSanPham(List<SanPhamBanChay> dsSanPham) {
        long lonNhat = dsSanPham.stream().mapToLong(SanPhamBanChay::soLuongBan).max().orElse(0);
        long buoc = buocTrucSoNguyen(lonNhat);
        long dinhTruc = buoc * 4;

        List<CotSanPham> dsCot = dsSanPham.stream()
                .map(sp -> new CotSanPham(sp, sp.soLuongBan() * 100.0 / dinhTruc))
                .toList();
        List<MocTruc> dsMoc = new ArrayList<>();
        for (int i = 0; i <= 4; i++) {
            dsMoc.add(new MocTruc(String.valueOf(buoc * i), i * 25.0));
        }
        return new BieuDoSanPham(dsCot, dsMoc);
    }

    /** Bước nguyên 1, 2, 5, 10, 20... đủ để bốn khoảng trên trục chứa được giá trị lớn nhất. */
    private long buocTrucSoNguyen(long lonNhat) {
        for (long coSo = 1; ; coSo *= 10) {
            for (long heSo : new long[]{1, 2, 5}) {
                if (coSo * heSo * 4 >= lonNhat) {
                    return coSo * heSo;
                }
            }
        }
    }

    /** Làm tròn lên số đẹp để bốn mốc trên trục đều là số dễ đọc. */
    private double lamTronDinhTruc(double giaTri) {
        if (giaTri <= 0) {
            return 1_000_000;
        }
        double coSo = Math.pow(10, Math.floor(Math.log10(giaTri)));
        for (double heSo : new double[]{1, 2, 4, 6, 8, 10}) {
            if (coSo * heSo >= giaTri) {
                return coSo * heSo;
            }
        }
        return coSo * 10;
    }

    private String rutGonTien(double giaTri) {
        if (giaTri >= 1_000_000_000) {
            return soGon(giaTri / 1_000_000_000) + " tỷ";
        }
        if (giaTri >= 1_000_000) {
            return soGon(giaTri / 1_000_000) + " tr";
        }
        return giaTri == 0 ? "0" : soGon(giaTri / 1_000) + " k";
    }

    private String soGon(double so) {
        return BigDecimal.valueOf(so).setScale(1, RoundingMode.HALF_UP).stripTrailingZeros()
                .toPlainString().replace('.', ',');
    }

    private String so(double giaTri) {
        return String.format(Locale.ROOT, "%.2f", giaTri);
    }
}
