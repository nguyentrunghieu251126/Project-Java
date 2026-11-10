package project.dien_may.project_java.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import project.dien_may.project_java.dto.DongTonKho;
import project.dien_may.project_java.entity.SanPham;
import project.dien_may.project_java.repository.SanPhamRepository;
import project.dien_may.project_java.service.admin.NghiepVuException;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * TV5 - Tiêu chí 5: xử lý đồng thời.
 * <ul>
 *   <li>NFR02: trừ tồn kho an toàn khi nhiều khách cùng đặt một sản phẩm (khóa bi quan PESSIMISTIC_WRITE).</li>
 *   <li>NFR03: các hàm tra cứu chỉ đọc cho socket server "Tra cứu kho".</li>
 * </ul>
 */
@Service
public class TonKhoService {

    private static final Logger log = LoggerFactory.getLogger(TonKhoService.class);

    private final SanPhamRepository sanPhamRepository;

    public TonKhoService(SanPhamRepository sanPhamRepository) {
        this.sanPhamRepository = sanPhamRepository;
    }

    /**
     * Trừ tồn kho của một sản phẩm. Hàm khóa dòng sản phẩm rồi mới kiểm tra, nên giữa bước "đọc tồn" và bước
     * "ghi tồn mới" không luồng nào khác chen vào được: tồn không bao giờ âm và không bán vượt số hàng còn lại.
     * <p>
     * Hàm dùng {@code @Transactional} mặc định (REQUIRED): khi được gọi từ nghiệp vụ đặt hàng (@Transactional của TV3)
     * thì tham gia chung transaction đó, nên khóa được giữ đến khi cả đơn hàng commit hoặc rollback (NFR06).
     * Gọi riêng lẻ thì tự mở một transaction ngắn.
     *
     * @throws TonKhoKhongDuException khi tồn hiện có nhỏ hơn số lượng cần trừ
     */
    @Transactional
    public void truTonKho(Integer maSanPham, int soLuong) {
        if (soLuong <= 0) {
            throw new NghiepVuException("Số lượng trừ kho phải lớn hơn 0.");
        }
        String luong = Thread.currentThread().getName();
        SanPham sanPham = sanPhamRepository.khoaTheoMa(maSanPham)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        int ton = sanPham.getSoLuong();
        if (ton < soLuong) {
            log.info("[{}] Từ chối: sản phẩm #{} còn {}, cần {}", luong, maSanPham, ton, soLuong);
            throw new TonKhoKhongDuException(maSanPham, sanPham.getTenSanPham(), ton, soLuong);
        }
        sanPham.setSoLuong(ton - soLuong);
        // Ghi ngay để lỗi (nếu có, ví dụ vi phạm CK_SanPham_SoLuong) nổ tại đây chứ không lúc commit.
        sanPhamRepository.saveAndFlush(sanPham);
        log.info("[{}] Đã trừ kho sản phẩm #{}: {} -> {}", luong, maSanPham, ton, ton - soLuong);
    }

    /**
     * Trừ tồn nhiều sản phẩm của cùng một đơn hàng. Luôn khóa theo thứ tự mã sản phẩm tăng dần để hai đơn có chung
     * nhiều sản phẩm không khóa chéo nhau (deadlock). Một sản phẩm thiếu hàng thì ném lỗi và transaction rollback
     * toàn bộ các sản phẩm đã trừ trước đó.
     *
     * @param soLuongTheoSanPham khóa là mã sản phẩm, giá trị là số lượng cần trừ
     */
    @Transactional
    public void truTonKho(Map<Integer, Integer> soLuongTheoSanPham) {
        new TreeMap<>(soLuongTheoSanPham).forEach(this::truTonKho);
    }

    // ===== Tra cứu chỉ đọc cho socket server (NFR03) =====

    /** Tìm theo mã sản phẩm (chuỗi số) hoặc mã model. */
    @Transactional(readOnly = true)
    public Optional<DongTonKho> traCuu(String ma) {
        Optional<SanPham> ketQua = Optional.empty();
        if (ma.matches("\\d{1,9}")) {
            ketQua = sanPhamRepository.findById(Integer.valueOf(ma));
        }
        if (ketQua.isEmpty()) {
            ketQua = sanPhamRepository.findByMaModelIgnoreCase(ma);
        }
        return ketQua.map(DongTonKho::tu);
    }

    /** Toàn bộ sản phẩm đang kinh doanh, xếp theo mã sản phẩm. */
    @Transactional(readOnly = true)
    public List<DongTonKho> tatCa() {
        return sanPhamRepository.findByDangKinhDoanhTrue().stream()
                .sorted(Comparator.comparing(SanPham::getMaSanPham))
                .map(DongTonKho::tu)
                .toList();
    }

    /** Sản phẩm sắp hết hoặc đã hết hàng (tồn dưới mức tối thiểu), ít hàng nhất đứng trước. */
    @Transactional(readOnly = true)
    public List<DongTonKho> sapHet() {
        return sanPhamRepository.danhSachCanNhap().stream().map(DongTonKho::tu).toList();
    }
}
