package br.com.financeiro.api.dashboardfinanceiro.service;

import br.com.financeiro.api.common.exception.BusinessException;
import br.com.financeiro.api.common.exception.ResourceNotFoundException;
import br.com.financeiro.api.dashboardfinanceiro.dto.ResumoCategoriaFinanceiraResponse;
import br.com.financeiro.api.dashboardfinanceiro.dto.ResumoMensalFinanceiroResponse;
import br.com.financeiro.api.dashboardfinanceiro.projection.ResumoCategoriaFinanceiraProjection;
import br.com.financeiro.api.dashboardfinanceiro.projection.ResumoMensalFinanceiroProjection;
import br.com.financeiro.api.dashboardfinanceiro.repository.DashboardFinanceiroRepository;
import br.com.financeiro.api.lancamentofinanceiro.enums.TipoLancamentoFinanceiro;
import br.com.financeiro.api.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DashboardFinanceiroService {

    private static final int ESCALA_PERCENTUAL = 2;

    private final DashboardFinanceiroRepository dashboardFinanceiroRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional(readOnly = true)
    public ResumoMensalFinanceiroResponse buscarResumoMensal(UUID usuarioId, Integer ano, Integer mes) {
        validarUsuario(usuarioId);
        YearMonth competencia = validarCompetencia(ano, mes);

        LocalDate dataInicio = competencia.atDay(1);
        LocalDate dataFim = competencia.atEndOfMonth();

        ResumoMensalFinanceiroProjection resumo = dashboardFinanceiroRepository.buscarResumoMensal(
                usuarioId,
                dataInicio,
                dataFim
        );

        BigDecimal totalReceitas = valorOuZero(resumo.getTotalReceitas());
        BigDecimal totalDespesas = valorOuZero(resumo.getTotalDespesas());
        Long quantidadeLancamentos = resumo.getQuantidadeLancamentos();

        List<ResumoCategoriaFinanceiraResponse> receitasPorCategoria = buscarResumoPorCategoria(
                usuarioId,
                TipoLancamentoFinanceiro.RECEITA,
                dataInicio,
                dataFim,
                totalReceitas
        );

        List<ResumoCategoriaFinanceiraResponse> despesasPorCategoria = buscarResumoPorCategoria(
                usuarioId,
                TipoLancamentoFinanceiro.DESPESA,
                dataInicio,
                dataFim,
                totalDespesas
        );

        return new ResumoMensalFinanceiroResponse(
                usuarioId,
                ano,
                mes,
                totalReceitas,
                totalDespesas,
                totalReceitas.subtract(totalDespesas),
                quantidadeLancamentos,
                receitasPorCategoria,
                despesasPorCategoria
        );
    }

    private List<ResumoCategoriaFinanceiraResponse> buscarResumoPorCategoria(UUID usuarioId,
                                                                             TipoLancamentoFinanceiro tipo,
                                                                             LocalDate dataInicio,
                                                                             LocalDate dataFim,
                                                                             BigDecimal totalPeriodo) {
        return dashboardFinanceiroRepository.buscarResumoPorCategoria(usuarioId, tipo, dataInicio, dataFim)
                .stream()
                .map(item -> paraResponse(item, totalPeriodo))
                .toList();
    }

    private ResumoCategoriaFinanceiraResponse paraResponse(ResumoCategoriaFinanceiraProjection projection,
                                                           BigDecimal totalPeriodo) {
        BigDecimal total = valorOuZero(projection.getTotal());

        return new ResumoCategoriaFinanceiraResponse(
                projection.getCategoriaId(),
                projection.getCategoriaNome(),
                total,
                calcularPercentual(total, totalPeriodo),
                projection.getQuantidadeLancamentos()
        );
    }

    private BigDecimal calcularPercentual(BigDecimal valor, BigDecimal total) {
        if (total == null || BigDecimal.ZERO.compareTo(total) == 0) {
            return BigDecimal.ZERO;
        }

        return valor
                .multiply(BigDecimal.valueOf(100))
                .divide(total, ESCALA_PERCENTUAL, RoundingMode.HALF_UP);
    }

    private BigDecimal valorOuZero(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }

    private void validarUsuario(UUID usuarioId) {
        if (!usuarioRepository.existsById(usuarioId)) {
            throw new ResourceNotFoundException("Usuario nao encontrado.");
        }
    }

    private YearMonth validarCompetencia(Integer ano, Integer mes) {
        if (ano == null || mes == null) {
            throw new BusinessException("Ano e mes sao obrigatorios.");
        }

        if (ano < 2000) {
            throw new BusinessException("Ano deve ser maior ou igual a 2000.");
        }

        if (mes < 1 || mes > 12) {
            throw new BusinessException("Mes deve estar entre 1 e 12.");
        }

        return YearMonth.of(ano, mes);
    }
}
