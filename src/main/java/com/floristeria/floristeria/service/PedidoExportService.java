package com.floristeria.floristeria.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
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
import org.apache.poi.ss.usermodel.IndexedColors;
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
import org.apache.poi.xddf.usermodel.chart.XDDFNumericalDataSource;
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
import org.openxmlformats.schemas.drawingml.x2006.main.CTShapeProperties;
import org.openxmlformats.schemas.drawingml.x2006.main.CTSolidColorFillProperties;
import org.springframework.stereotype.Service;

import com.floristeria.floristeria.dto.DetallePedidoExcelDTO;
import com.floristeria.floristeria.dto.PedidoExcelDTO;
import com.floristeria.floristeria.entity.DetallePedido;
import com.floristeria.floristeria.entity.Direccion;
import com.floristeria.floristeria.entity.EstadoPedido;
import com.floristeria.floristeria.entity.Pedido;
import com.floristeria.floristeria.repository.PedidoRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PedidoExportService {

    private final PedidoRepository pedidoRepository;

    public byte[] exportarPedidosExcel(Integer sedeId, EstadoPedido estado,
                                        LocalDateTime fechaInicio, LocalDateTime fechaFin) throws IOException {
        List<Pedido> pedidos = pedidoRepository.findAllPedidosParaExportar();

        if (sedeId != null) {
            pedidos = pedidos.stream()
                .filter(p -> p.getSede() != null && p.getSede().getId().equals(sedeId))
                .toList();
        }
        if (estado != null) {
            pedidos = pedidos.stream()
                .filter(p -> p.getEstado() == estado)
                .toList();
        }
        if (fechaInicio != null) {
            pedidos = pedidos.stream()
                .filter(p -> p.getCreadoEn() != null && p.getCreadoEn().isAfter(fechaInicio))
                .toList();
        }
        if (fechaFin != null) {
            pedidos = pedidos.stream()
                .filter(p -> p.getCreadoEn() != null && p.getCreadoEn().isBefore(fechaFin))
                .toList();
        }

        List<PedidoExcelDTO> pedidosDTO = new ArrayList<>();
        List<DetallePedidoExcelDTO> detallesDTO = new ArrayList<>();

        for (Pedido pedido : pedidos) {
            pedidosDTO.add(mapearPedidoDTO(pedido));
            if (pedido.getDetalles() != null) {
                for (DetallePedido detalle : pedido.getDetalles()) {
                    detallesDTO.add(mapearDetalleDTO(pedido.getCodigo(), detalle));
                }
            }
        }

        return generarExcel(pedidosDTO, detallesDTO);
    }

    private PedidoExcelDTO mapearPedidoDTO(Pedido pedido) {
        String direccionEntrega = "";
        if (pedido.getDireccion() != null) {
            Direccion dir = pedido.getDireccion();
            StringBuilder sb = new StringBuilder();
            if (dir.getAlias() != null) sb.append(dir.getAlias());
            if (dir.getDireccion() != null) {
                if (!sb.isEmpty()) sb.append(" - ");
                sb.append(dir.getDireccion());
            }
            if (dir.getCiudad() != null) {
                if (!sb.isEmpty()) sb.append(", ");
                sb.append(dir.getCiudad());
            }
            if (dir.getDetalles() != null) {
                if (!sb.isEmpty()) sb.append(" (");
                sb.append(dir.getDetalles());
                sb.append(")");
            }
            direccionEntrega = sb.toString();
        }

        return PedidoExcelDTO.builder()
                .codigo(pedido.getCodigo())
                .creadoEn(pedido.getCreadoEn())
                .clienteNombre(pedido.getCliente() != null ? pedido.getCliente().getNombre() : "")
                .clienteTelefono(pedido.getCliente() != null ? pedido.getCliente().getTelefono() : "")
                .clienteEmail(pedido.getCliente() != null ? pedido.getCliente().getEmail() : "")
                .sedeNombre(pedido.getSedeNombre() != null ? pedido.getSedeNombre() :
                            (pedido.getSede() != null ? pedido.getSede().getNombre() : ""))
                .total(pedido.getTotal())
                .costoEnvio(pedido.getCostoEnvio() != null ? pedido.getCostoEnvio() : BigDecimal.ZERO)
                .estado(pedido.getEstado() != null ? pedido.getEstado().name() : "")
                .metodoPago(pedido.getMetodoPago() != null ? pedido.getMetodoPago() : "")
                .referenciaPago(pedido.getReferenciaPago() != null ? pedido.getReferenciaPago() : "")
                .direccionEntrega(direccionEntrega)
                .notasEntrega(pedido.getNotasEntrega() != null ? pedido.getNotasEntrega() : "")
                .build();
    }

    private DetallePedidoExcelDTO mapearDetalleDTO(String pedidoCodigo, DetallePedido detalle) {
        BigDecimal subtotal = BigDecimal.ZERO;
        if (detalle.getCantidad() != null && detalle.getPrecioUnitario() != null) {
            subtotal = detalle.getPrecioUnitario().multiply(BigDecimal.valueOf(detalle.getCantidad()));
        }

        return DetallePedidoExcelDTO.builder()
                .pedidoCodigo(pedidoCodigo)
                .productoNombre(detalle.getProducto() != null ? detalle.getProducto().getNombre() : "")
                .productoSku(detalle.getProducto() != null ? detalle.getProducto().getSku() : "")
                .cantidad(detalle.getCantidad())
                .precioUnitario(detalle.getPrecioUnitario())
                .subtotal(subtotal)
                .notaPersonalizacion(detalle.getNotaPersonalizacion() != null ? detalle.getNotaPersonalizacion() : "")
                .build();
    }

    private byte[] generarExcel(List<PedidoExcelDTO> pedidos, List<DetallePedidoExcelDTO> detalles) throws IOException {
        XSSFWorkbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            XSSFSheet dashboardSheet = workbook.createSheet("Dashboard");
            XSSFSheet pedidosSheet = workbook.createSheet("Pedidos");
            XSSFSheet detallesSheet = workbook.createSheet("Detalles");
            XSSFSheet dataSheet = workbook.createSheet("_Datos");
            dataSheet.setSelected(false);

            workbook.setSheetHidden(3, true);

            CellStyle headerStyle = crearEstiloCabecera(workbook);
            CellStyle cellStyle = crearEstiloCelda(workbook);
            CellStyle fechaStyle = crearEstiloFecha(workbook);
            CellStyle moneyStyle = crearEstiloDinero(workbook);
            CellStyle titleStyle = crearEstiloTitulo(workbook);
            CellStyle subtitleStyle = crearEstiloSubtitulo(workbook);
            CellStyle kpiLabelStyle = crearEstiloKpiLabel(workbook);
            CellStyle kpiValueStyle = crearEstiloKpiValor(workbook);

            llenarDataSheet(dataSheet, pedidos, detalles);

            crearKPIs(dashboardSheet, pedidos, titleStyle, subtitleStyle, cellStyle, kpiLabelStyle, kpiValueStyle, moneyStyle);

            // Gráficas desde fila 8
            crearGraficoPastel(dashboardSheet, dataSheet, workbook, 0, 4, 5, 18);
            crearGraficoDonut(dashboardSheet, dataSheet, workbook, 6, 4, 11, 18);
            crearGraficoBarrasHorizontal(dashboardSheet, dataSheet, workbook, 0, 20, 11, 37);

            crearHojaPedidos(pedidosSheet, pedidos, headerStyle, cellStyle, fechaStyle, moneyStyle);
            crearHojaDetalles(detallesSheet, detalles, headerStyle, cellStyle, moneyStyle);

            workbook.write(out);
            return out.toByteArray();
        } finally {
            workbook.close();
        }
    }

    private void llenarDataSheet(XSSFSheet sheet, List<PedidoExcelDTO> pedidos,
                                   List<DetallePedidoExcelDTO> detalles) {
        String[] estadoLabels = {"Pendiente Pago", "Pagado", "En Preparación", "En Camino", "Entregado", "Cancelado"};
        String[] estadoKeys = {"PENDIENTE_PAGO", "PAGADO", "EN_PREPARACION", "EN_CAMINO", "ENTREGADO", "CANCELADO"};

        Map<String, Long> pedidosPorEstado = pedidos.stream()
                .collect(Collectors.groupingBy(PedidoExcelDTO::getEstado, Collectors.counting()));

        int row = 0;
        XSSFRow headerRow = sheet.createRow(row++);
        headerRow.createCell(0).setCellValue("Estado");
        headerRow.createCell(1).setCellValue("Cantidad");

        for (int i = 0; i < estadoLabels.length; i++) {
            long cantidad = pedidosPorEstado.getOrDefault(estadoKeys[i], 0L);
            XSSFRow dataRow = sheet.createRow(row++);
            dataRow.createCell(0).setCellValue(estadoLabels[i]);
            dataRow.createCell(1).setCellValue((double) cantidad);
        }

        row++;

        Map<String, int[]> ventasPorProducto = new LinkedHashMap<>();
        for (DetallePedidoExcelDTO d : detalles) {
            String key = d.getProductoNombre() + "|" + d.getProductoSku();
            ventasPorProducto.merge(key, new int[]{0, 0}, (a, b) -> a);
            int[] vals = ventasPorProducto.get(key);
            vals[0] += d.getCantidad() != null ? d.getCantidad() : 0;
            vals[1] += d.getSubtotal() != null ? d.getSubtotal().intValue() : 0;
        }
        List<Map.Entry<String, int[]>> topProductos = ventasPorProducto.entrySet().stream()
                .sorted((a, b) -> b.getValue()[0] - a.getValue()[0])
                .limit(10)
                .toList();

        XSSFRow prodHeader = sheet.createRow(row++);
        prodHeader.createCell(0).setCellValue("SKU");
        prodHeader.createCell(1).setCellValue("Cantidad");

        if (topProductos.isEmpty()) {
            XSSFRow dataRow = sheet.createRow(row++);
            dataRow.createCell(0).setCellValue("(sin datos)");
            dataRow.createCell(1).setCellValue(0.0);
        } else {
            for (Map.Entry<String, int[]> entry : topProductos) {
                String[] parts = entry.getKey().split("\\|");
                String sku = parts.length > 1 && !parts[1].isBlank() ? parts[1] : parts[0];
                XSSFRow dataRow = sheet.createRow(row++);
                dataRow.createCell(0).setCellValue(sku);
                dataRow.createCell(1).setCellValue((double) entry.getValue()[0]);
            }
        }

        row++;

        Map<String, List<PedidoExcelDTO>> pedidosPorSede = pedidos.stream()
                .collect(Collectors.groupingBy(PedidoExcelDTO::getSedeNombre));

        XSSFRow sedeHeader = sheet.createRow(row++);
        sedeHeader.createCell(0).setCellValue("Sede");
        sedeHeader.createCell(1).setCellValue("Total");

        if (pedidosPorSede.isEmpty()) {
            XSSFRow dataRow = sheet.createRow(row++);
            dataRow.createCell(0).setCellValue("(sin datos)");
            dataRow.createCell(1).setCellValue(0.0);
        } else {
            for (Map.Entry<String, List<PedidoExcelDTO>> entry : pedidosPorSede.entrySet()) {
                BigDecimal sumaTotal = entry.getValue().stream()
                        .map(PedidoExcelDTO::getTotal)
                        .filter(t -> t != null)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                XSSFRow dataRow = sheet.createRow(row++);
                dataRow.createCell(0).setCellValue(entry.getKey());
                dataRow.createCell(1).setCellValue(sumaTotal.doubleValue());
            }
        }
    }

    private int getDataRowCount(XSSFSheet sheet, int startRow) {
        int count = 0;
        for (int i = startRow; ; i++) {
            var row = sheet.getRow(i);
            if (row == null) break;
            var cell = row.getCell(0);
            if (cell == null || cell.getStringCellValue() == null || cell.getStringCellValue().isEmpty()) break;
            count++;
        }
        return count;
    }

    private int encontrarFilaConCabecera(XSSFSheet sheet, String cabecera) {
        for (int i = 0; i <= 100; i++) {
            var row = sheet.getRow(i);
            if (row != null) {
                var cell = row.getCell(0);
                if (cell != null) {
                    try {
                        if (cabecera.equals(cell.getStringCellValue())) return i;
                    } catch (Exception e) { }
                }
            }
        }
        return -1;
    }

    private void crearKPIs(XSSFSheet sheet, List<PedidoExcelDTO> pedidos,
                           CellStyle titleStyle, CellStyle subtitleStyle,
                           CellStyle cellStyle, CellStyle kpiLabelStyle, CellStyle kpiValueStyle, CellStyle moneyStyle) {
        XSSFRow titleRow = sheet.createRow(0);
        createCell(titleRow, 0, "RESUMEN DE PEDIDOS", titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 10));

        int totalPedidos = pedidos.size();
        BigDecimal totalVentas = pedidos.stream()
                .map(PedidoExcelDTO::getTotal)
                .filter(t -> t != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalEnvios = pedidos.stream()
                .map(PedidoExcelDTO::getCostoEnvio)
                .filter(e -> e != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal promedioPedido = totalPedidos > 0
                ? totalVentas.divide(BigDecimal.valueOf(totalPedidos), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        XSSFRow kpiRow = sheet.createRow(2);
        createCell(kpiRow, 0, "Total Pedidos:", kpiLabelStyle);
        createNumericCell(kpiRow, 1, totalPedidos, kpiValueStyle);
        createCell(kpiRow, 3, "Total Ventas:", kpiLabelStyle);
        createNumericCell(kpiRow, 4, totalVentas.doubleValue(), kpiValueStyle);
        createCell(kpiRow, 6, "Promedio/Pedido:", kpiLabelStyle);
        createNumericCell(kpiRow, 7, promedioPedido.doubleValue(), kpiValueStyle);
        createCell(kpiRow, 9, "Total Envíos:", kpiLabelStyle);
        createNumericCell(kpiRow, 10, totalEnvios.doubleValue(), kpiValueStyle);

        int[] anchos = {4000, 3500, 1000, 4000, 3500, 1000, 4000, 3500, 1000, 4000, 3500};
        for (int i = 0; i < anchos.length; i++) {
            sheet.setColumnWidth(i, anchos[i]);
        }
    }


    private void crearGraficoPastel(XSSFSheet sheet, XSSFSheet dataSheet, XSSFWorkbook workbook,
                                     int col1, int row1, int col2, int row2) {
        XSSFDrawing drawing = sheet.createDrawingPatriarch();
        XSSFClientAnchor anchor = new XSSFClientAnchor(0, 0, 0, 0, col1, row1, col2, row2);

        XSSFChart chart = drawing.createChart(anchor);
        chart.setTitleText("Pedidos por Estado");
        chart.setTitleOverlay(false);

        XDDFChartLegend legend = chart.getOrAddLegend();
        legend.setPosition(LegendPosition.BOTTOM);

        XDDFCategoryAxis bottomAxis = chart.createCategoryAxis(AxisPosition.BOTTOM);
        bottomAxis.setTickLabelPosition(AxisTickLabelPosition.NONE);
        XDDFValueAxis leftAxis = chart.createValueAxis(AxisPosition.LEFT);
        leftAxis.setTitle("Cantidad");

        // Force value axis to cross between categories → gap from Y-axis
        CTPlotArea ctPlotArea = chart.getCTChart().getPlotArea();
        for (CTValAx ctValAx : ctPlotArea.getValAxList()) {
            CTCrossBetween crossBetween = ctValAx.isSetCrossBetween()
                ? ctValAx.getCrossBetween()
                : ctValAx.addNewCrossBetween();
            crossBetween.setVal(STCrossBetween.BETWEEN);
        }

        XDDFBarChartData data = (XDDFBarChartData) chart.createData(ChartTypes.BAR, bottomAxis, leftAxis);
        data.setBarDirection(BarDirection.COL);

        int dataStart = encontrarFilaConCabecera(dataSheet, "Estado") + 1;
        int dataEnd = dataStart + getDataRowCount(dataSheet, dataStart) - 1;

        XDDFChartData.Series series = data.addSeries(
                XDDFDataSourcesFactory.fromStringCellRange(dataSheet, new CellRangeAddress(dataStart, dataEnd, 0, 0)),
                XDDFDataSourcesFactory.fromNumericCellRange(dataSheet, new CellRangeAddress(dataStart, dataEnd, 1, 1)));
        series.setTitle("Estados", null);

        chart.plot(data);
        int numPuntos = dataEnd - dataStart + 1;
        aplicarColoresMarca(chart, numPuntos);
    }

    private void crearGraficoDonut(XSSFSheet sheet, XSSFSheet dataSheet, XSSFWorkbook workbook,
                                    int col1, int row1, int col2, int row2) {
        XSSFDrawing drawing = sheet.createDrawingPatriarch();
        XSSFClientAnchor anchor = new XSSFClientAnchor(0, 0, 0, 0, col1, row1, col2, row2);

        XSSFChart chart = drawing.createChart(anchor);
        chart.setTitleText("Ventas por Sede");
        chart.setTitleOverlay(false);

        XDDFChartLegend legend = chart.getOrAddLegend();
        legend.setPosition(LegendPosition.BOTTOM);

        XDDFDoughnutChartData data = (XDDFDoughnutChartData) chart.createData(ChartTypes.DOUGHNUT, null, null);
        data.setVaryColors(true);

        int dataStart = encontrarFilaConCabecera(dataSheet, "Sede") + 1;
        int dataEnd = dataStart + getDataRowCount(dataSheet, dataStart) - 1;

        XDDFChartData.Series series = data.addSeries(
                XDDFDataSourcesFactory.fromStringCellRange(dataSheet, new CellRangeAddress(dataStart, dataEnd, 0, 0)),
                XDDFDataSourcesFactory.fromNumericCellRange(dataSheet, new CellRangeAddress(dataStart, dataEnd, 1, 1)));
        series.setTitle("Sedes", null);

        chart.plot(data);
        int numPuntos = dataEnd - dataStart + 1;
        aplicarColoresMarca(chart, numPuntos);
    }

    private void crearGraficoBarrasHorizontal(XSSFSheet sheet, XSSFSheet dataSheet, XSSFWorkbook workbook,
                                               int col1, int row1, int col2, int row2) {
        XSSFDrawing drawing = sheet.createDrawingPatriarch();
        XSSFClientAnchor anchor = new XSSFClientAnchor(0, 0, 0, 0, col1, row1, col2, row2);

        XSSFChart chart = drawing.createChart(anchor);
        chart.setTitleText("Top 10 Productos Más Vendidos");
        chart.setTitleOverlay(false);

        XDDFChartLegend legend = chart.getOrAddLegend();
        legend.setPosition(LegendPosition.BOTTOM);

        XDDFCategoryAxis bottomAxis = chart.createCategoryAxis(AxisPosition.BOTTOM);
        XDDFValueAxis leftAxis = chart.createValueAxis(AxisPosition.LEFT);
        leftAxis.setTitle("Cantidad Vendida");

        CTPlotArea ctPlotArea2 = chart.getCTChart().getPlotArea();
        for (CTValAx ctValAx : ctPlotArea2.getValAxList()) {
            CTCrossBetween crossBetween = ctValAx.isSetCrossBetween()
                ? ctValAx.getCrossBetween()
                : ctValAx.addNewCrossBetween();
            crossBetween.setVal(STCrossBetween.BETWEEN);
        }

        XDDFBarChartData data = (XDDFBarChartData) chart.createData(ChartTypes.BAR, bottomAxis, leftAxis);
        data.setBarDirection(BarDirection.BAR);

        int dataStart = encontrarFilaConCabecera(dataSheet, "SKU") + 1;
        int dataEnd = dataStart + getDataRowCount(dataSheet, dataStart) - 1;

        XDDFChartData.Series series = data.addSeries(
                XDDFDataSourcesFactory.fromStringCellRange(dataSheet, new CellRangeAddress(dataStart, dataEnd, 0, 0)),
                XDDFDataSourcesFactory.fromNumericCellRange(dataSheet, new CellRangeAddress(dataStart, dataEnd, 1, 1)));
        series.setTitle("Cantidad", null);

        chart.plot(data);
        int numPuntos = dataEnd - dataStart + 1;
        aplicarColoresMarca(chart, numPuntos);
    }

    private void aplicarColoresMarca(XSSFChart chart, int numPuntos) {
        String[] palette = generarPaleta(numPuntos);
        CTPlotArea area = chart.getCTChart().getPlotArea();
        for (CTBarChart bc : area.getBarChartList()) {
            for (CTBarSer ser : bc.getSerList()) {
                if (ser.isSetSpPr()) {
                    ser.unsetSpPr();
                }
                for (int i = 0; i < numPuntos; i++) {
                    CTDPt dp = ser.addNewDPt();
                    dp.addNewIdx().setVal(i);
                    dp.addNewSpPr().addNewSolidFill().addNewSrgbClr().setVal(hexToBytes(palette[i]));
                }
            }
        }
        for (CTDoughnutChart dc : area.getDoughnutChartList()) {
            for (CTPieSer ser : dc.getSerList()) {
                if (ser.isSetSpPr()) {
                    ser.unsetSpPr();
                }
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
            {229, 190, 111}, // #E5BE6F Mustard
            {212, 175, 92},  // #D4AF5C Mustard dark
            {234, 195, 189}, // #EAC3BD Rose
            {122, 138, 115}  // #7A8A73 Sage
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

    private void crearHojaPedidos(XSSFSheet sheet, List<PedidoExcelDTO> pedidos,
                                   CellStyle headerStyle, CellStyle cellStyle,
                                   CellStyle fechaStyle, CellStyle moneyStyle) {
        String[] cabeceras = {"Código", "Fecha", "Cliente", "Teléfono", "Email", "Sede",
                              "Total (incl. envío)", "Envío", "Estado", "Método Pago", "Referencia", "Dirección", "Notas"};

        XSSFRow headerRow = sheet.createRow(0);
        for (int i = 0; i < cabeceras.length; i++) {
            headerRow.createCell(i).setCellValue(cabeceras[i]);
            headerRow.getCell(i).setCellStyle(headerStyle);
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        int rowNum = 1;
        for (PedidoExcelDTO pedido : pedidos) {
            XSSFRow row = sheet.createRow(rowNum++);

            createCell(row, 0, pedido.getCodigo(), cellStyle);
            createCell(row, 1, pedido.getCreadoEn() != null ? pedido.getCreadoEn().format(formatter) : "", cellStyle);
            createCell(row, 2, pedido.getClienteNombre(), cellStyle);
            createCell(row, 3, pedido.getClienteTelefono(), cellStyle);
            createCell(row, 4, pedido.getClienteEmail(), cellStyle);
            createCell(row, 5, pedido.getSedeNombre(), cellStyle);
            createNumericCell(row, 6, pedido.getTotal() != null ? pedido.getTotal().doubleValue() : 0, moneyStyle);
            createNumericCell(row, 7, pedido.getCostoEnvio() != null ? pedido.getCostoEnvio().doubleValue() : 0, moneyStyle);
            createCell(row, 8, pedido.getEstado(), cellStyle);
            createCell(row, 9, pedido.getMetodoPago(), cellStyle);
            createCell(row, 10, pedido.getReferenciaPago(), cellStyle);
            createCell(row, 11, pedido.getDireccionEntrega(), cellStyle);
            createCell(row, 12, pedido.getNotasEntrega(), cellStyle);
        }

        for (int i = 0; i < cabeceras.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void crearHojaDetalles(XSSFSheet sheet, List<DetallePedidoExcelDTO> detalles,
                                    CellStyle headerStyle, CellStyle cellStyle, CellStyle moneyStyle) {
        String[] cabeceras = {"Código Pedido", "Producto", "SKU", "Cantidad", "Precio Unitario", "Subtotal", "Nota"};

        XSSFRow headerRow = sheet.createRow(0);
        for (int i = 0; i < cabeceras.length; i++) {
            headerRow.createCell(i).setCellValue(cabeceras[i]);
            headerRow.getCell(i).setCellStyle(headerStyle);
        }

        int rowNum = 1;
        for (DetallePedidoExcelDTO detalle : detalles) {
            XSSFRow row = sheet.createRow(rowNum++);

            createCell(row, 0, detalle.getPedidoCodigo(), cellStyle);
            createCell(row, 1, detalle.getProductoNombre(), cellStyle);
            createCell(row, 2, detalle.getProductoSku(), cellStyle);
            createNumericCell(row, 3, detalle.getCantidad() != null ? detalle.getCantidad() : 0, cellStyle);
            createNumericCell(row, 4, detalle.getPrecioUnitario() != null ? detalle.getPrecioUnitario().doubleValue() : 0, moneyStyle);
            createNumericCell(row, 5, detalle.getSubtotal() != null ? detalle.getSubtotal().doubleValue() : 0, moneyStyle);
            createCell(row, 6, detalle.getNotaPersonalizacion(), cellStyle);
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
    // ESTILOS DE CELDA
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

    private CellStyle crearEstiloSubtitulo(XSSFWorkbook workbook) {
        XSSFCellStyle style = workbook.createCellStyle();
        XSSFFont font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        font.setColor(new XSSFColor(new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF}, null));
        style.setFont(font);
        style.setFillForegroundColor(new XSSFColor(new byte[]{(byte) 0x7A, (byte) 0x8A, (byte) 0x73}, null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
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
