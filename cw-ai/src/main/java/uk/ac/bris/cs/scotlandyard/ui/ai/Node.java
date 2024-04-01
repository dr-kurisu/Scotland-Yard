package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableSet;
import com.google.common.graph.ImmutableValueGraph;
import uk.ac.bris.cs.scotlandyard.model.*;

import java.util.*;
public class Node {
    private List<Node> children;
    private boolean isBest;
    private Board.GameState state;
    private int score;  // minimum distance to detectives
    private int mrXLocation;
    private float cost;   // total cost of tickets
    private Node parent;


    public Node(Board.GameState state, Node parent, float cost, int mrXLocation){
        this.state = state;
        this.isBest = false;
        this.cost = cost;
        this.mrXLocation = mrXLocation;
        this.parent = parent;
    }
    public void addmrXChildren(){
        this.children = new ArrayList<>();
        ImmutableSet<Move> moves = state.getAvailableMoves();
        Map<Integer, Float> costMap = new HashMap<>();
        for(Move move : moves){
            int destination = move.accept(new Move.Visitor<Integer>() {
                @Override
                public Integer visit(Move.SingleMove move) {
                    return move.destination;
                }
                @Override
                public Integer visit(Move.DoubleMove move) {
                    return move.destination2;
                }
            });
            float cost = calculateCost(move.tickets());
            if (!costMap.containsKey(destination)){
                costMap.put(destination, cost);
                children.add(new Node(state.advance(move),this, calculateCost(move.tickets()), destination));
            }
            else if (costMap.get(destination) > cost){
                costMap.put(destination, cost);
                for (Node child: children){
                    if (child.mrXLocation == destination & child.cost == cost){ children.remove(child); }
                }
                children.add(new Node(state.advance(move),this, calculateCost(move.tickets()), destination));
            }
        }
    }
    public void addChild(Node node) {
        if (this.children == null) this.children = new ArrayList<>();
        this.children.add(node);
    }

    // Function used to calculate cost of tickets for *** mrX ***
    private float calculateCost(Iterable<ScotlandYard.Ticket> tickets){
        float cost = 0;
        Board.TicketBoard tb = null;
        for(Piece p : state.getPlayers()){
            if (p.isMrX()){ tb = state.getPlayerTickets(p).orElseThrow(); }
        }
        if (tb == null) {throw new RuntimeException("tb does not exist"); }
        int bus = tb.getCount(ScotlandYard.Ticket.BUS);
        int taxi = tb.getCount(ScotlandYard.Ticket.TAXI);
        int secret = tb.getCount(ScotlandYard.Ticket.SECRET);
        int underground = tb.getCount(ScotlandYard.Ticket.UNDERGROUND);
        for (ScotlandYard.Ticket t : tickets){
            switch (t){
                case BUS -> bus--;
                case DOUBLE -> cost += 10;
                case TAXI -> taxi--;
                case SECRET -> secret--;
                case UNDERGROUND -> underground--;
            }
            // To make number of ticket distribute more uniformly
            cost += variance(taxi, bus, secret, underground);
        }
        return cost;
    }

    public static float variance(int x1, int x2, int x3, int x4){
        float mean = (float) (x1 + x2 + x3 + x4) / 4;
        return ((x1 - mean)*(x1 - mean) + (x2 - mean)*(x2 - mean) + (x3 - mean)*(x3 - mean) + (x4 - mean)*(x4 - mean))/4;
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
    public int findShortestDistance(ImmutableValueGraph<Integer, Integer> valueGraph, int source, int destination) {
        return dijkstraAlgo(valueGraph, source, destination).size();
    }

    public void setScore(int score) { this.score = score; }
    // getter
    public List<Node> getChildren(){ return this.children; }
    public boolean getIsBest(){
        return this.isBest;
    }
    public int getMrXLocation(){
        return this.mrXLocation;
    }
    public float getCost() { return cost; }
    public int getScore() { return score; }

    public Node getParent() { return parent; }
    public Board.GameState getState() { return this.state; }
}
