import { useEffect, useMemo, useState } from "react";
import { AlertCircle, ArrowLeft, Package, Search } from "lucide-react";
import { fetchOrders } from "./api/orders";
import { recordPageView } from "./api/pageViews";
import OrderCard from "./components/OrderCard";
import { STATUS_FILTERS, toOrderStatusLabel } from "./status";
import { buildOrderView } from "./transform";
import { ItemStatusCode, OrderView, PageViewStats } from "./types";
import logo from "./image/logo1.png";
import icon from "./image/icon.png";

export default function App() {
  const [currentPage, setCurrentPage] = useState<"search" | "results">("search");
  const [searchName, setSearchName] = useState("");
  const [currentSearchName, setCurrentSearchName] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [orders, setOrders] = useState<OrderView[]>([]);
  const [activeTab, setActiveTab] = useState<ItemStatusCode | "ALL">("ALL");
  const [error, setError] = useState<string | null>(null);
  const [pageViews, setPageViews] = useState<PageViewStats | null>(null);

  useEffect(() => {
    recordPageView()
      .then((stats) => setPageViews(stats))
      .catch(() => setPageViews(null));
  }, []);

  const filteredOrders = useMemo(() => {
    if (activeTab === "ALL") return orders;

    const label = toOrderStatusLabel(activeTab);

    return orders.reduce<OrderView[]>((acc, order) => {
      const items = order.items.filter((item) => item.statusCode === activeTab);
      if (items.length === 0) return acc;

      const totalAmount = items.reduce((sum, item) => sum + item.totalAmount, 0);
      const depositAmount = items.reduce((sum, item) => sum + item.depositAmount, 0);
      const balanceAmount = items.reduce((sum, item) => sum + item.balanceAmount, 0);

      acc.push({
        ...order,
        statusCode: activeTab,
        status: label,
        items,
        totalAmount,
        depositAmount,
        balanceAmount
      });
      return acc;
    }, []);
  }, [orders, activeTab]);

  const showStatus = activeTab !== "ALL";
  const emptyLabel = activeTab === "ALL" ? "全部" : toOrderStatusLabel(activeTab);

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
      setActiveTab("ALL");
    } catch (err) {
      const message =
        err instanceof Error ? err.message : "搜尋發生錯誤，請稍後再試。";
      setError(message);
    } finally {
      setIsLoading(false);
    }
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
                <div
                  className="shrink-0 z-10 relative -mb-[21px] order-2"
                >
                  <img
                    src={icon}
                    alt="訂單圖示"
                    className="h-20 md:h-20 w-auto object-contain block"
                  />
                </div>
                <h2
                  className="text-2xl md:text-3xl font-black leading-tight text-right order-1 pr-1"
                >
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

            <div className="mb-8 overflow-x-auto pb-4 hide-scrollbar">
              <div className="flex gap-3 min-w-max px-1">
                {STATUS_FILTERS.map((status) => (
                  <button
                    key={status.key}
                    onClick={() => setActiveTab(status.key)}
                    className={`
                      px-5 py-2 font-black border-2 border-[#2C1E16] transition-all
                      ${
                        activeTab === status.key
                          ? "bg-[#2A5C5B] text-[#EBE3CC] shadow-[inset_3px_3px_0px_rgba(0,0,0,0.3)] translate-y-[2px] translate-x-[2px]"
                          : "bg-white text-[#2C1E16] shadow-[4px_4px_0px_#2C1E16] hover:bg-[#F5F0E6] hover:translate-y-[1px] hover:translate-x-[1px] hover:shadow-[3px_3px_0px_#2C1E16]"
                      }
                    `}
                  >
                    {status.label}
                  </button>
                ))}
              </div>
            </div>

            <div>
              {filteredOrders.length > 0 ? (
                filteredOrders.map((order) => (
                  <OrderCard key={order.id} order={order} showStatus={showStatus} />
                ))
              ) : (
                <div className="text-center py-16 bg-white border-4 border-dashed border-[#2C1E16]">
                  <Package size={48} className="mx-auto mb-4 text-[#D9A036] opacity-50" />
                  <h3 className="text-xl font-black text-[#2C1E16] mb-2">查無相關狀態的訂單</h3>
                  <p className="font-bold text-[#2A5C5B]">
                    目前「{emptyLabel}」分類下沒有任何紀錄喔！
                  </p>
                </div>
              )}
            </div>
          </div>
        </div>
      )}

      <footer className="py-6 text-center text-sm text-gray-700">
        <p>© 2026 Susan 代購系統．版權所有</p>
        <p className="mt-1 text-xs text-gray-600">系統每 5 分鐘自動更新</p>
        <p className="mt-2 text-xs text-[#2A5C5B]">
          瀏覽量：今日 {pageViews?.daily ?? "--"} / 本月 {pageViews?.monthly ?? "--"} / 總計 {pageViews?.total ?? "--"}
        </p>
      </footer>
    </div>
  );
}
