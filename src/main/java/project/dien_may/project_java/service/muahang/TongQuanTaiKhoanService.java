package project.dien_may.project_java.service.muahang;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.dien_may.project_java.dto.muahang.BaoHanhSanPham;
import project.dien_may.project_java.dto.muahang.DonHangTomTat;
import project.dien_may.project_java.dto.muahang.MaUuDai;
import project.dien_may.project_java.entity.ChiTietDonHang;
import project.dien_may.project_java.entity.KhuyenMai;
import project.dien_may.project_java.entity.TrangThaiDonHang;
import project.dien_may.project_java.repository.DonHangKhachHangRepository;
import project.dien_may.project_java.repository.KhuyenMaiKhachHangRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** TV3 – dữ liệu trang Tổng quan tài khoản: đơn hàng gần đây, ưu đãi, thông tin bảo hành. */
@Service
public class TongQuanTaiKhoanService {

    public static final int SO_DON_GAN_DAY = 3;
    public static final int SO_UU_DAI = 4;
    public static final int SO_BAO_HANH = 3;

    private final DonHangKhachHangService donHangService;
    private final DonHangKhachHangRepository donHangRepository;
    private final KhuyenMaiKhachHangRepository khuyenMaiRepository;

    public TongQuanTaiKhoanService(DonHangKhachHangService donHangService, DonHangKhachHangRepository donHangRepository,
                                   KhuyenMaiKhachHangRepository khuyenMaiRepository) {
        this.donHangService = donHangService;
        this.donHangRepository = donHangRepository;
        this.khuyenMaiRepository = khuyenMaiRepository;
    }

    @Transactional(readOnly = true)
    public List<DonHangTomTat> donGanDay(String email) {
        return donHangService.layLichSu(email, null, null, 1).getContent().stream().limit(SO_DON_GAN_DAY).toList();
    }

    /** Mã giảm giá đang dùng được, mã gần hết hạn trước. */
    @Transactional(readOnly = true)
    public List<MaUuDai> uuDai(int soLuong) {
        return khuyenMaiRepository.dangChay(LocalDate.now()).stream().limit(soLuong).map(this::chuyenMa).toList();
    }

    @Transactional(readOnly = true)
    public List<BaoHanhSanPham> baoHanh(String email) {
        return donHangRepository.daGiaoCuaKhach(email, TrangThaiDonHang.DA_GIAO, PageRequest.of(0, SO_BAO_HANH))
                .stream().map(this::chuyenBaoHanh).toList();
    }

    private BaoHanhSanPham chuyenBaoHanh(ChiTietDonHang ct) {
        var d = ct.getDonHang();
        LocalDate ngayNhan = (d.getNgayGiao() != null ? d.getNgayGiao() : d.getNgayDat()).toLocalDate();
        int soThang = ct.getSanPham().getThoiGianBaoHanh() == null ? 0 : ct.getSanPham().getThoiGianBaoHanh();
        return new BaoHanhSanPham(d.getMaDonHang(), ct.getSanPham().getTenSanPham(), ct.getSanPham().getHinhAnh(),
                ngayNhan, soThang, ngayNhan.plusMonths(soThang));
    }

    private MaUuDai chuyenMa(KhuyenMai km) {
        Integer conLai = km.getSoLuotToiDa() == null ? null : km.getSoLuotToiDa() - km.getSoLuotDaDung();
        return new MaUuDai(km.getMaGiamGia(), km.getTenKhuyenMai(), km.getLoaiGiamGia(), km.getGiaTriGiam(),
                km.getNgayBatDau(), km.getNgayKetThuc(), conLai, true, BigDecimal.ZERO, false);
    }
}
