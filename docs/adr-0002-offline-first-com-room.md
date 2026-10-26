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

## Atualização (2026-10-25): decisões de implementação
- **Cache x paginação x filtros:** snapshot único, substituído por inteiro no refresh (evita buracos) e estendido por "carregar mais" com o cursor guardado em `transactions_sync`. Cache por filtro e `RemoteMediator` ficam como evolução.
- **Invalidação:** TTL com *stale-while-revalidate* (saldo 2 min, extrato 5 min). Falha de rede preserva o cache.
- **Migração:** `fallbackToDestructiveMigration`, pois o banco é apenas cache; schema exportado.

## Atualização (2026-10-26): logout
- O banco guarda dados de uma conta, então encerrar a sessão o **apaga por completo**, incluindo os marcadores de sincronização (um `fetchedAt` antigo faria a conta seguinte pular o primeiro refresh). Ver ADR-0006.
