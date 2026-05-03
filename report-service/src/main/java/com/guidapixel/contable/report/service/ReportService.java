package com.guidapixel.contable.report.service;

import com.guidapixel.contable.report.client.AuthClient;
import com.guidapixel.contable.report.client.LedgerClient;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    private final LedgerClient ledgerClient;
    private final AuthClient authClient;

    private static final BaseColor HEADER_BG = new BaseColor(30, 64, 175);
    private static final BaseColor HEADER_BG_RED = new BaseColor(220, 38, 38);
    private static final BaseColor ROW_ALT = new BaseColor(248, 250, 252);

    private static com.itextpdf.text.Font iTextFont(int size, int style, BaseColor color) {
        return new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, size, style, color);
    }

    private static com.itextpdf.text.Font iTextFont(int size, int style) {
        return new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, size, style);
    }

    public byte[] generateAccountStatementPdf(Long tenantId, Long clientId, LocalDate from, LocalDate to) {
        String tenantName = authClient.getTenantName(tenantId);
        List<Map<String, Object>> movements = ledgerClient.getAccountStatementData(tenantId, clientId, from.toString(), to.toString());

        String clientName = movements.isEmpty() ? "Cliente" : (String) movements.get(0).get("clientName");
        if (clientName == null) clientName = "Cliente";

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 36, 36, 36, 54);
            PdfWriter.getInstance(document, baos);
            document.open();

            addPdfHeader(document, tenantName, "Estado de Cuenta", HEADER_BG);

            Paragraph info = new Paragraph();
            info.setSpacingAfter(12);
            info.add(new Chunk("Cliente: ", iTextFont(10, com.itextpdf.text.Font.BOLD)));
            info.add(new Chunk(clientName + "\n", iTextFont(12, com.itextpdf.text.Font.NORMAL, new BaseColor(100, 100, 100))));
            info.add(new Chunk("Período: ", iTextFont(10, com.itextpdf.text.Font.BOLD)));
            info.add(new Chunk(from.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + " al " +
                    to.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), iTextFont(12, com.itextpdf.text.Font.NORMAL, new BaseColor(100, 100, 100))));
            document.add(info);

            PdfPTable table = new PdfPTable(7);
            table.setWidthPercentage(100);
            table.setWidths(new int[]{3, 5, 3, 3, 3, 3, 3});

            String[] headers = {"Fecha", "Descripción", "Tipo", "Débito", "Crédito", "Estado", "Vencimiento"};
            addPdfTableHeader(table, headers);

            BigDecimal totalDebit = BigDecimal.ZERO;
            BigDecimal totalCredit = BigDecimal.ZERO;
            boolean alt = false;

            for (Map<String, Object> m : movements) {
                BaseColor bgColor = alt ? ROW_ALT : BaseColor.WHITE;
                alt = !alt;

                String direction = (String) m.get("direction");
                BigDecimal amount = toBigDecimal(m.get("amount"));
                BigDecimal debit = "DEBIT".equals(direction) ? amount : BigDecimal.ZERO;
                BigDecimal credit = "CREDIT".equals(direction) ? amount : BigDecimal.ZERO;
                totalDebit = totalDebit.add(debit);
                totalCredit = totalCredit.add(credit);

                addPdfCell(table, formatCreatedAt(m.get("createdAt")), bgColor);
                addPdfCell(table, (String) m.get("description"), bgColor);
                addPdfCell(table, formatType((String) m.get("type")), bgColor);
                addPdfCell(table, debit.compareTo(BigDecimal.ZERO) > 0 ? "$" + debit.toPlainString() : "-", bgColor);
                addPdfCell(table, credit.compareTo(BigDecimal.ZERO) > 0 ? "$" + credit.toPlainString() : "-", bgColor);
                addPdfCell(table, m.get("paidAt") != null ? "Pagado" : "Pendiente", bgColor);
                addPdfCell(table, formatDueDate(m.get("dueDate")), bgColor);
            }

            document.add(table);

            Paragraph totals = new Paragraph();
            totals.setSpacingBefore(12);
            totals.add(new Chunk("Total Débitos: $", iTextFont(10, com.itextpdf.text.Font.BOLD)));
            totals.add(new Chunk(totalDebit.toPlainString() + "  |  ", iTextFont(10, com.itextpdf.text.Font.BOLD)));
            totals.add(new Chunk("Total Créditos: $", iTextFont(10, com.itextpdf.text.Font.BOLD)));
            totals.add(new Chunk(totalCredit.toPlainString() + "  |  ", iTextFont(10, com.itextpdf.text.Font.BOLD)));
            totals.add(new Chunk("Saldo: $", iTextFont(10, com.itextpdf.text.Font.BOLD)));
            totals.add(new Chunk(totalDebit.subtract(totalCredit).toPlainString(), iTextFont(10, com.itextpdf.text.Font.BOLD)));
            document.add(totals);

            addPdfFooter(document);
            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Error generando PDF de cuenta corriente: {}", e.getMessage());
            throw new RuntimeException("Error generando reporte PDF", e);
        }
    }

    public byte[] generateAccountStatementExcel(Long tenantId, Long clientId, LocalDate from, LocalDate to) {
        String tenantName = authClient.getTenantName(tenantId);
        List<Map<String, Object>> movements = ledgerClient.getAccountStatementData(tenantId, clientId, from.toString(), to.toString());

        String clientName = movements.isEmpty() ? "Cliente" : (String) movements.get(0).get("clientName");
        if (clientName == null) clientName = "Cliente";

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Workbook wb = new XSSFWorkbook();
            Sheet sheet = wb.createSheet("Estado de Cuenta");

            CellStyle headerStyle = wb.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            org.apache.poi.ss.usermodel.Font headerFont = wb.createFont();
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            CellStyle titleStyle = wb.createCellStyle();
            org.apache.poi.ss.usermodel.Font titleFont = wb.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            titleStyle.setFont(titleFont);

            CellStyle infoStyle = wb.createCellStyle();
            org.apache.poi.ss.usermodel.Font infoFont = wb.createFont();
            infoFont.setFontHeightInPoints((short) 11);
            infoStyle.setFont(infoFont);

            CellStyle numberStyle = wb.createCellStyle();
            numberStyle.setDataFormat(wb.createDataFormat().getFormat("$#,##0.00"));

            CellStyle totalStyle = wb.createCellStyle();
            totalStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            totalStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            org.apache.poi.ss.usermodel.Font totalFont = wb.createFont();
            totalFont.setBold(true);
            totalStyle.setFont(totalFont);

            Row row0 = sheet.createRow(0);
            Cell cell0 = row0.createCell(0);
            cell0.setCellValue(tenantName);
            cell0.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 6));

            Row row1 = sheet.createRow(1);
            Cell cell1 = row1.createCell(0);
            cell1.setCellValue("Cliente: " + clientName + " | Período: " +
                    from.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + " al " +
                    to.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            cell1.setCellStyle(infoStyle);
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 6));

            String[] headers = {"Fecha", "Descripción", "Tipo", "Débito", "Crédito", "Estado", "Vencimiento"};
            Row headerRow = sheet.createRow(3);
            for (int i = 0; i < headers.length; i++) {
                Cell hCell = headerRow.createCell(i);
                hCell.setCellValue(headers[i]);
                hCell.setCellStyle(headerStyle);
            }

            int rowNum = 4;
            BigDecimal totalDebit = BigDecimal.ZERO;
            BigDecimal totalCredit = BigDecimal.ZERO;

            for (Map<String, Object> m : movements) {
                Row r = sheet.createRow(rowNum++);
                r.createCell(0).setCellValue(formatCreatedAt(m.get("createdAt")));
                r.createCell(1).setCellValue((String) m.get("description"));
                r.createCell(2).setCellValue(formatType((String) m.get("type")));

                String direction = (String) m.get("direction");
                BigDecimal amount = toBigDecimal(m.get("amount"));
                BigDecimal debit = "DEBIT".equals(direction) ? amount : BigDecimal.ZERO;
                BigDecimal credit = "CREDIT".equals(direction) ? amount : BigDecimal.ZERO;
                totalDebit = totalDebit.add(debit);
                totalCredit = totalCredit.add(credit);

                Cell dCell = r.createCell(3);
                if (debit.compareTo(BigDecimal.ZERO) > 0) { dCell.setCellValue(debit.doubleValue()); dCell.setCellStyle(numberStyle); }

                Cell cCell = r.createCell(4);
                if (credit.compareTo(BigDecimal.ZERO) > 0) { cCell.setCellValue(credit.doubleValue()); cCell.setCellStyle(numberStyle); }

                r.createCell(5).setCellValue(m.get("paidAt") != null ? "Pagado" : "Pendiente");
                r.createCell(6).setCellValue(formatDueDate(m.get("dueDate")));
            }

            Row totalRow = sheet.createRow(rowNum);
            totalRow.createCell(0).setCellValue("TOTALES");
            totalRow.getCell(0).setCellStyle(totalStyle);
            Cell tdCell = totalRow.createCell(3);
            tdCell.setCellValue(totalDebit.doubleValue());
            tdCell.setCellStyle(numberStyle);
            Cell tcCell = totalRow.createCell(4);
            tcCell.setCellValue(totalCredit.doubleValue());
            tcCell.setCellStyle(numberStyle);

            for (int i = 0; i < 7; i++) sheet.autoSizeColumn(i);

            wb.write(baos);
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Error generando Excel de cuenta corriente: {}", e.getMessage());
            throw new RuntimeException("Error generando reporte Excel", e);
        }
    }

    public byte[] generateFeePeriodSummaryPdf(Long tenantId, String yearMonth) {
        String tenantName = authClient.getTenantName(tenantId);
        List<Map<String, Object>> fees = ledgerClient.getActiveFees(tenantId);

        List<Map<String, Object>> feeData = fees.stream().map(fee -> {
            BigDecimal baseAmount = toBigDecimal(fee.get("baseAmount"));
            Map<String, Object> override = ledgerClient.getFeeOverride(tenantId, toLong(fee.get("clientId")), yearMonth);
            BigDecimal overrideAmount = override != null ? toBigDecimal(override.get("overrideAmount")) : null;
            BigDecimal finalAmount = overrideAmount != null ? overrideAmount : baseAmount;

            List<Map<String, Object>> logs = ledgerClient.getFeeGenerationLog(tenantId, toLong(fee.get("clientId")), yearMonth);
            Map<String, Object> genLog = logs.isEmpty() ? null : logs.get(0);

            String estado = "Pendiente";
            LocalDate fechaGeneracion = null;
            if (genLog != null && Boolean.TRUE.equals(genLog.get("success"))) {
                estado = "Generado";
                fechaGeneracion = parseLocalDateTime(genLog.get("generatedAt")).toLocalDate();
            }

            return Map.<String, Object>of(
                    "clientName", fee.get("clientName") != null ? fee.get("clientName") : "Cliente",
                    "baseAmount", baseAmount,
                    "overrideAmount", overrideAmount,
                    "finalAmount", finalAmount,
                    "estado", estado,
                    "fechaGeneracion", fechaGeneracion
            );
        }).collect(Collectors.toList());

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 36, 36, 36, 54);
            PdfWriter.getInstance(document, baos);
            document.open();

            addPdfHeader(document, tenantName, "Resumen de Honorarios - " + yearMonth, HEADER_BG);

            Paragraph info = new Paragraph();
            info.setSpacingAfter(12);
            info.add(new Chunk("Período: ", iTextFont(10, com.itextpdf.text.Font.BOLD)));
            info.add(new Chunk(yearMonth, iTextFont(12, com.itextpdf.text.Font.NORMAL, new BaseColor(100, 100, 100))));
            document.add(info);

            PdfPTable table = new PdfPTable(6);
            table.setWidthPercentage(100);
            table.setWidths(new int[]{4, 3, 3, 3, 3, 3});

            String[] headers = {"Cliente", "Monto Base", "Override", "Monto Final", "Estado", "Fecha Generación"};
            addPdfTableHeader(table, headers);

            BigDecimal totalFinal = BigDecimal.ZERO;
            boolean alt = false;

            for (Map<String, Object> row : feeData) {
                BaseColor bgColor = alt ? ROW_ALT : BaseColor.WHITE;
                alt = !alt;

                BigDecimal finalAmount = (BigDecimal) row.get("finalAmount");
                totalFinal = totalFinal.add(finalAmount);

                addPdfCell(table, (String) row.get("clientName"), bgColor);
                addPdfCell(table, "$" + ((BigDecimal) row.get("baseAmount")).toPlainString(), bgColor);
                addPdfCell(table, row.get("overrideAmount") != null ? "$" + ((BigDecimal) row.get("overrideAmount")).toPlainString() : "-", bgColor);
                addPdfCell(table, "$" + finalAmount.toPlainString(), bgColor);
                addPdfCell(table, (String) row.get("estado"), bgColor);
                LocalDate fg = (LocalDate) row.get("fechaGeneracion");
                addPdfCell(table, fg != null ? fg.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "-", bgColor);
            }

            document.add(table);

            Paragraph totals = new Paragraph();
            totals.setSpacingBefore(12);
            totals.add(new Chunk("Total Honorarios: $", iTextFont(10, com.itextpdf.text.Font.BOLD)));
            totals.add(new Chunk(totalFinal.toPlainString(), iTextFont(10, com.itextpdf.text.Font.BOLD)));
            document.add(totals);

            addPdfFooter(document);
            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Error generando PDF de honorarios: {}", e.getMessage());
            throw new RuntimeException("Error generando reporte PDF", e);
        }
    }

    public byte[] generateFeePeriodSummaryExcel(Long tenantId, String yearMonth) {
        String tenantName = authClient.getTenantName(tenantId);
        List<Map<String, Object>> fees = ledgerClient.getActiveFees(tenantId);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Workbook wb = new XSSFWorkbook();
            Sheet sheet = wb.createSheet("Honorarios " + yearMonth);

            CellStyle headerStyle = wb.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            org.apache.poi.ss.usermodel.Font headerFont = wb.createFont();
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            CellStyle titleStyle = wb.createCellStyle();
            org.apache.poi.ss.usermodel.Font titleFont = wb.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            titleStyle.setFont(titleFont);

            CellStyle infoStyle = wb.createCellStyle();
            org.apache.poi.ss.usermodel.Font infoFont = wb.createFont();
            infoFont.setFontHeightInPoints((short) 11);
            infoStyle.setFont(infoFont);

            CellStyle numberStyle = wb.createCellStyle();
            numberStyle.setDataFormat(wb.createDataFormat().getFormat("$#,##0.00"));

            CellStyle totalStyle = wb.createCellStyle();
            totalStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            totalStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            org.apache.poi.ss.usermodel.Font totalFont = wb.createFont();
            totalFont.setBold(true);
            totalStyle.setFont(totalFont);

            Row row0 = sheet.createRow(0);
            Cell cell0 = row0.createCell(0);
            cell0.setCellValue(tenantName);
            cell0.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 5));

            Row row1 = sheet.createRow(1);
            Cell cell1 = row1.createCell(0);
            cell1.setCellValue("Período: " + yearMonth);
            cell1.setCellStyle(infoStyle);
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 5));

            String[] headers = {"Cliente", "Monto Base", "Override", "Monto Final", "Estado", "Fecha Generación"};
            Row headerRow = sheet.createRow(3);
            for (int i = 0; i < headers.length; i++) {
                Cell hCell = headerRow.createCell(i);
                hCell.setCellValue(headers[i]);
                hCell.setCellStyle(headerStyle);
            }

            int rowNum = 4;
            BigDecimal totalFinal = BigDecimal.ZERO;

            for (Map<String, Object> fee : fees) {
                BigDecimal baseAmount = toBigDecimal(fee.get("baseAmount"));
                Map<String, Object> override = ledgerClient.getFeeOverride(tenantId, toLong(fee.get("clientId")), yearMonth);
                BigDecimal overrideAmount = override != null ? toBigDecimal(override.get("overrideAmount")) : null;
                BigDecimal finalAmount = overrideAmount != null ? overrideAmount : baseAmount;
                totalFinal = totalFinal.add(finalAmount);

                List<Map<String, Object>> logs = ledgerClient.getFeeGenerationLog(tenantId, toLong(fee.get("clientId")), yearMonth);
                Map<String, Object> genLog = logs.isEmpty() ? null : logs.get(0);
                String estado = (genLog != null && Boolean.TRUE.equals(genLog.get("success"))) ? "Generado" : "Pendiente";
                LocalDate fechaGen = genLog != null ? parseLocalDateTime(genLog.get("generatedAt")).toLocalDate() : null;

                Row r = sheet.createRow(rowNum++);
                r.createCell(0).setCellValue(fee.get("clientName") != null ? (String) fee.get("clientName") : "Cliente");

                Cell bCell = r.createCell(1);
                bCell.setCellValue(baseAmount.doubleValue());
                bCell.setCellStyle(numberStyle);

                Cell oCell = r.createCell(2);
                if (overrideAmount != null) { oCell.setCellValue(overrideAmount.doubleValue()); oCell.setCellStyle(numberStyle); }

                Cell fCell = r.createCell(3);
                fCell.setCellValue(finalAmount.doubleValue());
                fCell.setCellStyle(numberStyle);

                r.createCell(4).setCellValue(estado);
                r.createCell(5).setCellValue(fechaGen != null ? fechaGen.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "");
            }

            Row totalRow = sheet.createRow(rowNum);
            totalRow.createCell(0).setCellValue("TOTAL");
            totalRow.getCell(0).setCellStyle(totalStyle);
            Cell tfCell = totalRow.createCell(3);
            tfCell.setCellValue(totalFinal.doubleValue());
            tfCell.setCellStyle(numberStyle);

            for (int i = 0; i < 6; i++) sheet.autoSizeColumn(i);

            wb.write(baos);
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Error generando Excel de honorarios: {}", e.getMessage());
            throw new RuntimeException("Error generando reporte Excel", e);
        }
    }

    public byte[] generateIncomeSummaryPdf(Long tenantId, LocalDate from, LocalDate to) {
        String tenantName = authClient.getTenantName(tenantId);
        List<Map<String, Object>> movements = ledgerClient.getAllMovements(tenantId, from.toString(), to.toString());

        Map<String, BigDecimal> monthlyIncome = movements.stream()
                .filter(m -> "CREDIT".equals(m.get("direction")) && m.get("paidAt") != null)
                .collect(Collectors.groupingBy(
                        m -> YearMonth.from(parseLocalDateTime(m.get("createdAt"))).toString(),
                        Collectors.reducing(BigDecimal.ZERO, m -> toBigDecimal(m.get("amount")), BigDecimal::add)
                ));

        BigDecimal totalPending = movements.stream()
                .filter(m -> "DEBIT".equals(m.get("direction")) && m.get("paidAt") == null)
                .map(m -> toBigDecimal(m.get("amount")))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long activeClients = movements.stream()
                .map(m -> m.get("clientId"))
                .distinct()
                .count();

        List<Map.Entry<String, BigDecimal>> sortedMonths = monthlyIncome.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toList());

        BigDecimal maxIncome = sortedMonths.stream()
                .map(Map.Entry::getValue)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ONE);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 36, 36, 36, 54);
            PdfWriter.getInstance(document, baos);
            document.open();

            addPdfHeader(document, tenantName, "Resumen de Ingresos", HEADER_BG_RED);

            Paragraph period = new Paragraph();
            period.setSpacingAfter(16);
            period.add(new Chunk("Período: ", iTextFont(10, com.itextpdf.text.Font.BOLD)));
            period.add(new Chunk(from.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + " al " +
                    to.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), iTextFont(12, com.itextpdf.text.Font.NORMAL, new BaseColor(100, 100, 100))));
            document.add(period);

            PdfPTable metricsTable = new PdfPTable(3);
            metricsTable.setWidthPercentage(100);
            metricsTable.setWidths(new int[]{1, 1, 1});
            metricsTable.setSpacingAfter(20);

            addMetricCell(metricsTable, "Total Cobrado", "$" + sortedMonths.stream()
                    .map(Map.Entry::getValue).reduce(BigDecimal.ZERO, BigDecimal::add).toPlainString());
            addMetricCell(metricsTable, "Pendiente de Cobro", "$" + totalPending.toPlainString());
            addMetricCell(metricsTable, "Clientes Activos", String.valueOf(activeClients));

            document.add(metricsTable);

            if (!sortedMonths.isEmpty()) {
                Paragraph chartTitle = new Paragraph();
                chartTitle.setSpacingAfter(8);
                chartTitle.add(new Chunk("Ingresos por Mes", iTextFont(12, com.itextpdf.text.Font.BOLD)));
                document.add(chartTitle);

                PdfPTable chartTable = new PdfPTable(2);
                chartTable.setWidthPercentage(100);
                chartTable.setWidths(new int[]{2, 5});

                for (Map.Entry<String, BigDecimal> entry : sortedMonths) {
                    addPdfCell(chartTable, entry.getKey(), BaseColor.WHITE);

                    float barWidth = entry.getValue().divide(maxIncome, 2, java.math.RoundingMode.HALF_UP).floatValue() * 100;
                    if (barWidth < 2) barWidth = 2;

                    PdfPCell barCell = new PdfPCell();
                    barCell.setFixedHeight(18);
                    barCell.setBorder(Rectangle.NO_BORDER);
                    barCell.setPadding(2);

                    PdfPTable innerTable = new PdfPTable(1);
                    innerTable.setWidthPercentage(barWidth);
                    innerTable.setHorizontalAlignment(Element.ALIGN_LEFT);
                    PdfPCell fillCell = new PdfPCell(new Phrase("$" + entry.getValue().toPlainString(),
                            iTextFont(8, com.itextpdf.text.Font.BOLD, BaseColor.WHITE)));
                    fillCell.setBackgroundColor(HEADER_BG_RED);
                    fillCell.setBorder(Rectangle.NO_BORDER);
                    fillCell.setPadding(3);
                    innerTable.addCell(fillCell);

                    barCell.addElement(innerTable);
                    chartTable.addCell(barCell);
                }

                document.add(chartTable);
            }

            addPdfFooter(document);
            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Error generando PDF de resumen de ingresos: {}", e.getMessage());
            throw new RuntimeException("Error generando reporte PDF", e);
        }
    }

    private void addPdfHeader(Document document, String tenantName, String subtitle, BaseColor bgColor) throws DocumentException {
        PdfPTable headerTable = new PdfPTable(1);
        headerTable.setWidthPercentage(100);
        PdfPCell headerCell = new PdfPCell(new Phrase(tenantName, iTextFont(18, com.itextpdf.text.Font.BOLD, BaseColor.WHITE)));
        headerCell.setBackgroundColor(bgColor);
        headerCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        headerCell.setPadding(14);
        headerCell.setBorder(Rectangle.NO_BORDER);
        headerTable.addCell(headerCell);
        document.add(headerTable);

        Paragraph subtitleParagraph = new Paragraph(subtitle, iTextFont(14, com.itextpdf.text.Font.BOLD, new BaseColor(50, 50, 50)));
        subtitleParagraph.setAlignment(Element.ALIGN_CENTER);
        subtitleParagraph.setSpacingBefore(8);
        subtitleParagraph.setSpacingAfter(16);
        document.add(subtitleParagraph);
    }

    private void addPdfTableHeader(PdfPTable table, String[] headers) {
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, iTextFont(9, com.itextpdf.text.Font.BOLD, BaseColor.WHITE)));
            cell.setBackgroundColor(HEADER_BG);
            cell.setPadding(6);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cell);
        }
    }

    private void addPdfCell(PdfPTable table, String text, BaseColor bgColor) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "", iTextFont(9, com.itextpdf.text.Font.NORMAL)));
        cell.setBackgroundColor(bgColor);
        cell.setPadding(5);
        table.addCell(cell);
    }

    private void addMetricCell(PdfPTable table, String label, String value) throws DocumentException {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(new BaseColor(248, 250, 252));
        cell.setPadding(12);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);

        PdfPTable inner = new PdfPTable(1);
        inner.setWidthPercentage(100);
        Phrase labelPhrase = new Phrase(label, iTextFont(9, com.itextpdf.text.Font.NORMAL, new BaseColor(100, 100, 100)));
        Phrase valuePhrase = new Phrase(value, iTextFont(16, com.itextpdf.text.Font.BOLD, HEADER_BG));

        PdfPCell labelCell = new PdfPCell(labelPhrase);
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        labelCell.setPaddingBottom(4);
        inner.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(valuePhrase);
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        inner.addCell(valueCell);

        cell.addElement(inner);
        table.addCell(cell);
    }

    private void addPdfFooter(Document document) throws DocumentException {
        Paragraph footer = new Paragraph();
        footer.setSpacingBefore(24);
        footer.setAlignment(Element.ALIGN_CENTER);
        footer.add(new Chunk("Generado por Guida Contable | guidapixel.com | " +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")), iTextFont(8, com.itextpdf.text.Font.NORMAL, new BaseColor(150, 150, 150))));
        document.add(footer);
    }

    private String formatCreatedAt(Object createdAt) {
        if (createdAt == null) return "-";
        LocalDateTime dt = parseLocalDateTime(createdAt);
        return dt != null ? dt.toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "-";
    }

    private String formatDueDate(Object dueDate) {
        if (dueDate == null) return "-";
        if (dueDate instanceof java.time.LocalDate) {
            return ((LocalDate) dueDate).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        }
        return "-";
    }

    private String formatType(String type) {
        if (type == null) return "-";
        return switch (type) {
            case "CARGO_FACTURA" -> "Factura";
            case "CARGO_MANUAL" -> "Cargo";
            case "PAGO_EFECTIVO" -> "Pago Efectivo";
            case "PAGO_TRANSFERENCIA" -> "Transferencia";
            case "PAGO_OTRO" -> "Otro Pago";
            default -> type;
        };
    }

    private BigDecimal toBigDecimal(Object val) {
        if (val == null) return BigDecimal.ZERO;
        if (val instanceof BigDecimal) return (BigDecimal) val;
        if (val instanceof Number) return new BigDecimal(val.toString());
        try { return new BigDecimal(val.toString()); } catch (Exception e) { return BigDecimal.ZERO; }
    }

    private Long toLong(Object val) {
        if (val == null) return 0L;
        if (val instanceof Number) return ((Number) val).longValue();
        try { return Long.parseLong(val.toString()); } catch (Exception e) { return 0L; }
    }

    private LocalDateTime parseLocalDateTime(Object val) {
        if (val == null) return null;
        if (val instanceof LocalDateTime) return (LocalDateTime) val;
        try { return LocalDateTime.parse(val.toString()); } catch (Exception e) { return null; }
    }
}
