# language: pt-BR
# Jornada do Inquilino — ver docs/jornadas-e-prioridades-negocio.md (seção 3)
# e o diagrama de sequência correspondente.
#
# Tags:
#   @implementado — comportamento já existe hoje e pode virar teste de integração real agora.
#   @pendente     — especificação de comportamento ainda não construído, incluindo bugs
#                   conhecidos com causa raiz já identificada (ver
#                   docs/jornadas-e-prioridades-negocio.md).

Funcionalidade: Jornada do Inquilino
  Como inquilino convidado por um proprietário
  Quero concluir cadastro, enviar garantia e assinar o contrato sem precisar de ajuda
  Para alugar o imóvel sem burocracia manual

  @implementado
  Cenário: Aceitar convite criando conta nova
    Dado que recebi um token de convite de locação válido
    E não tenho conta de inquilino ainda
    Quando eu informo username, CPF, e-mail e senha em "/convites/{token}/cadastro"
    Então minha conta de inquilino é criada
    E uma candidatura é criada com status "PENDENTE"

  @implementado
  Cenário: Aceitar convite com conta já existente
    Dado que recebi um token de convite de locação válido
    E já tenho uma conta de inquilino autenticada
    Quando eu confirmo o vínculo em "/convites/{token}/aceitar-vinculo"
    Então uma candidatura é criada associada à minha conta existente

  @implementado
  Cenário: Enviar tipo e dados da garantia
    Dado que tenho uma candidatura pendente vinculada a um convite
    Quando eu informo o tipo de garantia e os dados específicos em "/convites/{token}/garantia"
    Então a candidatura é atualizada com a garantia escolhida

  @implementado
  Cenário: Acompanhar status da candidatura
    Dado que enviei uma candidatura para um convite
    Quando eu consulto "/convites/me"
    Então vejo o status atual da candidatura (pendente, aprovada ou recusada)

  @implementado
  Cenário: Assinar contrato e anexar documentos
    Dado que minha candidatura foi aprovada e um contrato foi gerado
    Quando eu anexo o documento do contrato e documentos de garantia
    E assino o contrato como inquilino
    Então "assinouInquilino" passa a ser verdadeiro
    E, se o proprietário já tiver assinado, o contrato fica com status "ASSINADO" e os pagamentos são gerados

  @implementado
  Cenário: Acessar a própria tela de perfil
    Dado que estou autenticado como inquilino
    Quando eu clico no avatar para abrir "Meu perfil"
    Então sou levado à tela de perfil do inquilino, não redirecionado de volta para a home
    # rota corrigida: (tenant)/tenant/perfil.tsx, aninhada pra não colidir
    # com (owner)/perfil/index.tsx

  @implementado
  Cenário: Ver endereço do imóvel e nome do proprietário na home, não IDs técnicos
    Dado que tenho um contrato assinado
    Quando eu abro minha home
    Então vejo o endereço do imóvel e o nome do proprietário
    E não vejo nenhum UUID técnico na tela
    # ConviteInquilinoResponse ganhou enderecoImovel/nomeProprietario

  @implementado
  Cenário: Contador de convites em andamento bate com a lista exibida
    Dado que tenho um convite já assinado e nenhum convite realmente em andamento
    Quando eu abro minha home
    Então o contador "Convites em andamento" mostra "0"
    E nenhum card de convite já assinado aparece na seção "em andamento"

  @implementado
  Cenário: Visualizar os próprios pagamentos
    Dado que tenho um contrato assinado com pagamentos gerados
    Quando eu abro a seção de pagamentos do meu contrato
    Então vejo a lista dos meus pagamentos com status e datas de vencimento
    # (tenant)/tenant/pagamentos.tsx

  @implementado
  Cenário: Abrir chamado de manutenção
    Dado que estou autenticado como inquilino com um contrato ativo
    Quando eu abro um chamado informando categoria e descrição
    Então o chamado é criado com status "ABERTO"
    E aparece na lista de chamados do imóvel, visível também ao proprietário
    # (tenant)/tenant/chamados.tsx — exige que o inquilino tenha contrato na
    # unidade do imóvel (checagem adicionada na auditoria de posse por rota)

  @implementado
  Cenário: Upload de documento comprobatório da garantia já na candidatura
    Dado que estou preenchendo a garantia durante o aceite do convite
    Quando eu anexo o documento comprobatório (RG, comprovante de renda, apólice) via "/convites/{token}/garantia/documentos"
    Então o documento fica disponível para o proprietário conferir em "/candidaturas" antes de aprovar a candidatura
    E, se o proprietário aprovar e o contrato for criado, o mesmo documento continua visível em "/contratos/{id}/documentos"
