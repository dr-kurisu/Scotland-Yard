package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableSet;
import uk.ac.bris.cs.scotlandyard.model.Board.GameState;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;

import java.util.HashSet;
import java.util.Optional;

/**
 * cw-model
 * Stage 1: Complete this class
 */
public final class MyGameStateFactory implements Factory<GameState> {

	@Nonnull @Override public GameState build(
			GameSetup setup,
			Player mrX,
			ImmutableList<Player> detectives) {
		// TODO
		GameState MyGameState = new GameState(){
			@Nonnull @Override
			public GameSetup getSetup(){ return setup; }
			@Override
			public ImmutableSet<Piece> getPlayers(){
				ImmutableSet.Builder<Piece> builder = ImmutableSet.builder();
				builder.add(mrX.piece());
				for (Player detective : detectives){ builder.add(detective.piece()); }
				return builder.build();
			}
			@Nonnull @Override
			public Optional<Integer> getDetectiveLocation(Piece.Detective detective){
				throw new RuntimeException(); }
			@Nonnull @Override
			public Optional<TicketBoard> getPlayerTickets(Piece piece){throw new RuntimeException(); }
			@Nonnull @Override
			public ImmutableList<LogEntry> getMrXTravelLog(){ throw new RuntimeException(); }
			@Nonnull  @Override
			public ImmutableSet<Piece> getWinner(){ throw new RuntimeException(); }
			@Nonnull @Override
			public ImmutableSet<Move> getAvailableMoves(){ throw new RuntimeException(); }
			@Override
			public GameState advance(Move move){ throw new RuntimeException(); }
		};
		return MyGameState;
	}


}

