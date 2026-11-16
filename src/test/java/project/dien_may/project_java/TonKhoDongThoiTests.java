package project.dien_may.project_java;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import project.dien_may.project_java.service.TonKhoService;
import project.dien_may.project_java.service.TonKhoKhongDuException;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@SpringBootTest
public class TonKhoDongThoiTests {

    @Autowired
    private TonKhoService service;

    @Test
    public void test50LuongMuaDongThoi() throws InterruptedException {
        int soLuongThread = 50;
        ExecutorService executor = Executors.newFixedThreadPool(soLuongThread);

        AtomicInteger thanhCong = new AtomicInteger(0);
        AtomicInteger thatBai = new AtomicInteger(0);

        System.out.println("BAT DAU KIEM THU 50 LUONG CUNG TRANH MUA SAN PHAM");

        for (int i = 1; i <= soLuongThread; i++) {
            final int clientId = i;
            executor.submit(() -> {
                String khachHang = "KhachHang-" + clientId;
                Thread.currentThread().setName(khachHang);
                try {
                    service.truTonKho(4L, 1);
                    thanhCong.incrementAndGet();
                    System.out.println("--> [" + khachHang + "] MUA THANH CONG!");
                } catch (TonKhoKhongDuException e) {
                    thatBai.incrementAndGet();
                    System.out.println("--> [" + khachHang + "] MUA THAT BAI: " + e.getMessage());
                } catch (Exception e) {
                    System.out.println("--> [" + khachHang + "] LOI: " + e.getMessage());
                }
            });
        }

        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        System.out.println("KET QUA KIEM THU 50 LUONG");
        System.out.println("Tong so giao dich thanh cong: " + thanhCong.get());
        System.out.println("Tong so giao dich that bai (het hang): " + thatBai.get());
        System.out.println("Ton kho cuoi cung con lai tren SQL: " + service.getTonKho(1L));
    }
}