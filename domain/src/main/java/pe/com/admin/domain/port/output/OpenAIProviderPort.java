package pe.com.admin.domain.port.output;

public interface OpenAIProviderPort {

	String question(final String question, final String context);
}
