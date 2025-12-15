package c2c.user;

import c2c.common.Result;

public interface UserService {
    Result<User> register(String email, String phone, String password, String name);

    Result<User> login(String email, String password);

    Result<User> updateProfile(String userId, String name, String phone);
}
