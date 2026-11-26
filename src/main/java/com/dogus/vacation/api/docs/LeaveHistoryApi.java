package com.dogus.vacation.api.docs;

import static com.dogus.vacation.api.docs.ApiExamples.JSON;

import java.time.LocalDate;

import com.dogus.vacation.api.advice.ErrorResponse;
import com.dogus.vacation.application.usecase.history.search.SearchLeaveHistoryResponse;
import com.dogus.vacation.domain.model.enums.LeaveRequestStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Şirket izin geçmişi uç noktasının Swagger açıklaması ({@code LeaveHistoryController} uygular).
 */
@Tag(name = "İzin geçmişi", description = "İK için şirket genelinde izin durum değişiklikleri. FR-26")
public interface LeaveHistoryApi {

    @Operation(summary = "Şirket izin geçmişini ara (İK)",
            description = """
                    Her satır bir talebin bir durumdan diğerine geçişidir; talep açılışında önceki durum boştur. \
                    Bütün filtreler isteğe bağlıdır ve birlikte kullanılabilir. En yeni olay önce gelir. \
                    Tarihler olayın tarihine göre filtrelenir ve iki uç da dahildir.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Geçmişin istenen sayfası",
                    content = @Content(mediaType = JSON,
                            schema = @Schema(implementation = SearchLeaveHistoryResponse.class),
                            examples = @ExampleObject(name = "Bir talebin açılışı ve onayı",
                                    value = ApiExamples.HISTORY_PAGE))),
            @ApiResponse(responseCode = "400", description = "Filtrelerden biri geçersiz",
                    content = @Content(mediaType = JSON, schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "Ters tarih aralığı", value = ApiExamples.HISTORY_REVERSED_RANGE))),
            @ApiResponse(responseCode = "401", description = "Token yok, geçersiz veya süresi dolmuş",
                    content = @Content(mediaType = JSON, schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "Token yok", value = ApiExamples.UNAUTHORIZED))),
            @ApiResponse(responseCode = "403", description = "İK değil",
                    content = @Content(mediaType = JSON, schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "Yetki yok", value = ApiExamples.FORBIDDEN)))
    })
    SearchLeaveHistoryResponse search(
            @Parameter(description = "Bu günden itibaren (yyyy-MM-dd, dahil)", example = "2026-11-01") LocalDate from,
            @Parameter(description = "Bu güne kadar (yyyy-MM-dd, dahil)", example = "2026-11-30") LocalDate to,
            @Parameter(description = "Talep sahibinin departmanı", example = "3") Long departmentId,
            @Parameter(description = "Geçişin yeni durumu", example = "APPROVED") LeaveRequestStatus status,
            @Parameter(description = "Talep sahibi", example = "12") Long employeeId,
            @Parameter(description = "Sayfa numarası, 0'dan başlar; boşsa 0", example = "0") Integer page,
            @Parameter(description = "Sayfa büyüklüğü, 1–100; boşsa 20", example = "20") Integer size);
}
