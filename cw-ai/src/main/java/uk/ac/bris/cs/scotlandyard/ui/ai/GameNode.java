package uk.ac.bris.cs.scotlandyard.ui.ai;
import com.google.common.collect.ImmutableList;
import com.google.common.graph.ImmutableValueGraph;

import com.google.common.collect.ImmutableSet;
import uk.ac.bris.cs.scotlandyard.model.*;

import javax.swing.plaf.nimbus.State;
import java.util.List;
import java.util.Optional;
/*
public class GameNode {
    private Optional<Integer> score = Optional.empty();
    private int mrXLocation;
    private Piece pieceNearest;
    private int NearestLocation;
    private ImmutableSet<Move> moves;
    private boolean type;    // type of min or max node (0: min, 1 :max)
    private GameNode parent;  // parent node
    public ImmutableSet<Piece> winner;
    public Board.GameState state;  // state of game


    GameNode(Board.GameState state, boolean type, GameNode parent, int mrXlocation, ImmutableValueGraph<Integer, Integer> graph) {
        this.state = state;
        this.type = type;
        this.parent = parent;
        this.moves = state.getAvailableMoves();  // reduce running time
        this.winner = state.getWinner();
        this.mrXLocation = mrXlocation;
        if (winner.isEmpty()) {
            this.pieceNearest = StateCreator.findNearestDetective(state, graph, moves);
            this.NearestLocation = state.getDetectiveLocation((Piece.Detective) pieceNearest).orElseThrow();
        }
    }

    public ImmutableSet<GameNode> addChildren(ImmutableValueGraph<Integer, Integer> graph) {
        ImmutableSet.Builder<GameNode> builder = new ImmutableSet.Builder<>();
        // use moveModifier to find best move of each destination
        List<Move> bestMoves = StateCreator.BestMove(graph, state, moves, pieceNearest, type);
        // if it is mrX's turn
        boolean flag = bestMoves.get(0).commencedBy().isMrX();
        for (var move : bestMoves) {
            if (!flag) builder.add(new GameNode(state, !type, this, mrXLocation, graph));
            else {
                var newLocation = StateCreator.getDestination(move);
                builder.add(new GameNode(state, !type, this, newLocation, graph));
            }
        }
        return builder.build();
    }
    private Piece getPieceNearest(int[] distance, ImmutableList<Player> players) {
        int minDistance = 999;
        Piece piece = null;
        for (Player p : players) {
            if (!p.isMrX() && distance[p.location()] < minDistance) {
                minDistance = p.location();
                piece = p.piece();
            }
        }
        return piece;
    }

    // setter
    public void setScore(int score) { this.score = Optional.of(score); }
    // getter
    public Optional<Integer> getScore() { return this.score; }
    public boolean getType() { return this.type; }

    public int getMrXLocation() { return this.mrXLocation; }
    public GameNode getParent() { return this.parent; }
    public Board.GameState getState() { return this.state; }
    public int getNearestLocation() { return this.NearestLocation; }
}


 */