# Protótipo — Clareza Financeira V7

Abra `index.html` no navegador.

## Fluxo de Histórico corrigido

### Dashboard do mês atual
Topo:

`[ + ]   Setembro de 2026   [ calendário ]`

### Tela de Histórico
O botão de voltar ficou visualmente mais leve e segue o padrão simples de seta.

### Ao escolher um mês anterior
Exemplo: Agosto de 2026.

O Dashboard entra em **modo histórico**:

`[ ← ]   Agosto de 2026   [ vazio ]`

Nesse modo:
- o botão `+` desaparece;
- aparece uma seta de voltar no lugar dele;
- o ícone de Histórico desaparece;
- tocar na seta retorna à lista de meses do Histórico.

Isso evita sugerir que o usuário está no fluxo normal do mês atual quando está consultando um período antigo.

Ao retornar ao mês atual, o cabeçalho normal com `+` e Histórico volta a aparecer.
