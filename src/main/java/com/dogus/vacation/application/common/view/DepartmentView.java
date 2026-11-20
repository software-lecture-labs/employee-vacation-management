package com.dogus.vacation.application.common.view;

/**
 * Bir departmanın dışarı verilen hali (departman listesi, ekleme ve güncelleme cevapları).
 *
 * @param id   departman id'si
 * @param name departman adı; şirkette tekil
 */
public record DepartmentView(Long id, String name) {
}
