package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableSet;
import com.google.common.graph.ImmutableValueGraph;
import uk.ac.bris.cs.scotlandyard.model.Board;
import uk.ac.bris.cs.scotlandyard.model.Move;
import uk.ac.bris.cs.scotlandyard.model.Piece;

import java.util.*;

import static java.util.Collections.min;

public class StateCreator {
    public static List<Board.GameState> BestMove(ImmutableValueGraph<Integer, Integer> graph,
                                                 Board.GameState state,
                                                 ImmutableSet<Move> moves,
                                                 Piece pieceNearest,
                                                 boolean type) {
        if (!type) return maxBestMoves(graph, state, moves);
        else return minBestMoves(graph, state, pieceNearest);
    }
    public static List<Board.GameState> maxBestMoves(ImmutableValueGraph<Integer, Integer> graph, Board.GameState state, ImmutableSet<Move> moves) {
        // mrX choice one best move of each destination according to the ticket
        Map<Integer, Move> moveMap = new HashMap<>();
        Map<Integer, Float> costMap = new HashMap<>();
        for (Move move : moves) {
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
        List<Board.GameState> states = new ArrayList<>();
        for (Move move : moveMap.values()) {
            states.add(state.advance(move));
        }
        return states;
    }

    public static List<Board.GameState> minBestMoves(ImmutableValueGraph<Integer, Integer> graph, Board.GameState state, Piece pieceNearest) {
        // except one detective (The Nearest)
        // Detectives choice one best move of each destination according to the ticket
        var moves = List.copyOf(state.getAvailableMoves());

        Board.GameState tempState = moveAllExceptNearestPiece(graph, state, pieceNearest, moves);
        List<Board.GameState> states = new ArrayList<>();
        List<Move> nearestMoves = List.copyOf(tempState.getAvailableMoves());
        for (Move move : nearestMoves) {
            states.add(tempState.advance(move));
        }
        return states;
    }

    public static Board.GameState moveAllExceptNearestPiece2(
            ImmutableValueGraph<Integer, Integer> valueGraph,
            Board.GameState state,
            Piece pieceNearest,
            List<Move> moves) {
        var pieces = getDetectives(state);
        int mrXLocation = getMrXLocation(moves);
        var tempState = state;

        for (Piece piece : pieces) {
            if (piece == pieceNearest) continue;
            Map<Integer, Move> moveMap = new HashMap<>();
            Map<Integer, Float> costMap = new HashMap<>();
            Map<Integer, Integer> distanceMap = new HashMap<>();
            for (Move move : moves) {
                if (move.commencedBy() != piece) continue;
                float cost = CostCalculator.minCalculateCost(state, move);
                int destination = getDestination(move);
                if (!distanceMap.containsKey(destination)) {
                    distanceMap.put(destination, dijkstraAlgo(valueGraph, destination, mrXLocation));
                }
                if (!costMap.containsKey(destination)) {
                    costMap.put(destination, cost);
                    moveMap.put(destination, move);
                }else if (cost < costMap.get(destination)){
                    costMap.put(destination, cost);
                    moveMap.put(destination, move);
                }
            }
            // find the minimum distance
            int min = 999;
            for (int des : distanceMap.keySet()) {
                if (distanceMap.get(des) < min) {
                    min = distanceMap.get(des);
                }
            }
            // find the least cost of move
            Move bestMove = null;
            float minCost = 999;
            for (Map.Entry<Integer, Move> entry : moveMap.entrySet()) {
                var node = entry.getKey();
                if (distanceMap.get(node) != min) { continue; }
                if (costMap.get(node) < minCost){
                    minCost = costMap.get(node);
                    bestMove = entry.getValue();
                }
            }
            if (bestMove == null) {
                System.out.println(moveMap);
                System.out.println(costMap);
                System.out.println(distanceMap);
            }
            tempState = tempState.advance(bestMove);
            moves = List.copyOf(tempState.getAvailableMoves());
            // not efficient
        }
        return tempState;
    }
    public static Board.GameState moveAllExceptNearestPiece(
            ImmutableValueGraph<Integer, Integer> valueGraph,
            Board.GameState state,
            Piece pieceNearest,
            List<Move> moves) {
        List<Piece.Detective> detectives = getDetectives(state);
        var tempState = state;
        Map<Integer, Integer> distanceMap = new HashMap<>(); // to store distance to mrX at that point
        Map<Integer, Float> costMap = new HashMap<>(); // to store cost at that point
        Map<Integer, Move> moveMap = new HashMap<>(); // to store Move at that point
        int mrXLocation = getMrXLocation(moves);

        for (Piece.Detective d : detectives) {
            for (Move move : moves) {
                if (move.commencedBy() != d) continue;
                int destination = getDestination(move);
                float cost = CostCalculator.minCalculateCost(state, move);
                if (!distanceMap.containsKey(destination)) {
                    // can be simplified (reduce usage of dijkstar algorithm)
                    distanceMap.put(destination, dijkstraAlgo(valueGraph, mrXLocation, destination));
                    costMap.put(destination, cost);
                    moveMap.put(destination, move);
                }else if (cost < costMap.get(destination)) {
                    costMap.put(destination, cost);
                    moveMap.put(destination, move);
                }
            }
            // check
            if (distanceMap.size() == 0) throw new RuntimeException("here");
            // find min distance
            int minDistance = min(distanceMap.values());
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

    public static Piece findNearestDetective(Board.GameState state,
                                             ImmutableValueGraph<Integer, Integer> graph,
                                             ImmutableSet<Move> moves)
    {
        var detectives = getDetectives(state);
        int mrXLocation = getMrXLocation(moves);
        int min = 999;
        Piece piece = null;
        for (Piece.Detective p : detectives) {
            int location = state.getDetectiveLocation(p).orElseThrow();
            var distance = dijkstraAlgo(graph, mrXLocation, location);
            if (distance < min) {
                min = distance;
                piece = p;
            }
        }
        return piece;
    }

    public static int dijkstraAlgo(
            ImmutableValueGraph<Integer, Integer> graph,
            Integer source,
            Integer destination) {
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
        return distance[destination-1];
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

    public static int getMrXLocation(ImmutableSet<Move> moves) {
        if (moves.size() <= 0) throw new RuntimeException("MrX is unable to move");
        return moves.asList().get(0).source();
    }
    public static int getMrXLocation(List<Move> moves) {
        if (moves.size() <= 0) throw new RuntimeException("MrX is unable to move");
        return moves.get(0).source();
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
