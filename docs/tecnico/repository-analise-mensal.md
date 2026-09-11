# FEAT 03 — Repository de análise mensal

`MonthlyAnalysisRepository` expõe duas operações de leitura:

```kotlin
val repository = MonthlyAnalysisRepository(database)
val analysis = repository.getMonthlyAnalysis(YearMonth.of(2026, 9))
val updates = repository.observeMonthlyAnalysis(YearMonth.of(2026, 9))
```

O ponto de composição fornece a instância existente de `FinancialDatabase`.
Os consumidores recebem apenas `MonthlyAnalysis`, sem manipular DAOs, entidades
ou resolver recorrências. Não há interface adicional nem framework de DI.

## Caminho dos dados

1. `MonthlyAnalysisDao.snapshot` executa quatro consultas em uma única transação.
2. Movimentos são filtrados entre o primeiro e o último dia do mês, inclusive.
3. Recorrências são candidatas quando o período está dentro de sua vigência.
4. Versões são limitadas às recorrências candidatas e a `validFrom <= period`.
   Todas as versões históricas elegíveis dessas recorrências são retornadas;
   a escolha da versão efetiva continua exclusivamente na engine.
5. Exceções são limitadas às recorrências candidatas e ao mês solicitado.
6. Fora da transação, o repository aplica `toDomain()` e chama a engine.

Os joins evitam consultas por recorrência (N+1) e listas enormes de parâmetros.
Os filtros SQL reduzem a leitura; não calculam valores, não escolhem a versão
efetiva e não aplicam overrides. A engine mantém as validações e todas as regras
financeiras da FEAT 02. Seu trabalho e os mapeamentos rodam em `Dispatchers.Default`;
as consultas suspend usam os executores do Room.

## Atualização automática

O Flow é frio e usa `InvalidationTracker.createFlow` nas quatro tabelas de fatos.
Emite a análise inicial e relê um snapshot transacional após invalidações do Room.
Isso evita combinar resultados parciais de quatro Flows independentes e não usa
polling, relógio, cache manual ou escopo de coroutines mantido pelo repository.

Room invalida por tabela: uma alteração em outro mês pode disparar nova leitura.
`distinctUntilChanged` impede nova emissão quando a análise final é igual.
Alterações rápidas podem ser agrupadas; o Flow representa o estado atual, não um
log de cada gravação. Cancelar a coleta encerra a observação.

Referência da API: [Room InvalidationTracker](https://developer.android.com/reference/kotlin/androidx/room/InvalidationTracker).

## Escrita, erros e escopo

Este repository é somente de análise. Não foram adicionadas operações de escrita
ou formulários: os DAOs existentes continuam disponíveis na camada de dados,
incluindo a criação transacional de recorrência com primeira versão, stop por
endPeriod e exclusão física em cascata. Nenhuma regra de escrita foi alterada.

Erros de integridade, overflow e inconsistências detectadas pela engine propagam
para o chamador ou encerram a coleta com erro; não há fallback financeiro.
MonthlyAnalysis permanece em memória. O schema e a versão do banco não mudaram.

## Testes

`./gradlew.bat test` executa a suíte completa. Os testes novos usam Room em memória
com SQLite nativo via Robolectric e a engine real. Cobrem filtragem, histórico,
exceções, totais integrados, mudanças em todas as tabelas e erros explícitos.
Os testes de Flow aguardam emissões com timeout e cancelam seus coletores, sem sleeps.
`./gradlew.bat build` valida também compilação e lint.
