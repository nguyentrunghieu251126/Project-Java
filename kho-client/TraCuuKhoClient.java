import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;

/**
 * Client "Tra cứu kho" (TV5 - NFR03). Chương trình Java độc lập, không dùng Spring: chỉ kết nối TCP tới
 * socket server chạy cùng ứng dụng Spring Boot, dữ liệu tồn kho do server đọc từ SQL Server.
 *
 * Chạy:  java TraCuuKhoClient.java [máy chủ] [cổng]      (mặc định localhost 9090)
 * Lệnh:  TONKHO [mã sản phẩm hoặc mã model] | SAPHET | QUIT
 */
public class TraCuuKhoClient {

    private static final int THOI_GIAN_CHO_PHAN_HOI_MS = 15_000;

    public static void main(String[] args) {
        String maychu = args.length > 0 ? args[0] : "localhost";
        int cong = args.length > 1 ? Integer.parseInt(args[1]) : 9090;
        PrintStream hienThi = new PrintStream(System.out, true, StandardCharsets.UTF_8);

        try (Socket socket = new Socket(maychu, cong);
             BufferedReader tuServer = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter denServer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
             BufferedReader banPhim = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))) {

            socket.setSoTimeout(THOI_GIAN_CHO_PHAN_HOI_MS);
            hienThi.println("Đã kết nối tới " + maychu + ":" + cong);
            if (!docPhanHoi(tuServer, hienThi)) {
                return; // server từ chối (đang bận) hoặc đóng ngay
            }

            String dong;
            hienThi.print("kho> ");
            hienThi.flush();
            while ((dong = banPhim.readLine()) != null) {
                dong = dong.trim();
                if (dong.isEmpty()) {
                    hienThi.print("kho> ");
                    hienThi.flush();
                    continue;
                }
                denServer.println(dong);
                if (denServer.checkError()) {
                    hienThi.println("Mất kết nối tới server (không gửi được lệnh).");
                    return;
                }
                if (!docPhanHoi(tuServer, hienThi) || dong.equalsIgnoreCase("QUIT")) {
                    return;
                }
                hienThi.print("kho> ");
                hienThi.flush();
            }
        } catch (ConnectException e) {
            hienThi.println("Không kết nối được tới " + maychu + ":" + cong
                    + ". Hãy chắc chắn ứng dụng Spring Boot đang chạy.");
            System.exit(1);
        } catch (SocketTimeoutException e) {
            hienThi.println("Server không phản hồi trong " + (THOI_GIAN_CHO_PHAN_HOI_MS / 1000) + " giây, đóng kết nối.");
            System.exit(1);
        } catch (IOException e) {
            hienThi.println("Mất kết nối tới server: " + e.getMessage());
            System.exit(1);
        }
    }

    /**
     * Đọc một phản hồi theo giao thức: "OK n" kèm n dòng, hoặc "LOI nội dung".
     *
     * @return false khi server đã đóng kết nối hoặc báo lỗi kết nối
     */
    private static boolean docPhanHoi(BufferedReader tuServer, PrintStream hienThi) throws IOException {
        String dauDong = tuServer.readLine();
        if (dauDong == null) {
            hienThi.println("Server đã ngắt kết nối.");
            return false;
        }
        if (dauDong.startsWith("LOI")) {
            hienThi.println("Lỗi: " + dauDong.substring(3).trim());
            return !dauDong.contains("tối đa"); // báo bận thì server sẽ đóng kết nối
        }
        if (!dauDong.startsWith("OK ")) {
            hienThi.println("Phản hồi không đúng giao thức: " + dauDong);
            return false;
        }
        int soDong = Integer.parseInt(dauDong.substring(3).trim());
        for (int i = 0; i < soDong; i++) {
            String noiDung = tuServer.readLine();
            if (noiDung == null) {
                hienThi.println("Server ngắt kết nối giữa chừng.");
                return false;
            }
            hienThi.println(noiDung);
        }
        return true;
    }
}
