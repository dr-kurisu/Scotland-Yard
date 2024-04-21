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
    private int round;

    Tree(Board.GameState state, int round, int depth, int mrXLocation) {
        // MrX turn
        this.leaves = new HashSet<>();
        this.currentLayer = new HashSet<>();
        this.distanceGraph = createValueGraph(state.getSetup().graph);
        this.depth = depth;
        this.considerDouble = false;
        this.count = 0;
        this.round = round;
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
                    if (nodes != null) nextLayer.addAll(nodes);
                } else {
                    this.leaves.add(node);
                }
            }
            this.currentLayer = nextLayer;
        }
        // add last layer to leaves
        this.leaves.addAll(this.currentLayer);
        if (leaves.isEmpty()) {
            System.out.println("Leave is empty...");
        }
        assignLeaves();
    }

    public Integer minimax(Node node, int height, boolean isMaximizingPlayer, int alpha, int beta){
        if (leaves.contains(node)) { return node.getScore().orElseThrow(); }
        int bestValue;
        int score;
        if (isMaximizingPlayer) {
            bestValue = -999;
            for (Node childNode : node.getChildren()) {
                if (considerDouble) {
                    if (height == this.depth
                            && childNode.getMove() instanceof Move.SingleMove) { continue;}
                } else {
                    if (height == this.depth
                            && childNode.getMove() instanceof Move.DoubleMove) { continue;}
                }
                score = minimax(childNode, height - 1, false, alpha, beta);
                childNode.setScore(score);
                bestValue = Math.max(bestValue, childNode.getScore().orElseThrow());
                alpha = Math.max(alpha, bestValue);
                //System.out.println("bestValue = " + bestValue);
                if (beta <= alpha) {break;}
            }
        } else {
            bestValue = 999;
            for (Node childNode : node.getChildren()) {
                score = minimax(childNode, height - 1, true, alpha, beta);
                childNode.setScore(score);
                bestValue = Math.min(bestValue, childNode.getScore().orElseThrow());
                beta = Math.min(beta, bestValue);
                System.out.println("Score: " + score);
                if (beta <= alpha) {break;}

            }
        }
        count+=1;
        return bestValue;
    }

    // reduce the depth of the tree
    // so new leaves are the parent nodes of old leaves
    public void reduceDepth() {
        Set<Node> newLeaves = new HashSet<>();
        for (Node node : leaves) {
            if (node.getScore().orElseThrow() == 999) {
                newLeaves.add(node);
            }
            else{
                if (node.getParent() == root) continue;
                newLeaves.add(node.getParent());
            }
        }
        //if (newLeaves.isEmpty()) leaves = ImmutableSet.of(root);
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
                int score = ScoreCalculator.getScore(node.getDistances(), node.state, restOfRoundsToReveal(node.state, round), nearestLocation.orElseThrow(), node.getFreedom());
                node.setScore(score);
            }
            //System.out.println("leave Score: " + node.getScore());
        }
    }

    public static int restOfRoundsToReveal(Board.GameState state,int numberOfRound) {
        GameSetup setup = state.getSetup();
        int logCounter = 0;
        for (int i = 0; i < numberOfRound; i++) {
            if (i != 0 && setup.moves.get(i - 1)) {logCounter = 0;}
            logCounter++;
        }
        //System.out.println("round: " + numberOfRound + " log: " + logCounter);
        return logCounter;
    }

    //public void resetTree() {
    //    this.currentLayer = this.leaves;
    //    while (!this.currentLayer.isEmpty()) {
    //        Set<Node> newLayer = new HashSet<>();
    //        for (Node node : currentLayer) {
    //            node.score = Optional.empty();
    //            newLayer.add(node);
    //        }
    //        this.currentLayer = newLayer;
    //    }
    //}

    public ImmutableValueGraph<Integer, Integer> createValueGraph(ImmutableValueGraph<Integer, ImmutableSet<ScotlandYard.Transport>> graph){
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
