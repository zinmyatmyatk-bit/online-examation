package ExamInterface;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

public class ExamImpl extends UnicastRemoteObject implements ExamInterface {

    private static final long serialVersionUID = 1L;

    //  Teacher account 
    private static final String TEACHER_NAME = "ayemyatmyatpaing";
    private static final String TEACHER_PASS = "123456";

    // Server CSV files (stored where ExamServer runs)
    private static final String RESULT_FILE = "results.csv";
    private static final String QUESTION_FILE = "questions.csv";

    // usernameLower -> Student
    private final Map<String, Student> studentAccounts;

    // subject -> question text list / correct answers list
    private final Map<String, List<String>> questionsMap;
    private final Map<String, List<Character>> answersMap;

    // Keep ALL attempts (history)
    private final List<Result> allAttempts;

    // username|subject -> last submitted answers for review
    private final Map<String, List<Character>> submittedAnswersMap;

    // Full question rows for teacher edit/delete
    private final List<QuestionRow> questionRows;

    protected ExamImpl() throws RemoteException {
        super();

        studentAccounts = new HashMap<>();
        questionsMap = new HashMap<>();
        answersMap = new HashMap<>();
        allAttempts = new ArrayList<>();
        submittedAnswersMap = new HashMap<>();
        questionRows = new ArrayList<>();

     
        addStudent("yaminaye", "12345");
        addStudent("khinhninwai", "12345");
        addStudent("wuttmoneoo", "12345");
        addStudent("wintwahmyomyint", "12345");
        addStudent("zinnaingnaing", "12345");
        addStudent("myathsupaing", "12345");
        addStudent("eaintchyuthaw", "12345");
        addStudent("linwaizuthwe", "12345");
        addStudent("kyikyichit", "12345");
        addStudent("thantthureinphyo", "12345");
        addStudent("zinmyatmyatkyaw", "12345");
        addStudent("nyiminnyein", "12345");
        addStudent("zarnimaung", "12345");
        addStudent("phyominhtet", "12345");
        addStudent("yepaing", "12345");

        // show where server reads/writes CSV
        System.out.println("=== ExamServer Working Directory ===");
        System.out.println("cwd = " + new File(".").getAbsolutePath());
        System.out.println("questions.csv path = " + new File(QUESTION_FILE).getAbsolutePath());
        System.out.println("results.csv path   = " + new File(RESULT_FILE).getAbsolutePath());
        System.out.println("====================================");

        // Load attempt history from results.csv
        loadResultsFromFile();

        // Ensure questions.csv exists (teacher can edit in Excel)
        ensureQuestionsCsvExists();

        // Load questions from CSV into memory
        boolean ok = loadQuestionsFromCsv();

        // If file exists but loading failed / no questions, rebuild the file and reload
        if (!ok || questionsMap.isEmpty()) {
            System.out.println("⚠ No questions loaded. Rebuilding questions.csv and reloading...");
            rebuildQuestionsCsv();
            loadQuestionsFromCsv();
        }

    
        printQuestionSummary();
    }

    private void printQuestionSummary() {
        System.out.println("=== Questions Loaded Summary ===");
        System.out.println("Subjects count = " + questionsMap.keySet().size());
        for (String s : questionsMap.keySet()) {
            System.out.println(" - " + s + " => " + questionsMap.get(s).size() + " questions");
        }
        System.out.println("================================");
    }

    // BASIC VALIDATION 
    private boolean isValidUsername(String u) {
        return u != null && u.matches("^[a-zA-Z0-9._]+$");
    }

    private boolean isValidPassword(String p) {
        return p != null && p.length() >= 3;
    }

    private void addStudent(String u, String p) {
        if (isValidUsername(u) && isValidPassword(p)) {
            studentAccounts.put(u.toLowerCase(), new Student(u, p));
        }
    }

    // STUDENT METHODS
    @Override
    public boolean login(String username, String password) {
        if (username == null || password == null) return false;
        Student s = studentAccounts.get(username.toLowerCase());
        return s != null && s.password.equals(password);
    }

    @Override
    public synchronized boolean registerStudent(String username, String email, String password) {
        if (!isValidUsername(username) || !isValidPassword(password)) return false;
        String key = username.toLowerCase();
        if (studentAccounts.containsKey(key)) return false;

        studentAccounts.put(key, new Student(username, password));
        return true;
    }

    @Override
    public List<String> getSubjects() {
        List<String> subs = new ArrayList<>(questionsMap.keySet());
        subs.sort(String::compareToIgnoreCase);
        return subs;
    }

    @Override
    public List<String> getQuestions(String subject) {
        if (subject == null) return new ArrayList<>();
        return new ArrayList<>(questionsMap.getOrDefault(subject.trim(), new ArrayList<>()));
    }

    @Override
    public synchronized int submitAnswers(String username, String subject, List<Character> answers) {
        if (username == null || subject == null || answers == null) return 0;

        subject = subject.trim();

        List<Character> correct = answersMap.get(subject);
        if (correct == null) return 0;

        int marks = 0;
        for (int i = 0; i < correct.size() && i < answers.size(); i++) {
            if (Objects.equals(correct.get(i), answers.get(i))) marks++;
        }

        // Store last answers for review
        String key = username.toLowerCase() + "|" + subject;
        submittedAnswersMap.put(key, new ArrayList<>(answers));

        //  (keep ALL attempts)
        Result r = new Result(
                username,
                subject,
                marks,
                correct.size(),
                LocalDateTime.now().format(Result.TS_FORMAT)
        );
        allAttempts.add(r);

        // Append to results.csv
        appendResultToCsv(r);

        return marks;
    }

    @Override
    public String getResult(String username, String subject) {
        if (username == null || subject == null) return "No result found.";
        subject = subject.trim();

        // return latest attempt for this student + subject
        Result latest = null;
        for (Result r : allAttempts) {
            if (r.username != null && r.subject != null
                    && r.username.equalsIgnoreCase(username)
                    && r.subject.equalsIgnoreCase(subject)) {
                latest = r;
            }
        }
        return (latest == null) ? "No result found." : latest.toString();
    }

    @Override
    public String getAnswerReview(String username, String subject) {
        if (username == null || subject == null) return "No review available.";

        subject = subject.trim();

        String key = username.toLowerCase() + "|" + subject;
        List<Character> ua = submittedAnswersMap.get(key);
        List<Character> ca = answersMap.get(subject);

        if (ua == null || ca == null) return "No review available.";

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ca.size(); i++) {
            char your = (i < ua.size()) ? ua.get(i) : ' ';
            char corr = ca.get(i);
            sb.append("Q").append(i + 1)
              .append(": Your=").append(your)
              .append(" Correct=").append(corr)
              .append("\n");
        }
        return sb.toString();
    }

    //TEACHER METHODS 
    @Override
    public boolean teacherLogin(String name, String password) {
        if (name == null || password == null) return false;
        return TEACHER_NAME.equalsIgnoreCase(name.trim())
                && TEACHER_PASS.equals(password.trim());
    }

    @Override
    public List<Result> getAllResults() {
        return new ArrayList<>(allAttempts);
    }

    @Override
    public List<Result> searchResultsBySubject(String subjectQuery) {
        if (subjectQuery == null) return new ArrayList<>();
        String q = subjectQuery.trim().toLowerCase();

        List<Result> out = new ArrayList<>();
        for (Result r : allAttempts) {
            if (r.subject != null && r.subject.toLowerCase().contains(q)) out.add(r);
        }
        return out;
    }

    @Override
    public List<Result> searchResultsByStudent(String studentQuery) {
        if (studentQuery == null) return new ArrayList<>();
        String q = studentQuery.trim().toLowerCase();

        List<Result> out = new ArrayList<>();
        for (Result r : allAttempts) {
            if (r.username != null && r.username.toLowerCase().contains(q)) out.add(r);
        }
        return out;
    }

    @Override
    public int getAttemptCount(String username) {
        if (username == null) return 0;

        String q = username.trim().toLowerCase();
        int c = 0;

        for (Result r : allAttempts) {
            if (r.username != null && r.username.toLowerCase().equals(q)) c++;
        }
        return c;
    }


    // esults delete helpers 

    @Override
    public synchronized boolean deleteResult(String username, String subject, String submittedAt) {
        if (username == null || subject == null || submittedAt == null) return false;

        String u = username.trim();
        String s = subject.trim();
        String t = submittedAt.trim();

        boolean removed = allAttempts.removeIf(r ->
                r != null
                        && r.username != null && r.subject != null && r.submittedAt != null
                        && r.username.equalsIgnoreCase(u)
                        && r.subject.equalsIgnoreCase(s)
                        && r.submittedAt.equals(t)
        );

        if (!removed) return false;
        return rewriteResultsCsv();
    }

    @Override
    public synchronized boolean clearAllResults() {
        allAttempts.clear();
        return rewriteResultsCsv();
    }

    private synchronized boolean rewriteResultsCsv() {
        try (OutputStreamWriter osw = new OutputStreamWriter(
                new FileOutputStream(RESULT_FILE, false), StandardCharsets.UTF_8);
             BufferedWriter bw = new BufferedWriter(osw)) {

            for (Result r : allAttempts) {
                if (r == null) continue;
                bw.write(r.toCsvLine());
                bw.newLine();
            }
            bw.flush();
            return true;
        } catch (Exception e) {
            System.out.println("Error rewriting results.csv: " + e.getMessage());
            return false;
        }
    }

    // Questions (CSV/Excel) 
    @Override
    public synchronized boolean addQuestion(String subject, String question,
                                           String A, String B, String C, String D,
                                           char correct) {

        if (subject == null || subject.trim().isEmpty()) return false;
        if (question == null || question.trim().isEmpty()) return false;

        correct = Character.toUpperCase(correct);
        if (correct != 'A' && correct != 'B' && correct != 'C' && correct != 'D') return false;

        // simple CSV safety: replace commas with ;
        String s = clean(subject);
        String q = clean(question);
        String a = clean(A);
        String b = clean(B);
        String c = clean(C);
        String d = clean(D);

        ensureQuestionsCsvExists();

        try (FileWriter fw = new FileWriter(QUESTION_FILE, true)) {
            fw.write(s + "," + q + "," + a + "," + b + "," + c + "," + d + "," + correct + "\n");
        } catch (Exception e) {
            System.out.println("Error writing questions.csv: " + e.getMessage());
            return false;
        }

        // Update
        int newId = questionRows.size() + 1;
        questionRows.add(new QuestionRow(newId, s, q, a, b, c, d, correct));

        String qText = formatQuestionText(q, a, b, c, d);
        questionsMap.computeIfAbsent(s, k -> new ArrayList<>()).add(qText);
        answersMap.computeIfAbsent(s, k -> new ArrayList<>()).add(correct);

        return true;
    }

    @Override
    public synchronized boolean reloadQuestionsFromCsv() {
        boolean ok = loadQuestionsFromCsv();
        printQuestionSummary();
        return ok;
    }

    @Override
    public List<QuestionRow> getAllQuestions() {
        return new ArrayList<>(questionRows);
    }

    @Override
    public synchronized boolean updateQuestion(int id, String subject, String question,
                                              String A, String B, String C, String D,
                                              char correct) {

        if (id <= 0) return false;
        if (subject == null || subject.trim().isEmpty()) return false;
        if (question == null || question.trim().isEmpty()) return false;

        correct = Character.toUpperCase(correct);
        if (correct != 'A' && correct != 'B' && correct != 'C' && correct != 'D') return false;

        String s = clean(subject);
        String q = clean(question);
        String a = clean(A);
        String b = clean(B);
        String c = clean(C);
        String d = clean(D);

        int idx = -1;
        for (int i = 0; i < questionRows.size(); i++) {
            if (questionRows.get(i).id == id) {
                idx = i;
                break;
            }
        }
        if (idx == -1) return false;

        questionRows.set(idx, new QuestionRow(id, s, q, a, b, c, d, correct));

        saveQuestionsBackToCsv();
        boolean ok = loadQuestionsFromCsv();
        printQuestionSummary();
        return ok;
    }

    @Override
    public synchronized boolean deleteQuestion(int id) {
        if (id <= 0) return false;

        boolean removed = questionRows.removeIf(q -> q.id == id);
        if (!removed) return false;

        // re-number ids
        for (int i = 0; i < questionRows.size(); i++) {
            questionRows.get(i).id = i + 1;
        }

        saveQuestionsBackToCsv();
        boolean ok = loadQuestionsFromCsv();
        printQuestionSummary();
        return ok;
    }

    // ================= RESULTS CSV =================
    private void appendResultToCsv(Result r) {
        try (FileWriter fw = new FileWriter(RESULT_FILE, true)) {
            fw.write(r.toCsvLine() + "\n");
        } catch (Exception e) {
            System.out.println("Error writing results.csv: " + e.getMessage());
        }
    }

    private void loadResultsFromFile() {
        File f = new File(RESULT_FILE);
        if (!f.exists()) return;

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8))) {

            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                // expected: username,subject,marks,total,percent,submittedAt
                String[] p = line.split(",", 6);
                if (p.length < 4) continue;

                String username = p[0].trim();
                String subject  = (p.length >= 2) ? p[1].trim() : "";
                int marks       = (p.length >= 3) ? parseIntSafe(p[2]) : 0;
                int total       = (p.length >= 4) ? parseIntSafe(p[3]) : 0;

                String submittedAt = (p.length >= 6) ? p[5].trim()
                        : LocalDateTime.now().format(Result.TS_FORMAT);

                allAttempts.add(new Result(username, subject, marks, total, submittedAt));
            }

        } catch (Exception e) {
            System.out.println("Error reading results.csv: " + e.getMessage());
        }
    }

    private int parseIntSafe(String s) {
        try { return Integer.parseInt(s.trim()); }
        catch (Exception e) { return 0; }
    }

    // ================= QUESTIONS CSV (Excel) =================

    private void ensureQuestionsCsvExists() {
        File f = new File(QUESTION_FILE);
        if (f.exists()) return;
        rebuildQuestionsCsv();
    }

    // Rebuild full CSV with your 45 questions (called when missing OR broken)
    private void rebuildQuestionsCsv() {
        try (FileWriter fw = new FileWriter(QUESTION_FILE, false)) {

            fw.write("subject,question,A,B,C,D,correct\n");

            // ===== Distributed and Parallel Computing =====
            fw.write("Parallel and Distributed Computing,What does the term parallel computing refer to in computer science?,Sequential processing,Executing multiple tasks simultaneously,Slow computation,Using a single CPU only,B\n");
            fw.write("Parallel and Distributed Computing,Which of the following is a recognized type of parallelism?,Data parallelism,Serial processing,Disk processing,No parallelism,A\n");
            fw.write("Parallel and Distributed Computing,What best describes a distributed system?,A collection of independent computers working together,A single personal computer,A system with only a server,A system with only a client,A\n");
            fw.write("Parallel and Distributed Computing,Which medium is commonly used for communication in distributed systems?,Computer network,Printer,Mouse,Scanner,A\n");
            fw.write("Parallel and Distributed Computing,What does MPI stand for in parallel computing?,Message Passing Interface,Image Processing Interface,Memory Program Index,Machine Performance Indicator,A\n");
            fw.write("Parallel and Distributed Computing,What is a major advantage of parallel computing?,Slower execution,Faster processing speed,Manual operation,Lower accuracy,B\n");
            fw.write("Parallel and Distributed Computing,Which programming model is commonly used for shared memory systems?,OpenMP,MPI,FTP,SMTP,A\n");
            fw.write("Parallel and Distributed Computing,Which of the following represents a type of parallel computer architecture?,SIMD,SDIM,SIDS,None of the above,A\n");
            fw.write("Parallel and Distributed Computing,What is the primary goal of load balancing in parallel systems?,To distribute work equally among processors,To add more workload,To remove CPUs,To disconnect the network,A\n");
            fw.write("Parallel and Distributed Computing,In computing what is meant by a cluster?,A group of interconnected computers,A single laptop,A printer device,A collection of files,A\n");
            fw.write("Parallel and Distributed Computing,What does the term speedup measure in parallel computing?,Slowness of execution,Performance gain compared to sequential execution,Memory size,RAM capacity,B\n");
            fw.write("Parallel and Distributed Computing,Which issue commonly occurs due to improper synchronization in parallel programs?,Race condition,Formatting error,Pixel error,Printing issue,A\n");
            fw.write("Parallel and Distributed Computing,Which of the following is NOT a parallel programming model?,MPI,OpenMP,CUDA,HTML,D\n");
            fw.write("Parallel and Distributed Computing,What does the term deadlock describe in concurrent systems?,Successful execution,A situation where processes wait indefinitely,Faster execution,Improved speedup,B\n");
            fw.write("Parallel and Distributed Computing,What does fault tolerance mean in a distributed system?,System failure,Ability to continue operating despite failures,No network connection,Single node operation,B\n");
            // ===== Strategies of Emerging Technology I =====
            fw.write("Strategies of Emerging Technology I,What is meant by cloud computing?,Local computing only,Computing services delivered over the Internet,Offline processing,Physical hardware only,B\n");
            fw.write("Strategies of Emerging Technology I,What does SaaS stand for in cloud computing?,Software as a Service,Storage as a System,System as a Server,Software and Storage,A\n");
            fw.write("Strategies of Emerging Technology I,What does IaaS represent in cloud service models?,Internet as a Service,Infrastructure as a Service,Information as a Service,Interface as a Service,B\n");
            fw.write("Strategies of Emerging Technology I,What does PaaS stand for in cloud computing?,Platform as a Service,Process as a Service,Product as a Service,Power as a Service,A\n");
            fw.write("Strategies of Emerging Technology I,Which of the following is a type of cloud deployment model?,Private cloud,Local storage,Offline mode,Personal device,A\n");
            fw.write("Strategies of Emerging Technology I,Which of the following is a well-known cloud service provider?,Google Cloud,Paint application,Notes application,Calculator,A\n");
            fw.write("Strategies of Emerging Technology I,What is a key benefit of using cloud computing services?,Cost efficiency and scalability,No Internet access,Single device usage,Manual processing,B\n");
            fw.write("Strategies of Emerging Technology I,Which of the following is an example of a SaaS application?,Google Docs,CPU,Router,RAM,A\n");
            fw.write("Strategies of Emerging Technology I,Which technology is fundamental to cloud computing?,Virtualization,Manual configuration,Data typing,Paper documentation,A\n");
            fw.write("Strategies of Emerging Technology I,What characterizes a public cloud?,Owned by one organization,Shared among multiple users,Completely offline,Used only by government,B\n");
            fw.write("Strategies of Emerging Technology I,What does IoT stand for?,Internet of Things,Input Output Transfer,Internal Online Tools,Integrated Office Technology,A\n");
            fw.write("Strategies of Emerging Technology I,What does AI mean in modern computing?,Artificial Intelligence,Automated Internet,Advanced Input,Application Interface,A\n");
            fw.write("Strategies of Emerging Technology I,What does the term Big Data refer to?,Small datasets,Extremely large and complex datasets,Image files only,Text documents only,B\n");
            fw.write("Strategies of Emerging Technology I,What is blockchain technology mainly used for?,Secure and transparent transactions,Typing documents,Playing games,Editing images,A\n");
            fw.write("Strategies of Emerging Technology I,What is the primary goal of cybersecurity?,Protecting systems and data from attacks,Painting systems,Purchasing software,Installing hardware,A\n");

            // ===== Advanced Artificial Intelligence =====
            fw.write("Advanced Artificial Intelligence,What is the main goal of a heuristic function in AI search?,To guarantee optimality always,To estimate the cost to reach the goal,To increase memory usage,To randomize the search,B\n");
            fw.write("Advanced Artificial Intelligence,In machine learning overfitting happens when...,Model performs well on new data,Model learns training data too well and fails on new data,Dataset is too small always,Training is too slow,B\n");
            fw.write("Advanced Artificial Intelligence,Which model is best known for handling sequences in deep learning?,CNN,RNN,KNN,PCA,B\n");
            fw.write("Advanced Artificial Intelligence,What does the term gradient descent do?,Increases loss,Minimizes loss by updating weights,Stops training immediately,Normalizes features,B\n");
            fw.write("Advanced Artificial Intelligence,In reinforcement learning what is a reward?,Input data,A signal that evaluates an action,A hidden layer,A type of dataset,B\n");
            fw.write("Advanced Artificial Intelligence,What is a Markov property in MDP?,Future depends on entire history,Future depends only on current state,Future is random always,States cannot repeat,B\n");
            fw.write("Advanced Artificial Intelligence,Which is a common activation function in neural networks?,ReLU,IF-ELSE,SQL,BFS,A\n");
            fw.write("Advanced Artificial Intelligence,What does NLP stand for?,Network Link Protocol,Natural Language Processing,Neural Learning Pattern,Normal Loss Prediction,B\n");
            fw.write("Advanced Artificial Intelligence,What is the main job of an optimizer in deep learning?,Draw graphs,Update model parameters to reduce loss,Increase dataset size,Convert images to text,B\n");
            fw.write("Advanced Artificial Intelligence,Which algorithm is used for classification?,KNN,Dijkstra,Floyd Warshall,Prim,A\n");
            fw.write("Advanced Artificial Intelligence,What is the purpose of a confusion matrix?,To confuse model,To evaluate classification performance,To train faster,To compress data,B\n");
            fw.write("Advanced Artificial Intelligence,What is transfer learning?,Training from scratch always,Reusing a pretrained model for a new task,Deleting old model,Encrypting model,B\n");
            fw.write("Advanced Artificial Intelligence,What does epoch mean in training?,One forward pass only,One full pass through training dataset,Testing phase,A loss function,B\n");
            fw.write("Advanced Artificial Intelligence,Which is a feature of CNNs?,Works only with text,Uses convolution filters to extract features,Cannot learn patterns,Needs no data,B\n");
            fw.write("Advanced Artificial Intelligence,What is the purpose of regularization?,Increase overfitting,Reduce overfitting,Remove labels,Break the model,B\n");

            System.out.println("✅ questions.csv created/rebuilt successfully.");

        } catch (Exception e) {
            System.out.println("Error creating questions.csv: " + e.getMessage());
        }
    }

    private synchronized boolean loadQuestionsFromCsv() {
        File f = new File(QUESTION_FILE);
        if (!f.exists()) return false;

        Map<String, List<String>> qMap = new HashMap<>();
        Map<String, List<Character>> aMap = new HashMap<>();
        questionRows.clear();

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8))) {

            String line;
            boolean first = true;

            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                // skip header
                if (first && line.toLowerCase().startsWith("subject,")) {
                    first = false;
                    continue;
                }
                first = false;

                // naive CSV split: do not use commas in text (use ; instead)
                String[] p = line.split(",", 7);
                if (p.length < 7) continue;

                String subject = p[0].trim();
                String question = p[1].trim();
                String A = p[2].trim();
                String B = p[3].trim();
                String C = p[4].trim();
                String D = p[5].trim();
                char correct = p[6].trim().isEmpty() ? 'A' : Character.toUpperCase(p[6].trim().charAt(0));

                // ignore invalid correct option
                if (correct != 'A' && correct != 'B' && correct != 'C' && correct != 'D') continue;

                // fill maps for students
                String qText = formatQuestionText(question, A, B, C, D);
                qMap.computeIfAbsent(subject, k -> new ArrayList<>()).add(qText);
                aMap.computeIfAbsent(subject, k -> new ArrayList<>()).add(correct);

                // fill list for teacher (edit/delete)
                int id = questionRows.size() + 1;
                questionRows.add(new QuestionRow(id, subject, question, A, B, C, D, correct));
            }

            questionsMap.clear();
            answersMap.clear();
            questionsMap.putAll(qMap);
            answersMap.putAll(aMap);

            return !questionsMap.isEmpty();

        } catch (Exception e) {
            System.out.println("Error reading questions.csv: " + e.getMessage());
            return false;
        }
    }

    private synchronized void saveQuestionsBackToCsv() {
        try (FileWriter fw = new FileWriter(QUESTION_FILE, false)) {
            fw.write("subject,question,A,B,C,D,correct\n");
            for (QuestionRow q : questionRows) {
                fw.write(clean(q.subject) + "," + clean(q.question) + "," +
                        clean(q.A) + "," + clean(q.B) + "," +
                        clean(q.C) + "," + clean(q.D) + "," +
                        Character.toUpperCase(q.correct) + "\n");
            }
        } catch (Exception e) {
            System.out.println("Error saving questions.csv: " + e.getMessage());
        }
    }

    private String formatQuestionText(String question, String A, String B, String C, String D) {
        return "Q: " + question +
                "\nA) " + A +
                "\nB) " + B +
                "\nC) " + C +
                "\nD) " + D;
    }

    private String clean(String s) {
        return (s == null) ? "" : s.trim().replace(",", ";");
    }
}