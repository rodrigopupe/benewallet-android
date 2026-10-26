# ADR-0003: Feature flags fornecidas pelo BFF

- **Status:** Aceito
- **Data:** 2026-10-23

## Contexto
É desejável liberar funcionalidades de forma gradual (canary) e desligá-las rapidamente sem publicar nova versão do app.

## Opções
1. **Flags em `BuildConfig`:** simples, mas exigem novo release para mudar.
2. **Serviço externo de flags:** completo, mas adiciona dependência e configuração fora do escopo.
3. **Flags servidas pelo BFF**, com rollout percentual calculado por um identificador estável do usuário.

## Decisão
Servir as flags pelo endpoint `GET /feature-flags` do BFF, com cache local e valores padrão seguros caso a requisição falhe.

## Consequências
- (+) Rollout progressivo e *kill switch* sem novo release.
- (+) Demonstra a atuação no BFF sem dependência externa.
- (−) Implementação própria é menos rica que uma solução dedicada.
- (−) É preciso tratar flags desatualizadas no cliente (valor padrão e cache).

## Atualização (2026-10-26): identificação pelo token
- O BFF resolve as flags para o **usuário do token de acesso**; o header `X-User-Id` e o UUID de instalação do app deixaram de existir (ADR-0006). O rollout percentual passa a valer por conta.
- No app, as flags ficam em cache no DataStore, com valores padrão por flag (`statement_filters` ligada, `goals` desligada), e o cache é apagado quando a sessão termina, para a conta seguinte não herdar os valores da anterior.
