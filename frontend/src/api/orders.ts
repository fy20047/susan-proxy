import { ApiOrderGroup, ApiResponse } from "../types";

export async function fetchOrders(nickname: string): Promise<ApiOrderGroup[]> {
  // 透過環境變數取得後端網址，如果沒有就預設為空字串 (Fallback)
  const baseUrl = import.meta.env.VITE_API_URL || '';
  const resp = await fetch(`${baseUrl}/api/orders?nickname=${encodeURIComponent(nickname)}`);
  const payload = (await resp.json()) as ApiResponse<ApiOrderGroup[]>;
  if (!resp.ok || !payload.success) {
    throw new Error(payload.error?.message ?? "查詢失敗，請稍後再試。");
  }
  return payload.data ?? [];
}
