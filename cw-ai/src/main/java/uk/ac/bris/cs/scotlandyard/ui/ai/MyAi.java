package uk.ac.bris.cs.scotlandyard.ui.ai;

import java.awt.geom.Arc2D;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;

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
		var moves = board.getAvailableMoves();
		if (isMrXTurn(moves)) {
			System.out.println("Round: " + ++counter);
			// returns a random move, replace with your own implementation
			createState(board); // create new game state for each round
			if (moves.isEmpty()) { throw new RuntimeException("No available move"); }
			createTree(4, board);
			int miniMaxDepth = tree.getDepth();
			int max = -999;
			//Set<Node> origLeaves = tree.getLeaves();
			while (max <= -999 && miniMaxDepth > 0 && !tree.getLeaves().isEmpty()) {
				System.out.print(" Depth: " + miniMaxDepth);
				max = tree.minimax(tree.getRoot(), miniMaxDepth, !tree.getRoot().getType(), Integer.MIN_VALUE, Integer.MAX_VALUE);

				if (max <= -999) {
					System.out.print(" consider double ");
					tree.setConsiderDoubleToTrue();
					max = tree.minimax(tree.getRoot(), miniMaxDepth, !tree.getRoot().getType(), Integer.MIN_VALUE, Integer.MAX_VALUE);
				}
				if (max <= -999) {
					// not checked
					System.out.println("consider less depth");
					miniMaxDepth--;
					//createTree(miniMaxDepth, board);
					//tree.resetTree();
					tree.reduceDepth();
					//tree.cleanLeaf();
					tree.assignLeaves();
					//System.out.println("finished assign leaves");
					tree.setConsiderDoubleToFalse();
					//max = tree.minimax(tree.getRoot(), miniMaxDepth, !tree.getRoot().getType(), Integer.MIN_VALUE, Integer.MAX_VALUE);
				} else miniMaxDepth = -1;
				System.out.print(" finish");
				//System.out.println(" ");
				//System.out.println("max for single: " + max);
			}
			System.out.println("Max Score: " + max);
			System.out.println("Count: " + tree.getCount());

			List<Node> firstLayer = tree.getRoot().getChildren();

			for (Node node : firstLayer) {
				if (!tree.getConsiderDouble() && node.getMove() instanceof Move.DoubleMove) {
					continue;
				} else if (tree.getConsiderDouble() && node.getMove() instanceof Move.SingleMove) {
					continue;
				}
				System.out.print(node.getScore().orElseThrow() + " ");
			}
			for (Node node : firstLayer) {
				System.out.println("Consider Double: " + tree.getConsiderDouble());
				if (!tree.getConsiderDouble() && node.getMove() instanceof Move.DoubleMove) {continue;}
				else if (tree.getConsiderDouble() && node.getMove() instanceof Move.SingleMove) {continue;}
				System.out.print(node.getScore().orElseThrow() + " ");
				if (node.getScore().orElseThrow().equals(max)) {
					if (node.getMove() instanceof Move.DoubleMove) { counter++; }
					return node.getMove();
				}
			}
			throw new RuntimeException("Impossible");
		}else {
			var mrXlocation = AIUtil.getMrXLocation(board);
			if (mrXlocation.isEmpty()) {
				if (pickedMoves.isEmpty()) {
					valueGraph = Tree.createValueGraph(board.getSetup().graph);
					AIUtil util = new AIUtil(valueGraph);
					// case detectives do not know location of mrX
					// find the largest freedom place to go
					// previous location, next location
					Map<Integer, Integer> bestLocations = util.searchFromAllNextState(board, valueGraph, moves);
					Map<Integer, Move> moveMap = new HashMap<>();
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
			} else {
				System.out.println("round after reveal: " + roundsAfterReveal(board));
				// know location of mrX
				List<Move> movesList = new ArrayList<>(moves);
				if (pickedMoves.isEmpty()){
					AIUtil util = new AIUtil(valueGraph);
					int passRound = roundsAfterReveal(board);
					int[] distances = ScoreCalculator.getDistances(valueGraph,mrXlocation.orElseThrow());
					List<Integer> locations = AIUtil.getDetectiveLocations(board);
					List<Integer> visited = new ArrayList<>();
					List<Integer> newLocations = new ArrayList<>(locations);
					for (int l : locations) {
						if (distances[l - 1] > passRound + 1) {
							// unable to reach location of mrX
							// move toward mrX
							// 1. find piece of current location
							Piece p = AIUtil.getPieceFromLocation(board, l);
							// 2. find best move with the least cost
							Move bestMove = util.getBestMoveForPieceWithLongDistance(movesList, p, distances);
							//if (bestMove == null) {
							//	newLocations.remove(Integer.valueOf(l)); // could misguide
							//	Iterator<Move> iterator = movesList.iterator();
							//	while (iterator.hasNext()) {
							//		var move = iterator.next();
							//		if (move.commencedBy() == p) {
							//			iterator.remove();
							//		}
							//	}
							//	continue;
							//}
							// 3. add to pickedMove
							System.out.println("bestMove: " + bestMove);
							pickedMoves.add(bestMove);
							// 4. remove location from locations
							newLocations.remove(Integer.valueOf(l)); // could misguide
							visited.add(getDestination(bestMove));
							// 5. remove moves
							// go through and delete at the same time !
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
					Map<Integer, int[]> mrXPossibleLocaitons = util.mrXPossibleLocaitons(board, passRound+1, mrXlocation.orElseThrow());
					util.setPossibleLocations(board, locations);
					Set<Map<Integer, Integer>> detectivePossibleLocations = util.getPossibleLocations();
					// situation of win
					if (passRound == 0) {
						for (Move move : movesList) {
							if (getDestination(move) == mrXlocation.orElseThrow()) {
								System.out.println("win!!!");
								return move;
							}
						}
					}
					//System.out.println(detectivePossibleLocations);
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
			Move firstMove = pickedMoves.get(0);
			pickedMoves.remove(firstMove);
			return firstMove;
		}
	}

	public void createTree(int depth, Board board){
		tree = new Tree(state, counter,depth, getMrxLocation(board));
	}

	public boolean isMrXTurn(ImmutableSet<Move> moves) {
		return moves.asList().get(0).commencedBy().isMrX();
	}
	public int roundsAfterReveal(Board board) {
		int numberOfRound = board.getMrXTravelLog().size();
		return (numberOfRound - 3) % 5;
	}
}
