package com.dogus.vacation.application.usecase.balance.carryover;

import com.dogus.vacation.application.mediator.Request;
import jakarta.validation.constraints.NotNull;

/**
 * Kullanılmayan yıllık izni sonraki yıla devreder (BR-12, bonus; yalnızca İK).
 *
 * <p>Hangi yılların kabul edildiği {@link RunCarryOverValidator}'dadır: devir yalnızca biten
 * yıldan bu yıla yapılır.
 *
 * @param fromYear günleri devredilecek (biten) yıl; zorunlu
 */
public record RunCarryOverRequest(
        @NotNull(message = "Devredilecek yıl zorunludur.")
        Integer fromYear)
        implements Request<RunCarryOverResponse> {
}
