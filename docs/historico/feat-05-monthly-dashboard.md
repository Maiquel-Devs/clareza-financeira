# FEAT 05 — Dashboard financeiro mensal

O app abre diretamente no Dashboard Compose. `MainActivity` obtém o
`MonthlyAnalysisViewModel` com a factory do `AppContainer`, associado à Activity.
`DashboardRoute` coleta `uiState` com `collectAsStateWithLifecycle`;
`DashboardScreen` recebe apenas esse estado, sem DAO, Room ou Entity.
O título e os dados usam o período do mesmo estado, evitando atribuir dados antigos
a um novo mês. A rolagem volta ao topo quando o período muda.

## Dados e apresentação

Os quatro indicadores usam, respectivamente, `consideredIncomeCents`,
`registeredExpenseCents`, `forecastExpenseCents` e `forecastRemainingCents`.
O percentual usa `expensePercentageOfIncome`, sem limitar a 100%.
As fontes vêm de `incomeSources`, com origem apresentada como Mensal ou Pontual.
Os valores das categorias vêm de `expensesByCategory`; somente valores positivos
aparecem. A quantidade de gastos é a contagem dos `expenseItems` daquela categoria.
Nenhum total ou distribuição é recalculado; o domínio e a engine não foram alterados.

`DashboardFormatting` apresenta centavos via `BigDecimal` e `NumberFormat` pt-BR,
sem `Double`. Também formata mês/ano em português e o percentual já calculado com
até uma casa decimal. Tradução dos rótulos, pluralização e contagem de itens são
transformações de apresentação.

## Estados

- Loading: indicador e mensagem, sem análise anterior.
- Success: resumo, interpretação, fontes de renda e categorias.
- Sem renda: mensagem explícita, sem percentual; sobra nula é “Não disponível”.
- Vazio (`analysis.isEmpty`): orientação inicial, sem quatro cards com zeros.
- Error: mensagem humana, sem causa técnica ou botão de retry sem implementação.

## Visual e validação

`Prototipo/prototipo_12` orientou a hierarquia, os rótulos e a paleta clara.
A implementação é nativa Material 3: fundo azul claro, renda/sobra em verde,
apoios amarelo/roxo, superfícies brancas e texto escuro. Os cards iniciais se
empilham com largura pequena ou fonte ampliada; títulos possuem semântica de
cabeçalho, os cards agrupam seus textos para acessibilidade e não há altura fixa
ou truncamento dos valores. A tela usa os insets do Android.

Diferenças conscientes: renda verde, tipografia e espaçamentos nativos, detalhes
em cards verticais e nenhum botão de cadastro, histórico, gráfico ou drill-down.
O tema é claro nesta etapa. Não houve conversão de HTML/CSS ou biblioteca de design.

Previews ficam somente em `src/debug`, com dados em memória, sem acesso ao banco.
Testes JVM verificam formatação, estados Compose por semântica e integração com o
ViewModel. A validação visual foi feita em emulador Android 35 x86_64 isolado:
abertura real com banco vazio; previews normal, sem renda, loading e erro;
rolagem com 14 fontes/6 categorias; largura 320 dp com fonte a 160%.
As capturas e o emulador ficam em `app/build/android-validation` (ignorado pelo Git).
Nenhum dado fictício foi inserido no banco.

Dependências: `lifecycle-runtime-compose:2.9.4` declarado diretamente (já transitivo),
`ui-test-junit4` para testes e `ui-test-manifest` somente em debug, ambos alinhados
ao BOM Compose existente. Nenhuma versão existente foi atualizada.

Referências: [coleta de estado com lifecycle](https://developer.android.com/develop/ui/compose/state)
e [testes semânticos Compose](https://developer.android.com/develop/ui/compose/testing).
