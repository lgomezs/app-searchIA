package pe.com.admin.application.usecase;

import pe.com.admin.domain.model.Question;
import pe.com.admin.domain.model.RagResult;

public interface AskQuestionUseCase {

	RagResult ask(final Question question);
}
