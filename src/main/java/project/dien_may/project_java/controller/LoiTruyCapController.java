package project.dien_may.project_java.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/** Trang báo không đủ quyền; nhận mọi phương thức vì Security forward nguyên request bị chặn tới đây. */
@Controller
public class LoiTruyCapController {

    @RequestMapping("/403")
    public String khongDuQuyen() {
        return "error/403";
    }
}
