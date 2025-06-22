package com.example.candycrush;

import com.example.candycrush.model.Tile;
import com.example.candycrush.service.GameService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class MoveValidationTest {
    GameService service;
    private Tile[][] board;

    @BeforeEach
    void setup() {
        service = new GameService();
        board = new Tile[8][8];
        // Fill board with alternating colors to prevent accidental matches
        String[] colors = {"RED", "GREEN", "ORANGE", "PURPLE"};
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                board[i][j] = new Tile(colors[(i + j) % colors.length]);
            }
        }
        // Now, override specific tiles for each test scenario in the tests themselves
    }

    @Test
    void testNonAdjacentMoveIsInvalid() {
        assertFalse(callIsValidMove(0,0,2,2));
    }

    @Test
    void testAdjacentButNoMatchIsInvalid() {
        // Set up a non-matching scenario
        board[0][2] = new Tile("GREEN");
        board[0][3] = new Tile("BLUE");
        // Swapping (0,2) and (0,3) does not create a match
        assertFalse(callIsValidMove(0,2,0,3));
    }

    @Test
    void testValidHorizontalMatchMove() {
        // Set up: [BLUE, BLUE, RED, BLUE]
        board[0][0] = new Tile("BLUE");
        board[0][1] = new Tile("BLUE");
        board[0][2] = new Tile("RED");
        board[0][3] = new Tile("BLUE");
        // Swapping (0,2) and (0,3) creates [BLUE, BLUE, BLUE, RED], which is a match
        assertTrue(callIsValidMove(0,2,0,3));
    }

    @Test
    void testValidVerticalMatchMove() {
        // Set up so that swapping (2,0) and (2,1) creates a vertical match at column 0
        // Clear possible accidental matches
        for (int i = 0; i < 8; i++) {
            board[i][0] = new Tile("RED");
            board[2][i] = new Tile("RED");
        }
        // Set up only the relevant tiles for the vertical match (pre-swap)
        board[0][0] = new Tile("BLUE");
        board[1][0] = new Tile("BLUE");
        board[2][0] = new Tile("RED");
        board[2][1] = new Tile("BLUE");
        // Swap (2,0) and (2,1) should create a vertical match at column 0
        assertTrue(callIsValidMove(2,0,2,1));
    }

    private boolean callIsValidMove(int fr, int fc, int tr, int tc) {
        try {
            java.lang.reflect.Method m = GameService.class.getDeclaredMethod("isValidMove", Tile[][].class, int.class, int.class, int.class, int.class);
            m.setAccessible(true);
            return (boolean)m.invoke(service, board, fr, fc, tr, tc);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void printBoard(String label) {
        // removed logging label);
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                System.out.print(board[i][j].getColor().charAt(0) + " ");
            }
            // removed logging );
        }
        // removed logging );
    }

    @Test
    void testValidEdgeOfBoardHorizontalMatch() {
        // Set all of row 0 to GREEN
        for (int i = 0; i < 8; i++) board[0][i] = new Tile("GREEN");
        // Set up: [GREEN, GREEN, GREEN, GREEN, BLUE, RED, BLUE, BLUE]
        board[0][4] = new Tile("BLUE");
        board[0][5] = new Tile("RED");
        board[0][6] = new Tile("BLUE");
        board[0][7] = new Tile("BLUE");
        printBoard("Before swap (horizontal edge)");
        // Swap (0,4)-(0,5) to create three BLUEs at the edge
        Tile temp = board[0][4]; board[0][4] = board[0][5]; board[0][5] = temp;
        printBoard("After swap (horizontal edge)");
        // Swap back for isValidMove call
        temp = board[0][4]; board[0][4] = board[0][5]; board[0][5] = temp;
        boolean valid = callIsValidMove(0,4,0,5);
        // removed logging "isValidMove result (horizontal edge): " + valid);
        assertTrue(valid);
    }

    @Test
    void testValidEdgeOfBoardVerticalMatch() {
        // Set all of col 7 to GREEN
        for (int i = 0; i < 8; i++) board[i][7] = new Tile("GREEN");
        // Set up: [GREEN, GREEN, GREEN, GREEN, BLUE, RED, BLUE, BLUE] in col 7
        board[4][7] = new Tile("BLUE");
        board[5][7] = new Tile("RED");
        board[6][7] = new Tile("BLUE");
        board[7][7] = new Tile("BLUE");
        printBoard("Before swap (vertical edge)");
        // Swap (4,7)-(5,7) to create three BLUEs at the bottom edge
        Tile temp = board[4][7]; board[4][7] = board[5][7]; board[5][7] = temp;
        printBoard("After swap (vertical edge)");
        // Swap back for isValidMove call
        temp = board[4][7]; board[4][7] = board[5][7]; board[5][7] = temp;
        boolean valid = callIsValidMove(4,7,5,7);
        // removed logging "isValidMove result (vertical edge): " + valid);
        assertTrue(valid);
    }

    @Test
    void testValidLShapedMatch() {
        // Set all tiles to GREEN to avoid accidental matches
        for (int i = 0; i < 8; i++) {
            board[3][i] = new Tile("GREEN");
            board[i][4] = new Tile("GREEN");
        }
        // Set up L-shape: row 3 [GREEN, GREEN, BLUE, BLUE, GREEN], col 4 [GREEN, BLUE, GREEN, GREEN, BLUE]
        board[3][2] = new Tile("BLUE");
        board[3][3] = new Tile("BLUE");
        board[3][4] = new Tile("GREEN");
        board[2][4] = new Tile("BLUE");
        board[4][4] = new Tile("BLUE");
        // Swapping (2,4) and (3,4) creates an L-shape of BLUEs at (3,2)-(3,4) and (2,4)-(4,4)
        assertTrue(callIsValidMove(2,4,3,4));
    }

    @Test
    void testValidTShapedMatch() {
        // Clear accidental matches
        for (int i = 0; i < 8; i++) {
            board[4][i] = new Tile("GREEN");
            board[i][3] = new Tile("GREEN");
        }
        // Set up T-shape: row 4 [GREEN, GREEN, BLUE, BLUE, BLUE, GREEN], col 3 [GREEN, GREEN, BLUE, GREEN, BLUE, GREEN]
        board[4][2] = new Tile("BLUE");
        board[4][3] = new Tile("BLUE");
        board[4][4] = new Tile("BLUE");
        board[2][3] = new Tile("BLUE");
        board[4][5] = new Tile("GREEN");
        // Swapping (2,3) and (3,3) creates a T-shape of BLUEs at (4,2)-(4,4) and (2,3)-(4,3)
        assertTrue(callIsValidMove(2,3,3,3));
    }
}

