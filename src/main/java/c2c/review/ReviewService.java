package c2c.review;

import c2c.common.Result;

import java.util.List;

public interface ReviewService {
    Result<Review> addReview(String orderId, String productId, String buyerId, int rating, String comment);

    Result<List<Review>> list(String productId);

    Result<Double> averageRating(String productId);
}
