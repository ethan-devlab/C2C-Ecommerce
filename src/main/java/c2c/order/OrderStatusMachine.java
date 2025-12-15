package c2c.order;

public final class OrderStatusMachine {
    private OrderStatusMachine() {
    }

    public static boolean canTransit(OrderStatus from, OrderStatus to) {
        if (from == to) {
            return true;
        }
        return switch (from) {
            case PENDING -> to == OrderStatus.PAID || to == OrderStatus.CANCELED;
            case PAID -> to == OrderStatus.SHIPPED || to == OrderStatus.CANCELED || to == OrderStatus.REFUNDED;
            case SHIPPED -> to == OrderStatus.COMPLETED || to == OrderStatus.REFUNDED;
            case COMPLETED, CANCELED, REFUNDED -> false;
        };
    }
}
