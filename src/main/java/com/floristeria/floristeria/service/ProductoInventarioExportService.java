package com.floristeria.floristeria.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xddf.usermodel.chart.AxisPosition;
import org.apache.poi.xddf.usermodel.chart.AxisTickLabelPosition;
import org.apache.poi.xddf.usermodel.chart.BarDirection;
import org.apache.poi.xddf.usermodel.chart.ChartTypes;
import org.apache.poi.xddf.usermodel.chart.LegendPosition;
import org.apache.poi.xddf.usermodel.chart.XDDFBarChartData;
import org.apache.poi.xddf.usermodel.chart.XDDFCategoryAxis;
import org.apache.poi.xddf.usermodel.chart.XDDFChartData;
import org.apache.poi.xddf.usermodel.chart.XDDFChartLegend;
import org.apache.poi.xddf.usermodel.chart.XDDFDataSourcesFactory;
import org.apache.poi.xddf.usermodel.chart.XDDFDoughnutChartData;
import org.apache.poi.xddf.usermodel.chart.XDDFValueAxis;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFChart;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTBarChart;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTBarSer;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTCrossBetween;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTDPt;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTDoughnutChart;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTPieSer;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTPlotArea;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTValAx;
import org.openxmlformats.schemas.drawingml.x2006.chart.STCrossBetween;
import org.openxmlformats.schemas.drawingml.x2006.main.CTSRgbColor;
import org.openxmlformats.schemas.drawingml.x2006.main.CTSolidColorFillProperties;
import org.springframework.stereotype.Service;

import com.floristeria.floristeria.dto.ProductoInventarioExcelDTO;
import com.floristeria.floristeria.entity.Categoria;
import com.floristeria.floristeria.entity.Inventario;
import com.floristeria.floristeria.entity.Producto;
import com.floristeria.floristeria.repository.InventarioRepository;
import com.floristeria.floristeria.repository.ProductoRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductoInventarioExportService {

    private final ProductoRepository productoRepository;
    private final InventarioRepository inventarioRepository;

    public byte[] exportarProductosInventarioExcel(Integer sedeId) throws IOException {
        List<Producto> productos = productoRepository.findAll();
        List<Inventario> inventarios = sedeId != null
            ? inventarioRepository.findActiveBySedeId(sedeId)
            : inventarioRepository.findAllActive();

        List<ProductoInventarioExcelDTO> dtoList = new ArrayList<>();
        for (Inventario inv : inventarios) {
            Producto p = inv.getProducto();
            if (p == null) continue;
            String cats = p.getCategorias() != null
                ? p.getCategorias().stream().map(Categoria::getNombre).collect(Collectors.joining(", "))
                : "";
            BigDecimal precioFinal = inv.getDescuentoPorcentaje() != null && inv.getDescuentoPorcentaje() > 0
                ? inv.getPrecio().subtract(inv.getPrecio().multiply(BigDecimal.valueOf(inv.getDescuentoPorcentaje()))
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP))
                : inv.getPrecio();
            dtoList.add(ProductoInventarioExcelDTO.builder()
                .productoId(p.getId())
                .sku(p.getSku())
                .nombre(p.getNombre())
                .descripcion(p.getDescripcion())
                .categorias(cats)
                .activoGlobal(p.getActivoGlobal())
                .creadoEn(p.getCreadoEn())
                .sedeNombre(inv.getSede() != null ? inv.getSede().getNombre() : "")
                .precio(inv.getPrecio())
                .stock(inv.getStock())
                .disponible(inv.getDisponible())
                .descuentoPorcentaje(inv.getDescuentoPorcentaje())
                .precioFinal(precioFinal)
                .build());
        }

        return generarExcel(productos, dtoList);
    }

    private byte[] generarExcel(List<Producto> productos, List<ProductoInventarioExcelDTO> inventarios) throws IOException {
        XSSFWorkbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            XSSFSheet dashboardSheet = workbook.createSheet("Dashboard");
            XSSFSheet productosSheet = workbook.createSheet("Productos");
            XSSFSheet inventarioSheet = workbook.createSheet("Inventario");
            XSSFSheet dataSheet = workbook.createSheet("_Datos");
            dataSheet.setSelected(false);

            CellStyle headerStyle = crearEstiloCabecera(workbook);
            CellStyle cellStyle = crearEstiloCelda(workbook);
            CellStyle fechaStyle = crearEstiloFecha(workbook);
            CellStyle moneyStyle = crearEstiloDinero(workbook);
            CellStyle titleStyle = crearEstiloTitulo(workbook);
            CellStyle kpiLabelStyle = crearEstiloKpiLabel(workbook);
            CellStyle kpiValueStyle = crearEstiloKpiValor(workbook);

            llenarDataSheet(dataSheet, productos, inventarios);

            crearKPIs(dashboardSheet, productos, inventarios, titleStyle, kpiLabelStyle, kpiValueStyle, moneyStyle);

            crearGraficoBarrasVertical(dashboardSheet, dataSheet, "Valor del Inventario por Sede", "ValorInventarioSede", 0, 5, 5, 19);
            crearGraficoDonut(dashboardSheet, dataSheet, "Productos con Descuento por Sede", "DescuentoSede", 6, 5, 11, 19);
            crearGraficoBarrasHorizontal(dashboardSheet, dataSheet, "Productos Disponibles por Sede", "ProdDisponibles", 0, 21, 11, 38);

            crearHojaProductos(productosSheet, inventarios, headerStyle, cellStyle, fechaStyle);
            crearHojaInventario(inventarioSheet, inventarios, headerStyle, cellStyle, moneyStyle);

            workbook.write(out);
            return out.toByteArray();
        } finally {
            workbook.close();
        }
    }

    private void llenarDataSheet(XSSFSheet sheet, List<Producto> productos,
                                   List<ProductoInventarioExcelDTO> inventarios) {
        int row = 0;

        // Bloque 1: Valor del Inventario por Sede
        Map<String, BigDecimal> valorPorSede = inventarios.stream()
            .collect(Collectors.groupingBy(
                ProductoInventarioExcelDTO::getSedeNombre,
                Collectors.reducing(BigDecimal.ZERO,
                    i -> i.getPrecio() != null && i.getStock() != null
                        ? i.getPrecio().multiply(BigDecimal.valueOf(i.getStock()))
                        : BigDecimal.ZERO,
                    BigDecimal::add)));

        XSSFRow sedeHeader = sheet.createRow(row++);
        sedeHeader.createCell(0).setCellValue("ValorInventarioSede");
        sedeHeader.createCell(1).setCellValue("Valor");
        for (Map.Entry<String, BigDecimal> entry : valorPorSede.entrySet()) {
            XSSFRow dataRow = sheet.createRow(row++);
            dataRow.createCell(0).setCellValue(entry.getKey());
            dataRow.createCell(1).setCellValue(entry.getValue().doubleValue());
        }
        row++;

        // Bloque 2: Productos con Descuento por Sede
        Map<String, Long> descuentoPorSede = inventarios.stream()
            .filter(i -> i.getDescuentoPorcentaje() != null && i.getDescuentoPorcentaje() > 0)
            .collect(Collectors.groupingBy(
                ProductoInventarioExcelDTO::getSedeNombre,
                Collectors.counting()));

        XSSFRow descHeader = sheet.createRow(row++);
        descHeader.createCell(0).setCellValue("DescuentoSede");
        descHeader.createCell(1).setCellValue("Productos");
        for (Map.Entry<String, Long> entry : descuentoPorSede.entrySet()) {
            XSSFRow dataRow = sheet.createRow(row++);
            dataRow.createCell(0).setCellValue(entry.getKey());
            dataRow.createCell(1).setCellValue((double) entry.getValue());
        }
        row++;

        // Bloque 3: Productos Disponibles por Sede
        Map<String, Long> prodDisponiblesPorSede = inventarios.stream()
            .filter(i -> Boolean.TRUE.equals(i.getDisponible()) && i.getStock() != null && i.getStock() > 0)
            .collect(Collectors.groupingBy(
                ProductoInventarioExcelDTO::getSedeNombre,
                Collectors.counting()));
        List<Map.Entry<String, Long>> topSedes = prodDisponiblesPorSede.entrySet().stream()
            .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
            .limit(10)
            .toList();

        XSSFRow stockHeader = sheet.createRow(row++);
        stockHeader.createCell(0).setCellValue("ProdDisponibles");
        stockHeader.createCell(1).setCellValue("Productos");
        for (Map.Entry<String, Long> entry : topSedes) {
            XSSFRow dataRow = sheet.createRow(row++);
            dataRow.createCell(0).setCellValue(entry.getKey());
            dataRow.createCell(1).setCellValue((double) entry.getValue());
        }
    }

    private int getDataRowCount(XSSFSheet sheet, int startRow) {
        int count = 0;
        for (int i = startRow; ; i++) {
            var r = sheet.getRow(i);
            if (r == null) break;
            var cell = r.getCell(0);
            if (cell == null || cell.getStringCellValue() == null || cell.getStringCellValue().isEmpty()) break;
            count++;
        }
        return count;
    }

    private int encontrarFilaConCabecera(XSSFSheet sheet, String cabecera) {
        for (int i = 0; i <= 100; i++) {
            var r = sheet.getRow(i);
            if (r != null) {
                var cell = r.getCell(0);
                if (cell != null) {
                    try {
                        if (cabecera.equals(cell.getStringCellValue())) return i;
                    } catch (Exception e) { }
                }
            }
        }
        return -1;
    }

    private void crearKPIs(XSSFSheet sheet, List<Producto> productos,
                           List<ProductoInventarioExcelDTO> inventarios,
                           CellStyle titleStyle, CellStyle kpiLabelStyle,
                           CellStyle kpiValueStyle, CellStyle moneyStyle) {
        XSSFRow titleRow = sheet.createRow(0);
        createCell(titleRow, 0, "RESUMEN DE PRODUCTOS E INVENTARIO", titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 10));

        long totalProductos = productos.size();
        long enDescuento = inventarios.stream()
            .filter(i -> i.getDescuentoPorcentaje() != null && i.getDescuentoPorcentaje() > 0)
            .count();
        int stockTotal = inventarios.stream()
            .mapToInt(i -> i.getStock() != null ? i.getStock() : 0)
            .sum();
        BigDecimal valorInventario = inventarios.stream()
            .map(i -> i.getPrecio() != null && i.getStock() != null
                ? i.getPrecio().multiply(BigDecimal.valueOf(i.getStock())) : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        XSSFRow kpiRow1 = sheet.createRow(2);
        createCell(kpiRow1, 0, "Productos Únicos:", kpiLabelStyle);
        createNumericCell(kpiRow1, 1, (double) totalProductos, kpiValueStyle);
        createCell(kpiRow1, 3, "En Descuento:", kpiLabelStyle);
        createNumericCell(kpiRow1, 4, (double) enDescuento, kpiValueStyle);
        createCell(kpiRow1, 6, "Stock Total:", kpiLabelStyle);
        createNumericCell(kpiRow1, 7, (double) stockTotal, kpiValueStyle);
        createCell(kpiRow1, 9, "Valor Inventario:", kpiLabelStyle);
        createNumericCell(kpiRow1, 10, valorInventario.doubleValue(), kpiValueStyle);

        long productosTotales = inventarios.size();
        XSSFRow kpiRow2 = sheet.createRow(3);
        createCell(kpiRow2, 0, "Productos Totales:", kpiLabelStyle);
        createNumericCell(kpiRow2, 1, (double) productosTotales, kpiValueStyle);

        int[] anchos = {4000, 3500, 1000, 4000, 3500, 1000, 4000, 3500, 1000, 4000, 3500};
        for (int i = 0; i < anchos.length; i++) {
            sheet.setColumnWidth(i, anchos[i]);
        }
    }

    private void crearGraficoBarrasVertical(XSSFSheet sheet, XSSFSheet dataSheet,
                                             String titulo, String cabecera,
                                             int col1, int row1, int col2, int row2) {
        XSSFDrawing drawing = sheet.createDrawingPatriarch();
        XSSFClientAnchor anchor = new XSSFClientAnchor(0, 0, 0, 0, col1, row1, col2, row2);
        XSSFChart chart = drawing.createChart(anchor);
        chart.setTitleText(titulo);
        chart.setTitleOverlay(false);

        XDDFChartLegend legend = chart.getOrAddLegend();
        legend.setPosition(LegendPosition.BOTTOM);

        XDDFCategoryAxis bottomAxis = chart.createCategoryAxis(AxisPosition.BOTTOM);
        bottomAxis.setTickLabelPosition(AxisTickLabelPosition.NONE);
        XDDFValueAxis leftAxis = chart.createValueAxis(AxisPosition.LEFT);
        leftAxis.setTitle("Cantidad");

        CTPlotArea ctPlotArea = chart.getCTChart().getPlotArea();
        for (CTValAx ctValAx : ctPlotArea.getValAxList()) {
            CTCrossBetween crossBetween = ctValAx.isSetCrossBetween()
                ? ctValAx.getCrossBetween()
                : ctValAx.addNewCrossBetween();
            crossBetween.setVal(STCrossBetween.BETWEEN);
        }

        XDDFBarChartData data = (XDDFBarChartData) chart.createData(ChartTypes.BAR, bottomAxis, leftAxis);
        data.setBarDirection(BarDirection.COL);

        int dataStart = encontrarFilaConCabecera(dataSheet, cabecera) + 1;
        int dataEnd = dataStart + getDataRowCount(dataSheet, dataStart) - 1;

        XDDFChartData.Series series = data.addSeries(
                XDDFDataSourcesFactory.fromStringCellRange(dataSheet, new CellRangeAddress(dataStart, dataEnd, 0, 0)),
                XDDFDataSourcesFactory.fromNumericCellRange(dataSheet, new CellRangeAddress(dataStart, dataEnd, 1, 1)));
        series.setTitle(cabecera, null);

        chart.plot(data);
        int numPuntos = dataEnd - dataStart + 1;
        aplicarColoresMarca(chart, numPuntos);
    }

    private void crearGraficoDonut(XSSFSheet sheet, XSSFSheet dataSheet,
                                    String titulo, String cabecera,
                                    int col1, int row1, int col2, int row2) {
        XSSFDrawing drawing = sheet.createDrawingPatriarch();
        XSSFClientAnchor anchor = new XSSFClientAnchor(0, 0, 0, 0, col1, row1, col2, row2);
        XSSFChart chart = drawing.createChart(anchor);
        chart.setTitleText(titulo);
        chart.setTitleOverlay(false);

        XDDFChartLegend legend = chart.getOrAddLegend();
        legend.setPosition(LegendPosition.BOTTOM);

        XDDFDoughnutChartData data = (XDDFDoughnutChartData) chart.createData(ChartTypes.DOUGHNUT, null, null);
        data.setVaryColors(true);

        int dataStart = encontrarFilaConCabecera(dataSheet, cabecera) + 1;
        int dataEnd = dataStart + getDataRowCount(dataSheet, dataStart) - 1;

        XDDFChartData.Series series = data.addSeries(
                XDDFDataSourcesFactory.fromStringCellRange(dataSheet, new CellRangeAddress(dataStart, dataEnd, 0, 0)),
                XDDFDataSourcesFactory.fromNumericCellRange(dataSheet, new CellRangeAddress(dataStart, dataEnd, 1, 1)));
        series.setTitle(cabecera, null);

        chart.plot(data);
        int numPuntos = dataEnd - dataStart + 1;
        aplicarColoresMarca(chart, numPuntos);
    }

    private void crearGraficoBarrasHorizontal(XSSFSheet sheet, XSSFSheet dataSheet,
                                               String titulo, String cabecera,
                                               int col1, int row1, int col2, int row2) {
        XSSFDrawing drawing = sheet.createDrawingPatriarch();
        XSSFClientAnchor anchor = new XSSFClientAnchor(0, 0, 0, 0, col1, row1, col2, row2);
        XSSFChart chart = drawing.createChart(anchor);
        chart.setTitleText(titulo);
        chart.setTitleOverlay(false);

        XDDFChartLegend legend = chart.getOrAddLegend();
        legend.setPosition(LegendPosition.BOTTOM);

        XDDFCategoryAxis bottomAxis = chart.createCategoryAxis(AxisPosition.BOTTOM);
        XDDFValueAxis leftAxis = chart.createValueAxis(AxisPosition.LEFT);
        leftAxis.setTitle("Cantidad");

        CTPlotArea ctPlotArea = chart.getCTChart().getPlotArea();
        for (CTValAx ctValAx : ctPlotArea.getValAxList()) {
            CTCrossBetween crossBetween = ctValAx.isSetCrossBetween()
                ? ctValAx.getCrossBetween()
                : ctValAx.addNewCrossBetween();
            crossBetween.setVal(STCrossBetween.BETWEEN);
        }

        XDDFBarChartData data = (XDDFBarChartData) chart.createData(ChartTypes.BAR, bottomAxis, leftAxis);
        data.setBarDirection(BarDirection.BAR);

        int dataStart = encontrarFilaConCabecera(dataSheet, cabecera) + 1;
        int dataEnd = dataStart + getDataRowCount(dataSheet, dataStart) - 1;

        XDDFChartData.Series series = data.addSeries(
                XDDFDataSourcesFactory.fromStringCellRange(dataSheet, new CellRangeAddress(dataStart, dataEnd, 0, 0)),
                XDDFDataSourcesFactory.fromNumericCellRange(dataSheet, new CellRangeAddress(dataStart, dataEnd, 1, 1)));
        series.setTitle("Stock", null);

        chart.plot(data);
        int numPuntos = dataEnd - dataStart + 1;
        aplicarColoresMarca(chart, numPuntos);
    }

    private void aplicarColoresMarca(XSSFChart chart, int numPuntos) {
        String[] palette = generarPaleta(numPuntos);
        CTPlotArea area = chart.getCTChart().getPlotArea();
        for (CTBarChart bc : area.getBarChartList()) {
            for (CTBarSer ser : bc.getSerList()) {
                if (ser.isSetSpPr()) ser.unsetSpPr();
                for (int i = 0; i < numPuntos; i++) {
                    CTDPt dp = ser.addNewDPt();
                    dp.addNewIdx().setVal(i);
                    dp.addNewSpPr().addNewSolidFill().addNewSrgbClr().setVal(hexToBytes(palette[i]));
                }
            }
        }
        for (CTDoughnutChart dc : area.getDoughnutChartList()) {
            for (CTPieSer ser : dc.getSerList()) {
                if (ser.isSetSpPr()) ser.unsetSpPr();
                for (int i = 0; i < numPuntos; i++) {
                    CTDPt dp = ser.addNewDPt();
                    dp.addNewIdx().setVal(i);
                    dp.addNewSpPr().addNewSolidFill().addNewSrgbClr().setVal(hexToBytes(palette[i]));
                }
            }
        }
    }

    private String[] generarPaleta(int n) {
        if (n <= 0) return new String[0];
        int[][] anchors = {
            {229, 190, 111}, {212, 175, 92}, {234, 195, 189}, {122, 138, 115}
        };
        String[] paleta = new String[n];
        int segments = anchors.length;
        for (int i = 0; i < n; i++) {
            double pos = (double) i / n * segments;
            int seg = (int) Math.floor(pos);
            double local = pos - seg;
            int aIdx = seg % segments;
            int bIdx = (seg + 1) % segments;
            int r = (int) Math.round(anchors[aIdx][0] + (anchors[bIdx][0] - anchors[aIdx][0]) * local);
            int g = (int) Math.round(anchors[aIdx][1] + (anchors[bIdx][1] - anchors[aIdx][1]) * local);
            int blu = (int) Math.round(anchors[aIdx][2] + (anchors[bIdx][2] - anchors[aIdx][2]) * local);
            paleta[i] = String.format("%02X%02X%02X", r, g, blu);
        }
        return paleta;
    }

    private byte[] hexToBytes(String hex) {
        byte[] bytes = new byte[3];
        for (int i = 0; i < 3; i++) {
            bytes[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
        }
        return bytes;
    }

    private void crearHojaProductos(XSSFSheet sheet, List<ProductoInventarioExcelDTO> inventarios,
                                     CellStyle headerStyle, CellStyle cellStyle, CellStyle fechaStyle) {
        String[] cabeceras = {"SKU", "Nombre", "Descripción", "Categorías", "Estado", "Fecha Creación"};

        XSSFRow headerRow = sheet.createRow(0);
        for (int i = 0; i < cabeceras.length; i++) {
            headerRow.createCell(i).setCellValue(cabeceras[i]);
            headerRow.getCell(i).setCellStyle(headerStyle);
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        Map<Integer, ProductoInventarioExcelDTO> productosUnicos = new LinkedHashMap<>();
        for (ProductoInventarioExcelDTO dto : inventarios) {
            productosUnicos.putIfAbsent(dto.getProductoId(), dto);
        }

        int rowNum = 1;
        for (ProductoInventarioExcelDTO dto : productosUnicos.values()) {
            XSSFRow row = sheet.createRow(rowNum++);
            createCell(row, 0, dto.getSku(), cellStyle);
            createCell(row, 1, dto.getNombre(), cellStyle);
            createCell(row, 2, dto.getDescripcion(), cellStyle);
            createCell(row, 3, dto.getCategorias(), cellStyle);
            createCell(row, 4, Boolean.TRUE.equals(dto.getActivoGlobal()) ? "Activo" : "Inactivo", cellStyle);
            createCell(row, 5, dto.getCreadoEn() != null ? dto.getCreadoEn().format(formatter) : "", fechaStyle);
        }

        for (int i = 0; i < cabeceras.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void crearHojaInventario(XSSFSheet sheet, List<ProductoInventarioExcelDTO> inventarios,
                                      CellStyle headerStyle, CellStyle cellStyle, CellStyle moneyStyle) {
        String[] cabeceras = {"Producto", "SKU", "Sede", "Precio", "Stock", "Disponible", "Descuento %", "Precio Final"};

        XSSFRow headerRow = sheet.createRow(0);
        for (int i = 0; i < cabeceras.length; i++) {
            headerRow.createCell(i).setCellValue(cabeceras[i]);
            headerRow.getCell(i).setCellStyle(headerStyle);
        }

        int rowNum = 1;
        for (ProductoInventarioExcelDTO dto : inventarios) {
            XSSFRow row = sheet.createRow(rowNum++);
            createCell(row, 0, dto.getNombre(), cellStyle);
            createCell(row, 1, dto.getSku(), cellStyle);
            createCell(row, 2, dto.getSedeNombre(), cellStyle);
            createNumericCell(row, 3, dto.getPrecio() != null ? dto.getPrecio().doubleValue() : 0, moneyStyle);
            createNumericCell(row, 4, dto.getStock() != null ? (double) dto.getStock() : 0, cellStyle);
            createCell(row, 5, Boolean.TRUE.equals(dto.getDisponible()) ? "Sí" : "No", cellStyle);
            createNumericCell(row, 6, dto.getDescuentoPorcentaje() != null ? (double) dto.getDescuentoPorcentaje() : 0, cellStyle);
            createNumericCell(row, 7, dto.getPrecioFinal() != null ? dto.getPrecioFinal().doubleValue() : 0, moneyStyle);
        }

        for (int i = 0; i < cabeceras.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // UTILIDADES DE CELDAS
    // ═══════════════════════════════════════════════════════════════

    private void createCell(XSSFRow row, int col, String value, CellStyle style) {
        XSSFCell cell = row.createCell(col);
        cell.setCellValue(value != null ? value : "");
        cell.setCellStyle(style);
    }

    private void createNumericCell(XSSFRow row, int col, double value, CellStyle style) {
        XSSFCell cell = row.createCell(col);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    // ═══════════════════════════════════════════════════════════════
    // ESTILOS DE CELDA (copiar de PedidoExportService)
    // ═══════════════════════════════════════════════════════════════

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

    private CellStyle crearEstiloFecha(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.cloneStyleFrom(crearEstiloCelda(workbook));
        return style;
    }

    private CellStyle crearEstiloDinero(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.cloneStyleFrom(crearEstiloCelda(workbook));
        style.setAlignment(HorizontalAlignment.RIGHT);
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
