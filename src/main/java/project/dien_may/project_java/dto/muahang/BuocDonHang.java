package project.dien_may.project_java.dto.muahang;

import java.time.LocalDateTime;

public record BuocDonHang(String ten, String icon, boolean xong, boolean hienTai, LocalDateTime thoiGian) {
}
