package project.dien_may.project_java.controller;

import jakarta.persistence.PersistenceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;


@ControllerAdvice(assignableTypes = {
        HomeController.class,
        SanPhamController.class,
        SoSanhSanPhamController.class,
        DangKyController.class,
        DangNhapController.class,
        DoiMatKhauController.class,
        HoSoTaiKhoanController.class
})
public class XuLyLoiControllerAdvice {

    private static final Logger log = LoggerFactory.getLogger(XuLyLoiControllerAdvice.class);

    @ExceptionHandler({DataAccessException.class, PersistenceException.class})
    public ModelAndView xuLyLoiDuLieu(Exception exception) {
        // Ghi chi tiet tren server, khong bao gio day stack trace / SQL ra giao dien.
        log.error("Loi thao tac du lieu (NFR10)", exception);
        ModelAndView view = new ModelAndView("error/500");
        view.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        return view;
    }
}
