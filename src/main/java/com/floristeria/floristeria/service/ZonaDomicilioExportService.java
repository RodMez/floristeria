package com.floristeria.floristeria.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.floristeria.floristeria.dto.ZonaDomicilioExcelDTO;
import com.floristeria.floristeria.entity.ZonaDomicilio;
import com.floristeria.floristeria.repository.ZonaDomicilioRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ZonaDomicilioExportService {

    private final ZonaDomicilioRepository zonaDomicilioRepository;

    @Transactional(readOnly = true)
    public byte[] exportarZonasExcel(Integer sedeId) throws IOException {
        List<ZonaDomicilio> zonas = sedeId != null
                ? zonaDomicilioRepository.findBySedeIdWithSede(sedeId)
                : zonaDomicilioRepository.findAllWithSede();

        List<ZonaDomicilioExcelDTO> dtos = zonas.stream()
                .map(this::mapearDTO)
                .collect(Collectors.toList());

        return generarExcel(dtos, sedeId);
    }

    private ZonaDomicilioExcelDTO mapearDTO(ZonaDomicilio z) {
        String sedeNombre = z.getSede() != null ? z.getSede().getNombre() : "";
        String ciudad = z.getSede() != null ? z.getSede().getCiudad() : "";
        String barrio = z.getBarrio() != null ? z.getBarrio() : "";
        String estado = Boolean.TRUE.equals(z.getExcluido()) ? "Excluida" : "Activa";
        return ZonaDomicilioExcelDTO.builder()
                .sedeNombre(sedeNombre)
                .ciudad(ciudad != null ? ciudad : "")
                .localidad(z.getLocalidad() != null ? z.getLocalidad() : "")
                .barrio(barrio)
                .precio(z.getPrecio())
                .estado(estado)
                .build();
    }

    private byte[] generarExcel(List<ZonaDomicilioExcelDTO> zonas, Integer sedeIdFiltro) throws IOException {
        XSSFWorkbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            XSSFSheet sheet = workbook.createSheet("Zonas");

            CellStyle headerStyle = crearEstiloCabecera(workbook);
            CellStyle cellStyle = crearEstiloCelda(workbook);
            CellStyle moneyStyle = crearEstiloDinero(workbook);
            CellStyle titleStyle = crearEstiloTitulo(workbook);
            CellStyle kpiLabelStyle = crearEstiloKpiLabel(workbook);
            CellStyle kpiValueStyle = crearEstiloKpiValor(workbook);

            crearKpis(sheet, zonas, titleStyle, kpiLabelStyle, kpiValueStyle, moneyStyle, sedeIdFiltro);
            crearHojaZonas(sheet, zonas, headerStyle, cellStyle, moneyStyle);

            // Filtros y congelado
            if (!zonas.isEmpty()) {
                sheet.setAutoFilter(new CellRangeAddress(5, 5, 0, 5));
            }
            sheet.createFreezePane(0, 6);

            // Config impresión
            sheet.setFitToPage(true);
            sheet.getPrintSetup().setFitWidth((short) 1);
            sheet.getPrintSetup().setFitHeight((short) 0);

            workbook.write(out);
            return out.toByteArray();
        } finally {
            workbook.close();
        }
    }

    private void crearKpis(XSSFSheet sheet, List<ZonaDomicilioExcelDTO> zonas,
                           CellStyle titleStyle, CellStyle kpiLabelStyle,
                           CellStyle kpiValueStyle, CellStyle moneyStyle, Integer sedeIdFiltro) {
        XSSFRow titleRow = sheet.createRow(0);
        createCell(titleRow, 0, "ZONAS DE DOMICILIO", titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 5));

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        XSSFRow subRow = sheet.createRow(1);
        String filtroTxt = sedeIdFiltro != null ? "Sede ID: " + sedeIdFiltro : "Todas las sedes";
        createCell(subRow, 0, "Generado: " + LocalDateTime.now().format(fmt) + "  |  Filtro: " + filtroTxt, null);
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 5));

        long total = zonas.size();
        long excluidas = zonas.stream().filter(z -> "Excluida".equals(z.getEstado())).count();
        long activas = total - excluidas;

        BigDecimal totalPrecio = zonas.stream()
                .map(ZonaDomicilioExcelDTO::getPrecio)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal promedio = total > 0
                ? totalPrecio.divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        long sedesIncluidas = zonas.stream().map(ZonaDomicilioExcelDTO::getSedeNombre).filter(s -> s != null && !s.isBlank()).distinct().count();

        XSSFRow kpiRow = sheet.createRow(2);
        createCell(kpiRow, 0, "Total Zonas:", kpiLabelStyle);
        createNumericCell(kpiRow, 1, (double) total, kpiValueStyle);
        createCell(kpiRow, 2, "Activas:", kpiLabelStyle);
        createNumericCell(kpiRow, 3, (double) activas, kpiValueStyle);
        createCell(kpiRow, 4, "Excluidas:", kpiLabelStyle);
        createNumericCell(kpiRow, 5, (double) excluidas, kpiValueStyle);

        XSSFRow kpiRow2 = sheet.createRow(3);
        createCell(kpiRow2, 0, "Precio Promedio:", kpiLabelStyle);
        createNumericCell(kpiRow2, 1, promedio.doubleValue(), moneyStyle);
        if (sedeIdFiltro == null) {
            createCell(kpiRow2, 2, "Sedes incluidas:", kpiLabelStyle);
            createNumericCell(kpiRow2, 3, (double) sedesIncluidas, kpiValueStyle);
        }

        int[] anchos = {4500, 3500, 4500, 3500, 4500, 3500};
        for (int i = 0; i < anchos.length; i++) {
            sheet.setColumnWidth(i, anchos[i]);
        }
    }

    private void crearHojaZonas(XSSFSheet sheet, List<ZonaDomicilioExcelDTO> zonas,
                                CellStyle headerStyle, CellStyle cellStyle, CellStyle moneyStyle) {
        String[] cabeceras = {"Sede", "Ciudad", "Localidad", "Barrio", "Precio", "Estado"};

        XSSFRow headerRow = sheet.createRow(5);
        for (int i = 0; i < cabeceras.length; i++) {
            headerRow.createCell(i).setCellValue(cabeceras[i]);
            headerRow.getCell(i).setCellStyle(headerStyle);
        }

        int rowNum = 6;
        for (ZonaDomicilioExcelDTO z : zonas) {
            XSSFRow row = sheet.createRow(rowNum++);
            createCell(row, 0, z.getSedeNombre(), cellStyle);
            createCell(row, 1, z.getCiudad(), cellStyle);
            createCell(row, 2, z.getLocalidad(), cellStyle);
            createCell(row, 3, z.getBarrio(), cellStyle);
            createNumericCell(row, 4, z.getPrecio() != null ? z.getPrecio().doubleValue() : 0, moneyStyle);
            createCell(row, 5, z.getEstado(), cellStyle);
        }
    }

    private void createCell(XSSFRow row, int col, String value, CellStyle style) {
        XSSFCell cell = row.createCell(col);
        cell.setCellValue(value != null ? value : "");
        if (style != null) {
            cell.setCellStyle(style);
        }
    }

    private void createNumericCell(XSSFRow row, int col, double value, CellStyle style) {
        XSSFCell cell = row.createCell(col);
        cell.setCellValue(value);
        if (style != null) {
            cell.setCellStyle(style);
        }
    }

    private CellStyle crearEstiloCabecera(XSSFWorkbook workbook) {
        XSSFCellStyle style = workbook.createCellStyle();
        XSSFFont font = workbook.createFont();
        font.setBold(true);
        font.setColor(new XSSFColor(new java.awt.Color(0x44, 0x40, 0x3C), null));
        style.setFont(font);
        style.setFillForegroundColor(new XSSFColor(new java.awt.Color(0xE5, 0xBE, 0x6F), null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setWrapText(true);
        return style;
    }

    private CellStyle crearEstiloCelda(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle crearEstiloDinero(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.cloneStyleFrom(crearEstiloCelda(workbook));
        style.setAlignment(HorizontalAlignment.RIGHT);
        DataFormat fmt = workbook.createDataFormat();
        style.setDataFormat(fmt.getFormat("\"$\" #,##0.00"));
        return style;
    }

    private CellStyle crearEstiloTitulo(XSSFWorkbook workbook) {
        XSSFCellStyle style = workbook.createCellStyle();
        XSSFFont font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 16);
        font.setColor(new XSSFColor(new byte[]{(byte) 0xD4, (byte) 0xAF, (byte) 0x5C}, null));
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle crearEstiloKpiLabel(XSSFWorkbook workbook) {
        XSSFCellStyle style = workbook.createCellStyle();
        style.cloneStyleFrom(crearEstiloCelda(workbook));
        XSSFFont font = workbook.createFont();
        font.setBold(true);
        font.setColor(new XSSFColor(new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF}, null));
        style.setFont(font);
        style.setFillForegroundColor(new XSSFColor(new byte[]{(byte) 0xEA, (byte) 0xC3, (byte) 0xBD}, null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private CellStyle crearEstiloKpiValor(XSSFWorkbook workbook) {
        XSSFCellStyle style = workbook.createCellStyle();
        XSSFFont font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 14);
        font.setColor(new XSSFColor(new byte[]{(byte) 0xD4, (byte) 0xAF, (byte) 0x5C}, null));
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.RIGHT);
        return style;
    }
}
