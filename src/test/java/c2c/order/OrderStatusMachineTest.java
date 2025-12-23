package c2c.order;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("OrderStatusMachine 測試")
class OrderStatusMachineTest {

    static {
        System.setProperty("net.bytebuddy.experimental", "true");
    }

    @Nested
    @DisplayName("狀態轉換測試 - 使用決策表")
    class StatusTransitionTests {

        @ParameterizedTest(name = "OSM-001~002: PENDING可轉換至{0}")
        @EnumSource(value = OrderStatus.class, names = { "PAID", "CANCELED" })
        @DisplayName("從PENDING狀態的有效轉換")
        void validTransitionsFromPending(OrderStatus toStatus) {
            // When: 從PENDING轉換
            boolean result = OrderStatusMachine.canTransit(OrderStatus.PENDING, toStatus);

            // Then: 允許轉換
            assertTrue(result,
                    String.format("PENDING → %s should be allowed", toStatus));
        }

        @ParameterizedTest(name = "OSM-003: PENDING不可轉換至{0}")
        @EnumSource(value = OrderStatus.class, names = { "SHIPPED", "COMPLETED", "REFUNDED" })
        @DisplayName("從PENDING狀態的非法轉換")
        void invalidTransitionsFromPending(OrderStatus toStatus) {
            // When: 從PENDING轉換
            boolean result = OrderStatusMachine.canTransit(OrderStatus.PENDING, toStatus);

            // Then: 不允許轉換
            assertFalse(result,
                    String.format("PENDING → %s should not be allowed", toStatus));
        }

        @ParameterizedTest(name = "OSM-004~006: PAID可轉換至{0}")
        @EnumSource(value = OrderStatus.class, names = { "SHIPPED", "CANCELED", "REFUNDED" })
        @DisplayName("從PAID狀態的有效轉換")
        void validTransitionsFromPaid(OrderStatus toStatus) {
            // When: 從PAID轉換
            boolean result = OrderStatusMachine.canTransit(OrderStatus.PAID, toStatus);

            // Then: 允許轉換
            assertTrue(result,
                    String.format("PAID → %s should be allowed", toStatus));
        }

        @ParameterizedTest(name = "PAID不可轉換至{0}")
        @EnumSource(value = OrderStatus.class, names = { "PENDING", "COMPLETED" })
        @DisplayName("從PAID狀態的非法轉換")
        void invalidTransitionsFromPaid(OrderStatus toStatus) {
            // When: 從PAID轉換
            boolean result = OrderStatusMachine.canTransit(OrderStatus.PAID, toStatus);

            // Then: 不允許轉換
            assertFalse(result,
                    String.format("PAID → %s should not be allowed", toStatus));
        }

        @ParameterizedTest(name = "OSM-007~008: SHIPPED可轉換至{0}")
        @EnumSource(value = OrderStatus.class, names = { "COMPLETED", "REFUNDED" })
        @DisplayName("從SHIPPED狀態的有效轉換")
        void validTransitionsFromShipped(OrderStatus toStatus) {
            // When: 從SHIPPED轉換
            boolean result = OrderStatusMachine.canTransit(OrderStatus.SHIPPED, toStatus);

            // Then: 允許轉換
            assertTrue(result,
                    String.format("SHIPPED → %s should be allowed", toStatus));
        }

        @ParameterizedTest(name = "SHIPPED不可轉換至{0}")
        @EnumSource(value = OrderStatus.class, names = { "PENDING", "PAID", "CANCELED" })
        @DisplayName("從SHIPPED狀態的非法轉換")
        void invalidTransitionsFromShipped(OrderStatus toStatus) {
            // When: 從SHIPPED轉換
            boolean result = OrderStatusMachine.canTransit(OrderStatus.SHIPPED, toStatus);

            // Then: 不允許轉換
            assertFalse(result,
                    String.format("SHIPPED → %s should not be allowed", toStatus));
        }

        @ParameterizedTest(name = "OSM-009/011: {0}為終止狀態，不可轉換至{1}")
        @CsvSource({
                "COMPLETED, PENDING",
                "COMPLETED, PAID",
                "COMPLETED, SHIPPED",
                "COMPLETED, CANCELED",
                "COMPLETED, REFUNDED",
                "CANCELED, PENDING",
                "CANCELED, PAID",
                "CANCELED, SHIPPED",
                "CANCELED, COMPLETED",
                "CANCELED, REFUNDED",
                "REFUNDED, PENDING",
                "REFUNDED, PAID",
                "REFUNDED, SHIPPED",
                "REFUNDED, COMPLETED",
                "REFUNDED, CANCELED"
        })
        @DisplayName("終止狀態不可轉換")
        void terminalStatusCannotTransit(String fromStatus, String toStatus) {
            // When: 從終止狀態轉換
            OrderStatus from = OrderStatus.valueOf(fromStatus);
            OrderStatus to = OrderStatus.valueOf(toStatus);
            boolean result = OrderStatusMachine.canTransit(from, to);

            // Then: 不允許轉換
            assertFalse(result,
                    String.format("%s → %s should not be allowed (terminal state)", from, to));
        }
    }

    @Nested
    @DisplayName("特殊情況測試")
    class SpecialCasesTests {

        @ParameterizedTest(name = "OSM-010: {0}可保持相同狀態")
        @EnumSource(OrderStatus.class)
        @DisplayName("相同狀態轉換允許")
        void sameStatusTransitionAllowed(OrderStatus status) {
            // When: 轉換至相同狀態
            boolean result = OrderStatusMachine.canTransit(status, status);

            // Then: 允許轉換（from == to）
            assertTrue(result,
                    String.format("%s → %s (same status) should be allowed", status, status));
        }
    }

    @Nested
    @DisplayName("完整決策表測試")
    class DecisionTableTests {

        @ParameterizedTest(name = "{0} → {1} = {2}")
        @CsvSource({
                // PENDING transitions
                "PENDING, PENDING, true",
                "PENDING, PAID, true",
                "PENDING, CANCELED, true",
                "PENDING, SHIPPED, false",
                "PENDING, COMPLETED, false",
                "PENDING, REFUNDED, false",

                // PAID transitions
                "PAID, PENDING, false",
                "PAID, PAID, true",
                "PAID, CANCELED, true",
                "PAID, SHIPPED, true",
                "PAID, COMPLETED, false",
                "PAID, REFUNDED, true",

                // SHIPPED transitions
                "SHIPPED, PENDING, false",
                "SHIPPED, PAID, false",
                "SHIPPED, CANCELED, false",
                "SHIPPED, SHIPPED, true",
                "SHIPPED, COMPLETED, true",
                "SHIPPED, REFUNDED, true",

                // COMPLETED transitions (all false except same state)
                "COMPLETED, PENDING, false",
                "COMPLETED, PAID, false",
                "COMPLETED, CANCELED, false",
                "COMPLETED, SHIPPED, false",
                "COMPLETED, COMPLETED, true",
                "COMPLETED, REFUNDED, false",

                // CANCELED transitions (all false except same state)
                "CANCELED, PENDING, false",
                "CANCELED, PAID, false",
                "CANCELED, CANCELED, true",
                "CANCELED, SHIPPED, false",
                "CANCELED, COMPLETED, false",
                "CANCELED, REFUNDED, false",

                // REFUNDED transitions (all false except same state)
                "REFUNDED, PENDING, false",
                "REFUNDED, PAID, false",
                "REFUNDED, CANCELED, false",
                "REFUNDED, SHIPPED, false",
                "REFUNDED, COMPLETED, false",
                "REFUNDED, REFUNDED, true"
        })
        @DisplayName("完整狀態轉換決策表")
        void completeDecisionTable(String fromStatus, String toStatus, boolean expected) {
            // When: 執行狀態轉換檢查
            OrderStatus from = OrderStatus.valueOf(fromStatus);
            OrderStatus to = OrderStatus.valueOf(toStatus);
            boolean result = OrderStatusMachine.canTransit(from, to);

            // Then: 結果符合預期
            assertEquals(expected, result,
                    String.format("%s → %s should be %s", from, to, expected ? "allowed" : "not allowed"));
        }
    }

    @Nested
    @DisplayName("業務流程路徑測試")
    class BusinessFlowTests {

        @Test
        @DisplayName("正常訂單流程：PENDING → PAID → SHIPPED → COMPLETED")
        void normalOrderFlow() {
            // Given: 訂單正常流程
            assertTrue(OrderStatusMachine.canTransit(OrderStatus.PENDING, OrderStatus.PAID));
            assertTrue(OrderStatusMachine.canTransit(OrderStatus.PAID, OrderStatus.SHIPPED));
            assertTrue(OrderStatusMachine.canTransit(OrderStatus.SHIPPED, OrderStatus.COMPLETED));
        }

        @Test
        @DisplayName("訂單取消流程：PENDING → CANCELED")
        void cancelOrderFlow_fromPending() {
            // Given: 未付款訂單取消
            assertTrue(OrderStatusMachine.canTransit(OrderStatus.PENDING, OrderStatus.CANCELED));
        }

        @Test
        @DisplayName("訂單取消流程：PAID → CANCELED")
        void cancelOrderFlow_fromPaid() {
            // Given: 已付款訂單取消
            assertTrue(OrderStatusMachine.canTransit(OrderStatus.PAID, OrderStatus.CANCELED));
        }

        @Test
        @DisplayName("退款流程：PAID → REFUNDED")
        void refundFlow_fromPaid() {
            // Given: 已付款訂單退款
            assertTrue(OrderStatusMachine.canTransit(OrderStatus.PAID, OrderStatus.REFUNDED));
        }

        @Test
        @DisplayName("退款流程：SHIPPED → REFUNDED")
        void refundFlow_fromShipped() {
            // Given: 已出貨訂單退款
            assertTrue(OrderStatusMachine.canTransit(OrderStatus.SHIPPED, OrderStatus.REFUNDED));
        }

        @Test
        @DisplayName("非法流程：PENDING直接到COMPLETED")
        void invalidFlow_pendingToCompleted() {
            // Given: 跳過中間狀態
            assertFalse(OrderStatusMachine.canTransit(OrderStatus.PENDING, OrderStatus.COMPLETED));
        }

        @Test
        @DisplayName("非法流程：COMPLETED後無法再變更")
        void invalidFlow_completedCannotChange() {
            // Given: 已完成訂單不可變更
            assertFalse(OrderStatusMachine.canTransit(OrderStatus.COMPLETED, OrderStatus.PAID));
            assertFalse(OrderStatusMachine.canTransit(OrderStatus.COMPLETED, OrderStatus.SHIPPED));
            assertFalse(OrderStatusMachine.canTransit(OrderStatus.COMPLETED, OrderStatus.CANCELED));
            assertFalse(OrderStatusMachine.canTransit(OrderStatus.COMPLETED, OrderStatus.REFUNDED));
        }
    }

    @Nested
    @DisplayName("邊界情況測試")
    class EdgeCasesTests {

        @Test
        @DisplayName("所有狀態自我轉換都允許")
        void allStatusSelfTransitionAllowed() {
            // When & Then: 所有狀態都可以轉換到自己
            for (OrderStatus status : OrderStatus.values()) {
                assertTrue(OrderStatusMachine.canTransit(status, status),
                        String.format("%s → %s should be allowed", status, status));
            }
        }

        @Test
        @DisplayName("驗證所有狀態組合都有定義")
        void allStatusCombinationsAreDefined() {
            // When & Then: 所有狀態組合調用不拋異常
            for (OrderStatus from : OrderStatus.values()) {
                for (OrderStatus to : OrderStatus.values()) {
                    assertDoesNotThrow(() -> OrderStatusMachine.canTransit(from, to),
                            String.format("canTransit(%s, %s) should not throw exception", from, to));
                }
            }
        }
    }

    @Nested
    @DisplayName("狀態機屬性測試")
    class StateMachinePropertiesTests {

        @Test
        @DisplayName("終止狀態驗證：COMPLETED, CANCELED, REFUNDED")
        void terminalStatesCannotTransitToOthers() {
            // Given: 終止狀態
            OrderStatus[] terminalStates = {
                    OrderStatus.COMPLETED,
                    OrderStatus.CANCELED,
                    OrderStatus.REFUNDED
            };

            // When & Then: 終止狀態只能轉換到自己
            for (OrderStatus terminalState : terminalStates) {
                for (OrderStatus toStatus : OrderStatus.values()) {
                    boolean result = OrderStatusMachine.canTransit(terminalState, toStatus);
                    if (terminalState == toStatus) {
                        assertTrue(result,
                                String.format("%s should transit to itself", terminalState));
                    } else {
                        assertFalse(result,
                                String.format("%s should not transit to %s", terminalState, toStatus));
                    }
                }
            }
        }

        @Test
        @DisplayName("初始狀態驗證：PENDING可以轉換到PAID或CANCELED")
        void initialStatePendingHasLimitedTransitions() {
            // Given: PENDING狀態
            OrderStatus pending = OrderStatus.PENDING;

            // When & Then: PENDING只能轉換到PAID、CANCELED或自己
            assertTrue(OrderStatusMachine.canTransit(pending, OrderStatus.PENDING));
            assertTrue(OrderStatusMachine.canTransit(pending, OrderStatus.PAID));
            assertTrue(OrderStatusMachine.canTransit(pending, OrderStatus.CANCELED));
            assertFalse(OrderStatusMachine.canTransit(pending, OrderStatus.SHIPPED));
            assertFalse(OrderStatusMachine.canTransit(pending, OrderStatus.COMPLETED));
            assertFalse(OrderStatusMachine.canTransit(pending, OrderStatus.REFUNDED));
        }
    }
}
