import { ApiResponse, PageViewStats } from "../types";

export async function recordPageView(): Promise<PageViewStats> {
  const baseUrl = import.meta.env.VITE_API_URL || "";
  const resp = await fetch(`${baseUrl}/api/pv/visit`, { method: "POST" });
  const payload = (await resp.json()) as ApiResponse<PageViewStats>;
  if (!resp.ok || !payload.success || !payload.data) {
    throw new Error(payload.error?.message ?? "瀏覽量更新失敗");
  }
  return payload.data;
}
