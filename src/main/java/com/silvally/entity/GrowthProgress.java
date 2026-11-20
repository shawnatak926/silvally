package com.silvally.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import org.hibernate.annotations.ColumnDefault;

import java.util.LinkedHashMap;
import java.util.Map;

/** 1점 미만으로 줄어든 양수 성장분을 1/1000점 단위로 누적한다. */
@Embeddable
public class GrowthProgress {
    public static final int UNIT = 1000;

    @ColumnDefault("0")
    @Column(name = "progress_p", nullable = false)
    private int p;
    @ColumnDefault("0")
    @Column(name = "progress_f", nullable = false)
    private int f;
    @ColumnDefault("0")
    @Column(name = "progress_b", nullable = false)
    private int b;
    @ColumnDefault("0")
    @Column(name = "progress_i", nullable = false)
    private int i;
    @ColumnDefault("0")
    @Column(name = "progress_a", nullable = false)
    private int a;
    @ColumnDefault("0")
    @Column(name = "progress_en", nullable = false)
    private int en;

    public int get(String stat) {
        return switch (stat) {
            case "P" -> p;
            case "F" -> f;
            case "B" -> b;
            case "I" -> i;
            case "A" -> a;
            case "EN" -> en;
            default -> 0;
        };
    }

    public void set(String stat, int value) {
        switch (stat) {
            case "P" -> p = value;
            case "F" -> f = value;
            case "B" -> b = value;
            case "I" -> i = value;
            case "A" -> a = value;
            case "EN" -> en = value;
            default -> throw new IllegalArgumentException("누적 성장 대상이 아닙니다: " + stat);
        }
    }

    public Map<String, Integer> toMap() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("P", p);
        map.put("F", f);
        map.put("B", b);
        map.put("I", i);
        map.put("A", a);
        map.put("EN", en);
        return map;
    }
}
