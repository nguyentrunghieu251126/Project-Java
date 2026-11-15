package project.dien_may.project_java.service.muahang;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.dien_may.project_java.dto.muahang.ThongKeDiem;
import project.dien_may.project_java.entity.DonHang;
import project.dien_may.project_java.entity.LichSuDiemThuong;
import project.dien_may.project_java.entity.LoaiGiaoDichDiem;
import project.dien_may.project_java.entity.TaiKhoan;
import project.dien_may.project_java.repository.LichSuDiemThuongRepository;

import java.time.LocalDateTime;
import java.util.List;

/** TV3 – điểm thưởng: xem số điểm, lịch sử; ghi cộng / trừ khi đặt và hủy đơn. */
@Service
public class DiemThuongService {

    private static final int SO_DONG_MOI_TRANG = 10;

    private final LichSuDiemThuongRepository lichSuRepository;

    public DiemThuongService(LichSuDiemThuongRepository lichSuRepository) {
        this.lichSuRepository = lichSuRepository;
    }

    @Transactional(readOnly = true)
    public long tongDiem(String email) {
        return lichSuRepository.tongDiem(email);
    }

    /** loai: "cong", "tru" hoặc null/khác = tất cả. */
    @Transactional(readOnly = true)
    public Page<LichSuDiemThuong> lichSu(String email, String loai, int trang) {
        return lichSuRepository.cuaKhach(email, chuanHoaLoai(loai), PageRequest.of(Math.max(trang, 1) - 1, SO_DONG_MOI_TRANG));
    }

    @Transactional(readOnly = true)
    public List<LichSuDiemThuong> ganDay(String email, int soDong) {
        return lichSuRepository.cuaKhach(email, null, PageRequest.of(0, soDong)).getContent();
    }

    @Transactional(readOnly = true)
    public ThongKeDiem thongKe(String email) {
        long tongCong = 0, soLanCong = 0, tongTru = 0, soLanTru = 0;
        for (Object[] dong : lichSuRepository.thongKe(email)) {
            long tong = ((Number) dong[1]).longValue();
            long soLan = ((Number) dong[2]).longValue();
            if (dong[0] == LoaiGiaoDichDiem.CONG) {
                tongCong = tong;
                soLanCong = soLan;
            } else {
                tongTru = tong;
                soLanTru = soLan;
            }
        }
        return new ThongKeDiem(tongCong, soLanCong, tongTru, soLanTru);
    }

    public static LoaiGiaoDichDiem chuanHoaLoai(String loai) {
        if ("cong".equalsIgnoreCase(loai)) return LoaiGiaoDichDiem.CONG;
        if ("tru".equalsIgnoreCase(loai)) return LoaiGiaoDichDiem.TRU;
        return null;
    }

    @Transactional(readOnly = true)
    public List<LichSuDiemThuong> theoDon(Integer maDon) {
        return lichSuRepository.theoDon(maDon);
    }

    /** Gọi trong giao dịch đặt hàng, trước khi đọc số điểm để kiểm tra. */
    public void khoaTaiKhoan(TaiKhoan taiKhoan) {
        lichSuRepository.khoaTaiKhoan(taiKhoan.getMaTaiKhoan());
    }

    public void ghi(TaiKhoan taiKhoan, DonHang donHang, LoaiGiaoDichDiem loai, int soDiem, String moTa) {
        if (soDiem <= 0) {
            return;
        }
        LichSuDiemThuong ls = new LichSuDiemThuong();
        ls.setTaiKhoan(taiKhoan);
        ls.setDonHang(donHang);
        ls.setLoaiGiaoDich(loai);
        ls.setSoDiem(soDiem);
        ls.setMoTa(moTa);
        ls.setNgayGiaoDich(LocalDateTime.now());
        lichSuRepository.save(ls);
    }

    /**
     * Hủy đơn: hoàn lại điểm đã dùng cho đơn và thu hồi điểm đã cộng từ đơn
     * (không thu hồi quá số điểm khách đang có để tổng điểm không âm).
     */
    public void hoanKhiHuy(DonHang donHang) {
        TaiKhoan taiKhoan = donHang.getTaiKhoan();
        khoaTaiKhoan(taiKhoan);
        String ma = String.format("#DH%06d", donHang.getMaDonHang());
        int daDung = 0;
        int daCong = 0;
        for (LichSuDiemThuong ls : lichSuRepository.theoDon(donHang.getMaDonHang())) {
            if (ls.getLoaiGiaoDich() == LoaiGiaoDichDiem.TRU) {
                daDung += ls.getSoDiem();
            } else {
                daCong += ls.getSoDiem();
            }
        }
        // Hàm chỉ chạy một lần cho mỗi đơn (hủy đơn là câu UPDATE có điều kiện)
        ghi(taiKhoan, donHang, LoaiGiaoDichDiem.CONG, daDung, "Hoàn điểm đã dùng do hủy đơn " + ma);
        if (daCong > 0) {
            long hienCo = lichSuRepository.tongDiem(taiKhoan.getEmail());
            ghi(taiKhoan, donHang, LoaiGiaoDichDiem.TRU, (int) Math.min(daCong, Math.max(0, hienCo)),
                    "Thu hồi điểm tích từ đơn " + ma + " do hủy đơn");
        }
    }
}
