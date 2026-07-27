package com.floristeria.floristeria.util;

import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.floristeria.floristeria.exception.TipoArchivoNoSoportadoException;

@Component
public class MimeValidator {

    private static final Map<String, byte[]> MAGIC_BYTES = Map.of(
            "image/jpeg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF},
            "image/png",  new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A},
            "image/webp", new byte[]{0x52, 0x49, 0x46, 0x46}  // "RIFF" - se valida adicionalmente el chunk "WEBP"
    );

    private static final Map<String, String> MIME_A_EXTENSION = Map.of(
            "image/jpeg", ".jpg",
            "image/png",  ".png",
            "image/webp", ".webp",
            "image/avif", ".avif"
    );

    public ResultadoMime validar(MultipartFile archivo) {
        try {
            byte[] bytes = archivo.getBytes();
            if (bytes.length < 12) {
                throw new TipoArchivoNoSoportadoException("archivo vacio o muy pequeno");
            }
            String mime = detectarPorMagicBytes(bytes);
            if (mime == null || !MIME_A_EXTENSION.containsKey(mime)) {
                throw new TipoArchivoNoSoportadoException(
                        mime != null ? mime : "desconocido (" + formatoHex(bytes) + ")");
            }
            return new ResultadoMime(mime, MIME_A_EXTENSION.get(mime));
        } catch (TipoArchivoNoSoportadoException e) {
            throw e;
        } catch (Exception e) {
            throw new TipoArchivoNoSoportadoException("error al leer archivo: " + e.getMessage());
        }
    }

    private String detectarPorMagicBytes(byte[] b) {
        // PNG: 89 50 4E 47 0D 0A 1A 0A
        if (b[0] == (byte) 0x89 && b[1] == 0x50 && b[2] == 0x4E && b[3] == 0x47
                && b[4] == 0x0D && b[5] == 0x0A && b[6] == 0x1A && b[7] == 0x0A) {
            return "image/png";
        }
        // JPEG: FF D8 FF
        if (b[0] == (byte) 0xFF && b[1] == (byte) 0xD8 && b[2] == (byte) 0xFF) {
            return "image/jpeg";
        }
        // RIFF....WEBP
        if (b[0] == 0x52 && b[1] == 0x49 && b[2] == 0x46 && b[3] == 0x46
                && b[8] == 0x57 && b[9] == 0x45 && b[10] == 0x42 && b[11] == 0x50) {
            return "image/webp";
        }
        // AVIF/HEIF: ....ftypavif or ....ftypavis (ISO BMFF)
        if (b[4] == 0x66 && b[5] == 0x74 && b[6] == 0x79 && b[7] == 0x70) {  // "ftyp"
            if ((b[8] == 0x61 && b[9] == 0x76 && b[10] == 0x69 && b[11] == 0x66)   // "avif"
                    || (b[8] == 0x61 && b[9] == 0x76 && b[10] == 0x69 && b[11] == 0x73)  // "avis"
                    || (b[8] == 0x6D && b[9] == 0x69 && b[10] == 0x66 && b[11] == 0x31)) {  // "mif1"
                return "image/avif";
            }
        }
        return null;
    }

    private String formatoHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        int limit = Math.min(bytes.length, 16);
        for (int i = 0; i < limit; i++) {
            sb.append(String.format("%02X", bytes[i] & 0xFF));
            if (i < limit - 1) sb.append(" ");
        }
        return sb.toString();
    }

    public record ResultadoMime(String mime, String extension) {}
}
