package project.dien_may.project_java.service.admin;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import project.dien_may.project_java.entity.*;
import project.dien_may.project_java.repository.ChiTietYeuCauNhapHangRepository;
import project.dien_may.project_java.repository.PhieuNhapRepository;
import project.dien_may.project_java.repository.SanPhamRepository;
import project.dien_may.project_java.repository.TaiKhoanRepository;
import project.dien_may.project_java.repository.YeuCauNhapHangRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * TV4 – NVK10: Nhân viên kho lập phiếu nhập cho yêu cầu đã được duyệt.
 * Luồng: CHO_DUYET → (quản trị viên duyệt) DA_DUYET → (kho lập phiếu nhập) DA_NHAP.
 * Chỉ khi lập phiếu nhập, hàng mới thực sự được cộng vào tồn kho.
 */
@Service
public class PhieuNhapService {

    /** Giới hạn của cột DECIMAL(18, 0). */
    private static final BigDecimal GIA_TOI_DA = new BigDecimal("999999999999");
    /** Giá nhập gợi ý bằng 80% giá bán, làm tròn đến nghìn đồng; nhân viên sửa lại theo hóa đơn thực tế. */
    private static final BigDecimal TI_LE_GOI_Y = new BigDecimal("0.8");

    private final YeuCauNhapHangRepository yeuCauRepository;
    private final ChiTietYeuCauNhapHangRepository chiTietYeuCauRepository;
    private final PhieuNhapRepository phieuNhapRepository;
    private final SanPhamRepository sanPhamRepository;
    private final TaiKhoanRepository taiKhoanRepository;

    public PhieuNhapService(YeuCauNhapHangRepository yeuCauRepository,
                            ChiTietYeuCauNhapHangRepository chiTietYeuCauRepository,
                            PhieuNhapRepository phieuNhapRepository, SanPhamRepository sanPhamRepository,
                            TaiKhoanRepository taiKhoanRepository) {
        this.yeuCauRepository = yeuCauRepository;
        this.chiTietYeuCauRepository = chiTietYeuCauRepository;
        this.phieuNhapRepository = phieuNhapRepository;
        this.sanPhamRepository = sanPhamRepository;
        this.taiKhoanRepository = taiKhoanRepository;
    }

    @Transactional(readOnly = true)
    public PhieuNhap timTheoYeuCau(Integer maYeuCau) {
        return phieuNhapRepository.findFirstByYeuCauMaYeuCauNhap(maYeuCau).orElse(null);
    }

    /** Giá nhập gợi ý cho từng dòng của yêu cầu, khóa là mã chi tiết yêu cầu. */
    @Transactional(readOnly = true)
    public Map<Integer, BigDecimal> giaNhapGoiY(Integer maYeuCau) {
        Map<Integer, BigDecimal> goiY = new LinkedHashMap<>();
        for (ChiTietYeuCauNhapHang ct : chiTietYeuCauRepository.findByYeuCauMaYeuCauNhap(maYeuCau)) {
            BigDecimal gia = ct.getSanPham().getDonGia().multiply(TI_LE_GOI_Y)
                    .divide(BigDecimal.valueOf(1000), 0, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(1000));
            goiY.put(ct.getMaChiTietYeuCauNhap(), gia);
        }
        return goiY;
    }

    /**
     * Lập phiếu nhập: tạo PhieuNhap và từng ChiTietPhieuNhap, cộng tồn kho, chuyển yêu cầu sang DA_NHAP.
     * Nhiều bước nên chạy trong một transaction: lỗi ở bất kỳ bước nào thì hoàn tác toàn bộ (NFR06).
     *
     * @param dsMaChiTiet mã chi tiết yêu cầu, cùng thứ tự với dsDonGia
     * @param dsDonGia    đơn giá nhập của từng dòng
     */
    @Transactional
    public PhieuNhap lapPhieuNhap(Integer maYeuCau, String emailNhanVien, List<Integer> dsMaChiTiet,
                                  List<BigDecimal> dsDonGia, String ghiChu) {
        YeuCauNhapHang yeuCau = yeuCauRepository.findById(maYeuCau)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (yeuCau.getTrangThai() != TrangThaiYeuCauNhap.DA_DUYET) {
            throw new NghiepVuException("Chỉ lập phiếu nhập được cho yêu cầu ở trạng thái \"Đã duyệt\".");
        }
        if (phieuNhapRepository.existsByYeuCauMaYeuCauNhap(maYeuCau)) {
            throw new NghiepVuException("Yêu cầu #" + maYeuCau + " đã có phiếu nhập.");
        }
        if (ghiChu != null && ghiChu.length() > 500) {
            throw new NghiepVuException("Ghi chú không được vượt quá 500 ký tự.");
        }

        Map<Integer, BigDecimal> giaTheoDong = ghepGia(dsMaChiTiet, dsDonGia);
        List<ChiTietYeuCauNhapHang> dsChiTiet = chiTietYeuCauRepository.findByYeuCauMaYeuCauNhap(maYeuCau);
        if (dsChiTiet.isEmpty() || giaTheoDong.size() != dsChiTiet.size()) {
            throw new NghiepVuException("Vui lòng nhập đơn giá cho tất cả sản phẩm trong yêu cầu.");
        }

        TaiKhoan nhanVien = taiKhoanRepository.findByEmailIgnoreCase(emailNhanVien).orElseThrow();
        PhieuNhap phieu = new PhieuNhap();
        phieu.setYeuCau(yeuCau);
        phieu.setTaiKhoan(nhanVien);
        phieu.setNgayNhap(LocalDateTime.now());
        phieu.setGhiChu(ghiChu);

        BigDecimal tongTien = BigDecimal.ZERO;
        Map<Integer, Integer> soLuongCong = new HashMap<>(); // mã sản phẩm → số lượng cộng vào kho
        for (ChiTietYeuCauNhapHang ct : dsChiTiet) {
            BigDecimal donGia = giaTheoDong.get(ct.getMaChiTietYeuCauNhap());
            if (donGia == null) {
                throw new NghiepVuException("Thiếu đơn giá nhập của " + ct.getSanPham().getTenSanPham() + ".");
            }
            ChiTietPhieuNhap dong = new ChiTietPhieuNhap();
            dong.setPhieuNhap(phieu);
            dong.setSanPham(ct.getSanPham());
            dong.setSoLuongNhap(ct.getSoLuongYeuCau());
            dong.setDonGiaNhap(donGia);
            dong.setThanhTien(donGia.multiply(BigDecimal.valueOf(ct.getSoLuongYeuCau())));
            phieu.getChiTiet().add(dong);
            tongTien = tongTien.add(dong.getThanhTien());
            soLuongCong.merge(ct.getSanPham().getMaSanPham(), ct.getSoLuongYeuCau(), Integer::sum);
        }
        phieu.setTongTienNhap(tongTien);
        phieuNhapRepository.save(phieu); // cascade lưu luôn các dòng chi tiết

        // Khóa trạng thái trước khi cộng kho: nếu người khác đã lập phiếu thì dừng và hoàn tác phiếu vừa lưu
        if (yeuCauRepository.chuyenTrangThai(maYeuCau, TrangThaiYeuCauNhap.DA_DUYET, TrangThaiYeuCauNhap.DA_NHAP) == 0) {
            throw new NghiepVuException("Yêu cầu #" + maYeuCau + " vừa được lập phiếu nhập bởi người khác.");
        }
        // Cộng thẳng trên CSDL (UPDATE ... SoLuong = SoLuong + ?) để không ghi đè số lượng khi có đơn đặt cùng lúc
        soLuongCong.forEach(sanPhamRepository::congTonKho);
        return phieu;
    }

    /** Ghép danh sách mã dòng với danh sách đơn giá gửi từ form, kiểm tra từng giá trị. */
    private Map<Integer, BigDecimal> ghepGia(List<Integer> dsMaChiTiet, List<BigDecimal> dsDonGia) {
        if (dsMaChiTiet == null || dsDonGia == null || dsMaChiTiet.size() != dsDonGia.size()) {
            throw new NghiepVuException("Dữ liệu đơn giá nhập không hợp lệ.");
        }
        Map<Integer, BigDecimal> ketQua = new HashMap<>();
        for (int i = 0; i < dsMaChiTiet.size(); i++) {
            BigDecimal gia = dsDonGia.get(i);
            if (gia == null) {
                throw new NghiepVuException("Vui lòng nhập đơn giá cho tất cả sản phẩm.");
            }
            if (gia.signum() < 0 || gia.compareTo(GIA_TOI_DA) > 0) {
                throw new NghiepVuException("Đơn giá nhập phải từ 0 đến " + GIA_TOI_DA.toPlainString() + " ₫.");
            }
            if (gia.stripTrailingZeros().scale() > 0) {
                throw new NghiepVuException("Đơn giá nhập phải là số nguyên (đồng).");
            }
            ketQua.put(dsMaChiTiet.get(i), gia.setScale(0, RoundingMode.UNNECESSARY));
        }
        return ketQua;
    }
}
