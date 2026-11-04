package project.dien_may.project_java.service.admin;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Errors;
import org.springframework.web.server.ResponseStatusException;
import project.dien_may.project_java.dto.SanPhamForm;
import project.dien_may.project_java.entity.DanhMuc;
import project.dien_may.project_java.entity.SanPham;
import project.dien_may.project_java.repository.DanhMucRepository;
import project.dien_may.project_java.repository.SanPhamRepository;

@Service
public class SanPhamAdminService {

    private final SanPhamRepository sanPhamRepository;
    private final DanhMucRepository danhMucRepository;

    public SanPhamAdminService(SanPhamRepository sanPhamRepository, DanhMucRepository danhMucRepository) {
        this.sanPhamRepository = sanPhamRepository;
        this.danhMucRepository = danhMucRepository;
    }

    @Transactional(readOnly = true)
    public SanPhamForm layForm(Integer ma) {
        return SanPhamForm.tu(timTheoMa(ma));
    }

    @Transactional(readOnly = true)
    public void kiemTraTrung(SanPhamForm form, Errors errors) {
        if (form.getMaModel() == null) {
            return;
        }
        boolean trung = form.getMaSanPham() == null
                ? sanPhamRepository.existsByMaModelIgnoreCase(form.getMaModel())
                : sanPhamRepository.existsByMaModelIgnoreCaseAndMaSanPhamNot(form.getMaModel(), form.getMaSanPham());
        if (trung) {
            errors.rejectValue("maModel", "maModel.trung", "Mã model này đã tồn tại.");
        }
    }

    @Transactional
    public void them(SanPhamForm form) {
        SanPham sp = new SanPham();
        sp.setSoLuong(form.getSoLuong());
        ganThongTin(sp, form);
        sanPhamRepository.save(sp);
    }

    @Transactional
    public void capNhat(Integer ma, SanPhamForm form) {
        SanPham sp = timTheoMa(ma);
        if (!sp.getPhienBan().equals(form.getPhienBan())) {
            throw new OptimisticLockingFailureException("Sản phẩm " + ma + " đã thay đổi");
        }
        ganThongTin(sp, form); // không gán soLuong: tồn kho chỉ đổi qua phiếu nhập, phiếu xuất
    }

    /**
     * Sản phẩm đã có chứng từ phải giữ lại để không làm sai lịch sử và báo cáo,
     * nên chuyển sang ngừng kinh doanh thay vì xóa.
     *
     * @return true nếu đã xóa hẳn, false nếu chỉ ngừng kinh doanh
     */
    @Transactional
    public boolean xoa(Integer ma) {
        SanPham sp = timTheoMa(ma);
        if (sanPhamRepository.daPhatSinhChungTu(ma) == 1) {
            sp.setDangKinhDoanh(false);
            return false;
        }
        sanPhamRepository.delete(sp);
        return true;
    }

    private void ganThongTin(SanPham sp, SanPhamForm form) {
        DanhMuc danhMuc = danhMucRepository.findById(form.getMaDanhMuc())
                .orElseThrow(() -> new NghiepVuException("Danh mục không tồn tại."));
        sp.setMaModel(form.getMaModel());
        sp.setTenSanPham(form.getTenSanPham());
        sp.setThuongHieu(form.getThuongHieu());
        sp.setDanhMuc(danhMuc);
        sp.setDonGia(form.getDonGia());
        sp.setMucTonToiThieu(form.getMucTonToiThieu());
        sp.setThoiGianBaoHanh(form.getThoiGianBaoHanh());
        sp.setHinhAnh(form.getHinhAnh());
        sp.setMoTa(form.getMoTa());
        sp.setDangKinhDoanh(form.isDangKinhDoanh());
    }

    private SanPham timTheoMa(Integer ma) {
        return sanPhamRepository.findById(ma)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
}
