package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableSet;
import com.google.common.graph.ImmutableValueGraph;
import com.google.common.graph.MutableValueGraph;
import com.google.common.graph.ValueGraphBuilder;
import uk.ac.bris.cs.scotlandyard.model.*;

import java.util.*;

/*
 * Class tree is used to represent the connection between game states.
 * It is possible to get children game state by advance of parent game state
 */
public class Tree {
    private Set<Node> leaves;
    private final Node root;
    private final int depth;
    private boolean considerDouble;
    private final List<Integer> freedomCoef;
    Tree(Board.GameState state, int depth, int mrXLocation, List<Integer> freedomCoef) {
        // MrX turn
        this.leaves = new HashSet<>();
        this.depth = depth;
        this.considerDouble = false;
        this.freedomCoef = freedomCoef;
        // initialise the distance graph, help to process Dijkstra algorithm
        ImmutableValueGraph<Integer, Integer> distanceGraph = createValueGraph(state.getSetup().graph);
        // initialise root
        this.root = new Node(state, false, null, mrXLocation, ScoreCalculator.getDistances(distanceGraph, mrXLocation), null, ImmutableSet.of());
        // set current layer to root
        Set<Node> currentLayer = new HashSet<>();
        currentLayer.add(this.root);

        // create the tree to the corresponding depth.
        for (int i = 0; i < depth; i++) {
            Set<Node> nextLayer = new HashSet<>();
            // goes through each node of next layer
            // add children nodes if no winner appears
            for (Node node : currentLayer) {
                if (node.winner.isEmpty()) {
                    var nodes = node.addChildren(distanceGraph);
                    if (nodes == null) this.leaves.add(node);
                    if (nodes != null) nextLayer.addAll(nodes);
                } else {
                    this.leaves.add(node);
                }
            }
            currentLayer = nextLayer;
        }
        // add last layer to leaves
        this.leaves.addAll(currentLayer);
        assignLeaves();
    }

    // minimax algorithm with alpha-beta pruning to find the max value
    public Integer minimax(Node node, int height, boolean isMax, int alpha, int beta){
        // return the score in node if it's a leave
        if (leaves.contains(node)) { return node.getScore().orElseThrow();}

        int bestValue;
        int score;
        if (isMax) {
            // initialise alpha
            bestValue = -999;
            // iterate through all children nodes
            for (Node childNode : node.getChildren()) {
                // skip if it is not supposed to be considered
                if (considerDouble) {
                    if (childNode.getParent() == root
                            && childNode.getMove() instanceof Move.SingleMove) { continue;}
                } else {
                    if (childNode.getParent() == root
                            && childNode.getMove() instanceof Move.DoubleMove) { continue;}
                }
                // use recursion to get to the leaves
                score = minimax(childNode, height - 1, false, alpha, beta);
                childNode.setScore(score);
                bestValue = Math.max(bestValue, score);
                alpha = Math.max(alpha, bestValue);
                // break when alpha is greater or equal to beta
                if (beta <= alpha) {break;}
            }
        } else {
            // similar approach as above but finding minimum instead.
            // consider double is not needed because min is always detectives move, and they
            // don't have double moves.
            bestValue = 999;
            for (Node childNode : node.getChildren()) {
                score = minimax(childNode, height - 1, true, alpha, beta);
                childNode.setScore(score);
                bestValue = Math.min(bestValue, score);
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
        // reassign scores to the new leaves;
        assignLeaves();
    }

    public void assignLeaves() {
         //get scores for leaves and broadcast values
        for (Node node : this.leaves) {
            Optional<Integer> nearestLocation = node.state.getDetectiveLocation((Piece.Detective) node.getNearestPiece());
            // check
            if (nearestLocation.isEmpty()) System.out.println("nearest location is empty");
            // only assign scores when winner is empty
            // otherwise will be assigned as -999 or 999 when created.
            if (node.winner.isEmpty()) {
                int score = ScoreCalculator.getScore(
                        node.getDistances(),
                        node.state,
                        freedomCoef.get(node.state.getMrXTravelLog().size()),
                        nearestLocation.orElseThrow(),
                        node.getFreedom());
                node.setScore(score);
            }
        }
    }

    // this function creates the value graph of the game map given
    public static ImmutableValueGraph<Integer, Integer> createValueGraph(ImmutableValueGraph<Integer, ImmutableSet<ScotlandYard.Transport>> graph){
        MutableValueGraph<Integer, Integer> boardValueGraph = ValueGraphBuilder.undirected().build();
        // consider each node
        for (int sourceNode : graph.nodes()) {
            for (Integer destinationNode : graph.adjacentNodes(sourceNode)) {
                // give a value of 1 to each edge
                boardValueGraph.putEdgeValue(sourceNode, destinationNode, 1);
            }
        }
        return ImmutableValueGraph.copyOf(boardValueGraph);
    }

    public void setConsiderDoubleToTrue() { considerDouble = true;}
    public void setConsiderDoubleToFalse() { considerDouble = false;}
    public Set<Node> getLeaves() { return this.leaves; }
    public int getDepth() { return this.depth; }
    public boolean getConsiderDouble() { return this.considerDouble; }
    public Node getRoot() { return this.root; }
}
