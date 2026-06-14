package ExamInterface;

import java.io.Serializable;

public class QuestionRow implements Serializable {
    private static final long serialVersionUID = 1L;

    public int id;
    public String subject;
    public String question;
    public String A;
    public String B;
    public String C;
    public String D;
    public char correct;

    public QuestionRow(int id, String subject, String question,
                       String A, String B, String C, String D,
                       char correct) {
        this.id = id;
        this.subject = safe(subject);
        this.question = safe(question);
        this.A = safe(A);
        this.B = safe(B);
        this.C = safe(C);
        this.D = safe(D);
        this.correct = Character.toUpperCase(correct);
    }

    private static String safe(String s) {
        return (s == null) ? "" : s.trim();
    }
}
