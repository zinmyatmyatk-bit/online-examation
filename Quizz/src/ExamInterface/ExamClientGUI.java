package ExamInterface;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.rmi.Naming;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExamClientGUI extends JFrame {
    private ExamInterface exam;

  
    private final Color PRIMARY = Color.decode("#0c3635");
    private final Color BG = new Color(246, 246, 246);
    private final Color WHITE = Color.WHITE;
    private final Color TEXT = new Color(20, 20, 20);
    private final Color MUTED = new Color(110, 110, 110);
    private final Color BORDER = new Color(220, 220, 220);

    
    private CardLayout cardLayout;
    private JPanel cards;

  
    private JLabel headerUserLabel;
    private JButton headerLogoutBtn;
    private JLabel headerProfileLabel;

    //  LOGIN UI 
    private HintTextField usernameField;
    private HintPasswordField passwordField;

    // DASHBOARD UI 
    private JPanel coursesListPanel;

    // EXAM UI 
    private JLabel examSubjectLabel;
    private JLabel qCountLabel;
    private JLabel timerLabel;
    private JProgressBar timerBar;

    private JTextArea questionArea;
    private JRadioButton optA, optB, optC, optD;
    private ButtonGroup optionGroup;

    private JButton backToCoursesBtn;
    private JButton prevBtn, nextBtn, submitBtn;

    // RESULT UI
    private JTextArea resultArea;

    // STATE
    private String currentUser = "";
    private boolean isTeacherUser = false;
    private String selectedSubject = "";

    //to close on logout)
    private TeacherDashboardGUI teacherDashboard;

    private List<String> questionsRaw = new ArrayList<>();
    private List<Character> userAnswers = new ArrayList<>();
    private int index = 0;

    // Timer
    private javax.swing.Timer countdownTimer;
    private int totalSeconds = 15 * 60;
    private int remainingSeconds = totalSeconds;

    // background cache for glass blur
    private BufferedImage bgOriginal;
    private BufferedImage bgBlur;

    public ExamClientGUI() {
        connectRMI();

        setTitle("Online Quiz System");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setLocationRelativeTo(null);

        bgOriginal = loadBufferedImage("/images/background.jpg");
        bgBlur = (bgOriginal != null) ? blurImage(bgOriginal, 18) : null;

        add(buildHeader(), BorderLayout.NORTH);

        cardLayout = new CardLayout();
        cards = new JPanel(cardLayout);

        cards.add(buildLoginPage(), "login");
        cards.add(buildCoursesPage(), "courses");
        cards.add(buildExamPage(), "exam");
        cards.add(buildResultPage(), "result");

        add(cards, BorderLayout.CENTER);

        setHeaderForLoggedIn(false);
        showPage("login");
        setVisible(true);
    }

    // ===================== RMI CONNECT =====================
    private void connectRMI() {
        try {
            exam = (ExamInterface) Naming.lookup("rmi://localhost/OnlineExam");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(
                    null,
                    "Cannot connect to server.\nStart server first.\n\n" + e.getMessage(),
                    "Connection Error",
                    JOptionPane.ERROR_MESSAGE
            );
            System.exit(0);
        }
    }

    // HEADER 
    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(PRIMARY);
        header.setBorder(new EmptyBorder(10, 12, 10, 12));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        left.setOpaque(false);

        JLabel logo = new JLabel();
        ImageIcon logoIcon = loadIcon("/images/logo.png", 60, 60);
        if (logoIcon == null) logoIcon = loadIcon("/images/logo.jpg", 60, 60);
        if (logoIcon != null) logo.setIcon(logoIcon);

        JLabel title = new JLabel("Online Quiz System");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Segoe UI", Font.BOLD, 25));

        left.add(logo);
        left.add(title);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        right.setOpaque(false);

        headerProfileLabel = new JLabel();
        headerProfileLabel.setPreferredSize(new Dimension(36, 36));

        headerUserLabel = new JLabel(" ");
        headerUserLabel.setForeground(Color.WHITE);
        headerUserLabel.setFont(new Font("Segoe UI", Font.PLAIN, 15));

        headerLogoutBtn = new JButton("Logout");
        styleHeaderButton(headerLogoutBtn);
        headerLogoutBtn.addActionListener(e -> doLogout());
        headerLogoutBtn.setVisible(false);

        right.add(headerProfileLabel);
        right.add(headerUserLabel);
        right.add(headerLogoutBtn);

        header.add(left, BorderLayout.WEST);
        header.add(right, BorderLayout.EAST);

        return header;
    }

    private void styleHeaderButton(JButton b) {
        b.setFont(new Font("Segoe UI", Font.BOLD, 15));
        b.setBackground(Color.WHITE);
        b.setForeground(PRIMARY);
        b.setFocusPainted(false);
        b.setBorder(new EmptyBorder(7, 14, 7, 14));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    private void setHeaderForLoggedIn(boolean loggedIn) {
        headerLogoutBtn.setVisible(loggedIn);

        if (!loggedIn) {
            headerUserLabel.setText(" ");
            headerProfileLabel.setIcon(makeInitialsAvatar("?", 36));
        } else {
            String role = isTeacherUser ? "Teacher" : "Student";
            headerUserLabel.setText(role + ": " + currentUser);
            headerProfileLabel.setIcon(makeInitialsAvatar(currentUser, 36));
        }
    }

    // LOGIN PAGE 
    private JPanel buildLoginPage() {
        JPanel root = new JPanel(new GridBagLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

                if (bgOriginal != null) {
                    g2.drawImage(bgOriginal, 0, 0, getWidth(), getHeight(), this);
                    g2.setColor(new Color(0, 0, 0, 70));
                    g2.fillRect(0, 0, getWidth(), getHeight());
                } else {
                    GradientPaint gp = new GradientPaint(
                            0, 0, new Color(25, 80, 100),
                            0, getHeight(), new Color(5, 35, 45)
                    );
                    g2.setPaint(gp);
                    g2.fillRect(0, 0, getWidth(), getHeight());
                }
                g2.dispose();
            }
        };

        GlassCard card = new GlassCard(root, bgBlur);
        card.setPreferredSize(new Dimension(440, 440));
        card.setLayout(new BorderLayout());

        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setBorder(new EmptyBorder(34, 40, 10, 40));
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));

        JLabel logo = new JLabel();
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);
        ImageIcon logoIcon = loadIcon("/images/logo.png", 110, 110);
        if (logoIcon == null) logoIcon = loadIcon("/images/logo.jpg", 110, 110);
        if (logoIcon != null) logo.setIcon(logoIcon);

        JLabel title = new JLabel("WELCOME");
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        title.setForeground(TEXT);
        title.setFont(new Font("Segoe UI", Font.BOLD, 14));

        form.add(logo);
        form.add(Box.createVerticalStrut(12));
        form.add(title);
        form.add(Box.createVerticalStrut(18));

        usernameField = new HintTextField("Enter your name");
        usernameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        styleInput(usernameField);

        JPanel passRow = new JPanel(new BorderLayout(14, 0));
        passRow.setOpaque(false);
        passRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));

        passwordField = new HintPasswordField("Enter your password");
        styleInput(passwordField);
        passwordField.setEchoChar('•');

        JButton showBtn = new JButton("SHOW");
        showBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        showBtn.setFocusPainted(false);
        showBtn.setBackground(new Color(245, 245, 245));
        showBtn.setForeground(new Color(60, 60, 60));
        showBtn.setBorder(new LineBorder(new Color(210, 210, 210), 1, true));
        showBtn.setPreferredSize(new Dimension(60, 30));
        showBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        final boolean[] shown = {false};
        showBtn.addActionListener(e -> {
            shown[0] = !shown[0];
            passwordField.setEchoChar(shown[0] ? (char) 0 : '•');
            showBtn.setText(shown[0] ? "HIDE" : "SHOW");
        });

        passRow.add(passwordField, BorderLayout.CENTER);
        passRow.add(showBtn, BorderLayout.EAST);

        JButton loginBtn = new JButton("Log in");
        loginBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        loginBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        loginBtn.setForeground(Color.WHITE);
        loginBtn.setBackground(PRIMARY);
        loginBtn.setFocusPainted(false);
        loginBtn.setBorder(new EmptyBorder(6, 18, 6, 18));
        loginBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        loginBtn.setMaximumSize(new Dimension(100, 40));
        loginBtn.addActionListener(e -> doLogin());

        addSmoothHover(loginBtn, PRIMARY, PRIMARY.darker());

        form.add(usernameField);
        form.add(Box.createVerticalStrut(8));
        form.add(passRow);
        form.add(Box.createVerticalStrut(10));
        form.add(loginBtn);

        JLabel infoText = new JLabel(
                "<html><center>Sign in with your account<br>to start your quizzes.</center></html>"
        );
        infoText.setHorizontalAlignment(SwingConstants.CENTER);
        infoText.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        infoText.setForeground(new Color(95, 95, 95));
        infoText.setBorder(new EmptyBorder(8, 12, 22, 12));

        card.add(form, BorderLayout.CENTER);
        card.add(infoText, BorderLayout.SOUTH);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(18, 18, 18, 18);
        root.add(card, gbc);

        SwingUtilities.invokeLater(() -> root.requestFocusInWindow());
        return root;
    }

    private void styleInput(JComponent c) {
        c.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        c.setForeground(TEXT);
        c.setBackground(Color.WHITE);
        c.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDER, 1, true),
                new EmptyBorder(11, 14, 11, 14)
        ));
    }

    private static String cleanName(String s) {
        if (s == null) return "";
        return s.trim().replaceAll("\\s+", " "); // remove extra spaces
    }

    private void doLogin() {
        String user = cleanName(usernameField.getText());
        String pass = new String(passwordField.getPassword()).trim();

        if (user.isEmpty() || pass.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter your name and password.");
            return;
        }

        try {
            // TEACHER LOGIN FIRST
            boolean isTeacher = exam.teacherLogin(user, pass);
            if (isTeacher) {
                isTeacherUser = true;
                currentUser = user;
                setHeaderForLoggedIn(true);

                // open teacher dashboard
                SwingUtilities.invokeLater(() -> {
                    teacherDashboard = new TeacherDashboardGUI(exam, this::doLogout);
                });

                // keep this window on "courses" or stay on login.
                
                loadCoursesToDashboard();
                showPage("courses");
                return;
            }

            // STUDENT LOGIN
            boolean ok = exam.login(user, pass);
            if (!ok) {
                JOptionPane.showMessageDialog(this, "Invalid username or password.");
                return;
            }

            isTeacherUser = false;
            currentUser = user;
            setHeaderForLoggedIn(true);

            loadCoursesToDashboard();
            showPage("courses");

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Server error: " + e.getMessage());
        }
    }

    private void doLogout() {
        stopTimer();
        // close teacher dashboard if open
        if (teacherDashboard != null) {
            teacherDashboard.dispose();
            teacherDashboard = null;
        }
        currentUser = "";
        selectedSubject = "";
        isTeacherUser = false;

        questionsRaw = new ArrayList<>();
        userAnswers = new ArrayList<>();
        index = 0;

        setHeaderForLoggedIn(false);

        if (usernameField != null) usernameField.setText("");
        if (passwordField != null) passwordField.setText("");

        showPage("login");
    }

    // COURSES PAGE
    private JPanel buildCoursesPage() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG);

        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(BG);
        top.setBorder(new EmptyBorder(12, 16, 0, 16));

        JLabel title = new JLabel("My Courses");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(TEXT);

        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        refreshBtn.setBackground(WHITE);
        refreshBtn.setForeground(PRIMARY);
        refreshBtn.setFocusPainted(false);
        refreshBtn.setBorder(new LineBorder(new Color(210, 210, 210), 1, true));
        refreshBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        refreshBtn.addActionListener(e -> loadCoursesToDashboard());

        top.add(title, BorderLayout.WEST);
        top.add(refreshBtn, BorderLayout.EAST);

        coursesListPanel = new JPanel();
        coursesListPanel.setBackground(BG);
        coursesListPanel.setLayout(new BoxLayout(coursesListPanel, BoxLayout.Y_AXIS));
        coursesListPanel.setBorder(new EmptyBorder(10, 16, 16, 16));

        JScrollPane sp = new JScrollPane(coursesListPanel);
        sp.setBorder(null);
        sp.getVerticalScrollBar().setUnitIncrement(18);

        root.add(top, BorderLayout.NORTH);
        root.add(sp, BorderLayout.CENTER);

        return root;
    }

    private void loadCoursesToDashboard() {
        coursesListPanel.removeAll();

        List<String> subjects = getSubjectsSafe();
        if (subjects.isEmpty()) {
            subjects.add("Advanced Artificial Intelligence");
            subjects.add("Parallel and Distributed  Computing");
            subjects.add("Strategies of Emerging Technology I");
        }

        for (String sub : subjects) {
            CourseStats stats = getCourseStats(currentUser, sub);
            JPanel card = createCourseCard(sub, "Computer Science • Online Quiz", subjectToImage(sub), stats);
            coursesListPanel.add(card);
            coursesListPanel.add(Box.createVerticalStrut(12));
        }

        coursesListPanel.revalidate();
        coursesListPanel.repaint();
    }

    private JPanel createCourseCard(String subjectTitle, String subtitle, String imagePath, CourseStats stats) {
        JPanel card = new JPanel(new BorderLayout(18, 0));
        card.setBackground(WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(220, 220, 220), 1, true),
                new EmptyBorder(16, 16, 16, 16)
        ));

        JLabel img = new JLabel();
        img.setPreferredSize(new Dimension(260, 170));
        ImageIcon icon = loadIcon(imagePath, 250, 160);
        if (icon != null) img.setIcon(icon);
        card.add(img, BorderLayout.WEST);

        JPanel mid = new JPanel();
        mid.setLayout(new BoxLayout(mid, BoxLayout.Y_AXIS));
        mid.setBackground(WHITE);

        JLabel t = new JLabel(subjectTitle);
        t.setFont(new Font("Segoe UI", Font.BOLD, 22));
        t.setForeground(TEXT);

        JLabel s = new JLabel(subtitle);
        s.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        s.setForeground(MUTED);

        JLabel scoreLine = new JLabel(stats.hasResult
                ? ("Last Score: " + stats.marks + "/" + stats.total + " (" + stats.percent + "%)")
                : "Last Score: Not submitted yet");
        scoreLine.setFont(new Font("Segoe UI", Font.PLAIN, 17));
        scoreLine.setForeground(stats.hasResult ? new Color(30, 120, 30) : new Color(140, 90, 0));

        mid.add(t);
        mid.add(Box.createVerticalStrut(6));
        mid.add(s);
        mid.add(Box.createVerticalStrut(6));
        mid.add(scoreLine);

        card.add(mid, BorderLayout.CENTER);

        JButton start = new JButton(stats.hasResult ? "Retake" : "Start");
        start.setFont(new Font("Segoe UI", Font.BOLD, 17));
        start.setBackground(PRIMARY);
        start.setForeground(WHITE);
        start.setFocusPainted(false);
        start.setBorder(new EmptyBorder(14, 28, 14, 26));
        start.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        start.addActionListener(e -> {
            if (stats.hasResult) {
                int ok = JOptionPane.showConfirmDialog(this,
                        "You already have a score.\nDo you want to RETAKE this exam?",
                        "Retake Exam", JOptionPane.YES_NO_OPTION);
                if (ok != JOptionPane.YES_OPTION) return;
            }
            startExamForSubject(subjectTitle);
        });

        JPanel right = new JPanel(new GridBagLayout());
        right.setBackground(WHITE);
        right.add(start);

        card.add(right, BorderLayout.EAST);
        return card;
    }

    private String subjectToImage(String subject) {
        String s = subject.toLowerCase();
        if (s.contains("advanced") || s.contains("artificial") || s.contains("ai")) return "/images/ai.png";
        if (s.contains("distributed") || s.contains("parallel")) return "/images/parallel.png";
        if (s.contains("emerging") || s.contains("technology")) return "/images/emerging.png";
        return "/images/default.png";
    }

    // ===================== EXAM PAGE =====================
    private JPanel buildExamPage() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG);

        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(WHITE);
        top.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(220, 220, 220), 1, true),
                new EmptyBorder(10, 16, 10, 16)
        ));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        left.setOpaque(false);

        backToCoursesBtn = new JButton("← Back");
        backToCoursesBtn.setFont(new Font("Segoe UI", Font.BOLD, 16));
        backToCoursesBtn.setBackground(WHITE);
        backToCoursesBtn.setForeground(TEXT);
        backToCoursesBtn.setFocusPainted(false);
        backToCoursesBtn.setBorder(new LineBorder(new Color(210, 210, 210), 1, true));
        backToCoursesBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        backToCoursesBtn.addActionListener(e -> {
            int ok = JOptionPane.showConfirmDialog(this,
                    "Leave the exam and go back to courses?\nTimer will stop.",
                    "Confirm", JOptionPane.YES_NO_OPTION);
            if (ok == JOptionPane.YES_OPTION) {
                stopTimer();
                showPage("courses");
            }
        });

        examSubjectLabel = new JLabel("Subject: ");
        examSubjectLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        examSubjectLabel.setForeground(TEXT);

        qCountLabel = new JLabel("Question 0/0");
        qCountLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        qCountLabel.setForeground(MUTED);

        left.add(backToCoursesBtn);
        left.add(examSubjectLabel);
        left.add(new JLabel(" | "));
        left.add(qCountLabel);

        JPanel right = new JPanel();
        right.setOpaque(false);
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));

        timerLabel = new JLabel("Time: 00:00");
        timerLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        timerLabel.setForeground(PRIMARY);

        timerBar = new JProgressBar();
        timerBar.setMinimum(0);
        timerBar.setMaximum(totalSeconds);
        timerBar.setValue(totalSeconds);
        timerBar.setPreferredSize(new Dimension(260, 12));

        right.add(timerLabel);
        right.add(timerBar);

        top.add(left, BorderLayout.WEST);
        top.add(right, BorderLayout.EAST);

        JPanel centerWrap = new JPanel(new GridBagLayout());
        centerWrap.setBackground(BG);
        centerWrap.setBorder(new EmptyBorder(14, 16, 14, 16));

        JPanel qCard = new JPanel();
        qCard.setLayout(new BoxLayout(qCard, BoxLayout.Y_AXIS));
        qCard.setBackground(WHITE);
        qCard.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(220, 220, 220), 1, true),
                new EmptyBorder(18, 18, 18, 18)
        ));
        qCard.setPreferredSize(new Dimension(650, 320));

        questionArea = new JTextArea();
        questionArea.setFont(new Font("Segoe UI", Font.BOLD, 20));
        questionArea.setForeground(TEXT);
        questionArea.setLineWrap(true);
        questionArea.setWrapStyleWord(true);
        questionArea.setEditable(false);
        questionArea.setOpaque(false);
        questionArea.setAlignmentX(Component.LEFT_ALIGNMENT);

        qCard.add(questionArea);
        qCard.add(Box.createVerticalStrut(3));

        optionGroup = new ButtonGroup();

        optA = makeModernOption();
        optB = makeModernOption();
        optC = makeModernOption();
        optD = makeModernOption();

        optionGroup.add(optA);
        optionGroup.add(optB);
        optionGroup.add(optC);
        optionGroup.add(optD);

        qCard.add(optA);
        qCard.add(Box.createVerticalStrut(5));
        qCard.add(optB);
        qCard.add(Box.createVerticalStrut(5));
        qCard.add(optC);
        qCard.add(Box.createVerticalStrut(5));
        qCard.add(optD);

        GridBagConstraints wrap = new GridBagConstraints();
        wrap.gridx = 0;
        wrap.gridy = 0;
        centerWrap.add(qCard, wrap);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 14));
        bottom.setBackground(BG);

        prevBtn = new JButton("Previous");
        nextBtn = new JButton("Next");
        submitBtn = new JButton("Submit");

        styleExamButton(prevBtn, false);
        styleExamButton(nextBtn, false);
        styleExamButton(submitBtn, true);

        prevBtn.addActionListener(e -> goPrev());
        nextBtn.addActionListener(e -> goNext());
        submitBtn.addActionListener(e -> submitExam(false));

        bottom.add(prevBtn);
        bottom.add(nextBtn);
        bottom.add(submitBtn);

        root.add(top, BorderLayout.NORTH);
        root.add(centerWrap, BorderLayout.CENTER);
        root.add(bottom, BorderLayout.SOUTH);

        return root;
    }

    private JRadioButton makeModernOption() {
        JRadioButton rb = new JRadioButton(" ");
        rb.setOpaque(true);
        rb.setBackground(new Color(250, 250, 250));
        rb.setForeground(TEXT);
        rb.setFont(new Font("Segoe UI", Font.PLAIN, 17));
        rb.setFocusPainted(false);
        rb.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(230, 230, 230), 1, true),
                new EmptyBorder(8, 12, 8, 12)
        ));
        rb.setAlignmentX(Component.LEFT_ALIGNMENT);
        rb.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        rb.addChangeListener(e -> {
            if (rb.isSelected()) {
                rb.setBackground(new Color(235, 245, 245));
                rb.setBorder(BorderFactory.createCompoundBorder(
                        new LineBorder(PRIMARY, 2, true),
                        new EmptyBorder(11, 13, 11, 13)
                ));
            } else {
                rb.setBackground(new Color(250, 250, 250));
                rb.setBorder(BorderFactory.createCompoundBorder(
                        new LineBorder(new Color(230, 230, 230), 1, true),
                        new EmptyBorder(12, 14, 12, 14)
                ));
            }
        });

        return rb;
    }

    private void styleExamButton(JButton b, boolean primary) {
        b.setFont(new Font("Segoe UI", Font.BOLD, 16));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(new EmptyBorder(12, 20, 12, 20));

        if (primary) {
            b.setBackground(PRIMARY);
            b.setForeground(WHITE);
        } else {
            b.setBackground(WHITE);
            b.setForeground(TEXT);
            b.setBorder(new LineBorder(new Color(210, 210, 210), 1, true));
        }
    }

    // RESULT PAGE 
    private JPanel buildResultPage() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG);
        root.setBorder(new EmptyBorder(14, 16, 14, 16));

        JLabel title = new JLabel("Exam Result");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(TEXT);

        resultArea = new JTextArea();
        resultArea.setFont(new Font("Segoe UI", Font.PLAIN, 17));
        resultArea.setEditable(false);
        resultArea.setLineWrap(true);
        resultArea.setWrapStyleWord(true);

        JScrollPane sp = new JScrollPane(resultArea);
        sp.setBorder(new LineBorder(new Color(220, 220, 220), 1, true));

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 10));
        bottom.setBackground(BG);

        JButton backBtn = new JButton("Back to Courses");
        styleExamButton(backBtn, true);
        backBtn.addActionListener(e -> {
            loadCoursesToDashboard();
            showPage("courses");
        });

        bottom.add(backBtn);

        root.add(title, BorderLayout.NORTH);
        root.add(sp, BorderLayout.CENTER);
        root.add(bottom, BorderLayout.SOUTH);

        return root;
    }

    // EXAM FLOW 
    private void startExamForSubject(String subject) {
        selectedSubject = subject.trim();
        examSubjectLabel.setText("Subject: " + selectedSubject);

        totalSeconds = 15 * 60;
        remainingSeconds = totalSeconds;

        loadQuestionsFromServer();

        if (questionsRaw == null || questionsRaw.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "No questions found for:\n" + selectedSubject +
                            "\n\nCheck questions.csv or server console.");
            return;
        }

        userAnswers = new ArrayList<>();
        for (int i = 0; i < questionsRaw.size(); i++) userAnswers.add(' ');

        index = 0;
        showQuestion(index);

        startTimer();
        showPage("exam");
    }

    private void loadQuestionsFromServer() {
        questionsRaw = new ArrayList<>();
        try {
            List<String> qs = exam.getQuestions(selectedSubject.trim());
            if (qs == null || qs.isEmpty()) return;
            questionsRaw = new ArrayList<>(qs);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Cannot load questions.\n\n" + e.getMessage());
        }
    }

    private void showQuestion(int idx) {
        if (idx < 0 || idx >= questionsRaw.size()) return;

        optionGroup.clearSelection();

        String raw = questionsRaw.get(idx);
        String[] lines = raw.split("\n");

        qCountLabel.setText("Question " + (idx + 1) + " / " + questionsRaw.size());

        questionArea.setText(lines.length > 0 ? lines[0] : "Question");
        optA.setText(lines.length > 1 ? lines[1] : "A) ");
        optB.setText(lines.length > 2 ? lines[2] : "B) ");
        optC.setText(lines.length > 3 ? lines[3] : "C) ");
        optD.setText(lines.length > 4 ? lines[4] : "D) ");

        char saved = userAnswers.get(idx);
        if (saved == 'A') optA.setSelected(true);
        else if (saved == 'B') optB.setSelected(true);
        else if (saved == 'C') optC.setSelected(true);
        else if (saved == 'D') optD.setSelected(true);

        prevBtn.setEnabled(idx > 0);
        nextBtn.setEnabled(idx < questionsRaw.size() - 1);
        submitBtn.setEnabled(idx == questionsRaw.size() - 1);
    }

    private void saveCurrentAnswer() {
        if (questionsRaw.isEmpty()) return;
        userAnswers.set(index, getSelected());
    }

    private void goNext() {
        saveCurrentAnswer();
        if (index < questionsRaw.size() - 1) {
            index++;
            showQuestion(index);
        }
    }

    private void goPrev() {
        saveCurrentAnswer();
        if (index > 0) {
            index--;
            showQuestion(index);
        }
    }

    private char getSelected() {
        if (optA.isSelected()) return 'A';
        if (optB.isSelected()) return 'B';
        if (optC.isSelected()) return 'C';
        if (optD.isSelected()) return 'D';
        return ' ';
    }

    private void submitExam(boolean autoTimeout) {
        saveCurrentAnswer();
        stopTimer();

        try {
            exam.submitAnswers(currentUser, selectedSubject, userAnswers);

            String scoreText = exam.getResult(currentUser, selectedSubject);
            String reviewText = exam.getAnswerReview(currentUser, selectedSubject);

            String header = autoTimeout ? "Time is up. Answers were submitted automatically.\n\n" : "";

            resultArea.setText(
                    header +
                            scoreText + "\n\n" +
                            "---------------------------------\n" +
                            "Answer Review\n" +
                            "---------------------------------\n\n" +
                            reviewText
            );

            showPage("result");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Submit failed: " + e.getMessage());
        }
    }

    // ===================== TIMER =====================
    private void startTimer() {
        stopTimer();
        updateTimerUI();

        countdownTimer = new javax.swing.Timer(1000, e -> {
            remainingSeconds--;
            updateTimerUI();
            if (remainingSeconds <= 0) {
                stopTimer();
                submitExam(true);
            }
        });
        countdownTimer.start();
    }

    private void stopTimer() {
        if (countdownTimer != null) {
            countdownTimer.stop();
            countdownTimer = null;
        }
    }

    private void updateTimerUI() {
        int m = Math.max(0, remainingSeconds) / 60;
        int s = Math.max(0, remainingSeconds) % 60;
        timerLabel.setText(String.format("Time: %02d:%02d", m, s));

        timerBar.setMaximum(totalSeconds);
        timerBar.setValue(Math.max(0, remainingSeconds));

        timerLabel.setForeground(remainingSeconds <= 60 ? Color.RED : PRIMARY);
    }

  
    private CourseStats getCourseStats(String username, String subject) {
        CourseStats st = new CourseStats();
        if (username == null || username.isBlank()) return st;

        try {
            String res = exam.getResult(username, subject);
            if (res == null || res.toLowerCase().contains("no result")) return st;

            st.hasResult = true;

            Pattern marksP = Pattern.compile("Marks:\\s*(\\d+)\\s*/\\s*(\\d+)");
            Matcher mm = marksP.matcher(res);
            if (mm.find()) {
                st.marks = Integer.parseInt(mm.group(1));
                st.total = Integer.parseInt(mm.group(2));
            }

            Pattern pctP = Pattern.compile("Percent:\\s*([0-9.]+)");
            Matcher mp = pctP.matcher(res);
            if (mp.find()) st.percent = mp.group(1);

        } catch (Exception ignored) { }

        return st;
    }

    private static class CourseStats {
        boolean hasResult = false;
        int marks = 0;
        int total = 0;
        String percent = "0.00";
    }

    // ===================== UTIL =====================
    private List<String> getSubjectsSafe() {
        try {
            List<String> s = exam.getSubjects();
            return (s == null) ? new ArrayList<>() : new ArrayList<>(s);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private ImageIcon loadIcon(String path, int w, int h) {
        try {
            java.net.URL url = getClass().getResource(path);
            if (url == null) return null;
            ImageIcon icon = new ImageIcon(url);
            Image img = icon.getImage().getScaledInstance(w, h, Image.SCALE_SMOOTH);
            return new ImageIcon(img);
        } catch (Exception e) {
            return null;
        }
    }

    private BufferedImage loadBufferedImage(String path) {
        try {
            java.net.URL url = getClass().getResource(path);
            if (url == null) return null;
            return javax.imageio.ImageIO.read(url);
        } catch (Exception e) {
            return null;
        }
    }

    private ImageIcon makeInitialsAvatar(String name, int size) {
        String initials = "?";
        try {
            String n = (name == null) ? "" : name.trim();
            if (!n.isEmpty()) initials = ("" + Character.toUpperCase(n.charAt(0)));
        } catch (Exception ignored) { }

        BufferedImage bi = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = bi.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setColor(new Color(255, 255, 255, 40));
        g2.fillOval(0, 0, size, size);

        g2.setColor(new Color(255, 255, 255, 220));
        g2.setStroke(new BasicStroke(2));
        g2.drawOval(1, 1, size - 3, size - 3);

        g2.setFont(new Font("Segoe UI", Font.BOLD, size / 2));
        FontMetrics fm = g2.getFontMetrics();
        int x = (size - fm.stringWidth(initials)) / 2;
        int y = (size - fm.getHeight()) / 2 + fm.getAscent();

        g2.setColor(Color.WHITE);
        g2.drawString(initials, x, y);

        g2.dispose();
        return new ImageIcon(bi);
    }

    private void showPage(String name) {
        cardLayout.show(cards, name);
    }

    // ===================== MAIN =====================
    public static void main(String[] args) {
        SwingUtilities.invokeLater(ExamClientGUI::new);
    }

       static class GlassCard extends JPanel {
        private final JComponent root;
        private final BufferedImage blurredBg;

        GlassCard(JComponent root, BufferedImage blurredBg) {
            this.root = root;
            this.blurredBg = blurredBg;
            setOpaque(false);
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            int arc = 28;

            g2.setColor(new Color(0, 0, 0, 90));
            g2.fillRoundRect(10, 12, w - 20, h - 18, arc, arc);

            Shape clip = new RoundRectangle2D.Double(0, 0, w, h, arc, arc);
            g2.setClip(clip);

            if (blurredBg != null && root != null && root.getWidth() > 0 && root.getHeight() > 0) {
                Point p = SwingUtilities.convertPoint(this, 0, 0, root);
                BufferedImage scaled = scaleTo(blurredBg, root.getWidth(), root.getHeight());
                if (scaled != null) g2.drawImage(scaled, -p.x, -p.y, null);
            } else {
                g2.setColor(new Color(255, 255, 255, 170));
                g2.fillRect(0, 0, w, h);
            }

            g2.setColor(new Color(255, 255, 255, 165));
            g2.fillRoundRect(0, 0, w, h, arc, arc);

            g2.setColor(new Color(255, 255, 255, 160));
            g2.setStroke(new BasicStroke(2f));
            g2.drawRoundRect(1, 1, w - 3, h - 3, arc, arc);

            g2.dispose();
            super.paintComponent(g);
        }

        private static BufferedImage scaleTo(BufferedImage src, int w, int h) {
            if (src == null || w <= 0 || h <= 0) return null;
            BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = out.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.drawImage(src, 0, 0, w, h, null);
            g2.dispose();
            return out;
        }
    }

    private void addSmoothHover(JButton btn, Color base, Color hover) {
        btn.setBackground(base);
        btn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { btn.setBackground(hover); }
            @Override public void mouseExited(MouseEvent e) { btn.setBackground(base); }
        });
    }

    private BufferedImage blurImage(BufferedImage src, int radius) {
        if (src == null || radius < 1) return src;

        int size = radius * 2 + 1;
        float sigma = radius / 3f;
        float[] data = new float[size * size];
        float sum = 0f;
        int idx = 0;

        for (int y = -radius; y <= radius; y++) {
            for (int x = -radius; x <= radius; x++) {
                float v = (float) Math.exp(-(x * x + y * y) / (2 * sigma * sigma));
                data[idx++] = v;
                sum += v;
            }
        }
        for (int i = 0; i < data.length; i++) data[i] /= sum;

        java.awt.image.Kernel kernel = new java.awt.image.Kernel(size, size, data);
        java.awt.image.ConvolveOp op =
                new java.awt.image.ConvolveOp(kernel, java.awt.image.ConvolveOp.EDGE_NO_OP, null);

        BufferedImage dst = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = dst.createGraphics();
        g2.drawImage(src, 0, 0, null);
        g2.dispose();

        return op.filter(dst, null);
    }

    // ===================== PLACEHOLDER FIELDS =====================
    static class HintTextField extends JTextField {
        private final String hint;
        HintTextField(String hint) { this.hint = hint; }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (getText().isEmpty() && !isFocusOwner()) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new Color(150, 150, 150));
                g2.setFont(getFont());
                Insets in = getInsets();
                g2.drawString(hint, in.left + 2,
                        getHeight() / 2 + g2.getFontMetrics().getAscent() / 2 - 2);
                g2.dispose();
            }
        }
    }
    

    static class HintPasswordField extends JPasswordField {
        private final String hint;
        HintPasswordField(String hint) { this.hint = hint; }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (getPassword().length == 0 && !isFocusOwner()) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new Color(150, 150, 150));
                g2.setFont(getFont());
                Insets in = getInsets();
                g2.drawString(hint, in.left + 2,
                        getHeight() / 2 + g2.getFontMetrics().getAscent() / 2 - 2);
                g2.dispose();
            }
        }
    }
}  