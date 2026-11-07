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

    int width = 1280;  // screen width
    int height = 720;  // screen height

    int majorStation = 40;  // big station
    int normalStation= 20;  // normal station

    // the four trains, one on each line
    Train train1 = Train.t1();
    Train train2 = Train.t2();
    Train train3 = Train.t3();
    Train train4 = Train.t4();
    Time time;
    SidePanel sidePanel = new SidePanel();
    
    Passenger pass1 = Passenger.pass1();
    ArrayList<Passenger> passengers = new ArrayList<>();

    // accidents waiting for their step, earliest first
    EventQueue<SimulationEvent> accidents = new EventQueue<>();

    Image dbImage;
    Graphics dbGraphics;

    // background pictures, null if the file is missing
    Image skyImage;
    Image treeImage;

    Insets insets = new Insets(0, 0, 0, 0);

    // dragging the map moves it, the side panels stay still
    int panOffsetX = 0;
    int panOffsetY = 0;
    int dragStartX;
    int dragStartY;
    boolean dragging = false;
    boolean draggingScrollbar = false;  // true while the scrollbar is held


    // sets up the window, the clock and the mouse and key handling
    public Panel() {
        setTitle("Transit Sim");
        setSize(width, height);
        // load the pictures before the window shows so the first paint is not blank
        loadBackgroundImages();
        setVisible(true);

        // close the program when the window is closed
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent we) {
                System.exit(0);
            }

            // the border size is only right once the window is open
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

        // every 5th tick (1 real second) the trains move and passengers board
        time = new Time(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // advance() throws when service opens or closes, the tick just carries on
                try {
                    time.advance();

                    // unused: spawnPassengers() does not create anyone yet
                    // if (time.isServiceHours()) {
                    //     Passenger.spawnPassengers();
                    // }

                    if (time.isOnSecond() && time.isServiceHours()) {
                        long step = time.ticks / 5;
                        Vehicles[] allTrains = {train1, train2, train3, train4};

                        // run the accidents that are due
                        while (accidents.hasDueEvent(step)) {
                            accidents.nextEvent().execute(allTrains);
                        }

                        for (Vehicles train : allTrains) {
                            train.moveVehicle();
                        }

                        // each passenger gets on or off at the stop the train is at
                        for (Vehicles train : allTrains) {
                            for (Passenger p : passengers) {
                                p.checkBoarding(train.getCurStop(), train.onBoard);
                            }
                        }
                    }
                } catch (SimulationTimeException ste) {
                    //System.out.println(ste.getMessage());
                }
                repaint();
            }
        });

        // space starts and pauses the clock
        addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                    time.toggle();
                }
            }
        });

        // one handler for click, drag and wheel
        // mx and my take off the window border so clicks line up with the drawing
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
                // highlight the buttons and scrollbar when the mouse is over them
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
                // the wheel scrolls the train list when the mouse is over the left panel
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

    // 3 to 5 accidents, each with a random train, step and delay length
    private void scheduleRandomAccidents() {
        Random random = new Random();
        Train[] allTrains = {train1, train2, train3, train4};
        int accidentCount = 3 + random.nextInt(3);

        for (int i = 0; i < accidentCount; i++) {
            Train chosen = allTrains[random.nextInt(allTrains.length)];
            long step = 10 + random.nextInt(200);  // spread across the day
            int delaySteps = 3 + random.nextInt(6);
            accidents.add(new AccidentEvent(step, chosen.getName(), delaySteps));
        }
    }

    // the pictures are optional, the map works without them
    private void loadBackgroundImages() {
        skyImage = Assets.load("1.png");
        treeImage = Assets.load("2.png");
    }

    // red ring over each burning station, drawn on top of the black one
    private void drawFireOutlines(Graphics g) {
        Train[] allTrains = {train1, train2, train3, train4};
        java.util.Arrays.stream(allTrains)
                .flatMap(train -> train.route.stations.stream())
                .filter(stop -> stop.onFire)
                .distinct()
                .forEach(stop -> {
                    int size = normalStation;
                    if (stop.capacity >= 100) {
                        size = majorStation;  // Central, Town Hall, Troll-upon-Bridge
                    }
                    g.setColor(Color.red);
                    g.drawOval(stop.x - size / 2, stop.y - size / 2, size, size);
                });
    }

    // stop the clock, close this window and show the home screen
    private void goHome() {
        if (time.isRunning()) {
            time.toggle();
        }
        dispose();
        new Home();
    }

    // draw to an off-screen image first, then copy it over, so it does not flicker
    public void update(Graphics g) {
        if (dbImage == null) {
            dbImage = createImage(width, height);
            dbGraphics = dbImage.getGraphics();
        }
        dbGraphics.setColor(getBackground());
        dbGraphics.fillRect(0, 0, width, height);
        paint(dbGraphics);
        insets = getInsets();  // read every frame, a one time read can be wrong
        g.drawImage(dbImage, insets.left, insets.top, this);
    }

    //@Override
    public void paint(Graphics screenGraphics) {
        // reset the font each frame, the clock uses a bigger one
        screenGraphics.setFont(new Font("SansSerif", Font.PLAIN, 12));

        // pictures stay fixed when the map is dragged
        if (skyImage != null) {
            screenGraphics.drawImage(skyImage, 0, 0, width, height, this);
        }
        if (treeImage != null) {
            screenGraphics.drawImage(treeImage, 0, 0, width, height, this);
        }

        // the map layer moves with the drag, the side panels do not
        Graphics g = screenGraphics.create();
        g.translate(panOffsetX, panOffsetY);

        // line thickness
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
        

        // burning stations get a red outline
        drawFireOutlines(g);

        //Vehicles
        train1.displayVehicle(g, 255, 0, 0);
        train2.displayVehicle(g, 0, 0, 255);
        train3.displayVehicle(g, 255, 0, 255);
        train4.displayVehicle(g, 0, 255, 0);

        // end of the map layer
        g.dispose();

        // side panels are drawn last so the drag does not move them
        sidePanel.display(screenGraphics, width, height, time,
                new Vehicles[]{train1, train2, train3, train4});
        
    
        


    }

    // always start from the home screen
    public static void main(String[] args) {
        new Home();
    }
}

