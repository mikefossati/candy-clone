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


import java.util.Random;

@Service
public class GameService {

    private static final int BOARD_SIZE = 8;
    private static final String[] COLORS = {"RED", "BLUE", "GREEN", "YELLOW", "PURPLE", "ORANGE"};

    private final GameRepository gameRepository;
    private final PlayerRepository playerRepository;
    private final ObjectMapper objectMapper;

    @Autowired
    public GameService(GameRepository gameRepository, PlayerRepository playerRepository) {
        this.gameRepository = gameRepository;
        this.playerRepository = playerRepository;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Public no-arg constructor for test use only.
     */
    public GameService() {
        this.gameRepository = null;
        this.playerRepository = null;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Creates a new game for the given player ID.
     * @param playerId the player ID
     * @return the created Game
     */
    public Game createNewGame(Long playerId) {
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new IllegalArgumentException("Player not found with id: " + playerId));
        Game game = new Game(player);
        Tile[][] board = generateNewBoard();
        try {
            String boardJson = objectMapper.writeValueAsString(board);
            game.setBoard(boardJson);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error processing board state", e);
        }
        return gameRepository.save(game);
    }

    /**
     * Retrieves the game state for the given game ID.
     * @param gameId the game ID
     * @return the Game
     */
    public Game getGameState(Long gameId) {
        return gameRepository.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("Game not found with id: " + gameId));
    }

    /**
     * Makes a move on the board for the given game ID and coordinates.
     * @param gameId the game ID
     * @param fromRow source row
     * @param fromCol source column
     * @param toRow destination row
     * @param toCol destination column
     * @return updated Game
     */
    public Game makeMove(Long gameId, int fromRow, int fromCol, int toRow, int toCol) {
        Game game = getGameState(gameId);
        try {
            Tile[][] board = objectMapper.readValue(game.getBoard(), Tile[][].class);
            if (!isValidMove(board, fromRow, fromCol, toRow, toCol)) {
                // removed logging "[makeMove] Invalid move attempted, board remains unchanged.");
                return game; // Do not mutate or save, return current state
            }
            // Swap tiles
            // removed logging "[makeMove] Swapping tiles: (" + fromRow + "," + fromCol + ") <-> (" + toRow + "," + toCol + ")");
            swap(board, fromRow, fromCol, toRow, toCol);
            // removed logging board);

            int totalScore = game.getScore();
            boolean foundMatch;
            do {
                boolean[][] matched = findMatches(board);
                foundMatch = hasAnyMatch(matched);
                if (foundMatch) {
                    int cleared = clearMatches(board, matched);
                    // removed logging "[makeMove] Cleared " + cleared + " tiles:");
                    // removed logging board);
                    totalScore += cleared * 10; // 10 points per cleared tile
                    cascadeTiles(board);
                    // removed logging "[makeMove] After cascade:");
                    // removed logging board);
                }
            } while (foundMatch);

            game.setScore(totalScore);
            game.setBoard(objectMapper.writeValueAsString(board));
            return gameRepository.save(game);
        } catch (Exception e) {
            throw new RuntimeException("Error processing move", e);
        }
    }

    /**
     * Checks if a move is valid (adjacent and results in a match).
     */
    boolean isValidMove(Tile[][] board, int fromRow, int fromCol, int toRow, int toCol) {
        // removed logging "\n[isValidMove] Checking move: (" + fromRow + "," + fromCol + ") <-> (" + toRow + "," + toCol + ")");
        // Only adjacent tiles
        if (Math.abs(fromRow - toRow) + Math.abs(fromCol - toCol) != 1) {
            // removed logging "[isValidMove] Tiles are not adjacent. Move invalid.");
            return false;
        }
        // Work on a deep copy so the original board is not mutated
        Tile[][] boardCopy = deepCopyBoard(board);
        // removed logging "[isValidMove] Board before swap:");
        // removed logging boardCopy);
        boolean[][] before = findMatches(boardCopy);
        swap(boardCopy, fromRow, fromCol, toRow, toCol);
        // removed logging "[isValidMove] Board after swap:");
        // removed logging boardCopy);
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
        // removed logging "[isValidMove] Move valid? " + valid);
        return valid;
    }



    /**
     * Swaps two tiles on the board.
     */
    private void swap(Tile[][] board, int r1, int c1, int r2, int c2) {
        // removed logging "[swap] Swapping (" + r1 + "," + c1 + ") <-> (" + r2 + "," + c2 + ")");
        Tile temp = board[r1][c1];
        board[r1][c1] = board[r2][c2];
        board[r2][c2] = temp;
    }

    /**
     * Finds all matches (3 or more in a row/column) on the board.
     */
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

    /**
     * Returns true if any matches are found on the board.
     */
    private boolean hasAnyMatch(boolean[][] matched) {
        for (int i = 0; i < BOARD_SIZE; i++)
            for (int j = 0; j < BOARD_SIZE; j++)
                if (matched[i][j]) return true;
        return false;
    }

    /**
     * Clears matched tiles and returns the number cleared.
     */
    int clearMatches(Tile[][] board, boolean[][] matched) {
        int cleared = 0;
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (matched[i][j]) {
                    // removed logging "[clearMatches] Clearing tile at (" + i + "," + j + ")");
                    board[i][j] = null;
                    cleared++;
                }
            }
        }
        return cleared;
    }

    /**
     * Cascades tiles down to fill empty spaces and fills from the top.
     */
    void cascadeTiles(Tile[][] board) {
        Random random = new Random();
        for (int col = 0; col < BOARD_SIZE; col++) {
            // Debug: Print column before cascading
            StringBuilder before = new StringBuilder();
            for (int row = 0; row < BOARD_SIZE; row++) {
                before.append(board[row][col] == null ? "." : board[row][col].getColor().charAt(0)).append(" ");
            }
            // removed logging "[cascadeTiles] Col " + col + " BEFORE: " + before.toString());

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
                // removed logging "[cascadeTiles] New tile at (" + writeRow + "," + col + ") color: " + board[writeRow][col].getColor());
            }

            // Debug: Print column after cascading
            StringBuilder after = new StringBuilder();
            for (int r = 0; r < BOARD_SIZE; r++) {
                after.append(board[r][col] == null ? "." : board[r][col].getColor().charAt(0)).append(" ");
            }
            // removed logging "[cascadeTiles] Col " + col + " AFTER:  " + after.toString());
        }
    }

    /**
     * Deep copies a board.
     */
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

    /**
     * Generates a new board with no initial matches.
     */
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
            // removed logging "[generateNewBoard] WARNING: Initial board has matches!");
            // removed logging board);
        }
        return board;
    }
}
