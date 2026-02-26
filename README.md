# Susan Proxy System (俗三代購訂單管理系統)

這是一個專為代購賣家打造的訂單同步與查詢系統。
賣家可以維持原有的習慣，使用熟悉的 Google Sheet 或 Excel 管理訂單，系統會定時將資料同步至資料庫。買家則無需登入，僅需輸入「暱稱」即可即時查詢個人在各個連線群組的訂單明細、到貨進度與欠款狀態。
為求快速上線與穩定同步，目前專案採 MVP (Minimum Viable Product) 策略：省略較為複雜的後台管理介面，所有配置先透過環境變數與 Google Sheet 內的「設定」分頁達成。

## 後續規劃 (Future Roadmap)

- **雲端同步升級：** 有計畫導入 Google Sheets API，將目前的「本機 CSV 手動同步」升級為「雲端表單自動同步」，進一步自動化代購賣家的工作流程。
- **身分驗證與權限：** 為同步 API (`/api/dev/sync-csv`) 加入 API Key 或 Spring Security JWT 防護，確保後台資料的安全。

## 技術棧 (Tech Stack)

| **類別** | **技術工具** |
| --- | --- |
| Backend | Java 21, Spring Boot 3, Spring Data JPA, Hibernate, Lombok |
| Frontend | React 18, TypeScript, Vite, Tailwind CSS, Lucide React |
| Database | MariaDB |
|ETL / Parser |	EasyExcel (XLSX 解析), Apache Commons CSV |
| DevOps | Docker, Docker Compose, Zeabur (雲端部署) |

## 核心設計與資料流程

目前系統的核心主為「資料的一致性」與「簡易查詢」：
1. 自動化 ETL 流程：目前設定後端 `SheetSyncService` 每 5 分鐘自動抓取 Google Sheet 的 XLSX 匯出連結。
2. 先採用「Wipe and Replace (全刪全增)」設計，同步時以分頁名稱作為 `groupName`，自動清理舊資料並重建，來避免出現幽靈資料或狀態不同步的問題。
3. 效能優化 (JPA)：
* 利用 `JOIN FETCH` 解決 N+1 查詢問題，一次抓取 `OrderGroup` 與 `OrderItem`。
* 配合 `DISTINCT` 關鍵字過濾重複的 JOIN 結果，確保回傳最乾淨的 JSON。
4. 資料隔離 (DTO Pattern)：使用 `OrderGroupDto` 來隔離資料庫實體，防止 JSON 雙向關聯造成的 `StackOverflowError`。

## 📊 Google Sheet 配置規則

為了讓系統正確讀取資料，目前使用時須遵循以下規則：
### 1. 表頭
* 表頭必須位於 **第一列**。
* 欄位名稱需與系統預設一致，但順序可以隨意調整。

### 2. 顯示分頁名稱

系統目前只會同步在「設定」分頁中標註為顯示的分頁表。所以需要先自行新增一個名為 `設定` 的分頁，內容範例如下：

| 分頁名稱 | 顯示 |
| --- | --- |
| 0405【我英原畫展-福岡】 | TRUE |
| 0210【多聞FACE OFF】 | TRUE |
| 0523 測試用 | FALSE |

* **顯示** 欄位支援：`TRUE`, `T`, `1`, `Y`, `YES`, `V`。
* **名稱處理**：若分頁名稱含 `/` 會自動移除（例如 `F/ACE` 轉為 `FACE`）。

### 3. 合併欄位邏輯

* 若同買家有多筆訂單的合併儲存格，系統會自動抓同一買家在該團中的最大值作為顯示結果。

## 專案中的 API

* 買家查詢：`GET /api/orders?nickname={買家暱稱}`
* 手動同步：`POST /api/dev/sync-sheet` (開發與除錯用，讓資料能夠立即同步)
* JSON 回應格式：
```json
{
  "success": true,
  "data": { ... },
  "error": null,
  "timestamp": "2026-02-27T04:37:13"
}
```

## 後續規劃 (Future Roadmap)

* 後台管理介面：手動選擇同步的分頁、查看同步歷史與錯誤提示。
* 後台安全問題：加入 API Key 或 JWT 驗證。