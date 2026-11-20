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

        // Đọc lại tài khoản của người dùng từ sql
        UserDetails taiKhoan = taiLai(hienTai.getName());
        if (taiKhoan == null || !taiKhoan.isEnabled()) {
            logoutHandler.logout(request, response, hienTai);
            response.sendRedirect(request.getContextPath() + "/dang-nhap?khoa");
            return;
        }

        // Cập nhật lại quyền mới nhất của tài khoản từ sql
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                taiKhoan, null, taiKhoan.getAuthorities()));
        SecurityContextHolder.setContext(context);
        chain.doFilter(request, response);
    }

    // Đọc lại dữ liệu
    private UserDetails taiLai(String email) {
        try {
            return userDetailsService.loadUserByUsername(email);
        } catch (UsernameNotFoundException e) {
            return null;
        }
    }
}
