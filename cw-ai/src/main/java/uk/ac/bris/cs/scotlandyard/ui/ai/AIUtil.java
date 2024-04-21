package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableSet;
import com.google.common.graph.ImmutableValueGraph;
import uk.ac.bris.cs.scotlandyard.model.*;

import java.util.*;

import static uk.ac.bris.cs.scotlandyard.ui.ai.MoveModifier.getDestination;

public class AIUtil {

    public Set<Map<Integer, Integer>> PossibleLocations;
    public ImmutableValueGraph<Integer, Integer> graph;

    AIUtil(ImmutableValueGraph<Integer, Integer> graph){
        PossibleLocations = new HashSet<>();
        this.graph = graph;
    }
    // does not know the location of mrX
    public static Set<Integer> getPossibleLocationsOfNode(ImmutableValueGraph<Integer, Integer> distanceGraph, int location) {
        Set<Integer> locations = new HashSet<>();
        for (int node : distanceGraph.nodes()) {
            if (distanceGraph.hasEdgeConnecting(location, node)) {
                locations.add(node);
            }
        }
        return locations;
    }

    // round 2 => find the maximum freedom of all possible state
    public Map<Integer, Integer> searchFromAllNextState(Board board, ImmutableValueGraph<Integer, Integer> graph, ImmutableSet<Move> moves){
        // at the begining of game, ticket does not need to be considered.
        // * return
        // get destinations for detectives
        setPossibleLocations(board, getDetectiveLocations(board));
        // check
        //for (Map<Integer, Integer> combine : PossibleLocations) {
        //    if (combine.size() != 5) {
        //        System.out.println(combine);
        //        throw new RuntimeException("here");
        //    }
        //}
        int max = 0;
        Map<Integer, Integer> bestLocations = null;
        for (Map<Integer, Integer> detectiveLocations : PossibleLocations) {
            int freedom = getLocaitonsCanReach(detectiveLocations.values()).size();
            if (freedom > max) {
                max = freedom;
                bestLocations = detectiveLocations;
            }
        }
        System.out.println("bestLocaitons: " + bestLocations);
        return bestLocations;
    }

    public Set<Integer> getLocaitonsCanReach(Collection<Integer> sets) {
        Set<Integer> canReach = new HashSet<>();
        for (int location : sets) {
            canReach.addAll(graph.adjacentNodes(location));
        }
        return canReach;
    }

    public void setPossibleLocations(Board board, List<Integer> locations){
        Map<Integer, Set<Integer>> destinations = new HashMap<>();
        for (int location : locations) {
            destinations.put(location, getPossibleLocationsOfNode(graph, location));
        }
        Map<Integer, Integer> visited = new HashMap<>();
        findPossibleLocations(destinations, visited);
        // System.out.println("size of Possible Location: " + PossibleLocations.size());
    }
    public void findPossibleLocations(Map<Integer, Set<Integer>> destinations, Map<Integer, Integer> visited){
        if (visited.size() == destinations.size() && !visited.isEmpty()) {
            Map<Integer, Integer> newVisited = new HashMap<>(visited);
            PossibleLocations.add(newVisited);
            return;
        }
        int count = 0;
        for (var entry : destinations.entrySet()) {
            var desForOnePiece = entry.getValue();
            var previousLocation = entry.getKey();
            if (visited.containsKey(previousLocation)) continue;
            for (int des : desForOnePiece) {
                if (visited.values().contains(des)) continue;
                Map<Integer, Integer> newVisited = new HashMap<>(visited);
                newVisited.put(previousLocation, des);
                findPossibleLocations(destinations, newVisited);
            }
        }
    }
    public static Optional<Integer> getMrXLocation(Board board) {
        Optional<Integer> location = Optional.empty();
        for (LogEntry entry : board.getMrXTravelLog()) {
            if (!entry.location().isEmpty()) location = entry.location();
        }
        return location;
    }

    public static List<Integer> getDetectiveLocations(Board board) {
        List<Integer> locations = new ArrayList<>();
       for(Piece p : board.getPlayers()) {
           if (p.isMrX()) continue;
           locations.add(board.getDetectiveLocation((Piece.Detective) p).orElseThrow());
       }
       return locations;
    }

    public static Piece getPieceFromLocation(Board board, int location) {
        List<Integer> locations = new ArrayList<>();
        for(Piece p : board.getPlayers()) {
            if (p.isMrX()) continue;
            if (board.getDetectiveLocation((Piece.Detective) p).orElseThrow() == location) {
                return p;
            }
        }
        throw new RuntimeException("");
    }

    public static Map<Integer, int[]> mrXPossibleLocaitons(Board board, int passedRound, int mrXLocation) {
        Map<Integer, int[]> currentPossibleLocations = new HashMap<>();
        Piece piece = null;
        for(Piece p : board.getPlayers()) if (p.isMrX()) piece = p;
        var tb = board.getPlayerTickets(piece).orElseThrow();
        currentPossibleLocations.put(mrXLocation, setTickets(tb));
        var graph = board.getSetup().graph;
        for (int i = 0; i < passedRound; i++) {
            Map<Integer, int[]> newPossibleLocations = new HashMap<>();
            for (Map.Entry<Integer, int[]> entry : currentPossibleLocations.entrySet()) {
                int node = entry.getKey();
                int[] tickets = entry.getValue();
                for (int adajacent : graph.adjacentNodes(node)){
                    int[] newTickets = tickets;
                    var ts = graph.edgeValue(node, adajacent).orElseThrow();
                    if (ts.contains(ScotlandYard.Ticket.TAXI) && tickets[0] > 0) {
                        newTickets[0]--;
                        continue;
                    }
                    if (ts.contains(ScotlandYard.Ticket.BUS) && tickets[1] > 0) {
                        newTickets[1]--;
                        continue;
                    }
                    if (ts.contains(ScotlandYard.Ticket.UNDERGROUND) && tickets[2] > 0) {
                        newTickets[2]--;
                        continue;
                    }
                    if (tickets[3] > 0) {
                        newTickets[3]--;
                        continue;
                    }
                    newPossibleLocations.put(adajacent, newTickets);
                }
            }
            currentPossibleLocations = newPossibleLocations;
        }
        return currentPossibleLocations;
    }
    public static int[] setTickets(Board.TicketBoard tb) {
        int[] tickets ;
        tickets = new int[4];
        tickets[0] = tb.getCount(ScotlandYard.Ticket.TAXI);
        tickets[1] = tb.getCount(ScotlandYard.Ticket.BUS);
        tickets[2] = tb.getCount(ScotlandYard.Ticket.UNDERGROUND);
        tickets[3] = tb.getCount(ScotlandYard.Ticket.SECRET);
        return tickets;
    }

    public int compareOverlap(Map<Integer, Integer> map, Map<Integer, int[]> mrXPossibleLocations) {
        // map : previous location of detective = next location of detective
        Set<Integer> detectiveReachable = new HashSet<>();
        for (Map.Entry<Integer, Integer> entry : map.entrySet()) {
            var locationDetective = entry.getValue();
            detectiveReachable.addAll(getPossibleLocationsOfNode(graph, locationDetective));
        }
        int counter = 0;
        for (int node : detectiveReachable) {
            if (mrXPossibleLocations.containsKey(node)) counter++;
        }
        return counter;
    }

    public Move getBestMoveForPieceWithLongDistance(List<Move> moves, Piece p, int[] distances) {
        Map<Integer, Integer> distanceMap = new HashMap<>(); // to store distance to mrX at that point
        Map<Integer, Integer> costMap = new HashMap<>(); // to store cost at that point
        Map<Integer, Move> moveMap = new HashMap<>(); // to store Move at that point
        for (Move move : moves) {
            if (move.commencedBy() == p){
                int destination = getDestination(move);
                int cost = calCost(move);
                if (!distanceMap.containsKey(destination)) {
                    distanceMap.put(destination, distances[destination - 1]);
                    costMap.put(destination, cost);
                    moveMap.put(destination, move);
                }else if (costMap.get(destination) > cost) {
                    costMap.put(destination, cost);
                    moveMap.put(destination, move);
                }
            }
        }
        int min = 999;
        int bestDes = -1;
        for (Map.Entry<Integer, Integer> entry : distanceMap.entrySet()) {
            int des = entry.getKey();
            int distance = entry.getValue();
            if (distance < min) {
                min = distance; bestDes = des;
            }
        }
        return moveMap.get(bestDes);
    }
    public boolean isReachable (Map<Integer, Integer> combine, List<Integer> vistied, List<Move> moves, Board board) {
        // function: checking node has not been occupied or unable to reach
        for (int des : combine.values()) if (vistied.contains(des)) return false;
        // unable to reach
        for (Map.Entry<Integer, Integer> entry : combine.entrySet()) {
            int src = entry.getKey();
            int des = entry.getValue();
            Piece p = getPieceFromLocation(board, src);
            boolean flag = false;
            for (Move move : moves) {
                if (move.commencedBy() == p && getDestination(move) == des && move.source() == src) {
                    flag = true;
                }
            }
            if (!flag) return false;
        }

        return true;
    }


    public static int calCost(Move move) {
        int cost = 0;
        for (ScotlandYard.Ticket t : move.tickets()) {
            switch (t) {
                case BUS -> cost += 3;
                case TAXI -> cost += 1;
                case UNDERGROUND -> cost += 5;
            }
        }
        return cost;
    }

    public Set<Map<Integer, Integer>> getPossibleLocations() { return this.PossibleLocations; }
}