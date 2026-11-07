import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Random;

public class Panel extends Frame{

    int width = 1280; //screen width
    int height = 720;  //screen height

    int majorStation = 40; //Big station 
    int normalStation= 20;//Normal station

    // Train running the red line, driven by a clock started with space.
    Train train1 = Train.t1();
    Train train2 = Train.t2();
    Train train3 = Train.t3();
    Train train4 = Train.t4();
    Time time;
    SidePanel sidePanel = new SidePanel();
    
    Passenger pass1 = Passenger.pass1();
    ArrayList<Passenger> passengers = new ArrayList<>();

    // A handful of random accidents scheduled across the service, so trains
    // do not all run on a perfectly fixed timetable.
    EventQueue<SimulationEvent> accidents = new EventQueue<>();

    Image dbImage;
    Graphics dbGraphics;

    // Sky and tree-line scenery drawn behind the routes. Null (and skipped)
    // if the files cannot be read, so a missing asset never crashes the map.
    Image skyImage;
    Image treeImage;

    Insets insets = new Insets(0, 0, 0, 0);

    // Dragging the map pans it; the side panels stay fixed on screen.
    int panOffsetX = 0;
    int panOffsetY = 0;
    int dragStartX;
    int dragStartY;
    boolean dragging = false;
    boolean draggingScrollbar = false; // true while the left-bar scrollbar thumb is held


    public Panel() {
        setTitle("Transit Sim");
        setSize(width, height);
        // Loaded before the window is shown, so the very first paint already
        // has the pictures ready instead of racing them and drawing blank.
        loadBackgroundImages();
        setVisible(true);

        // Window closing adapter to safely close the application
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent we) {
                System.exit(0);
            }

            // getInsets() right after setVisible() is unreliable — the native
            // peer isn't fully realized yet and often reports 0. windowOpened
            // fires once the window is actually up, so the title bar height
            // read here is the real one, not a stale/zero guess.
            public void windowOpened(WindowEvent we) {
                insets = getInsets();
                setSize(width + insets.left + insets.right, height + insets.top + insets.bottom);
                repaint();
            }
        });

        setFocusable(true);
        requestFocusInWindow();

        passengers.add(pass1);
        scheduleRandomAccidents();

        // Every 5th tick (1 real second) moves the train and checks boarding.

        time = new Time(new ActionListener() {
            public void actionPerformed(ActionEvent e ) {
                // The clock keeps running around the clock; SimulationTimeException
                // only marks the moment service opens or closes, so it is just
                // reported here rather than stopping anything.
                try {
                    time.advance();

                    if (time.isServiceHours()) {
                    Passenger.spawnPassengers();   // once per timer tick (a "frame")
                    }

                    if (time.isOnSecond() && time.isServiceHours()) {
                    long step = time.ticks / 5;
                    Vehicles[] allTrains = {train1, train2, train3, train4};
                    while (accidents.hasDueEvent(step)) {
                    accidents.nextEvent().execute(allTrains);
                    }

                    train1.moveVehicle();
                    train2.moveVehicle();
                    train3.moveVehicle();
                    train4.moveVehicle();

    // Every waiting passenger checks every train at its current stop.
    Train[] trains = {train1, train2, train3, train4};
    for (Train t : trains) {
        for (Passenger p : passengers) {
            p.checkBoarding(t.getCurStop(), t.onBoard);
        }
    }
}
                } catch (SimulationTimeException ste) {
                    //System.out.println(ste.getMessage());
                }
                repaint();
            }
        });

        // Space toggles the clock: first press starts it, next press pauses, next resumes.
        addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                    time.toggle();
                }
            }
        });

        // One handler for click, drag and wheel: MouseAdapter already covers
        // MouseListener + MouseMotionListener + MouseWheelListener, so one object is enough.
        // Clicking the button toggles the clock; dragging the map (not the side panels) pans it;
        // the wheel and the left-bar scrollbar scroll the train list.
        //
        // mx()/my() subtract the window insets: the whole scene is drawn at
        // (insets.left, insets.top) in update(), but mouse events arrive in
        // frame coordinates, so without this the hit boxes sit a title-bar
        // height too high.
        MouseAdapter mouseHandler = new MouseAdapter() {
            private int mx(MouseEvent e) {
                return e.getX() - insets.left;
            }

            private int my(MouseEvent e) {
                return e.getY() - insets.top;
            }

            public void mousePressed(MouseEvent e) {
                if (sidePanel.isOverScrollbar(mx(e), my(e))) {
                    draggingScrollbar = true;
                    sidePanel.setThumbDragging(true);
                    sidePanel.dragScrollTo(my(e));
                    repaint();
                } else if (!sidePanel.isOverPanel(mx(e), my(e), width, height)) {
                    dragStartX = mx(e) - panOffsetX;
                    dragStartY = my(e) - panOffsetY;
                    dragging = true;
                }
            }

            public void mouseReleased(MouseEvent e) {
                dragging = false;
                draggingScrollbar = false;
                sidePanel.setThumbDragging(false);
                repaint();
            }

            public void mouseClicked(MouseEvent e) {
                if (sidePanel.isPauseClicked(mx(e), my(e), width)) {
                    time.toggle();
                    repaint();
                } else if (sidePanel.isHomeClicked(mx(e), my(e), width)) {
                    goHome();
                } else if (sidePanel.handleTabClick(mx(e), my(e), width)) {
                    repaint();
                }
            }

            public void mouseMoved(MouseEvent e) {
                // Light up the scrollbar / buttons when the mouse is near them.
                boolean changed = sidePanel.setPointer(mx(e), my(e));
                changed |= sidePanel.setButtonHover(mx(e), my(e), width);
                if (changed) {
                    repaint();
                }
            }

            public void mouseExited(MouseEvent e) {
                boolean changed = sidePanel.setPointer(-100, -100);
                changed |= sidePanel.setButtonHover(-100, -100, width);
                if (changed) {
                    repaint();
                }
            }

            public void mouseDragged(MouseEvent e) {
                if (draggingScrollbar) {
                    sidePanel.dragScrollTo(my(e));
                    repaint();
                } else if (dragging) {
                    panOffsetX = mx(e) - dragStartX;
                    panOffsetY = my(e) - dragStartY;
                    repaint();
                }
            }

            public void mouseWheelMoved(MouseWheelEvent e) {
                // Scroll the train list when the pointer is over the left bar.
                if (mx(e) < sidePanel.getLeftWidth()) {
                    sidePanel.scrollBy(e.getWheelRotation() * 40);
                    repaint();
                }
            }
        };
        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);
        addMouseWheelListener(mouseHandler);
    }

    // Picks 2-3 random accidents across the run: a random train, a random
    // step to trigger on, and a random number of steps it gets stuck for.
    // The tick loop executes each one from accidents once its step is due.
    private void scheduleRandomAccidents() {
        Random random = new Random();
        Train[] allTrains = {train1, train2, train3, train4};
        int accidentCount = 3 + random.nextInt(3);

        for (int i = 0; i < accidentCount; i++) {
            Train chosen = allTrains[random.nextInt(allTrains.length)];
            long step = 10 + random.nextInt(200); // spread across the 06:00 AM - midnight run
            int delaySteps = 3 + random.nextInt(6);
            accidents.add(new AccidentEvent(step, chosen.getName(), delaySteps));
        }
    }

    // Reads the sky and tree scenery once at startup. If either file is
    // missing or unreadable the map just falls back to a plain background.
    private void loadBackgroundImages() {
        skyImage = Assets.load("1.png");
        treeImage = Assets.load("2.png");
    }

    // Redraws the ring of every burning station in red (same 5px stroke as the
    // black ring). Stops know where they are, so no station positions repeat here.
    private void drawFireOutlines(Graphics g) {
        Train[] allTrains = {train1, train2, train3, train4};
        java.util.Arrays.stream(allTrains)
                .flatMap(train -> train.route.stations.stream())
                .filter(stop -> stop.onFire)
                .distinct()
                .forEach(stop -> {
                    int size = normalStation;
                    if (stop.capacity >= 100) {
                        size = majorStation; // Central, Town Hall, Troll-upon-Bridge
                    }
                    g.setColor(Color.red);
                    g.drawOval(stop.x - size / 2, stop.y - size / 2, size, size);
                });
    }

    // Pauses the clock, closes the simulation window and returns to the home screen.
    private void goHome() {
        if (time.isRunning()) {
            time.toggle();
        }
        dispose();
        new Home();
    }

    // Draws to an off-screen image first, then blits it in one go, so the
    // clock/train redraw doesn't flash the whole panel.
    public void update(Graphics g) {
        if (dbImage == null) {
            dbImage = createImage(width, height);
            dbGraphics = dbImage.getGraphics();
        }
        dbGraphics.setColor(getBackground());
        dbGraphics.fillRect(0, 0, width, height);
        paint(dbGraphics);
        insets = getInsets(); // read fresh each frame; a one-time read can be stale
        g.drawImage(dbImage, insets.left, insets.top, this);
    }

    //@Override
    public void paint(Graphics screenGraphics) {
        // Reset font each frame so the clock's bigger font doesn't carry over.
        screenGraphics.setFont(new Font("SansSerif", Font.PLAIN, 12));

        // Scenery behind everything else. Fixed on screen (not panned), like
        // a sky, so dragging the map never leaves a gap behind it.
        if (skyImage != null) {
            screenGraphics.drawImage(skyImage, 0, 0, width, height, this);
        }
        if (treeImage != null) {
            screenGraphics.drawImage(treeImage, 0, 0, width, height, this);
        }

        // Map layer: shifted by the pan offset, so dragging moves the network
        // and stations without moving the side panels drawn later.
        Graphics g = screenGraphics.create();
        g.translate(panOffsetX, panOffsetY);

        // Set Stroke Thickness
        Graphics2D g2d=(Graphics2D) g;
        g2d.setStroke(new BasicStroke(3));
        //Draw Train Lines 

        //train line 1 (blue)
        g.setColor(new Color(100, 150, 200));
        g.drawLine(width/2 - 300, height/2 + 300, width/2 - 500, height/2 + 300);
        g.drawLine(width/2 - 300, height/2 + 300, width/2 - 400, height/2 + 300);
        g.drawLine(width/2 - 200, height/2 + 200, width/2 - 300, height/2 + 300);
        g.drawLine(width/2 - 200, height/2, width/2 - 200, height/2 + 200);
        g.drawLine(width/2 - 200, height/2, width/2 - 200, height/2 + 100);
        g.drawLine(width/2, height/2, width/2 - 200, height/2);
        g.drawLine(width/2, height/2, width/2 - 100, height/2);
        g.drawLine(width/2, height/2, width/2 + 100, height/2);
        g.drawLine(width/2, height/2, width/2 + 200, height/2);
        g.drawLine(width/2, height/2, width/2 + 300, height/2);
        g.drawLine(width/2 + 300, height/2, width/2 + 400, height/2 - 100);
        g.drawLine(width/2 + 400, height/2 - 100, width/2 + 400, height/2 - 200);
        g.drawLine(width/2 + 400, height/2 - 100, width/2 + 400, height/2 - 300);

        //train line 2 (red)
        g.setColor(Color.red);
        g.drawLine(width/2 + 2, height/2, width/2 + 2, height/2 - 300);
        g.drawLine(width/2 + 2, height/2, width/2 + 2, height/2 - 200);
        g.drawLine(width/2 + 2, height/2, width/2 + 2, height/2 - 100);
        g.drawLine(width/2, height/2 + 5, width/2 + 100, height/2 + 5);
        g.drawLine(width/2, height/2 + 5, width/2 + 200, height/2 + 5);
        g.drawLine(width/2, height/2 + 5, width/2 + 300, height/2 + 5);
        g.drawLine(width/2 + 300, height/2, width/2 + 400, height/2 + 100);
        g.drawLine(width/2 + 400, height/2 + 100, width/2 + 400, height/2 + 200);

        //train line 3 (green)
        g.setColor(Color.green);
        g.drawLine(width/2 - 300, height/2 - 100, width/2 - 400, height/2 - 200);
        g.drawLine(width/2 - 200, height/2, width/2 - 300, height/2 - 100);
        g.drawLine(width/2, height/2 - 5, width/2 - 200, height/2 - 5);
        g.drawLine(width/2, height/2 - 5, width/2 - 100, height/2 - 5);

        //train line 4 (purple)
        g.setColor(new Color(191, 0, 255));
        g.drawLine(width/2 - 2, height/2, width/2 - 2, height/2 - 300);
        g.drawLine(width/2 - 2, height/2, width/2 - 2, height/2 - 200);
        g.drawLine(width/2 - 2, height/2, width/2 - 2, height/2 - 100);
        g.drawLine(width/2, height/2 + 5, width/2 - 100, height/2 + 5);
        g.drawLine(width/2, height/2 + 5, width/2 - 200, height/2 + 5);
        g.drawLine(width/2 - 195, height/2 + 5, width/2 - 195, height/2 + 100);
        g.drawLine(width/2 - 195, height/2 + 100, width/2 - 195, height/2 + 200);


        
        g2d.setStroke(new BasicStroke(5)); //Stroke Thickness 
        g.setColor(Color.black);

        //Draw Stations 
        //Central Station
        g.drawString("Central", width/2 -majorStation/2, height/2 -majorStation/2 + 60);
        g.setColor(Color.gray);
        g.fillOval(width/2 -majorStation/2, height/2 -majorStation/2, majorStation, majorStation);  
        g.setColor(Color.black);
        g.drawOval(width/2 -majorStation/2, height/2 -majorStation/2, majorStation, majorStation);
        //line 1
        //station 1
        //provide a name for the station east1
        g.drawString("Jubilee", width/2 -normalStation/2 + 100 - 10, height/2 -normalStation/2 - 10);
        g.setColor(Color.gray);
        g.fillOval(width/2 -normalStation/2 + 100, height/2 -normalStation/2, normalStation, normalStation);  
        g.setColor(Color.black);
        g.drawOval(width/2 -normalStation/2 + 100, height/2 -normalStation/2, normalStation, normalStation);
        //station 2
        g.drawString("Bakerville", width/2 -normalStation/2 + 200 - 15, height/2 -normalStation/2 - 10);
        g.setColor(Color.gray);
        g.fillOval(width/2 -normalStation/2 + 200, height/2 -normalStation/2, normalStation, normalStation);  
        g.setColor(Color.black);
        g.drawOval(width/2 -normalStation/2 + 200, height/2 -normalStation/2, normalStation, normalStation);
        //station 3
        g.drawString("Percy Port", width/2 -normalStation/2 + 335, height/2 -normalStation/2 + 15);
        g.setColor(Color.gray);
        g.fillOval(width/2 -normalStation/2 + 300, height/2 -normalStation/2, normalStation, normalStation);  
        g.setColor(Color.black);
        g.drawOval(width/2 -normalStation/2 + 300, height/2 -normalStation/2, normalStation, normalStation);
        //station 4        
        g.drawString("Reeds", width/2 -normalStation/2 + 435, height/2 -normalStation/2 - 100 + 15);
        g.setColor(Color.gray);
        g.fillOval(width/2 -normalStation/2 + 400, height/2 -normalStation/2 - 100, normalStation, normalStation);  
        g.setColor(Color.black);
        g.drawOval(width/2 -normalStation/2 + 400, height/2 -normalStation/2 - 100, normalStation, normalStation);
        //station 5
        g.drawString("Daisy Hill", width/2 -normalStation/2 + 435, height/2 -normalStation/2 - 200 + 15);
        g.setColor(Color.gray);
        g.fillOval(width/2 -normalStation/2 + 400, height/2 -normalStation/2 - 200, normalStation, normalStation);  
        g.setColor(Color.black);
        g.drawOval(width/2 -normalStation/2 + 400, height/2 -normalStation/2 - 200, normalStation, normalStation);   
        //station 6
        g.drawString("Sunset Point", width/2 -normalStation/2 + 435, height/2 -normalStation/2 - 300 + 15);
        g.setColor(Color.gray);
        g.fillOval(width/2 -normalStation/2 + 400, height/2 -normalStation/2 - 300, normalStation, normalStation);  
        g.setColor(Color.black);
        g.drawOval(width/2 -normalStation/2 + 400, height/2 -normalStation/2 - 300, normalStation, normalStation);             
        
       
        //line 2  
        //station 2.1
        g.drawString("Merrybrook", width/2 -normalStation/2 + 435, height/2 -normalStation/2 + 100 + 15);
        g.setColor(Color.gray);
        g.fillOval(width/2 -normalStation/2 + 400, height/2 -normalStation/2 + 100, normalStation, normalStation);  
        g.setColor(Color.black);
        g.drawOval(width/2 -normalStation/2 + 400, height/2 -normalStation/2 + 100, normalStation, normalStation);
        //station 2.2
        g.drawString("Brookchester", width/2 -normalStation/2 + 435, height/2 -normalStation/2 + 200 + 15);
        g.setColor(Color.gray);
        g.fillOval(width/2 -normalStation/2 + 400, height/2 -normalStation/2 + 200, normalStation, normalStation);  
        g.setColor(Color.black);
        g.drawOval(width/2 -normalStation/2 + 400, height/2 -normalStation/2 + 200, normalStation, normalStation); 
        //Station 2.3 
        g.drawString("Sherie Grove", width/2 -normalStation/2 - 125, height/2 -normalStation/2 - 10);
        g.setColor(Color.gray);
        g.fillOval(width/2 -normalStation/2 - 100, height/2 -normalStation/2, normalStation, normalStation);  
        g.setColor(Color.black);
        g.drawOval(width/2 -normalStation/2 - 100, height/2 -normalStation/2, normalStation, normalStation); 
        //Station 2.4
        g.drawString("Prudence", width/2 -normalStation/2 - 270, height/2 -normalStation/2 + 15);
        g.setColor(Color.gray);
        g.fillOval(width/2 -normalStation/2 - 200, height/2 -normalStation/2, normalStation, normalStation);  
        g.setColor(Color.black);
        g.drawOval(width/2 -normalStation/2 - 200, height/2 -normalStation/2, normalStation, normalStation); 
        //Station 2.5
        g.drawString("Andie Park", width/2 -normalStation/2 - 270, height/2 -normalStation/2 - 100 + 15);
        g.setColor(Color.gray);
        g.fillOval(width/2 -normalStation/2 -300, height/2 -normalStation/2 - 100, normalStation, normalStation);  
        g.setColor(Color.black);
        g.drawOval(width/2 -normalStation/2 -300, height/2 -normalStation/2 - 100, normalStation, normalStation); 
        //Station 2.6
        g.drawString("Mount Presley", width/2 -normalStation/2 - 370, height/2 -normalStation/2 - 200 + 15);
        g.setColor(Color.gray);
        g.fillOval(width/2 -normalStation/2 -400, height/2 -normalStation/2 - 200, normalStation, normalStation);  
        g.setColor(Color.black);
        g.drawOval(width/2 -normalStation/2 -400, height/2 -normalStation/2 - 200, normalStation, normalStation); 
        
        //Station3.1
        g.drawString("Rosebury", width/2 -normalStation/2 + 30, height/2 -normalStation/2 - 85);              
        g.setColor(Color.gray);
        g.fillOval(width/2 -normalStation/2 , height/2 -normalStation/2 - 100, normalStation, normalStation);  
        g.setColor(Color.black);
        g.drawOval(width/2 -normalStation/2 , height/2 -normalStation/2 - 100, normalStation, normalStation); 
        //Station 3.2
        g.drawString("Eagleston", width/2 -normalStation/2 + 30, height/2 -normalStation/2 - 185);              
        g.setColor(Color.gray);
        g.fillOval(width/2 -normalStation/2 , height/2 -normalStation/2 - 200, normalStation, normalStation);  
        g.setColor(Color.black);
        g.drawOval(width/2 -normalStation/2 , height/2 -normalStation/2 - 200, normalStation, normalStation); 
        //Station 3.3
        g.drawString("Town Hall", width/2 -majorStation/2 + 50, height/2 -majorStation/2 - 275);
        g.setColor(Color.gray);
        g.fillOval(width/2 -majorStation/2 , height/2 -majorStation/2 - 300, majorStation, majorStation);  
        g.setColor(Color.black);
        g.drawOval(width/2 -majorStation/2 , height/2 -majorStation/2 - 300, majorStation, majorStation); 
        
        //Station 4.1
        g.drawString("Tiger Bay", width/2 -normalStation/2 - 200 + 30, height/2 -normalStation/2 + 100 + 15);
        g.setColor(Color.gray);
        g.fillOval(width/2 -normalStation/2 -200, height/2 -normalStation/2 +100, normalStation, normalStation);  
        g.setColor(Color.black);
        g.drawOval(width/2 -normalStation/2 -200, height/2 -normalStation/2 +100, normalStation, normalStation); 
        //Station 4.2
        g.drawString("Troll-upon-Bridge", width/2 -majorStation/2 - 200 + 50, height/2 -majorStation/2 + 200 + 25);
        g.setColor(Color.gray);
        g.fillOval(width/2 -majorStation/2 -200, height/2 -majorStation/2 +200, majorStation, majorStation);  
        g.setColor(Color.black);
        g.drawOval(width/2 -majorStation/2 -200, height/2 -majorStation/2 +200, majorStation, majorStation); 
        //Station 4.2
        g.drawString("Celeste", width/2 -normalStation/2 - 270, height/2 -normalStation/2 + 300 + 15);
        g.setColor(Color.gray);
        g.fillOval(width/2 -normalStation/2 -300, height/2 -normalStation/2 +300, normalStation, normalStation);  
        g.setColor(Color.black);
        g.drawOval(width/2 -normalStation/2 -300, height/2 -normalStation/2 +300, normalStation, normalStation); 
        
        
        //Station 5.1
        g.drawString("Hailstone", width/2 -normalStation/2 - 415, height/2 -normalStation/2 + 300 + 40);
        g.setColor(Color.gray);
        g.fillOval(width/2 -normalStation/2 -500, height/2 -normalStation/2 +300, normalStation, normalStation);  
        g.setColor(Color.black);
        g.drawOval(width/2 -normalStation/2 -500, height/2 -normalStation/2 +300, normalStation, normalStation);         
        //Station 5.2
        g.drawString("Windy Junction", width/2 -normalStation/2 - 530, height/2 -normalStation/2 + 300 + 40);
        g.setColor(Color.gray);
        g.fillOval(width/2 -normalStation/2 -400, height/2 -normalStation/2 +300, normalStation, normalStation);  
        g.setColor(Color.black);
        g.drawOval(width/2 -normalStation/2 -400, height/2 -normalStation/2 +300, normalStation, normalStation); 
        
        //count up to 16

        // A burning station gets a red outline over its black one.
        drawFireOutlines(g);

        //Vehicles
        train1.displayVehicle(g, 255, 0, 0);
        train2.displayVehicle(g, 0, 0, 255);
        train3.displayVehicle(g, 255, 0, 255);
        train4.displayVehicle(g, 0, 255, 0);

        // Done with the map layer.
        g.dispose();

        // Side panels: drawn on the untranslated graphics, so dragging the
        // map never moves them.
        sidePanel.display(screenGraphics, width, height, time,
                new Vehicles[]{train1, train2, train3, train4});
        
    
        


    }

    // The home screen is always the true entry point, even if this class is
    // run directly, so the app never skips straight into the simulation.
    public static void main(String[] args) {
        new Home();
    }
}

