package pe.com.admin.adapter;

import jakarta.enterprise.context.ApplicationScoped;
import pe.com.admin.domain.port.output.SearchRGAPort;

import java.util.List;

@ApplicationScoped
public class AzureRAGAdapter implements SearchRGAPort {

	private final AzureSearchAIAdapter azureSearchAIAdapter;

	private final AzureOpenAiAdapter azureOpenAiAdapter;

	public AzureRAGAdapter(AzureSearchAIAdapter azureSearchAIAdapter, AzureOpenAiAdapter azureOpenAiAdapter) {
		this.azureSearchAIAdapter = azureSearchAIAdapter;
		this.azureOpenAiAdapter = azureOpenAiAdapter;
	}

	@Override
	public String generate(final String question) {
		final long start = System.currentTimeMillis();

		// 1. Recuperar información de la empresa
		final List<String> chunks = this.azureSearchAIAdapter.generate(question);

		// 2. Construir contexto
		final String context = String.join("\n\n---\n\n", chunks);

		System.out.println("Chunks encontrados: " + chunks.size());
		System.out.println("Context chars: " + context.length());

		// 3. Enviar pregunta + contexto a Azure OpenAI
		final String answer = this.azureOpenAiAdapter.question(question, context);

		final long end = System.currentTimeMillis();

		System.out.println("RAG TOTAL: " + (end - start) + " ms");

		return answer;

	}
}
