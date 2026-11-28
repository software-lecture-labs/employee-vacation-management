package com.dogus.vacation.api.docs;

/**
 * İptal uç noktalarının Swagger örnekleri (FR-23, FR-25, FR-28, FR-29).
 *
 * <p>{@link ApiExamples} ile aynı kurallar: alan adları Response record'larıyla, hata
 * mesajları kodun döndürdüğü metinlerle aynıdır; {@code OpenApiDocumentationTest} örneklerin
 * alanlarını record'larla karşılaştırır. Ortak 401 ve 403 örnekleri {@link ApiExamples}'tadır.
 */
public final class CancellationExamples {

    private CancellationExamples() {
    }

    public static final String CANCEL_REQUEST = """
            {
              "reason": "Proje teslim haftasına denk geliyor"
            }
            """;

    public static final String CANCELLED_BY_MANAGER = """
            {
              "leaveRequest": {
                "id": 42,
                "employee": {
                  "id": 5,
                  "fullName": "Ayşe Demir"
                },
                "leaveType": {
                  "id": 1,
                  "code": "ANNUAL",
                  "name": "Yıllık izin"
                },
                "startDate": "2026-12-09",
                "endDate": "2026-12-11",
                "workingDays": 3.0,
                "reason": "Aile ziyareti",
                "status": "CANCELLED",
                "approver": {
                  "id": 3,
                  "fullName": "Ahmet Yılmaz"
                },
                "decisionNote": null,
                "decidedAt": "2026-11-24T09:12:00+03:00",
                "cancelRequestReason": null,
                "cancelRequestedAt": null,
                "cancelledBy": {
                  "id": 3,
                  "fullName": "Ahmet Yılmaz"
                },
                "cancelReason": "Proje teslim haftasına denk geliyor",
                "cancelledAt": "2026-11-28T14:05:00+03:00"
              }
            }
            """;

    public static final String CANCEL_REASON_BLANK = """
            {
              "timestamp": "2026-11-26T10:15:30+03:00",
              "status": 400,
              "code": "VALIDATION_FAILED",
              "message": "İstek geçersiz.",
              "path": "/api/leave-requests/42/cancel",
              "fieldErrors": [
                {
                  "field": "reason",
                  "message": "İptal gerekçesi zorunlu."
                }
              ]
            }
            """;

    public static final String CANCEL_STARTED = """
            {
              "timestamp": "2026-11-26T10:15:30+03:00",
              "status": 409,
              "code": "INVALID_STATUS_TRANSITION",
              "message": "Başlamış bir izin iptal edilemez.",
              "path": "/api/leave-requests/42/cancel",
              "fieldErrors": []
            }
            """;

    public static final String CANCEL_NOT_APPROVER = """
            {
              "timestamp": "2026-11-26T10:15:30+03:00",
              "status": 403,
              "code": "ACTION_NOT_ALLOWED",
              "message": "Bu işlem için yetkiniz yok.",
              "path": "/api/leave-requests/42/cancel",
              "fieldErrors": []
            }
            """;

    public static final String LEAVE_REQUEST_NOT_FOUND = """
            {
              "timestamp": "2026-11-26T10:15:30+03:00",
              "status": 404,
              "code": "LEAVE_REQUEST_NOT_FOUND",
              "message": "İzin talebi bulunamadı.",
              "path": "/api/leave-requests/99/cancel",
              "fieldErrors": []
            }
            """;

    public static final String CANCELLATION_REQUEST = """
            {
              "reason": "Planlarım değişti"
            }
            """;

    public static final String CANCELLATION_REQUESTED = """
            {
              "leaveRequest": {
                "id": 42,
                "employee": {
                  "id": 5,
                  "fullName": "Ayşe Demir"
                },
                "leaveType": {
                  "id": 1,
                  "code": "ANNUAL",
                  "name": "Yıllık izin"
                },
                "startDate": "2026-12-09",
                "endDate": "2026-12-11",
                "workingDays": 3.0,
                "reason": "Aile ziyareti",
                "status": "CANCEL_REQUESTED",
                "approver": {
                  "id": 3,
                  "fullName": "Ahmet Yılmaz"
                },
                "decisionNote": null,
                "decidedAt": "2026-11-24T09:12:00+03:00",
                "cancelRequestReason": "Planlarım değişti",
                "cancelRequestedAt": "2026-11-28T10:30:00+03:00",
                "cancelledBy": null,
                "cancelReason": null,
                "cancelledAt": null
              }
            }
            """;

    public static final String CANCELLATION_NOT_OWNER = """
            {
              "timestamp": "2026-11-26T10:15:30+03:00",
              "status": 403,
              "code": "ACTION_NOT_ALLOWED",
              "message": "Bu işlem için yetkiniz yok.",
              "path": "/api/leave-requests/42/cancel-request",
              "fieldErrors": []
            }
            """;

    public static final String CANCELLATION_NOT_APPROVED = """
            {
              "timestamp": "2026-11-26T10:15:30+03:00",
              "status": 409,
              "code": "INVALID_STATUS_TRANSITION",
              "message": "Talep 'Bekliyor' durumundayken için iptal talebi açılamaz.",
              "path": "/api/leave-requests/42/cancel-request",
              "fieldErrors": []
            }
            """;

    public static final String CANCELLATION_APPROVED = """
            {
              "leaveRequest": {
                "id": 42,
                "employee": {
                  "id": 5,
                  "fullName": "Ayşe Demir"
                },
                "leaveType": {
                  "id": 1,
                  "code": "ANNUAL",
                  "name": "Yıllık izin"
                },
                "startDate": "2026-12-09",
                "endDate": "2026-12-11",
                "workingDays": 3.0,
                "reason": "Aile ziyareti",
                "status": "CANCELLED",
                "approver": {
                  "id": 3,
                  "fullName": "Ahmet Yılmaz"
                },
                "decisionNote": null,
                "decidedAt": "2026-11-24T09:12:00+03:00",
                "cancelRequestReason": null,
                "cancelRequestedAt": null,
                "cancelledBy": {
                  "id": 3,
                  "fullName": "Ahmet Yılmaz"
                },
                "cancelReason": "Planlarım değişti",
                "cancelledAt": "2026-11-28T14:05:00+03:00"
              }
            }
            """;

    public static final String CANCELLATION_SELF_DECISION = """
            {
              "timestamp": "2026-11-26T10:15:30+03:00",
              "status": 403,
              "code": "SELF_APPROVAL_NOT_ALLOWED",
              "message": "Kendi talebiniz üzerinde karar veremezsiniz.",
              "path": "/api/leave-requests/42/cancel-request/approve",
              "fieldErrors": []
            }
            """;

    public static final String CANCELLATION_NO_REQUEST = """
            {
              "timestamp": "2026-11-26T10:15:30+03:00",
              "status": 409,
              "code": "INVALID_STATUS_TRANSITION",
              "message": "Talep 'Onaylandı' durumundayken iptal talebi kabul edilemez.",
              "path": "/api/leave-requests/42/cancel-request/approve",
              "fieldErrors": []
            }
            """;

    public static final String CANCELLATION_REJECT_REQUEST = """
            {
              "reason": "Ekip o hafta eksik kalıyor"
            }
            """;

    public static final String CANCELLATION_REJECTED = """
            {
              "leaveRequest": {
                "id": 42,
                "employee": {
                  "id": 5,
                  "fullName": "Ayşe Demir"
                },
                "leaveType": {
                  "id": 1,
                  "code": "ANNUAL",
                  "name": "Yıllık izin"
                },
                "startDate": "2026-12-09",
                "endDate": "2026-12-11",
                "workingDays": 3.0,
                "reason": "Aile ziyareti",
                "status": "APPROVED",
                "approver": {
                  "id": 3,
                  "fullName": "Ahmet Yılmaz"
                },
                "decisionNote": null,
                "decidedAt": "2026-11-24T09:12:00+03:00",
                "cancelRequestReason": null,
                "cancelRequestedAt": null,
                "cancelledBy": null,
                "cancelReason": null,
                "cancelledAt": null
              }
            }
            """;

    public static final String CANCELLATION_WITHDRAWN = """
            {
              "leaveRequest": {
                "id": 42,
                "employee": {
                  "id": 5,
                  "fullName": "Ayşe Demir"
                },
                "leaveType": {
                  "id": 1,
                  "code": "ANNUAL",
                  "name": "Yıllık izin"
                },
                "startDate": "2026-12-09",
                "endDate": "2026-12-11",
                "workingDays": 3.0,
                "reason": "Aile ziyareti",
                "status": "APPROVED",
                "approver": {
                  "id": 3,
                  "fullName": "Ahmet Yılmaz"
                },
                "decisionNote": null,
                "decidedAt": "2026-11-24T09:12:00+03:00",
                "cancelRequestReason": null,
                "cancelRequestedAt": null,
                "cancelledBy": null,
                "cancelReason": null,
                "cancelledAt": null
              }
            }
            """;

    public static final String CANCELLATION_WITHDRAW_NONE = """
            {
              "timestamp": "2026-11-26T10:15:30+03:00",
              "status": 409,
              "code": "INVALID_STATUS_TRANSITION",
              "message": "Talep 'Onaylandı' durumundayken iptal talebinden vazgeçilemez.",
              "path": "/api/leave-requests/42/cancel-request/withdraw",
              "fieldErrors": []
            }
            """;
}
