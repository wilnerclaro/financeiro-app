package br.com.financeiro.api.common.util;

import java.text.Normalizer;

public final class NormalizadorTexto {

    private NormalizadorTexto() {
    }

    public static String normalizarNome(String valor) {
        if (valor == null) {
            return null;
        }

        return valor
                .trim()
                .replaceAll("\\s+", " ");
    }

    public static String normalizarParaComparacao(String valor) {
        if (valor == null) {
            return null;
        }

        String textoNormalizado = Normalizer.normalize(valor.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("\\s+", " ")
                .toLowerCase();

        return textoNormalizado;
    }
}