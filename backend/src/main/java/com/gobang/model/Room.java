package com.gobang.model;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Room {
    public enum RoomStatus {
        WAITING,
        PREPARING,
        PLAYING,
        FINISHED
    }

    private String id;
    private String name;
    private String ownerId;
    private List<Player> players;
    private RoomStatus status;
    private String gameId;
    private int maxPlayers;
    private long createdAt;

    public Room(String id, String name, String ownerId, String ownerName) {
        this.id = id;
        this.name = name;
        this.ownerId = ownerId;
        this.players = new ArrayList<>();
        this.players.add(new Player(ownerId, ownerName));
        this.status = RoomStatus.WAITING;
        this.gameId = null;
        this.maxPlayers = 2;
        this.createdAt = System.currentTimeMillis();
    }

    public boolean addPlayer(Player player) {
        if (players.size() >= maxPlayers) return false;
        if (players.stream().anyMatch(p -> p.getId().equals(player.getId()))) return false;
        players.add(player);
        if (players.size() == maxPlayers) {
            this.status = RoomStatus.PREPARING;
        }
        return true;
    }

    public void removePlayer(String playerId) {
        players.removeIf(p -> p.getId().equals(playerId));
        if (players.isEmpty()) {
            this.status = RoomStatus.FINISHED;
        } else if (ownerId.equals(playerId)) {
            this.ownerId = players.get(0).getId();
            if (players.size() < maxPlayers) {
                this.status = RoomStatus.WAITING;
            }
        } else if (players.size() < maxPlayers) {
            this.status = RoomStatus.WAITING;
        }
    }

    public boolean setPlayerReady(String playerId, boolean ready) {
        for (Player player : players) {
            if (player.getId().equals(playerId)) {
                player.setReady(ready);
                return true;
            }
        }
        return false;
    }

    public boolean areAllPlayersReady() {
        return players.size() >= 2 && players.stream().allMatch(Player::isReady);
    }

    public void assignColors() {
        if (players.size() >= 1) {
            players.get(0).setColor(1);
        }
        if (players.size() >= 2) {
            players.get(1).setColor(2);
        }
    }

    public Player getPlayer(String playerId) {
        return players.stream()
                .filter(p -> p.getId().equals(playerId))
                .findFirst()
                .orElse(null);
    }
}
