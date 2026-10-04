package com.rojojun.familyshare.invitation;

import com.rojojun.familyshare.common.ApiException;
import com.rojojun.familyshare.common.ErrorCode;

public class InvitationNotFoundException extends ApiException {

    public InvitationNotFoundException() {
        super(ErrorCode.INVITATION_NOT_FOUND, "초대를 찾을 수 없습니다.");
    }
}
