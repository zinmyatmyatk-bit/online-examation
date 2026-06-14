package ExamInterface;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Result implements Serializable {

    private static final long serialVersionUID = 1L;

    // store timestamp format used in CSV + dashboard
    public static final DateTimeFormatter TS_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public final String username;
    public final String subject;
    public final int marks;
    public final int total;
    public final double percent;

    // submitted time (real-world)
    public final String submittedAt;

    // (auto timestamp now)
    public Result(String username, String subject, int marks, int total) {
        this(username, subject, marks, total,
                LocalDateTime.now().format(TS_FORMAT));
    }

    // ✅ constructor when reading from CSV (you already have submittedAt)
    public Result(String username, String subject, int marks, int total, String submittedAt) {
        this.username = safe(username);
        this.subject = safe(subject);
        this.marks = Math.max(0, marks);
        this.total = Math.max(0, total);
        this.percent = (this.total == 0) ? 0.0 : (this.marks * 100.0 / this.total);
        this.submittedAt = safe(submittedAt);
    }

    // ✅ CSV line (6 columns)
    public String toCsvLine() {
        return username + "," + subject + "," + marks + "," + total + ","
                + String.format("%.2f", percent) + "," + submittedAt;
    }

    @Override
    public String toString() {
        return "User: " + username +
                "\nSubject: " + subject +
                "\nMarks: " + marks + "/" + total +
                "\nPercent: " + String.format("%.2f", percent) + "%" +
                "\nSubmitted At: " + submittedAt;
    }

    private static String safe(String s) {
        return (s == null) ? "" : s.trim();
    }
}
