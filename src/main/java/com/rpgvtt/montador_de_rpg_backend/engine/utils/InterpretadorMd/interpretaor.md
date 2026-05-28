# Linguagem de Expressões JSON para Regras e Efeitos

Este documento descreve o formato JSON usado para representar expressões dinâmicas no sistema. As expressões podem ser avaliadas em diferentes contextos (atributos de uma entidade, propriedades de um sistema, estado de campanha, etc.) e permitem implementar:

- Cálculos de atributos derivados (ex: `hp_maximo = for * 5`)
- Condições para efeitos (ex: `se hp_atual < hp_maximo/2 então "ferido"`)
- Filtros em listas (ex: `itens onde quantidade > 0`)
- Interpolação de textos dinâmicos

## Estrutura Geral

Toda expressão é um objeto JSON com pelo menos um campo `"tipo"` que define seu comportamento. Os demais campos variam conforme o tipo.

---

## Tipos de Expressão

### 1. `constante` –> Valor fixo

Retorna um valor literal (número, texto, booleano ou nulo).

```json
{ "tipo": "constante", "valor": 10 }
{ "tipo": "constante", "valor": "Olá mundo" }
{ "tipo": "constante", "valor": true }
```

### 2. `caminho` -> Acesso a dados de um contexto

Busca um valor no contexto (ex: atributos da entidade) usando um caminho com notação de ponto.

- Suporta objetos aninhados: "hp.atual"
- Suporta arrays por índice: "inventario.0.nome"

```json
{ "tipo": "caminho", "caminho": "for" }
{ "tipo": "caminho", "caminho": "hp.maximo", "padrao": 10 }
```

### 3. `formula` -> Operação aritmética

Aplica um operador a duas sub‑expressões. Operadores: +, -, *, /, %, ^ (potência).

```json
{
  "tipo": "formula",
  "operandoA": { "tipo": "caminho", "caminho": "for" },
  "operador": "*",
  "operandoB": { "tipo": "constante", "valor": 5 }
}
```

### 4. `formula_raw` -> Expressão matemática em texto

Avalia uma expressão matemática fornecida como string. Use com cuidado – apenas para casos simples ou valores pré‑validados.

```json
{ "tipo": "formula_raw", "expressao": "2 + 3 * 4" }
```

### 5. `condicao` -> Comparação entre dois valores

Retorna booleano. Operadores suportados:
- Números: <, >, <=, >=, ==, !=
- Texto: ==, !=

```json
{
  "tipo": "condicao",
  "operandoA": { "tipo": "caminho", "caminho": "hp.atual" },
  "operador": "<",
  "operandoB": {
    "tipo": "formula",
    "operandoA": { "tipo": "caminho", "caminho": "hp.maximo" },
    "operador": "/",
    "operandoB": { "tipo": "constante", "valor": 2 }
  }
}
```

### 6. `condicao_composta` -> Combinação lógica

Junta várias condições com "E" (AND) ou "OU" (OR).

```json
{
  "tipo": "condicao_composta",
  "operador": "E",
  "condicoes": [
    { "tipo": "condicao", "operandoA": { "tipo": "caminho", "caminho": "for" }, "operador": ">=", "operandoB": { "tipo": "constante", "valor": 10 } },
    { "tipo": "condicao", "operandoA": { "tipo": "caminho", "caminho": "hp.atual" }, "operador": ">", "operandoB": { "tipo": "constante", "valor": 0 } }
  ]
}
```

### 7. `ternario` -> Condicional em três partes

condicao? entao : senao

```json
{
  "tipo": "ternario",
  "condicao": { "tipo": "condicao", /* ... */ },
  "entao": { "tipo": "constante", "valor": "Ferido" },
  "senao": { "tipo": "constante", "valor": "Saudável" }
}
```

### 8. `filtro` -> Seleciona elementos de uma lista 

Percorre uma lista e mantém apenas os itens que satisfazem a condição. Dentro da condição, o item atual pode ser acessado pelo caminho especial "item".

```json
{
  "tipo": "filtro",
  "lista": { "tipo": "caminho", "caminho": "inventario" },
  "condicao": {
    "tipo": "condicao",
    "operandoA": { "tipo": "caminho", "caminho": "item.quantidade" },
    "operador": ">",
    "operandoB": { "tipo": "constante", "valor": 0 }
  }
}
```

### 9. `funcao` -> Operações pré‑definidas

Aplica uma função a uma lista de argumentos (cada argumento é uma sub‑expressão). 

- Funções matemáticas e lógicas

| Função  | Descrição                                                | Nº de args |
|---------|----------------------------------------------------------|------------|
| ceil    | Arredonda para cima                                      | 1          |
| floor   | Arredonda para baixo                                     | 1          |
| round   | Arredonda para o inteiro mais próximo                    | 1          |
| abs     | Valor absoluto                                           | 1          |
| min     | Menor entre dois números                                 | 2          |
| max     | Maior entre dois números                                 | 2          |
| clamp   | Limita um valor entre um mínimo e um máximo              | 3          |
| if      | Condicional funcional: (cond, se_verdadeiro, se_falso)   | 3          |


- Funções para listas

Todas as funções de lista esperam que o primeiro argumento seja uma lista (obtida via caminho com * ou via filtro). Os demais argumentos, quando existem, são expressões que podem referenciar o item atual via "item"

| Função       | Descrição                                                        | Exemplo de uso                                                                 |
|--------------|------------------------------------------------------------------|--------------------------------------------------------------------------------|
| size         | Retorna o número de elementos da lista                           | ```json { "nome": "size", "argumentos": [ { "tipo": "caminho", "caminho": "inventario.*" } ] }``` |
| sumList      | Soma os valores de um campo numérico dentro de cada item da lista | ```json{ "nome": "sumList", "argumentos": [ { "tipo": "caminho", "caminho": "itens.inventario" }, { "tipo": "caminho", "caminho": "item.preco" } ] }``` |
| averageList  | Calcula a média dos valores de um campo numérico dentro de cada item | ```json{ "nome": "averageList", "argumentos": [ lista, campo ] }``` |

**Atenção**: Para sumList e averageList, o segundo argumento é uma expressão que será avaliada para cada item, geralmente um caminho relativo a "item".
**OBS**: ainda sem um hanlde para os .* nos caminhos.

**Exemplo prático: soma dos preços de todos os itens do inventário**
```json
{
  "tipo": "funcao",
  "nome": "sumList",
  "argumentos": [
    { "tipo": "caminho", "caminho": "inventario.*" },
    { "tipo": "caminho", "caminho": "item.preco" }
  ]
}
```

**Exemplo: média de dano de armas equipadas (apenas as com quantidade > 0)**
```json
{
  "tipo": "funcao",
  "nome": "averageList",
  "argumentos": [
    {
      "tipo": "filtro",
      "lista": { "tipo": "caminho", "caminho": "armas.*" },
      "condicao": {
        "tipo": "condicao",
        "operandoA": { "tipo": "caminho", "caminho": "item.quantidade" },
        "operador": ">",
        "operandoB": { "tipo": "constante", "valor": 0 }
      }
    },
    { "tipo": "caminho", "caminho": "item.dano" }
  ]
}
```

### 10. `alvo`-> Referência a uma entidade externa

Usado para indicar um identificador (ex: ID de uma criatura, jogador, item). O valor é um texto.

```json
{
  "tipo": "alvo",
  "tipoEntidade": "criatura",
  "id": 42
}
```

### 11. `texto` -> Template com interpolação

Substitui placeholders {nome} pelos valores das variáveis correspondentes. As variáveis são expressões avaliadas no contexto atual.

```json
{
  "tipo": "texto",
  "template": "{nome} causa {dano} pontos de dano!",
  "variaveis": {
    "nome": { "tipo": "caminho", "caminho": "personagem.nome" },
    "dano": { "tipo": "formula", "operandoA": { "tipo": "constante", "valor": 2 }, "operador": "*", "operandoB": { "tipo": "caminho", "caminho": "for" } }
  }
}
```

### 12. `instrucao` -> Representa uma ação a ser executada depois

A própria expressão é guardada como uma instrução. Útil para sistemas de efeitos encadeados ou para comunicação com o front‑end.

```json
{ "tipo": "instrucao", "acao": "aplicar_efeito", "efeito_id": 123 }
```

---

## **Exemplo Completo: Condição de Vida Crítica**

```json
{
  "tipo": "ternario",
  "condicao": {
    "tipo": "condicao",
    "operandoA": { "tipo": "caminho", "caminho": "hp.atual" },
    "operador": "<=",
    "operandoB": {
      "tipo": "formula",
      "operandoA": { "tipo": "caminho", "caminho": "hp.maximo" },
      "operador": "/",
      "operandoB": { "tipo": "constante", "valor": 4 }
    }
  },
  "entao": { "tipo": "constante", "valor": "Crítico" },
  "senao": { "tipo": "constante", "valor": "Normal" }
}
```

## Como Usar no Código

A classe InterpretadorJson (Spring @Component) expõe o método:

```java
ResultadoExpressao interpretar(JsonNode expressao, Contexto contexto);
```

O Contexto é uma interface que você implementa para fornecer os valores dos caminhos. Para dados vindos de uma entidade (campo atributos como JsonNode), use a implementação pronta ContextoJsonNode.

O ResultadoExpressao possui os métodos comoNumero(), comoTexto(), comoBooleano(), getValor() e o enum getTipo().

---

**Versão da linguagem**: 1.0

**Última atualização**: maio de 2026










