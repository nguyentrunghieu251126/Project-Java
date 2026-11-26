package project.dien_may.project_java.service.admin;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import project.dien_may.project_java.entity.ChiTietYeuCauNhapHang;
import project.dien_may.project_java.entity.DonHang;
import project.dien_may.project_java.entity.SanPham;
import project.dien_may.project_java.entity.TaiKhoan;
import project.dien_may.project_java.entity.TinhTrangTonKho;
import project.dien_may.project_java.entity.TrangThaiDonHang;
import project.dien_may.project_java.entity.TrangThaiYeuCauNhap;
import project.dien_may.project_java.entity.YeuCauNhapHang;
import project.dien_may.project_java.repository.ChiTietYeuCauNhapHangRepository;
import project.dien_may.project_java.repository.DonHangRepository;
import project.dien_may.project_java.repository.SanPhamRepository;
import project.dien_may.project_java.repository.TaiKhoanRepository;
import project.dien_may.project_java.repository.YeuCauNhapHangRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Việc ở khâu kho: xem đơn hàng, lập và duyệt yêu cầu nhập (QT17, QT19 – QT21, NVK09). */
@Service
public class KhoAdminService {

    private final DonHangRepository donHangRepository;
    private final YeuCauNhapHangRepository yeuCauRepository;
    private final ChiTietYeuCauNhapHangRepository chiTietYeuCauRepository;
    private final SanPhamRepository sanPhamRepository;
    private final TaiKhoanRepository taiKhoanRepository;

    public KhoAdminService(DonHangRepository donHangRepository, YeuCauNhapHangRepository yeuCauRepository,
                           ChiTietYeuCauNhapHangRepository chiTietYeuCauRepository,
                           SanPhamRepository sanPhamRepository, TaiKhoanRepository taiKhoanRepository) {
        this.donHangRepository = donHangRepository;
        this.yeuCauRepository = yeuCauRepository;
        this.chiTietYeuCauRepository = chiTietYeuCauRepository;
        this.sanPhamRepository = sanPhamRepository;
        this.taiKhoanRepository = taiKhoanRepository;
    }

    private static final String VAI_TRO_QUAN_TRI = "QUAN_TRI";

    /** @param daDuyet true khi quản trị viên lập nên yêu cầu được duyệt luôn, chờ kho lập phiếu nhập (NVK10) */
    public record KetQuaNhap(Integer maYeuCau, boolean daDuyet) {
    }

    /** Giới hạn số lượng cho mỗi dòng yêu cầu nhập. */
    private static final int SO_LUONG_NHAP_TOI_DA = 10000;

    /**
     * Một dòng trên trang lập yêu cầu nhập nhiều sản phẩm.
     * @param canhBao true khi sản phẩm hết hoặc sắp hết hàng; false là sản phẩm còn hàng, nhập thêm để dự trữ
     */
    public record DongCanNhap(SanPham sanPham, int soLuongGoiY, boolean canhBao) {
    }

    /**
     * Lập phiếu yêu cầu nhập cho một sản phẩm đang ở mức cảnh báo (sắp hết hoặc hết hàng).
     * Nhân viên kho gửi thì phiếu chờ duyệt; quản trị viên lập thì phiếu được duyệt luôn.
     * TV4: hàng chỉ vào kho khi nhân viên kho lập phiếu nhập (PhieuNhapService), không cộng ở bước này.
     */
    @Transactional
    public KetQuaNhap lapYeuCauNhap(Integer maSanPham, Integer soLuong, String emailNguoiGui) {
        return lapYeuCau(Collections.singletonList(maSanPham), Collections.singletonList(soLuong),
                emailNguoiGui, false);
    }

    /**
     * TV4: sản phẩm đang có yêu cầu nhập chưa xong (chờ duyệt hoặc đã duyệt chờ lập phiếu nhập),
     * khóa là mã sản phẩm, giá trị là mã yêu cầu. Dùng để hiện "Đang chờ nhập #..." thay cho nút nhập.
     */
    @Transactional(readOnly = true)
    public Map<Integer, Integer> yeuCauDangChoTheoSanPham() {
        Map<Integer, Integer> ketQua = new HashMap<>();
        for (Object[] dong : chiTietYeuCauRepository.sanPhamVaYeuCauTheoTrangThai(
                List.of(TrangThaiYeuCauNhap.CHO_DUYET, TrangThaiYeuCauNhap.DA_DUYET))) {
            ketQua.putIfAbsent((Integer) dong[0], (Integer) dong[1]);
        }
        return ketQua;
    }

    /**
     * TV4 – NVK09: mọi sản phẩm đang kinh doanh chưa có yêu cầu nhập nào đang chờ.
     * Sản phẩm hết hàng, sắp hết đứng trước (gợi ý phần còn thiếu); sản phẩm còn hàng đứng sau
     * (gợi ý nhập thêm bằng mức tồn tối thiểu).
     */
    @Transactional(readOnly = true)
    public List<DongCanNhap> sanPhamCoTheYeuCauNhap() {
        Set<Integer> daCoYeuCau = new HashSet<>(
                chiTietYeuCauRepository.maSanPhamTheoTrangThai(TrangThaiYeuCauNhap.CHO_DUYET));
        daCoYeuCau.addAll(chiTietYeuCauRepository.maSanPhamTheoTrangThai(TrangThaiYeuCauNhap.DA_DUYET));
        return sanPhamRepository.danhSachDangKinhDoanh().stream()
                .filter(sp -> !daCoYeuCau.contains(sp.getMaSanPham()))
                .sorted(Comparator.comparing(SanPham::getTinhTrangTonKho))
                .map(sp -> {
                    boolean canhBao = sp.getTinhTrangTonKho() != TinhTrangTonKho.DU;
                    int goiY = canhBao ? sp.getMucTonToiThieu() - sp.getSoLuong() : sp.getMucTonToiThieu();
                    return new DongCanNhap(sp, Math.max(1, goiY), canhBao);
                })
                .toList();
    }

    /**
     * TV4 – NVK09: lập MỘT yêu cầu nhập gồm nhiều sản phẩm. Khi được duyệt, kho lập một phiếu nhập
     * chứa đủ các sản phẩm này (NVK10). Chạy trong một transaction: một dòng không hợp lệ thì không lưu gì.
     *
     * @param dsMaSanPham mã sản phẩm, cùng thứ tự với dsSoLuong
     */
    @Transactional
    public KetQuaNhap lapYeuCauNhapNhieu(List<Integer> dsMaSanPham, List<Integer> dsSoLuong, String emailNguoiGui) {
        return lapYeuCau(dsMaSanPham, dsSoLuong, emailNguoiGui, true);
    }

    /** @param choNhapThem true thì cho phép cả sản phẩm còn đủ hàng (nhập thêm để dự trữ) */
    private KetQuaNhap lapYeuCau(List<Integer> dsMaSanPham, List<Integer> dsSoLuong, String emailNguoiGui,
            boolean choNhapThem) {
        if (dsMaSanPham == null || dsMaSanPham.isEmpty()) {
            throw new NghiepVuException("Vui lòng chọn ít nhất một sản phẩm cần nhập.");
        }
        if (dsSoLuong == null || dsSoLuong.size() != dsMaSanPham.size()) {
            throw new NghiepVuException("Vui lòng nhập số lượng cho từng sản phẩm đã chọn.");
        }
        if (new HashSet<>(dsMaSanPham).size() != dsMaSanPham.size()) {
            throw new NghiepVuException("Mỗi sản phẩm chỉ được chọn một lần trong một yêu cầu.");
        }

        List<SanPham> dsSanPham = new ArrayList<>();
        for (int i = 0; i < dsMaSanPham.size(); i++) {
            Integer ma = dsMaSanPham.get(i);
            SanPham sanPham = (ma == null ? Optional.<SanPham>empty() : sanPhamRepository.findById(ma))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
            kiemTraCoTheYeuCauNhap(sanPham, dsSoLuong.get(i), choNhapThem);
            dsSanPham.add(sanPham);
        }

        TaiKhoan nguoiGui = taiKhoanRepository.findByEmailIgnoreCase(emailNguoiGui).orElseThrow();
        boolean nhapNgay = VAI_TRO_QUAN_TRI.equals(nguoiGui.getVaiTro().getTenVaiTro());
        LocalDateTime bayGio = LocalDateTime.now();

        YeuCauNhapHang yeuCau = new YeuCauNhapHang();
        yeuCau.setTaiKhoan(nguoiGui);
        yeuCau.setNgayYeuCau(bayGio);
        if (nhapNgay) {
            yeuCau.setTrangThai(TrangThaiYeuCauNhap.DA_DUYET);
            yeuCau.setNguoiDuyet(nguoiGui);
            yeuCau.setNgayDuyet(bayGio);
        } else {
            yeuCau.setTrangThai(TrangThaiYeuCauNhap.CHO_DUYET);
        }
        yeuCauRepository.save(yeuCau);

        for (int i = 0; i < dsSanPham.size(); i++) {
            ChiTietYeuCauNhapHang chiTiet = new ChiTietYeuCauNhapHang();
            chiTiet.setYeuCau(yeuCau);
            chiTiet.setSanPham(dsSanPham.get(i));
            chiTiet.setSoLuongYeuCau(dsSoLuong.get(i));
            chiTietYeuCauRepository.save(chiTiet);
        }
        return new KetQuaNhap(yeuCau.getMaYeuCauNhap(), nhapNgay);
    }

    private void kiemTraCoTheYeuCauNhap(SanPham sanPham, Integer soLuong, boolean choNhapThem) {
        Integer maSanPham = sanPham.getMaSanPham();
        if (choNhapThem && !sanPham.isDangKinhDoanh()) {
            throw new NghiepVuException("Sản phẩm " + sanPham.getTenSanPham() + " đã ngừng kinh doanh.");
        }
        if (!choNhapThem && sanPham.getTinhTrangTonKho() == TinhTrangTonKho.DU) {
            throw new NghiepVuException("Sản phẩm " + sanPham.getTenSanPham() + " vẫn đủ hàng, chưa cần yêu cầu nhập.");
        }
        if (soLuong == null || soLuong <= 0) {
            throw new NghiepVuException("Số lượng yêu cầu nhập của " + sanPham.getTenSanPham() + " phải lớn hơn 0.");
        }
        if (soLuong > SO_LUONG_NHAP_TOI_DA) {
            throw new NghiepVuException("Số lượng yêu cầu nhập của " + sanPham.getTenSanPham()
                    + " không được vượt quá " + SO_LUONG_NHAP_TOI_DA + ".");
        }
        if (chiTietYeuCauRepository.existsBySanPhamMaSanPhamAndYeuCauTrangThai(maSanPham, TrangThaiYeuCauNhap.CHO_DUYET)) {
            throw new NghiepVuException("Sản phẩm " + sanPham.getTenSanPham() + " đã có yêu cầu nhập đang chờ duyệt.");
        }
        if (chiTietYeuCauRepository.existsBySanPhamMaSanPhamAndYeuCauTrangThai(maSanPham, TrangThaiYeuCauNhap.DA_DUYET)) {
            throw new NghiepVuException("Sản phẩm " + sanPham.getTenSanPham()
                    + " đã có yêu cầu được duyệt, đang chờ kho lập phiếu nhập.");
        }
    }

    @Transactional(readOnly = true)
    public Page<DonHang> timDonHang(String tuKhoa, TrangThaiDonHang trangThai, LocalDate tuNgay, LocalDate denNgay,
                                    Pageable pageable) {
        Integer maDon = tuKhoa != null && tuKhoa.matches("\\d{1,9}") ? Integer.valueOf(tuKhoa) : null;
        LocalDateTime tu = tuNgay == null ? null : tuNgay.atStartOfDay();
        LocalDateTime den = denNgay == null ? null : denNgay.plusDays(1).atStartOfDay();
        return donHangRepository.timKiem(tuKhoa, maDon, trangThai, tu, den, pageable);
    }

    /**
     * Duyệt yêu cầu. TV4: chỉ đổi trạng thái sang DA_DUYET, chưa cộng kho;
     * hàng vào kho khi nhân viên kho lập phiếu nhập (NVK10), tránh cộng hai lần.
     */
    @Transactional
    public void duyet(Integer ma, String emailNguoiDuyet) {
        xuLy(ma, TrangThaiYeuCauNhap.DA_DUYET, emailNguoiDuyet, null);
    }

    @Transactional
    public void tuChoi(Integer ma, String emailNguoiDuyet, String lyDo) {
        if (lyDo == null) {
            throw new NghiepVuException("Vui lòng nhập lý do từ chối.");
        }
        if (lyDo.length() > 500) {
            throw new NghiepVuException("Lý do từ chối không được vượt quá 500 ký tự.");
        }
        xuLy(ma, TrangThaiYeuCauNhap.TU_CHOI, emailNguoiDuyet, lyDo);
    }

    private void xuLy(Integer ma, TrangThaiYeuCauNhap trangThai, String emailNguoiDuyet, String lyDo) {
        YeuCauNhapHang yeuCau = yeuCauRepository.findById(ma)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        TaiKhoan nguoiDuyet = taiKhoanRepository.findByEmailIgnoreCase(emailNguoiDuyet).orElseThrow();
        int soDong = yeuCauRepository.xuLy(yeuCau.getMaYeuCauNhap(), trangThai, nguoiDuyet, LocalDateTime.now(), lyDo);
        if (soDong == 0) {
            throw new NghiepVuException("Yêu cầu #" + ma + " đã được xử lý trước đó.");
        }
    }
}
