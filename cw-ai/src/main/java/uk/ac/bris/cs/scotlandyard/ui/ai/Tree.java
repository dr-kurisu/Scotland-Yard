package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableSet;
import com.google.common.graph.ImmutableValueGraph;
import com.google.common.graph.MutableValueGraph;
import com.google.common.graph.ValueGraphBuilder;
import uk.ac.bris.cs.scotlandyard.model.Board;
import uk.ac.bris.cs.scotlandyard.model.Move;
import uk.ac.bris.cs.scotlandyard.model.Piece;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard;

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

    Tree(Board.GameState state, int depth, int mrXLocation) {
        // MrX turn
        this.leaves = new HashSet<>();
        this.currentLayer = new HashSet<>();
        this.distanceGraph = createValueGraph(state.getSetup().graph);
        this.depth = depth;
        this.considerDouble = false;
        this.count = 0;
        // add first layer of node
        this.root = new Node(state, false, null, mrXLocation, ScoreCalculator.getDistances(distanceGraph, mrXLocation), null);
        this.currentLayer.addAll(root.addChildren(distanceGraph));
        this.firstLayer = this.currentLayer;

        for (int i = 0; i < depth; i++) {
            Set<Node> nextLayer = new HashSet<>();
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
        this.leaves.addAll(this.currentLayer);
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
                System.out.println("bestValue = " + bestValue);
                if (beta <= alpha) {break;}
            }
        } else {
            bestValue = 999;
            for (Node childNode : node.getChildren()) {
                score = minimax(childNode, height - 1, true, alpha, beta);
                childNode.setScore(score);
                bestValue = Math.min(bestValue, childNode.getScore().orElseThrow());
                beta = Math.min(beta, bestValue);
                if (beta <= alpha) {break;}
            }
        }
        count+=1;
        return bestValue;
    }

    public void assignLeaves() {
         //get score for leaves and broadcast values
        for (Node node : this.leaves) {
            Optional<Integer> nearestLocation = node.state.getDetectiveLocation((Piece.Detective) node.getNearestPiece());
            // check
            if (nearestLocation.isEmpty()) System.out.println("nearest location is empty");
            Optional<Integer> nodeScore = node.getScore();
            if (nodeScore.isEmpty()) {
                int score = ScoreCalculator.getScore(node.getDistances(), node.state, nearestLocation.orElseThrow(), node.getFreedom());
                node.setScore(score);
            }
        }
    }
    //public void upPropagation() {
    //    // refer to back propagation
    //    // get score for leaves and broadcast values
    //    for (Node node : this.leaves) {
    //        Optional<Integer> nearestLocation = node.state.getDetectiveLocation((Piece.Detective) node.getNearestPiece());
    //        // check
    //        if (nearestLocation.isEmpty()) System.out.println("nearest location is empty");
    //        Optional<Integer> nodeScore = node.getScore();
    //        if (nodeScore.isEmpty()) {
    //            int score = ScoreCalculator.getScore(node.getDistances(), node.state, nearestLocation.orElseThrow(), node.getFreedom());
    //            node.setScore(score);
    //        }
    //    }
    //    // check
    //    for (Node node : this.leaves) {  if (node.getScore().isEmpty()) throw new RuntimeException("pio"); }
//
    //    this.currentLayer = this.leaves;
    //    while (!this.currentLayer.isEmpty()) {
    //        Set<Node> previousLayer = new HashSet<>();
    //        for (Node node : currentLayer) {
    //            if (node.getParent() == root) continue;
    //            previousLayer.add(node.getParent());
    //            Optional<Integer> parentScore = node.getParent().getScore();
    //            int score = node.getScore().orElseThrow();
    //            if (parentScore.isEmpty()) {
    //                node.getParent().setScore(score);
    //            } else {
    //                if (node.getType()) {
    //                    if (score > parentScore.orElseThrow()) {
    //                        node.getParent().setScore(score);
    //                    }
    //                } else {
    //                    if (score < parentScore.orElseThrow()) {
    //                        node.getParent().setScore(score);
    //                    }
    //                }
    //            }
    //        }
    //        this.currentLayer = previousLayer;
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

    public void setConsiderDoubleToTrue() {
        considerDouble = true;
    }
    public Set<Node> getLeaves() { return this.leaves; }
    public List<Node> getFirstLayer() { return this.firstLayer.stream().toList(); }
    public Set<Node> getCurrentLayer() { return this.currentLayer; }
    public int getDepth() { return this.depth; }
    public boolean getConsiderDouble() { return this.considerDouble; }
    public Node getRoot() { return this.root; }
    public int getCount() { return this.count; }
}
