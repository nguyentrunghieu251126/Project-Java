
package project.dien_may.project_java.service;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import project.dien_may.project_java.entity.TaiKhoan;
import project.dien_may.project_java.entity.VaiTro;
import project.dien_may.project_java.repository.TaiKhoanRepository;

import java.util.ArrayList;
import java.util.List;

@Service
public class TaiKhoanUserDetailsService implements UserDetailsService {

    private final TaiKhoanRepository taiKhoanRepository;

    public TaiKhoanUserDetailsService(
            TaiKhoanRepository taiKhoanRepository) {
        this.taiKhoanRepository = taiKhoanRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        TaiKhoan taiKhoan = taiKhoanRepository.findByEmailIgnoreCase(email).orElseThrow(() -> new UsernameNotFoundException("Email hoặc mật khẩu không đúng"));

        VaiTro vaiTro = taiKhoan.getVaiTro();

        // Vai trò dùng cho hasRole, mã chức năng trong bảng Quyen dùng cho hasAuthority
        List<SimpleGrantedAuthority> quyen = new ArrayList<>();
        quyen.add(new SimpleGrantedAuthority("ROLE_" + vaiTro.getTenVaiTro()));
        vaiTro.getDsQuyen().forEach(q -> quyen.add(new SimpleGrantedAuthority(q.getMaChucNang())));

        return User.builder().username(taiKhoan.getEmail()).password(taiKhoan.getMatKhau()).authorities(quyen).disabled(!taiKhoan.isTrangThai()).build();
    }
}
