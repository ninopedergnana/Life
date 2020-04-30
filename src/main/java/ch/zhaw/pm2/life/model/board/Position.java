package ch.zhaw.pm2.life.model.board;

/**
 * Position Objects represent the positions of the GameObjects in the field
 */
public class Position {
    private int xValue;
    private int yValue;


    public Position(int xValue, int yValue){
        this.xValue = xValue;
        this.yValue = yValue;
    }

    public int getX() {
        return xValue;
    }

    public int getY() {
        return yValue;
    }
}