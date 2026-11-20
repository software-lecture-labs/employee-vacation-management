package com.dogus.vacation.application.usecase.leavetype.list;

import com.dogus.vacation.application.mediator.Request;

/**
 * Tüm izin türlerini listeleme isteği (FR-12, FR-13). Alanı yoktur; pasif türler de
 * listelenir, böylece İK onları görüp yeniden aktif yapabilir.
 */
public record ListLeaveTypesRequest() implements Request<ListLeaveTypesResponse> {
}
