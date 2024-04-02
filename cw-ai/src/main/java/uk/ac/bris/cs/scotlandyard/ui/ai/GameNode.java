package uk.ac.bris.cs.scotlandyard.ui.ai;
import com.google.common.graph.ImmutableValueGraph;

import com.google.common.collect.ImmutableSet;
import uk.ac.bris.cs.scotlandyard.model.Board;

import java.util.List;

public class GameNode {
    private int score;
    private boolean type;    // type of min or max node (0: min, 1 :max)
    private GameNode parent;  // parent node
    public Board.GameState state;  // state of game


    GameNode(Board.GameState state, boolean type, GameNode parent){
        this.state = state;
        this.type = type;
        this.parent = parent;
    }

    public ImmutableSet<GameNode> addChildren(ImmutableValueGraph<Integer, Integer> graph) {
        ImmutableSet.Builder<GameNode> builder = new ImmutableSet.Builder<>();
        // use moveModifier to find best move of each destination
        List<Board.GameState> states = StateCreator.BestMove(graph, state, type);
        for (var state : states) {
            builder.add(new GameNode(state, !type, this));
        }
        return builder.build();
    }

    // getter
    public int getScore() { return this.score; }
    public GameNode getParent() { return this.parent; }
    public Board.GameState getState() { return this.state; }
}
