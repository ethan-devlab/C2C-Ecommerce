package c2c.review;

import c2c.common.IdGenerator;
import c2c.common.Result;
import c2c.common.exceptions.ValidationException;
import c2c.order.Order;
import c2c.order.OrderRepository;
import c2c.order.OrderStatus;

import java.time.Instant;
import java.util.List;

public class DefaultReviewService implements ReviewService {
    private final ReviewRepository reviewRepository;
    private final OrderRepository orderRepository;

    public DefaultReviewService(ReviewRepository reviewRepository, OrderRepository orderRepository) {
        this.reviewRepository = reviewRepository;
        this.orderRepository = orderRepository;
    }

    @Override
    public Result<Review> addReview(String orderId, String productId, String buyerId, int rating, String comment) {
        if (rating < 1 || rating > 5) {
            throw new ValidationException("rating must be 1-5");
        }
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new ValidationException("order not found"));
        if (!order.getBuyerId().equals(buyerId)) {
            throw new ValidationException("buyer mismatch");
        }
        if (order.getStatus() != OrderStatus.COMPLETED) {
            throw new ValidationException("order not completed");
        }
        Review review = new Review(IdGenerator.nextNumeric(), orderId, productId, buyerId, rating, comment,
                Instant.now());
        reviewRepository.save(review);
        return Result.ok(review);
    }

    @Override
    public Result<List<Review>> list(String productId) {
        return Result.ok(reviewRepository.listByProduct(productId));
    }

    @Override
    public Result<Double> averageRating(String productId) {
        List<Review> reviews = reviewRepository.listByProduct(productId);
        if (reviews.isEmpty()) {
            return Result.ok(0.0);
        }
        double avg = reviews.stream().mapToInt(Review::getRating).average().orElse(0);
        return Result.ok(avg);
    }
}
