import { ApiResponse } from "../types";

const baseUrl = import.meta.env.VITE_API_URL || "";

export type AdminSession = {
  token: string;
  expiresAt: string;
};

export type AdminSyncResult = {
  status: string;
  source: string;
  syncedAt: string;
};

export class AdminApiError extends Error {
  readonly status: number;
  readonly code?: string;

  constructor(message: string, status: number, code?: string) {
    super(message);
    this.name = "AdminApiError";
    this.status = status;
    this.code = code;
  }
}

export async function createAdminSession(
  username: string,
  password: string
): Promise<AdminSession> {
  return requestAdmin<AdminSession>("/api/admin/sessions", {
    method: "POST",
    headers: {
      "Content-Type": "application/json"
    },
    body: JSON.stringify({ username, password })
  });
}

export async function deleteAdminSession(token: string): Promise<void> {
  await requestAdmin<{ status: string }>("/api/admin/sessions/current", {
    method: "DELETE",
    headers: {
      Authorization: `Bearer ${token}`
    }
  });
}

export async function syncGoogleSheet(token: string): Promise<AdminSyncResult> {
  return requestAdmin<AdminSyncResult>("/api/admin/sheet-sync", {
    method: "POST",
    headers: {
      Authorization: `Bearer ${token}`
    }
  });
}

async function requestAdmin<T>(path: string, init: RequestInit): Promise<T> {
  const resp = await fetch(`${baseUrl}${path}`, init);
  const payload = (await resp.json()) as ApiResponse<T>;
  if (!resp.ok || !payload.success || !payload.data) {
    throw new AdminApiError(
      payload.error?.message ?? "後台 API 請求失敗，請稍後再試。",
      resp.status,
      payload.error?.code
    );
  }
  return payload.data;
}
