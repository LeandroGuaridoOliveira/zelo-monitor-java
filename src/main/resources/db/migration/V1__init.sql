-- V1__init.sql: Tabelas base do Zelo Monitor

CREATE TABLE casa_monitorada (
    id UUID PRIMARY KEY,
    idoso_id UUID NOT NULL,
    nome_idoso VARCHAR(255) NOT NULL,
    endereco VARCHAR(255) NOT NULL,
    bairro VARCHAR(255),
    cidade VARCHAR(255),
    estado_casa VARCHAR(50) NOT NULL,
    consentimento_ativo BOOLEAN NOT NULL,
    ultimo_comodo VARCHAR(50),
    ultimo_estado_sensor VARCHAR(50),
    ultimo_evento_em TIMESTAMP WITH TIME ZONE,
    porta_entrada_aberta_em TIMESTAMP WITH TIME ZONE
);

CREATE TABLE regra_configuracao (
    id UUID PRIMARY KEY,
    casa_id UUID NOT NULL UNIQUE,
    horario_habitual_levantar VARCHAR(10) NOT NULL,
    tolerancia_matinal_min INT NOT NULL,
    limite_banheiro_min INT NOT NULL,
    limite_inatividade_min INT NOT NULL,
    espera_checkin_critico_min INT NOT NULL,
    espera_escalada_critico_min INT NOT NULL,
    espera_checkin_atencao_min INT NOT NULL,
    espera_escalada_atencao_min INT NOT NULL,
    CONSTRAINT fk_regra_casa FOREIGN KEY (casa_id) REFERENCES casa_monitorada (id)
);

CREATE TABLE alerta (
    id UUID PRIMARY KEY,
    casa_id UUID NOT NULL,
    regra VARCHAR(50) NOT NULL,
    gravidade VARCHAR(50) NOT NULL,
    origem VARCHAR(50) NOT NULL,
    estado VARCHAR(50) NOT NULL,
    contexto TEXT,
    nivel_escalada_atual INT NOT NULL,
    aberto_em TIMESTAMP WITH TIME ZONE NOT NULL,
    proxima_acao_em TIMESTAMP WITH TIME ZONE,
    encerrado_em TIMESTAMP WITH TIME ZONE,
    desfecho VARCHAR(50),
    observacao TEXT,
    CONSTRAINT fk_alerta_casa FOREIGN KEY (casa_id) REFERENCES casa_monitorada (id)
);

CREATE TABLE alerta_etapa (
    id UUID PRIMARY KEY,
    alerta_id UUID NOT NULL,
    nivel INT NOT NULL,
    contato_id UUID,
    nome_contato VARCHAR(255),
    canal VARCHAR(50) NOT NULL,
    enviado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    respondido_em TIMESTAMP WITH TIME ZONE,
    resposta VARCHAR(255),
    CONSTRAINT fk_etapa_alerta FOREIGN KEY (alerta_id) REFERENCES alerta (id)
);

CREATE TABLE conversa_checkin (
    id UUID PRIMARY KEY,
    alerta_id UUID NOT NULL,
    casa_id UUID NOT NULL,
    texto_idoso TEXT NOT NULL,
    classificacao VARCHAR(50) NOT NULL,
    motivo TEXT,
    resposta_para_idoso TEXT NOT NULL,
    origem_classificacao VARCHAR(50) NOT NULL,
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_conversa_alerta FOREIGN KEY (alerta_id) REFERENCES alerta (id)
);

-- Seed Casa da Dona Maria e RegraConfiguracao padrao
INSERT INTO casa_monitorada (
    id, idoso_id, nome_idoso, endereco, bairro, cidade, estado_casa, consentimento_ativo
) VALUES (
    '00000000-0000-0000-0000-000000000100',
    '00000000-0000-0000-0000-000000000010',
    'Dona Maria',
    'Rua das Flores, 120',
    'Vila Mariana',
    'São Paulo',
    'EM_CASA',
    TRUE
);

INSERT INTO regra_configuracao (
    id, casa_id, horario_habitual_levantar, tolerancia_matinal_min,
    limite_banheiro_min, limite_inatividade_min, espera_checkin_critico_min,
    espera_escalada_critico_min, espera_checkin_atencao_min, espera_escalada_atencao_min
) VALUES (
    '00000000-0000-0000-0000-000000000200',
    '00000000-0000-0000-0000-000000000100',
    '07:20',
    90,
    40,
    240,
    3,
    5,
    15,
    15
);
