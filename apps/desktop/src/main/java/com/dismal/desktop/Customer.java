package com.dismal.desktop;

public record Customer(
        String id,
        String firstname,
        String lastname,
        String phone,
        String email,
        boolean hasCredit,
        double creditLimit,
        Integer creditDays
) {}
