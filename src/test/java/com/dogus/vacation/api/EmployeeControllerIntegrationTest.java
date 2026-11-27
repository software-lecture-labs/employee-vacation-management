package com.dogus.vacation.api;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
 * Çalışan listesi ve detayı uç noktalarını uçtan uca doğrular (FR-09): gerçek giriş ve JWT,
 * güvenlik filtreleri, mediator, Handler ve PostgreSQL birlikte çalışır. İkisi de yalnızca İK'ya
 * açıktır; yönetici ve çalışan 403 alır.
 */
class EmployeeControllerIntegrationTest extends AbstractIntegrationTest {

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
    private Employee manager;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        department = departmentRepository.save(TestData.department());
        hr = saveWithPassword(TestData.employee(department, Role.HR));
        manager = saveWithPassword(TestData.employee(department, Role.MANAGER));
    }

    @Test
    void hrListsEmployeesWithFilters() throws Exception {
        mockMvc.perform(get("/api/employees").param("departmentId", department.getId().toString())
                        .param("active", "true").header(HttpHeaders.AUTHORIZATION, bearer(hr)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employees.totalElements").value(2))
                .andExpect(jsonPath("$.employees.content[0].departmentName").value(department.getName()))
                .andExpect(jsonPath("$.employees.content[0].passwordHash").doesNotExist());
    }

    @Test
    void aManagerCannotListEmployees() throws Exception {
        mockMvc.perform(get("/api/employees").header(HttpHeaders.AUTHORIZATION, bearer(manager)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACTION_NOT_ALLOWED"));
    }

    @Test
    void listingWithoutATokenIs401() throws Exception {
        mockMvc.perform(get("/api/employees"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidPagingIs400() throws Exception {
        mockMvc.perform(get("/api/employees").param("size", "0").header(HttpHeaders.AUTHORIZATION, bearer(hr)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("size"));
    }

    @Test
    void hrSeesAnEmployee() throws Exception {
        mockMvc.perform(get("/api/employees/" + manager.getId()).header(HttpHeaders.AUTHORIZATION, bearer(hr)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employee.id").value(manager.getId()))
                .andExpect(jsonPath("$.employee.role").value("MANAGER"))
                .andExpect(jsonPath("$.employee.passwordHash").doesNotExist());
    }

    @Test
    void aManagerCannotSeeAnEmployeeDetail() throws Exception {
        mockMvc.perform(get("/api/employees/" + hr.getId()).header(HttpHeaders.AUTHORIZATION, bearer(manager)))
                .andExpect(status().isForbidden());
    }

    @Test
    void aMissingEmployeeIs404() throws Exception {
        mockMvc.perform(get("/api/employees/999999").header(HttpHeaders.AUTHORIZATION, bearer(hr)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("EMPLOYEE_NOT_FOUND"));
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
