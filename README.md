# Montador de RPG Back-End

API backend para um projeto semestral (matéria BRAAOOB) com o objetivo de criar um **ambiente unificado para rodar RPGs de mesa**. Este serviço não apenas fornece CRUD, mas também interpreta regras, executa procedimentos de sistemas e entrega respostas contextualizadas para o front-end.

## O que é este projeto?

O **Montador de RPG Back-End** é um servidor que:

1. **Permite criar campanhas** com qualquer sistema de RPG — D&D, Pathfinder, Mythas Bastionland, ou qualquer outro.
2. **Interpreta e processa regras** — não armazena apenas dados; o backend sabe como ler e aplicar as regras de cada sistema.
3. **Executa procedimentos de jogo** — através de um engine customizável que entende ações, testes, combates e outros eventos.
4. **Gerencia sessões em tempo real** via WebSocket STOMP, permitindo comunicação bidirecional entre mestres e jogadores.
5. **Autoriza participantes** com OAuth2 (Google, Discord) e JWT, garantindo que apenas usuários autenticados possam jogar.

### Exemplo de fluxo

- Um mestre cria uma campanha de D&D 5e e convida 4 jogadores.
- Ao iniciar a sessão, o backend carrega as regras do D&D 5e.
- Um jogador clica em "Atacar" com a espada; o backend interpreta a ação, aplica modificadores e retorna o resultado.
- Tudo acontece em tempo real através do WebSocket.

## Visão geral técnica

- **Backend focado em jogos de RPG de mesa** com suporte a múltiplos sistemas e extensível.
- **Schema-driven**: modelos, DTOs e contratos definem como dados e comportamentos fluem pela API.
- **Não é apenas CRUD**: há uma camada de interpretação e execução de procedimentos de jogo.
- **Engine configurável** para suportar diferentes sistemas de regras sem reescrita de código.

## Tecnologias

- Java 25
- Spring Boot 4.0.6
- Spring Web
- Spring Data JPA
- Spring Security + OAuth2 + JWT
- Spring WebSocket / STOMP
- PostgreSQL
- H2 console para desenvolvimento
- Lombok
- Testcontainers + JUnit para testes

## Principais responsabilidades da API

- Gerenciar campanhas e participantes
- Iniciar e encerrar sessões de jogo
- Autorizar jogadores e mestres
- Fornecer comunicação WebSocket para fluxo de jogo em tempo real
- Interpretar procedimentos de jogo e enviar contexto para o front

## Como rodar localmente

### Requisitos

- JDK 25
- Maven (opcional, o wrapper `mvnw` já está incluído)
- PostgreSQL local ou qualquer banco compatível com JDBC PostgreSQL

### Comando de execução

No Windows:

```powershell
.\\mvnw.cmd spring-boot:run
```

No Linux / macOS:

```bash
./mvnw spring-boot:run
```

### Build

```bash
./mvnw clean package
```

### Perfil

O projeto usa por padrão o perfil `dev` definido em `application.yaml`.

## Configuração

A configuração principal está em `src/main/resources/application.yaml`. Ela já inclui suporte a:

- Conexão PostgreSQL via `spring.datasource.url`
- OAuth2 para Google e Discord
- JWT
- CORS dinâmico via `app.cors.allowed-origins`

### Variáveis de ambiente úteis

- `SUPABASE_DB_URL` ou URL JDBC do Postgres
- `SUPABASE_DB_USER`
- `SUPABASE_DB_PASSWORD`
- `GOOGLE_CLIENT_ID`
- `GOOGLE_CLIENT_SECRET`
- `DISCORD_CLIENT_ID`
- `DISCORD_CLIENT_SECRET`
- `JWT_SECRET`
- `CORS_ALLOWED_ORIGINS`

## Endpoints REST principais

### Campanhas

- `POST /api/campanhas` — cria nova campanha
- `POST /api/campanhas/{campanhaId}/jogadores` — adiciona jogador à campanha
- `GET /api/campanhas` — lista campanhas
- `GET /api/campanhas/{id}` — busca campanha por ID
- `DELETE /api/campanhas/{id}` — exclui campanha

### Sessões

- `POST /api/sessoes/campanhas/{idCampanha}/iniciar` — inicia sessão de jogo
- `POST /api/sessoes/{idSessao}/encerrar` — encerra sessão
- `POST /api/sessoes/{idSessao}/entrar` — jogador entra na sessão
- `POST /api/sessoes/{idSessao}/convite` — mestre convida usuário
- `PATCH /api/sessoes/{idSessao}/instancias/{idInstancia}/atributos` — mestre altera atributos de uma instância
- `PATCH /api/sessoes/{idSessao}/meu-personagem/atributos` — jogador altera atributos do próprio personagem

## Endpoints WebSocket

O WebSocket STOMP é registrado em:

- `ws://<host>:<porta>/ws` (SockJS fallback ativado)

Mensagens enviadas pelo cliente:

- `/app/sessao/{idSessao}/acao` — envia ação de jogador ou resposta de input
- `/app/sessao/{idSessao}/declarar` — envia declaração de ação em fases de input paralelo

Tópicos de subscrição:

- `/topic/sessao/{idSessao}` — broadcast de resultados e atualizações de sessão
- `/topic/sessao/{idSessao}/status` — status de espera ou declarações pendentes
- `/user/queue/sessao/{idSessao}` — mensagens privadas para um usuário específico

## Autenticação e segurança

- A API protege a maior parte dos endpoints com JWT.
- As rotas abertas são `/auth/**`, `/oauth2/**` e `/login/**`.
- O projeto inclui login OAuth2 para Google e Discord.
- O filtro JWT foi configurado antes do filtro padrão do Spring Security.

## Observações

- O projeto foi pensado para rodar em VPS sem Docker, mas há arquivos Docker no repositório apenas como referência.
- Para produção, lembre-se de não deixar `spring.jpa.hibernate.ddl-auto=update` em ambientes críticos.
- O backend é construído para ser schema-driven e interpretativo: ele entende regras do sistema e as traduz em respostas para o front-end.

## Testes

- Execute os testes com:

```bash
./mvnw test
```

---

Se quiser, posso também adicionar seções extras como um diagrama de fluxo ou exemplos de payloads JSON para os principais endpoints.