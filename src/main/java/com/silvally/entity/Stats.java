package com.silvally.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.LinkedHashMap;
import java.util.Map;

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

    public java.util.Map<String, Integer> values() {
        return java.util.Map.of("P", p, "F", f, "B", b, "I", i, "A", a, "C", c, "H", h, "S", s);
    }
    public void apply(java.util.Map<String, Integer> delta) {
        p = clamp(p + delta.getOrDefault("P", 0));
        f = clamp(f + delta.getOrDefault("F", 0));
        b = clamp(b + delta.getOrDefault("B", 0));
        i = clamp(i + delta.getOrDefault("I", 0));
        a = clamp(a + delta.getOrDefault("A", 0));
        c = clamp(c + delta.getOrDefault("C", 0));
        h = clamp(h + delta.getOrDefault("H", 0));
        s = clamp(s + delta.getOrDefault("S", 0));
    }
    private int clamp(int value) { return Math.max(0, Math.min(100, value)); }

    public Map<String, Integer> toMap() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("P", p);
        map.put("F", f);
        map.put("B", b);
        map.put("I", i);
        map.put("A", a);
        map.put("C", c);
        map.put("H", h);
        map.put("S", s);
        return map;
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
