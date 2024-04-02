package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.graph.ImmutableValueGraph;
import uk.ac.bris.cs.scotlandyard.model.Board;
import uk.ac.bris.cs.scotlandyard.model.Move;
import uk.ac.bris.cs.scotlandyard.model.Piece;

public class ScoreCalculator {
    // two parts of score
    // 1. distance between mrX and the nearest detective
    // 2. freedom of next location
    public static int getScore(ImmutableValueGraph<Integer, Integer> graph, Board.GameState state, int source, int destination) {
        int distance = StateCreator.dijkstraAlgo(graph, source, destination);
        return 10 * distance + getFreedom(state); // increase weight of distance
    }

    public static int getFreedom(Board.GameState state) {
        return state.getAvailableMoves().size();
    }
}
