import java.util.regex.Pattern;

    public class AccountService {
        private static final Pattern EMAIL_PATTERN =
                Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za z]{2,}$");

        public boolean isValidEmail(String email) {
            if (email == null || email.isBlank()) {
                return false;
            }
            return EMAIL_PATTERN.matcher(email).matches();
        }
        public boolean registerAccount(String username, String password, String
                email) {
            if (username == null || username.isBlank()) return false;
            if (password == null || password.length() <= 6) return false;
            return isValidEmail(email);
// (Demo) — bỏ qua phần lưu DB
        }
    }

