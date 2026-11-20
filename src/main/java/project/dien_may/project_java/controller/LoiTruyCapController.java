package project.dien_may.project_java.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class LoiTruyCapController {

    @RequestMapping("/403")
    public String khongDuQuyen() {
        return "error/403";
    }
}
