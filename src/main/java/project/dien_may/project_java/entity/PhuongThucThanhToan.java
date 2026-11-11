package project.dien_may.project_java.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Giá trị khớp ràng buộc CK_ThanhToan_PhuongThuc trong CSDL (xem CapNhat_ThanhToan_TV3.sql). */
@Getter
@RequiredArgsConstructor
public enum PhuongThucThanhToan {
    COD("Thanh toán khi nhận hàng (COD)", "bi-cash-coin", "mh-pay-cod", "Trả tiền mặt cho nhân viên giao hàng."),
    CHUYEN_KHOAN("Chuyển khoản ngân hàng", "bi-bank", "mh-pay-bank", "Chuyển khoản sau khi đặt, ghi mã đơn ở nội dung."),
    THE("Thẻ ATM / Visa / Mastercard", "bi-credit-card", "mh-pay-card", "Quẹt thẻ qua máy POS khi nhận hàng."),
    ZALOPAY("Ví ZaloPay / Tài khoản trả sau", "bi-wallet2", "mh-pay-zalo", "Mua trước – thanh toán sau qua ZaloPay."),
    SACOMBANK("Thẻ quốc tế Sacombank", "bi-credit-card-2-front", "mh-pay-sacom", "Visa / Mastercard do Sacombank phát hành."),
    NAMA_JCB("Thẻ tín dụng Nam A Bank JCB", "bi-credit-card-2-back", "mh-pay-nama", "Thẻ tín dụng JCB do Nam A Bank phát hành."),
    HOME_PAYLATER("Home PayLater", "bi-house-heart", "mh-pay-home", "Mua trước trả sau qua Home Credit."),
    KREDIVO("Kredivo", "bi-phone", "mh-pay-kredivo", "Mua trước trả sau, trả góp kỳ hạn 6 / 12 tháng.");

    private final String nhan;
    private final String icon;
    /** Lớp CSS màu logo ở trang thanh toán. */
    private final String logo;
    private final String moTa;
}
