package com.example.candycrush.service;

import com.example.candycrush.model.Game;
import com.example.candycrush.model.Player;
import com.example.candycrush.model.Tile;
import com.example.candycrush.repository.GameRepository;
import com.example.candycrush.repository.PlayerRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Random;

@Service
public class GameService {

    private static final int BOARD_SIZE = 8;
    private static final String[] COLORS = {"RED", "BLUE", "GREEN", "YELLOW", "PURPLE", "ORANGE"};

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private PlayerRepository playerRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public Game createNewGame(Long playerId) {
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new IllegalArgumentException("Player not found with id: " + playerId));

        Game game = new Game(player);
        Tile[][] board = generateNewBoard();
        try {
            String boardJson = objectMapper.writeValueAsString(board);
            game.setBoard(boardJson);
        } catch (JsonProcessingException e) {
            // In a real app, handle this more gracefully
            throw new RuntimeException("Error processing board state", e);
        }

        return gameRepository.save(game);
    }

    public Game getGameState(Long gameId) {
        return gameRepository.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("Game not found with id: " + gameId));
    }

    public Game makeMove(Long gameId, int fromRow, int fromCol, int toRow, int toCol) {
        Game game = getGameState(gameId);
        try {
            Tile[][] board = objectMapper.readValue(game.getBoard(), Tile[][].class);
            if (!isValidMove(board, fromRow, fromCol, toRow, toCol)) {
                throw new IllegalArgumentException("Invalid move");
            }
            // Swap tiles
            Tile temp = board[fromRow][fromCol];
            board[fromRow][fromCol] = board[toRow][toCol];
            board[toRow][toCol] = temp;

            int totalScore = game.getScore();
            boolean foundMatch;
            do {
                boolean[][] matched = findMatches(board);
                foundMatch = hasAnyMatch(matched);
                if (foundMatch) {
                    int cleared = clearMatches(board, matched);
                    totalScore += cleared * 10; // 10 points per cleared tile
                    cascadeTiles(board);
                }
            } while (foundMatch);

            game.setScore(totalScore);
            game.setBoard(objectMapper.writeValueAsString(board));
            return gameRepository.save(game);
        } catch (Exception e) {
            throw new RuntimeException("Error processing move", e);
        }
    }

    private boolean isValidMove(Tile[][] board, int fromRow, int fromCol, int toRow, int toCol) {
        // Only adjacent tiles
        if (Math.abs(fromRow - toRow) + Math.abs(fromCol - toCol) != 1) return false;
        // Find matches before swap
        boolean[][] before = findMatches(board);
        // Swap and find matches after
        swap(board, fromRow, fromCol, toRow, toCol);
        boolean[][] after = findMatches(board);
        swap(board, fromRow, fromCol, toRow, toCol); // swap back
        // Only valid if swap creates a new match anywhere
        for (int i = 0; i < after.length; i++) {
            for (int j = 0; j < after[i].length; j++) {
                if (after[i][j] && !before[i][j]) {
                    return true;
                }
            }
        }
        return false;
    }

    private void swap(Tile[][] board, int r1, int c1, int r2, int c2) {
        Tile temp = board[r1][c1];
        board[r1][c1] = board[r2][c2];
        board[r2][c2] = temp;
    }

    private boolean[][] findMatches(Tile[][] board) {
        boolean[][] matched = new boolean[BOARD_SIZE][BOARD_SIZE];
        // Horizontal
        for (int i = 0; i < BOARD_SIZE; i++) {
            int count = 1;
            for (int j = 1; j < BOARD_SIZE; j++) {
                if (board[i][j] != null && board[i][j-1] != null && board[i][j].getColor().equals(board[i][j-1].getColor())) {
                    count++;
                } else {
                    if (count >= 3) for (int k = 0; k < count; k++) matched[i][j-1-k] = true;
                    count = 1;
                }
            }
            if (count >= 3) for (int k = 0; k < count; k++) matched[i][BOARD_SIZE-1-k] = true;
        }
        // Vertical
        for (int j = 0; j < BOARD_SIZE; j++) {
            int count = 1;
            for (int i = 1; i < BOARD_SIZE; i++) {
                if (board[i][j] != null && board[i-1][j] != null && board[i][j].getColor().equals(board[i-1][j].getColor())) {
                    count++;
                } else {
                    if (count >= 3) for (int k = 0; k < count; k++) matched[i-1-k][j] = true;
                    count = 1;
                }
            }
            if (count >= 3) for (int k = 0; k < count; k++) matched[BOARD_SIZE-1-k][j] = true;
        }
        return matched;
    }

    private boolean hasAnyMatch(boolean[][] matched) {
        for (int i = 0; i < BOARD_SIZE; i++)
            for (int j = 0; j < BOARD_SIZE; j++)
                if (matched[i][j]) return true;
        return false;
    }

    private int clearMatches(Tile[][] board, boolean[][] matched) {
        int cleared = 0;
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (matched[i][j]) {
                    board[i][j] = null;
                    cleared++;
                }
            }
        }
        return cleared;
    }

    private void cascadeTiles(Tile[][] board) {
        Random random = new Random();
        for (int j = 0; j < BOARD_SIZE; j++) {
            int empty = BOARD_SIZE - 1;
            for (int i = BOARD_SIZE - 1; i >= 0; i--) {
                if (board[i][j] != null) {
                    board[empty][j] = board[i][j];
                    if (empty != i) board[i][j] = null;
                    empty--;
                }
            }
            // Fill new tiles at the top
            for (int i = empty; i >= 0; i--) {
                board[i][j] = new Tile(COLORS[random.nextInt(COLORS.length)]);
            }
        }
    }

    private Tile[][] generateNewBoard() {
        Tile[][] board = new Tile[BOARD_SIZE][BOARD_SIZE];
        Random random = new Random();

        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                // Simple random generation. A real implementation would prevent initial matches.
                board[i][j] = new Tile(COLORS[random.nextInt(COLORS.length)]);
            }
        }
        return board;
    }
}
