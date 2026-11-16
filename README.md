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
├── schedule/      # expediente semanal e consulta de disponibilidade
├── appointment/   # criação e ciclo de vida dos agendamentos
├── notification/  # módulo reservado para RabbitMQ
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

Os módulos `appointment`, `schedule` e `notification` estão reservados para as próximas etapas e podem adotar a mesma organização quando forem implementados.

## API do MVP

- `GET /api/barbers` — lista barbeiros ativos.
- `POST /api/barbers` — cadastra um barbeiro. Exemplo: `{"name":"João Silva"}`.
- `GET /api/services` — lista serviços ativos.
- `POST /api/services` — cadastra um serviço. Exemplo: `{"name":"Corte","durationMinutes":30,"price":45.00}`.
- `GET /api/barbers/{barberId}/working-hours` — lista os intervalos semanais cadastrados (dia da semana ISO: 1 = segunda, 7 = domingo).
- `POST /api/barbers/{barberId}/working-hours` — cadastra um intervalo. Exemplo: `{"dayOfWeek":1,"opensAt":"09:00:00","closesAt":"18:00:00"}`. Intervalos sobrepostos no mesmo dia são rejeitados.
- `GET /api/barbers/{barberId}/availability?date=2026-11-10&serviceId={serviceId}` — retorna inícios possíveis a cada 15 minutos, considerando a duração do serviço e o expediente do barbeiro.
- `POST /api/appointments` — cria um agendamento. Exemplo: `{"barberId":"...","serviceId":"...","customerName":"Ana Souza","customerPhone":"11999999999","startsAt":"2026-11-17T10:00:00"}`. O início deve ser futuro, alinhado a intervalos de 15 minutos e caber no expediente.
- `GET /api/appointments/{appointmentId}` — consulta os dados e o status do agendamento.
- `PATCH /api/appointments/{appointmentId}/cancel` — cancela um agendamento ainda marcado.

Os cadastros e a criação de agendamento retornam `201 Created` e validam os campos obrigatórios. A criação confirma barbeiro e serviço ativos, expediente e ausência de sobreposição com outro agendamento ativo. Agendamentos cancelados liberam o horário.

## Conectar ao MySQL local

O projeto usa o MySQL instalado na máquina, em `localhost:3306`. Crie o banco antes de iniciar a aplicação:

```sql
CREATE DATABASE agenda_barbearia CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
```

No IntelliJ, abra **Run > Edit Configurations** e defina as variáveis de ambiente `DB_USERNAME` e `DB_PASSWORD` com o usuário e a senha do seu MySQL. `DB_URL` já assume `jdbc:mysql://localhost:3306/agenda_barbearia?serverTimezone=America/Sao_Paulo`; ajuste se o servidor usar outra porta ou endereço. O Flyway cria as tabelas na primeira inicialização.

O arquivo `.env.example` serve como referência; o Spring não carrega esse arquivo automaticamente. Não coloque sua senha real no Git. O endpoint `/actuator/health` fica disponível para conferir a saúde da aplicação.

## Front-end Angular

O projeto Angular fica em `frontend/`. Ele consulta a API para listar e cadastrar barbeiros e serviços, configurar o expediente semanal e buscar horários disponíveis. O proxy local encaminha `/api` e `/actuator` para o back-end em `localhost:8080`.

Requisitos: Node.js 20.19+, 22.12+ ou 24+. Em dois terminais, inicie primeiro o back-end e depois rode:

```bash
cd frontend
npm install
npm start
```

Abra `http://localhost:4200`. O front-end depende da API e do MySQL estarem disponíveis.

## Próximas etapas

1. ~~Implementar barbeiros e catálogo de serviços.~~
2. ~~Implementar expediente e consulta de disponibilidade.~~
3. ~~Criar agendamento com validação de conflito e cancelamento.~~
4. Adicionar testes automatizados e documentação OpenAPI.
5. Acrescentar RabbitMQ para confirmação e lembrete simulados.
6. Adicionar autenticação e deploy depois que o fluxo principal estiver estável.
