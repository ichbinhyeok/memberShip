package org.example.membership.common.enums;

public enum CategoryType {
    FRESH_FOOD("신선식품"),
    BEVERAGE("음료"),
    SNACK("간식"),
    BEAUTY("뷰티"),
    ELECTRONICS("가전"),
    HOUSEHOLD("생활용품"),
    BABY("유아"),
    PET("반려동물");

    private final String label;

    CategoryType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}