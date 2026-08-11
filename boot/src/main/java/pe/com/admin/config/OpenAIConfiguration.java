package pe.com.admin.config;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Singleton;
import jakarta.ws.rs.Produces;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class OpenAIConfiguration {

	@ConfigProperty(name = "azure.openai.endpoint")
	String endpoint;

	@ConfigProperty(name = "azure.openai.api-key")
	String apiKey;

	@Produces
	@Singleton
	OpenAIClient openAIClient() {
		return OpenAIOkHttpClient.builder().baseUrl(this.endpoint).apiKey(this.apiKey).build();
	}

}
