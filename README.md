# Patient Management

## Descrição

Este projeto é uma aplicação de microsserviços para gerenciamento de pacientes, desenvolvida com foco em arquitetura modular e comunicação eficiente entre serviços. Ele serve como um estudo prático para aprofundar conhecimentos em Java, Spring Boot, Docker, gRPC, e futuramente, AWS.

O sistema atualmente conta com dois microsserviços principais:

- **patient-service**: Responsável por criar, atualizar, deletar e consultar dados dos pacientes.
- **billing-service**: Em desenvolvimento, será responsável por criar contas de cobrança para pacientes, acionado pelo patient-service via gRPC.

## Arquitetura e Tecnologias

- **Java 21** e **Spring Boot** para desenvolvimento dos microsserviços.
- **PostgreSQL** como banco de dados para o patient-service, rodando em container Docker.
- **Docker** para conteinerização tanto do banco de dados quanto dos microsserviços, facilitando a portabilidade e o isolamento.
- **OpenAPI (Swagger)** para documentação e testes da API REST do patient-service.
- **gRPC** para comunicação eficiente entre microsserviços (em desenvolvimento).
- **Maven** para gerenciamento das dependências e build do projeto.
