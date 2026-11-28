package com.dogus.vacation.api.docs;

import static com.dogus.vacation.api.docs.ApiExamples.JSON;

import com.dogus.vacation.api.advice.ErrorResponse;
import com.dogus.vacation.application.usecase.cancellation.approve.ApproveLeaveCancellationResponse;
import com.dogus.vacation.application.usecase.cancellation.cancel.CancelLeaveRequest;
import com.dogus.vacation.application.usecase.cancellation.cancel.CancelLeaveResponse;
import com.dogus.vacation.application.usecase.cancellation.reject.RejectLeaveCancellationRequest;
import com.dogus.vacation.application.usecase.cancellation.reject.RejectLeaveCancellationResponse;
import com.dogus.vacation.application.usecase.cancellation.request.RequestLeaveCancellationRequest;
import com.dogus.vacation.application.usecase.cancellation.request.RequestLeaveCancellationResponse;
import com.dogus.vacation.application.usecase.cancellation.withdraw.WithdrawLeaveCancellationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Onaylı iznin iptal uç noktalarının Swagger açıklamaları ({@code LeaveCancellationController} uygular).
 */
@Tag(name = "İzin iptali", description = """
        Onaylı izni şirket adına iptal (yönetici, İK) veya iptal talebi (talep sahibi) ve kararı (onaylayıcı). \
        Günler yalnızca iptal gerçekleşince iade edilir. FR-23, FR-25, FR-28, FR-29, T2""")
public interface LeaveCancellationApi {

    @Operation(summary = "Onaylı izni şirket adına iptal et (yönetici, İK)",
            description = """
                    İzni onaylayabilecek kişi iptal eder: çalışanın yöneticisi; yöneticisi yoksa bir İK; İK'nın \
                    iznini başka bir İK (BR-15). İzin başlamadan yapılabilir (BR-08) ve gerekçe zorunludur (BR-09). \
                    Durum, geçmiş satırı ve gün iadesi tek transaction'dadır (T2).""",
            requestBody = @RequestBody(required = true, content = @Content(mediaType = JSON,
                    examples = @ExampleObject(name = "Gerekçe", value = CancellationExamples.CANCEL_REQUEST))))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "İzin iptal edildi, günler iade edildi",
                    content = @Content(mediaType = JSON,
                            schema = @Schema(implementation = CancelLeaveResponse.class),
                            examples = @ExampleObject(name = "İptal edildi", value = CancellationExamples.CANCELLED_BY_MANAGER))),
            @ApiResponse(responseCode = "400", description = "Gerekçe boş veya çok uzun",
                    content = @Content(mediaType = JSON, schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "Gerekçe yok", value = CancellationExamples.CANCEL_REASON_BLANK))),
            @ApiResponse(responseCode = "401", description = "Token yok, geçersiz veya süresi dolmuş",
                    content = @Content(mediaType = JSON, schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "Token yok", value = ApiExamples.UNAUTHORIZED))),
            @ApiResponse(responseCode = "403", description = "Rol veya bu talep için yetki yok",
                    content = @Content(mediaType = JSON, schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "Onaylayıcı değil", value = CancellationExamples.CANCEL_NOT_APPROVER))),
            @ApiResponse(responseCode = "404", description = "İzin talebi yok",
                    content = @Content(mediaType = JSON, schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "Bulunamadı", value = CancellationExamples.LEAVE_REQUEST_NOT_FOUND))),
            @ApiResponse(responseCode = "409", description = "İzin onaylı değil veya başladı",
                    content = @Content(mediaType = JSON, schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "İzin başladı", value = CancellationExamples.CANCEL_STARTED)))
    })
    CancelLeaveResponse cancel(@Parameter(description = "İzin talebinin id'si", example = "42") Long id, CancelLeaveRequest request);

    @Operation(summary = "Onaylı iznim için iptal talebi aç",
            description = """
                    Yalnızca talep sahibi açar; izin onaylı olmalı ve başlamamış olmalı. Gerekçe zorunlu. Günler \
                    onaylayıcı kabul edince iade edilir; başlangıç günü gelene kadar karar verilmezse talep sonuçsuz kalır.""",
            requestBody = @RequestBody(required = true, content = @Content(mediaType = JSON,
                    examples = @ExampleObject(name = "Gerekçe", value = CancellationExamples.CANCELLATION_REQUEST))))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "İptal talebi açıldı",
                    content = @Content(mediaType = JSON,
                            schema = @Schema(implementation = RequestLeaveCancellationResponse.class),
                            examples = @ExampleObject(name = "İptal talebinde", value = CancellationExamples.CANCELLATION_REQUESTED))),
            @ApiResponse(responseCode = "400", description = "Gerekçe boş veya çok uzun",
                    content = @Content(mediaType = JSON, schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "Gerekçe yok", value = CancellationExamples.CANCEL_REASON_BLANK))),
            @ApiResponse(responseCode = "401", description = "Token yok, geçersiz veya süresi dolmuş",
                    content = @Content(mediaType = JSON, schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "Token yok", value = ApiExamples.UNAUTHORIZED))),
            @ApiResponse(responseCode = "403", description = "Talep başkasına ait",
                    content = @Content(mediaType = JSON, schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "Sahibi değil", value = CancellationExamples.CANCELLATION_NOT_OWNER))),
            @ApiResponse(responseCode = "404", description = "İzin talebi yok",
                    content = @Content(mediaType = JSON, schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "Bulunamadı", value = CancellationExamples.LEAVE_REQUEST_NOT_FOUND))),
            @ApiResponse(responseCode = "409", description = "İzin onaylı değil veya başladı",
                    content = @Content(mediaType = JSON, schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "Onaylı değil", value = CancellationExamples.CANCELLATION_NOT_APPROVED)))
    })
    RequestLeaveCancellationResponse requestCancellation(@Parameter(description = "İzin talebinin id'si", example = "42") Long id,
                                                         RequestLeaveCancellationRequest request);

    @Operation(summary = "İptal talebini kabul et (onaylayıcı)",
            description = """
                    Karar yetkisi onaydakiyle aynıdır; kendi talebinizde karar veremezsiniz. İzin iptal olur, günler \
                    aynı transaction'da iade edilir (BR-17, T2). İzin başladıysa kabul edilemez.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "İzin iptal edildi, günler iade edildi",
                    content = @Content(mediaType = JSON,
                            schema = @Schema(implementation = ApproveLeaveCancellationResponse.class),
                            examples = @ExampleObject(name = "Kabul edildi", value = CancellationExamples.CANCELLATION_APPROVED))),
            @ApiResponse(responseCode = "401", description = "Token yok, geçersiz veya süresi dolmuş",
                    content = @Content(mediaType = JSON, schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "Token yok", value = ApiExamples.UNAUTHORIZED))),
            @ApiResponse(responseCode = "403", description = "Rol veya bu talep için yetki yok",
                    content = @Content(mediaType = JSON, schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "Kendi talebi", value = CancellationExamples.CANCELLATION_SELF_DECISION))),
            @ApiResponse(responseCode = "404", description = "İzin talebi yok",
                    content = @Content(mediaType = JSON, schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "Bulunamadı", value = CancellationExamples.LEAVE_REQUEST_NOT_FOUND))),
            @ApiResponse(responseCode = "409", description = "Açık bir iptal talebi yok veya izin başladı",
                    content = @Content(mediaType = JSON, schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "İptal talebi yok", value = CancellationExamples.CANCELLATION_NO_REQUEST)))
    })
    ApproveLeaveCancellationResponse approveCancellation(@Parameter(description = "İzin talebinin id'si", example = "42") Long id);

    @Operation(summary = "İptal talebini reddet (onaylayıcı)",
            description = "İzin onaylı kalır, günler iade edilmez (BR-17). Açıklama isteğe bağlıdır; gövde gönderilmeyebilir.",
            requestBody = @RequestBody(required = false, content = @Content(mediaType = JSON,
                    examples = @ExampleObject(name = "Açıklama", value = CancellationExamples.CANCELLATION_REJECT_REQUEST))))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "İptal talebi reddedildi, izin onaylı",
                    content = @Content(mediaType = JSON,
                            schema = @Schema(implementation = RejectLeaveCancellationResponse.class),
                            examples = @ExampleObject(name = "Reddedildi", value = CancellationExamples.CANCELLATION_REJECTED))),
            @ApiResponse(responseCode = "401", description = "Token yok, geçersiz veya süresi dolmuş",
                    content = @Content(mediaType = JSON, schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "Token yok", value = ApiExamples.UNAUTHORIZED))),
            @ApiResponse(responseCode = "403", description = "Rol veya bu talep için yetki yok",
                    content = @Content(mediaType = JSON, schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "Kendi talebi", value = CancellationExamples.CANCELLATION_SELF_DECISION))),
            @ApiResponse(responseCode = "404", description = "İzin talebi yok",
                    content = @Content(mediaType = JSON, schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "Bulunamadı", value = CancellationExamples.LEAVE_REQUEST_NOT_FOUND))),
            @ApiResponse(responseCode = "409", description = "Açık bir iptal talebi yok",
                    content = @Content(mediaType = JSON, schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "İptal talebi yok", value = CancellationExamples.CANCELLATION_NO_REQUEST)))
    })
    RejectLeaveCancellationResponse rejectCancellation(@Parameter(description = "İzin talebinin id'si", example = "42") Long id,
                                                       RejectLeaveCancellationRequest request);

    @Operation(summary = "İptal talebimden vazgeç",
            description = "Yalnızca talep sahibi vazgeçebilir; izin onaylı kalır.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "İptal talebi geri alındı, izin onaylı",
                    content = @Content(mediaType = JSON,
                            schema = @Schema(implementation = WithdrawLeaveCancellationResponse.class),
                            examples = @ExampleObject(name = "Vazgeçildi", value = CancellationExamples.CANCELLATION_WITHDRAWN))),
            @ApiResponse(responseCode = "401", description = "Token yok, geçersiz veya süresi dolmuş",
                    content = @Content(mediaType = JSON, schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "Token yok", value = ApiExamples.UNAUTHORIZED))),
            @ApiResponse(responseCode = "403", description = "Talep başkasına ait",
                    content = @Content(mediaType = JSON, schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "Sahibi değil", value = CancellationExamples.CANCELLATION_NOT_OWNER))),
            @ApiResponse(responseCode = "404", description = "İzin talebi yok",
                    content = @Content(mediaType = JSON, schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "Bulunamadı", value = CancellationExamples.LEAVE_REQUEST_NOT_FOUND))),
            @ApiResponse(responseCode = "409", description = "Açık bir iptal talebi yok",
                    content = @Content(mediaType = JSON, schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "İptal talebi yok", value = CancellationExamples.CANCELLATION_WITHDRAW_NONE)))
    })
    WithdrawLeaveCancellationResponse withdrawCancellation(@Parameter(description = "İzin talebinin id'si", example = "42") Long id);
}
