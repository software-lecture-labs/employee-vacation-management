package com.dogus.vacation.application.mediator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;

/**
 * {@link SpringMediator} için birim testleri. Spring context açılmaz; Handler ve
 * Validator'lar elle verilir, Jakarta anotasyonlarını gerçek Hibernate Validator çalıştırır.
 */
class SpringMediatorTest {

    private static ValidatorFactory validatorFactory;
    private static Validator beanValidator;

    private GreetHandler handler;
    private GreetValidator validator;
    private SpringMediator mediator;

    @BeforeAll
    static void createBeanValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        beanValidator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeBeanValidator() {
        validatorFactory.close();
    }

    @BeforeEach
    void setUp() {
        handler = new GreetHandler();
        validator = new GreetValidator();
        mediator = new SpringMediator(List.of(handler), List.of(validator), beanValidator);
    }

    @Test
    void sendsValidRequestToItsHandler() {
        String response = mediator.send(new GreetRequest("Ayşe", "Bey"));

        assertThat(response).isEqualTo("Merhaba Ayşe");
        assertThat(handler.calls).isEqualTo(1);
    }

    @Test
    void annotationErrorsStopRequestBeforeHandler() {
        GreetRequest request = new GreetRequest(" ", "Bey");

        assertThatThrownBy(() -> mediator.send(request))
                .isInstanceOf(ValidationException.class)
                .extracting(e -> ((ValidationException) e).getErrors())
                .isEqualTo(List.of(new ValidationError("name", "Ad boş olamaz.")));
        assertThat(handler.calls).isZero();
    }

    @Test
    void customValidatorIsSkippedWhenAnnotationsFail() {
        GreetRequest request = new GreetRequest("", "Bey");

        assertThatThrownBy(() -> mediator.send(request)).isInstanceOf(ValidationException.class);
        assertThat(validator.calls).isZero();
    }

    @Test
    void customValidatorErrorsStopRequestBeforeHandler() {
        GreetRequest request = new GreetRequest("Ayşe", "Ayşe");

        assertThatThrownBy(() -> mediator.send(request))
                .isInstanceOf(ValidationException.class)
                .extracting(e -> ((ValidationException) e).getErrors())
                .isEqualTo(List.of(new ValidationError("nickname", "Takma ad, adla aynı olamaz.")));
        assertThat(validator.calls).isEqualTo(1);
        assertThat(handler.calls).isZero();
    }

    @Test
    void annotationErrorsAreSortedByFieldName() {
        GreetRequest request = new GreetRequest(" ", "Çok uzun bir takma ad");

        assertThatThrownBy(() -> mediator.send(request))
                .isInstanceOf(ValidationException.class)
                .extracting(e -> ((ValidationException) e).getErrors())
                .isEqualTo(List.of(
                        new ValidationError("name", "Ad boş olamaz."),
                        new ValidationError("nickname", "Takma ad en fazla 10 karakter olabilir.")));
    }

    @Test
    void worksWithoutCustomValidator() {
        SpringMediator withoutValidator = new SpringMediator(List.of(handler), List.of(), beanValidator);

        assertThat(withoutValidator.send(new GreetRequest("Ayşe", "Ayşe"))).isEqualTo("Merhaba Ayşe");
    }

    @Test
    void findsRequestTypeOfProxiedHandler() {
        ProxyFactory proxyFactory = new ProxyFactory(handler);
        proxyFactory.setProxyTargetClass(true);
        GreetHandler proxy = (GreetHandler) proxyFactory.getProxy();
        SpringMediator proxied = new SpringMediator(List.of(proxy), List.of(), beanValidator);

        assertThat(proxied.send(new GreetRequest("Ayşe", null))).isEqualTo("Merhaba Ayşe");
        assertThat(handler.calls).isEqualTo(1);
    }

    @Test
    void failsWhenRequestHasNoHandler() {
        SpringMediator empty = new SpringMediator(List.of(), List.of(), beanValidator);
        GreetRequest request = new GreetRequest("Ayşe", null);

        assertThatThrownBy(() -> empty.send(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("GreetRequest");
    }

    @Test
    void rejectsTwoHandlersForSameRequest() {
        List<RequestHandler<?, ?>> twoHandlers = List.of(new GreetHandler(), new GreetHandler());
        List<RequestValidator<?>> noValidators = List.of();

        assertThatThrownBy(() -> new SpringMediator(twoHandlers, noValidators, beanValidator))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("iki Handler");
    }

    @Test
    void rejectsMissingRequest() {
        assertThatThrownBy(() -> mediator.send(null)).isInstanceOf(NullPointerException.class);
    }

    // ---- Testte kullanılan örnek use case ----

    record GreetRequest(
            @NotBlank(message = "Ad boş olamaz.") String name,
            @Size(max = 10, message = "Takma ad en fazla 10 karakter olabilir.") String nickname)
            implements Request<String> {
    }

    static class GreetHandler implements RequestHandler<GreetRequest, String> {
        int calls;

        @Override
        public String handle(GreetRequest request) {
            calls++;
            return "Merhaba " + request.name();
        }
    }

    static class GreetValidator implements RequestValidator<GreetRequest> {
        int calls;

        @Override
        public List<ValidationError> validate(GreetRequest request) {
            calls++;
            if (request.name().equals(request.nickname())) {
                return List.of(new ValidationError("nickname", "Takma ad, adla aynı olamaz."));
            }
            return List.of();
        }
    }
}
