package com.fy20047.susan.service;

import java.util.Set;

public final class SheetNameNormalizer {

    private static final Set<String> ILLEGAL_CHARS = Set.of("/", "\\", "?", "*", "[", "]", ":");

    private SheetNameNormalizer() {
    }

    public static String normalize(String rawName) {
        if (rawName == null) {
            return "";
        }

        String trimmed = rawName.trim();
        if (trimmed.isEmpty()) {
            return "";
        }

        String normalized = trimmed;
        for (String illegalChar : ILLEGAL_CHARS) {
            normalized = normalized.replace(illegalChar, "");
        }
        return normalized.trim();
    }
}
