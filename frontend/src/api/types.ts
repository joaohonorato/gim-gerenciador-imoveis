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

export interface Contrato {
  id: string;
  unidadeId: string;
  inquilinoId: string;
  proprietarioId: string;
  tipo?: string;
  tipoContrato?: string;
  valorAluguel: number;
  dataInicio: string;
  dataFim: string;
  statusAssinatura: 'PENDENTE' | 'ASSINADO';
  assinouProprietario: boolean;
  assinouInquilino: boolean;
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
}

export interface Inquilino {
  id: string;
  nome: string;
  cpf: string | null;
  email: string | null;
  criadoEm: string;
}

export interface Chamado {
  id: string;
  imovelId: string;
  abertoPor: string;
  categoria: string;
  descricao: string;
  status: 'ABERTO' | 'EM_ANDAMENTO' | 'RESOLVIDO';
  abertoEm: string;
  resolvidoEm: string | null;
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
}

export interface ConviteInquilino {
  conviteId: string;
  token: string;
  imovelId: string;
  unidadeId: string;
  proprietarioId: string;
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
