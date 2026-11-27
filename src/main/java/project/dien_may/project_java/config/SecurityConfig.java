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
        private static final String[] VAI_TRO_KHU_QUAN_LY = { "QUAN_TRI", "NHAN_VIEN_KHO" };
        private static final Set<String> ROLE_KHU_QUAN_LY = Set.of("ROLE_QUAN_TRI", "ROLE_NHAN_VIEN_KHO");

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http, UserDetailsService userDetailsService)
                        throws Exception {
                http
                                .authorizeHttpRequests(auth -> auth
                                                .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                                                .requestMatchers("/", "/san-pham/**", "/danh-muc/**", "/tim-kiem",
                                                                "/so-sanh", "/dang-ky", "/dang-nhap", "/quen-mat-khau",
                                                                "/error", "/403",
                                                                "/css/**", "/js/**", "/images/**")
                                                .permitAll()
                                                .requestMatchers("/admin").hasAnyRole(VAI_TRO_KHU_QUAN_LY)
                                                .requestMatchers("/admin/bao-cao/doanh-thu")
                                                .access(quyen("BAOCAO_DOANHTHU"))
                                                .requestMatchers("/admin/bao-cao/ban-chay")
                                                .access(quyen("BAOCAO_BANCHAY"))
                                                .requestMatchers("/admin/bao-cao/ton-kho")
                                                .access(quyen("BAOCAO_TONKHO"))
                                                .requestMatchers("/admin/kho/kiem-thu/**")
                                                .access(quyen("KHO_XEM_TONKHO"))       
                                                .requestMatchers("/admin/san-pham").access(quyen("SANPHAM_XEM"))
                                                .requestMatchers("/admin/san-pham/them").access(quyen("SANPHAM_THEM"))
                                                .requestMatchers("/admin/san-pham/*/sua").access(quyen("SANPHAM_SUA"))
                                                .requestMatchers("/admin/san-pham/*/xoa").access(quyen("SANPHAM_XOA"))
                                                .requestMatchers("/admin/khuyen-mai").access(quyen("KHUYENMAI_XEM"))
                                                .requestMatchers("/admin/khuyen-mai/them")
                                                .access(quyen("KHUYENMAI_THEM"))
                                                .requestMatchers("/admin/khuyen-mai/*/sua")
                                                .access(quyen("KHUYENMAI_SUA"))
                                                .requestMatchers("/admin/khuyen-mai/*/xoa")
                                                .access(quyen("KHUYENMAI_XOA"))
                                                .requestMatchers("/admin/khuyen-mai/*/bat-tat")
                                                .access(quyen("KHUYENMAI_BATTAT"))
                                                .requestMatchers(HttpMethod.GET, "/admin/kho/don-hang",
                                                                "/admin/kho/don-hang/*")
                                                .access(quyen("DONHANG_XEM_TATCA", "DONHANG_XULY"))
                                                .requestMatchers(HttpMethod.POST, "/admin/kho/don-hang/*/xac-nhan",
                                                                "/admin/kho/don-hang/*/giao-hang",
                                                                "/admin/kho/don-hang/*/da-giao")
                                                .access(quyen("DONHANG_XULY"))
                                                .requestMatchers(HttpMethod.POST, "/admin/kho/don-hang/*/xuat-kho")
                                                .access(quyen("KHO_XUAT_HANG"))
                                                .requestMatchers("/admin/kho/ton-kho").access(quyen("KHO_XEM_TONKHO"))
                                                .requestMatchers(HttpMethod.POST, "/admin/kho/ton-kho/*/yeu-cau-nhap")
                                                .access(quyen("KHO_YEUCAU_NHAP"))
                                                .requestMatchers(HttpMethod.POST, "/admin/kho/yeu-cau-nhap/*/duyet",
                                                                "/admin/kho/yeu-cau-nhap/*/tu-choi")
                                                .access(quyen("KHO_DUYET_NHAP"))
                                                .requestMatchers(HttpMethod.POST,
                                                                "/admin/kho/yeu-cau-nhap/*/lap-phieu-nhap")
                                                .access(quyen("KHO_NHAP_HANG"))
                                                .requestMatchers(HttpMethod.GET, "/admin/kho/yeu-cau-nhap",
                                                                "/admin/kho/yeu-cau-nhap/*")
                                                .access(quyen("KHO_DUYET_NHAP", "KHO_YEUCAU_NHAP"))
                                                .requestMatchers("/admin/kho/bao-hanh", "/admin/kho/bao-hanh/**")
                                                .access(quyen("BAOHANH_XULY"))
                                                .requestMatchers("/admin/kho/doi-tra", "/admin/kho/doi-tra/**")
                                                .access(quyen("DOITRA_XULY"))
                                                .requestMatchers("/admin/tai-khoan").access(quyen("TAIKHOAN_XEM"))
                                                .requestMatchers("/admin/tai-khoan/them").access(quyen("TAIKHOAN_THEM"))
                                                .requestMatchers("/admin/tai-khoan/*/sua", "/admin/tai-khoan/*/khoa")
                                                .access(quyen("TAIKHOAN_SUA"))
                                                .requestMatchers("/admin/tai-khoan/*/xoa").access(quyen("TAIKHOAN_XOA"))

                                                .requestMatchers("/admin/phan-quyen", "/admin/phan-quyen/nhan-vien",
                                                                "/admin/phan-quyen/nhan-vien/*")
                                                .access(quyen("PHANQUYEN_QUANLY"))
                                                .requestMatchers("/admin/**").denyAll()
                                                .requestMatchers("/kho/**").hasRole("NHAN_VIEN_KHO")
                                                .requestMatchers("/gio-hang/**", "/dat-hang/**", "/don-hang/**",
                                                                "/tai-khoan", "/tai-khoan/**",
                                                                "/bao-hanh-doi-tra", "/bao-hanh-doi-tra/**")
                                                .hasRole("KHACH_HANG")
                                                .anyRequest().authenticated())

                                // Cấu hình security xử lý đăng nhập
                                .formLogin(form -> form
                                                .loginPage("/dang-nhap")
                                                .loginProcessingUrl("/dang-nhap")
                                                .usernameParameter("username")
                                                .passwordParameter("password")
                                                .successHandler((request, response, authentication) -> {
                                                        boolean vaoKhuQuanLy = authentication.getAuthorities().stream()
                                                                        .anyMatch(a -> ROLE_KHU_QUAN_LY
                                                                                        .contains(a.getAuthority()));
                                                        String dich = vaoKhuQuanLy
                                                                        ? TabKhuQuanLy.tabDauTien(
                                                                                        authentication.getAuthorities())
                                                                                        .orElse(TabKhuQuanLy.TONG_QUAN)
                                                                        : "/?dangnhap";
                                                        response.sendRedirect(request.getContextPath() + dich);
                                                })
                                                .failureUrl("/dang-nhap?error")
                                                .permitAll())

                                .logout(logout -> logout
                                                .logoutUrl("/dang-xuat")
                                                .logoutSuccessUrl("/?dangxuat")
                                                .invalidateHttpSession(true)
                                                .deleteCookies("JSESSIONID")
                                                .permitAll())

                                .exceptionHandling(ex -> ex.accessDeniedPage("/403"))
                                .addFilterBefore(new DongBoTaiKhoanFilter(userDetailsService),
                                                AuthorizationFilter.class);
                return http.build();
        }

        private static AuthorizationManager<RequestAuthorizationContext> quyen(String... maChucNang) {
                return AuthorizationManagers.allOf(
                                AuthorityAuthorizationManager.hasAnyRole(VAI_TRO_KHU_QUAN_LY),
                                AuthorityAuthorizationManager.hasAnyAuthority(maChucNang));
        }

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }
}
