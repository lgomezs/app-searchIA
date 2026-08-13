package pe.com.admin.domain.model;

import java.util.List;

public record RagResult(String answer, List<RagSource> sources, int chunksFound, long searchTimeMs, long openAiTimeMs,
		long totalTimeMs) {
}
