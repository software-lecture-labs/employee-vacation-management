package com.dogus.vacation.application.usecase.auth.profile;

import com.dogus.vacation.application.mediator.Request;

/**
 * Giriş yapan çalışanın kendi profili (FR-11). Alan yoktur: kim olduğu token'dan gelir.
 */
public record GetMyProfileRequest() implements Request<GetMyProfileResponse> {
}
