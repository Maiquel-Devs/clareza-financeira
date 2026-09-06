# Protótipo — Clareza Financeira V18

Abra `index.html` no navegador.

## Correção do gráfico do Histórico

Foi corrigido o mesmo problema da barra cheia no gráfico **“Evolução do ano”**.

Antes, o tamanho das barras era calculado em relação ao maior mês do ano exibido.
Por isso, o mês com maior previsão de despesas sempre aparecia com a barra completa.

Agora a largura da barra representa o **percentual real daquele mês dentro do total dos meses exibidos no gráfico anual**.

Exemplo:
- Jul = parte proporcional do total do ano mostrado;
- Ago = parte proporcional do total do ano mostrado;
- Set = parte proporcional do total do ano mostrado.

A correção afeta apenas a visualização do gráfico do Histórico.
