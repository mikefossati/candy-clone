package com.example.candycrush.controller;

import com.example.candycrush.dto.MoveRequest;
import com.example.candycrush.dto.NewGameRequest;
import com.example.candycrush.model.Game;
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
    public Game startNewGame(@RequestBody NewGameRequest request) {
        return gameService.createNewGame(request.getPlayerId());
    }

    @GetMapping("/{id}")
    public Game getGameState(@PathVariable Long id) {
        return gameService.getGameState(id);
    }

    @PostMapping("/{id}/moves")
    public Game makeMove(@PathVariable Long id, @RequestBody MoveRequest request) {
        return gameService.makeMove(id, request.getFromRow(), request.getFromCol(), request.getToRow(), request.getToCol());
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
