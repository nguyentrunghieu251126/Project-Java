package project.dien_may.project_java.service.muahang;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.dien_may.project_java.dto.muahang.DongGioHang;
import project.dien_may.project_java.entity.ChiTietGioHang;
import project.dien_may.project_java.entity.GioHang;
import project.dien_may.project_java.entity.SanPham;
import project.dien_may.project_java.entity.TaiKhoan;
import project.dien_may.project_java.entity.ChiTietDonHang;
import project.dien_may.project_java.entity.DonHang;
import project.dien_may.project_java.entity.TrangThaiDonHang;
import project.dien_may.project_java.repository.ChiTietGioHangRepository;
import project.dien_may.project_java.repository.DonHangKhachHangRepository;
import project.dien_may.project_java.repository.GioHangRepository;
import project.dien_may.project_java.repository.SanPhamRepository;
import project.dien_may.project_java.repository.TaiKhoanRepository;
import project.dien_may.project_java.service.ChiTietSanPhamService;
import project.dien_may.project_java.service.admin.NghiepVuException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Service
public class GioHangService {

    private final GioHangRepository gioHangRepository;
    private final ChiTietGioHangRepository chiTietGioHangRepository;
    private final TaiKhoanRepository taiKhoanRepository;
    private final SanPhamRepository sanPhamRepository;
    private final ChiTietSanPhamService chiTietSanPhamService;
    private final DonHangKhachHangRepository donHangRepository;

    public record KetQuaMuaLai(List<Integer> daThem, List<String> hetHang) {
    }

    public GioHangService(GioHangRepository gioHangRepository, ChiTietGioHangRepository chiTietGioHangRepository,
            TaiKhoanRepository taiKhoanRepository, SanPhamRepository sanPhamRepository,
            ChiTietSanPhamService chiTietSanPhamService,
            DonHangKhachHangRepository donHangRepository) {
        this.gioHangRepository = gioHangRepository;
        this.chiTietGioHangRepository = chiTietGioHangRepository;
        this.taiKhoanRepository = taiKhoanRepository;
        this.sanPhamRepository = sanPhamRepository;
        this.chiTietSanPhamService = chiTietSanPhamService;
        this.donHangRepository = donHangRepository;
    }

    @Transactional(readOnly = true)
    public List<DongGioHang> layGioHang(String email) {
        return chiTietGioHangRepository.timTheoEmail(email).stream().map(this::chuyenDong).toList();
    }

    @Transactional(readOnly = true)
    public long demSanPham(String email) {
        return chiTietGioHangRepository.tongSoLuong(email);
    }

    @Transactional
    public String them(String email, Integer maSanPham, int soLuong) {
        SanPham sanPham = sanPhamRepository.findById(maSanPham)
                .orElseThrow(() -> new NghiepVuException("Sản phẩm không tồn tại."));
        if (!sanPham.isDangKinhDoanh() || sanPham.getSoLuong() <= 0) {
            throw new NghiepVuException(sanPham.getTenSanPham() + " hiện đã hết hàng.");
        }
        int them = Math.max(soLuong, 1);
        ChiTietGioHang dong = chiTietGioHangRepository.timDong(email, maSanPham).orElse(null);
        if (dong == null) {
            dong = new ChiTietGioHang();
            dong.setGioHang(layHoacTaoGio(email));
            dong.setSanPham(sanPham);
            dong.setSoLuong(Math.min(them, sanPham.getSoLuong()));
            chiTietGioHangRepository.save(dong);
            return sanPham.getTenSanPham();
        }
        if (dong.getSoLuong() >= sanPham.getSoLuong()) {
            throw new NghiepVuException("Giỏ hàng đã có " + dong.getSoLuong() + " " + sanPham.getTenSanPham()
                    + ", bằng số lượng còn trong kho.");
        }
        dong.setSoLuong(Math.min(dong.getSoLuong() + them, sanPham.getSoLuong()));
        return sanPham.getTenSanPham();
    }

    @Transactional
    public int capNhatSoLuong(String email, Integer maSanPham, int soLuong) {
        ChiTietGioHang dong = chiTietGioHangRepository.timDong(email, maSanPham)
                .orElseThrow(() -> new NghiepVuException("Sản phẩm không còn trong giỏ hàng."));
        int tonKho = dong.getSanPham().getSoLuong();
        int moi = Math.max(1, Math.min(soLuong, Math.max(tonKho, 1)));
        dong.setSoLuong(moi);
        return moi;
    }

    @Transactional
    public int xoa(String email, Collection<Integer> dsMaSanPham) {
        if (dsMaSanPham == null || dsMaSanPham.isEmpty()) {
            return 0;
        }
        TaiKhoan taiKhoan = layTaiKhoan(email);
        return gioHangRepository.findByTaiKhoanMaTaiKhoan(taiKhoan.getMaTaiKhoan())
                .map(gio -> chiTietGioHangRepository.xoaCacDong(gio.getMaGioHang(), dsMaSanPham))
                .orElse(0);
    }

    @Transactional
    public KetQuaMuaLai muaLai(String email, Integer maDon) {
        DonHang donHang = donHangRepository.timCuaKhach(maDon, email)
                .orElseThrow(() -> new NghiepVuException("Không tìm thấy đơn hàng."));
        if (donHang.getTrangThai() != TrangThaiDonHang.DA_GIAO && donHang.getTrangThai() != TrangThaiDonHang.DA_HUY) {
            throw new NghiepVuException("Chỉ mua lại được đơn đã giao hoặc đã hủy.");
        }
        List<Integer> daThem = new ArrayList<>();
        List<String> hetHang = new ArrayList<>();
        GioHang gio = null;
        for (ChiTietDonHang ct : donHangRepository.chiTietCuaCacDon(List.of(maDon))) {
            SanPham sp = ct.getSanPham();
            if (!sp.isDangKinhDoanh() || sp.getSoLuong() <= 0) {
                hetHang.add(sp.getTenSanPham());
                continue;
            }
            int soLuong = Math.min(ct.getSoLuong(), sp.getSoLuong());
            ChiTietGioHang dong = chiTietGioHangRepository.timDong(email, sp.getMaSanPham()).orElse(null);
            if (dong == null) {
                if (gio == null)
                    gio = layHoacTaoGio(email);
                dong = new ChiTietGioHang();
                dong.setGioHang(gio);
                dong.setSanPham(sp);
                dong.setSoLuong(soLuong);
                chiTietGioHangRepository.save(dong);
            } else {
                dong.setSoLuong(soLuong);
            }
            daThem.add(sp.getMaSanPham());
        }
        if (daThem.isEmpty()) {
            throw new NghiepVuException("Các sản phẩm trong đơn " + String.format("#DH%06d", maDon) + " đã hết hàng.");
        }
        return new KetQuaMuaLai(daThem, hetHang);
    }

    DongGioHang chuyenDong(ChiTietGioHang ct) {
        SanPham sp = ct.getSanPham();
        String anh = chiTietSanPhamService.layAnhDaiDien(sp);
        return new DongGioHang(sp.getMaSanPham(), sp.getTenSanPham(), sp.getThuongHieu(), sp.getThoiGianBaoHanh(),
                anh != null ? anh : sp.getHinhAnh(), sp.getDonGia(), ct.getSoLuong(),
                sp.getSoLuong() == null ? 0 : sp.getSoLuong(), sp.isDangKinhDoanh(),
                sp.getDanhMuc() == null ? null : sp.getDanhMuc().getTenDanhMuc());
    }

    private GioHang layHoacTaoGio(String email) {
        TaiKhoan taiKhoan = layTaiKhoan(email);
        return gioHangRepository.findByTaiKhoanMaTaiKhoan(taiKhoan.getMaTaiKhoan()).orElseGet(() -> {
            GioHang gio = new GioHang();
            gio.setTaiKhoan(taiKhoan);
            gio.setNgayTao(LocalDateTime.now());
            return gioHangRepository.save(gio);
        });
    }

    private TaiKhoan layTaiKhoan(String email) {
        return taiKhoanRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new NghiepVuException("Không tìm thấy tài khoản, vui lòng đăng nhập lại."));
    }
}
