package com.fy20047.susan.dto;

// API 錯誤資訊
public class ApiError {

    // 錯誤代碼，通常前端會拿這個來判斷要顯示什麼畫面 (例如："404", "ERR_NO_NICKNAME")
    private String code;
    // 給我們看的錯誤訊息 (例如："找不到該買家的訂單")
    private String message;

    // 預設的空建構子 (Spring Boot 底層的 JSON 轉換器在運作時，必須要有這個才能把 JSON 轉回 Java 物件)
    public ApiError() {
    }

    // 帶參數的建構子，方便在程式裡一行就把它 new 出來並塞好資料
    public ApiError(String code, String message) {
        this.code = code;
        this.message = message;
    }

    // Getter 和 Setter
    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
