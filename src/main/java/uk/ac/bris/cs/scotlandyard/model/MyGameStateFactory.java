package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableSet;
import jdk.jshell.spi.ExecutionControl;
import uk.ac.bris.cs.scotlandyard.model.Board.GameState;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;

import java.awt.*;
import java.util.*;
import java.util.List;

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
				if (d.tickets().get(ScotlandYard.Ticket.SECRET) != 0) {throw new IllegalArgumentException("Wrong number of Bus tickets.");}
				if (d.tickets().get(ScotlandYard.Ticket.DOUBLE) != 0) {throw new IllegalArgumentException("Wrong number of Underground tickets.");}
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
			for (Piece piece : remaining) {
				if (piece.isMrX()) {
					moveSet.addAll(makeSingleMoves(setup, detectives, mrX, mrX.location()));
					if (mrX.tickets().get(ScotlandYard.Ticket.DOUBLE) > 0 && (!setup.moves.get(log.size()))) {
							moveSet.addAll(makeDoubleMoves(setup, detectives, mrX, mrX.location()));
					}
				} else {
					for (Player detective : detectives) {
						//if (detective.piece() == piece) {
						moveSet.addAll(makeSingleMoves(setup, detectives, detective, detective.location()));
						//}
						//moveSet.addAll(makeDoubleMove(setup, detectives, detective, detective.location()));
					}
				}
			}
			//}
			moves = moveSet.build();
			return moves;
		}

		@Override
		public GameState advance(Move move) {
			if(!moves.contains(move)) throw new IllegalArgumentException("Illegal move: "+move);
			if (move.commencedBy() == mrX.piece()){
				//ImmutableList.Builder<LogEntry> NewLogBuilder = ImmutableList.builder();
				//for (LogEntry entry : log) {
				//	NewLogBuilder.add(entry);
				//}
				//int EntryCounter = 1;
				//for (ScotlandYard.Ticket t : move.tickets()){
				//	if (EntryCounter++ % 5 == 3){
				//		NewLogBuilder.add(LogEntry.reveal(t, mrX.location()));
				//	}else{
				//		NewLogBuilder.add(LogEntry.hidden(t));
				//	}
				//}
			}

			for (Player detective : detectives){
				if (move.commencedBy() == detective.piece()){}
			}
			ImmutableSet.Builder<Piece> NewRemainingBuilder = ImmutableSet.builder();
			for (Piece p : remaining) {
				if (p != move.commencedBy()) NewRemainingBuilder.add(p);
			}

			GameState AdvancedGameState = new MyGameState(setup, NewRemainingBuilder.build(), log, mrX, detectives);
			return AdvancedGameState;
		}
	}

		private static Set<Move.SingleMove> makeSingleMoves(GameSetup setup, List<Player> detectives, Player player, int source){

			// TODO create an empty collection of some sort, say, HashSet, to store all the SingleMove we generate
			HashSet<Move.SingleMove> s = new HashSet<>();

			for(int destination : setup.graph.adjacentNodes(source)) {
				// TODO find out if destination is occupied by a detective
				//  if the location is occupied, don't add to the collection of moves to return
				boolean detectiveOccupied = false;
				for (Player d : detectives) {
					if (d.location() == destination) {
						detectiveOccupied = true;
					}
				}

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

	// TODO Below is the implementation of the double move.
	private static Set<Move.DoubleMove> makeDoubleMoves(GameSetup setup, List<Player> detectives, Player player, int source) {
		HashSet<Move.DoubleMove> s = new HashSet<>();
		Set<Move.SingleMove> firstMoves = makeSingleMoves(setup, detectives, player, source);

		for (Move.SingleMove firstMove : firstMoves) {
			Set<Move.SingleMove> secondMoves = makeSingleMoves(setup, detectives, player, firstMove.destination);
			for (Move.SingleMove secondMove : secondMoves) {
				if (firstMove.ticket != secondMove.ticket || (firstMove.ticket == secondMove.ticket && player.hasAtLeast(secondMove.ticket, 2))) {
					//if (source != secondMove.destination) {
						s.add(new Move.DoubleMove(
								player.piece(),
								source,
								firstMove.ticket,
								firstMove.destination,
								secondMove.ticket,
								secondMove.destination));
					//}
				}
			}
		}
		return s;
	}

	@Nonnull
	@Override
	public GameState build(
			GameSetup setup,
			Player mrX,
			ImmutableList<Player> detectives) {
		return new MyGameState(setup, ImmutableSet.of(Piece.MrX.MRX), ImmutableList.of(), mrX, detectives);
	}
}

