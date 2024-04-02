package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableSet;
import com.google.common.graph.ImmutableValueGraph;
import uk.ac.bris.cs.scotlandyard.model.Board;
import uk.ac.bris.cs.scotlandyard.model.Move;
import uk.ac.bris.cs.scotlandyard.model.Piece;

import java.util.*;

public class StateCreator {
    public static List<Board.GameState> BestMove(ImmutableValueGraph<Integer, Integer> graph, Board.GameState state, boolean type) {
        if (type) return maxBestMoves(graph, state);
        else return minBestMoves(graph, state);
    }
    public static List<Board.GameState> maxBestMoves(ImmutableValueGraph<Integer, Integer> graph, Board.GameState state) {
        // mrX choice one best move of each destination according to the ticket
        var moves = state.getAvailableMoves();
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

    public static List<Board.GameState> minBestMoves(ImmutableValueGraph<Integer, Integer> graph, Board.GameState state) {
        // except one detective (The Nearest)
        // Detectives choice one best move of each destination according to the ticket
        Board.GameState tempState = moveAllExceptNearestPiece(graph, state);
        var moves = tempState.getAvailableMoves();
        List<Board.GameState> states = new ArrayList<>();
        for (Move move : moves) {
            states.add(tempState.advance(move));
        }
        return states;
    }

    public static Board.GameState moveAllExceptNearestPiece(
            ImmutableValueGraph<Integer, Integer> valueGraph,
            Board.GameState state) {

        Map<Piece, Integer> distanceMap = new HashMap<>();
        Map<Piece, Move> moveMap = new HashMap<>();
        var moves = state.getAvailableMoves();
        var mrXLocation = getMrXLocation(moves);
        Piece pieceNearest = findClosestDetective(state, valueGraph, moves);

        for (Move move : moves) {
            Piece piece = move.commencedBy();
            if (piece != pieceNearest) {
                int destination = getDestination(move);
                int distance = dijkstraAlgo(valueGraph, destination, mrXLocation);
                if (!distanceMap.containsKey(destination)) {
                    distanceMap.put(piece, distance);
                    moveMap.put(piece, move);
                }else if (distance < distanceMap.get(piece)) {
                    distanceMap.put(piece, distance);
                    moveMap.put(piece, move);
                }
            }
        }
        Board.GameState tempState = state;
        for (Move move : moveMap.values()) {
            tempState = tempState.advance(move);
        }
        return tempState;
    }

    public static Piece findClosestDetective(Board.GameState state,
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
