package c2c.common;

import java.util.Objects;

/**
 * Simple success/failure wrapper with optional payload and message.
 */
public class Result<T> {
    private final boolean success;
    private final T data;
    private final String message;

    private Result(boolean success, T data, String message) {
        this.success = success;
        this.data = data;
        this.message = message;
    }

    public static <T> Result<T> ok(T data) {
        return new Result<>(true, data, null);
    }

    public static <T> Result<T> fail(String message) {
        return new Result<>(false, null, message);
    }

    public boolean isSuccess() {
        return success;
    }

    public T getData() {
        return data;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return "Result{" +
                "success=" + success +
                ", data=" + data +
                ", message='" + message + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Result<?> result = (Result<?>) o;
        return success == result.success && Objects.equals(data, result.data)
                && Objects.equals(message, result.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(success, data, message);
    }
}
