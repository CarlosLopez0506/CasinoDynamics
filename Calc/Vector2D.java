package Calc;

public class Vector2D {
    private float x;
    private float y;

    public static final Vector2D ZERO = new Vector2D(0,0);

    public Vector2D(float x, float y){
        this.x = x;
        this.y = y;

    }

    public float getMagnitude(){
        double temp = (Math.pow(x, 2) + Math.pow(y, 2));
        return (float) Math.sqrt(temp);
    }


    public void scale(float s){
        this.x *= s;
        this.y *= s;
    }

    public float getX(){ return this.x; }
    public void setX(float x) { this.x = x; }

    public float getY(){ return this.y; }
    public void setY(float y) { this.y = y; }

    public void copy(Vector2D vec){
        setX(vec.getX());
        setY(vec.getY());
    }

    public Vector2D normalized(){
        float mag = this.getMagnitude();
        return new Vector2D(this.x / mag, this.y / mag);
    }

    public static float distance(Vector2D from, Vector2D to) { return Vector2D.sub(to, from).getMagnitude(); }

    public static Vector2D add(Vector2D a, Vector2D b){
        return new Vector2D(a.x + b.x, a.y + b.y);
    }

    public static Vector2D sub(Vector2D b, Vector2D a){
        return new Vector2D(b.x - a.x, b.y - a.y);
    }

    public static Vector2D getDirection(Vector2D from, Vector2D to){
        return Vector2D.sub(to, from).normalized();
    }

    public static float dot(Vector2D a, Vector2D b){
        return (a.x * b.x) + (a.y * b.y);
    }
}
