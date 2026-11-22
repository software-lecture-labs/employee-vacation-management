package com.dogus.vacation.application.usecase.auth.profile;

import com.dogus.vacation.application.common.CurrentUserProvider;
import com.dogus.vacation.application.common.mapper.EmployeeMapper;
import com.dogus.vacation.application.mediator.RequestHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Giriş yapan çalışanın güncel bilgilerini döner (FR-11). Departman ve yönetici adı da
 * gelir; şifre hash'i dönmez.
 */
@Component
public class GetMyProfileHandler implements RequestHandler<GetMyProfileRequest, GetMyProfileResponse> {

    private final CurrentUserProvider currentUser;

    public GetMyProfileHandler(CurrentUserProvider currentUser) {
        this.currentUser = currentUser;
    }

    @Override
    @Transactional(readOnly = true)
    public GetMyProfileResponse handle(GetMyProfileRequest request) {
        return new GetMyProfileResponse(EmployeeMapper.toView(currentUser.currentEmployee()));
    }
}
