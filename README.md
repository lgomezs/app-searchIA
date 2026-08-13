# Implementación de un RAG corporativo con Quarkus, Azure AI Search y Azure OpenAI

## 1. Objetivo

Construir la base de un portal de asistencia técnica para desarrolladores de la empresa. El programador podrá preguntar sobre el framework técnico corporativo, estándares de arquitectura, buenas prácticas, convenciones y documentación técnica.

La solución utiliza RAG (Retrieval-Augmented Generation).

```text
Pregunta del desarrollador
        |
        v
      Quarkus
        |
        v
 Azure AI Search
        |
        v
    Contexto técnico
        |
        v
 Azure OpenAI / GPT-5-mini
        |
        v
 Respuesta basada en documentación
```

La IA no se entrena nuevamente con cada documento. La documentación se indexa y, ante cada pregunta, se recuperan los fragmentos relevantes y se entregan al modelo como contexto.

## 2. Recursos utilizados

### Azure Blob Storage

Se utiliza como almacenamiento de los documentos originales. Se cargó `arquitectura-hexagonal.adoc`, que contiene una guía técnica para construir arquitectura hexagonal.

Blob Storage es el repositorio de documentos; no genera respuestas.

### Azure AI Search

Se utiliza para indexar y recuperar información. No es Azure OpenAI y no genera la respuesta final. Su función es encontrar los fragmentos de documentación más relevantes para una pregunta.

### Azure OpenAI

Genera la respuesta final utilizando la pregunta y el contexto recuperado. Deployment: `gpt-5-mini-1`.

### Quarkus

Es el microservicio que coordina todo: recibe la pregunta, consulta Search, recupera chunks, construye el contexto, llama a Azure OpenAI y devuelve la respuesta.

## 3. Documento AsciiDoc creado

Se creó `arquitectura-hexagonal.adoc` con contenido sobre principios de arquitectura hexagonal, separación dominio/infraestructura, organización de paquetes, Input Ports, Output Ports, Input Adapters, Output Adapters, responsabilidades y ejemplos. El archivo se cargó en Azure Blob Storage.

## 4. Azure AI Search e indexación

El flujo es:

```text
arquitectura-hexagonal.adoc
          |
          v
      Blob Storage
          |
          v
      Indexación
          |
          v
       chunks
          |
          v
     vectorización
          |
          v
      text_vector
          |
          v
    Azure AI Search
```

El documento se divide en fragmentos o chunks. Cada chunk puede tener identificador, documento padre, texto, título y vector.

## 5. Índice utilizado

Campos configurados:

```text
chunk_id       String
parent_id      String
chunk          String
title          String
text_vector    Collection(Edm.Single)
```

`chunk_id` identifica el fragmento. `parent_id` relaciona el fragmento con su documento padre. `chunk` contiene el texto que luego se usa como contexto. `title` contiene el título. `text_vector` contiene la representación vectorial del contenido y permite búsqueda vectorial.

## 6. Vectorización y búsqueda semántica

Un texto puede convertirse en un embedding o vector. La pregunta también puede vectorizarse y compararse con los vectores almacenados en `text_vector`. Esto permite encontrar contenido relacionado semánticamente aunque las palabras exactas no coincidan.

La implementación utiliza búsqueda híbrida: búsqueda textual y búsqueda vectorial.

```java
.setSearchText(question)
```

y:

```java
VectorizableTextQuery vectorQuery =
        new VectorizableTextQuery(question)
                .setKNearestNeighbors(5)
                .setFields("text_vector");
```

Conceptualmente:

```text
Pregunta
   |
   +--> búsqueda textual
   |
   +--> búsqueda vectorial
           |
           v
    Azure AI Search
           |
           v
    resultados relevantes
```

## 7. Dependencia Maven

Se utilizó `azure-search-documents` versión `12.0.0`:

```xml
<dependency>
    <groupId>com.azure</groupId>
    <artifactId>azure-search-documents</artifactId>
    <version>12.0.0</version>
</dependency>
```

Se validó la API de esta versión. La implementación utiliza `SearchClient`, `SearchOptions`, `SearchResult`, `VectorizableTextQuery` y `SearchPagedIterable`. No se utiliza `VectorSearchOptions` ni `SearchDocument` en la implementación final.

## 8. Configuración de Azure AI Search

En `application.properties`:

```properties
azure.search.endpoint=https://TU-SEARCH-SERVICE.search.windows.net
azure.search.index-name=TU_INDICE
azure.search.api-key=${AZURE_SEARCH_API_KEY}
```

La API key debe pertenecer al mismo recurso de Azure AI Search que contiene el índice. No debe confundirse con la clave de Azure OpenAI, Storage u otros recursos. Las claves reales deben mantenerse fuera de Git.

## 9. Creación del SearchClient

Se creó un productor CDI:

```java
@ApplicationScoped
public class AzureSearchConfig {

    @ConfigProperty(name = "azure.search.endpoint")
    String endpoint;

    @ConfigProperty(name = "azure.search.index-name")
    String indexName;

    @ConfigProperty(name = "azure.search.api-key")
    String apiKey;

    @Produces
    @ApplicationScoped
    public SearchClient searchClient() {

        return new SearchClientBuilder()
                .endpoint(this.endpoint)
                .indexName(this.indexName)
                .credential(new AzureKeyCredential(this.apiKey))
                .buildClient();
    }
}
```

Esto permite inyectar `SearchClient` en los servicios Quarkus.

## 10. AzureSearchService

El servicio consulta Azure AI Search. Implementación base:

```java
@ApplicationScoped
public class AzureSearchService {

    private final SearchClient searchClient;

    public AzureSearchService(SearchClient searchClient) {
        this.searchClient = searchClient;
    }

    public List<String> search(String question) {

        final VectorizableTextQuery vectorQuery =
                new VectorizableTextQuery(question)
                        .setKNearestNeighbors(5)
                        .setFields("text_vector");

        final SearchOptions searchOptions =
                new SearchOptions()
                        .setSearchText(question)
                        .setTop(5)
                        .setSelect(
                                "chunk_id",
                                "parent_id",
                                "chunk",
                                "title"
                        )
                        .setVectorQueries(
                                List.of(vectorQuery)
                        );

        final SearchPagedIterable results =
                this.searchClient.search(searchOptions);

        final List<String> chunks = new ArrayList<>();

        for (final SearchResult result : results) {
            final Map<String, Object> document =
                    result.getAdditionalProperties();
            final Object chunk = document.get("chunk");
            if (chunk != null && !chunk.toString().isBlank()) {
                chunks.add(chunk.toString());
            }
        }

        return chunks;
    }
}
```

El flujo es: pregunta -> VectorizableTextQuery -> SearchOptions -> SearchClient -> SearchResult -> `getAdditionalProperties()` -> campo `chunk`.

## 11. Azure OpenAI existente

La aplicación ya tenía una integración funcionando con Azure OpenAI. El adaptador es `AzureOpenAiAdapter implements OpenAIProvider` y utiliza `OpenAIClient`. El deployment es `gpt-5-mini-1`.

La interfaz es:

```java
public interface OpenAIProvider {
    String question(String question, String context);
}
```

Esto permite separar la lógica de aplicación del proveedor concreto.

## 12. Adaptación de AzureOpenAiAdapter para RAG

El método ya recibía `question` y `context`, pero inicialmente solo utilizaba `question`. Se modificó para incluir ambos en el prompt:

```java
String prompt = """
    Eres el asistente técnico de la empresa.

    Responde utilizando únicamente el CONTEXTO.
    No inventes información.
    Si el contexto no contiene la respuesta, indícalo.
    Responde de forma concisa y en Markdown.

    CONTEXTO:
    %s

    PREGUNTA:
    %s
    """.formatted(context, question);
```

Luego se utiliza:

```java
.addUserMessage(prompt)
```

La modificación fundamental fue pasar de enviar únicamente la pregunta a enviar pregunta + contexto recuperado.

## 13. RagService

Se creó una capa para coordinar Retrieval y Generation:

```java
@ApplicationScoped
public class RagService {

    private final AzureSearchService searchService;
    private final OpenAIProvider openAIProvider;

    public RagService(
            AzureSearchService searchService,
            OpenAIProvider openAIProvider) {
        this.searchService = searchService;
        this.openAIProvider = openAIProvider;
    }

    public String question(String question) {
        final List<String> chunks = this.searchService.search(question);
        final String context = String.join("\n\n---\n\n", chunks);
        return this.openAIProvider.question(question, context);
    }
}
```

Este servicio representa el corazón del RAG.

## 14. Controller y respuesta JSON

Se modificó el endpoint para devolver JSON.

```java
public record AiResponse(String answer) {}
```

Request recomendado:

```java
public record AiQuestionRequest(String question) {}
```

Controller:

```java
@POST
@Path("/question")
public AiResponse question(AiQuestionRequest request) {
    final String answer = ragService.question(request.question());
    return new AiResponse(answer);
}
```

Request:

```json
{"question":"¿Cómo debo construir una arquitectura hexagonal?"}
```

Response:

```json
{"answer":"Resumen técnico..."}
```

## 15. Problema de formato de respuesta

Inicialmente la respuesta parecía mostrar palabras pegadas. Se comprobó la salida directamente en la consola de Quarkus y Azure OpenAI estaba devolviendo correctamente los espacios y saltos de línea. Por tanto, el problema estaba en la representación visual de la respuesta. Para el portal final se recomienda renderizar Markdown correctamente.

## 16. Qué es RAG

RAG significa Retrieval-Augmented Generation, o Generación aumentada mediante recuperación.

Tiene dos etapas:

### Retrieval

```text
Pregunta
   |
   v
Azure AI Search
   |
   v
chunks relevantes
```

### Generation

```text
Pregunta + chunks
       |
       v
Azure OpenAI
       |
       v
respuesta
```

Combinadas:

```text
Pregunta
   |
   v
Azure AI Search
   |
   v
Contexto
   |
   v
Azure OpenAI
   |
   v
Respuesta
```

## 17. La IA no aprende automáticamente del documento

Una conclusión fundamental es que la IA no se entrena con el archivo `.adoc`.

No ocurre:

```text
documento -> entrenamiento del modelo
```

Ocurre:

```text
documento -> Azure AI Search -> índice
```

Y ante una pregunta:

```text
pregunta -> Azure AI Search -> chunks -> Azure OpenAI
```

Si la documentación cambia, se actualiza el proceso de indexación. No es necesario volver a entrenar GPT.

## 18. Rendimiento medido

Se agregaron mediciones de Azure AI Search, Azure OpenAI, RAG completo, cantidad de chunks y tamaño del contexto.

Una medición inicial fue:

```text
Azure AI Search: 471 ms
Azure OpenAI: 12410 ms
RAG TOTAL: 12882 ms
```

Después de optimizar el prompt:

```text
Azure AI Search: 609 ms
Chunks encontrados: 5
Context chars: 9790
OpenAI choices: 1
Azure OpenAI: 5801 ms
RAG TOTAL: 6411 ms
```

La optimización del prompt redujo notablemente la latencia de Azure OpenAI sin cambiar el modelo.

## 19. Optimización y próximos experimentos

Se está evaluando el número óptimo de chunks: 5, 3 y 2. La decisión debe considerar calidad de respuesta, precisión de recuperación, latencia y tamaño del contexto. No conviene reducir contexto si se pierde información relevante.

También conviene medir tokens de entrada y salida y evaluar posteriormente límites de generación, caching, configuración del deployment y rendimiento de Search.

## 20. Arquitectura hexagonal aplicada al RAG

La solución también aplica principios de arquitectura hexagonal:

```text
                 APPLICATION
                     |
                RagService
                     |
          +----------+----------+
          |                     |
          v                     v
  OpenAIProvider        AzureSearchService
     (Port)                  |
          |                   |
          v                   v
AzureOpenAiAdapter      Azure AI Search
```

`OpenAIProvider` es una abstracción/puerto y `AzureOpenAiAdapter` es el adaptador concreto. Esto permite cambiar el proveedor externo sin cambiar la lógica principal.

## 21. Qué se ha conseguido

```text
[OK] Azure OpenAI
[OK] Deployment gpt-5-mini-1
[OK] Quarkus conectado a Azure OpenAI
[OK] Azure Blob Storage
[OK] arquitectura-hexagonal.adoc
[OK] Azure AI Search
[OK] Indexación
[OK] Chunks
[OK] Vectorización
[OK] text_vector
[OK] Búsqueda vectorial
[OK] Búsqueda híbrida
[OK] Quarkus -> Azure AI Search
[OK] Recuperación de chunks
[OK] Construcción de contexto
[OK] Contexto -> Azure OpenAI
[OK] Respuesta basada en documentación
[OK] Endpoint JSON
[OK] Medición de rendimiento
[OK] Optimización inicial del prompt
```

Resultado: **RAG funcional de extremo a extremo**.

### Frontend

 Repo: https://github.com/lgomezs/spa-searchIA

## 22. Qué falta para el portal corporativo

### Markdown
Renderizar títulos, listas, código Java, tablas y bloques de código.

### Seguridad
En producción, evolucionar de API Keys hacia Managed Identity + Azure RBAC.

### Observabilidad
Medir latencia de Search, latencia de OpenAI, latencia total, número de chunks, tamaño del contexto, tokens de entrada/salida, errores y preguntas sin información suficiente.

### Actualización automática
Configurar la indexación para incorporar automáticamente cambios de documentación.

## 23. Arquitectura objetivo

```text
                    +-----------------------+
                    | Developer Portal      |
                    | Pregunta técnica      |
                    +-----------+-----------+
                                |
                                v
                    +-----------------------+
                    | Quarkus API           |
                    | RagService             |
                    +-----------+-----------+
                                |
                   +------------+------------+
                   |                         |
                   v                         v
          +----------------+        +----------------+
          | Azure AI       |        | Azure OpenAI   |
          | Search         |        | GPT-5-mini-1  |
          +-------+--------+        +--------+-------+
                  |                          |
                  | chunks                   |
                  +------------+-------------+
                               |
                               v
                        respuesta técnica

        +------------------------------+
        | Azure Blob Storage           |
        | documentación .adoc/.md      |
        +--------------+---------------+
                       |
                       v
                 Azure AI Search
                    Indexación
```


## 25. Estado final

**Estado actual: RAG funcional de extremo a extremo.**

```text
DOCUMENTACIÓN
      |
      v
Blob Storage
      |
      v
Azure AI Search
      |
      | recuperación
      v
Quarkus
      |
      | contexto
      v
Azure OpenAI
      |
      v
GPT-5-mini-1
      |
      v
RESPUESTA
```

La solución ya permite realizar preguntas técnicas sobre `arquitectura-hexagonal.adoc` y obtener respuestas generadas por GPT-5-mini utilizando como contexto los fragmentos recuperados desde Azure AI Search.

El siguiente nivel es convertir esta base técnica en un portal corporativo completo con frontend, Markdown, fuentes, seguridad, observabilidad, actualización automática de documentación y optimización avanzada de recuperación.
