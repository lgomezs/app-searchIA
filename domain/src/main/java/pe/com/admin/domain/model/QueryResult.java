package pe.com.admin.domain.model;

import java.util.List;

public record QueryResult(String response, List<RagSource> sources) {
}
