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

    public static boolean isCompatible(
            String configuredName,
            String actualName,
            int maxCompareLength,
            int minCompareLength
    ) {
        String left = normalize(configuredName);
        String right = normalize(actualName);
        if (left.isEmpty() || right.isEmpty()) {
            return false;
        }
        if (left.equals(right)) {
            return true;
        }

        int boundedMax = Math.max(1, maxCompareLength);
        int boundedMin = Math.max(1, minCompareLength);
        int compareLength = Math.min(Math.min(left.length(), right.length()), boundedMax);
        if (compareLength < boundedMin) {
            return false;
        }

        boolean samePrefix = left.regionMatches(0, right, 0, compareLength);
        if (!samePrefix) {
            return false;
        }

        // 只接受前綴完全一致（避免短字串誤判）
        return left.length() == compareLength || right.length() == compareLength;
    }
}
