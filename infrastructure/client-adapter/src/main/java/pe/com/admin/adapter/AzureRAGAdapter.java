package pe.com.admin.adapter;

import jakarta.enterprise.context.ApplicationScoped;
import pe.com.admin.domain.model.RagResult;
import pe.com.admin.domain.model.RagSource;
import pe.com.admin.domain.model.SearchChunk;
import pe.com.admin.domain.port.output.SearchRGAPort;

import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class AzureRAGAdapter implements SearchRGAPort {

	private final AzureSearchAIAdapter azureSearchAIAdapter;

	private final AzureOpenAiAdapter azureOpenAiAdapter;

	public AzureRAGAdapter(AzureSearchAIAdapter azureSearchAIAdapter, AzureOpenAiAdapter azureOpenAiAdapter) {
		this.azureSearchAIAdapter = azureSearchAIAdapter;
		this.azureOpenAiAdapter = azureOpenAiAdapter;
	}

	@Override
	public RagResult generate(final String question) {
		final long totalStart = System.currentTimeMillis();

		// 1. Recuperar información de la empresa
		final long searchStart = System.currentTimeMillis();
		final List<SearchChunk> chunks = this.azureSearchAIAdapter.generate(question);

		final long searchTimeMs = System.currentTimeMillis() - searchStart;

		// 2. Construir contexto
		final String context = chunks.stream().map(SearchChunk::chunk).collect(Collectors.joining("\n\n---\n\n"));

		System.out.println("Chunks encontrados: " + chunks.size());

		final List<RagSource> sources = chunks.stream()
				.map(chunk -> new RagSource(chunk.title(), chunk.chunkId(), chunk.parentId())).toList();

		// 3. Enviar pregunta + contexto a Azure OpenAI
		final long openAiStart = System.currentTimeMillis();
		final String answer = this.azureOpenAiAdapter.question(question, context);

		final long openAiTimeMs = System.currentTimeMillis() - openAiStart;

		final long totalTimeMs = System.currentTimeMillis() - totalStart;

		System.out.println("RAG TOTAL: " + totalTimeMs + " ms");

		return new RagResult(answer, sources, chunks.size(), searchTimeMs, openAiTimeMs, totalTimeMs);
	}
}
