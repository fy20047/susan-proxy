import { useState } from "react";
import { ChevronDown, ChevronUp, Gift, Package } from "lucide-react";
import { OrderView } from "../types";
import { getItemStatusClass, getOrderStatusClass } from "../status";

type OrderCardProps = {
  order: OrderView;
  showStatus: boolean;
};

export default function OrderCard({ order, showStatus }: OrderCardProps) {
  const [isExpanded, setIsExpanded] = useState(false);

  const actualTotalItems = order.items.reduce((sum, item) => sum + item.quantity, 0);
  const actualTotalAmount = order.totalAmount;
  const deposit = order.depositAmount;
  const balance = order.balanceAmount;

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
          {showStatus && (
            <span
              className={`px-3 py-1 font-bold border-2 border-[#2C1E16] text-sm md:text-base shadow-[2px_2px_0px_#2C1E16] ${getOrderStatusClass(
                order.statusCode
              )}`}
            >
              {order.status}
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
                className="flex justify-between items-start md:items-center border-b-2 border-dashed border-[#2C1E16] pb-3 text-sm md:text-base"
              >
                <div className="flex items-start md:items-center gap-3 flex-1 pr-4">
                  <div className="mt-0.5 md:mt-0">
                    <span className={getItemStatusClass(item.status)}>{item.status}</span>
                  </div>
                  <div className="font-bold leading-tight text-[#2C1E16]">
                    {item.name}
                    <span className="ml-2">×{item.quantity}</span>
                    {item.orderSn && (
                      <span className="ml-2 text-xs text-[#2C1E16]">
                        【順位：{item.orderSn} ({item.queued ? "目前已排到" : "目前尚未排到"})】
                      </span>
                    )}
                  </div>
                </div>
                <div className="font-bold mt-1 md:mt-0 text-right whitespace-nowrap text-[#2A5C5B]">
                  NT$ {item.totalAmount.toLocaleString()}
                </div>
              </div>
            ))}
          </div>

          <div className="bg-[#EBE3CC] border-2 border-[#2C1E16] p-4 flex flex-col md:flex-row justify-between items-start md:items-center gap-6">
            {order.bonusCount > 0 && (
              <div className="flex items-center gap-2 font-black text-[#BC4A3C]">
                <Gift size={20} />
                <span>訂單特典：{order.bonusCount} 張</span>
              </div>
            )}

            <div className="w-full md:w-auto bg-white p-4 border-2 border-[#2C1E16] shadow-[4px_4px_0px_#2C1E16] min-w-[260px]">
              <div className="flex flex-col font-bold text-[#2C1E16]">
                <div className="flex justify-between gap-6 mb-1 text-[#2A5C5B] text-sm md:text-base">
                  <span>訂單總額</span>
                  <span>NT$ {actualTotalAmount.toLocaleString()}</span>
                </div>
                <div className="flex justify-between gap-6 mb-2 text-sm md:text-base">
                  <span>定金</span>
                  <span>NT$ {deposit.toLocaleString()}</span>
                </div>

                <div className="border-t-2 border-dashed border-[#2C1E16] my-2"></div>

                <div className="flex justify-between items-center gap-4 bg-[#BC4A3C] text-[#EBE3CC] p-2 mt-1 border-2 border-[#2C1E16] shadow-[2px_2px_0px_#2C1E16] transform -rotate-1 hover:rotate-0 transition-transform">
                  <span className="font-black text-base md:text-lg tracking-wider">取付尾款</span>
                  <span className="font-black text-lg md:text-xl">NT$ {balance.toLocaleString()}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
