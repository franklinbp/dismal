package com.dismal.distribuciones.modules.sales.dto;

import java.time.LocalDate;
import java.util.UUID;

public record CreateInvoiceRequest(
        UUID saleId,
        LocalDate dueDate
) {}
