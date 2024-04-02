package uk.ac.bris.cs.scotlandyard.ui.ai;

import uk.ac.bris.cs.scotlandyard.model.Board;
import uk.ac.bris.cs.scotlandyard.model.Move;
import uk.ac.bris.cs.scotlandyard.model.Piece;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard;

public class CostCalculator {
    public static float maxCalculateCost(Board.GameState state, Move move) {
        var tickets = move.tickets();
        float cost = 0;
        Board.TicketBoard tb = null;
        for(Piece p : state.getPlayers()){
            if (p.isMrX()){ tb = state.getPlayerTickets(p).orElseThrow(); }
        }
        if (tb == null) {throw new RuntimeException("tb does not exist"); }
        int bus = tb.getCount(ScotlandYard.Ticket.BUS);
        int taxi = tb.getCount(ScotlandYard.Ticket.TAXI);
        int secret = tb.getCount(ScotlandYard.Ticket.SECRET);
        int underground = tb.getCount(ScotlandYard.Ticket.UNDERGROUND);
        for (ScotlandYard.Ticket t : tickets){
            switch (t){
                case BUS -> bus--;
                case DOUBLE -> cost += 10;
                case TAXI -> taxi--;
                case SECRET -> cost += 5;
                case UNDERGROUND -> underground--;
            }
            // To make number of ticket distribute more uniformly
            cost += variance(taxi, bus, secret, underground);
        }
        return cost;
    }

    public static float variance(int x1, int x2, int x3, int x4){
        float mean = (float) (x1 + x2 + x3 + x4) / 4;
        return ((x1 - mean)*(x1 - mean) + (x2 - mean)*(x2 - mean) + (x3 - mean)*(x3 - mean) + (x4 - mean)*(x4 - mean))/4;
    }
}
