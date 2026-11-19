package com.silvally.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import org.hibernate.annotations.ColumnDefault;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Embeddable
public class Stats {
    public static final List<String> KEYS = List.of("P", "F", "B", "I", "A", "G", "C", "H", "S", "EN");
    public static final int LEGACY_GRADE = 70;

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
    @ColumnDefault("70")
    @Column(name = "stat_g", nullable = false)
    private int g = LEGACY_GRADE;
    @Column(name = "stat_c", nullable = false)
    private int c;
    @Column(name = "stat_h", nullable = false)
    private int h;
    @Column(name = "stat_s", nullable = false)
    private int s;
    @Column(name = "stat_en", nullable = false)
    private int en = 22;

    protected Stats() {
    }

    public static Stats of(Map<String, Integer> values) {
        Stats stats = new Stats();
        stats.p = clamp(values.get("P"));
        stats.f = clamp(values.get("F"));
        stats.b = clamp(values.get("B"));
        stats.i = clamp(values.get("I"));
        stats.a = clamp(values.get("A"));
        stats.g = clamp(values.get("G"));
        stats.c = clamp(values.get("C"));
        stats.h = clamp(values.get("H"));
        stats.s = clamp(values.get("S"));
        stats.en = clamp(values.get("EN"));
        return stats;
    }

    public Map<String, Integer> values() { return toMap(); }

    /** 성적 G는 시험 평가로만 바뀌므로 일반 증감에서는 무시한다. */
    public void apply(Map<String, Integer> delta) {
        en = clamp(en + delta.getOrDefault("EN", 0));
        p = clamp(p + delta.getOrDefault("P", 0));
        f = clamp(f + delta.getOrDefault("F", 0));
        b = clamp(b + delta.getOrDefault("B", 0));
        i = clamp(i + delta.getOrDefault("I", 0));
        a = clamp(a + delta.getOrDefault("A", 0));
        c = clamp(c + delta.getOrDefault("C", 0));
        h = clamp(h + delta.getOrDefault("H", 0));
        s = clamp(s + delta.getOrDefault("S", 0));
    }

    public void changeGrade(int grade) {
        g = clamp(grade);
    }

    public int get(String stat) {
        return toMap().get(stat);
    }

    private static int clamp(int value) { return Math.max(0, Math.min(100, value)); }

    public Map<String, Integer> toMap() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("P", p);
        map.put("F", f);
        map.put("B", b);
        map.put("I", i);
        map.put("A", a);
        map.put("G", g);
        map.put("C", c);
        map.put("H", h);
        map.put("S", s);
        map.put("EN", en);
        return map;
    }

    public int getEn() { return en; }
    public int getP() { return p; }
    public int getF() { return f; }
    public int getB() { return b; }
    public int getI() { return i; }
    public int getA() { return a; }
    public int getG() { return g; }
    public int getC() { return c; }
    public int getH() { return h; }
    public int getS() { return s; }
}
