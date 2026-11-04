package project.dien_may.project_java.service.admin;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Errors;
import org.springframework.web.server.ResponseStatusException;
import project.dien_may.project_java.dto.KhuyenMaiForm;
import project.dien_may.project_java.entity.KhuyenMai;
import project.dien_may.project_java.entity.LoaiGiamGia;
import project.dien_may.project_java.repository.DonHangRepository;
import project.dien_may.project_java.repository.KhuyenMaiRepository;

import java.math.BigDecimal;
import java.util.Locale;

@Service
public class KhuyenMaiAdminService {

    private final KhuyenMaiRepository khuyenMaiRepository;
    private final DonHangRepository donHangRepository;

    public KhuyenMaiAdminService(KhuyenMaiRepository khuyenMaiRepository, DonHangRepository donHangRepository) {
        this.khuyenMaiRepository = khuyenMaiRepository;
        this.donHangRepository = donHangRepository;
    }

    @Transactional(readOnly = true)
    public KhuyenMaiForm layForm(Integer ma) {
        return KhuyenMaiForm.tu(timTheoMa(ma));
    }

    /** Các ràng buộc liên trường của CSDL, kiểm tra trước để báo lỗi ngay tại ô nhập. */
    @Transactional(readOnly = true)
    public void kiemTra(KhuyenMaiForm form, Errors errors) {
        Integer ma = form.getMaKhuyenMai();
        if (form.getMaGiamGia() != null) {
            boolean trung = ma == null
                    ? khuyenMaiRepository.existsByMaGiamGiaIgnoreCase(form.getMaGiamGia())
                    : khuyenMaiRepository.existsByMaGiamGiaIgnoreCaseAndMaKhuyenMaiNot(form.getMaGiamGia(), ma);
            if (trung) {
                errors.rejectValue("maGiamGia", "maGiamGia.trung", "Mã giảm giá này đã tồn tại.");
            }
        }
        if (form.getLoaiGiamGia() == LoaiGiamGia.PHAN_TRAM && form.getGiaTriGiam() != null
                && form.getGiaTriGiam().compareTo(BigDecimal.valueOf(100)) > 0) {
            errors.rejectValue("giaTriGiam", "giaTriGiam.qua100", "Giảm theo phần trăm không được vượt quá 100.");
        }
        if (form.getNgayBatDau() != null && form.getNgayKetThuc() != null
                && form.getNgayKetThuc().isBefore(form.getNgayBatDau())) {
            errors.rejectValue("ngayKetThuc", "ngayKetThuc.truocBatDau", "Ngày kết thúc phải từ ngày bắt đầu trở đi.");
        }
        if (ma != null && form.getSoLuotToiDa() != null) {
            int daDung = timTheoMa(ma).getSoLuotDaDung();
            if (form.getSoLuotToiDa() < daDung) {
                errors.rejectValue("soLuotToiDa", "soLuotToiDa.nhoHonDaDung",
                        "Mã này đã dùng " + daDung + " lượt, không thể đặt thấp hơn.");
            }
        }
    }

    @Transactional
    public void them(KhuyenMaiForm form) {
        KhuyenMai km = new KhuyenMai();
        ganThongTin(km, form);
        khuyenMaiRepository.save(km);
    }

    @Transactional
    public void capNhat(Integer ma, KhuyenMaiForm form) {
        ganThongTin(timTheoMa(ma), form);
    }

    /** @return trạng thái mới: true là đang bật */
    @Transactional
    public boolean batTat(Integer ma) {
        KhuyenMai km = timTheoMa(ma);
        km.setTrangThai(!km.isTrangThai());
        return km.isTrangThai();
    }

    @Transactional
    public void xoa(Integer ma) {
        KhuyenMai km = timTheoMa(ma);
        if (donHangRepository.existsByKhuyenMaiMaKhuyenMai(ma)) {
            throw new NghiepVuException("Mã " + km.getMaGiamGia()
                    + " đã được dùng trong đơn hàng nên không thể xóa. Hãy tạm dừng mã này.");
        }
        khuyenMaiRepository.delete(km);
    }

    private void ganThongTin(KhuyenMai km, KhuyenMaiForm form) {
        km.setTenKhuyenMai(form.getTenKhuyenMai());
        km.setMaGiamGia(form.getMaGiamGia().toUpperCase(Locale.ROOT));
        km.setLoaiGiamGia(form.getLoaiGiamGia());
        km.setGiaTriGiam(form.getGiaTriGiam());
        km.setNgayBatDau(form.getNgayBatDau());
        km.setNgayKetThuc(form.getNgayKetThuc());
        km.setSoLuotToiDa(form.getSoLuotToiDa());
    }

    private KhuyenMai timTheoMa(Integer ma) {
        return khuyenMaiRepository.findById(ma)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
}
