# ADR-0005: Koin para injeção de dependências

- **Status:** Aceito
- **Data:** 2026-10-23

## Contexto
O app precisa de injeção de dependências para ViewModels, repositórios, rede, banco e observabilidade. As duas opções mais comuns no Android são Hilt e Koin. A configuração do Hilt chegou a ser iniciada, mas ainda não havia código de negócio dependendo dele.

## Opções
1. **Hilt:** validação do grafo em tempo de compilação, solução recomendada oficialmente, exige KSP/kapt.
2. **Koin:** DSL em Kotlin, menos código de configuração, sem processamento de anotações; erros de grafo aparecem em tempo de execução.

## Decisão
Adotar **Koin**. A equipe que desenvolve o projeto tem experiência recente e profunda com Koin, o que reduz o risco de erros de configuração e acelera a implementação dentro do prazo.

As dependências são expostas por **interfaces**; cada pacote `feature` declara o seu módulo Koin e o pacote `app` os agrega no `startKoin`, preservando as regras do ADR-0004.

## Consequências
- (+) Menos configuração e menos *boilerplate*; sem dependência de KSP para DI.
- (+) Módulos por feature combinam com a estrutura de pacotes "module-like".
- (+) Como o código depende de interfaces, a troca por outra solução de DI fica restrita aos módulos de injeção.
- (−) Dependências ausentes só falham em tempo de execução. Mitigação: teste que sobe o Koin com as definições reais do módulo e resolve o repositório (`WalletWiringTest`), rodando no CI. O `verify()` do `koin-test` foi descartado porque analisa apenas construtores e as definições do projeto usam lambdas.
- (−) Sem a validação em compilação do Hilt, exige disciplina ao registrar dependências.
