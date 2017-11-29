/**
 * Created by constie on 18.11.2017.
 */
/**
 * Created by constie on 15.11.2017.
 */
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.LinkedBlockingQueue;

public class MoonBase implements MoonBaseInterface {
    private List<AirlockInterface> air = new ArrayList<>();
//    private SortedMap<Integer, ArrayList<AirlockInterface>> air = new TreeMap<Integer, ArrayList<AirlockInterface>>();
    private ConcurrentSkipListMap<Integer, ArrayList<AirlockInterface>> dummyair = new ConcurrentSkipListMap<Integer, ArrayList<AirlockInterface>>();
    private ConcurrentHashMap<AirlockInterface, List<CargoInterface>> ac = new ConcurrentHashMap<AirlockInterface, List<CargoInterface>>();

//    private HashMap<AirlockInterface, Runnable> airthread = new HashMap<AirlockInterface, Runnable>();
//    private List<CargoInterface> cargos = Collections.synchronizedList(new ArrayList<CargoInterface>());
    private ConcurrentHashMap<AirlockInterface, Boolean> flagMap = new ConcurrentHashMap<AirlockInterface, Boolean>();
    private ConcurrentHashMap<AirlockInterface, Boolean> cargoFlagMap = new ConcurrentHashMap<AirlockInterface, Boolean>();


    @Override
    public void setAirlocksConfiguration(List<AirlockInterface> airlocks) {
        this.air = airlocks;


        for (AirlockInterface airlock: air) {
//            this.air.putIfAbsent(airlock.getSize(), new ArrayList<AirlockInterface>());
            this.dummyair.putIfAbsent(airlock.getSize(), new ArrayList<AirlockInterface>());
//            ArrayList<AirlockInterface> tmpa = this.air.get(airlock.getSize());
            ArrayList<AirlockInterface> tmpa = this.dummyair.get(airlock.getSize());
            tmpa.add(airlock);
            this.ac.put(airlock, Collections.synchronizedList(new ArrayList<>()));
            this.flagMap.put(airlock, Boolean.FALSE);
//            this.cargoFlagMap.put(airlock, Boolean.FALSE);


            new Thread(new Runnable() {
                public void run() {

//                    while (ac.get(airlock).isEmpty() || flagMap.get(airlock)) {
                        synchronized (airlock) {
                            try {
                                airlock.wait();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
//                    }
//                        if(flagMap.get(airlock)){
//                            PMO_SystemOutRedirect.println("11ac.get(airlock).get(0), airlock): " + ac.get(airlock).get(0) + " " + airlock);
//                            airlock.setEventsListener(eventListenerInside(ac.get(airlock).get(0), airlock));
//                        }
//                    if (!ac.get(airlock).isEmpty() && !flagMap.get(airlock)){
                    while (!ac.get(airlock).isEmpty()) {
//                            PMO_SystemOutRedirect.println("ac: " + ac);


//                            synchronized (airlock) {
//                            PMO_SystemOutRedirect.println("22ac.get(airlock).get(0), airlock): " + ac.get(airlock).get(0) + " " + airlock);
//                            synchronized (ac) {
                        airlock.setEventsListener(eventListenerInside(ac.get(airlock).get(0), airlock));
//                            }

                            CargoInterface c = ac.get(airlock).get(0);

                            if (c.getDirection() == Direction.INSIDE) {
//                                if (!flagMap.get(airlock)) {
                                flagMap.put(airlock, Boolean.TRUE);
                                airlock.openExternalAirtightDoors();
//                                }


                            } else {
//                                if (!flagMap.get(airlock)) {
                                flagMap.put(airlock, Boolean.TRUE);
                                airlock.openInternalAirtightDoors();

//                                }
                            }


                            while (flagMap.get(airlock)) {
                                synchronized (airlock) {
                                    try {
                                        airlock.wait();
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }


//                            synchronized (ac) {
//                                ac.get(airlock).remove(0);
//                            }
//                            if(cargoFlagMap.get(airlock)){
                                ac.get(airlock).remove(0);
//                            }

                    }
//                    }
//                    else {
//
//                    }
                    }
            }).start();

        }

    }


    @Override
     public void cargoTransfer(CargoInterface cargo, Direction direction)  {
        AirlockInterface airlock = null;
        AirlockInterface minAirlock = null;
//        PMO_SystemOutRedirect.println("ac: " + air);
//        Map<Integer, ArrayList<AirlockInterface>> mm = this.air.tailMap(cargo.getSize());
        Map<Integer, ArrayList<AirlockInterface>> mm = this.dummyair.tailMap(cargo.getSize());


        for (Integer i : mm.keySet()) {
            for (AirlockInterface aa : mm.get(i)) {

                if (ac.get(aa).isEmpty()) {
//                    for (AirlockInterface as: air) {
//                        if(aa == as){
//                            airlock = as;
//                            break;
//                        }
//
//                    }
//                        synchronized (ac){

                            ac.get(aa).add(cargo);
//                        }
                    synchronized (aa){
                        if(!flagMap.get(aa)) {
                            aa.notify();

                        }
                        return;
                    }

                } else {
                    if (minAirlock == null) {
                        minAirlock = aa;
                    } else {

                        if (ac.get(aa).size() < ac.get(minAirlock).size()) {
                            minAirlock = aa;
                        }
                    }
                }


            }
        }
//        for (AirlockInterface as: air) {
//            if(minAirlock == as){
//                airlock = as;
//                break;
//            }
//
//        }
//        synchronized (ac) {
            ac.get(minAirlock).add(cargo);
//        }
        synchronized (minAirlock){
            if(!flagMap.get(minAirlock)) {
                minAirlock.notify();

            }
            return;

        }

    }


    private AirlockInterface.EventsListenerInterface eventListenerInside(CargoInterface cargo, AirlockInterface airlock) {
        return new AirlockInterface.EventsListenerInterface() {
            public void newAirlockEvent(AirlockInterface.Event event) {
//                flagMap.put(airlock, Boolean.FALSE);

                flagMap.put(airlock, Boolean.TRUE);
                boolean reaction = eventReaction(event, cargo, airlock);
                if (!reaction) {

                        while (flagMap.get(airlock)) {
                            synchronized (airlock) {
                                try {
                                    airlock.wait();
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        }

                } else {
                    flagMap.put(airlock, Boolean.FALSE);
//                    cargoFlagMap.put(airlock, Boolean.TRUE);
                }

                synchronized (airlock) {
                    airlock.notify();
                }
            }


        };

    }

    private boolean eventReaction(AirlockInterface.Event event, CargoInterface cargo, AirlockInterface airlock){
        if (cargo.getDirection() == Direction.INSIDE) {
            if (event == AirlockInterface.Event.EXTERNAL_AIRTIGHT_DOORS_OPENED) {
                airlock.insertCargo(cargo);
                return false;
            } else if (event == AirlockInterface.Event.CARGO_INSIDE) {
                airlock.closeExternalAirtightDoors();
                return false;
            } else if (event == AirlockInterface.Event.EXTERNAL_AIRTIGHT_DOORS_CLOSED) {
                airlock.openInternalAirtightDoors();
                return false;
            } else if (event == AirlockInterface.Event.INTERNAL_AIRTIGHT_DOORS_OPENED) {
                airlock.ejectCargo();
                return false;
            } else if (event == AirlockInterface.Event.AIRLOCK_EMPTY) {
                airlock.closeInternalAirtightDoors();
                return false;
            } else if (event == AirlockInterface.Event.INTERNAL_AIRTIGHT_DOORS_CLOSED) {
                return true;
            } else if (event == AirlockInterface.Event.INTERNAL_AIRTIGHT_DOORS_CLOSED) {
                throw new IllegalStateException();

            }
        } else {
            if (event == AirlockInterface.Event.INTERNAL_AIRTIGHT_DOORS_OPENED) {
                airlock.insertCargo(cargo);
                return false;
            } else if (event == AirlockInterface.Event.CARGO_INSIDE) {
                airlock.closeInternalAirtightDoors();
                return false;
            } else if (event == AirlockInterface.Event.INTERNAL_AIRTIGHT_DOORS_CLOSED) {
                airlock.openExternalAirtightDoors();
                return false;
            } else if (event == AirlockInterface.Event.EXTERNAL_AIRTIGHT_DOORS_OPENED) {
                airlock.ejectCargo();
                return false;
            } else if (event == AirlockInterface.Event.AIRLOCK_EMPTY) {
                airlock.closeExternalAirtightDoors();
                return false;
            } else if (event == AirlockInterface.Event.EXTERNAL_AIRTIGHT_DOORS_CLOSED) {
                return true;
            } else if (event == AirlockInterface.Event.INTERNAL_AIRTIGHT_DOORS_CLOSED) {
                throw new IllegalStateException();

            }
        }
        return false;
    }

}
