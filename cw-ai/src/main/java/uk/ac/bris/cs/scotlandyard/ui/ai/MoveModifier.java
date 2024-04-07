package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.graph.ImmutableValueGraph;
import uk.ac.bris.cs.scotlandyard.model.Board;
import uk.ac.bris.cs.scotlandyard.model.Move;
import uk.ac.bris.cs.scotlandyard.model.Piece;

import java.util.*;

import static java.util.Collections.min;

public class MoveModifier {
    public static List<Move> BestMove(           Board.GameState state,
                                                 ImmutableSet<Move> moves,
                                                 Piece pieceNearest,
                                                 boolean type,
                                                 int mrXLocation,
                                                 ImmutableValueGraph<Integer, Integer> graph) {
        if (!type) return maxBestMoves(state, moves);
        else return minBestMoves(state, moves, pieceNearest, mrXLocation, graph);
    }
    public static List<Move> maxBestMoves(Board.GameState state, ImmutableSet<Move> moves) {
        // mrX choice one best move of each destination according to the ticket
        Map<Integer, Move> moveMap = new HashMap<>();
        Map<Integer, Float> costMap = new HashMap<>();
        for (Move move : moves) {
            // get cost of tickets
            float cost = CostCalculator.maxCalculateCost(state, move);
            int destination = getDestination(move);
            if (!costMap.containsKey(destination)) {
                costMap.put(destination, cost);
                moveMap.put(destination, move);
            }else if (cost < costMap.get(destination)){
                costMap.put(destination, cost);
                moveMap.put(destination, move);
            }
        }
        // System.out.println("min cost: " + costMap);
        List<Move> result = new ArrayList<>();
        for (Move move : moveMap.values()) {
            result.add(move);
        }
        return result;
    }

    public static List<Move> minBestMoves(Board.GameState state, ImmutableSet<Move> moves, Piece pieceNearest, int mrXLocation, ImmutableValueGraph<Integer, Integer> graph) {
        // except one detective (The Nearest)
        // Detectives choice one best move of each destination according to the ticket
        var distances = ScoreCalculator.getDistances(graph, mrXLocation);
        Board.GameState tempState = moveAllExceptNearestPiece(state, distances, pieceNearest, List.copyOf(moves));
        List<Move> nearestMoves = List.copyOf(tempState.getAvailableMoves());
        return nearestMoves;
    }

    public static Board.GameState moveAllExceptNearestPiece(
            Board.GameState state,
            int[] distances,
            Piece pieceNearest,
            List<Move> moves) {
        List<Piece.Detective> detectives = getDetectives(state);
        var tempState = state;
        Map<Integer, Integer> distanceMap = new HashMap<>(); // to store distance to mrX at that point
        Map<Integer, Float> costMap = new HashMap<>(); // to store cost at that point
        Map<Integer, Move> moveMap = new HashMap<>(); // to store Move at that point

        for (Piece.Detective d : detectives) {
            if (d == pieceNearest) continue;
            for (Move move : moves) {
                if (move.commencedBy() != d) continue;
                int destination = getDestination(move);
                float cost = CostCalculator.minCalculateCost(state, move);
                if (!distanceMap.containsKey(destination)) {
                    // can be simplified (reduce usage of dijkstar algorithm)
                    distanceMap.put(destination, distances[destination]);
                    costMap.put(destination, cost);
                    moveMap.put(destination, move);
                }else if (cost < costMap.get(destination)) {
                    costMap.put(destination, cost);
                    moveMap.put(destination, move);
                }
            }
            // check
            if (distanceMap.size() == 0) throw new RuntimeException("Impossible!");
            // find min distance
            int minDistance = min(distanceMap.values());
            // System.out.println("minDistance: " + minDistance);
            // extract all destination at that point
            List<Integer> detinations = new ArrayList<>();
            for (Map.Entry<Integer, Integer> entry : distanceMap.entrySet()) {
                if (entry.getValue() == minDistance) detinations.add(entry.getKey());
            }
            float min = 999;
            Move bestMove = null;
            for (int des : detinations) {
                if (costMap.get(des) < min) {
                    min = costMap.get(des);
                    bestMove = moveMap.get(des);
                }
            }
            tempState.advance(bestMove);
        }
        return tempState;
    }

    public static int[] dijkstraAlgo(
            ImmutableValueGraph<Integer, Integer> graph,
            Integer source) {
        int size = graph.nodes().size();
        int [] distance = new int[size];
        boolean [] visited = new boolean[size];

        for(Integer node : graph.nodes()){ distance[node-1] = 999; }
        distance[source-1] = 0;

        PriorityQueue<Integer> pq = new PriorityQueue<>(new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                return distance[o1-1] - distance[o2-1];
            }
        });

        pq.offer(source);
        while(!pq.isEmpty()){
            int current = pq.poll();
            visited[current-1] = true;
            for(Integer node : graph.nodes()){
                int weight = graph.edgeValueOrDefault(current, node, 999);
                if(!visited[node-1] && weight + distance[current-1] < distance[node-1]){
                    distance[node-1] = weight + distance[current-1];
                    pq.offer(node);
                }
            }
        }
        return distance;
    }

    public static List<Piece.Detective> getDetectives(Board.GameState state) {
        List<Piece.Detective> detectives = new ArrayList<>();
        for (Piece p : state.getPlayers()) {
            if (p.isDetective()) {
                detectives.add((Piece.Detective) p);
            }
        }
        return detectives;
    }

    public static int getDestination(Move move) {
        return move.accept(new Move.Visitor<Integer>() {
            @Override
            public Integer visit(Move.SingleMove move) {
                return move.destination;
            }
            @Override
            public Integer visit(Move.DoubleMove move) {
                return move.destination2;
            }
        });
    }
}
