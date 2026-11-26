package com.dogus.vacation.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import com.dogus.vacation.application.mediator.Request;
import com.dogus.vacation.application.mediator.RequestHandler;
import com.dogus.vacation.application.mediator.RequestValidator;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.ResolvableType;
import org.springframework.core.type.filter.AssignableTypeFilter;

/**
 * Use case yapısı kuralını (plan 4.2) her derlemede denetleyen mimari testi.
 *
 * <p>Kural: {@code application.usecase.<alan>.<işlem>} paketindeki her {@code XxxRequest}
 * için aynı pakette dört dosya bulunur ve birbirine bağlıdır:
 * <ul>
 *   <li>{@code XxxRequest implements Request<XxxResponse>}</li>
 *   <li>{@code XxxResponse}: cevap (entity değil; ortak bir view'ı ya da sayfayı sarar)</li>
 *   <li>{@code XxxHandler implements RequestHandler<XxxRequest, XxxResponse>}</li>
 *   <li>{@code XxxValidator implements RequestValidator<XxxRequest>}: alanlar arası kural
 *       yoksa boş liste döner</li>
 * </ul>
 * Kurala uymayan yeni bir use case eklenirse bu test kırılır ve PR CI'da yeşil olmaz.
 * Spring context ve veritabanı açılmaz; sınıflar classpath taramasıyla bulunur.
 */
class UseCaseStructureTest {

    private static final String USE_CASE_PACKAGE = "com.dogus.vacation.application.usecase";

    /**
     * Handler'ı henüz yazılmamış, üzerinde çalışılan use case'ler. Handler'ı eklenince
     * buradan silinir; liste boşalınca kural istisnasız uygulanır.
     */
    private static final Set<String> HANDLER_IN_PROGRESS = Set.of("SubmitLeave");

    @Test
    void findsTheUseCases() {
        assertThat(requestTypes()).as("use case taraması boş döndü").isNotEmpty();
    }

    @TestFactory
    Stream<DynamicTest> everyUseCaseHasFourConnectedFiles() {
        return requestTypes().stream().map(request -> DynamicTest.dynamicTest(
                useCaseName(request), () -> assertUseCaseStructure(request)));
    }

    private static void assertUseCaseStructure(Class<?> request) throws ClassNotFoundException {
        String name = useCaseName(request);
        String packageName = request.getPackageName();

        assertThat(packageName.substring(USE_CASE_PACKAGE.length() + 1).split("\\."))
                .as("%s paketi application.usecase.<alan>.<işlem> biçiminde olmalı", name)
                .hasSize(2);

        Class<?> response = Class.forName(packageName + "." + name + "Response");
        assertThat(ResolvableType.forClass(request).as(Request.class).resolveGeneric(0))
                .as("%sRequest, Request<%sResponse> olmalı", name, name)
                .isEqualTo(response);

        Class<?> validator = Class.forName(packageName + "." + name + "Validator");
        assertThat(ResolvableType.forClass(validator).as(RequestValidator.class).resolveGeneric(0))
                .as("%sValidator, RequestValidator<%sRequest> olmalı", name, name)
                .isEqualTo(request);

        if (HANDLER_IN_PROGRESS.contains(name)) {
            return;
        }
        Class<?> handler = Class.forName(packageName + "." + name + "Handler");
        ResolvableType handlerType = ResolvableType.forClass(handler).as(RequestHandler.class);
        assertThat(handlerType.resolveGeneric(0))
                .as("%sHandler'ın isteği %sRequest olmalı", name, name)
                .isEqualTo(request);
        assertThat(handlerType.resolveGeneric(1))
                .as("%sHandler'ın cevabı %sResponse olmalı", name, name)
                .isEqualTo(response);
    }

    private static List<Class<?>> requestTypes() {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AssignableTypeFilter(Request.class));
        return scanner.findCandidateComponents(USE_CASE_PACKAGE).stream()
                .map(BeanDefinition::getBeanClassName)
                .map(UseCaseStructureTest::load)
                .sorted(Comparator.comparing(Class::getName))
                .toList();
    }

    private static Class<?> load(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(className + " yüklenemedi", e);
        }
    }

    private static String useCaseName(Class<?> request) {
        String simpleName = request.getSimpleName();
        assertThat(simpleName).as("İstek sınıfının adı Request ile bitmeli").endsWith("Request");
        return simpleName.substring(0, simpleName.length() - "Request".length());
    }
}
