package com.dogus.vacation.application.mediator;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import jakarta.validation.Validator;

import org.springframework.core.ResolvableType;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

/**
 * {@link Mediator}'un Spring uygulaması.
 *
 * <p>Uygulama açılırken bütün {@link RequestHandler} ve {@link RequestValidator}
 * bean'lerini işledikleri istek tipine göre eşler. {@link #send(Request)} çağrısında
 * sırasıyla şunları yapar:
 * <ol>
 *   <li>İstek record'undaki Jakarta anotasyonlarını ({@code @NotNull}, {@code @Size}...)
 *       çalıştırır.</li>
 *   <li>Anotasyonlar geçtiyse isteğin {@code XxxValidator}'ını çalıştırır. Böylece
 *       Validator'lar boş alan kontrolü yapmak zorunda kalmaz.</li>
 *   <li>Hata varsa {@link ValidationException} atar ve Handler hiç çağrılmaz;
 *       yoksa isteğin Handler'ını çağırıp cevabını döner.</li>
 * </ol>
 *
 * <p>Yeni bir use case eklemek bu sınıfı değiştirmez: yeni Handler ve Validator
 * bean'leri açılışta kendiliğinden bulunur (Open/Closed).
 */
@Component
public class SpringMediator implements Mediator {

    private static final Comparator<ValidationError> ERROR_ORDER =
            Comparator.comparing(ValidationError::field).thenComparing(ValidationError::message);

    private final Map<Class<?>, RequestHandler<?, ?>> handlers = new HashMap<>();
    private final Map<Class<?>, List<RequestValidator<?>>> validators = new HashMap<>();
    private final Validator beanValidator;

    /**
     * @param handlers      uygulamadaki bütün Handler bean'leri (hiç yoksa boş liste)
     * @param validators    uygulamadaki bütün Validator bean'leri (hiç yoksa boş liste)
     * @param beanValidator Jakarta anotasyonlarını çalıştıran doğrulayıcı
     * @throws IllegalStateException aynı istek için iki Handler varsa ya da bir sınıfın
     *                               istek tipi çözülemiyorsa
     */
    public SpringMediator(List<RequestHandler<?, ?>> handlers,
                          List<RequestValidator<?>> validators,
                          Validator beanValidator) {
        this.beanValidator = Objects.requireNonNull(beanValidator, "beanValidator boş olamaz");
        for (RequestHandler<?, ?> handler : handlers) {
            Class<?> requestType = requestTypeOf(handler, RequestHandler.class);
            RequestHandler<?, ?> existing = this.handlers.putIfAbsent(requestType, handler);
            if (existing != null) {
                throw new IllegalStateException(requestType.getSimpleName()
                        + " için iki Handler var: " + ClassUtils.getUserClass(existing).getSimpleName()
                        + " ve " + ClassUtils.getUserClass(handler).getSimpleName());
            }
        }
        for (RequestValidator<?> validator : validators) {
            Class<?> requestType = requestTypeOf(validator, RequestValidator.class);
            this.validators.computeIfAbsent(requestType, type -> new ArrayList<>()).add(validator);
        }
    }

    @Override
    public <R> R send(Request<R> request) {
        Objects.requireNonNull(request, "request boş olamaz");
        List<ValidationError> errors = validate(request);
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
        return handlerFor(request).handle(request);
    }

    private List<ValidationError> validate(Request<?> request) {
        List<ValidationError> errors = annotationErrors(request);
        if (errors.isEmpty()) {
            errors = validatorErrors(request);
        }
        return errors;
    }

    private List<ValidationError> annotationErrors(Request<?> request) {
        return beanValidator.validate(request).stream()
                .map(violation -> new ValidationError(
                        violation.getPropertyPath().toString(), violation.getMessage()))
                .sorted(ERROR_ORDER)
                .toList();
    }

    @SuppressWarnings("unchecked")
    private List<ValidationError> validatorErrors(Request<?> request) {
        List<ValidationError> errors = new ArrayList<>();
        for (RequestValidator<?> validator : validators.getOrDefault(request.getClass(), List.of())) {
            List<ValidationError> found = ((RequestValidator<Request<?>>) validator).validate(request);
            errors.addAll(Objects.requireNonNullElse(found, List.of()));
        }
        return errors;
    }

    @SuppressWarnings("unchecked")
    private <R> RequestHandler<Request<R>, R> handlerFor(Request<R> request) {
        RequestHandler<?, ?> handler = handlers.get(request.getClass());
        if (handler == null) {
            throw new IllegalStateException(
                    request.getClass().getSimpleName() + " için bir Handler bulunamadı");
        }
        return (RequestHandler<Request<R>, R>) handler;
    }

    /**
     * Bean'in hangi istek tipini işlediğini generic parametresinden bulur. Sınıf
     * {@code @Transactional} yüzünden proxy'lenmiş olsa da asıl sınıfa bakar.
     */
    private static Class<?> requestTypeOf(Object bean, Class<?> contract) {
        Class<?> beanClass = ClassUtils.getUserClass(bean);
        Class<?> requestType = ResolvableType.forClass(beanClass).as(contract).resolveGeneric(0);
        if (requestType == null) {
            throw new IllegalStateException(beanClass.getName() + " sınıfının istek tipi çözülemedi; "
                    + contract.getSimpleName() + "<XxxRequest, ...> açıkça yazılmalı");
        }
        return requestType;
    }
}
