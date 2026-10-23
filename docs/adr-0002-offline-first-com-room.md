# ADR-0002: Estratégia offline-first com Room

- **Status:** Aceito
- **Data:** 2026-10-23

## Contexto
O usuário consulta saldo e extrato em situações com rede instável. A experiência não deve depender de a requisição concluir para mostrar algo útil.

## Opções
1. **Online-only:** a UI lê direto da rede.
2. **Cache simples em memória.**
3. **Offline-first:** o Room é a fonte da verdade da UI; a rede apenas atualiza o banco.

## Decisão
Adotar **offline-first com Room**. A UI observa o banco via `Flow`; o repositório busca da rede e persiste. DataStore fica para preferências e metas, não para dados relacionais.

## Consequências
- (+) Resposta imediata e funcionamento offline.
- (+) Uma única fonte de dados para a UI.
- (−) Necessidade de definir invalidação de cache (começa com TTL simples).
- (−) Mais código de mapeamento (DTO ↔ entidade ↔ domínio).
