# ADR-0004: Pacotes "module-like" em vez de módulos Gradle

- **Status:** Aceito
- **Data:** 2026-10-23

## Contexto
A separação em módulos Gradle melhora fronteiras e tempo de build, mas o setup (Hilt entre módulos, version catalog, dependências) consome tempo relevante em um projeto com prazo curto.

## Opções
1. **Módulos Gradle** (`:app`, `:core`, `:feature:auth`, `:feature:wallet`).
2. **Módulo único** sem regra de organização.
3. **Módulo único com pacotes "module-like"**: `core`, `feature.auth`, `feature.wallet`, cada um com API pública explícita e implementação interna.

## Decisão
Adotar **pacotes "module-like"** em um único módulo `:app`:
- Dependências em um só sentido: `feature.*` → `core`; nunca o inverso nem entre features.
- Cada feature expõe apenas seus contratos (interfaces, rotas de navegação); o restante é `internal`.
- Comunicação entre features por contratos definidos em `core`.

## Consequências
- (+) Setup rápido, sem perder a disciplina arquitetural.
- (+) Migração futura para módulos Gradle quase mecânica, pois as fronteiras já existem.
- (−) O compilador não impede violações de fronteira como um módulo Gradle faria. Mitigação: `internal` no Kotlin e uma verificação simples no CI (ex.: regras de arquitetura com Konsist).
- (−) Sem ganho de build incremental por módulo.
