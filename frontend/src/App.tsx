import { useEffect, useMemo, useState } from "react";
import {
  AlertCircle,
  ArrowLeft,
  CheckCircle2,
  ChevronDown,
  Copy,
  ExternalLink,
  LockKeyhole,
  LogIn,
  LogOut,
  Package,
  RefreshCw,
  Search
} from "lucide-react";
import {
  AdminApiError,
  AdminSession,
  createAdminSession,
  deleteAdminSession,
  syncGoogleSheet
} from "./api/admin";
import { fetchOrders } from "./api/orders";
import { recordPageView } from "./api/pageViews";
import OrderCard from "./components/OrderCard";
import {
  matchesPreorderFilters,
  PREORDER_ITEM_FILTERS,
  PREORDER_SHIPPING_FILTERS,
  STANDARD_STATUS_FILTERS,
  toShippingStatusLabel,
  toStandardOrderStatusLabel
} from "./status";
import { buildOrderView, rebuildOrderView } from "./transform";
import {
  OrderView,
  PageViewStats,
  PreorderItemStatusCode,
  ShippingStatusCode,
  StandardItemStatusCode
} from "./types";
import logo from "./image/logo1.png";
import icon from "./image/icon.png";

const SELLER_STORE_URL = "https://myship.7-11.com.tw/general/detail/GM2602284842246";
const ADMIN_SESSION_STORAGE_KEY = "susan-admin-session";

type AppPage = "search" | "results" | "admin-login" | "admin-dashboard";

function isAdminPath(): boolean {
  return window.location.pathname === "/admin" || window.location.pathname.startsWith("/admin/");
}

function loadStoredAdminSession(): AdminSession | null {
  const raw = window.localStorage.getItem(ADMIN_SESSION_STORAGE_KEY);
  if (!raw) {
    return null;
  }

  try {
    const session = JSON.parse(raw) as AdminSession;
    const expiresAt = new Date(session.expiresAt);
    if (!session.token || Number.isNaN(expiresAt.getTime()) || expiresAt <= new Date()) {
      window.localStorage.removeItem(ADMIN_SESSION_STORAGE_KEY);
      return null;
    }
    return session;
  } catch {
    window.localStorage.removeItem(ADMIN_SESSION_STORAGE_KEY);
    return null;
  }
}

function storeAdminSession(session: AdminSession) {
  window.localStorage.setItem(ADMIN_SESSION_STORAGE_KEY, JSON.stringify(session));
}

function clearStoredAdminSession() {
  window.localStorage.removeItem(ADMIN_SESSION_STORAGE_KEY);
}

function formatDateTime(value: string): string {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  return date.toLocaleString("zh-TW", { hour12: false });
}

export default function App() {
  const [adminSession, setAdminSession] = useState<AdminSession | null>(() => loadStoredAdminSession());
  const [currentPage, setCurrentPage] = useState<AppPage>(() => {
    if (!isAdminPath()) {
      return "search";
    }
    return loadStoredAdminSession() ? "admin-dashboard" : "admin-login";
  });
  const [searchName, setSearchName] = useState("");
  const [currentSearchName, setCurrentSearchName] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [adminUsername, setAdminUsername] = useState("");
  const [adminPassword, setAdminPassword] = useState("");
  const [isAdminLoggingIn, setIsAdminLoggingIn] = useState(false);
  const [adminLoginError, setAdminLoginError] = useState<string | null>(null);
  const [isSyncing, setIsSyncing] = useState(false);
  const [syncMessage, setSyncMessage] = useState<string | null>(null);
  const [syncError, setSyncError] = useState<string | null>(null);
  const [orders, setOrders] = useState<OrderView[]>([]);
  const [standardFilter, setStandardFilter] = useState<StandardItemStatusCode | "ALL">("ALL");
  const [standardShippingFilter, setStandardShippingFilter] = useState<ShippingStatusCode | "ALL">("ALL");
  const [preorderItemFilter, setPreorderItemFilter] = useState<PreorderItemStatusCode | "ALL">("ALL");
  const [preorderShippingFilter, setPreorderShippingFilter] = useState<ShippingStatusCode | "ALL">("ALL");
  const [error, setError] = useState<string | null>(null);
  const [pageViews, setPageViews] = useState<PageViewStats | null>(null);
  const [selectedQuickOrderIds, setSelectedQuickOrderIds] = useState<number[]>([]);
  const [quickOrderMessage, setQuickOrderMessage] = useState<string | null>(null);

  useEffect(() => {
    recordPageView()
      .then((stats) => setPageViews(stats))
      .catch(() => setPageViews(null));
  }, []);

  const preorderOrders = useMemo(
    () => orders.filter((order) => order.sourceType === "PREORDER"),
    [orders]
  );
  const standardOrders = useMemo(
    () => orders.filter((order) => order.sourceType !== "PREORDER"),
    [orders]
  );

  const filteredPreorderOrders = useMemo(() => {
    if (preorderItemFilter === "ALL" && preorderShippingFilter === "ALL") {
      return preorderOrders;
    }

    return preorderOrders.reduce<OrderView[]>((acc, order) => {
      const items = order.items.filter((item) =>
        matchesPreorderFilters(item, preorderItemFilter, preorderShippingFilter)
      );
      if (!items.length) {
        return acc;
      }
      acc.push(rebuildOrderView(order, items));
      return acc;
    }, []);
  }, [preorderOrders, preorderItemFilter, preorderShippingFilter]);

  const filteredStandardOrders = useMemo(() => {
    if (standardFilter === "ALL" && standardShippingFilter === "ALL") {
      return standardOrders;
    }

    return standardOrders.reduce<OrderView[]>((acc, order) => {
      const items = order.items.filter((item) => {
        const matchesItemStatus = standardFilter === "ALL" || item.statusCode === standardFilter;
        const matchesShippingStatus =
          standardShippingFilter === "ALL" || item.shippingStatusCode === standardShippingFilter;
        return matchesItemStatus && matchesShippingStatus;
      });
      if (!items.length) {
        return acc;
      }
      acc.push(rebuildOrderView(order, items));
      return acc;
    }, []);
  }, [standardOrders, standardFilter, standardShippingFilter]);

  const quickOrderEligibleOrders = useMemo(
    () =>
      orders.reduce<OrderView[]>((acc, order) => {
        const items = order.items.filter(isQuickOrderItem);
        if (items.length) {
          acc.push(rebuildOrderView(order, items));
        }
        return acc;
      }, []),
    [orders]
  );

  const selectedQuickOrders = useMemo(() => {
    const selectedIds = new Set(selectedQuickOrderIds);
    return quickOrderEligibleOrders.filter((order) => selectedIds.has(order.id));
  }, [quickOrderEligibleOrders, selectedQuickOrderIds]);

  const selectedQuickOrderBalance = useMemo(
    () => selectedQuickOrders.reduce((sum, order) => sum + order.balanceAmount, 0),
    [selectedQuickOrders]
  );
  const hasCheckedQuickOrder = selectedQuickOrderIds.length > 0;
  const showQuickOrderPanel = hasCheckedQuickOrder && selectedQuickOrders.length > 0;

  useEffect(() => {
    const selectableIds = new Set(quickOrderEligibleOrders.map((order) => order.id));
    setSelectedQuickOrderIds((prev) => prev.filter((id) => selectableIds.has(id)));
  }, [quickOrderEligibleOrders]);

  const lastUpdatedLabel = useMemo(() => {
    const timestamps = orders
      .map((order) => order.lastUpdated)
      .filter((value): value is string => Boolean(value))
      .map((value) => new Date(value))
      .filter((date) => !Number.isNaN(date.getTime()));
    if (!timestamps.length) {
      return null;
    }
    const latest = new Date(Math.max(...timestamps.map((date) => date.getTime())));
    return latest.toLocaleString("zh-TW", { hour12: false });
  }, [orders]);

  const showPreorderStatus =
    preorderItemFilter !== "ALL" || preorderShippingFilter !== "ALL";
  const showStandardStatus = standardFilter !== "ALL" || standardShippingFilter !== "ALL";
  const preorderEmptyLabel =
    preorderShippingFilter !== "ALL"
      ? toShippingStatusLabel(preorderShippingFilter)
      : preorderItemFilter === "ALL"
        ? "全部"
        : PREORDER_ITEM_FILTERS.find((filter) => filter.key === preorderItemFilter)?.label ?? "全部";
  const standardEmptyLabel =
    standardShippingFilter !== "ALL"
      ? toShippingStatusLabel(standardShippingFilter)
      : standardFilter === "ALL"
        ? "全部"
        : toStandardOrderStatusLabel(standardFilter);

  const handleAdminLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    const normalizedUsername = adminUsername.trim();
    const normalizedPassword = adminPassword.trim();
    if (!normalizedUsername || !normalizedPassword) {
      setAdminLoginError("請輸入帳號與密碼。");
      return;
    }

    setAdminLoginError(null);
    setIsAdminLoggingIn(true);
    try {
      const session = await createAdminSession(normalizedUsername, normalizedPassword);
      storeAdminSession(session);
      setAdminSession(session);
      setAdminPassword("");
      setSyncMessage(null);
      setSyncError(null);
      setCurrentPage("admin-dashboard");
      window.history.replaceState(null, "", "/admin");
    } catch (err) {
      const message =
        err instanceof Error ? err.message : "登入失敗，請稍後再試。";
      setAdminLoginError(message);
    } finally {
      setIsAdminLoggingIn(false);
    }
  };

  const handleAdminLogout = async () => {
    const token = adminSession?.token;
    if (token) {
      try {
        await deleteAdminSession(token);
      } catch {
        // Local session cleanup still needs to happen even if the API call fails.
      }
    }

    clearStoredAdminSession();
    setAdminSession(null);
    setAdminPassword("");
    setAdminLoginError(null);
    setSyncMessage(null);
    setSyncError(null);
    setCurrentPage("admin-login");
    window.history.replaceState(null, "", "/admin");
  };

  const handleAdminSync = async () => {
    if (!adminSession) {
      clearStoredAdminSession();
      setCurrentPage("admin-login");
      return;
    }

    setIsSyncing(true);
    setSyncMessage(null);
    setSyncError(null);
    try {
      const result = await syncGoogleSheet(adminSession.token);
      setSyncMessage(`同步完成：${formatDateTime(result.syncedAt)}`);
    } catch (err) {
      if (err instanceof AdminApiError && err.status === 401) {
        clearStoredAdminSession();
        setAdminSession(null);
        setCurrentPage("admin-login");
        setAdminLoginError("登入已逾時，請重新登入。");
        return;
      }

      const message =
        err instanceof Error ? err.message : "同步失敗，請稍後再試。";
      setSyncError(message);
    } finally {
      setIsSyncing(false);
    }
  };

  const handleBackToSearch = () => {
    setCurrentPage("search");
    window.history.replaceState(null, "", "/");
  };

  const handleSearch = async (e: React.FormEvent) => {
    e.preventDefault();
    const normalized = searchName.trim();
    if (!normalized) {
      setError("請輸入完整暱稱進行查詢");
      return;
    }

    setError(null);
    setIsLoading(true);
    try {
      const data = await fetchOrders(normalized);
      const views = sortOrdersByGroupDateDesc(data.map(buildOrderView));
      setOrders(views);
      setCurrentSearchName(normalized);
      setCurrentPage("results");
      setStandardFilter("ALL");
      setStandardShippingFilter("ALL");
      setPreorderItemFilter("ALL");
      setPreorderShippingFilter("ALL");
      setSelectedQuickOrderIds([]);
      setQuickOrderMessage(null);
    } catch (err) {
      const message =
        err instanceof Error ? err.message : "搜尋發生錯誤，請稍後再試。";
      setError(message);
    } finally {
      setIsLoading(false);
    }
  };

  const handleQuickOrderSelectChange = (orderId: number, checked: boolean) => {
    setQuickOrderMessage(null);
    setSelectedQuickOrderIds((prev) => {
      if (checked) {
        return prev.includes(orderId) ? prev : [...prev, orderId];
      }
      return prev.filter((id) => id !== orderId);
    });
  };

  const handleCopyQuickOrderDetails = async () => {
    if (!selectedQuickOrders.length) {
      return;
    }

    const summaryText = selectedQuickOrders.map((order) => order.groupName).join("\n");
    try {
      await navigator.clipboard.writeText(summaryText);
      setQuickOrderMessage(`已複製 ${selectedQuickOrders.length} 筆團名。`);
    } catch {
      setQuickOrderMessage("複製失敗，請確認瀏覽器是否允許剪貼簿權限。");
    }
  };

  const handleQuickOrderSubmit = () => {
    if (!selectedQuickOrders.length) {
      return;
    }

    setQuickOrderMessage(null);
    window.open(SELLER_STORE_URL, "_blank", "noopener,noreferrer");
  };

  return (
    <div
      className="min-h-screen font-serif bg-[#EBE3CC] text-[#2C1E16] selection:bg-[#BC4A3C] selection:text-[#EBE3CC] flex flex-col"
      style={{
        backgroundImage: "radial-gradient(#D5CBB3 1px, transparent 1px)",
        backgroundSize: "20px 20px"
      }}
    >
      {currentPage === "admin-login" && (
        <AdminLoginScreen
          username={adminUsername}
          password={adminPassword}
          error={adminLoginError}
          isLoading={isAdminLoggingIn}
          onUsernameChange={setAdminUsername}
          onPasswordChange={setAdminPassword}
          onSubmit={handleAdminLogin}
          onBackToSearch={handleBackToSearch}
        />
      )}

      {currentPage === "admin-dashboard" && (
        <AdminDashboardScreen
          username={adminUsername || "管理員"}
          expiresAt={adminSession?.expiresAt}
          isSyncing={isSyncing}
          syncMessage={syncMessage}
          syncError={syncError}
          onSync={handleAdminSync}
          onLogout={handleAdminLogout}
          onBackToSearch={handleBackToSearch}
        />
      )}

      {currentPage === "search" && (
        <div className="flex-1 flex flex-col">
          <div className="flex-1 flex items-center justify-center p-6">
            <div className="w-full max-w-lg">
              <header className="mb-10 text-center">
                <div className="absolute top-0 left-0 w-full h-2 bg-[#BC4A3C]"></div>
                <h1 className="text-3xl md:text-4xl font-black tracking-widest mb-3 flex flex-col items-center gap-2">
                  <div className="flex justify-center">
                    <img
                      src={logo}
                      alt="Susan 代購系統 Logo"
                      className="h-60 w-auto object-contain"
                    />
                  </div>
                </h1>
                <p className="text-lg md:text-xl font-black tracking-[0.2em] text-[#2A5C5B] border-[#2C1E16] py-2">
                  日本動漫代購
                </p>
                <p className="text-3xl font-black tracking-[0.2em] text-[#000000] border-[#2C1E16]">
                  訂單查詢系統
                </p>
              </header>

              <form
                onSubmit={handleSearch}
                className="bg-white border-4 border-[#2C1E16] p-6 md:p-8 shadow-[8px_8px_0px_#2C1E16]"
              >
                <div className="mb-6 relative">
                  <input
                    type="text"
                    value={searchName}
                    onChange={(e) => setSearchName(e.target.value)}
                    placeholder="請輸入完整暱稱進行查詢"
                    className="w-full px-4 py-4 bg-[#F5F0E6] border-4 border-[#2C1E16] text-lg font-bold placeholder-[#2C1E16]/40 focus:outline-none focus:bg-white transition-colors text-center shadow-[inset_2px_2px_0px_rgba(0,0,0,0.1)]"
                    required
                  />
                </div>

                <button
                  type="submit"
                  disabled={isLoading}
                  className="w-full py-4 bg-[#BC4A3C] text-[#EBE3CC] font-black text-xl border-4 border-[#2C1E16] shadow-[4px_4px_0px_#2C1E16] hover:translate-y-[2px] hover:translate-x-[2px] hover:shadow-[2px_2px_0px_#2C1E16] active:translate-y-[4px] active:translate-x-[4px] active:shadow-none transition-all flex justify-center items-center gap-2"
                >
                  {isLoading ? (
                    <span className="animate-pulse">檢索中...</span>
                  ) : (
                    <>
                      <Search size={24} /> 查 詢
                    </>
                  )}
                </button>

                {error ? (
                  <div className="mt-6 flex items-start gap-2 text-[#BC4A3C] bg-[#EBE3CC] p-3 border-2 border-[#2C1E16]">
                    <AlertCircle size={20} className="flex-shrink-0 mt-0.5" />
                    <p className="text-sm font-bold leading-relaxed">{error}</p>
                  </div>
                ) : (
                  <div className="mt-6 flex flex-col gap-1 text-[#BC4A3C] bg-[#EBE3CC] p-3 border-2 border-[#2C1E16]">
                    <div className="flex items-start gap-2">
                      <AlertCircle size={20} className="flex-shrink-0 mt-0.5" />
                      <p className="text-sm font-bold leading-relaxed">
                        注意：請輸入您在群組喊單的 <strong>完整暱稱</strong> 。
                      </p>
                    </div>
                    <div className="flex items-start gap-2">
                      <AlertCircle size={20} className="flex-shrink-0 mt-0.5" />
                      <p className="text-sm font-bold leading-relaxed">
                        如有變更暱稱，請務必聯繫官方LINE進行登記，以免查詢不到您的訂單。
                      </p>
                    </div>
                  </div>
                )}
              </form>
            </div>
          </div>
        </div>
      )}

      {currentPage === "results" && (
        <div className="flex-1">
          <div className="max-w-4xl mx-auto p-4 md:p-8 pt-8">
            <div className="flex flex-row justify-between items-end mb-8 border-b-4 border-[#2C1E16] pb-4 relative w-full">
              <button
                onClick={() => setCurrentPage("search")}
                className="flex items-center gap-2 font-bold px-4 py-2 bg-white border-2 border-[#2C1E16] shadow-[2px_2px_0px_#2C1E16] hover:bg-[#F5F0E6] transition-colors shrink-0 z-20"
              >
                <ArrowLeft size={18} />
                <span className="hidden xs:inline">返回查詢</span>
              </button>
              <div className="flex flex-row items-end flex-1 justify-end">
                <div className="shrink-0 z-10 relative -mb-[21px] order-2">
                  <img
                    src={icon}
                    alt="訂單圖示"
                    className="h-20 md:h-20 w-auto object-contain block"
                  />
                </div>
                <h2 className="text-2xl md:text-3xl font-black leading-tight text-right order-1 pr-1">
                  <span>
                    『
                    <span className="text-[#BC4A3C] underline decoration-[#D9A036] decoration-4 underline-offset-4">
                      {currentSearchName}
                    </span>
                    』買了蝦咪
                  </span>
                </h2>
              </div>
            </div>

            {lastUpdatedLabel && (
              <div className="mb-6 text-right">
                <p className="text-xs md:text-sm font-bold text-[#2A5C5B]">
                  最後更新：{lastUpdatedLabel}
                </p>
              </div>
            )}

                {showQuickOrderPanel && (
                  <div className="mb-6 bg-white border-4 border-[#2C1E16] p-4 md:p-5 shadow-[4px_4px_0px_#2C1E16]">
                    <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
                      <div className="space-y-1">
                        <p className="text-lg font-black text-[#2C1E16]">快速下單</p>
                        <p className="text-sm md:text-base font-bold text-[#2A5C5B]">
                          已選 {selectedQuickOrders.length} 筆，可出貨尾款合計 NT$ {selectedQuickOrderBalance.toLocaleString()}
                        </p>
                        <p className="text-xs md:text-sm font-bold text-[#2C1E16]/70">
                          勾選要一起出貨的團名後，可直接複製明細並前往賣貨便賣場。
                        </p>
                        {quickOrderMessage && (
                          <p className="text-sm font-bold text-[#BC4A3C]">{quickOrderMessage}</p>
                        )}
                      </div>

                      <div className="flex flex-col sm:flex-row gap-3 sm:items-center">
                        <button
                          type="button"
                          onClick={handleCopyQuickOrderDetails}
                          disabled={!selectedQuickOrders.length}
                          className="flex items-center justify-center gap-2 px-4 py-3 bg-white border-2 border-[#2C1E16] font-black shadow-[3px_3px_0px_#2C1E16] hover:bg-[#F5F0E6] disabled:cursor-not-allowed disabled:opacity-50 disabled:shadow-none"
                        >
                          <Copy size={18} />
                          <span>複製明細</span>
                        </button>
                        <button
                          type="button"
                          onClick={handleQuickOrderSubmit}
                          disabled={!selectedQuickOrders.length}
                          className="flex items-center justify-center gap-2 px-4 py-3 bg-[#BC4A3C] text-[#EBE3CC] border-2 border-[#2C1E16] font-black shadow-[3px_3px_0px_#2C1E16] hover:bg-[#A33E33] disabled:cursor-not-allowed disabled:opacity-50 disabled:shadow-none"
                        >
                          <ExternalLink size={18} />
                          <span>下單</span>
                        </button>
                      </div>
                    </div>
                  </div>
                )}


            {preorderOrders.length > 0 && (
              <section className="mb-12">
                <div className="mb-4 flex items-center justify-between gap-4 border-l-4 border-[#BC4A3C] pl-4">
                  <h3 className="text-xl md:text-2xl font-black">受注團</h3>
                  {/*<span className="text-sm font-bold text-[#2A5C5B]">*/}
                  {/*  依貨況與出貨狀態篩選*/}
                  {/*</span>*/}
                </div>

                <div className="mb-4 overflow-x-auto pb-2 hide-scrollbar">
                  <div className="mb-2 px-1 text-base font-black text-[#2C1E16]">
                    商品貨況
                  </div>
                  <div className="flex gap-3 min-w-max px-1">
                    {PREORDER_ITEM_FILTERS.map((status) => (
                      <button
                        key={status.key}
                        onClick={() => setPreorderItemFilter(status.key)}
                        className={`px-5 py-2 font-black border-2 border-[#2C1E16] transition-all ${
                          preorderItemFilter === status.key
                            ? "bg-[#2A5C5B] text-[#EBE3CC] shadow-[inset_3px_3px_0px_rgba(0,0,0,0.3)] translate-y-[2px] translate-x-[2px]"
                            : "bg-white text-[#2C1E16] shadow-[4px_4px_0px_#2C1E16] hover:bg-[#F5F0E6] hover:translate-y-[1px] hover:translate-x-[1px] hover:shadow-[3px_3px_0px_#2C1E16]"
                        }`}
                      >
                        {status.label}
                      </button>
                    ))}
                  </div>
                </div>

                <FilterMenu
                  label="出貨進度"
                  value={preorderShippingFilter}
                  options={PREORDER_SHIPPING_FILTERS}
                  onChange={(value) => setPreorderShippingFilter(value)}
                />

                {filteredPreorderOrders.length > 0 ? (
                  filteredPreorderOrders.map((order) => (
                    <OrderCard
                      key={order.id}
                      order={order}
                      showStatus={showPreorderStatus}
                      quickOrderSelectable={hasQuickOrderItems(order)}
                      quickOrderSelected={selectedQuickOrderIds.includes(order.id)}
                      onQuickOrderSelectChange={handleQuickOrderSelectChange}
                    />
                  ))
                ) : (
                  <EmptyState label={preorderEmptyLabel} />
                )}
              </section>
            )}

            {standardOrders.length > 0 && (
              <section>
                <div className="mb-4 flex items-center justify-between gap-4 border-l-4 border-[#2A5C5B] pl-4">
                  <h3 className="text-xl md:text-2xl font-black">一般團</h3>
                  {/*<span className="text-sm font-bold text-[#2A5C5B]">*/}
                  {/*  維持原本狀態篩選*/}
                  {/*</span>*/}
                </div>

                <div className="mb-4 overflow-x-auto pb-2 hide-scrollbar">
                  <div className="mb-2 px-1 text-base font-black text-[#2C1E16]">
                    商品貨況
                  </div>
                  <div className="flex gap-3 min-w-max px-1">
                    {STANDARD_STATUS_FILTERS.map((status) => (
                      <button
                        key={status.key}
                        onClick={() => setStandardFilter(status.key)}
                        className={`px-5 py-2 font-black border-2 border-[#2C1E16] transition-all ${
                          standardFilter === status.key
                            ? "bg-[#2A5C5B] text-[#EBE3CC] shadow-[inset_3px_3px_0px_rgba(0,0,0,0.3)] translate-y-[2px] translate-x-[2px]"
                            : "bg-white text-[#2C1E16] shadow-[4px_4px_0px_#2C1E16] hover:bg-[#F5F0E6] hover:translate-y-[1px] hover:translate-x-[1px] hover:shadow-[3px_3px_0px_#2C1E16]"
                        }`}
                      >
                        {status.label}
                      </button>
                    ))}
                  </div>
                </div>

                <FilterMenu
                  label="出貨進度"
                  value={standardShippingFilter}
                  options={PREORDER_SHIPPING_FILTERS}
                  onChange={(value) => setStandardShippingFilter(value)}
                />

                {filteredStandardOrders.length > 0 ? (
                  filteredStandardOrders.map((order) => (
                    <OrderCard
                      key={order.id}
                      order={order}
                      showStatus={showStandardStatus}
                      quickOrderSelectable={hasQuickOrderItems(order)}
                      quickOrderSelected={selectedQuickOrderIds.includes(order.id)}
                      onQuickOrderSelectChange={handleQuickOrderSelectChange}
                    />
                  ))
                ) : (
                  <EmptyState label={standardEmptyLabel} />
                )}
              </section>
            )}
          </div>
        </div>
      )}

      <footer className="py-6 text-center text-sm font-bold text-[#2A5C5B]">
        <p>© 2026 俗三連線中. All Rights Reserved.</p>
        <p className="mt-2 text-s text-[#2A5C5B]">
          瀏覽量：今日 {pageViews?.daily ?? "--"} / 本周 {pageViews?.weekly ?? "--"} / 本月 {pageViews?.monthly ?? "--"} / 總計 {pageViews?.total ?? "--"}
        </p>
      </footer>
    </div>
  );
}

function AdminLoginScreen({
  username,
  password,
  error,
  isLoading,
  onUsernameChange,
  onPasswordChange,
  onSubmit,
  onBackToSearch
}: {
  username: string;
  password: string;
  error: string | null;
  isLoading: boolean;
  onUsernameChange: (value: string) => void;
  onPasswordChange: (value: string) => void;
  onSubmit: (event: React.FormEvent) => void;
  onBackToSearch: () => void;
}) {
  return (
    <div className="flex-1 flex flex-col">
      <div className="flex-1 flex items-center justify-center p-6">
        <div className="w-full max-w-lg">
          <header className="mb-10 text-center">
            <div className="absolute top-0 left-0 w-full h-2 bg-[#BC4A3C]"></div>
            <div className="mb-4 flex justify-center">
              <img
                src={logo}
                alt="Susan 代購系統 Logo"
                className="h-44 w-auto object-contain"
              />
            </div>
            <p className="text-lg md:text-xl font-black tracking-[0.2em] text-[#2A5C5B] py-2">
              後台管理
            </p>
            <p className="text-3xl font-black tracking-[0.2em] text-[#000000]">
              管理員登入
            </p>
          </header>

          <form
            onSubmit={onSubmit}
            className="bg-white border-4 border-[#2C1E16] p-6 md:p-8 shadow-[8px_8px_0px_#2C1E16]"
          >
            <label className="mb-2 block px-1 text-base font-black text-[#2C1E16]">
              帳號
            </label>
            <input
              type="text"
              value={username}
              onChange={(e) => onUsernameChange(e.target.value)}
              autoComplete="username"
              className="mb-5 w-full px-4 py-4 bg-[#F5F0E6] border-4 border-[#2C1E16] text-lg font-bold focus:outline-none focus:bg-white transition-colors shadow-[inset_2px_2px_0px_rgba(0,0,0,0.1)]"
              required
            />

            <label className="mb-2 block px-1 text-base font-black text-[#2C1E16]">
              密碼
            </label>
            <input
              type="password"
              value={password}
              onChange={(e) => onPasswordChange(e.target.value)}
              autoComplete="current-password"
              className="mb-6 w-full px-4 py-4 bg-[#F5F0E6] border-4 border-[#2C1E16] text-lg font-bold focus:outline-none focus:bg-white transition-colors shadow-[inset_2px_2px_0px_rgba(0,0,0,0.1)]"
              required
            />

            <button
              type="submit"
              disabled={isLoading}
              className="w-full py-4 bg-[#BC4A3C] text-[#EBE3CC] font-black text-xl border-4 border-[#2C1E16] shadow-[4px_4px_0px_#2C1E16] hover:translate-y-[2px] hover:translate-x-[2px] hover:shadow-[2px_2px_0px_#2C1E16] active:translate-y-[4px] active:translate-x-[4px] active:shadow-none transition-all flex justify-center items-center gap-2 disabled:cursor-not-allowed disabled:opacity-60 disabled:shadow-none"
            >
              {isLoading ? (
                <span className="animate-pulse">登入中...</span>
              ) : (
                <>
                  <LogIn size={24} /> 登入
                </>
              )}
            </button>

            {error && (
              <div className="mt-6 flex items-start gap-2 text-[#BC4A3C] bg-[#EBE3CC] p-3 border-2 border-[#2C1E16]">
                <AlertCircle size={20} className="flex-shrink-0 mt-0.5" />
                <p className="text-sm font-bold leading-relaxed">{error}</p>
              </div>
            )}
          </form>

          <button
            type="button"
            onClick={onBackToSearch}
            className="mt-6 mx-auto flex items-center gap-2 font-bold px-4 py-2 bg-white border-2 border-[#2C1E16] shadow-[2px_2px_0px_#2C1E16] hover:bg-[#F5F0E6] transition-colors"
          >
            <ArrowLeft size={18} />
            返回查詢
          </button>
        </div>
      </div>
    </div>
  );
}

function AdminDashboardScreen({
  username,
  expiresAt,
  isSyncing,
  syncMessage,
  syncError,
  onSync,
  onLogout,
  onBackToSearch
}: {
  username: string;
  expiresAt?: string;
  isSyncing: boolean;
  syncMessage: string | null;
  syncError: string | null;
  onSync: () => void;
  onLogout: () => void;
  onBackToSearch: () => void;
}) {
  return (
    <div className="flex-1">
      <div className="max-w-3xl mx-auto p-4 md:p-8 pt-8">
        <div className="flex flex-row justify-between items-end mb-8 border-b-4 border-[#2C1E16] pb-4 relative w-full">
          <button
            type="button"
            onClick={onBackToSearch}
            className="flex items-center gap-2 font-bold px-4 py-2 bg-white border-2 border-[#2C1E16] shadow-[2px_2px_0px_#2C1E16] hover:bg-[#F5F0E6] transition-colors shrink-0 z-20"
          >
            <ArrowLeft size={18} />
            <span className="hidden xs:inline">返回查詢</span>
          </button>
          <div className="flex flex-row items-end flex-1 justify-end">
            <div className="shrink-0 z-10 relative -mb-[21px] order-2">
              <img
                src={icon}
                alt="後台圖示"
                className="h-20 md:h-20 w-auto object-contain block"
              />
            </div>
            <h2 className="text-2xl md:text-3xl font-black leading-tight text-right order-1 pr-1">
              <span>
                Susan
                <span className="text-[#BC4A3C] underline decoration-[#D9A036] decoration-4 underline-offset-4">
                  後台
                </span>
              </span>
            </h2>
          </div>
        </div>

        <section className="bg-white border-4 border-[#2C1E16] p-5 md:p-6 shadow-[8px_8px_0px_#2C1E16]">
          <div className="mb-6 flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
            <div className="flex items-center gap-4">
              <div className="flex h-12 w-12 items-center justify-center border-2 border-[#2C1E16] bg-[#2A5C5B] text-[#EBE3CC] shadow-[2px_2px_0px_#2C1E16]">
                <LockKeyhole size={24} />
              </div>
              <div>
                <p className="text-lg font-black text-[#2C1E16]">{username}</p>
                {expiresAt && (
                  <p className="text-xs md:text-sm font-bold text-[#2A5C5B]">
                    登入有效至：{formatDateTime(expiresAt)}
                  </p>
                )}
              </div>
            </div>

            <button
              type="button"
              onClick={onLogout}
              className="flex items-center justify-center gap-2 px-4 py-3 bg-white border-2 border-[#2C1E16] font-black shadow-[3px_3px_0px_#2C1E16] hover:bg-[#F5F0E6] transition-colors"
            >
              <LogOut size={18} />
              登出
            </button>
          </div>

          <div className="border-t-4 border-[#2C1E16] pt-6">
            <p className="mb-4 text-xl md:text-2xl font-black text-[#2C1E16]">
              Google Sheet 同步
            </p>
            <button
              type="button"
              onClick={onSync}
              disabled={isSyncing}
              className="w-full py-4 bg-[#BC4A3C] text-[#EBE3CC] font-black text-xl border-4 border-[#2C1E16] shadow-[4px_4px_0px_#2C1E16] hover:translate-y-[2px] hover:translate-x-[2px] hover:shadow-[2px_2px_0px_#2C1E16] active:translate-y-[4px] active:translate-x-[4px] active:shadow-none transition-all flex justify-center items-center gap-2 disabled:cursor-not-allowed disabled:opacity-60 disabled:shadow-none"
            >
              <RefreshCw size={24} className={isSyncing ? "animate-spin" : ""} />
              {isSyncing ? "同步中..." : "同步 Google Sheet"}
            </button>

            {syncMessage && (
              <div className="mt-6 flex items-start gap-2 text-[#2A5C5B] bg-[#EBE3CC] p-3 border-2 border-[#2C1E16]">
                <CheckCircle2 size={20} className="flex-shrink-0 mt-0.5" />
                <p className="text-sm font-bold leading-relaxed">{syncMessage}</p>
              </div>
            )}

            {syncError && (
              <div className="mt-6 flex items-start gap-2 text-[#BC4A3C] bg-[#EBE3CC] p-3 border-2 border-[#2C1E16]">
                <AlertCircle size={20} className="flex-shrink-0 mt-0.5" />
                <p className="text-sm font-bold leading-relaxed">{syncError}</p>
              </div>
            )}
          </div>
        </section>
      </div>
    </div>
  );
}

function EmptyState({ label }: { label: string }) {
  return (
    <div className="text-center py-16 bg-white border-4 border-dashed border-[#2C1E16]">
      <Package size={48} className="mx-auto mb-4 text-[#D9A036] opacity-50" />
      <h3 className="text-xl font-black text-[#2C1E16] mb-2">查無相關狀態的訂單</h3>
      <p className="font-bold text-[#2A5C5B]">目前「{label}」分類下沒有任何紀錄喔！</p>
    </div>
  );
}

type ShippingFilterOption = {
  key: ShippingStatusCode | "ALL";
  label: string;
};

function FilterMenu({
  label,
  value,
  options,
  onChange
}: {
  label: string;
  value: ShippingStatusCode | "ALL";
  options: ShippingFilterOption[];
  onChange: (value: ShippingStatusCode | "ALL") => void;
}) {
  const [isOpen, setIsOpen] = useState(false);
  const selectedLabel = options.find((option) => option.key === value)?.label ?? "全部";

  return (
    <div
      className="relative mb-8 max-w-xs"
      onBlur={(event) => {
        const nextTarget = event.relatedTarget as Node | null;
        if (!nextTarget || !event.currentTarget.contains(nextTarget)) {
          setIsOpen(false);
        }
      }}
    >
      <label className="mb-2 block px-1 text-base font-black text-[#2C1E16]">
        {label}
      </label>
      <button
        type="button"
        onClick={() => setIsOpen((prev) => !prev)}
        className="flex w-full items-center justify-between gap-3 border-2 border-[#2C1E16] bg-white px-4 py-2 font-black text-[#2C1E16] shadow-[4px_4px_0px_#2C1E16] transition-all hover:bg-[#F5F0E6] focus:outline-none"
      >
        <span>{selectedLabel}</span>
        <ChevronDown
          size={18}
          className={`transition-transform ${isOpen ? "rotate-180" : ""}`}
        />
      </button>

      {isOpen && (
        <div className="absolute left-0 right-0 z-30 mt-2 flex flex-col gap-2 border-2 border-[#2C1E16] bg-[#EBE3CC] p-2 shadow-[4px_4px_0px_#2C1E16]">
          {options.map((option) => (
            <button
              key={option.key}
              type="button"
              onClick={() => {
                onChange(option.key);
                setIsOpen(false);
              }}
              className={`px-4 py-2 text-left font-black border-2 border-[#2C1E16] transition-all ${
                value === option.key
                  ? "bg-[#BC4A3C] text-[#EBE3CC] shadow-[inset_3px_3px_0px_rgba(0,0,0,0.3)] translate-y-[1px] translate-x-[1px]"
                  : "bg-white text-[#2C1E16] shadow-[3px_3px_0px_#2C1E16] hover:bg-[#F5F0E6] hover:translate-y-[1px] hover:translate-x-[1px] hover:shadow-[2px_2px_0px_#2C1E16]"
              }`}
            >
              {option.label}
            </button>
          ))}
        </div>
      )}
    </div>
  );
}

function hasQuickOrderItems(order: OrderView): boolean {
  return order.items.some(isQuickOrderItem);
}

function isQuickOrderItem(item: OrderView["items"][number]): boolean {
  return item.shippingStatusCode === "READY_TO_SHIP";
}

function sortOrdersByGroupDateDesc(orders: OrderView[]): OrderView[] {
  return orders
    .map((order, index) => ({
      order,
      index,
      dateKey: getGroupDateSortKey(order.groupName)
    }))
    .sort((a, b) => b.dateKey - a.dateKey || a.index - b.index)
    .map(({ order }) => order);
}

function getGroupDateSortKey(groupName: string): number {
  const normalized = groupName.trim();
  const candidates: Array<{ index: number; dateKey: number }> = [];

  for (const match of normalized.matchAll(/(\d{1,2})[./-](\d{1,2})/g)) {
    const dateKey = toMonthDaySortKey(Number(match[1]), Number(match[2]));
    if (dateKey > 0) {
      candidates.push({ index: match.index ?? 0, dateKey });
    }
  }

  for (const match of normalized.matchAll(/\d{3,4}/g)) {
    const raw = match[0];
    const month = Number(raw.slice(0, raw.length - 2));
    const day = Number(raw.slice(-2));
    const dateKey = toMonthDaySortKey(month, day);
    if (dateKey > 0) {
      candidates.push({ index: match.index ?? 0, dateKey });
    }
  }

  if (!candidates.length) {
    return -1;
  }

  candidates.sort((a, b) => a.index - b.index);
  return candidates[0].dateKey;
}

function toMonthDaySortKey(month: number, day: number): number {
  if (month < 1 || month > 12 || day < 1 || day > 31) {
    return -1;
  }
  return month * 100 + day;
}
