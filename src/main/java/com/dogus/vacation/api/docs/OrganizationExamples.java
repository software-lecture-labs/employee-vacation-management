package com.dogus.vacation.api.docs;

/**
 * Departman ve çalışan uç noktalarının Swagger örnekleri (FR-06 – FR-10).
 *
 * <p>{@link ApiExamples} ile aynı kurallar: alan adları Response record'larıyla, hata
 * mesajları kodun döndürdüğü metinlerle aynıdır; {@code OpenApiDocumentationTest} örneklerin
 * alanlarını record'larla karşılaştırır. Ortak 401 ve 403 örnekleri {@link ApiExamples}'tadır.
 */
public final class OrganizationExamples {

    private OrganizationExamples() {
    }

    public static final String DEPARTMENT_LIST = """
            {
              "departments": {
                "content": [
                  {
                    "id": 1,
                    "name": "İnsan Kaynakları"
                  },
                  {
                    "id": 3,
                    "name": "Satış"
                  },
                  {
                    "id": 2,
                    "name": "Yazılım Geliştirme"
                  }
                ],
                "page": 0,
                "size": 20,
                "totalElements": 3,
                "totalPages": 1
              }
            }
            """;

    public static final String DEPARTMENT_CREATE_REQUEST = """
            {
              "name": "Hukuk"
            }
            """;

    public static final String DEPARTMENT_CREATED = """
            {
              "department": {
                "id": 4,
                "name": "Hukuk"
              }
            }
            """;

    public static final String DEPARTMENT_UPDATE_REQUEST = """
            {
              "name": "Satış ve Pazarlama"
            }
            """;

    public static final String DEPARTMENT_UPDATED = """
            {
              "department": {
                "id": 3,
                "name": "Satış ve Pazarlama"
              }
            }
            """;

    public static final String DEPARTMENT_BLANK_NAME = """
            {
              "timestamp": "2026-11-26T10:15:30+03:00",
              "status": 400,
              "code": "VALIDATION_FAILED",
              "message": "İstek geçersiz.",
              "path": "/api/departments",
              "fieldErrors": [
                {
                  "field": "name",
                  "message": "Departman adı zorunlu."
                }
              ]
            }
            """;

    public static final String DEPARTMENT_DUPLICATE = """
            {
              "timestamp": "2026-11-26T10:15:30+03:00",
              "status": 409,
              "code": "DUPLICATE_VALUE",
              "message": "'Satış' adlı bir departman zaten var.",
              "path": "/api/departments",
              "fieldErrors": []
            }
            """;

    public static final String DEPARTMENT_NOT_FOUND = """
            {
              "timestamp": "2026-11-26T10:15:30+03:00",
              "status": 404,
              "code": "DEPARTMENT_NOT_FOUND",
              "message": "Departman bulunamadı.",
              "path": "/api/departments/99",
              "fieldErrors": []
            }
            """;

    public static final String DEPARTMENT_INVALID_PAGE = """
            {
              "timestamp": "2026-11-26T10:15:30+03:00",
              "status": 400,
              "code": "VALIDATION_FAILED",
              "message": "İstek geçersiz.",
              "path": "/api/departments",
              "fieldErrors": [
                {
                  "field": "size",
                  "message": "Sayfa büyüklüğü en fazla 100 olabilir."
                }
              ]
            }
            """;

    public static final String EMPLOYEE_LIST = """
            {
              "employees": {
                "content": [
                  {
                    "id": 3,
                    "firstName": "Ahmet",
                    "lastName": "Yılmaz",
                    "email": "ahmet.yilmaz@demo.local",
                    "role": "MANAGER",
                    "active": true,
                    "hireDate": "2010-03-15",
                    "birthDate": "1981-01-20",
                    "departmentId": 2,
                    "departmentName": "Yazılım Geliştirme",
                    "manager": null
                  },
                  {
                    "id": 5,
                    "firstName": "Ayşe",
                    "lastName": "Demir",
                    "email": "ayse.demir@demo.local",
                    "role": "EMPLOYEE",
                    "active": true,
                    "hireDate": "2023-04-01",
                    "birthDate": "1998-02-10",
                    "departmentId": 2,
                    "departmentName": "Yazılım Geliştirme",
                    "manager": {
                      "id": 3,
                      "fullName": "Ahmet Yılmaz"
                    }
                  }
                ],
                "page": 0,
                "size": 20,
                "totalElements": 2,
                "totalPages": 1
              }
            }
            """;

    public static final String EMPLOYEE_DETAIL = """
            {
              "employee": {
                "id": 5,
                "firstName": "Ayşe",
                "lastName": "Demir",
                "email": "ayse.demir@demo.local",
                "role": "EMPLOYEE",
                "active": true,
                "hireDate": "2023-04-01",
                "birthDate": "1998-02-10",
                "departmentId": 2,
                "departmentName": "Yazılım Geliştirme",
                "manager": {
                  "id": 3,
                  "fullName": "Ahmet Yılmaz"
                }
              }
            }
            """;

    public static final String EMPLOYEE_CREATE_REQUEST = """
            {
              "firstName": "Deniz",
              "lastName": "Yıldız",
              "email": "deniz.yildiz@demo.local",
              "password": "IlkSifre1!",
              "hireDate": "2023-11-27",
              "birthDate": "1996-05-14",
              "role": "EMPLOYEE",
              "departmentId": 2,
              "managerId": 3
            }
            """;

    public static final String EMPLOYEE_CREATED = """
            {
              "employee": {
                "id": 11,
                "firstName": "Deniz",
                "lastName": "Yıldız",
                "email": "deniz.yildiz@demo.local",
                "role": "EMPLOYEE",
                "active": true,
                "hireDate": "2023-11-27",
                "birthDate": "1996-05-14",
                "departmentId": 2,
                "departmentName": "Yazılım Geliştirme",
                "manager": {
                  "id": 3,
                  "fullName": "Ahmet Yılmaz"
                }
              },
              "balances": [
                {
                  "leaveType": {
                    "id": 1,
                    "code": "ANNUAL",
                    "name": "Yıllık izin"
                  },
                  "year": 2026,
                  "entitledDays": 14.0,
                  "usedDays": 0.0,
                  "remainingDays": 14.0
                },
                {
                  "leaveType": {
                    "id": 2,
                    "code": "EXCUSE",
                    "name": "Mazeret izni"
                  },
                  "year": 2026,
                  "entitledDays": 5.0,
                  "usedDays": 0.0,
                  "remainingDays": 5.0
                }
              ]
            }
            """;

    public static final String EMPLOYEE_UPDATE_REQUEST = """
            {
              "firstName": "Ayşe",
              "lastName": "Demir",
              "email": "ayse.demir@demo.local",
              "hireDate": "2023-04-01",
              "birthDate": "1998-02-10",
              "role": "MANAGER",
              "departmentId": 2,
              "managerId": 3
            }
            """;

    public static final String EMPLOYEE_UPDATED = """
            {
              "employee": {
                "id": 5,
                "firstName": "Ayşe",
                "lastName": "Demir",
                "email": "ayse.demir@demo.local",
                "role": "MANAGER",
                "active": true,
                "hireDate": "2023-04-01",
                "birthDate": "1998-02-10",
                "departmentId": 2,
                "departmentName": "Yazılım Geliştirme",
                "manager": {
                  "id": 3,
                  "fullName": "Ahmet Yılmaz"
                }
              }
            }
            """;

    public static final String EMPLOYEE_DEACTIVATED = """
            {
              "employee": {
                "id": 5,
                "firstName": "Ayşe",
                "lastName": "Demir",
                "email": "ayse.demir@demo.local",
                "role": "EMPLOYEE",
                "active": false,
                "hireDate": "2023-04-01",
                "birthDate": "1998-02-10",
                "departmentId": 2,
                "departmentName": "Yazılım Geliştirme",
                "manager": {
                  "id": 3,
                  "fullName": "Ahmet Yılmaz"
                }
              }
            }
            """;

    public static final String EMPLOYEE_INVALID_DATES = """
            {
              "timestamp": "2026-11-26T10:15:30+03:00",
              "status": 400,
              "code": "VALIDATION_FAILED",
              "message": "İstek geçersiz.",
              "path": "/api/employees",
              "fieldErrors": [
                {
                  "field": "birthDate",
                  "message": "Çalışan en az 15 yaşında olmalı."
                },
                {
                  "field": "hireDate",
                  "message": "İşe giriş tarihi gelecekte olamaz."
                }
              ]
            }
            """;

    public static final String EMPLOYEE_DUPLICATE_EMAIL = """
            {
              "timestamp": "2026-11-26T10:15:30+03:00",
              "status": 409,
              "code": "DUPLICATE_VALUE",
              "message": "'ayse.demir@demo.local' e-postası başka bir çalışanda kayıtlı.",
              "path": "/api/employees",
              "fieldErrors": []
            }
            """;

    public static final String EMPLOYEE_NOT_FOUND = """
            {
              "timestamp": "2026-11-26T10:15:30+03:00",
              "status": 404,
              "code": "EMPLOYEE_NOT_FOUND",
              "message": "Çalışan bulunamadı.",
              "path": "/api/employees/99",
              "fieldErrors": []
            }
            """;

    public static final String EMPLOYEE_DEPARTMENT_NOT_FOUND = """
            {
              "timestamp": "2026-11-26T10:15:30+03:00",
              "status": 404,
              "code": "DEPARTMENT_NOT_FOUND",
              "message": "Departman bulunamadı.",
              "path": "/api/employees",
              "fieldErrors": []
            }
            """;

    public static final String EMPLOYEE_LAST_HR = """
            {
              "timestamp": "2026-11-26T10:15:30+03:00",
              "status": 422,
              "code": "MINIMUM_ACTIVE_HR_REQUIRED",
              "message": "Şirkette en az 2 aktif İK çalışanı bulunmalı.",
              "path": "/api/employees/1/deactivate",
              "fieldErrors": []
            }
            """;

    public static final String EMPLOYEE_MANAGER_CYCLE = """
            {
              "timestamp": "2026-11-26T10:15:30+03:00",
              "status": 422,
              "code": "MANAGER_CYCLE",
              "message": "Yönetici zinciri döngü oluşturamaz; çalışan kendi ekibindeki birine bağlanamaz.",
              "path": "/api/employees/3",
              "fieldErrors": []
            }
            """;

    public static final String EMPLOYEE_OWN_MANAGER = """
            {
              "timestamp": "2026-11-26T10:15:30+03:00",
              "status": 400,
              "code": "VALIDATION_FAILED",
              "message": "İstek geçersiz.",
              "path": "/api/employees/5",
              "fieldErrors": [
                {
                  "field": "managerId",
                  "message": "Çalışan kendi yöneticisi olamaz."
                }
              ]
            }
            """;

    public static final String EMPLOYEE_INVALID_FILTER = """
            {
              "timestamp": "2026-11-26T10:15:30+03:00",
              "status": 400,
              "code": "VALIDATION_FAILED",
              "message": "İstek geçersiz.",
              "path": "/api/employees",
              "fieldErrors": [
                {
                  "field": "size",
                  "message": "Sayfa büyüklüğü en az 1 olmalı."
                }
              ]
            }
            """;
}
