package com.dtech.auth.enums;

public enum MimeType {
    PNG("image/png"),
    JPEG("image/jpeg"),
    JPG("image/jpg"),
    PDF("image/pdf");

    private final String code;

    MimeType(String code) {
        this.code = code;
    }
    public String getCode() {
        return code;
    }
}
