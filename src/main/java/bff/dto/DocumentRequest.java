package bff.dto;

public record DocumentRequest(
        Long userId,
        String docType,
        Long sourceId,
        String from,
        String to,
        String status) {
}
