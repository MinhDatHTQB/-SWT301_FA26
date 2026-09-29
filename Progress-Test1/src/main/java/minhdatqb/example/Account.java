package minhdatqb.example;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Account {
    private String username;
    private String email;
    private LocalDate dateOfBirth;
    private String phone;
    private String salt;
    private AccountStatus status;
    private int failedAttempts;
    private boolean locked;
    private List<String> passwordHistory;

    public Account(String username, String email, LocalDate dateOfBirth, String phone,
                   String salt, String initialPasswordHash, AccountStatus status) {
        this.username = username;
        this.email = email;
        this.dateOfBirth = dateOfBirth;
        this.phone = phone;
        this.salt = salt;
        this.status = status;
        this.failedAttempts = 0;
        this.locked = false;
        this.passwordHistory = new ArrayList<>();
        if (initialPasswordHash != null) {
            this.passwordHistory.add(initialPasswordHash);
        }
    }

    // Public Getters
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public String getPhone() { return phone; }
    public String getSalt() { return salt; }
    public AccountStatus getStatus() { return status; }
    public int getFailedAttempts() { return failedAttempts; }
    public boolean isLocked() { return locked; }

    public String getCurrentPasswordHash() {
        if (passwordHistory == null || passwordHistory.isEmpty()) return null;
        return passwordHistory.get(passwordHistory.size() - 1);
    }

    public List<String> getPasswordHistory() {
        return List.copyOf(passwordHistory);
    }

    // Package-private state changers
    void incrementFailedAttempts() { failedAttempts++; }
    void resetFailedAttempts() { failedAttempts = 0; }
    void lock() { locked = true; }
    void unlock() { locked = false; failedAttempts = 0; }
    void setStatus(AccountStatus s) { status = s; }
    void setSalt(String salt) { this.salt = salt; }
    void addPasswordHash(String hash) {
        if (this.passwordHistory == null) {
            this.passwordHistory = new ArrayList<>();
        }
        this.passwordHistory.add(hash);
    }
}