# Susan Proxy System (俗三代購訂單管理系統)

這是給代購賣家使用的訂單同步與買家查詢系統。賣家可以繼續用 Google Sheet 或 Excel 管理訂單，系統會把可同步的資料整理進資料庫；買家不用登入，只要輸入暱稱，就能查詢自己在各團的訂單、到貨進度與付款狀態。

目前專案包含買家查詢頁與 `/admin` 後台。後台提供管理者登入、Google Sheet 同步來源管理、手動同步、單一來源同步與自動同步開關。

## 技術棧

| 類別 | 技術 |
| --- | --- |
| Backend | Java 21, Spring Boot 3, Spring Data JPA, Hibernate, Lombok |
| Frontend | React 18, TypeScript, Vite, Tailwind CSS, Lucide React |
| Database | MariaDB |
| ETL / Parser | EasyExcel, Apache Commons CSV |
| DevOps | Docker, Docker Compose |

## 核心資料流

1. 後台或部署設定提供 Google Sheet 同步來源。同步來源會保存於資料庫，環境變數主要用於初始化預設來源與部署設定。
2. `SheetSyncService` 讀取 Google Sheet XLSX 匯出內容或本機 CSV，依來源設定與表格欄位解析為一般團 `STANDARD` 或受注團 `PREORDER`。
3. 同步採用 replace-by-source-and-group 的策略：以來源與分頁名稱更新該團資料，避免舊資料殘留。
4. `OrderQueryController` 依買家暱稱查詢訂單，透過 DTO 回傳前端需要的資料，避免直接暴露 JPA entity。
5. 前端在 `transform.ts` 與 `status.ts` 組合訂單摘要、狀態標籤、篩選與快速下單顯示邏輯。

詳細欄位、狀態與端點以程式碼為準，不在 README 複製完整清單。常用 source of truth：

- API：`src/main/java/com/fy20047/susan/controller` 與 `frontend/src/api`
- Sheet 欄位與同步：`SheetRowDto`、`SheetRowListener`、`SheetSyncService`
- 狀態：`ItemStatus`、`ShippingStatus`、`frontend/src/status.ts`
- 前端資料形狀：`frontend/src/types.ts`、`frontend/src/transform.ts`

## 主要功能入口

- 買家查詢：`/api/orders`
- 管理登入：`/api/admin/sessions`
- Sheet 同步管理：`/api/admin/sheet-sync`
- 頁面瀏覽統計：`/api/pv`
- 開發用同步：`/api/dev/*`

管理登入使用後端記憶體中的 bearer session，session 有效期限目前由 `AdminAuthService` 控制。正式部署時應以環境變數設定管理者帳密，不要依賴程式碼中的預設值。

## Google Sheet 使用方式

Sheet 的欄位名稱與解析規則會隨營運表單調整，請以 `SheetRowDto` 與相關測試為準。穩定規則如下：

- 每個訂單分頁需要有系統可辨識的表頭。
- 可以使用名為 `設定` 的分頁控制哪些分頁要顯示或同步。
- 分頁名稱會經過正規化後成為團名顯示與同步比對依據。
- 受注團與一般團會走不同狀態推導規則。

## 本機開發

啟動 MariaDB：

```powershell
docker compose up -d susan-mariadb
```

啟動後端：

```powershell
.\mvnw.cmd spring-boot:run
```

啟動前端：

```powershell
cd frontend
npm install
npm run dev
```

前端 API base URL 使用 `VITE_API_URL` 設定；未設定時會呼叫同源路徑。後端資料庫、port、管理者帳密與 Sheet 來源使用環境變數覆蓋，變數名稱請參考 `src/main/resources/application.yml`，不要在文件或 commit 訊息中重述敏感預設值。

## 驗證

後端測試：

```powershell
mvn -q test
```

或使用 Maven wrapper。若 Windows PowerShell wrapper 入口出錯，可改用已安裝 Maven 或 wrapper cache 中的 `mvn.cmd`。

前端指令以 `frontend/package.json` 為準：

```powershell
cd frontend
npm run lint
npm run build
```

目前前端 lint 可能受既有 ESLint 設定影響而失敗；修正前請先確認錯誤是否與本次變更相關。`npm run build` 可能更新 TypeScript build info，執行前後請檢查 git diff。

## 維護原則

- README 保留穩定產品與架構資訊；容易變動的 API、欄位、狀態細節回到程式碼查。
- 給 coding agent 的專案規則寫在 `AGENTS.md`。
- 若 README 與程式碼衝突，先相信程式碼，再更新 README。
