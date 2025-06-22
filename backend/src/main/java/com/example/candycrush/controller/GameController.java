package com.example.candycrush.controller;

import com.example.candycrush.dto.MoveRequest;
import com.example.candycrush.dto.NewGameRequest;
import com.example.candycrush.model.Game;
import com.example.candycrush.dto.GameResponse;
import com.example.candycrush.model.Tile;
import com.example.candycrush.service.GameService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/games")
@CrossOrigin(origins = "*")
public class GameController {

    @Autowired
    private GameService gameService;

    @PostMapping
    public GameResponse startNewGame(@RequestBody NewGameRequest request) {
        Game game = gameService.createNewGame(request.getPlayerId());
        Tile[][] board = parseBoard(game.getBoard());
        return new GameResponse(game.getId(), game.getPlayer(), board, game.getScore());
    }

    @GetMapping("/{id}")
    public GameResponse getGameState(@PathVariable Long id) {
        Game game = gameService.getGameState(id);
        Tile[][] board = parseBoard(game.getBoard());
        return new GameResponse(game.getId(), game.getPlayer(), board, game.getScore());
    }

    @PostMapping("/{id}/moves")
    public GameResponse makeMove(@PathVariable Long id, @RequestBody MoveRequest request) {
        Game game = gameService.makeMove(id, request.getFromRow(), request.getFromCol(), request.getToRow(), request.getToCol());
        Tile[][] board = parseBoard(game.getBoard());
        return new GameResponse(game.getId(), game.getPlayer(), board, game.getScore());
    }

    private Tile[][] parseBoard(String boardJson) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(boardJson, Tile[][].class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse board JSON", e);
        }
    }

    @Autowired
    private com.example.candycrush.repository.GameRepository gameRepository;

    @GetMapping("/leaderboard")
    public List<com.example.candycrush.controller.LeaderboardEntry> getLeaderboard() {
        return gameRepository.findAll().stream()
                .filter(g -> g.getPlayer() != null)
                .sorted((g1, g2) -> Integer.compare(g2.getScore(), g1.getScore()))
                .limit(10)
                .map(g -> new com.example.candycrush.controller.LeaderboardEntry(g.getPlayer().getName(), g.getScore()))
                .collect(Collectors.toList());
    }
}
