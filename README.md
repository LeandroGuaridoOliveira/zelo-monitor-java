# zelo-monitor-java · Monitoramento Não Invasivo de Rotina (Zelo)

Serviço central (`zelo-monitor`) em **Java com Spring Boot 3.2.4 e Java 17** para a disciplina de **Java Advanced** (Global Solution 2026/2 — FIAP — Tema **CitizenTech**).

> **Aviso de MVP Hipotético:** A casa, os sensores e as pessoas (Dona Maria, Ana e Seu José) são **simulados** usando os identificadores UUID fixos definidos no documento de requisitos do projeto. Nenhum dado de pessoa real é utilizado.

---

## Integrantes do Grupo (2TDSPV)

| Nome | RM |
| :--- | :--- |
| Leandro Guarido de Oliveira | 561760 |
| Gabriel Costa Solano | 562325 |
| Kaiky Pereira Rodrigues Da Silva | 564578 |

---

## 🎥 Vídeo Demonstrativo (Pitch GS)

- **Link no YouTube:** [https://youtu.be/tu2Uynehp2Q](https://youtu.be/tu2Uynehp2Q)

---

## Requisitos Atendidos na Solução

1. **Requisito 1: API REST do Domínio (20 pts):**
   - 5 entidades JPA relacionadas: `CasaMonitorada`, `RegraConfiguracao`, `Alerta`, `EtapaAlerta`, `ConversaCheckin`.
   - Migração e seed inicial de banco com **Flyway** (`src/main/resources/db/migration/V1__init.sql`).
   - Consultas paginadas com `Pageable`, `Page<T>` e `@PageableDefault`.
2. **Requisito 2: Validação e Tratamento de Erros (10 pts):**
   - Bean Validation com anotações (`@NotNull`, `@NotBlank`, `@Min`, `@Max`, `@Pattern`, `@Size`) em todos os DTOs de entrada.
   - Tratamento centralizado de exceções via `@RestControllerAdvice` (`GlobalExceptionHandler`) com formato padronizado: `{"codigo": "...", "mensagem": "..."}`.
3. **Requisito 3: Segurança com Spring Security + JWT (20 pts):**
   - Autenticação stateless via token JWT (HS256 com `jjwt 0.11.5`).
   - Mapeamento de papéis (`ROLE_IDOSO`, `ROLE_CUIDADOR`, `ROLE_FAMILIAR`, `ROLE_GUARDIAO`, `ROLE_ADMIN`, `ROLE_SERVICO`).
   - Proteção granular de rotas por `hasRole` e `hasAnyRole`.
4. **Requisito 4: Spring AI Integrado ao Fluxo (20 pts):**
   - Injeção de `OpenAiChatClient` (via `spring-ai-openai-spring-boot-starter`).
   - Classificação do check-in do idoso em `ESTOU_BEM`, `PRECISO_AJUDA` ou `INCERTO`.
   - Geração de resposta acolhedora de até 2 frases em português para o idoso.
   - Guardrail de segurança mandatório (força `PRECISO_AJUDA` se houver termos como "caí", "dor", "tonta").
   - Fallback determinístico por palavras-chave caso o LLM esteja inacessível.
5. **Requisito 5: Arquitetura Distribuída — Opção A: RabbitMQ (20 pts):**
   - Exchange do tipo topic (`zelo` e integração com `amq.topic`).
   - Filas persistentes: `zelo.sensor.eventos`, `zelo.sensor.relogio`, `zelo.alertas.notificacoes`.
   - Produtor (`SensorEventProducer`) e Consumidor (`SensorEventConsumer` com `@RabbitListener`).
6. **Requisito 6: Documentação, Testes e Docker (10 pts):**
   - Swagger / OpenAPI 3 disponível em `/swagger-ui.html`.
   - Suíte de testes automatizados JUnit 5 + Mockito cobrindo regras R1 a R5, emergência E1, máquina de estados, Spring AI e segurança.
   - `Dockerfile` multi-stage para containerização eficiente.

---

## Usuários de Teste Pré-Configurados

Todos os usuários abaixo possuem a senha padrão `123456`:

| E-mail | Username | Papel | Perfil no Sistema |
| :--- | :--- | :--- | :--- |
| `maria@zelo.com` | `maria` | `IDOSO` | Dona Maria (Idosa monitorada) |
| `ana@zelo.com` | `ana` | `FAMILIAR` (`ROLE_CUIDADOR`) | Ana (Filha / Contato Ordem 1) |
| `jose@zelo.com` | `jose` | `GUARDIAO` (`ROLE_CUIDADOR`) | Seu José (Vizinho / Contato Ordem 2) |
| `admin@zelo.com` | `admin` | `ADMIN` | Administrador da Plataforma |
| `servico@zelo.com` | `servico` | `SERVICO` | Integrações e Barramento IoT |

---

## Principais Endpoints da API

### Autenticação (`/api/v1/auth`)
- `POST /api/v1/auth/login`: Autentica com e-mail/username e senha, retornando token JWT e claims.
- `POST /api/v1/auth/signup`: Cadastro de novos usuários.

### Monitoramento e Alertas (`/api/v1`)
- `GET /api/v1/casas/{id}/status`: Resumo do status da casa, último evento e alerta aberto.
- `GET /api/v1/alertas`: Consulta paginada de alertas com filtros por `casaId` e `estado`.
- `GET /api/v1/alertas/{id}`: Detalhes de um alerta específico.
- `POST /api/v1/alertas`: Abertura externa de alerta (ex.: Lembrete não confirmado R5).
- `POST /api/v1/casas/{id}/emergencia`: Acionamento do botão de emergência E1.
- `POST /api/v1/alertas/{id}/checkin`: Resposta do idoso (por botão ou texto livre analisado por Spring AI).
- `POST /api/v1/alertas/{id}/confirmacao`: Cuidador confirma recebimento ("Vou verificar").
- `POST /api/v1/alertas/{id}/desfecho`: Cuidador registra desfecho e encerra o alerta.

### Gestão de Casas e Regras (`/api/v1/casas`)
- `GET /api/v1/casas`: Listagem paginada de casas.
- `POST /api/v1/casas`: Cadastro de nova casa e regras padrão (requer `ADMIN`).
- `GET /api/v1/casas/{id}/regras`: Consulta de limites e tolerâncias de monitoramento.
- `PUT /api/v1/casas/{id}/regras`: Atualização dos limites da casa.

### Simulação e Demonstração (`/api/v1/simulacao`)
- `POST /api/v1/simulacao/evento`: Envio manual de evento de sensor.
- `POST /api/v1/simulacao/relogio`: Avanço manual do relógio simulado da casa.
- `POST /api/v1/simulacao/roteiro/{numero}`: Execução imediata dos Roteiros 1 a 4.
- `POST /api/v1/simulacao/reset`: Reseta o estado da casa para condições iniciais.

---

## Execução dos Roteiros de Teste

1. **Roteiro 1 — Dia Normal:** Eventos matinais normais no quarto, banheiro e cozinha. Nenhum alerta é gerado.
2. **Roteiro 2 — Ida à Feira:** Abertura da porta de entrada sem eventos internos por 10 minutos altera a casa para `FORA_DE_CASA`, suspendendo regras de sensor por 2 horas até o retorno de Dona Maria.
3. **Roteiro 3 — Queda no Banheiro (R2):** Dona Maria entra no banheiro. Aos 39 minutos nada acontece. Aos 40 minutos exatos, o alerta `R2` `CRITICO` é aberto em `AGUARDANDO_IDOSO`.
4. **Roteiro 4 — Sem Atividade Matinal (R1):** Nenhum evento após as 04:00. Ao atingir o horário habitual (07:20) + tolerância (90 min) = 08:50, abre alerta `R1` `ATENCAO`.

---

## Como Executar

### Pré-requisitos
- JDK 17
- Docker e Docker Compose (para RabbitMQ)

### Passo a Passo Local
```bash
# 1. Suba o RabbitMQ
docker compose up -d

# 2. Execute os testes automatizados
./mvnw clean test

# 3. Inicie o servidor Spring Boot
./mvnw spring-boot:run
```

Acesse o Swagger UI em:
`http://localhost:8080/swagger-ui.html`

### Execução via Docker
```bash
# Construção da imagem multi-stage
docker build -t zelo-monitor:latest .

# Execução do container
docker run -p 8080:8080 zelo-monitor:latest
```

---

## Declaração de Uso de IA

O desenvolvimento desta aplicação utilizou modelos de linguagem para auxílio na estruturação de padrões arquiteturais Spring Boot, geração de prompts de classificação para o Spring AI e elaboração de casos de teste automatizados em JUnit 5. Todos os códigos e regras de negócio foram revisados, ajustados e validados pela equipe para atender integralmente aos requisitos da disciplina e aos critérios do tema CitizenTech.
