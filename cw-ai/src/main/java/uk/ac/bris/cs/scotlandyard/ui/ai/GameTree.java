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
    private List<GameNode> firstLayer;
    private ImmutableValueGraph<Integer, Integer> distanceGraph;
    GameTree(Board.GameState state, int depth){
        // MrX turn
        this.leaves = new ArrayList<>();
        this.distanceGraph = createValueGraph(state.getSetup().graph);

        // add first layer of node
        var moves = state.getAvailableMoves();
        for (Move move : moves) {
            this.leaves.add(new GameNode(state.advance(move), true, null));
        }
        this.firstLayer = this.leaves;
        for (int i = 0; i < depth; i++) {
            List<GameNode> newLeaves = new ArrayList<>();
            for (GameNode node : this.leaves) {
                newLeaves.addAll(node.addChildren(distanceGraph));
            }
            System.out.println("size1: " + this.leaves.size());
            this.leaves = newLeaves;
            System.out.println("size2: " + this.leaves.size());
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
}
