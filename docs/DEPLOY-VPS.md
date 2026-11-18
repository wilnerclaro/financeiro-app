# Publicar o financeiro-app numa VPS

Passo a passo para colocar o app no ar num servidor próprio, com HTTPS, para a
família usar de qualquer lugar. Você executa os comandos; cada bloco diz em que
máquina ele roda:

- **No seu PC**: terminal do WSL (Ubuntu), como no README.
- **Na VPS**: terminal conectado por SSH ao servidor.

O que sobe na VPS (tudo em `deploy/compose.yaml`):

| Serviço | O que faz |
| --- | --- |
| `web` | Caddy: serve o Angular, repassa `/api/*` para a API e emite o certificado HTTPS sozinho (Let's Encrypt) |
| `api` | Spring Boot, só na rede interna do Docker |
| `postgres` | PostgreSQL 16 com volume persistente, sem porta aberta para fora |
| `backup` | `pg_dump` diário em `deploy/backups/`, guardando 14 dias |

Swagger e Actuator não ficam acessíveis pela internet: o Caddy só repassa
`/api/*`, e o Swagger está desligado na produção.

Tempo total estimado: 1 a 2 horas na primeira vez.

---

## 1. Escolher e contratar a VPS

Mínimo recomendado: **2 GB de RAM** para rodar (o app usa uns 500 MB) e
**4 GB** para compilar sem sofrimento, Ubuntu 24.04 e 40 GB de disco. Com 2 GB
dá para compilar criando swap (passo 4).

Preços conferidos em novembro de 2026; mudam com frequência, confirme no site.

| Opção | Configuração | Preço aproximado | Prós | Contras |
| --- | --- | --- | --- | --- |
| **Hostinger KVM 1 / KVM 2** (recomendada) | 1 vCPU, 4 GB, 50 GB / 2 vCPU, 8 GB, 100 GB | R$ 25 a 35/mês na promoção de 24 meses; renova a R$ 48 a 70/mês | Datacenter em São Paulo (rápido para a família), paga em real com Pix ou boleto, painel e suporte em português | Preço bom só com contrato longo pago adiantado |
| **Contabo Cloud VPS 10** | 4 vCPU, 8 GB, 75 GB | € 4,50 a 5,50/mês (uns R$ 30 a 40, mais IOF do cartão) | Muito hardware pelo preço, mensal | Datacenters na Europa e EUA (mais latência), cobra em euro, suporte fraco |
| **Oracle Cloud Always Free** | ARM, até 2 OCPU e 12 GB | Grátis | Custo zero, tem região em São Paulo | Cadastro difícil e pede cartão, falta capacidade com frequência, a Oracle já cortou o limite pela metade em 2026 sem aviso. Bom para experimentar, arriscado para dado da família |

A Hetzner, opção clássica barata, não está vendendo os planos pequenos (CX23 e
CAX11) desde outubro de 2026 e só tem esses planos na Europa.

**Na contratação (Hostinger, por exemplo):**

1. Escolha o plano, a localização **Brasil** e o sistema **Ubuntu 24.04**
   (o "simples", sem painel; o template "Ubuntu 24.04 com Docker" também serve).
2. Quando pedir a forma de acesso, cadastre a **chave SSH** (passo 3) em vez de
   senha. Se o painel só aceitar senha, use uma forte; vamos desligar o login
   por senha depois.
3. Anote o **IP público** do servidor (algo como `203.0.113.10`).

Qualquer provedor serve: o roteiro abaixo só depende de Ubuntu com acesso SSH.

---

## 2. Conseguir um endereço (domínio)

O HTTPS e o "Entrar com Google" precisam de um nome, não de um IP.

**Grátis: DuckDNS**

1. Entre em <https://www.duckdns.org> com sua conta Google ou GitHub.
2. Crie um subdomínio, por exemplo `financeiro-familia` →
   `financeiro-familia.duckdns.org`.
3. No campo **current ip**, coloque o IP da VPS e clique em **update ip**.
   O IP da VPS é fixo, então isso é feito uma vez só.

**Pago, com nome próprio:** um `.com.br` no <https://registro.br> custa uns
R$ 40/ano. Crie um registro **A** apontando `financeiro.seudominio.com.br` (ou o
domínio raiz) para o IP da VPS.

Conferir do seu PC (pode levar alguns minutos para propagar):

```bash
# No seu PC
nslookup financeiro-familia.duckdns.org   # deve responder o IP da VPS
```

---

## 3. Primeiro acesso SSH

**Criar a chave no seu PC**, se ainda não tiver uma (`ls ~/.ssh/id_ed25519.pub`):

```bash
# No seu PC
ssh-keygen -t ed25519 -C "financeiro-vps"
cat ~/.ssh/id_ed25519.pub    # esta linha é a chave pública que vai no painel
```

**Entrar como root** (troque pelo IP da sua VPS):

```bash
# No seu PC
ssh root@203.0.113.10
```

Na primeira vez ele pergunta se confia no servidor: responda `yes`.

**Criar o seu usuário** e parar de usar o root:

```bash
# Na VPS, como root
adduser wilner                    # escolha uma senha; ela vai ser pedida no sudo
usermod -aG sudo wilner
mkdir -p /home/wilner/.ssh
cp ~/.ssh/authorized_keys /home/wilner/.ssh/
chown -R wilner:wilner /home/wilner/.ssh
chmod 700 /home/wilner/.ssh && chmod 600 /home/wilner/.ssh/authorized_keys
```

Se você entrou com senha (sem chave), copie a chave do seu PC, **em outro
terminal**: `ssh-copy-id wilner@203.0.113.10`.

**Teste num terminal novo, sem fechar o do root**:

```bash
# No seu PC
ssh wilner@203.0.113.10
sudo whoami     # deve responder root
```

Só funcionou? Então **desligue o login por senha e o login do root**. Rode
`sudo -v` antes e digite a senha: assim o `sudo` não engole as linhas coladas.

```bash
# Na VPS
sudo tee /etc/ssh/sshd_config.d/00-seguranca.conf > /dev/null <<'EOF'
PasswordAuthentication no
KbdInteractiveAuthentication no
PermitRootLogin no
EOF
sudo systemctl restart ssh
sudo sshd -T | grep -E '^(passwordauthentication|permitrootlogin)'
```

A última linha precisa mostrar `passwordauthentication no` e
`permitrootlogin no`. O arquivo começa com `00-` porque no SSH vale o primeiro
valor lido, e provedores como a Hostinger deixam um `50-cloud-init.conf` que
liga a senha. Antes de fechar o terminal, confira num terminal novo que
`ssh wilner@IP` ainda entra.

Ao colar vários comandos de uma vez, rode o `adduser` sozinho: as perguntas de
senha engolem as linhas coladas depois dele.

Atalho opcional no seu PC, para digitar só `ssh financeiro`:

```bash
# No seu PC
cat >> ~/.ssh/config <<'EOF'
Host financeiro
  HostName 203.0.113.10
  User wilner
EOF
```

---

## 4. Preparar o servidor

**Atualizar o sistema e ligar o firewall** (libera só SSH, HTTP e HTTPS):

```bash
# Na VPS
sudo DEBIAN_FRONTEND=noninteractive apt-get update
sudo DEBIAN_FRONTEND=noninteractive apt-get -y -o Dpkg::Options::=--force-confold upgrade
sudo ufw allow OpenSSH
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw allow 443/udp
sudo ufw --force enable
sudo ufw status
```

O Ubuntu já instala as atualizações de segurança sozinho
(`unattended-upgrades`). Se o painel da VPS tiver firewall próprio (a Hostinger
e a Oracle têm), libere as mesmas portas lá também. Na Oracle isso é
obrigatório: crie regras de entrada para 80 e 443 na *Security List* da VCN. A
imagem Ubuntu da Oracle ainda traz regras próprias de `iptables` que bloqueiam
tudo fora o SSH; libere com:

```bash
# Na VPS, só na Oracle
sudo iptables -I INPUT 6 -p tcp -m multiport --dports 80,443 -j ACCEPT
sudo iptables -I INPUT 6 -p udp --dport 443 -j ACCEPT
sudo netfilter-persistent save
```

**Swap** (só se a VPS tiver 4 GB de RAM ou menos; evita o build travar):

```bash
# Na VPS
sudo fallocate -l 2G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
free -h      # a linha Swap deve mostrar 2,0Gi
```

**Instalar o Docker** (script oficial):

```bash
# Na VPS
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker wilner
exit
```

Entre de novo (`ssh financeiro`) para o grupo valer e confira:

```bash
# Na VPS
docker run --rm hello-world
docker compose version
```

---

## 5. Baixar o código

O código precisa estar em `develop` com a pasta `deploy/` (este roteiro).

Se o repositório for **público**:

```bash
# Na VPS
git clone -b develop https://github.com/wilnerclaro/financeiro-app.git
```

Se for **privado**, crie uma *deploy key* (só leitura, só este repositório):

```bash
# Na VPS
ssh-keygen -t ed25519 -f ~/.ssh/github_financeiro -N "" -C "vps-financeiro"
cat ~/.ssh/github_financeiro.pub
```

No GitHub: repositório → **Settings → Deploy keys → Add deploy key**, cole a
chave, deixe **Allow write access desmarcado** e salve. Depois:

```bash
# Na VPS
cat >> ~/.ssh/config <<'EOF'
Host github.com
  IdentityFile ~/.ssh/github_financeiro
EOF
git clone -b develop git@github.com:wilnerclaro/financeiro-app.git
```

---

## 6. Configurar os segredos

```bash
# Na VPS
cd ~/financeiro-app/deploy
cp .env.example .env
openssl rand -hex 24       # copie: senha do banco
openssl rand -base64 48    # copie: segredo do JWT
nano .env
```

Preencha:

| Variável | Valor |
| --- | --- |
| `DOMINIO` | o endereço do passo 2, sem `https://` (ex.: `financeiro-familia.duckdns.org`) |
| `POSTGRES_PASSWORD` | a saída do `openssl rand -hex 24` |
| `JWT_SECRET` | a saída do `openssl rand -base64 48` |
| `GOOGLE_CLIENT_ID` | o mesmo Client ID que você usa no desenvolvimento (passo 8); vazio esconde o botão do Google |
| `BACKUP_HORA` / `BACKUP_DIAS` | deixe `3` e `14`, ou ajuste |

Salve (`Ctrl+O`, `Enter`, `Ctrl+X`) e proteja o arquivo:

```bash
# Na VPS
chmod 600 .env
```

Guarde uma cópia desses valores num gerenciador de senhas. **Não troque o
`POSTGRES_PASSWORD` depois da primeira subida**: o Postgres só lê a senha na
criação do volume (veja "Problemas comuns"). Trocar o `JWT_SECRET` desloga todo
mundo, mas não perde dado.

---

## 7. Subir o app

```bash
# Na VPS
cd ~/financeiro-app/deploy
docker compose up -d --build
```

O primeiro build baixa dependências e compila o Java e o Angular: leva de 5 a 15
minutos dependendo da VPS. Depois:

```bash
# Na VPS
docker compose ps            # api deve ficar "healthy" em 1 a 2 minutos
docker compose logs -f api   # Ctrl+C para sair; procure "Started FinanceiroApiApplication"
docker compose logs web      # deve mostrar o certificado sendo obtido ("certificate obtained successfully")
```

Abra `https://financeiro-familia.duckdns.org` no navegador e no celular: deve
aparecer a tela de Entrar com o cadeado do HTTPS. Crie a sua conta primeiro e
depois passe o endereço para a família.

---

## 8. Liberar o novo endereço no Google

Sem isso o botão "Entrar com Google" falha no endereço novo.

1. Abra <https://console.cloud.google.com/apis/credentials> no mesmo projeto
   do Client ID que está no `.env`.
2. Clique no Client ID do tipo **Aplicativo da Web**.
3. Em **Origens JavaScript autorizadas**, clique em **Adicionar URI** e coloque
   `https://financeiro-familia.duckdns.org` (com `https://`, sem barra no fim).
   Mantenha o `http://localhost:4200` do desenvolvimento.
4. **URIs de redirecionamento** não precisa mexer: o app usa o botão do Google,
   que não redireciona.
5. Salve. A mudança pode levar de 5 minutos a algumas horas para valer.

**Importante:** em **Google Auth Platform → Público-alvo** (antiga "Tela de
consentimento OAuth"), se o status estiver **Em teste**, só os e-mails da lista
de **usuários de teste** conseguem entrar com Google. Ou adicione o Gmail de
cada pessoa da família lá, ou clique em **Publicar app**. Como o app só pede
nome e e-mail, a publicação não passa por verificação do Google.

---

## 9. Atualizar depois

Sempre que houver código novo em `develop`:

```bash
# Na VPS
cd ~/financeiro-app/deploy
docker compose exec backup sh /usr/local/bin/backup.sh agora   # backup antes, por garantia
git pull
docker compose up -d --build
docker image prune -f        # apaga imagens antigas para não encher o disco
```

As migrations novas do Flyway rodam sozinhas quando a API sobe. Durante o
rebuild o app fica fora do ar por 1 a 2 minutos.

---

## 10. Backups

O serviço `backup` faz um `pg_dump` por dia a partir de `BACKUP_HORA` (horário
de Brasília) em `~/financeiro-app/deploy/backups/` e apaga os arquivos com mais
de `BACKUP_DIAS` dias.

```bash
# Na VPS
ls -lh ~/financeiro-app/deploy/backups/                         # backups existentes
docker compose logs backup                                      # histórico
docker compose exec backup sh /usr/local/bin/backup.sh agora    # backup na hora
```

**Copie para fora da VPS** de vez em quando. Backup que mora só no servidor
some junto com ele:

```bash
# No seu PC (com o atalho do passo 3; sem ele, troque "financeiro" por wilner@IP)
rsync -av financeiro:financeiro-app/deploy/backups/ ~/backups-financeiro/
```

Se o provedor oferecer snapshot automático da VPS (a Hostinger tem um semanal),
deixe ligado também.

**Restaurar um backup** (apaga o banco atual e volta ao estado do arquivo):

```bash
# Na VPS
cd ~/financeiro-app/deploy
docker compose stop api
docker compose exec -T postgres sh -c 'dropdb -U "$POSTGRES_USER" "$POSTGRES_DB" && createdb -U "$POSTGRES_USER" "$POSTGRES_DB"'
docker compose exec -T postgres sh -c 'pg_restore -U "$POSTGRES_USER" -d "$POSTGRES_DB" --no-owner' < backups/financeiro-AAAA-MM-DD_HHMM.dump
docker compose start api
```

---

## Comandos do dia a dia

Todos na VPS, dentro de `~/financeiro-app/deploy`.

| Para | Comando |
| --- | --- |
| Ver o estado | `docker compose ps` |
| Log da API | `docker compose logs -f api` |
| Reiniciar tudo | `docker compose restart` |
| Parar tudo (mantém os dados) | `docker compose down` |
| Abrir o psql | `docker compose exec postgres psql -U financeiro_user -d financeiro_db` |
| Uso de memória | `docker stats --no-stream` |
| Espaço em disco | `df -h /` e `docker system df` |

Nunca rode `docker compose down -v` na VPS: o `-v` apaga o volume do banco.

---

## Problemas comuns

**O navegador diz que o certificado é inválido ou o site não abre**
Veja `docker compose logs web`. As causas comuns: o domínio ainda não aponta
para o IP (`nslookup`), as portas 80 e 443 estão fechadas no `ufw` ou no
firewall do painel, ou o `DOMINIO` do `.env` está com `https://` ou barra.
Corrija e rode `docker compose up -d`.

**O build para no meio com "Killed" ou a VPS congela**
Faltou memória. Crie o swap do passo 4 e rode o `up -d --build` de novo.

**A API reinicia sem parar com `password authentication failed`**
O `POSTGRES_PASSWORD` do `.env` mudou depois que o volume foi criado. Volte a
senha original. Se ainda não houver dado que importe, recrie o banco do zero
com `docker compose down -v && docker compose up -d` (apaga tudo).

**"Entrar com Google" mostra erro de origem (`origin_mismatch` ou
"The given origin is not allowed")**
O endereço não está nas Origens JavaScript autorizadas (passo 8), ou ainda não
propagou. Confira se está com `https://` e sem barra no fim.

**"Entrar com Google" funciona para você mas não para a família**
O app no Google está "Em teste" e o e-mail da pessoa não é usuário de teste
(passo 8).

**A API não sobe e reclama de `JWT_SECRET`**
O segredo está vazio ou tem menos de 32 caracteres no `.env`.
