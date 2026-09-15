# Clareza Financeira — arquitetura atual

Este documento descreve a organização técnica vigente do MVP. [Produto atual](produto-atual.md) explica o que o aplicativo é; [regras do produto](regras-do-produto.md) define os invariantes financeiros. Aqui estão as responsabilidades, dependências e limites necessários para evoluir o código sem criar complexidade desnecessária.

A direção arquitetural é **moderna, simples, sustentável, testável e adequada ao MVP**. A separação existente serve às necessidades do aplicativo; não é uma implementação cerimonial de Clean Architecture.

## 1. Visão geral: responsabilidades e fluxo real

O projeto possui um único módulo Android, `:app`. Apresentação, domínio e dados são separados por pacotes, não por módulos Gradle.

O fluxo de leitura da análise mensal é:

```text
Compose (Route / Screen)
    ↕ estado e ações
ViewModel
    ↓ contrato MonthlyAnalysisSource, definido em domain
MonthlyAnalysisRepository
    ├─ consulta MonthlyAnalysisDao → Room → SQLite
    └─ converte entidades → modelos de domínio
       → MonthlyAnalysisEngine → MonthlyAnalysis
    ↑ resultado entregue ao ViewModel e à UI
```

O domínio não é uma etapa que consulta o repository. Os ViewModels dependem de contratos definidos em `domain`; os repositories os implementam. O repository de análise coordena a leitura dos fatos e chama a engine pura. A engine não acessa banco, repository, Android, rede ou relógio.

Na escrita, o formulário envia uma intenção ao ViewModel, que valida a entrada e chama um contrato de escrita. `FinancialEntryRepository` coordena entidades, DAOs e transações. A alteração persistida provoca uma nova análise pelo fluxo de observação; a UI não calcula uma compensação financeira após salvar.

Não há uma classe de caso de uso para cada ação, repository genérico ou camada adicional obrigatória entre essas partes.

## 2. Stack e configuração

As tecnologias efetivamente declaradas no projeto são:

| Área | Tecnologia |
| --- | --- |
| Plataforma e linguagem | Android e Kotlin. |
| Interface | Jetpack Compose, Material 3 e Activity Compose. |
| Navegação e estado | Navigation Compose, Lifecycle ViewModel e integração Lifecycle/Compose. |
| Assincronia | Kotlin Coroutines, Flow e StateFlow. |
| Persistência | Room sobre SQLite; processamento do Room por KSP. |
| Build | Gradle Wrapper e scripts Kotlin DSL (`.gradle.kts`). |
| Testes | JUnit 4, Robolectric, Coroutines Test e testes Compose. |

O módulo configura `minSdk = 26`, `compileSdk = 37`, `targetSdk = 37` e compatibilidade Java 17. O Wrapper aponta para Gradle 9.5.0. As versões de plugins ficam em `build.gradle.kts`; as dependências estão diretamente em `app/build.gradle.kts`, com BOM para Compose. Não há catálogo `libs.versions.toml`.

Os arquivos Gradle são a referência para versões de ferramentas e bibliotecas. Esta documentação não implica atualização de dependências.

## 3. Onde procurar no código

O pacote base é `com.clarezafinanceira.app`, sob `app/src/main/java/`.

| Diretório/pacote | Responsabilidade |
| --- | --- |
| Raiz do pacote | `Application`, `MainActivity` e composição das dependências em `AppContainer`. |
| `presentation/` | Estado e ViewModel da análise mensal. |
| `presentation/dashboard/` | Dashboard, componentes, tema e formatação de apresentação. |
| `presentation/about/` | Tela estática Sobre e abertura segura da URL pública do projeto. |
| `presentation/entry/` | Formulários pontuais/recorrentes, validação de entrada, diálogos e grafo de navegação. |
| `presentation/history/` | Seleção anual e estado/apresentação do Histórico. |
| `presentation/detail/` | Categorias, detalhes dos itens e estado de exclusão pontual. |
| `presentation/charts/` | Projeções visuais e gráficos Compose. |
| `domain/` | Modelos financeiros, engine e contratos de leitura/escrita usados pelos ViewModels. |
| `data/repository/` | Coordenação de persistência, análise e projeções de Histórico/detalhes. |
| `data/local/` | Banco Room, entidades, DAOs, conversores e mapeamento para domínio. |
| `app/src/main/res/` | Recursos Android, textos e tema de inicialização. |
| `app/src/test/` | Testes locais de domínio, dados e apresentação. |
| `app/src/debug/` | Previews com dados em memória, fora do código de produção. |
| `app/schemas/` | Schema exportado pelo Room. |

O diretório `Prototipo/` é referência histórica; não é módulo Android nem implementação da interface de produção.

## 4. Apresentação, navegação e estado

`MainActivity` é uma `ComponentActivity` que instala o conteúdo Compose e o tema. As funções `Route` coletam os estados dos ViewModels com `collectAsStateWithLifecycle`; telas e componentes recebem estados e callbacks. A renderização não recebe entidades Room nem acessa DAOs.

O grafo Navigation Compose está em `presentation/entry/EntryNavigation.kt`, apesar de abranger também Dashboard, Histórico, categorias e detalhes. As rotas carregam identificadores, origem do item e período conforme necessário, não cópias serializadas dos valores financeiros. O detalhe observa novamente o item efetivo pela referência recebida.

No REF03, o menu de overflow do Dashboard principal abre a rota `about` no mesmo NavHost. `AboutRoute`/`AboutScreen` reutilizam o tema e o ícone adaptativo oficial, sem ViewModel, repository ou acesso ao banco. `openProjectPage` lança `Intent.ACTION_VIEW` com a URL HTTPS fixa da página do projeto, sem extras ou parâmetros financeiros; trata `ActivityNotFoundException` e `SecurityException`, permitindo à tela informar a falha. Não há WebView, nova dependência ou mudança nas permissões/backup.

O ViewModel do Dashboard principal pertence à Activity. Os demais são criados no escopo das entradas de navegação; o Dashboard histórico recebe seu próprio `MonthlyAnalysisViewModel` e período. Isso evita substituir a seleção do Dashboard principal ao consultar outro mês.

`MonthlyAnalysisViewModel` expõe `selectedPeriod` e `uiState`. O estado mensal distingue `Loading`, `Success` e `Error`, sempre associado ao período. A UI deve usar o período do próprio estado, sem combinar uma análise antiga com uma seleção mais recente. Mês vazio continua sendo sucesso; erro não se transforma em análise zerada.

Histórico e detalhes usam estados específicos. Formulários mantêm campos, validação e situação de gravação, bloqueando envios repetidos durante a operação. As gravações são lançadas em `viewModelScope`; erros preservam os campos e permitem nova tentativa, sem apresentar sucesso antes da conclusão.

A seleção anual usa `SavedStateHandle`. Estados visuais, como expansão dos gráficos e posição de listas, usam mecanismos de estado salvo do Compose/Navigation. Campos dos formulários ficam no ViewModel durante sua vida, mas não há persistência de rascunhos após encerramento do processo. Esses estados de apresentação não são fatos financeiros.

## 5. Domínio e engine financeira

`MonthlyAnalysisEngine.analyze` recebe um `YearMonth` e listas de movimentações, recorrências, versões e exceções. Resolve os itens efetivos e devolve `MonthlyAnalysis`, incluindo totais, agrupamentos e itens que sustentam o detalhamento.

É um cálculo puro e determinístico: não grava resultados, não consulta relógio e não contém textos ou moeda formatada. Valida entradas efetivas, duplicidades, existência de versão aplicável e operações monetárias exatas. Inconsistências e overflow geram erro, sem inventar resultados financeiros.

Os modelos de domínio não são entidades Room. `DomainMappings.kt` converte os fatos persistidos e seus enums explicitamente para os modelos usados pela engine; timestamps técnicos permanecem na camada de dados.

Os contratos em `domain`, como `MonthlyAnalysisSource`, `HistorySource`, `FinancialDetailSource` e os contratos de escrita, permitem testar estado e ações sem abrir banco. Não existe obrigação de criar uma interface para cada classe: essas fronteiras correspondem aos consumidores atuais.

As regras de edição temporal são coordenadas pelo repository de escrita; a engine resolve a análise. Portanto, centralizar o cálculo financeiro não significa afirmar que todo comportamento de escrita está dentro da engine. Os invariantes completos estão em [regras do produto](regras-do-produto.md).

## 6. Repositories

| Repository | Papel atual |
| --- | --- |
| `MonthlyAnalysisRepository` | Oferece leitura pontual e observação mensal; lê snapshot, converte entidades e chama a engine. |
| `FinancialEntryRepository` | Implementa cadastro/edição pontual, criação/alteração/interrupção/exclusão recorrente e exclusão pontual pelos contratos de escrita. |
| `HistoryRepository` | Combina até 12 observações mensais de um ano, limita ao mês atual e projeta os meses relevantes. |
| `FinancialDetailRepository` | Projeta categorias e itens a partir de `MonthlyAnalysisSource`, sem reconstruir regras de recorrência. |

O repository de análise executa mapeamento e cálculo em `Dispatchers.Default`; as consultas `suspend` usam a execução oferecida pelo Room. O cálculo acontece fora da transação de leitura, sobre o conjunto de fatos já obtido.

Histórico e detalhes não consultam diretamente suas próprias tabelas nem possuem regras financeiras alternativas. Seus dados vêm da mesma fonte mensal usada pelo Dashboard.

Para carregar a edição recorrente, `FinancialEntryRepository` também usa a engine para resolver o item no período. As operações de escrita tratam alcance, versões e exceções dentro de transações, sem gerar movimentações pontuais futuras.

## 7. Persistência, entidades e schema

`FinancialDatabase` é o banco Room sobre SQLite. Usa o arquivo `clareza-financeira.db`, versão **1**, `exportSchema = true` e uma instância compartilhada por processo, construída com `applicationContext`.

O schema exportado está em `app/schemas/com.clarezafinanceira.app.data.local.FinancialDatabase/1.json`. O KSP recebe esse diretório pelo build do módulo. Não há migrações registradas nem fallback destrutivo no builder atual; uma mudança futura de schema precisa tratar a evolução de versão e a preservação dos dados.

| Entidade | Fato representado |
| --- | --- |
| `MovementEntity` | Renda ou despesa pontual, com valor e data financeira. |
| `RecurrenceEntity` | Identidade, tipo e vigência da regra mensal. |
| `RecurrenceVersionEntity` | Nome, valor, categoria e dia habitual válidos a partir de um marco mensal. |
| `RecurrenceExceptionEntity` | Substituições de campos ou exclusão de uma ocorrência em um período específico. |

Versões e exceções referenciam a recorrência por chave estrangeira com exclusão em cascata. Há unicidade para recorrência + início de versão e recorrência + período de exceção. Os identificadores são strings; o repository de escrita usa UUID por padrão e fornece timestamps pelo relógio injetado.

`MovementDao` concentra operações pontuais. `RecurrenceDao` oferece operações sobre a regra e seus filhos. `MonthlyAnalysisDao` lê candidatos para a análise: filtra datas, vigência, versões elegíveis e exceções do mês. O SQL reduz o conjunto de entrada; não soma indicadores nem escolhe a versão efetiva no lugar da engine.

### Integridade distribuída entre banco e Kotlin

SQLite garante chaves, nulabilidade, unicidade e cascatas. Outras validações são feitas em entidades, métodos de DAO, repository e engine; não estão integralmente codificadas como restrições SQL.

Os métodos atuais `putVersion` e `putException` são `@Upsert` diretos. Eles não repetem todas as verificações relacionais dos métodos `create`, `addVersion` e `addException`. Por isso, uma nova escrita não deve assumir que qualquer chamada isolada ao DAO valida toda a operação de produto. Os fluxos de edição vigentes passam pela coordenação do `FinancialEntryRepository`.

## 8. Recorrências e ausência de entidade mês

```text
RecurrenceEntity + RecurrenceVersionEntity + RecurrenceExceptionEntity
    → modelos de domínio
    → ocorrência efetiva do período, resolvida pela engine
    → itens e totais de MonthlyAnalysis
```

Não há `MonthEntity`: mês é uma chave temporal para selecionar e resolver fatos, não um registro financeiro independente. Uma recorrência não precisa ser expandida em linhas futuras de movimentação para participar da análise.

`MonthlyAnalysisSnapshot` também não é uma entidade: é o conjunto de entradas retornado pelo DAO em uma leitura consistente. `MonthlyAnalysis`, Dashboard, totais, sobra, percentuais, resumos mensais e gráficos são derivados em memória.

Resultados derivados não precisam virar tabelas. Persisti-los como fonte alternativa exigiria uma decisão arquitetural futura deliberada e justificada, incluindo como manter sua consistência com os fatos. O MVP não possui esse mecanismo nem cache persistente de análises.

## 9. Flow, Coroutines e atualização reativa

O caminho efetivo de atualização é:

```text
Escrita pelo repository / DAO → Room / SQLite
    → InvalidationTracker.createFlow nas quatro tabelas de fatos
    → MonthlyAnalysisRepository relê snapshot e chama a engine
    → Flow<MonthlyAnalysis>
    → ViewModel / StateFlow
    → Compose
```

Embora os DAOs também exponham consultas com Flow, a análise mensal não combina quatro Flows de entidades. Ela observa invalidações e relê as quatro listas dentro de uma única transação, evitando montar uma análise com partes de momentos diferentes.

A observação do repository é fria, emite a análise inicial e usa `distinctUntilChanged` para suprimir resultados iguais. A invalidação é por tabela: uma escrita em outro mês pode provocar nova leitura e cálculo, sem mudar o resultado financeiro do período observado. O fluxo representa estado atual, não um log de cada gravação; alterações rápidas podem ser agrupadas.

Nos ViewModels de análise e Histórico, `flatMapLatest` troca a observação ao selecionar mês/ano. Análise, Histórico e detalhes compartilham seus estados com `stateIn` e `SharingStarted.Eagerly` no `viewModelScope`. A observação permanece ativa enquanto esses ViewModels existem, inclusive em entradas mantidas na pilha, e é cancelada quando seu escopo termina. Coleta da UI vinculada ao lifecycle não significa, aqui, desligar automaticamente a fonte do ViewModel.

O repository não mantém escopo próprio, polling ou cache global compartilhado. Reutilizar a instância do repository não compartilha automaticamente todas as coletas mensais entre telas. No REF02, `HistoryRepository` observa o `CurrentPeriod` compartilhado e recompõe os meses elegíveis e a marcação ATUAL quando o calendário muda, mesmo sem escrita no banco.

`CurrentPeriod` usa o `Clock` existente e expõe o mês atual em `StateFlow`. `MainActivity` executa sua observação em `repeatOnLifecycle(STARTED)`: atualiza ao entrar em primeiro plano e mantém uma única espera até o início do próximo mês no fuso do relógio. A espera é cancelada em segundo plano e recalculada no retorno, sem polling periódico. O Dashboard principal acompanha esse estado; `selectPeriod` fixa uma consulta explícita, inclusive se o período escolhido era o atual. O Histórico acompanha o ano até uma seleção explícita do usuário; essa intenção fica no `SavedStateHandle`, enquanto o limite do ano atual continua reativo. Formulários não observam esse estado e conservam seus rascunhos.

## 10. Transações e tratamento de falhas

As fronteiras atômicas relevantes são:

- leitura das quatro listas de fatos em `MonthlyAnalysisDao.snapshot`, com `@Transaction`;
- criação da recorrência com a primeira versão, em `RecurrenceDao.create`;
- leitura, decisão e escrita de alterações recorrentes em `FinancialEntryRepository`, com `Room.withTransaction`;
- atualização conjunta de versão e exceção, ou término e exclusão da primeira ocorrência, quando a operação exige ambas;
- exclusão recorrente, com remoção dos filhos pelas chaves estrangeiras.

Essas fronteiras evitam análise de entradas parcialmente lidas ou regras parcialmente gravadas. Os testes incluem falhas injetadas para confirmar rollback de operações relacionadas.

Erros de leitura, conversão e engine propagam até o estado de erro da apresentação. A análise mensal preserva a causa no estado; a UI mostra mensagem humana. Não existe fallback financeiro ou retry automático na análise. Formulários e exclusões possuem estados próprios de falha e nova tentativa. Cancelamento de coroutines é preservado nos fluxos de escrita.

## 11. Dinheiro, datas e conversores

Dinheiro usa **`Long` em centavos** na persistência e nos cálculos monetários da engine, com soma/subtração exatas. `MoneyInput` converte entrada decimal para centavos usando `BigDecimal`; `DashboardFormatting` formata nas fronteiras de apresentação. Percentuais usam aritmética decimal e não servem de base para recalcular dinheiro.

`Float` aparece nas proporções geométricas das barras dos gráficos, não como armazenamento do valor monetário. `ChartData` usa os valores derivados existentes e `BigInteger`/`BigDecimal` para projeções e percentuais visuais; não consulta banco.

| Tipo | Uso e conversão persistida |
| --- | --- |
| `LocalDate` | Data financeira pontual; inteiro de dias desde a época Unix. |
| `YearMonth` | Período da análise e marcos de recorrência; inteiro de mês proléptico. |
| `Instant` | Timestamps técnicos; texto ISO-8601 que preserva nanossegundos. |

`createdAt` e `updatedAt` não definem o mês financeiro. O relógio de produção é `Clock.systemDefaultZone()`, fornecido às partes que precisam de data atual ou timestamps. Testes usam relógios fixos; a engine recebe o período sem consultar relógio.

Enums persistidos usam códigos explícitos, não ordinais nem rótulos traduzidos. O conversor exige código conhecido e falha caso não encontre correspondência; não transforma categoria inválida em “Outros”.

## 12. Construção das dependências

`ClarezaFinanceiraApplication`, registrada no Manifest, possui um `AppContainer` criado sob demanda. Ele mantém acesso ao banco singleton, relógio, repositories de análise, escrita e detalhe, além da factory do ViewModel mensal.

`MainActivity` obtém o container pela Application e cria o ViewModel principal com `ViewModelProvider`. `EntryNavigation` usa factories com `initializer` e construtores explícitos para os ViewModels das rotas. `HistoryRepository` é criado na factory do Histórico, usando a fonte mensal, o relógio e o `CurrentPeriod` compartilhado do container; não é uma propriedade do container.

Isso é fornecimento manual de dependências. Não existem Hilt, Koin, Dagger ou outro framework de DI. As interfaces pequenas permitem substituição nos testes, e a composição explícita atende ao tamanho atual do projeto. Um framework não deve ser adicionado apenas para “modernizar”.

## 13. Estratégia de testes

Os testes estão em `app/src/test/`, organizados por domínio, dados e apresentação:

- **Engine e conversões:** JUnit puro protege regras mensais, versões/exceções, precisão, independência temporal e erros de integridade.
- **Persistência e repositories:** Room real sob Robolectric, com SQLite nativo, verifica consultas, reatividade, chaves, cascatas, rollback e reabertura de banco em arquivo. Há bancos em memória para isolamento.
- **ViewModels e formulários:** contratos substituídos por implementações controladas, `Clock.fixed` e Coroutines Test verificam estados, cancelamento, validação, erros e bloqueio de operações repetidas.
- **Atualização temporal (REF02):** relógio controlado por tempo virtual verifica viradas de mês/ano, retorno pelo lifecycle, limites e ATUAL no Histórico, seleções explícitas fixas e preservação dos rascunhos.
- **Compose e navegação:** testes locais com Robolectric verificam semântica, estados, passagem do período, edição/exclusão, navegação e restauração de scroll. Não dependem de comparação de pixels.
- **Gráficos e formatação:** verificam precisão, projeções visuais, escala e estados sem dados apropriados.

O build habilita recursos Android nos testes locais e inclui ferramentas de teste Compose. Esses testes não exigem emulador; as validações visuais manuais registradas na documentação complementam sua cobertura. Previews debug não gravam dados e não substituem testes.

A referência técnica atual é o REF06A: **301 testes em 29 classes, todos aprovados, com 0 falhas, 0 erros e 0 ignorados**. Builds Debug e Release foram aprovados; o Android Lint registrou 0 erros e 11 warnings de versões disponíveis. O [registro do RC1](produto-atual.md#estado-atual) reúne também a aprovação física do REF06B e as observações conhecidas. O marco anterior de **278 testes** permanece na [fotografia histórica do MVP](estado-do-mvp.md); estes resultados não representam nova execução no fechamento documental REF06C.

## 14. Limites da arquitetura atual

O MVP não possui backend próprio, API remota do produto, autenticação, sincronização própria/cloud, IA ou integração com serviço financeiro externo. Não há módulos separados de domínio/dados, framework complexo de DI ou biblioteca externa de gráficos.

Na REF05, o Manifest mantém `android:allowBackup="true"` para permitir transferência nativa, mas aponta para regras explícitas. `res/xml/data_extraction_rules.xml` exclui todos os domínios do cloud backup no Android 12+ e permite somente `clareza-financeira.db` em `device-transfer`. A inclusão de um banco também contempla journal/WAL pelo mecanismo do Android; as quatro tabelas financeiras permanecem uma unidade. Outros arquivos e bancos futuros não entram automaticamente nessa lista de transferência.

No Android 9–11, `res/xml-v28/backup_rules.xml` inclui somente esse banco com `requireFlags="deviceToDeviceTransfer"`: transporte sem essa condição não recebe os dados. No Android 8/8.1, `res/xml/backup_rules.xml` exclui todos os domínios como fallback conservador. As regras não criam sincronização, exportação ou servidor. A disponibilidade da transferência depende do sistema/fabricante; perder o aparelho pode significar perder o histórico. A configuração não comprova remoção retroativa de cópias feitas por versões anteriores.

Referências: [Auto Backup e regras por versão](https://developer.android.com/identity/data/autobackup) e [tratamento de bancos/journal/WAL no Android](https://github.com/aosp-mirror/platform_frameworks_base/blob/master/core/java/android/app/backup/FullBackup.java). Testes locais verificam o contrato e a seleção de recursos; não substituem homologação de transferência real entre aparelhos.

## Referências internas

- [Produto atual](produto-atual.md) e [regras do produto](regras-do-produto.md).
- [Análise mensal](tecnico/analise-mensal.md), [persistência local](tecnico/persistencia-local.md), [repository/análise](tecnico/repository-analise-mensal.md) e [application state](historico/feat-04-application-state.md).
- [Dashboard](historico/feat-05-monthly-dashboard.md), [cadastro](historico/FEAT-06.md), [recorrências](historico/FEAT-07.md), [Histórico](historico/FEAT-08.md), [detalhamento](historico/FEAT-09.md), [gráficos](historico/FEAT-10.md) e [revisão pré-FEAT 11](historico/PRE-FEAT-11-UX-REVIEW.md).

Os documentos históricos que descrevem ausência de repository, interfaces, UI ou edição recorrente retratam etapas anteriores. Hoje existem contratos de domínio, repositories de leitura/escrita e `upserts` para edição de versões/exceções. Essas limitações antigas não definem a arquitetura vigente.

## Decisões arquiteturais que devem ser preservadas

- Manter arquitetura suficiente para o problema, sem cerimônia ou tecnologia por moda.
- Centralizar a resolução financeira na engine; UI, Histórico e detalhes não reinventam o cálculo.
- Preservar fatos financeiros como fonte da análise; resultados derivados não precisam virar tabelas.
- Manter `Long` em centavos e conversões exatas nas fronteiras.
- Respeitar período, identidade e consistência transacional ao ler ou alterar recorrências.
- Distinguir estado vazio, indisponibilidade e erro, sem fabricar resultados.
- Preservar contratos testáveis e composição explícita enquanto atenderem às necessidades reais.
- Exigir um problema concreto para justificar novas camadas, interfaces, módulos ou frameworks.
