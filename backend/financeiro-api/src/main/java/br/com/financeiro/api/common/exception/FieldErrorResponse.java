package br.com.financeiro.api.common.exception;

public record FieldErrorResponse(String field, String message) {}
