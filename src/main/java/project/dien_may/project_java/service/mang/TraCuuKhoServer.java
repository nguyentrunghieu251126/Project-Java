package project.dien_may.project_java.service.mang;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;
import project.dien_may.project_java.dto.DongTonKho;
import project.dien_may.project_java.service.TonKhoService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * TV5 - NFR03: socket server "Tra cứu kho" chạy cùng Spring Boot.
 * <p>
 * Giao thức văn bản, mã hóa UTF-8, mỗi lệnh một dòng:
 * <pre>
 *   TONKHO [mã]   không có mã: liệt kê toàn bộ; có mã: tra theo mã sản phẩm hoặc mã model
 *   SAPHET        liệt kê sản phẩm sắp hết hoặc hết hàng
 *   QUIT          ngắt kết nối
 * </pre>
 * Mỗi lệnh được trả lời bằng "OK n" rồi đúng n dòng nội dung, hoặc bằng một dòng "LOI nội dung lỗi".
 * <p>
 * Mỗi client do một luồng riêng trong thread pool phục vụ, nên client chậm hoặc mất kết nối không làm treo
 * client khác. Tên luồng (kho-client-N) được ghi vào log để minh họa xử lý song song.
 */
@Component
@ConditionalOnProperty(name = "kho.socket.enabled", havingValue = "true", matchIfMissing = true)
public class TraCuuKhoServer implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(TraCuuKhoServer.class);

    private final TonKhoService tonKhoService;
    private final int port;
    private final int soLuongToiDa;
    private final int thoiGianChoMs;

    private final Set<Socket> ketNoiDangMo = ConcurrentHashMap.newKeySet();
    private volatile boolean dangChay;
    private ServerSocket serverSocket;
    private ThreadPoolExecutor threadPool;
    private Thread luongChapNhan;

    public TraCuuKhoServer(TonKhoService tonKhoService,
                           @Value("${kho.socket.port:9090}") int port,
                           @Value("${kho.socket.pool-size:10}") int soLuongToiDa,
                           @Value("${kho.socket.idle-timeout-ms:300000}") int thoiGianChoMs) {
        this.tonKhoService = tonKhoService;
        this.port = port;
        this.soLuongToiDa = soLuongToiDa;
        this.thoiGianChoMs = thoiGianChoMs;
    }

    @Override
    public synchronized void start() {
        if (dangChay) {
            return;
        }
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            // Cổng bị chiếm không được làm hỏng cả website, chỉ ghi log để người chạy đổi cổng.
            log.error("Không mở được socket server Tra cứu kho ở cổng {}: {}", port, e.getMessage());
            return;
        }
        AtomicInteger dem = new AtomicInteger();
        ThreadFactory tenLuong = r -> new Thread(r, "kho-client-" + dem.incrementAndGet());
        // SynchronousQueue + từ chối khi đủ luồng: client thừa nhận ngay câu báo bận thay vì chờ vô hạn.
        threadPool = new ThreadPoolExecutor(soLuongToiDa, soLuongToiDa, 0L, TimeUnit.MILLISECONDS,
                new SynchronousQueue<>(), tenLuong, (r, executor) -> ((PhucVuClient) r).tuChoiViBan());
        dangChay = true;
        luongChapNhan = new Thread(this::vongLapChapNhan, "kho-accept");
        luongChapNhan.start();
        log.info("Socket server Tra cứu kho đang lắng nghe ở cổng {} (tối đa {} client cùng lúc)", port, soLuongToiDa);
    }

    @Override
    public synchronized void stop() {
        if (!dangChay) {
            return;
        }
        dangChay = false;
        dongYenLang(serverSocket);
        // Luồng đang chờ readLine() không bị interrupt được, phải đóng socket của từng client.
        ketNoiDangMo.forEach(TraCuuKhoServer::dongYenLang);
        threadPool.shutdownNow();
        log.info("Đã dừng socket server Tra cứu kho");
    }

    @Override
    public boolean isRunning() {
        return dangChay;
    }

    private void vongLapChapNhan() {
        while (dangChay) {
            try {
                Socket socket = serverSocket.accept();
                threadPool.execute(new PhucVuClient(socket));
            } catch (IOException e) {
                if (dangChay) {
                    log.warn("Lỗi khi nhận kết nối mới: {}", e.getMessage());
                }
            }
        }
    }

    private static void dongYenLang(java.io.Closeable doiTuong) {
        try {
            if (doiTuong != null) {
                doiTuong.close();
            }
        } catch (IOException ignored) {
            // đóng để dọn tài nguyên, lỗi không cần xử lý
        }
    }

    /** Phục vụ trọn một client trên một luồng của thread pool. */
    private final class PhucVuClient implements Runnable {

        private final Socket socket;

        PhucVuClient(Socket socket) {
            this.socket = socket;
        }

        void tuChoiViBan() {
            try (socket; PrintWriter out = taoWriter(socket)) {
                out.println("LOI Server đang phục vụ tối đa " + soLuongToiDa + " client, vui lòng thử lại sau.");
                log.warn("[{}] Từ chối {}: đã đủ {} client", Thread.currentThread().getName(),
                        socket.getRemoteSocketAddress(), soLuongToiDa);
            } catch (IOException e) {
                // client đã đi mất, không còn gì để báo
            }
        }

        @Override
        public void run() {
            String luong = Thread.currentThread().getName();
            Object diaChi = socket.getRemoteSocketAddress();
            ketNoiDangMo.add(socket);
            log.info("[{}] Client kết nối: {}", luong, diaChi);
            try (socket;
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                 PrintWriter out = taoWriter(socket)) {
                socket.setSoTimeout(thoiGianChoMs);
                traLoi(out, List.of("Kết nối thành công. Lệnh: TONKHO [mã sản phẩm hoặc mã model], SAPHET, QUIT."));
                String dong;
                while (dangChay && (dong = in.readLine()) != null) {
                    log.info("[{}] {} gửi lệnh: {}", luong, diaChi, dong);
                    boolean tiepTuc = xuLyLenh(dong.trim(), out);
                    // PrintWriter không ném IOException; checkError() cho biết client đã đóng kết nối.
                    if (!tiepTuc || out.checkError()) {
                        break;
                    }
                }
            } catch (SocketTimeoutException e) {
                log.warn("[{}] Ngắt {} vì không hoạt động quá {} ms", luong, diaChi, thoiGianChoMs);
            } catch (IOException e) {
                log.warn("[{}] Client {} mất kết nối: {}", luong, diaChi, e.getMessage());
            } catch (RuntimeException e) {
                log.error("[{}] Lỗi không mong đợi khi phục vụ {}", luong, diaChi, e);
            } finally {
                ketNoiDangMo.remove(socket);
                log.info("[{}] Client đã ngắt: {}", luong, diaChi);
            }
        }

        /** @return false khi cần đóng kết nối (lệnh QUIT) */
        private boolean xuLyLenh(String dong, PrintWriter out) {
            if (dong.isEmpty()) {
                return true;
            }
            String[] phan = dong.split("\\s+", 2);
            String lenh = phan[0].toUpperCase(Locale.ROOT);
            String doiSo = phan.length > 1 ? phan[1].trim() : "";
            try {
                switch (lenh) {
                    case "TONKHO" -> traLoi(out, doiSo.isEmpty() ? dinhDang(tonKhoService.tatCa()) : traCuuMot(doiSo));
                    case "SAPHET" -> traLoi(out, dinhDang(tonKhoService.sapHet()));
                    case "QUIT" -> {
                        traLoi(out, List.of("Tạm biệt."));
                        return false;
                    }
                    default -> out.println("LOI Lệnh không hợp lệ: " + phan[0] + ". Dùng TONKHO, SAPHET hoặc QUIT.");
                }
            } catch (RuntimeException e) {
                // Lỗi CSDL chỉ báo lỗi cho client này, vòng lặp vẫn tiếp tục và server không bị ảnh hưởng.
                log.error("[{}] Lỗi truy vấn CSDL cho lệnh '{}'", Thread.currentThread().getName(), dong, e);
                out.println("LOI Không truy vấn được cơ sở dữ liệu, vui lòng thử lại sau.");
            }
            return true;
        }

        private List<String> traCuuMot(String ma) {
            return tonKhoService.traCuu(ma).map(d -> List.of(d.dinhDang()))
                    .orElse(List.of("Không tìm thấy sản phẩm: " + ma));
        }

        private List<String> dinhDang(List<DongTonKho> danhSach) {
            return danhSach.isEmpty() ? List.of("Không có sản phẩm nào.")
                    : danhSach.stream().map(DongTonKho::dinhDang).toList();
        }

        private void traLoi(PrintWriter out, List<String> noiDung) {
            out.println("OK " + noiDung.size());
            noiDung.forEach(out::println);
            out.flush();
        }

        private PrintWriter taoWriter(Socket s) throws IOException {
            return new PrintWriter(new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8), true);
        }
    }
}
