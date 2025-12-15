package c2c.bank;

import java.util.List;

public interface TransactionLogRepository {
    void save(Transaction transaction);

    List<Transaction> findAll();
}
