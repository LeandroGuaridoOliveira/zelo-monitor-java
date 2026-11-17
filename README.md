# zelo-monitor-java · Monitoramento Não Invasivo de Rotina (Zelo)

Serviço central (`zelo-monitor`) em **Java com Spring Boot** para a disciplina de **Java Advanced** (Global Solution 2026/2 — FIAP — Tema **CitizenTech**).

> **Aviso de MVP Hipotético:** A casa, os sensores e as pessoas (Dona Maria, Ana e Seu José) são **simulados** usando os IDs fixos de teste do documento-base do projeto. Nenhum dado de pessoa real é utilizado.

## Integrantes do Grupo (2TDSPV)

| Nome | RM |
| :--- | :--- |
| Leandro Guarido de Oliveira | 561760 |
| Gabriel Costa Solano | 562325 |
| Kaiky Pereira Rodrigues Da Silva | 564578 |

## Arquitetura e Requisitos Atendidos

1. **API REST do Domínio (20 pts):** 5 entidades JPA relacionadas (`CasaMonitorada`, `RegraConfiguracao`, `Alerta`, `EtapaAlerta`, `ConversaCheckin`), migração e seed inicial com Flyway, CRUD e paginação (`Page<T>`).
2. **Validação e Tratamento de Erros (10 pts):** Bean Validation (`@Valid`, `@NotBlank`, `@NotNull`) nos DTOs e `@RestControllerAdvice` com retorno padronizado `{"codigo": "...", "mensagem": "..."}`.
3. **Segurança com Spring Security + JWT (20 pts):** Autenticação stateless via JWT HS256 (`JWT_SECRET`), com papéis `IDOSO`, `CUIDADOR` (mapeado também de `FAMILIAR` e `GUARDIAO`) e `ADMIN`.
4. **Spring AI Integrado ao Fluxo (20 pts):** Classificação automática da resposta em texto livre do idoso no check-in (`ESTOU_BEM`, `PRECISO_AJUDA` ou `INCERTO`) e geração de resposta acolhedora em português, com fallback determinístico por palavras-chave.
5. **Arquitetura Distribuída — Opção A: RabbitMQ (20 pts):** Publicação e consumo assíncrono de eventos de sensores (`zelo.sensor.eventos`) e relógio simulado (`zelo.sensor.relogio`) no exchange `zelo` (e suporte ao `amq.topic` do simulador MQTT).
6. **Documentação e Integração (10 pts):** Swagger/OpenAPI acessível em `/swagger-ui.html` e integração com o aplicativo mobile `zelo-app`.

## Como Executar

1. Copie `.env.example` para `.env` (opcional para execução local com valores padrão de desenvolvimento).
2. Suba o RabbitMQ via Docker Compose:
   ```bash
   docker compose up -d
   ```
3. Inicie a aplicação com o Maven Wrapper:
   ```bash
   ./mvnw spring-boot:run
   ```
4. Acesse a documentação interativa (Swagger UI):
   - `http://localhost:8080/swagger-ui.html`
