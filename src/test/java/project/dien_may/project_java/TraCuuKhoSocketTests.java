package project.dien_may.project_java;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import project.dien_may.project_java.service.TonKhoService;
import project.dien_may.project_java.service.mang.TraCuuKhoServer;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class TraCuuKhoSocketTests {
    private TraCuuKhoServer server;
    private final int PORT = 8889;

    @BeforeEach
    public void setUp() {
        TonKhoService tonKhoService = new TonKhoService();
        server = new TraCuuKhoServer(tonKhoService);
        server.start();
    }

    @AfterEach
    public void tearDown() {
        server.stop();
    }

    @Test
    public void testSocketMuaVaTraCuu() {
        try (
                Socket socket = new Socket("localhost", PORT);
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)
        ) {
            String greeting = reader.readLine();
            assertTrue(greeting.contains("CHAO MUNG"));

            writer.println("MUA 1 2");
            String response = reader.readLine();
            assertTrue(response.contains("MUA_THANH_CONG"));

            writer.println("TRA_CUU 1");
            response = reader.readLine();
            assertTrue(response.contains("TON_KHO 1 8"));

            writer.println("THOAT");
        } catch (Exception e) {
            System.err.println("Loi test socket: " + e.getMessage());
        }
    }
}