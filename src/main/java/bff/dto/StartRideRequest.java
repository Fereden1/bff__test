package bff.dto;

import java.math.BigDecimal;

public class StartRideRequest {
    private Long scooterId;
    private Long rateId;
    private BigDecimal posX;
    private BigDecimal posY;

    public Long getScooterId() { return scooterId; }
    public void setScooterId(Long scooterId) { this.scooterId = scooterId; }
    public Long getRateId() { return rateId; }
    public void setRateId(Long rateId) { this.rateId = rateId; }
    public BigDecimal getPosX() { return posX; }
    public void setPosX(BigDecimal posX) { this.posX = posX; }
    public BigDecimal getPosY() { return posY; }
    public void setPosY(BigDecimal posY) { this.posY = posY; }
}
