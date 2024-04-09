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
            result += distances[state.getDetectiveLocation((Piece.Detective) p).orElseThrow() - 1];
        }
        return result;
    }

    // *** tomorrow need to do : add scoring function to tree
    public static int getScore(int[] distances, Board.GameState state, int des, int freedom) {
        // to make this function maximum
        // the first important thing is the nearest distance
        // the second is total distance and freedom
        // calculateTotalDistance(distances, state) + (100 - 10 * distances[des]) * distances[des] + freedom;
        return calculateTotalDistance(distances, state) + distances[des - 1] * 30;
    }
}
