
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class TraCuuKhoClient {
    public static void main(String[] args) {
        String host = "localhost";
        int port = 9090;
        System.out.println("Ket noi toi Socket Server tai " + host + ":" + port);

        try (
                Socket socket = new Socket(host, port);
                BufferedReader serverReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
                Scanner scanner = new Scanner(System.in)
        ) {
            String greeting = serverReader.readLine();
            if (greeting == null) {
                throw new IOException("Server da dong ket noi ma khong gui loi chao.");
            }
            System.out.println("Server ph hoi: " + greeting);
            System.out.println("Nhap lenh (VD: MUA 1 1 hoac TRA_CUU 1 hoac THOAT):");

            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                writer.println(line);
                if (writer.checkError()) {
                    throw new IOException("Khong gui duoc lenh den server.");
                }
                String response = serverReader.readLine();
                if (response == null) {
                    System.err.println("Server da dong ket noi truoc khi gui phan hoi.");
                    break;
                }
                System.out.println("Phan hoi: " + response);
                if ("TAM_BIET".equals(response)) {
                    break;
                }
            }
        } catch (Exception e) {
            System.err.println("Loi client: " + e.getMessage());
            System.err.println("Hay khoi dong ung dung Spring Boot truoc va kiem tra server kho dang chay tren port " + port + ".");
            System.exit(1);
        }
    }
}