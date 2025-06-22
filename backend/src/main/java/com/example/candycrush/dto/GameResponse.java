package com.example.candycrush.dto;

import com.example.candycrush.model.Player;
import com.example.candycrush.model.Tile;

public class GameResponse {
    public Long id;
    public Player player;
    public Tile[][] board;
    public int score;

    public GameResponse(Long id, Player player, Tile[][] board, int score) {
        this.id = id;
        this.player = player;
        this.board = board;
        this.score = score;
    }
}
