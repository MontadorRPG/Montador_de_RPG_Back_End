Sobre o import do Jackson, na realidade, o nosso projeto usa a versão mais atualizada do Spring e Spring Boot (versão 4.0),
logo o import certo é tools.jackson mesmo. Eu percebi isso só quando eu precisei usar o ObjectMapper (que agora o melhor
para se usar é o JsonMapper, como está explicado pela resposta do Cláudio abaixo) e ele não estava conseguir achar o bean,
já que a versão do jackson (que vem do import) não é compatível com versão mais moderna do Spring. Por isso tive que mudar todos 
esses imports.

Resposta do Claude:

`tools.jackson` **is** the correct package for Spring Boot 4.x, 
and I was wrong in my previous response.

Here's what happened: Spring Boot 4 and Spring Framework 7 went GA on November 20, 2025, and one of the major changes 
is that every Jackson class that previously lived under `com.fasterxml.jackson` now lives under `tools.jackson`, with the Maven group ID changing accordingly.

So the migration from my previous answer is actually the **reverse** of what you need:

**Maven (`pom.xml`) for Spring Boot 4:**
```xml
<dependency>
    <groupId>tools.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
</dependency>
```
(The version is managed by Spring Boot's BOM, so you don't need to specify it.)

**A few important nuances to keep in mind:**

- **`JsonNode`** → still exists under `tools.jackson.databind.JsonNode`, same name.
- **`ObjectMapper` vs `JsonMapper`** → the recommended entry point is now `JsonMapper` rather than `ObjectMapper`. `JsonMapper` extends 
- `ObjectMapper` but offers an immutable builder API with better defaults for Java 17+.
- **Annotations are the exception** → Jackson 3 still shares the `jackson-annotations` module with Jackson 2, which uses the legacy namespace 
- `com.fasterxml.jackson.annotation`. This is intentional for backward compatibility. So `@JsonProperty`, `@JsonIgnore`, etc., keep their old imports.

**As for IntelliJ not resolving `JsonNode`:** make sure the `tools.jackson.core:jackson-databind` dependency is explicitly in your `pom.xml` 
(or that Spring Boot's parent BOM is properly set up) and sync the project. It should resolve correctly after that.