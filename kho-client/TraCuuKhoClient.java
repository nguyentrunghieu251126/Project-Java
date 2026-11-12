
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class TraCuuKhoClient {
    public static void main(String[] args) {
        String host = "localhost";
        int port = 8888;
        System.out.println("Ket noi toi Socket Server tai " + host + ":" + port);

        try (
                Socket socket = new Socket(host, port);
                BufferedReader serverReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
                Scanner scanner = new Scanner(System.in)
        ) {
            System.out.println("Server ph hoi: " + serverReader.readLine());
            System.out.println("Nhap lenh (VD: MUA 1 1 hoac TRA_CUU 1 hoac THOAT):");

            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                writer.println(line);
                String response = serverReader.readLine();
                System.out.println("Phan hoi: " + response);
                if ("TAM_BIET".equals(response)) {
                    break;
                }
            }
        } catch (Exception e) {
            System.err.println("Loi client: " + e.getMessage());
        }
    }
}