package bff.dto;

import java.math.BigDecimal;

public class ScooterDto {
    private Long id;
    private String numberPlate;
    private int charge;
    private String status;
    private BigDecimal posX;
    private BigDecimal posY;
    private boolean deleted;
    private Double distance;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNumberPlate() { return numberPlate; }
    public void setNumberPlate(String numberPlate) { this.numberPlate = numberPlate; }
    public int getCharge() { return charge; }
    public void setCharge(int charge) { this.charge = charge; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public BigDecimal getPosX() { return posX; }
    public void setPosX(BigDecimal posX) { this.posX = posX; }
    public BigDecimal getPosY() { return posY; }
    public void setPosY(BigDecimal posY) { this.posY = posY; }
    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }
    public Double getDistance() { return distance; }
    public void setDistance(Double distance) { this.distance = distance; }
}
