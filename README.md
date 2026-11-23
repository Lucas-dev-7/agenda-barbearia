# Agenda sem Furo

API para organizar os horários de uma barbearia. O primeiro objetivo é permitir que clientes consultem disponibilidade e marquem serviços sem gerar conflito entre atendimentos do mesmo barbeiro.

## Escopo inicial

- Cadastro de barbeiros.
- Catálogo de serviços com preço e duração.
- Expediente semanal por barbeiro.
- Criação, consulta e cancelamento de agendamentos.
- Proteção contra horários sobrepostos.
- Respostas de erro consistentes e validação de entrada.

## Estrutura

```text
src/main/java/br/com/agendasemfuro/
├── barber/        # barbeiros: controller, DTOs, entity, repository e service
├── catalog/       # catálogo: controller, DTOs, entity, repository e service
├── schedule/      # expediente semanal
├── appointment/   # disponibilidade, criação e ciclo de vida dos agendamentos
├── notification/  # evento, publicação e consumo das confirmações simuladas
└── shared/        # componentes comuns, como tratamento de erros

src/main/resources/
├── application.yml
└── db/migration/  # versões do esquema MySQL via Flyway
```

Cada módulo pode começar com controller, service, repository, entidade e DTOs próprios. A regra de conflito de horário fica no fluxo de agendamento, com transação no banco; não deve depender só de uma validação no controller.

Nos módulos implementados, a organização interna segue estas responsabilidades:

- `controller/`: endpoints HTTP e conversão das requisições/respostas.
- `dto/`: contratos de entrada e saída da API.
- `entity/`: entidades persistidas pelo JPA.
- `repository/`: interfaces Spring Data para acesso a dados.
- `service/`: casos de uso e limites transacionais.

Os módulos `appointment` e `schedule` implementam o fluxo de expediente, disponibilidade e agendamento. `notification` publica a confirmação simulada depois que a transação do agendamento termina e consome a mensagem com RabbitMQ.

## API do MVP

- `GET /api/barbers` — lista barbeiros ativos.
- `POST /api/barbers` — cadastra um barbeiro. Exemplo: `{"name":"João Silva"}`.
- `GET /api/services` — lista serviços ativos.
- `POST /api/services` — cadastra um serviço. Exemplo: `{"name":"Corte","durationMinutes":30,"price":45.00}`.
- `GET /api/barbers/{barberId}/working-hours` — lista os intervalos semanais cadastrados (dia da semana ISO: 1 = segunda, 7 = domingo).
- `POST /api/barbers/{barberId}/working-hours` — cadastra um intervalo. Exemplo: `{"dayOfWeek":1,"opensAt":"09:00:00","closesAt":"18:00:00"}`. Intervalos sobrepostos no mesmo dia são rejeitados.
- `GET /api/barbers/{barberId}/availability?date=2026-11-10&serviceId={serviceId}` — retorna inícios possíveis a cada 15 minutos, considerando a duração do serviço, o expediente e os agendamentos ativos. Horários passados não são listados.
- `POST /api/appointments` — cria um agendamento. Exemplo: `{"barberId":"...","serviceId":"...","customerName":"Ana Souza","customerPhone":"11999999999","startsAt":"2026-11-17T10:00:00"}`. O início deve ser futuro, alinhado a intervalos de 15 minutos e caber no expediente.
- `GET /api/appointments/{appointmentId}` — consulta os dados e o status do agendamento.
- `PATCH /api/appointments/{appointmentId}/cancel` — cancela um agendamento ainda marcado.

Os cadastros e a criação de agendamento retornam `201 Created` e validam os campos obrigatórios. A criação confirma barbeiro e serviço ativos, expediente e ausência de sobreposição com outro agendamento ativo. Agendamentos cancelados liberam o horário.

### Documentação interativa

Com o back-end iniciado, abra `http://localhost:8080/swagger-ui.html` para explorar os endpoints e enviar requisições pela interface Swagger. A especificação OpenAPI em JSON fica em `http://localhost:8080/v3/api-docs`.

## MySQL e RabbitMQ locais com Docker

O `compose.yaml` inicia o MySQL 8.4 na porta `3307` do Windows (porta `3306` dentro do container) e o RabbitMQ 4 na porta AMQP `5672`, com a interface de gerenciamento em `15672`. As portas ficam disponíveis somente no próprio computador e o Compose exige as credenciais configuradas no `.env`. Os dois serviços têm volumes persistentes e verificações de saúde. Na raiz do projeto, copie o arquivo de exemplo, troque a senha de administrador por uma senha local própria e inicie os serviços:

```powershell
Copy-Item .env.example .env
docker compose up -d mysql rabbitmq
docker compose ps
```

Espere os dois serviços aparecerem como `healthy`. A interface do RabbitMQ fica em `http://localhost:15672`; use `RABBITMQ_USERNAME` e `RABBITMQ_PASSWORD` do `.env`. O Spring Boot carrega esse arquivo automaticamente quando é iniciado na raiz do projeto; variáveis definidas no sistema têm prioridade, o que permite configurar o deploy sem arquivo `.env`. Para iniciar o back-end, abra um terminal na raiz e rode `mvn spring-boot:run`. A migração Flyway é aplicada na inicialização; confira o estado em `http://localhost:8080/actuator/health`.

O painel usa sessão no servidor com Spring Security. Todas as rotas da API exigem autenticação, exceto `GET /api/auth/csrf`, `POST /api/auth/login` e o health check. O usuário administrador é configurado pelas variáveis `ADMIN_USERNAME` e `ADMIN_PASSWORD`; a senha é armazenada em memória apenas como hash BCrypt e precisa ter pelo menos 16 caracteres. A sessão expira após 30 minutos de inatividade, usa cookie `HttpOnly` e `SameSite=Strict`, e as operações de escrita exigem token CSRF. O login do painel acontece em `http://localhost:4200`.

Ao criar um agendamento, a API publica um evento JSON na exchange `appointments.events`, chave `appointment.created`, e o consumidor registra uma confirmação simulada no log. O envio acontece após o commit do banco para evitar mensagens de reservas que não foram gravadas. Se o broker estiver indisponível, o agendamento continua salvo e o back-end registra um aviso; esta primeira versão ainda não tem outbox nem retentativa. Desative a integração local definindo `RABBITMQ_ENABLED=false`.

Os valores de `.env.example` são credenciais apenas para desenvolvimento local. Troque-os e nunca use esses valores em produção. Em produção, configure `SESSION_COOKIE_SECURE=true`, sirva o painel e a API por HTTPS e forneça credenciais próprias para banco, RabbitMQ e administrador. A API não tem uma área pública de autoagendamento nesta etapa: o painel inteiro exige login. A criação pública de reservas e os tokens privados de consulta/cancelamento são uma etapa futura.

## Deploy em uma VM

O `compose.deploy.yaml` prepara a aplicação para uma VM Linux com Docker Compose: Caddy recebe o tráfego público e emite HTTPS, enquanto Angular, API, MySQL e RabbitMQ ficam na rede interna do Compose. Somente as portas 80 e 443 são publicadas; não exponha as portas do banco, do broker ou da API.

Antes de subir a aplicação, aponte o registro DNS `A` do domínio para o IP público da VM e libere as portas TCP 80 e 443 no firewall da nuvem. Restrinja SSH ao seu IP. Na VM, copie `deploy.env.example` para `.env.production`, configure domínio e senhas próprias, proteja o arquivo (`chmod 600 .env.production`) e execute:

```bash
docker compose --env-file .env.production -f compose.deploy.yaml config --quiet
docker compose --env-file .env.production -f compose.deploy.yaml up -d --build
```

O Caddy só consegue provisionar o certificado quando o domínio já resolve para a VM e as portas 80/443 estão acessíveis. Os dados do MySQL e do RabbitMQ ficam em volumes Docker locais; configure cópias de segurança fora da VM antes de usar dados reais. O Compose de deploy não publica a interface de administração do RabbitMQ.

Para a cota Always Free da Oracle, a documentação atual descreve até 2 OCPUs e 12 GB de memória para Ampere A1; a criação ainda depende de capacidade na região da conta. A VM precisa ser ARM64, suportada pelas imagens multi-arquitetura usadas aqui. Consulte os limites diretamente na [documentação Always Free da Oracle](https://docs.oracle.com/en-us/iaas/Content/FreeTier/freetier_topic-Always_Free_Resources.htm) antes de provisionar.

## Front-end Angular

O projeto Angular fica em `frontend/`. Ele autentica o administrador e consulta a API para listar e cadastrar barbeiros e serviços, configurar o expediente semanal, buscar horários disponíveis e criar ou cancelar agendamentos. Ao selecionar um horário, a tela solicita nome e telefone do cliente; se outra pessoa ocupar o horário antes da confirmação, a interface informa o conflito e atualiza a disponibilidade. O proxy local encaminha `/api` e `/actuator` para o back-end em `localhost:8080`.

Requisitos para executar o projeto localmente: Java 21, Maven, Docker com Compose e Node.js 20.19+, 22.12+ ou 24+. Depois de iniciar o MySQL e o RabbitMQ, rode `mvn spring-boot:run` na raiz do projeto. Em outro terminal, inicie o front-end:

```bash
cd frontend
npm install
npm start
```

Abra `http://localhost:4200`. O front-end depende da API e do MySQL estarem disponíveis.

## Testes

O back-end usa H2 em memória no perfil de teste, sem precisar de um MySQL local. A suíte de integração sobe o contexto Spring e percorre cadastro, expediente, disponibilidade, reserva, conflito e cancelamento. Nesse perfil, o Hibernate cria o esquema e o Flyway fica desativado. A migração Flyway do MySQL foi aplicada com sucesso na inicialização local do back-end; o fluxo de expediente, reserva, conflito e cancelamento também foi conferido manualmente com MySQL e RabbitMQ em execução pelo Compose.

```bash
mvn test
```

No front-end, rode os testes e o build de produção com:

```bash
cd frontend
npm test -- --watch=false
npm run build
```

## Próximas etapas

1. ~~Implementar barbeiros e catálogo de serviços.~~
2. ~~Implementar expediente e consulta de disponibilidade.~~
3. ~~Criar agendamento com validação de conflito e cancelamento.~~
4. ~~Documentar a API com OpenAPI.~~
5. ~~Publicar uma confirmação simulada com RabbitMQ.~~
6. Acrescentar lembretes agendados e resiliência de entrega, como outbox e retentativas.
7. ~~Implementar autenticação do painel com sessão, proteção CSRF e configuração por ambiente.~~
8. Preparar e validar o deploy em um ambiente gratuito, com HTTPS e credenciais próprias.

## Identidade visual

O painel usa verde floresta (`#17231f`), marfim (`#f7f7f3`) e dourado quente (`#d1a86a`) para manter o clima artesanal da barbearia com boa leitura. A foto editorial em `frontend/public/assets/barbershop-editorial.png` aparece no cartão de destaque com uma camada escura para preservar o contraste do texto. O recurso fica na pasta pública do Angular e é incluído no build de produção.
