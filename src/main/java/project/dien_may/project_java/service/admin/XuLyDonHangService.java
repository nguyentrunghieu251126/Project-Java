package project.dien_may.project_java.service.admin;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import project.dien_may.project_java.entity.ChiTietDonHang;
import project.dien_may.project_java.entity.ChiTietPhieuXuat;
import project.dien_may.project_java.entity.DonHang;
import project.dien_may.project_java.entity.PhieuXuat;
import project.dien_may.project_java.entity.TaiKhoan;
import project.dien_may.project_java.entity.TrangThaiDonHang;
import project.dien_may.project_java.repository.ChiTietDonHangRepository;
import project.dien_may.project_java.repository.DonHangRepository;
import project.dien_may.project_java.repository.PhieuXuatRepository;
import project.dien_may.project_java.repository.TaiKhoanRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * TV4 – Nhân viên kho xử lý đơn hàng (NVK03 – NVK06).
 * Luồng trạng thái: CHO_XAC_NHAN → DA_XAC_NHAN → DA_XUAT_KHO (kho), rồi DANG_GIAO → DA_GIAO do nhân viên giao hàng.
 * Tồn kho đã bị trừ lúc khách đặt hàng (KH12), nên khi xuất kho chỉ lập phiếu xuất, không trừ kho lần nữa.
 */
@Service
public class XuLyDonHangService {

    private final DonHangRepository donHangRepository;
    private final ChiTietDonHangRepository chiTietDonHangRepository;
    private final PhieuXuatRepository phieuXuatRepository;
    private final TaiKhoanRepository taiKhoanRepository;

    public XuLyDonHangService(DonHangRepository donHangRepository, ChiTietDonHangRepository chiTietDonHangRepository,
                              PhieuXuatRepository phieuXuatRepository, TaiKhoanRepository taiKhoanRepository) {
        this.donHangRepository = donHangRepository;
        this.chiTietDonHangRepository = chiTietDonHangRepository;
        this.phieuXuatRepository = phieuXuatRepository;
        this.taiKhoanRepository = taiKhoanRepository;
    }

    /** NVK03: Xác nhận đơn mới. */
    @Transactional
    public void xacNhan(Integer maDonHang) {
        doiTrangThai(maDonHang, TrangThaiDonHang.CHO_XAC_NHAN, TrangThaiDonHang.DA_XAC_NHAN);
    }

    /**
     * NVK05: Lập phiếu xuất kho cho đơn đã xác nhận.
     * Nhiều bước (tạo phiếu, tạo từng dòng chi tiết, đổi trạng thái đơn) nên chạy trong một transaction:
     * lỗi ở bất kỳ bước nào thì hoàn tác toàn bộ (NFR06).
     */
    @Transactional
    public PhieuXuat xuatKho(Integer maDonHang, String emailNhanVien, String ghiChu) {
        DonHang donHang = timDonHang(maDonHang);
        if (donHang.getTrangThai() != TrangThaiDonHang.DA_XAC_NHAN) {
            throw new NghiepVuException("Chỉ xuất kho được đơn ở trạng thái \"Đã xác nhận\".");
        }
        if (phieuXuatRepository.existsByDonHangMaDonHang(maDonHang)) {
            throw new NghiepVuException("Đơn hàng #" + maDonHang + " đã có phiếu xuất.");
        }
        if (ghiChu != null && ghiChu.length() > 500) {
            throw new NghiepVuException("Ghi chú không được vượt quá 500 ký tự.");
        }
        List<ChiTietDonHang> dsChiTiet = chiTietDonHangRepository.findByDonHangMaDonHang(maDonHang);
        if (dsChiTiet.isEmpty()) {
            throw new NghiepVuException("Đơn hàng #" + maDonHang + " không có sản phẩm để xuất.");
        }

        TaiKhoan nhanVien = taiKhoanRepository.findByEmailIgnoreCase(emailNhanVien).orElseThrow();
        PhieuXuat phieu = new PhieuXuat();
        phieu.setDonHang(donHang);
        phieu.setTaiKhoan(nhanVien);
        phieu.setNgayXuat(LocalDateTime.now());
        phieu.setGhiChu(ghiChu);
        for (ChiTietDonHang ct : dsChiTiet) {
            ChiTietPhieuXuat dong = new ChiTietPhieuXuat();
            dong.setPhieuXuat(phieu);
            dong.setSanPham(ct.getSanPham());
            dong.setSoLuongXuat(ct.getSoLuong());
            phieu.getChiTiet().add(dong);
        }
        phieuXuatRepository.save(phieu); // cascade lưu luôn các dòng chi tiết

        doiTrangThai(maDonHang, TrangThaiDonHang.DA_XAC_NHAN, TrangThaiDonHang.DA_XUAT_KHO);
        return phieu;
    }

    // TV4: các bước "Đang giao" và "Đã giao" chuyển sang nhân viên giao hàng (GiaoHangService).

    @Transactional(readOnly = true)
    public PhieuXuat timPhieuXuat(Integer maDonHang) {
        return phieuXuatRepository.findByDonHangMaDonHang(maDonHang).orElse(null);
    }

    private void doiTrangThai(Integer maDonHang, TrangThaiDonHang cu, TrangThaiDonHang moi) {
        timDonHang(maDonHang);
        if (donHangRepository.doiTrangThai(maDonHang, cu, moi) == 0) {
            throw new NghiepVuException("Đơn hàng #" + maDonHang + " không còn ở trạng thái \""
                    + cu.getNhan() + "\", có thể đã được người khác xử lý.");
        }
    }

    private DonHang timDonHang(Integer maDonHang) {
        return donHangRepository.findById(maDonHang)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
}
