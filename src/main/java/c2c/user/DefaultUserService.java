package c2c.user;

import c2c.common.IdGenerator;
import c2c.common.Result;
import c2c.common.exceptions.ConflictException;
import c2c.common.exceptions.NotFoundException;
import c2c.common.exceptions.ValidationException;

import java.time.Instant;
import java.util.Set;

public class DefaultUserService implements UserService {
    private final UserRepository repository;
    private final PasswordHasher passwordHasher;

    public DefaultUserService(UserRepository repository, PasswordHasher passwordHasher) {
        this.repository = repository;
        this.passwordHasher = passwordHasher;
    }

    @Override
    public Result<User> register(String email, String phone, String password, String name) {
        if (email == null || email.isBlank()) {
            throw new ValidationException("email is required");
        }
        if (password == null || password.isBlank()) {
            throw new ValidationException("password is required");
        }
        if (repository.existsByEmail(email)) {
            throw new ConflictException("email already registered");
        }
        if (phone != null && repository.existsByPhone(phone)) {
            throw new ConflictException("phone already registered");
        }
        String id = IdGenerator.nextNumeric();
        String hashed = passwordHasher.hash(password);
        User user = new User(id, email, phone, hashed, name, Set.of("BUYER"), UserStatus.ACTIVE, Instant.now());
        repository.save(user);
        return Result.ok(user);
    }

    @Override
    public Result<User> login(String email, String password) {
        User user = repository.findByEmail(email).orElseThrow(() -> new NotFoundException("user not found"));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ValidationException("user disabled");
        }
        if (!passwordHasher.matches(password, user.getHashedPassword())) {
            throw new ValidationException("invalid credentials");
        }
        return Result.ok(user);
    }

    @Override
    public Result<User> updateProfile(String userId, String name, String phone) {
        User user = repository.findById(userId).orElseThrow(() -> new NotFoundException("user not found"));
        user.setName(name);
        user.setPhone(phone);
        repository.save(user);
        return Result.ok(user);
    }
}
