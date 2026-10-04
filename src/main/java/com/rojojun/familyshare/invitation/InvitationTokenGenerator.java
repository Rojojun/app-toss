package com.rojojun.familyshare.invitation;

public interface InvitationTokenGenerator {

    IssuedToken generate();

    String hash(String rawToken);

    record IssuedToken(String rawToken, String tokenHash) {}
}
