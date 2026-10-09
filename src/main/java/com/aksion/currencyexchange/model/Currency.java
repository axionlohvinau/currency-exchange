package com.aksion.currencyexchange.model;

public record Currency(
        int id,
        String code,
        String name,
        String sign
) {
}
