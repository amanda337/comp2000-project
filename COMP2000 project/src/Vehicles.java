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
    Routes route;
    ArrayList<Passenger> onBoard = new ArrayList<>();

    int stationIndex = 0; // position along route.stations
    int width = 30;
    int height = 16;

    int delayStepsRemaining = 0; // set by a scheduled AccidentEvent, counted down in moveVehicle()

    public Vehicles(){
        
    }

    public Vehicles(int capacity, String name, Routes route){
        this.capacity = capacity;
        this.name = name;
        this.route = route;
    }

    public Stops getCurStop(){
        return route.stations.get(stationIndex);
    }

    // What moveVehicle() will step to next, including the bounce at the end of the line.
    public Stops getNextStop(){
        int lastIndex = route.stations.size() - 1;
        if(stationIndex < lastIndex){
            return route.stations.get(stationIndex + 1);
        }
        if(lastIndex > 0){
            return route.stations.get(lastIndex - 1); // where index 1 will be after the reverse
        }
        return null;
    }

    public ArrayList<Passenger> getPassengers(){
        return onBoard;
    }

    public String getName(){
        return name;
    }

    // Called by an AccidentEvent: stops this vehicle at its current stop for
    // the given number of steps before it moves again.
    public void delay(int steps){
        delayStepsRemaining = delayStepsRemaining + steps;
    }

    public boolean isDelayed(){
        return delayStepsRemaining > 0;
    }

    // Set by fog or a large fire (see Time.applyEvents): a stopped train does
    // not move at all and shows a stop sign, unlike a delayed one.
    boolean stopped = false;

    public void setStopped(boolean stopped){
        this.stopped = stopped;
    }

    public boolean isStopped(){
        return stopped;
    }

    // Steps forward one stop; reverses direction once it reaches the end of the line.
    // Stays put while a delay set by delay(...) is still counting down.
    public void moveVehicle(){
        if(stopped){
            return; // not even a delay counts down while completely stopped
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

    // Coloured rectangle at the current stop, labelled with the vehicle's name,
    // with a red outline and hazard stripe while delayed.
    public void displayVehicle(Graphics g, int red, int green, int blue){
        Stops curStop = getCurStop();
        int x = curStop.x-width/2;
        int y = curStop.y-height/2;

        if(isDelayed()){
            HazardStripe.draw(g, x, y, width, height, 4);
        }

        Graphics2D g2d=(Graphics2D) g;
        g2d.setStroke(new BasicStroke(2));

        g.setColor(new Color(red, green, blue)); // solid red
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
            StopSign.draw(g, x + width / 2, y - 18, 32); // above the train
        }
        // Passenger indicator: a small dot in the corner when someone is aboard.
        if(onBoard.size() > 0){
            int passengerSize = 6;
            int passengerX = x + width - passengerSize - 2;
            int passengerY = y + 2;
            g.setColor(Color.orange);
            g.fillOval(passengerX, passengerY, passengerSize, passengerSize);
        }
    }



}