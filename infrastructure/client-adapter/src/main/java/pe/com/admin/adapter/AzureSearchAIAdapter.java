package pe.com.admin.adapter;

import com.azure.search.documents.SearchClient;
import com.azure.search.documents.models.SearchOptions;
import com.azure.search.documents.models.SearchPagedIterable;
import com.azure.search.documents.models.SearchResult;
import com.azure.search.documents.models.VectorizableTextQuery;
import jakarta.enterprise.context.ApplicationScoped;
import pe.com.admin.domain.port.output.SearchAdapterPort;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class AzureSearchAIAdapter implements SearchAdapterPort {

	private final SearchClient searchClient;

	public AzureSearchAIAdapter(SearchClient client) {
		this.searchClient = client;
	}

	@Override
	public List<String> generate(String question) {
		final long start = System.currentTimeMillis();

		final VectorizableTextQuery vectorQuery = new VectorizableTextQuery(question).setKNearestNeighbors(5)
				.setFields("text_vector");

		final SearchOptions searchOptions = new SearchOptions().setSearchText(question).setTop(5)
				.setSelect("chunk_id", "parent_id", "chunk", "title").setVectorQueries(List.of(vectorQuery));

		final SearchPagedIterable results = this.searchClient.search(searchOptions);

		final List<String> chunks = new ArrayList<>();

		for (final SearchResult result : results) {

			final Map<String, Object> document = result.getAdditionalProperties();

			final Object chunk = document.get("chunk");

			if (chunk != null && !chunk.toString().isBlank()) {
				chunks.add(chunk.toString());
			}
		}

		final long end = System.currentTimeMillis();

		System.out.println("Azure AI Search: " + (end - start) + " ms");

		return chunks;

	}

}
