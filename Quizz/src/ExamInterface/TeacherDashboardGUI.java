package ExamInterface;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.DecimalFormat;
import java.util.List;

public class TeacherDashboardGUI extends JFrame {

    private final ExamInterface exam;

    private final Runnable onLogout;

    private final Color theme = Color.decode("#0c3635");
    private final Color bgWhite = Color.WHITE;

    // ===== Results tab =====
    private JTable resultsTable;
    private DefaultTableModel resultsModel;
    private JLabel resultsStatusLabel;
    private JLabel resultsInfoLabel;
    private JTextField searchField;
    private JComboBox<String> searchType;
    private static final String[] RESULT_COLS =
            {"Username", "Subject", "Marks", "Total", "Percent", "Submitted At"};
    private static final DecimalFormat DF = new DecimalFormat("0.00");

    // Questions tab
    private JTable questionsTable;
    private DefaultTableModel questionsModel;
    private JLabel questionsStatusLabel;
    private static final String[] QUESTION_COLS =
            {"ID", "Subject", "Question", "A", "B", "C", "D", "Correct"};

    public TeacherDashboardGUI(ExamInterface exam, Runnable onLogout) {
        super("Teacher Dashboard");
        this.exam = exam;
        this.onLogout = onLogout;

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(1250, 700);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        add(buildHeader(), BorderLayout.NORTH);
        add(buildTabs(), BorderLayout.CENTER);

        // initial loads
        loadAllResults();
        loadAllQuestions();

        setVisible(true);
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(theme);
        header.setBorder(new EmptyBorder(14, 18, 14, 18));

        JLabel title = new JLabel("Teacher Dashboard");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));

        header.add(title, BorderLayout.WEST);

        JButton logoutBtn = new JButton("Logout");
        logoutBtn.setBackground(Color.WHITE);
        logoutBtn.setForeground(theme);
        logoutBtn.setFocusPainted(false);
        logoutBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        logoutBtn.addActionListener(e -> {
            // client to return to login and close this window
            if (onLogout != null) onLogout.run();
            dispose();
        });
        header.add(logoutBtn, BorderLayout.EAST);
        return header;
    }

    private JTabbedPane buildTabs() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Results", buildResultsTab());
        tabs.addTab("Questions", buildQuestionsTab());
        return tabs;
    }

    // ===================== RESULTS TAB =====================
    private JPanel buildResultsTab() {
        JPanel main = new JPanel(new BorderLayout(10, 10));
        main.setBackground(bgWhite);
        main.setBorder(new EmptyBorder(12, 12, 12, 12));

        main.add(buildResultsTopBar(), BorderLayout.NORTH);
        main.add(buildResultsTablePanel(), BorderLayout.CENTER);
        main.add(buildResultsBottomBar(), BorderLayout.SOUTH);

        return main;
    }

    private JPanel buildResultsTopBar() {
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        top.setBackground(bgWhite);
        top.setBorder(new LineBorder(theme, 2));

        JButton refreshBtn = mkBtn("Refresh");
        JButton searchBtn = mkBtn("Search");
     
        JButton deleteSelectedBtn = mkBtn("Delete Selected");
        JButton deleteAllBtn = mkBtn("Delete All");
        JButton printBtn = mkBtn("Print");

        searchType = new JComboBox<>(new String[]{"Subject", "Student"});
        searchField = new JTextField(22);

        top.add(refreshBtn);
        top.add(new JLabel("Search by:"));
        top.add(searchType);
        top.add(searchField);
        top.add(searchBtn);
    
        top.add(deleteSelectedBtn);
        top.add(deleteAllBtn);
        top.add(printBtn);

        refreshBtn.addActionListener(e -> loadAllResults());
        searchBtn.addActionListener(e -> doResultSearch());
        
        deleteSelectedBtn.addActionListener(e -> deleteSelectedResult());
        deleteAllBtn.addActionListener(e -> deleteAllResults());
        printBtn.addActionListener(e -> printResults());

        return top;
    }

    private JScrollPane buildResultsTablePanel() {
        resultsModel = new DefaultTableModel(RESULT_COLS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        resultsTable = new JTable(resultsModel);
        resultsTable.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        resultsTable.setRowHeight(26);

        resultsTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 14));
        resultsTable.getTableHeader().setBackground(theme);
        resultsTable.getTableHeader().setForeground(Color.WHITE);

        // color highlight by percent
        resultsTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable t, Object value, boolean isSelected,
                    boolean hasFocus, int row, int col) {

                Component c = super.getTableCellRendererComponent(
                        t, value, isSelected, hasFocus, row, col);

                if (!isSelected) {
                    double pct = Double.parseDouble(resultsModel.getValueAt(row, 4).toString());
                    if (pct >= 80) c.setBackground(new Color(220, 255, 220));
                    else if (pct < 50) c.setBackground(new Color(255, 235, 235));
                    else c.setBackground(Color.WHITE);
                }
                return c;
            }
        });

        JScrollPane sp = new JScrollPane(resultsTable);
        sp.setBorder(new LineBorder(theme, 2));
        return sp;
    }

    private JPanel buildResultsBottomBar() {
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBackground(bgWhite);
        bottom.setBorder(new LineBorder(theme, 2));

        resultsStatusLabel = new JLabel("Loaded: 0");
        resultsInfoLabel = new JLabel(" ");

        resultsStatusLabel.setBorder(new EmptyBorder(8, 12, 4, 12));
        resultsInfoLabel.setBorder(new EmptyBorder(0, 12, 8, 12));

        bottom.add(resultsStatusLabel, BorderLayout.WEST);
        bottom.add(resultsInfoLabel, BorderLayout.CENTER);
        return bottom;
    }

    private void loadAllResults() {
        try {
            List<Result> results = exam.getAllResults();
            fillResultsTable(results);
            resultsInfoLabel.setText("All results loaded.");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Server error: " + e.getMessage());
        }
    }

    private void doResultSearch() {
        String q = searchField.getText().trim();
        if (q.isEmpty()) {
            loadAllResults();
            return;
        }

        try {
            String type = (String) searchType.getSelectedItem();
            List<Result> results;

            if ("Subject".equals(type)) {
                results = exam.searchResultsBySubject(q);
                fillResultsTable(results);
                resultsInfoLabel.setText("Found " + results.size() + " record(s) where subject contains: " + q);
            } else {
                results = exam.searchResultsByStudent(q);
                fillResultsTable(results);

                // attempt count for exact username
                int attempts = exam.getAttemptCount(q);
                resultsInfoLabel.setText("Found " + results.size() + " record(s). Attempts for '" + q + "': " + attempts);
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Server error: " + e.getMessage());
        }
    }

    private void fillResultsTable(List<Result> results) {
        resultsModel.setRowCount(0);
        for (Result r : results) {
            resultsModel.addRow(new Object[]{
                    r.username, r.subject, r.marks, r.total,
                    DF.format(r.percent), r.submittedAt
            });
        }
        resultsStatusLabel.setText("Loaded: " + results.size());
    }

    private void deleteSelectedResult() {
        int row = resultsTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a result row first.");
            return;
        }

        String username = resultsModel.getValueAt(row, 0).toString();
        String subject = resultsModel.getValueAt(row, 1).toString();
        String submittedAt = resultsModel.getValueAt(row, 5).toString();

        int ok = JOptionPane.showConfirmDialog(
        	    this,
        	    "Delete this result?"
        	    +
        	    "Student: " + username +  ""
        	    +
        	    "Subject: " + subject + " "
        	    +
        	    "Submitted: " + submittedAt,
        	    "Confirm Delete",
        	    JOptionPane.YES_NO_OPTION
        	);
        if (ok != JOptionPane.YES_OPTION) return;

        try {
            boolean done = exam.deleteResult(username, subject, submittedAt);
            if (!done) {
                JOptionPane.showMessageDialog(this, "Delete failed (record not found on server).");
                return;
            }
            loadAllResults();
            resultsInfoLabel.setText("Deleted 1 record.");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Server error: " + e.getMessage());
        }
    }

    private void deleteAllResults() {
        int ok = JOptionPane.showConfirmDialog(
                this,
                "Delete ALL results? This cannot be undone.",
                "Confirm Delete All",
                JOptionPane.YES_NO_OPTION
        );
        if (ok != JOptionPane.YES_OPTION) return;

        try {
            boolean done = exam.clearAllResults();
            JOptionPane.showMessageDialog(this, done ? "All results deleted." : "Delete all failed.");
            loadAllResults();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Server error: " + e.getMessage());
        }
    }

    private void printResults() {
        try {
            // built-in Swing printing dialog
            boolean ok = resultsTable.print(JTable.PrintMode.FIT_WIDTH,
                    new java.text.MessageFormat("Online Quiz System - Results"),
                    new java.text.MessageFormat("Page {0}")
            );
            if (ok) resultsInfoLabel.setText("Print job sent.");
            else resultsInfoLabel.setText("Print cancelled.");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Print error: " + e.getMessage());
        }
    }

    // ===================== QUESTIONS TAB =====================
    private JPanel buildQuestionsTab() {
        JPanel main = new JPanel(new BorderLayout(10, 10));
        main.setBackground(bgWhite);
        main.setBorder(new EmptyBorder(12, 12, 12, 12));

        main.add(buildQuestionsTopBar(), BorderLayout.NORTH);
        main.add(buildQuestionsTablePanel(), BorderLayout.CENTER);
        main.add(buildQuestionsBottomBar(), BorderLayout.SOUTH);

        return main;
    }

    private JPanel buildQuestionsTopBar() {
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        top.setBackground(bgWhite);
        top.setBorder(new LineBorder(theme, 2));

        JButton refreshBtn = mkBtn("Refresh Questions");
        JButton addBtn = mkBtn("Add Question");
        JButton editBtn = mkBtn("Edit Selected");
        JButton deleteBtn = mkBtn("Delete Selected");
        top.add(refreshBtn);
        top.add(addBtn);
        top.add(editBtn);
        top.add(deleteBtn);
        refreshBtn.addActionListener(e -> loadAllQuestions());
        addBtn.addActionListener(e -> openAddQuestionDialog());
        editBtn.addActionListener(e -> openEditQuestionDialog());
        deleteBtn.addActionListener(e -> deleteSelectedQuestion());
        return top;
    }

    private JScrollPane buildQuestionsTablePanel() {
        questionsModel = new DefaultTableModel(QUESTION_COLS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        questionsTable = new JTable(questionsModel);
        questionsTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        questionsTable.setRowHeight(24);

        questionsTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        questionsTable.getTableHeader().setBackground(theme);
        questionsTable.getTableHeader().setForeground(Color.WHITE);

        JScrollPane sp = new JScrollPane(questionsTable);
        sp.setBorder(new LineBorder(theme, 2));
        return sp;
    }

    private JPanel buildQuestionsBottomBar() {
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBackground(bgWhite);
        bottom.setBorder(new LineBorder(theme, 2));

        questionsStatusLabel = new JLabel("Questions loaded: 0");
        questionsStatusLabel.setBorder(new EmptyBorder(8, 12, 8, 12));

        bottom.add(questionsStatusLabel, BorderLayout.WEST);
        return bottom;
    }

    private void loadAllQuestions() {
        try {
            List<QuestionRow> list = exam.getAllQuestions();
            fillQuestionsTable(list);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Server error: " + e.getMessage());
        }
    }

    private void fillQuestionsTable(List<QuestionRow> list) {
        questionsModel.setRowCount(0);
        for (QuestionRow q : list) {
            questionsModel.addRow(new Object[]{
                    q.id, q.subject, q.question, q.A, q.B, q.C, q.D, String.valueOf(q.correct)
            });
        }
        questionsStatusLabel.setText("Questions loaded: " + list.size());
    }

    private void openAddQuestionDialog() {
        QuestionForm f = new QuestionForm(this, "Add New Question", null);
        f.setVisible(true);
        if (!f.saved) return;

        try {
            boolean ok = exam.addQuestion(
                    f.subject, f.question, f.A, f.B, f.C, f.D, f.correct
            );
            JOptionPane.showMessageDialog(this, ok ? "Saved to questions.csv (server)." : "Failed to save.");
            loadAllQuestions();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Server error: " + ex.getMessage());
        }
    }

    private void openEditQuestionDialog() {
        int row = questionsTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a question row first.");
            return;
        }

        int id = Integer.parseInt(questionsModel.getValueAt(row, 0).toString());
        QuestionRow current = new QuestionRow(
                id,
                questionsModel.getValueAt(row, 1).toString(),
                questionsModel.getValueAt(row, 2).toString(),
                questionsModel.getValueAt(row, 3).toString(),
                questionsModel.getValueAt(row, 4).toString(),
                questionsModel.getValueAt(row, 5).toString(),
                questionsModel.getValueAt(row, 6).toString(),
                questionsModel.getValueAt(row, 7).toString().charAt(0)
        );

        QuestionForm f = new QuestionForm(this, "Edit Question (ID " + id + ")", current);
        f.setVisible(true);
        if (!f.saved) return;

        try {
            boolean ok = exam.updateQuestion(
                    id, f.subject, f.question, f.A, f.B, f.C, f.D, f.correct
            );
            JOptionPane.showMessageDialog(this, ok ? "Updated successfully." : "Update failed.");
            loadAllQuestions();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Server error: " + ex.getMessage());
        }
    }

    private void deleteSelectedQuestion() {
        int row = questionsTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a question row first.");
            return;
        }

        int id = Integer.parseInt(questionsModel.getValueAt(row, 0).toString());

        int ok = JOptionPane.showConfirmDialog(
                this,
                "Delete Question ID " + id + " ?",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION
        );
        if (ok != JOptionPane.YES_OPTION) return;

        try {
            boolean done = exam.deleteQuestion(id);
            JOptionPane.showMessageDialog(this, done ? "Deleted." : "Delete failed.");
            loadAllQuestions();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Server error: " + ex.getMessage());
        }
    }

   
    private JButton mkBtn(String text) {
        JButton b = new JButton(text);
        b.setBackground(theme);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    // ===== Small dialog class for add/edit form =====
    private static class QuestionForm extends JDialog {
        boolean saved = false;

        String subject;
        String question;
        String A, B, C, D;
        char correct = 'A';

        JTextField subjectField = new JTextField();
        JTextArea questionArea = new JTextArea(3, 35);
        JTextField fieldA = new JTextField();
        JTextField fieldB = new JTextField();
        JTextField fieldC = new JTextField();
        JTextField fieldD = new JTextField();
        JComboBox<String> correctBox = new JComboBox<>(new String[]{"A","B","C","D"});

        QuestionForm(JFrame owner, String title, QuestionRow preset) {
            super(owner, title, true);

            setSize(520, 420);
            setLocationRelativeTo(owner);
            setLayout(new BorderLayout(10, 10));

            questionArea.setLineWrap(true);
            questionArea.setWrapStyleWord(true);

            JPanel p = new JPanel(new GridLayout(0, 1, 6, 6));
            p.setBorder(new EmptyBorder(10, 10, 10, 10));

            p.add(new JLabel("Subject:"));
            p.add(subjectField);
            p.add(new JLabel("Question:"));
            p.add(new JScrollPane(questionArea));
            p.add(new JLabel("Option A:"));
            p.add(fieldA);
            p.add(new JLabel("Option B:"));
            p.add(fieldB);
            p.add(new JLabel("Option C:"));
            p.add(fieldC);
            p.add(new JLabel("Option D:"));
            p.add(fieldD);
            p.add(new JLabel("Correct Answer:"));
            p.add(correctBox);

            if (preset != null) {
                subjectField.setText(preset.subject);
                questionArea.setText(preset.question);
                fieldA.setText(preset.A);
                fieldB.setText(preset.B);
                fieldC.setText(preset.C);
                fieldD.setText(preset.D);
                correctBox.setSelectedItem(String.valueOf(preset.correct).toUpperCase());
            }

            JButton saveBtn = new JButton("Save");
            JButton cancelBtn = new JButton("Cancel");

            JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            btns.add(cancelBtn);
            btns.add(saveBtn);

            add(p, BorderLayout.CENTER);
            add(btns, BorderLayout.SOUTH);

            cancelBtn.addActionListener(e -> dispose());
            saveBtn.addActionListener(e -> {
                subject = subjectField.getText().trim();
                question = questionArea.getText().trim();
                A = fieldA.getText().trim();
                B = fieldB.getText().trim();
                C = fieldC.getText().trim();
                D = fieldD.getText().trim();
                correct = correctBox.getSelectedItem().toString().charAt(0);

                if (subject.isEmpty() || question.isEmpty() || A.isEmpty() || B.isEmpty() || C.isEmpty() || D.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Please fill all fields.");
                    return;
                }

                saved = true;
                dispose();
            });
        }
    }
}