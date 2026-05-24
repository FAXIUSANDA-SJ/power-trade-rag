package com.powertrade.common.constant;

public enum DocumentType {
    POLICY("政策文件"),
    RULE("规则文件"),
    CASE("案例文件"),
    REPORT("报告文件"),
    OTHER("其他文件");

    private final String description;

    DocumentType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}