# Developer AI Assistant --- RAG con Azure, Quarkus y React

![Screenshot from running application](img/architecture.png?raw=true "Screenshot")

> **Estado:** RAG funcional de extremo a extremo, con Azure Blob
> Storage, Azure AI Search, búsqueda vectorial, Hybrid Search, Semantic
> Ranker, Azure OpenAI, Quarkus y React.

------------------------------------------------------------------------

## 1. Objetivo del proyecto

Construir un **Developer AI Assistant** capaz de responder preguntas
técnicas sobre documentación corporativa utilizando un patrón
**Retrieval-Augmented Generation (RAG)**.

El flujo implementado es:

``` text
                    ┌─────────────────────────────┐
                    │       Frontend React        │
                    │ Developer AI Assistant      │
                    └──────────────┬──────────────┘
                                   │ HTTP/JSON
                                   ▼
                    ┌─────────────────────────────┐
                    │       Quarkus Backend       │
                    │          REST API            │
                    └──────────────┬──────────────┘
                                   │
                                   ▼
                    ┌─────────────────────────────┐
                    │      Azure AI Search        │
                    │                             │
                    │ Keyword + Vector Search     │
                    │ Hybrid Search + RRF         │
                    │ Semantic Ranker             │
                    └──────────────┬──────────────┘
                                   │
                             top chunks
                                   │
                                   ▼
                    ┌─────────────────────────────┐
                    │       Azure OpenAI          │
                    │        gpt-5-mini-1         │
                    └──────────────┬──────────────┘
                                   │
                                   ▼
                    ┌─────────────────────────────┐
                    │       Frontend React        │
                    │ Markdown + código + fuentes │
                    └─────────────────────────────┘


Documentación .adoc
       │
       ▼
Azure Blob Storage
       │
       ▼
Azure AI Search
       │
       ├── extracción / chunking
       ├── indexación
       └── embeddings
```

------------------------------------------------------------------------

# 2. ¿Qué se ha conseguido?

El proyecto ya permite:

-   almacenar documentación `.adoc` en Azure Blob Storage;
-   indexarla en Azure AI Search;
-   almacenar texto y vectores;
-   realizar búsqueda por palabras clave;
-   realizar búsqueda vectorial;
-   utilizar **Hybrid Search**;
-   utilizar **Semantic Ranker**;
-   recuperar los chunks más relevantes;
-   enviar esos chunks como contexto a Azure OpenAI;
-   generar respuestas basadas en la documentación;
-   devolver fuentes de la respuesta;
-   devolver métricas de ejecución;
-   consumir todo mediante un backend Quarkus;
-   mostrar las respuestas en un frontend React;
-   renderizar Markdown y bloques de código;
-   copiar código desde el frontend.

------------------------------------------------------------------------

# 3. Recursos de Azure utilizados

  -----------------------------------------------------------------------
Recurso                             Uso
  ----------------------------------- -----------------------------------
 - Azure Blob Storage                  Almacenar los documentos fuente

 - Azure AI Search                     Indexar y recuperar información

 - Configuración semántica de AI       Definir campos para Semantic Ranker
Search

 - Vectorización / embeddings          Convertir texto en vectores

 - Azure OpenAI / Microsoft Foundry    Modelos de IA

`text-embedding-3-small`            Embeddings

`gpt-5-mini-1`                      Generación de respuestas
-----------------------------------------------------------------------

------------------------------------------------------------------------

# 4. Azure Blob Storage

## ¿Qué es?

Azure Blob Storage es almacenamiento de objetos.

En este proyecto se utiliza como repositorio de los documentos
originales.

Ejemplo:

``` text
Azure Blob Storage
└── technical-documentation/
    └── Guía de Arquitectura Hexagonal.adoc
```

## ¿Para qué sirve?

Permite separar:

``` text
Documento original
       ↓
Blob Storage
       ↓
Azure AI Search
       ↓
Índice
```

El documento original permanece almacenado en Blob Storage mientras
Azure AI Search mantiene una representación optimizada para búsqueda.

------------------------------------------------------------------------

# 5. Azure AI Search

## ¿Qué es?

Azure AI Search es el motor de recuperación utilizado por el RAG.

Se encarga de:

-   indexar documentos;
-   almacenar chunks;
-   almacenar embeddings;
-   buscar por palabras;
-   buscar por vectores;
-   combinar resultados;
-   aplicar Semantic Ranker.

Es importante diferenciarlo de Azure OpenAI:

``` text
Azure AI Search
    = encuentra información

Azure OpenAI
    = genera la respuesta
```

------------------------------------------------------------------------

# 6. Índice de Azure AI Search

El índice utilizado tiene estos campos:

Campo           Tipo               Función
  --------------- ------------------ -----------------------------------
`chunk_id`      String             Identificador del chunk
`parent_id`     String             Identificador del documento padre
`chunk`         String             Contenido textual
`title`         String             Título/documento
`text_vector`   SingleCollection   Vector/embedding

Conceptualmente:

``` text
Documento
   │
   ├── chunk 1 → vector 1
   ├── chunk 2 → vector 2
   ├── chunk 3 → vector 3
   └── chunk N → vector N
```

------------------------------------------------------------------------

# 7. Vectorización

La vectorización permite transformar texto en representaciones
numéricas.

En el proyecto se utilizó:

``` text
text-embedding-3-small
```

Conceptualmente:

``` text
Pregunta
   ↓
Embedding
   ↓
Vector
   ↓
Comparación con text_vector
   ↓
Chunks similares
```

Esto permite recuperar información por significado y no solamente por
coincidencia literal de palabras.

------------------------------------------------------------------------

# 8. Importación de datos RAG

En Azure AI Search se utilizó la opción:

``` text
Importación de datos
        ↓
RAG
```

La documentación se obtiene desde Blob Storage y se prepara para
búsqueda.

El flujo es:

``` text
Blob Storage
      ↓
Extracción
      ↓
Chunking
      ↓
Vectorización
      ↓
Azure AI Search
```

------------------------------------------------------------------------

# 9. Vectorizar y enriquecer imágenes

En la primera implementación no se seleccionó:

``` text
Vectorizar y enriquecer las imágenes
```

La documentación utilizada es principalmente textual y contiene código.

Para una futura evolución, si existen diagramas o imágenes relevantes,
se puede incorporar procesamiento multimodal.

------------------------------------------------------------------------

# 10. Programación de indexación

La programación de indexación permite actualizar periódicamente el
índice cuando cambia la documentación.

Para un entorno productivo:

``` text
Documento actualizado
       ↓
Blob Storage
       ↓
Indexación
       ↓
Chunks actualizados
       ↓
Embeddings actualizados
       ↓
Azure AI Search
```

Durante el desarrollo se utilizó inicialmente la indexación manual.

------------------------------------------------------------------------

# 11. Semantic Ranker

## ¿Qué es?

Semantic Ranker es una capacidad de Azure AI Search.

No es necesario crear un segundo servicio independiente.

Su función es mejorar el orden de relevancia de los resultados
recuperados.

Flujo:

``` text
Resultados candidatos
       ↓
Semantic Ranker
       ↓
Resultados mejor ordenados
```

------------------------------------------------------------------------

# 12. Configuración semántica

Se creó la configuración:

``` text
rag-semantic-config
```

Con:

``` text
Campo de título:
title

Campos de contenido:
chunk
```

En Azure AI Search se configuró:

``` text
Nombre:
rag-semantic-config

Title:
title

Content:
chunk
```

Esto permite que Semantic Ranker analice el título y el contenido de los
chunks.

------------------------------------------------------------------------

# 13. Hybrid Search

## ¿Qué es?

Hybrid Search combina:

``` text
Keyword Search
+
Vector Search
```

### Keyword Search

Es útil para coincidencias exactas:

``` text
OrderRepository
Quarkus
Kafka
PostgreSQL
JPA
REST
```

### Vector Search

Busca por similitud semántica.

Por ejemplo:

``` text
¿Dónde se implementan los repositorios?
```

puede recuperar información relacionada con:

``` text
Output Adapter
Infrastructure
OrderRepository
```

aunque la pregunta no contenga exactamente esas palabras.

------------------------------------------------------------------------

# 14. Hybrid Search + RRF

Azure AI Search combina los resultados de búsqueda textual y vectorial
utilizando un mecanismo de fusión de rankings como **RRF (Reciprocal
Rank Fusion)**.

Conceptualmente:

``` text
                 Pregunta
                    │
          ┌─────────┴─────────┐
          ▼                   ▼
   Keyword Search       Vector Search
          │                   │
          └─────────┬─────────┘
                    ▼
                   RRF
                    │
                    ▼
             candidatos
                    │
                    ▼
             Semantic Ranker
                    │
                    ▼
               Top 5
```

------------------------------------------------------------------------

# 15. Configuración utilizada en el código

La búsqueda vectorial utiliza 50 candidatos:

``` java
final VectorizableTextQuery vectorQuery =
        new VectorizableTextQuery(question)
            .setKNearestNeighbors(50)
            .setFields("text_vector");
```

Después se solicitan los 5 resultados principales:

``` java
.setTop(5)
```

La idea es:

``` text
50 candidatos
      ↓
Hybrid Search
      ↓
Semantic Ranker
      ↓
Top 5
```

------------------------------------------------------------------------

# 16. Backend Quarkus

La solución está organizada siguiendo una arquitectura hexagonal.

Conceptualmente:

``` text
Controller
    ↓
Use Case / Port
    ↓
AzureRAGAdapter
    ↓
AzureSearchAIAdapter
    ↓
Azure AI Search

AzureRAGAdapter
    ↓
AzureOpenAiAdapter
    ↓
Azure OpenAI
```

------------------------------------------------------------------------

# 17. AzureSearchAIAdapter

La clase recupera información desde Azure AI Search.

Implementación:

``` java
@ApplicationScoped
public class AzureSearchAIAdapter implements SearchAdapterPort {

    private final SearchClient searchClient;

    public AzureSearchAIAdapter(SearchClient client) {
        this.searchClient = client;
    }

    @Override
    public List<SearchChunk> generate(String question) {

        final VectorizableTextQuery vectorQuery =
                new VectorizableTextQuery(question)
                    .setKNearestNeighbors(50)
                    .setFields("text_vector");

        final SearchOptions searchOptions =
                new SearchOptions()
                    .setSearchText(question)
                    .setQueryType(QueryType.SEMANTIC)
                    .setSemanticConfigurationName("rag-semantic-config")
                    .setTop(5)
                    .setSelect(
                        "chunk_id",
                        "parent_id",
                        "chunk",
                        "title"
                    )
                    .setVectorQueries(List.of(vectorQuery));

        final SearchPagedIterable results =
                this.searchClient.search(searchOptions);

        return getSearchChunks(results);
    }

    private static List<SearchChunk> getSearchChunks(
            SearchPagedIterable results) {

        final List<SearchChunk> chunks = new ArrayList<>();

        for (SearchResult result : results) {

            final Map<String, Object> document =
                    result.getAdditionalProperties();

            final String chunkId =
                    Objects.toString(document.get("chunk_id"), "");

            final String parentId =
                    Objects.toString(document.get("parent_id"), "");

            final String chunk =
                    Objects.toString(document.get("chunk"), "");

            final String title =
                    Objects.toString(document.get("title"), "");

            if (chunk != null && !chunk.isBlank()) {
                chunks.add(
                    new SearchChunk(
                        chunkId,
                        parentId,
                        chunk,
                        title
                    )
                );
            }
        }

        return chunks;
    }
}
```

------------------------------------------------------------------------

# 18. SearchClient

El `SearchClient` utilizado pertenece a:

``` java
com.azure.search.documents.SearchClient
```

La dependencia del SDK utilizada durante el desarrollo fue:

``` text
com.azure:azure-search-documents
```

Versión utilizada:

``` text
12.0.0
```

La búsqueda se realiza utilizando:

``` java
SearchOptions
```

y:

``` java
searchClient.search(searchOptions);
```

------------------------------------------------------------------------

# 19. SearchChunk

Se creó un objeto para transportar los resultados recuperados:

``` java
public record SearchChunk(
    String chunkId,
    String parentId,
    String chunk,
    String title
) {}
```

Esto evita propagar directamente objetos del SDK de Azure por todas las
capas.

------------------------------------------------------------------------

# 20. AzureRAGAdapter

Esta clase coordina todo el proceso RAG.

Responsabilidades:

1.  recuperar chunks;
2.  construir contexto;
3.  preparar fuentes;
4.  llamar a Azure OpenAI;
5.  medir tiempos;
6.  devolver `RagResult`.

Implementación utilizada:

``` java
@Override
public RagResult generate(final String question) {

    final long totalStart = System.currentTimeMillis();

    // 1. Recuperar información
    final long searchStart = System.currentTimeMillis();

    final List<SearchChunk> chunks =
            this.azureSearchAIAdapter.generate(question);

    final long searchTimeMs =
            System.currentTimeMillis() - searchStart;

    // 2. Construir contexto
    final String context =
            chunks.stream()
                .map(SearchChunk::chunk)
                .collect(
                    Collectors.joining(
                        "\n\n---\n\n"
                    )
                );

    System.out.println(
        "Chunks encontrados: " + chunks.size()
    );

    System.out.println(
        "========== RAG CONTEXT =========="
    );

    System.out.println(context);

    System.out.println(
        "================================="
    );

    // 3. Fuentes
    final List<RagSource> sources =
            chunks.stream()
                .map(chunk ->
                    new RagSource(
                        chunk.title(),
                        chunk.chunkId(),
                        chunk.parentId()
                    )
                )
                .toList();

    // 4. Azure OpenAI
    final long openAiStart =
            System.currentTimeMillis();

    final String answer =
            this.azureOpenAiAdapter.question(
                question,
                context
            );

    final long openAiTimeMs =
            System.currentTimeMillis() - openAiStart;

    final long totalTimeMs =
            System.currentTimeMillis() - totalStart;

    System.out.println(
        "RAG TOTAL: " + totalTimeMs + " ms"
    );

    return new RagResult(
        answer,
        sources,
        chunks.size(),
        searchTimeMs,
        openAiTimeMs,
        totalTimeMs
    );
}
```

------------------------------------------------------------------------

# 21. RagResult

El resultado interno del RAG contiene:

``` text
answer
sources
chunksFound
searchTimeMs
openAiTimeMs
totalTimeMs
```

Por ejemplo:

``` java
public record RagResult(
    String answer,
    List<RagSource> sources,
    int chunksFound,
    long searchTimeMs,
    long openAiTimeMs,
    long totalTimeMs
) {}
```

------------------------------------------------------------------------

# 22. RagSource

Las fuentes permiten saber qué documentos/chunks se utilizaron.

``` java
public record RagSource(
    String title,
    String chunkId,
    String parentId
) {}
```

Ejemplo:

``` json
{
  "title": "Guía de Arquitectura Hexagonal.adoc",
  "chunkId": "...._pages_0",
  "parentId": "...."
}
```

Esto aporta trazabilidad a la respuesta.

------------------------------------------------------------------------

# 23. Azure OpenAI

Azure OpenAI se utiliza para generar la respuesta final.

Deployment:

``` text
gpt-5-mini-1
```

El flujo es:

``` text
Pregunta
   +
Contexto recuperado
   ↓
Azure OpenAI
   ↓
Respuesta
```

El modelo no debe considerarse como el repositorio principal de
conocimiento corporativo. La información específica de la empresa se
recupera mediante Azure AI Search.

------------------------------------------------------------------------

# 24. AzureOpenAiAdapter

La aplicación utiliza un adapter para aislar la integración con Azure
OpenAI.

La idea arquitectónica es:

``` text
OpenAIProvider
       ↑
AzureOpenAiAdapter
       ↓
OpenAIClient
       ↓
Azure OpenAI
```

El adapter recibe:

``` java
question
context
```

y devuelve:

``` java
String answer
```

Un punto importante es que la implementación final debe enviar
**pregunta + contexto** al modelo. El contexto recuperado por
`AzureRAGAdapter` es el corazón del RAG.

------------------------------------------------------------------------

# 25. Flujo RAG completo

Una pregunta como:

``` text
¿Qué es arquitectura hexagonal?
```

recorre:

``` text
1. React
      ↓
2. Quarkus Controller
      ↓
3. AzureRAGAdapter
      ↓
4. AzureSearchAIAdapter
      ↓
5. Keyword Search
      +
   Vector Search
      ↓
6. Hybrid Search
      ↓
7. Semantic Ranker
      ↓
8. Top 5 chunks
      ↓
9. Context
      ↓
10. AzureOpenAiAdapter
      ↓
11. gpt-5-mini-1
      ↓
12. Respuesta Markdown
      ↓
13. Sources + metadata
      ↓
14. React
```

------------------------------------------------------------------------

# 26. Respuesta del backend

El backend terminó devolviendo una estructura similar a:

``` json
{
  "queryId": "5c729b24-f2a0-4c1f-b100-8fdab87ccc78",
  "timestamp": "2026-08-12T20:06:44.617416001-05:00",
  "queryRequest": {
    "query": "dame un ejemplo se service usando la arquitectura hexagonal.",
    "metadata": {
      "user": {
        "username": "lgomezs"
      }
    }
  },
  "queryResult": {
    "response": "Correcto:\n\n```java\npublic class CreateOrderService {\n\n    private final OrderRepository repository;\n\n    public CreateOrderService(OrderRepository repository) {\n        this.repository = repository;\n    }\n\n    public void create(Order order) {\n        repository.save(order);\n    }\n}\n```",
    "sources": [
      {
        "title": "Guía de Arquitectura Hexagonal.adoc",
        "chunkId": "...._pages_0",
        "parentId": "...."
      }
    ]
  },
  "executionMetadata": {
    "chunksFound": 5,
    "searchTimeMs": 634,
    "openAiTimeMs": 5902,
    "totalTimeMs": 6537
  }
}
```

------------------------------------------------------------------------

# 27. ¿Por qué Markdown?

Azure OpenAI genera respuestas técnicas que pueden contener:

``` markdown
## Título

Texto explicativo.

### Ejemplo

```java
public class CreateOrderService {
    ...
}
```


    Por eso el frontend no debe mostrar la respuesta como texto plano.

    Debe interpretarla como Markdown.

    ---

    # 28. Frontend React

    Se eligió React para el frontend.

    El proyecto utiliza Vite.

    Arranque:

    ```bash
    npm run dev -- --host

Por defecto:

``` text
http://localhost:5173
```

El frontend funciona como interfaz del:

``` text
Developer AI Assistant
```

------------------------------------------------------------------------

# 29. Mejoras realizadas en el frontend

Se implementó:

-   renderizado Markdown;
-   bloques de código;
-   syntax highlighting;
-   botón para copiar código;
-   visualización de fuentes;
-   visualización de metadata;
-   mayor espacio para respuestas;
-   eliminación de botones inferiores innecesarios.

Se eliminaron:

``` text
Nueva conversación
Eliminar historial
```

de la zona inferior para aprovechar mejor el espacio.

------------------------------------------------------------------------
# 34. Problemas encontrados

## 34.1 Error 403 de Azure AI Search

Error:

``` text
403

The given API key doesn't match service's
internal, primary or secondary keys.
```

La causa es una API key que no corresponde al servicio de Azure AI
Search configurado.

Hay que revisar:

``` text
Azure Search endpoint
Azure Search API key
Azure Search index name
```

y comprobar que pertenecen al mismo recurso.

------------------------------------------------------------------------

## 34.2 `SearchRequest` vs `SearchOptions`

Se encontró:

``` text
Required type:
SearchOptions

Provided:
SearchRequest
```

La solución fue utilizar:

``` java
SearchOptions
```

y:

``` java
searchClient.search(searchOptions);
```

------------------------------------------------------------------------

## 34.3 `SearchPagedIterable`

Los resultados se procesaron mediante iteración:

``` java
for (SearchResult result : results) {
    ...
}
```

y se obtuvieron los campos mediante:

``` java
result.getAdditionalProperties()
```

------------------------------------------------------------------------

# 35. Arquitectura del backend

Una organización recomendada para este proyecto:

``` text
src/main/java/
└── com.company.application/
    ├── domain/
    │   ├── model/
    │   └── port/
    │       ├── in/
    │       └── out/
    │
    ├── application/
    │   └── service/
    │
    └── infrastructure/
        ├── adapter/
        │   ├── in/
        │   └── out/
        └── configuration/
```

La idea principal es:

``` text
Dominio
  ↓
Interfaces
  ↓
Adapters
  ↓
Tecnologías concretas
```

------------------------------------------------------------------------

# 36. Arquitectura Hexagonal aplicada al propio RAG

Por ejemplo:

``` text
SearchAdapterPort
       ↑
AzureSearchAIAdapter
       ↓
Azure AI Search
```

y:

``` text
OpenAIProvider
       ↑
AzureOpenAiAdapter
       ↓
Azure OpenAI
```

El dominio/aplicación no debería depender directamente de las clases
concretas del SDK.

Esto facilita reemplazar una tecnología posteriormente.

------------------------------------------------------------------------

# 37. Conceptos aprendidos

## RAG

Retrieval-Augmented Generation.

Primero se recupera información y después se genera la respuesta.

``` text
Retrieve
   ↓
Generate
```

------------------------------------------------------------------------

## Chunk

Fragmento de un documento.

En vez de enviar un documento completo:

``` text
Documento completo
```

se recuperan fragmentos:

``` text
Chunk 1
Chunk 2
Chunk 3
```

------------------------------------------------------------------------

## Embedding

Representación numérica del contenido.

Permite comparar similitud semántica.

------------------------------------------------------------------------

## Vector Search

Busca información por similitud de vectores.

------------------------------------------------------------------------

## Keyword Search

Busca coincidencias textuales.

------------------------------------------------------------------------

## Hybrid Search

Combina búsqueda textual y vectorial.

------------------------------------------------------------------------

## Semantic Ranker

Reordena los resultados para mejorar la relevancia semántica.

------------------------------------------------------------------------

## Context

Es la información recuperada que se envía al modelo.

``` text
Pregunta
+
Contexto
↓
Azure OpenAI
```

------------------------------------------------------------------------

# 38. ¿Por qué no enviar todo el documento?

Enviar todo el documento aumentaría:

-   tokens;
-   coste;
-   latencia;
-   ruido;
-   información irrelevante.

El objetivo del RAG es recuperar solamente la información necesaria.

------------------------------------------------------------------------

# 39. Fuentes y trazabilidad

La respuesta devuelve:

``` text
title
chunkId
parentId
```

Esto permite saber de qué documento y chunk se obtuvo la información.

En una evolución productiva se puede ampliar con:

``` text
score
rerankerScore
page
section
url
documentVersion
```

para mejorar la trazabilidad.

------------------------------------------------------------------------

# 40. Seguridad para producción

El prototipo funciona con credenciales de desarrollo, pero en producción
se debe evolucionar hacia:

-   Managed Identity;
-   Azure Key Vault;
-   RBAC;
-   autenticación;
-   autorización;
-   eliminación de secretos del código;
-   control de acceso por documento;
-   filtros de seguridad en Azure AI Search.

El flujo productivo debería ser:

``` text
Usuario
   ↓
Autenticación
   ↓
Autorización
   ↓
RAG
   ↓
¿Puede acceder al documento?
   ↓
Sí → recuperar
No → excluir
```

------------------------------------------------------------------------

# 41. Observabilidad para producción

La implementación ya registra tiempos básicos.

La siguiente evolución es integrar:

``` text
OpenTelemetry
Application Insights
Prometheus
Grafana
```

y métricas como:

``` text
rag.search.duration
rag.openai.duration
rag.total.duration
rag.chunks.count
rag.context.length
rag.tokens.input
rag.tokens.output
```

------------------------------------------------------------------------

# 42. Evaluación de calidad

Un RAG productivo no debe evaluarse solamente por velocidad.

También hay que comprobar:

``` text
¿La respuesta es correcta?
¿La fuente realmente soporta la respuesta?
¿Recuperó los chunks correctos?
¿Está inventando información?
```

Una estrategia recomendada:

``` text
Pregunta
Respuesta esperada
Fuentes esperadas
```

y ejecutar un conjunto de pruebas automáticamente.

------------------------------------------------------------------------

# 44. Actualización de documentación

En producción debe existir una estrategia de actualización:

``` text
Documento actualizado
       ↓
Blob Storage
       ↓
Indexación
       ↓
Chunking
       ↓
Embeddings
       ↓
Azure AI Search
```

La programación de indexación puede utilizarse para automatizar este
proceso.

------------------------------------------------------------------------

# 45. Próximas mejoras

Las siguientes capacidades son las más importantes para evolucionar el
prototipo:

### Seguridad

-   Managed Identity
-   Key Vault
-   RBAC
-   autenticación
-   autorización
-   filtros de seguridad

### Observabilidad

-   OpenTelemetry
-   Application Insights
-   métricas RAG
-   trazas distribuidas

### Evaluación

-   dataset de preguntas
-   respuestas esperadas
-   evaluación de fuentes
-   evaluación de grounding
-   pruebas automáticas

### Rendimiento

-   optimización de chunks
-   reducción de contexto
-   control de tokens
-   caching cuando corresponda
-   pruebas de carga

### Documentación

-   versionado
-   indexación automática
-   metadata
-   clasificación documental

------------------------------------------------------------------------

# 49. Resultado final

La solución evolucionó desde un flujo simple:

``` text
Pregunta
   ↓
Azure OpenAI
```

hasta:

``` text
Pregunta
   ↓
Azure AI Search
   ↓
Keyword Search
+
Vector Search
   ↓
Hybrid Search
   ↓
Semantic Ranker
   ↓
Top chunks
   ↓
Context
   ↓
Azure OpenAI
   ↓
Respuesta fundamentada
   ↓
Fuentes
   ↓
React
```

El resultado es un **prototipo funcional end-to-end de un asistente
corporativo basado en RAG**, con una base sólida para evolucionar hacia
un entorno productivo.

------------------------------------------------------------------------

## Fin del README

**Proyecto:** Developer AI Assistant\
**Arquitectura:** RAG + Hybrid Search + Semantic Ranker\
**Backend:** Quarkus / Java\
**Frontend:** React / Vite\
**Cloud:** Microsoft Azure\
**Estado:** Prototipo funcional end-to-end
