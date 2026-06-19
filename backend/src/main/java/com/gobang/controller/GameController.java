package com.gobang.controller;

import com.gobang.model.GameState;
import com.gobang.model.Player;
import com.gobang.model.Room;
import com.gobang.service.AiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/game")
@CrossOrigin(origins = "*")
public class GameController {

    @Autowired
    private AiService aiService;

    private final Map<String, GameState> games = new ConcurrentHashMap<>();
    private final Map<String, Room> rooms = new ConcurrentHashMap<>();

    @PostMapping("/new")
    public ResponseEntity<GameState> newGame() {
        String id = UUID.randomUUID().toString();
        GameState game = new GameState(id);
        games.put(id, game);
        return ResponseEntity.ok(game);
    }

    @GetMapping("/{id}")
    public ResponseEntity<GameState> getGame(@PathVariable String id) {
        GameState game = games.get(id);
        if (game == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(game);
    }

    @PostMapping("/{id}/move")
    public ResponseEntity<?> makeMove(@PathVariable String id, @RequestBody Map<String, Integer> body) {
        GameState game = games.get(id);
        if (game == null) return ResponseEntity.notFound().build();
        if (game.getWinner() != null) return ResponseEntity.badRequest().body("Game is over");

        int row = body.getOrDefault("row", -1);
        int col = body.getOrDefault("col", -1);

        if (!game.placeStone(row, col)) {
            return ResponseEntity.badRequest().body("Invalid move");
        }
        return ResponseEntity.ok(game);
    }

    @PostMapping("/{id}/ai-move")
    public ResponseEntity<?> aiMove(@PathVariable String id, @RequestBody Map<String, Integer> body) {
        GameState game = games.get(id);
        if (game == null) return ResponseEntity.notFound().build();
        if (game.getWinner() != null) return ResponseEntity.badRequest().body("Game is over");

        int aiPlayer = body.getOrDefault("player", GameState.WHITE);
        int depth = body.getOrDefault("depth", 3);

        int[][] boardCopy = new int[GameState.BOARD_SIZE][GameState.BOARD_SIZE];
        for (int r = 0; r < GameState.BOARD_SIZE; r++) {
            System.arraycopy(game.getBoard()[r], 0, boardCopy[r], 0, GameState.BOARD_SIZE);
        }

        int[] move = aiService.getBestMove(boardCopy, aiPlayer, depth);
        if (move == null) return ResponseEntity.badRequest().body("No valid move");

        game.placeStone(move[0], move[1]);
        return ResponseEntity.ok(game);
    }

    @GetMapping("/records")
    public ResponseEntity<List<GameState>> getRecords() {
        List<GameState> finished = games.values().stream()
                .filter(g -> g.getWinner() != null)
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .toList();
        return ResponseEntity.ok(finished);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGame(@PathVariable String id) {
        games.remove(id);
        return ResponseEntity.ok().build();
    }

    // ==================== Room APIs ====================

    @PostMapping("/room/create")
    public ResponseEntity<Room> createRoom(@RequestBody Map<String, String> body) {
        String playerId = body.getOrDefault("playerId", UUID.randomUUID().toString());
        String playerName = body.getOrDefault("playerName", "玩家");
        String roomName = body.getOrDefault("roomName", playerName + "的房间");

        String roomId = UUID.randomUUID().toString();
        Room room = new Room(roomId, roomName, playerId, playerName);
        rooms.put(roomId, room);
        return ResponseEntity.ok(room);
    }

    @GetMapping("/room/list")
    public ResponseEntity<List<Room>> listRooms() {
        List<Room> roomList = rooms.values().stream()
                .filter(r -> r.getStatus() != Room.RoomStatus.FINISHED)
                .sorted((a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()))
                .toList();
        return ResponseEntity.ok(roomList);
    }

    @GetMapping("/room/{id}")
    public ResponseEntity<Room> getRoom(@PathVariable String id) {
        Room room = rooms.get(id);
        if (room == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(room);
    }

    @PostMapping("/room/{id}/join")
    public ResponseEntity<?> joinRoom(@PathVariable String id, @RequestBody Map<String, String> body) {
        Room room = rooms.get(id);
        if (room == null) return ResponseEntity.notFound().build();
        if (room.getStatus() == Room.RoomStatus.PLAYING || room.getStatus() == Room.RoomStatus.FINISHED) {
            return ResponseEntity.badRequest().body("房间已开始或已结束");
        }

        String playerId = body.getOrDefault("playerId", UUID.randomUUID().toString());
        String playerName = body.getOrDefault("playerName", "玩家");

        Player player = new Player(playerId, playerName);
        if (!room.addPlayer(player)) {
            return ResponseEntity.badRequest().body("房间已满或玩家已存在");
        }
        return ResponseEntity.ok(room);
    }

    @PostMapping("/room/{id}/leave")
    public ResponseEntity<?> leaveRoom(@PathVariable String id, @RequestBody Map<String, String> body) {
        Room room = rooms.get(id);
        if (room == null) return ResponseEntity.notFound().build();

        String playerId = body.get("playerId");
        if (playerId == null) return ResponseEntity.badRequest().body("缺少 playerId");

        room.removePlayer(playerId);
        if (room.getStatus() == Room.RoomStatus.FINISHED) {
            rooms.remove(id);
        }
        return ResponseEntity.ok(room);
    }

    @PostMapping("/room/{id}/ready")
    public ResponseEntity<?> setReady(@PathVariable String id, @RequestBody Map<String, Object> body) {
        Room room = rooms.get(id);
        if (room == null) return ResponseEntity.notFound().build();
        if (room.getStatus() == Room.RoomStatus.PLAYING || room.getStatus() == Room.RoomStatus.FINISHED) {
            return ResponseEntity.badRequest().body("游戏已开始或已结束");
        }

        String playerId = (String) body.get("playerId");
        Boolean ready = (Boolean) body.get("ready");
        if (playerId == null || ready == null) {
            return ResponseEntity.badRequest().body("缺少参数");
        }

        if (!room.setPlayerReady(playerId, ready)) {
            return ResponseEntity.badRequest().body("玩家不存在");
        }
        return ResponseEntity.ok(room);
    }

    @PostMapping("/room/{id}/start")
    public ResponseEntity<?> startGameFromRoom(@PathVariable String id, @RequestBody Map<String, String> body) {
        Room room = rooms.get(id);
        if (room == null) return ResponseEntity.notFound().build();

        String playerId = body.get("playerId");
        if (playerId == null) return ResponseEntity.badRequest().body("缺少 playerId");
        if (!room.getOwnerId().equals(playerId)) {
            return ResponseEntity.badRequest().body("只有房主可以开始游戏");
        }
        if (!room.areAllPlayersReady()) {
            return ResponseEntity.badRequest().body("有玩家未准备");
        }
        if (room.getPlayers().size() < 2) {
            return ResponseEntity.badRequest().body("需要至少2名玩家");
        }

        room.assignColors();

        String gameId = UUID.randomUUID().toString();
        GameState game = new GameState(gameId);
        games.put(gameId, game);

        room.setGameId(gameId);
        room.setStatus(Room.RoomStatus.PLAYING);

        Map<String, Object> result = new HashMap<>();
        result.put("room", room);
        result.put("game", game);
        return ResponseEntity.ok(result);
    }
}
