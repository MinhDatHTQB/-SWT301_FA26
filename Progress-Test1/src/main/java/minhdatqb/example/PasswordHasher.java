package minhdatqb.example;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;

public final class PasswordHasher {

    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordHasher() {
        // Ngăn khởi tạo đối tượng
    }

    public static String generateSalt() {
        byte[] saltBytes = new byte[16];
        RANDOM.nextBytes(saltBytes);
        return HexFormat.of().formatHex(saltBytes);
    }

    public static String hash(String salt, String rawPassword) {
        if (salt == null || rawPassword == null) return null;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(md.digest(rawPassword.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public static boolean matches(String salt, String rawPassword, String storedHash) {
        if (salt == null || rawPassword == null || storedHash == null) {
            return false;
        }
        String computedHash = hash(salt, rawPassword);
        return computedHash != null && computedHash.equals(storedHash);
    }
}