-- Baseline snapshot of the schema as it exists in production (pg_dump --schema-only
-- --schema=public, cleaned of pg_dump housekeeping), taken 2026-07-31. Applied for
-- real only against empty databases (fresh local resets, future dev/hml); existing
-- databases are baselined at this version instead (see flyway.baseline-on-migrate).

CREATE TABLE public.candidaturas (
    id uuid NOT NULL,
    convite_id uuid NOT NULL,
    criada_em timestamp(6) with time zone NOT NULL,
    dados_garantia text,
    garantia_escolhida character varying(255),
    inquilino_id uuid NOT NULL,
    status character varying(255) NOT NULL,
    CONSTRAINT candidaturas_garantia_escolhida_check CHECK (((garantia_escolhida)::text = ANY ((ARRAY['CAUCAO'::character varying, 'FIADOR'::character varying, 'SEGURO_FIANCA'::character varying, 'TITULO_CAPITALIZACAO'::character varying, 'NENHUMA'::character varying])::text[]))),
    CONSTRAINT candidaturas_status_check CHECK (((status)::text = ANY ((ARRAY['PENDENTE'::character varying, 'APROVADA'::character varying, 'RECUSADA'::character varying])::text[])))
);

CREATE TABLE public.chamados (
    id uuid NOT NULL,
    aberto_em timestamp(6) with time zone NOT NULL,
    aberto_por uuid NOT NULL,
    categoria character varying(255) NOT NULL,
    descricao character varying(4000) NOT NULL,
    imovel_id uuid NOT NULL,
    resolvido_em timestamp(6) with time zone,
    status character varying(255) NOT NULL,
    CONSTRAINT chamados_categoria_check CHECK (((categoria)::text = ANY ((ARRAY['ELETRICA'::character varying, 'HIDRAULICA'::character varying, 'ESTRUTURAL'::character varying, 'OUTRO'::character varying])::text[]))),
    CONSTRAINT chamados_status_check CHECK (((status)::text = ANY ((ARRAY['ABERTO'::character varying, 'EM_ANDAMENTO'::character varying, 'RESOLVIDO'::character varying])::text[])))
);

CREATE TABLE public.contas (
    id uuid NOT NULL,
    imovel_id uuid NOT NULL,
    status character varying(255) NOT NULL,
    tipo character varying(255) NOT NULL,
    valor numeric(38,2) NOT NULL,
    vencimento date NOT NULL,
    CONSTRAINT contas_status_check CHECK (((status)::text = ANY ((ARRAY['PENDENTE'::character varying, 'PAGO'::character varying])::text[]))),
    CONSTRAINT contas_tipo_check CHECK (((tipo)::text = ANY ((ARRAY['IPTU'::character varying, 'CONDOMINIO'::character varying, 'AGUA'::character varying, 'LUZ'::character varying])::text[])))
);

CREATE TABLE public.contas_acesso (
    id uuid NOT NULL,
    criado_em timestamp(6) with time zone NOT NULL,
    email character varying(255) NOT NULL,
    inquilino_id uuid,
    proprietario_id uuid,
    senha_hash character varying(255) NOT NULL,
    tipo character varying(255) NOT NULL,
    CONSTRAINT contas_acesso_tipo_check CHECK (((tipo)::text = ANY ((ARRAY['PROPRIETARIO'::character varying, 'INQUILINO'::character varying])::text[])))
);

CREATE TABLE public.contratos (
    id uuid NOT NULL,
    assinou_inquilino boolean NOT NULL,
    assinou_proprietario boolean NOT NULL,
    data_fim date NOT NULL,
    data_inicio date NOT NULL,
    garantia_dados text,
    garantia_id uuid,
    garantia_tipo character varying(255),
    garantia_vencimento date,
    indice_reajuste character varying(255) NOT NULL,
    inquilino_id uuid NOT NULL,
    proprietario_id uuid NOT NULL,
    status_assinatura character varying(255) NOT NULL,
    tipo character varying(255) NOT NULL,
    unidade_id uuid NOT NULL,
    valor_aluguel numeric(38,2) NOT NULL,
    CONSTRAINT contratos_garantia_tipo_check CHECK (((garantia_tipo)::text = ANY ((ARRAY['CAUCAO'::character varying, 'FIADOR'::character varying, 'SEGURO_FIANCA'::character varying, 'TITULO_CAPITALIZACAO'::character varying, 'NENHUMA'::character varying])::text[]))),
    CONSTRAINT contratos_status_assinatura_check CHECK (((status_assinatura)::text = ANY ((ARRAY['PENDENTE'::character varying, 'ASSINADO'::character varying])::text[]))),
    CONSTRAINT contratos_tipo_check CHECK (((tipo)::text = ANY ((ARRAY['RESIDENCIAL'::character varying, 'TEMPORADA'::character varying])::text[])))
);

CREATE TABLE public.convites (
    id uuid NOT NULL,
    candidatura_id uuid,
    expira_em timestamp(6) with time zone NOT NULL,
    garantia_aceita character varying(255),
    imovel_id uuid NOT NULL,
    periodo_fim date NOT NULL,
    periodo_inicio date NOT NULL,
    proprietario_id uuid NOT NULL,
    status character varying(255) NOT NULL,
    tentativas_envio integer NOT NULL,
    tipo_contrato character varying(255) NOT NULL,
    token character varying(64) NOT NULL,
    ultimo_canal_envio character varying(255),
    ultimo_destino_envio character varying(255),
    ultimo_detalhe_envio text,
    ultimo_envio_em timestamp(6) with time zone,
    ultimo_status_envio character varying(255),
    ultimo_whatsapp_share_url text,
    unidade_id uuid NOT NULL,
    valor_aluguel numeric(38,2) NOT NULL,
    CONSTRAINT convites_garantia_aceita_check CHECK (((garantia_aceita)::text = ANY ((ARRAY['CAUCAO'::character varying, 'FIADOR'::character varying, 'SEGURO_FIANCA'::character varying, 'TITULO_CAPITALIZACAO'::character varying, 'NENHUMA'::character varying])::text[]))),
    CONSTRAINT convites_status_check CHECK (((status)::text = ANY ((ARRAY['PENDENTE'::character varying, 'EM_ANALISE'::character varying, 'CONSUMIDO'::character varying, 'EXPIRADO'::character varying, 'RECUSADO'::character varying, 'REVOGADO'::character varying])::text[]))),
    CONSTRAINT convites_tipo_contrato_check CHECK (((tipo_contrato)::text = ANY ((ARRAY['RESIDENCIAL'::character varying, 'TEMPORADA'::character varying])::text[]))),
    CONSTRAINT convites_ultimo_canal_envio_check CHECK (((ultimo_canal_envio)::text = ANY ((ARRAY['EMAIL'::character varying, 'WHATSAPP'::character varying])::text[])))
);

CREATE TABLE public.convites_acesso (
    id uuid NOT NULL,
    consumido boolean NOT NULL,
    criado_em timestamp(6) with time zone NOT NULL,
    documento character varying(255) NOT NULL,
    email character varying(255) NOT NULL,
    expira_em timestamp(6) with time zone NOT NULL,
    nome character varying(255) NOT NULL,
    perfil_proprietario character varying(255),
    tipo character varying(255) NOT NULL,
    token character varying(64) NOT NULL,
    CONSTRAINT convites_acesso_tipo_check CHECK (((tipo)::text = ANY ((ARRAY['PROPRIETARIO'::character varying, 'INQUILINO'::character varying])::text[])))
);

CREATE TABLE public.imoveis (
    id uuid NOT NULL,
    bairro character varying(255),
    cidade character varying(255) NOT NULL,
    complemento character varying(255),
    endereco character varying(255) NOT NULL,
    matricula character varying(255) NOT NULL,
    numero character varying(255),
    proprietario_id uuid NOT NULL,
    tipo_imovel character varying(255),
    visibilidade character varying(255) NOT NULL,
    CONSTRAINT imoveis_tipo_imovel_check CHECK (((tipo_imovel)::text = ANY ((ARRAY['CASA'::character varying, 'APARTAMENTO'::character varying, 'COMERCIAL'::character varying, 'OUTRO'::character varying])::text[]))),
    CONSTRAINT imoveis_visibilidade_check CHECK (((visibilidade)::text = ANY ((ARRAY['PRIVADO'::character varying, 'PUBLICADO'::character varying])::text[])))
);

CREATE TABLE public.inquilinos (
    id uuid NOT NULL,
    cpf character varying(255) NOT NULL,
    criado_em timestamp(6) with time zone NOT NULL,
    email character varying(255) NOT NULL,
    nome character varying(255) NOT NULL
);

CREATE TABLE public.magic_links (
    id uuid NOT NULL,
    consumido boolean NOT NULL,
    criado_em timestamp(6) with time zone NOT NULL,
    email character varying(255) NOT NULL,
    expira_em timestamp(6) with time zone NOT NULL,
    token character varying(64) NOT NULL
);

CREATE TABLE public.pagamentos (
    id uuid NOT NULL,
    contrato_id uuid NOT NULL,
    pago_em date,
    status character varying(255) NOT NULL,
    valor numeric(38,2) NOT NULL,
    vencimento date NOT NULL,
    CONSTRAINT pagamentos_status_check CHECK (((status)::text = ANY ((ARRAY['PENDENTE'::character varying, 'PAGO'::character varying, 'ATRASADO'::character varying])::text[])))
);

CREATE TABLE public.proprietarios (
    id uuid NOT NULL,
    cpf_cnpj character varying(255) NOT NULL,
    criado_em timestamp(6) with time zone NOT NULL,
    email character varying(255) NOT NULL,
    nome character varying(255) NOT NULL,
    perfil character varying(255) NOT NULL
);

CREATE TABLE public.sessoes (
    id uuid NOT NULL,
    conta_acesso_id uuid NOT NULL,
    expira_em timestamp(6) with time zone NOT NULL,
    inquilino_id uuid,
    proprietario_id uuid,
    tipo_conta character varying(255) NOT NULL,
    token character varying(64) NOT NULL,
    CONSTRAINT sessoes_tipo_conta_check CHECK (((tipo_conta)::text = ANY ((ARRAY['PROPRIETARIO'::character varying, 'INQUILINO'::character varying])::text[])))
);

CREATE TABLE public.unidades (
    id uuid NOT NULL,
    imovel_id uuid NOT NULL,
    nome character varying(255) NOT NULL,
    padrao boolean NOT NULL,
    status character varying(255) NOT NULL,
    CONSTRAINT unidades_status_check CHECK (((status)::text = ANY ((ARRAY['VAGO'::character varying, 'RESERVADO'::character varying, 'ALUGADO'::character varying, 'MANUTENCAO'::character varying])::text[])))
);

ALTER TABLE ONLY public.candidaturas ADD CONSTRAINT candidaturas_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.chamados ADD CONSTRAINT chamados_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.contas_acesso ADD CONSTRAINT contas_acesso_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.contas ADD CONSTRAINT contas_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.contratos ADD CONSTRAINT contratos_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.convites_acesso ADD CONSTRAINT convites_acesso_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.convites ADD CONSTRAINT convites_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.imoveis ADD CONSTRAINT imoveis_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.inquilinos ADD CONSTRAINT inquilinos_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.magic_links ADD CONSTRAINT magic_links_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.pagamentos ADD CONSTRAINT pagamentos_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.proprietarios ADD CONSTRAINT proprietarios_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.sessoes ADD CONSTRAINT sessoes_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.unidades ADD CONSTRAINT unidades_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.convites ADD CONSTRAINT uk1v01mdxw3kgqtmvn9uodp0hg2 UNIQUE (token);
ALTER TABLE ONLY public.inquilinos ADD CONSTRAINT uk3wjgobru88g913rmv6de2v465 UNIQUE (cpf);
ALTER TABLE ONLY public.magic_links ADD CONSTRAINT uk76nv5akw8tgvns3b7msrjs0jl UNIQUE (token);
ALTER TABLE ONLY public.convites_acesso ADD CONSTRAINT ukc9c4vp5e0wl5b2vt9idiobpbd UNIQUE (token);
ALTER TABLE ONLY public.proprietarios ADD CONSTRAINT ukcrba7aq8746rrscoqqyw5klek UNIQUE (cpf_cnpj);
ALTER TABLE ONLY public.proprietarios ADD CONSTRAINT ukqw1rsij7t1cfut0w4nm73bqo UNIQUE (email);
ALTER TABLE ONLY public.contas_acesso ADD CONSTRAINT ukrpnjkp0o54o2m15pmofmxntn7 UNIQUE (email);
ALTER TABLE ONLY public.inquilinos ADD CONSTRAINT ukt3wl8yutnbr2r0jtvs0yk3jl8 UNIQUE (email);
ALTER TABLE ONLY public.sessoes ADD CONSTRAINT uktmocqmoc8nhxb6g6xs66xks9f UNIQUE (token);
