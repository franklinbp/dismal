package com.dismal.distribuciones.modules.admin.service;

import com.dismal.distribuciones.exception.BadRequestException;
import com.dismal.distribuciones.exception.ResourceNotFoundException;
import com.dismal.distribuciones.modules.admin.dto.AdminImportResult;
import com.dismal.distribuciones.modules.admin.dto.AdminUserRequest;
import com.dismal.distribuciones.modules.admin.dto.AdminUserResponse;
import com.dismal.distribuciones.modules.admin.dto.AdminUserType;
import com.dismal.distribuciones.modules.admin.dto.ArSummaryResponse;
import com.dismal.distribuciones.modules.admin.dto.ArSummaryStatus;
import com.dismal.distribuciones.modules.integrations.crm.event.CustomerChangedCrmSyncEvent;
import com.dismal.distribuciones.modules.sales.domain.AccountsReceivableStatus;
import com.dismal.distribuciones.modules.sales.repository.AccountsReceivableRepository;
import com.dismal.distribuciones.modules.security.domain.CustomerType;
import com.dismal.distribuciones.modules.security.domain.Role;
import com.dismal.distribuciones.modules.security.domain.User;
import com.dismal.distribuciones.modules.security.customer.domain.CustomerMarket;
import com.dismal.distribuciones.modules.security.customer.repository.CustomerMarketRepository;
import com.dismal.distribuciones.modules.security.customer.service.CustomerMarketService;
import com.dismal.distribuciones.modules.security.repository.UserRepository;
import com.dismal.distribuciones.modules.security.util.PhoneNormalizer;
import com.dismal.distribuciones.modules.storefront.domain.StorefrontCountry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminUserService {

    private static final String DEFAULT_IMPORT_PASSWORD = "ChangeMe123!";
    private static final Set<Role> INTERNAL_ROLES = EnumSet.of(Role.ADMIN, Role.MANAGER);
    private static final Set<Role> CLIENT_ROLES = EnumSet.of(Role.USER, Role.CUSTOMER);

    private final UserRepository userRepository;
    private final AccountsReceivableRepository accountsReceivableRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;
    private final CustomerMarketRepository customerMarketRepository;
    private final CustomerMarketService customerMarketService;

    @Value("${app.admin.ar.block-days:30}")
    private int blockDays;

    @Value("${app.admin.reset-password-enabled:false}")
    private boolean resetPasswordEnabled;

    @Value("${app.admin.import-default-password:" + DEFAULT_IMPORT_PASSWORD + "}")
    private String importDefaultPassword;

    @Transactional(readOnly = true)
    public Page<AdminUserResponse> listUsers(AdminUserType type, String q, int page, int size) {
        return listUsers(type, q, null, page, size);
    }

    @Transactional(readOnly = true)
    public Page<AdminUserResponse> listUsers(
            AdminUserType type,
            String q,
            StorefrontCountry country,
            int page,
            int size
    ) {
        Specification<User> spec = Specification.where(null);
        StorefrontCountry marketCountry = country != null ? country : StorefrontCountry.EC;
        if (type != null) {
            if (type == AdminUserType.INTERNAL) {
                spec = spec.and((root, query, cb) -> root.get("role").in(INTERNAL_ROLES));
            } else if (type == AdminUserType.CLIENT) {
                spec = spec.and((root, query, cb) -> root.get("role").in(CLIENT_ROLES));
                spec = spec.and((root, query, cb) -> {
                    var marketQuery = query.subquery(UUID.class);
                    var marketRoot = marketQuery.from(CustomerMarket.class);
                    marketQuery.select(marketRoot.get("user").get("id"));
                    marketQuery.where(cb.equal(marketRoot.get("country"), marketCountry));
                    return root.get("id").in(marketQuery);
                });
            }
        }
        if (q != null && !q.isBlank()) {
            String like = "%" + q.toLowerCase(Locale.ROOT) + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("email")), like),
                    cb.like(cb.lower(root.get("firstname")), like),
                    cb.like(cb.lower(root.get("lastname")), like),
                    cb.like(cb.lower(root.get("phone")), like),
                    cb.like(cb.lower(root.get("taxId")), like)
            ));
        }
        PageRequest pageRequest = PageRequest.of(Math.max(page, 0), Math.max(size, 1));
        return userRepository.findAll(spec, pageRequest)
                .map(user -> toAdminUserResponse(
                        user,
                        type == AdminUserType.CLIENT ? marketCountry : null
                ));
    }

    @Transactional
    public AdminUserResponse createUser(AdminUserRequest request) {
        if (request == null) {
            throw new BadRequestException("Request body is required.");
        }
        Role role = normalizeRole(request.role());
        boolean requirePassword = !CLIENT_ROLES.contains(role);
        StorefrontCountry country = CLIENT_ROLES.contains(role)
                ? safeCountry(request.country()) : null;
        validateRequest(request, requirePassword, true);
        User existing = userRepository.findByEmail(request.email()).orElse(null);
        if (existing != null) {
            if (CLIENT_ROLES.contains(role) && CLIENT_ROLES.contains(existing.getRole())
                    && customerMarketRepository.findByUserIdAndCountry(existing.getId(), country).isEmpty()) {
                existing.setFirstname(request.firstname());
                existing.setLastname(request.lastname());
                existing.setPhone(PhoneNormalizer.normalize(request.phone()));
                User savedExisting = userRepository.save(existing);
                CustomerMarket market = customerMarketService.updateCommercialProfile(
                        savedExisting,
                        country,
                        resolveCustomerType(request.customerType()),
                        request.taxId(),
                        request.billingEmail(),
                        request.hasCredit(),
                        request.creditLimit(),
                        request.creditDays(),
                        request.enabled()
                );
                publishCustomerSync(savedExisting);
                return toAdminUserResponse(savedExisting, market);
            }
            throw new BadRequestException("Email already in use.");
        }
        String rawPassword = request.password();
        if (!requirePassword && (rawPassword == null || rawPassword.isBlank())) {
            rawPassword = resolveDefaultClientPassword();
        }
        String normalizedPhone = PhoneNormalizer.normalize(request.phone());

        User user = User.builder()
                .firstname(request.firstname())
                .lastname(request.lastname())
                .email(request.email())
                .phone(normalizedPhone)
                .role(role)
                .customerType(resolveCustomerType(request.customerType()))
                .enabled(request.enabled() != null ? request.enabled() : true)
                .taxId(request.taxId())
                .billingEmail(request.billingEmail())
                .hasCredit(request.hasCredit() != null && request.hasCredit())
                .creditLimit(request.creditLimit() != null ? request.creditLimit() : BigDecimal.ZERO)
                .creditDays(request.creditDays() != null ? request.creditDays() : 0)
                .creditUsed(BigDecimal.ZERO)
                .password(passwordEncoder.encode(rawPassword))
                .build();

        User saved = userRepository.save(user);
        CustomerMarket market = null;
        if (CLIENT_ROLES.contains(role)) {
            market = customerMarketService.updateCommercialProfile(
                    saved,
                    country,
                    resolveCustomerType(request.customerType()),
                    request.taxId(),
                    request.billingEmail(),
                    request.hasCredit(),
                    request.creditLimit(),
                    request.creditDays(),
                    request.enabled()
            );
        }
        publishCustomerSync(saved);
        return market != null
                ? toAdminUserResponse(saved, market)
                : toAdminUserResponse(saved, (CustomerMarket) null);
    }

    @Transactional
    public AdminUserResponse updateUser(UUID userId, AdminUserRequest request) {
        validateRequest(request, false, false);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        if (request.email() != null && !request.email().equalsIgnoreCase(user.getEmail())
                && userRepository.existsByEmail(request.email())) {
            throw new BadRequestException("Email already in use.");
        }

        user.setFirstname(request.firstname());
        user.setLastname(request.lastname());
        user.setEmail(request.email());
        user.setPhone(PhoneNormalizer.normalize(request.phone()));
        if (request.role() != null) {
            user.setRole(normalizeRole(request.role()));
        }
        boolean clientAccount = CLIENT_ROLES.contains(user.getRole());
        StorefrontCountry country = clientAccount ? safeCountry(request.country()) : null;
        if (request.enabled() != null && !clientAccount) {
            if (!request.enabled() && user.getRole() == Role.ADMIN) {
                ensureNotLastAdmin(user);
            }
            user.setEnabled(request.enabled());
        }
        if (!clientAccount) {
            if (request.customerType() != null) {
                user.setCustomerType(resolveCustomerType(request.customerType()));
            }
            user.setTaxId(request.taxId());
            user.setBillingEmail(request.billingEmail());
            if (request.hasCredit() != null) {
                user.setHasCredit(request.hasCredit());
            }
            if (request.creditLimit() != null) {
                user.setCreditLimit(request.creditLimit());
            }
            if (request.creditDays() != null) {
                user.setCreditDays(request.creditDays());
            }
        }

        User saved = userRepository.save(user);
        CustomerMarket market = null;
        if (clientAccount) {
            market = customerMarketService.updateCommercialProfile(
                    saved,
                    country,
                    request.customerType(),
                    request.taxId(),
                    request.billingEmail(),
                    request.hasCredit(),
                    request.creditLimit(),
                    request.creditDays(),
                    request.enabled()
            );
        }
        publishCustomerSync(saved);
        return market != null
                ? toAdminUserResponse(saved, market)
                : toAdminUserResponse(saved, (CustomerMarket) null);
    }

    @Transactional(readOnly = true)
    public Role getUserRole(UUID userId) {
        return userRepository.findById(userId)
                .map(User::getRole)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
    }

    @Transactional
    public AdminUserResponse updateStatus(UUID userId, boolean enabled) {
        return updateStatus(userId, null, enabled);
    }

    @Transactional
    public AdminUserResponse updateStatus(
            UUID userId,
            StorefrontCountry country,
            boolean enabled
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
        if (CLIENT_ROLES.contains(user.getRole())) {
            CustomerMarket market = customerMarketService.getOrCreate(user, safeCountry(country));
            market.setActive(enabled);
            CustomerMarket savedMarket = customerMarketRepository.save(market);
            publishCustomerSync(user);
            return toAdminUserResponse(user, savedMarket);
        }
        if (!enabled && user.getRole() == Role.ADMIN) {
            ensureNotLastAdmin(user);
        }
        user.setEnabled(enabled);
        User saved = userRepository.save(user);
        publishCustomerSync(saved);
        return toAdminUserResponse(saved, (CustomerMarket) null);
    }

    @Transactional
    public String resetPassword(UUID userId) {
        if (!resetPasswordEnabled) {
            throw new BadRequestException("Reset password disabled.");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
        String tempPassword = generateTempPassword();
        user.setPassword(passwordEncoder.encode(tempPassword));
        userRepository.save(user);
        return tempPassword;
    }

    @Transactional
    public AdminImportResult importClients(MultipartFile file, String format) {
        return importClients(file, format, StorefrontCountry.EC);
    }

    @Transactional
    public AdminImportResult importClients(
            MultipartFile file,
            String format,
            StorefrontCountry defaultCountry
    ) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is required.");
        }
        if (importDefaultPassword == null || importDefaultPassword.isBlank()
                || DEFAULT_IMPORT_PASSWORD.equals(importDefaultPassword)) {
            throw new BadRequestException("Import default password must be configured.");
        }
        String safeFormat = format == null ? "csv" : format.toLowerCase(Locale.ROOT);
        List<Map<String, String>> rows;
        try {
            if ("xlsx".equals(safeFormat)) {
                rows = parseXlsx(file);
            } else if ("csv".equals(safeFormat)) {
                rows = parseCsv(file);
            } else {
                throw new BadRequestException("Unsupported format: " + format);
            }
        } catch (BadRequestException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BadRequestException("Invalid file format.");
        }

        int imported = 0;
        int skipped = 0;
        List<String> errors = new java.util.ArrayList<>();

        for (int i = 0; i < rows.size(); i++) {
            Map<String, String> row = rows.get(i);
            int rowIndex = i + 2;
            String email = normalizeValue(row.get("email"));
            if (email == null) {
                skipped++;
                trackError(errors, rowIndex, "Email requerido.");
                continue;
            }
            String firstname = normalizeValue(row.get("firstname"));
            String lastname = normalizeValue(row.get("lastname"));
            if (firstname == null || lastname == null) {
                skipped++;
                trackError(errors, rowIndex, "Nombre y apellido requeridos.");
                continue;
            }
            BigDecimal creditLimit = parseBigDecimal(row.get("credit_limit"), BigDecimal.ZERO);
            Integer creditDays = parseInteger(row.get("credit_days"), 0);
            if (creditLimit.signum() < 0 || creditDays < 0) {
                skipped++;
                trackError(errors, rowIndex, "Credito invalido.");
                continue;
            }

            Role role = resolveClientRole(row.get("role"));
            CustomerType customerType = resolveImportedCustomerType(row.get("customer_type"));
            boolean hasCredit = parseBoolean(row.get("has_credit"), false);
            boolean enabled = parseBoolean(row.get("enabled"), true);
            StorefrontCountry importCountry = parseImportCountry(
                    row.get("country"), safeCountry(defaultCountry)
            );

            String phone = normalizeValue(row.get("phone"));
            if (phone != null) {
                try {
                    phone = PhoneNormalizer.normalize(phone);
                } catch (BadRequestException ex) {
                    skipped++;
                    trackError(errors, rowIndex, "Telefono invalido.");
                    continue;
                }
            }

            User existing = userRepository.findByEmail(email).orElse(null);
            if (existing != null) {
                if (!CLIENT_ROLES.contains(existing.getRole())
                        || customerMarketRepository.findByUserIdAndCountry(
                                existing.getId(), importCountry
                        ).isPresent()) {
                    skipped++;
                    continue;
                }
                CustomerMarket market = customerMarketService.updateCommercialProfile(
                        existing,
                        importCountry,
                        customerType,
                        normalizeValue(row.get("tax_id")),
                        normalizeValue(row.get("billing_email")),
                        hasCredit,
                        creditLimit,
                        creditDays,
                        enabled
                );
                if (market != null) {
                    imported++;
                    publishCustomerSync(existing);
                }
                continue;
            }

            User user = User.builder()
                    .firstname(firstname)
                    .lastname(lastname)
                    .email(email)
                    .phone(phone)
                    .role(role)
                    .customerType(customerType)
                    .enabled(enabled)
                    .taxId(normalizeValue(row.get("tax_id")))
                    .billingEmail(normalizeValue(row.get("billing_email")))
                    .hasCredit(hasCredit)
                    .creditLimit(creditLimit)
                    .creditDays(creditDays)
                    .creditUsed(BigDecimal.ZERO)
                    .password(passwordEncoder.encode(importDefaultPassword))
                    .build();
            User saved = userRepository.save(user);
            customerMarketService.getOrCreate(
                    saved,
                    importCountry
            );
            publishCustomerSync(saved);
            imported++;
        }

        return new AdminImportResult(imported, skipped, errors);
    }

    @Transactional(readOnly = true)
    public byte[] exportClients(String format) {
        return exportClients(format, StorefrontCountry.EC);
    }

    @Transactional(readOnly = true)
    public byte[] exportClients(String format, StorefrontCountry country) {
        String safeFormat = format == null ? "csv" : format.toLowerCase(Locale.ROOT);
        List<AdminClientExportRow> clients = customerMarketRepository
                .findAllByCountryOrderByUserEmailAsc(safeCountry(country))
                .stream()
                .filter(market -> CLIENT_ROLES.contains(market.getUser().getRole()))
                .map(market -> new AdminClientExportRow(market.getUser(), market))
                .toList();
        try {
            if ("xlsx".equals(safeFormat)) {
                return exportXlsx(clients);
            }
            if ("csv".equals(safeFormat)) {
                return exportCsv(clients);
            }
        } catch (Exception ex) {
            throw new BadRequestException("Export failed.");
        }
        throw new BadRequestException("Unsupported format: " + format);
    }

    @Transactional(readOnly = true)
    public byte[] exportClientTemplate(String format) {
        return exportClientTemplate(format, StorefrontCountry.EC);
    }

    @Transactional(readOnly = true)
    public byte[] exportClientTemplate(String format, StorefrontCountry country) {
        String safeFormat = format == null ? "csv" : format.toLowerCase(Locale.ROOT);
        try {
            if ("xlsx".equals(safeFormat)) {
                return exportXlsx(List.of());
            }
            if ("csv".equals(safeFormat)) {
                return exportCsv(List.of());
            }
        } catch (Exception ex) {
            throw new BadRequestException("Export failed.");
        }
        throw new BadRequestException("Unsupported format: " + format);
    }

    private void validateRequest(AdminUserRequest request, boolean requirePassword, boolean requireRole) {
        if (request == null) {
            throw new BadRequestException("Request body is required.");
        }
        if (request.email() == null || request.email().isBlank()) {
            throw new BadRequestException("Email is required.");
        }
        if (requireRole && request.role() == null) {
            throw new BadRequestException("Role is required.");
        }
        if (request.creditLimit() != null && request.creditLimit().signum() < 0) {
            throw new BadRequestException("credit_limit must be >= 0.");
        }
        if (request.creditDays() != null && request.creditDays() < 0) {
            throw new BadRequestException("credit_days must be >= 0.");
        }
        if (requirePassword && (request.password() == null || request.password().isBlank())) {
            throw new BadRequestException("Password is required.");
        }
    }

    private String resolveDefaultClientPassword() {
        if (importDefaultPassword == null || importDefaultPassword.isBlank()) {
            return generateTempPassword();
        }
        return importDefaultPassword;
    }

    private void ensureNotLastAdmin(User user) {
        long admins = userRepository.countByRoleAndEnabledTrue(Role.ADMIN);
        if (admins <= 1 && user.getRole() == Role.ADMIN) {
            throw new BadRequestException("Cannot disable the last ADMIN user.");
        }
    }

    private AdminUserResponse toAdminUserResponse(User user, StorefrontCountry country) {
        CustomerMarket market = country != null
                ? customerMarketRepository.findByUserIdAndCountry(user.getId(), country).orElse(null)
                : null;
        return toAdminUserResponse(user, market);
    }

    private AdminUserResponse toAdminUserResponse(User user, CustomerMarket market) {
        ArSummaryResponse summary = resolveArSummary(user, market);
        CustomerType customerType = market != null
                ? market.getCustomerType()
                : user.getCustomerType() != null ? user.getCustomerType() : CustomerType.FINAL;
        return new AdminUserResponse(
                user.getId(),
                user.getFirstname(),
                user.getLastname(),
                user.getEmail(),
                user.getPhone(),
                user.getRole(),
                customerType,
                market != null ? market.getCountry() : null,
                market != null ? market.getCurrency() : null,
                market != null && market.isPrimaryMarket(),
                market != null ? market.isActive() : user.isEnabled(),
                user.isEmailVerified(),
                market != null ? market.getTaxId() : user.getTaxId(),
                market != null ? market.getBillingEmail() : user.getBillingEmail(),
                market != null ? market.isHasCredit() : user.isHasCredit(),
                market != null ? market.getCreditLimit() : user.getCreditLimit(),
                market != null ? market.getCreditUsed() : user.getCreditUsed(),
                market != null ? market.getCreditDays() : user.getCreditDays(),
                summary
        );
    }

    private ArSummaryResponse resolveArSummary(User user, CustomerMarket market) {
        if (!CLIENT_ROLES.contains(user.getRole())) {
            return new ArSummaryResponse(BigDecimal.ZERO, 0, ArSummaryStatus.AL_DIA);
        }
        try {
            List<AccountsReceivableStatus> openStatuses = List.of(
                    AccountsReceivableStatus.OPEN,
                    AccountsReceivableStatus.OVERDUE
            );
            BigDecimal balance = market != null
                    ? accountsReceivableRepository.sumBalanceByCustomerMarketAndStatusIn(
                            market.getId(), openStatuses
                    )
                    : accountsReceivableRepository.sumBalanceByClientAndStatusIn(user.getId(), openStatuses);
            LocalDate overdueDate = market != null
                    ? accountsReceivableRepository.findOldestOverdueDateByCustomerMarket(market.getId())
                    : accountsReceivableRepository.findOldestOverdueDate(user.getId());
            long daysOverdue = 0;
            if (overdueDate != null) {
                daysOverdue = ChronoUnit.DAYS.between(overdueDate, LocalDate.now());
                if (daysOverdue < 0) {
                    daysOverdue = 0;
                }
            }
            ArSummaryStatus status;
            if (balance == null || balance.compareTo(BigDecimal.ZERO) <= 0 || daysOverdue == 0) {
                status = ArSummaryStatus.AL_DIA;
            } else if (daysOverdue >= blockDays) {
                status = ArSummaryStatus.BLOQUEADO;
            } else {
                status = ArSummaryStatus.VENCIDO;
            }
            return new ArSummaryResponse(balance != null ? balance : BigDecimal.ZERO, daysOverdue, status);
        } catch (Exception ex) {
            log.warn("AR summary unavailable for user {}: {}", user.getId(), ex.getMessage());
            return new ArSummaryResponse(BigDecimal.ZERO, 0, ArSummaryStatus.AL_DIA);
        }
    }

    private String generateTempPassword() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
        SecureRandom random = new SecureRandom();
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 12; i++) {
            builder.append(chars.charAt(random.nextInt(chars.length())));
        }
        return builder.toString();
    }

    private List<Map<String, String>> parseCsv(MultipartFile file) throws Exception {
        try (CSVParser parser = CSVFormat.DEFAULT
                .withFirstRecordAsHeader()
                .withIgnoreHeaderCase()
                .withTrim()
                .parse(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            Map<String, String> headerMap = normalizeHeaders(parser.getHeaderMap().keySet());
            List<Map<String, String>> rows = new java.util.ArrayList<>();
            for (CSVRecord record : parser) {
                Map<String, String> row = new LinkedHashMap<>();
                for (Map.Entry<String, String> entry : headerMap.entrySet()) {
                    row.put(entry.getValue(), record.get(entry.getKey()));
                }
                rows.add(row);
            }
            return rows;
        }
    }

    private List<Map<String, String>> parseXlsx(MultipartFile file) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook(file.getInputStream())) {
            var sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                return List.of();
            }
            Map<Integer, String> headers = new LinkedHashMap<>();
            DataFormatter formatter = new DataFormatter();
            for (Cell cell : headerRow) {
                String header = normalizeHeaderValue(formatter.formatCellValue(cell));
                if (header != null) {
                    headers.put(cell.getColumnIndex(), header);
                }
            }
            List<Map<String, String>> rows = new java.util.ArrayList<>();
            int lastRow = sheet.getLastRowNum();
            for (int i = 1; i <= lastRow; i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }
                Map<String, String> mapped = new LinkedHashMap<>();
                for (Map.Entry<Integer, String> entry : headers.entrySet()) {
                    Cell cell = row.getCell(entry.getKey());
                    mapped.put(entry.getValue(), cellToString(cell));
                }
                rows.add(mapped);
            }
            return rows;
        }
    }

    private byte[] exportCsv(List<AdminClientExportRow> clients) throws Exception {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            String header = String.join(",",
                    "country", "firstname", "lastname", "email", "phone", "tax_id", "billing_email",
                    "customer_type", "has_credit", "credit_limit", "credit_days", "enabled", "role");
            output.write((header + "\n").getBytes(StandardCharsets.UTF_8));
            for (AdminClientExportRow exportRow : clients) {
                User user = exportRow.user();
                CustomerMarket market = exportRow.market();
                String line = String.join(",",
                        market.getCountry().name(),
                        sanitizeCsv(user.getFirstname()),
                        sanitizeCsv(user.getLastname()),
                        sanitizeCsv(user.getEmail()),
                        sanitizeCsv(user.getPhone()),
                        sanitizeCsv(market.getTaxId()),
                        sanitizeCsv(market.getBillingEmail()),
                        market.getCustomerType() != null ? market.getCustomerType().name() : CustomerType.FINAL.name(),
                        String.valueOf(market.isHasCredit()),
                        market.getCreditLimit() != null ? market.getCreditLimit().toString() : "0",
                        market.getCreditDays() != null ? market.getCreditDays().toString() : "0",
                        String.valueOf(market.isActive()),
                        user.getRole() != null ? user.getRole().name() : "USER");
                output.write((line + "\n").getBytes(StandardCharsets.UTF_8));
            }
            return output.toByteArray();
        }
    }

    private byte[] exportXlsx(List<AdminClientExportRow> clients) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("clientes");
            Row header = sheet.createRow(0);
            String[] headers = {
                    "country", "firstname", "lastname", "email", "phone", "tax_id", "billing_email",
                    "customer_type", "has_credit", "credit_limit", "credit_days", "enabled", "role"
            };
            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
            }
            int rowIndex = 1;
            for (AdminClientExportRow exportRow : clients) {
                User user = exportRow.user();
                CustomerMarket market = exportRow.market();
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(market.getCountry().name());
                row.createCell(1).setCellValue(safe(user.getFirstname()));
                row.createCell(2).setCellValue(safe(user.getLastname()));
                row.createCell(3).setCellValue(safe(user.getEmail()));
                row.createCell(4).setCellValue(safe(user.getPhone()));
                row.createCell(5).setCellValue(safe(market.getTaxId()));
                row.createCell(6).setCellValue(safe(market.getBillingEmail()));
                row.createCell(7).setCellValue(market.getCustomerType() != null ? market.getCustomerType().name() : CustomerType.FINAL.name());
                row.createCell(8).setCellValue(String.valueOf(market.isHasCredit()));
                row.createCell(9).setCellValue(market.getCreditLimit() != null ? market.getCreditLimit().toString() : "0");
                row.createCell(10).setCellValue(market.getCreditDays() != null ? market.getCreditDays().toString() : "0");
                row.createCell(11).setCellValue(String.valueOf(market.isActive()));
                row.createCell(12).setCellValue(user.getRole() != null ? user.getRole().name() : "USER");
            }
            workbook.write(output);
            return output.toByteArray();
        }
    }

    private Role resolveClientRole(String value) {
        if (value == null) {
            return Role.CUSTOMER;
        }
        try {
            Role parsed = Role.valueOf(value.trim().toUpperCase(Locale.ROOT));
            if (CLIENT_ROLES.contains(parsed)) {
                return normalizeRole(parsed);
            }
        } catch (IllegalArgumentException ignored) {
            return Role.CUSTOMER;
        }
        return Role.CUSTOMER;
    }

    private Role normalizeRole(Role role) {
        if (role == null) {
            return null;
        }
        return role;
    }

    private CustomerType resolveCustomerType(CustomerType customerType) {
        return customerType != null ? customerType : CustomerType.FINAL;
    }

    private StorefrontCountry safeCountry(StorefrontCountry country) {
        return country != null ? country : StorefrontCountry.EC;
    }

    private StorefrontCountry parseImportCountry(String value, StorefrontCountry fallback) {
        if (value == null || value.isBlank()) {
            return safeCountry(fallback);
        }
        try {
            return StorefrontCountry.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return safeCountry(fallback);
        }
    }

    private BigDecimal parseBigDecimal(String value, BigDecimal fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (Exception ex) {
            return fallback;
        }
    }

    private Integer parseInteger(String value, Integer fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception ex) {
            return fallback;
        }
    }

    private boolean parseBoolean(String value, boolean fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return normalized.equals("true") || normalized.equals("1") || normalized.equals("yes") || normalized.equals("si");
    }

    private String normalizeValue(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Map<String, String> normalizeHeaders(java.util.Set<String> headers) {
        Map<String, String> normalized = new LinkedHashMap<>();
        for (String header : headers) {
            String normalizedHeader = normalizeHeaderValue(header);
            if (normalizedHeader != null) {
                normalized.put(header, normalizedHeader);
            }
        }
        return normalized;
    }

    private String normalizeHeaderValue(String header) {
        if (header == null) {
            return null;
        }
        String normalized = header.trim().toLowerCase(Locale.ROOT)
                .replace(" ", "_")
                .replace("-", "_");
        return switch (normalized) {
            case "first_name" -> "firstname";
            case "last_name" -> "lastname";
            case "taxid" -> "tax_id";
            case "billingemail" -> "billing_email";
            case "customertype" -> "customer_type";
            case "creditlimit" -> "credit_limit";
            case "creditdays" -> "credit_days";
            case "hascredit" -> "has_credit";
            default -> normalized;
        };
    }

    private String cellToString(Cell cell) {
        if (cell == null) {
            return null;
        }
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf(cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCellFormula();
            default -> null;
        };
    }

    private void trackError(List<String> errors, int rowIndex, String message) {
        if (errors.size() >= 50) {
            return;
        }
        errors.add("Fila " + rowIndex + ": " + message);
    }

    private CustomerType resolveImportedCustomerType(String value) {
        if (value == null || value.isBlank()) {
            return CustomerType.FINAL;
        }
        try {
            return CustomerType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return CustomerType.FINAL;
        }
    }

    private String sanitizeCsv(String value) {
        if (value == null) {
            return "";
        }
        String candidate = value;
        String stripped = candidate.stripLeading();
        if (!stripped.isEmpty() && "=+-@".indexOf(stripped.charAt(0)) >= 0) {
            candidate = "'" + candidate;
        }
        String escaped = candidate.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\n") || escaped.contains("\r")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private void publishCustomerSync(User user) {
        if (user != null && CLIENT_ROLES.contains(user.getRole())) {
            eventPublisher.publishEvent(new CustomerChangedCrmSyncEvent(this, user.getId()));
        }
    }

    private record AdminClientExportRow(User user, CustomerMarket market) {}
}
