Susan Proxy Management System (俗三代購訂單管理系統)
=============================================

本專案是一個為代購賣家打造的訂單同步與查詢系統。目前涵蓋了從後端資料（CSV/Excel 解析、ETL 轉換）、關聯式資料庫建置（Spring Data JPA + MariaDB）到標準化 RESTful API 的完整後端架構。React 前端尚未開始實作，但已經先完成了資料封裝（DTO）與 CORS 的準備。

### 後續規劃 (Future Roadmap)

- **前端實作：** 採用 React 18 + Vite + TypeScript 建置買家查詢介面，透過串接後端 API 渲染出訂單總覽與對帳單。
- **雲端同步升級：** 有計畫導入 Google Sheets API，將目前的「本機 CSV 手動同步」升級為「雲端表單自動同步」，進一步自動化代購賣家的工作流程。
- **身分驗證與權限：** 為同步 API (`/api/dev/sync-csv`) 加入 API Key 或 Spring Security JWT 防護，確保後台資料的安全。

專案總覽 (Project Overview)
-----------------------

- **功能目標：** 提供代購賣家一鍵同步對帳表單（Google Sheet）的功能，並提供買家透過「暱稱」即時查詢個人所有連線群組的訂單明細與欠款狀態。
- **核心技術：** Java (Spring Boot 3), Spring Data JPA, MariaDB, Docker Compose, React (Vite)。
- 目前專案實作的重點包括實作了 DTO 轉換模式以隔離資料庫實體、解決 JPA 的 N+1 查詢與笛卡兒積效能陷阱，並建立了一套全局 API 的回應格式。

技術棧 (Tech Stack)
----------------

| **Category** | **Technologies** |
| --- | --- |
| Backend | Java, Spring Boot 3, Spring Web, Spring Data JPA, Lombok |
| Frontend | React 18, TypeScript, Vite (開發中) |
| Database | MariaDB |
| DevOps | Docker, Docker Compose (本地資料庫環境) |
| Tools | Maven, IntelliJ IDEA Ultimate (內建 Database Tools), Talend API Tester |

系統架構 (System Architecture)
--------------------------

### 1. 後端 (Backend)
- **資料同步 (ETL Pipeline)：**
  - 實作 `SheetSyncService` 讀取並解析 CSV 檔案（如：東京連線對帳單）。
  - 採用 **「Wipe and Replace (全刪全增)」** 的冪等性設計：以 CSV 檔名作為 `groupName`，每次同步會自動清理該團舊資料並重建，避免出現幽靈資料或狀態不同步的問題。
- **資料庫存取與效能優化 (JPA)：**
  - 實作 `@OneToMany` 與 `@ManyToOne` 關聯，將資料正規化為 `OrderGroup` (主要檔案) 與 `OrderItem` (明細)。
  - 為優化資料庫效能，撰寫 JPQL 語法，利用 JOIN FETCH 讓系統一次抓出主檔和明細，避免查詢次數過多的 N+1 問題；同時加上 DISTINCT 關鍵字，把 JOIN 產生的重複訂單過濾掉，確保傳給前端的 JSON 是最乾淨的。
- **API 設計與安全隔離 (DTO Pattern)：**
  - 透過 `OrderGroupDto` 與 `OrderItemDto` 進行資料處理，切斷 JSON 雙向關聯造成的無限迴圈 (`StackOverflowError`)，並隱藏底層資料庫細節。
  - 實作全局統一的 `ApiResponse<T>` 泛型包裝類別，提供前端一致的 JSON 結構（包含 `success`, `data`, `error`, `timestamp`）。
  - 設定攔截空值 (400 Bad Request) 與查無資料 (404 Not Found)。
  - Controller 層已撰寫 `@CrossOrigin`，支援前後端分離架構的跨網域請求 (CORS)。

### 2. 前端 (Frontend - 準備中)
- **架構選型：** 預計採用 React + Vite 進行建置。
- **API 串接：** 前端不處理複雜的錯誤邏輯，僅判斷 `ApiResponse` 中的 `success` ，決定渲染訂單畫面或直接顯示後端回傳的錯誤訊息 (`error.message`)。

### 3. 本地開發環境 (Docker & Infrastructure)
- **Docker Compose：**
  - `docker-compose.yml` 定義了 `mariadb` 服務，讓開發時不用安裝本地資料庫，一鍵啟動儲存環境。
  - 資料庫連線帳密與設定都拉到 `application.yml` 統一管理。

啟動專案 (Getting Started)
----------------------
1. **啟動資料庫：**
  確保已安裝 Docker Desktop，並於根目錄執行（之後應該會建 `infra` 目錄）：
    ```Bash
    docker-compose up -d
    ```
2. **啟動 Spring Boot 應用程式：**
    執行 `Application.java`，伺服器會運行在 `http://localhost:8080`。
3. **測試資料 (Talend API Tester)：**
  - **匯入資料：** 發送 `POST http://localhost:8080/api/dev/sync-csv` 觸發本機 CSV 解析與資料庫寫入。 
  - **查詢資料：** 瀏覽器開啟或發送 `GET http://localhost:8080/api/orders?nickname={買家暱稱}` 驗證 JSON 回傳結果。

### 📂 當前目錄結構摘要
```text
├── src/main/java/com/fy20047/susan/
│   ├── controller/      # API 進入點 (OrderQueryController, LocalCsvSyncController)
│   ├── service/         # 業務邏輯與 CSV 解析 (SheetSyncService)
│   ├── repository/      # JPA 資料庫操作與自訂 JPQL
│   ├── domain/          # 實體模型 (Entity: OrderGroup, OrderItem)
│   └── dto/             # 資料傳輸物件與 API 回應格式 (ApiResponse, OrderGroupDto)
├── src/main/resources/
│   ├── application.yml  # Spring Boot 與資料庫環境設定
│   └── sample-data/     # 測試用 CSV 來源檔
├── docker-compose.yml   # MariaDB 容器配置
└── README.md            # 專案說明文件
```
