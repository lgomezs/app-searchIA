package pe.com.admin.domain.port.output;

import pe.com.admin.domain.model.RagResult;

public interface SearchRGAPort {

	RagResult generate(String question);
}
