import { ChangeEvent, MouseEvent, useState } from "react";
import { ChevronDown, ChevronUp, Gift, Package } from "lucide-react";
import { OrderView } from "../types";
import { getItemStatusClass, getSummaryStatusClass } from "../status";

type OrderCardProps = {
  order: OrderView;
  showStatus: boolean;
  quickOrderSelectable?: boolean;
  quickOrderSelected?: boolean;
  onQuickOrderSelectChange?: (orderId: number, checked: boolean) => void;
};

export default function OrderCard({
  order,
  showStatus,
  quickOrderSelectable = false,
  quickOrderSelected = false,
  onQuickOrderSelectChange
}: OrderCardProps) {
  const [isExpanded, setIsExpanded] = useState(false);

  const actualTotalItems = order.purchasedQuantity;
  const actualTotalAmount = order.totalAmount;
  const paidDeposit = order.paidDepositAmount;
  const pendingDeposit = order.pendingDepositAmount;
  const balance = order.balanceAmount;
  const isBalancePaid = order.isBalancePaid;

  const handleQuickOrderClick = (event: MouseEvent<HTMLLabelElement>) => {
    event.stopPropagation();
  };

  const handleQuickOrderChange = (event: ChangeEvent<HTMLInputElement>) => {
    onQuickOrderSelectChange?.(order.id, event.target.checked);
  };

  return (
    <div className="bg-white border-4 border-[#2C1E16] shadow-[6px_6px_0px_#2C1E16] mb-6 transition-all duration-300">
      <div
        className="p-4 cursor-pointer hover:bg-[#F5F0E6] transition-colors flex flex-col md:flex-row gap-4 justify-between items-start"
        onClick={() => setIsExpanded((prev) => !prev)}
      >
        <div className="flex-1 w-full">
          <div className="flex items-center gap-2 mb-2">
            <Package size={20} className="text-[#BC4A3C] flex-shrink-0" />
            <h3 className="font-black text-lg md:text-xl text-[#2C1E16] leading-snug">
              {order.groupName}
            </h3>
          </div>

          <div className="flex flex-wrap items-center gap-3 text-sm md:text-base font-bold text-[#2A5C5B]">
            <span>訂單總數：{actualTotalItems} 件</span>
            <span className="text-[#2C1E16]">|</span>
            <span className="text-[#BC4A3C] font-black">
              總金額：NT$ {actualTotalAmount.toLocaleString()}
            </span>
          </div>
        </div>

        <div className="flex items-center justify-between w-full md:w-auto gap-4 border-t-2 md:border-t-0 border-dashed border-[#2C1E16] pt-3 md:pt-0 mt-2 md:mt-0">
          {quickOrderSelectable && (
            <label
              className="flex items-center gap-2 px-3 py-1 bg-[#EBE3CC] border-2 border-[#2C1E16] text-sm font-bold text-[#2C1E16] shadow-[2px_2px_0px_#2C1E16]"
              onClick={handleQuickOrderClick}
            >
              <input
                type="checkbox"
                checked={quickOrderSelected}
                onChange={handleQuickOrderChange}
                className="h-4 w-4 accent-[#BC4A3C]"
                aria-label={`選取 ${order.groupName} 進行快速下單`}
              />
              <span>快速下單</span>
            </label>
          )}
          {showStatus && (
            <span
              className={`px-3 py-1 font-bold border-2 border-[#2C1E16] text-sm md:text-base shadow-[2px_2px_0px_#2C1E16] ${getSummaryStatusClass(
                order.summaryStatusCode
              )}`}
            >
              {order.summaryStatus}
            </span>
          )}
          <button className="p-1 bg-[#EBE3CC] border-2 border-[#2C1E16] hover:bg-[#D9A036] transition-colors flex-shrink-0">
            {isExpanded ? <ChevronUp size={20} /> : <ChevronDown size={20} />}
          </button>
        </div>
      </div>

      {isExpanded && (
        <div className="border-t-4 border-double border-[#2C1E16] bg-[#FAFAFA] p-4 md:p-6 animate-slide-in">
          <h4 className="font-black text-lg mb-4 flex items-center gap-2">
            <span className="w-2 h-2 bg-[#BC4A3C]"></span> 購買訂單明細
          </h4>

          <div className="space-y-3 mb-6">
            {order.items.map((item) => (
              <div
                key={item.id}
                className="flex flex-col md:flex-row md:items-center justify-between border-b-2 border-dashed border-[#2C1E16] pb-4 md:pb-3 text-sm md:text-base gap-2 md:gap-0"
              >
                <div className="flex flex-col md:flex-row md:items-center gap-2 md:gap-4 flex-1">
                  <div className="flex shrink-0">
                    <span className={getItemStatusClass(item.status)}>{item.status}</span>
                  </div>

                  <div className="flex justify-between items-start md:block flex-1">
                    <div className="font-bold text-[#2C1E16] text-base md:text-base flex flex-wrap items-center gap-2">
                      <span>{item.name}</span>
                      <span className="ml-2 text-[#2A5C5B]">×{item.quantity}</span>
                      {item.isBalancePaid && (
                        <span className="whitespace-nowrap text-xs font-black text-[#BC4A3C] md:text-sm">
                          （尾款已付）
                        </span>
                      )}
                    </div>

                    <div className="font-black text-[#2A5C5B] text-base md:hidden ml-2 whitespace-nowrap">
                      NT$ {item.totalAmount.toLocaleString()}
                    </div>
                  </div>

                  {item.orderSn && (
                    <div className="text-xs text-[#2C1E16] bg-[#F5F0E6] md:bg-transparent p-2 md:p-0 border-l-4 border-[#BC4A3C] md:border-none rounded-r">
                      <span className="md:text-[#BC4A3C] md:font-bold">
                        {item.checkedIn && "⚠️您尚未報到，請盡速登記資料⚠️ | "}
                        順位：{item.orderSn}
                        {typeof item.queued === "boolean" && (
                          <span className="ml-1">({item.queued ? "已排到" : "未排到"})</span>
                        )}
                      </span>
                    </div>
                  )}
                </div>

                <div className="hidden md:block font-black text-right text-[#2A5C5B] text-base md:ml-4 whitespace-nowrap">
                  NT$ {item.totalAmount.toLocaleString()}
                </div>
              </div>
            ))}
          </div>

          <div className="bg-[#EBE3CC] border-2 border-[#2C1E16] p-4 flex flex-col md:flex-row md:items-center items-start gap-6">
            {order.bonusCount > 0 && (
              <div className="flex items-center gap-2 font-black text-[#BC4A3C] md:w-1/4 text-left">
                <Gift size={20} />
                <span>訂單特典：{order.bonusCount} 張</span>
              </div>
            )}

            <div className={`w-full ${order.bonusCount > 0 ? "md:w-3/4 md:ml-auto" : ""}`}>
              <div className="w-full bg-white p-4 border-2 border-[#2C1E16] shadow-[4px_4px_0px_#2C1E16] min-w-[234px]">
                <div className="flex flex-col font-bold text-[#2C1E16]">
                  <div className="flex justify-between gap-6 mb-1 text-[#2A5C5B] text-sm md:text-base">
                    <span>訂單總額</span>
                    <span>NT$ {actualTotalAmount.toLocaleString()}</span>
                  </div>
                  <div className="flex justify-between gap-6 mb-1 text-sm md:text-base">
                    <span>已付定金</span>
                    <span>NT$ {paidDeposit.toLocaleString()}</span>
                  </div>
                  <div className="flex justify-between gap-6 mb-2 text-sm md:text-base">
                    <span>待付定金</span>
                    <span>NT$ {pendingDeposit.toLocaleString()}</span>
                  </div>

                  <div className="border-t-2 border-dashed border-[#2C1E16] my-2"></div>

                  <div className="flex justify-between items-center gap-4 bg-[#BC4A3C] text-[#EBE3CC] p-2 mt-1 border-2 border-[#2C1E16] shadow-[2px_2px_0px_#2C1E16] transform -rotate-1 hover:rotate-0 transition-transform">
                    <span className="font-black text-base md:text-lg tracking-wider">
                      {isBalancePaid ? "已結清尾款" : "取付尾款"}
                    </span>
                    <span className="flex flex-wrap items-center justify-end gap-2 font-black text-lg md:text-xl">
                      <span>NT$ {balance.toLocaleString()}</span>
                    </span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
