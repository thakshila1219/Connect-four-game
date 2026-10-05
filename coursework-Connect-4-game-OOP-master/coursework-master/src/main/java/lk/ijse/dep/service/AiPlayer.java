package lk.ijse.dep.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.lang.Math;

/*
 * This class represents the AI player that uses the Monte Carlo Tree Search (MCTS)
 * algorithm to decide its next move in the connect 4 game.
 */

public class AiPlayer extends Player {
    // Game board size (6 column x 5 rows)
    public static final int NUM_OF_COLS = 6;
    public static final int NUM_OF_ROWS = 5;

    // Constant for UCT formula
    private static final double c = Math.sqrt(2);

    // Number of simulations
    private static final int ITERATIONS = 100;

    public AiPlayer(Board newBoard) {
        super(newBoard);
    }

    /*
     * This is the main method that the game calls when it is the AI's turn.
     * It uses MCTS to find the best column to drop the AI's piece.
     */
    public void movePiece(int col) {
        Winner winner;

        // Create root node of the MCTS tree using the current board state
        // Last move was BLUE (human player), so AI (GREEN) plays next
        Node root = new Node(board.getPieces(), -1, Piece.BLUE, null);

        // Runs the MCTS algorithm to find the best move
        Node bestNode = MCTSAlgorithm.getBestNode(root, ITERATIONS);

        // Choose column
        int aiCol;
        if (bestNode != null && board.isLegalMove(bestNode.column)) {
            aiCol = bestNode.column;
        } else {
            List<Integer> legal = getLegalMoves(board.getPieces());
            aiCol = legal.isEmpty() ? -1 : legal.get(0);
        }

        // Make the move on board if valid
        if (aiCol != -1) {
            board.updateMove(aiCol, Piece.GREEN);
            board.getBoardUI().update(aiCol, false);
        }

        // After the move, check for winner or tie.
        winner = board.findWinner();
        if (winner.getWinningPiece() != Piece.EMPTY) {
            board.getBoardUI().notifyWinner(winner);
        } else if (!board.existLegalMoves()) {
            board.getBoardUI().notifyWinner(new Winner(Piece.EMPTY));
        }
    }


    //Inner class that implements the actual MCTS algorithm.
    //All methods are static. they are do not depend on an AiPlayer instance

    private static class MCTSAlgorithm {
        static Random ran = new Random();


        //UCT formula
        //It balances exploration and exploitation

        private static double uctValue(Node node, int parentVisits) {
            if (node.visits == 0)
                return Double.POSITIVE_INFINITY;
            double exploitation = node.wins / (double) node.visits;
            double exploration = c * Math.sqrt(Math.log(Math.max(1, parentVisits)) / (double) node.visits);
            return exploitation + exploration;
        }

        // Runs the full MCTS proces for a given number of iterations.
        public static Node getBestNode(Node parent, int iterations) {
            for (int i = 0; i < iterations; i++) {

                // 1 Selection
                Node node = parent;
                while (!node.children.isEmpty() && !isTerminal(node.gameState)) {
                    Node bestChild = null;
                    double bestUctValue = Double.NEGATIVE_INFINITY;
                    for (Node child : node.children) {
                        double uct = uctValue(child, node.visits);
                        if (uct > bestUctValue) {
                            bestUctValue = uct;
                            bestChild = child;
                        }
                    }
                    if (bestChild == null)
                        break;
                    node = bestChild;
                }
                // 2 Expansion
                if (!isTerminal(node.gameState)) {
                    expandNode(node);
                }
                // 3 Simulation
                Node nodeToExplore = node;
                if (!node.children.isEmpty()) {
                    nodeToExplore = node.children.get(ran.nextInt(node.children.size()));
                }

                double playoutResult = simulateRandomPlayout(nodeToExplore, Piece.GREEN);

                // 4 Backpropagation
                backPropagate(nodeToExplore, playoutResult, Piece.GREEN);
            }

            // After all itterations, choose the best child
            Node bestChild = null;
            double bestWinRate = Double.NEGATIVE_INFINITY;
            int bestVisits = -1;
            for (Node child : parent.children) {
                double winRate = (child.visits == 0) ? 0.0 : (child.wins / child.visits);
                if (winRate > bestWinRate || (winRate == bestWinRate && child.visits > bestVisits)) {
                    bestWinRate = winRate;
                    bestVisits = child.visits;
                    bestChild = child;
                }
            }
            if (bestChild == null && !parent.children.isEmpty()) {
                bestChild = parent.children.get(0);
            }
            return bestChild;
        }


        //Update win/visit counts for nodes going up the tree (after simulations)

        private static void backPropagate(Node node, double result, Piece aiPiece) {
            Node temp = node;
            while (temp != null) {
                temp.visits++;
                // If this node represent AI move, add full result -> 1 for win, 0 for lose
                // If opponent move, add inverse (1-result)
                if (temp.player == aiPiece) {
                    temp.wins += result;
                } else if (temp.player == getOpponent(aiPiece)) {
                    temp.wins += (1.0 - result);
                } else {
                    temp.wins += result;
                }
                temp = temp.parent;
            }
        }

        /*
         * Randomly plays the game from a node until terminate (win/loss/draw)
         * return the result
         */
        private static double simulateRandomPlayout(Node node, Piece aiPiece) {
            Piece[][] tempState = duplicateBoard(node.gameState);
            Piece currentPlayer = getOpponent(node.player); // next to move

            // if node is terminal, return immediate reward
            if (isTerminal(tempState)) {
                Piece winner = getWinner(tempState);
                if (winner == aiPiece)
                    return 1.0; // win
                if (winner == Piece.EMPTY)
                    return 0.5; // draw
                return 0.0; // lose
            }

            // play random moves until terminate
            while (!isTerminal(tempState)) {
                List<Integer> moves = getLegalMoves(tempState);
                int move = moves.get(ran.nextInt(moves.size()));
                makeMoveInPlace(tempState, currentPlayer, move);
                currentPlayer = getOpponent(currentPlayer);
            }

            Piece winner = getWinner(tempState);
            if (winner == aiPiece)
                return 1.0;
            if (winner == Piece.EMPTY)
                return 0.5;
            return 0.0;
        }

        // Expands a node by generating all possible next moves
        private static void expandNode(Node node) {
            List<Integer> legalMoves = getLegalMoves(node.gameState);
            for (int col : legalMoves) {
                // if already expanded for this column, skip
                boolean exists = false;
                for (Node ch : node.children) {
                    if (ch.column == col) {
                        exists = true;
                        break; // stop checking once found
                    }
                }
                if (exists)
                    continue;

                // copy board and make moves for child node
                Piece[][] newState = duplicateBoard(node.gameState);
                int newMoveCol = col;
                Piece currentPlayer = getOpponent(node.player);
                makeMoveInPlace(newState, currentPlayer, col);
                Node child = new Node(newState, newMoveCol, currentPlayer, node);
                node.children.add(child);
            }
        }

        // make a move directly in a given board state
        private static void makeMoveInPlace(Piece[][] newState, Piece player, int col) {

            int availableRow = findNextAvailableSpot(newState, col);
            if (availableRow != -1) {
                newState[col][availableRow] = player;
            }
        }

    }

    // board handling
    public static List<Integer> getLegalMoves(Piece[][] state) {
        ArrayList<Integer> availableMoves = new ArrayList<Integer>();
        for (int i = 0; i < NUM_OF_COLS; i++) {
            int spot = findNextAvailableSpot(state, i);
            if (spot == -1) {
                continue;
            }
            availableMoves.add(i);
        }
        return availableMoves;
    }

    public static int findNextAvailableSpot(Piece[][] state, int col) {

        for (int i = 0; i < NUM_OF_ROWS; i++) {
            if (state[col][i] == (Piece.EMPTY)) {
                return i;
            }
        }
        return -1;
    }

    public static Piece[][] duplicateBoard(Piece[][] status) {
        Piece[][] temp = new Piece[NUM_OF_COLS][NUM_OF_ROWS];
        for (int i = 0; i < NUM_OF_COLS; i++) {
            for (int j = 0; j < NUM_OF_ROWS; j++) {
                temp[i][j] = status[i][j];
            }
        }
        return temp;
    }

    public static boolean makeMoveInPlace(Piece[][] board, int col, Piece player) {
        for (int r = 0; r < NUM_OF_ROWS; r++) { // 0 is bottom
            if (board[col][r] == Piece.EMPTY) {
                board[col][r] = player;
                return true;
            }
        }
        return false; // column full
    }

    public static boolean isTerminal(Piece[][] board) {
        return getWinner(board) != null || getLegalMoves(board).isEmpty();
    }

    // check winner
    public static Piece getWinner(Piece[][] pieces) {
        // Horizontal
        for (int row = 0; row < NUM_OF_ROWS; row++) {
            for (int col = 0; col <= NUM_OF_COLS - 4; col++) {
                Piece p = pieces[col][row];
                if (p != Piece.EMPTY &&
                        p == pieces[col + 1][row] &&
                        p == pieces[col + 2][row] &&
                        p == pieces[col + 3][row]) {
                    return p;
                }
            }
        }

        // Vertical
        for (int col = 0; col < NUM_OF_COLS; col++) {
            for (int row = 0; row <= NUM_OF_ROWS - 4; row++) {
                Piece p = pieces[col][row];
                if (p != Piece.EMPTY &&
                        p == pieces[col][row + 1] &&
                        p == pieces[col][row + 2] &&
                        p == pieces[col][row + 3]) {
                    return p;
                }
            }
        }

        // if no moves left, it is a draw
        if (getLegalMoves(pieces).isEmpty())
            return Piece.EMPTY;
        return null; // still running game
    }

    private static Piece getOpponent(Piece p) {
        if (p == Piece.BLUE)
            return Piece.GREEN;
        if (p == Piece.GREEN)
            return Piece.BLUE;
        return Piece.EMPTY; // for unmatched, but shouldn't happen
    }

    /*
     * Node class represents one state in the MCTS tree.
     * Each node knows its board, parent, move, and statistics.
     */
    private static class Node {
        Piece[][] gameState;
        Node parent;
        int column;
        ArrayList<Node> children = new ArrayList<Node>();
        int visits;
        double wins;
        Piece player;

        public Node(Piece[][] status, int column, Piece player, Node parent) {
            this.gameState = duplicateBoard(status);
            this.column = column;
            this.player = player;
            this.parent = parent;
        }

        public boolean isFullyExpanded() {
            List<Integer> legal = getLegalMoves(gameState);
            return children.size() == legal.size();
        }

        public boolean isLeaf() {
            return children.isEmpty();
        }
    }
}