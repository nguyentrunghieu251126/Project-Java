package project.dien_may.project_java.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

/** TV3 – NFR10: lỗi CSDL ở phần mua hàng hiển thị trang lỗi thân thiện, không lộ SQL. */
@ControllerAdvice(assignableTypes = {GioHangController.class, DatHangController.class, DonHangController.class})
public class XuLyLoiMuaHangControllerAdvice {

    private static final Logger log = LoggerFactory.getLogger(XuLyLoiMuaHangControllerAdvice.class);

    @ExceptionHandler(DataAccessException.class)
    public ModelAndView loiDuLieu(DataAccessException e) {
        log.error("Loi du lieu phan mua hang (NFR10)", e);
        ModelAndView view = new ModelAndView("error/500");
        view.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        return view;
    }
}
