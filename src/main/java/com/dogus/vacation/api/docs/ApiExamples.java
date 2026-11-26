package com.dogus.vacation.api.docs;

import org.springframework.http.MediaType;

/**
 * Swagger UI'da görünen örnek istek ve cevaplar (JSON).
 *
 * <p>Örnekler gerçek cevapların biçimindedir: alan adları Response record'larıyla, hata
 * mesajları kodun döndürdüğü metinlerle aynıdır. {@code OpenApiDocumentationTest} her örneğin
 * alanlarını ilgili record'un alanlarıyla karşılaştırır; bir record'a alan eklenir veya
 * çıkarılırsa test burayı güncellemeyi hatırlatır.
 *
 * <p>Değerler anotasyonlarda kullanıldığı için derleme zamanı sabitidir.
 */
public final class ApiExamples {

    /** Örneklerin içerik türü. */
    public static final String JSON = MediaType.APPLICATION_JSON_VALUE;

    private ApiExamples() {
    }

    // ---- ortak hatalar ----

    public static final String UNAUTHORIZED = """
            {"timestamp": "2026-11-26T10:15:30+03:00", "status": 401, "code": "AUTHENTICATION_REQUIRED",
             "message": "Bu işlem için giriş yapılmalı; token eksik, geçersiz veya süresi dolmuş.",
             "path": "/api/leave-balances/mine", "fieldErrors": []}
            """;

    public static final String FORBIDDEN = """
            {"timestamp": "2026-11-26T10:15:30+03:00", "status": 403, "code": "ACTION_NOT_ALLOWED",
             "message": "Bu işlem için yetkiniz yok.",
             "path": "/api/leave-types", "fieldErrors": []}
            """;

    // ---- izin türleri ----

    public static final String LEAVE_TYPE_LIST = """
            {"leaveTypes": [
              {"id": 1, "code": "ANNUAL", "name": "Yıllık izin", "deductsFromBalance": true,
               "annualLimitDays": null, "active": true},
              {"id": 2, "code": "EXCUSE", "name": "Mazeret izni", "deductsFromBalance": true,
               "annualLimitDays": 5.0, "active": true},
              {"id": 3, "code": "SICK", "name": "Hastalık izni", "deductsFromBalance": false,
               "annualLimitDays": null, "active": true},
              {"id": 4, "code": "UNPAID", "name": "Ücretsiz izin", "deductsFromBalance": false,
               "annualLimitDays": null, "active": true}
            ]}
            """;

    public static final String LEAVE_TYPE_CREATE_REQUEST = """
            {"code": "MARRIAGE_LEAVE", "name": "Evlilik izni", "deductsFromBalance": true, "annualLimitDays": 3}
            """;

    public static final String LEAVE_TYPE_CREATED = """
            {"leaveType": {"id": 5, "code": "MARRIAGE_LEAVE", "name": "Evlilik izni",
             "deductsFromBalance": true, "annualLimitDays": 3, "active": true}}
            """;

    public static final String LEAVE_TYPE_INVALID_CODE = """
            {"timestamp": "2026-11-26T10:15:30+03:00", "status": 400, "code": "VALIDATION_FAILED",
             "message": "İstek geçersiz.", "path": "/api/leave-types",
             "fieldErrors": [{"field": "code",
               "message": "Kod büyük harfle başlamalı; yalnızca büyük harf ve alt çizgi içerebilir (2–30 karakter)."}]}
            """;

    public static final String LEAVE_TYPE_DUPLICATE_CODE = """
            {"timestamp": "2026-11-26T10:15:30+03:00", "status": 409, "code": "DUPLICATE_VALUE",
             "message": "'MARRIAGE_LEAVE' kodlu bir izin türü zaten var.",
             "path": "/api/leave-types", "fieldErrors": []}
            """;

    public static final String LEAVE_TYPE_UPDATE_REQUEST = """
            {"name": "Mazeret izni", "annualLimitDays": 3, "active": true}
            """;

    public static final String LEAVE_TYPE_UPDATED = """
            {"leaveType": {"id": 2, "code": "EXCUSE", "name": "Mazeret izni",
             "deductsFromBalance": true, "annualLimitDays": 3, "active": true}}
            """;

    public static final String LEAVE_TYPE_LIMIT_WITHOUT_BALANCE = """
            {"timestamp": "2026-11-26T10:15:30+03:00", "status": 400, "code": "VALIDATION_FAILED",
             "message": "İstek geçersiz.", "path": "/api/leave-types/3",
             "fieldErrors": [{"field": "annualLimitDays",
               "message": "Bakiyeden düşmeyen türde yıllık sınır boş olmalı."}]}
            """;

    public static final String LEAVE_TYPE_NOT_FOUND = """
            {"timestamp": "2026-11-26T10:15:30+03:00", "status": 404, "code": "LEAVE_TYPE_NOT_FOUND",
             "message": "İzin türü bulunamadı.", "path": "/api/leave-types/99", "fieldErrors": []}
            """;

    public static final String LEAVE_TYPE_DUPLICATE_NAME = """
            {"timestamp": "2026-11-26T10:15:30+03:00", "status": 409, "code": "DUPLICATE_VALUE",
             "message": "'Yıllık izin' adlı bir izin türü zaten var.",
             "path": "/api/leave-types/2", "fieldErrors": []}
            """;

    // ---- izin geçmişi ----

    public static final String HISTORY_PAGE = """
            {"history": {
              "content": [
                {"id": 87, "occurredAt": "2026-11-24T14:05:12+03:00", "leaveRequestId": 43,
                 "employee": {"id": 12, "fullName": "Ayşe Yılmaz"}, "departmentName": "Yazılım Geliştirme",
                 "leaveType": {"id": 1, "code": "ANNUAL", "name": "Yıllık izin"},
                 "startDate": "2026-12-21", "endDate": "2026-12-25",
                 "fromStatus": "PENDING", "toStatus": "APPROVED",
                 "actor": {"id": 5, "fullName": "Mehmet Kaya"}, "note": null},
                {"id": 86, "occurredAt": "2026-11-20T09:41:03+03:00", "leaveRequestId": 43,
                 "employee": {"id": 12, "fullName": "Ayşe Yılmaz"}, "departmentName": "Yazılım Geliştirme",
                 "leaveType": {"id": 1, "code": "ANNUAL", "name": "Yıllık izin"},
                 "startDate": "2026-12-21", "endDate": "2026-12-25",
                 "fromStatus": null, "toStatus": "PENDING",
                 "actor": {"id": 12, "fullName": "Ayşe Yılmaz"}, "note": "Yılbaşı tatili"}
              ],
              "page": 0, "size": 20, "totalElements": 2, "totalPages": 1}}
            """;

    public static final String HISTORY_REVERSED_RANGE = """
            {"timestamp": "2026-11-26T10:15:30+03:00", "status": 400, "code": "VALIDATION_FAILED",
             "message": "İstek geçersiz.", "path": "/api/leave-requests/history",
             "fieldErrors": [{"field": "to", "message": "Bitiş tarihi başlangıç tarihinden önce olamaz."}]}
            """;

    // ---- bakiyeler ----

    public static final String MY_BALANCES = """
            {"year": 2026, "balances": [
              {"leaveType": {"id": 1, "code": "ANNUAL", "name": "Yıllık izin"}, "year": 2026,
               "entitledDays": 20.0, "usedDays": 5.0, "remainingDays": 15.0},
              {"leaveType": {"id": 2, "code": "EXCUSE", "name": "Mazeret izni"}, "year": 2026,
               "entitledDays": 5.0, "usedDays": 0.0, "remainingDays": 5.0}
            ]}
            """;

    public static final String BALANCE_YEAR_OUT_OF_RANGE = """
            {"timestamp": "2026-11-26T10:15:30+03:00", "status": 400, "code": "VALIDATION_FAILED",
             "message": "İstek geçersiz.", "path": "/api/leave-balances/mine",
             "fieldErrors": [{"field": "year", "message": "Yıl 2000'den küçük olamaz."}]}
            """;

    public static final String EMPLOYEE_BALANCES = """
            {"employee": {"id": 12, "fullName": "Ayşe Yılmaz"}, "year": 2026, "balances": [
              {"leaveType": {"id": 1, "code": "ANNUAL", "name": "Yıllık izin"}, "year": 2026,
               "entitledDays": 20.0, "usedDays": 5.0, "remainingDays": 15.0},
              {"leaveType": {"id": 2, "code": "EXCUSE", "name": "Mazeret izni"}, "year": 2026,
               "entitledDays": 5.0, "usedDays": 0.0, "remainingDays": 5.0}
            ]}
            """;

    public static final String EMPLOYEE_BALANCES_OTHER_TEAM = """
            {"timestamp": "2026-11-26T10:15:30+03:00", "status": 403, "code": "ACTION_NOT_ALLOWED",
             "message": "Bu işlem için yetkiniz yok.",
             "path": "/api/employees/31/leave-balances", "fieldErrors": []}
            """;

    public static final String EMPLOYEE_NOT_FOUND = """
            {"timestamp": "2026-11-26T10:15:30+03:00", "status": 404, "code": "EMPLOYEE_NOT_FOUND",
             "message": "Çalışan bulunamadı.", "path": "/api/employees/999/leave-balances", "fieldErrors": []}
            """;

    // ---- yıllık hak ediş (T3) ----

    public static final String ACCRUAL_RUN = """
            {"year": 2027, "accruedEmployees": 148, "skippedEmployees": 2, "openedBalances": 296,
             "runAt": "2026-12-15T10:30:00+03:00"}
            """;

    public static final String ACCRUAL_INVALID_YEAR = """
            {"timestamp": "2026-11-26T10:15:30+03:00", "status": 400, "code": "VALIDATION_FAILED",
             "message": "İstek geçersiz.", "path": "/api/leave-balances/accruals",
             "fieldErrors": [{"field": "year",
               "message": "Hak ediş yalnızca bu yıl (2026) veya gelecek yıl (2027) için çalıştırılabilir."}]}
            """;

    public static final String ACCRUAL_ALREADY_RUN = """
            {"timestamp": "2026-12-15T10:31:02+03:00", "status": 409, "code": "ACCRUAL_ALREADY_RUN",
             "message": "2027 yılının hak edişi zaten çalıştırıldı.",
             "path": "/api/leave-balances/accruals", "fieldErrors": []}
            """;
}
