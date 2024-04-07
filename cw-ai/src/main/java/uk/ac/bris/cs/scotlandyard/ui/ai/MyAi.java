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

	public static int getMrxLocation(Board board) {
		for (Move move : board.getAvailableMoves()){
			if (move.commencedBy().isMrX()){ return move.source(); }
		}
		throw new NullPointerException("No corresponding piece");
	}

	public void createState(Board board){
		var pieces = board.getPlayers();
		detectives = new ArrayList<>(); // what a stupid bug
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

	@Nonnull @Override public Move pickMove(
			@Nonnull Board board,
			Pair<Long, TimeUnit> timeoutPair) {
		// returns a random move, replace with your own implementation
		createState(board); // create new game state for each round
		var moves = state.getAvailableMoves();
		if (moves.isEmpty()) { throw new RuntimeException("No available move"); }
		// if (!isMrXTurn(moves)) { System.out.println("enter"); return null; }
		createTree(2, board);
		System.out.println("Size of currentLayer1: " + tree.getCurrentLayer().size());
		// tree.upPropagation();
		System.out.println("Round: " + counter++);
		//System.out.println("maxScore: " + tree.getRoot().getScore().orElseThrow());
		//for (Node node: tree.getFirstLayer()) {
		//	System.out.print(node.getScore().orElseThrow() + " ");
		//}
		//System.out.println();
		//System.out.println("Size of Leaves: " + tree.getLeaves().size());
		//System.out.println("First Layer Size: " + tree.getFirstLayer().size());
		//for (int i = 0; i < tree.getFirstLayer().size(); i++) {
		//	Node node = tree.getFirstLayer().get(i);
		//	if (node.getScore().orElseThrow().equals(tree.getRoot().getScore().orElseThrow())) {
		//		return moves.asList().get(i);
		//	}
		//}
		throw new RuntimeException("Impossible");
	}

	public void createTree(int depth, Board board){
		tree = new Tree(state, depth, getMrxLocation(board));
	}

	public boolean isMrXTurn(ImmutableSet<Move> moves) {
		return moves.asList().get(0).commencedBy().isMrX();
	}
}
