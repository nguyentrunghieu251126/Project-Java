package project.dien_may.project_java.dto.muahang;

import project.dien_may.project_java.entity.PhuongThucThanhToan;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class UuDaiThanhToan {
    public record DieuKien(String moTa, long donToiThieu, boolean chiLaptop, long tienGiam, int phanTram, long toiDa) {

        BigDecimal tinh(BigDecimal tamTinh, BigDecimal tienLaptop) {
            BigDecimal coSo = chiLaptop ? tienLaptop : tamTinh;
            if (coSo.signum() <= 0 || coSo.compareTo(BigDecimal.valueOf(donToiThieu)) < 0) {
                return BigDecimal.ZERO;
            }
            if (phanTram > 0) {
                BigDecimal giam = coSo.multiply(BigDecimal.valueOf(phanTram)).divide(BigDecimal.valueOf(100), 0,
                        RoundingMode.HALF_UP);
                return giam.min(BigDecimal.valueOf(toiDa));
            }
            return BigDecimal.valueOf(tienGiam).min(tamTinh);
        }
    }

    public record KetQua(DieuKien dieuKien, BigDecimal tienGiam) {
        public static final KetQua KHONG = new KetQua(null, BigDecimal.ZERO);

        public boolean coGiam() {
            return tienGiam.signum() > 0;
        }
    }

    private static final Map<PhuongThucThanhToan, List<DieuKien>> DIEU_KIEN = Map.of(
            PhuongThucThanhToan.ZALOPAY, List.of(
                    new DieuKien("Laptop từ 15 triệu giảm 150K", 15_000_000, true, 150_000, 0, 0),
                    new DieuKien("Đơn từ 6 triệu giảm 100K", 6_000_000, false, 100_000, 0, 0)),
            PhuongThucThanhToan.SACOMBANK, List.of(
                    new DieuKien("Đơn từ 10 triệu giảm 500K", 10_000_000, false, 500_000, 0, 0)),
            PhuongThucThanhToan.NAMA_JCB, List.of(
                    new DieuKien("Đơn từ 10 triệu giảm 1.000.000₫", 10_000_000, false, 1_000_000, 0, 0)),
            PhuongThucThanhToan.HOME_PAYLATER, List.of(
                    new DieuKien("Lần đầu giảm 20% (tối đa 50K)", 0, false, 0, 20, 50_000),
                    new DieuKien("Đơn từ 700K giảm 5% (tối đa 200K)", 700_000, false, 0, 5, 200_000)),
            PhuongThucThanhToan.KREDIVO, List.of(
                    new DieuKien("Lần đầu giảm 50% (tối đa 100K)", 0, false, 0, 50, 100_000),
                    new DieuKien("Đơn từ 700K giảm 5% (tối đa 200K)", 700_000, false, 0, 5, 200_000)));

    private UuDaiThanhToan() {
    }

    public static List<DieuKien> dieuKien(PhuongThucThanhToan pt) {
        return DIEU_KIEN.getOrDefault(pt, List.of());
    }

    public static KetQua tinh(PhuongThucThanhToan pt, BigDecimal tamTinh, BigDecimal tienLaptop) {
        return dieuKien(pt).stream()
                .map(dk -> new KetQua(dk, dk.tinh(tamTinh, tienLaptop)))
                .filter(KetQua::coGiam)
                .max(Comparator.comparing(KetQua::tienGiam))
                .orElse(KetQua.KHONG);
    }
}
