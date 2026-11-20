package project.dien_may.project_java.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.dien_may.project_java.entity.SanPham;
import project.dien_may.project_java.repository.SanPhamRepository;

import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TonKhoService {

    @Autowired(required = false)
    private SanPhamRepository sanPhamRepository;

    private final ConcurrentHashMap<Integer, ReentrantLock> locks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, Integer> tonKhoTam = new ConcurrentHashMap<>();

    public TonKhoService() {
        tonKhoTam.put(1, 10);
        tonKhoTam.put(4, 50);
    }

    public int getTonKho(Integer maSanPham) {
        if (sanPhamRepository != null) {
            Optional<SanPham> sp = sanPhamRepository.findById(maSanPham);
            if (sp.isPresent()) {
                return sp.get().getSoLuong();
            }
        }
        return tonKhoTam.getOrDefault(maSanPham, 0);
    }

    @Transactional
    public void truTonKho(Integer maSanPham, int soLuong) throws TonKhoKhongDuException {
        String threadName = Thread.currentThread().getName();
        System.out
                .println("[LUONG: " + threadName + "] Bat dau tru kho SP: " + maSanPham + ", so luong mua: " + soLuong);

        ReentrantLock lock = locks.computeIfAbsent(maSanPham, k -> new ReentrantLock());
        lock.lock();
        try {
            if (sanPhamRepository == null) {
                int hienTai = tonKhoTam.getOrDefault(maSanPham, 0);
                System.out.println("[LUONG: " + threadName + "] Ton kho hien tai trong bo nho cua SP " + maSanPham
                        + " la: " + hienTai);

                if (hienTai < soLuong) {
                    System.out.println("[LUONG: " + threadName + "] LOI: Khong du ton kho!");
                    throw new TonKhoKhongDuException("Ton kho khong du! Con lai: " + hienTai + ", Yeu cau: " + soLuong);
                }

                tonKhoTam.put(maSanPham, hienTai - soLuong);
                System.out.println("[LUONG: " + threadName + "] THANH CONG. Ton kho moi trong bo nho: "
                        + tonKhoTam.get(maSanPham));
                return;
            }

            SanPham sp = sanPhamRepository.findById(maSanPham)
                    .orElseThrow(() -> new RuntimeException("Khong tim thay san pham co ID: " + maSanPham));

            int hienTai = sp.getSoLuong();
            System.out.println(
                    "[LUONG: " + threadName + "] Ton kho hien tai tren SQL cua SP " + maSanPham + " la: " + hienTai);

            if (hienTai < soLuong) {
                System.out.println("[LUONG: " + threadName + "] LOI: Khong du ton kho!");
                throw new TonKhoKhongDuException("Ton kho khong du! Con lai: " + hienTai + ", Yeu cau: " + soLuong);
            }

            sp.setSoLuong(hienTai - soLuong);
            sanPhamRepository.save(sp);

            System.out.println("[LUONG: " + threadName + "] THANH CONG. Ton kho moi tren SQL: " + sp.getSoLuong());
        } finally {
            lock.unlock();
        }
    }
}