package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableSet;
import com.google.common.graph.ImmutableValueGraph;
import com.google.common.graph.MutableValueGraph;
import com.google.common.graph.ValueGraphBuilder;
import uk.ac.bris.cs.scotlandyard.model.*;

import java.util.*;

public class Tree {
    private Set<Node> leaves;
    private Node root;
    private Set<Node> firstLayer;
    private Set<Node> currentLayer;
    private ImmutableValueGraph<Integer, Integer> distanceGraph;
    private int count;
    private final int depth;
    private boolean considerDouble;

    Tree(Board.GameState state, int round, int depth, int mrXLocation) {
        // MrX turn
        this.leaves = new HashSet<>();
        this.currentLayer = new HashSet<>();
        this.distanceGraph = createValueGraph(state.getSetup().graph);
        this.depth = depth;
        this.considerDouble = false;
        this.count = 0;
        // initialise root
        this.root = new Node(state, false, null, mrXLocation, ScoreCalculator.getDistances(distanceGraph, mrXLocation), null, ImmutableSet.of());
        // set current layer to root
        this.currentLayer.add(this.root);

        //this.currentLayer.addAll(root.addChildren(distanceGraph));
        //this.firstLayer = this.currentLayer;

        // create the tree to the corresponding depth.
        for (int i = 0; i < depth; i++) {
            Set<Node> nextLayer = new HashSet<>();
            // goes through each node of next layer
            // add children nodes if no winner appears
            for (Node node : this.currentLayer) {
                if (node.winner.isEmpty()) {
                    var nodes = node.addChildren(distanceGraph);
                    if (nodes == null) this.leaves.add(node);
                    if (nodes != null) nextLayer.addAll(nodes);
                } else {
                    this.leaves.add(node);
                }
            }
            this.currentLayer = nextLayer;
        }
        // add last layer to leaves
        this.leaves.addAll(this.currentLayer);
        assignLeaves();
    }

    public Integer minimax(Node node, int height, boolean isMaximizingPlayer, int alpha, int beta){
        if (leaves.contains(node)) { return node.getScore().orElseThrow();}
        int bestValue;
        int score;
        if (isMaximizingPlayer) {
            bestValue = -999;
            for (Node childNode : node.getChildren()) {
                if (considerDouble) {
                    if (childNode.getParent() == root
                            && childNode.getMove() instanceof Move.SingleMove) { continue;}
                } else {
                    if (childNode.getParent() == root
                            && childNode.getMove() instanceof Move.DoubleMove) { continue;}
                }
                score = minimax(childNode, height - 1, false, alpha, beta);
                childNode.setScore(score);
                bestValue = Math.max(bestValue, score);
                alpha = Math.max(alpha, bestValue);
                //System.out.println("bestValue = " + bestValue);
                if (beta <= alpha) {break;}
            }
        } else {
            bestValue = 999;
            for (Node childNode : node.getChildren()) {
                score = minimax(childNode, height - 1, true, alpha, beta);
                childNode.setScore(score);
                //System.out.print("best score: " + bestValue + "child score: " + score);
                bestValue = Math.min(bestValue, score);
                //System.out.println("new best score: " + bestValue);
                beta = Math.min(beta, bestValue);
                if (beta <= alpha) {break;}
            }
        }
        return bestValue;
    }

    // reduce the depth of the tree
    // so new leaves are the parent nodes of old leaves
    public void reduceDepth() {
        Set<Node> newLeaves = new HashSet<>();
        for (Node node : leaves) {
            if (node.getParent() == root) { newLeaves.add(node); continue;}
            newLeaves.add(node.getParent());
        }
        leaves = newLeaves;
    }

    public void assignLeaves() {
         //get scores for leaves and broadcast values
        for (Node node : this.leaves) {
            Optional<Integer> nearestLocation = node.state.getDetectiveLocation((Piece.Detective) node.getNearestPiece());
            // check
            if (nearestLocation.isEmpty()) System.out.println("nearest location is empty");
            //Optional<Integer> nodeScore = node.getScore();
            if (node.winner.isEmpty()) {
                int score = ScoreCalculator.getScore(node.getDistances(), node.state, restOfRoundsToReveal(node.state), nearestLocation.orElseThrow(), node.getFreedom());
                node.setScore(score);
            }
        }
    }

    public static int restOfRoundsToReveal(Board.GameState state) {
        GameSetup setup = state.getSetup();
        int logCounter = 0;
        for (int i = 0; i < state.getMrXTravelLog().size() + 1; i++) {
            if (i != 0 && setup.moves.get(i - 1)) {logCounter = 0;}
            logCounter++;
        }
        return logCounter;
    }

    public static ImmutableValueGraph<Integer, Integer> createValueGraph(ImmutableValueGraph<Integer, ImmutableSet<ScotlandYard.Transport>> graph){
        MutableValueGraph<Integer, Integer> boardValueGraph = ValueGraphBuilder.undirected().build();
        // consider each node
        for (int sourceNode : graph.nodes()) {
            for (Integer destinationNode : graph.adjacentNodes(sourceNode)) {
                // give value to each possible transport
                boardValueGraph.putEdgeValue(sourceNode, destinationNode, 1);
            }
        }
        return ImmutableValueGraph.copyOf(boardValueGraph);
    }

    public void setConsiderDoubleToTrue() { considerDouble = true;}
    public void setConsiderDoubleToFalse() { considerDouble = false;}
    public Set<Node> getLeaves() { return this.leaves; }
    public List<Node> getFirstLayer() { return root.getChildren(); }
    public Set<Node> getCurrentLayer() { return this.currentLayer; }
    public int getDepth() { return this.depth; }
    public boolean getConsiderDouble() { return this.considerDouble; }
    public Node getRoot() { return this.root; }
    public int getCount() { return this.count; }
}
