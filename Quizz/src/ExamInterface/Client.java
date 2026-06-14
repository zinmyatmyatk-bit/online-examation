package ExamInterface;
import java.io.*;
import java.net.*;

public class Client {
    public static void main(String[] args) {
        String serverIp = "192.168.1.6";
        int port = 5000;

        try (Socket socket = new Socket(serverIp, port);
             BufferedReader keyboard = new BufferedReader(new InputStreamReader(System.in));
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            System.out.println("Connected to server " + serverIp + ":" + port);

            while (true) {
                System.out.print("Enter message: ");
                String msg = keyboard.readLine();
                out.println(msg);
                System.out.println("Server reply: " + in.readLine());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
