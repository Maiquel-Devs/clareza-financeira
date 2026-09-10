# Clareza Financeira — regras do produto

Este documento registra os comportamentos financeiros vigentes do MVP e os cuidados necessários para evoluí-los sem mudar silenciosamente seu significado. [Produto atual](produto-atual.md) apresenta o que o aplicativo é; aqui estão as regras que definem como ele funciona.

As regras abaixo foram conferidas no código e nos testes atuais. Documentos de FEATs são referências históricas e podem conter limitações já superadas.

## 1. Princípio financeiro e independência mensal

**Renda − Despesas = Sobra.**

O Clareza Financeira acompanha **fluxo financeiro**. Não acompanha saldo bancário, patrimônio acumulado, dinheiro disponível em contas ou patrimônio líquido. Os indicadores mensais não comprovam recebimento, pagamento ou disponibilidade bancária.

Cada mês é analisado individualmente. A sobra de um mês não se transforma automaticamente em saldo do seguinte. Não existe transporte de saldo entre períodos.

Uma movimentação pontual pertence ao mês de sua data. Alterá-la ou excluí-la deve mudar somente os resultados que dependem dela. Se a edição transferir a data para outro mês, tanto o mês de origem quanto o de destino passam a ter outra composição.

Essa independência é financeira: atualmente, a invalidação do Room ocorre por tabela e pode provocar nova leitura e cálculo de outros meses observados. O repository suprime emissões de análises iguais com `distinctUntilChanged`. Não há garantia de executar o cálculo somente para o mês alterado; há garantia de não transportar seus efeitos financeiros para meses independentes.

## 2. Renda e composição das despesas

A renda possui nome/origem e valor, sem categoria. A renda pontual entra no mês de sua data; a recorrente entra nos meses em que sua regra é válida, com a versão e a exceção aplicáveis.

| Indicador | Composição vigente |
| --- | --- |
| Renda considerada (`consideredIncomeCents`) | Renda pontual do mês + renda recorrente efetiva do mês. |
| Despesas registradas (`registeredExpenseCents`) | Somente despesas **pontuais** efetivamente registradas no período. |
| Previsão de despesas (`forecastExpenseCents`) | Despesas pontuais registradas + despesas recorrentes efetivas, já resolvidas as versões e exceções do período. |

O card **Renda** usa a renda considerada. A separação interna entre renda pontual (`registeredIncomeCents`) e recorrente (`recurringIncomeCents`) não retira a recorrente do cálculo da sobra ou do percentual.

**Despesas registradas não incluem automaticamente recorrências.** A previsão inclui as recorrências conhecidas e válidas, sem exigir que já tenha chegado seu dia habitual. Exceções podem modificar seus campos ou retirar uma ocorrência; seus efeitos são aplicados ao item recorrente, sem somar a exceção como um gasto separado.

Previsão não é adivinhação: o sistema não estima gastos que não foram informados ou derivados de uma regra fornecida pelo usuário.

### Registros independentes, sem conciliação automática

Uma despesa pontual e uma recorrência são registros independentes. O sistema não tenta descobrir se a pontual corresponde ao pagamento da recorrência, nem por nome, valor ou data. Se ambas forem válidas no período, ambas participam da análise. Uma futura “deduplicação inteligente” alteraria essa regra e não deve ser introduzida acidentalmente.

## 3. Sobra, percentual e ausência de dados

Quando a renda considerada é maior que zero:

- **Sobra prevista = renda considerada − previsão de despesas.**
- **Percentual da renda = previsão de despesas ÷ renda considerada × 100%.**

A sobra pode ser negativa e o percentual pode ultrapassar 100%. Esses resultados devem ser apresentados sem julgamento e sem limitar artificialmente seus valores. Com renda e nenhuma despesa, a sobra corresponde à renda e o percentual é zero; esse mês não é vazio.

O percentual da engine usa `BigDecimal`, quatro casas decimais e arredondamento `HALF_UP`; `150.0000` significa 150%. A apresentação pode reduzir as casas exibidas, sem usar o percentual arredondado para calcular dinheiro.

### Sem renda

Quando não há renda considerada, a sobra prevista e o percentual são `null`. Não se divide por zero nem se apresenta uma sobra negativa como se renda zero fosse uma renda informada. As despesas e sua previsão continuam visíveis. A interface informa **“Nenhuma renda informada neste mês.”** e apresenta a sobra como **“Não disponível”**.

Não é necessário cadastrar renda antes de registrar despesas.

### Período completamente vazio

Um período é vazio quando não possui nenhum item efetivo de renda ou despesa (`isEmpty`). Isso também pode ocorrer quando a única ocorrência foi excluída do período.

A engine devolve totais zero, listas e categorias vazias, sobra e percentual nulos. A interface usa esse estado para orientar a adicionar renda ou gasto, sem apresentar uma coleção de cards zerados como se existisse análise. Categorias sem despesas são omitidas.

Erro de leitura ou de integridade não equivale a período vazio. Não deve ser convertido em análise fictícia, valores de fallback ou sucesso sem dados.

## 4. Movimentações pontuais

O cadastro pontual começa com a data atual do relógio local da aplicação e permite escolher outra data. Na edição, carrega a data registrada. Um lançamento retroativo pertence ao período real informado, independentemente do instante em que foi cadastrado ou editado.

Nome, valor, categoria de despesa e data podem ser editados. A edição preserva a identidade e o tipo da movimentação; não a converte em recorrência. Renda continua sem categoria. A exclusão pontual exige confirmação e remove a contribuição desse registro.

Dashboard, categorias, detalhes e Histórico observam os dados atualizados. Se uma edição mover o item para outro período ou uma exclusão o retirar, o detalhe do período original pode ficar indisponível e permitir o retorno. Não se deve conservar um total antigo para simular uma fotografia histórica.

## 5. Recorrências e dia habitual

**O normal deve acontecer automaticamente. O usuário só precisa agir principalmente quando houver uma exceção.**

Uma recorrência mensal representa uma regra contínua fornecida pelo usuário. Seu cadastro define tipo (renda ou despesa), mês inicial, nome/origem, valor, dia habitual de 1 a 31 e categoria quando despesa. A primeira versão começa no mês inicial; inicialmente não há mês final. O cadastro sugere o mês atual e permite informar outro.

A recorrência participa automaticamente de cada período válido. Não exige confirmação mensal e não cria artificialmente uma coleção de movimentações pontuais futuras. A edição recorrente atua sobre nome, valor, categoria de despesa e dia habitual, com alcance explícito; não altera o tipo nem o início da recorrência.

O dia habitual informa quando o evento costuma acontecer. **Não é uma condição para aparecer na previsão mensal.** A engine não consulta o dia atual: uma regra com dia 31 participa inclusive em fevereiro, sem inventar uma data ajustada ou uma movimentação naquele mês.

### Resolução da ocorrência de um mês

1. O período deve ser igual ou posterior ao início e, quando houver término, igual ou anterior a ele. O término é inclusivo.
2. Uma exceção da mesma recorrência e do mesmo período com `excluded = true` retira a ocorrência e prevalece sobre quaisquer substituições de campos.
3. Usa-se a versão com o maior `validFrom` que não ultrapasse o período consultado. Versões futuras não são antecipadas.
4. Campos não nulos da exceção substituem os campos da versão somente naquele mês. Campos nulos herdam a versão.

Essa resolução vale para rendas e despesas. Uma recorrência ativa, não excluída e sem versão efetiva é uma inconsistência: a engine falha, em vez de inventar um valor.

## 6. Alterar somente um período

**“Somente em [mês/ano]”** grava uma exceção da ocorrência selecionada. Pode alterar nome, valor, categoria de despesa ou dia habitual sem modificar a versão base nem os outros meses.

Há no máximo uma exceção por recorrência e período. Editar uma existente reutiliza sua identidade. Campos que voltam a coincidir com a base deixam de ser substituições; uma exceção sem exclusão e sem campos substituídos é removida.

O modelo também representa a exclusão de uma ocorrência com `excluded = true`, sem apagar a recorrência. Isso é diferente de excluir toda a regra. O diálogo de alteração mensal edita os campos; não oferece uma ação independente de “pular mês”. A interrupção no mês inicial usa essa exclusão no caso descrito adiante.

## 7. Alterar a partir de um período

**“A partir de [mês/ano]”** cria uma versão com início de validade no período selecionado. Se já existe uma versão nesse marco, atualiza esse registro, sem duplicá-lo. Períodos anteriores permanecem com suas versões anteriores.

**Versões posteriores já programadas permanecem intactas.** A versão alterada vale até o próximo marco existente, respeitando também o término da recorrência. Por exemplo: uma regra de R$ 100 em agosto, alterada para R$ 160 em setembro, continua com R$ 200 em dezembro se essa versão de dezembro já estava programada. A alteração de setembro não se propaga sobre os campos da versão de dezembro.

Quando há exceção no mês editado, a operação permanente:

- aplica à versão base apenas os campos efetivamente editados em relação à ocorrência exibida;
- remove, na exceção desse mês, as substituições desses campos, permitindo que o novo valor base apareça;
- preserva as demais substituições desse mês e todas as exceções de outros períodos.

Assim, um valor excepcional não editado **não se torna permanente**. Se setembro tem valor excepcional de R$ 180 sobre base de R$ 100, mudar somente o nome permanentemente mantém os R$ 180 em setembro e a base de R$ 100 nos meses seguintes, na ausência de outro marco ou exceção. Exceções preservadas continuam sendo resolvidas sobre a versão aplicável a cada mês.

Salvar sem diferenças não cria versão nem exceção. A escrita de versão e o ajuste da exceção são atômicos: uma falha não deve deixar apenas metade da alteração aplicada.

## 8. Interromper e excluir recorrências

### Interromper (“Parar recorrência”)

Interromper encerra a participação futura e preserva os dados e a contribuição dos períodos anteriores ao alcance escolhido. O usuário decide o que acontece com o período selecionado:

| Escolha | Resultado |
| --- | --- |
| Manter em [mês/ano] | O término fica nesse mês; a recorrência deixa de participar a partir do seguinte. |
| Remover de [mês/ano] | O término fica no mês anterior; a recorrência deixa de participar já no mês selecionado. |

Se o período removido é o próprio mês inicial, o término fica igual ao início e uma exceção `excluded = true` retira essa primeira ocorrência. Isso mantém um intervalo válido sem deixar contribuição no mês inicial ou nos seguintes. Encerramento e exceção são gravados juntos.

Repetir uma interrupção não estende nem reabre uma recorrência já encerrada. Versões e exceções são preservadas, mas não fazem a recorrência participar fora da vigência. Não há fluxo de retomada ou lista específica de recorrências encerradas no MVP.

Ao operar pelo Histórico, o período de referência é o mês consultado, não o mês atual. As escolhas de alcance e interrupção mencionam mês e ano explicitamente, inclusive o mês seguinte quando pertinente.

### Excluir recorrência

Excluir é destrutivo: remove a existência da recorrência, suas versões e suas exceções, retirando sua participação de todos os períodos em que existia, inclusive anteriores. Não apaga movimentações pontuais independentes.

A confirmação informa a remoção de todos os períodos e que a ação não pode ser desfeita; a ação final é **“Excluir de todos os períodos”**. Cancelar não grava. Essa operação não deve ser apresentada como equivalente a interromper.

## 9. Categorias e identidade dos itens

As categorias de despesas do MVP são fixas:

| Categoria apresentada | Código persistido |
| --- | --- |
| Alimentação | `FOOD` |
| Moradia | `HOUSING` |
| Transporte | `TRANSPORT` |
| Lazer | `LEISURE` |
| Saúde | `HEALTH` |
| Outros | `OTHER` |

Renda não possui categoria; despesa exige uma categoria válida. Os códigos são explícitos, não posições de enum nem rótulos traduzidos. Alterar um texto de apresentação não deve alterar o significado do código armazenado.

**“Outros” é uma categoria válida escolhida para uma despesa. Um código desconhecido no banco não é “Outros”.** O conversor atual exige correspondência exata e lança erro para código desconhecido; não há conversão automática ou categorização corretiva silenciosa.

Totais por categoria e listas de gastos incluem os itens efetivos da previsão, pontuais e recorrentes, com as exceções aplicadas. A quantidade representa esses itens, não apenas linhas da tabela de movimentações. Categorias sem gastos não aparecem na análise.

Os detalhes usam origem (pontual ou recorrente), ID e período para identificar o item. Nome, valor e posição na lista não constituem identidade. Rendas são fontes individuais e abrem diretamente seu detalhe, sem agrupamento por categoria.

## 10. Histórico e gráficos

O Histórico consulta análises por período, agrupadas por ano, até o mês atual. Exibe somente meses com pelo menos uma renda ou despesa efetiva, em ordem decrescente. Um ano sem informação recebe uma mensagem própria, sem meses artificiais zerados. A consulta captura o limite atual ao ser criada; não há temporizador para atualizar esse limite na virada do mês com a tela continuamente aberta.

**Histórico não é necessariamente uma fotografia imutável.** Ele é calculado com os registros e regras aplicáveis. Edições retroativas e exclusões podem alterar seus resultados ou retirar um mês que ficou vazio. Preservar os períodos anteriores ao alterar ou interromper uma recorrência não significa tornar todos os dados históricos intocáveis.

O Dashboard aberto pelo Histórico permite consultar e editar os itens existentes, mantendo o período de referência. Não oferece `+ Adicionar`; registros retroativos podem ser cadastrados pelo formulário com a data ou o mês inicial escolhidos.

Gráficos são complementares e derivam da mesma análise:

- A distribuição por categoria representa a **previsão de despesas**, não somente despesas registradas. Seu percentual usa o total de despesas como denominador, não a renda; o arredondamento de apresentação distribui décimos para somar 100%.
- A evolução anual compara as previsões dos meses relevantes em ordem cronológica e escala comum. Com menos de dois meses ou sem despesas comparáveis, informa a limitação. Um mês relevante com renda e sem despesas pode aparecer com zero numa comparação válida.

Esses percentuais e escalas visuais não alteram os valores financeiros nem substituem o percentual da renda comprometida.

## 11. Fatos armazenados e dados derivados

As fontes primárias são movimentações pontuais, recorrências, versões e exceções, persistidas localmente. A análise mensal é produzida pela `MonthlyAnalysisEngine` a partir desses fatos; o repository lê um conjunto consistente de entradas antes de calcular.

Totais, previsão, sobra, percentuais, agrupamentos, resumos históricos e dados dos gráficos são **derivados**. Não são registros financeiros independentes nem fontes alternativas de verdade. Não há tabelas de Dashboard, gráficos ou análise mensal para armazenar esses resultados; uma evolução não deve criá-las apenas para substituir o cálculo dos fatos por resultados persistidos.

Dashboard, Histórico e detalhamento devem continuar usando a mesma resolução financeira. Uma apresentação mais simples não autoriza recalcular recorrências com regras diferentes ou ocultar inconsistências mediante valores inventados.

## 12. Valores monetários, datas e períodos

Valores monetários persistidos usam **`Long` em centavos**, inclusive valores de versões e substituições monetárias de exceções. Não usam `Float` ou `Double`. Os valores de entrada são positivos; a direção financeira vem do tipo renda/despesa. A sobra derivada pode ser negativa.

A entrada monetária faz conversão decimal exata e rejeita zero, negativos, formatos inválidos e valores fora do limite suportado. A engine usa soma e subtração exatas: overflow gera erro explícito, sem truncar, limitar ou produzir um total incorreto.

| Conceito | Representação e significado |
| --- | --- |
| Data pontual | `LocalDate`: data financeira escolhida; define o mês do registro. Persistida como dias desde a época Unix. |
| Mês/período | `YearMonth`: ano e mês da análise, início/término, versão ou exceção; persistido como mês proléptico ordenável. |
| Timestamps técnicos | `Instant`: criação/atualização conforme o registro; persistido como texto ISO-8601 com precisão de nanossegundos. Não define o período financeiro. |

O relógio de produção usa o fuso padrão do dispositivo para a data e o mês atuais. A engine recebe o período explicitamente e não consulta relógio. Não se deve trocar a data financeira por `createdAt`/`updatedAt`, nem transformar o dia habitual em uma data pontual para decidir a previsão.

## 13. Automação segura

**Automatizamos aquilo que o sistema consegue prever com segurança a partir das regras fornecidas pelo próprio usuário. Intervenção manual fica principalmente para acontecimentos novos ou exceções.**

Esse princípio não autoriza previsões financeiras especulativas, categorização automática não implementada, conciliação automática, aconselhamento financeiro ou IA tomando decisões financeiras pelo usuário.

## Referências para conferir alterações

- Contexto consolidado: [produto atual](produto-atual.md).
- Documentação de apoio: [análise mensal](analise-mensal.md), [persistência](persistencia-local.md), [repository/análise](repository-analise-mensal.md), [application state](feat-04-application-state.md), [Dashboard](feat-05-monthly-dashboard.md), [cadastro](FEAT-06.md), [recorrências](FEAT-07.md), [Histórico](FEAT-08.md), [detalhamento](FEAT-09.md), [gráficos](FEAT-10.md) e [revisão pré-FEAT 11](PRE-FEAT-11-UX-REVIEW.md).
- Código principal, sob `app/src/main/java/com/clarezafinanceira/app/`: `domain/MonthlyAnalysisEngine.kt`, `domain/FinancialModels.kt`, `domain/MonthlyAnalysis.kt`; repositories `FinancialEntryRepository`, `MonthlyAnalysisRepository`, `HistoryRepository`; `data/local/FinancialTypes.kt` e `FinancialConverters.kt`; formulários e diálogos em `presentation/entry/`.
- Testes de referência, sob `app/src/test/java/com/clarezafinanceira/app/`: `domain/MonthlyAnalysisEngineTest.kt` e `data/repository/RecurrenceRepositoryTest.kt`, especialmente ausência de renda, independência mensal, dia 31, marcos futuros, coexistência de versão e exceção, interrupção inicial, exclusão e precisão monetária.

## Regras que uma evolução futura não deve quebrar

- Fluxo financeiro não é patrimônio ou saldo bancário; meses não acumulam saldo.
- Despesas registradas são pontuais; previsão inclui regras recorrentes conhecidas e suas exceções, sem adivinhação ou conciliação automática.
- Recorrência normal não exige confirmação mensal nem geração de pontuais futuras.
- Exceção mensal não muda outros meses; alteração permanente respeita o passado, os marcos posteriores e os campos excepcionais não editados.
- Interromper preserva a existência e o histórico anterior; excluir retira a recorrência de todos os períodos e exige confirmação explícita.
- Ausência de renda não gera sobra ou percentual sem sentido; vazio e erro não se disfarçam de análise financeira.
- Centavos exatos e fatos armazenados sustentam os resultados; dados derivados não os substituem.
- Simplicidade para o usuário não pode ser obtida alterando silenciosamente o significado financeiro dos dados.
