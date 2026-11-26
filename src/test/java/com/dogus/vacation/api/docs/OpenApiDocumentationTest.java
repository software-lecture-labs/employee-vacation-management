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
import com.dogus.vacation.application.common.view.EmployeeRef;
import com.dogus.vacation.application.common.view.HistoryEntryView;
import com.dogus.vacation.application.common.view.LeaveTypeRef;
import com.dogus.vacation.application.common.view.LeaveTypeView;
import com.dogus.vacation.application.usecase.balance.accrual.RunAnnualAccrualResponse;
import com.dogus.vacation.application.usecase.balance.employee.GetEmployeeBalancesResponse;
import com.dogus.vacation.application.usecase.balance.mine.GetMyBalancesResponse;
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
 * Uygulamanın ürettiği OpenAPI belgesini ({@code /v3/api-docs}) okuyup bakiye, izin türü ve
 * geçmiş uç noktalarının Swagger açıklamalarını denetler.
 *
 * <p>Asıl amaç örneklerin koddan kopmamasıdır: her örneğin alanları ilgili record'un
 * alanlarıyla birebir karşılaştırılır. Bir Response'a alan eklenir de {@link ApiExamples}
 * güncellenmezse bu test kırılır. Örnek geçerli bir JSON değilse de kırılır.
 */
class OpenApiDocumentationTest extends AbstractIntegrationTest {

    /** Belgelenen uç nokta: yol, HTTP metodu, başarı kodu ve cevap record'u. */
    record Endpoint(String path, String method, String successCode, Class<? extends Record> response) {

        @Override
        public String toString() {
            return method.toUpperCase() + " " + path;
        }
    }

    static Stream<Endpoint> endpoints() {
        return Stream.of(
                new Endpoint("/api/leave-types", "get", "200", ListLeaveTypesResponse.class),
                new Endpoint("/api/leave-types", "post", "201", CreateLeaveTypeResponse.class),
                new Endpoint("/api/leave-types/{id}", "put", "200", UpdateLeaveTypeResponse.class),
                new Endpoint("/api/leave-requests/history", "get", "200", SearchLeaveHistoryResponse.class),
                new Endpoint("/api/leave-balances/mine", "get", "200", GetMyBalancesResponse.class),
                new Endpoint("/api/employees/{id}/leave-balances", "get", "200", GetEmployeeBalancesResponse.class),
                new Endpoint("/api/leave-balances/accruals", "post", "201", RunAnnualAccrualResponse.class));
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
    }

    @Test
    void requestBodyExamplesHaveTheFieldsOfTheRequests() {
        assertThat(requestBodyExample("/api/leave-types", "post").keySet())
                .containsExactlyInAnyOrderElementsOf(fieldsOf(CreateLeaveTypeRequest.class));
        // id yoldan gelir; gövdedeki id yok sayıldığı için örnekte yok.
        assertThat(requestBodyExample("/api/leave-types/{id}", "put").keySet())
                .containsExactlyInAnyOrderElementsOf(
                        fieldsOf(UpdateLeaveTypeRequest.class).stream().filter(name -> !name.equals("id")).toList());
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
