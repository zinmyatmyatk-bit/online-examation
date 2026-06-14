package ExamInterface;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface ExamInterface extends Remote {

    // ===== Student =====
    boolean login(String username, String password) throws RemoteException;

    boolean registerStudent(String username, String email, String password)
            throws RemoteException;

    List<String> getSubjects() throws RemoteException;

    List<String> getQuestions(String subject) throws RemoteException;

    int submitAnswers(String username, String subject, List<Character> answers)
            throws RemoteException;

    String getResult(String username, String subject)
            throws RemoteException;

    // show right / wrong answers
    String getAnswerReview(String username, String subject)
            throws RemoteException;

    // ===== Teacher =====
    boolean teacherLogin(String name, String password) throws RemoteException;

    List<Result> getAllResults() throws RemoteException;

    List<Result> searchResultsBySubject(String subjectQuery) throws RemoteException;

    List<Result> searchResultsByStudent(String studentQuery) throws RemoteException;

    int getAttemptCount(String username) throws RemoteException;

    // Teacher adds question to SERVER questions.csv
    boolean addQuestion(String subject, String question,
                        String A, String B, String C, String D,
                        char correct) throws RemoteException;

    // Reload questions if teacher edited Excel CSV
    boolean reloadQuestionsFromCsv() throws RemoteException;

    // NEW for edit/delete questions
    List<QuestionRow> getAllQuestions() throws RemoteException;

    boolean updateQuestion(int id, String subject, String question,
                           String A, String B, String C, String D,
                           char correct) throws RemoteException;

    boolean deleteQuestion(int id) throws RemoteException;

    // Teacher: delete
    // Delete one result row 
    boolean deleteResult(String username, String subject, String submittedAt) throws RemoteException;

    // Delete ALL results (clears results.csv)
    boolean clearAllResults() throws RemoteException;

}
