package project.dien_may.project_java.service.mang;

import org.springframework.stereotype.Component;
import project.dien_may.project_java.service.TonKhoService;
import project.dien_may.project_java.service.TonKhoKhongDuException;

import jakarta.annotation.PostConstruct; // Hoặc javax.annotation.PostConstruct tùy phiên bản Spring Boot
import jakarta.annotation.PreDestroy;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

@Component // Giúp Spring quản lý và tự động khởi chạy
public class TraCuuKhoServer {
    private final int port = 8888; // Cổng riêng cho Socket Server
    private final TonKhoService tonKhoService;
    private boolean running = false;
    private ServerSocket serverSocket;

    public TraCuuKhoServer(TonKhoService tonKhoService) {
        this.tonKhoService = tonKhoService;
    }

    @PostConstruct // Tự động chạy ngầm ngay khi ứng dụng Spring Boot khởi động xong
    public void start() {
        running = true;
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(port);
                System.out.println("[SERVER] TraCuuKhoServer da khoi dong thanh cong tren port " + port);
                while (running) {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("[SERVER] Client moi ket noi: " + clientSocket.getRemoteSocketAddress());
                    new Thread(new ClientHandler(clientSocket, tonKhoService)).start();
                }
            } catch (Exception e) {
                if (running) {
                    System.err.println("[SERVER] Loi Server Socket: " + e.getMessage());
                }
            }
        }, "SocketServer-Thread").start();
    }

    @PreDestroy // Tự động đóng socket khi tắt ứng dụng
    public void stop() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (Exception ignored) {}
    }

    private static class ClientHandler implements Runnable {
        private final Socket socket;
        private final TonKhoService tonKhoService;

        public ClientHandler(Socket socket, TonKhoService tonKhoService) {
            this.socket = socket;
            this.tonKhoService = tonKhoService;
        }

        @Override
        public void run() {
            String clientThread = Thread.currentThread().getName();
            try (
                    BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)
            ) {
                writer.println("CHAO MUNG DEN VOI SERVER KHO DIEN MAY");
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("[" + clientThread + "] Nhan lenh: " + line);
                    String[] parts = line.trim().split("\\s+");
                    String command = parts[0].toUpperCase();

                    if ("TRA_CUU".equals(command)) {
                        if (parts.length > 1) {
                            Long maSP = Long.parseLong(parts[1]);
                            int ton = tonKhoService.getTonKho(maSP);
                            writer.println("TON_KHO " + maSP + " " + ton);
                        } else {
                            writer.println("LOI Cu phap TRA_CUU <maSanPham>");
                        }
                    } else if ("MUA".equals(command)) {
                        if (parts.length >= 3) {
                            Long maSP = Long.parseLong(parts[1]);
                            int soLuong = Integer.parseInt(parts[2]);
                            try {
                                tonKhoService.truTonKho(maSP, soLuong);
                                writer.println("MUA_THANH_CONG SP=" + maSP + " SL=" + soLuong);
                            } catch (TonKhoKhongDuException e) {
                                writer.println("MUA_THAT_BAI " + e.getMessage());
                            }
                        } else {
                            writer.println("LOI Cu phap MUA <maSanPham> <soLuong>");
                        }
                    } else if ("THOAT".equals(command)) {
                        writer.println("TAM_BIET");
                        break;
                    } else {
                        writer.println("LOI Lenh khong hop le");
                    }
                }
            } catch (Exception e) {
                System.err.println("[" + clientThread + "] Loi xu ly client: " + e.getMessage());
            } finally {
                try {
                    socket.close();
                } catch (Exception ignored) {}
            }
        }
    }
}