package c2c.bank;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class InMemoryTransactionLogRepository implements TransactionLogRepository {
    private final List<Transaction> storage = Collections.synchronizedList(new ArrayList<>());

    @Override
    public void save(Transaction transaction) {
        storage.add(transaction);
    }

    @Override
    public List<Transaction> findAll() {
        return new ArrayList<>(storage);
    }
}
