package com.floristeria.floristeria.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.List;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import com.floristeria.floristeria.entity.Sede;
import com.floristeria.floristeria.entity.ZonaDomicilio;
import com.floristeria.floristeria.repository.ZonaDomicilioRepository;

class ZonaDomicilioExportServiceTest {

    @Test
    void exportarZonasExcel_generaWorkbookValido() throws Exception {
        ZonaDomicilioRepository repo = mock(ZonaDomicilioRepository.class);

        Sede sedeBogota = Sede.builder().id(1).nombre("Bogotá Centro").ciudad("Bogotá").build();
        Sede sedeMedellin = Sede.builder().id(2).nombre("Medellín Poblado").ciudad("Medellín").build();

        ZonaDomicilio z1 = ZonaDomicilio.builder().id(1).sede(sedeBogota).localidad("Suba").barrio("Prado").precio(new BigDecimal("15000")).excluido(false).build();
        ZonaDomicilio z2 = ZonaDomicilio.builder().id(2).sede(sedeBogota).localidad("Chapinero").barrio(null).precio(new BigDecimal("20000")).excluido(true).build();
        ZonaDomicilio z3 = ZonaDomicilio.builder().id(3).sede(sedeMedellin).localidad("El Poblado").barrio("Manila").precio(new BigDecimal("12000")).excluido(false).build();

        when(repo.findAllWithSede()).thenReturn(List.of(z1, z2, z3));
        when(repo.findBySedeIdWithSede(1)).thenReturn(List.of(z1, z2));
        when(repo.findBySedeIdWithSede(2)).thenReturn(List.of(z3));

        ZonaDomicilioExportService service = new ZonaDomicilioExportService(repo);

        // Test sin filtro (todas)
        byte[] bytes = service.exportarZonasExcel(null);
        assertNotNull(bytes);
        assertTrue(bytes.length > 0);

        try (XSSFWorkbook wb = new XSSFWorkbook(new java.io.ByteArrayInputStream(bytes))) {
            assertEquals(1, wb.getNumberOfSheets());
            Sheet sheet = wb.getSheetAt(0);
            assertEquals("Zonas", sheet.getSheetName());
            // Header en fila 5 (0-indexed)
            assertEquals("Sede", sheet.getRow(5).getCell(0).getStringCellValue());
            assertEquals("Ciudad", sheet.getRow(5).getCell(1).getStringCellValue());
            assertEquals("Localidad", sheet.getRow(5).getCell(2).getStringCellValue());
            assertEquals("Barrio", sheet.getRow(5).getCell(3).getStringCellValue());
            assertEquals("Precio", sheet.getRow(5).getCell(4).getStringCellValue());
            assertEquals("Estado", sheet.getRow(5).getCell(5).getStringCellValue());
            // Datos desde fila 6
            assertEquals(3, sheet.getLastRowNum() - 5); // 3 datos
            // KPIs
            assertTrue(sheet.getRow(0).getCell(0).getStringCellValue().contains("ZONAS"));
        }

        // Test filtrado por sede
        byte[] bytesFiltrado = service.exportarZonasExcel(1);
        try (XSSFWorkbook wb = new XSSFWorkbook(new java.io.ByteArrayInputStream(bytesFiltrado))) {
            Sheet sheet = wb.getSheetAt(0);
            assertEquals(2, sheet.getLastRowNum() - 5);
        }

        // Test lista vacía
        when(repo.findAllWithSede()).thenReturn(List.of());
        byte[] empty = service.exportarZonasExcel(null);
        try (XSSFWorkbook wb = new XSSFWorkbook(new java.io.ByteArrayInputStream(empty))) {
            Sheet sheet = wb.getSheetAt(0);
            assertEquals("Zonas", sheet.getSheetName());
            assertNotNull(sheet.getRow(5)); // cabecera existe aunque vacía
        }
    }
}
