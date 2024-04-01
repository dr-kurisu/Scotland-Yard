package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableSet;
import com.google.common.graph.ImmutableValueGraph;
import uk.ac.bris.cs.scotlandyard.model.Board;
import uk.ac.bris.cs.scotlandyard.model.Move;
import uk.ac.bris.cs.scotlandyard.model.Piece;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard;

import java.util.*;
public class Tree {
    private Node root;
    private int lastLocation; // last visible location of mrX
    private ImmutableValueGraph<Integer, Integer> valueGraph;
    private List<Node> lastLevel;
    public Tree(Node node, int depth, ImmutableValueGraph<Integer, Integer> valueGraph){
        root = node;
        this.valueGraph = valueGraph;
        List<Node> currentLevel = new ArrayList<>();
        currentLevel.add(root);
        this.lastLevel = currentLevel;
        lastLocation = -1;
        // change here
        for(int i = 0; i < depth; i++){
            addMrXLayer();
            addDectivesLayer();
        }
    }
    public void addDectivesLayer() {
        for (Node node : lastLevel) {
            ImmutableSet<Move> moves = node.getState().getAvailableMoves();
            // detectives do not know location of mrX
            if (lastLocation == -1) {
                Board.GameState nextState = null;
                // loop number of detectives times
                Random random = new Random();
                for (int i = 0; i < node.getState().getPlayers().size() - 1; i++) {
                    if (moves.size() == 0) break;
                    // pick move randomly
                    nextState = node.getState().advance(moves.asList().get(random.nextInt(moves.size())));
                    moves = nextState.getAvailableMoves();
                }
                node.addChild(new Node(nextState, node, -1, node.getMrXLocation()));
                continue;
            }
            System.out.println("enter");
            // detectives know location of mrX

            // find the closest piece
            Piece cloesetPiece = null;
            int min = 999;
            for (Piece p : node.getState().getPlayers()) {
                if (p.isDetective()) {
                    int location = node.getState().getDetectiveLocation((Piece.Detective) p).orElseThrow();
                    System.out.println("lastLocation: " + lastLocation);
                    int distance = findShortestDistance(valueGraph, location, lastLocation);
                    if (distance < min) {
                        min = distance;
                        cloesetPiece = p;
                    }
                }
            }
            // others move to lastlocation
            Board.GameState newState = node.getState();
            for (Piece p : node.getState().getPlayers()){
                if (p.isDetective() && p != cloesetPiece) {
                    int minDistance = 999;
                    Move bestMove = null;
                    for (Move move : moves) {
                        if (move.commencedBy() == p) {
                            int destination = getDestination(move);
                            int distance = findShortestDistance(valueGraph, destination, lastLocation);
                            if (distance < minDistance) {
                                minDistance = distance;
                                bestMove = move;
                            }
                        }
                    }
                    newState = newState.advance(bestMove);
                }
            }
            // move the closest detective
            // every position maintain the least cost move
            Map<Integer, Float> costMap = new HashMap<>();
            for (Move move : newState.getAvailableMoves()) {
                int destination = getDestination(move);
                Board.TicketBoard tb = newState.advance(move).getPlayerTickets(cloesetPiece).orElseThrow();
                float cost = calculateCost(tb);
                if (!costMap.containsKey(destination)){
                    costMap.put(destination, calculateCost(tb));
                    node.addChild(new Node(newState.advance(move), node, cost, node.getMrXLocation()));
                }else if (calculateCost(tb) < costMap.get(destination)) {
                    for (Node child: node.getChildren()){
                        if (child.getMrXLocation() == destination & child.getCost() == cost){ node.getChildren().remove(child); }
                    }
                    node.addChild(new Node(newState.advance(move), node, cost, node.getMrXLocation()));
                }
            }
        }
    }

    private static int getDestination(Move move) {
        int destination = move.accept(new Move.Visitor<Integer>() {
            @Override
            public Integer visit(Move.SingleMove move) {
                return move.destination;
            }
            @Override
            public Integer visit(Move.DoubleMove move) {
                throw new RuntimeException("No double move");
            }
        });
        return destination;
    }

    private float calculateCost(Board.TicketBoard tb) {
        int bus = tb.getCount(ScotlandYard.Ticket.BUS);
        int taxi = tb.getCount(ScotlandYard.Ticket.TAXI);
        int under = tb.getCount(ScotlandYard.Ticket.UNDERGROUND);
        return variance(bus, taxi, under);
    }

    public static float variance(int x1, int x2, int x3){
        float mean = (float) (x1 + x2 + x3) / 3;
        return ((x1 - mean)*(x1 - mean) + (x2 - mean)*(x2 - mean) + (x3 - mean)*(x3 - mean))/3;
    }

    public void addMrXLayer(){
        List<Node> lastLevel = new ArrayList<>();
        for(Node node : this.lastLevel){
            node.addmrXChildren();
            lastLevel.addAll(node.getChildren());
        }
        this.lastLevel = lastLevel;
    }

    public static List<Integer> dijkstraAlgo(
            ImmutableValueGraph<Integer, Integer> graph,
            Integer source,
            Integer destination) {
        int size = graph.nodes().size();
        int [] distance = new int[size];
        boolean [] visited = new boolean[size];

        int [] previous = new int[size];
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
                    previous[node-1] = current;
                    pq.offer(node);
                }
            }
        }

        List<Integer> path = new ArrayList<>();
        for(int v=destination; v != source; v = previous[v-1]){
            path.add(v);
        }
        path.add(source);
        Collections.reverse(path);
        return path;
    }
    public static int findShortestDistance(ImmutableValueGraph<Integer, Integer> valueGraph, int source, int destination) {
        return dijkstraAlgo(valueGraph, source, destination).size();
    }

    public Node getRoot(){ return root; }
    public List<Node> getLastLevel(){ return lastLevel; }
}
