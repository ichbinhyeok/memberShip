package org.example.membership.common.enums;

public enum CategoryType {
    ELECTRONICS("가전"),
    FOOD("음식"),
    STATIONERY("문구"),
    DAILY("생활"),
    CLOTHING("의복");

    private final String label;

    CategoryType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}