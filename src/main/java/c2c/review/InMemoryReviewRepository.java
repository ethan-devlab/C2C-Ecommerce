package c2c.review;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class InMemoryReviewRepository implements ReviewRepository {
    private final List<Review> storage = Collections.synchronizedList(new ArrayList<>());

    @Override
    public Review save(Review review) {
        storage.add(review);
        return review;
    }

    @Override
    public List<Review> listByProduct(String productId) {
        List<Review> result = new ArrayList<>();
        for (Review review : storage) {
            if (review.getProductId().equals(productId)) {
                result.add(review);
            }
        }
        return result;
    }

    @Override
    public List<Review> findAll() {
        return new ArrayList<>(storage);
    }
}
