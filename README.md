# Montador de RPG Back-End

API backend para um projeto semestral (matéria BRAAOOB) com o objetivo de criar um **ambiente unificado para rodar RPGs de mesa**. Este serviço não é apenas um CRUD — ele interpreta regras, executa procedimentos definidos pelo sistema e fornece respostas contextualizadas para o front-end, permitindo que qualquer sistema de RPG seja jogado sem alterações no código.

## 🎯 O que é este projeto?

O **Montador de RPG Back-End** é um servidor que:

1. **Permite criar campanhas** com qualquer sistema de RPG — D&D, Pathfinder, Mythic Bastionland, ou qualquer outro.
2. **Interpreta e processa regras** — não armazena apenas dados; o backend sabe como ler e aplicar as regras de cada sistema através de um **motor de procedimentos**.
3. **Executa procedimentos de jogo** — através de uma engine customizável que entende ações, testes, combates e outros eventos.
4. **Gerencia sessões em tempo real** via WebSocket STOMP, permitindo comunicação bidirecional entre mestres e jogadores.
5. **Autoriza participantes** com OAuth2 (Google, Discord) e JWT, garantindo que apenas usuários autenticados possam jogar.
6. **Modela sistemas de RPG de forma orientada a dados** — classes, itens, habilidades e monstros são definidos como **entidades** no banco de dados, e suas mecânicas são implementadas como **procedimentos** e **resoluções**.

### Exemplo de fluxo

- Um mestre cria uma campanha de Mythic Bastionland e convida jogadores.
- Ao iniciar a sessão, o backend carrega as regras do sistema.
- Um jogador clica em "Atacar" com a espada; o backend interpreta a ação, aplica modificadores, rola dados e retorna o resultado.
- Durante a criação de personagem, o backend guia o jogador por etapas de escolha de classe, rolagem de atributos e concessão de itens iniciais.
- Tudo acontece em tempo real através do WebSocket.

## 🧠 Arquitetura do sistema

### Entidades

Tudo no sistema é modelado como **entidades** (`EntidadeSistema`). Cada sistema define seu próprio schema de entidades (`schemaEntidades`) e atributos (`schemaAtributos`), permitindo total flexibilidade.

**Exemplos de entidades:**
- `jogador` — modelo base para personagens de jogadores
- `cavaleiro` — classes de personagem (ex.: Cavaleiro da Moeda, Cavaleiro Âmbar)
- `npc` — criaturas e NPCs
- `montaria` — cavalos e montarias
- `item` — armas, armaduras, escudos e objetos
- `mito` — mitos que assolam o reino
- `habilidade` — feitos e habilidades especiais

Cada entidade possui:
- **Atributos** numéricos, booleanos ou strings (ex.: VIG, CLA, SPI, GD, gloria, fatigado, pesado, longo, dado, etc.)
- **Propriedades** (JSON livre) para armazenar dados customizados como tabelas de personalização, listas de itens iniciais, habilidades, etc.

### Instâncias e personagens

Quando um personagem é criado, uma **instância** (`EntidadeInstancia`) da entidade `jogador` é gerada. Itens e habilidades concedidos por uma classe são associados como **relações** (`EntidadeRelacao`) entre instâncias.

### Procedimentos

O coração da engine é o sistema de **procedimentos**. Cada procedimento é uma sequência de **etapas** (`EtapaProcedimento`) que são executadas pelo `ProcedimentoEngine`.

**Tipos de etapa disponíveis:**
- `SOLICITAR_INPUT` — pede um valor ao usuário (texto, número, escolha entre opções)
- `SOLICITAR_ROLAGEM` — solicita que o front-end role dados e retorne o resultado
- `ALTERAR_ATRIBUTO` — modifica um atributo de uma instância
- `GERENCIAR_ITEM` — concede/remove itens ou habilidades (CRUD de relações)
- `CHAMAR_PROCEDIMENTO` — inicia um subprocedimento (suporte a pilha de execução)
- `VERIFICAR_CONDICAO` — verifica condições usando o interpretador JSON
- `DEFINIR_VARIAVEL` — define variáveis no contexto usando expressões interpretadas
- `PARA_CADA` — itera sobre instâncias ou listas, aplicando uma etapa a cada item
- `CUSTOMIZAR_ENTIDADE` — aplica tabelas de personalização (rolagens 2d6, etc.)
- `RESOLVER` — avalia resoluções (tabelas, testes, etc.)
- `SELECIONAR_ENTIDADE` — permite escolher uma entidade do sistema (ex.: classe de cavaleiro)
- `USAR_HABILIDADE` — dispara o procedimento associado a uma habilidade
- E muitos outros (Acumular Valor, Calcular, Agrupar Por, etc.)

### Resoluções

Resoluções são mecânicas de teste definidas por dados. Suportam:
- `DADO_UNICO` — rola 1 dado (ex.: d20 ≤ atributo)
- `TABELA` — consulta uma tabela baseada no resultado de um dado
- `TABELA_DUPLA` — consulta uma tabela 2×6 (ex.: tabelas de personalização)
- `POOL_SUCESSO` — conta sucessos em um pool de dados

### Contexto e interpretador

O `InterpretadorJson` avalia expressões JSON dinâmicas, permitindo que etapas de procedimento acessem atributos de instâncias, variáveis de contexto e configurações do sistema.  
O `InterpretadorContexto` expõe caminhos como:
- `instancia.VIG` — atributo da instância ativa
- `contexto.dano_ataque` — variável salva no contexto do procedimento
- `cena.rodada` — estado atual da cena (rodada, turno, etc.)

### WebSocket

A comunicação em tempo real usa STOMP sobre WebSocket, com tópicos para:
- Ações de procedimento (`/app/sessao/{id}/acao`)
- Declarações paralelas (`/app/sessao/{id}/declarar`)
- Movimentação no mapa (`/app/sessao/{id}/mover`)
- Chat da sessão (`/app/sessao/{id}/chat`)
- Atualizações de cena (`/topic/sessao/{id}/cena`)

### Cenas e combate

O modelo de **Cena** (`Cena`) substituiu o antigo conceito de Batalha, oferecendo:
- Gerenciamento de participantes com times/lados
- Estado da cena em JSON (rodada, turno atual, quem já agiu)
- Suporte a diferentes tipos de cena (Combate, Exploração, Social, Narrativa)
- Mapa em formato JSON com grid hexagonal

## 🛠️ Funcionalidades implementadas

### Criação de personagem
- Escolha do sistema e tipo de início
- Rolagem automática de atributos (VIG, CLA, SPI, GD) baseada em fórmulas configuráveis
- Definição de Glória inicial
- Escolha ou rolagem aleatória de classe (cavaleiro)
- Concessão automática de itens iniciais, habilidades e tabelas de personalização
- Suporte a criação sem campanha (temporária) ou vinculada a uma campanha existente

### Sistema de combate (Mythic Bastionland)
- Fluxo completo de ataque: declaração de alvos, agrupamento, rolagem múltipla, aplicação de dano
- Suporte a armadura, Guarda (GD), Vigor (VIG), Scars, Wounds e Mortal Wounds
- Habilidades especiais (Feats) com procedimentos dedicados
- Gambits usando dados excedentes
- Rolagem de dados no front-end com componente 3D (dados físicos)

### Gerenciamento de campanhas e sessões
- Criação de campanhas com qualquer sistema
- Convite de jogadores via JWT
- Sessões com data de início e ordem
- Cenas vinculadas a sessões

### Dashboard do usuário
- Indicadores em tempo real (campanhas ativas, cenas criadas, sessões no mês)
- Lista de amigos com status online (via WebSocket)
- Gerenciador de tarefas (preparação de sessão)
- Atividades recentes

### Compêndio de regras
- Leitura de livros de regras em formato JSON (páginas, colunas, tabelas, citações)
- Renderização de tabelas e colunas dinâmicas
- Suporte a Markdown nos textos

## 🚀 Como rodar localmente

### Requisitos

- JDK 25
- PostgreSQL local ou qualquer banco compatível com JDBC PostgreSQL

### Comando de execução

```bash
./mvnw spring-boot:run
```

### Build

```bash
./mvnw clean package
```

## 🔧 Configuração

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

## 📡 Endpoints REST principais

### Campanhas
- `POST /api/campanhas` — cria nova campanha
- `POST /api/campanhas/{id}/jogadores` — adiciona jogador
- `GET /api/campanhas` — lista campanhas do usuário
- `GET /api/campanhas/{id}` — busca campanha por ID

### Sessões
- `POST /api/sessoes/campanhas/{id}/iniciar` — inicia sessão
- `POST /api/sessoes/{id}/encerrar` — encerra sessão
- `POST /api/sessoes/{id}/entrar` — jogador entra na sessão

### Personagens
- `POST /api/personagens/completo` — cria personagem com instância
- `GET /api/personagens/usuario/{id}` — lista personagens do usuário

### Procedimentos
- `GET /api/procedimentos?sistemaId={id}` — lista procedimentos do sistema
- `POST /api/procedimentos/{id}/iniciar-com-instancia` — inicia procedimento com instância
- `POST /api/procedimentos/{id}/responder` — envia resposta de input

### Resoluções
- `CRUD /api/resolucoes` — gerencia resoluções
- `POST /api/resolucoes/{id}/executar` — avalia uma resolução com contexto

### Importação em lote
- `POST /api/importar` — importa múltiplas definições (entidades, procedimentos, resoluções) de uma vez usando aliases (`@nome`)

## 🔌 Endpoints WebSocket

O WebSocket STOMP é registrado em:

- `ws://<host>:<porta>/ws`

**Tópicos de envio (cliente → servidor):**
- `/app/sessao/{id}/acao` — envia ação de jogador ou resposta de input
- `/app/sessao/{id}/declarar` — envia declaração em fases de input paralelo
- `/app/sessao/{id}/mover` — move token no mapa
- `/app/sessao/{id}/chat` — envia mensagem no chat

**Tópicos de subscrição (servidor → cliente):**
- `/topic/sessao/{id}` — broadcast de resultados do procedimento
- `/topic/sessao/{id}/cena` — atualizações de movimentação e cena
- `/topic/sessao/{id}/chat` — mensagens do chat
- `/user/queue/sessao/{id}` — mensagens privadas para um usuário

## 🧩 Exemplo de importação de conteúdo

Você pode importar um sistema completo com classes, itens, habilidades e procedimentos usando um único JSON:

```json
{
  "definicoes": [
    { "alias": "@cavaleiro_moeda", "tipo": "entidade", "dados": { ... } },
    { "alias": "@proc_criacao", "tipo": "procedimento", "dados": { ... } },
    ...
  ]
}
```

Os aliases (ex.: `@estrela`, `@cavaleiro_moeda`) são resolvidos automaticamente, permitindo referências circulares sem precisar saber IDs do banco.

## 🧪 Testes

```bash
./mvnw test
```

## 📌 Observações

- O projeto foi pensado para rodar em VPS sem Docker, mas há arquivos Docker no repositório apenas como referência.
- Para produção, lembre-se de não deixar `spring.jpa.hibernate.ddl-auto=update` em ambientes críticos.
- O backend é construído para ser schema-driven e interpretativo: ele entende regras do sistema e as traduz em respostas para o front-end.
- Toda a lógica de jogo (ataques, habilidades, criação de personagem) é definida como **dados** (JSON no banco), sem hardcoding de regras específicas.
