package project.dien_may.project_java.config;

import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        // NFR10: Cho phép Spring Boot hiển thị trang lỗi, không nới quyền URL thông thường.
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        // 1. Trang công khai: ai cũng xem được
                        .requestMatchers("/", "/san-pham/**", "/danh-muc/**", "/tim-kiem",
                                "/so-sanh", "/dang-ky", "/dang-nhap", "/quen-mat-khau", "/error",
                                "/css/**", "/js/**", "/images/**")
                        .permitAll()
                        // 2. Chỉ quản trị viên
                        .requestMatchers("/admin/**").hasRole("QUAN_TRI")
                        // 3. Chỉ nhân viên kho
                        .requestMatchers("/kho/**").hasRole("NHAN_VIEN_KHO")
                        // 4. Chỉ khách hàng đã đăng nhập
                        .requestMatchers("/gio-hang/**", "/dat-hang/**", "/don-hang/**", "/tai-khoan", "/tai-khoan/**")
                        .hasRole("KHACH_HANG")
                        // 5. Các trang còn lại: phải đăng nhập
                        .anyRequest().authenticated())

                .formLogin(form -> form
                        .loginPage("/dang-nhap")
                        .loginProcessingUrl("/dang-nhap")
                        .usernameParameter("username")
                        .passwordParameter("password")
                        .defaultSuccessUrl("/?dangnhap", true)
                        .failureUrl("/dang-nhap?error")
                        .permitAll()
                )


                .logout(logout -> logout
                        .logoutUrl("/dang-xuat")
                        .logoutSuccessUrl("/?dangxuat")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                );
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(); // mật khẩu trong DB đã mã hóa BCrypt (NFR04)
    }
}