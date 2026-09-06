# Protótipo — Clareza Financeira

Abra `index.html` no navegador.

## Mudanças desta versão

O Dashboard agora mostra a composição da renda e das despesas diretamente na mesma análise.

### Resumo
- Renda
- Despesas registradas
- Previsão de despesas do mês
- Sobra prevista da renda

### De onde vem e para onde vai

Abaixo do resumo aparecem dois blocos:

**Renda**
- Salário
- Freelance
- outras origens cadastradas

**Despesas**
- Moradia
- Alimentação
- Transporte
- Saúde
- Lazer
- Outros, quando houver

Os cards de **Renda** e **Despesas registradas** não são mais botões de navegação. Eles servem somente como resumo rápido.

O usuário não precisa mais clicar no card de renda para descobrir de onde o dinheiro veio. Essa informação fica visível no próprio Dashboard.

As linhas da análise continuam clicáveis quando existe utilidade:
- tocar em uma renda abre os detalhes daquela renda;
- tocar em uma categoria de despesa mostra as movimentações que formam aquele valor.

## Regra de previsão

- **Despesas registradas**: gastos pontuais informados como ocorridos.
- **Previsão de despesas do mês**: gastos registrados + recorrências conhecidas.
- **Sobra prevista da renda**: renda - previsão de despesas.

O protótipo continua sendo HTML/CSS/JavaScript puro e descartável, sem backend ou banco real.
