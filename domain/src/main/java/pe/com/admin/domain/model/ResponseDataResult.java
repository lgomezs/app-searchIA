package pe.com.admin.domain.model;

import java.time.OffsetDateTime;

public record ResponseDataResult(String queryId, OffsetDateTime timestamp, QueryRequest queryRequest,
		QueryResult queryResult, ExecutionMetadata executionMetadata) {
}
