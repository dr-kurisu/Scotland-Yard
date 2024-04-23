package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableSet;
import uk.ac.bris.cs.scotlandyard.model.Board.GameState;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;

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
			// initialise the game state
			this.setup = setup;
			this.remaining = remaining;
			this.log = log;
			this.mrX = mrX;
			this.detectives = detectives;
			this.winner = ImmutableSet.of();
			// checks
			if(setup.graph.nodes().isEmpty()) throw new IllegalArgumentException("Graph is empty!");
			if(setup.moves.isEmpty()) throw new IllegalArgumentException("Moves is empty!");

			// checks to make sure detectives don't get either secret or double tickets
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
			// adds mrX and detectives to the set
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
			// iterate to get each detective's location
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
			// implements the Ticketboard as an anonymous subclass
			TicketBoard tb = new TicketBoard() {
				@Override
				public int getCount(@Nonnull ScotlandYard.Ticket ticket) {
					// consider Mrx and Detectives separately.
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
			return log;
		}

		@Nonnull
		@Override
		public ImmutableSet<Piece> getWinner() {
			// return winner is not empty
			// otherwise find winner
			if (winner.isEmpty()) {
				ImmutableSet.Builder<Piece> winnerSet = ImmutableSet.builder();
				boolean detectivesWin = false;
				boolean mrXWin = false;
				// check each detective, whether overlaps with mrX
				for (Player d : detectives) {
					if (d.location() == mrX.location()) {
						detectivesWin = true;
						break;
					}
				}
				// MrX win if rounds all finishes and detective still not catch mrX
				if (log.size() == setup.moves.size() && remaining.contains(mrX.piece())) {
					mrXWin = true;
				}
				// if winner still not determined, check lower priority winning conditions
				if (!detectivesWin && !mrXWin) {
					// store old remaining first
					ImmutableSet.Builder<Piece> oldRemaining = ImmutableSet.builder();
					for (Piece p : remaining) {
						oldRemaining.add(p);
					}
					// check whether mrX able to move first
					ImmutableSet.Builder<Piece> mrXRemaining = ImmutableSet.builder();
					mrXRemaining.add(mrX.piece());
					remaining = mrXRemaining.build();
					if (getAvailableMoves().isEmpty()) {
						detectivesWin = true;
					}
					// still no winner, then check whether detectives are able to move
					if (!detectivesWin) {
						ImmutableSet.Builder<Piece> newRemaining = ImmutableSet.builder();
						for (Player d : detectives) {
							newRemaining.add(d.piece());
						}
						remaining = newRemaining.build();
						if (getAvailableMoves().isEmpty()) {
							mrXWin = true;
						}
					}
					remaining = oldRemaining.build();
				}
				//producing the winnerSet if winner appears
				if (detectivesWin) {
					for (Player d : detectives) {
						winnerSet.add(d.piece());
					}
				} else if (mrXWin) {
					winnerSet.add(mrX.piece());
				}
				winner = winnerSet.build();
			}
			return winner;
		}

		// this is the function to help find the available move
		public ImmutableSet<Move> getAvailablePieceMoves(Piece p){
			ImmutableSet.Builder<Move> moveSet = ImmutableSet.builder();
			// consider pieces separately
			// since mrX has double moves but detectives don't
			if (p.isMrX()) {
				moveSet.addAll(makeSingleMoves(setup, detectives, mrX, mrX.location()));
				if (mrX.tickets().get(ScotlandYard.Ticket.DOUBLE) > 0 && log.size() + 1 != setup.moves.size()) {
					moveSet.addAll(makeDoubleMoves(setup, detectives, mrX, mrX.location()));
				}
			} else {
				for (Player detective : detectives) {
					if (detective.piece() == p) {
						moveSet.addAll(makeSingleMoves(setup, detectives, detective, detective.location()));
					}
				}
			}
			return moveSet.build();
		}

		@Nonnull
		@Override
		public ImmutableSet<Move> getAvailableMoves() {
			ImmutableSet.Builder<Move> moveSet = ImmutableSet.builder();
			// iterate through all remaining to get all remaining's available moves
			for (Piece piece : remaining) {
				moveSet.addAll(getAvailablePieceMoves(piece));
			}
			moves = moveSet.build();
			// return empty if winner exist.
			if (winner != null && !winner.isEmpty()) return ImmutableSet.of();
			return moves;
		}

		@Override
		public GameState advance(Move move) {
			// exception check
			if(!getAvailableMoves().contains(move)) throw new IllegalArgumentException("Illegal move: "+move);
			// use visitor pattern to separate single move and double move
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

			// update log entry if is moved by mrX
			if (move.commencedBy().isMrX()) updateLogEntry(move, destinations);

			// update detectives' and mrX's tickets when detective move
			if (move.commencedBy().isDetective()) {
				ImmutableList.Builder<Player> newDetectives = ImmutableList.builder();
				for (Player detective : detectives) {
					if (move.commencedBy() == detective.piece()) {
						newDetectives.add(detective.at(destinations.get(0)).use(move.tickets()));
						mrX = mrX.give(move.tickets());
					} else newDetectives.add(detective);
				}
				detectives = newDetectives.build();
			}

			// create new remaining,
			// remaining is either all detectives or just MrX
			ImmutableSet.Builder<Piece> NewRemainingBuilder = ImmutableSet.builder();
			if (remaining.contains(mrX.piece())) {
				for(Player d : detectives){
					if (!getAvailablePieceMoves(d.piece()).isEmpty()) NewRemainingBuilder.add(d.piece());
				}
			}else{
				for (Piece p : remaining) {
					if (!getAvailablePieceMoves(p).isEmpty()){
						// if all detectives has moved, then swap to mrX
						if (remaining.size() > 1){
							if (p != move.commencedBy()) NewRemainingBuilder.add(p);
						} else NewRemainingBuilder.add(mrX.piece());
					}

				}
			}
			remaining = NewRemainingBuilder.build();
			GameState AdvancedGameState = new MyGameState(setup, remaining, log, mrX, detectives);
			return AdvancedGameState;
		}

		private void updateLogEntry(Move move, ImmutableList<Integer> destinations) {
			ImmutableList.Builder<LogEntry> newLog = ImmutableList.builder();
			newLog.addAll(log);
			int count = 0;
			for (ScotlandYard.Ticket t : move.tickets()) {
				// only consider first 2 tickets, since third ticket can only be double
				if (count < 2) {
					if (setup.moves.get(log.size() + count)) {
						newLog.add(LogEntry.reveal(t,destinations.get(count)));
					} else {
						newLog.add(LogEntry.hidden(t));
					}
				}
				count++;
			}
			mrX = mrX.at(destinations.get(destinations.size() - 1)).use(move.tickets());
			log = newLog.build();
		}
	}

	private static Set<Move.SingleMove> makeSingleMoves(GameSetup setup, List<Player> detectives, Player player, int source){

		// create an empty collection of some sort, say, HashSet, to store all the SingleMove we generate
		HashSet<Move.SingleMove> s = new HashSet<>();

		for(int destination : setup.graph.adjacentNodes(source)) {
			//  find out if destination is occupied by a detective
			//  if the location is occupied, don't add to the collection of moves to return
			boolean detectiveOccupied = false;
			for (Player d : detectives) {
				if (d.location() == destination) {
					detectiveOccupied = true;
				}
			}

			for (ScotlandYard.Transport t : setup.graph.edgeValueOrDefault(source, destination, ImmutableSet.of())) {
				//  find out if the player has the required tickets
				//  if it does, construct a SingleMove and add it the collection of moves to return
				if (player.tickets().get(t.requiredTicket()) > 0 && !detectiveOccupied) {
					s.add(new Move.SingleMove(player.piece(),source, t.requiredTicket(), destination));
				}
			}

			//  consider the rules of secret moves here
			//  add moves to the destination via a secret ticket if there are any left with the player
			if (player.tickets().get(ScotlandYard.Ticket.SECRET) > 0 && !detectiveOccupied) {
				s.add(new Move.SingleMove(player.piece(),source, ScotlandYard.Ticket.SECRET, destination));
			}
		}
		// return the collection of moves
		return s;
	}

	//  Below is the implementation of the double move.
	private static Set<Move.DoubleMove> makeDoubleMoves(GameSetup setup, List<Player> detectives, Player player, int source) {
		HashSet<Move.DoubleMove> s = new HashSet<>();
		Set<Move.SingleMove> firstMoves = makeSingleMoves(setup, detectives, player, source);

		for (Move.SingleMove firstMove : firstMoves) {
			Set<Move.SingleMove> secondMoves = makeSingleMoves(setup, detectives, player, firstMove.destination);
			for (Move.SingleMove secondMove : secondMoves) {
				if (firstMove.ticket != secondMove.ticket || player.hasAtLeast(secondMove.ticket, 2)) {
					s.add(new Move.DoubleMove(
							player.piece(),
							source,
							firstMove.ticket,
							firstMove.destination,
							secondMove.ticket,
							secondMove.destination));
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