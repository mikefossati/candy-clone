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
                System.out.println("[makeMove] Invalid move attempted, board remains unchanged.");
                return game; // Do not mutate or save, return current state
            }
            // Swap tiles
            System.out.println("[makeMove] Swapping tiles: (" + fromRow + "," + fromCol + ") <-> (" + toRow + "," + toCol + ")");
            swap(board, fromRow, fromCol, toRow, toCol);
            printBoardDebug(board);

            int totalScore = game.getScore();
            boolean foundMatch;
            do {
                boolean[][] matched = findMatches(board);
                foundMatch = hasAnyMatch(matched);
                if (foundMatch) {
                    int cleared = clearMatches(board, matched);
                    System.out.println("[makeMove] Cleared " + cleared + " tiles:");
                    printBoardDebug(board);
                    totalScore += cleared * 10; // 10 points per cleared tile
                    cascadeTiles(board);
                    System.out.println("[makeMove] After cascade:");
                    printBoardDebug(board);
                }
            } while (foundMatch);

            game.setScore(totalScore);
            game.setBoard(objectMapper.writeValueAsString(board));
            return gameRepository.save(game);
        } catch (Exception e) {
            throw new RuntimeException("Error processing move", e);
        }
    }

    boolean isValidMove(Tile[][] board, int fromRow, int fromCol, int toRow, int toCol) {
        System.out.println("\n[isValidMove] Checking move: (" + fromRow + "," + fromCol + ") <-> (" + toRow + "," + toCol + ")");
        // Only adjacent tiles
        if (Math.abs(fromRow - toRow) + Math.abs(fromCol - toCol) != 1) {
            System.out.println("[isValidMove] Tiles are not adjacent. Move invalid.");
            return false;
        }
        // Work on a deep copy so the original board is not mutated
        Tile[][] boardCopy = deepCopyBoard(board);
        System.out.println("[isValidMove] Board before swap:");
        printBoardDebug(boardCopy);
        boolean[][] before = findMatches(boardCopy);
        swap(boardCopy, fromRow, fromCol, toRow, toCol);
        System.out.println("[isValidMove] Board after swap:");
        printBoardDebug(boardCopy);
        boolean[][] after = findMatches(boardCopy);
        // Only valid if swap creates a new match anywhere
        boolean valid = false;
        for (int i = 0; i < after.length; i++) {
            for (int j = 0; j < after[i].length; j++) {
                if (after[i][j] && !before[i][j]) {
                    valid = true;
                }
            }
        }
        System.out.println("[isValidMove] Move valid? " + valid);
        return valid;
    }

    // Helper to print the board for debugging
    private void printBoardDebug(Tile[][] board) {
        for (int i = 0; i < board.length; i++) {
            for (int j = 0; j < board[i].length; j++) {
                if (board[i][j] == null) {
                    System.out.print(". ");
                } else {
                    String c = board[i][j].getColor();
                    System.out.print((c.length() > 0 ? c.charAt(0) : '.') + " ");
                }
            }
            System.out.println();
        }
    }

    private void swap(Tile[][] board, int r1, int c1, int r2, int c2) {
        System.out.println("[swap] Swapping (" + r1 + "," + c1 + ") <-> (" + r2 + "," + c2 + ")");
        Tile temp = board[r1][c1];
        board[r1][c1] = board[r2][c2];
        board[r2][c2] = temp;
    }

    boolean[][] findMatches(Tile[][] board) {
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

    int clearMatches(Tile[][] board, boolean[][] matched) {
        int cleared = 0;
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (matched[i][j]) {
                    System.out.println("[clearMatches] Clearing tile at (" + i + "," + j + ")");
                    board[i][j] = null;
                    cleared++;
                }
            }
        }
        return cleared;
    }

    void cascadeTiles(Tile[][] board) {
        Random random = new Random();
        for (int col = 0; col < BOARD_SIZE; col++) {
            // Debug: Print column before cascading
            StringBuilder before = new StringBuilder();
            for (int row = 0; row < BOARD_SIZE; row++) {
                before.append(board[row][col] == null ? "." : board[row][col].getColor().charAt(0)).append(" ");
            }
            System.out.println("[cascadeTiles] Col " + col + " BEFORE: " + before.toString());

            // Step 1: Collect all non-null tiles in this column (from bottom to top)
            java.util.List<Tile> nonNullTiles = new java.util.ArrayList<>();
            for (int row = BOARD_SIZE - 1; row >= 0; row--) {
                if (board[row][col] != null) {
                    nonNullTiles.add(board[row][col]);
                }
            }
            // Step 2: Place non-null tiles at the bottom of the column
            int writeRow = BOARD_SIZE - 1;
            for (Tile tile : nonNullTiles) {
                board[writeRow][col] = tile;
                writeRow--;
            }
            // Step 3: Fill remaining cells at the top with new tiles
            for (; writeRow >= 0; writeRow--) {
                board[writeRow][col] = new Tile(COLORS[random.nextInt(COLORS.length)]);
                System.out.println("[cascadeTiles] New tile at (" + writeRow + "," + col + ") color: " + board[writeRow][col].getColor());
            }

            // Debug: Print column after cascading
            StringBuilder after = new StringBuilder();
            for (int r = 0; r < BOARD_SIZE; r++) {
                after.append(board[r][col] == null ? "." : board[r][col].getColor().charAt(0)).append(" ");
            }
            System.out.println("[cascadeTiles] Col " + col + " AFTER:  " + after.toString());
        }
    }

    private Tile[][] deepCopyBoard(Tile[][] board) {
        int n = board.length;
        int m = board[0].length;
        Tile[][] copy = new Tile[n][m];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                Tile t = board[i][j];
                if (t != null) {
                    Tile newTile = new Tile(t.getColor());
                    newTile.setType(t.getType());
                    copy[i][j] = newTile;
                } else {
                    copy[i][j] = null;
                }
            }
        }
        return copy;
    }

    private Tile[][] generateNewBoard() {
        Tile[][] board = new Tile[BOARD_SIZE][BOARD_SIZE];
        Random random = new Random();
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                java.util.List<String> possibleColors = new java.util.ArrayList<>(java.util.Arrays.asList(COLORS));
                // Remove color if it would create a horizontal match
                if (j >= 2 && board[i][j-1] != null && board[i][j-2] != null &&
                        board[i][j-1].getColor().equals(board[i][j-2].getColor())) {
                    possibleColors.remove(board[i][j-1].getColor());
                }
                // Remove color if it would create a vertical match
                if (i >= 2 && board[i-1][j] != null && board[i-2][j] != null &&
                        board[i-1][j].getColor().equals(board[i-2][j].getColor())) {
                    possibleColors.remove(board[i-1][j].getColor());
                }
                String color = possibleColors.get(random.nextInt(possibleColors.size()));
                board[i][j] = new Tile(color);
            }
        }
        // Optional: Debug check for accidental matches
        boolean[][] matched = findMatches(board);
        if (hasAnyMatch(matched)) {
            System.out.println("[generateNewBoard] WARNING: Initial board has matches!");
            printBoardDebug(board);
        }
        return board;
    }
}
