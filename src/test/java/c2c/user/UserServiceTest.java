package c2c.user;

import c2c.common.exceptions.NotFoundException;
import c2c.common.exceptions.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mockStatic;

@DisplayName("UserService 測試")
class UserServiceTest {

    static {
        System.setProperty("net.bytebuddy.experimental", "true");
    }

    private UserRepository repo;
    private UserService service;

    @BeforeEach
    void setUp() throws Exception {
        java.lang.reflect.Field seqField = Class.forName("c2c.common.IdGenerator").getDeclaredField("SEQ");
        seqField.setAccessible(true);
        java.util.concurrent.atomic.AtomicLong seq = (java.util.concurrent.atomic.AtomicLong) seqField.get(null);
        seq.set(1);

        repo = new InMemoryUserRepository();
        service = new DefaultUserService(repo, new SimplePasswordHasher());
    }

    // ==========================================
    // 1.1 Register 功能測試 (US-R-001 ~ 010)
    // ==========================================
    
    @Nested
    @DisplayName("Register 功能測試")
    class RegisterTests {

        @Test
        @DisplayName("US-R-001: 註冊成功 - 完整資訊含電話")
        void register_success_with_full_info() {
        User user = service.register("valid@example.com", "0912345678", "validPwd", "Alice").getData();

        assertNotNull(user);
        assertNotNull(user.getId());
        assertEquals("valid@example.com", user.getEmail());
        assertEquals("0912345678", user.getPhone());
        assertEquals("Alice", user.getName());
        assertEquals(Set.of("BUYER"), user.getRoles());
        assertEquals(UserStatus.ACTIVE, user.getStatus());
        assertTrue(repo.existsByEmail("valid@example.com"));

        user.setEmail("test@example.com");
        assertEquals("test@example.com", user.getEmail());

        user.setHashedPassword("1234567890hashed");
        assertEquals("1234567890hashed", user.getHashedPassword());

        assertNotNull(user.getCreatedAt());

        assertEquals(java.util.Set.of("BUYER"), user.getRoles());

        java.util.List<User> allUsers = repo.findAll();
        assertEquals(1, allUsers.size());
        assertEquals("Alice", allUsers.get(0).getName());

        assertTrue(user.equals(user));
        assertFalse(user.equals(null));
        assertFalse(user.equals("這是一個字串，不是User物件"));
        assertFalse(user.equals(new User("test-id",
                "test@test.com",
                "0900000000",
                "hashed",
                "Name",
                null,
                UserStatus.ACTIVE,
                java.time.Instant.now())));
    }

    @Test
    @DisplayName("US-R-002: 註冊成功 - 無電話號碼")
    void register_success_without_phone() {
        User user = service.register("valid@example.com", null, "validPwd", "Alice").getData();
        assertNotNull(user);
        assertNull(user.getPhone());
        assertFalse(repo.existsByPhone(null));
        assertTrue(repo.existsByEmail("valid@example.com"));
    }

    @Test
    @DisplayName("US-R-003: 註冊失敗 - email為null")
    void register_fail_email_null() {
        assertThrows(ValidationException.class, () ->
                service.register(null, "0912345678", "validPwd", "Alice")
        );
    }

    @Test
    @DisplayName("US-R-004: 註冊失敗 - email為空字串")
    void register_fail_email_empty() {
        assertThrows(ValidationException.class, () ->
                service.register("", "0912345678", "validPwd", "Alice")
        );
    }

    @Test
    @DisplayName("US-R-005: 註冊失敗 - email為空白字串")
    void register_fail_email_blank() {
        assertThrows(ValidationException.class, () ->
                service.register("   ", "0912345678", "validPwd", "Alice")
        );
    }

    @Test
    @DisplayName("US-R-006: 註冊失敗 - password為null")
    void register_fail_password_null() {
        assertThrows(ValidationException.class, () ->
                service.register("valid@example.com", "0912345678", null, "Alice")
        );
    }

    @Test
    @DisplayName("US-R-007: 註冊失敗 - password為空字串")
    void register_fail_password_empty() {
        assertThrows(ValidationException.class, () ->
                service.register("valid@example.com", "0912345678", "", "Alice")
        );
    }

    @Test
    @DisplayName("US-R-008: 註冊失敗 - email重複")
    void register_fail_email_duplicate() {
        service.register("existing@example.com", "0912345678", "pwd1", "User1");
        assertThrows(c2c.common.exceptions.ConflictException.class, () ->
                service.register("existing@example.com", "0987654321", "pwd2", "User2")
        );
    }

    @Test
    @DisplayName("US-R-009: 註冊失敗 - phone重複")
    void register_fail_phone_duplicate() {
        service.register("user1@example.com", "0912345678", "pwd1", "User1");
        assertThrows(c2c.common.exceptions.ConflictException.class, () ->
                service.register("user2@example.com", "0912345678", "pwd2", "User2")
        );
    }

        @Test
        @DisplayName("US-R-010: 註冊成功 - 特殊字元email")
        void register_success_special_char_email() {
            String specialEmail = "test+tag@example.com";
            User user = service.register(specialEmail, null, "validPwd", "Alice").getData();
            assertEquals(specialEmail, user.getEmail());
        }
    }

    // ==========================================
    // 1.2 Login 功能測試
    // ==========================================
    
    @Nested
    @DisplayName("Login 功能測試")
    class LoginTests {

        @Test
        @DisplayName("US-L-001: 登入成功 - 正確憑證")
        void login_success() {
        String email = "alice@example.com";
        String password = "securePassword";
        User registered = service.register(email, null, password, "Alice").getData();

        User loggedIn = service.login(email, password).getData();

        assertNotNull(loggedIn);
        assertEquals(registered.getId(), loggedIn.getId());
    }

    @Test
    @DisplayName("US-L-002: 登入失敗 - 用戶不存在")
    void login_fail_user_not_found() {
        NotFoundException exception = assertThrows(NotFoundException.class, () ->
                service.login("ghost@example.com", "anyPwd")
        );
        assertEquals("user not found", exception.getMessage());
    }

    @Test
    @DisplayName("US-L-003: 登入失敗 - 用戶被停用")
    void login_fail_user_disabled() {
        String email = "banned@example.com";
        String password = "pwd";
        User user = service.register(email, null, password, "BannedUser").getData();

        user.setStatus(UserStatus.DISABLED);
        repo.save(user);

        ValidationException exception = assertThrows(ValidationException.class, () ->
                service.login(email, password)
        );
        assertEquals("user disabled", exception.getMessage());
    }

    @Test
    @DisplayName("US-L-004: 登入失敗 - 密碼錯誤")
    void login_fail_wrong_password() {
        String email = "bob@example.com";
        service.register(email, null, "correctPwd", "Bob");

        ValidationException exception = assertThrows(ValidationException.class, () ->
                service.login(email, "wrongPwd")
        );
        assertEquals("invalid credentials", exception.getMessage());
    }
    }

    // ==========================================
    // 1.3 UpdateProfile 功能測試
    // ==========================================
    
    @Nested
    @DisplayName("UpdateProfile 功能測試")
    class UpdateProfileTests {

        @Test
        @DisplayName("US-U-001: 更新成功 - 修改name和phone")
        void update_profile_success() {
        User original = service.register("charlie@example.com", "0912345678", "pwd", "Charlie").getData();
        String userId = original.getId();

        User updated = service.updateProfile(userId, "Charlie New", "0987654321").getData();

        assertEquals("Charlie New", updated.getName());
        assertEquals("0987654321", updated.getPhone());

        User fromDb = repo.findById(userId).orElseThrow();
        assertEquals("Charlie New", fromDb.getName());
    }

    @Test
    @DisplayName("US-U-002: 更新失敗 - 用戶不存在")
    void update_profile_fail_user_not_found() {
        NotFoundException exception = assertThrows(NotFoundException.class, () ->
                service.updateProfile("invalid-id-123", "New Name", "0911111111")
        );
        assertEquals("user not found", exception.getMessage());
    }
    }
}