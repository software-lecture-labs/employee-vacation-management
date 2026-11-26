package com.dogus.vacation.application.usecase.history.search;

import com.dogus.vacation.application.common.PageResponse;
import com.dogus.vacation.application.common.view.HistoryEntryView;

/**
 * Şirket izin geçmişi aramasının cevabı (FR-26).
 *
 * @param history filtreye uyan olaylar, sayfalı ve en yeni önce
 */
public record SearchLeaveHistoryResponse(PageResponse<HistoryEntryView> history) {
}
