package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableSet;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;

import uk.ac.bris.cs.scotlandyard.model.Board.GameState;

import java.util.ArrayList;

/**
 * cw-model
 * Stage 2: Complete this class
 */
public final class MyModelFactory implements Factory<Model> {
		@Nonnull @Override public Model build(GameSetup setup,
	                                      Player mrX,
	                                      ImmutableList<Player> detectives) {

		Model MyModel = new Model(){
			private MyGameStateFactory GameStateFactory = new MyGameStateFactory();
			private GameState state = GameStateFactory.build(setup, mrX, detectives);

			private ArrayList<Observer> observers = new ArrayList<>();
			/**
			 * @return the current game board
			 */
			@Nonnull public Board getCurrentBoard(){
				return (Board)state;
			}
			/**
			 * Registers an observer to the model. It is an error to register the same observer more than
			 * once.
			 *
			 * @param observer the observer to register
			 */
			public void registerObserver(@Nonnull Observer observer){
				if (observer == null) throw new NullPointerException("Invalid Null Observer");
				if (observers.contains(observer)) throw new IllegalArgumentException("Invalid Register");
				observers.add(observer);
			}
			/**
			 * Unregisters an observer to the model. It is an error to unregister an observer not
			 * previously registered with {@link #registerObserver(Observer)}.
			 *
			 * @param observer the observer to register
			 */
			public void unregisterObserver(@Nonnull Observer observer){
				if (observer == null) throw new NullPointerException("Invalid Null Observer");
				if (!observers.contains(observer)) throw new IllegalArgumentException("Observer does not exist");
				observers.remove(observer);
			}
			/**
			 * @return all currently registered observers of the model
			 */
			@Nonnull
			public ImmutableSet<Observer> getObservers(){
				ImmutableSet.Builder<Observer> ObserverBuilder = ImmutableSet.builder();
				for(Observer observer : observers){
					ObserverBuilder.add(observer);
				}
				return ObserverBuilder.build();
			}
			/**
			 * @param move delegates the move to the underlying
			 * {@link uk.ac.bris.cs.scotlandyard.model.Board.GameState}
			 */
			public void chooseMove(@Nonnull Move move){
				// if current state can still move, then advance move
				if (!state.getAvailableMoves().isEmpty()) state.advance(move);
				// moves are only made when no winner appears
				// otherwise observe Game_Over
				if (state.getWinner().isEmpty()){
					for (Observer observer : observers) {
						observer.onModelChanged(getCurrentBoard(), Observer.Event.MOVE_MADE);
					}
				}else{
					for (Observer observer : observers) {
						observer.onModelChanged(getCurrentBoard(), Observer.Event.GAME_OVER);
					}
				}
			}
		};

		return MyModel;
	}

}
