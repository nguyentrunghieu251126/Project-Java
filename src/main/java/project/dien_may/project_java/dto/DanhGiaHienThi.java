package project.dien_may.project_java.dto;

import java.time.LocalDateTime;

public record DanhGiaHienThi(
                String tenHienThi,
                Integer soSao,
                String noiDung,
                LocalDateTime ngayDanhGia) {
}
