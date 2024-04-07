package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableSet;
import com.google.common.graph.ImmutableValueGraph;
import com.google.common.graph.MutableValueGraph;
import com.google.common.graph.ValueGraphBuilder;
import uk.ac.bris.cs.scotlandyard.model.Board;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard;

import java.util.ArrayList;
import java.util.List;

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
        this.root = new Node(state, false, null, mrXLocation, ScoreCalculator.getDistances(distanceGraph, mrXLocation), distanceGraph);
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

    public ImmutableValueGraph<Integer, Integer> createValueGraph(ImmutableValueGraph<Integer, ImmutableSet<ScotlandYard.Transport>> graph){
        MutableValueGraph<Integer, Integer> boardValueGraph = ValueGraphBuilder.undirected().build();

        // consider each node
        for (int sourceNode : graph.nodes()) {
            boardValueGraph.addNode(sourceNode);
            // consider each adjacent nodes
            for (Integer destinationNode : graph.adjacentNodes(sourceNode)) {
                // give value to each possible transport
                for (ScotlandYard.Transport t : graph.edgeValueOrDefault(sourceNode, destinationNode, ImmutableSet.of())) {
                    boardValueGraph.putEdgeValue(sourceNode, destinationNode, 1);
                }
            }
        }

        return ImmutableValueGraph.copyOf(boardValueGraph);
    }
    public List<Node> getLeaves() { return this.leaves; }
    public List<Node> getFirstLayer() { return this.firstLayer; }
    public List<Node> getCurrentLayer() { return this.currentLayer; }

    public Node getRoot() { return this.root; }}
