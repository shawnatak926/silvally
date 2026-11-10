package com.silvally.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class Stats {
    @Column(name = "stat_p", nullable = false)
    private int p;
    @Column(name = "stat_f", nullable = false)
    private int f;
    @Column(name = "stat_b", nullable = false)
    private int b;
    @Column(name = "stat_i", nullable = false)
    private int i;
    @Column(name = "stat_a", nullable = false)
    private int a;
    @Column(name = "stat_c", nullable = false)
    private int c;
    @Column(name = "stat_h", nullable = false)
    private int h;
    @Column(name = "stat_s", nullable = false)
    private int s;

    protected Stats() {
    }

    public Stats(int p, int f, int b, int i, int a, int c, int h, int s) {
        this.p = p;
        this.f = f;
        this.b = b;
        this.i = i;
        this.a = a;
        this.c = c;
        this.h = h;
        this.s = s;
    }

    public static Stats initial() {
        return new Stats(20, 8, 8, 8, 24, 20, 72, 18);
    }

    public int getP() { return p; }
    public int getF() { return f; }
    public int getB() { return b; }
    public int getI() { return i; }
    public int getA() { return a; }
    public int getC() { return c; }
    public int getH() { return h; }
    public int getS() { return s; }
}
