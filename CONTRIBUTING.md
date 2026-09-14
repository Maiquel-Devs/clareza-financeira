# Como contribuir

## Preparar o ambiente

Tenha Git, Android Studio compatível com o AGP 9.3.2, um JDK compatível com o AGP e o Gradle 9.5.0, e Android SDK Platform 37. A compatibilidade Java declarada é 17; JDK 21 é o ambiente local validado, não um requisito universal. Use o Gradle Wrapper incluído.

Faça um fork no GitHub para enviar contribuições sem acesso de escrita. Clone seu fork (substituindo `SEU-USUARIO`) e crie uma branch:

```sh
git clone https://github.com/SEU-USUARIO/clareza-financeira.git
cd clareza-financeira
git switch -c minha-alteracao
```

Para apenas executar o projeto, use a URL original indicada no [README](README.md#como-executar).

Abra a raiz no Android Studio, configure o JDK do Gradle, instale o SDK solicitado pelo SDK Manager e sincronize. Deixe o Studio criar `local.properties` com o caminho do seu SDK. Selecione um emulador ou dispositivo com API 26 ou superior e execute `app`. A primeira sincronização requer internet; não é necessário configurar backend ou assinatura de release.

## Fazer uma alteração

Mantenha a contribuição focada e siga o estilo do código existente. Antes de alterar comportamentos financeiros, consulte as [regras do produto](docs/regras-do-produto.md), a [arquitetura](docs/arquitetura-atual.md) e a documentação em [docs/](docs/). Preserve as regras existentes; explique e discuta mudanças de comportamento antes de implementá-las.

O código Android fica em `app/src/main/`, os testes locais em `app/src/test/` e os schemas Room em `app/schemas/`. `Prototipo/` contém referências históricas em HTML/CSS/JavaScript, não o aplicativo Android.

Nunca inclua dados financeiros pessoais, tokens, senhas, keystores, configurações privadas de assinatura, `local.properties` ou artefatos gerados. Revise o diff antes de enviar; `.gitignore` não protege arquivos já rastreados.

## Validar e abrir um Pull Request

Na raiz, com `JAVA_HOME` e o SDK configurados conforme o README, execute os comandos relevantes à alteração. No Windows/PowerShell:

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug lintDebug
```

No macOS/Linux:

```sh
./gradlew testDebugUnitTest
./gradlew assembleDebug lintDebug
```

Os testes locais usam JUnit/Robolectric e não exigem emulador. Para mudanças de comportamento, acrescente ou ajuste os testes relevantes; confira também no dispositivo quando houver impacto visual. Alterações apenas documentais não exigem repetir toda a suíte. O README contém o comando de validação ampla.

Revise `git diff`, faça commit dos arquivos da contribuição e envie sua branch ao fork com `git push -u origin minha-alteracao`. No GitHub, abra um Pull Request para o repositório original. Descreva o problema, a mudança e o que foi validado, incluindo limitações; adicione capturas quando ajudarem a avaliar mudanças visuais. Atualize a documentação quando necessário.
