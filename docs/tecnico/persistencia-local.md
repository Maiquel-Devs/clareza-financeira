# FEAT 01 — Persistência local

O pacote `com.clarezafinanceira.app.data.local` contém somente persistência:

- `FinancialDatabase`: Room/SQLite versão 1, arquivo `clareza-financeira.db`.
  `getInstance(context)` mantém uma instância por processo usando applicationContext.
- `MovementDao`: inserção, atualização, remoção e consulta de movimentações;
  observação por intervalo inclusivo de datas com Flow.
- `RecurrenceDao`: criação atômica com primeira versão, inserção de versões
  permanentes e exceções, consultas suspend/Flow e encerramento por endPeriod.
- Quatro entidades, enums com códigos de armazenamento explícitos e converters.

Não há ligação com a UI, ViewModel ou cálculos financeiros. Um repository que
apenas encaminhasse chamadas aos DAOs não acrescentaria comportamento nesta etapa;
por isso não foi criada essa camada nem interfaces de abstração adicionais.

## Integridade e histórico

As entidades imutáveis validam valores locais na construção. O DAO valida relações
entre recorrência, categoria e período dentro da mesma transação da gravação.
As inserções usam ABORT: conflitos não substituem registros nem apagam filhos.
SQLite garante chaves primárias, NOT NULL, FKs, unicidade e cascatas; as demais
validações são Kotlin, não CHECKs/triggers SQL. A aplicação deve gravar pelos DAOs,
sem SQL externo que contorne essas validações.

A primeira versão é gravada junto da recorrência e começa em startPeriod.
Novas versões têm outro id e validFrom exclusivo, sem operação de atualização
ou remoção de versões. Não há atualização do tipo ou início da recorrência.
endPeriod é inclusivo; stop preserva todos os filhos. deletePermanently é a
operação explícita de exclusão física que aciona as cascatas.

Exceções excluídas são normalizadas para todos os overrides nulos antes de
persistir, inclusive quando os overrides recebidos seriam inválidos. Nas demais,
null significa herdar o valor da versão; receita não aceita override de categoria.
Esta etapa insere exceções e rejeita duplicatas no mesmo mês; edição de uma
exceção existente não é exposta ainda.

IDs são Strings não vazias, compatíveis com UUID, fornecidas pelo chamador.
Timestamps também são fornecidos pelo chamador, permitindo testes determinísticos.
Valores monetários usam Long em centavos sem conversão para ponto flutuante.

## Formatos e evolução

- LocalDate: INTEGER de dias desde a época Unix.
- YearMonth: INTEGER de mês proléptico (ano * 12 + mês - 1), ordenável no SQLite.
- Instant: TEXT ISO-8601, preservando nanossegundos.
- Enums: TEXT com códigos explícitos INCOME/EXPENSE e FOOD/HOUSING/TRANSPORT/
  LEISURE/HEALTH/OTHER; nunca ordinais ou rótulos traduzidos.

O schema gerado pelo Room fica em `app/schemas/`. Alterações futuras do schema
precisam aumentar a versão e fornecer migrations. Não há fallback destrutivo.
Nenhuma tabela de análises, totais, meses ou categorias é criada.

## Validação

`./gradlew.bat testDebugUnitTest` executa testes JUnit e testes Room com Robolectric
(API 35, SQLite nativo), incluindo reabertura de banco em arquivo, round-trips,
Flow, unicidade, FKs, transações, validações e cascatas. Não exige emulador.

`./gradlew.bat build` executa o build completo e verificações do projeto.
AGP 9.3.2, Gradle 9.5.0, Kotlin/Compose e configuração do JDK 21 foram mantidos.
