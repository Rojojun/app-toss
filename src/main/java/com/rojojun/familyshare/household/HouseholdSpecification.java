package com.rojojun.familyshare.household;

import java.util.List;

public class HouseholdSpecification {

    private final List<HouseholdMember> householdMember;

    public HouseholdSpecification(List<HouseholdMember> householdMember) {
        this.householdMember = householdMember;
    }

    public void validate() {
        householdMember.getFirst();
    }
}
