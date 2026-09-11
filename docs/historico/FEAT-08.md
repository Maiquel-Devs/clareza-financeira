# FEAT 08 — Histórico mensal

O histórico localiza meses e abre a análise existente para responder como estava a situação financeira naquele período. O acesso fica junto ao período no Dashboard atual.

## Consulta e relevância

`HistoryRepository` combina até 12 flows de `MonthlyAnalysisSource`, em ordem decrescente. Só entram análises com `isEmpty == false`: renda ou despesa efetiva, pontual ou recorrente. A engine existente resolve início, encerramento, versões e exceções; excluir a única ocorrência deixa o mês vazio e remove o card. Não há regras financeiras novas nem mudanças na engine.

Os cards projetam renda considerada, previsão de despesas e sobra prevista. Ausência de renda mantém a sobra nula (“Não disponível”), sem produzir saldo negativo artificial. A formatação usa `DashboardFormatting`.

O relógio da aplicação limita a consulta ao mês atual, marcado como ATUAL. O seletor rejeita anos futuros e a navegação também verifica o período antes de abri-lo. Um ano sem itens apresenta uma mensagem, sem cards zerados.

## Navegação e estado

`HistoryViewModel` guarda o ano no `SavedStateHandle`, troca a consulta com `flatMapLatest` e mantém o Flow ativo enquanto sua entrada existe na pilha. Isso evita substituir os cards por carregamento ao voltar de uma consulta longa. Alterações no Room atualizam os cards naturalmente, sem polling.

`dashboard/{period}` instancia o mesmo `MonthlyAnalysisViewModel` no escopo da entrada e seleciona o `YearMonth` recebido. Reutiliza `DashboardRoute` e `DashboardScreen`; o Dashboard atual mantém seu ViewModel e seu período. O modo histórico mostra voltar e oculta Histórico e + Adicionar, inclusive ao consultar o mês atual. Os callbacks de edição existentes continuam disponíveis.

O `rememberLazyListState` é salvo pelo estado da entrada do Navigation Compose. Voltar preserva índice e deslocamento; mudar de ano cria uma lista no topo. Uma nova entrada de Dashboard começa no topo. O ano também sobrevive à recriação pelo estado salvo.

Nenhum resumo é persistido: sem tabela, entidade, migração, cache em disco ou dependência nova. Permanecem apenas os fatos financeiros já persistidos.

## Limites e validação

Sem gráficos, comparações, filtros, exportação ou adição pelo Dashboard histórico. A consulta usa no máximo 12 observadores mensais, priorizando simplicidade. O limite temporal é capturado ao criar a consulta; não há temporizador para virada de mês com a tela continuamente aberta.

`HistoryRepositoryTest`, `HistoryViewModelTest` e `HistoryNavigationTest` adicionam 26 testes, incluindo Room real sob Robolectric, reatividade e navegação Compose com restauração do valor semântico de scroll. Os 191 anteriores continuam aprovados: total de 217, sem falhas. `./gradlew.bat test build lint` passou; lint: zero erros e oito avisos de dependências/ícone, fora dos arquivos desta feature.

Teste manual no `emulator-5556`: acesso pelo Dashboard, ATUAL, ordenação, troca para 2025, abertura de novembro no mesmo Dashboard, ausência do FAB e retorno à mesma posição confirmados. Fechar e reabrir manteve os dados e valores. Layout inspecionado também em 320dp com fonte de 160%; resolução e fonte originais restauradas. Foi cadastrada pela UI a receita recorrente identificada `FEAT08 historico`, somente no banco de desenvolvimento do emulador, para disponibilizar vários meses. Nenhuma fixture automática foi adicionada ao aplicativo. O estado de ano vazio foi validado pelos testes automatizados.
