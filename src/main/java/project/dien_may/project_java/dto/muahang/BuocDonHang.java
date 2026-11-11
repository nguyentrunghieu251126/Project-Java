package project.dien_may.project_java.dto.muahang;

import java.time.LocalDateTime;

/** TV3 – KH17: một bước trên thanh theo dõi đơn hàng. thoiGian null khi CSDL không lưu mốc đó. */
public record BuocDonHang(String ten, String icon, boolean xong, boolean hienTai, LocalDateTime thoiGian) {
}
