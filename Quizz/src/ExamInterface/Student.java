package ExamInterface;

import java.io.Serializable;

public class Student implements Serializable {
    public String username;
        public String password;

  
    public Student(String username,String password) {
        this.username = username;
        this.password = password;
    }
}
