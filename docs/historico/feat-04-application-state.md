# FEAT 04 — Estado da aplicação

`MonthlyAnalysisViewModel` mantém `selectedPeriod` e publica `uiState` como
`StateFlow`: `Loading`, `Success` (com o próprio `MonthlyAnalysis`) ou `Error`
(com a causa original). Não calcula valores financeiros nem acessa persistência.

Fluxo: Room → MonthlyAnalysisRepository → MonthlyAnalysisViewModel → futura UI.
A interface `MonthlyAnalysisSource` contém somente a observação mensal. Ela permite
testes JVM com Flows controlados, sem precisar abrir Room ou simular seus DAOs.
O repository concreto implementa essa interface e mantém sua API de leitura pontual.

`selectPeriod` atualiza o mês; `flatMapLatest` cancela a coleta anterior e inicia
`Loading` para o novo período. Selecionar o mesmo mês não reinicia a observação.
As transições são assíncronas: a UI deve renderizar o período junto ao próprio
`uiState`, sem combinar uma análise anterior com o `selectedPeriod` mais recente.
`stateIn` compartilha a observação de forma imediata (`Eagerly`) durante a vida do
ViewModel, mesmo sem coletores de UI. `viewModelScope` cancela tudo ao encerrá-lo.
Erros são tratados por período; outro mês pode ser selecionado após uma falha.
Meses vazios permanecem `Success`. Não há fallback financeiro ou retry automático.

O mês inicial usa `YearMonth.now(clock)`. Em produção, o container fornece
`Clock.systemDefaultZone()`; nos testes, `Clock.fixed` controla instante e fuso,
inclusive na virada do mês. A seleção fica em memória durante a vida do ViewModel.

`ClarezaFinanceiraApplication`, registrada no Manifest, possui um `AppContainer`.
Ele reutiliza o singleton existente de `FinancialDatabase` com application context,
cria um único repository sob demanda e fornece `MonthlyAnalysisViewModelFactory`.
A futura tela pode obter o ViewModel com `ViewModelProvider` e a factory do container
da Application. A factory usa construção explícita, sem reflexão.
Não foi necessário alterar a tela provisória ou criar Dashboard.

Não há framework de DI: poucas dependências são resolvidas diretamente por
construtores. Lifecycle ViewModel 2.9.4, já transitivo, passa a ser dependência
direta. `kotlinx-coroutines-test:1.10.2` controla Main e o agendamento nos testes.
As versões existentes de ferramentas e bibliotecas foram preservadas.

Referência: [ciclo de vida do ViewModel](https://developer.android.com/topic/libraries/architecture/viewmodel).
