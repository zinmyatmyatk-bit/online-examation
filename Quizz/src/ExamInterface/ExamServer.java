package ExamInterface;

import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class ExamServer {

    public static void main(String[] args) {
        String serverIP = "172.22.138.33"; // ✅ your server PC IP
        int port = 1099;

        try {
            // for clients to call back to correct IP
            System.setProperty("java.rmi.server.hostname", serverIP);

            // Start registry if not running
            try {
                Registry reg = LocateRegistry.getRegistry(port);
                reg.list();
                System.out.println("RMI Registry already running on port " + port + ".");
            } catch (Exception e) {
                LocateRegistry.createRegistry(port);
                System.out.println("New RMI Registry started on port " + port + ".");
            }

            ExamInterface exam = new ExamImpl();

            // bind using the real server IP (NOT 0.0.0.0)
            Naming.rebind("rmi://" + serverIP + ":" + port + "/OnlineExam", exam);

            System.out.println("Online quiz Server is running...");
            System.out.println("Bound as: rmi://" + serverIP + ":" + port + "/OnlineExam");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}