# FEAT 10 — Gráficos e refinamento visual/UX

## Objetivo e mudanças

Visualizações opcionais ajudam a interpretar as despesas sem mudar as regras ou os fluxos do MVP. A identidade clara/azul, renda e sobra em verde, cards, detalhes da FEAT 09 e FAB + Adicionar foram preservados.

- Dashboard: “Ver gráfico das despesas” abre barras horizontais junto à seção de despesas. “Ocultar gráfico” recolhe. Só existe ação quando há previsão de despesas; funciona também no Dashboard histórico.
- Histórico: “Ver evolução do ano” compara a previsão dos meses relevantes, em ordem cronológica, com a mesma escala. A maior previsão ocupa a barra inteira. Os cards históricos mantêm sua ordem decrescente. Trocar de ano atualiza o gráfico aberto. Com menos de dois meses, ou sem despesas no ano, há explicação simples; ano vazio mantém a mensagem existente sem gráfico artificial.
- Navegação: fades de 100 ms na entrada e 70 ms na saída, tanto ao avançar quanto ao voltar, sem deslocamento. São transições nativas do Compose, sem alterar a escala de animação do sistema. Gráficos não animam as barras.
- Histórico: a lista fica contida na área disponível, com recorte dos itens durante a rolagem para não sobrepor o seletor de ano.
- Formulário mensal: o campo simples MM/AAAA continua editável e ganha leitura por extenso logo abaixo quando válido. Reutiliza o parser existente e `DashboardFormatting`. Não muda validação, YearMonth, data inicial ou seletor nativo de data pontual. Edição recorrente já mostra o período por extenso e foi preservada.

## Gráficos e arquitetura

Compose nativo (`Box`, `Surface`, texto e listas), sem biblioteca externa nem dependência nova. Barras são decorativas; nomes, valores exatos e percentuais são textos acessíveis agrupados. Conteúdo vertical permite quebra de texto em telas estreitas/fontes grandes. Controles usam TextButton e feedback Material, sem escala/bounce.

`ChartData` é um helper de apresentação, sem acesso a banco. O Dashboard fornece `MonthlyAnalysis.expensesByCategory`, que representa a previsão efetiva. A distribuição usa o total dessas categorias, não renda ou somente despesas pontuais. A engine continua resolvendo recorrências, versões, exceções e vigência. Categorias zeradas são omitidas.

Percentuais usam décimos e distribuição dos maiores restos para somar 100% na apresentação, com desempate estável. Barras usam proporções anteriores ao arredondamento. `BigInteger`/`BigDecimal` evitam overflow na soma/multiplicação; proporções ficam entre 0 e 1. Evolução anual utiliza diretamente `MonthlyHistoryItem.forecastExpensesCents`; o repositório histórico existente continua responsável por relevância e exclusão de futuro. Meses relevantes com renda e despesa zero mantêm valor zero quando há comparação anual válida.

Dados chegam pelos Flows existentes e as projeções visuais são memorizadas por entrada de dados durante a composição. Nenhuma consulta é criada pelo gráfico. Estado aberto/fechado usa estado salvo do Compose, inicia fechado, pode ser preservado ao voltar e nunca é gravado em Room. Mudar o período do Dashboard fecha seu gráfico; trocar o ano mantém a opção aberta exibindo os novos dados. LazyListState continua por entrada de navegação.

## Limites

Sem redesign global, calendário personalizado, alteração de regras financeiras, cache persistente, tabelas de gráfico, auditoria geral de performance, recursos de conta/cloud ou expansão do MVP. Detalhes, exclusão, edição e ordem dos campos foram apenas revisados e preservados. Não houve otimização especulativa.

## Validação

Durante o desenvolvimento foram executados somente testes direcionados de navegação/formulário e gráficos. São 18 testes novos em `ChartDataTest` e `FinancialChartsTest`: precisão/escala, limites numéricos, previsão efetiva com versões/exceções, relevância/futuro pelo Histórico existente, estados vazios, abrir/fechar, troca de período/ano, reatividade e fonte ampliada. Testes usam dados e semântica, não pixels.

Validação completa final: `.\gradlew.bat test build lint` aprovada. Total de 278 testes (260 anteriores + 18 novos), zero falhas, erros ou testes ignorados. Build debug/release aprovado; lint com zero erros e oito avisos preexistentes de dependências/ícone. Nenhuma dependência adicionada ou alterada.

Validação manual no `emulator-5556`, com dados de desenvolvimento já existentes:

- Dashboard atual: abrir/fechar gráfico, previsão R$ 450,00; FAB mantido.
- Categoria → detalhe → editar recorrência → voltar; renda → detalhe direto → voltar. Edição mantém período por extenso e os fluxos da FEAT 09.
- Adicionar despesa/receita: ordem e campos preservados, data atual e seletor existentes; opção mensal mostra “Setembro de 2026” abaixo do campo numérico. Rascunho descartado sem salvar.
- Histórico: abrir evolução de 2026, trocar para 2025 com a opção aberta (mensagem de ausência de despesas comparáveis), retornar a 2026. Agosto R$ 170,00 e setembro R$ 450,00 usam escala comum.
- Dashboard de agosto: gráfico mostra R$ 170,00, período correto e 100% em Moradia, sem FAB; retorno ao Histórico preservado.
- Inspeção visual dos dois gráficos em 320dp com fonte de 160%: textos quebram linha, barras legíveis e controles acessíveis por rolagem. Resolução e fonte originais restauradas. Capturas locais em `app/build/android-validation/feat10-year.png` e `feat10-expenses.png`, ignoradas pelo Git.

As novas transições usam apenas fade curto; não houve travamento observado nos gráficos durante os fluxos testados. Não foi feita auditoria quantitativa de frames, performance geral ou TalkBack. Nenhuma fixture automática ou novo registro financeiro foi criado nesta FEAT.
