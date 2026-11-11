package project.dien_may.project_java.dto.muahang;

import project.dien_may.project_java.entity.LoaiGiamGia;

import java.math.BigDecimal;
import java.time.LocalDate;

/** TV3 – KH15: một mã giảm giá hiển thị trong hộp chọn mã, kèm số tiền giảm tính trên đơn hiện tại. */
public record MaUuDai(
        String maGiamGia,
        String tenKhuyenMai,
        LoaiGiamGia loai,
        BigDecimal giaTriGiam,
        LocalDate ngayBatDau,
        LocalDate ngayKetThuc,
        Integer soLuotConLai,
        boolean dungDuoc,
        BigDecimal tienGiam,
        boolean totNhat
) {
    /** Mã đã dùng hết lượt (còn lại 0). */
    public boolean hetLuot() {
        return soLuotConLai != null && soLuotConLai <= 0;
    }

    /** Số ngày còn lại đến hết hạn (0 = hết hạn hôm nay). */
    public long soNgayConLai() {
        return java.time.temporal.ChronoUnit.DAYS.between(java.time.LocalDate.now(), ngayKetThuc);
    }

    public boolean sapHetHan() {
        long con = soNgayConLai();
        return con >= 0 && con <= 3;
    }

    public boolean phanTram() {
        return loai == LoaiGiamGia.PHAN_TRAM;
    }

    /** Chữ to trên cuống vé: "15%" hoặc "250K" / "1TR". */
    public String nhanNgan() {
        if (phanTram()) {
            return giaTriGiam.stripTrailingZeros().toPlainString() + "%";
        }
        long dong = giaTriGiam.longValue();
        if (dong >= 1_000_000 && dong % 100_000 == 0) {
            BigDecimal trieu = BigDecimal.valueOf(dong).divide(BigDecimal.valueOf(1_000_000));
            return trieu.stripTrailingZeros().toPlainString().replace('.', ',') + "TR";
        }
        if (dong >= 1_000 && dong % 1_000 == 0) {
            return (dong / 1_000) + "K";
        }
        return String.valueOf(dong);
    }
}
