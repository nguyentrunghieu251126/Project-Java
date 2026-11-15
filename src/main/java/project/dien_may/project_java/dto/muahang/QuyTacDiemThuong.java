package project.dien_may.project_java.dto.muahang;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * TV3 – quy tắc điểm thưởng (theo Project-Website).
 * Tích: mỗi 100.000₫ thanh toán = 1 điểm. Đổi: 1 điểm = 1.000₫, dùng ít nhất 10 điểm,
 * giảm tối đa 50% giá trị đơn sau mã giảm giá.
 */
public final class QuyTacDiemThuong {

    public static final long SO_TIEN_1_DIEM = 100_000;
    public static final long GIA_TRI_1_DIEM = 1_000;
    public static final int DIEM_TOI_THIEU = 10;
    public static final int PHAN_TRAM_TOI_DA = 50;

    /** Mở đầu mô tả của hai bản ghi tạo lúc đặt hàng (dùng để nhận ra chúng ở trang chi tiết đơn). */
    public static final String MO_TA_DUNG = "Dùng điểm giảm giá cho đơn ";
    public static final String MO_TA_TICH = "Tích điểm từ đơn ";

    private QuyTacDiemThuong() {
    }

    /** Số điểm được dùng tối đa cho đơn (0 nếu chưa đủ điểm tối thiểu). */
    public static int diemToiDa(long diemHienCo, BigDecimal tienSauMaGiam) {
        if (diemHienCo < DIEM_TOI_THIEU || tienSauMaGiam == null || tienSauMaGiam.signum() <= 0) {
            return 0;
        }
        long theoDon = tienSauMaGiam.multiply(BigDecimal.valueOf(PHAN_TRAM_TOI_DA))
                .divide(BigDecimal.valueOf(100L * GIA_TRI_1_DIEM), 0, RoundingMode.DOWN).longValue();
        long toiDa = Math.min(diemHienCo, theoDon);
        return toiDa < DIEM_TOI_THIEU ? 0 : (int) toiDa;
    }

    /** Số điểm nhận được từ số tiền khách thực trả. */
    public static int diemNhanDuoc(BigDecimal tongThanhToan) {
        if (tongThanhToan == null || tongThanhToan.signum() <= 0) {
            return 0;
        }
        return tongThanhToan.divide(BigDecimal.valueOf(SO_TIEN_1_DIEM), 0, RoundingMode.DOWN).intValue();
    }

    public static BigDecimal tienGiam(int soDiem) {
        return BigDecimal.valueOf(soDiem * GIA_TRI_1_DIEM);
    }
}
