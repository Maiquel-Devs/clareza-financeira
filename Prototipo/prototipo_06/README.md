# Protótipo — Clareza Financeira V16

Abra `index.html` no navegador.

## Correção do gráfico

O problema da primeira barra sempre aparecer cheia foi corrigido.

Antes, o tamanho das barras era calculado em relação à maior categoria do mês. Por isso a maior categoria sempre ocupava 100% da largura, mesmo representando, por exemplo, apenas 64% do total.

Agora a largura da barra usa o percentual real daquela categoria sobre o total das despesas.

Exemplo:
- Moradia = 64% → barra com 64% da largura;
- Alimentação = 25% → barra com 25%;
- Transporte = 5% → barra com 5%.

A correção afeta apenas a visualização do gráfico.
