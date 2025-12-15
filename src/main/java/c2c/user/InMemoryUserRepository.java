package c2c.user;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.ArrayList;
import java.util.List;

public class InMemoryUserRepository implements UserRepository {
    private final Map<String, User> storage = new ConcurrentHashMap<>();
    private final Map<String, String> emailIndex = new ConcurrentHashMap<>();
    private final Map<String, String> phoneIndex = new ConcurrentHashMap<>();

    @Override
    public User save(User user) {
        storage.put(user.getId(), user);
        emailIndex.put(user.getEmail(), user.getId());
        if (user.getPhone() != null) {
            phoneIndex.put(user.getPhone(), user.getId());
        }
        return user;
    }

    @Override
    public Optional<User> findById(String id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public Optional<User> findByEmail(String email) {
        String id = emailIndex.get(email);
        return id == null ? Optional.empty() : Optional.ofNullable(storage.get(id));
    }

    @Override
    public boolean existsByEmail(String email) {
        return emailIndex.containsKey(email);
    }

    @Override
    public boolean existsByPhone(String phone) {
        return phone != null && phoneIndex.containsKey(phone);
    }

    @Override
    public List<User> findAll() {
        return new ArrayList<>(storage.values());
    }
}
