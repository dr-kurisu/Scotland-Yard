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
					if (piece.isMrX()) {
						return mrX.tickets().get(ticket);
					}
					// change!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
					if (piece.isDetective()) {
						for (Player p : detectives) {
							if (p.piece().equals(piece)) {
								return p.tickets().get(ticket);
							}
						}
					}
					throw new RuntimeException("Wrong TicketBoard");
				}
			};
			if (piece.isMrX()){ return Optional.of(tb); }
			for (Player p : detectives) {
				if (p.piece().equals(piece)) {
					return Optional.of(tb);
				}
			}
			return Optional.empty();
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
			throw new RuntimeException();
		}

		@Override
		public GameState advance(Move move) {
			throw new RuntimeException();
		}
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


