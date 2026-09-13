# Clareza Financeira — produto atual

## O que é e para quem existe

O Clareza Financeira é um aplicativo Android de organização e clareza financeira pessoal. Seu objetivo é ajudar uma pessoa comum a entender rapidamente sua situação financeira e para onde seu dinheiro está indo, com o mínimo de esforço possível.

A experiência desejada é: **abriu o aplicativo → bateu o olho → entendeu.**

Muitas pessoas conseguem registrar valores, mas têm dificuldade de transformar esses números em uma visão simples do mês. O produto organiza e interpreta as informações fornecidas pelo usuário para responder perguntas como:

- Quanto eu ganhei?
- Quanto eu gastei?
- Para onde meu dinheiro foi?
- Quanto da minha renda deve sobrar?

Foi pensado para quem quer compreender a própria vida financeira sem precisar usar uma ferramenta financeira complexa. Não exige conhecimento contábil nem uma estrutura completa de orçamento para começar.

## Como o produto entende a vida financeira

### Fluxo financeiro, não patrimônio

A relação central é **renda − despesas = sobra**. Na visão prevista do mês, a sobra considera a renda informada e a previsão de despesas.

O aplicativo acompanha o fluxo financeiro do período. Não procura responder “quanto dinheiro eu possuo?” nem representar o saldo de uma conta bancária.

Por exemplo, uma pessoa pode ter R$ 100.000,00 no banco e, em determinado mês, informar:

| Informação do mês | Valor |
| --- | ---: |
| Renda | R$ 10.000,00 |
| Despesas | R$ 5.000,00 |
| Sobra | R$ 5.000,00 |

Os R$ 100.000,00 que já estavam no banco não fazem parte dessa análise. **O Clareza Financeira acompanha fluxo financeiro, não patrimônio ou saldo bancário.**

### Cada mês é independente

Cada mês possui sua própria análise. A sobra de setembro não se transforma automaticamente em saldo de outubro. O aplicativo não simula uma conta bancária acumulativa.

### O que foi registrado e o que está previsto

**Despesas registradas** são os gastos pontuais efetivamente registrados para o mês.

**Previsão de despesas do mês** reúne esses gastos pontuais e as despesas recorrentes válidas e conhecidas do período. Isso permite compreender o mês antes de ele terminar, sem depender de previsões que o usuário não forneceu.

A renda considerada também reúne entradas pontuais e recorrentes válidas. A sobra prevista é essa renda menos a previsão de despesas. Os indicadores refletem os dados informados; não são uma confirmação de saldo disponível no banco.

Uma movimentação pontual pertence ao mês de sua data. Um registro com a data de hoje participa imediatamente da análise correspondente. O formulário começa com a data atual e permite escolher outra data.

Uma despesa pontual e uma recorrência são entradas independentes: o aplicativo não identifica automaticamente uma como pagamento da outra. Se ambas forem informadas, ambas participam da previsão.

### Indicadores adequados à situação

Quando há renda, o percentual mostra quanto dela está comprometido pela previsão de despesas. Ele pode ultrapassar 100%, e a sobra pode ser negativa. O produto comunica essa realidade sem julgamento.

Sem renda informada, as despesas continuam visíveis, mas não há percentual artificial nem sobra negativa calculada como se existisse renda. A interface informa a ausência de renda.

Quando o período está totalmente vazio, há uma orientação para começar, em vez de uma coleção de zeros. Categorias sem despesas são omitidas.

## Princípios do produto

- **Clareza antes de complexidade:** facilitar a compreensão da situação financeira, sem expor complexidade técnica.
- **Mínimo esforço:** eliminar ações que não acrescentem informação importante e permitir começar com poucos dados, complementando depois.
- **Automatizar o previsível:** automatizar somente o que pode ser previsto com segurança a partir das regras fornecidas pelo usuário.
- **Interpretar, não apenas mostrar números:** relacionar renda, despesas e sobra para explicar a situação do mês.
- **Realidade sem julgamento:** representar os dados sem repreender a pessoa por suas escolhas ou resultados.
- **Indicadores somente quando fazem sentido:** não inventar renda, percentual ou saldo para preencher a interface.
- **Complexidade por trás, simplicidade na frente:** resolver internamente as regras necessárias, apresentando escolhas compreensíveis ao usuário.

## A experiência atual

### Começar sem preencher tudo

O aplicativo abre no Dashboard. Sem dados, explica que ainda não há informação no período e oferece **+ Adicionar**. O usuário pode começar com uma renda ou despesa, pontual ou mensal, e completar sua estrutura aos poucos. Não há cadastro de conta nem obrigação de informar renda antes de registrar despesas.

### Dashboard e detalhamento

O Dashboard é a visão principal do mês. Reúne renda, despesas registradas, previsão de despesas, sobra prevista e percentual da renda comprometida, quando aplicável. A explicação da previsão e sua interpretação aparecem em um bloco visual unificado.

A seção **“De onde vem e para onde vai”** mostra as fontes de renda diretamente e organiza as despesas por categoria, com quantidade e total. O detalhamento permite compreender os itens que compõem a análise e acessar sua edição:

- Renda: **Dashboard → fonte de renda → detalhe → edição**.
- Despesa: **Dashboard → categoria → despesas da categoria → detalhe → edição**.

As categorias de despesas são fixas: **Alimentação, Moradia, Transporte, Lazer, Saúde e Outros**. Renda não possui categoria.

### Recorrências: o normal acontece automaticamente

O usuário pode cadastrar rendas e despesas mensais a partir de um mês inicial. Elas passam a participar automaticamente dos períodos correspondentes. O dia habitual informa quando costumam acontecer; não é necessário esperar esse dia para que componham a previsão.

O princípio é: **o normal deve acontecer automaticamente; o usuário age principalmente quando existe uma exceção.**

É possível alterar uma ocorrência somente no mês escolhido ou aplicar uma mudança daquele período em diante. A primeira opção não muda os outros meses; a segunda preserva os anteriores e respeita alterações já programadas para períodos posteriores.

Também é possível interromper uma recorrência, escolhendo se ela permanece no período selecionado. A interrupção preserva o histórico anterior. Excluir é uma ação diferente: remove a recorrência de todos os períodos, inclusive anteriores, mediante confirmação.

Essas regras representam uma continuidade fornecida pelo usuário, sem criar artificialmente movimentações pontuais futuras. O detalhamento completo está em [regras do produto](regras-do-produto.md).

### Histórico

O Histórico permite selecionar um ano e abrir os meses que possuem informação, até o mês atual. O ano fica no seletor, os cards mostram os nomes dos meses e o período atual recebe o selo **ATUAL**. Um ano sem informação tem uma mensagem própria.

Ao abrir um mês, o usuário encontra a mesma organização do Dashboard, com **Voltar**, sem acesso redundante ao Histórico e sem **+ Adicionar**. Pode consultar e editar itens existentes pelos detalhes, com mês e ano explícitos nas decisões sensíveis.

O histórico representa os dados e as regras aplicáveis a cada período. Não é uma fotografia imutável: uma edição retroativa ou exclusão pode alterar a análise do mês afetado. Ao voltar de um detalhe, a navegação preserva a posição de rolagem das listas.

### Gráficos complementares

Os gráficos ajudam a interpretar os dados; não substituem as informações essenciais. Começam fechados e podem ser abertos e recolhidos pelo usuário.

No Dashboard, **“Ver gráfico das despesas”** aparece depois das categorias e mostra a distribuição da previsão de despesas. No Histórico, **“Ver evolução do ano”** compara as previsões dos meses relevantes com uma escala comum. Quando não há dados suficientes para a comparação, o aplicativo explica essa limitação.

### Dados locais

Os registros são mantidos localmente no dispositivo e permanecem após fechar e reabrir o aplicativo. A análise é derivada das movimentações e das regras de recorrência informadas, acompanhando suas alterações. Não há conta do produto ou sincronização entre dispositivos.

## Direção de UX

O refinamento pós-MVP REF03 disponibiliza **Dashboard → ⋮ → Sobre**. A tela apresenta o propósito do aplicativo, o desenvolvedor Maiquel, a condição de projeto open source e a licença MIT, com acesso à página oficial no navegador externo. Esse acesso discreto mantém o Dashboard focado nas finanças e não envia dados financeiros ao abrir o link.

A direção vigente é **leve, confortável, clara, amigável e moderna**. Títulos, espaçamentos e hierarquia favorecem a leitura; os controles de retorno usam “Voltar”, e o acesso ao histórico é apresentado como “Histórico ›”.

A legibilidade tem prioridade sobre uma disposição fixa. Os cards Renda e Despesas registradas podem ficar lado a lado ou empilhados, conforme largura, fonte e conteúdo. Valores maiores podem exigir empilhamento; isso faz parte da experiência responsiva aceita.

O protótipo HTML/CSS/JS foi importante para a descoberta e a validação inicial. A implementação Android evoluiu nos testes em dispositivo e é hoje a principal referência de experiência. O protótipo permanece como registro histórico e referência de decisões anteriores, sem exigir fidelidade visual literal.

## Principais capacidades do MVP

| Área | Capacidade disponível |
| --- | --- |
| Rendas e despesas | Cadastro pontual e recorrente mensal, edição e exclusão nos fluxos apropriados. |
| Organização | Categorias fixas para despesas, origem da renda e detalhamento financeiro. |
| Recorrências | Exceções mensais, alterações permanentes a partir de um período e interrupção. |
| Análise | Visão mensal, previsão, sobra, percentual quando aplicável e distribuição por categoria. |
| Consulta | Histórico por ano e mês, detalhes e gráficos opcionais de despesas e evolução anual. |
| Continuidade | Persistência local e atualização das análises após alterações nos dados. |
| Estados de uso | Primeiro acesso, período vazio e período com despesas sem renda informada. |

## O que o produto não é

O Clareza Financeira não é um aplicativo bancário, controle de saldo bancário, rastreador de patrimônio, sistema contábil, plataforma complexa de orçamento ou aplicativo de investimentos. Também não é um chatbot financeiro nem um sistema de aconselhamento financeiro.

Essa delimitação protege a simplicidade e o propósito central: ajudar a compreender o fluxo financeiro mensal com base nas informações fornecidas pela própria pessoa.

## Deliberadamente fora do MVP

Os itens abaixo são limites de escopo, não funcionalidades faltantes nem promessas de implementação:

- Conta/login, e-mail e senha.
- Cloud e sincronização como recursos do produto.
- IA, chatbot e aconselhamento financeiro.
- Investimentos e gamificação.
- Notificações complexas e configurações complexas.
- Categorias personalizadas.
- Múltiplas contas e carteiras.
- Cartão de crédito e parcelamentos complexos.
- Relatórios avançados.
- Versões para iOS e desktop.
- Assinatura e implementação final de monetização.

## Estado atual

**CLAREZA FINANCEIRA — MVP CONCLUÍDO.**

A homologação FEAT 11 terminou como **APROVADO COM OBSERVAÇÕES**, com confirmação de que o produto pode ser declarado MVP concluído. Esse estado se refere ao escopo homologado; não significa publicação em loja.

O resultado foi apresentado na homologação e confirmado para esta consolidação. Ainda não há um arquivo específico da FEAT 11 em `docs/`; o resultado técnico e suas observações estão registrados em [estado do MVP](estado-do-mvp.md).

## Referências internas

Este documento apresenta o produto vigente. Os documentos de FEATs continuam sendo registros da evolução e podem descrever limitações de uma etapa já superada.

- [Análise mensal](tecnico/analise-mensal.md) e [persistência local](tecnico/persistencia-local.md).
- [Dashboard](historico/feat-05-monthly-dashboard.md) e [cadastro e edição](historico/FEAT-06.md).
- [Recorrências e exceções](historico/FEAT-07.md).
- [Histórico](historico/FEAT-08.md) e [detalhamento financeiro](historico/FEAT-09.md).
- [Gráficos e UX](historico/FEAT-10.md) e [revisão pré-FEAT 11](historico/PRE-FEAT-11-UX-REVIEW.md).

Em caso de dúvida sobre o comportamento vigente, considerar o código e os testes atuais, as decisões posteriores que substituem as antigas e o resultado da homologação. Esta visão de produto não substitui a documentação consolidada de [arquitetura](arquitetura-atual.md), [regras](regras-do-produto.md) ou [fluxos e UX](fluxos-e-ux.md).
