package com.example.doatest;

public class Ball {
    public int x;
    public int origin_x;
    public int y;
    public int origin_y;
    public int target_r;
    public int theta;
    public int dx;
    public int dy;
    public int r;
    public int g;
    public int b;
    public int size;
    public int end_count;
    public double speed;

    public void move() {
        this.x = this.x + (int)((this.dx) * this.speed);
        this.y = this.y + (int)((this.dy) * this.speed);
    }

    public boolean delete() {
        if (Math.pow(Math.pow(x - origin_x, 2) + Math.pow(y - origin_y, 2), 0.5) > this.target_r)
            return true;
        return false;
    }

    public Ball(int x, int y, int target_r, int theta,
                int r, int g, int b, int size, int end_count, double speed) {
        this.x = x;
        this.origin_x = x;
        this.y = y;
        this.origin_y = y;
        this.target_r = target_r;
        this.theta = theta;
        this.r = r;
        this.g = g;
        this.b = b;
        double PI = 3.14;
        this.dx = (int) ((int) r * (Math.cos(theta / 180.0 * PI) / end_count));
        this.dy = (int) ((int) r * ((Math.sin(theta / 180.0 * PI)) / end_count));
        this.size = size;
        this.end_count = end_count;
        this.speed = speed;
    }
}
