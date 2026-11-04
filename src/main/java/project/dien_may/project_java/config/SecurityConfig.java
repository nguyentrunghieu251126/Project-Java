package project.dien_may.project_java.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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

@Configuration
public class SecurityConfig {

    private static final String ROLE_QUAN_TRI = "ROLE_QUAN_TRI";

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, UserDetailsService userDetailsService)
            throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        // 1. Trang công khai: ai cũng xem được
                        .requestMatchers("/", "/san-pham/**", "/danh-muc/**", "/tim-kiem",
                                "/dang-ky", "/dang-nhap", "/quen-mat-khau", "/error", "/403",
                                "/css/**", "/js/**", "/images/**")
                        .permitAll()
                        // 2. Khu quản trị (NFR05): phải là quản trị viên VÀ có đúng quyền chức năng.
                        // Mã quyền lấy từ bảng Quyen, gán theo vai trò ở màn hình phân quyền (QT16).
                        .requestMatchers("/admin").hasRole("QUAN_TRI")
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

                        .requestMatchers("/admin/kho/don-hang", "/admin/kho/don-hang/*")
                        .access(quyen("DONHANG_XEM_TATCA"))
                        .requestMatchers("/admin/kho/ton-kho").access(quyen("KHO_XEM_TONKHO"))
                        .requestMatchers("/admin/kho/yeu-cau-nhap", "/admin/kho/yeu-cau-nhap/*",
                                "/admin/kho/yeu-cau-nhap/*/duyet", "/admin/kho/yeu-cau-nhap/*/tu-choi")
                        .access(quyen("KHO_DUYET_NHAP"))

                        .requestMatchers("/admin/tai-khoan").access(quyen("TAIKHOAN_XEM"))
                        .requestMatchers("/admin/tai-khoan/them").access(quyen("TAIKHOAN_THEM"))
                        .requestMatchers("/admin/tai-khoan/*/sua", "/admin/tai-khoan/*/khoa")
                        .access(quyen("TAIKHOAN_SUA"))
                        .requestMatchers("/admin/tai-khoan/*/xoa").access(quyen("TAIKHOAN_XOA"))

                        .requestMatchers("/admin/phan-quyen").access(quyen("PHANQUYEN_QUANLY"))
                        // URL quản trị chưa khai báo quyền ở trên thì chặn hết, tránh quên bảo vệ
                        .requestMatchers("/admin/**").denyAll()
                        // 3. Chỉ nhân viên kho
                        .requestMatchers("/kho/**").hasRole("NHAN_VIEN_KHO")
                        // 4. Chỉ khách hàng đã đăng nhập
                        .requestMatchers("/gio-hang/**", "/dat-hang/**", "/don-hang/**", "/tai-khoan/**")
                        .hasRole("KHACH_HANG")
                        // 5. Các trang còn lại: phải đăng nhập
                        .anyRequest().authenticated())

                .formLogin(form -> form
                        .loginPage("/dang-nhap")
                        .loginProcessingUrl("/dang-nhap")
                        .usernameParameter("username")
                        .passwordParameter("password")
                        // Quản trị viên vào thẳng khu quản trị, các vai trò khác về trang chủ
                        .successHandler((request, response, authentication) -> {
                            boolean laQuanTri = authentication.getAuthorities().stream()
                                    .anyMatch(a -> ROLE_QUAN_TRI.equals(a.getAuthority()));
                            response.sendRedirect(request.getContextPath() + (laQuanTri ? "/admin" : "/"));
                        })
                        .failureUrl("/dang-nhap?error")
                        .permitAll()
                )

                .logout(logout -> logout
                        .logoutUrl("/dang-xuat")
                        .logoutSuccessUrl("/dang-nhap?logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )

                .exceptionHandling(ex -> ex.accessDeniedPage("/403"))
                .addFilterBefore(new DongBoTaiKhoanFilter(userDetailsService), AuthorizationFilter.class);
        return http.build();
    }

    private static AuthorizationManager<RequestAuthorizationContext> quyen(String maChucNang) {
        return AuthorizationManagers.allOf(
                AuthorityAuthorizationManager.hasAuthority(ROLE_QUAN_TRI),
                AuthorityAuthorizationManager.hasAuthority(maChucNang));
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(); // mật khẩu trong DB đã mã hóa BCrypt (NFR04)
    }
}
