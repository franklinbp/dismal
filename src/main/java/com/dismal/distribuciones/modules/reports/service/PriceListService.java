package com.dismal.distribuciones.modules.reports.service;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.dismal.distribuciones.exception.BadRequestException;
import com.dismal.distribuciones.modules.reports.dto.PriceListColumnSelection;
import com.dismal.distribuciones.modules.pricing.repository.PriceListItemRepository;
import com.dismal.distribuciones.modules.reports.dto.PriceListItemResponse;
import com.dismal.distribuciones.modules.reports.dto.PriceListResponse;
import com.dismal.distribuciones.modules.store.domain.Software;
import com.dismal.distribuciones.modules.store.repository.LicenseRepository;
import com.dismal.distribuciones.modules.store.repository.SoftwareRepository;
import com.dismal.distribuciones.modules.store.repository.StockSummaryView;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PriceListService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final SoftwareRepository softwareRepository;
    private final LicenseRepository licenseRepository;
    private final PriceListItemRepository priceListItemRepository;

    @Transactional(readOnly = true)
    public PriceListResponse buildPriceList() {
        List<PriceListItemResponse> items = loadItems();
        String title = buildTitle();
        PriceListColumnSelection selection = PriceListColumnSelection.all();
        String text = buildText(items, title, selection);
        String html = buildHtml(items, title, selection);
        return new PriceListResponse(LocalDateTime.now(), items, text, html);
    }

    @Transactional(readOnly = true)
    public PriceListResponse buildPriceList(String countryCode, com.dismal.distribuciones.modules.security.domain.CustomerType customerType) {
        return buildPriceList();
    }

    @Transactional(readOnly = true)
    public PriceListResponse buildPriceList(PriceListColumnSelection selection) {
        PriceListColumnSelection resolvedSelection = normalizeSelection(selection);
        List<PriceListItemResponse> items = loadItems();
        String title = buildTitle();
        String text = buildText(items, title, resolvedSelection);
        String html = buildHtml(items, title, resolvedSelection);
        return new PriceListResponse(LocalDateTime.now(), items, text, html);
    }

    @Transactional(readOnly = true)
    public byte[] buildPdf(List<PriceListItemResponse> items) {
        return buildPdf(items, PriceListColumnSelection.all());
    }

    @Transactional(readOnly = true)
    public byte[] buildPdf(List<PriceListItemResponse> items, PriceListColumnSelection selection) {
        try {
            PriceListColumnSelection resolvedSelection = normalizeSelection(selection);
            Document document = new Document(PageSize.A4, 36, 36, 36, 36);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            PdfWriter.getInstance(document, output);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Font subtitleFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
            Font noteFont = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
            Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 10);

            document.add(new Paragraph(buildTitle(), titleFont));
            document.add(new Paragraph("Actualizado: " + DATE_FORMAT.format(LocalDateTime.now()), subtitleFont));
            document.add(new Paragraph("Valores expresados en USD. La disponibilidad corresponde al stock actual y puede cambiar sin previo aviso.", noteFont));
            document.add(new Paragraph(" "));

            int columnCount = 2
                    + (resolvedSelection.includeEcFinalPrice() ? 1 : 0)
                    + (resolvedSelection.includeEcDistributorPrice() ? 1 : 0)
                    + (resolvedSelection.includePeFinalPrice() ? 1 : 0)
                    + (resolvedSelection.includePeDistributorPrice() ? 1 : 0)
                    + (resolvedSelection.includeStock() ? 1 : 0);
            PdfPTable table = new PdfPTable(columnCount);
            table.setWidthPercentage(100);
            table.setWidths(buildPdfWidths(resolvedSelection));

            addHeaderCell(table, "Producto", headerFont);
            addHeaderCell(table, "Plataforma", headerFont);
            if (resolvedSelection.includeEcFinalPrice()) {
                addHeaderCell(table, "EC Final", headerFont);
            }
            if (resolvedSelection.includeEcDistributorPrice()) {
                addHeaderCell(table, "EC Dist.", headerFont);
            }
            if (resolvedSelection.includePeFinalPrice()) {
                addHeaderCell(table, "PE Final", headerFont);
            }
            if (resolvedSelection.includePeDistributorPrice()) {
                addHeaderCell(table, "PE Dist.", headerFont);
            }
            if (resolvedSelection.includeStock()) {
                addHeaderCell(table, "Stock", headerFont);
            }

            for (PriceListItemResponse item : items) {
                table.addCell(createTextCell(item.name(), cellFont, Element.ALIGN_LEFT));
                table.addCell(createTextCell(safe(item.platform()), cellFont, Element.ALIGN_LEFT));
                if (resolvedSelection.includeEcFinalPrice()) {
                    table.addCell(createTextCell(formatPrice(item.ecFinalPrice()), cellFont, Element.ALIGN_RIGHT));
                }
                if (resolvedSelection.includeEcDistributorPrice()) {
                    table.addCell(createTextCell(formatPrice(item.ecDistributorPrice()), cellFont, Element.ALIGN_RIGHT));
                }
                if (resolvedSelection.includePeFinalPrice()) {
                    table.addCell(createTextCell(formatPrice(item.peFinalPrice()), cellFont, Element.ALIGN_RIGHT));
                }
                if (resolvedSelection.includePeDistributorPrice()) {
                    table.addCell(createTextCell(formatPrice(item.peDistributorPrice()), cellFont, Element.ALIGN_RIGHT));
                }
                if (resolvedSelection.includeStock()) {
                    table.addCell(createTextCell(String.valueOf(item.availableStock()), cellFont, Element.ALIGN_RIGHT));
                }
            }

            document.add(table);
            document.close();
            return output.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to generate PDF", ex);
        }
    }

    @Transactional(readOnly = true)
    public byte[] buildXlsx(List<PriceListItemResponse> items) {
        return buildXlsx(items, PriceListColumnSelection.all());
    }

    @Transactional(readOnly = true)
    public byte[] buildXlsx(List<PriceListItemResponse> items, PriceListColumnSelection selection) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PriceListColumnSelection resolvedSelection = normalizeSelection(selection);
            var sheet = workbook.createSheet("Lista de Precios");
            Row header = sheet.createRow(0);
            int column = 0;
            header.createCell(column++).setCellValue("Producto");
            header.createCell(column++).setCellValue("Plataforma");
            if (resolvedSelection.includeEcFinalPrice()) {
                header.createCell(column++).setCellValue("EC Final");
            }
            if (resolvedSelection.includeEcDistributorPrice()) {
                header.createCell(column++).setCellValue("EC Distribuidor");
            }
            if (resolvedSelection.includePeFinalPrice()) {
                header.createCell(column++).setCellValue("PE Final");
            }
            if (resolvedSelection.includePeDistributorPrice()) {
                header.createCell(column++).setCellValue("PE Distribuidor");
            }
            if (resolvedSelection.includeStock()) {
                header.createCell(column).setCellValue("Stock");
            }

            CellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font font = workbook.createFont();
            font.setBold(true);
            font.setUnderline(org.apache.poi.ss.usermodel.Font.U_SINGLE);
            headerStyle.setFont(font);
            headerStyle.setAlignment(HorizontalAlignment.LEFT);
            for (int i = 0; i < header.getLastCellNum(); i++) {
                header.getCell(i).setCellStyle(headerStyle);
            }

            int rowIndex = 1;
            for (PriceListItemResponse item : items) {
                Row row = sheet.createRow(rowIndex++);
                int rowColumn = 0;
                row.createCell(rowColumn++).setCellValue(item.name());
                row.createCell(rowColumn++).setCellValue(safe(item.platform()));
                if (resolvedSelection.includeEcFinalPrice()) {
                    row.createCell(rowColumn++).setCellValue(formatPrice(item.ecFinalPrice()));
                }
                if (resolvedSelection.includeEcDistributorPrice()) {
                    row.createCell(rowColumn++).setCellValue(formatPrice(item.ecDistributorPrice()));
                }
                if (resolvedSelection.includePeFinalPrice()) {
                    row.createCell(rowColumn++).setCellValue(formatPrice(item.peFinalPrice()));
                }
                if (resolvedSelection.includePeDistributorPrice()) {
                    row.createCell(rowColumn++).setCellValue(formatPrice(item.peDistributorPrice()));
                }
                if (resolvedSelection.includeStock()) {
                    row.createCell(rowColumn).setCellValue(item.availableStock());
                }
            }

            for (int i = 0; i < header.getLastCellNum(); i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(output);
            return output.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to generate XLSX", ex);
        }
    }

    public String buildWhatsappUrl(String phone, String message) {
        if (phone == null || phone.isBlank()) {
            return null;
        }
        String normalized = phone.replaceAll("[^0-9]", "");
        if (normalized.isBlank()) {
            return null;
        }
        String encoded = URLEncoder.encode(message, StandardCharsets.UTF_8);
        return "https://wa.me/" + normalized + "?text=" + encoded;
    }

    private List<PriceListItemResponse> loadItems() {
        Map<java.util.UUID, Long> stockBySoftware = new HashMap<>();
        for (StockSummaryView view : licenseRepository.getStockSummary()) {
            long available = view.getAvailableCount() != null ? view.getAvailableCount() : 0L;
            stockBySoftware.put(view.getSoftwareId(), available);
        }

        return softwareRepository.findAll().stream()
                .sorted(Comparator.comparing(Software::getName, String.CASE_INSENSITIVE_ORDER))
                .map(software -> new PriceListItemResponse(
                        software.getId(),
                        software.getName(),
                        software.getPlatform(),
                        resolveItemPrice(software, "EC", "FINAL"),
                        resolveItemPrice(software, "EC", "FINAL"),
                        resolveItemPrice(software, "EC", "DISTRIBUTOR"),
                        resolveItemPrice(software, "PE", "FINAL"),
                        resolveItemPrice(software, "PE", "DISTRIBUTOR"),
                        stockBySoftware.getOrDefault(software.getId(), 0L)
                ))
                .toList();
    }

    private BigDecimal resolveItemPrice(Software software, String countryCode, String customerType) {
        com.dismal.distribuciones.modules.pricing.domain.PriceListType priceListType =
                com.dismal.distribuciones.modules.pricing.domain.PriceListType.from(
                        countryCode,
                        com.dismal.distribuciones.modules.security.domain.CustomerType.valueOf(customerType)
                );
        return priceListItemRepository.findPriceByTypeAndSoftwareId(priceListType, software.getId())
                .or(() -> "DISTRIBUTOR".equals(customerType)
                        ? priceListItemRepository.findPriceByTypeAndSoftwareId(
                                com.dismal.distribuciones.modules.pricing.domain.PriceListType.from(
                                        countryCode,
                                        com.dismal.distribuciones.modules.security.domain.CustomerType.FINAL
                                ),
                                software.getId()
                        )
                        : java.util.Optional.empty())
                .orElse(software.getPrice());
    }

    private String buildText(List<PriceListItemResponse> items, String title, PriceListColumnSelection selection) {
        StringBuilder builder = new StringBuilder();
        builder.append(resolveTextTitle(selection)).append("\n\n");

        Map<String, List<PriceListItemResponse>> grouped = items.stream()
                .sorted(Comparator
                        .comparing((PriceListItemResponse item) -> categoryOrder(normalizeCategory(item.platform(), item.name())))
                        .thenComparing(PriceListItemResponse::name, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.groupingBy(
                        item -> normalizeCategory(item.platform(), item.name()),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        grouped.forEach((category, categoryItems) -> {
            builder.append("- ").append(category).append("\n\n");
            for (PriceListItemResponse item : categoryItems) {
                builder.append(item.name())
                        .append(": ")
                        .append(formatPrice(resolveTextPrice(item, selection)))
                        .append("\n");
            }
            builder.append("\n");
        });

        if (selection.includeStock()) {
            builder.append("Stock sujeto a disponibilidad.\n");
        }
        builder.append("Para pedidos o cotizaciones, escribenos y te ayudamos de inmediato.");
        return builder.toString();
    }

    private String resolveTextTitle(PriceListColumnSelection selection) {
        if (selection.includeEcDistributorPrice() || selection.includePeDistributorPrice()) {
            return "Lista de Precios al por mayor:";
        }
        return "Lista de Precios:";
    }

    private BigDecimal resolveTextPrice(PriceListItemResponse item, PriceListColumnSelection selection) {
        if (selection.includeEcDistributorPrice()) {
            return item.ecDistributorPrice();
        }
        if (selection.includePeDistributorPrice()) {
            return item.peDistributorPrice();
        }
        if (selection.includeEcFinalPrice()) {
            return item.ecFinalPrice();
        }
        if (selection.includePeFinalPrice()) {
            return item.peFinalPrice();
        }
        return item.price();
    }

    private String normalizeCategory(String platform, String name) {
        String raw = (platform != null && !platform.isBlank() ? platform : inferCategoryFromName(name)).trim();
        String lowered = raw.toLowerCase();
        if (lowered.contains("antivirus") || lowered.contains("eset") || lowered.contains("kaspersky")) {
            return "Antivirus";
        }
        if (lowered.contains("dise") || lowered.contains("autodesk") || lowered.contains("canva")) {
            return "Diseno Grafico";
        }
        if (lowered.contains("office") || lowered.contains("microsoft")) {
            return "Microsoft Office";
        }
        if (lowered.contains("windows")) {
            return "Windows";
        }
        return raw.isBlank() || "-".equals(raw) ? "Otros" : raw;
    }

    private String inferCategoryFromName(String name) {
        return name == null ? "Otros" : name;
    }

    private int categoryOrder(String category) {
        return switch (category) {
            case "Antivirus" -> 1;
            case "Diseno Grafico" -> 2;
            case "Microsoft Office" -> 3;
            case "Windows" -> 4;
            default -> 50;
        };
    }

    private String buildHtml(List<PriceListItemResponse> items, String title, PriceListColumnSelection selection) {
        StringBuilder builder = new StringBuilder();
        builder.append("<html><body style=\"font-family:Arial,sans-serif;color:#0f172a;background:#f8fafc;margin:0;padding:24px;\">");
        builder.append("<div style=\"max-width:960px;margin:0 auto;\">");
        builder.append("<div style=\"background:#ffffff;border:1px solid #e2e8f0;border-radius:20px;padding:24px;box-shadow:0 12px 30px rgba(15,23,42,0.06);\">");
        builder.append("<p style=\"margin:0 0 6px 0;font-size:12px;letter-spacing:0.18em;text-transform:uppercase;color:#64748b;\">Dismal</p>");
        builder.append("<h2 style=\"margin:0 0 8px 0;\">").append(escape(title)).append("</h2>");
        builder.append("<p style=\"margin:0 0 4px 0;color:#475569;\">Actualizado: ")
                .append(DATE_FORMAT.format(LocalDateTime.now()))
                .append("</p>");
        builder.append("<p style=\"margin:0 0 6px 0;color:#475569;\">Hola, te compartimos nuestra lista oficial de precios actualizada.</p>");
        builder.append("<p style=\"margin:0 0 16px 0;color:#475569;\">Valores expresados en USD. La disponibilidad corresponde al stock actual y puede cambiar sin previo aviso.</p>");
        builder.append("<table border=\"1\" cellpadding=\"8\" cellspacing=\"0\" style=\"border-collapse:collapse;width:100%;background:#ffffff;\">");
        builder.append("<tr style=\"background:#f1f5f9;\">")
                .append("<th align=\"left\">Producto</th>")
                .append("<th align=\"left\">Plataforma</th>");
        if (selection.includeEcFinalPrice()) {
            builder.append("<th align=\"right\">EC Final</th>");
        }
        if (selection.includeEcDistributorPrice()) {
            builder.append("<th align=\"right\">EC Distribuidor</th>");
        }
        if (selection.includePeFinalPrice()) {
            builder.append("<th align=\"right\">PE Final</th>");
        }
        if (selection.includePeDistributorPrice()) {
            builder.append("<th align=\"right\">PE Distribuidor</th>");
        }
        if (selection.includeStock()) {
            builder.append("<th align=\"right\">Stock</th>");
        }
        builder.append("</tr>");
        for (PriceListItemResponse item : items) {
            builder.append("<tr>");
            builder.append("<td>").append(escape(item.name())).append("</td>");
            builder.append("<td>").append(escape(safe(item.platform()))).append("</td>");
            if (selection.includeEcFinalPrice()) {
                builder.append("<td align=\"right\">").append(escape(formatPrice(item.ecFinalPrice()))).append("</td>");
            }
            if (selection.includeEcDistributorPrice()) {
                builder.append("<td align=\"right\">").append(escape(formatPrice(item.ecDistributorPrice()))).append("</td>");
            }
            if (selection.includePeFinalPrice()) {
                builder.append("<td align=\"right\">").append(escape(formatPrice(item.peFinalPrice()))).append("</td>");
            }
            if (selection.includePeDistributorPrice()) {
                builder.append("<td align=\"right\">").append(escape(formatPrice(item.peDistributorPrice()))).append("</td>");
            }
            if (selection.includeStock()) {
                builder.append("<td align=\"right\">").append(item.availableStock()).append("</td>");
            }
            builder.append("</tr>");
        }
        builder.append("</table>");
        builder.append("<p style=\"margin:16px 0 0 0;color:#475569;\">Si deseas una recomendacion, pedido o cotizacion, responde este mensaje y te ayudamos de inmediato.</p>");
        builder.append("</div>");
        builder.append("</div>");
        builder.append("</body></html>");
        return builder.toString();
    }

    private void addHeaderCell(PdfPTable table, String label, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(label, font));
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        cell.setBackgroundColor(new java.awt.Color(241, 245, 249));
        table.addCell(cell);
    }

    private PdfPCell createTextCell(String value, Font font, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(value, font));
        cell.setHorizontalAlignment(alignment);
        return cell;
    }

    private String formatPrice(BigDecimal price) {
        if (price == null) {
            return "$0.00";
        }
        return "$" + price.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "-" : value.trim();
    }

    private String buildTitle() {
        return "Lista Comercial de Precios - Dismal";
    }

    private PriceListColumnSelection normalizeSelection(PriceListColumnSelection selection) {
        PriceListColumnSelection resolved = selection != null ? selection : PriceListColumnSelection.all();
        if (!resolved.hasAnyPrice()) {
            throw new BadRequestException("Selecciona al menos un precio para generar la lista.");
        }
        return resolved;
    }

    private float[] buildPdfWidths(PriceListColumnSelection selection) {
        java.util.List<Float> widths = new java.util.ArrayList<>();
        widths.add(3.0f);
        widths.add(1.3f);
        if (selection.includeEcFinalPrice()) {
            widths.add(1.2f);
        }
        if (selection.includeEcDistributorPrice()) {
            widths.add(1.4f);
        }
        if (selection.includePeFinalPrice()) {
            widths.add(1.2f);
        }
        if (selection.includePeDistributorPrice()) {
            widths.add(1.4f);
        }
        if (selection.includeStock()) {
            widths.add(1.0f);
        }
        float[] result = new float[widths.size()];
        for (int i = 0; i < widths.size(); i++) {
            result[i] = widths.get(i);
        }
        return result;
    }

    private String escape(String value) {
        return value == null ? "" : value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
