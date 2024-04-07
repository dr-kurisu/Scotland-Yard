package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.graph.ImmutableValueGraph;
import uk.ac.bris.cs.scotlandyard.model.Board;
import uk.ac.bris.cs.scotlandyard.model.Move;
import uk.ac.bris.cs.scotlandyard.model.Piece;

public class ScoreCalculator {
    // two parts of score
    // 1. distance between mrX and the nearest detective
    // 2. freedom of next location
    public static int[] getDistances(ImmutableValueGraph<Integer, Integer> graph, int src) {
        return MoveModifier.dijkstraAlgo(graph, src);
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

    // *** tomorrow need to do : add scoring function to tree
    public static int getScore(ImmutableValueGraph<Integer, Integer> graph, Board.GameState state, int src, int des, int freedom) {
        int[] distances = getDistances(graph, src);
        // to make this function maximum
        // the first important thing is nearest distance
        // the second is total distance and freedom
        return calculateTotalDistance(distances, state) + 50 * distances[des] + freedom;
    }
}
