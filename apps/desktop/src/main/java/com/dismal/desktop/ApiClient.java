package com.dismal.desktop;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

public class ApiClient {

    private static final String DEFAULT_BASE_URL = "http://localhost:8080";
    private static final Duration TIMEOUT = Duration.ofSeconds(20);

    private final HttpClient httpClient;
    private final ObjectMapper mapper;
    private final String baseUrl;
    private String token;

    public ApiClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .build();
        this.mapper = new ObjectMapper();
        this.mapper.registerModule(new JavaTimeModule());
        this.mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.baseUrl = resolveBaseUrl();
    }

    public LoginResult login(String email, String password) throws IOException, InterruptedException {
        AuthRequest request = new AuthRequest(email, password);
        String body = mapper.writeValueAsString(request);

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/v1/auth/authenticate"))
                .timeout(TIMEOUT)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new ApiException(response.statusCode(), extractMessage(response.body()));
        }

        AuthResponse auth = mapper.readValue(response.body(), AuthResponse.class);
        if (auth.token() == null || auth.token().isBlank()) {
            throw new ApiException(500, "Token missing from response");
        }

        this.token = auth.token();
        UserMe user = fetchMe();
        return new LoginResult(user, auth.token());
    }

    public UserMe fetchMe() throws IOException, InterruptedException {
        if (token == null || token.isBlank()) {
            throw new ApiException(401, "Missing token");
        }
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/v1/users/me"))
                .timeout(TIMEOUT)
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new ApiException(response.statusCode(), "Unauthorized");
        }
        return mapper.readValue(response.body(), UserMe.class);
    }

    public DashboardSummary getDashboardSummary() throws IOException, InterruptedException {
        return getJson("/api/v1/dashboard/admin/summary", DashboardSummary.class);
    }

    public PageResponse<RecentSale> getRecentSales(int page, int size) throws IOException, InterruptedException {
        String path = String.format("/api/v1/dashboard/admin/recent-sales?page=%d&size=%d", page, size);
        return getJson(path, new TypeReference<>() {});
    }

    public PageResponse<ArItem> getArItems(String status, int page, int size) throws IOException, InterruptedException {
        String path = String.format("/api/v1/dashboard/admin/ar?status=%s&page=%d&size=%d", status, page, size);
        return getJson(path, new TypeReference<>() {});
    }

    public ArDetail getArBySale(String saleId) throws IOException, InterruptedException {
        return getJson("/api/v1/ar/sale/" + saleId, ArDetail.class);
    }

    public PageResponse<DashboardTarget> getDashboardTargets(String sort, int page, int size)
            throws IOException, InterruptedException {
        String path = String.format("/api/v1/dashboard/admin/sales-targets?sort=%s&page=%d&size=%d", sort, page, size);
        return getJson(path, new TypeReference<>() {});
    }

    public PageResponse<OutboxItem> getDashboardOutbox(String status, int page, int size)
            throws IOException, InterruptedException {
        String path = String.format("/api/v1/dashboard/admin/outbox?status=%s&page=%d&size=%d", status, page, size);
        return getJson(path, new TypeReference<>() {});
    }

    public java.util.Map<String, Double> getMonthlySales(int year) throws IOException, InterruptedException {
        String path = String.format("/api/v1/dashboard/admin/monthly-sales?year=%d", year);
        return getJson(path, new TypeReference<>() {});
    }

    public SalesPage listSales(int page, int size) throws IOException, InterruptedException {
        String path = String.format("/api/v1/sales?page=%d&size=%d", Math.max(page, 0), Math.max(size, 1));
        return getJson(path, SalesPage.class);
    }

    public SalesPage listSales(String status, int page, int size) throws IOException, InterruptedException {
        return listSalesFiltered(null, status, null, null, null, page, size);
    }

    public SalesPage listSalesFiltered(
            String clientId,
            String status,
            String type,
            LocalDate from,
            LocalDate to,
            int page,
            int size
    ) throws IOException, InterruptedException {
        StringBuilder path = new StringBuilder("/api/v1/sales");
        path.append("?page=").append(Math.max(page, 0)).append("&size=").append(Math.max(size, 1));
        if (clientId != null && !clientId.isBlank()) {
            path.append("&clientId=").append(clientId);
        }
        if (status != null && !status.isBlank()) {
            path.append("&status=").append(status);
        }
        if (type != null && !type.isBlank()) {
            path.append("&type=").append(type);
        }
        if (from != null) {
            path.append("&from=").append(from);
        }
        if (to != null) {
            path.append("&to=").append(to);
        }
        return getJson(path.toString(), SalesPage.class);
    }

    public SaleRow createSale(String clientId, String saleType, List<SaleItemPayload> items)
            throws IOException, InterruptedException {
        List<SaleItemRequest> mapped = items.stream()
                .map(item -> new SaleItemRequest(item.softwareId(), item.quantity(), item.unitPrice()))
                .toList();
        CreateSaleRequest payload = new CreateSaleRequest(clientId, saleType, mapped);
        return sendJson("/api/v1/sales", "POST", payload, SaleRow.class);
    }

    public SaleRow confirmSale(String saleId, List<String> deliveryChannels) throws IOException, InterruptedException {
        ConfirmSaleRequest payload = new ConfirmSaleRequest(deliveryChannels);
        return sendJson("/api/v1/sales/" + saleId + "/confirm", "POST", payload, SaleRow.class);
    }

    public PageResponse<License> listLicenses(String softwareId, String status, int page, int size)
            throws IOException, InterruptedException {
        StringBuilder path = new StringBuilder("/api/v1/inventory/licenses");
        path.append("?page=").append(Math.max(page, 0)).append("&size=").append(Math.max(size, 1));
        if (softwareId != null && !softwareId.isBlank()) {
            path.append("&softwareId=").append(softwareId);
        }
        if (status != null && !status.isBlank()) {
            path.append("&status=").append(status);
        }
        return getJson(path.toString(), new TypeReference<>() {});
    }

    public List<LicenseAssignment> getLicenseAssignments(String licenseId) throws IOException, InterruptedException {
        return getJson("/api/v1/inventory/licenses/" + licenseId + "/assignments", new TypeReference<>() {});
    }

    public List<Product> listProducts() throws IOException, InterruptedException {
        return getJson("/api/v1/store/products", new TypeReference<>() {});
    }

    public Product createProduct(Product payload) throws IOException, InterruptedException {
        return sendJson("/api/v1/store/products", "POST", payload, Product.class);
    }

    public Product updateProduct(String id, Product payload) throws IOException, InterruptedException {
        return sendJson("/api/v1/store/products/" + id, "PUT", payload, Product.class);
    }

    public void deleteProduct(String id, boolean force) throws IOException, InterruptedException {
        String path = "/api/v1/store/product/" + id + "?force=" + force;
        sendJson(path, "DELETE", null, Void.class);
    }

    public void createLicense(LicenseCreateRequest payload) throws IOException, InterruptedException {
        sendJson("/api/v1/inventory/licenses", "POST", payload, Void.class);
    }

    public void createLicensesBulk(List<LicenseCreateRequest> licenses) throws IOException, InterruptedException {
        BulkLicenseRequest payload = new BulkLicenseRequest(licenses);
        sendJson("/api/v1/inventory/licenses/bulk", "POST", payload, Void.class);
    }

    public List<Customer> listCustomers() throws IOException, InterruptedException {
        return getJson("/api/v1/customers", new TypeReference<>() {});
    }

    public PageResponse<AdminUser> listUsers(String type, int page, int size, String query)
            throws IOException, InterruptedException {
        StringBuilder path = new StringBuilder("/api/v1/admin/users");
        path.append("?type=").append(type);
        path.append("&page=").append(Math.max(page, 0));
        path.append("&size=").append(Math.max(size, 1));
        if (query != null && !query.isBlank()) {
            path.append("&q=").append(query);
        }
        return getJson(path.toString(), new TypeReference<>() {});
    }

    public AdminUser createUser(AdminUser payload, String password) throws IOException, InterruptedException {
        UserSaveRequest request = UserSaveRequest.from(payload, password);
        return sendJson("/api/v1/admin/users", "POST", request, AdminUser.class);
    }

    public AdminUser updateUser(String id, AdminUser payload) throws IOException, InterruptedException {
        UserSaveRequest request = UserSaveRequest.from(payload, null);
        return sendJson("/api/v1/admin/users/" + id, "PUT", request, AdminUser.class);
    }

    public AdminUser updateUserStatus(String id, boolean enabled) throws IOException, InterruptedException {
        return sendJson("/api/v1/admin/users/" + id + "/status", "PATCH", new StatusUpdate(enabled), AdminUser.class);
    }

    public TempPassword resetUserPassword(String id) throws IOException, InterruptedException {
        return sendJson("/api/v1/admin/users/" + id + "/reset-password", "POST", null, TempPassword.class);
    }

    public AdminImportResult importUsers(Path filePath, String format) throws IOException, InterruptedException {
        String boundary = "----DismalBoundary" + System.currentTimeMillis();
        byte[] fileBytes = Files.readAllBytes(filePath);
        String filename = filePath.getFileName().toString();
        byte[] body = buildMultipart(boundary, format, filename, fileBytes);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/v1/admin/users/import"))
                .timeout(TIMEOUT)
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new ApiException(response.statusCode(), extractMessage(response.body()));
        }
        return mapper.readValue(response.body(), AdminImportResult.class);
    }

    public PageResponse<InvoiceSummary> listInvoices(String query, String from, String to, int page, int size)
            throws IOException, InterruptedException {
        StringBuilder path = new StringBuilder("/api/v1/invoices");
        path.append("?page=").append(Math.max(page, 0)).append("&size=").append(Math.max(size, 1));
        if (query != null && !query.isBlank()) {
            path.append("&q=").append(query);
        }
        if (from != null && !from.isBlank()) {
            path.append("&from=").append(from);
        }
        if (to != null && !to.isBlank()) {
            path.append("&to=").append(to);
        }
        return getJson(path.toString(), new TypeReference<>() {});
    }

    public InvoiceDetail getInvoice(String id) throws IOException, InterruptedException {
        return getJson("/api/v1/invoices/" + id, InvoiceDetail.class);
    }

    public InvoiceDetail createInvoice(String saleId, String dueDate) throws IOException, InterruptedException {
        InvoiceCreateRequest payload = new InvoiceCreateRequest(saleId, dueDate);
        return sendJson("/api/v1/invoices", "POST", payload, InvoiceDetail.class);
    }

    public InvoiceDetail voidInvoice(String id) throws IOException, InterruptedException {
        return sendJson("/api/v1/invoices/" + id + "/void", "POST", null, InvoiceDetail.class);
    }

    public SaleRow addPayment(String saleId, double amount, String method, String reference)
            throws IOException, InterruptedException {
        PaymentRequest payload = new PaymentRequest(saleId, amount, method, reference);
        return sendJson("/api/v1/payments", "POST", payload, SaleRow.class);
    }

    public PageResponse<QuoteSummary> listQuotes(String status, int page, int size)
            throws IOException, InterruptedException {
        StringBuilder path = new StringBuilder("/api/v1/quotes");
        path.append("?page=").append(Math.max(page, 0)).append("&size=").append(Math.max(size, 1));
        if (status != null && !status.isBlank()) {
            path.append("&status=").append(status);
        }
        return getJson(path.toString(), new TypeReference<>() {});
    }

    public QuoteDetail getQuote(String id) throws IOException, InterruptedException {
        return getJson("/api/v1/quotes/" + id, QuoteDetail.class);
    }

    public QuoteDetail createQuote(QuoteCreateRequest payload) throws IOException, InterruptedException {
        return sendJson("/api/v1/quotes", "POST", payload, QuoteDetail.class);
    }

    public void convertQuote(String id) throws IOException, InterruptedException {
        sendJson("/api/v1/quotes/" + id + "/convert-to-sale", "POST", null, Void.class);
    }

    public PageResponse<Campaign> listCampaigns(String status, int page, int size)
            throws IOException, InterruptedException {
        StringBuilder path = new StringBuilder("/api/v1/marketing/campaigns");
        path.append("?page=").append(Math.max(page, 0)).append("&size=").append(Math.max(size, 1));
        if (status != null && !status.isBlank()) {
            path.append("&status=").append(status);
        }
        return getJson(path.toString(), new TypeReference<>() {});
    }

    public List<MarketingAnalysisResult> getProductAnalysis() throws IOException, InterruptedException {
        return getJson("/api/v1/marketing/intelligence/analysis", new TypeReference<>() {});
    }

    public PageResponse<StrategyAction> listStrategyActions(String status, int page, int size)
            throws IOException, InterruptedException {
        StringBuilder path = new StringBuilder("/api/v1/marketing/strategy/actions");
        path.append("?page=").append(Math.max(page, 0)).append("&size=").append(Math.max(size, 1));
        if (status != null && !status.isBlank()) {
            path.append("&status=").append(status);
        }
        return getJson(path.toString(), new TypeReference<>() {});
    }

    public StrategyAction updateStrategyActionStatus(String id, String status) throws IOException, InterruptedException {
        return sendJson("/api/v1/marketing/strategy/actions/" + id + "/status", "PATCH",
                new StrategyActionStatusUpdate(status, null), StrategyAction.class);
    }

    public Campaign createCampaign(CampaignRequest payload) throws IOException, InterruptedException {
        return sendJson("/api/v1/marketing/campaigns", "POST", payload, Campaign.class);
    }

    public Campaign updateCampaign(String id, CampaignRequest payload) throws IOException, InterruptedException {
        return sendJson("/api/v1/marketing/campaigns/" + id, "PUT", payload, Campaign.class);
    }

    public Campaign scheduleCampaign(String id, String scheduledAt) throws IOException, InterruptedException {
        return sendJson("/api/v1/marketing/campaigns/" + id + "/schedule", "POST",
                new ScheduleRequest(scheduledAt), Campaign.class);
    }

    public void sendCampaignTest(String id, String clientId, String channel) throws IOException, InterruptedException {
        sendJson("/api/v1/marketing/campaigns/" + id + "/send-test", "POST",
                new CampaignTestRequest(clientId, channel), Void.class);
    }

    public List<SalesTarget> listSalesTargets() throws IOException, InterruptedException {
        return getJson("/api/v1/sales-targets", new TypeReference<>() {});
    }

    public SalesTargetSummary getSalesTargetSummary() throws IOException, InterruptedException {
        return getJson("/api/v1/sales-targets/summary", SalesTargetSummary.class);
    }

    public List<RealActivationCost> getRealActivationCosts() throws IOException, InterruptedException {
        return getJson("/api/v1/sales-targets/real-costs", new TypeReference<>() {});
    }

    public SalesTargetSettings getSalesTargetSettings() throws IOException, InterruptedException {
        return getJson("/api/v1/sales-targets/settings", SalesTargetSettings.class);
    }

    public SalesTargetSettings updateSalesTargetSettings(SalesTargetSettings payload) throws IOException, InterruptedException {
        return sendJson("/api/v1/sales-targets/settings", "PUT", payload, SalesTargetSettings.class);
    }

    public void createSalesTarget(SalesTargetSaveRequest payload) throws IOException, InterruptedException {
        sendJson("/api/v1/sales-targets", "POST", payload, Void.class);
    }

    public void updateSalesTarget(String id, SalesTargetSaveRequest payload) throws IOException, InterruptedException {
        sendJson("/api/v1/sales-targets/" + id, "PUT", payload, Void.class);
    }

    public void deleteSalesTarget(String id) throws IOException, InterruptedException {
        sendJson("/api/v1/sales-targets/" + id, "DELETE", null, Void.class);
    }

    public StockSummary[] getInventorySummary() throws IOException, InterruptedException {
        return getJson("/api/v1/inventory/stock", StockSummary[].class);
    }

    public java.util.List<ExpenseItem> listExpenses() throws IOException, InterruptedException {
        return getJson("/api/v1/expenses", new TypeReference<>() {});
    }

    public ExpenseItem createExpense(ExpenseRequest payload) throws IOException, InterruptedException {
        return sendJson("/api/v1/expenses", "POST", payload, ExpenseItem.class);
    }

    public ExpenseItem updateExpense(String id, ExpenseRequest payload) throws IOException, InterruptedException {
        return sendJson("/api/v1/expenses/" + id, "PUT", payload, ExpenseItem.class);
    }

    public void deleteExpense(String id) throws IOException, InterruptedException {
        sendJson("/api/v1/expenses/" + id, "DELETE", null, Void.class);
    }

    public ExpenseSummary getExpenseSummary() throws IOException, InterruptedException {
        return getJson("/api/v1/expenses/summary", ExpenseSummary.class);
    }

    public String getPriceListText(String countryCode, String customerType) throws IOException, InterruptedException {
        return getText("/api/v1/reports/price-list?format=text&countryCode=" + countryCode + "&customerType=" + customerType);
    }

    public byte[] downloadPriceListPdf(String countryCode, String customerType) throws IOException, InterruptedException {
        return getBytes("/api/v1/reports/price-list?format=pdf&countryCode=" + countryCode + "&customerType=" + customerType);
    }

    public byte[] downloadPriceListXlsx(String countryCode, String customerType) throws IOException, InterruptedException {
        return getBytes("/api/v1/reports/price-list?format=xlsx&countryCode=" + countryCode + "&customerType=" + customerType);
    }

    public PriceListSendResponse sendPriceList(PriceListSendRequest payload) throws IOException, InterruptedException {
        return sendJson("/api/v1/reports/price-list/send", "POST", payload, PriceListSendResponse.class);
    }

    public SalesPeriodReport getSalesPeriodReport(String period, LocalDate from, LocalDate to)
            throws IOException, InterruptedException {
        StringBuilder path = new StringBuilder("/api/v1/reports/sales/").append(period);
        path.append(buildDateQuery(from, to));
        return getJson(path.toString(), SalesPeriodReport.class);
    }

    public SalesVsTargetReport getSalesVsTargets(LocalDate from, LocalDate to)
            throws IOException, InterruptedException {
        StringBuilder path = new StringBuilder("/api/v1/reports/sales/vs-targets");
        path.append(buildDateQuery(from, to));
        return getJson(path.toString(), SalesVsTargetReport.class);
    }

    public IntegrationSettings getIntegrationSettings() throws IOException, InterruptedException {
        return getJson("/api/v1/integrations/settings", IntegrationSettings.class);
    }

    public IntegrationSettings updateIntegrationSettings(IntegrationSettingsUpdate update)
            throws IOException, InterruptedException {
        return sendJson("/api/v1/integrations/settings", "PUT", update, IntegrationSettings.class);
    }

    public void testN8n(String to, String message) throws IOException, InterruptedException {
        sendJson("/api/v1/integrations/n8n/test", "POST", new N8nTestRequest(to, message), Void.class);
    }

    public void testSmtp(String to, String subject, String body) throws IOException, InterruptedException {
        TestEmailRequest payload = new TestEmailRequest(to, subject, body);
        sendJson("/api/v1/integrations/settings/test-email", "POST", payload, Void.class);
    }

    public List<NotificationTemplate> listTemplates() throws IOException, InterruptedException {
        return getJson("/api/v1/integrations/notifications/templates", new TypeReference<>() {});
    }

    public NotificationTemplate createTemplate(NotificationTemplatePayload payload)
            throws IOException, InterruptedException {
        return sendJson("/api/v1/integrations/notifications/templates", "POST", payload, NotificationTemplate.class);
    }

    public NotificationTemplate updateTemplate(String id, NotificationTemplatePayload payload)
            throws IOException, InterruptedException {
        return sendJson("/api/v1/integrations/notifications/templates/" + id, "PUT", payload, NotificationTemplate.class);
    }

    public void deleteTemplate(String id) throws IOException, InterruptedException {
        sendJson("/api/v1/integrations/notifications/templates/" + id, "DELETE", null, Void.class);
    }

    public OutboxPage listOutboxFailed(int page, int size) throws IOException, InterruptedException {
        String path = String.format("/api/v1/integrations/outbox?status=FAILED&page=%d&size=%d", page, size);
        return getJson(path, OutboxPage.class);
    }

    public OutboxPage listOutboxForSale(String saleId) throws IOException, InterruptedException {
        String path = String.format(
                "/api/v1/integrations/outbox?eventType=LICENSES_DELIVERED&aggregateId=%s&page=0&size=20",
                saleId
        );
        return getJson(path, OutboxPage.class);
    }

    public List<PaymentItem> listPayments(String saleId) throws IOException, InterruptedException {
        String path = String.format("/api/v1/sales/%s/payments", saleId);
        return getJson(path, new TypeReference<>() {});
    }

    public List<SaleLicense> listSaleLicenses(String saleId) throws IOException, InterruptedException {
        String path = String.format("/api/v1/sales/%s/licenses", saleId);
        return getJson(path, new TypeReference<>() {});
    }

    public List<DeliveryLog> listDeliveryLogs(String outboxId) throws IOException, InterruptedException {
        String path = String.format(
                "/api/v1/integrations/notifications/templates/delivery-logs?outboxEventId=%s",
                outboxId
        );
        return getJson(path, new TypeReference<>() {});
    }

    public void retryOutbox(String id) throws IOException, InterruptedException {
        sendJson("/api/v1/integrations/outbox/" + id + "/retry", "POST", null, Void.class);
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }

    public void clearToken() {
        this.token = null;
    }

    private String resolveBaseUrl() {
        String fromProperty = System.getProperty("dismal.api.url");
        if (fromProperty != null && !fromProperty.isBlank()) {
            return fromProperty;
        }
        String fromEnv = System.getenv("DISMAL_API_URL");
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv;
        }
        return DEFAULT_BASE_URL;
    }

    private String extractMessage(String body) {
        if (body == null || body.isBlank()) {
            return "Request failed";
        }
        try {
            ErrorResponse error = mapper.readValue(body, ErrorResponse.class);
            if (error.message() != null && !error.message().isBlank()) {
                return error.message();
            }
        } catch (Exception ignored) {
        }
        return body;
    }

    private record AuthRequest(String email, String password) {}
    private record ConfirmSaleRequest(List<String> deliveryChannels) {}
    private record TestEmailRequest(String to, String subject, String body) {}
    private record N8nTestRequest(String to, String message) {}

    private record AuthResponse(String token) {}

    private record ErrorResponse(String message) {}

    private record StatusUpdate(boolean enabled) {}

    public record TempPassword(String tempPassword) {}

    private record CreateSaleRequest(String clientId, String saleType, List<SaleItemRequest> items) {}

    public record SaleItemPayload(String softwareId, int quantity, double unitPrice) {}

    private record SaleItemRequest(String softwareId, int quantity, double unitPrice) {}

    private record InvoiceCreateRequest(String saleId, String dueDate) {}

    private record PaymentRequest(String saleId, double amount, String method, String reference) {}

    public record QuoteCreateItem(String softwareId, int quantity, double unitPrice) {}

    public record QuoteCreateRequest(String clientId, String notes, List<QuoteCreateItem> items) {}

    public record CampaignRequest(
            String title,
            String messageBody,
            String imageUrl,
            String targetRole,
            String channel,
            String productId,
            String scheduledAt,
            String status
    ) {}

    public record ScheduleRequest(String scheduledAt) {}

    public record CampaignTestRequest(String clientId, String channel) {}

    private record StrategyActionStatusUpdate(String status, String resultNotes) {}

    public record SalesTargetSaveRequest(
            String softwareId,
            int metaUnits,
            double salePrice,
            double variableCost,
            Double fixedCostProduct,
            int unitsSoldCurrent,
            String deadline,
            String notes
    ) {}

    public record SalesTargetSettings(
            Double fixedCostGlobal
    ) {}

    public record ExpenseRequest(
            String name,
            String category,
            String type,
            double totalAmount,
            int months,
            int dueDay,
            String priority,
            boolean active
    ) {}

    public record ExpenseItem(
            String id,
            String name,
            String category,
            String type,
            double totalAmount,
            int months,
            double monthlyAmount,
            int dueDay,
            String priority,
            boolean active,
            String createdAt
    ) {}

    public record ExpenseSummary(
            double monthPayments,
            double monthExpenses,
            double balance,
            String status,
            String recommendation
    ) {}

    public record LicenseCreateRequest(
            String softwareId,
            String licenseKey,
            double purchasePrice,
            int maxActivations
    ) {}

    private record BulkLicenseRequest(List<LicenseCreateRequest> licenses) {}

    private record UserSaveRequest(
            String firstname,
            String lastname,
            String email,
            String phone,
            String role,
            boolean enabled,
            String taxId,
            String billingEmail,
            boolean hasCredit,
            double creditLimit,
            int creditDays,
            String password
    ) {
        static UserSaveRequest from(AdminUser user, String password) {
            return new UserSaveRequest(
                    user.firstname(),
                    user.lastname(),
                    user.email(),
                    user.phone(),
                    user.role(),
                    user.enabled(),
                    user.taxId(),
                    user.billingEmail(),
                    user.hasCredit(),
                    user.creditLimit(),
                    user.creditDays(),
                    password
            );
        }
    }

    private <T> T getJson(String path, Class<T> type) throws IOException, InterruptedException {
        if (token == null || token.isBlank()) {
            throw new ApiException(401, "Missing token");
        }
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .timeout(TIMEOUT)
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new ApiException(response.statusCode(), extractMessage(response.body()));
        }
        if (type == Void.class) {
            return null;
        }
        return mapper.readValue(response.body(), type);
    }

    private <T> T getJson(String path, TypeReference<T> type) throws IOException, InterruptedException {
        if (token == null || token.isBlank()) {
            throw new ApiException(401, "Missing token");
        }
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .timeout(TIMEOUT)
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new ApiException(response.statusCode(), extractMessage(response.body()));
        }
        return mapper.readValue(response.body(), type);
    }

    private String getText(String path) throws IOException, InterruptedException {
        if (token == null || token.isBlank()) {
            throw new ApiException(401, "Missing token");
        }
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .timeout(TIMEOUT)
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new ApiException(response.statusCode(), extractMessage(response.body()));
        }
        return response.body();
    }

    private byte[] getBytes(String path) throws IOException, InterruptedException {
        if (token == null || token.isBlank()) {
            throw new ApiException(401, "Missing token");
        }
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .timeout(TIMEOUT)
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();
        HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() >= 400) {
            String body = new String(response.body(), StandardCharsets.UTF_8);
            throw new ApiException(response.statusCode(), extractMessage(body));
        }
        return response.body();
    }

    private <T> T sendJson(String path, String method, Object body, Class<T> type)
            throws IOException, InterruptedException {
        if (token == null || token.isBlank()) {
            throw new ApiException(401, "Missing token");
        }
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .timeout(TIMEOUT)
                .header("Authorization", "Bearer " + token);
        if (body != null) {
            String payload = mapper.writeValueAsString(body);
            builder.header("Content-Type", "application/json");
            builder.method(method, HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8));
        } else {
            builder.method(method, HttpRequest.BodyPublishers.noBody());
        }

        HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new ApiException(response.statusCode(), extractMessage(response.body()));
        }
        if (type == Void.class) {
            return null;
        }
        return mapper.readValue(response.body(), type);
    }

    private byte[] buildMultipart(String boundary, String format, String filename, byte[] fileBytes) {
        String line = "\r\n";
        StringBuilder builder = new StringBuilder();
        builder.append("--").append(boundary).append(line);
        builder.append("Content-Disposition: form-data; name=\"format\"").append(line).append(line);
        builder.append(format).append(line);

        builder.append("--").append(boundary).append(line);
        builder.append("Content-Disposition: form-data; name=\"file\"; filename=\"").append(filename).append("\"").append(line);
        builder.append("Content-Type: application/octet-stream").append(line).append(line);
        byte[] header = builder.toString().getBytes(StandardCharsets.UTF_8);
        byte[] footer = (line + "--" + boundary + "--" + line).getBytes(StandardCharsets.UTF_8);

        byte[] body = new byte[header.length + fileBytes.length + footer.length];
        System.arraycopy(header, 0, body, 0, header.length);
        System.arraycopy(fileBytes, 0, body, header.length, fileBytes.length);
        System.arraycopy(footer, 0, body, header.length + fileBytes.length, footer.length);
        return body;
    }

    private String buildDateQuery(LocalDate from, LocalDate to) {
        if (from == null && to == null) {
            return "";
        }
        StringBuilder query = new StringBuilder("?");
        if (from != null) {
            query.append("from=").append(from);
        }
        if (to != null) {
            if (query.length() > 1) {
                query.append("&");
            }
            query.append("to=").append(to);
        }
        return query.toString();
    }
}
