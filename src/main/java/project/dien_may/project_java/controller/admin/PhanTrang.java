package project.dien_may.project_java.controller.admin;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

final class PhanTrang {

    private static final int KICH_THUOC_TRANG = 10;

    private PhanTrang() {
    }

    /** @param trang số trang trên URL, tính từ 1 */
    static Pageable tao(int trang, Sort sapXep) {
        return PageRequest.of(Math.max(trang, 1) - 1, KICH_THUOC_TRANG, sapXep);
    }
}
