# language: pt-BR
# Jornada do Proprietário — ver docs/jornadas-e-prioridades-negocio.md (seção 2)
# e o diagrama de sequência correspondente.
#
# Tags:
#   @implementado — comportamento já existe hoje e pode virar teste de integração real agora.
#   @pendente     — comportamento descrito em docs/jornadas-e-prioridades-negocio.md, ainda
#                   não implementado. Cenário serve de especificação para quando for construído;
#                   não deve ser automatizado antes disso (vai falhar de propósito).

Funcionalidade: Jornada do Proprietário
  Como proprietário de imóveis
  Quero cadastrar meus imóveis, convidar inquilinos e acompanhar contratos e pagamentos
  Para gerenciar minha operação de locação sem depender de planilha ou WhatsApp

  @implementado
  Cenário: Registro com cadastro básico, sem CPF/CNPJ obrigatório
    Dado que não existe conta para o e-mail "novo.proprietario@teste.com"
    Quando eu me registro como proprietário informando nome, e-mail e senha, sem informar CPF/CNPJ
    Então a conta é criada com sucesso
    E recebo um token de sessão válido
    E "GET /auth/me" retorna "cpfCnpj" nulo

  @implementado
  Cenário: Login com e-mail e senha
    Dado que existe uma conta de proprietário ativa com e-mail e senha conhecidos
    Quando eu envio e-mail e senha corretos para "/auth/login"
    Então recebo um token de sessão válido
    E sou redirecionado para o hub de Imóveis

  @implementado
  Cenário: Recuperação de senha esquecida
    Dado que existe uma conta de proprietário com e-mail "proprietario@teste.com"
    Quando eu solicito redefinição de senha para esse e-mail em "/auth/senha/esqueci"
    Então recebo sempre uma resposta 204, exista ou não conta para o e-mail
    E, se a conta existir, um token de redefinição é gerado e um e-mail é enviado (ou logado, se Resend não estiver configurado)
    Quando eu envio esse token com uma nova senha para "/auth/senha/redefinir"
    Então a senha é atualizada
    E consigo fazer login com a nova senha

  @implementado
  Cenário: Verificação de e-mail após registro
    Dado que acabei de registrar uma conta de proprietário
    Então um token de verificação de e-mail é gerado automaticamente
    Quando eu confirmo esse token em "/auth/email/confirmar"
    Então "GET /auth/me" passa a retornar "emailVerificado": true

  @implementado
  Cenário: Cadastro de imóvel com dados completos e autofill de CEP
    Dado que estou autenticado como proprietário
    Quando eu informo um CEP válido no formulário de novo imóvel
    Então endereço, bairro e cidade são preenchidos automaticamente via ViaCEP
    Quando eu completo quartos, banheiros, vagas, área em m² e IPTU e salvo
    Então o imóvel é criado com uma unidade padrão associada
    E os campos ficam disponíveis na tela de detalhe do imóvel

  @implementado
  Cenário: Geração de convite de locação
    Dado que estou autenticado como proprietário e tenho um imóvel com unidade vaga
    Quando eu gero um convite informando tipo de contrato, valor de aluguel, período e garantia aceita
    Então um token de convite é criado com status "PENDENTE"
    E um link é enviado ao e-mail do inquilino informado (ou marcado como "PULADO" se o envio não estiver configurado)

  @implementado
  Cenário: Aprovação de candidatura gera contrato
    Dado que existe uma candidatura pendente com garantia já informada
    Quando eu aprovo essa candidatura
    Então um contrato é criado com status de assinatura "PENDENTE"
    E a unidade correspondente passa para o status "RESERVADO"

  @implementado
  Cenário: Assinatura de contrato é bloqueada com cadastro incompleto
    Dado que estou autenticado como proprietário sem CPF/CNPJ cadastrado
    E existe um contrato pendente de minha assinatura
    Quando eu tento assinar o contrato como proprietário
    Então recebo erro HTTP 422 com código "CADASTRO_INCOMPLETO"
    E o contrato continua sem minha assinatura

  @implementado
  Cenário: Completar cadastro libera a assinatura de contrato
    Dado o cenário anterior, com a assinatura bloqueada por cadastro incompleto
    Quando eu informo um CPF ou CNPJ válido em "/auth/me/cpf-cnpj"
    E tento assinar o contrato novamente
    Então a assinatura é concluída com sucesso
    E "assinouProprietario" passa a ser verdadeiro

  @implementado
  Cenário: Visualizar despesas do imóvel ("Contas")
    Dado que estou autenticado como proprietário e tenho um imóvel com contas registradas (IPTU, condomínio, água, luz)
    Quando eu abro a tela de detalhe do imóvel
    Então vejo uma seção "Contas" listando cada despesa, referência e valor
    E consigo adicionar uma nova conta pelo formulário da própria seção

  @implementado
  Cenário: Marcar chamado de manutenção como resolvido
    Dado que existe um chamado com status "ABERTO" em um dos meus imóveis
    Quando eu marco o chamado como "em andamento" e depois como "resolvido"
    Então o status do chamado é atualizado e refletido na lista
    # botões "Marcar em andamento"/"Marcar resolvido" em (owner)/imoveis/[id]/index.tsx

  @implementado
  Cenário: Aviso de contrato próximo do vencimento
    Dado que tenho um contrato assinado cujo fim está a 60 dias ou menos da data atual
    Quando eu abro o hub de Contratos
    Então esse contrato aparece destacado com um quadrado laranja "Vence em X dias"
    E, se a data de fim já passou, com um triângulo vermelho "Vencido há X dias"
