package com.dogus.vacation.domain.model;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Resmi tatil ({@code public_holidays}); bonus FR-38.
 */
@Entity
@Table(name = "public_holidays")
public class PublicHoliday extends BaseEntity {

    @Column(name = "holiday_date", nullable = false)
    private LocalDate holidayDate;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /** JPA için; uygulama kodu kullanmaz. */
    protected PublicHoliday() {
    }

    public PublicHoliday(LocalDate holidayDate, String name) {
        this.holidayDate = holidayDate;
        this.name = name;
    }

    public LocalDate getHolidayDate() {
        return holidayDate;
    }

    public void setHolidayDate(LocalDate holidayDate) {
        this.holidayDate = holidayDate;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
