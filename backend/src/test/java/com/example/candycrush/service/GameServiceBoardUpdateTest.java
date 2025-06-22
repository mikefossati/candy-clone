package com.example.candycrush.service;

import com.example.candycrush.model.Tile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class GameServiceBoardUpdateTest {
    GameService service;
    private Tile[][] board;
    private String[][] initialColors;
    private static final int BOARD_SIZE = 8;

    @BeforeEach
    void setup() {
        service = new GameService();
        board = new Tile[BOARD_SIZE][BOARD_SIZE];
        initialColors = new String[BOARD_SIZE][BOARD_SIZE];
        // Robust fill: guarantees no initial matches horizontally or vertically
        String[] baseColors = {"GREEN", "BLUE", "ORANGE", "PURPLE", "RED", "YELLOW"};
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                for (String color : baseColors) {
                    boolean match = false;
                    // Check horizontal match
                    if (j >= 2 && color.equals(board[i][j-1].getColor()) && color.equals(board[i][j-2].getColor())) {
                        match = true;
                    }
                    // Check vertical match
                    if (i >= 2 && color.equals(board[i-1][j].getColor()) && color.equals(board[i-2][j].getColor())) {
                        match = true;
                    }
                    if (!match) {
                        board[i][j] = new Tile(color);
                        initialColors[i][j] = color;
                        break;
                    }
                }
            }
        }
        // Debug: warn if any accidental matches exist
        boolean[][] matched = service.findMatches(board);
        boolean found = false;
        for (int x = 0; x < BOARD_SIZE; x++)
            for (int y = 0; y < BOARD_SIZE; y++)
                if (matched[x][y]) found = true;
        if (found) {}
    }

    @Test
    void testValidHorizontalMatchUpdatesOnlyAffectedTiles() {
        // Set up a board with a horizontal match after swap
        // Row 3: [BLUE, BLUE, RED, BLUE, RED, RED, RED, BLUE]
        // Explicitly set up a horizontal match at row 3: [*, RED, RED, RED, *, *, *, *]
        board[3][1] = new Tile("RED");
        board[3][2] = new Tile("RED");
        board[3][3] = new Tile("GREEN"); // Will swap with RED at (3,4)
        board[3][4] = new Tile("RED");
        // Swap (3,3) and (3,4) to create the match (3,2)-(3,4) all RED after swap
        boolean valid = service.isValidMove(board, 3, 3, 3, 4);
        assertTrue(valid, "Move should be valid and create a horizontal match");

        // Perform the swap explicitly
        Tile temp = board[3][3];
        board[3][3] = board[3][4];
        board[3][4] = temp;

        // Simulate clear and cascade
        boolean[][] matched = service.findMatches(board);
        int cleared = service.clearMatches(board, matched);
        assertEquals(3, cleared, "Should clear 3 tiles in row 3");
        service.cascadeTiles(board);

        // Only tiles outside the affected row (row 3) remain unchanged
        for (int i = 0; i < BOARD_SIZE; i++) {
            if (i == 3) continue; // Skip affected row
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (j >= 1 && j <= 3) continue; // Skip affected columns
                assertEquals(initialColors[i][j], board[i][j].getColor(), "Tiles outside affected row and columns should remain unchanged");
            }
        }
        // Debug print matched array
        boolean[][] matchedDebug = service.findMatches(board);
        System.out.print("[DEBUG] Matched (horizontal): ");
        for (int j = 0; j < BOARD_SIZE; j++) System.out.print((matchedDebug[3][j] ? "X" : ".") + " ");
        // removed logging );
    }

    @Test
    void testValidVerticalMatchUpdatesOnlyAffectedTiles() {
        // Set up a board with a vertical match after swap
        // Explicitly set up a vertical match at column 2: (2,2):YELLOW, (3,2):YELLOW, (4,2):GREEN, (5,2):YELLOW
        board[2][2] = new Tile("YELLOW");
        board[3][2] = new Tile("YELLOW");
        board[4][2] = new Tile("GREEN"); // Will swap with YELLOW at (5,2)
        board[5][2] = new Tile("YELLOW");
        // Swap (4,2) and (5,2) to create the match (2,2)-(4,2) all YELLOW after swap
        boolean valid = service.isValidMove(board, 4, 2, 5, 2);
        assertTrue(valid, "Move should be valid and create a vertical match");

        // Perform the swap explicitly
        Tile temp = board[4][2];
        board[4][2] = board[5][2];
        board[5][2] = temp;

        boolean[][] matched = service.findMatches(board);
        int cleared = service.clearMatches(board, matched);
        assertEquals(3, cleared, "Should clear 3 tiles in column 2");
        service.cascadeTiles(board);
        // Only tiles outside the affected column (col 2) remain unchanged
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (j == 2) continue; // Skip affected column
                assertEquals(initialColors[i][j], board[i][j].getColor(), "Tiles outside affected column should remain unchanged");
            }
        }
        // Debug print matched array
        boolean[][] matchedDebug = service.findMatches(board);
        System.out.print("[DEBUG] Matched (vertical): ");
        for (int i = 0; i < BOARD_SIZE; i++) System.out.print((matchedDebug[i][2] ? "X" : ".") + " ");
        // removed logging );
    }

    @Test
    void testNoAffectOnUnrelatedTiles() {
        // Set up a board with a match in one column only
        // Explicitly set up a vertical match at column 1: (3,1):BLUE, (4,1):BLUE, (5,1):GREEN, (6,1):BLUE
        board[2][1] = new Tile("RED"); // Not BLUE, prevents 4-in-a-row
        board[3][1] = new Tile("BLUE");
        board[4][1] = new Tile("BLUE");
        board[5][1] = new Tile("RED"); // Will swap with BLUE at (6,1)
        board[6][1] = new Tile("BLUE"); // After swap, rows 3-5 in col 1 are all BLUE
        board[7][1] = new Tile("RED"); // Not BLUE, prevents 4-in-a-row
        // Swap (5,1) and (6,1) to create the match (3,1)-(5,1) all BLUE after swap
        boolean valid = service.isValidMove(board, 5, 1, 6, 1);
        assertTrue(valid, "Move should be valid and create a vertical match");

        // Perform the swap explicitly
        Tile temp = board[5][1];
        board[5][1] = board[6][1];
        board[6][1] = temp;

        boolean[][] matched = service.findMatches(board);
        int cleared = service.clearMatches(board, matched);
        assertEquals(3, cleared, "Should clear 3 tiles in column 1");
        service.cascadeTiles(board);
        // Only tiles outside the affected column (col 1) remain unchanged
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (j == 1) continue; // Skip affected column
                assertEquals(initialColors[i][j], board[i][j].getColor(), "Tiles outside affected column should remain unchanged");
            }
        }
        // Debug print matched array
        boolean[][] matchedDebug = service.findMatches(board);
        System.out.print("[DEBUG] Matched (unrelated): ");
        for (int i = 0; i < BOARD_SIZE; i++) System.out.print((matchedDebug[i][1] ? "X" : ".") + " ");
        // removed logging );
    }
}
