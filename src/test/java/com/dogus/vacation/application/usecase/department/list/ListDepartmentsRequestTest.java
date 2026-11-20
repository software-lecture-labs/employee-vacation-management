package com.dogus.vacation.application.usecase.department.list;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Sayfalama varsayılanları; Spring gerekmez. */
class ListDepartmentsRequestTest {

    @Test
    void usesTheFirstPageOfTwentyWhenPagingIsNotGiven() {
        ListDepartmentsRequest request = new ListDepartmentsRequest(null, null);

        assertThat(request.page()).isZero();
        assertThat(request.size()).isEqualTo(ListDepartmentsRequest.DEFAULT_SIZE).isEqualTo(20);
    }

    @Test
    void keepsTheGivenPaging() {
        ListDepartmentsRequest request = new ListDepartmentsRequest(3, 50);

        assertThat(request.page()).isEqualTo(3);
        assertThat(request.size()).isEqualTo(50);
    }
}
