package project.dien_may.project_java.dto;

import project.dien_may.project_java.entity.SanPham;

/** Một dòng tồn kho trả về cho client của socket server; không giữ tham chiếu lazy nên dùng được ngoài transaction. */
public record DongTonKho(Integer maSanPham, String maModel, String tenSanPham, int soLuong, int mucTonToiThieu,
                         String tinhTrang) {

    public static DongTonKho tu(SanPham sanPham) {
        return new DongTonKho(sanPham.getMaSanPham(), sanPham.getMaModel(), sanPham.getTenSanPham(),
                sanPham.getSoLuong(), sanPham.getMucTonToiThieu(), sanPham.getTinhTrangTonKho().getNhan());
    }

    /** Dạng một dòng văn bản gửi qua socket. */
    public String dinhDang() {
        return "#" + maSanPham + " | " + maModel + " | " + tenSanPham
                + " | Tồn: " + soLuong + " | Mức tối thiểu: " + mucTonToiThieu + " | " + tinhTrang;
    }
}
