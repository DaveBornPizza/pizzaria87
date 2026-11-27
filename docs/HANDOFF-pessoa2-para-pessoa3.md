# Handoff — Pessoa 2 (Fluxo do Salão / Garçom) → Pessoa 3 (Cozinha, Notificações e Documentação)

Branch: `feature/fluxo-garcom` (criada a partir de `master`, que contém a fase da Pessoa 1).

## 1. O que foi entregue

| Caso de uso / item | Onde está | Rotas |
|---|---|---|
| Painel do garçom (grade de fichas, filtros Todas/Livres/Ocupadas/Desativadas, busca por número, consumo parcial, atualização automática a cada 30 s) | `AtendimentoController.painel`, `AtendimentoService.montarPainel`, `garcom/painel.html` | `GET /garcom/painel?filtro=…&numero=…` |
| Sino de notificações (contador de não lidas, dropdown com *Pedido Pronto* e *Falta de Ingrediente*, marcar uma/todas como lidas) | `SinoNotificacoesAdvice`, `NotificacaoGarcomController`, `NotificacaoGarcomService`, `fragments/sino-notificacoes.html` | `POST /garcom/notificacoes/{id}/lida`, `POST /garcom/notificacoes/lidas` |
| **UC005 [H7]** Abrir Ficha | `AtendimentoService.abrirFicha` | `POST /garcom/fichas/{n}/abrir` |
| Detalhe da ficha ocupada (pedidos, status, consumo parcial) | `AtendimentoService.detalharFicha`, `garcom/ficha.html` | `GET /garcom/fichas/{n}` |
| **UC002 [H5]** Registrar Pedido | `PedidoService.registrarPedido`, `garcom/novo-pedido.html` | `GET /garcom/fichas/{n}/pedidos/novo`, `POST /garcom/fichas/{n}/pedidos` |
| **UC006 [H5]** Alterar ou Cancelar Pedido | `PedidoService.alterarPedido`, `cancelarPedido`, `exigirEditavel` | `GET /garcom/pedidos/{id}/editar`, `POST /garcom/pedidos/{id}/alterar`, `POST /garcom/pedidos/{id}/cancelar` |
| **UC004 [H7]** Consultar e Fechar Ficha | `AtendimentoService.gerarExtrato`, `fecharFicha`, `liberarFichaSemPedidos`, `garcom/extrato.html` | `GET /garcom/fichas/{n}/extrato`, `POST /garcom/fichas/{n}/fechar`, `POST /garcom/fichas/{n}/liberar` |

### Arquivos novos
```
src/main/java/br/ufu/pizzaria87/
├── controller/garcom/
│   ├── AtendimentoController.java
│   ├── PedidoController.java
│   ├── NotificacaoGarcomController.java
│   └── SinoNotificacoesAdvice.java      (@ControllerAdvice só do pacote garcom)
└── service/garcom/
    ├── AtendimentoService.java
    ├── PedidoService.java
    ├── PedidoForm.java                   (DTO do formulário de itens)
    └── NotificacaoGarcomService.java
src/main/resources/
├── static/css/garcom.css
└── templates/
    ├── fragments/sino-notificacoes.html
    └── garcom/ painel.html, ficha.html, novo-pedido.html, extrato.html
```

### Arquivo existente alterado (1 linha, aditiva)
- `templates/fragments/navbar.html`: inclui o fragmento do sino **somente quando o atributo de modelo `sino` existe**, o que só acontece nas telas do pacote `controller/garcom`. As telas de admin, login e cozinha ficam idênticas.

**Nenhuma alteração** em `model/`, `repository/`, `data.sql`, `application.properties` ou nas classes da Pessoa 1.

## 2. Regras de negócio implementadas (o que vale como verdade)

- Status são **Strings** (não há enums no código, apesar do plano). Use as constantes já existentes:
  - Ficha: `FichaService.STATUS_LIVRE | STATUS_OCUPADA | STATUS_DESATIVADA` (Pessoa 1)
  - Pedido: `PedidoService.STATUS_PENDENTE | STATUS_EM_PREPARO | STATUS_PRONTO | STATUS_CANCELADO`
  - Notificação: `NotificacaoGarcomService.TIPO_PEDIDO_PRONTO | TIPO_FALTA_INGREDIENTE`
- **UC005**: só abre ficha `LIVRE`, sem atendimento aberto. Cria `Atendimento(dataAbertura=agora, funcionarioAbertura=garçom logado)` e muda a ficha para `OCUPADA`.
- **UC002**: mostra só itens `disponivel = true`, agrupados por categoria. Quantidade > 0 (0 = item não selecionado; negativa é rejeitada) e observação opcional por item (até 255 caracteres). O pedido nasce `PENDENTE`, com `funcionario` = garçom logado. O `precoUnitario` do `ItemPedido` é **congelado** com o preço do cardápio no momento do lançamento.
- **UC006**: só pedidos `PENDENTE` de atendimento ainda aberto. O bloqueio vale na tela (botões trocados por "Não editável") **e** no Service (`exigirEditavel`).
  - Na alteração, os itens existentes mantêm o preço congelado e os itens acrescentados recebem o preço atual.
  - Um item que ficou indisponível depois do lançamento pode ser mantido ou ter a quantidade reduzida, mas não aumentada.
  - O cancelamento muda o status para `CANCELADO`. O pedido não é apagado, para preservar o histórico.
- **UC004**:
  - O extrato consolida item + preço unitário e ignora pedidos `CANCELADO`.
  - Fichas **sem nenhum pedido registrado não podem ter a conta fechada** (regra da fase 2). Para não deixar uma ficha aberta por engano presa como OCUPADA, existe a ação separada **"Liberar ficha"**: ela só funciona sem pedidos e encerra o atendimento com `valorTotal = 0` (o registro é mantido).
  - Uma ficha cujos pedidos foram todos cancelados conta como "com pedidos" e fecha com total R$ 0,00.
  - Com pedidos `PENDENTE`/`EM_PREPARO`, o fechamento exige confirmação explícita (checkbox validado também no servidor).
  - O fechamento grava `dataFechamento`, `valorTotal` e `funcionarioFechamento`, e a ficha volta a `LIVRE`.

## 3. Contrato com o fluxo da cozinha (o que a Pessoa 3 precisa respeitar)

O sino do garçom **só lê** a tabela `notificacao`. Para uma notificação aparecer:

```java
new Notificacao(
    "PEDIDO_PRONTO" /* ou "FALTA_INGREDIENTE" */,
    "texto livre exibido no sino",
    LocalDateTime.now(),
    pedido,                  // obrigatório: coluna pedido_id é NOT NULL
    funcionarioDestino       // obrigatório: quem verá no sino
);
```

- Destinatário sugerido para `PEDIDO_PRONTO`: `pedido.getAtendimento().getFuncionarioAbertura()` (quem abriu a ficha) ou `pedido.getFuncionario()` (quem lançou o pedido). O plano aceita os dois.
- O sino já mostra "Pedido #id · Ficha N" a partir do `pedido`, então a mensagem não precisa repetir esses dados (pode repetir, se quiser).
- **Atenção, FALTA_INGREDIENTE "para a equipe de garçons"**: a entidade tem **um** `funcionarioDestino` e o `pedido` é obrigatório. Para avisar vários garçons, grave **uma notificação por garçom ativo**. O pedido afetado é obrigatório pelo mapeamento atual; mudar isso exige alterar o `model` e alinhar com a equipe.
- Tipos desconhecidos aparecem no sino com ícone genérico, sem quebrar nada.

### Pontos de atenção para o KDS
- O fechamento de conta pode acontecer com pedidos ainda `PENDENTE`/`EM_PREPARO`, desde que o garçom confirme, como pede o UC004. Esses pedidos **continuam** na fila da cozinha; decida se o KDS mostra ou não pedidos de atendimentos já fechados (`pedido.atendimento.dataFechamento != null`).
- O KDS deve ocultar `CANCELADO`. O garçom só cancela pedidos `PENDENTE`.
- O garçom pode alterar os itens de um pedido `PENDENTE` a qualquer momento antes de a cozinha clicar em "Iniciar preparo". O KDS deve ler os itens do banco ao renderizar.
- Se o KDS for usar os status, reaproveite as constantes de `PedidoService` para não divergir das strings.

## 4. Problemas encontrados no código existente (NÃO corrigidos — são da Pessoa 1)

1. **`data.sql` sobrescreve dados a cada inicialização.** Os `MERGE INTO ... KEY(...) VALUES (...)` atualizam as linhas existentes, então ao reiniciar a aplicação:
   - fichas 1–6 voltam ao status inicial (ex.: uma ficha `OCUPADA` volta a `LIVRE` mesmo com atendimento aberto). A ficha fica **travada**: não pode ser aberta ("já possui atendimento em aberto") nem fechada ("não está ocupada");
   - preço e disponibilidade dos 4 itens iniciais voltam aos valores do script, desfazendo edições do admin;
   - PIN/cargo/ativo dos 3 funcionários iniciais também são restaurados.
   Sugestão: só inserir quando não existir (ex.: `INSERT INTO ... SELECT ... WHERE NOT EXISTS (...)`). Confirmado em teste numa cópia do projeto.
2. O `NotificacaoRepository` não tem o método `findByFuncionarioDestinoAndLidaFalseOrderByDataHoraDesc` citado no plano. O sino usa os métodos que existem e filtra no Service.
3. `Ficha` não tem controle de concorrência (`@Version`). Dois garçons abrindo a mesma ficha no mesmo instante é um caso não tratado (aceitável para o MVP).

## 5. Roteiro de teste ponta a ponta (parte do salão)

Usuários do `data.sql`: `mike01` / `1111` (garçom), `william` / `1234` (admin), `jeremy02` / `2222` (cozinheiro).

1. Login `mike01` → cai em `/garcom/painel`. Filtros e contadores batem com as fichas.
2. Tocar numa ficha Livre → modal → "Abrir ficha" → vai para o detalhe; no painel ela aparece Ocupada.
3. Tentar abrir a ficha 5 (Desativada) ou buscar uma inexistente → mensagem de erro.
4. "Novo pedido" → a Pizza Quatro Queijos (indisponível) não aparece; enviar sem itens → erro; enviar itens com observação → pedido `PENDENTE`, consumo parcial atualizado.
5. Admin muda o preço de um item → novos pedidos usam o preço novo; os antigos mantêm o preço congelado (conferir no extrato).
6. Alterar o pedido pendente (mudar quantidade, acrescentar, zerar para remover) e cancelar outro pedido.
7. **Cozinha** (Pessoa 3): "Iniciar preparo" → no garçom o pedido fica "Não editável"; "Marcar pronto" → o **sino** mostra 1 não lida; marcar como lida zera o contador.
8. "Fechar conta" → extrato consolidado; com pedido em preparo exige o checkbox; confirmar → total registrado, ficha Livre.
9. Abrir outra ficha e, sem pedidos, ir em "Fechar conta" → fechamento bloqueado; "Liberar ficha" → volta a Livre.

Todos os passos acima, exceto o 7 (simulado alterando o status direto no banco), foram executados numa cópia do projeto em 27/11/2026, com Spring Boot 4.2.0 e JDK 25.

## 6. Para o Diagrama de Classes / Matriz de Rastreabilidade

| Método | UC / História |
|---|---|
| `AtendimentoController.painel` / `AtendimentoService.montarPainel` | Painel do garçom (H7) |
| `AtendimentoController.abrirFicha` / `AtendimentoService.abrirFicha` | UC005 / H7 |
| `AtendimentoController.detalheFicha` / `AtendimentoService.detalharFicha`, `buscarAtendimentoAberto` | UC004, UC002, UC006 (consulta) |
| `PedidoController.novoPedido`, `registrarPedido` / `PedidoService.opcoesNovoPedido`, `registrarPedido` | UC002 / H5 |
| `PedidoController.editarPedido`, `alterarPedido`, `cancelarPedido` / `PedidoService.opcoesEdicao`, `alterarPedido`, `cancelarPedido`, `exigirEditavel` | UC006 / H5 |
| `AtendimentoController.extrato`, `fecharFicha`, `liberarFicha` / `AtendimentoService.gerarExtrato`, `fecharFicha`, `liberarFichaSemPedidos` | UC004 / H7 |
| `NotificacaoGarcomController.marcarComoLida`, `marcarTodasComoLidas` / `NotificacaoGarcomService.carregarSino`, `marcarComoLida`, `marcarTodasComoLidas`; `SinoNotificacoesAdvice` | Sino (leitura de notificações, UC003 lado do garçom / H6) |
