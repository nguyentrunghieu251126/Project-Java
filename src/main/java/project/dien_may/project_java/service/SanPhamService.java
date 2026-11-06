package project.dien_may.project_java.service;

import org.springframework.stereotype.Service;
import project.dien_may.project_java.entity.SanPham;
import project.dien_may.project_java.dto.KhoangGiaNhanh;
import project.dien_may.project_java.entity.DanhMuc;
import project.dien_may.project_java.repository.DanhMucRepository;
import project.dien_may.project_java.repository.SanPhamRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;

@Service
public class SanPhamService {

    // So san pham toi da tren moi trang
    public static final int SO_SAN_PHAM_MOI_TRANG = 8;

    private final SanPhamRepository sanPhamRepository;
    private final DanhMucRepository danhMucRepository;

    public SanPhamService(SanPhamRepository sanPhamRepository,
                          DanhMucRepository danhMucRepository) {
        this.sanPhamRepository = sanPhamRepository;
        this.danhMucRepository = danhMucRepository;
    }

    // Lay tat ca san pham dang kinh doanh
    public List<SanPham> layDanhSachSanPham() {
        return sanPhamRepository.findByDangKinhDoanhTrue();
    }

    // Lay san pham theo danh muc
    public List<SanPham> laySanPhamTheoDanhMuc(Integer maDanhMuc) {
        if (maDanhMuc == null || maDanhMuc <= 0) {
            throw new IllegalArgumentException("Mã danh mục không hợp lệ");
        }

        return sanPhamRepository
                .findByDanhMuc_MaDanhMucAndDangKinhDoanhTrue(maDanhMuc);
    }

    // Tim kiem san pham theo ten
    public List<SanPham> timKiemSanPham(String tuKhoa) {
        if (tuKhoa == null || tuKhoa.trim().isEmpty()) {
            return layDanhSachSanPham();
        }

        return sanPhamRepository
                .findByTenSanPhamContainingIgnoreCaseAndDangKinhDoanhTrue(
                        tuKhoa.trim()
                );
    }

    // Lay chi tiet san pham theo ma
    public Optional<SanPham> timSanPhamTheoMa(Integer maSanPham) {
        if (maSanPham == null || maSanPham <= 0) {
            return Optional.empty();
        }

        return sanPhamRepository.findById(maSanPham)
                .filter(SanPham::isDangKinhDoanh);
    }

    // Lay danh sach san pham co phan trang va sap xep
    public Page<SanPham> laySanPhamPhanTrang(
            int trang,
            String sapXep
    ) {

        // Kiem tra so trang
        if (trang < 0) {
            trang = 0;
        }

        // Mac dinh sap xep theo ten san pham
        Sort sort = Sort.by("tenSanPham").ascending();

        // Sap xep theo gia tang dan
        if ("gia-tang".equals(sapXep)) {
            sort = Sort.by("donGia").ascending();
        }

        // Sap xep theo gia giam dan
        else if ("gia-giam".equals(sapXep)) {
            sort = Sort.by("donGia").descending();
        }

        // Sap xep theo ten Z-A
        else if ("ten-giam".equals(sapXep)) {
            sort = Sort.by("tenSanPham").descending();
        }

        // Moi trang hien thi toi da SO_SAN_PHAM_MOI_TRANG san pham
        PageRequest pageRequest = PageRequest.of(
                trang,
                SO_SAN_PHAM_MOI_TRANG,
                sort
        );

        return sanPhamRepository.findByDangKinhDoanhTrue(pageRequest);
    }

    // Loc san pham theo danh muc, co phan trang
    public Page<SanPham> locSanPhamTheoDanhMuc(
            Integer maDanhMuc,
            int trang,
            String sapXep
    ) {
        if (maDanhMuc == null || maDanhMuc <= 0) {
            throw new IllegalArgumentException("Mã danh mục không hợp lệ");
        }
        if (trang < 0) {
            trang = 0;
        }
        // Mac dinh sap xep theo ten A-Z
        Sort sort = Sort.by("tenSanPham").ascending();

        if ("ten-giam".equals(sapXep)) {
            sort = Sort.by("tenSanPham").descending();
        } else if ("gia-tang".equals(sapXep)) {
            sort = Sort.by("donGia").ascending();
        } else if ("gia-giam".equals(sapXep)) {
            sort = Sort.by("donGia").descending();
        }
        // Moi trang toi da SO_SAN_PHAM_MOI_TRANG san pham
        PageRequest pageRequest = PageRequest.of(trang, SO_SAN_PHAM_MOI_TRANG, sort);
        return sanPhamRepository.findByDanhMuc_MaDanhMucAndDangKinhDoanhTrue(maDanhMuc, pageRequest);
    }

    // ====================  TIM KIEM, LOC, SAP XEP ====================

    public String chuanHoaTuKhoa(String tuKhoa) {
        if (tuKhoa == null) {
            return "";
        }
        return tuKhoa.trim().replaceAll("\\s+", " ");
    }
    public List<DanhMuc> layDanhSachDanhMuc() {
        return danhMucRepository.findAll(Sort.by("tenDanhMuc").ascending());
    }
    public Optional<DanhMuc> timDanhMucTheoMa(Integer maDanhMuc) {
        if (maDanhMuc == null || maDanhMuc <= 0) {
            return Optional.empty();
        }
        return danhMucRepository.findById(maDanhMuc);
    }

    // Lay danh sach thuong hieu de do vao bo loc
    public List<String> layDanhSachThuongHieu() {
        return sanPhamRepository.layDanhSachThuongHieu();
    }
    public Page<SanPham> timKiemVaLoc(String tuKhoa, Integer maDanhMuc, List<String> thuongHieu, BigDecimal giaTu, BigDecimal giaDen, int trang, String sapXep) {
        if (trang < 0) {
            trang = 0;
        }
        PageRequest pageRequest = PageRequest.of(trang, SO_SAN_PHAM_MOI_TRANG, taoSapXep(sapXep));
        return sanPhamRepository.findAll(taoDieuKien(tuKhoa, maDanhMuc, thuongHieu, giaTu, giaDen), pageRequest);
    }

    // Chi cho phep 4 kieu sap xep hop le, them ma san pham de thu tu on dinh khi phan trang
    private Sort taoSapXep(String sapXep) {
        Sort sort;
        if ("ten-giam".equals(sapXep)) {
            sort = Sort.by("tenSanPham").descending();
        } else if ("gia-tang".equals(sapXep)) {
            sort = Sort.by("donGia").ascending();
        } else if ("gia-giam".equals(sapXep)) {
            sort = Sort.by("donGia").descending();
        } else {
            sort = Sort.by("tenSanPham").ascending();
        }

        return sort.and(Sort.by("maSanPham").ascending());
    }
    private Specification<SanPham> taoDieuKien(
            String tuKhoa,
            Integer maDanhMuc,
            List<String> thuongHieu,
            BigDecimal giaTu,
            BigDecimal giaDen
    ) {
        String tuKhoaChuan = chuanHoaTuKhoa(tuKhoa);
        List<String> thuongHieuChuan = chuanHoaDanhSachThuongHieu(thuongHieu);

        return (root, query, cb) -> {
            List<Predicate> dieuKien = new ArrayList<>();
            // Chi lay san pham dang kinh doanh
            dieuKien.add(cb.isTrue(root.get("dangKinhDoanh")));
            // Tu khoa: khong phan biet hoa thuong, thoat ky tu dac biet cua LIKE
            if (!tuKhoaChuan.isEmpty()) {
                String mau = "%" + thoatKyTuLike(tuKhoaChuan.toLowerCase(Locale.ROOT)) + "%";
                dieuKien.add(cb.like(cb.lower(root.get("tenSanPham")), mau, '\\'));
            }
            if (maDanhMuc != null) {
                dieuKien.add(cb.equal(root.get("danhMuc").get("maDanhMuc"), maDanhMuc));
            }
            // Nhieu thuong hieu: lay san pham thuoc BAT KY thuong hieu nao duoc chon
            if (!thuongHieuChuan.isEmpty()) {
                dieuKien.add(cb.lower(root.get("thuongHieu")).in(thuongHieuChuan));
            }
            if (giaTu != null) {
                dieuKien.add(cb.greaterThanOrEqualTo(root.get("donGia"), giaTu));
            }
            if (giaDen != null) {
                dieuKien.add(cb.lessThanOrEqualTo(root.get("donGia"), giaDen));
            }
            return cb.and(dieuKien.toArray(new Predicate[0]));
        };
    }
    // ==================== HO TRO BO LOC ====================
    public String chuanHoaSapXep(String sapXep) {
        if ("ten-giam".equals(sapXep)
                || "gia-tang".equals(sapXep)
                || "gia-giam".equals(sapXep)) {
            return sapXep;
        }

        return "ten-tang";
    }
    public List<String> layDanhSachThuongHieu(Integer maDanhMuc) {
        if (maDanhMuc == null || maDanhMuc <= 0) {
            return sanPhamRepository.layDanhSachThuongHieu();
        }

        return sanPhamRepository.layDanhSachThuongHieuTheoDanhMuc(maDanhMuc);
    }

    public BigDecimal layGiaThanhKeoToiThieu() {
        BigDecimal gia = sanPhamRepository.layGiaThapNhat();

        if (gia == null) {
            return BigDecimal.ZERO;
        }

        return gia.divide(BigDecimal.valueOf(1000), 0, RoundingMode.FLOOR)
                .multiply(BigDecimal.valueOf(1000));
    }
    public BigDecimal layGiaThanhKeoToiDa() {
        BigDecimal gia = sanPhamRepository.layGiaCaoNhat();

        if (gia == null) {
            return BigDecimal.ZERO;
        }

        return gia.divide(BigDecimal.valueOf(1000), 0, RoundingMode.CEILING)
                .multiply(BigDecimal.valueOf(1000));
    }

    public List<KhoangGiaNhanh> layKhoangGiaNhanh(BigDecimal giaTuChon, BigDecimal giaDenChon) {
        List<KhoangGiaNhanh> danhSach = new ArrayList<>();

        danhSach.add(taoKhoangGia("Dưới 2 triệu", null, 1_999_999L, giaTuChon, giaDenChon));
        danhSach.add(taoKhoangGia("Từ 2 - 4 triệu", 2_000_000L, 3_999_999L, giaTuChon, giaDenChon));
        danhSach.add(taoKhoangGia("Từ 4 - 7 triệu", 4_000_000L, 6_999_999L, giaTuChon, giaDenChon));
        danhSach.add(taoKhoangGia("Từ 7 - 13 triệu", 7_000_000L, 12_999_999L, giaTuChon, giaDenChon));
        danhSach.add(taoKhoangGia("Từ 13 - 20 triệu", 13_000_000L, 19_999_999L, giaTuChon, giaDenChon));
        danhSach.add(taoKhoangGia("Trên 20 triệu", 20_000_000L, null, giaTuChon, giaDenChon));

        return danhSach;
    }

    private KhoangGiaNhanh taoKhoangGia(
            String nhan,
            Long giaTu,
            Long giaDen,
            BigDecimal giaTuChon,
            BigDecimal giaDenChon
    ) {
        boolean dangChon = bangNhau(giaTu, giaTuChon) && bangNhau(giaDen, giaDenChon);

        return new KhoangGiaNhanh(nhan, giaTu, giaDen, dangChon);
    }

    private boolean bangNhau(Long moc, BigDecimal giaTriChon) {
        if (moc == null || giaTriChon == null) {
            return moc == null && giaTriChon == null;
        }

        return giaTriChon.compareTo(BigDecimal.valueOf(moc)) == 0;
    }

    private List<String> chuanHoaDanhSachThuongHieu(List<String> thuongHieu) {
        List<String> ketQua = new ArrayList<>();

        if (thuongHieu == null) {
            return ketQua;
        }

        for (String hang : thuongHieu) {
            if (hang == null) {
                continue;
            }

            String hangChuan = hang.trim().toLowerCase(Locale.ROOT);

            if (!hangChuan.isEmpty() && !ketQua.contains(hangChuan)) {
                ketQua.add(hangChuan);
            }
        }

        return ketQua;
    }
    private String thoatKyTuLike(String giaTri) {
        return giaTri
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_")
                .replace("[", "\\[");
    }
}