package ExamInterface;


import java.rmi.Naming;

public class ExamRMIClient {
    public static void main(String[] args) {
        try {
        	String serverIp = "localhost";

            ExamInterface exam =
                    (ExamInterface) Naming.lookup("rmi://" + serverIp + ":1099/OnlineExam");

            System.out.println("✅ Connected to RMI server: " + serverIp);

            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
