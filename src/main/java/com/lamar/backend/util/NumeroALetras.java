package com.lamar.backend.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class NumeroALetras {

    private static final String[] BASE = {
        "CERO", "UN", "DOS", "TRES", "CUATRO", "CINCO", "SEIS", "SIETE", "OCHO", "NUEVE",
        "DIEZ", "ONCE", "DOCE", "TRECE", "CATORCE", "QUINCE", "DIECISÉIS", "DIECISIETE",
        "DIECIOCHO", "DIECINUEVE", "VEINTE", "VEINTIÚN", "VEINTIDÓS", "VEINTITRÉS",
        "VEINTICUATRO", "VEINTICINCO", "VEINTISÉIS", "VEINTISIETE", "VEINTIOCHO", "VEINTINUEVE"
    };
    private static final String[] DECENAS = {
        "", "", "", "TREINTA", "CUARENTA", "CINCUENTA", "SESENTA", "SETENTA", "OCHENTA", "NOVENTA"
    };
    private static final String[] CENTENAS = {
        "", "CIENTO", "DOSCIENTOS", "TRESCIENTOS", "CUATROCIENTOS", "QUINIENTOS",
        "SEISCIENTOS", "SETECIENTOS", "OCHOCIENTOS", "NOVECIENTOS"
    };

    private NumeroALetras() {}

    /** Ejemplo: 373.44 -> "TRESCIENTOS SETENTA Y TRES CON 44/100 SOLES" */
    public static String soles(BigDecimal monto) {
        BigDecimal m = monto.setScale(2, RoundingMode.HALF_UP);
        long entero = m.toBigInteger().longValue();
        int centimos = m.remainder(BigDecimal.ONE).movePointRight(2).intValue();
        return convertir(entero) + String.format(" CON %02d/100 SOLES", centimos);
    }

    private static String convertir(long n) {
        if (n == 0) return "CERO";
        if (n >= 1_000_000) {
            long millones = n / 1_000_000;
            long resto = n % 1_000_000;
            String s = millones == 1 ? "UN MILLÓN" : convertir(millones) + " MILLONES";
            return resto > 0 ? s + " " + convertir(resto) : s;
        }
        if (n >= 1000) {
            long miles = n / 1000;
            long resto = n % 1000;
            String s = miles == 1 ? "MIL" : convertir(miles) + " MIL";
            return resto > 0 ? s + " " + convertir(resto) : s;
        }
        return menorMil((int) n);
    }

    private static String menorMil(int n) {
        if (n == 100) return "CIEN";
        int c = n / 100;
        int r = n % 100;
        String centena = c > 0 ? CENTENAS[c] : "";
        if (r == 0) return centena;
        String resto = r < 30 ? BASE[r]
                : DECENAS[r / 10] + (r % 10 > 0 ? " Y " + BASE[r % 10] : "");
        return centena.isEmpty() ? resto : centena + " " + resto;
    }
}