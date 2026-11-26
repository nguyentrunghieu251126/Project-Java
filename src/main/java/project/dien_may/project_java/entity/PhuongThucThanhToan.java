package project.dien_may.project_java.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PhuongThucThanhToan {
    COD("Thanh toán khi nhận hàng (COD)", "bi-cash-coin", "mh-pay-cod", "Trả tiền mặt cho nhân viên giao hàng."),
    CHUYEN_KHOAN("Chuyển khoản ngân hàng", "bi-bank", "mh-pay-bank",
            "Quét mã QR bằng ứng dụng ngân hàng sau khi đặt hàng."),
    THE("Thẻ ATM / Visa / Mastercard", "bi-credit-card", "mh-pay-card", "Quét mã QR để thanh toán bằng thẻ sau khi đặt hàng."),
    ZALOPAY("Ví ZaloPay / Tài khoản trả sau", "bi-wallet2", "mh-pay-zalo", "Mua trước – thanh toán sau qua ZaloPay."),
    SACOMBANK("Thẻ quốc tế Sacombank", "bi-credit-card-2-front", "mh-pay-sacom",
            "Visa / Mastercard do Sacombank phát hành."),
    NAMA_JCB("Thẻ tín dụng Nam A Bank JCB", "bi-credit-card-2-back", "mh-pay-nama",
            "Thẻ tín dụng JCB do Nam A Bank phát hành."),
    HOME_PAYLATER("Home PayLater", "bi-house-heart", "mh-pay-home", "Mua trước trả sau qua Home Credit."),
    KREDIVO("Kredivo", "bi-phone", "mh-pay-kredivo", "Mua trước trả sau, trả góp kỳ hạn 6 / 12 tháng.");

    private final String nhan;
    private final String icon;
    private final String logo;
    private final String moTa;

    /** Mọi phương thức trừ COD đều thanh toán trước qua mã QR (mô phỏng). */
    public boolean isQuetQr() {
        return this != COD;
    }

    /** Ứng dụng khách dùng để quét mã QR. */
    public String getUngDungQuet() {
        return switch (this) {
            case COD -> "";
            case CHUYEN_KHOAN -> "ứng dụng ngân hàng hoặc ví điện tử";
            case THE -> "ứng dụng ngân hàng phát hành thẻ";
            case ZALOPAY -> "ứng dụng ZaloPay";
            case SACOMBANK -> "ứng dụng Sacombank Pay";
            case NAMA_JCB -> "ứng dụng Nam A Bank";
            case HOME_PAYLATER -> "ứng dụng Home Credit";
            case KREDIVO -> "ứng dụng Kredivo";
        };
    }
}
