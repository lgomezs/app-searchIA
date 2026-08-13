package pe.com.admin.domain.port.output;

import pe.com.admin.domain.model.SearchChunk;

import java.util.List;

public interface SearchAdapterPort {

	List<SearchChunk> generate(String prompt);

}
