package c2c.bank;

public interface BankService {
    boolean checkBalance(String payerId, String cardNumber, double amount);

    boolean debit(String payerId, String cardNumber, double amount);
}
