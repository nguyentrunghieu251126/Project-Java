package project.dien_may.project_java.config;

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
                        // 1. Trang công khai: ai cũng xem được
                        .requestMatchers("/", "/san-pham/**", "/danh-muc/**", "/tim-kiem",
                                "/dang-ky", "/dang-nhap", "/error",
                                "/css/**", "/js/**", "/images/**")
                        .permitAll()
                        // 2. Chỉ quản trị viên
                        .requestMatchers("/admin/**").hasRole("QUAN_TRI")
                        // 3. Chỉ nhân viên kho
                        .requestMatchers("/kho/**").hasRole("NHAN_VIEN_KHO")
                        // 4. Chỉ khách hàng đã đăng nhập
                        .requestMatchers("/gio-hang/**", "/dat-hang/**", "/don-hang/**", "/tai-khoan/**")
                        .hasRole("KHACH_HANG")
                        // 5. Các trang còn lại: phải đăng nhập
                        .anyRequest().authenticated())
                .formLogin(form -> form.permitAll()) // tạm dùng trang đăng nhập mặc định
                .logout(logout -> logout.logoutSuccessUrl("/")); // đăng xuất xong quay về trang chủ
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(); // mật khẩu trong DB đã mã hóa BCrypt (NFR04)
    }
}