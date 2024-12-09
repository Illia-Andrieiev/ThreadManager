package homework;

public class ComputingFunctions {
    private int x;
    public ComputingFunctions(int x){
        this.x = x;
    }
    public void setX(int x){
        this.x = x;
    }
    public int getX(){
        return this.x;
    }
    public double squareRoot(){
        if (x < 0) {
            throw new IllegalArgumentException("Number must be non-negative.");
        }
        double epsilon = 1e-10;
        double guess = x;
        while (Math.abs(guess * guess - x) > epsilon) {
            guess = (guess + x / guess) / 2.0;
        }
        return guess;
    }
    public double duplicate(){
        return 2*x;
    }
    public double abs(){
        return Math.abs(x);
    }
    public double square(){
        return x*x;
    }
    public double factorial(){
        if (x < 0) {
            throw new IllegalArgumentException("Number must be non-negative.");
        }
        double res = 1;
        for(int i = 2; i<=x;i++){
            res *= i;
        }
        return res;
    }
}

