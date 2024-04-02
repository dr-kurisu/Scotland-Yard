package uk.ac.bris.cs.scotlandyard.ui.ai;
import com.google.common.graph.ImmutableValueGraph;

import com.google.common.collect.ImmutableSet;
import uk.ac.bris.cs.scotlandyard.model.Board;
import uk.ac.bris.cs.scotlandyard.model.Move;
import uk.ac.bris.cs.scotlandyard.model.Piece;

import javax.swing.plaf.nimbus.State;
import java.util.List;
import java.util.Optional;

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


    GameNode(Board.GameState state, boolean type, GameNode parent, ImmutableValueGraph<Integer, Integer> graph) {
        this.state = state;
        this.type = type;
        this.parent = parent;
        this.moves = state.getAvailableMoves();  // reduce running time
        this.winner = state.getWinner();
        if (winner.isEmpty()) {
            this.mrXLocation = StateCreator.getMrXLocation(moves);
            this.pieceNearest = StateCreator.findNearestDetective(state, graph, moves);
            this.NearestLocation = state.getDetectiveLocation((Piece.Detective) pieceNearest).orElseThrow();
        }
    }

    public ImmutableSet<GameNode> addChildren(ImmutableValueGraph<Integer, Integer> graph) {
        ImmutableSet.Builder<GameNode> builder = new ImmutableSet.Builder<>();
        // use moveModifier to find best move of each destination
        List<Board.GameState> states = StateCreator.BestMove(graph, state, moves, pieceNearest, type);
        for (var state : states) {
            builder.add(new GameNode(state, !type, this, graph));
        }
        return builder.build();
    }

    // setter
    public void setScore(int score) { this.score = Optional.of(score); }
    // getter
    public Optional<Integer> getScore() { return this.score; }
    public boolean getType() { return this.type; }
    public GameNode getParent() { return this.parent; }
    public Board.GameState getState() { return this.state; }
    public int getMrXLocation() { return this.mrXLocation; }
    public int getNearestLocation() { return this.NearestLocation; }
}
