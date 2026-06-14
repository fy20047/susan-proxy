# AGENTS.md

此檔是給之後的 coding agent 快速理解專案用。它不是完整 API 規格；當文件和程式碼衝突時，先讀程式碼並修正文件。

## 專案定位

Susan Proxy 是俗三代購的訂單同步與買家查詢系統。後端負責 Google Sheet / CSV 同步、訂單查詢、管理者 session 與同步設定；前端提供買家查詢頁與 `/admin` 後台。

## 目錄導覽

- `src/main/java/com/fy20047/susan/controller`：REST API 入口。
- `src/main/java/com/fy20047/susan/service`：同步、狀態解析、PV、管理登入等業務邏輯。
- `src/main/java/com/fy20047/susan/domain`：JPA entity 與 enum。
- `src/main/java/com/fy20047/susan/dto`：API 回傳與請求 DTO。
- `src/test/java/com/fy20047/susan`：後端測試。
- `frontend/src/App.tsx`：前端主要頁面與狀態管理。
- `frontend/src/api`：前端 API client。
- `frontend/src/status.ts`、`frontend/src/transform.ts`、`frontend/src/types.ts`：前端狀態推導、資料轉換與型別。

## Source Of Truth

- API 端點與權限：看 controller 與 `frontend/src/api`，不要只看 README。
- Sheet 欄位：看 `SheetRowDto`、`SheetVisibilityRow`、`SheetRowListener`、`SheetSyncService`。
- 同步流程：看 `SheetSyncService`、`SheetRowListener`、`SheetSyncWriter`。
- 訂單狀態：後端看 `ItemStatus`、`ShippingStatus`、`StatusResolver`；前端看 `status.ts`。
- 前端顯示資料：看 `types.ts`、`transform.ts`、`OrderCard.tsx`。
- 部署與環境變數：看 `application.yml`、`Dockerfile`、`docker-compose.yaml`，但不要在回覆、文件或 commit 中重述敏感預設值。

## 工作規則

- 開始前先跑 `git status --short`，確認是否有使用者未交代的變更。
- 不要覆蓋或回復不是你造成的變更。
- 手動改檔使用 `apply_patch`。
- 優先維持既有架構與命名；避免順手重構。
- README 只放穩定資訊。容易過時的 API 細節、Sheet 欄位、狀態表，應指向 source of truth。
- 若任務碰到同步、狀態、欄位解析，優先補或更新對應後端測試。
- 若任務碰到前端資料呈現，檢查 `transform.ts` 和 `status.ts` 是否也需要同步調整。

## Commit 規則

Commit 訊息使用繁體中文摘要，格式：

```text
type: 繁中摘要
```

常用 type：

- `feat`: 新功能
- `fix`: 修 bug
- `style`: UI 或格式調整
- `refactor`: 不改行為的重構
- `test`: 測試
- `docs`: 文件
- `chore`: 維護雜項

範例：

```text
docs: 更新專案 agent 規格
fix: 修正受注團狀態判斷
style: 調整快速下單區塊
```

## 驗證

後端：

```powershell
mvn -q test
```

或使用 Maven wrapper。若 Windows PowerShell wrapper 入口失敗，可改用已安裝 Maven 或 wrapper cache 中的 `mvn.cmd`。

前端：

```powershell
cd frontend
npm run lint
npm run build
```

目前前端 lint 有既存 ESLint 設定問題，常見為 React/DOM globals 或 TypeScript function type 參數被 `no-unused-vars` 誤判。不要未確認就把既存 lint 失敗歸因於本次變更。

`npm run build` 可能更新 tracked 的 `frontend/tsconfig*.tsbuildinfo`。執行前後要看 git diff，避免把無關 build info 變更混進提交。

## 安全與敏感資訊

- 不要在回覆、README、AGENTS.md、commit 訊息或 PR 說明中重述 `application.yml` 裡的預設管理者帳密或 Sheet URL。
- 文件可提環境變數名稱，例如 `ADMIN_USERNAME`、`ADMIN_PASSWORD`、`STANDARD_GOOGLE_SHEET_URL`、`PREORDER_GOOGLE_SHEET_URL`、`DB_URL`。
- 後台目前使用 `AdminAuthService` 的 2 小時 bearer session；不要描述成 JWT 或 Spring Security，除非程式碼已改。
