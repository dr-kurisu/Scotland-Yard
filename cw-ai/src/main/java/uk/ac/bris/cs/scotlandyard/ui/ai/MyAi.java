package uk.ac.bris.cs.scotlandyard.ui.ai;

import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.graph.MutableValueGraph;
import io.atlassian.fugue.Pair;

import com.google.common.graph.ImmutableValueGraph;
import com.google.common.graph.ValueGraphBuilder;

import java.util.*;

import uk.ac.bris.cs.scotlandyard.model.Move;
import uk.ac.bris.cs.scotlandyard.model.Player;
import uk.ac.bris.cs.scotlandyard.model.Board;
import uk.ac.bris.cs.scotlandyard.model.Ai;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard;
import uk.ac.bris.cs.scotlandyard.model.Piece;
import uk.ac.bris.cs.scotlandyard.model.MyGameStateFactory;


public class MyAi implements Ai {
	private ImmutableValueGraph<Integer, Integer> valueGraph;
	private Player mrX;
	private List<Player> detectives = new ArrayList<>();
	private Board.GameState state;
	private Tree tree;
	private int counter = 1;  // counter for round number
	@Nonnull @Override public String name() { return "Scottish ALPHA-GO"; }

	public int getMrxLocation(Board board){
		for (Move move : board.getAvailableMoves()){
			if (move.commencedBy().isMrX()){ return move.source(); }
		}
		throw new NullPointerException("No corresponding piece");
	}

	public void update(Board board){
		valueGraph = createValueGraph(board.getSetup().graph);
		if (state == null) {
			var pieces = board.getPlayers();
			for (Piece piece : pieces) {
				Board.TicketBoard tb = board.getPlayerTickets(piece).orElseThrow();
				if (piece.isMrX()) {
					ImmutableMap.Builder<ScotlandYard.Ticket, Integer> map = ImmutableMap.builder();
					map.put(ScotlandYard.Ticket.TAXI, tb.getCount(ScotlandYard.Ticket.TAXI));
					map.put(ScotlandYard.Ticket.DOUBLE, tb.getCount(ScotlandYard.Ticket.DOUBLE));
					map.put(ScotlandYard.Ticket.BUS, tb.getCount(ScotlandYard.Ticket.BUS));
					map.put(ScotlandYard.Ticket.SECRET, tb.getCount(ScotlandYard.Ticket.SECRET));
					map.put(ScotlandYard.Ticket.UNDERGROUND, tb.getCount(ScotlandYard.Ticket.UNDERGROUND));
					mrX = new Player(piece, map.build(), getMrxLocation(board));
				}
				if (piece.isDetective()) {
					ImmutableMap.Builder<ScotlandYard.Ticket, Integer> map = ImmutableMap.builder();
					map.put(ScotlandYard.Ticket.TAXI, tb.getCount(ScotlandYard.Ticket.TAXI));
					map.put(ScotlandYard.Ticket.BUS, tb.getCount(ScotlandYard.Ticket.BUS));
					map.put(ScotlandYard.Ticket.UNDERGROUND, tb.getCount(ScotlandYard.Ticket.UNDERGROUND));
					detectives.add(new Player(piece, map.build(), board.getDetectiveLocation((Piece.Detective) piece).orElseThrow()));
				}
			}
			state = new MyGameStateFactory().build(board.getSetup(), mrX, ImmutableList.copyOf(detectives));
		}
	}

	public Move minimax(Tree tree){
		int max = 0;
		for (Node node : tree.getLastLevel()) {
			var score = scoring(node);
			System.out.print(score + " ");
			if (score > max) { max = score; }
			node.setScore(score);
		}
		System.out.println();

		return tree.getRoot().getState().getAvailableMoves().asList().get(0);
	}

	public int scoring(Node node) {
		int score = 999;
		for (Piece p : node.getState().getPlayers()) {
			if (p.isDetective()){
				int location = node.getState().getDetectiveLocation((Piece.Detective) p).orElseThrow();
				int distance = Tree.findShortestDistance(valueGraph, location, node.getMrXLocation());
				if (distance < score) {
					score = distance;
				}
			}
		}
		return score;
	}
	@Nonnull @Override public Move pickMove(
			@Nonnull Board board,
			Pair<Long, TimeUnit> timeoutPair) {
		// returns a random move, replace with your own implementation
		update(board); // update var in class
		createGameTree(2);
		System.out.print(counter++ + " ");
		System.out.println(tree.getLastLevel().size());
		return minimax(tree);
		/*
		int maxScore = 0;
		int score;
		int counter = 0;
		int index = 0;
		for (Move move : board.getAvailableMoves()){
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
			score = scoringFunction(destinations.get(destinations.size() - 1));
			if (score > maxScore) {
				maxScore = score;
				index = counter;
			}
			counter++;
		}
		*/
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

	public void createGameTree(int depth){
		tree = new Tree(new Node(state,null, -1, mrX.location()), depth, valueGraph);
	}
}
