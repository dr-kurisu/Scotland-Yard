package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableSet;
import com.google.common.graph.ImmutableValueGraph;
import uk.ac.bris.cs.scotlandyard.model.*;

import java.util.*;

import static uk.ac.bris.cs.scotlandyard.ui.ai.MoveModifier.getDestination;

public class AIUtil {

    public Set<Map<Integer, Integer>> PossibleLocations;
    public ImmutableValueGraph<Integer, Integer> graph;

    /* Constructor for AIUti to prevent passing in params again and again
     *
     * @param graph has edge value 1 when nodes are connected
     * @return object itself
     */
    AIUtil(ImmutableValueGraph<Integer, Integer> graph){
        PossibleLocations = new HashSet<>();
        this.graph = graph;
    }
    /* Get the Possible Locations of given node
     *
     * @param graph (To make this function static)
     * @param given location
     * @return Possible location without consider of tickets
     */
    public static Set<Integer> getPossibleLocationsOfNode(ImmutableValueGraph<Integer, Integer> distanceGraph, int location) {
        Set<Integer> locations = new HashSet<>();
        for (int node : distanceGraph.nodes()) {
            if (distanceGraph.hasEdgeConnecting(location, node)) {
                locations.add(node);
            }
        }
        return locations;
    }

    /* find the combination of detective locations having maximum freedom
     *
     * @param board
     * @param available moves
     * @return Map<PreviousLocation : NextLocation>
     */
    public Map<Integer, Integer> searchFromAllNextState(Board board, ImmutableSet<Move> moves){
        // get all possible location of detectives
        setPossibleLocations(getDetectiveLocations(board, moves));
        int max = 0;
        Map<Integer, Integer> bestLocations = null;
        for (Map<Integer, Integer> detectiveLocations : PossibleLocations) {
            int freedom = getLocaitonsCanReach(detectiveLocations.values()).size();
            if (freedom > max) {
                max = freedom;
                bestLocations = detectiveLocations;
            }
        }
        return bestLocations;
    }

    /* Get the set of next move that given nodes can reach
     *
     * @param set of given nodes to calculate location they can reach
     * @return a set of locations they can reach
     */
    public Set<Integer> getLocaitonsCanReach(Collection<Integer> sets) {
        Set<Integer> canReach = new HashSet<>();
        for (int location : sets) {
            canReach.addAll(graph.adjacentNodes(location));
        }
        return canReach;
    }
    /* Set the private attribute possibleLocations
     *
     * @param List of locations of detectives
     * @return null
     */
    public void setPossibleLocations(List<Integer> locations){
        Map<Integer, Set<Integer>> destinations = new HashMap<>();
        for (int location : locations) {
            destinations.put(location, getPossibleLocationsOfNode(graph, location));
        }
        Map<Integer, Integer> visited = new HashMap<>();
        findPossibleLocations(destinations, visited);
    }
    /* Recursion function used to generate all possible combination of detectives locations
     *
     * @param Map<Integer, Set<Integer>> destinations (currentLocation : possibleNextLocations) that detective can go
     * @param map visited to store visited nodes prevent occupying same node twice
     * @return null (add visited to PossibleLocations)
     */
    public void findPossibleLocations(Map<Integer, Set<Integer>> destinations, Map<Integer, Integer> visited){
        // if the size of visited is equals to number of detectives need to consider overlap
        if (visited.size() == destinations.size() && !visited.isEmpty()) {
            // prevent overlapping
            Map<Integer, Integer> newVisited = new HashMap<>(visited);
            PossibleLocations.add(newVisited);
            return;
        }
        for (var entry : destinations.entrySet()) {
            var desForOnePiece = entry.getValue();
            var previousLocation = entry.getKey();
            if (visited.containsKey(previousLocation)) continue;
            for (int des : desForOnePiece) {
                if (visited.values().contains(des)) continue;
                // prevent overwriting visited
                Map<Integer, Integer> newVisited = new HashMap<>(visited);
                newVisited.put(previousLocation, des);
                findPossibleLocations(destinations, newVisited);
            }
        }
    }
    /* Get the location of mrX by log
     *
     * @param board
     * @return Optional<Integer> location
     */
    public static Optional<Integer> getMrXLocation(Board board) {
        Optional<Integer> location = Optional.empty();
        for (LogEntry entry : board.getMrXTravelLog()) {
            if (!entry.location().isEmpty()) location = entry.location();
        }
        return location;
    }

    /* Get Detectives current locations by board and moves
     *
     * @param board
     * @param available moves
     * @return List of detectives who have moves/move
     */
    public static List<Integer> getDetectiveLocations(Board board, ImmutableSet<Move> moves) {
        List<Integer> locations = new ArrayList<>();
        for(Piece p : board.getPlayers()) {
            if (p.isMrX()) continue;
            // only add detectives' locations when they have moves
            boolean flag = false;
            for (Move move : moves) {
                if (move.commencedBy() == p) flag = true;
            }
            if (flag) locations.add(board.getDetectiveLocation((Piece.Detective) p).orElseThrow());
        }
        return locations;
    }
    /* Get Piece from given current location
     *
     * @param board
     * @param location of piece
     * @return Piece at that location
     */
    public static Piece getPieceFromLocation(Board board, int location) {
        for(Piece p : board.getPlayers()) {
            if (p.isMrX()) continue;
            if (board.getDetectiveLocation((Piece.Detective) p).orElseThrow() == location) {
                return p;
            }
        }
        throw new RuntimeException("");
    }

    /* Get set of mrX possible locations by passed rounds and used tickets (smart!)
     *
     * @param board
     * @param passedRound
     * @param MrXLocation
     * @param detective locations (ensure mrX and detectives not to overlap)
     * @return Set of mrXPossible locations
     */
    public static Set<Integer> mrXPossibleLocaitons(Board board, int passedRound, int mrXLocation, List<Integer> detectiveLocations) {
        Set<Integer> currentPossibleLocations = new HashSet<>();
        currentPossibleLocations.add(mrXLocation);
        var graph = board.getSetup().graph;
        // get used tickets for mrX
        ScotlandYard.Ticket[] usedTickets;
        usedTickets = new ScotlandYard.Ticket[passedRound];
        int numberOfRound = board.getMrXTravelLog().size();
        for (int i = 0; i < passedRound; i++) {
            usedTickets[passedRound-i-1] = board.getMrXTravelLog().get(numberOfRound - i - 1).ticket();
        }
        for (int i = 0; i < passedRound; i++) {
            Set<Integer> newPossibleLocations = new HashSet<>();
            for (int node : currentPossibleLocations) {
                for (int adjacent : graph.adjacentNodes(node)){
                    // mrX cannot go to detective current location
                    if (detectiveLocations.contains(adjacent)) continue;
                    // if mrX used secret, he can go wherever he wants
                    if (usedTickets[i] == ScotlandYard.Ticket.SECRET) newPossibleLocations.add(adjacent);
                    for (ScotlandYard.Transport t : graph.edgeValue(node, adjacent).orElseThrow()){
                        if (t.requiredTicket() == usedTickets[i]){
                            newPossibleLocations.add(adjacent);
                        }
                    }
                }
            }
            currentPossibleLocations = newPossibleLocations;
        }
        return currentPossibleLocations;
    }
    /* Compare the rate of overlap to determine which combinations of moves we need to choose
     *
     * @param Detectives Possible Locations
     * @param MrX Possible Locations
     */
    public int compareOverlap(Map<Integer, Integer> detectivePossibleLocations, Set<Integer> mrXPossibleLocations) {
        // map : previous location of detective -> next location of detective
        Set<Integer> detectiveReachable = new HashSet<>();
        for (Map.Entry<Integer, Integer> entry : detectivePossibleLocations.entrySet()) {
            var locationDetective = entry.getValue();
            detectiveReachable.addAll(getPossibleLocationsOfNode(graph, locationDetective));
        }
        int counter = 0;
        for (int node : detectiveReachable) {
            if (mrXPossibleLocations.contains(node)) counter++;
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
    /* Check if the combination of detectives locations are reachable by moves
     *
     * @param single combination of detectives locations
     * @param visited nodes to prevent overlapping
     * @param List of moves of remaining detectives
     * @param board
     * @return boolean to represent if the combine is valid
     */
    public boolean isReachable (Map<Integer, Integer> combine, List<Integer> visited, List<Move> moves, Board board) {
        // function: checking node has not been occupied or unable to reach
        for (int des : combine.values()) if (visited.contains(des)) return false;
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

    /* Calculate cost of move (Priority: taxi, bus, underground)
     *
     * @param move
     * @return cost of move
     */
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
    /* Getter for PossibleLocation for detectives
     *
     * @return attribute PossibleLocations
     */
    public Set<Map<Integer, Integer>> getPossibleLocations() { return this.PossibleLocations; }
}