package com.fy20047.susan.dto;

import java.time.LocalDateTime;

// API 標準回應格式
// <T> 代表 Type (型別)，表示他是萬用的
// 所以之後可以宣告 ApiResponse<List<OrderGroupDto>>，也可以宣告 ApiResponse<String>。
// T 就是箱子裡面那個 `data` 的真實型態
public class ApiResponse<T> {

    // 前端第一時間用來判斷的依據 (if(res.success) { 畫畫面 } else { 彈出錯誤訊息 })
    private boolean success;
    // 成功時這裡有東西，失敗時這裡是 null
    private T data;
    // 錯誤詳細資訊，把剛剛寫好的 ApiError 小卡片放進來，成功時為 null
    private ApiError error;
    // 記錄這包資料是幾點幾分產生的，方便除錯與前端快取判斷
    private LocalDateTime timestamp;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public ApiError getError() {
        return error;
    }

    public void setError(ApiError error) {
        this.error = error;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    // 當 API 成功時，只要呼叫這行： ApiResponse.success(我的資料)
    public static <T> ApiResponse<T> success(T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setSuccess(true); // 把 success 設為 true
        response.setData(data); // 把資料塞進去
        response.setTimestamp(LocalDateTime.now()); // 記下現在的時間
        return response;
    }

    // 當 API 失敗時，只要呼叫這行： ApiResponse.error("404", "找不到訂單")
    public static <T> ApiResponse<T> error(String code, String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setSuccess(false);
        response.setError(new ApiError(code, message));
        response.setTimestamp(LocalDateTime.now());
        return response;
    }
}
