package br.com.financeiro.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode;

/**
 * Serializa {@code Page} como um DTO estável ({@code content} + {@code page}), em vez de expor a
 * estrutura interna do {@code PageImpl}, que pode mudar entre versões do Spring Data.
 */
@Configuration
@EnableSpringDataWebSupport(pageSerializationMode = PageSerializationMode.VIA_DTO)
public class WebConfig {}
