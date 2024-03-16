package uk.ac.bris.cs.scotlandyard.ui.ai;

import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.graph.MutableValueGraph;
import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.scotlandyard.model.*;

import com.google.common.graph.ImmutableValueGraph;
import com.google.common.graph.ValueGraphBuilder;

import java.util.*;


public class MyAi implements Ai {

	private int mrXLocation;
	private ImmutableSet<Integer> detectiveLocations;
	private ImmutableSet<Move> moves;
	private ImmutableValueGraph<Integer, Integer> valueGraph;
	@Nonnull @Override public String name() { return "Scottish ALPHA-GO"; }

	public ImmutableSet<Integer> getDetectiveLocations(Board board){
		List<Integer> locations = new ArrayList<>();
		for(Piece p : board.getPlayers()) {
			if (p.isDetective()) {
				var location = board.getDetectiveLocation((Piece.Detective)p).orElse(-1);
				if (location != -1){
					locations.add(location);
				}else{
					throw new RuntimeException("Detective does not exist");
				}
			}
		}
		return ImmutableSet.copyOf(locations);
	}

	public int getMrxLocation(Board board){
		for (Move move : moves){
			if (move.commencedBy().isMrX()){ return move.source(); }
		}
		throw new NullPointerException("No corresponding piece");
	}

	public void update(Board board){
		valueGraph = createValueGraph(board.getSetup().graph);
		moves = board.getAvailableMoves();
		// update mrXLocation
		mrXLocation = getMrxLocation(board);
		// update detectives locations
		detectiveLocations = getDetectiveLocations(board);
	}


	@Nonnull @Override public Move pickMove(
			@Nonnull Board board,
			Pair<Long, TimeUnit> timeoutPair) {
		// returns a random move, replace with your own implementation
		update(board); // update var in class
		int maxScore = 0;
		int score;
		int counter = 0;
		int index = 0;
		for (Move move : moves){
			ImmutableList<Integer> destinations = move.accept(new Move.Visitor<ImmutableList<Integer>>() {
				@Override
				public ImmutableList<Integer> visit(Move.SingleMove move) {
					return ImmutableList.of(move.destination);
				}
				@Override
				public ImmutableList<Integer> visit(Move.DoubleMove move) {
					return ImmutableList.of(move.destination1, move.destination2);
				}
			});
			score = scoringFunction(destinations.get(destinations.size() - 1), board);
			if (score > maxScore) {
				maxScore = score;
				index = counter;
			}
			counter++;
		}
		return moves.asList().get(index);
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
			}});

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
		System.out.println(path);
		return path;
	}

	public int findShortestDistance(int source, int destination) {
		return dijkstraAlgo(valueGraph, source, destination).size();
	}

	public int scoringFunction(int node, Board board){
		int min = 999;
		for (int location : detectiveLocations){
			int distance = findShortestDistance(node, location);
			if (distance < min) min = distance;
		}
		return min;
	}


}
