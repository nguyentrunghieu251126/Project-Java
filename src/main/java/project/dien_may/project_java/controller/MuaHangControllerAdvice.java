package project.dien_may.project_java.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import project.dien_may.project_java.service.muahang.GioHangService;

@ControllerAdvice
public class MuaHangControllerAdvice {

    private static final Logger log = LoggerFactory.getLogger(MuaHangControllerAdvice.class);

    private final GioHangService gioHangService;

    public MuaHangControllerAdvice(GioHangService gioHangService) {
        this.gioHangService = gioHangService;
    }

    @ModelAttribute
    public void napSanMaCsrf(HttpServletRequest request) {
        Object token = request.getAttribute(CsrfToken.class.getName());
        if (token instanceof CsrfToken csrf) {
            csrf.getToken();
        }
    }

    @ModelAttribute("soLuongGioHang")
    public long soLuongGioHang() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken
                || auth.getAuthorities().stream().noneMatch(a -> "ROLE_KHACH_HANG".equals(a.getAuthority()))) {
            return 0;
        }
        try {
            return gioHangService.demSanPham(auth.getName());
        } catch (DataAccessException e) {
            log.warn("Khong dem duoc gio hang: {}", e.getMessage());
            return 0;
        }
    }
}