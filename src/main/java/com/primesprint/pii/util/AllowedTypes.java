package com.primesprint.pii.util;

import java.util.Set;

public final class AllowedTypes {
    private static final Set<String> ALLOWED = Set.of(
            "PERSON",
            "ORGANIZATION",
            "CASE_ID",
            "ADDRESS",
            "EMAIL",
            "PHONE",
            "NATIONAL_ID",
            "PASSPORT",
            "DRIVER_LICENSE",
            "BANK_ACCOUNT",
            "CARD_NUMBER",
            "CONTRACT_REF",
            "EVIDENCE_ID",
            "AMOUNT"
    );

    private AllowedTypes() {
    }

    public static boolean isAllowed(String type) {
        return type != null && ALLOWED.contains(type);
    }
}

