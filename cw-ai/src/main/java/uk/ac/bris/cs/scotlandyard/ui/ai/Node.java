package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.graph.ImmutableValueGraph;
import uk.ac.bris.cs.scotlandyard.model.Board;
import uk.ac.bris.cs.scotlandyard.model.Move;
import uk.ac.bris.cs.scotlandyard.model.Piece;
import uk.ac.bris.cs.scotlandyard.model.Player;

import java.util.*;

public class Node {
    private Optional<Integer> score;   // score of node
    private int[] distances;   // distance from mrX to other points
    private int mrXLocation;
    private Node parent;  // parent node
    private boolean type;      // type of min or max node (0: min, 1 :max)
    private Piece pieceNearest;
    private ImmutableSet<Move> moves;
    public Board.GameState state;  // state of game

    public ImmutableSet<Piece> winner;

    Node(Board.GameState state, boolean type, Node parent, int mrXlocation,
         int [] distances, ImmutableValueGraph<Integer, Integer> graph)
    {
        this.state = state;
        this.type = type;
        this.mrXLocation = mrXlocation;
        this.parent = parent;
        this.distances = distances;
        // this.distances = ScoreCalculator.getDistances(graph, mrXlocation);
        this.moves = state.getAvailableMoves();
        this.winner = state.getWinner();
    }
    public ImmutableSet<Node> addChildren(ImmutableValueGraph<Integer, Integer> graph) {
        ImmutableSet.Builder<Node> builder = new ImmutableSet.Builder<>();
        pieceNearest = getPieceNearest(state);
        List<Move> bestMoves = MoveModifier.BestMove(state, moves, pieceNearest, type, mrXLocation, graph);
        for (Move move : bestMoves) {
            if (move.commencedBy().isMrX()) {  // mrX changes location
                int newLocation = MoveModifier.getDestination(move);
                builder.add(new Node(state.advance(move), !type, this, newLocation, ScoreCalculator.getDistances(graph, newLocation), graph));
            }else{
                builder.add(new Node(state.advance(move), !type, this, mrXLocation, ScoreCalculator.getDistances(graph, mrXLocation), graph));
            }
        }
        return builder.build();
    }
    private Piece getPieceNearest(Board.GameState state) {
        var pieces = state.getPlayers();
        int minDistance = 999;
        Piece piece = null;
        for (Piece p : pieces) {
            if (p.isMrX()) continue;
            int location = state.getDetectiveLocation((Piece.Detective) p).orElseThrow();
            if (distances[location] < minDistance) {
                minDistance = location;
                piece = p;
            }
        }
        return piece;
    }

    public static int calculateTotalDistance(int[] distances, Board.GameState state) {
        int result = 0;
        var pieces = state.getPlayers();
        for (Piece p : pieces) {
            if (p.isMrX()) continue;
            result += distances[state.getDetectiveLocation((Piece.Detective) p).orElseThrow()];
        }
        return result;
    }

    public void setScore(int score) { this.score = Optional.of(score); }
    public Optional<Integer> getScore() { return this.score; }
    public boolean getType() { return this.type; }

    public int getMrXLocation() { return this.mrXLocation; }
    public Node getParent() { return this.parent; }
    public Board.GameState getState() { return this.state; }
    public Piece getNearestPiece() { return this.pieceNearest; }
    public int[] getDistances() { return this.distances; }
    public int getFreedom() { return this.moves.size(); }
}
