package project.dien_may.project_java.service.muahang;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.dien_may.project_java.dto.muahang.DiaChiForm;
import project.dien_may.project_java.dto.muahang.TinhThanh;
import project.dien_may.project_java.entity.SoDiaChi;
import project.dien_may.project_java.entity.TaiKhoan;
import project.dien_may.project_java.repository.SoDiaChiRepository;
import project.dien_may.project_java.repository.TaiKhoanRepository;
import project.dien_may.project_java.service.admin.NghiepVuException;

import java.time.LocalDateTime;
import java.util.List;

/**
 * TV3 – sổ địa chỉ giao hàng (thêm, sửa, xóa, đặt mặc định), theo ý tưởng Project-Website.
 * Địa chỉ mặc định được chép sang TaiKhoan.DiaChi để trang hồ sơ (TV2) luôn hiển thị đúng.
 */
@Service
public class SoDiaChiService {

    /** DonHang.DiaChiGiaoHang chỉ chứa 255 ký tự. */
    private static final int DO_DAI_TOI_DA = 255;
    public static final int SO_DIA_CHI_TOI_DA = 10;

    private final SoDiaChiRepository soDiaChiRepository;
    private final TaiKhoanRepository taiKhoanRepository;

    public SoDiaChiService(SoDiaChiRepository soDiaChiRepository, TaiKhoanRepository taiKhoanRepository) {
        this.soDiaChiRepository = soDiaChiRepository;
        this.taiKhoanRepository = taiKhoanRepository;
    }

    /** Sổ còn trống mà hồ sơ đã có đủ SĐT + địa chỉ thì tự chép thành địa chỉ mặc định đầu tiên. */
    @Transactional
    public List<SoDiaChi> danhSach(String email) {
        if (soDiaChiRepository.demCuaKhach(email) == 0) {
            TaiKhoan tk = layTaiKhoan(email);
            String diaChi = tk.getDiaChi() == null ? "" : tk.getDiaChi().trim();
            int phay = diaChi.lastIndexOf(',');
            if (tk.getSoDienThoai() != null && tk.getSoDienThoai().matches("0\\d{9}") && phay > 0
                    && phay < diaChi.length() - 1) {
                SoDiaChi dc = new SoDiaChi();
                dc.setTaiKhoan(tk);
                dc.setTenNguoiNhan(tk.getHoTen());
                dc.setSoDienThoai(tk.getSoDienThoai());
                dc.setDiaChiChiTiet(catDoDai(diaChi.substring(0, phay).trim(), 200));
                dc.setKhuVuc(catDoDai(diaChi.substring(phay + 1).trim(), 50));
                dc.setLoaiDiaChi(SoDiaChi.NHA_RIENG);
                dc.setLaMacDinh(true);
                dc.setNgayTao(LocalDateTime.now());
                soDiaChiRepository.save(dc);
            }
        }
        return soDiaChiRepository.cuaKhach(email);
    }

    @Transactional(readOnly = true)
    public SoDiaChi timCuaKhach(String email, Integer maDiaChi) {
        return soDiaChiRepository.timCuaKhach(maDiaChi, email)
                .orElseThrow(() -> new NghiepVuException("Địa chỉ không tồn tại hoặc đã bị xóa."));
    }

    /** Form trống cho địa chỉ mới: điền sẵn họ tên, SĐT trong hồ sơ; sổ trống thì đặt mặc định luôn. */
    @Transactional(readOnly = true)
    public DiaChiForm formMoi(String email) {
        TaiKhoan tk = layTaiKhoan(email);
        DiaChiForm form = new DiaChiForm();
        form.setTenNguoiNhan(tk.getHoTen());
        form.setSoDienThoai(tk.getSoDienThoai());
        form.setLaMacDinh(soDiaChiRepository.demCuaKhach(email) == 0);
        return form;
    }

    /** Thêm (maDiaChi null) hoặc sửa địa chỉ. @return mã địa chỉ vừa lưu */
    @Transactional
    public Integer luu(String email, DiaChiForm form) {
        String khuVuc = form.getKhuVuc() == null ? "" : form.getKhuVuc().trim();
        SoDiaChi cu = form.getMaDiaChi() == null ? null : timCuaKhach(email, form.getMaDiaChi());
        if (!TinhThanh.coTrongDanhSach(khuVuc) && !(cu != null && khuVuc.equals(cu.getKhuVuc()))) {
            throw new NghiepVuException("Vui lòng chọn tỉnh / thành phố trong danh sách.");
        }
        String chiTiet = form.getDiaChiChiTiet().trim();
        if ((chiTiet + ", " + khuVuc).length() > DO_DAI_TOI_DA) {
            throw new NghiepVuException("Địa chỉ quá dài, vui lòng rút gọn phần địa chỉ chi tiết.");
        }
        long soLuong = soDiaChiRepository.demCuaKhach(email);
        if (cu == null && soLuong >= SO_DIA_CHI_TOI_DA) {
            throw new NghiepVuException("Sổ địa chỉ chỉ lưu tối đa " + SO_DIA_CHI_TOI_DA + " địa chỉ.");
        }
        TaiKhoan tk = layTaiKhoan(email);
        // Địa chỉ đầu tiên luôn là mặc định; địa chỉ đang mặc định không tự bỏ mặc định được
        boolean macDinh = form.isLaMacDinh() || soLuong == 0 || (cu != null && cu.isLaMacDinh());
        if (macDinh) {
            soDiaChiRepository.boMacDinh(tk.getMaTaiKhoan(), cu == null ? null : cu.getMaDiaChi());
            tk = layTaiKhoan(email); // câu UPDATE ở trên làm mới bộ nhớ đệm, nạp lại thực thể
        }
        SoDiaChi dc = cu == null ? new SoDiaChi() : timCuaKhach(email, cu.getMaDiaChi());
        if (cu == null) {
            dc.setTaiKhoan(tk);
            dc.setNgayTao(LocalDateTime.now());
        }
        dc.setTenNguoiNhan(form.getTenNguoiNhan().trim());
        dc.setSoDienThoai(form.getSoDienThoai().trim());
        dc.setKhuVuc(khuVuc);
        dc.setDiaChiChiTiet(chiTiet);
        dc.setLoaiDiaChi(SoDiaChi.VAN_PHONG.equals(form.getLoaiDiaChi()) ? SoDiaChi.VAN_PHONG : SoDiaChi.NHA_RIENG);
        dc.setLaMacDinh(macDinh);
        dc = soDiaChiRepository.save(dc);
        if (macDinh) {
            tk.setDiaChi(dc.getDiaChiDayDu());
        }
        return dc.getMaDiaChi();
    }

    @Transactional
    public void datMacDinh(String email, Integer maDiaChi) {
        SoDiaChi dc = timCuaKhach(email, maDiaChi);
        if (dc.isLaMacDinh()) {
            return;
        }
        soDiaChiRepository.boMacDinh(dc.getTaiKhoan().getMaTaiKhoan(), maDiaChi);
        SoDiaChi moi = timCuaKhach(email, maDiaChi);
        moi.setLaMacDinh(true);
        layTaiKhoan(email).setDiaChi(moi.getDiaChiDayDu());
    }

    /** Xóa địa chỉ mặc định thì địa chỉ đầu tiên còn lại thành mặc định (như Project-Website). */
    @Transactional
    public void xoa(String email, Integer maDiaChi) {
        SoDiaChi dc = timCuaKhach(email, maDiaChi);
        boolean laMacDinh = dc.isLaMacDinh();
        soDiaChiRepository.delete(dc);
        soDiaChiRepository.flush();
        if (laMacDinh) {
            List<SoDiaChi> conLai = soDiaChiRepository.cuaKhach(email);
            if (!conLai.isEmpty()) {
                SoDiaChi moi = conLai.get(0);
                moi.setLaMacDinh(true);
                layTaiKhoan(email).setDiaChi(moi.getDiaChiDayDu());
            }
        }
    }

    private TaiKhoan layTaiKhoan(String email) {
        return taiKhoanRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new NghiepVuException("Không tìm thấy tài khoản, vui lòng đăng nhập lại."));
    }

    private static String catDoDai(String chuoi, int toiDa) {
        return chuoi.length() <= toiDa ? chuoi : chuoi.substring(0, toiDa);
    }
}
