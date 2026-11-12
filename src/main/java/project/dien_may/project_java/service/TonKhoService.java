package project.dien_may.project_java.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TonKhoService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public int getTonKho(Long maSanPham) {
        String sql = "SELECT SoLuong FROM SanPham WHERE MaSanPham = ?";
        try {
            Integer soLuong = jdbcTemplate.queryForObject(sql, Integer.class, maSanPham);
            return soLuong != null ? soLuong : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    @Transactional
    public void truTonKho(Long maSanPham, int soLuong) throws TonKhoKhongDuException {
        String threadName = Thread.currentThread().getName();
        System.out.println("[LUONG: " + threadName + "] Yeu cau mua SP: " + maSanPham + ", so lượng: " + soLuong);

        String sql = "UPDATE SanPham SET SoLuong = SoLuong - ? WHERE MaSanPham = ? AND SoLuong >= ?";

        int rowsAffected = jdbcTemplate.update(sql, soLuong, maSanPham, soLuong);

        if (rowsAffected == 0) {
            int tonHienTai = getTonKho(maSanPham);
            System.out.println("[LUONG: " + threadName + "] MUA THAT BAI: Khong du ton kho hoac khong tim thay SP!");
            throw new TonKhoKhongDuException("Ton kho khong du! Hien con lai: " + tonHienTai + ", Yeu cau mua: " + soLuong);
        }

        System.out.println("[LUONG: " + threadName + "] MUA THANH CONG! Da tru " + soLuong + " cho SP ID=" + maSanPham);
    }
}