package project.dien_may.project_java;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import project.dien_may.project_java.entity.DanhMuc;
import project.dien_may.project_java.entity.SanPham;
import project.dien_may.project_java.repository.DanhMucRepository;
import project.dien_may.project_java.repository.SanPhamRepository;
import project.dien_may.project_java.service.TonKhoKhongDuException;
import project.dien_may.project_java.service.TonKhoService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * TV5 - NFR02: kiểm thử 50 luồng cùng đặt mua một sản phẩm.
 * <p>
 * Lớp này KHÔNG dùng @Transactional ở mức test: mỗi luồng phải tự mở và commit transaction riêng thì mới
 * tranh khóa thật trên SQL Server. Test tự tạo sản phẩm riêng (mã model TEST-DONGTHOI-...) và xóa khi xong,
 * không đụng đến dữ liệu mẫu. Cần SQL Server đang chạy và đã nạp QLCuaHangDienMay.sql.
 */
@SpringBootTest(properties = "kho.socket.enabled=false")
class TonKhoDongThoiTests {

    private static final int SO_LUONG_LUONG = 50;

    @Autowired
    private TonKhoService tonKhoService;

    @Autowired
    private SanPhamRepository sanPhamRepository;

    @Autowired
    private DanhMucRepository danhMucRepository;

    private final List<Integer> sanPhamDaTao = new ArrayList<>();

    @BeforeEach
    void kiemTraCoDanhMuc() {
        assumeTrue(!danhMucRepository.findAll().isEmpty(), "CSDL chưa có danh mục, hãy nạp QLCuaHangDienMay.sql");
    }

    @AfterEach
    void donDep() {
        sanPhamDaTao.forEach(sanPhamRepository::deleteById);
        sanPhamDaTao.clear();
    }

    private Integer taoSanPham(int tonBanDau) {
        DanhMuc danhMuc = danhMucRepository.findAll().get(0);
        SanPham sanPham = new SanPham();
        sanPham.setMaModel("TEST-DONGTHOI-" + System.nanoTime());
        sanPham.setTenSanPham("Sản phẩm kiểm thử đồng thời");
        sanPham.setThuongHieu("Kiểm thử");
        sanPham.setDonGia(BigDecimal.valueOf(1_000_000));
        sanPham.setSoLuong(tonBanDau);
        sanPham.setDanhMuc(danhMuc);
        Integer ma = sanPhamRepository.save(sanPham).getMaSanPham();
        sanPhamDaTao.add(ma);
        return ma;
    }

    private int tonHienTai(Integer maSanPham) {
        return sanPhamRepository.findById(maSanPham).orElseThrow().getSoLuong();
    }

    private record KetQua(int thanhCong, int hetHang, int loiKhac) {
    }

    /** Cho 50 luồng cùng bấm "mua" một lúc: mỗi luồng chạy một công việc, chờ cổng xuất phát rồi mới chạy. */
    private KetQua chay50Luong(Runnable... congViecMoiLuong) throws InterruptedException {
        AtomicInteger thanhCong = new AtomicInteger();
        AtomicInteger hetHang = new AtomicInteger();
        AtomicInteger loiKhac = new AtomicInteger();
        AtomicInteger dem = new AtomicInteger();
        CountDownLatch sanSang = new CountDownLatch(SO_LUONG_LUONG);
        CountDownLatch xuatPhat = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(SO_LUONG_LUONG,
                r -> new Thread(r, "khach-" + dem.incrementAndGet()));

        for (int i = 0; i < SO_LUONG_LUONG; i++) {
            Runnable congViec = congViecMoiLuong[i % congViecMoiLuong.length];
            pool.execute(() -> {
                sanSang.countDown();
                try {
                    xuatPhat.await();
                    congViec.run();
                    thanhCong.incrementAndGet();
                } catch (TonKhoKhongDuException e) {
                    hetHang.incrementAndGet();
                } catch (Throwable e) {
                    loiKhac.incrementAndGet();
                    System.err.println("[" + Thread.currentThread().getName() + "] Lỗi ngoài dự kiến: " + e);
                }
            });
        }
        sanSang.await();
        long batDau = System.nanoTime();
        xuatPhat.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(120, TimeUnit.SECONDS), "50 luồng không kết thúc trong 120 giây (nghi deadlock)");
        long ms = (System.nanoTime() - batDau) / 1_000_000;
        System.out.printf("[TonKhoDongThoiTests] %d luồng xong sau %d ms: %d thành công, %d hết hàng, %d lỗi khác%n",
                SO_LUONG_LUONG, ms, thanhCong.get(), hetHang.get(), loiKhac.get());
        return new KetQua(thanhCong.get(), hetHang.get(), loiKhac.get());
    }

    @Test
    void khongBanVuotTonKho() throws InterruptedException {
        Integer ma = taoSanPham(20);

        KetQua ketQua = chay50Luong(() -> tonKhoService.truTonKho(ma, 1));

        assertEquals(20, ketQua.thanhCong(), "Chỉ được bán đúng bằng số hàng đang có");
        assertEquals(30, ketQua.hetHang(), "30 luồng còn lại phải bị từ chối vì hết hàng");
        assertEquals(0, ketQua.loiKhac());
        assertEquals(0, tonHienTai(ma), "Tồn kho phải về đúng 0, không âm");
    }

    @Test
    void khongMatCapNhatKhiDuHang() throws InterruptedException {
        Integer ma = taoSanPham(100);

        KetQua ketQua = chay50Luong(() -> tonKhoService.truTonKho(ma, 1));

        assertEquals(50, ketQua.thanhCong());
        assertEquals(0, ketQua.hetHang());
        assertEquals(0, ketQua.loiKhac());
        assertEquals(50, tonHienTai(ma), "100 - 50 = 50; nếu có lần ghi đè nhau thì tồn sẽ lớn hơn 50");
    }

    @Test
    void muaHaiSanPhamTheoThuTuNguocNhauKhongDeadlock() throws InterruptedException {
        Integer a = taoSanPham(100);
        Integer b = taoSanPham(100);
        // Một nửa số luồng khai báo (A, B), nửa kia khai báo (B, A); truTonKho(Map) luôn khóa theo mã tăng dần.
        Runnable donAB = () -> tonKhoService.truTonKho(Map.of(a, 1, b, 1));
        Runnable donBA = () -> tonKhoService.truTonKho(Map.of(b, 1, a, 1));

        KetQua ketQua = chay50Luong(donAB, donBA);

        assertEquals(50, ketQua.thanhCong());
        assertEquals(0, ketQua.loiKhac());
        assertEquals(50, tonHienTai(a));
        assertEquals(50, tonHienTai(b));
    }

    @Test
    void thieuHangMotSanPhamThiHoanTacCaDon() {
        Integer du = taoSanPham(10);
        Integer thieu = taoSanPham(1);

        assertThrows(TonKhoKhongDuException.class,
                () -> tonKhoService.truTonKho(Map.of(du, 3, thieu, 5)));

        assertEquals(10, tonHienTai(du), "Sản phẩm đã trừ trước đó phải được hoàn lại khi đơn lỗi (NFR06)");
        assertEquals(1, tonHienTai(thieu));
    }
}
