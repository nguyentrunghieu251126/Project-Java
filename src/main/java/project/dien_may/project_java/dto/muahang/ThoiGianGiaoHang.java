package project.dien_may.project_java.dto.muahang;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * TV3 – thời gian giao hàng dự kiến theo khoảng cách từ cửa hàng (123 Nguyễn Văn Linh, Q.7, TP.HCM)
 * đến tỉnh / thành nhận hàng: càng xa thì càng lâu.
 *
 * @param vung   tên vùng giao hàng, ví dụ "Miền Trung"
 * @param tuNgay số ngày nhanh nhất
 * @param denNgay số ngày chậm nhất
 */
public record ThoiGianGiaoHang(String vung, int tuNgay, int denNgay) {

    private static final ThoiGianGiaoHang NOI_THANH = new ThoiGianGiaoHang("Nội thành TP. Hồ Chí Minh", 1, 2);
    private static final ThoiGianGiaoHang KHONG_RO = new ThoiGianGiaoHang("Toàn quốc", 3, 5);

    /** Mỗi vùng xa dần cửa hàng (tỉnh / thành theo sắp xếp năm 2025, khớp TinhThanh). */
    private static final Map<ThoiGianGiaoHang, List<String>> VUNG = Map.of(
            NOI_THANH, List.of("TP. Hồ Chí Minh"),
            new ThoiGianGiaoHang("Lân cận TP. Hồ Chí Minh", 2, 3),
            List.of("Đồng Nai", "Tây Ninh", "Đồng Tháp", "Vĩnh Long"),
            new ThoiGianGiaoHang("Miền Tây và Lâm Đồng", 3, 4),
            List.of("Cần Thơ", "An Giang", "Cà Mau", "Lâm Đồng"),
            new ThoiGianGiaoHang("Nam Trung Bộ và Tây Nguyên", 4, 5),
            List.of("Khánh Hòa", "Đắk Lắk", "Gia Lai", "Quảng Ngãi"),
            new ThoiGianGiaoHang("Miền Trung", 5, 6),
            List.of("Đà Nẵng", "Huế", "Quảng Trị", "Hà Tĩnh", "Nghệ An", "Thanh Hóa"),
            new ThoiGianGiaoHang("Miền Bắc", 5, 7),
            List.of("Hà Nội", "Hải Phòng", "Bắc Ninh", "Hưng Yên", "Ninh Bình", "Quảng Ninh", "Phú Thọ", "Thái Nguyên"),
            new ThoiGianGiaoHang("Miền núi phía Bắc", 6, 8),
            List.of("Cao Bằng", "Điện Biên", "Lai Châu", "Lạng Sơn", "Lào Cai", "Sơn La", "Tuyên Quang"));

    private static final DateTimeFormatter NGAY = DateTimeFormatter.ofPattern("dd/MM");

    /** Theo tỉnh / thành (SoDiaChi.khuVuc). */
    public static ThoiGianGiaoHang theoKhuVuc(String khuVuc) {
        if (khuVuc == null || khuVuc.isBlank()) {
            return KHONG_RO;
        }
        String ten = khuVuc.trim();
        return VUNG.entrySet().stream()
                .filter(e -> e.getValue().stream().anyMatch(t -> t.equalsIgnoreCase(ten)))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(KHONG_RO);
    }

    /** Theo địa chỉ đầy đủ đã lưu trong đơn ("..., Tỉnh"): lấy phần sau dấu phẩy cuối cùng. */
    public static ThoiGianGiaoHang theoDiaChi(String diaChiDayDu) {
        if (diaChiDayDu == null) {
            return KHONG_RO;
        }
        int phay = diaChiDayDu.lastIndexOf(',');
        ThoiGianGiaoHang kq = theoKhuVuc(phay < 0 ? diaChiDayDu : diaChiDayDu.substring(phay + 1));
        // Địa chỉ cũ ghi "TP.HCM", "Hồ Chí Minh"…
        if (kq == KHONG_RO) {
            String thuong = diaChiDayDu.toLowerCase();
            if (thuong.contains("hồ chí minh") || thuong.contains("tp.hcm") || thuong.contains("tphcm")) {
                return NOI_THANH;
            }
        }
        return kq;
    }

    /** "2 – 3 ngày" hoặc "1 – 2 ngày". */
    public String soNgay() {
        return tuNgay + " – " + denNgay + " ngày";
    }

    /** Khoảng ngày nhận hàng tính từ ngày đặt, ví dụ "Thứ Năm 12/11 – Thứ Sáu 13/11". */
    public String khoangNgay(LocalDate ngayDat) {
        return thu(ngayDat.plusDays(tuNgay)) + " – " + thu(ngayDat.plusDays(denNgay));
    }

    public String khoangNgayTuHomNay() {
        return khoangNgay(LocalDate.now());
    }

    private static String thu(LocalDate ngay) {
        String thu = ngay.getDayOfWeek() == DayOfWeek.SUNDAY ? "Chủ nhật"
                : "Thứ " + new String[] {"Hai", "Ba", "Tư", "Năm", "Sáu", "Bảy"}[ngay.getDayOfWeek().getValue() - 1];
        return thu + " " + ngay.format(NGAY);
    }
}
