-- Generic file-attachment table backing avatars, imóvel photos, and
-- contrato/garantia documents — one blob reference per row, disambiguated by
-- tipo + dono_id. Avatar semantics (single file, replace-on-upload) and
-- gallery/document semantics (many files, list-and-append) both fit this
-- same shape; only the use case differs.
CREATE TABLE arquivos (
    id uuid PRIMARY KEY,
    tipo varchar(50) NOT NULL,
    dono_id uuid NOT NULL,
    blob_key varchar(500) NOT NULL,
    nome_original varchar(255),
    content_type varchar(100),
    tamanho_bytes bigint NOT NULL,
    criado_em timestamptz NOT NULL,
    CONSTRAINT arquivos_tipo_check CHECK (tipo IN (
        'AVATAR_PROPRIETARIO', 'AVATAR_INQUILINO', 'FOTO_IMOVEL',
        'DOCUMENTO_CONTRATO', 'DOCUMENTO_GARANTIA'
    ))
);

CREATE INDEX idx_arquivos_dono_tipo ON arquivos (dono_id, tipo);
