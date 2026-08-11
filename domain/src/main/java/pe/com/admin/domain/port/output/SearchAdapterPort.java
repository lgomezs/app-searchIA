package pe.com.admin.domain.port.output;

import java.util.List;

public interface SearchAdapterPort {

	List<String> generate(String prompt);

}
