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
    private List<Move> bestMoves;
    public Board.GameState state;  // state of game

    public ImmutableSet<Piece> winner;

    Node(Board.GameState state, boolean type, Node parent, int mrXlocation,
         int [] distances)
    {
        this.state = state;
        this.type = type;  // false: max, true: min
        this.mrXLocation = mrXlocation;
        this.parent = parent;
        this.distances = distances;
        this.winner = state.getWinner();
        getPieceNearest(this.state);
        this.score = Optional.empty();
        if (!this.winner.isEmpty()) {
            if (this.winner.asList().get(0).isMrX()) this.score = Optional.of(999);
            else this.score = Optional.of(-999);
            return;
        }
        this.moves = state.getAvailableMoves();
    }
    public ImmutableSet<Node> addChildren(ImmutableValueGraph<Integer, Integer> graph) {
        if (this.type) {
            state = MoveModifier.moveAllExceptNearestPiece(this.state, this.distances, pieceNearest);
            // assert this.state.getAvailableMoves().asList().get(0).commencedBy().isMrX();
        }
        this.bestMoves = MoveModifier.BestMove(state, moves, pieceNearest, type, mrXLocation, graph);
        //System.out.println(bestMoves);
        ImmutableSet.Builder<Node> builder = new ImmutableSet.Builder<>();
        for (Move move : bestMoves) {
            if (move.commencedBy().isMrX()) {  // mrX changes location
                int newLocation = MoveModifier.getDestination(move);
                builder.add(new Node(state.advance(move), !type, this, newLocation, ScoreCalculator.getDistances(graph, newLocation)));
            }else{
                builder.add(new Node(state.advance(move), !type, this, mrXLocation, distances));
            }
        }
        return builder.build();
    }
    private void getPieceNearest(Board.GameState state) {
        var pieces = state.getPlayers();
        int minDistance = 999;
        Piece piece = null;
        for (Piece p : pieces) {
            if (p.isMrX()) continue;
            int location = state.getDetectiveLocation((Piece.Detective) p).orElseThrow();
            if (distances[location - 1] < minDistance) {
                minDistance = distances[location - 1];
                piece = p;
            }
        }
        pieceNearest = piece;
    }
    public void setScore(int score) {
        if (this.score.orElse(998) != 999 && this.score.orElse(998) != -999){
            this.score = Optional.of(score);
        }
    }
    public Optional<Integer> getScore() { return this.score; }
    public boolean getType() { return this.type; }
    public Node getParent() { return this.parent; }
    public Piece getNearestPiece() { return this.pieceNearest; }
    public int[] getDistances() { return this.distances; }
    public int getFreedom() {
        if (this.moves == null) return 0;
        return this.moves.size();
    }

    public List<Move> getBestMoves() { return this.bestMoves;}
}
