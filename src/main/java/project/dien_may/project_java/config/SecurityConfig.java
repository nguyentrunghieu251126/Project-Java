package project.dien_may.project_java.config;

import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authorization.AuthorityAuthorizationManager;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationManagers;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

import java.util.Set;

@Configuration
public class SecurityConfig {

    /** Vai trò được vào khu quản lý; khách hàng không bao giờ vào được, kể cả khi lỡ được gán quyền. */
    private static final String[] VAI_TRO_KHU_QUAN_LY = {"QUAN_TRI", "NHAN_VIEN_KHO"};
    private static final Set<String> ROLE_KHU_QUAN_LY = Set.of("ROLE_QUAN_TRI", "ROLE_NHAN_VIEN_KHO");

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, UserDetailsService userDetailsService)
            throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        // NFR10: Cho phép Spring Boot hiển thị trang lỗi, không nới quyền URL thông thường.
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        // 1. Trang công khai: ai cũng xem được
                        .requestMatchers("/", "/san-pham/**", "/danh-muc/**", "/tim-kiem",
                                "/so-sanh", "/dang-ky", "/dang-nhap", "/quen-mat-khau", "/error", "/403",
                                "/css/**", "/js/**", "/images/**")
                        .permitAll()
                        // 2. Khu quản lý (NFR05): phải là quản trị viên hoặc nhân viên kho VÀ có đúng quyền chức năng.
                        // Mã quyền lấy từ bảng Quyen, gán theo vai trò ở màn hình phân quyền (QT16).
                        .requestMatchers("/admin").hasAnyRole(VAI_TRO_KHU_QUAN_LY)
                        .requestMatchers("/admin/bao-cao/doanh-thu").access(quyen("BAOCAO_DOANHTHU"))
                        .requestMatchers("/admin/bao-cao/ban-chay").access(quyen("BAOCAO_BANCHAY"))
                        .requestMatchers("/admin/bao-cao/ton-kho").access(quyen("BAOCAO_TONKHO"))

                        .requestMatchers("/admin/san-pham").access(quyen("SANPHAM_XEM"))
                        .requestMatchers("/admin/san-pham/them").access(quyen("SANPHAM_THEM"))
                        .requestMatchers("/admin/san-pham/*/sua").access(quyen("SANPHAM_SUA"))
                        .requestMatchers("/admin/san-pham/*/xoa").access(quyen("SANPHAM_XOA"))

                        .requestMatchers("/admin/khuyen-mai").access(quyen("KHUYENMAI_XEM"))
                        .requestMatchers("/admin/khuyen-mai/them").access(quyen("KHUYENMAI_THEM"))
                        .requestMatchers("/admin/khuyen-mai/*/sua").access(quyen("KHUYENMAI_SUA"))
                        .requestMatchers("/admin/khuyen-mai/*/xoa").access(quyen("KHUYENMAI_XOA"))
                        .requestMatchers("/admin/khuyen-mai/*/bat-tat").access(quyen("KHUYENMAI_BATTAT"))

                        // TV4: nhân viên kho có quyền DONHANG_XULY cũng xem được đơn để xử lý (NVK03 – NVK06)
                        .requestMatchers(HttpMethod.GET, "/admin/kho/don-hang", "/admin/kho/don-hang/*")
                        .access(quyen("DONHANG_XEM_TATCA", "DONHANG_XULY"))
                        .requestMatchers(HttpMethod.POST, "/admin/kho/don-hang/*/xac-nhan",
                                "/admin/kho/don-hang/*/giao-hang", "/admin/kho/don-hang/*/da-giao")
                        .access(quyen("DONHANG_XULY"))
                        .requestMatchers(HttpMethod.POST, "/admin/kho/don-hang/*/xuat-kho")
                        .access(quyen("KHO_XUAT_HANG"))
                        .requestMatchers("/admin/kho/ton-kho").access(quyen("KHO_XEM_TONKHO"))
                        .requestMatchers(HttpMethod.POST, "/admin/kho/ton-kho/*/yeu-cau-nhap")
                        .access(quyen("KHO_YEUCAU_NHAP"))
                        // Người gửi yêu cầu được xem, chỉ người có quyền duyệt mới duyệt hoặc từ chối được
                        .requestMatchers(HttpMethod.POST, "/admin/kho/yeu-cau-nhap/*/duyet",
                                "/admin/kho/yeu-cau-nhap/*/tu-choi")
                        .access(quyen("KHO_DUYET_NHAP"))
                        // TV4 (NVK10): nhân viên kho lập phiếu nhập sau khi yêu cầu được duyệt
                        .requestMatchers(HttpMethod.POST, "/admin/kho/yeu-cau-nhap/*/lap-phieu-nhap")
                        .access(quyen("KHO_NHAP_HANG"))
                        .requestMatchers(HttpMethod.GET, "/admin/kho/yeu-cau-nhap", "/admin/kho/yeu-cau-nhap/*")
                        .access(quyen("KHO_DUYET_NHAP", "KHO_YEUCAU_NHAP"))

                        // TV4: bảo hành (NVK11, NVK13) và đổi trả (NVK12, NVK14)
                        .requestMatchers("/admin/kho/bao-hanh", "/admin/kho/bao-hanh/**").access(quyen("BAOHANH_XULY"))
                        .requestMatchers("/admin/kho/doi-tra", "/admin/kho/doi-tra/**").access(quyen("DOITRA_XULY"))

                        .requestMatchers("/admin/tai-khoan").access(quyen("TAIKHOAN_XEM"))
                        .requestMatchers("/admin/tai-khoan/them").access(quyen("TAIKHOAN_THEM"))
                        .requestMatchers("/admin/tai-khoan/*/sua", "/admin/tai-khoan/*/khoa")
                        .access(quyen("TAIKHOAN_SUA"))
                        .requestMatchers("/admin/tai-khoan/*/xoa").access(quyen("TAIKHOAN_XOA"))

                        .requestMatchers("/admin/phan-quyen", "/admin/phan-quyen/nhan-vien",
                                "/admin/phan-quyen/nhan-vien/*")
                        .access(quyen("PHANQUYEN_QUANLY"))
                        // URL khu quản lý chưa khai báo quyền ở trên thì chặn hết, tránh quên bảo vệ
                        .requestMatchers("/admin/**").denyAll()
                        // 3. Chỉ nhân viên kho
                        .requestMatchers("/kho/**").hasRole("NHAN_VIEN_KHO")
                        // 4. Chỉ khách hàng đã đăng nhập
                        .requestMatchers("/gio-hang/**", "/dat-hang/**", "/don-hang/**", "/tai-khoan", "/tai-khoan/**",
                                "/bao-hanh-doi-tra", "/bao-hanh-doi-tra/**")
                        .hasRole("KHACH_HANG")
                        // 5. Các trang còn lại: phải đăng nhập
                        .anyRequest().authenticated())

                .formLogin(form -> form
                        .loginPage("/dang-nhap")
                        .loginProcessingUrl("/dang-nhap")
                        .usernameParameter("username")
                        .passwordParameter("password")
                        // Quản trị viên và nhân viên kho mở thẳng tab đầu tiên được phép trong khu quản lý
                        // (chưa được cấp tab nào thì về trang chào ở /admin); khách hàng về trang chủ kèm thông báo
                        .successHandler((request, response, authentication) -> {
                            boolean vaoKhuQuanLy = authentication.getAuthorities().stream()
                                    .anyMatch(a -> ROLE_KHU_QUAN_LY.contains(a.getAuthority()));
                            String dich = vaoKhuQuanLy
                                    ? TabKhuQuanLy.tabDauTien(authentication.getAuthorities()).orElse(TabKhuQuanLy.TONG_QUAN)
                                    : "/?dangnhap";
                            response.sendRedirect(request.getContextPath() + dich);
                        })
                        .failureUrl("/dang-nhap?error")
                        .permitAll()
                )

                .logout(logout -> logout
                        .logoutUrl("/dang-xuat")
                        .logoutSuccessUrl("/?dangxuat")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )

                .exceptionHandling(ex -> ex.accessDeniedPage("/403"))
                .addFilterBefore(new DongBoTaiKhoanFilter(userDetailsService), AuthorizationFilter.class);
        return http.build();
    }

    /** Thuộc vai trò khu quản lý VÀ có ít nhất một trong các mã chức năng. */
    private static AuthorizationManager<RequestAuthorizationContext> quyen(String... maChucNang) {
        return AuthorizationManagers.allOf(
                AuthorityAuthorizationManager.hasAnyRole(VAI_TRO_KHU_QUAN_LY),
                AuthorityAuthorizationManager.hasAnyAuthority(maChucNang));
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(); // mật khẩu trong DB đã mã hóa BCrypt (NFR04)
    }
}
