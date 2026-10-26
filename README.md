# SaaS-Billing-Co
Repositório para a atividade de refatoração da disciplina de Implantação e entrega de Software

## Membros:
- Hugo Pereira Borges - 202403075
- Pedro Augusto Rodrigues - 202403089

## Tecnologias utilizadas

- **Java 21** — linguagem de programação.
- **Apache Maven** — gerenciamento de dependências e build.
- **JUnit 5** — execução de testes unitários.
- **GitHub Actions** — automação de compilação e testes.
- **Apache Commons Email 1.5** — suporte ao envio de e-mails.

## Pré-requisitos

Antes de executar o projeto, certifique-se de ter instalado:

- [Java Development Kit (JDK) 21](https://adoptium.net/)
- [Apache Maven](https://maven.apache.org/download.cgi)
- [Git](https://git-scm.com/), para clonar o repositório.

Verifique se o Java e o Maven estão instalados corretamente:

```bash
java -version
mvn -version
```

O Java utilizado na execução deve ser compatível com a versão 21 configurada no projeto.

## Estrutura do projeto

O código-fonte está organizado nos seguintes pacotes:

```text
src/
└── main/
    └── java/
        └── com/
            └── saas/
                └── billing/
                    ├── application/
                    ├── domain/
                    ├── infrastructure/
                    └── port/
```

### Responsabilidades dos pacotes

- **application:** contém os casos de uso e a lógica de orquestração das operações da aplicação.
- **domain:** concentra as entidades, regras de negócio e conceitos centrais do domínio.
- **infrastructure:** reúne implementações técnicas e integrações com serviços externos.
- **port:** define contratos de comunicação entre o domínio, os casos de uso e as implementações externas.

Essa divisão busca separar as regras de negócio dos detalhes técnicos, facilitando a manutenção e a evolução do sistema.

## Como executar o projeto

### 1. Clonar o repositório

```bash
git clone <URL_DO_REPOSITORIO>
```

Entre no diretório do projeto:

```bash
cd SaaS-Billing-Co
```

Substitua `<URL_DO_REPOSITORIO>` pela URL real do repositório Git.

### 2. Compilar o projeto

Execute o comando abaixo na raiz do projeto, onde está localizado o arquivo `pom.xml`:

```bash
mvn clean compile
```

Esse comando remove os artefatos de builds anteriores e compila o código-fonte principal.

### 3. Executar os testes

Para executar os testes unitários:

```bash
mvn test
```

Os testes são executados utilizando o JUnit 5.

### 4. Validar o build completo

Para compilar o projeto, executar os testes e realizar as verificações previstas nas fases do Maven, utilize:

```bash
mvn clean verify
```

Esse é o comando utilizado também no pipeline de integração contínua do projeto.

Ao final da execução, os relatórios de testes ficam normalmente disponíveis em:

```text
target/surefire-reports/
```

## Executando pelo IntelliJ IDEA

1. Abra o IntelliJ IDEA.
2. Selecione **Open** e escolha o diretório do projeto.
3. Aguarde o Maven importar as dependências definidas no `pom.xml`.
4. Configure o JDK do projeto para a versão 21.
5. Abra a janela **Maven** e execute as fases `compile`, `test` ou `verify`, conforme necessário.

Também é possível executar os comandos Maven diretamente pelo terminal integrado da IDE.

## Integração contínua (CI)

O projeto possui um workflow do GitHub Actions responsável pela compilação e execução dos testes automatizados.

O pipeline é acionado quando ocorre:

- Um `push` para a branch `main`.
- A abertura ou atualização de um `pull request` direcionado à branch `main`.
- Uma execução manual pelo GitHub Actions.

O workflow executa as seguintes etapas:

1. Baixa o código do repositório.
2. Configura o Java 21 utilizando a distribuição Temurin.
3. Configura o cache de dependências Maven.
4. Executa o comando `mvn --batch-mode --no-transfer-progress verify`.

O build é considerado bem-sucedido quando todas as etapas são concluídas sem erros.

## Dependências principais

As dependências são gerenciadas pelo Maven por meio do arquivo `pom.xml`.

| Dependência | Finalidade |
|---|---|
| Apache Commons Email 1.5 | Funcionalidades relacionadas ao envio de e-mails |
| JUnit Jupiter 5.11.4 | Testes unitários automatizados |

## Comandos úteis

| Comando | Descrição |
|---|---|
| `mvn clean` | Remove os artefatos de build anteriores |
| `mvn compile` | Compila o código-fonte principal |
| `mvn test` | Executa os testes automatizados |
| `mvn package` | Gera o artefato do projeto |
| `mvn clean verify` | Executa o ciclo de build e as verificações |
| `mvn dependency:tree` | Exibe a árvore de dependências |

## Observações

- Execute os comandos Maven na raiz do projeto, no mesmo diretório do `pom.xml`.
- O projeto está configurado para utilizar Java 21.
- A execução de `mvn verify` não inicia automaticamente um servidor ou uma aplicação. Ela realiza as etapas de build e verificação configuradas no Maven.
- Caso o projeto dependa de configurações externas, como credenciais de serviços ou parâmetros de conexão, configure-as conforme a implementação existente antes de executar os casos de uso correspondentes.
- Para executar a aplicação diretamente, é necessário que exista um ponto de entrada configurado, como uma classe com o método `public static void main(String[] args)`, ou outro mecanismo de inicialização.

## Licença

Consulte o responsável pelo repositório para obter informações sobre a licença e as condições de utilização do projeto.
