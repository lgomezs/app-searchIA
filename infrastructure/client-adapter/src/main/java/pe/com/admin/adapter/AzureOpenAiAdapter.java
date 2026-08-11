package pe.com.admin.adapter;

import com.openai.client.OpenAIClient;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import pe.com.admin.domain.port.output.OpenAIProviderPort;

import java.util.Optional;

@ApplicationScoped
public class AzureOpenAiAdapter implements OpenAIProviderPort {

	@ConfigProperty(name = "azure.openai.deployment")
	String deployment;

	private final OpenAIClient openAIClient;

	public AzureOpenAiAdapter(OpenAIClient client) {
		this.openAIClient = client;
	}

	@Override
	public String question(final String question, final String context) {
		final long start = System.currentTimeMillis();

		final String prompt = """
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

		final ChatCompletionCreateParams params = ChatCompletionCreateParams.builder().model(this.deployment)
				.addUserMessage(prompt).build();

		final ChatCompletion completion = this.openAIClient.chat().completions().create(params);

		final Optional<ChatCompletion.Choice> responseOptional = completion.choices().stream().findFirst();

		final long end = System.currentTimeMillis();

		System.out.println("OpenAI choices: " + completion.choices().size());

		System.out.println("Azure OpenAI: " + (end - start) + " ms");

		return responseOptional.map(choice -> choice.message().content().orElse(""))
				.orElseGet(() -> String.format("No response from Azure OpenAI for question: %s", question));

	}

}
