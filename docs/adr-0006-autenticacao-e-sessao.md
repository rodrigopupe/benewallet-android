# ADR-0006: Autenticação com tokens opacos, refresh rotativo e sessão cifrada

- **Status:** Aceito
- **Data:** 2026-10-26

## Contexto
O BFF servia saldo e extrato a qualquer requisição, e identificava o "usuário" por um header (`X-User-Id`) que o próprio cliente escolhia. Para um app de benefícios isso não se sustenta: qualquer um poderia ler os dados de outra conta. A RFC já previa um login simulado. Precisamos identificar quem pede, para proteger as rotas, servir dados e flags por conta e apagar os dados locais quando a conta sai.

## Opções
1. **Tela de login só no app**, sem proteção no BFF. Rápido, mas é cenografia: o BFF continua aberto.
2. **Manter o header de identificação.** Qualquer cliente se passa por qualquer usuário.
3. **Token de acesso curto + refresh token rotativo, emitidos pelo BFF.** Mais trabalho, mas protege de verdade e permite revogar sessões.

Dentro da opção 3, o formato do token: **JWT assinado** (sem estado no servidor, porém difícil de revogar antes de expirar) ou **token opaco aleatório** (o servidor guarda o estado e revoga na hora).

## Decisão
Adotar a opção 3 com **tokens opacos**.

**Por que opaco e não JWT:** nesse cenário, o token opaco é mais simples e permite revogar a sessão na hora, o que viabiliza o logout e a detecção de reuso do refresh token. Como só existe o BFF, não é preciso proteger chaves de assinatura nem manter claims compatíveis entre app e BFF, e o custo de guardar estado é baixo, já que ele é mockado e mantém as sessões em memória. Se vários serviços precisassem validar a identidade sem consultar o BFF, ou se ele rodasse em várias instâncias, o JWT (ou um armazenamento compartilhado das sessões) passaria a fazer mais sentido.

**BFF**
- `POST /auth/login`, `/auth/refresh` e `/auth/logout`. As rotas de dados e de flags exigem `Authorization: Bearer`.
- Token de acesso de **15 minutos**; refresh token de **7 dias**, de **uso único**: cada renovação devolve um par novo. Apresentar de novo um refresh token já usado indica vazamento e **revoga a sessão inteira**.
- Senhas com PBKDF2; a resposta é a mesma para e-mail desconhecido e senha errada; cinco falhas bloqueiam a conta por um minuto (`429` com `Retry-After`).
- O usuário vem **só do token**. O header `X-User-Id` deixou de existir.

**App**
- `core.auth` guarda os tokens **cifrados com AES-256-GCM, com a chave no Android Keystore**, no DataStore. Se não for possível decifrar (chave perdida, dado adulterado), o resultado é "sem sessão", não um erro.
- Um interceptor anexa o token e um `Authenticator` renova no `HTTP 401` e repete a requisição uma vez. Um `Mutex` garante uma única renovação mesmo com várias requisições falhando juntas: como o refresh token é de uso único, uma segunda renovação seria tratada como vazamento.
- Há **dois clientes OkHttp**. O das chamadas de login, renovação e logout não carrega token e não renova (senão o gerenciador de sessão dependeria de si mesmo), e loga só cabeçalhos, para a senha e os tokens nunca irem ao Logcat. O outro é o padrão do app.
- A **sessão é a fonte da navegação**: `Loading`, `SignedOut` e `SignedIn` escolhem a árvore de telas. Qualquer fim de sessão (botão, refresh token recusado) cai no login, sem pilha para voltar.
- Encerrar a sessão apaga tokens, banco e flags, por contrato (`SessionDataCleaner`), e **não pode ser cancelado no meio**. Só a revogação no servidor é "melhor esforço".

## Consequências
- (+) O BFF deixa de ser público e as flags passam a ser por conta, sem UUID de instalação.
- (+) Revogação imediata e detecção de reuso.
- (+) Segredos nunca em texto puro no disco nem no Logcat.
- (−) Estado no servidor: reiniciar o BFF (em memória) desloga todos. As duas contas são fixas e de demonstração, sem cadastro nem recuperação de senha.
- (−) Logout offline: a sessão local acaba, mas o refresh token continua válido no servidor até expirar (7 dias). Evolução: reenviar a revogação quando houver rede.
- (−) Os IDs de transação repetem entre contas, porque o mock os gera de 1 a N por conta. É seguro porque o logout apaga o banco; um cache por conta exigiria chave composta.
- (−) A cifra do Keystore só pode ser testada em aparelho e não tem teste automatizado; os testes usam um fake dela.
- (−) Sem biometria e sem *certificate pinning* (o BFF local roda em HTTP).
