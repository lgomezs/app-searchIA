package pe.com.admin.adapter;

import com.azure.search.documents.SearchClient;
import com.azure.search.documents.models.*;
import jakarta.enterprise.context.ApplicationScoped;
import org.jetbrains.annotations.NotNull;
import pe.com.admin.domain.model.SearchChunk;
import pe.com.admin.domain.port.output.SearchAdapterPort;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@ApplicationScoped
public class AzureSearchAIAdapter implements SearchAdapterPort {

	private final SearchClient searchClient;

	public AzureSearchAIAdapter(SearchClient client) {
		this.searchClient = client;
	}

	@Override
	public List<SearchChunk> generate(String question) {
		final VectorizableTextQuery vectorQuery = new VectorizableTextQuery(question).setKNearestNeighbors(50)
				.setFields("text_vector");

		final SearchOptions searchOptions = new SearchOptions().setSearchText(question).setQueryType(QueryType.SEMANTIC)
				.setSemanticConfigurationName("rag-semantic-config").setTop(5)
				.setSelect("chunk_id", "parent_id", "chunk", "title").setVectorQueries(List.of(vectorQuery));

		final SearchPagedIterable results = this.searchClient.search(searchOptions);

		return getSearchChunks(results);

	}

	private static @NotNull List<SearchChunk> getSearchChunks(SearchPagedIterable results) {
		final List<SearchChunk> chunks = new ArrayList<>();

		for (final SearchResult result : results) {

			final Map<String, Object> document = result.getAdditionalProperties();

			final String chunkId = Objects.toString(document.get("chunk_id"), "");

			final String parentId = Objects.toString(document.get("parent_id"), "");

			final String chunk = Objects.toString(document.get("chunk"), "");

			final String title = Objects.toString(document.get("title"), "");

			System.out.println("SEARCH SCORE: " + result.getScore());

			System.out.println("RERANKER SCORE: " + result.getRerankerScore());

			if (!chunk.isBlank()) {
				chunks.add(new SearchChunk(chunkId, parentId, chunk, title));
			}

		}

		return chunks;
	}

}
