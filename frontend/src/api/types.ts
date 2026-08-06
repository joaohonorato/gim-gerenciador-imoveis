export interface Imovel {
  id: string;
  endereco: string;
  cidade: string;
  matricula: string;
  numero?: string | null;
  bairro?: string | null;
  complemento?: string | null;
  tipoImovel?: 'CASA' | 'APARTAMENTO' | 'COMERCIAL' | 'OUTRO' | null;
  enderecoCompleto?: string;
  visibilidade: 'PUBLICADO' | 'PRIVADO';
  unidades?: Array<{
    id: string;
    nome: string;
    padrao: boolean;
    status: 'VAGO' | 'RESERVADO' | 'ALUGADO' | 'MANUTENCAO';
  }>;
  fotos?: Array<{ id: string; url: string }>;
  quartos?: number | null;
  banheiros?: number | null;
  vagas?: number | null;
  areaM2?: number | null;
  iptu?: number | null;
  cep?: string | null;
  criadoEm: string;
}

// Mesmo request cobre cadastro individual (nomes com 1 item) e em lote
// (vários de uma vez) — ver POST /imoveis/{id}/unidades.
export interface AdicionarUnidadesRequest {
  nomes: string[];
}

export interface NovoImovelRequest {
  endereco: string;
  cidade: string;
  matricula: string;
  numero?: string;
  bairro?: string;
  complemento?: string;
  tipoImovel?: string;
  quartos?: number;
  banheiros?: number;
  vagas?: number;
  areaM2?: number;
  iptu?: number;
  cep?: string;
}

export interface Convite {
  token: string;
  id?: string;
  tipoContrato: string;
  valorAluguel: number;
  dataInicio: string;
  dataFim: string;
  garantiaAceita: string | null;
  status?: 'PENDENTE' | 'EM_ANALISE' | 'CONSUMIDO' | 'EXPIRADO' | 'RECUSADO' | 'REVOGADO';
  // Calculado no backend a partir de expiraEm — o domínio nunca grava
  // status = EXPIRADO de verdade (ver docs/especificacao-produto.md §3),
  // então este é o único jeito confiável de saber se o link já morreu
  // enquanto status ainda aparece como PENDENTE.
  expirado?: boolean;
  imovelId?: string;
  unidadeId?: string;
  proprietarioId?: string;
  envio?: {
    canal: 'EMAIL' | 'WHATSAPP';
    status: 'ENVIADO' | 'PRONTO_PARA_ENVIO' | 'PULADO' | 'FALHA';
    destino?: string | null;
    whatsappShareUrl?: string | null;
    detalhe?: string | null;
    enviadoEm?: string | null;
    tentativas?: number | null;
  } | null;
}

export interface EventoAuditoriaConvite {
  tipoEvento: 'CRIADO' | 'ENVIADO' | 'RENOVADO' | 'CANDIDATURA_CRIADA' | 'CONSUMIDO' | 'ACEITO'
    | 'REVOGADO' | 'RECUSADO' | 'TENTATIVA_COM_TOKEN_EXPIRADO' | 'TENTATIVA_COM_TOKEN_INVALIDO_OU_CONSUMIDO';
  detalhe: string | null;
  criadoEm: string;
}

export interface Contrato {
  id: string;
  unidadeId: string;
  imovelId: string | null;
  inquilinoId: string;
  proprietarioId: string;
  nomeProprietario: string | null;
  enderecoImovel: string | null;
  tipo?: string;
  tipoContrato?: string;
  valorAluguel: number;
  dataInicio: string;
  dataFim: string;
  statusAssinatura: 'PENDENTE' | 'ASSINADO';
  assinouProprietario: boolean;
  assinouInquilino: boolean;
  garantiaTipo?: string | null;
}

export interface ArquivoInfo {
  id: string;
  nomeOriginal: string;
}

export interface DocumentosContrato {
  documentoContrato: ArquivoInfo | null;
  documentosGarantia: ArquivoInfo[];
}

export interface Pagamento {
  id: string;
  contratoId: string;
  vencimento: string;
  valor: number;
  status: 'PENDENTE' | 'PAGO' | 'ATRASADO';
}

export interface ApiError {
  code: string;
  message: string;
  details?: string[];
}

export type TipoConta = 'PROPRIETARIO' | 'INQUILINO';

export interface MeResponse {
  id: string | null;
  nome: string | null;
  email: string | null;
  tipoConta: TipoConta;
  avatarUrl: string | null;
  emailVerificado: boolean;
  cpfCnpj: string | null;
  telefone: string | null;
  // E-mail solicitado via POST /auth/me/email, ainda não confirmado — o
  // login continua pelo `email` acima até o link de confirmação (enviado
  // pra este endereço) ser aberto.
  emailPendente: string | null;
}

export interface AtualizarTelefoneRequest {
  telefone: string;
}

export interface AtualizarNomeRequest {
  nome: string;
}

export interface SolicitarAlteracaoEmailRequest {
  novoEmail: string;
}

export interface TrocarSenhaRequest {
  senhaAtual: string;
  novaSenha: string;
}

export interface Inquilino {
  id: string;
  nome: string;
  cpf: string | null;
  email: string | null;
  criadoEm: string;
}

// Catálogo dinâmico por proprietário — antes um union fixo
// ('ELETRICA' | 'HIDRAULICA' | 'ESTRUTURAL' | 'OUTRO'), mesmo padrão de
// TipoContaImovel.
export interface CategoriaChamado {
  id: string;
  nome: string;
}

export interface Chamado {
  id: string;
  imovelId: string;
  unidadeId: string;
  unidadeNome: string | null;
  abertoPor: string;
  categoriaId: string;
  categoriaNome: string;
  descricao: string;
  status: 'ABERTO' | 'EM_ANDAMENTO' | 'RESOLVIDO';
  abertoEm: string;
  resolvidoEm: string | null;
}

export interface NovoChamadoRequest {
  categoriaId: string;
  descricao: string;
}

export interface AtualizarChamadoRequest {
  status: 'EM_ANDAMENTO' | 'RESOLVIDO';
}

export interface NovaCategoriaChamadoRequest {
  nome: string;
}

// "TipoContaImovel" (não "TipoConta") pra não colidir com o TipoConta de
// conta de acesso (PROPRIETARIO | INQUILINO) já definido acima. Catálogo
// dinâmico por proprietário, não mais um union fixo.
export interface TipoContaImovel {
  id: string;
  nome: string;
}

export interface Conta {
  id: string;
  unidadeId: string;
  tipoContaId: string;
  tipoContaNome: string;
  vencimento: string;
  valor: number;
  status: 'PENDENTE' | 'PAGO';
  responsavel: 'PROPRIETARIO' | 'INQUILINO';
  contratoId: string | null;
}

export interface NovaContaRequest {
  tipoContaId: string;
  vencimento: string;
  valor: number;
  responsavel: 'PROPRIETARIO' | 'INQUILINO';
  contratoId?: string | null;
}

export interface NovoTipoContaRequest {
  nome: string;
}

export interface CandidaturaPendente {
  id: string;
  imovelId: string;
  unidadeId: string;
  inquilinoId: string;
  inquilinoNome: string | null;
  inquilinoEmail: string | null;
  tipoContrato: string;
  valorAluguel: number;
  dataInicio: string;
  dataFim: string;
  garantiaAceita: string | null;
  garantiaEscolhida: string | null;
  criadaEm: string;
  // Micronaut Serde omite campos de lista vazios em objetos aninhados — vem
  // ausente (não `[]`) quando a candidatura não tem nenhum documento ainda.
  documentosGarantia?: ArquivoInfo[];
}

export interface ConviteInquilino {
  conviteId: string;
  token: string;
  imovelId: string;
  enderecoImovel: string | null;
  unidadeId: string;
  proprietarioId: string;
  nomeProprietario: string | null;
  conviteStatus: 'PENDENTE' | 'EM_ANALISE' | 'CONSUMIDO' | 'EXPIRADO' | 'RECUSADO' | 'REVOGADO';
  candidaturaId: string;
  candidaturaStatus: 'PENDENTE' | 'APROVADA' | 'RECUSADA';
  tipoContrato: string;
  valorAluguel: number;
  dataInicio: string;
  dataFim: string;
  garantiaAceita: string | null;
  garantiaEscolhida: string | null;
  criadaEm: string;
  contratoId: string | null;
  statusAssinaturaContrato: 'PENDENTE' | 'ASSINADO' | null;
}
