package com.dogus.vacation.api;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Departman uç noktalarını uçtan uca doğrular (FR-10): gerçek giriş ve JWT, güvenlik
 * filtreleri, mediator, Handler ve PostgreSQL birlikte çalışır. Listeleme herkese, ekleme
 * ve güncelleme yalnızca İK'ya açıktır.
 */
class DepartmentControllerIntegrationTest extends AbstractIntegrationTest {

    private static final String PASSWORD = "Sifre123!";

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private MockMvc mockMvc;
    private Department department;
    private Employee hr;
    private Employee employee;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        department = departmentRepository.save(new Department("Finans"));
        hr = saveWithPassword(TestData.employee(department, Role.HR));
        employee = saveWithPassword(TestData.employee(department, Role.EMPLOYEE));
    }

    @Test
    void anyLoggedInEmployeeCanListDepartments() throws Exception {
        mockMvc.perform(get("/api/departments").header(HttpHeaders.AUTHORIZATION, bearer(employee)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.departments.content[0].name").value("Finans"))
                .andExpect(jsonPath("$.departments.totalElements").value(1));
    }

    @Test
    void listingWithoutATokenIs401() throws Exception {
        mockMvc.perform(get("/api/departments"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    void hrCreatesADepartment() throws Exception {
        mockMvc.perform(post("/api/departments").header(HttpHeaders.AUTHORIZATION, bearer(hr))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Hukuk\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION,
                        startsWith("/api/departments/")))
                .andExpect(jsonPath("$.department.id").isNumber())
                .andExpect(jsonPath("$.department.name").value("Hukuk"));
    }

    @Test
    void anEmployeeCannotCreateADepartment() throws Exception {
        mockMvc.perform(post("/api/departments").header(HttpHeaders.AUTHORIZATION, bearer(employee))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Hukuk\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACTION_NOT_ALLOWED"));
    }

    @Test
    void creatingADuplicateNameIs409() throws Exception {
        mockMvc.perform(post("/api/departments").header(HttpHeaders.AUTHORIZATION, bearer(hr))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"fINANS\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_VALUE"));
    }

    @Test
    void creatingWithABlankNameIs400() throws Exception {
        mockMvc.perform(post("/api/departments").header(HttpHeaders.AUTHORIZATION, bearer(hr))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("name"));
    }

    @Test
    void hrRenamesADepartment() throws Exception {
        mockMvc.perform(put("/api/departments/" + department.getId()).header(HttpHeaders.AUTHORIZATION, bearer(hr))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Finans ve Muhasebe\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.department.id").value(department.getId()))
                .andExpect(jsonPath("$.department.name").value("Finans ve Muhasebe"));
    }

    @Test
    void anEmployeeCannotRenameADepartment() throws Exception {
        mockMvc.perform(put("/api/departments/" + department.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(employee))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Başka\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void renamingAMissingDepartmentIs404() throws Exception {
        mockMvc.perform(put("/api/departments/999999").header(HttpHeaders.AUTHORIZATION, bearer(hr))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Yok\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("DEPARTMENT_NOT_FOUND"));
    }

    private Employee saveWithPassword(Employee newEmployee) {
        newEmployee.setPasswordHash(passwordEncoder.encode(PASSWORD));
        return employeeRepository.save(newEmployee);
    }

    private String bearer(Employee who) throws Exception {
        String body = "{\"email\":\"" + who.getEmail() + "\",\"password\":\"" + PASSWORD + "\"}";
        String json = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(json, "$.accessToken");
    }
}
