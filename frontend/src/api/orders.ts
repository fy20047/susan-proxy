import { ApiOrderGroup, ApiResponse } from "../types";

export async function fetchOrders(nickname: string): Promise<ApiOrderGroup[]> {
  const resp = await fetch(`/api/orders?nickname=${encodeURIComponent(nickname)}`);
  const payload = (await resp.json()) as ApiResponse<ApiOrderGroup[]>;
  if (!resp.ok || !payload.success) {
    throw new Error(payload.error?.message ?? "查詢失敗");
  }
  return payload.data ?? [];
}
