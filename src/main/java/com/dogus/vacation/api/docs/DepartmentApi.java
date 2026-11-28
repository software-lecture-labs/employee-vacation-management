package com.dogus.vacation.api.docs;

import static com.dogus.vacation.api.docs.ApiExamples.JSON;

import com.dogus.vacation.api.advice.ErrorResponse;
import com.dogus.vacation.application.usecase.department.create.CreateDepartmentRequest;
import com.dogus.vacation.application.usecase.department.create.CreateDepartmentResponse;
import com.dogus.vacation.application.usecase.department.list.ListDepartmentsResponse;
import com.dogus.vacation.application.usecase.department.update.UpdateDepartmentRequest;
import com.dogus.vacation.application.usecase.department.update.UpdateDepartmentResponse;
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
 * Departman uç noktalarının Swagger açıklamaları ({@code DepartmentController} uygular).
 * Açıklamalar controller'dan ayrıdır; Request ve Response record'ları Swagger'a bağımlı olmaz.
 */
@Tag(name = "Departmanlar", description = "Departman listeleme (herkes), ekleme ve güncelleme (İK). FR-10")
public interface DepartmentApi {

    @Operation(summary = "Departmanları listele",
            description = "Ada göre sıralı, sayfalı departman listesi. Giriş yapmış herkes.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Departmanlar",
                    content = @Content(mediaType = JSON,
                            schema = @Schema(implementation = ListDepartmentsResponse.class),
                            examples = @ExampleObject(name = "Üç departman", value = OrganizationExamples.DEPARTMENT_LIST))),
            @ApiResponse(responseCode = "400", description = "Sayfalama geçersiz",
                    content = @Content(mediaType = JSON, schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "Sayfa çok büyük", value = OrganizationExamples.DEPARTMENT_INVALID_PAGE))),
            @ApiResponse(responseCode = "401", description = "Token yok, geçersiz veya süresi dolmuş",
                    content = @Content(mediaType = JSON, schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "Token yok", value = ApiExamples.UNAUTHORIZED)))
    })
    ListDepartmentsResponse list(
            @Parameter(description = "Sayfa numarası, 0'dan başlar; boşsa 0", example = "0") Integer page,
            @Parameter(description = "Sayfa büyüklüğü, 1–100; boşsa 20", example = "20") Integer size);

    @Operation(summary = "Departman ekle (İK)",
            description = """
                    Ad zorunlu, en fazla 100 karakter ve büyük/küçük harfe bakmadan tekil. Yeni departmanın \
                    adresi `Location` başlığında döner. Departman silinmez.""",
            requestBody = @RequestBody(required = true, content = @Content(mediaType = JSON,
                    examples = @ExampleObject(name = "Hukuk", value = OrganizationExamples.DEPARTMENT_CREATE_REQUEST))))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Departman eklendi",
                    headers = @Header(name = "Location", description = "Yeni departmanın adresi, örn. /api/departments/4"),
                    content = @Content(mediaType = JSON,
                            schema = @Schema(implementation = CreateDepartmentResponse.class),
                            examples = @ExampleObject(name = "Eklendi", value = OrganizationExamples.DEPARTMENT_CREATED))),
            @ApiResponse(responseCode = "400", description = "Ad boş veya çok uzun",
                    content = @Content(mediaType = JSON, schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "Boş ad", value = OrganizationExamples.DEPARTMENT_BLANK_NAME))),
            @ApiResponse(responseCode = "401", description = "Token yok, geçersiz veya süresi dolmuş",
                    content = @Content(mediaType = JSON, schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "Token yok", value = ApiExamples.UNAUTHORIZED))),
            @ApiResponse(responseCode = "403", description = "İK değil",
                    content = @Content(mediaType = JSON, schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "Yetki yok", value = ApiExamples.FORBIDDEN))),
            @ApiResponse(responseCode = "409", description = "Ad başka bir departmanda kullanılıyor",
                    content = @Content(mediaType = JSON, schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "Ad kullanılıyor", value = OrganizationExamples.DEPARTMENT_DUPLICATE)))
    })
    ResponseEntity<CreateDepartmentResponse> create(CreateDepartmentRequest request);

    @Operation(summary = "Departmanın adını güncelle (İK)",
            description = """
                    Hangi departmanın güncelleneceği yoldaki id'den alınır. Departman kendi adını koruyabilir; \
                    başka bir departmanın adı (harf farkı gözetmeden) verilemez.""",
            requestBody = @RequestBody(required = true, content = @Content(mediaType = JSON,
                    examples = @ExampleObject(name = "Yeni ad", value = OrganizationExamples.DEPARTMENT_UPDATE_REQUEST))))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Departman güncellendi",
                    content = @Content(mediaType = JSON,
                            schema = @Schema(implementation = UpdateDepartmentResponse.class),
                            examples = @ExampleObject(name = "Güncellendi", value = OrganizationExamples.DEPARTMENT_UPDATED))),
            @ApiResponse(responseCode = "400", description = "Ad boş veya çok uzun",
                    content = @Content(mediaType = JSON, schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "Boş ad", value = OrganizationExamples.DEPARTMENT_BLANK_NAME))),
            @ApiResponse(responseCode = "401", description = "Token yok, geçersiz veya süresi dolmuş",
                    content = @Content(mediaType = JSON, schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "Token yok", value = ApiExamples.UNAUTHORIZED))),
            @ApiResponse(responseCode = "403", description = "İK değil",
                    content = @Content(mediaType = JSON, schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "Yetki yok", value = ApiExamples.FORBIDDEN))),
            @ApiResponse(responseCode = "404", description = "Departman yok",
                    content = @Content(mediaType = JSON, schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "Bulunamadı", value = OrganizationExamples.DEPARTMENT_NOT_FOUND))),
            @ApiResponse(responseCode = "409", description = "Ad başka bir departmanda kullanılıyor",
                    content = @Content(mediaType = JSON, schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "Ad kullanılıyor", value = OrganizationExamples.DEPARTMENT_DUPLICATE)))
    })
    UpdateDepartmentResponse update(
            @Parameter(description = "Güncellenecek departmanın id'si", example = "3") Long id,
            UpdateDepartmentRequest request);
}
