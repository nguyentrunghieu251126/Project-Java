package project.dien_may.project_java.controller;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import project.dien_may.project_java.entity.DanhMuc;
import project.dien_may.project_java.entity.SanPham;
import project.dien_may.project_java.service.SanPhamService;
import project.dien_may.project_java.service.SanPhamNoiBatService;
import project.dien_may.project_java.service.ChiTietSanPhamService;
import project.dien_may.project_java.dto.SanPhamNoiBat;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Controller
public class HomeController {

    // Do dai toi da cua tu khoa tim kiem (khop maxlength cua o tim kiem tren Header)
    private static final int DO_DAI_TU_KHOA_TOI_DA = 100;

    private final SanPhamService sanPhamService;
    private final SanPhamNoiBatService sanPhamNoiBatService;
    private final ChiTietSanPhamService chiTietSanPhamService;

    public HomeController(SanPhamService sanPhamService, SanPhamNoiBatService sanPhamNoiBatService, ChiTietSanPhamService chiTietSanPhamService) {
        this.sanPhamService = sanPhamService;
        this.sanPhamNoiBatService = sanPhamNoiBatService;
        this.chiTietSanPhamService = chiTietSanPhamService;
    }

    // Trang chu: tim kiem, loc (danh muc, thuong hieu, khoang gia), sap xep va phan trang
    @GetMapping("/")
    public String Index(
            @RequestParam(name = "trang", defaultValue = "0") String trangText,
            @RequestParam(name = "sapXep", defaultValue = "ten-tang") String sapXepText,
            @RequestParam(name = "maDanhMuc", required = false) String maDanhMucText,
            @RequestParam(name = "tuKhoa", required = false) String tuKhoaText,
            @RequestParam(name = "thuongHieu", required = false) List<String> thuongHieuText,
            @RequestParam(name = "giaTu", required = false) String giaTuText,
            @RequestParam(name = "giaDen", required = false) String giaDenText,
            Model model
    ) {

        // Kiem tra va chuan hoa tham so (tham so sai thi bo qua, khong bao loi 400)
        int trang = docSoTrang(trangText);
        String sapXep = sanPhamService.chuanHoaSapXep(sapXepText);
        String tuKhoa = chuanHoaTuKhoa(tuKhoaText);
        Integer maDanhMuc = docSoNguyenDuong(maDanhMucText);
        BigDecimal giaTu = docGia(giaTuText);
        BigDecimal giaDen = docGia(giaDenText);

        // Gia toi thieu lon hon gia toi da thi doi cho hai gia tri
        if (giaTu != null && giaDen != null && giaTu.compareTo(giaDen) > 0) {
            BigDecimal tam = giaTu;
            giaTu = giaDen;
            giaDen = tam;
        }

        // Gia tri mac dinh de Index.html luon hien thi duoc, ke ca khi truy van loi
        model.addAttribute("maDanhMuc", maDanhMuc);
        model.addAttribute("sapXep", sapXep);
        model.addAttribute("tuKhoa", tuKhoa);
        model.addAttribute("giaTu", giaTu);
        model.addAttribute("giaDen", giaDen);
        model.addAttribute("thuongHieuDaChon", Collections.emptyList());
        model.addAttribute("danhSachDanhMuc", Collections.emptyList());
        model.addAttribute("danhSachThuongHieu", Collections.emptyList());
        model.addAttribute("khoangGiaNhanh", Collections.emptyList());
        model.addAttribute("giaThanhKeoToiThieu", BigDecimal.ZERO);
        model.addAttribute("giaThanhKeoToiDa", BigDecimal.ZERO);
        // Các khung trang chủ có mặc định rỗng khi SQL Server chưa có dữ liệu.
        model.addAttribute("danhSachNoiBat", Collections.<SanPhamNoiBat>emptyList());
        model.addAttribute("danhSachBanChay", Collections.<SanPhamNoiBat>emptyList());
        model.addAttribute("tieuDeDanhSach", "Tất cả sản phẩm");
        model.addAttribute("coBoLoc", false);
        model.addAttribute("chuoiLoc", "");
        model.addAttribute("chuoiKhongSapXep", "");
        model.addAttribute("chuoiKhongDanhMuc", "");
        model.addAttribute("chuoiKhongGia", "");

        try {
            // Du lieu do vao bo loc, lay tu SQL Server
            List<DanhMuc> danhSachDanhMuc = sanPhamService.layDanhSachDanhMuc();
            // Dang chon danh muc thi chi hien thuong hieu cua danh muc do
            List<String> danhSachThuongHieu = sanPhamService.layDanhSachThuongHieu(maDanhMuc);
            // Chi nhan thuong hieu co trong danh sach hien thi, dung dung chu hoa chu thuong
            List<String> thuongHieuDaChon = locThuongHieuHopLe(thuongHieuText, danhSachThuongHieu);
            // Tim kiem + loc + sap xep + phan trang trong mot lan truy van
            Page<SanPham> ketQua = sanPhamService.timKiemVaLoc(tuKhoa, maDanhMuc, thuongHieuDaChon, giaTu, giaDen, trang, sapXep);
            // Trang vuot qua gioi han thi quay ve trang dau
            if (ketQua.getTotalPages() > 0 && trang >= ketQua.getTotalPages()) {
                trang = 0;
                ketQua = sanPhamService.timKiemVaLoc(tuKhoa, maDanhMuc, thuongHieuDaChon, giaTu, giaDen, trang, sapXep);
            }

            // Ten danh muc dang chon (neu co) de lam tieu de danh sach
            String tenDanhMuc = null;
            if (maDanhMuc != null) {
                tenDanhMuc = sanPhamService.timDanhMucTheoMa(maDanhMuc).map(DanhMuc::getTenDanhMuc).orElse(null);
            }

            // Truyen du lieu sang Index.html
            model.addAttribute("danhSachSanPham", ketQua.getContent());
            // Ánh xạ ảnh đúng mã model chỉ cho danh sách, không sửa layout trang chủ.
            java.util.Map<Integer, String> anhDanhSachSanPham = new java.util.LinkedHashMap<>();
            for (SanPham sp : ketQua.getContent()) {anhDanhSachSanPham.put(sp.getMaSanPham(), chiTietSanPhamService.layAnhDaiDien(sp));
            }
            model.addAttribute("anhDanhSachSanPham", anhDanhSachSanPham);
            model.addAttribute("trangHienTai", trang);
            model.addAttribute("tongSoTrang", ketQua.getTotalPages());
            model.addAttribute("tongSoSanPham", ketQua.getTotalElements());

            model.addAttribute("danhSachDanhMuc", danhSachDanhMuc);
            model.addAttribute("danhSachThuongHieu", danhSachThuongHieu);
            model.addAttribute("thuongHieuDaChon", thuongHieuDaChon);
            model.addAttribute("khoangGiaNhanh", sanPhamService.layKhoangGiaNhanh(giaTu, giaDen));
            model.addAttribute("giaThanhKeoToiThieu", sanPhamService.layGiaThanhKeoToiThieu());
            model.addAttribute("giaThanhKeoToiDa", sanPhamService.layGiaThanhKeoToiDa());

            model.addAttribute("tieuDeDanhSach", taoTieuDe(tuKhoa, tenDanhMuc));
            model.addAttribute("coBoLoc", !tuKhoa.isEmpty() || maDanhMuc != null || !thuongHieuDaChon.isEmpty() || giaTu != null || giaDen != null);

            // Cac chuoi query de giu dieu kien khi phan trang, doi sap xep, doi danh muc, doi gia
            String phanTuKhoa = tuKhoa.isEmpty() ? "" : "&tuKhoa=" + maHoa(tuKhoa);
            String phanDanhMuc = maDanhMuc == null ? "" : "&maDanhMuc=" + maDanhMuc;
            String phanThuongHieu = taoPhanThuongHieu(thuongHieuDaChon);
            String phanGia = (giaTu == null ? "" : "&giaTu=" + giaTu.toPlainString()) + (giaDen == null ? "" : "&giaDen=" + giaDen.toPlainString());
            String phanSapXep = "&sapXep=" + sapXep;

            model.addAttribute("chuoiLoc", phanTuKhoa + phanDanhMuc + phanThuongHieu + phanGia + phanSapXep);
            model.addAttribute("chuoiKhongSapXep", phanTuKhoa + phanDanhMuc + phanThuongHieu + phanGia);
            // Doi danh muc thi bo thuong hieu da chon (thuong hieu phu thuoc danh muc)
            model.addAttribute("chuoiKhongDanhMuc", phanTuKhoa + phanGia + phanSapXep);
            model.addAttribute("chuoiKhongGia", phanTuKhoa + phanDanhMuc + phanThuongHieu + phanSapXep);

        } catch (Exception e) {
            model.addAttribute("danhSachSanPham", Collections.emptyList());
            model.addAttribute("trangHienTai", 0);
            model.addAttribute("tongSoTrang", 0);
            model.addAttribute("tongSoSanPham", 0);
            model.addAttribute("loiSanPham", "Không thể tải danh sách sản phẩm. Vui lòng thử lại sau."
            );
        }
        try {
            model.addAttribute("danhSachNoiBat", sanPhamNoiBatService.laySanPhamNoiBat());
            model.addAttribute("danhSachBanChay", sanPhamNoiBatService.laySanPhamBanChay());
        } catch (Exception e) {
            model.addAttribute("loiNoiBat", "Không thể tải sản phẩm nổi bật. Vui lòng thử lại sau.");
        }
        return "Index";
    }

    // So trang: khong phai so hoac am thi ve trang dau
    private int docSoTrang(String giaTri) {
        try {
            int trang = Integer.parseInt(giaTri.trim());
            return Math.max(trang, 0);
        } catch (Exception e) {
            return 0;
        }
    }

    // Ma danh muc: khong phai so nguyen duong thi bo qua
    private Integer docSoNguyenDuong(String giaTri) {
        if (giaTri == null || giaTri.isBlank()) {
            return null;
        }
        try {
            int so = Integer.parseInt(giaTri.trim());
            return so > 0 ? so : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
    // Gia: chi nhan so nguyen khong am, gia tri la thi bo qua
    private BigDecimal docGia(String giaTri) {
        if (giaTri == null || giaTri.isBlank()) {
            return null;
        }

        String chuoi = giaTri.trim();
        if (!chuoi.matches("\\d{1,12}")) {
            return null;
        }
        return new BigDecimal(chuoi);
    }
    // Tu khoa: bo khoang trang thua, gioi han do dai
    private String chuanHoaTuKhoa(String tuKhoa) {
        String chuan = sanPhamService.chuanHoaTuKhoa(tuKhoa);

        if (chuan.length() > DO_DAI_TU_KHOA_TOI_DA) {
            chuan = chuan.substring(0, DO_DAI_TU_KHOA_TOI_DA).trim();
        }

        return chuan;
    }

    // Giu lai thuong hieu co trong danh sach hien thi, lay dung ten goc trong SQL
    private List<String> locThuongHieuHopLe(List<String> duocChon, List<String> choPhep) {
        List<String> ketQua = new ArrayList<>();
        if (duocChon == null) {
            return ketQua;
        }
        for (String hang : duocChon) {
            if (hang == null) {
                continue;
            }
            for (String hangHopLe : choPhep) {
                if (hangHopLe.equalsIgnoreCase(hang.trim()) && !ketQua.contains(hangHopLe)) {
                    ketQua.add(hangHopLe);
                }
            }
        }
        return ketQua;
    }
    // Tieu de khu vuc danh sach
    private String taoTieuDe(String tuKhoa, String tenDanhMuc) {
        if (!tuKhoa.isEmpty()) {
            return "Kết quả tìm kiếm cho \"" + tuKhoa + "\"";
        }
        if (tenDanhMuc != null) {
            return tenDanhMuc;
        }
        return "Tất cả sản phẩm";
    }

    private String taoPhanThuongHieu(List<String> thuongHieu) {
        StringBuilder sb = new StringBuilder();
        for (String hang : thuongHieu) {
            sb.append("&thuongHieu=").append(maHoa(hang));
        }
        return sb.toString();
    }
    private String maHoa(String giaTri) {
        return URLEncoder.encode(giaTri, StandardCharsets.UTF_8);
    }
}
