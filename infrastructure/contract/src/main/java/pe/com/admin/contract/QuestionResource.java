package pe.com.admin.contract;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import pe.com.admin.application.usecase.AskQuestionUseCase;
import pe.com.admin.domain.model.Question;

@Path("/assistant")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class QuestionResource {

	@Inject
	private AskQuestionUseCase askQuestionUseCase;

	@POST
	@Path("/search")
	public AiResponse ask(AiQuestionRequest question) {
		final String response = this.askQuestionUseCase.ask(new Question(question.question()));
		return new AiResponse(response);
	}
}
