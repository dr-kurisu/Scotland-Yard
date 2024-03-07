package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableSet;
import jdk.jshell.spi.ExecutionControl;
import uk.ac.bris.cs.scotlandyard.model.Board.GameState;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;

import java.util.*;

/**
 * cw-model
 * Stage 1: Complete this class
 */
public final class MyGameStateFactory implements Factory<GameState> {
	private final class MyGameState implements GameState {
		private GameSetup setup;
		private ImmutableSet<Piece> remaining;
		private ImmutableList<LogEntry> log;
		private Player mrX;
		private List<Player> detectives;
		private ImmutableSet<Move> moves;
		private ImmutableSet<Piece> winner;

		private MyGameState(final GameSetup setup,
						   final ImmutableSet<Piece> remaining,
						   final ImmutableList<LogEntry> log,
						   final Player mrX,
						   final List<Player> detectives) {
			this.setup = setup;
			this.remaining = remaining;
			this.log = log;
			this.mrX = mrX;
			this.detectives = detectives;
			// this.winner = null;
			if(setup.graph.nodes().isEmpty()) throw new IllegalArgumentException("Graph is empty!");
			if(setup.moves.isEmpty()) throw new IllegalArgumentException("Moves is empty!");
			for (Player d : detectives) {
				if (d.tickets().get(ScotlandYard.Ticket.SECRET) != 0) {throw new IllegalArgumentException("Wrong number of Secret tickets.");}
				if (d.tickets().get(ScotlandYard.Ticket.DOUBLE) != 0) {throw new IllegalArgumentException("Wrong number of Double tickets.");}
			}

			// Null Check
			if(setup == null || remaining == null || log == null || mrX == null || detectives == null) {
				throw new NullPointerException("Some game states are null.");
			}

			// LocationOverlap Check
			HashSet<Integer> locations = new HashSet<>();
			for(Player d : detectives){
				locations.add(d.location());
			}
			if(locations.size() != detectives.size()){
				throw new IllegalArgumentException("Detective's location overlap");
			}
		}

		@Nonnull
		@Override
		public GameSetup getSetup() { return setup;	}
		@Override
		public ImmutableSet<Piece> getPlayers() {
			ImmutableSet.Builder<Piece> builder = ImmutableSet.builder();
			builder.add(mrX.piece());
			for (Player detective : detectives) {
				builder.add(detective.piece());
			}
			return builder.build();
		}

		@Nonnull
		@Override
		public Optional<Integer> getDetectiveLocation(Piece.Detective detective) {
			for (Player d : detectives) {
				if (d.piece().equals(detective)) {
					return Optional.of(d.location());
				}
			}
			return Optional.empty();
		}

		@Nonnull
		@Override
		public Optional<TicketBoard> getPlayerTickets(Piece piece) {
			TicketBoard tb = new TicketBoard() {
				@Override
				public int getCount(@Nonnull ScotlandYard.Ticket ticket) {
					if (piece.isMrX()) { return mrX.tickets().get(ticket); }
					if (piece.isDetective()) {
						for (Player p : detectives) {
							if (p.piece().equals(piece)) {
								return p.tickets().get(ticket);
							}
						}
					}
					return -1; // if not ticket not found, then invalid count.
				}
			};
			// if not found, return empty.
			if (tb.getCount(ScotlandYard.Ticket.SECRET) == -1) return Optional.empty();
			return Optional.of(tb);
		}

		@Nonnull
		@Override
		public ImmutableList<LogEntry> getMrXTravelLog() {
			throw new RuntimeException();
		}

		@Nonnull
		@Override
		public ImmutableSet<Piece> getWinner() {
			return ImmutableSet.of();
		}

		@Nonnull
		@Override
		public ImmutableSet<Move> getAvailableMoves() {
			ImmutableSet.Builder<Move> moveSet = ImmutableSet.builder();
			//for (Piece piece : remaining) {
				for (Player detective : detectives) {
					//if (detective.piece() == piece) {
						moveSet.addAll(makeSingleMoves(setup, detectives, detective, detective.location()));
					//}
					//moveSet.addAll(makeDoubleMove(setup, detectives, detective, detective.location()));
				}
			//}
			return moveSet.build();
			//throw new RuntimeException();
		}

		@Override
		public GameState advance(Move move) {
			if(!moves.contains(move)) throw new IllegalArgumentException("Illegal move: "+move);
			//ImmutableSet<Piece> newRemaining = new ImmutableSet<>();


			//ImmutableSet.Builder<Piece> newRemaining = ImmutableSet.builder();
			//for (Piece piece : remaining) {
			//	if (move.commencedBy() != piece) {
			//		newRemaining.add(piece);
			//	}
			//}
			//GameState newGameState = new MyGameState(setup, newRemaining.build(), log, mrX, detectives);
//
			//return newGameState;

			throw new RuntimeException();
		}

		private static Set<Move.SingleMove> makeSingleMoves(GameSetup setup, List<Player> detectives, Player player, int source){

			// TODO create an empty collection of some sort, say, HashSet, to store all the SingleMove we generate
			HashSet<Move.SingleMove> s = new HashSet<>();

			for(int destination : setup.graph.adjacentNodes(source)) {
				// TODO find out if destination is occupied by a detective
				//  if the location is occupied, don't add to the collection of moves to return
				boolean detectiveOccupied = false;
				for (Player d : detectives) {
					if (d.location() == destination) {detectiveOccupied = true;}
				}
				// if (!detectiveOccupied) s.add(new Move.SingleMove());

				for (ScotlandYard.Transport t : setup.graph.edgeValueOrDefault(source, destination, ImmutableSet.of())) {
					// TODO find out if the player has the required tickets
					//  if it does, construct a SingleMove and add it the collection of moves to return
					if (player.tickets().get(t.requiredTicket()) > 0 && !detectiveOccupied) {
						s.add(new Move.SingleMove(player.piece(),source, t.requiredTicket(), destination));
					}
				}

				// TODO consider the rules of secret moves here
				//  add moves to the destination via a secret ticket if there are any left with the player
				if (player.tickets().get(ScotlandYard.Ticket.SECRET) > 0 && !detectiveOccupied) {
					s.add(new Move.SingleMove(player.piece(),source, ScotlandYard.Ticket.SECRET, destination));
				}
			}

			// TODO return the collection of moves
			return s;
		}
	}

	// TODO Below is the implementation of the double move.

	//private static Set<Move.DoubleMove> makeDoubleMove(GameSetup setup, List<Player> detectives, Player player, int source) {
	//	HashSet<Move.DoubleMove> s = new HashSet<>();
//
	//	for(int destination : setup.graph.adjacentNodes(source)) {
	//		// TODO find out if destination is occupied by a detective
	//		//  if the location is occupied, don't add to the collection of moves to return
	//		boolean detectiveOccupied = false;
	//		for (Player d : detectives) {
	//			if (d.location() == destination) {detectiveOccupied = true;}
	//		}
	//		// if (!detectiveOccupied) s.add(new Move.SingleMove());
//
	//		for (ScotlandYard.Transport t : setup.graph.edgeValueOrDefault(source, destination, ImmutableSet.of())) {
	//			// TODO find out if the player has the required tickets
	//			//  if it does, construct a SingleMove and add it the collection of moves to return
	//			if (player.tickets().get(t.requiredTicket()) > 0 && !detectiveOccupied) {
	//				//s.add(new Move.DoubleMove(player.piece(),source, t.requiredTicket(), destination));
	//			}
	//		}
//
	//		// TODO consider the rules of secret moves here
	//		//  add moves to the destination via a secret ticket if there are any left with the player
	//		if (player.tickets().get(ScotlandYard.Ticket.SECRET) > 0 && !detectiveOccupied) {
	//			//s.add(new Move.DoubleMove(player.piece(),source, ScotlandYard.Ticket.SECRET, destination));
	//		}
	//	}
//
	//	// TODO return the collection of moves
	//	return s;
	//}

	@Nonnull
	@Override
	public GameState build(
			GameSetup setup,
			Player mrX,
			ImmutableList<Player> detectives) {
		return new MyGameState(setup, ImmutableSet.of(Piece.MrX.MRX), ImmutableList.of(), mrX, detectives);
	}
}


