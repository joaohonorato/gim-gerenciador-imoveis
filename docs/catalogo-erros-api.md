# Catálogo de erros da API

Toda resposta de erro da API (`backend/src/main/java/br/com/imoveis/infrastructure/rest/GlobalErrorHandler.java`)
tem o formato `{ "code": string, "message": string }`
(`infrastructure/rest/dto/ErrorResponse.java`). O `code` é estável e faz
parte do contrato da API; o `message` é a mensagem crua da exceção de
domínio/aplicação — pensada pra log e depuração, **não** pra mostrar direto
pro usuário final (é frequentemente técnica, às vezes em fragmentos como
`"tipoContaId é obrigatório..."`, ou concatenação de `path: msg` no caso de
`VALIDATION_ERROR`).

Esta tabela é o mapeamento oficial `code → mensagem de frontend orientada a
ação`. O frontend usa `getErrorMessage(error)` em
[`frontend/src/api/client.ts`](../frontend/src/api/client.ts) pra resolver
essa mensagem automaticamente a partir de qualquer erro lançado por
`apiFetch` — nenhuma tela deve montar a mensagem na mão a partir de
`error.message`/`error.error.message`.

| Código | HTTP | Exceção de origem | Quando ocorre | Mensagem para o usuário |
|---|---|---|---|---|
| `NOT_FOUND` | 404 | `NaoEncontradoException` | Recurso inexistente, ou existente mas fora da posse do principal autenticado (ver `docs/matriz-acesso-por-rota.md` — o padrão do projeto é responder 404, nunca 403, quando o chamador não tem acesso). | "Não encontramos o que você está procurando. Pode ter sido removido, ou você não tem acesso a ele." |
| `CONFLICT` | 409 | `ConflitoException` | Violação de unicidade ou de regra de negócio que impede a operação por já existir outro registro conflitante — e-mail/CPF-CNPJ já cadastrado, convite expirado/já iniciado/consumido, nome de categoria/tipo de conta duplicado, contrato sobreposto na mesma unidade. | "Já existe um registro com esses dados, ou a ação não é mais possível nesse contexto. Confira as informações e tente novamente." |
| `AUTH_INVALID` | 401 | `AutenticacaoInvalidaException` | Login/senha incorretos, sessão expirada ou token inválido/expirado/já usado (e-mail, redefinição de senha, magic link), ou tentativa de usar uma rota restrita a outro tipo de conta (ex.: inquilino chamando uma rota só de proprietário). O cliente já limpa a sessão local automaticamente quando esse código chega numa chamada autenticada. | "Não foi possível confirmar suas credenciais. Verifique login e senha, ou faça login novamente." |
| `CADASTRO_INCOMPLETO` | 422 | `CadastroIncompletoException` | Proprietário tentando uma ação que exige CPF/CNPJ cadastrado (hoje: assinar contrato) sem ter completado esse dado no perfil. | "Complete seu cadastro (CPF/CNPJ) no seu perfil antes de continuar." |
| `INVALID_TRANSITION` | 409 | `TransicaoInvalidaException` | Tentativa de mudar o estado de um agregado do domínio (unidade, contrato, candidatura etc.) de um jeito que a máquina de estados não permite a partir do estado atual. | "Essa ação não é permitida no momento — o item pode já ter mudado de status. Atualize a página e tente novamente." |
| `INVALID_INPUT` | 400 | `IllegalArgumentException` | Validação de regra de negócio feita no domínio/use case (não coberta por Bean Validation) — ex.: campo obrigatório condicional ausente, valor fora de faixa, combinação de campos inconsistente. | "Alguns dados enviados são inválidos. Revise as informações e tente novamente." |
| `INVALID_STATE` | 409 | `IllegalStateException` | Pré-condição de negócio não satisfeita que não é modelada como transição formal de estado — ex.: tentar interagir com uma candidatura/convite antes de uma etapa anterior do fluxo ter sido concluída. | "Essa ação não pode ser concluída agora. Verifique se as etapas anteriores foram concluídas e tente novamente." |
| `VALIDATION_ERROR` | 400 | `ConstraintViolationException` | Falha de Bean Validation (`@NotBlank`, `@Email`, `@Valid` etc.) nos campos do corpo da requisição. | "Verifique os campos preenchidos e tente novamente." |

Erros de rede/timeout (sem resposta HTTP da API) não passam por
`GlobalErrorHandler` — são representados no frontend por
`ApiNetworkException` (`NETWORK_ERROR`/`TIMEOUT`/`REQUEST_ABORTED`) e também
têm mensagem própria em `getErrorMessage`.

## Adicionando um código novo

1. Backend: nova exceção de domínio/aplicação + handler em
   `GlobalErrorHandler` mapeando pra um `code` novo (siga o padrão
   `SCREAMING_SNAKE_CASE` já usado).
2. Frontend: adicione o código em `ApiErrorCode` e sua mensagem em
   `ERROR_MESSAGES`, ambos em `frontend/src/api/client.ts`.
3. Atualize a tabela acima na mesma PR — este arquivo é o catálogo vivo, não
   um snapshot pontual.
