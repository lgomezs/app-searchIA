package pe.com.admin.contract;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import pe.com.admin.application.usecase.AskQuestionUseCase;
import pe.com.admin.domain.model.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Path("/assistant")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class QuestionResource {

	@Inject
	private AskQuestionUseCase askQuestionUseCase;

	@POST
	@Path("/search")
	public Response ask(AIQuestionRequest question) {
		final String queryId = UUID.randomUUID().toString();
		// get user from session
		final RagResult rgResultResponse = this.askQuestionUseCase.ask(new Question(question.question()));
		final ResponseDataResult response = new ResponseDataResult(queryId, OffsetDateTime.now(),
				new QueryRequest(question.question(), new QueryMetadata(new UserMetadata("lgomezs"))),
				new QueryResult(rgResultResponse.answer(), rgResultResponse.sources()),
				new ExecutionMetadata(rgResultResponse.chunksFound(), rgResultResponse.searchTimeMs(),
						rgResultResponse.openAiTimeMs(), rgResultResponse.totalTimeMs()));

		return Response.ok(response).build();
	}
}
