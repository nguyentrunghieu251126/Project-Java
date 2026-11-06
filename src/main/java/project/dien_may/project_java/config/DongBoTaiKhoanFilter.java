package project.dien_may.project_java.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Nạp lại quyền từ CSDL ở mỗi request, vì quyền trong session chỉ được chụp lúc đăng nhập.
 * Nhờ vậy thay đổi ở màn hình phân quyền có hiệu lực ngay, và tài khoản bị khóa hoặc xóa
 * bị đăng xuất ngay thay vì dùng tiếp được đến khi hết phiên.
 *
 * Không khai báo @Component: nếu là bean, Spring Boot sẽ đăng ký thêm một lần ngoài chuỗi Security.
 */
public class DongBoTaiKhoanFilter extends OncePerRequestFilter {

    private final UserDetailsService userDetailsService;
    private final SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();

    public DongBoTaiKhoanFilter(UserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String duongDan = request.getServletPath();
        return duongDan.startsWith("/css/") || duongDan.startsWith("/js/") || duongDan.startsWith("/images/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication hienTai = SecurityContextHolder.getContext().getAuthentication();
        if (!(hienTai instanceof UsernamePasswordAuthenticationToken)) {
            chain.doFilter(request, response);
            return;
        }

        UserDetails taiKhoan = taiLai(hienTai.getName());
        if (taiKhoan == null || !taiKhoan.isEnabled()) {
            logoutHandler.logout(request, response, hienTai);
            response.sendRedirect(request.getContextPath() + "/dang-nhap?khoa");
            return;
        }

        // Chỉ thay trong request này, không lưu vào session: request sau lại đọc từ CSDL
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                taiKhoan, null, taiKhoan.getAuthorities()));
        SecurityContextHolder.setContext(context);
        chain.doFilter(request, response);
    }

    private UserDetails taiLai(String email) {
        try {
            return userDetailsService.loadUserByUsername(email);
        } catch (UsernameNotFoundException e) {
            return null;
        }
    }
}
