package pe.com.admin.domain.model;

public record ExecutionMetadata(int chunksFound, long searchTimeMs, long openAiTimeMs, long totalTimeMs) {
}
