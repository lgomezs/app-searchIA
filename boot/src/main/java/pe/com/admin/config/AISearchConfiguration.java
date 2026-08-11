package pe.com.admin.config;

import com.azure.core.credential.AzureKeyCredential;
import com.azure.search.documents.SearchClient;
import com.azure.search.documents.SearchClientBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class AISearchConfiguration {

	@ConfigProperty(name = "azure.search.endpoint")
	String endpoint;

	@ConfigProperty(name = "azure.search.index-name")
	String indexName;

	@ConfigProperty(name = "azure.search.api-key")
	String apiKey;

	@Produces
	@ApplicationScoped
	public SearchClient searchClient() {
		return new SearchClientBuilder().endpoint(this.endpoint).indexName(this.indexName)
				.credential(new AzureKeyCredential(this.apiKey)).buildClient();
	}

}
