package project.dien_may.project_java;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** NFR05: chặn truy cập URL khi không đủ quyền. Quyền đọc thật từ CSDL theo dữ liệu mẫu, chỉ gọi GET hoặc bị chặn trước khi xử lý. */
@SpringBootTest
@AutoConfigureMockMvc
class PhanQuyenUrlTests {

    private static final String QUAN_TRI = "admin1@dienmay.vn";
    private static final String NHAN_VIEN_KHO = "kho1@dienmay.vn";
    private static final String KHACH_HANG = "lan.bui7@gmail.com";

    @Autowired
    private MockMvc mockMvc;

    @ParameterizedTest
    @ValueSource(strings = {"/admin", "/admin/bao-cao/doanh-thu", "/admin/bao-cao/ban-chay", "/admin/bao-cao/ton-kho",
            "/admin/san-pham", "/admin/san-pham/them", "/admin/khuyen-mai", "/admin/kho/don-hang", "/admin/kho/ton-kho",
            "/admin/kho/yeu-cau-nhap", "/admin/tai-khoan", "/admin/phan-quyen"})
    void quanTriVaoDuocMoiTrang(String url) throws Exception {
        mockMvc.perform(get(url).with(user(QUAN_TRI))).andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/admin", "/admin/san-pham", "/admin/tai-khoan", "/admin/phan-quyen"})
    void nguoiKhongPhaiQuanTriBiChan(String url) throws Exception {
        mockMvc.perform(get(url).with(user(NHAN_VIEN_KHO))).andExpect(status().isForbidden());
        mockMvc.perform(get(url).with(user(KHACH_HANG))).andExpect(status().isForbidden());
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
    void quanTriCoLinkVeTrangQuanTri(String url) throws Exception {
        mockMvc.perform(get(url).with(user(QUAN_TRI)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("admin-link")))
                .andExpect(content().string(not(containsString("Tổng quan tài khoản"))));
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
        mockMvc.perform(get("/403").with(user(KHACH_HANG)))
                .andExpect(content().string(not(containsString("Về trang quản trị"))));
    }

    @Test
    void dangXuatVeTrangChuKemThongBao() throws Exception {
        mockMvc.perform(post("/dang-xuat").with(user(KHACH_HANG)).with(csrf()))
                .andExpect(redirectedUrl("/?dangxuat"));
    }
}
