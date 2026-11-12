
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class KiemThuBaClient {
    public static void main(String[] args) {
        for (int i = 1; i <= 3; i++) {
            final int id = i;
            new Thread(() -> {
                String clientName = "ClientDemo-" + id;
                try (
                        Socket socket = new Socket("localhost", 8888);
                        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                        PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)
                ) {
                    System.out.println("[" + clientName + "] Ket noi thanh cong. Server: " + reader.readLine());

                    Thread.sleep(id * 50);
                    String request = "MUA 4 4";
                    System.out.println("[" + clientName + "] Gui lenh: " + request);
                    writer.println(request);

                    String resp = reader.readLine();
                    System.out.println("[" + clientName + "] Nhan ket qua: " + resp);

                    writer.println("THOAT");
                } catch (Exception e) {
                    System.err.println("[" + clientName + "] Loi: " + e.getMessage());
                }
            }).start();
        }
    }
}