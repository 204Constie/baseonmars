/**
 * Created by constie on 18.11.2017.
 */
/**
 * Created by constie on 15.11.2017.
 */
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

public class MoonBase implements MoonBaseInterface {
    private List<AirlockInterface> air = new ArrayList<>();
//    private SortedMap<Integer, ArrayList<AirlockInterface>> air = new TreeMap<Integer, ArrayList<AirlockInterface>>();
    private SortedMap<Integer, ArrayList<AirlockInterface>> dummyair = new TreeMap<Integer, ArrayList<AirlockInterface>>();
    private ConcurrentHashMap<AirlockInterface, LinkedBlockingQueue<CargoInterface>> ac = new ConcurrentHashMap<AirlockInterface, LinkedBlockingQueue<CargoInterface>>();

//    private HashMap<AirlockInterface, Runnable> airthread = new HashMap<AirlockInterface, Runnable>();
    private List<CargoInterface> cargos = Collections.synchronizedList(new ArrayList<CargoInterface>());
    private Integer jj = new Integer(3);
    private HashMap<AirlockInterface, Boolean> flagMap = new HashMap<AirlockInterface, Boolean>();
    private HashMap<AirlockInterface, Boolean> cargoFlagMap = new HashMap<AirlockInterface, Boolean>();
    int iter =0;
    int bigiter =0;

    @Override
    public void setAirlocksConfiguration(List<AirlockInterface> airlocks) {
        this.air = airlocks;
//        PMO_SystemOutRedirect.println("init activeCount: " + java.lang.Thread.activeCount());


        for (AirlockInterface airlock: air) {
//            this.air.putIfAbsent(airlock.getSize(), new ArrayList<AirlockInterface>());
            this.dummyair.putIfAbsent(airlock.getSize(), new ArrayList<AirlockInterface>());
//            ArrayList<AirlockInterface> tmpa = this.air.get(airlock.getSize());
            ArrayList<AirlockInterface> tmpa = this.dummyair.get(airlock.getSize());
            tmpa.add(airlock);
            this.ac.put(airlock, new LinkedBlockingQueue<>());
            this.flagMap.put(airlock, Boolean.FALSE);
            this.cargoFlagMap.put(airlock, Boolean.FALSE);


            new Thread(new Runnable() {
                public void run() {
                    PMO_SystemOutRedirect.println("bigiter: " + bigiter);
                    PMO_SystemOutRedirect.println("iter: " + iter);
//                    synchronized (airlock) {
//                        try {
//                            airlock.wait();
//
//                        } catch (InterruptedException e) {
//                            //                            e.printStackTrace();
//                        }
//                    }
//                    if(ac.contains(airlock)){};
//                    if(flagMap.get(airlock)){};

                        if(ac.get(airlock).isEmpty() || flagMap.get(airlock)){
                            synchronized (airlock) {
                                try {
                                    airlock.wait();

                                } catch (InterruptedException e) {
                                e.printStackTrace();
                                }
                            }
                        }
                        if(flagMap.get(airlock)){
                            airlock.setEventsListener(eventListenerInside(ac.get(airlock).peek(), airlock));
                        }
//                        PMO_SystemOutRedirect.println("checkvalue " + !ac.get(airlock).isEmpty());
                        while (!ac.get(airlock).isEmpty() && !flagMap.get(airlock)) {
//                            PMO_SystemOutRedirect.println("1111111111111111ac.get(airlock).peek():    " + airlock + ac.get(airlock).peek());
//                            PMO_SystemOutRedirect.println("ac.get(airlock)ac.get(airlock)ac.get(airlock)ac.get(airlock): " + ac.get(airlock).size() + airlock);
//                            if(cargoFlagMap.get(airlock)){
//                                PMO_SystemOutRedirect.println("cargoFlagMap.get(airlock): " + cargoFlagMap.get(airlock));
//
//                                try {
//                                    ac.get(airlock).take();
//                                    cargoFlagMap.put(airlock, Boolean.FALSE);
//
//                                } catch (InterruptedException e) {
//                                    e.printStackTrace();
//                                }
//                                PMO_SystemOutRedirect.println("ac.get(airlock)ac.get(airlock)ac.get(airlock)ac.get(airlock): " + ac.get(airlock).size() + airlock);
//                            }
//                            PMO_SystemOutRedirect.println("22222222222222222222ac.get(airlock).peek():    " + airlock + ac.get(airlock).peek());
                            airlock.setEventsListener(eventListenerInside(ac.get(airlock).peek(), airlock));

//                            PMO_SystemOutRedirect.println("                     notified" );
                            CargoInterface c = ac.get(airlock).peek();
                            if (c.getDirection() == Direction.INSIDE) {
                                if(!flagMap.get(airlock)){
                                    airlock.openExternalAirtightDoors();
                                    flagMap.put(airlock, Boolean.TRUE);
                                }
//                                airlock.openExternalAirtightDoors();
//                                flagMap.put(airlock, Boolean.TRUE);
                                synchronized (airlock) {
                                    while (flagMap.get(airlock)) {
                                        try {
                                            airlock.wait();
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }
                            } else {
                                if(!flagMap.get(airlock)){
                                    airlock.openInternalAirtightDoors();
                                    flagMap.put(airlock, Boolean.TRUE);
                                }
//
// airlock.openInternalAirtightDoors();
//                                flagMap.put(airlock, Boolean.TRUE);
                                synchronized (airlock) {
                                    while (flagMap.get(airlock)) {
                                        try {
                                            airlock.wait();
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }
                            }

                            try {
                                ac.get(airlock).take();

                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
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

        bigiter++;

        for (Integer i : mm.keySet()) {
            for (AirlockInterface aa : mm.get(i)) {

                    if (ac.get(aa).isEmpty()) {
                        for (AirlockInterface as: air) {
                            if(aa == as){
                                airlock = as;
                                break;
                            }

                        }
//                        synchronized (ac){
//                            try {
//                                ac.get(aa).put(cargo);
//
//                            } catch (InterruptedException e) {
//                                continue;
//                            }
//                        }
                        synchronized (airlock){
                            try {
                                ac.get(aa).put(cargo);
                                iter++;

                            } catch (InterruptedException e) {
                                continue;
                            }
                        }
                        synchronized (airlock){
//                            try {
//                                ac.get(aa).put(cargo);
//
//                            } catch (InterruptedException e) {
//                                continue;
//                            }

//                                ac.get(aa).put(cargo);
//                                PMO_SystemOutRedirect.println("cargo                      cargo: " + cargo);
//                                PMO_SystemOutRedirect.println("notify on: " + airlock);
//                                PMO_SystemOutRedirect.println("notify value: " + ac.get(airlock).isEmpty());
//                                PMO_SystemOutRedirect.println("activeCount: " + java.lang.Thread.activeCount());
//                                airlock.notify();
                                if(!flagMap.get(airlock)) {
                                    airlock.notify();
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
        for (AirlockInterface as: air) {
            if(minAirlock == as){
                airlock = as;
                break;
            }

        }
//        synchronized (ac){
//            try {
//                ac.get(minAirlock).put(cargo);
//            } catch (InterruptedException e) {
//
//            }
//        }
        synchronized (airlock) {
            try {
                ac.get(minAirlock).put(cargo);
                iter++;
            } catch (InterruptedException e) {

            }
        }
        synchronized (airlock){
//            try {
//                ac.get(minAirlock).put(cargo);
//            } catch (InterruptedException e) {
//
//            }

//                ac.get(minAirlock).put(cargo);
//                PMO_SystemOutRedirect.println("cargo                      cargo: " + cargo);
//                PMO_SystemOutRedirect.println("notify on: " + airlock);
//                PMO_SystemOutRedirect.println("notify value: " + ac.get(airlock).isEmpty());
//                PMO_SystemOutRedirect.println("activeCount: " + java.lang.Thread.activeCount());
                if(!flagMap.get(airlock)) {
                    airlock.notify();
                }
                return;


        }

    }


    private AirlockInterface.EventsListenerInterface eventListenerInside(CargoInterface cargo, AirlockInterface airlock) {
        return new AirlockInterface.EventsListenerInterface() {
            public void newAirlockEvent(AirlockInterface.Event event) {
//                flagMap.put(airlock, Boolean.FALSE);
//                synchronized (airlock) {
//                    airlock.notify();
//                }
                boolean reaction = eventReaction(event, cargo, airlock);
                if (!reaction) {
                    flagMap.put(airlock, Boolean.TRUE);
                    synchronized (airlock) {
                        while (flagMap.get(airlock)) {
                            try {
                                airlock.wait();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                } else {
                    flagMap.put(airlock, Boolean.FALSE);
                    cargoFlagMap.put(airlock, Boolean.TRUE);
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
