package com.dogus.vacation.api.docs;

import static com.dogus.vacation.api.docs.ApiExamples.JSON;

import com.dogus.vacation.api.advice.ErrorResponse;
import com.dogus.vacation.application.usecase.balance.accrual.RunAnnualAccrualResponse;
import com.dogus.vacation.application.usecase.balance.employee.GetEmployeeBalancesResponse;
import com.dogus.vacation.application.usecase.balance.mine.GetMyBalancesResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * İzin bakiyesi ve yıllık hak ediş uç noktalarının Swagger açıklamaları
 * ({@code LeaveBalanceController} uygular).
 */
@Tag(name = "İzin bakiyeleri", description = "Bakiye görüntüleme ve yıllık hak ediş (T3). FR-32, FR-34")
public interface LeaveBalanceApi {

    @Operation(summary = "Kendi bakiyelerim",
            description = """
                    Giriş yapan çalışanın o yıldaki bakiyeleri, izin türü sırasıyla. Kimin bakiyesi olduğu \
                    token'dan alınır. O yıl için bakiye açılmamışsa (örneğin yıllık hak ediş henüz \
                    çalıştırılmamışsa) boş liste döner. Giriş yapmış herkes.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Bakiyeler",
                    content = @Content(mediaType = JSON,
                            schema = @Schema(implementation = GetMyBalancesResponse.class),
                            examples = @ExampleObject(name = "Yıllık ve mazeret", value = ApiExamples.MY_BALANCES))),
            @ApiResponse(responseCode = "400", description = "Yıl 2000–2100 dışında",
                    content = @Content(mediaType = JSON, schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "Geçersiz yıl", value = ApiExamples.BALANCE_YEAR_OUT_OF_RANGE))),
            @ApiResponse(responseCode = "401", description = "Token yok, geçersiz veya süresi dolmuş",
                    content = @Content(mediaType = JSON, schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "Token yok", value = ApiExamples.UNAUTHORIZED)))
    })
    GetMyBalancesResponse mine(@Parameter(description = "Bakiye yılı, 2000–2100; boşsa bu yıl", example = "2026")
                               Integer year);

    @Operation(summary = "Bir çalışanın bakiyeleri (yönetici, İK)",
            description = """
                    İK herkesi, yönetici yalnızca kendisine doğrudan bağlı çalışanları görür. Normal çalışan \
                    kendi bakiyesini "Kendi bakiyelerim"den görür; bu adres ona kendi id'siyle bile 403 döner.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Bakiyeler",
                    content = @Content(mediaType = JSON,
                            schema = @Schema(implementation = GetEmployeeBalancesResponse.class),
                            examples = @ExampleObject(name = "Ekipteki bir çalışan", value = ApiExamples.EMPLOYEE_BALANCES))),
            @ApiResponse(responseCode = "400", description = "Yıl 2000–2100 dışında",
                    content = @Content(mediaType = JSON, schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "Geçersiz yıl", value = ApiExamples.BALANCE_YEAR_OUT_OF_RANGE))),
            @ApiResponse(responseCode = "401", description = "Token yok, geçersiz veya süresi dolmuş",
                    content = @Content(mediaType = JSON, schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "Token yok", value = ApiExamples.UNAUTHORIZED))),
            @ApiResponse(responseCode = "403", description = "Çalışan rolü ya da başka ekipten biri",
                    content = @Content(mediaType = JSON, schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "Başka ekip", value = ApiExamples.EMPLOYEE_BALANCES_OTHER_TEAM))),
            @ApiResponse(responseCode = "404", description = "Çalışan yok",
                    content = @Content(mediaType = JSON, schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "Bulunamadı", value = ApiExamples.EMPLOYEE_NOT_FOUND)))
    })
    GetEmployeeBalancesResponse ofEmployee(@Parameter(description = "Çalışan id'si", example = "12") Long id,
                                           @Parameter(description = "Bakiye yılı, 2000–2100; boşsa bu yıl",
                                                   example = "2026") Integer year);

    @Operation(summary = "Yıllık hak edişi çalıştır (İK)",
            description = """
                    Bütün aktif çalışanlara o yılın bakiyelerini tek işlemde açar (T3): yıllık izin kıdem ve \
                    yaşa göre, mazeret izni yıllık sınırı kadar. O yılın bakiyesi zaten açık olan ve henüz işe \
                    başlamamış çalışanlar atlanır. Bir çalışanda hata olursa hiçbir çalışana hak yazılmaz. \
                    Yalnızca bu yıl veya gelecek yıl için, her yıl bir kez çalıştırılabilir (BR-11).""")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Hak ediş çalıştırıldı",
                    content = @Content(mediaType = JSON,
                            schema = @Schema(implementation = RunAnnualAccrualResponse.class),
                            examples = @ExampleObject(name = "2027 hak edişi", value = ApiExamples.ACCRUAL_RUN))),
            @ApiResponse(responseCode = "400", description = "Yıl boş ya da bu yıl veya gelecek yıl değil",
                    content = @Content(mediaType = JSON, schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "Geçersiz yıl", value = ApiExamples.ACCRUAL_INVALID_YEAR))),
            @ApiResponse(responseCode = "401", description = "Token yok, geçersiz veya süresi dolmuş",
                    content = @Content(mediaType = JSON, schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "Token yok", value = ApiExamples.UNAUTHORIZED))),
            @ApiResponse(responseCode = "403", description = "İK değil",
                    content = @Content(mediaType = JSON, schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "Yetki yok", value = ApiExamples.FORBIDDEN))),
            @ApiResponse(responseCode = "409", description = "Bu yılın hak edişi zaten çalıştırılmış",
                    content = @Content(mediaType = JSON, schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "Zaten çalıştırıldı", value = ApiExamples.ACCRUAL_ALREADY_RUN)))
    })
    RunAnnualAccrualResponse runAccrual(@Parameter(description = "Hak ediş yılı: bu yıl veya gelecek yıl",
            example = "2027") Integer year);
}
