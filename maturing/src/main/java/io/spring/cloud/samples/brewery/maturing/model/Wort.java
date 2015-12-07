package io.spring.cloud.samples.brewery.maturing.model;

import lombok.Data;

@Data
public class Wort {
    private int wort;

    public Wort(int wort) {
        this.wort = wort;
    }
}
