# ether-ai-openai

Adapter de Ether para OpenAI implementado con `java.net.http.HttpClient` y `ether-json`,
exponiendo una API pública basada en los contratos de `ether-ai-core`.
No depende de ningún SDK oficial de OpenAI.

## Maven Dependency

```xml
<dependency>
    <groupId>dev.rafex.ether.ai</groupId>
    <artifactId>ether-ai-openai</artifactId>
    <version>8.0.0-SNAPSHOT</version>
</dependency>
```

---

## Configuración: `OpenAiConfig`

```java
// Constructor rápido — modelo y URI por defecto
OpenAiConfig config = OpenAiConfig.of(System.getenv("OPENAI_API_KEY"));

// Constructor completo
OpenAiConfig config = new OpenAiConfig(
    System.getenv("OPENAI_API_KEY"),   // apiKey
    "gpt-4o-mini",                     // defaultModel
    "https://api.openai.com/v1/chat/completions"  // chatCompletionsUri
);

String uri   = config.chatCompletionsUri();
String model = config.defaultModel();
```

### Azure OpenAI

Para usar Azure OpenAI cambia solo la URI y la apiKey:

```java
OpenAiConfig azure = new OpenAiConfig(
    System.getenv("AZURE_OPENAI_KEY"),
    "gpt-4o",
    "https://<resource>.openai.azure.com/openai/deployments/<deployment>/chat/completions?api-version=2024-02-01"
);
```

---

## Uso: `OpenAiChatModel`

```java
OpenAiConfig    config = OpenAiConfig.of(System.getenv("OPENAI_API_KEY"));
OpenAiChatModel model  = new OpenAiChatModel(config);

AiChatRequest  request  = AiChatRequest.of("¿Qué es el garbage collector en Java?");
AiChatResponse response = model.chat(request);

System.out.println(response.text());
System.out.println("Tokens usados: " + response.usage().totalTokens());
```

### HttpClient personalizado

```java
HttpClient client = HttpClient.newBuilder()
    .connectTimeout(Duration.ofSeconds(10))
    .build();

OpenAiChatModel model = new OpenAiChatModel(config, client);
```

---

## Conversación multi-turno

```java
List<AiMessage> history = new ArrayList<>();
history.add(AiMessage.system("Eres un tutor de programación Java."));

// Turno 1
history.add(AiMessage.user("Explica qué es un record en Java."));
AiChatResponse r1 = model.chat(new AiChatRequest(history, null, null));
history.add(AiMessage.assistant(r1.text()));

// Turno 2
history.add(AiMessage.user("¿Puedo extender un record?"));
AiChatResponse r2 = model.chat(new AiChatRequest(history, null, null));
System.out.println(r2.text());
```

---

## Manejo de errores

```java
try {
    AiChatResponse response = model.chat(request);
} catch (AiHttpException e) {
    switch (e.statusCode()) {
        case 401 -> System.err.println("API key inválida");
        case 429 -> System.err.println("Rate limit — reintenta después");
        case 500 -> System.err.println("Error interno OpenAI: " + e.responseBody());
        default  -> System.err.println("HTTP " + e.statusCode() + ": " + e.responseBody());
    }
}
```

---

## Inyección con ether-di

```java
public class AiContainer {

    private final Lazy<OpenAiConfig>    config = new Lazy<>(() ->
            OpenAiConfig.of(System.getenv("OPENAI_API_KEY")));
    private final Lazy<OpenAiChatModel> model  = new Lazy<>(() ->
            new OpenAiChatModel(config.get()));

    public AiChatModel chatModel() { return model.get(); }
}
```

---

## Modelos recomendados

| Modelo | Uso |
|---|---|
| `gpt-4o` | Máxima capacidad, ideal para tareas complejas |
| `gpt-4o-mini` | Rápido y económico, ideal para producción |
| `o1-mini` | Razonamiento mejorado para problemas técnicos |

---

## Módulos relacionados

| Módulo | Descripción |
|---|---|
| [ether-ai-core](../ether-ai-core/README.md) | Contratos compartidos: `AiChatModel`, `AiMessage`, `AiChatRequest/Response` |
| [ether-ai-deepseek](../ether-ai-deepseek/README.md) | Adapter alternativo para DeepSeek |
