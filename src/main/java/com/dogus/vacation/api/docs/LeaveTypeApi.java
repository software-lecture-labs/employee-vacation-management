package com.dogus.vacation.api.docs;

import static com.dogus.vacation.api.docs.ApiExamples.JSON;

import com.dogus.vacation.api.advice.ErrorResponse;
import com.dogus.vacation.application.usecase.leavetype.create.CreateLeaveTypeRequest;
import com.dogus.vacation.application.usecase.leavetype.create.CreateLeaveTypeResponse;
import com.dogus.vacation.application.usecase.leavetype.list.ListLeaveTypesResponse;
import com.dogus.vacation.application.usecase.leavetype.update.UpdateLeaveTypeRequest;
import com.dogus.vacation.application.usecase.leavetype.update.UpdateLeaveTypeResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

/**
 * İzin türü uç noktalarının Swagger açıklamaları ({@code LeaveTypeController} uygular).
 *
 * <p>Açıklamalar controller'dan ayrı tutulur: controller yalnızca isteği mediator'a verir,
 * Request ve Response record'ları (application katmanı) Swagger'a bağımlı olmaz.
 */
@Tag(name = "İzin türleri", description = "İzin türlerini listeleme (herkes), ekleme ve güncelleme (İK). FR-12, FR-13")
public interface LeaveTypeApi {

    @Operation(summary = "İzin türlerini listele",
            description = "Bütün izin türleri, id sırasıyla; pasif türler de listelenir. Giriş yapmış herkes.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "İzin türleri",
                    content = @Content(mediaType = JSON,
                            schema = @Schema(implementation = ListLeaveTypesResponse.class),
                            examples = @ExampleObject(name = "Hazır dört tür", value = ApiExamples.LEAVE_TYPE_LIST))),
            @ApiResponse(responseCode = "401", description = "Token yok, geçersiz veya süresi dolmuş",
                    content = @Content(mediaType = JSON, schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "Token yok", value = ApiExamples.UNAUTHORIZED)))
    })
    ListLeaveTypesResponse list();

    @Operation(summary = "İzin türü ekle (İK)",
            description = """
                    Kod büyük harf ve alt çizgiden oluşur ve sonradan değişmez. "Bakiyeden düşer mi" de \
                    sonradan değişmez. Bakiyeden düşmeyen türde yıllık sınır boş olmalıdır. Yeni türün \
                    adresi `Location` başlığında döner.""",
            requestBody = @RequestBody(required = true, content = @Content(mediaType = JSON,
                    examples = @ExampleObject(name = "Evlilik izni", value = ApiExamples.LEAVE_TYPE_CREATE_REQUEST))))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "İzin türü eklendi",
                    headers = @Header(name = "Location", description = "Yeni türün adresi, örn. /api/leave-types/5"),
                    content = @Content(mediaType = JSON,
                            schema = @Schema(implementation = CreateLeaveTypeResponse.class),
                            examples = @ExampleObject(name = "Eklendi", value = ApiExamples.LEAVE_TYPE_CREATED))),
            @ApiResponse(responseCode = "400", description = "Alanlardan biri geçersiz",
                    content = @Content(mediaType = JSON, schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "Geçersiz kod", value = ApiExamples.LEAVE_TYPE_INVALID_CODE))),
            @ApiResponse(responseCode = "401", description = "Token yok, geçersiz veya süresi dolmuş",
                    content = @Content(mediaType = JSON, schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "Token yok", value = ApiExamples.UNAUTHORIZED))),
            @ApiResponse(responseCode = "403", description = "İK değil",
                    content = @Content(mediaType = JSON, schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "Yetki yok", value = ApiExamples.FORBIDDEN))),
            @ApiResponse(responseCode = "409", description = "Kod veya ad kullanılıyor",
                    content = @Content(mediaType = JSON, schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "Kod kullanılıyor", value = ApiExamples.LEAVE_TYPE_DUPLICATE_CODE)))
    })
    ResponseEntity<CreateLeaveTypeResponse> create(CreateLeaveTypeRequest request);

    @Operation(summary = "İzin türünü güncelle (İK)",
            description = """
                    Ad, yıllık sınır ve aktiflik değiştirilebilir; kod ve "bakiyeden düşer mi" değişmez. \
                    Hangi türün güncelleneceği yoldaki id'den alınır; gövdede id gönderilse bile yok sayılır. \
                    Pasif türle yeni talep açılamaz.""",
            requestBody = @RequestBody(required = true, content = @Content(mediaType = JSON,
                    examples = @ExampleObject(name = "Mazeret sınırını değiştir", value = ApiExamples.LEAVE_TYPE_UPDATE_REQUEST))))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "İzin türü güncellendi",
                    content = @Content(mediaType = JSON,
                            schema = @Schema(implementation = UpdateLeaveTypeResponse.class),
                            examples = @ExampleObject(name = "Güncellendi", value = ApiExamples.LEAVE_TYPE_UPDATED))),
            @ApiResponse(responseCode = "400", description = "Alanlardan biri geçersiz",
                    content = @Content(mediaType = JSON, schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "Düşmeyen türe sınır",
                                    value = ApiExamples.LEAVE_TYPE_LIMIT_WITHOUT_BALANCE))),
            @ApiResponse(responseCode = "401", description = "Token yok, geçersiz veya süresi dolmuş",
                    content = @Content(mediaType = JSON, schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "Token yok", value = ApiExamples.UNAUTHORIZED))),
            @ApiResponse(responseCode = "403", description = "İK değil",
                    content = @Content(mediaType = JSON, schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "Yetki yok", value = ApiExamples.FORBIDDEN))),
            @ApiResponse(responseCode = "404", description = "İzin türü yok",
                    content = @Content(mediaType = JSON, schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "Bulunamadı", value = ApiExamples.LEAVE_TYPE_NOT_FOUND))),
            @ApiResponse(responseCode = "409", description = "Ad başka bir türde kullanılıyor",
                    content = @Content(mediaType = JSON, schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "Ad kullanılıyor", value = ApiExamples.LEAVE_TYPE_DUPLICATE_NAME)))
    })
    UpdateLeaveTypeResponse update(@Parameter(description = "Güncellenecek izin türünün id'si", example = "2") Long id,
                                   UpdateLeaveTypeRequest request);
}
