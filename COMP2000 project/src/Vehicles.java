import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Collections;

public class Vehicles {
    int capacity; 
    String name;
    Stops.Routes route;
    ArrayList<Passenger> onBoard = new ArrayList<>();

    int stationIndex = 0;  // index of the current stop in route.stations
    int width = 30;
    int height = 16;

    int delayStepsRemaining = 0;  // steps left before the train can move again

    public Vehicles(){
        
    }

    // capacity is stored but not checked anywhere yet
    public Vehicles(int capacity, String name, Stops.Routes route){
        this.capacity = capacity;
        this.name = name;
        this.route = route;
    }

    // the stop the train is at now
    public Stops getCurStop(){
        return route.stations.get(stationIndex);
    }

    // the stop the train moves to next, it turns back at the end of the line
    public Stops getNextStop(){
        int lastIndex = route.stations.size() - 1;
        if(stationIndex < lastIndex){
            return route.stations.get(stationIndex + 1);
        }
        if(lastIndex > 0){
            return route.stations.get(lastIndex - 1);  // the line is reversed at the end
        }
        return null;
    }

    public ArrayList<Passenger> getPassengers(){
        return onBoard;
    }

    public String getName(){
        return name;
    }

    // holds the train at its stop for this many steps
    public void delay(int steps){
        delayStepsRemaining = delayStepsRemaining + steps;
    }

    public boolean isDelayed(){
        return delayStepsRemaining > 0;
    }

    // set by fog or a large fire, a stopped train does not move at all
    boolean stopped = false;

    public void setStopped(boolean stopped){
        this.stopped = stopped;
    }

    public boolean isStopped(){
        return stopped;
    }

    // moves one stop, turns around at the end of the line, waits while delayed
    public void moveVehicle(){
        if(stopped){
            return;  // a stopped train does not count its delay down
        }
        if(delayStepsRemaining > 0){
            delayStepsRemaining = delayStepsRemaining - 1;
            return;
        }
        int lastIndex = route.stations.size() - 1;
        if(stationIndex < lastIndex){
            stationIndex = stationIndex + 1;
        } else {
            Collections.reverse(route.stations);
            stationIndex = 0;
        }
    }

    // draws the train as a coloured box with its name, at its current stop
    public void displayVehicle(Graphics g, int red, int green, int blue){
        Stops curStop = getCurStop();
        int x = curStop.x-width/2;
        int y = curStop.y-height/2;

        if(isDelayed()){
            HazardStripe.draw(g, x, y, width, height, 4);
        }

        Graphics2D g2d=(Graphics2D) g;
        g2d.setStroke(new BasicStroke(2));

        g.setColor(new Color(red, green, blue));  // train colour
        g.fillRect(x, y, width, height);
        if(isDelayed()){
            g.setColor(Color.red);
        } else {
            g.setColor(Color.black);
        }
        g.drawRect(x, y, width, height);

        g.setFont(new Font("SansSerif", Font.BOLD, 12));
        FontMetrics fm = g.getFontMetrics();
        int textX = x + (width - fm.stringWidth(name)) / 2;
        int textY = y + (height + fm.getAscent()) / 2 - 2;
        g.setColor(Color.white);
        g.drawString(name, textX, textY);
        if(stopped){
            HazardStripe.StopSign.draw(g, x + width / 2, y - 18, 32); // above the train
        }
        // flame on every burning stop of this line
        for(Stops stop : route.stations){
            if(stop.onFire){
                HazardStripe.Flame.draw(g, stop.x - 24, stop.y - 6, 30);
            }
        }
        // orange dot when someone is on board
        if(onBoard.size() > 0){
            int passengerSize = 6;
            int passengerX = x + width - passengerSize - 2;
            int passengerY = y + 2;
            g.setColor(Color.orange);
            g.fillOval(passengerX, passengerY, passengerSize, passengerSize);
        }
    }



}