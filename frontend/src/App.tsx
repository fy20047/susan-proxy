import { useMemo, useState } from "react";
import { AlertCircle, ArrowLeft, Package, Search, ShoppingBag } from "lucide-react";
import { fetchOrders } from "./api/orders";
import OrderCard from "./components/OrderCard";
import { STATUS_FILTERS, toOrderStatusLabel } from "./status";
import { buildOrderView } from "./transform";
import { ItemStatusCode, OrderView } from "./types";

export default function App() {
  const [currentPage, setCurrentPage] = useState<"search" | "results">("search");
  const [searchName, setSearchName] = useState("");
  const [currentSearchName, setCurrentSearchName] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [orders, setOrders] = useState<OrderView[]>([]);
  const [activeTab, setActiveTab] = useState<ItemStatusCode | "ALL">("ALL");
  const [error, setError] = useState<string | null>(null);

  const filteredOrders = useMemo(() => {
    if (activeTab === "ALL") return orders;

    const label = toOrderStatusLabel(activeTab);

    return orders
      .map((order) => {
        const items = order.items.filter((item) => item.statusCode === activeTab);
        if (items.length === 0) return null;

        const totalAmount = items.reduce((sum, item) => sum + item.totalAmount, 0);
        const depositAmount = items.reduce((sum, item) => sum + item.depositAmount, 0);
        const balanceAmount = items.reduce((sum, item) => sum + item.balanceAmount, 0);

        return {
          ...order,
          statusCode: activeTab,
          status: label,
          items,
          totalAmount,
          depositAmount,
          balanceAmount
        };
      })
      .filter((order): order is OrderView => order !== null);
  }, [orders, activeTab]);

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
      className="min-h-screen font-serif bg-[#EBE3CC] text-[#2C1E16]"
      style={{
        backgroundImage: "radial-gradient(#D5CBB3 1px, transparent 1px)",
        backgroundSize: "20px 20px"
      }}
    >
      {currentPage === "search" && (
        <div className="flex flex-col items-center justify-center min-h-screen p-6">
          <div className="w-full max-w-lg">
            <header className="mb-10 text-center">
              <div className="inline-block border-4 border-[#2C1E16] bg-white p-6 shadow-[8px_8px_0px_#2C1E16] relative">
                <div className="absolute top-0 left-0 w-full h-2 bg-[#BC4A3C]"></div>
                <h1 className="text-3xl md:text-4xl font-black tracking-widest mb-3 flex flex-col items-center gap-2">
                  <ShoppingBag size={40} className="text-[#BC4A3C]" />
                  <span>俗三連線中</span>
                </h1>
                <p className="text-lg md:text-xl font-bold tracking-[0.2em] text-[#2A5C5B] border-t-2 border-dashed border-[#2C1E16] pt-2">
                  🇯🇵 日本動漫代購 🇯🇵
                </p>
              </div>
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

              {error && (
                <div className="mt-6 flex items-start gap-2 text-[#BC4A3C] bg-[#EBE3CC] p-3 border-2 border-[#2C1E16]">
                  <AlertCircle size={20} className="flex-shrink-0 mt-0.5" />
                  <p className="text-sm font-bold leading-relaxed">{error}</p>
                </div>
              )}

              {!error && (
                <div className="mt-6 flex items-start gap-2 text-[#BC4A3C] bg-[#EBE3CC] p-3 border-2 border-[#2C1E16]">
                  <AlertCircle size={20} className="flex-shrink-0 mt-0.5" />
                  <p className="text-sm font-bold leading-relaxed">
                    注意：請輸入您在群組喊單的完整暱稱。如有變更暱稱，請務必聯繫官方LINE進行登記，以免查詢不到您的訂單。
                  </p>
                </div>
              )}
            </form>

            <footer className="mt-8 text-center text-sm font-bold text-[#2A5C5B]">
              © 2026 俗三連線中. All Rights Reserved.
            </footer>
          </div>
        </div>
      )}

      {currentPage === "results" && (
        <div className="max-w-4xl mx-auto p-4 md:p-8 pt-8">
          <div className="flex flex-col md:flex-row justify-between items-start md:items-end mb-8 border-b-4 border-[#2C1E16] pb-4 gap-4">
            <button
              onClick={() => setCurrentPage("search")}
              className="flex items-center gap-2 font-bold px-4 py-2 bg-white border-2 border-[#2C1E16] shadow-[2px_2px_0px_#2C1E16] hover:bg-[#F5F0E6] transition-colors"
            >
              <ArrowLeft size={18} /> 返回查詢
            </button>
            <div className="text-right">
              <h2 className="text-2xl md:text-3xl font-black flex items-center gap-2 mt-2">
                『{" "}
                <span className="text-[#BC4A3C] underline decoration-[#D9A036] decoration-4 underline-offset-4">
                  {currentSearchName}
                </span>{" "}
                』的訂單紀錄
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
              filteredOrders.map((order) => <OrderCard key={order.id} order={order} />)
            ) : (
              <div className="text-center py-16 bg-white border-4 border-dashed border-[#2C1E16]">
                <Package size={48} className="mx-auto mb-4 text-[#D9A036] opacity-50" />
                <h3 className="text-xl font-black text-[#2C1E16] mb-2">
                  查無相關狀態的訂單
                </h3>
                <p className="font-bold text-[#2A5C5B]">
                  目前「
                  {STATUS_FILTERS.find((status) => status.key === activeTab)?.label ??
                    "全部"}
                  」分類下沒有任何紀錄喔！
                </p>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
