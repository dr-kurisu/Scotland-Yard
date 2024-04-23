package uk.ac.bris.cs.scotlandyard.ui.ai;

import java.awt.geom.Arc2D;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import javax.swing.*;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import io.atlassian.fugue.Pair;

import com.google.common.graph.ImmutableValueGraph;
import static uk.ac.bris.cs.scotlandyard.ui.ai.MoveModifier.getDestination;

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
	private List<Move> pickedMoves = new ArrayList<>();
	private Tree tree;
	private int counter = 0;  // counter for round number
	@Nonnull @Override public String name() { return "Scottish ALPHA-GO"; }

	/* Get the location of mrX by board
	 *
	 * @param board
	 * @return int location of mrX
	 */
	public static int getMrxLocation(Board board) {
		for (Move move : board.getAvailableMoves()){
			if (move.commencedBy().isMrX()){ return move.source(); }
		}
		throw new NullPointerException("No corresponding piece");
	}

	/* create a state without past log by board
	 *
	 * @param board
	 * @return null (assign value to private attribute)
	 */
	public void createState(Board board){
		var pieces = board.getPlayers();
		detectives = new ArrayList<>(); // what a stupid bug
		for (Piece piece : pieces) {
			Board.TicketBoard tb = board.getPlayerTickets(piece).orElseThrow();
			if (piece.isMrX()) {
				// create Tickets map for player
				ImmutableMap.Builder<ScotlandYard.Ticket, Integer> map = ImmutableMap.builder();
				map.put(ScotlandYard.Ticket.TAXI, tb.getCount(ScotlandYard.Ticket.TAXI));
				map.put(ScotlandYard.Ticket.DOUBLE, tb.getCount(ScotlandYard.Ticket.DOUBLE));
				map.put(ScotlandYard.Ticket.BUS, tb.getCount(ScotlandYard.Ticket.BUS));
				map.put(ScotlandYard.Ticket.SECRET, tb.getCount(ScotlandYard.Ticket.SECRET));
				map.put(ScotlandYard.Ticket.UNDERGROUND, tb.getCount(ScotlandYard.Ticket.UNDERGROUND));
				mrX = new Player(piece, map.build(), getMrxLocation(board));
			}
			if (piece.isDetective()) {
				// create Tickets map for player
				ImmutableMap.Builder<ScotlandYard.Ticket, Integer> map = ImmutableMap.builder();
				map.put(ScotlandYard.Ticket.TAXI, tb.getCount(ScotlandYard.Ticket.TAXI));
				map.put(ScotlandYard.Ticket.BUS, tb.getCount(ScotlandYard.Ticket.BUS));
				map.put(ScotlandYard.Ticket.UNDERGROUND, tb.getCount(ScotlandYard.Ticket.UNDERGROUND));
				detectives.add(new Player(piece, map.build(), board.getDetectiveLocation((Piece.Detective) piece).orElseThrow()));
			}
		}
		// create new state by board
		state = new MyGameStateFactory().build(board.getSetup(), mrX, ImmutableList.copyOf(detectives));
	}

	/* pick a move from moves by AI
	 *
	 * @param board
	 * @param timeoutPair (limit of time?)
	 * return Move
	 */
	@Nonnull @Override public Move pickMove(
			@Nonnull Board board,
			Pair<Long, TimeUnit> timeoutPair) {
		var moves = board.getAvailableMoves();
		/* ** MrX AI ** */
		if (isMrXTurn(moves)) {
			pickedMoves = new ArrayList<>();  // for restarting bug

			System.out.println("Round: " + ++counter);
			// returns a random move, replace with your own implementation
			createState(board); // create new game state for each round
			if (moves.isEmpty()) { throw new RuntimeException("No available move"); }
			createTree(4, board);
			int miniMaxDepth = tree.getDepth();
			int max = -999;
			while (max <= -999 && miniMaxDepth > 0 && !tree.getLeaves().isEmpty()) {
				max = tree.minimax(tree.getRoot(), miniMaxDepth, !tree.getRoot().getType(), Integer.MIN_VALUE, Integer.MAX_VALUE);

				if (max <= -999) {
					tree.setConsiderDoubleToTrue();
					max = tree.minimax(tree.getRoot(), miniMaxDepth, !tree.getRoot().getType(), Integer.MIN_VALUE, Integer.MAX_VALUE);
				}
				if (max <= -999) {
					// not checked
					miniMaxDepth--;
					tree.reduceDepth();
					tree.assignLeaves();
					tree.setConsiderDoubleToFalse();
				} else miniMaxDepth = -1;
			}
			List<Node> firstLayer = tree.getRoot().getChildren();

			for (Node node : firstLayer) {
				if (!tree.getConsiderDouble() && node.getMove() instanceof Move.DoubleMove) {continue;}
				else if (tree.getConsiderDouble() && node.getMove() instanceof Move.SingleMove) {continue;}
				if (node.getScore().orElseThrow().equals(max)) {
					if (node.getMove() instanceof Move.DoubleMove) { counter++; }
					return node.getMove();
				}
			}

			throw new RuntimeException("Impossible");

		}
		else {
			/* ** Detective AI ** */
			var mrXlocation = AIUtil.getMrXLocation(board);
			// mrX has not been revealed
			if (mrXlocation.isEmpty()) {
				moveWithoutMrXLocation(board, moves);
			}
			// mrX has been revealed
			else {
				int passedRound = roundsAfterReveal(board);
				// situation of immediate win
				if (passedRound == 0) {
					for (Move move : moves) {
						if (getDestination(move) == mrXlocation.orElseThrow()) {
							System.out.println("win!!!");
							return move;
						}
					}
				}
				moveWithMrXLocation(board, moves, mrXlocation.orElseThrow());
			}
			Move firstMove = pickedMoves.get(0);
			pickedMoves.remove(firstMove);
			return firstMove;
		}
	}

	/* The situation detectives do not know location of Mr.X
	 * Add move to Picked Move by total freedom
	 *
	 * @param board
	 * @param available moves
	 * @return null (assign private attribute pickedMoves)
	 */
	public void moveWithoutMrXLocation(Board board, ImmutableSet<Move> moves) {
		if (pickedMoves.isEmpty()) {
			valueGraph = Tree.createValueGraph(board.getSetup().graph);
			AIUtil util = new AIUtil(valueGraph);
			// find the place having the largest freedom
			// map: previous location, next location
			Map<Integer, Integer> bestLocations = util.searchFromAllNextState(board, moves);
			// map: destination, move
			Map<Integer, Move> moveMap = new HashMap<>();
			// map: destination, cost
			Map<Integer, Integer> costMap = new HashMap<>();
			for (Move move : moves) {
				int des = getDestination(move);
				int src = move.source();
				if (bestLocations.containsKey(src) && bestLocations.get(src) == des) {
					if (!moveMap.containsKey(des)) {
						moveMap.put(des, move);
						costMap.put(des, util.calCost(move));
					} else if (util.calCost(move) < costMap.get(des)) {
						costMap.put(des, util.calCost(move));
						moveMap.put(des, move);
					}
				}
			}
			pickedMoves.addAll(moveMap.values());
		}
	}
	/* The situation detectives know location of Mr.X
	 * Add move to Picked Move by rate of overlap of mrX possible locations
	   and detective possible locations
	 *
	 * @param board
	 * @param available moves
	 * @return null (assign private attribute pickedMoves)
	 */
	public void moveWithMrXLocation(Board board, ImmutableSet<Move> moves, int mrXLocation) {
		// System.out.println("round after reveal: " + roundsAfterReveal(board));
		// know location of mrX
		List<Move> movesList = new ArrayList<>(moves);
		if (pickedMoves.isEmpty()){
			AIUtil util = new AIUtil(valueGraph);
			int passRound = roundsAfterReveal(board);
			// situation of win
			int[] distances = ScoreCalculator.getDistances(valueGraph, mrXLocation);
			// add detective location to locations if they still have move
			List<Integer> locations = AIUtil.getDetectiveLocations(board, moves);
			List<Integer> visited = new ArrayList<>();
			List<Integer> newLocations = new ArrayList<>(locations);
			for (int l : locations) {
				if (distances[l - 1] > passRound + 1) {
					// when they are unable to reach location of mrX, move toward mrX
					// find piece of current location
					Piece p = AIUtil.getPieceFromLocation(board, l);
					// find best move with the least cost
					Move bestMove = util.getBestMoveForPieceWithLongDistance(movesList, p, distances);
					// add to pickedMove
					pickedMoves.add(bestMove);
					// remove location from locations
					newLocations.remove(Integer.valueOf(l)); // could misguide
					visited.add(getDestination(bestMove));
					// remove moves (go through and delete at the same time !)
					Iterator<Move> iterator = movesList.iterator();
					while (iterator.hasNext()) {
						var move = iterator.next();
						if (move.commencedBy()==p || getDestination(move) == getDestination(bestMove)) {
							iterator.remove();
						}
					}
				}
			}
			locations = newLocations;
			// get the best map of previous locations and next locations
			Set<Integer> mrXPossibleLocaitons = util.mrXPossibleLocaitons(board, passRound, mrXLocation, locations);
			util.setPossibleLocations(board, locations);
			Set<Map<Integer, Integer>> detectivePossibleLocations = util.getPossibleLocations();
			int max = -1;
			Map<Integer, Integer> bestLocations = null;
			for (Map<Integer, Integer> combine : detectivePossibleLocations) {
				if (!util.isReachable(combine, visited, movesList, board)) continue;
				int score = util.compareOverlap(combine, mrXPossibleLocaitons);
				if (score > max) {
					max = score;
					bestLocations = combine;
				}
			}
			System.out.println("max overlap: " + max);
			System.out.println("bestLocation: " + bestLocations);
			// when all detectives are far away from mrX, this could be null
			if (bestLocations != null) {
				// add bestLocations to pickedMoves
				for (Map.Entry<Integer, Integer> entry : bestLocations.entrySet()) {
					int src = entry.getKey();
					int des = entry.getValue();
					Piece p = AIUtil.getPieceFromLocation(board, src);
					int cost = 999;
					Move bestMove = null;
					for (Move move : movesList) {
						if (move.commencedBy() == p && getDestination(move) == des && AIUtil.calCost(move) < cost) {
							cost = AIUtil.calCost(move);
							bestMove = move;
						}
					}
					pickedMoves.add(bestMove);
				}
			}
		}
	}

	/* create game tree for Mr.X's AI to predict state of game
	 *
	 * @param depth of tree
	 * @param board
	 * @return null (assign private attribute tree)
	 */
	public void createTree(int depth, Board board){
		tree = new Tree(state, counter,depth, getMrxLocation(board));
	}

	/* judge if it is mrX's turn by moves
	 *
	 * @param available moves
	 * @param board
	 * @return boolean (it is mrX's turn)
	 */
	public boolean isMrXTurn(ImmutableSet<Move> moves) {
		return moves.asList().get(0).commencedBy().isMrX();
	}
	/* get rounds after revealing
	 *
	 * @param board
	 * @return number of rounds after revealing
	 */
	public int roundsAfterReveal(Board board) {
		int numberOfRound = board.getMrXTravelLog().size();
		return (numberOfRound - 3) % 5;
	}
}
