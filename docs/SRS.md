# C2C 電商平台 SRS (Software Requirement Specification)

## 1. 目的與範圍
- 目的：定義 C2C 平台的功能與非功能需求，作為設計、實作與測試依據。
- 範圍：提供用戶註冊/登入、商品上架/瀏覽/搜尋、購物車、訂單、評價、支付與銀行模擬（餘額檢查、扣款、交易鎖定與紀錄）。
- 不在範圍：實體金流、真實資料庫、行動推播、第三方物流串接。

## 2. 角色與用例概觀
- 訪客：瀏覽商品、搜尋商品、註冊、登入。
- 買家：瀏覽/搜尋/收藏商品、加入購物車、下單、付款、查詢訂單、評價。
- 賣家：上架/下架/編輯商品，查看訂單狀態與評價。

主要用例（精簡）：
1. 註冊/登入/登出。
2. 商品上架、下架、編輯、瀏覽、搜尋、查看詳情。
3. 購物車新增/移除/更新數量、查看購物車。
4. 下單：從購物車或直接購買，計算運費與總額。
5. 支付：信用卡/金融卡模擬扣款，銀行餘額檢查，交易鎖定與記錄。
6. 訂單：查詢、狀態更新（待付款→已付款→出貨→完成/取消/退款）。
7. 評價：買家對商品留言與評分；賣家回覆（可選）。
8. 日誌：操作與異常記錄。

## 3. 功能需求
- 用戶管理
  - 註冊需唯一 email/phone；密碼加密儲存；登入驗證；基本資料檢視/更新。
- 商品管理
  - 上架/下架/編輯，包含標題、描述、價格、庫存、分類、圖片 URL（字串）。
  - 瀏覽、分類/關鍵字搜尋、排序（上架時間、價格、銷量）。
- 購物車
  - 加入商品（驗庫存）、變更數量、刪除項目、查看合計。
- 訂單
  - 建立訂單（含購物車明細、金額、折扣策略預留）、生成唯一訂單編號。
  - 狀態流轉：PENDING → PAID → SHIPPED → COMPLETED / CANCELED / REFUNDED。
- 支付與銀行模擬
  - 交易流程：建立交易→鎖定→餘額檢查→扣款→釋放鎖→記錄交易結果。
  - 支援信用卡/金融卡，回傳交易碼與訊息；異常須回滾訂單狀態。
- 評價系統
  - 買家對已完成訂單的商品評分、評論；可查詢評價列表與平均分數。
- 日誌與例外
  - 操作與錯誤寫入 Log4j2；全域例外處理給出友善訊息。
- 介面
  - 先提供 CLI；保留 GUI/Swing 或 JavaFX 擴充接口。

## 4. 非功能需求
- 並發：交易鎖保護同一用戶/卡號/訂單的並行扣款，避免餘額競賽。
- 安全：密碼雜湊（如 BCrypt）、基本權限檢查；避免將敏感資訊寫入日誌。
- 可用性：輸入錯誤給予明確訊息；異常可重試支付或取消訂單。
- 可維護性：模組化分層，服務介面清晰；具單元測試與覆蓋度報告。
- 可移植性：純 Java/內存資料結構，無外部 DB 依賴。

## 5. 資料模型（邏輯）
- User：id, email, phone, hashedPassword, name, roles, createdAt, status。
- Product：id, sellerId, title, description, price, stock, category, images, status, createdAt。
- CartItem：userId, productId, quantity, priceSnapshot。
- Order：id, buyerId, items(list of OrderItem), amount, status, paymentId, createdAt, updatedAt。
- OrderItem：productId, titleSnapshot, priceSnapshot, quantity, sellerId。
- Payment：id, orderId, method, amount, status, transactionId, createdAt。
- Transaction：id, payerId, amount, type, lockKey, status, logEntries。
- Review：id, orderId, productId, buyerId, rating, comment, createdAt。

## 6. 介面需求（CLI 初版）
- user: register/login/profile
- product: list/search/view/add/update/remove
- cart: add/update/remove/view
- order: create/list/view/update
- pay: checkout/pay
- review: add/list

## 7. 測試需求
- 單元測試：服務層、交易鎖、庫存檢查、狀態流轉。
- 整合測試：下單→支付→狀態更新→評價整流程。
- Mockito：替換外部依賴（如 BankService）以測支付例外。
- 覆蓋度：行與分支覆蓋率目標 ≥ 90%，核心交易/訂單流程 ≥ 90%。
- 靜態檢查：PMD；度量：MetricsReloaded。

## 8. 風險與限制
- 無真實金流與 DB，資料重啟即清空；需提供資料匯入/匯出接口（JSON 序列化）以便測試。
- 並發示範僅限單機鎖，不覆蓋分散式鎖。

## 9. 驗收標準
- 必要用例皆可於 CLI 驗證；交易鎖下無重複扣款；日誌與錯誤提示清晰；測試與覆蓋度達標。
