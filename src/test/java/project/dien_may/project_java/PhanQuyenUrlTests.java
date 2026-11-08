package project.dien_may.project_java;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;
import project.dien_may.project_java.config.TabKhuQuanLy;
import project.dien_may.project_java.entity.ChiTietDonHang;
import project.dien_may.project_java.entity.ChiTietYeuCauNhapHang;
import project.dien_may.project_java.repository.ChiTietYeuCauNhapHangRepository;
import project.dien_may.project_java.entity.SanPham;
import project.dien_may.project_java.repository.ChiTietDonHangRepository;
import project.dien_may.project_java.entity.TrangThaiYeuCauNhap;
import project.dien_may.project_java.entity.YeuCauNhapHang;
import project.dien_may.project_java.repository.SanPhamRepository;
import project.dien_may.project_java.repository.TaiKhoanRepository;
import project.dien_may.project_java.repository.YeuCauNhapHangRepository;

import java.util.List;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.mockito.Mockito.doReturn;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** NFR05: chặn truy cập URL khi không đủ quyền. Quyền đọc thật từ CSDL theo dữ liệu mẫu, chỉ gọi GET hoặc bị chặn trước khi xử lý. */
@SpringBootTest
@AutoConfigureMockMvc
class PhanQuyenUrlTests {

    private static final String QUAN_TRI = "admin1@dienmay.vn";
    private static final String NHAN_VIEN_KHO = "kho1@dienmay.vn";
    private static final String KHACH_HANG = "lan.bui7@gmail.com";
    private static final String EMAIL_GIA_LAP = "gia-lap@dienmay.test";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private YeuCauNhapHangRepository yeuCauRepository;

    @Autowired
    private TaiKhoanRepository taiKhoanRepository;

    @Autowired
    private SanPhamRepository sanPhamRepository;

    @Autowired
    private ChiTietDonHangRepository chiTietDonHangRepository;

    @Autowired
    private ChiTietYeuCauNhapHangRepository chiTietYeuCauNhapRepository;

    @MockitoSpyBean
    private UserDetailsService userDetailsService;

    /**
     * Người dùng có đúng vai trò và quyền cho trước. DongBoTaiKhoanFilter nạp lại quyền ở mỗi request,
     * nên phải giả lập ở UserDetailsService; các tài khoản thật vẫn đọc từ CSDL.
     */
    private RequestPostProcessor nguoiDung(String vaiTro, String... dsQuyen) {
        UserDetails nguoiDung = User.withUsername(EMAIL_GIA_LAP).password("khong-dung")
                .authorities(Stream.concat(Stream.of("ROLE_" + vaiTro), Stream.of(dsQuyen)).toArray(String[]::new))
                .build();
        doReturn(nguoiDung).when(userDetailsService).loadUserByUsername(EMAIL_GIA_LAP);
        return user(nguoiDung);
    }

    /**
     * Cho một sản phẩm hết hàng để màn hình có dòng cảnh báo, không phụ thuộc dữ liệu hiện có.
     * Chỉ dùng trong test có @Transactional nên được rollback.
     */
    private void taoSanPhamCanhBao() {
        SanPham sanPham = sanPhamRepository.findByDangKinhDoanhTrue().get(0);
        sanPham.setSoLuong(0); // hết hàng nên luôn đứng đầu danh sách xếp theo tồn tăng dần
        sanPhamRepository.saveAndFlush(sanPham);
    }

    private YeuCauNhapHang yeuCauChoDuyet() {
        List<YeuCauNhapHang> ds = yeuCauRepository
                .timKiem(TrangThaiYeuCauNhap.CHO_DUYET, PageRequest.of(0, 1)).getContent();
        assumeFalse(ds.isEmpty(), "CSDL không còn yêu cầu nhập nào đang chờ duyệt");
        return ds.get(0);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/admin", "/admin/bao-cao/doanh-thu", "/admin/bao-cao/ban-chay", "/admin/bao-cao/ton-kho",
            "/admin/san-pham", "/admin/san-pham/them", "/admin/khuyen-mai", "/admin/kho/don-hang", "/admin/kho/ton-kho",
            "/admin/kho/yeu-cau-nhap", "/admin/tai-khoan", "/admin/phan-quyen", "/admin/phan-quyen/nhan-vien"})
    void quanTriVaoDuocMoiTrang(String url) throws Exception {
        mockMvc.perform(get(url).with(user(QUAN_TRI))).andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/admin", "/admin/san-pham", "/admin/tai-khoan", "/admin/phan-quyen"})
    void khachHangKhongVaoDuocKhuQuanLy(String url) throws Exception {
        mockMvc.perform(get(url).with(user(KHACH_HANG))).andExpect(status().isForbidden());
    }

    @Test
    void khachHangLoDuocGanQuyenKhuQuanLyVanBiChan() throws Exception {
        mockMvc.perform(get("/admin/san-pham").with(nguoiDung("KHACH_HANG", "SANPHAM_XEM")))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/admin/tai-khoan", "/admin/phan-quyen"})
    void nhanVienKhoChuaDuocCapQuyenThiBiChan(String url) throws Exception {
        mockMvc.perform(get(url).with(user(NHAN_VIEN_KHO))).andExpect(status().isForbidden());
    }

    @Test
    void nhanVienKhoDuocCapQuyenHeThongThiVaoDuoc() throws Exception {
        RequestPostProcessor nhanVien = nguoiDung("NHAN_VIEN_KHO", "PHANQUYEN_QUANLY", "TAIKHOAN_XEM");
        mockMvc.perform(get("/admin/phan-quyen").with(nhanVien)).andExpect(status().isOk());
        mockMvc.perform(get("/admin/tai-khoan").with(nhanVien)).andExpect(status().isOk());
    }

    @Test
    @Transactional
    void cotLapPhieuChiHienKhiCoQuyenVaChiChoSanPhamCanhBao() throws Exception {
        taoSanPhamCanhBao();
        mockMvc.perform(get("/admin/kho/ton-kho").with(nguoiDung("NHAN_VIEN_KHO", "KHO_XEM_TONKHO")))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Lập phiếu"))));
        mockMvc.perform(get("/admin/kho/ton-kho?tinhTrang=DU")
                        .with(nguoiDung("NHAN_VIEN_KHO", "KHO_XEM_TONKHO", "KHO_YEUCAU_NHAP")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(">Thao tác</th>")))
                .andExpect(content().string(not(containsString("name=\"soLuong\""))));
        // Danh sách xếp theo tồn tăng dần nên trang đầu có sản phẩm cảnh báo
        mockMvc.perform(get("/admin/kho/ton-kho")
                        .with(nguoiDung("NHAN_VIEN_KHO", "KHO_XEM_TONKHO", "KHO_YEUCAU_NHAP")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"soLuong\"")))
                .andExpect(content().string(containsString("name=\"_csrf\"")));
    }

    @Test
    void lapPhieuPhaiCoQuyenYeuCauNhap() throws Exception {
        mockMvc.perform(post("/admin/kho/ton-kho/1/yeu-cau-nhap").param("soLuong", "5")
                        .with(nguoiDung("NHAN_VIEN_KHO", "KHO_XEM_TONKHO")).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void nhanVienKhoVaoDuocTrangCoQuyen() throws Exception {
        RequestPostProcessor nhanVien = nguoiDung("NHAN_VIEN_KHO", "SANPHAM_XEM");
        mockMvc.perform(get("/admin/san-pham").with(nhanVien)).andExpect(status().isOk());
    }

    @Test
    void nhanVienKhoBiChanTrangKhongCoQuyen() throws Exception {
        RequestPostProcessor nhanVien = nguoiDung("NHAN_VIEN_KHO", "SANPHAM_XEM");
        mockMvc.perform(get("/admin/san-pham/them").with(nhanVien)).andExpect(status().isForbidden());
        mockMvc.perform(post("/admin/san-pham/1/xoa").with(nhanVien).with(csrf())).andExpect(status().isForbidden());
    }

    @Test
    void nutTrenDanhSachSanPhamTheoDungQuyen() throws Exception {
        mockMvc.perform(get("/admin/san-pham").with(nguoiDung("NHAN_VIEN_KHO", "SANPHAM_XEM", "SANPHAM_SUA")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/sua\"")))
                .andExpect(content().string(not(containsString("/xoa\""))))
                .andExpect(content().string(not(containsString("href=\"/admin/san-pham/them\""))));
    }

    @Test
    void menuNhanVienKhoChiCoMucDuocCap() throws Exception {
        mockMvc.perform(get("/admin/san-pham").with(nguoiDung("NHAN_VIEN_KHO", "SANPHAM_XEM", "KHO_XEM_TONKHO", "KHO_YEUCAU_NHAP")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("href=\"/admin/san-pham\"")))
                .andExpect(content().string(containsString("href=\"/admin/kho/ton-kho\"")))
                .andExpect(content().string(containsString("href=\"/admin/kho/yeu-cau-nhap\"")))
                .andExpect(content().string(not(containsString("href=\"/admin/phan-quyen\""))))
                .andExpect(content().string(not(containsString("href=\"/admin/tai-khoan\""))))
                .andExpect(content().string(not(containsString("Báo cáo thống kê"))));
    }

    @Test
    void khongDuocCapTabNaoThiHienLoiChao() throws Exception {
        // SANPHAM_THEM không mở được tab nào trên menu nên không có chỗ để chuyển tới
        mockMvc.perform(get("/admin").with(nguoiDung("NHAN_VIEN_KHO", "SANPHAM_THEM")))
                .andExpect(status().isOk())
                .andExpect(model().attributeDoesNotExist("tomTat", "bieuDo"))
                .andExpect(content().string(containsString("Xin chào")))
                .andExpect(content().string(containsString("Trang quản lý")))
                .andExpect(content().string(not(containsString("Doanh thu cả năm"))));
    }

    @Test
    void nguoiGuiYeuCauNhapXemDuocNhungKhongDuyetDuoc() throws Exception {
        RequestPostProcessor nhanVien = nguoiDung("NHAN_VIEN_KHO", "KHO_YEUCAU_NHAP");
        mockMvc.perform(get("/admin/kho/yeu-cau-nhap").with(nhanVien))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Xem và duyệt"))));
        mockMvc.perform(post("/admin/kho/yeu-cau-nhap/1/duyet").with(nhanVien).with(csrf()))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/admin/kho/yeu-cau-nhap/1/tu-choi").with(nhanVien).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void chiTietYeuCauNhapChiHienNutDuyetKhiCoQuyen() throws Exception {
        String url = "/admin/kho/yeu-cau-nhap/" + yeuCauChoDuyet().getMaYeuCauNhap();
        mockMvc.perform(get(url).with(nguoiDung("NHAN_VIEN_KHO", "KHO_YEUCAU_NHAP")))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("/duyet"))))
                .andExpect(content().string(not(containsString("hopTuChoi"))));
        mockMvc.perform(get(url).with(user(QUAN_TRI)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/duyet")))
                .andExpect(content().string(containsString("hopTuChoi")));
    }

    @Test
    void danhSachYeuCauNhapHienNutDuyetChoQuanTri() throws Exception {
        yeuCauChoDuyet();
        mockMvc.perform(get("/admin/kho/yeu-cau-nhap?trangThai=CHO_DUYET").with(user(QUAN_TRI)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Xem và duyệt")));
    }

    @Test
    void manHinhPhanQuyenCoHaiTabVaKhongKhoaO() throws Exception {
        mockMvc.perform(get("/admin/phan-quyen").with(user(QUAN_TRI)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Toàn thể nhân viên")))
                .andExpect(content().string(containsString("Từng nhân viên")))
                .andExpect(content().string(containsString("name=\"quyen\"")))
                .andExpect(content().string(not(containsString("disabled=\"disabled\""))))
                .andExpect(content().string(not(containsString("Khách hàng</th>"))));
    }

    @Test
    void tabTungNhanVienChiLietKeNhanVien() throws Exception {
        mockMvc.perform(get("/admin/phan-quyen/nhan-vien").with(user(QUAN_TRI)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(NHAN_VIEN_KHO)))
                .andExpect(content().string(containsString("Xem quyền")))
                .andExpect(content().string(not(containsString(">" + QUAN_TRI + "<"))))
                .andExpect(content().string(not(containsString(">" + KHACH_HANG + "<"))));
    }

    @Test
    void xemQuyenCuaMotNhanVien() throws Exception {
        Integer ma = taiKhoanRepository.findByEmailIgnoreCase(NHAN_VIEN_KHO).orElseThrow().getMaTaiKhoan();
        mockMvc.perform(get("/admin/phan-quyen/nhan-vien/" + ma).with(user(QUAN_TRI)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Lưu quyền của nhân viên")));
    }

    @Test
    void khongMoQuyenRiengChoTaiKhoanKhongPhaiNhanVien() throws Exception {
        Integer ma = taiKhoanRepository.findByEmailIgnoreCase(KHACH_HANG).orElseThrow().getMaTaiKhoan();
        mockMvc.perform(get("/admin/phan-quyen/nhan-vien/" + ma).with(user(QUAN_TRI)))
                .andExpect(status().isNotFound());
    }

    @Test
    void phanQuyenTungNhanVienCanQuyenPhanQuyen() throws Exception {
        RequestPostProcessor nhanVien = nguoiDung("NHAN_VIEN_KHO", "SANPHAM_XEM");
        mockMvc.perform(get("/admin/phan-quyen/nhan-vien").with(nhanVien)).andExpect(status().isForbidden());
        mockMvc.perform(post("/admin/phan-quyen/nhan-vien/1").param("quyen", "1").with(nhanVien).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void tongQuanLaMotChucNangRieng() throws Exception {
        mockMvc.perform(get("/admin").with(nguoiDung("NHAN_VIEN_KHO", "TONGQUAN_XEM")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("bi-speedometer2")))
                .andExpect(content().string(containsString("Doanh thu cả năm")))
                .andExpect(content().string(containsString("khong-bam")))
                .andExpect(content().string(not(containsString("Xem báo cáo"))));
        mockMvc.perform(get("/admin/bao-cao/doanh-thu").with(nguoiDung("NHAN_VIEN_KHO", "BAOCAO_DOANHTHU")))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("bi-speedometer2"))));
    }

    @Test
    void khongCoQuyenTongQuanThiMoTabDauTienDuocPhep() throws Exception {
        mockMvc.perform(get("/admin").with(nguoiDung("NHAN_VIEN_KHO", "SANPHAM_XEM", "BAOCAO_DOANHTHU")))
                .andExpect(redirectedUrl("/admin/bao-cao/doanh-thu"));
        mockMvc.perform(get("/admin").with(nguoiDung("NHAN_VIEN_KHO", "PHANQUYEN_QUANLY", "KHO_YEUCAU_NHAP")))
                .andExpect(redirectedUrl("/admin/kho/yeu-cau-nhap"));
        mockMvc.perform(get("/admin").with(nguoiDung("NHAN_VIEN_KHO", "SANPHAM_XEM", "TONGQUAN_XEM")))
                .andExpect(status().isOk());
    }

    @Test
    @Transactional
    void baoCaoTonKhoCoCotNhapHangVaTinhTrangDanhMuc() throws Exception {
        taoSanPhamCanhBao();
        mockMvc.perform(get("/admin/bao-cao/ton-kho").with(user(QUAN_TRI)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(">Nhập hàng</th>")))
                .andExpect(content().string(not(containsString("<th class=\"so\">Cần nhập</th>"))))
                .andExpect(content().string(containsString("Tình trạng</th></tr>")));
        // Chỉ được xem báo cáo thì không có cột nhập hàng
        mockMvc.perform(get("/admin/bao-cao/ton-kho").with(nguoiDung("NHAN_VIEN_KHO", "BAOCAO_TONKHO")))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString(">Nhập hàng</th>"))))
                .andExpect(content().string(not(containsString("name=\"quayVe\""))));
    }

    @Test
    void guiQuyenKhongHopLeLenServerBiTuChoi() throws Exception {
        mockMvc.perform(post("/admin/phan-quyen").param("quyen", "999999").with(user(QUAN_TRI)).with(csrf()))
                .andExpect(redirectedUrl("/admin/phan-quyen"))
                .andExpect(flash().attribute("loi", containsString("không hợp lệ")));
    }

    @Test
    void dangNhapDuaVeDungKhuVuc() throws Exception {
        // Nhân viên kho mở tab đầu tiên theo quyền đang có trong CSDL
        String tabNhanVien = TabKhuQuanLy.tabDauTien(
                userDetailsService.loadUserByUsername(NHAN_VIEN_KHO).getAuthorities()).orElse("/admin");
        mockMvc.perform(formLogin("/dang-nhap").user(NHAN_VIEN_KHO).password("123456"))
                .andExpect(redirectedUrl(tabNhanVien));
        mockMvc.perform(formLogin("/dang-nhap").user(QUAN_TRI).password("123456"))
                .andExpect(redirectedUrl("/admin"));
        mockMvc.perform(formLogin("/dang-nhap").user(KHACH_HANG).password("123456"))
                .andExpect(redirectedUrl("/?dangnhap"));
    }

    @Test
    void chuaDangNhapBiChuyenVeTrangDangNhap() throws Exception {
        mockMvc.perform(get("/admin/san-pham"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dang-nhap"));
    }

    @Test
    void thaoTacGhiCungBiChanTheoQuyen() throws Exception {
        mockMvc.perform(post("/admin/san-pham/1/xoa").with(user(NHAN_VIEN_KHO)).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void urlQuanTriChuaKhaiBaoQuyenBiChanMacDinh() throws Exception {
        mockMvc.perform(get("/admin/chuc-nang-moi").with(user(QUAN_TRI))).andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/", "/so-sanh", "/san-pham/1"})
    void trangCongKhaiVaoDuocKhiChuaDangNhap(String url) throws Exception {
        mockMvc.perform(get(url))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("title=\"Đăng nhập\"")))
                .andExpect(content().string(containsString("id=\"cartCount\"")));
    }

    @Test
    void khachHangCoMenuTaiKhoanVaVaoDuocHoSo() throws Exception {
        mockMvc.perform(get("/").with(user(KHACH_HANG)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Tổng quan tài khoản")))
                .andExpect(content().string(not(containsString("Trang quản trị"))));
        mockMvc.perform(get("/tai-khoan").with(user(KHACH_HANG))).andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/", "/so-sanh", "/san-pham/1"})
    void quanTriVaNhanVienKhoCoLinkVeTrangQuanTri(String url) throws Exception {
        for (String email : List.of(QUAN_TRI, NHAN_VIEN_KHO)) {
            mockMvc.perform(get(url).with(user(email)))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("admin-link")))
                    .andExpect(content().string(not(containsString("Tổng quan tài khoản"))));
        }
    }

    @Test
    @Transactional
    void chiTietYeuCauNhapCoAnhSanPham() throws Exception {
        ChiTietYeuCauNhapHang chiTiet = chiTietYeuCauNhapRepository.findAll().get(0);
        mockMvc.perform(get("/admin/kho/yeu-cau-nhap/" + chiTiet.getYeuCau().getMaYeuCauNhap()).with(user(QUAN_TRI)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("src=\"" + chiTiet.getSanPham().getHinhAnh() + "\"")));
    }

    @Test
    @Transactional
    void chiTietDonHangCoAnhSanPham() throws Exception {
        ChiTietDonHang chiTiet = chiTietDonHangRepository.findAll().get(0);
        mockMvc.perform(get("/admin/kho/don-hang/" + chiTiet.getDonHang().getMaDonHang()).with(user(QUAN_TRI)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("src=\"" + chiTiet.getSanPham().getHinhAnh() + "\"")));
    }

    @Test
    void tongQuanHienBonSoLieuDoanhThu() throws Exception {
        mockMvc.perform(get("/admin").with(user(QUAN_TRI)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Doanh thu cả năm")))
                .andExpect(content().string(containsString("Doanh thu tháng hiện tại")))
                .andExpect(content().string(containsString("Doanh thu tháng cao nhất")))
                .andExpect(content().string(containsString("Doanh thu tháng thấp nhất")));
    }

    @Test
    void trangLoiDanLoiVeDungKhuVuc() throws Exception {
        mockMvc.perform(get("/403").with(user(QUAN_TRI)))
                .andExpect(content().string(containsString("Về trang quản trị")));
        mockMvc.perform(get("/403").with(user(NHAN_VIEN_KHO)))
                .andExpect(content().string(containsString("Về trang quản trị")));
        mockMvc.perform(get("/403").with(user(KHACH_HANG)))
                .andExpect(content().string(containsString("Về trang chủ")))
                .andExpect(content().string(not(containsString("Về trang quản trị"))));
    }

    @Test
    void dangXuatVeTrangChuKemThongBao() throws Exception {
        mockMvc.perform(post("/dang-xuat").with(user(KHACH_HANG)).with(csrf()))
                .andExpect(redirectedUrl("/?dangxuat"));
    }
}
