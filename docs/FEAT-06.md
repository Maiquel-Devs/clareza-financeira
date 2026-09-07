# FEAT 06 — Cadastro e edição de movimentações

O botão **+ Adicionar** do Dashboard abre um único formulário, com escolha de Receita ou Despesa. Despesas pedem descrição, categoria fixa, valor, frequência e data; rendas não possuem categoria. O formulário é rolável e considera o teclado. Salvar valida, bloqueia cliques repetidos e aguarda a escrita; erros mantêm os campos preenchidos. A navegação volta ao Dashboard somente após sucesso. Voltar sem salvar não pede confirmação; durante a escrita, aguarda sua conclusão.

## Dados e arquitetura

- `EntryViewModel` cuida dos campos, validação e salvamento. `EntryInput`/`FinancialEntryStore` definem uma pequena fronteira de domínio para escrita. `FinancialEntryRepository` encapsula os DAOs. Composables não recebem entidades nem acessam Room.
- Pontual: insere apenas `MovementEntity`. A edição carrega pelo ID e mantém ID, tipo e `createdAt`, atualizando `updatedAt`, nome, valor, categoria e data. O Dashboard oferece ações de edição dos movimentos pontuais do período exibido, sem histórico ou drill-down.
- Mensal: chama o `RecurrenceDao.create` já existente, anotado com `@Transaction`, para inserir `RecurrenceEntity` e a primeira `RecurrenceVersionEntity`. `validFrom == startPeriod`, `endPeriod == null`. Se a segunda inserção falhar, Room desfaz a primeira. Não são criadas `MovementEntity` futuras.
- O Dashboard continua atualizado pelo fluxo de invalidação Room → repository de análise → ViewModel → UI. Não há recálculo manual após salvar nem regra financeira duplicada; `MonthlyAnalysisEngine` permanece responsável pelos cálculos.
- Navigation Compose tem somente Dashboard, criação e edição pontual por ID. O ViewModel pertence à entrada da navegação e mantém os campos em mudanças de configuração. Rascunhos não são persistidos após encerramento do processo.

## Dinheiro e data

`MoneyInput` aceita inteiros, vírgula decimal e agrupamento brasileiro (`10`, `10,50`, `1000`, `1.000,00`). Usa `BigDecimal` apenas na conversão exata para `Long` centavos, sem `Double`. Rejeita vazio, zero, negativos, formatos ambíguos e overflow. O formatter preserva a precisão inclusive no limite de `Long`.

A data pontual começa em `LocalDate.now(clock)`, com `Clock.systemDefaultZone()` injetado pelo container. O usuário altera pelo `DatePickerDialog` nativo do Android. Mês inicial começa no mês atual, com entrada numérica `MM/AAAA` e separador automático; dia habitual aceita 1 a 31. O dia não condiciona a previsão. Os campos mensais e pontuais nunca aparecem simultaneamente.

## Limites e diferenças conscientes

Recorrências existentes não oferecem edição nesta entrega: definir o alcance da alteração exige as decisões da FEAT 07. Nenhuma versão histórica é sobrescrita. A fronteira de escrita está separada para receber futuramente operações explícitas de escopo; não expõe um update genérico de recorrência. Exceções mensais, parada, exclusão, histórico e gráficos permanecem fora desta FEAT.

Em relação ao prototipo_12: escolha do tipo dentro do formulário, botão **+ Adicionar** com texto acessível, componentes Material/nativos e mês em campo numérico simples. Edição pontual por ações no fim do Dashboard, sem implementar telas de detalhamento antecipadamente. Mantida a identidade clara e azul, com verde para renda.

Dependências oficiais adicionadas: `androidx.navigation:navigation-compose:2.9.4` e `androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4`. Versões existentes de AGP, Gradle, Room e Compose BOM preservadas.

## Validação

`.\gradlew.bat test build`: 152 testes, 0 falhas (37 novos); build debug/release e lint concluídos com JDK 21.0.10 e Gradle 9.5.0. Lint: 0 erros, 8 avisos de versões disponíveis/ícone do app. Os testes novos cobrem parser, formulário, UI sem pixels, escrita Room, rollback real na segunda inserção, edição e atualização do Flow já observado.

Validação manual em emulador Android 35: quatro cadastros feitos pelo app (renda pontual R$ 100,50; despesa pontual R$ 20,50; renda mensal R$ 1.000,00; despesa mensal R$ 300,00, dia 31). Troca da data pontual para 06/09/2026 pelo seletor nativo. Após fechar/reabrir, Dashboard exibiu renda R$ 1.100,50, despesas registradas R$ 20,50, previsão R$ 320,50 e sobra R$ 780,00. Edição pontual para R$ 25,50 atualizou a categoria e a ação de edição automaticamente. Testados teclado decimal, rolagem até Salvar com teclado aberto, largura de 320 dp e fonte 1,6×. Não foi feita auditoria manual completa com TalkBack.

Também conferida a entrada `102026` → `10/2026` pelo teclado numérico, sem digitar a barra. Capturas locais em `app/build/android-validation/feat06-*.png` (ignoradas pelo Git). Os quatro registros de teste foram cadastrados exclusivamente pela interface e permanecem apenas no banco do emulador de validação; não há seed automático no app.

## Arquivos

Criados (caminhos Kotlin relativos a `app/src/main/java/com/clarezafinanceira/app`):

- `domain/FinancialEntryStore.kt`
- `data/repository/FinancialEntryRepository.kt`
- `presentation/entry/MoneyInput.kt`
- `presentation/entry/EntryViewModel.kt`
- `presentation/entry/EntryScreen.kt`
- `presentation/entry/EntryNavigation.kt`
- Testes em `app/src/test/java/com/clarezafinanceira/app`: `data/repository/FinancialEntryRepositoryTest.kt`, `presentation/entry/MoneyInputTest.kt`, `presentation/entry/EntryViewModelTest.kt`, `presentation/entry/EntryScreenTest.kt`.
- `docs/FEAT-06.md`.

Alterados: `app/build.gradle.kts`, `AppContainer.kt`, `MainActivity.kt`, `data/local/MovementEntity.kt` (nome não vazio), `presentation/dashboard/DashboardScreen.kt`.
