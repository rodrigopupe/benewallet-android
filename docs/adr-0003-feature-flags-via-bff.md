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
