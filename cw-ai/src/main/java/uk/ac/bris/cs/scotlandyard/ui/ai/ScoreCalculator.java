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
        // the weight of distances[des - 1] increase while decrease of distance[des - 1]
        // maximum weight: 100, min: 0;
        int score = calculateTotalDistance(distances, state) + 100 * distances[des - 1] + 2 * freedom;
        if (distances[des - 1] == 1) score -= 200;
        return score;
    }
}
