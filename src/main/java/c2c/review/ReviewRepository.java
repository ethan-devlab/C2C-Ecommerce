package c2c.review;

import java.util.List;

public interface ReviewRepository {
    Review save(Review review);

    List<Review> listByProduct(String productId);

    List<Review> findAll();
}
