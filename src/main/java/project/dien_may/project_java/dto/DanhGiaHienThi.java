package project.dien_may.project_java.dto;

import java.time.LocalDateTime;

/** Dữ liệu công khai tối thiểu: không đưa email, số điện thoại lên giao diện. */
public record DanhGiaHienThi(
        String tenHienThi,
        Integer soSao,
        String noiDung,
        LocalDateTime ngayDanhGia
) { }
