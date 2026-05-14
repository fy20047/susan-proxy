import { useEffect, useMemo, useState } from "react";
import {
  AlertCircle,
  ArrowLeft,
  Copy,
  ExternalLink,
  Package,
  Search
} from "lucide-react";
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

export default function App() {
  const [currentPage, setCurrentPage] = useState<"search" | "results">("search");
  const [searchName, setSearchName] = useState("");
  const [currentSearchName, setCurrentSearchName] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [orders, setOrders] = useState<OrderView[]>([]);
  const [standardFilter, setStandardFilter] = useState<StandardItemStatusCode | "ALL">("ALL");
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
    if (standardFilter === "ALL") {
      return standardOrders;
    }

    return standardOrders.reduce<OrderView[]>((acc, order) => {
      const items = order.items.filter((item) => item.statusCode === standardFilter);
      if (!items.length) {
        return acc;
      }
      acc.push(rebuildOrderView(order, items));
      return acc;
    }, []);
  }, [standardOrders, standardFilter]);

  const quickOrderEligibleOrders = useMemo(
    () =>
      preorderOrders.reduce<OrderView[]>((acc, order) => {
        const items = order.items.filter((item) => item.statusCode === "PREORDER_ARRIVED");
        if (items.length) {
          acc.push(rebuildOrderView(order, items));
        }
        return acc;
      }, []),
    [preorderOrders]
  );

  const selectedQuickOrders = useMemo(() => {
    const selectedIds = new Set(selectedQuickOrderIds);
    return quickOrderEligibleOrders.filter((order) => selectedIds.has(order.id));
  }, [quickOrderEligibleOrders, selectedQuickOrderIds]);

  const selectedQuickOrderBalance = useMemo(
    () => selectedQuickOrders.reduce((sum, order) => sum + order.balanceAmount, 0),
    [selectedQuickOrders]
  );
  const showQuickOrderPanel = selectedQuickOrders.length > 0;

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
  const showStandardStatus = standardFilter !== "ALL";
  const preorderEmptyLabel =
    preorderShippingFilter !== "ALL"
      ? toShippingStatusLabel(preorderShippingFilter)
      : preorderItemFilter === "ALL"
        ? "全部"
        : PREORDER_ITEM_FILTERS.find((filter) => filter.key === preorderItemFilter)?.label ?? "全部";
  const standardEmptyLabel =
    standardFilter === "ALL" ? "全部" : toStandardOrderStatusLabel(standardFilter);

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
      const views = data.map(buildOrderView);
      setOrders(views);
      setCurrentSearchName(normalized);
      setCurrentPage("results");
      setStandardFilter("ALL");
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

                <div className="mb-8 overflow-x-auto pb-4 hide-scrollbar">
                  <div className="mb-2 px-1 text-base font-black text-[#2C1E16]">
                    出貨狀態
                  </div>
                  <div className="flex gap-3 min-w-max px-1">
                    {PREORDER_SHIPPING_FILTERS.map((status) => (
                      <button
                        key={status.key}
                        onClick={() => setPreorderShippingFilter(status.key)}
                        className={`px-5 py-2 font-black border-2 border-[#2C1E16] transition-all ${
                          preorderShippingFilter === status.key
                            ? "bg-[#BC4A3C] text-[#EBE3CC] shadow-[inset_3px_3px_0px_rgba(0,0,0,0.3)] translate-y-[2px] translate-x-[2px]"
                            : "bg-white text-[#2C1E16] shadow-[4px_4px_0px_#2C1E16] hover:bg-[#F5F0E6] hover:translate-y-[1px] hover:translate-x-[1px] hover:shadow-[3px_3px_0px_#2C1E16]"
                        }`}
                      >
                        {status.label}
                      </button>
                    ))}
                  </div>
                </div>

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

                <div className="mb-8 overflow-x-auto pb-4 hide-scrollbar">
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

                {filteredStandardOrders.length > 0 ? (
                  filteredStandardOrders.map((order) => (
                    <OrderCard key={order.id} order={order} showStatus={showStandardStatus} />
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
        <p className="mt-1 text-xs text-gray-600">系統每 5 分鐘自動更新</p>
        <p className="mt-2 text-xs text-[#2A5C5B]">
          瀏覽量：今日 {pageViews?.daily ?? "--"} / 本周 {pageViews?.weekly ?? "--"} / 本月 {pageViews?.monthly ?? "--"} / 總計 {pageViews?.total ?? "--"}
        </p>
      </footer>
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

function hasQuickOrderItems(order: OrderView): boolean {
  return order.items.some((item) => item.statusCode === "PREORDER_ARRIVED");
}
