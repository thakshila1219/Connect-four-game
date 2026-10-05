package lk.ijse.dep.service;

public class BoardImpl implements Board {
    private final Piece[][] pieces = new Piece[NUM_OF_COLS][NUM_OF_ROWS];
    private final BoardUI boardUI;

    public BoardImpl(BoardUI boardUI) {
        this.boardUI = boardUI;

        for(int i = 0; i<pieces.length; i++) {
            for(int j = 0; j<pieces[i].length; j++) {
                pieces[i][j] = Piece.EMPTY;
            }
        }
    }

    @Override
    public Piece[][] getPieces() {
        return pieces;
    }

    @Override
    public BoardUI getBoardUI() {
        return boardUI;
    }

    @Override
    public int findNextAvailableSpot(int col) {

        for(int i = 0; i<NUM_OF_ROWS; i++) {
            if(pieces[col][i] == Piece.EMPTY) {
                return i;
            }
        }
        return -1;
    }

    public boolean isLegalMove(int col) {

        return findNextAvailableSpot(col) != -1;
    }

    public boolean existLegalMoves() {

        for(int col = 0; col<NUM_OF_COLS; col++) {
            if(isLegalMove(col)) {
                return true;
            }
        }
        return false;
    }

    public void updateMove(int col, Piece move) {
        int row = findNextAvailableSpot(col);
        if(row != -1) {
            pieces[col][row] = move;
        }
    }
    public void updateMove(int col, int row, Piece move) {
        pieces[col][row] = move;
    }

    public Winner findWinner() {
        for(int row = 0; row<NUM_OF_ROWS; row++) {
            for(int col = 0; col<=NUM_OF_COLS-4; col++) {
                Piece p = pieces[col][row];
                if(p != Piece.EMPTY &&
                        p == pieces[col+1][row] &&
                        p == pieces[col+2][row] &&
                        p == pieces[col+3][row]) {
                    return new Winner(p, col, row, col+3, row);
                }
            }
        }

        for(int col = 0; col<NUM_OF_COLS; col++) {
            for(int row = 0; row<=NUM_OF_ROWS-4; row++) {
                Piece p = pieces[col][row];
                if(p != Piece.EMPTY &&
                        p == pieces[col][row+1] &&
                        p == pieces[col][row+2] &&
                        p == pieces[col][row+3]) {
                    return new Winner(p, col, row, col,row+3);
                }
            }
        }
        return new Winner(Piece.EMPTY);
    }
}
