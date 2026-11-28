package com.dogus.vacation.api.docs;

import static com.dogus.vacation.api.docs.ApiExamples.JSON;

import com.dogus.vacation.api.advice.ErrorResponse;
import com.dogus.vacation.application.usecase.employee.create.CreateEmployeeRequest;
import com.dogus.vacation.application.usecase.employee.create.CreateEmployeeResponse;
import com.dogus.vacation.application.usecase.employee.deactivate.DeactivateEmployeeResponse;
import com.dogus.vacation.application.usecase.employee.get.GetEmployeeResponse;
import com.dogus.vacation.application.usecase.employee.list.ListEmployeesResponse;
import com.dogus.vacation.application.usecase.employee.update.UpdateEmployeeRequest;
import com.dogus.vacation.application.usecase.employee.update.UpdateEmployeeResponse;
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
 * Çalışan yönetimi uç noktalarının Swagger açıklamaları ({@code EmployeeController} uygular).
 * Hepsi yalnızca İK'ya açıktır.
 */
@Tag(name = "Çalışanlar", description = "Çalışan ekleme, güncelleme, pasife alma, listeleme ve detay (İK). FR-06 – FR-09, BR-13, T4")
public interface EmployeeApi {

    @Operation(summary = "Çalışanları listele (İK)",
            description = "Departmana ve aktifliğe göre süzülebilir; soyad, ad sırasıyla ve sayfalı döner.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Çalışanlar",
                    content = @Content(mediaType = JSON,
                            schema = @Schema(implementation = ListEmployeesResponse.class),
                            examples = @ExampleObject(name = "Yazılım Geliştirme", value = OrganizationExamples.EMPLOYEE_LIST))),
            @ApiResponse(responseCode = "400", description = "Filtre veya sayfalama geçersiz",
                    content = @Content(mediaType = JSON, schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "Sayfa büyüklüğü 0", value = OrganizationExamples.EMPLOYEE_INVALID_FILTER))),
            @ApiResponse(responseCode = "401", description = "Token yok, geçersiz veya süresi dolmuş",
                    content = @Content(mediaType = JSON, schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "Token yok", value = ApiExamples.UNAUTHORIZED))),
            @ApiResponse(responseCode = "403", description = "İK değil",
                    content = @Content(mediaType = JSON, schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "Yetki yok", value = ApiExamples.FORBIDDEN)))
    })
    ListEmployeesResponse list(
            @Parameter(description = "Yalnızca bu departmandakiler; boşsa hepsi", example = "2") Long departmentId,
            @Parameter(description = "true: aktifler, false: pasifler; boşsa hepsi", example = "true") Boolean active,
            @Parameter(description = "Sayfa numarası, 0'dan başlar; boşsa 0", example = "0") Integer page,
            @Parameter(description = "Sayfa büyüklüğü, 1–100; boşsa 20", example = "20") Integer size);

    @Operation(summary = "Çalışan ekle (İK)",
            description = """
                    Çalışan ve bu yılın ilk bakiyeleri tek transaction'da oluşturulur (T4): yıllık izin kıdem ve \
                    yaşa göre, mazeret 5 gün; hastalık ve ücretsiz izne bakiye açılmaz. Bakiye adımı hata verirse \
                    çalışan da oluşmaz. İlk şifre BCrypt ile saklanır ve cevapta yer almaz. İşe giriş tarihi \
                    gelecekte olamaz; çalışan en az 15 yaşında olmalı.""",
            requestBody = @RequestBody(required = true, content = @Content(mediaType = JSON,
                    examples = @ExampleObject(name = "Yeni çalışan", value = OrganizationExamples.EMPLOYEE_CREATE_REQUEST))))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Çalışan ve ilk bakiyeleri eklendi",
                    headers = @Header(name = "Location", description = "Yeni çalışanın adresi, örn. /api/employees/11"),
                    content = @Content(mediaType = JSON,
                            schema = @Schema(implementation = CreateEmployeeResponse.class),
                            examples = @ExampleObject(name = "Eklendi", value = OrganizationExamples.EMPLOYEE_CREATED))),
            @ApiResponse(responseCode = "400", description = "Alanlardan biri geçersiz",
                    content = @Content(mediaType = JSON, schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "Tarih kuralları", value = OrganizationExamples.EMPLOYEE_INVALID_DATES))),
            @ApiResponse(responseCode = "401", description = "Token yok, geçersiz veya süresi dolmuş",
                    content = @Content(mediaType = JSON, schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "Token yok", value = ApiExamples.UNAUTHORIZED))),
            @ApiResponse(responseCode = "403", description = "İK değil",
                    content = @Content(mediaType = JSON, schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "Yetki yok", value = ApiExamples.FORBIDDEN))),
            @ApiResponse(responseCode = "404", description = "Departman veya yönetici yok",
                    content = @Content(mediaType = JSON, schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "Departman yok", value = OrganizationExamples.EMPLOYEE_DEPARTMENT_NOT_FOUND))),
            @ApiResponse(responseCode = "409", description = "E-posta başka bir çalışanda kayıtlı",
                    content = @Content(mediaType = JSON, schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "E-posta kullanılıyor", value = OrganizationExamples.EMPLOYEE_DUPLICATE_EMAIL)))
    })
    ResponseEntity<CreateEmployeeResponse> create(CreateEmployeeRequest request);

    @Operation(summary = "Çalışan detayı (İK)", description = "Pasif çalışan da görüntülenir; şifre bilgisi dönmez.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Çalışan",
                    content = @Content(mediaType = JSON,
                            schema = @Schema(implementation = GetEmployeeResponse.class),
                            examples = @ExampleObject(name = "Ayşe Demir", value = OrganizationExamples.EMPLOYEE_DETAIL))),
            @ApiResponse(responseCode = "401", description = "Token yok, geçersiz veya süresi dolmuş",
                    content = @Content(mediaType = JSON, schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "Token yok", value = ApiExamples.UNAUTHORIZED))),
            @ApiResponse(responseCode = "403", description = "İK değil",
                    content = @Content(mediaType = JSON, schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "Yetki yok", value = ApiExamples.FORBIDDEN))),
            @ApiResponse(responseCode = "404", description = "Çalışan yok",
                    content = @Content(mediaType = JSON, schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "Bulunamadı", value = OrganizationExamples.EMPLOYEE_NOT_FOUND)))
    })
    GetEmployeeResponse get(@Parameter(description = "Çalışanın id'si", example = "5") Long id);

    @Operation(summary = "Çalışanı güncelle (İK)",
            description = """
                    Ad, e-posta, tarihler, rol, departman ve yönetici değişir; şifre ve aktiflik değişmez. \
                    Çalışan kendi yöneticisi veya kendi ekibindeki birinin çalışanı olamaz. Son iki aktif İK'dan \
                    birinin rolü değiştirilemez (BR-13). Açılmış bakiyeler değişmez.""",
            requestBody = @RequestBody(required = true, content = @Content(mediaType = JSON,
                    examples = @ExampleObject(name = "Yöneticiye terfi", value = OrganizationExamples.EMPLOYEE_UPDATE_REQUEST))))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Çalışan güncellendi",
                    content = @Content(mediaType = JSON,
                            schema = @Schema(implementation = UpdateEmployeeResponse.class),
                            examples = @ExampleObject(name = "Güncellendi", value = OrganizationExamples.EMPLOYEE_UPDATED))),
            @ApiResponse(responseCode = "400", description = "Alanlardan biri geçersiz",
                    content = @Content(mediaType = JSON, schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "Kendi yöneticisi", value = OrganizationExamples.EMPLOYEE_OWN_MANAGER))),
            @ApiResponse(responseCode = "401", description = "Token yok, geçersiz veya süresi dolmuş",
                    content = @Content(mediaType = JSON, schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "Token yok", value = ApiExamples.UNAUTHORIZED))),
            @ApiResponse(responseCode = "403", description = "İK değil",
                    content = @Content(mediaType = JSON, schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "Yetki yok", value = ApiExamples.FORBIDDEN))),
            @ApiResponse(responseCode = "404", description = "Çalışan, departman veya yönetici yok",
                    content = @Content(mediaType = JSON, schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "Bulunamadı", value = OrganizationExamples.EMPLOYEE_NOT_FOUND))),
            @ApiResponse(responseCode = "409", description = "E-posta başka bir çalışanda kayıtlı",
                    content = @Content(mediaType = JSON, schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "E-posta kullanılıyor", value = OrganizationExamples.EMPLOYEE_DUPLICATE_EMAIL))),
            @ApiResponse(responseCode = "422", description = "Yönetici döngüsü veya son iki İK'dan biri",
                    content = @Content(mediaType = JSON, schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "Yönetici döngüsü", value = OrganizationExamples.EMPLOYEE_MANAGER_CYCLE)))
    })
    UpdateEmployeeResponse update(
            @Parameter(description = "Güncellenecek çalışanın id'si", example = "5") Long id,
            UpdateEmployeeRequest request);

    @Operation(summary = "Çalışanı pasife al (İK)",
            description = """
                    Çalışan silinmez; pasif çalışan giriş yapamaz ve talep açamaz. Son iki aktif İK'dan biri \
                    pasife alınamaz (BR-13). Zaten pasif çalışan için aynı cevap döner.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Çalışan pasif",
                    content = @Content(mediaType = JSON,
                            schema = @Schema(implementation = DeactivateEmployeeResponse.class),
                            examples = @ExampleObject(name = "Pasife alındı", value = OrganizationExamples.EMPLOYEE_DEACTIVATED))),
            @ApiResponse(responseCode = "401", description = "Token yok, geçersiz veya süresi dolmuş",
                    content = @Content(mediaType = JSON, schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "Token yok", value = ApiExamples.UNAUTHORIZED))),
            @ApiResponse(responseCode = "403", description = "İK değil",
                    content = @Content(mediaType = JSON, schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "Yetki yok", value = ApiExamples.FORBIDDEN))),
            @ApiResponse(responseCode = "404", description = "Çalışan yok",
                    content = @Content(mediaType = JSON, schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "Bulunamadı", value = OrganizationExamples.EMPLOYEE_NOT_FOUND))),
            @ApiResponse(responseCode = "422", description = "Son iki aktif İK'dan biri",
                    content = @Content(mediaType = JSON, schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "Son İK", value = OrganizationExamples.EMPLOYEE_LAST_HR)))
    })
    DeactivateEmployeeResponse deactivate(
            @Parameter(description = "Pasife alınacak çalışanın id'si", example = "5") Long id);
}
