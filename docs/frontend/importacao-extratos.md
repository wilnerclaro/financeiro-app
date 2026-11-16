# Importação de extratos

## OFX bancário

Acesso por **Importar extrato** no topo em telas largas e em **Lançamentos**,
inclusive no celular. Rota: `/importar`.

Fluxo: escolher arquivo e conta, revisar transações e categorias, confirmar.
Ler o arquivo não altera saldos. A confirmação grava o lote em uma transação;
falhas não deixam lançamentos parcialmente importados.

Escopo inicial: OFX bancário em reais, conta corrente e poupança. Limite de
2 MB e 1.000 transações por arquivo. Identificadores OFX de transações já
importadas para a mesma conta impedem reimportação duplicada. Lançamentos
manuais precisam ser conferidos pelo usuário e desmarcados na revisão.
Transferências não são convertidas automaticamente em receita/despesa.

Referência técnica: [OFX Banking 2.3 — Financial Data Exchange](https://www.financialdataexchange.org/common/Uploaded%20files/OFX%20files/OFX%20Banking%20Specification%20v2.3.pdf).

## TODO — próximos formatos e capacidades

- [ ] **CSV:** seleção de separador/codificação, mapeamento de colunas,
  datas e valores brasileiros, prévia e identificação de duplicatas.
- [ ] **Excel (.xlsx/.xls):** escolha de aba, cabeçalho e colunas;
  importação da planilha de origem com mapeamento explícito, sem executar macros.
- [ ] **PDF bancário:** extração de texto, identificação do banco e revisão;
  OCR para documentos digitalizados, sem gravar valores de baixa confiança.
- [ ] **Faturas de cartão (PDF e OFX):** compras, estornos, parcelas,
  fechamento/vencimento e pagamento da fatura sem contabilizar em dobro.
- [ ] **Outros documentos:** definir formatos e exemplos reais antes de
  implementar; nunca tratar um arquivo não reconhecido como extrato válido.
- [ ] **Conciliação com lançamentos manuais e previstos:** propor
  correspondências para confirmação, preservando os lançamentos existentes.
- [ ] **Transferências em extratos:** associar origem/destino e conciliar os
  dois lados sem gerar receita/despesa.
- [ ] **Regras de categoria e histórico de lotes:** sugestões revisáveis e
  consulta do resultado de importações anteriores.

Os formatos pendentes aparecem como **Em breve**, sem seleção de arquivo ou
promessa de processamento antes de serem implementados.
