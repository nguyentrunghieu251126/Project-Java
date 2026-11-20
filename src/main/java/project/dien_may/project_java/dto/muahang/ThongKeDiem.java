package project.dien_may.project_java.dto.muahang;

public record ThongKeDiem(long tongCong, long soLanCong, long tongTru, long soLanTru) {

    public long hienCo() {
        return tongCong - tongTru;
    }

    public long quyRaTien() {
        return hienCo() * QuyTacDiemThuong.GIA_TRI_1_DIEM;
    }

    public long soGiaoDich() {
        return soLanCong + soLanTru;
    }
}
