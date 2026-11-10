package project.dien_may.project_java.service;

import project.dien_may.project_java.service.admin.NghiepVuException;

/** Số lượng yêu cầu vượt quá tồn kho hiện có; message hiển thị được thẳng cho người dùng. */
public class TonKhoKhongDuException extends NghiepVuException {

    private final Integer maSanPham;
    private final int tonHienCo;
    private final int soLuongYeuCau;

    public TonKhoKhongDuException(Integer maSanPham, String tenSanPham, int tonHienCo, int soLuongYeuCau) {
        super("Sản phẩm " + tenSanPham + " chỉ còn " + tonHienCo + " sản phẩm, không đủ " + soLuongYeuCau + " theo yêu cầu.");
        this.maSanPham = maSanPham;
        this.tonHienCo = tonHienCo;
        this.soLuongYeuCau = soLuongYeuCau;
    }

    public Integer getMaSanPham() {
        return maSanPham;
    }

    public int getTonHienCo() {
        return tonHienCo;
    }

    public int getSoLuongYeuCau() {
        return soLuongYeuCau;
    }
}
