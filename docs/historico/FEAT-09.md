# FEAT 09 — Detalhamento financeiro

O Dashboard agora abre a categoria de despesa e sua lista de gastos; cada gasto abre o detalhe. Rendas abrem diretamente o detalhe, porque já são itens individuais no Dashboard. O acesso provisório “Editar movimentações” foi removido. Cards clicáveis usam Surface do Material, feedback de toque e chevron discreto.

## Dados efetivos

`FinancialDetailRepository` projeta `MonthlyAnalysisSource`: filtra `expenseItems` por categoria, usa o total de `expensesByCategory` e localiza detalhes em `incomeSources`/`expenseItems`. A contagem corresponde aos itens efetivos. `AnalysisItem` já contém valor, nome, categoria, data e dia habitual resolvidos pela engine, incluindo versões e exceções. Uma ocorrência excluída ou fora da vigência não aparece. Nenhuma regra financeira é reconstruída.

`FinancialItemReference` leva origem, ID e YearMonth na navegação; nomes, valores e posições não identificam itens. Dados financeiros não são serializados nas rotas: o detalhe observa novamente o item efetivo. Nenhuma entidade, tabela, migração, cache persistente ou dependência foi adicionada. A engine permanece intacta, sem geração de movimentações futuras.

Pontuais mostram data real e período visualizado; recorrentes mostram dia habitual e período. Receitas não mostram categoria. Na retomada, foi acrescentado o campo explícito de período aos pontuais e ampliadas as asserções dos testes existentes, mantendo a quantidade de testes.

## Edição e exclusão

Editar reutiliza `EntryRoute` para pontuais e `RecurrenceRoute` para recorrentes. O YearMonth segue do Dashboard até o formulário recorrente, inclusive quando a navegação começa no Histórico. As opções de alteração somente no mês ou a partir dele continuam na FEAT 07. O Dashboard histórico permanece sem + Adicionar.

Só pontuais oferecem “Excluir movimentação” no detalhe. O ViewModel exige confirmação, bloqueia solicitações repetidas durante/depois da exclusão e chama `MovementDeletionStore`, implementado pelo repositório existente com remoção por ID no DAO de movimentos. Cancelar não grava; falhas permitem tentar novamente. Sucesso volta para a categoria ou Dashboard. Parar/excluir recorrência continua exclusivamente no formulário já existente.

Os ViewModels observam Flow no escopo da entrada de navegação; Room atualiza detalhe, categoria, Dashboard e Histórico. Não há atualização manual. Se editar muda o período, exclui a ocorrência ou remove a recorrência, o detalhe mostra uma mensagem simples de indisponibilidade e permite voltar.

## Navegação e limites

LazyListState é salvo pelo Navigation Compose para cada entrada. Voltar restaura índice e deslocamento; entradas novas começam no topo. Os fluxos permanecem ativos na pilha para evitar remover a lista durante um carregamento ao retornar. Dinheiro, período e data usam `DashboardFormatting`.

Sem gráficos, filtros, busca, exportação ou redesign global. A categoria representa a previsão efetiva do período, incluindo despesas recorrentes; não é uma lista das linhas físicas da tabela de movimentos.

## Validação

`./gradlew.bat test build lint` aprovado: 260 testes (217 anteriores + 43 novos), zero falhas; lint com zero erros e oito avisos nos arquivos preexistentes de dependências/ícone. A cobertura nova inclui Room/engine, identidade por origem e ID, versões, exceções, vigência, valores efetivos, confirmação/cancelamento/exclusão idempotente, reatividade até o Histórico e testes Compose de navegação real até os formulários. O teste histórico salva uma exceção no período antigo, verifica o mês seguinte preservado e retorna ao Histórico. Scroll é comparado por semântica, sem testes de pixels.

Validação manual concluída no `emulator-5556`, usando somente dados de desenvolvimento:

- Dashboard → Moradia → aluguel → Editar: o formulário da FEAT 07 salvou uma exceção de setembro de R$ 350,00; detalhe, categoria e Dashboard atualizaram automaticamente.
- Pontual Mercado test: edição pela FEAT 06, cancelamento da exclusão sem alteração e confirmação com retorno à categoria vazia; a categoria desapareceu do Dashboard.
- Rendas Venda test e Salario test abriram diretamente o detalhe, sem categoria nem lista intermediária; a renda mensal reutilizou o formulário existente.
- Histórico → agosto de 2026 → Moradia → FEAT09 moradia → Editar: diálogo manteve “Somente em agosto de 2026”/“A partir de agosto de 2026”. Salvar somente no mês mostrou R$ 170,00 no detalhe e Histórico. O retorno preservou a posição da lista. Dashboard histórico permaneceu sem + Adicionar.
- Detalhe inspecionado visualmente em 320dp com fonte de 160%, incluindo quebra da data e acesso por rolagem às ações. Resolução e fonte originais restauradas.

A recorrência identificada `FEAT09 moradia`, iniciada em agosto de 2026, permanece apenas no banco do emulador. Não há fixtures automáticas no aplicativo. A interrupção deixou pendente a conclusão desse teste manual e seu registro, sem exigir reconstrução da feature.
