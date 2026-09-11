# Revisão de UX pós-FEAT 10 / pré-FEAT 11

## Origem e direção de produto

Revisão de 09/09/2026, motivada pela validação do usuário em dispositivo Android real após a FEAT 10. A direção aprovada é **leve, confortável, clara, amigável e moderna**. A implementação Android atual é a referência visual principal. O protótipo permanece como referência histórica e funcional, sem orientar aproximação global ou pixel-perfect.

Este trabalho é um pacote localizado de apresentação, sem nova FEAT numerada, redesign ou antecipação da FEAT 11.

## Correções

- **Gráfico de despesas:** as categorias aparecem primeiro. Depois da última categoria ficam o acesso “Ver gráfico das despesas” e, quando aberto, o gráfico. Foi movida apenas a chamada existente, mantendo condição de exibição, estado salvo, cálculos, percentuais, barras e comportamento. Vale também para o Dashboard histórico.
- **Cards Renda / Despesas registradas:** ambos reservam o mesmo número de linhas para títulos e valores, medido com a tipografia, largura útil e escala de fonte atuais. Os valores ficam alinhados quando os cards estão lado a lado, mesmo com o segundo título em duas linhas. A disposição vertical existente para pouca largura/fonte ampliada permanece; valores que não caberiam na meia largura também usam disposição vertical. Não há redução de fonte, altura fixa ou truncamento. O par mantém proporção também quando o valor precisa quebrar linha.
- **Voltar:** retirada a seta em Histórico, Dashboard histórico, lista de gastos da categoria e detalhe financeiro (renda/despesa). Os formulários de entrada e o seletor de categoria já usavam “Voltar”. Nenhum callback ou comportamento de retorno foi alterado.
- **Cards do Histórico:** mostram somente o mês, à esquerda, em `titleLarge` com peso semibold. Mês e selo ATUAL formam um grupo com 4dp de intervalo; há 4dp adicionais abaixo do grupo, além do espaçamento existente do card, separando-o dos dados. O padding de 20dp permanece. A composição vertical evita disputa de espaço entre mês e selo com fonte ampliada; não foi criado espaço artificial para preencher o ano removido.
- **Cabeçalho do Dashboard:** preserva nome do produto → período → Histórico, alinhados pela margem externa. “Histórico ›” usa TextButton Material com superfície branca discreta da paleta existente, formato arredondado, padding horizontal de 12dp e área mínima de toque nativa. O indicador de avanço é decorativo para acessibilidade; o texto e a ação continuam acessíveis. Não há botão azul grande, menu ou navegação adicional.
- **Interpretação:** explicação e conclusão foram reunidas na superfície azul já existente. Padding compartilhado de 16dp, separação vertical de 12dp e respiro externo de 8dp. A explicação usa `bodyMedium` e cor secundária; a conclusão mantém `bodyLarge` e a cor de destaque. Não foram adicionados textos, cards, cores ou cálculos.

## Preservado explicitamente

- Renda: Dashboard → detalhe direto. Despesa: Dashboard → categoria → lista → detalhe.
- Navegação, back stack, restauração de scroll e botão/gesto do sistema.
- Transições da FEAT 10: fade de entrada de 100ms e saída de 70ms; arquivo de navegação intocado.
- FAB “+ Adicionar”, posição e componente atuais.
- Títulos principais à esquerda e seletor anual contextual centralizado.
- Mês e ano explícitos em Dashboard, detalhes, edição e mensagens como “Somente em abril de 2025”; o formatador de período não foi alterado.
- Engine financeira, regras, ViewModels, repositories, DAOs, Room, SQLite e modelos persistentes.
- Implementação dos gráficos, gráfico anual, barras, tecnologia, cálculos e percentuais.
- Arquitetura, tema, dependências e demais decisões visuais Android.

## Testes e build

Validação completa final: `.\gradlew.bat test build lint` — **BUILD SUCCESSFUL**.

- **278 testes existentes aprovados**, zero falhas, erros ou ignorados, conferidos nos XMLs de `app/build/test-results/testDebugUnitTest`.
- Nenhum teste novo. Três arquivos de testes existentes tiveram somente expectativas de apresentação atualizadas para “Voltar” e nomes mensais sem ano. Continuam verificando período inequívoco nos detalhes, edição histórica, navegação e restauração de scroll.
- A primeira tentativa da validação completa parou em uma expectativa remanescente de mês/ano no card histórico de `FinancialDetailNavigationTest`. Corrigida essa expectativa, a execução completa final passou. Não houve mudança no comportamento financeiro para acomodar o teste.
- Build debug e release aprovados; lint com **zero erros e 11 avisos**: 1 AndroidGradlePluginVersion, 7 GradleDependency, 2 NewerVersionAvailable e 1 MissingApplicationIcon. O registro da FEAT 10 citava oito avisos; nesta execução a contagem é onze. Os avisos de atualização dependem das versões disponíveis, e os arquivos de dependências/manifesto não foram alterados neste pacote. Nenhuma atualização ou correção de ícone foi feita fora do escopo.
- A prévia debug normal passou a mostrar os controles aprovados. Foi adicionada uma prévia de valores grandes no arquivo já existente, sem banco ou gravação de dados. Prévia não é teste automatizado nem funcionalidade de produção.

## Validação visual manual

Executada no `emulator-5556` (AVD de desenvolvimento existente), com captura de tela e inspeção visual. O dispositivo físico conectado não foi utilizado nem modificado.

| Cenário | Resultado |
| --- | --- |
| 1080px / densidade 420, aproximadamente 411dp, fonte 100% | Cabeçalho secundário e clicável; cards equilibrados; interpretação em bloco; Histórico com mês à esquerda e ATUAL próximo. |
| 1000px / densidade 420, aproximadamente 381dp, fonte 100% | “Despesas registradas” em duas linhas, cards lado a lado com mesma altura e valores alinhados. |
| 840px / densidade 420, 320dp, fonte 100% | Cards em coluna, mesma altura, valores completos e margens preservadas. |
| 320dp, fonte 160% | Cards proporcionais; explicação/conclusão completas por rolagem; Histórico legível, mês/ATUAL integrados e seletor anual claro. Gráfico abre depois da categoria e continua legível. |
| 320dp, fonte 160%, prévia com R$ 999.999.999,00 e R$ 888.888.888,00 | Ambos os cards crescem igualmente; símbolo e número quebram em linhas sem truncamento ou redução da fonte. Nenhum registro foi persistido. |

Fluxos conferidos: Histórico → Agosto (Dashboard mantém “Agosto de 2026”) → Voltar → Histórico → Voltar; Dashboard → Moradia → Aluguel test → Voltar → categoria → Voltar; Dashboard → Salario test → detalhe direto. Controles textuais sem seta nas telas equivalentes. O gráfico atual continua mostrando R$ 450,00 / 100% em Moradia e abre/recolhe após a categoria. O FAB permanece acessível; conteúdos passam atrás dele durante a rolagem como na implementação aprovada e podem ser trazidos inteiramente para a área livre.

Resolução original de 1080×2340 e fonte 100% restauradas; densidade 420 preservada. Não foram criadas, editadas ou excluídas informações financeiras durante a inspeção manual.

Capturas locais ignoradas pelo Git em `app/build/android-validation/`: `pre-feat11-dashboard.png`, `pre-feat11-paired-cards.png`, `pre-feat11-history.png`, `pre-feat11-expenses.png`, `pre-feat11-detail.png`, `pre-feat11-320.png`, `pre-feat11-320-font160.png`, `pre-feat11-interpretation-font160.png`, `pre-feat11-chart-font160.png`, `pre-feat11-history-font160.png` e `pre-feat11-large-amounts.png`.

## Arquivos e entrega

Criado: `docs/PRE-FEAT-11-UX-REVIEW.md` (caminho original; atualmente em `docs/historico/PRE-FEAT-11-UX-REVIEW.md`).

Alterados:

- `app/src/main/java/com/clarezafinanceira/app/presentation/dashboard/DashboardScreen.kt`
- `app/src/main/java/com/clarezafinanceira/app/presentation/history/HistoryScreen.kt`
- `app/src/main/java/com/clarezafinanceira/app/presentation/detail/FinancialDetailScreens.kt`
- `app/src/debug/java/com/clarezafinanceira/app/presentation/dashboard/DashboardPreviews.kt`
- `app/src/test/java/com/clarezafinanceira/app/presentation/dashboard/FinancialDetailNavigationTest.kt`
- `app/src/test/java/com/clarezafinanceira/app/presentation/dashboard/FinancialDetailScreenTest.kt`
- `app/src/test/java/com/clarezafinanceira/app/presentation/dashboard/HistoryNavigationTest.kt`

Nenhuma dependência alterada. Nenhum outro problema funcional identificado nos fluxos inspecionados; avisos de lint registrados acima sem implementação fora do escopo. Sem commit e sem push.

**Pronto para validação visual do usuário antes da FEAT 11.**
