package project.dien_may.project_java;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TV5 - NFR03: 3 client chạy song song, một client mất kết nối đột ngột không làm ảnh hưởng client khác.
 * Dùng cổng 19090 để không đụng ứng dụng đang chạy ở cổng 9090. Cần SQL Server đang chạy.
 */
@SpringBootTest(properties = {"kho.socket.enabled=true", "kho.socket.port=19090"})
class TraCuuKhoSocketTests {

    private static final int CONG = 19090;

    /** Đọc một phản hồi "OK n" + n dòng; trả về danh sách các dòng nội dung, hoặc ném lỗi nếu server báo LOI. */
    private static List<String> docPhanHoi(BufferedReader in) throws IOException {
        String dau = in.readLine();
        assertTrue(dau != null && dau.startsWith("OK "), "Phản hồi phải bắt đầu bằng OK, nhận được: " + dau);
        int n = Integer.parseInt(dau.substring(3).trim());
        List<String> dong = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            dong.add(in.readLine());
        }
        return dong;
    }

    private static BufferedReader docCua(Socket s) throws IOException {
        return new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
    }

    private static PrintWriter ghiCua(Socket s) throws IOException {
        return new PrintWriter(new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8), true);
    }

    @Test
    void baClientCungLucVaMotClientMatKetNoi() throws Exception {
        CountDownLatch xuatPhat = new CountDownLatch(1);
        AtomicInteger clientThanhCong = new AtomicInteger();
        List<Thread> cacLuong = new ArrayList<>();

        for (int i = 1; i <= 3; i++) {
            final boolean matKetNoi = i == 3;
            Thread t = new Thread(() -> {
                try (Socket s = new Socket("localhost", CONG)) {
                    s.setSoTimeout(15_000);
                    BufferedReader in = docCua(s);
                    PrintWriter out = ghiCua(s);
                    docPhanHoi(in); // lời chào
                    xuatPhat.await();
                    if (matKetNoi) {
                        out.println("TONKHO");
                        s.setSoLinger(true, 0); // đóng bằng RST, không đọc phản hồi
                        return;
                    }
                    out.println("TONKHO");
                    docPhanHoi(in);
                    out.println("SAPHET");
                    docPhanHoi(in);
                    out.println("QUIT");
                    docPhanHoi(in);
                    clientThanhCong.incrementAndGet();
                } catch (Exception e) {
                    System.err.println("Client lỗi: " + e);
                }
            }, "test-client-" + i);
            cacLuong.add(t);
            t.start();
        }
        xuatPhat.countDown();
        for (Thread t : cacLuong) {
            t.join(30_000);
        }
        assertEquals(2, clientThanhCong.get(), "2 client bình thường phải hoàn tất dù client thứ 3 mất kết nối");

        // Server vẫn phục vụ client mới sau khi có client mất kết nối
        try (Socket s = new Socket("localhost", CONG)) {
            s.setSoTimeout(15_000);
            BufferedReader in = docCua(s);
            PrintWriter out = ghiCua(s);
            docPhanHoi(in);
            out.println("TONKHO");
            assertTrue(!docPhanHoi(in).isEmpty());
            out.println("QUIT");
            docPhanHoi(in);
        }
    }

    @Test
    void lenhSaiBiTuChoiNhungKhongDongKetNoi() throws Exception {
        try (Socket s = new Socket("localhost", CONG)) {
            s.setSoTimeout(15_000);
            BufferedReader in = docCua(s);
            PrintWriter out = ghiCua(s);
            docPhanHoi(in);
            out.println("XOAHET");
            String phanHoi = in.readLine();
            assertTrue(phanHoi.startsWith("LOI"), "Lệnh lạ phải nhận LOI, nhận được: " + phanHoi);
            out.println("QUIT"); // kết nối còn dùng được sau lệnh sai
            docPhanHoi(in);
        }
    }
}
