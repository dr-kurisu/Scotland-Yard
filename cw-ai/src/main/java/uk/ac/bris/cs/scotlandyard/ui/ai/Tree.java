package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableSet;
import com.google.common.graph.ImmutableValueGraph;
import com.google.common.graph.MutableValueGraph;
import com.google.common.graph.ValueGraphBuilder;
import uk.ac.bris.cs.scotlandyard.model.Board;
import uk.ac.bris.cs.scotlandyard.model.Piece;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Tree {
    private List<Node> leaves;
    private Node root;
    private List<Node> firstLayer;
    private List<Node> currentLayer;
    private ImmutableValueGraph<Integer, Integer> distanceGraph;

    Tree(Board.GameState state, int depth, int mrXLocation) {
        // MrX turn
        this.leaves = new ArrayList<>();
        this.currentLayer = new ArrayList<>();
        this.distanceGraph = createValueGraph(state.getSetup().graph);

        // add first layer of node
        this.root = new Node(state, false, null, mrXLocation, ScoreCalculator.getDistances(distanceGraph, mrXLocation));
        this.currentLayer.addAll(root.addChildren(distanceGraph));
        this.firstLayer = this.currentLayer;

        for (int i = 0; i < depth; i++) {
            List<Node> nextLayer = new ArrayList<>();
            for (Node node : this.currentLayer) {
                if (node.winner.isEmpty()) {
                    nextLayer.addAll(node.addChildren(distanceGraph));
                } else {
                    this.leaves.add(node);
                }
            }
            this.currentLayer = nextLayer;
        }
        this.leaves.addAll(this.currentLayer);
    }

    public void upPropagation() {
        // refer to back propagation
        // get score for leaves and broadcast values
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
        // check
        for (Node node : this.leaves) { assert !node.getScore().isEmpty(); }

        this.currentLayer = this.leaves;
        while (!this.currentLayer.isEmpty()) {
            List<Node> previousLayer = new ArrayList<>();
            for (Node node : currentLayer) {
                if (node.getParent() == root) continue;
                previousLayer.add(node.getParent());
                Optional<Integer> parentScore = node.getParent().getScore();
                int score = node.getScore().orElseThrow();
                if (parentScore.isEmpty()) {
                    node.getParent().setScore(score);
                } else {
                    if (node.getType()) {
                        if (score > parentScore.orElseThrow()) {
                            node.getParent().setScore(score);
                        }
                    } else {
                        if (score < parentScore.orElseThrow()) {
                            node.getParent().setScore(score);
                        }
                    }
                }
            }
            this.currentLayer = previousLayer;
        }
    }

    public ImmutableValueGraph<Integer, Integer> createValueGraph(ImmutableValueGraph<Integer, ImmutableSet<ScotlandYard.Transport>> graph){
        MutableValueGraph<Integer, Integer> boardValueGraph = ValueGraphBuilder.undirected().build();

        // consider each node
        for (int sourceNode : graph.nodes()) {
            boardValueGraph.addNode(sourceNode);
            // consider each adjacent nodes
            for (Integer destinationNode : graph.adjacentNodes(sourceNode)) {
                // give value to each possible transport
                //for (ScotlandYard.Transport t : graph.edgeValueOrDefault(sourceNode, destinationNode, ImmutableSet.of())) {
                boardValueGraph.putEdgeValue(sourceNode, destinationNode, 1);
                //}
            }
        }

        return ImmutableValueGraph.copyOf(boardValueGraph);
    }
    public List<Node> getLeaves() { return this.leaves; }
    public List<Node> getFirstLayer() { return this.firstLayer; }
    public List<Node> getCurrentLayer() { return this.currentLayer; }

    public Node getRoot() { return this.root; }}
