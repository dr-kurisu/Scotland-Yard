package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.graph.ImmutableValueGraph;
import uk.ac.bris.cs.scotlandyard.model.Board;
import uk.ac.bris.cs.scotlandyard.model.GameSetup;
import uk.ac.bris.cs.scotlandyard.model.Move;
import uk.ac.bris.cs.scotlandyard.model.Piece;

/*
 * This is the class designed to process scores of the nodes.
 * Score is calculated of 3 variables: Total shortest distances from MrX,
 * freedom and the nearest distance.
 */
public class ScoreCalculator {
    // two parts of score
    // 1. distance between mrX and the nearest detective
    // 2. freedom of next location
    public static int[] getDistances(ImmutableValueGraph<Integer, Integer> graph, int src) {
        return MoveModifier.dijkstraAlgo(graph, src);
    }

    // returns the total of shortest distances of detectives from MrX
    public static int calculateTotalDistance(int[] distances, Board.GameState state) {
        int result = 0;
        var pieces = state.getPlayers();
        for (Piece p : pieces) {
            if (p.isMrX()) continue;
            result += distances[state.getDetectiveLocation((Piece.Detective) p).orElseThrow() - 1];
        }
        return result;
    }

    // scoring function
    public static int getScore(int[] distances, Board.GameState state, int freedomCoef, int des, int freedom) {
        // to make this function maximum
        // the most important thing is the nearest distance
        // the second is total distance and freedom
        // this weight will not exceed -999 and 999
        return 4 * calculateTotalDistance(distances, state) + 70 * distances[des - 1] + 15 * freedomCoef * freedom;
    }
}
