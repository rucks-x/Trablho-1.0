# Relatório de Serviço (Android)

1. Crie um repositório no GitHub e envie TODOS estes arquivos (inclusive a pasta oculta `.github`).
2. Abra a aba **Actions**, escolha **Build APK** e aguarde ficar verde (ou clique em **Run workflow**).
3. Dentro da execução, baixe o artefato **RelatorioServico-APK**, extraia o zip e instale o `app-debug.apk` (permita "fontes desconhecidas").

Android 10 ou mais novo. O app salva em Downloads e compartilha pelo menu do celular.

## Importante
- Os serviços ficam guardados dentro do app. Desinstalar o app apaga tudo.
- O arquivo `app/debug.keystore` mantém a mesma assinatura em todas as versões: guarde-o no repositório para poder atualizar o app sem desinstalar. (Se você já instalou uma versão anterior, desinstale-a uma vez.)
