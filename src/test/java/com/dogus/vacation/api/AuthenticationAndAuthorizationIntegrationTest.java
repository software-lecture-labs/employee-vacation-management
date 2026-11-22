package com.dogus.vacation.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dogus.vacation.AbstractIntegrationTest;
import com.dogus.vacation.domain.model.Department;
import com.dogus.vacation.domain.model.Employee;
import com.dogus.vacation.domain.model.enums.Role;
import com.dogus.vacation.infrastructure.persistence.DepartmentRepository;
import com.dogus.vacation.infrastructure.persistence.EmployeeRepository;
import com.dogus.vacation.support.TestData;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Güvenlik zincirini uçtan uca doğrular (FR-01–FR-04, FR-11, BR-10): gerçek filtre zinciri,
 * gerçek BCrypt, gerçek JWT ve {@code db/scripts} ile kurulan test veritabanı.
 *
 * <ul>
 *   <li>Giriş: doğru bilgiyle token; yanlış şifre ve pasif çalışan 401.</li>
 *   <li>Token yok veya bozuk: 401, ortak hata biçiminde.</li>
 *   <li>Rol yetmiyor: 403, ortak hata biçiminde; İK aynı adreste başarılı.</li>
 * </ul>
 */
class AuthenticationAndAuthorizationIntegrationTest extends AbstractIntegrationTest {

    private static final String PASSWORD = "Sifre123!";
    private static final String NEW_LEAVE_TYPE = """
            {"code":"MARRIAGE_LEAVE","name":"Evlilik izni","deductsFromBalance":true,"annualLimitDays":3}
            """;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private MockMvc mockMvc;
    private Employee hr;
    private Employee employee;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        Department it = departmentRepository.save(TestData.department());
        hr = saveWithPassword(TestData.employee(it, Role.HR));
        employee = saveWithPassword(TestData.employee(it, Role.EMPLOYEE));
    }

    // --- Giriş (FR-01, FR-02, BR-10) ---

    @Test
    void loginReturnsBearerTokenThatOpensProfile() throws Exception {
        login(hr.getEmail(), PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.expiresAt").isNotEmpty())
                .andExpect(jsonPath("$.employee.role").value("HR"));

        mockMvc.perform(get("/api/me").header(HttpHeaders.AUTHORIZATION, bearer(hr)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employee.id").value(hr.getId()))
                .andExpect(jsonPath("$.employee.email").value(hr.getEmail()))
                .andExpect(jsonPath("$.employee.departmentName").value(hr.getDepartment().getName()))
                .andExpect(jsonPath("$.employee.passwordHash").doesNotExist());
    }

    @Test
    void emailIsCaseInsensitive() throws Exception {
        login(hr.getEmail().toUpperCase(), PASSWORD).andExpect(status().isOk());
    }

    @Test
    void passwordIsStoredAsBcryptHash() {
        Employee saved = employeeRepository.findById(hr.getId()).orElseThrow();

        assertThat(saved.getPasswordHash()).startsWith("$2").doesNotContain(PASSWORD);
    }

    @Test
    void wrongPasswordIs401InCommonErrorFormat() throws Exception {
        login(hr.getEmail(), "YanlisSifre1")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.path").value("/api/auth/login"));
    }

    @Test
    void unknownEmailGetsSameAnswerAsWrongPassword() throws Exception {
        login("kimse@test.local", PASSWORD)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void inactiveEmployeeCannotSignIn() throws Exception {
        employee.setActive(false);
        employeeRepository.save(employee);

        login(employee.getEmail(), PASSWORD)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void blankEmailIs400() throws Exception {
        login("", PASSWORD)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("email"));
    }

    // --- Token (FR-03, FR-04) ---

    @Test
    void requestWithoutTokenIs401() throws Exception {
        mockMvc.perform(get("/api/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"))
                .andExpect(jsonPath("$.path").value("/api/me"));
    }

    @Test
    void tamperedTokenIs401() throws Exception {
        String tampered = bearer(employee) + "x";

        mockMvc.perform(get("/api/me").header(HttpHeaders.AUTHORIZATION, tampered))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    void apiDocumentationIsPublic() throws Exception {
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
    }

    // --- Roller (FR-03) ---

    @Test
    void signedInEmployeeCanListLeaveTypes() throws Exception {
        mockMvc.perform(get("/api/leave-types").header(HttpHeaders.AUTHORIZATION, bearer(employee)))
                .andExpect(status().isOk());
    }

    @Test
    void employeeGets403OnHrOnlyEndpoints() throws Exception {
        String token = bearer(employee);

        mockMvc.perform(post("/api/leave-types").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content(NEW_LEAVE_TYPE))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("ACTION_NOT_ALLOWED"));
        mockMvc.perform(get("/api/leave-requests/history").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACTION_NOT_ALLOWED"));

        assertThat(countRows("leave_types")).isEqualTo(4);
    }

    @Test
    void hrCanUseHrOnlyEndpoints() throws Exception {
        String token = bearer(hr);

        mockMvc.perform(post("/api/leave-types").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content(NEW_LEAVE_TYPE))
                .andExpect(status().isCreated());
        mockMvc.perform(get("/api/leave-requests/history").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk());
    }

    private Employee saveWithPassword(Employee newEmployee) {
        newEmployee.setPasswordHash(passwordEncoder.encode(PASSWORD));
        return employeeRepository.save(newEmployee);
    }

    private ResultActions login(String email, String password) throws Exception {
        String body = "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
        return mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body));
    }

    /** @return giriş yapıp aldığı token'la {@code Authorization} başlığının değeri */
    private String bearer(Employee who) throws Exception {
        String json = login(who.getEmail(), PASSWORD)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(json, "$.accessToken");
        return "Bearer " + token;
    }
}
