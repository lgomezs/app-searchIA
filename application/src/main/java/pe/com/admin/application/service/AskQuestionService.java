package pe.com.admin.application.service;

import jakarta.enterprise.context.ApplicationScoped;
import pe.com.admin.application.usecase.AskQuestionUseCase;
import pe.com.admin.domain.model.Question;
import pe.com.admin.domain.port.output.SearchRGAPort;

@ApplicationScoped
public class AskQuestionService implements AskQuestionUseCase {

	private final SearchRGAPort searchRGAPort;

	public AskQuestionService(SearchRGAPort searchRGAPort) {
		this.searchRGAPort = searchRGAPort;
	}

	@Override
	public String ask(final Question question) {
		return this.searchRGAPort.generate(question.text());
	}
}
