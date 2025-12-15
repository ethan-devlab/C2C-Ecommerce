package c2c.user;

import c2c.common.exceptions.ConflictException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserServiceTest {

    @Test
    void registerAndLoginHappyPath() {
        UserRepository repo = new InMemoryUserRepository();
        UserService service = new DefaultUserService(repo, new SimplePasswordHasher());

        User user = service.register("a@example.com", null, "pwd", "Alice").getData();
        assertNotNull(user.getId());

        User logged = service.login("a@example.com", "pwd").getData();
        assertEquals(user.getId(), logged.getId());
    }

    @Test
    void duplicateEmailFails() {
        UserRepository repo = new InMemoryUserRepository();
        UserService service = new DefaultUserService(repo, new SimplePasswordHasher());
        service.register("dup@example.com", null, "pwd", "Dup");
        assertThrows(ConflictException.class, () -> service.register("dup@example.com", null, "pwd", "Dup2"));
    }
}
