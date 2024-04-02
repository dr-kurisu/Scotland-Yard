package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableSet;
import com.google.common.graph.ImmutableValueGraph;
import com.google.common.graph.MutableValueGraph;
import com.google.common.graph.ValueGraphBuilder;
import uk.ac.bris.cs.scotlandyard.model.Board;
import uk.ac.bris.cs.scotlandyard.model.Move;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard;

import java.util.*;

public class GameTree {
    private List<GameNode> leaves;
    private GameNode root;
    private List<GameNode> firstLayer;
    private List<GameNode> currentLayer;
    private ImmutableValueGraph<Integer, Integer> distanceGraph;
    GameTree(Board.GameState state, int depth){
        // MrX turn
        this.leaves = new ArrayList<>();
        this.currentLayer = new ArrayList<>();
        this.distanceGraph = createValueGraph(state.getSetup().graph);

        // add first layer of node
        this.root = new GameNode(state, false, null, distanceGraph);
        this.currentLayer.addAll(root.addChildren(distanceGraph));
        this.firstLayer = this.currentLayer;

        for (int i = 0; i < depth; i++) {
            List<GameNode> nextLayer = new ArrayList<>();
            for (GameNode node : this.currentLayer) {
                if (node.winner.isEmpty()) {
                    nextLayer.addAll(node.addChildren(distanceGraph));
                }else {
                    this.leaves.add(node);
                }
            }
            this.currentLayer = nextLayer;
        }
        this.leaves.addAll(this.currentLayer);
    }

    // define here cos winner
    public void calculateScore(GameNode node) {
        if (node.winner.isEmpty()) {
            int score = ScoreCalculator.getScore(distanceGraph, node.getState(), node.getMrXLocation(), node.getNearestLocation());
            node.setScore(score);
        }else {
            if (node.winner.asList().get(0).isMrX()) {
                node.setScore(999);
            }else {
                node.setScore(-999);
            }
        }
    }
    public void upPropagation() {
        this.currentLayer = this.leaves;
        for (GameNode node1 : currentLayer) {
            calculateScore(node1);
        }
        while (!this.currentLayer.isEmpty()) {
            List<GameNode> previousLayer = new ArrayList<>();
            for (GameNode node : currentLayer) {
                if (node.getParent() == null) continue;
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
                for (ScotlandYard.Transport t : graph.edgeValueOrDefault(sourceNode, destinationNode, ImmutableSet.of())) {
                    boardValueGraph.putEdgeValue(sourceNode, destinationNode, 1);
                }
            }
        }

        return ImmutableValueGraph.copyOf(boardValueGraph);
    }

    // getter
    public List<GameNode> getLeaves() { return this.leaves; }
    public List<GameNode> getFirstLayer() { return this.firstLayer; }
    public List<GameNode> getCurrentLayer() { return this.currentLayer; }

    public GameNode getRoot() { return this.root; }
}
