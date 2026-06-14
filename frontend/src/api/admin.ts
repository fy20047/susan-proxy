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
  totalSources: number;
  syncedSources: number;
  failedSources: number;
  warnings: AdminSyncWarning[];
};

export type AdminSyncWarning = {
  source?: string;
  sheetName?: string;
  rowNumber?: number;
  buyerNickname?: string;
  itemName?: string;
  message: string;
};

export type SheetSyncSourceType = "STANDARD" | "PREORDER";

export type SheetSyncSource = {
  id: number;
  displayName: string;
  sheetUrl: string;
  defaultSourceType: SheetSyncSourceType;
  lastSyncedAt?: string;
};

export type SheetSyncSettings = {
  autoSyncEnabled: boolean;
  sources: SheetSyncSource[];
};

export type CreateSheetSyncSourceInput = {
  displayName: string;
  sheetUrl: string;
  defaultSourceType: SheetSyncSourceType;
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

export async function fetchSheetSyncSettings(token: string): Promise<SheetSyncSettings> {
  return requestAdmin<SheetSyncSettings>("/api/admin/sheet-sync", {
    method: "GET",
    headers: {
      Authorization: `Bearer ${token}`
    }
  });
}

export async function updateAutoSync(
  token: string,
  autoSyncEnabled: boolean
): Promise<SheetSyncSettings> {
  return requestAdmin<SheetSyncSettings>("/api/admin/sheet-sync/settings/auto-sync", {
    method: "PUT",
    headers: {
      Authorization: `Bearer ${token}`,
      "Content-Type": "application/json"
    },
    body: JSON.stringify({ autoSyncEnabled })
  });
}

export async function createSheetSyncSource(
  token: string,
  input: CreateSheetSyncSourceInput
): Promise<SheetSyncSource> {
  return requestAdmin<SheetSyncSource>("/api/admin/sheet-sync/sources", {
    method: "POST",
    headers: {
      Authorization: `Bearer ${token}`,
      "Content-Type": "application/json"
    },
    body: JSON.stringify(input)
  });
}

export async function deleteSheetSyncSource(token: string, sourceId: number): Promise<void> {
  await requestAdmin<{ deleted: boolean; sourceId: number }>(
    `/api/admin/sheet-sync/sources/${sourceId}`,
    {
      method: "DELETE",
      headers: {
        Authorization: `Bearer ${token}`
      }
    }
  );
}

export async function syncSheetSyncSource(
  token: string,
  sourceId: number
): Promise<AdminSyncResult> {
  return requestAdmin<AdminSyncResult>(`/api/admin/sheet-sync/sources/${sourceId}/sync`, {
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
