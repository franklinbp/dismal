package com.dismal.distribuciones.modules.reports.controller;

import com.dismal.distribuciones.modules.integrations.notifications.service.NotificationTemplateService;
import com.dismal.distribuciones.modules.reports.dto.ColdStockProductDTO;
import com.dismal.distribuciones.modules.reports.dto.InactiveCustomerDTO;
import com.dismal.distribuciones.modules.reports.dto.PriceListColumnSelection;
import com.dismal.distribuciones.modules.reports.dto.PriceListResponse;
import com.dismal.distribuciones.modules.reports.dto.PriceListSendRequest;
import com.dismal.distribuciones.modules.reports.dto.PriceListSendResponse;
import com.dismal.distribuciones.modules.reports.dto.ProfitReportDTO;
import com.dismal.distribuciones.modules.reports.dto.SalesPeriodReportResponse;
import com.dismal.distribuciones.modules.reports.dto.SalesReportDTO;
import com.dismal.distribuciones.modules.reports.dto.SalesVsTargetReportResponse;
import com.dismal.distribuciones.modules.reports.service.PriceListEmailService;
import com.dismal.distribuciones.modules.reports.service.PriceListService;
import com.dismal.distribuciones.modules.reports.service.PriceListWhatsappService;
import com.dismal.distribuciones.modules.reports.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;
    private final PriceListService priceListService;
    private final PriceListEmailService priceListEmailService;
    private final PriceListWhatsappService priceListWhatsappService;
    private final NotificationTemplateService notificationTemplateService;

    @GetMapping("/sales")
    public ResponseEntity<SalesReportDTO> getSalesReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "month") String period) {
        Authentication authentication = getAuthentication();
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        if (!isAdminOrManager(authentication)) {
            return ResponseEntity.status(403).build();
        }
        SalesReportDTO report = reportService.generateSalesReport(startDate, endDate, period);
        return ResponseEntity.ok(report);
    }

    @GetMapping("/sales/daily")
    public ResponseEntity<SalesPeriodReportResponse> getDailySales(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        Authentication authentication = getAuthentication();
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        if (!isAdminOrManager(authentication)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(reportService.generateSalesPeriodReport(from, to, "day"));
    }

    @GetMapping("/sales/weekly")
    public ResponseEntity<SalesPeriodReportResponse> getWeeklySales(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        Authentication authentication = getAuthentication();
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        if (!isAdminOrManager(authentication)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(reportService.generateSalesPeriodReport(from, to, "week"));
    }

    @GetMapping("/sales/monthly")
    public ResponseEntity<SalesPeriodReportResponse> getMonthlySales(
            @RequestParam(required = false) Integer year
    ) {
        Authentication authentication = getAuthentication();
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        if (!isAdminOrManager(authentication)) {
            return ResponseEntity.status(403).build();
        }
        int resolvedYear = year != null ? year : LocalDate.now().getYear();
        return ResponseEntity.ok(reportService.generateMonthlyReport(resolvedYear));
    }

    @GetMapping("/sales/vs-targets")
    public ResponseEntity<SalesVsTargetReportResponse> getSalesVsTargets(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        Authentication authentication = getAuthentication();
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        if (!isAdminOrManager(authentication)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(reportService.generateSalesVsTargets(from, to));
    }

    @GetMapping("/profit")
    public ResponseEntity<ProfitReportDTO> getProfitReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        Authentication authentication = getAuthentication();
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        if (!isAdminOrManager(authentication)) {
            return ResponseEntity.status(403).build();
        }
        ProfitReportDTO report = reportService.generateProfitReport(startDate, endDate);
        return ResponseEntity.ok(report);
    }

    @GetMapping("/inactive-customers")
    public ResponseEntity<List<InactiveCustomerDTO>> getInactiveCustomers() {
        Authentication authentication = getAuthentication();
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        if (!isAdminOrManager(authentication)) {
            return ResponseEntity.status(403).build();
        }
        List<InactiveCustomerDTO> customers = reportService.getInactiveCustomers();
        return ResponseEntity.ok(customers);
    }

    @GetMapping("/inventory-alerts")
    public ResponseEntity<List<ColdStockProductDTO>> getInventoryAlerts() {
        Authentication authentication = getAuthentication();
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        if (!isAdminOrManager(authentication)) {
            return ResponseEntity.status(403).build();
        }
        List<ColdStockProductDTO> alerts = reportService.getColdStockAlerts();
        return ResponseEntity.ok(alerts);
    }

    @GetMapping("/price-list")
    public ResponseEntity<?> getPriceList(
            @RequestParam(defaultValue = "text") String format,
            @RequestParam(defaultValue = "EC") String countryCode,
            @RequestParam(defaultValue = "FINAL") String customerType,
            @RequestParam(required = false) Boolean includeEcFinalPrice,
            @RequestParam(required = false) Boolean includeEcDistributorPrice,
            @RequestParam(required = false) Boolean includePeFinalPrice,
            @RequestParam(required = false) Boolean includePeDistributorPrice,
            @RequestParam(required = false) Boolean includeStock
    ) {
        Authentication authentication = getAuthentication();
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        if (!isAdminOrManager(authentication)) {
            return ResponseEntity.status(403).build();
        }

        PriceListColumnSelection selection = PriceListColumnSelection.fromNullable(
                includeEcFinalPrice,
                includeEcDistributorPrice,
                includePeFinalPrice,
                includePeDistributorPrice,
                includeStock
        );
        PriceListResponse response = priceListService.buildPriceList(selection);
        String normalized = format == null ? "text" : format.trim().toLowerCase();
        return switch (normalized) {
            case "html" -> ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .body(response.html());
            case "pdf" -> {
                byte[] pdf = priceListService.buildPdf(response.items(), selection);
                yield ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_PDF)
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=lista-precios.pdf")
                        .body(pdf);
            }
            case "xlsx" -> {
                byte[] xlsx = priceListService.buildXlsx(response.items(), selection);
                yield ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=lista-precios.xlsx")
                        .body(xlsx);
            }
            case "json" -> ResponseEntity.ok(response);
            default -> ResponseEntity.ok()
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(response.text());
        };
    }

    @PostMapping("/price-list/send")
    public ResponseEntity<PriceListSendResponse> sendPriceList(@RequestBody PriceListSendRequest request) {
        Authentication authentication = getAuthentication();
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        if (!isAdminOrManager(authentication)) {
            return ResponseEntity.status(403).build();
        }

        PriceListColumnSelection selection = PriceListColumnSelection.fromNullable(
                request != null ? request.includeEcFinalPrice() : null,
                request != null ? request.includeEcDistributorPrice() : null,
                request != null ? request.includePeFinalPrice() : null,
                request != null ? request.includePeDistributorPrice() : null,
                request != null ? request.includeStock() : null
        );
        PriceListResponse priceList = priceListService.buildPriceList(selection);
        boolean includePdf = true;
        boolean includeXlsx = true;
        byte[] pdf = includePdf ? priceListService.buildPdf(priceList.items(), selection) : null;
        byte[] xlsx = includeXlsx ? priceListService.buildXlsx(priceList.items(), selection) : null;

        String subject = request != null ? request.subject() : null;
        String toEmail = request != null ? request.toEmail() : null;
        boolean emailSent = false;
        String emailMessage = "";
        if (toEmail != null && !toEmail.trim().isBlank()) {
            try {
                priceListEmailService.sendPriceList(toEmail, subject, priceList.html(), pdf, xlsx);
                emailSent = true;
                emailMessage = "Lista enviada por email con PDF y Excel adjuntos.";
            } catch (Exception ex) {
                emailMessage = ex.getMessage() != null ? ex.getMessage() : "No se pudo enviar el email.";
            }
        }

        String whatsappText = resolvePriceListWhatsappText(request, priceList.text());
        String whatsappUrl = priceListService.buildWhatsappUrl(request != null ? request.whatsappPhone() : null, whatsappText);
        boolean whatsappSent = false;
        String whatsappMessage = "";
        if (request != null && Boolean.TRUE.equals(request.sendWhatsapp())) {
            try {
                priceListWhatsappService.sendPriceList(request.whatsappPhone(), whatsappText);
                whatsappSent = true;
                whatsappMessage = "Lista enviada por WhatsApp.";
            } catch (Exception ex) {
                whatsappMessage = ex.getMessage() != null ? ex.getMessage() : "No se pudo enviar el WhatsApp.";
            }
        } else if (request != null && request.whatsappPhone() != null && !request.whatsappPhone().trim().isBlank()) {
            whatsappMessage = "Texto de WhatsApp generado.";
        }

        PriceListSendResponse response = new PriceListSendResponse(
                LocalDateTime.now(),
                whatsappText,
                whatsappUrl,
                emailSent,
                whatsappSent,
                emailMessage,
                whatsappMessage
        );
        return ResponseEntity.ok(response);
    }

    private String resolvePriceListWhatsappText(PriceListSendRequest request, String defaultText) {
        Map<String, String> variables = new HashMap<>();
        variables.put("clientName", request != null && request.clientName() != null ? request.clientName().trim() : "");
        variables.put("priceListText", defaultText != null ? defaultText : "");
        variables.put("whatsappPhone", request != null && request.whatsappPhone() != null ? request.whatsappPhone().trim() : "");
        variables.put("subject", request != null && request.subject() != null ? request.subject().trim() : "");

        NotificationTemplateService.ResolvedTemplate resolved =
                notificationTemplateService.resolveTemplates("PRICE_LIST_SHARED", variables);
        if (resolved.whatsappText() != null && !resolved.whatsappText().isBlank()) {
            return resolved.whatsappText();
        }
        return defaultText;
    }

    private Authentication getAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() ||
                authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return authentication;
    }

    private boolean isAdminOrManager(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ADMIN")
                        || authority.getAuthority().equals("MANAGER"));
    }
}
