package bff.dto;

public class DocumentRequest {
    private Long userId;
    private String docType;
    private Long sourceId;
    private String from;
    private String to;
    private String status;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getDocType() { return docType; }
    public void setDocType(String docType) { this.docType = docType; }
    public Long getSourceId() { return sourceId; }
    public void setSourceId(Long sourceId) { this.sourceId = sourceId; }
    public String getFrom() { return from; }
    public void setFrom(String from) { this.from = from; }
    public String getTo() { return to; }
    public void setTo(String to) { this.to = to; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
