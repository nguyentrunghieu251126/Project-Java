package project.dien_may.project_java.dto.muahang;

import java.util.ArrayList;
import java.util.List;

/** TV3 – danh sách 34 tỉnh / thành phố (sau sắp xếp năm 2025), dùng cho ô "Tỉnh / Thành phố" của sổ địa chỉ. */
public final class TinhThanh {

    public static final List<String> THANH_PHO = List.of(
            "Hà Nội", "TP. Hồ Chí Minh", "Hải Phòng", "Đà Nẵng", "Huế", "Cần Thơ");

    public static final List<String> TINH = List.of(
            "An Giang", "Bắc Ninh", "Cà Mau", "Cao Bằng", "Đắk Lắk", "Điện Biên", "Đồng Nai",
            "Đồng Tháp", "Gia Lai", "Hà Tĩnh", "Hưng Yên", "Khánh Hòa", "Lai Châu", "Lâm Đồng",
            "Lạng Sơn", "Lào Cai", "Nghệ An", "Ninh Bình", "Phú Thọ", "Quảng Ngãi", "Quảng Ninh",
            "Quảng Trị", "Sơn La", "Tây Ninh", "Thái Nguyên", "Thanh Hóa", "Tuyên Quang", "Vĩnh Long");

    private TinhThanh() {
    }

    public static List<String> tatCa() {
        List<String> ketQua = new ArrayList<>(THANH_PHO);
        ketQua.addAll(TINH);
        return ketQua;
    }

    public static boolean coTrongDanhSach(String ten) {
        return ten != null && tatCa().stream().anyMatch(t -> t.equalsIgnoreCase(ten.trim()));
    }
}
