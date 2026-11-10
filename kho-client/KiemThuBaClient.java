import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * Chương trình demo NFR03: chạy 3 client cùng lúc tới socket server, trong đó client C3 bị ngắt đột ngột
 * (mô phỏng mất kết nối). Sau đó mở thêm một client kiểm tra để chứng minh server vẫn phục vụ bình thường.
 *
 * Chạy:  java KiemThuBaClient.java [máy chủ] [cổng]      (mặc định localhost 9090)
 */
public class KiemThuBaClient {

    private static final PrintStream HIEN_THI = new PrintStream(System.out, true, StandardCharsets.UTF_8);
    private static final DateTimeFormatter GIO = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    public static void main(String[] args) throws InterruptedException {
        String maychu = args.length > 0 ? args[0] : "localhost";
        int cong = args.length > 1 ? Integer.parseInt(args[1]) : 9090;

        CountDownLatch batDau = new CountDownLatch(1);
        List<Thread> cacLuong = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            final int so = i;
            final boolean matKetNoi = i == 3;
            Thread luong = new Thread(() -> {
                try {
                    batDau.await(); // cả 3 client cùng bắt đầu một lúc
                    chayClient("C" + so, maychu, cong, matKetNoi);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }, "client-" + so);
            cacLuong.add(luong);
            luong.start();
        }
        batDau.countDown();
        for (Thread luong : cacLuong) {
            luong.join();
        }

        log("KIEMTRA", "3 client đã xong, mở client mới để kiểm tra server còn sống");
        chayClient("KIEMTRA", maychu, cong, false);
    }

    private static void chayClient(String ten, String maychu, int cong, boolean matKetNoi) {
        try (Socket socket = new Socket(maychu, cong);
             BufferedReader tuServer = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter denServer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true)) {
            socket.setSoTimeout(15_000);
            log(ten, "đã kết nối");
            docPhanHoi(ten, tuServer);

            if (matKetNoi) {
                // Gửi lệnh rồi đóng đột ngột bằng RST, không đọc phản hồi, như client bị tắt máy hay rớt mạng.
                denServer.println("TONKHO");
                socket.setSoLinger(true, 0);
                log(ten, "ngắt kết nối đột ngột (mô phỏng mất kết nối)");
                return;
            }
            for (String lenh : new String[]{"TONKHO", "SAPHET", "TONKHO KHONG-TON-TAI", "QUIT"}) {
                log(ten, "gửi: " + lenh);
                denServer.println(lenh);
                docPhanHoi(ten, tuServer);
            }
        } catch (IOException e) {
            log(ten, "LỖI: " + e.getMessage());
        }
    }

    private static void docPhanHoi(String ten, BufferedReader tuServer) throws IOException {
        String dau = tuServer.readLine();
        if (dau == null) {
            log(ten, "server đã đóng kết nối");
            return;
        }
        if (!dau.startsWith("OK ")) {
            log(ten, "nhận: " + dau);
            return;
        }
        int soDong = Integer.parseInt(dau.substring(3).trim());
        log(ten, "nhận " + soDong + " dòng");
        for (int i = 0; i < soDong; i++) {
            String dong = tuServer.readLine();
            if (i < 3) { // chỉ in vài dòng đầu cho gọn
                log(ten, "  " + dong);
            }
        }
        if (soDong > 3) {
            log(ten, "  ... (" + (soDong - 3) + " dòng nữa)");
        }
    }

    private static synchronized void log(String ten, String noiDung) {
        HIEN_THI.println(LocalTime.now().format(GIO) + " [" + Thread.currentThread().getName() + "] " + ten + ": " + noiDung);
    }
}
