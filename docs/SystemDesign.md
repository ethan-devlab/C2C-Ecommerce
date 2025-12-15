# C2C 平台系統設計 (System Design Document)

## 1. 分層與套件對應
- CLI 層：命令解析、輸入檢查、輸出格式化。
- Application 層：Service 介面與用例流程（Order、Payment、Cart 等）。
- Domain 層：Entity / Value Object / 狀態機。
- Infrastructure 層：In-memory Repository、Lock、Id 生成、序列化、Log。

## 2. 核心類別與介面（建議草稿）
- common
  - `Result<T>`：成功/失敗封裝；含錯誤碼與訊息。
  - `DomainException`, `ValidationException`, `NotFoundException`, `ConflictException`。
  - `IdGenerator`：UUID 或遞增；可注入。
- user
  - `User {id,email,phone,hashedPassword,name,roles,status}`
  - `UserRepository`：`save, findById, findByEmail, existsByEmail/Phone`。
  - `UserService`：`register(cmd)`, `login(email,pwd)`, `updateProfile(id, dto)`。
  - `PasswordHasher`：`hash(raw)`, `matches(raw, hashed)`。
- product
  - `Product {id,sellerId,title,description,price,stock,category,images,status}`
  - `ProductRepository`：`save, findById, search(spec), decreaseStock(id, qty)`。
  - `ProductService`：`create/update/remove/list/search/detail`。
- cart
  - `CartItem {productId, quantity, priceSnapshot}`
  - `CartRepository`：`getCart(userId)`, `putItem(userId,item)`, `removeItem`, `clear`。
  - `CartService`：`addItem(userId, productId, qty)`, `updateQty`, `remove`, `view`。
- order
  - `Order {id,buyerId,items,status,amount,paymentId,createdAt,updatedAt}`
  - `OrderItem {productId,titleSnapshot,priceSnapshot,quantity,sellerId}`
  - `OrderStatus {PENDING, PAID, SHIPPED, COMPLETED, CANCELED, REFUNDED}`
  - `OrderRepository`：`save, findById, listByBuyer, updateStatus`。
  - `OrderStatusMachine`：`canTransit(from,to)`；集中狀態合法性。
  - `OrderService`：`createFromCart(buyerId)`, `createDirect(buyerId, productId, qty)`, `updateStatus`, `get`, `list`。
- payment
  - `Payment {id,orderId,method,amount,status,transactionId,createdAt}`
  - `PaymentStatus {PENDING, SUCCESS, FAILED}`
  - `PaymentService`：`pay(orderId, method, cardInfo)`；處理交易、更新訂單。
- bank
  - `Transaction {id,payerId,amount,type,lockKey,status,startedAt,endedAt,logs[]}`
  - `TransactionStatus {INIT, LOCKED, DEBITED, FAILED}`
  - `TransactionLockManager`：`lock(key, timeout)`, `unlock(key)`；基於 `ReentrantLock`。
  - `BankService`：`checkBalance(cardInfo)`, `debit(cardInfo, amount)`；可用 fake 資料源。
  - `TransactionLogRepository`：持久化/查詢交易紀錄（in-memory list）。
- review
  - `Review {id,orderId,productId,buyerId,rating,comment,createdAt}`
  - `ReviewRepository`：`save, listByProduct, averageRating(productId)`
  - `ReviewService`：`addReview(orderId, productId, buyerId, rating, comment)`, `list(productId)`。
- cli
  - `CommandRouter`、各 `CommandHandler`；輸出對 user 友善的文字。

## 3. 例外與錯誤碼（示意）
- `USER_EXISTS`, `INVALID_CREDENTIAL`, `PRODUCT_NOT_FOUND`, `INSUFFICIENT_STOCK`, `ORDER_NOT_FOUND`, `PAYMENT_FAILED`, `LOCK_TIMEOUT`。
- 例外統一在 CLI 層轉為人類可讀訊息；Service 層回傳 Result 或拋 domain 例外。

## 4. 狀態機與一致性
- 下單：建立訂單為 PENDING，同步預扣庫存（或保留）。失敗需回復庫存。
- 支付成功：訂單→PAID；失敗：訂單→CANCELED；退款（可選）：PAID/SHIPPED→REFUNDED。
- 交易鎖：以 `lockKey = payerId | cardNo` 確保同時只處理一筆；finally 釋放。

## 5. 資料匯入/匯出（可選）
- `DataExportService`：將 repository 轉為 JSON；`DataImportService`：啟動時讀檔載入，便於測試與示範。

## 6. 日誌與追蹤
- Log4j2 pattern: `%d{ISO8601} %-5p [%t] %c - %m%n`
- 事件：登入/註冊、上架/下架、建立訂單、支付、鎖失敗、例外。

## 7. 測試鉤子
- Repository 可替換為 in-memory fake；BankService 提供 stub 以模擬餘額不足/超時。
- TransactionLockManager 支援注入測試鎖或設定較短 timeout。
