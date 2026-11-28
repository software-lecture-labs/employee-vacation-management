package com.dogus.vacation.api.docs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.lang.reflect.RecordComponent;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import com.dogus.vacation.AbstractIntegrationTest;
import com.dogus.vacation.api.advice.ErrorResponse;
import com.dogus.vacation.application.common.PageResponse;
import com.dogus.vacation.application.common.view.BalanceView;
import com.dogus.vacation.application.common.view.DepartmentView;
import com.dogus.vacation.application.common.view.EmployeeRef;
import com.dogus.vacation.application.common.view.EmployeeView;
import com.dogus.vacation.application.common.view.HistoryEntryView;
import com.dogus.vacation.application.common.view.LeaveRequestDetailView;
import com.dogus.vacation.application.common.view.LeaveTypeRef;
import com.dogus.vacation.application.common.view.LeaveTypeView;
import com.dogus.vacation.application.usecase.balance.accrual.RunAnnualAccrualResponse;
import com.dogus.vacation.application.usecase.balance.carryover.RunCarryOverResponse;
import com.dogus.vacation.application.usecase.balance.employee.GetEmployeeBalancesResponse;
import com.dogus.vacation.application.usecase.balance.mine.GetMyBalancesResponse;
import com.dogus.vacation.application.usecase.cancellation.approve.ApproveLeaveCancellationResponse;
import com.dogus.vacation.application.usecase.cancellation.cancel.CancelLeaveRequest;
import com.dogus.vacation.application.usecase.cancellation.cancel.CancelLeaveResponse;
import com.dogus.vacation.application.usecase.cancellation.reject.RejectLeaveCancellationRequest;
import com.dogus.vacation.application.usecase.cancellation.reject.RejectLeaveCancellationResponse;
import com.dogus.vacation.application.usecase.cancellation.request.RequestLeaveCancellationRequest;
import com.dogus.vacation.application.usecase.cancellation.request.RequestLeaveCancellationResponse;
import com.dogus.vacation.application.usecase.cancellation.withdraw.WithdrawLeaveCancellationResponse;
import com.dogus.vacation.application.usecase.department.create.CreateDepartmentRequest;
import com.dogus.vacation.application.usecase.department.create.CreateDepartmentResponse;
import com.dogus.vacation.application.usecase.department.list.ListDepartmentsResponse;
import com.dogus.vacation.application.usecase.department.update.UpdateDepartmentRequest;
import com.dogus.vacation.application.usecase.department.update.UpdateDepartmentResponse;
import com.dogus.vacation.application.usecase.employee.create.CreateEmployeeRequest;
import com.dogus.vacation.application.usecase.employee.create.CreateEmployeeResponse;
import com.dogus.vacation.application.usecase.employee.deactivate.DeactivateEmployeeResponse;
import com.dogus.vacation.application.usecase.employee.get.GetEmployeeResponse;
import com.dogus.vacation.application.usecase.employee.list.ListEmployeesResponse;
import com.dogus.vacation.application.usecase.employee.update.UpdateEmployeeRequest;
import com.dogus.vacation.application.usecase.employee.update.UpdateEmployeeResponse;
import com.dogus.vacation.application.usecase.history.search.SearchLeaveHistoryResponse;
import com.dogus.vacation.application.usecase.leavetype.create.CreateLeaveTypeRequest;
import com.dogus.vacation.application.usecase.leavetype.create.CreateLeaveTypeResponse;
import com.dogus.vacation.application.usecase.leavetype.list.ListLeaveTypesResponse;
import com.dogus.vacation.application.usecase.leavetype.update.UpdateLeaveTypeRequest;
import com.dogus.vacation.application.usecase.leavetype.update.UpdateLeaveTypeResponse;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Uygulamanın ürettiği OpenAPI belgesini ({@code /v3/api-docs}) okuyup bakiye, devir, izin türü,
 * geçmiş, departman, çalışan ve iptal uç noktalarının Swagger açıklamalarını denetler.
 *
 * <p>Asıl amaç örneklerin koddan kopmamasıdır: her örneğin alanları ilgili record'un
 * alanlarıyla birebir karşılaştırılır. Bir Response'a alan eklenir de örnek sınıfları
 * ({@link ApiExamples}, {@link OrganizationExamples}, {@link CancellationExamples}) güncellenmezse bu test kırılır. Örnek geçerli bir JSON değilse de kırılır.
 */
class OpenApiDocumentationTest extends AbstractIntegrationTest {

    /** Belgelenen uç nokta: yol, HTTP metodu, başarı kodu ve cevap record'u. */
    record Endpoint(String path, String method, String successCode, Class<? extends Record> response) {

        @Override
        public String toString() {
            return method.toUpperCase() + " " + path;
        }
    }

    private static final String CANCEL = "/api/leave-requests/{id}";

    static Stream<Endpoint> endpoints() {
        return Stream.of(
                new Endpoint("/api/leave-types", "get", "200", ListLeaveTypesResponse.class),
                new Endpoint("/api/leave-types", "post", "201", CreateLeaveTypeResponse.class),
                new Endpoint("/api/leave-types/{id}", "put", "200", UpdateLeaveTypeResponse.class),
                new Endpoint("/api/leave-requests/history", "get", "200", SearchLeaveHistoryResponse.class),
                new Endpoint("/api/leave-balances/mine", "get", "200", GetMyBalancesResponse.class),
                new Endpoint("/api/employees/{id}/leave-balances", "get", "200", GetEmployeeBalancesResponse.class),
                new Endpoint("/api/leave-balances/accruals", "post", "201", RunAnnualAccrualResponse.class),
                new Endpoint("/api/leave-balances/carry-overs", "post", "200", RunCarryOverResponse.class),
                new Endpoint("/api/departments", "get", "200", ListDepartmentsResponse.class),
                new Endpoint("/api/departments", "post", "201", CreateDepartmentResponse.class),
                new Endpoint("/api/departments/{id}", "put", "200", UpdateDepartmentResponse.class),
                new Endpoint("/api/employees", "get", "200", ListEmployeesResponse.class),
                new Endpoint("/api/employees", "post", "201", CreateEmployeeResponse.class),
                new Endpoint("/api/employees/{id}", "get", "200", GetEmployeeResponse.class),
                new Endpoint("/api/employees/{id}", "put", "200", UpdateEmployeeResponse.class),
                new Endpoint("/api/employees/{id}/deactivate", "post", "200", DeactivateEmployeeResponse.class),
                new Endpoint(CANCEL + "/cancel", "post", "200", CancelLeaveResponse.class),
                new Endpoint(CANCEL + "/cancel-request", "post", "200", RequestLeaveCancellationResponse.class),
                new Endpoint(CANCEL + "/cancel-request/approve", "post", "200", ApproveLeaveCancellationResponse.class),
                new Endpoint(CANCEL + "/cancel-request/reject", "post", "200", RejectLeaveCancellationResponse.class),
                new Endpoint(CANCEL + "/cancel-request/withdraw", "post", "200",
                        WithdrawLeaveCancellationResponse.class));
    }

    @Autowired
    private WebApplicationContext context;

    private String apiDocs;

    @BeforeEach
    void loadApiDocs() throws Exception {
        apiDocs = MockMvcBuilders.webAppContextSetup(context).build()
                .perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("endpoints")
    void hasASummaryATagAndDescribedParameters(Endpoint endpoint) {
        Map<String, Object> operation = operation(endpoint);

        assertThat((String) operation.get("summary")).isNotBlank();
        assertThat((List<?>) operation.get("tags")).isNotEmpty();
        List<Map<String, Object>> parameters = listOrEmpty(operation.get("parameters"));
        assertThat(parameters).allSatisfy(parameter -> {
            assertThat((String) parameter.get("description")).as(parameter.get("name") + " açıklaması").isNotBlank();
            assertThat(parameter.get("example")).as(parameter.get("name") + " örneği").isNotNull();
        });
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("endpoints")
    void successExampleHasExactlyTheFieldsOfTheResponse(Endpoint endpoint) {
        List<Map<String, Object>> examples = examples(endpoint, endpoint.successCode());

        assertThat(examples).isNotEmpty();
        assertThat(examples).allSatisfy(example ->
                assertThat(example.keySet()).containsExactlyInAnyOrderElementsOf(fieldsOf(endpoint.response())));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("endpoints")
    void everyErrorExampleHasTheFieldsOfErrorResponse(Endpoint endpoint) {
        List<String> errorCodes = responseCodes(endpoint).stream().filter(code -> code.startsWith("4")).toList();

        assertThat(errorCodes).contains("401");
        assertThat(errorCodes).allSatisfy(code -> {
            List<Map<String, Object>> examples = examples(endpoint, code);
            assertThat(examples).as(code + " örneği").isNotEmpty();
            assertThat(examples).allSatisfy(example -> {
                assertThat(example.keySet()).containsExactlyInAnyOrderElementsOf(fieldsOf(ErrorResponse.class));
                assertThat(example.get("status")).as(code + " örneğindeki status").isEqualTo(Integer.valueOf(code));
            });
        });
    }

    @Test
    void nestedViewsInTheExamplesHaveTheFieldsOfTheirRecords() {
        Map<String, Object> leaveTypes = firstExample("/api/leave-types", "get", "200");
        assertThat(keysOf(first(leaveTypes.get("leaveTypes")))).containsExactlyInAnyOrderElementsOf(fieldsOf(LeaveTypeView.class));

        Map<String, Object> myBalances = firstExample("/api/leave-balances/mine", "get", "200");
        Map<String, Object> balance = first(myBalances.get("balances"));
        assertThat(balance.keySet()).containsExactlyInAnyOrderElementsOf(fieldsOf(BalanceView.class));
        assertThat(keysOf(balance.get("leaveType"))).containsExactlyInAnyOrderElementsOf(fieldsOf(LeaveTypeRef.class));

        Map<String, Object> employeeBalances = firstExample("/api/employees/{id}/leave-balances", "get", "200");
        assertThat(keysOf(employeeBalances.get("employee"))).containsExactlyInAnyOrderElementsOf(fieldsOf(EmployeeRef.class));

        Map<String, Object> history = firstExample("/api/leave-requests/history", "get", "200");
        Map<String, Object> page = asMap(history.get("history"));
        assertThat(page.keySet()).containsExactlyInAnyOrderElementsOf(fieldsOf(PageResponse.class));
        assertThat(keysOf(first(page.get("content")))).containsExactlyInAnyOrderElementsOf(fieldsOf(HistoryEntryView.class));

        Map<String, Object> departments = asMap(firstExample("/api/departments", "get", "200").get("departments"));
        assertThat(departments.keySet()).containsExactlyInAnyOrderElementsOf(fieldsOf(PageResponse.class));
        assertThat(keysOf(first(departments.get("content")))).containsExactlyInAnyOrderElementsOf(fieldsOf(DepartmentView.class));

        Map<String, Object> employees = asMap(firstExample("/api/employees", "get", "200").get("employees"));
        assertThat(employees.keySet()).containsExactlyInAnyOrderElementsOf(fieldsOf(PageResponse.class));
        List<Map<String, Object>> listed = listOrEmpty(employees.get("content"));
        assertThat(listed).allSatisfy(employee ->
                assertThat(employee.keySet()).containsExactlyInAnyOrderElementsOf(fieldsOf(EmployeeView.class)));
        // Yöneticisi olmayan çalışanda manager null'dır; alanları yöneticisi olanda denetlenir.
        Map<String, Object> withManager = listed.stream().filter(employee -> employee.get("manager") != null)
                .findFirst().orElseThrow();
        assertThat(keysOf(withManager.get("manager"))).containsExactlyInAnyOrderElementsOf(fieldsOf(EmployeeRef.class));

        Map<String, Object> created = firstExample("/api/employees", "post", "201");
        assertThat(keysOf(created.get("employee"))).containsExactlyInAnyOrderElementsOf(fieldsOf(EmployeeView.class));
        assertThat(keysOf(first(created.get("balances")))).containsExactlyInAnyOrderElementsOf(fieldsOf(BalanceView.class));

        Map<String, Object> cancelled = asMap(firstExample(CANCEL + "/cancel", "post", "200").get("leaveRequest"));
        assertThat(cancelled.keySet()).containsExactlyInAnyOrderElementsOf(fieldsOf(LeaveRequestDetailView.class));
        assertThat(keysOf(cancelled.get("employee"))).containsExactlyInAnyOrderElementsOf(fieldsOf(EmployeeRef.class));
        assertThat(keysOf(cancelled.get("leaveType"))).containsExactlyInAnyOrderElementsOf(fieldsOf(LeaveTypeRef.class));
    }

    @Test
    void requestBodyExamplesHaveTheFieldsOfTheRequests() {
        assertThat(requestBodyExample("/api/leave-types", "post").keySet())
                .containsExactlyInAnyOrderElementsOf(fieldsOf(CreateLeaveTypeRequest.class));
        // id yoldan gelir; gövdedeki id yok sayıldığı için örnekte yok.
        assertThat(requestBodyExample("/api/leave-types/{id}", "put").keySet())
                .containsExactlyInAnyOrderElementsOf(
                        fieldsOf(UpdateLeaveTypeRequest.class).stream().filter(name -> !name.equals("id")).toList());

        assertThat(requestBodyExample("/api/departments", "post").keySet())
                .containsExactlyInAnyOrderElementsOf(fieldsOf(CreateDepartmentRequest.class));
        assertThat(requestBodyExample("/api/departments/{id}", "put").keySet())
                .containsExactlyInAnyOrderElementsOf(withoutPathId(UpdateDepartmentRequest.class, "id"));
        assertThat(requestBodyExample("/api/employees", "post").keySet())
                .containsExactlyInAnyOrderElementsOf(fieldsOf(CreateEmployeeRequest.class));
        assertThat(requestBodyExample("/api/employees/{id}", "put").keySet())
                .containsExactlyInAnyOrderElementsOf(withoutPathId(UpdateEmployeeRequest.class, "id"));
        assertThat(requestBodyExample(CANCEL + "/cancel", "post").keySet())
                .containsExactlyInAnyOrderElementsOf(withoutPathId(CancelLeaveRequest.class, "leaveRequestId"));
        assertThat(requestBodyExample(CANCEL + "/cancel-request", "post").keySet())
                .containsExactlyInAnyOrderElementsOf(withoutPathId(RequestLeaveCancellationRequest.class, "leaveRequestId"));
        assertThat(requestBodyExample(CANCEL + "/cancel-request/reject", "post").keySet())
                .containsExactlyInAnyOrderElementsOf(withoutPathId(RejectLeaveCancellationRequest.class, "leaveRequestId"));
    }

    @Test
    void theRejectBodyIsOptional() {
        // İsteğe bağlı gövdede OpenAPI "required" alanını hiç yazmaz; yazarsa false olmalı.
        Map<String, Object> requestBody = JsonPath.read(apiDocs,
                "$.paths['" + CANCEL + "/cancel-request/reject'].post.requestBody");

        assertThat(requestBody).doesNotContainEntry("required", true);
    }

    // ---- yardımcılar ----

    private Map<String, Object> operation(Endpoint endpoint) {
        return JsonPath.read(apiDocs, "$.paths['" + endpoint.path() + "']." + endpoint.method());
    }

    private List<String> responseCodes(Endpoint endpoint) {
        Map<String, Object> responses = JsonPath.read(apiDocs,
                "$.paths['" + endpoint.path() + "']." + endpoint.method() + ".responses");
        return List.copyOf(responses.keySet());
    }

    /** Bir cevabın {@code application/json} örnekleri; örnek metin olarak tutulmuşsa JSON'a çevrilir. */
    private List<Map<String, Object>> examples(Endpoint endpoint, String code) {
        return examples(endpoint.path(), endpoint.method(), code);
    }

    private List<Map<String, Object>> examples(String path, String method, String code) {
        Map<String, Object> examples = JsonPath.read(apiDocs, "$.paths['" + path + "']." + method
                + ".responses['" + code + "'].content['application/json'].examples");
        return examples.values().stream().map(example -> asMap(asMap(example).get("value"))).toList();
    }

    private Map<String, Object> firstExample(String path, String method, String code) {
        return examples(path, method, code).getFirst();
    }

    private Map<String, Object> requestBodyExample(String path, String method) {
        Map<String, Object> examples = JsonPath.read(apiDocs, "$.paths['" + path + "']." + method
                + ".requestBody.content['application/json'].examples");
        return asMap(asMap(examples.values().iterator().next()).get("value"));
    }

    /** Gövdedeki alanlar: yoldan gelen id gövdede gönderilmez, o yüzden örnekte de yoktur. */
    private static List<String> withoutPathId(Class<? extends Record> type, String pathField) {
        return fieldsOf(type).stream().filter(name -> !name.equals(pathField)).toList();
    }

    private static List<String> fieldsOf(Class<? extends Record> type) {
        return Arrays.stream(type.getRecordComponents()).map(RecordComponent::getName).toList();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object value) {
        if (value instanceof String json) {
            return JsonPath.parse(json).json();
        }
        return (Map<String, Object>) value;
    }

    private static Set<String> keysOf(Object value) {
        return asMap(value).keySet();
    }

    private static Map<String, Object> first(Object list) {
        return asMap(((List<?>) list).getFirst());
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> listOrEmpty(Object value) {
        return value == null ? List.of() : (List<Map<String, Object>>) value;
    }
}
