# comp2000-simulation project

COMP2000 S2 2026 Assignment 

Team: OOPP The GOOP: Ashton, Luke, Daniel, Minying, Amanda

## Project Goal

A small transit-network simulation: four trains, each on its own coloured
line, share a station network and carry passengers between stops. Random
accidents, weather and fires delay or stop trains, and the whole timetable runs
on a simulated 06:00 AM to midnight day, resuming automatically each morning.

## Getting Started

1. Run `Home.java`.
2. START opens the simulation directly; INSTRUCTION explains the controls
   first, then NEXT opens it.
3. Space, or the play button (top right), starts/pauses the clock. The house
   button (top right) returns to the home screen. The 1x / 2x / 4x buttons
   next to it set the simulation speed.
4. Trains run 06:00 AM – midnight; the clock keeps going overnight and
   service resumes automatically at 6am.
5. The three tabs beside the left panel switch between Train current, Train
   table and Statistics.
6. To compile, from the `COMP2000 project` folder in PowerShell:
   `javac -d bin (Get-ChildItem src -Filter *.java).FullName`

## Folder Structure

- src: the folder to maintain sources
- lib: the folder to maintain dependencies

## Classes

```mermaid
graph TD
    Frame[java.awt.Frame]:::ext
    Home -->|extends| Frame
    Instruction -->|extends| Frame
    Panel -->|extends| Frame

    Home -->|START| Panel
    Home -->|INSTRUCTION| Instruction
    Instruction -->|BACK| Home
    Instruction -->|NEXT| Panel
    Panel -->|house button| Home

    Panel -->|creates & draws| SidePanel
    Panel -->|creates 4x| Train
    Panel -->|creates| Passenger
    Panel -->|creates| Time
    Panel -->|schedules| EventQueue

    Train -->|extends| Vehicles
    Vehicles -->|has a| Routes
    Vehicles -->|holds ArrayList of| Passenger
    Vehicles -->|delayed by| AccidentEvent
    Routes -->|has a list of| Stops

    AccidentEvent -->|extends| SimulationEvent
    EventQueue -->|orders| SimulationEvent

    Passenger -->|walks along| Routes
    Passenger -->|current / start / end| Stops

    SidePanel -->|owns| SideTrain
    SidePanel -->|owns| SideTable
    SidePanel -->|owns| SideStats
    SideTable -->|reads times from| Time

    Time -->|wraps| Timer[javax.swing.Timer]:::ext
    Time -->|owns| Weather
    Time -->|owns| Fire
    Time -->|owns| Speed

    classDef ext fill:#eee,stroke:#999,color:#333;
```

| Class | Role |
|---|---|
| `Home`, `Instruction`, `Panel` | the three screens (title, help, simulation); each `extends Frame` |
| `SidePanel` | top bar (clock, pause, house) and the tabs that switch views |
| `SideTrain` / `SideTable` / `SideStats` | the three left views: train-card list / timetable / statistics (delivered, average wait, on-time %) |
| `Vehicles` → `Train` | a vehicle on a route; `Train` adds the `t1()`–`t4()` factories |
| `Routes`, `Stops` | one line's ordered stops; one station |
| `Passenger` | a commuter walking stop to stop |
| `Time` | simulated clock, 06:00–midnight service window, throws on the hour boundary |
| `SimulationEvent` → `AccidentEvent` | a scheduled effect; delays one named train |
| `EventQueue<T extends SimulationEvent>` | priority queue of due events, wraps `PriorityQueue<T>` |
| `Pair<T, U>`, `GenericUtil` | small generics: a label/value pair, a bounded `max(T, T)` |
| `SimulationTimeException` | custom checked exception for the service window |
| `HazardStripe`, `StopSign`, `Flame`, `Assets` | drawing helpers (delay border, stop sign, flame); background-picture loader |
| `Speed` | the 1x / 2x / 4x setting; changes the timer delay in `Time` |
| `Weather`, `Fire` | the weather and fire rules (see below); both owned by `Time` |
| `DurationLimitException` | custom checked exception: a weather or fire asked to last too long |

To keep the number of files down, some small classes share a file:

| File | Classes inside |
|---|---|
| `Weather.java` | `Weather`, `Fire`, `HazardRule`, `Spell` |
| `SimulationEvent.java` | `SimulationEvent`, `AccidentEvent`, `EventQueue` |
| `Time.java` | `Time`, `Speed` |
| `Stops.java` | `Stops`, `Routes` |
| `SidePanel.java` | `SidePanel`, `SideView` |
| `HazardStripe.java` | `HazardStripe`, `StopSign`, `Flame` |
| `GenericUtil.java` | `GenericUtil`, `Pair` |
| `SimulationTimeException.java` | `SimulationTimeException`, `DurationLimitException` |

## Weather and Fire

Weather and fire are two separate systems. Both follow the simulation clock,
so the 1x / 2x / 4x speed changes them too. Weather covers the whole map. A
fire is at one station and only affects trains at or heading into that station.

**Weather** (each spell lasts a random time, from 15 minutes up to its limit):

| Weather | Longest | Effect on trains | Look |
|---|---|---|---|
| Sunny | 4 hours | none | |
| Cloudy | 2 hours | 15% chance of a short delay | red/yellow stripe |
| Rain | 1 hour | 80% chance of a delay | red/yellow stripe |
| Foggy | 30 minutes | every train stops until it clears | stop sign |

**Fire** (30% chance of starting each time the weather changes, at a random station):

| Fire | Longest | Spread | Effect on a train that reaches a burning station | Look |
|---|---|---|---|---|
| Small | 1 hour | none, one station | 20% chance to delay one train (only one train per fire) | red/yellow stripe |
| Medium | 1 hour | each neighbouring station has a 25% chance to catch fire, up to 2 more | 60% chance of a longer delay | red/yellow stripe |
| Large | 30 minutes | keeps spreading until 4 or 5 stations burn | stops until the fire is out | stop sign |

A train is checked once each time it reaches a burning station (or the stop
just before it), so a train that passes later is also affected. Neighbouring
stations are the ones right before and after a station on a line.
For example, if Central is on fire, a medium fire may spread to Rosebury,
Jubilee or Sherie Grove. The top bar shows the start and the spread, e.g.
"Fire: Medium fire at Central +2". Each burning station also shows a small
flame (`Flame`) and a red outline instead of the black one.

**The rules that tie them together:**

1. A weather never follows itself, so no weather can run past its limit.
2. Rain and fog never follow each other. After one of them it is sunny or
   cloudy first.
3. No fire in rain or fog. When rain or fog starts, any burning fire is put out straight away.
4. The time of day makes some weather more likely: fog at night, sunny or
   cloudy in the morning, sunny or rain in the afternoon.
5. A delayed train still moves after its delay ends. A stopped train does not
   move at all until the fog lifts or the fire is out.
6. A fire is certain after 5 weather changes without one. An hour after a small
   fire, the next fire is 50% more likely to be medium; two hours after a
   medium fire, it is 50% more likely to be large.

```mermaid
flowchart LR
    sim["simulation time"] --> weather["weather (sunny, cloudy, rain, foggy)"]
    sim --> fire["fire event (none, small, med, large)"]
    weather --> effect["train effect calculation (check weather, fire location, delay possible)"]
    fire --> effect
    effect --> normal["normal operation"]
    effect --> delayed["delayed / stop"]
```

Weather and fire both change the trains (a delay or a stop). Asking for a
weather spell or a fire longer than its limit throws `DurationLimitException`.



## Project Status

Status: DONE / PARTLY DONE / TODO

| Feature | Status | Note |
|---|---|---|
| Top bar | DONE | clock, date, pause, house button |
| Train list (SideTrain) | PARTLY DONE | no passenger-waiting count yet |
| Timetable (SideTable) | DONE | one row per train + "busiest right now" |
| Statistics (SideStats) | DONE | delivered, average wait, on-time % per train and in total |
| Time | DONE | 06:00–midnight, ~5 sim-min/real-sec, random wobble |
| Accidents / delays | DONE | `EventQueue<AccidentEvent>`, hazard-stripe indicator |
| Speed control | DONE | 1x / 2x / 4x buttons, `Speed` |
| Day-night tint | DONE | map colour follows the time of day |
| Weather | DONE | sunny / cloudy / rain / fog, time limits, stop sign for fog |
| Fire | DONE | small / medium / large, never in rain or fog |
| Random passengers | PARTLY DONE | random start/end; no rush-hour spawn curve |
| Passenger types by colour | TODO | e.g. student/worker colour key |
| Exceptions | DONE | `SimulationTimeException`, `DurationLimitException`; image loads fall back instead of crashing |
| Home / Instruction screens | DONE | |
| Clickable stations | TODO | show waiting passengers, next train |

## Ideas for later

- More vehicle types (`Bus`, `Tram`) as further `Vehicles` subclasses.
- More `SimulationEvent` types (rush hour, scheduled arrival) once passenger spawning supports them.
- `RouteNotFoundException`, `TrainFullException`.
- Train capacity limits; `Stops.checkCapacity()` wired into boarding time.
- Map zoom on scroll; click a line to highlight it; larger-font toggle.
- Station data in a text/JSON file instead of hardcoded in `Stops`/`Routes`.
- JUnit tests for `Routes.getNextTowards`, `Vehicles.moveVehicle`, `Time` formatting.
- More accident causes (breakdown, collision, derailment); different weather in different parts of the map; a replacement bus while a train is delayed.
- Train images instead of coloured rectangles.



