# ADR-0001: MVI na camada de apresentação

- **Status:** Aceito
- **Data:** 2026-10-23

## Contexto
O app tem telas com vários estados (carregando, erro, vazio, conteúdo, filtros ativos). Com MVVM tradicional e múltiplos `StateFlow`, o estado da tela pode ficar espalhado e inconsistente.

## Opções
1. **MVVM** com vários `StateFlow` por tela.
2. **MVI** com estado único imutável, `Intent` para ações do usuário e `Effect` para eventos únicos (navegação, snackbar).

## Decisão
Usar **MVI**.

## Consequências
- (+) Estado previsível, fácil de testar (entrada → novo estado) e de depurar.
- (+) Compose renderiza a partir de um único `State`.
- (−) Mais código repetitivo (boilerplate) que MVVM simples.
- (−) Exige disciplina para manter o estado realmente imutável.
