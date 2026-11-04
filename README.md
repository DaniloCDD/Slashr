# Slashr

O **Slashr** é um bot no Telegram que avisa quando um jogo entra em promoção na Steam.
Você pode acompanhar um jogo e, de tempos em tempos, o bot consulta os preços no IsThereAnyDeal (ITAD). Se o jogo estiver em promoção,
ele manda uma mensagem marcando a pessoa e dizendo se vale a pena comprar ou não.

![Demonstração do Slashr (animação ilustrativa)](/docs/slashr-demo.gif)

<sub>Animação ilustrativa do fluxo do bot, feita com apoio de IA. As mensagens são as que o bot realmente envia; os usuários e os preços são exemplos.</sub>


## Comandos

| Comando | O que faz |
|---|---|
| `/start` | Mostra a apresentação do bot e a lista de comandos |
| `/add nome do jogo` | Busca o jogo e mostra botões para escolher qual acompanhar |
| `/list` | Mostra os jogos que você acompanha |
| `/remove [nome]` | Mostra botões para escolher qual jogo deixar de acompanhar. Com o nome, filtra a lista |

Toda lista de botões tem um botão **Cancelar**, e só quem pediu pode clicar nos botões.

O bot confere os preços **de hora em hora**. Quando um jogo entra em promoção, ele compara o preço atual com a faixa entre o menor preço histórico e o preço cheio, e dá um veredito:

- 🟢 **Compre**: até 10% da faixa acima do menor preço histórico
- 🟡 **Razoável**: entre 10% e 30% da faixa
- 🔴 **Espere**: mais de 30% da faixa

Exemplo: um jogo de R$ 120,00 com menor histórico de R$ 5,99 tem uma faixa de R$ 114,01. Em promoção a R$ 11,99, ele está a cerca de 5% da faixa, então o veredito é **Compre**.


## Tecnologias

- Java 21
- Spring Boot 3.5
- Spring Data JPA + Hibernate
- PostgreSQL
- Flyway (migrações do banco)
- Lombok
- JUnit 5, Mockito e H2 (testes)
- Docker e Docker Compose
- API do Telegram e API do IsThereAnyDeal


## Como rodar

**1. Pré-requisitos**
- Um bot criado no [@BotFather](https://t.me/BotFather) e uma chave da API do [IsThereAnyDeal](https://isthereanydeal.com/apps/my/)
- **Para rodar com Docker:** Docker e Docker Compose
- **Para rodar sem Docker:** Java 21, Maven e PostgreSQL

**2. Arquivo `.env`**

Copie o `.env.example` para um arquivo `.env` na raiz do projeto (ao lado do `pom.xml`) e preencha:
```properties
TELEGRAM_BOT_TOKEN=seu_token_do_botfather
TELEGRAM_BOT_ALLOWED_CHAT_ID=id_do_grupo
ITAD_API_KEY=sua_chave_do_itad
BD_URL=jdbc:postgresql://localhost:5432/slashr
BD_USERNAME=slashr
BD_PASSWORD=slashr
```
O `.env` está no `.gitignore`, então os seus tokens não vão para o GitHub.

O usuário e a senha `slashr` servem só para testar no seu computador. Em produção, use uma senha forte.

**Como achar o Id do grupo:** o bot só responde no grupo cujo Id está em `TELEGRAM_BOT_ALLOWED_CHAT_ID`.
1. Com o bot **parado**, adicione-o ao grupo e mande uma mensagem qualquer, como `/start`.
2. Abra no navegador `https://api.telegram.org/bot<TOKEN>/getUpdates` (troque `<TOKEN>` pelo seu token).
3. O Id do grupo é o número em `"chat":{"id": ...}`, com o sinal de menos.

Se o bot estiver rodando, essa página volta vazia, porque o bot já consumiu as mensagens. Se o grupo virar supergrupo, o Id muda e passa a começar com `-100`.

**3a. Rodar com Docker (bot + banco)**
```bash
docker compose up -d --build
```
Isso sobe o PostgreSQL e o bot juntos. O bot só liga depois que o banco estiver pronto, e os dois reiniciam sozinhos se caírem.
Dentro do Docker, o bot se conecta no banco pelo endereço `db`, então não precisa mudar o `BD_URL`.

Para ver os logs do bot:
```bash
docker compose logs -f app
```

**3b. Rodar sem Docker (pelo IntelliJ)**

Crie o banco no PostgreSQL (pelo psql):
```sql
CREATE USER slashr WITH PASSWORD 'slashr';
CREATE DATABASE slashr OWNER slashr;
```
Depois rode a classe `SlashrApplication` pelo IntelliJ, ou pelo terminal:
```bash
mvn spring-boot:run
```
As tabelas são criadas sozinhas pelo Flyway na primeira execução.

**4. Rodar os testes**
```bash
mvn test
```


## Explicação Pasta a Pasta

- SlashrApplication: Ponto de entrada do projeto. Liga o Spring Boot e ativa a verificação de preços de hora em hora.

**Bot**
- TelegramBot: É o responsável por receber as mensagens do usuário e responder aos comandos e aos cliques nos botões.
- TecladoEscolhaJogo: Monta os botões de escolha do jogo ao usuário (no /add e no /remove), sempre com um botão "Cancelar" no final.
- CallbackAdd: Define e valida o texto escondido dentro de cada botão do /add, ou seja, envia a escolha do usuário.
- CallbackRemove: Igual ao CallbackAdd, mas para os botões do /remove.
- CallbackCancelar: Define e valida o texto escondido no botão "Cancelar".
- CacheBuscas: Guarda na memória, durante 10 minutos, os resultados da busca.

**Collector**
- PriceCollector: Contrato da fonte de preços (busca de jogos, preços atuais e menor preço histórico).
- ItadPriceCollector: Faz as chamadas para a API da ITAD.
- GameMatch: Resultado da busca do Id e nome do jogo.
- Offer: Guarda os dados de uma oferta: preço atual, preço normal, desconto, moeda e link.
- ItadException: É o erro padrão quando algo dá errado ao falar com o ITAD (ex.: limite de requisições excedido).

**Model**
- Game: Representa a tabela `games` do banco, com o Id do jogo no ITAD e o nome.
- PriceAlert: Representa a tabela `price_alerts`, ou seja, quem acompanha qual jogo e em qual chat. Também guarda se a promoção já foi avisada e o último preço avisado.

**Repository**
- GameRepository: Busca e salva os jogos no banco.
- PriceAlertRepository: Busca, salva e remove os alertas no banco (usado pelo /add, /list, /remove e pela verificação de preços).

**Scheduler**
- PrecoScheduler: É a "agenda" do bot, roda a verificação de preços de hora em hora.

**Service**
- VerificadorPrecos: É o cérebro do bot. A cada hora busca os preços de todos os jogos acompanhados e decide quem deve ser avisado.
- DecisaoAlerta: Decide o que fazer com cada alerta: avisar, encerrar a promoção ou não fazer nada (evita mandar a mesma promoção toda hora).
- - CalculadoraVeredito: Compara o preço atual com a faixa entre o menor preço histórico e o preço cheio e escolhe o veredito.
- Veredito: As três opções de veredito: Compre, Razoável e Espere.
- MensagemPromocao: Monta o texto da mensagem de promoção enviada no grupo.
- AlertaService: Cria, lista e remove os alertas de cada pessoa.
- TelegramService: Faz as chamadas para a API do Telegram (receber mensagens, enviar mensagens e editar mensagens).

**Resources**
- application.properties: Configurações do projeto. Os valores secretos vêm do arquivo `.env`.
- db/migration/V1__criar_tabelas.sql: Cria as tabelas do banco. É executado sozinho pelo Flyway na primeira vez que o bot liga.

**Test**
- Cada pasta do código tem a sua pasta de testes (`bot`, `collector`, `repository`, `scheduler` e `service`), com um teste para cada classe principal.
- Os testes usam JUnit 5, Mockito (para simular o Telegram, o ITAD e o banco) e AssertJ.
- Os testes dos repositories usam o H2, um banco em memória, então não precisam do PostgreSQL ligado.

**Raiz do projeto**
- Dockerfile: Compila o projeto e gera a imagem do bot para rodar no Docker.
- docker-compose.yml: Sobe o PostgreSQL e o bot juntos com um único comando.
- .env.example: Modelo do arquivo `.env`, com as variáveis que o bot precisa.


## Dados

Os preços são fornecidos pela API do [IsThereAnyDeal](https://isthereanydeal.com/).
