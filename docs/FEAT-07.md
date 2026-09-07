# FEAT 07 — Recorrências e exceções mensais

`RecurrenceStore` expressa as quatro intenções de edição. O `FinancialEntryRepository`
as persiste com `Room.withTransaction`; o formulário não acessa DAOs. A leitura do
formulário usa a própria `MonthlyAnalysisEngine`, sem duplicar a resolução financeira.

- **Somente no período:** grava os campos modificados em uma `RecurrenceExceptionEntity`
  identificada por recorrência + `YearMonth`. Reutiliza ID e data de criação da exceção
  existente; campos iguais à base deixam de ser overrides. Exceções vazias são removidas.
- **A partir do período:** cria uma `RecurrenceVersionEntity` com `validFrom` no período.
  Se o marco já existe, atualiza esse mesmo registro. Versões anteriores não são
  sobrescritas e marcos posteriores permanecem intactos.
- **Versão + exceção:** a alteração permanente aplica apenas os campos editados à
  versão base. Remove os overrides desses campos no período selecionado, para que a
  alteração seja visível nele; preserva os demais overrides e todas as exceções de
  outros períodos. Um valor excepcional não editado nunca vira base permanente.
- **Parar:** `endPeriod` é inclusivo. Manter o mês encerra nele; remover o mês encerra
  no anterior. No `startPeriod`, remover encerra no próprio início e grava uma exceção
  `excluded = true`, sem overrides. Não existe intervalo inválido. Repetir uma parada
  não estende uma recorrência já encerrada. Versões e exceções históricas são mantidas.
- **Excluir:** confirmação explícita informa a remoção de todos os períodos, inclusive
  anteriores. Exclui fisicamente o pai; as FKs existentes removem versões e exceções
  por cascata. É uma ação distinta da parada, com cor destrutiva.

Leitura, decisão e escrita de cada operação são transacionais. Testes com falhas
injetadas verificam rollback de versão + exceção e encerramento + exclusão do mês.
Os índices únicos existentes evitam duplicidade; não houve migração ou dependência nova.
Nenhuma `MovementEntity` futura é criada. Room/Flow atualiza naturalmente o Dashboard.

O escopo aparece após validar o formulário e somente quando há diferenças. Cancelar
não escreve. Durante uma escrita, novos cliques e navegação de retorno ficam bloqueados.
Erros mantêm os campos, exibem mensagem humana e permitem tentar novamente. O formatter
`YearMonth.portuguesePeriod()` inclui mês e ano em português brasileiro.

## Limites

O Dashboard reutiliza o acesso temporário de edição, agora incluindo recorrências do
período analisado. Não há conversão para pontual, lista de recorrências encerradas,
retomada de recorrência, navegação histórica definitiva ou drill-down da FEAT 09.
Não há gráficos, previsões novas, fixtures automáticas ou mudanças na engine.

## Validação

- `./gradlew.bat test`: 191 testes aprovados, 39 novos.
- `./gradlew.bat build lint`: aprovado; lint com zero erros e oito avisos de versões
  de ferramentas/dependências e ícone de aplicação ausente, fora do escopo desta FEAT.
- Testes temporais usam Room/SQLite real em memória e a engine existente; testes de
  ViewModel e Compose cobrem escolhas, confirmações, cancelamento, erros e repetição.
- Emulador: criação de despesa mensal, exceção de R$ 100 para R$ 150, alteração
  permanente para R$ 170, atualização automática do Dashboard, parada mantendo setembro,
  cancelamento, exclusão completa e parada removendo setembro no próprio `startPeriod`
  aprovados. Edição, exclusão e parada persistiram ao fechar/reabrir o app. Tela 840×1890 e fonte 160%:
  formulário rolável e confirmações legíveis, sem botões inacessíveis. Configuração
  original restaurada. Capturas locais em `app/build/android-validation/feat07-*.png`.
- Consultas de agosto/outubro e preservação de marcos futuros foram validadas nos testes
  de integração; não pela UI do emulador, pois ainda não há navegação histórica.
  Dados de teste foram criados apenas por interação no emulador, sem fixtures automáticas.
