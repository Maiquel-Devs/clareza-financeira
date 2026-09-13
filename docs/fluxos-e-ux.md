# Clareza Financeira — fluxos e UX

Este documento registra como a pessoa usa e navega pelo aplicativo Android atual e quais decisões de experiência devem orientar sua evolução. Complementa [produto atual](produto-atual.md), [regras do produto](regras-do-produto.md) e [arquitetura atual](arquitetura-atual.md), sem repetir fórmulas financeiras ou detalhes de implementação.

## 1. Objetivo e referência da experiência

**Abriu o aplicativo → bateu o olho → entendeu.**

A interface busca compreensão rápida e mínimo esforço: informação essencial primeiro, complexidade por trás e simplicidade na frente. A pessoa deve entender a situação financeira do período sem conhecimento contábil, preenchimento inicial obrigatório de toda a vida financeira ou navegação excessiva.

A direção vigente é **leve, confortável, clara, amigável e moderna**.

**A implementação Android atual é a principal referência de experiência.** O protótipo HTML/CSS/JS, incluindo `prototipo_12`, serviu à descoberta e à validação inicial e permanece como referência histórica. Não se exige fidelidade visual literal a ele. Mudanças futuras devem considerar primeiro os comportamentos confirmados no código, nos testes e nas validações posteriores do Android real.

## 2. Dashboard e hierarquia da informação

O aplicativo abre diretamente no Dashboard do mês atual. Não há onboarding, login ou tela intermediária obrigatória. A pessoa pode começar por uma renda ou uma despesa.

Quando há informação financeira, a leitura segue esta organização:

1. Nome do produto, período com mês e ano, acesso **“Histórico ›”** e subtítulo.
2. Resumo com **Renda**, **Despesas registradas**, **Previsão de despesas do mês** e **Sobra prevista da renda**.
3. Bloco unificado de explicação e interpretação da previsão, com percentual da renda quando aplicável ou mensagem de ausência de renda.
4. Seção **“De onde vem e para onde vai”**, primeiro com fontes de renda e depois com categorias de despesas.
5. Ação opcional **“Ver gráfico das despesas”**, depois das categorias, quando há previsão de despesas.

O botão flutuante **“+ Adicionar”** permanece como ação principal de entrada de informação, no canto inferior direito do Dashboard atual, inclusive no período vazio. Ele não é um item extra no fim da lista.

As fontes de renda mostram nome, identificação Mensal/Pontual e valor. As categorias de despesas mostram nome, quantidade de gastos e total. Somente categorias com despesas aparecem. A composição financeira desses indicadores está em [regras do produto](regras-do-produto.md).

**Informação essencial vem antes das análises complementares.** O gráfico de despesas não deve disputar a leitura inicial com o resumo, as fontes e as categorias. A explicação da previsão e sua interpretação compartilham um bloco, com hierarquia de texto que distingue explicação de conclusão.

### Ações secundárias

“Histórico ›” fica junto ao período, em uma superfície discreta, com aparência clicável e indicador de avanço. Não é um botão grande de destaque nem um menu. Cards de fontes, categorias e itens também indicam que podem ser abertos, com feedback de toque e sinal de avanço discreto.

O Dashboard deve continuar focado nas finanças. Ações secundárias não devem transformá-lo em uma central de configurações ou links.

## 3. Consultar renda e despesas

### Renda: acesso direto

**Dashboard → fonte de renda → detalhe → Editar.**

A fonte já é um item individual no Dashboard. Tocar nela abre diretamente **“Detalhes da renda”**, sem lista intermediária de rendas. Esse acesso direto é parte da experiência validada e deve ser preservado.

O detalhe mostra nome, valor, tipo Mensal/Pontual, data ou dia habitual e **“Período visualizado”**. Renda não apresenta categoria.

### Despesa: categoria antes do item

**Dashboard → categoria → lista de despesas efetivas → detalhe → Editar.**

A categoria funciona como agrupamento antes do item individual. Sua tela mostra categoria, mês e ano, quantidade, total e gastos daquele período. A lista inclui despesas pontuais e recorrentes efetivas, não apenas lançamentos pontuais. Cada item mostra nome, valor e identificação mensal ou data pontual.

Tocar no gasto abre **“Detalhes da despesa”**, com nome, valor, categoria, tipo, data ou dia habitual e período visualizado. A pessoa chega à edição pelo botão **“Editar”**.

Os detalhes refletem os dados atuais. Se o item deixar de pertencer ao período após uma alteração, a tela pode informar **“Esta movimentação não está disponível neste período.”** e permitir voltar. Uma categoria que fique vazia informa **“Nenhum gasto nesta categoria neste período.”**.

## 4. Adicionar movimentação

**Dashboard atual → + Adicionar → formulário → Salvar → Dashboard atual.**

O botão abre um único formulário, inicialmente de despesa pontual. Nele, a pessoa escolhe **“Despesa”** ou **“Renda”**. O título acompanha a escolha: **“Nova despesa”** ou **“Nova renda”**.

A opção **“Acontece todo mês?”** distingue pontual de mensal. Não há etapas adicionais de onboarding, escolha de conta ou confirmação mensal de uma recorrência normal.

### Formulário de despesa

Após a escolha do tipo, a ordem de cadastro pontual é:

1. **Descrição**.
2. **Categoria**, com a ação **“Escolher categoria”**.
3. **Valor (R$)**.
4. **Acontece todo mês?**.
5. **Data**, quando pontual.
6. **Salvar**.

A sequência acompanha **“o que foi? → onde se encaixa? → quanto custou?”**. A categoria é escolhida em um diálogo com Alimentação, Moradia, Transporte, Lazer, Saúde e Outros. Escolher uma opção fecha o diálogo; “Voltar” o fecha sem escolher outra.

Ao ativar a opção mensal, a área de Data é substituída por **“Mês de início (MM/AAAA)”**, leitura do período por extenso e **“Dia habitual (1 a 31)”**. O formulário explica que o valor será considerado a cada mês e que o dia informa quando costuma acontecer. Data pontual e campos temporais mensais não aparecem simultaneamente.

### Formulário de renda

A ordem é **“Origem / nome da renda” → “Valor (R$)” → “Acontece todo mês?” → campos temporais → “Salvar”**.

Renda não possui categoria. Quando pontual, usa Data; quando mensal, usa mês inicial e dia habitual, como o cadastro de despesa mensal.

### Datas e período inicial

No novo cadastro pontual, a data atual já vem preenchida. A pessoa toca na data exibida em `dd/MM/aaaa` para abrir o seletor nativo do Android (`DatePickerDialog`) e escolher outro dia, inclusive para registrar um período anterior. Não há calendário personalizado.

O cadastro mensal começa com o mês atual sugerido. O campo numérico `MM/AAAA` insere o separador automaticamente e, quando válido, mostra abaixo uma leitura como **“Setembro de 2026”**. O nome do mês ajuda a conferir o período sem substituir a entrada numérica. O dia habitual é um campo separado; não representa uma exigência de esperar aquela data para enxergar a previsão.

### Salvar, corrigir e sair

Os formulários são roláveis e consideram o teclado, permitindo alcançar os campos e a ação de salvar. Campos inválidos recebem mensagens próximas à entrada correspondente. Enquanto grava, o botão mostra **“Salvando…”** e o formulário bloqueia novas ações de escrita e retorno até concluir.

O cadastro só retorna ao Dashboard após sucesso. Em caso de falha, os campos permanecem preenchidos e uma mensagem permite tentar novamente. Voltar sem salvar não exige confirmação e descarta as alterações não salvas; não há rascunho persistido após encerramento do processo.

## 5. Editar ou excluir uma movimentação pontual

**Detalhe → Editar → formulário preenchido → Salvar → detalhe.**

O título é “Editar renda” ou “Editar despesa”. Nome/origem, valor, categoria quando despesa e data são carregados do registro. Na edição não aparecem os controles para trocar renda/despesa ou converter pontual em mensal.

Salvar uma edição pontual normal não exige confirmação redundante. Após sucesso, a pessoa retorna ao detalhe e os dados dependentes se atualizam. Se a data tiver sido movida para outro período, o detalhe original pode ficar indisponível, como descrito na consulta de itens.

**Excluir é uma ação separada e destrutiva.** No detalhe pontual, “Excluir movimentação” abre **“Excluir movimentação?”**, informa o mês e ano de onde o registro será removido e oferece **“Cancelar”** ou **“Excluir”**. Cancelar mantém o registro. Após exclusão bem-sucedida, o retorno é para a categoria, no caso de despesa, ou para o Dashboard, no caso de renda.

## 6. Editar recorrência e escolher o alcance

**Detalhe mensal → Editar → formulário preenchido → Salvar alteração → escolha do alcance.**

O formulário identifica a edição como mensal e mostra o período por extenso. Apresenta nome/origem, categoria quando despesa, valor e dia habitual. Não pede nova data pontual nem altera o mês de início pelo formulário de edição.

Depois de validar os campos, se houver diferenças, abre **“Como deseja aplicar esta alteração?”**. Para uma edição em setembro de 2026, as opções são:

- **“Somente em setembro de 2026”**.
- **“A partir de setembro de 2026”**.

O diálogo também informa: **“Alterações já programadas para períodos posteriores serão preservadas.”** Há “Cancelar”, que fecha a escolha sem gravar. Se não houve alteração de campos, salvar retorna sem abrir esse diálogo.

A escolha de alcance resolve uma ambiguidade real da recorrência; não é uma confirmação extra para toda edição. Após aplicar com sucesso, o formulário retorna ao detalhe. Falhas preservam os campos e permitem nova tentativa.

**Em contexto histórico, mês e ano precisam estar explícitos.** O período acompanha a navegação desde o Dashboard consultado até o formulário. Expressões como “este mês” e “daqui para frente” não substituem os rótulos precisos dessas decisões. O comportamento financeiro das duas opções está em [regras do produto](regras-do-produto.md).

## 7. Parar e excluir recorrência

As ações ficam no formulário de edição mensal, após “Salvar alteração”. **“Parar recorrência”** usa botão contornado; **“Excluir recorrência”** usa ação textual com cor destrutiva. O detalhe mensal leva à edição, sem repetir ali as ações de parar/excluir.

### Parar

“Parar recorrência” abre **“Parar recorrência?”**, explica a preservação do histórico anterior e permite decidir sobre o mês selecionado. Para setembro de 2026:

- **“Manter em setembro de 2026 e parar a partir de outubro de 2026”**.
- **“Remover de setembro de 2026 e parar a partir de setembro de 2026”**.

Essas opções distinguem manter a ocorrência selecionada de retirá-la já naquele período. O usuário pode cancelar. Mês e ano aparecem também quando a pessoa veio do Histórico, sem tratar o mês consultado como se fosse necessariamente o atual.

### Excluir toda a recorrência

“Excluir recorrência” abre uma confirmação diferente, que explica a remoção de todos os períodos, inclusive os anteriores, e informa que a ação não pode ser desfeita. A ação final é **“Excluir de todos os períodos”**, com destaque destrutivo, acompanhada de “Cancelar”.

Parar e excluir não são equivalentes. A interface deve manter essa distinção visível e compreensível; os efeitos detalhados sobre o histórico estão em [regras do produto](regras-do-produto.md).

Após parar ou excluir com sucesso, o formulário retorna ao detalhe. Quando a ocorrência não existe mais no período visualizado, o detalhe apresenta indisponibilidade e permite voltar; não há salto automático obrigatório até o Dashboard.

## 8. Histórico e Dashboard histórico

**Dashboard → Histórico › → seleção de ano → card de mês → Dashboard histórico.**

O Histórico abre com o ano atual selecionado e permite navegar pelos anos com controles de anterior/próximo. O ano fica no seletor; os cards mostram somente o nome do mês. O período atual recebe **“ATUAL”**, próximo ao nome, em uma composição vertical que preserva espaço com fonte ampliada.

Os cards aparecem do mês mais recente para o mais antigo e apresentam Renda, Previsão de despesas e Sobra prevista. Só entram meses com informação financeira efetiva, até o mês atual. Não se criam cards futuros ou meses vazios para preencher o ano. O controle de próximo ano fica desabilitado no ano atual.

Ao tocar em um mês, o Dashboard mostra novamente mês e ano completos e a mesma estrutura de análise. A apresentação dos cards somente com o nome do mês em contexto anual não deve eliminar o ano dos detalhes ou das decisões de edição.

| Dashboard principal | Dashboard aberto pelo Histórico |
| --- | --- |
| Mostra o mês atual. | Mostra o mês escolhido com ano explícito. |
| Tem “Histórico ›”. | Tem “Voltar”, sem acesso redundante ao Histórico. |
| Tem “+ Adicionar”. | Não tem “+ Adicionar”. |
| Permite abrir itens e editar. | Mantém consulta de categorias, detalhes e edição dos itens existentes. |

O modo histórico mantém essas diferenças mesmo se o card escolhido for o mês atual. Para um novo registro retroativo, a pessoa usa o cadastro do Dashboard principal e escolhe a data ou o mês inicial correspondente.

Com o REF02, o Dashboard principal acompanha a virada mensal, tanto com o aplicativo aberto quanto ao retornar do segundo plano. O Histórico atualiza os meses disponíveis, o selo ATUAL e o limite de próximo ano. Seu ano acompanha o calendário enquanto não houver seleção explícita; após a pessoa escolher um ano, ele permanece fixo. O Dashboard aberto por um card também permanece no mês escolhido, mesmo se ele era o atual ao abrir. Formulários já abertos preservam todos os campos; somente um novo cadastro recebe a data e o mês atuais como sugestões iniciais.

## 9. Voltar e recuperar a posição

Os controles de retorno usam **“Voltar”**, sem seta textual redundante. O retorno respeita a hierarquia e a tela de origem: edição retorna ao detalhe; detalhe de despesa retorna à categoria; categoria retorna ao Dashboard; mês histórico retorna ao Histórico. O botão/gesto de retorno do sistema participa da navegação existente, respeitadas as restrições dos formulários durante a gravação.

Ao voltar, a pessoa deve recuperar a região em que estava, sem precisar procurar novamente o item:

- Dashboard → fonte de renda → detalhe → voltar ao Dashboard.
- Dashboard → categoria → voltar ao Dashboard.
- Categoria → detalhe → voltar à mesma região da lista.
- Histórico → mês → voltar à mesma região dos cards e ao ano selecionado.

Entradas novas de Dashboard, categoria e detalhe começam no topo. Mudar o período do Dashboard ou o ano do Histórico também inicia a lista correspondente no topo. A restauração é do retorno à tela existente, não uma exigência de reaproveitar a posição em toda navegação nova. Se os dados mudarem, a composição da lista pode mudar junto.

As transições são fades curtos, sem deslocamento lateral. Devem apoiar a continuidade da navegação, sem disputar atenção com os dados.

## 10. Gráficos complementares

### Distribuição das despesas

O gráfico é opcional e começa fechado em uma nova tela. **“Ver gráfico das despesas” aparece depois da última categoria de despesas.** Ao abrir, o gráfico ocupa o final da seção, com identificação do período, categorias, valores e percentuais em texto, acompanhados de barras horizontais.

Representa a distribuição da **previsão de despesas**, não apenas gastos pontuais. O acesso só aparece quando há previsão de despesas. Funciona também no Dashboard histórico.

**“Ocultar gráfico”** recolhe a visualização. Voltar à mesma tela pode preservar o estado aberto; mudar o período do Dashboard fecha o gráfico. Os valores essenciais continuam disponíveis sem abri-lo.

### Evolução anual

**Histórico → Ver evolução do ano.**

O acesso é opcional, inicialmente fechado, e fica antes dos cards mensais na lista do Histórico quando há meses com informação. Ao abrir, compara previsões de despesas dos meses relevantes em ordem cronológica e com escala comum. Os cards continuam em ordem decrescente.

Trocar de ano mantém a opção de gráfico aberta e mostra os dados do ano escolhido, quando aplicável. “Ocultar gráfico” recolhe. A comparação precisa de pelo menos dois meses com informação e alguma despesa prevista; caso contrário, explica essa condição. Um ano completamente vazio apresenta sua mensagem própria, sem acesso a gráfico artificial.

As barras complementam os textos; nomes e valores continuam legíveis sem depender apenas de cor ou geometria. As barras não têm animação própria.

## 11. Estados sem dados, carregamento e erro

| Situação | Experiência atual |
| --- | --- |
| Período completamente vazio | “Nenhuma informação neste período.” e “Adicione uma renda ou gasto para começar a análise deste mês.”, sem conjunto de indicadores zerados. |
| Despesas sem renda | Despesas e previsão continuam visíveis; “Nenhuma renda informada neste mês.”, sobra “Não disponível” e orientação para informar renda, sem percentual artificial. |
| Sem despesas, mas com renda | A análise da renda permanece; a seção de despesas informa que não há despesa prevista, sem categorias vazias ou gráfico de despesas. |
| Ano sem histórico | “Nenhuma informação financeira em [ano].”, sem cards zerados. |
| Evolução anual insuficiente | “A evolução precisa de pelo menos dois meses com informação e alguma despesa prevista.” |
| Carregamento | Indicador de progresso; o Dashboard informa “Carregando sua análise…”, sem apresentar análise anterior como se fosse do novo período. |
| Falha de leitura | Mensagem de erro da tela correspondente, distinta de vazio; não fabrica uma análise sem dados. |

O Dashboard histórico reutiliza a mensagem orientativa de período vazio, mas continua sem “+ Adicionar”. Isso pode aparecer, por exemplo, depois de remover a última informação daquele mês durante a consulta. A inclusão de um novo registro permanece no Dashboard principal.

Sobra negativa e despesas acima da renda são comunicadas como situação financeira, sem julgamento. Os significados e a ausência de indicadores quando não cabem estão definidos em [regras do produto](regras-do-produto.md).

## 12. Responsividade, legibilidade e leveza visual

**Legibilidade tem prioridade sobre layout rígido.** Os cards Renda e Despesas registradas podem ficar lado a lado ou empilhados, conforme espaço, escala de fonte e conteúdo. Valores maiores também podem exigir empilhamento. Não se reduz agressivamente a fonte para forçar duas colunas.

O par de cards reserva espaço equivalente para títulos e valores, mantendo alinhamento quando lado a lado e proporção quando empilhado. Textos e valores podem quebrar linha; não se usa altura fixa ou truncamento para ocultar informação financeira.

A experiência mantém fundo claro, superfícies leves, contornos e cores de apoio discretos, títulos hierarquizados, espaçamento para leitura e destaque dos dados. Nome do mês e títulos principais ficam à esquerda; o ano ocupa o seletor contextual do Histórico. A lista histórica permanece contida abaixo do seletor durante a rolagem.

Formulários, detalhes e gráficos permitem acesso por rolagem em telas estreitas ou com fonte ampliada. Gráficos apresentam nomes, valores e percentuais em texto; seus indicadores decorativos não devem criar leitura redundante. Essas decisões e as validações com fonte ampliada não equivalem a uma auditoria completa de acessibilidade com TalkBack.

O “+ Adicionar” mantém sua posição flutuante atual. Conteúdo pode passar atrás dele durante a rolagem, como registrado na revisão validada, e pode ser trazido para a área livre. Esta consolidação não muda posicionamento, espaçamentos ou componentes.

## Referências e limites deste registro

- Contexto: [produto atual](produto-atual.md), [regras do produto](regras-do-produto.md) e [arquitetura atual](arquitetura-atual.md).
- Registros de apoio: [Dashboard](historico/feat-05-monthly-dashboard.md), [cadastro/edição](historico/FEAT-06.md), [recorrências](historico/FEAT-07.md), [Histórico](historico/FEAT-08.md), [detalhamento](historico/FEAT-09.md), [gráficos](historico/FEAT-10.md) e [revisão pré-FEAT 11](historico/PRE-FEAT-11-UX-REVIEW.md).
- Conferência no código: telas de Dashboard, formulários, diálogos recorrentes, Histórico, detalhes, gráficos e `EntryNavigation`, sob `app/src/main/java/com/clarezafinanceira/app/presentation/`.
- Conferência de navegação e retorno: `FinancialDetailNavigationTest.kt` e `HistoryNavigationTest.kt`, sob `app/src/test/java/com/clarezafinanceira/app/presentation/dashboard/`.

O acesso provisório “Editar movimentações” no Dashboard, a ausência de Histórico/detalhes/gráficos nas primeiras entregas, as setas textuais junto de “Voltar” e a repetição de ano nos cards mensais não descrevem mais a experiência vigente.

## Sobre / Desenvolvedor (REF03)

**Dashboard principal → ⋮ → Sobre.** O menu discreto no cabeçalho tem descrição acessível “Mais opções” e oferece somente “Sobre”. A informação institucional fica nessa tela, sem cards no conteúdo financeiro ou nova estrutura global de navegação. O Dashboard histórico conserva sua navegação existente.

A tela Sobre usa o tema nativo atual e o ícone oficial do REF01. Apresenta “Clareza Financeira”, “Organize sua vida financeira sem complicação.”, o propósito do projeto, “Desenvolvedor — Maiquel”, “Projeto open source” e “Licença MIT”. O conteúdo permite rolagem e respeita fonte ampliada. “Voltar” e o retorno do sistema seguem a pilha de navegação até o Dashboard.

“Página do projeto” abre `https://maiquel-devs.github.io/clareza-financeira-landing-page/` no navegador externo. A URL é fixa, sem parâmetros ou dados financeiros. Se não houver aplicação disponível ou a abertura for bloqueada, a tela mostra uma mensagem e permite tentar novamente. Não há WebView nem links de GitHub/LinkedIn nesta versão.

## Decisões de UX que uma evolução futura deve preservar

- Entendimento rápido e mínimo esforço antes de densidade de informação.
- Dashboard focado nas finanças, sem elementos secundários sem necessidade real.
- Informação essencial antes do gráfico de despesas; análises complementares opcionais.
- Renda abre diretamente o detalhe; despesas usam categoria como agrupamento.
- Períodos históricos explícitos, sobretudo em edição e decisões de alcance.
- Confirmação para exclusões e escolhas claras para parar recorrências; nenhuma confirmação redundante para salvar uma edição pontual normal.
- Retorno respeita a hierarquia e recupera a região anterior da lista.
- Legibilidade antes de duas colunas obrigatórias, fontes reduzidas ou valores cortados.
- Estados vazios, ausência de renda e falhas têm mensagens distintas e honestas.
- Android real é a referência principal; o protótipo histórico não impõe regressões visuais ou de navegação.
