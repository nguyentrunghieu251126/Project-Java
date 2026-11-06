package project.dien_may.project_java.dto;

// KH09 - Mot lua chon khoang gia nhanh trong bo loc (Duoi 2 trieu, Tu 2-4 trieu...)
public class KhoangGiaNhanh {

    private final String nhan;
    private final Long giaTu;
    private final Long giaDen;
    private final String chuoiGia;
    private final boolean dangChon;

    public KhoangGiaNhanh(String nhan, Long giaTu, Long giaDen, boolean dangChon) {
        this.nhan = nhan;
        this.giaTu = giaTu;
        this.giaDen = giaDen;
        this.dangChon = dangChon;

        StringBuilder sb = new StringBuilder();
        if (giaTu != null) {
            sb.append("&giaTu=").append(giaTu);
        }
        if (giaDen != null) {
            sb.append("&giaDen=").append(giaDen);
        }
        this.chuoiGia = sb.toString();
    }

    public String getNhan() {
        return nhan;
    }

    public Long getGiaTu() {
        return giaTu;
    }

    public Long getGiaDen() {
        return giaDen;
    }

    public String getChuoiGia() {
        return chuoiGia;
    }

    public boolean isDangChon() {
        return dangChon;
    }
}
