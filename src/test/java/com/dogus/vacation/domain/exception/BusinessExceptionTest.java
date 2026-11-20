package com.dogus.vacation.domain.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/** {@link BusinessException} ve {@link ErrorCode} için birim testleri. */
class BusinessExceptionTest {

    @Test
    void usesDefaultMessageOfCode() {
        BusinessException exception = new BusinessException(ErrorCode.LEAVE_REQUEST_NOT_FOUND);

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.LEAVE_REQUEST_NOT_FOUND);
        assertThat(exception.getMessage()).isEqualTo("İzin talebi bulunamadı.");
    }

    @Test
    void usesGivenDetailedMessage() {
        BusinessException exception = new BusinessException(
                ErrorCode.INSUFFICIENT_BALANCE, "Yetersiz izin bakiyesi: talep 5 gün, kalan 3 gün.");

        assertThat(exception.getMessage()).isEqualTo("Yetersiz izin bakiyesi: talep 5 gün, kalan 3 gün.");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void fallsBackToDefaultMessageWhenMessageIsEmpty(String message) {
        BusinessException exception = new BusinessException(ErrorCode.DUPLICATE_VALUE, message);

        assertThat(exception.getMessage()).isEqualTo(ErrorCode.DUPLICATE_VALUE.defaultMessage());
    }

    @Test
    void rejectsMissingCode() {
        assertThatThrownBy(() -> new BusinessException(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void isRuntimeExceptionSoTransactionsRollBack() {
        assertThat(new BusinessException(ErrorCode.INSUFFICIENT_BALANCE)).isInstanceOf(RuntimeException.class);
    }

    @ParameterizedTest
    @EnumSource(ErrorCode.class)
    void everyCodeHasCategoryAndTurkishSentence(ErrorCode code) {
        assertThat(code.category()).isNotNull();
        assertThat(code.defaultMessage()).isNotBlank().endsWith(".");
    }

    @Test
    void notFoundExceptionAcceptsOnlyNotFoundCodes() {
        NotFoundException exception = new NotFoundException(ErrorCode.LEAVE_REQUEST_NOT_FOUND);

        assertThat(exception.getErrorCode().category()).isEqualTo(ErrorCategory.NOT_FOUND);
        assertThat(exception.getMessage()).isEqualTo("İzin talebi bulunamadı.");
        assertThatThrownBy(() -> new NotFoundException(ErrorCode.INSUFFICIENT_BALANCE))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void forbiddenActionExceptionAcceptsOnlyForbiddenCodes() {
        assertThat(new ForbiddenActionException().getErrorCode()).isEqualTo(ErrorCode.ACTION_NOT_ALLOWED);
        assertThat(new ForbiddenActionException(ErrorCode.SELF_APPROVAL_NOT_ALLOWED).getErrorCode())
                .isEqualTo(ErrorCode.SELF_APPROVAL_NOT_ALLOWED);
        assertThatThrownBy(() -> new ForbiddenActionException(ErrorCode.EMPLOYEE_NOT_FOUND))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void stateAndBalanceExceptionsUseTheirFixedCodes() {
        InvalidStateTransitionException transition =
                new InvalidStateTransitionException("Reddedilmiş talep onaylanamaz.");
        InsufficientBalanceException balance =
                new InsufficientBalanceException("Yetersiz izin bakiyesi: talep 5 gün, kalan 3 gün.");

        assertThat(transition.getErrorCode()).isEqualTo(ErrorCode.INVALID_STATUS_TRANSITION);
        assertThat(transition.getMessage()).isEqualTo("Reddedilmiş talep onaylanamaz.");
        assertThat(balance.getErrorCode()).isEqualTo(ErrorCode.INSUFFICIENT_BALANCE);
        assertThat(balance).isInstanceOf(BusinessException.class);
    }
}
