package uk.ac.bris.cs.scotlandyard.ui.ai;

import java.util.Random;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableSet;
import com.google.common.graph.MutableValueGraph;
import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.scotlandyard.model.Ai;
import uk.ac.bris.cs.scotlandyard.model.Board;
import uk.ac.bris.cs.scotlandyard.model.Move;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard;

import com.google.common.graph.EndpointPair;
import com.google.common.graph.ImmutableValueGraph;
import com.google.common.graph.ImmutableValueGraph.Builder;
import com.google.common.graph.ValueGraphBuilder;

import java.util.*;


public class MyAi implements Ai {
	@Nonnull @Override public String name() { return "Scottish ALPHA-GO"; }

	@Nonnull @Override public Move pickMove(
			@Nonnull Board board,
			Pair<Long, TimeUnit> timeoutPair) {
		// returns a random move, replace with your own implementation
		var moves = board.getAvailableMoves().asList();
		ImmutableValueGraph<Integer, Integer> valueGraph = createValueGraph(board.getSetup().graph);
		return moves.get(new Random().nextInt(moves.size()));
	}

	public ImmutableValueGraph<Integer, Integer> createValueGraph(ImmutableValueGraph<Integer, ImmutableSet<ScotlandYard.Transport>> graph){
		MutableValueGraph<Integer, Integer> boardValueGraph =
				ValueGraphBuilder.undirected()
						.build();

		// consider each node
		for (Integer sourceNode : graph.nodes()) {
			boardValueGraph.addNode((Integer) sourceNode);
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

	public int findShortestPath(int source, int destination) {
		// initialise
		return 0;
	}
}
