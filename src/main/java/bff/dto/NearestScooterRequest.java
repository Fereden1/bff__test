package bff.dto;

import java.math.BigDecimal;

public class NearestScooterRequest {
    private BigDecimal posX;
    private BigDecimal posY;

    public BigDecimal getPosX() { return posX; }
    public void setPosX(BigDecimal posX) { this.posX = posX; }
    public BigDecimal getPosY() { return posY; }
    public void setPosY(BigDecimal posY) { this.posY = posY; }
}
