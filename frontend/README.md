# financeiro: front-end

Angular 22 + PrimeNG 22 (tema Aura esmeralda, direção visual ux-ui 2.1, ícones Lucide). Regras de código no agente
`.claude/agents/angular-frontend.md`; desenho das telas em
`docs/frontend/prototipo-primeng22.md`.

## Rodar

Pré-requisitos: Node 22.22+, 24.15+ ou 26, npm 11 e a API rodando em
`http://localhost:8080` (ver o README da raiz).

```bash
npm ci
npm start
```

Abra http://localhost:4200. O `proxy.conf.json` manda `/api` para a API, então
o navegador fala com uma origem só.

## Verificar

```bash
npm run verify   # Prettier, ESLint, Vitest com cobertura mínima de 80% e build de produção
npm run e2e      # Playwright, com a API simulada
```

Na primeira vez, instale o navegador do Playwright: `npx playwright install chromium`.
Para usar um Chromium já instalado, aponte `PLAYWRIGHT_CHROMIUM_EXECUTABLE` para ele.

## Autenticação

Segue o back-end: o `accessToken` (15 min) fica só em memória e o
`refreshToken` (7 dias) fica no `sessionStorage` da aba. Em 401 o app renova o
token uma vez e repete a chamada; se não der, volta para o login. Detalhes na
seção 9 do agente `angular-frontend`.

## Licença do PrimeNG

A chave Community do PrimeUI fica em `src/environments/environment.ts` e vence
em 14/11/2027. Renove antes em https://primeui.dev/licenses/community.
