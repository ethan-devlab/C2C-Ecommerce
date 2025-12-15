package c2c.bank;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FakeBankService implements BankService {
    private final Map<String, Double> balances = new ConcurrentHashMap<>();

    public void seedBalance(String cardNumber, double amount) {
        balances.put(cardNumber, amount);
    }

    @Override
    public boolean checkBalance(String payerId, String cardNumber, double amount) {
        return balances.getOrDefault(cardNumber, 0.0) >= amount;
    }

    @Override
    public boolean debit(String payerId, String cardNumber, double amount) {
        final boolean[] success = { false };
        balances.computeIfPresent(cardNumber, (key, balance) -> {
            if (balance >= amount) {
                success[0] = true;
                return balance - amount;
            }
            return balance;
        });
        return success[0];
    }
}
