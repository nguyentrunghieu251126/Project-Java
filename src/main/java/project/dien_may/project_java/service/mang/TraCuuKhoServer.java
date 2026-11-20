package project.dien_may.project_java.service.mang;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import project.dien_may.project_java.service.TonKhoService;
import project.dien_may.project_java.service.TonKhoKhongDuException;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Locale;

@Component
public class TraCuuKhoServer {
    private static final int DEFAULT_PORT = 8888;
    private final TonKhoService tonKhoService;
    private final int port;
    private boolean running = false;
    private ServerSocket serverSocket;

    public TraCuuKhoServer(TonKhoService tonKhoService) {
        this(tonKhoService, resolvePort(System.getProperty("kho.socket.port")));
    }

    @Autowired
    public TraCuuKhoServer(TonKhoService tonKhoService, Environment environment) {
        this(tonKhoService, resolvePort(environment.getProperty("kho.socket.port")));
    }

    public TraCuuKhoServer(TonKhoService tonKhoService, int port) {
        this.tonKhoService = tonKhoService;
        this.port = port;
    }

    private static int resolvePort(String configuredPort) {
        if (configuredPort != null && !configuredPort.isBlank()) {
            return Integer.parseInt(configuredPort.trim());
        }
        return DEFAULT_PORT;
    }

    @PostConstruct
    public void start() {
        if (running) {
            return;
        }

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

    @PreDestroy
    public void stop() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (Exception ignored) {
        }
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
                    PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)) {
                writer.println("CHAO MUNG DEN VOI SERVER KHO DIEN MAY");
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("[" + clientThread + "] Nhan lenh: " + line);
                    if (line.isBlank()) {
                        writer.println("LOI Lenh khong hop le");
                        continue;
                    }

                    String[] parts = line.trim().split("\\s+");
                    String command = parts[0].toUpperCase(Locale.ROOT);
                    try {
                        if ("TRA_CUU".equals(command)) {
                            if (parts.length != 2) {
                                writer.println("LOI Cu phap TRA_CUU <maSanPham>");
                                continue;
                            }
                            int maSP = Integer.parseInt(parts[1]);
                            int ton = tonKhoService.getTonKho(maSP);
                            writer.println("TON_KHO " + maSP + " " + ton);
                        } else if ("MUA".equals(command)) {
                            if (parts.length != 3) {
                                writer.println("LOI Cu phap MUA <maSanPham> <soLuong>");
                                continue;
                            }
                            int maSP = Integer.parseInt(parts[1]);
                            int soLuong = Integer.parseInt(parts[2]);
                            if (maSP <= 0 || soLuong <= 0) {
                                writer.println("LOI Ma san pham va so luong phai lon hon 0");
                                continue;
                            }
                            try {
                                tonKhoService.truTonKho(maSP, soLuong);
                                writer.println("MUA_THANH_CONG SP=" + maSP + " SL=" + soLuong);
                            } catch (TonKhoKhongDuException e) {
                                writer.println("MUA_THAT_BAI " + e.getMessage());
                            }
                        } else if ("THOAT".equals(command) && parts.length == 1) {
                            writer.println("TAM_BIET");
                            break;
                        } else {
                            writer.println("LOI Lenh khong hop le");
                        }
                    } catch (NumberFormatException e) {
                        writer.println("LOI Ma san pham va so luong phai la so nguyen");
                    } catch (Exception e) {
                        System.err.println("[" + clientThread + "] Loi xu ly lenh: " + line);
                        e.printStackTrace(System.err);
                        writer.println("LOI Khong the xu ly lenh. Kiem tra log server.");
                    }
                }
            } catch (Exception e) {
                System.err.println("[" + clientThread + "] Loi xu ly client: " + e.getMessage());
            } finally {
                try {
                    socket.close();
                } catch (Exception ignored) {
                }
            }
        }
    }
}