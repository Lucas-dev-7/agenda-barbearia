# Rascunho para LinkedIn

Desenvolvi o **Agenda sem Furo**, uma aplicação para organizar a agenda de uma barbearia — do cadastro de serviços à reserva de horários.

No painel, o administrador cadastra barbeiros e serviços, configura o expediente semanal, consulta horários disponíveis e cria ou cancela agendamentos.

O projeto foi construído com **Java 21 e Spring Boot** no back-end, **Angular e TypeScript** no front-end, **MySQL** para persistência e **RabbitMQ** para eventos de confirmação. Também implementei login administrativo com sessão, BCrypt e proteção CSRF, além da documentação da API com OpenAPI/Swagger.

Um dos desafios foi impedir reservas sobrepostas mesmo quando duas solicitações tentam ocupar horários ao mesmo tempo. A API serializa as reservas do mesmo profissional durante essa verificação. A confirmação via RabbitMQ é publicada depois que a transação do banco é concluída.

Validei localmente o fluxo de expediente, consulta de disponibilidade, reserva, conflito e cancelamento com MySQL e RabbitMQ. O projeto está organizado como um MVP de estudo; ainda não tem autoagendamento público nem deploy.

Repositório: **[adicione aqui o link público do GitHub]**

#Java #SpringBoot #Angular #MySQL #RabbitMQ #DesenvolvimentoDeSoftware #Portfólio
