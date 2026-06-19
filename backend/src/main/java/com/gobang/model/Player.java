package com.gobang.model;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Player {
    private String id;
    private String name;
    private boolean isReady;
    private Integer color; // 1=black, 2=white, null=not assigned

    public Player(String id, String name) {
        this.id = id;
        this.name = name;
        this.isReady = false;
        this.color = null;
    }
}
