# FEAT 02 — Análise financeira mensal

A função pura `MonthlyAnalysisEngine.analyze(period, movements, recurrences,
versions, exceptions)` recebe modelos do pacote `domain` e devolve
`MonthlyAnalysis`. Não acessa Room, Android, UI, relógio ou rede; não grava resultados.
As extensões em `data/local/DomainMappings.kt` convertem as quatro entidades para
modelos de domínio, sem modificar os contratos ou o schema da persistência.

## Cálculo

Cada mês é independente, sem transporte de saldo. Movimentações pontuais são
filtradas por `YearMonth.from(date)`.

- `registeredIncomeCents`: somente renda pontual do mês.
- `recurringIncomeCents`: renda recorrente efetiva no mês.
- `consideredIncomeCents`: soma das duas rendas, usada na sobra e no percentual.
- `registeredExpenseCents`: somente despesas pontuais registradas.
- `forecastExpenseCents`: despesas registradas mais despesas recorrentes efetivas.
- `forecastRemainingCents`: renda considerada menos previsão; aceita resultado negativo.
- `expensesByCategory`: valores da previsão agrupados por categoria; omite categorias vazias.

Uma movimentação pontual e uma recorrência são entradas independentes e ambas
contam na previsão. Não se deduz correspondência entre elas por nome ou valor.

Sem renda considerada, sobra e percentual são null; despesas permanecem disponíveis.
Um mês sem itens efetivos tem `isEmpty = true`, totais zero, listas/mapa vazios
e sobra/percentual nulos. Uma recorrência excluída não cria item fictício.

## Recorrências, versões e exceções

1. Incluir apenas quando startPeriod <= period e endPeriod for null ou >= period.
   O término é inclusivo.
2. Uma exceção de mesmo recurrenceId e period com excluded=true remove o item,
   ignorando todos os overrides.
3. Selecionar a versão de maior validFrom que não ultrapasse period.
4. Aplicar os overrides não nulos da exceção sobre essa versão.
   Valores nulos herdam a versão; nada é alterado nas entradas.
5. habitualDay é apenas contexto. Dia 31 participa inclusive em fevereiro;
   a engine não inventa uma data ajustada nem consulta o dia atual.

Versões futuras não são usadas. Alterações permanentes preservam análises anteriores;
exceções não afetam outros meses. A resolução é compartilhada por rendas e despesas.

## Precisão, integridade e detalhamento

Dinheiro usa Long em centavos com Math.addExact/subtractExact; overflow lança
ArithmeticException em vez de produzir totais incorretos.
O percentual usa BigDecimal, multiplicando a razão por 100, com quatro casas
decimais e HALF_UP: 150.0000 significa 150%. Não há limite em 100% e esse
arredondamento nunca é usado para calcular dinheiro.

A engine espera entradas coerentes com a persistência. Chaves duplicadas e
recorrências ativas não excluídas sem versão efetiva lançam IllegalArgumentException;
não escolhe arbitrariamente entre duplicatas nem inventa uma versão. Valores
efetivamente usados também são validados (positivos, categoria e dia válidos).
Isso é proteção contra entradas inconsistentes, não mudança nas regras do banco.

incomeSources e expenseItems fornecem os valores efetivos, origem e id da
movimentação/recorrência, versão e exceção aplicadas, data pontual ou dia habitual.
As listas são ordenadas por origem e id para resultado determinístico, mesmo que
as entradas sejam reordenadas. Não contêm textos de UI ou moeda formatada.

Os enums do domínio são independentes dos enums da persistência, mapeados
explicitamente. Não foram adicionados repository, ViewModel, dependências ou tabelas.

## Testes

Execute `./gradlew.bat test` e `./gradlew.bat build`.
Os testes do domínio e dos mapeamentos são JUnit puros, com períodos fixos.
A suíte existente de persistência continua sendo executada com Robolectric.
