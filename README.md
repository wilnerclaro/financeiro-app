# financeiro-app

Controle financeiro pessoal migrado de uma planilha Excel: contas, categorias,
lançamentos (receitas e despesas) e um resumo mensal. Hoje só existe o backend
(API REST em Spring Boot). O frontend em Angular ainda não começou.

## Pré-requisitos

| Ferramenta | Versão | Como conferir |
| --- | --- | --- |
| JDK | 25 | `java -version` |
| Docker com Compose | qualquer recente | `docker compose version` e `docker ps` |
| Git | qualquer | `git --version` |

O Maven não precisa ser instalado: o projeto usa o wrapper `./mvnw`, que baixa
a versão certa na primeira execução.

No Windows, rode tudo dentro do WSL (Ubuntu), com o projeto salvo no Linux
(`~/projetos/...`) e não em `/mnt/c/...`, porque o build fica bem mais lento
no disco do Windows.

Instalar o JDK 25 no Ubuntu 24.04:

```bash
sudo apt update
sudo apt install openjdk-25-jdk
java -version   # deve mostrar 25
```

Se tiver mais de um JDK instalado, escolha o 25 com
`sudo update-alternatives --config java`.

Docker no WSL: com o Docker Desktop, ligue em *Settings > Resources > WSL
integration* a sua distribuição Ubuntu. Sem o Docker Desktop, instale o Docker
Engine dentro do Ubuntu. Em qualquer caso, `docker ps` precisa funcionar no
terminal do WSL.

## Subir o projeto pela primeira vez

Todos os comandos partem da raiz do repositório.

**1. Criar o `.env`** com a senha do banco e o segredo dos tokens. O `.env`
não vai para o git.

```bash
cp .env.example .env
nano .env        # troque POSTGRES_PASSWORD e JWT_SECRET
```

Para gerar um `JWT_SECRET` (mínimo 32 caracteres): `openssl rand -base64 48`.
Sem ele a API não sobe e avisa no log.

**2. Subir o banco** (PostgreSQL 16, porta 5432):

```bash
docker compose up -d
docker compose ps     # espere o status "healthy"
```

**3. Subir a API:**

```bash
cd backend/financeiro-api
./mvnw spring-boot:run
```

A API lê o mesmo `.env` da raiz, então não é preciso exportar variável. Na
primeira vez o Flyway cria as tabelas (migrations V1 a V7). Quando aparecer
`Started FinanceiroApiApplication`, ela está no ar.

**4. Conferir:**

- Saúde: <http://localhost:8080/actuator/health> deve responder `{"status":"UP"}`
- Swagger (documentação e testes dos endpoints): <http://localhost:8080/swagger-ui.html>

**5. Criar o seu usuário e entrar.** Todo endpoint, fora o cadastro e o login,
exige um token. O cadastro já devolve o token:

```bash
curl -X POST localhost:8080/api/auth/cadastro \
  -H 'Content-Type: application/json' \
  -d '{"nome":"Wilner","email":"wilner@local.dev","senha":"uma-senha-forte"}'
```

Copie o `accessToken` da resposta e use no header `Authorization`:

```bash
curl -X POST localhost:8080/api/categorias \
  -H 'Authorization: Bearer <accessToken>' \
  -H 'Content-Type: application/json' \
  -d '{"nome":"Moradia","tipo":"DESPESA"}'
```

No Swagger, chame `POST /api/auth/login`, copie o `accessToken`, clique em
**Authorize** e cole. O token de acesso vale 15 minutos; para renovar sem
digitar a senha, chame `POST /api/auth/refresh` com o `refreshToken` (vale 7
dias).

## Testes e build

```bash
cd backend/financeiro-api
./mvnw verify
```

Roda o Spotless (formatação), o Checkstyle, os testes, o JaCoCo (cobertura) e o
ArchUnit (regras de arquitetura). Os testes de integração sobem um PostgreSQL
descartável pelo Testcontainers, então precisam do Docker rodando, mas não
usam o banco do `docker compose`.

Se o Spotless reclamar de formatação, corrija com `./mvnw spotless:apply`.
Detalhes em [docs/QUALIDADE-BUILD.md](docs/QUALIDADE-BUILD.md).

O mesmo `./mvnw verify` roda no GitHub Actions em todo pull request e em todo
push para `develop` e `main` ([.github/workflows/ci.yml](.github/workflows/ci.yml)).
PR com o check vermelho não deve ser mergeado.

## Comandos do dia a dia

| Para | Comando (na raiz) |
| --- | --- |
| Subir o banco | `docker compose up -d` |
| Parar o banco (mantém os dados) | `docker compose stop` |
| Ver o log do banco | `docker compose logs -f postgres` |
| Abrir o psql | `docker compose exec postgres psql -U financeiro_user -d financeiro_db` |
| Apagar o banco e começar do zero | `docker compose down -v` |
| Subir a API | `cd backend/financeiro-api && ./mvnw spring-boot:run` |

## Estrutura

```
financeiro-app/
├── compose.yaml            banco de desenvolvimento (PostgreSQL 16)
├── .env.example            modelo das variáveis do banco
├── backend/financeiro-api/ API Spring Boot (pom.xml, mvnw, src/)
├── config/checkstyle/      regras do Checkstyle
├── docs/                   auditoria e qualidade de build
├── CLAUDE.md               contexto e convenções para os agentes
└── .claude/                agentes e skill do glossário financeiro
```

A API é organizada por módulo de negócio (`categoria`, `conta`,
`lancamentofinanceiro`, `dashboardfinanceiro`), cada um com `controller`,
`service`, `repository`, `entity`, `dto` e `mapper`. Convenções em
[CLAUDE.md](CLAUDE.md).

## Problemas comuns

**`./mvnw: Permission denied`**
O wrapper perdeu a permissão de execução. Rode `chmod +x mvnw`.

**`release version 25 not supported` ou `invalid target release: 25`**
O Maven está usando um JDK mais antigo. Confira `java -version` e
`echo $JAVA_HOME`. Se precisar, aponte para o 25:
`export JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64`.

**API falha com `password authentication failed for user "financeiro_user"`**
O PostgreSQL só usa a senha do `.env` na **primeira** vez que o volume é
criado. Se você trocou a senha depois, o banco continua com a antiga. Para
desenvolvimento, apague e recrie: `docker compose down -v && docker compose up -d`
(isso apaga os dados).

**`Connection refused` na porta 5432**
O banco não está de pé (`docker compose ps`) ou outra instalação de PostgreSQL
ocupa a porta. Troque `POSTGRES_PORT` no `.env` (por exemplo, `5433`) e suba
de novo.

**`Cannot connect to the Docker daemon` no WSL**
O Docker não está rodando ou a integração com o WSL está desligada (veja
Pré-requisitos). Os testes também dependem dele.

**Maven ou git não conseguem baixar nada no WSL** (`Could not connect to server`)
É rede do WSL, não do projeto. Teste `curl -I https://github.com` no WSL e no
PowerShell. Se só o Windows responder, a causa costuma ser VPN ou DNS do WSL.
Com VPN, `networkingMode=mirrored` no `%UserProfile%\.wslconfig` costuma
resolver.

**Porta 8080 ocupada**
Suba a API em outra porta: `./mvnw spring-boot:run -Dspring-boot.run.arguments=--server.port=8081`.
