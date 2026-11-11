package project.dien_may.project_java.dto.muahang;

/**
 * TV3 – số liệu điểm thưởng của khách (trang Điểm thưởng / Lịch sử điểm).
 *
 * @param tongCong tổng điểm đã được cộng (tích điểm, hoàn điểm khi hủy đơn)
 * @param tongTru  tổng điểm đã trừ (dùng điểm, thu hồi khi hủy đơn)
 */
public record ThongKeDiem(long tongCong, long soLanCong, long tongTru, long soLanTru) {

    public long hienCo() {
        return tongCong - tongTru;
    }

    /** Số tiền tương đương với số điểm hiện có. */
    public long quyRaTien() {
        return hienCo() * QuyTacDiemThuong.GIA_TRI_1_DIEM;
    }

    public long soGiaoDich() {
        return soLanCong + soLanTru;
    }
}
