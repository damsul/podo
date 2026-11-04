package com.podo.domain.cs.entity;

public enum FaqCategory {
    ACCOUNT("회원/계정"),
    PAYROOM("더치페이 방"),
    PAYMENT("결제/정산"),
    ETC("기타");

    private final String label;

    FaqCategory(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}