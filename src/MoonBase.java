/**
 * Created by constie on 18.11.2017.
 */
/**
 * Created by constie on 15.11.2017.
 */
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

public class MoonBase implements MoonBaseInterface {
    private List<AirlockInterface> airlocks = new ArrayList<>();
//    private SortedMap<Integer, ArrayList<AirlockInterface>> airlocks = new TreeMap<Integer, ArrayList<AirlockInterface>>();
    private SortedMap<Integer, ArrayList<AirlockInterface>> dummyairlocks = new TreeMap<Integer, ArrayList<AirlockInterface>>();
    private HashMap<AirlockInterface, LinkedBlockingQueue<CargoInterface>> ac = new HashMap<AirlockInterface, LinkedBlockingQueue<CargoInterface>>();
//    private HashMap<AirlockInterface, Runnable> airthread = new HashMap<AirlockInterface, Runnable>();
    private List<CargoInterface> cargos = Collections.synchronizedList(new ArrayList<CargoInterface>());

    @Override
    public void setAirlocksConfiguration(List<AirlockInterface> airlocks) {
        this.airlocks = airlocks;
        PMO_SystemOutRedirect.println("init activeCount: " + java.lang.Thread.activeCount());


        for (AirlockInterface airlock: airlocks) {
//            this.airlocks.putIfAbsent(airlock.getSize(), new ArrayList<AirlockInterface>());
            this.dummyairlocks.putIfAbsent(airlock.getSize(), new ArrayList<AirlockInterface>());
//            ArrayList<AirlockInterface> tmpa = this.airlocks.get(airlock.getSize());
            ArrayList<AirlockInterface> tmpa = this.dummyairlocks.get(airlock.getSize());
            tmpa.add(airlock);
            this.ac.put(airlock, new LinkedBlockingQueue<>());


            Thread th = new Thread(new Runnable() {
                public void run() {


                    synchronized (airlock) {

//                        PMO_SystemOutRedirect.println("                     " + java.lang.Thread.currentThread().getName());
//                        PMO_SystemOutRedirect.println("]]]]]]]]]]]]" + java.lang.Thread.activeCount());
//                        PMO_SystemOutRedirect.println("------///////////////////////\\\\\\\\\\\\\\\\\\\\---"+ac.get(airlock).isEmpty());
                        if(ac.get(airlock).isEmpty()){

                            try {
                                PMO_SystemOutRedirect.println("wait on: " + airlock);
                                airlock.wait();

                            } catch (InterruptedException e) {
//                            e.printStackTrace();
                            }
                            return;
                        }
                        PMO_SystemOutRedirect.println("checkvalue " + !ac.get(airlock).isEmpty());
//                        while (!ac.get(airlock).isEmpty()) {
                        while (!ac.get(airlock).isEmpty()) {
                            PMO_SystemOutRedirect.println("                     notified" );
                            CargoInterface c = ac.get(airlock).peek();
                            if (c.getDirection() == Direction.INSIDE) {
                                airlock.openExternalAirtightDoors();
                                try {
                                    airlock.wait();
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            } else {
                                airlock.openInternalAirtightDoors();
                                try {
                                    airlock.wait();
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                            airlock.setEventsListener(eventListenerInside(ac.get(airlock).peek(), airlock));

//                            try {
//
//                                ac.get(airlock).take();
//                            } catch (InterruptedException e) {
//                                e.printStackTrace();
//                            }
//                            PMO_SystemOutRedirect.println("airlock not empty");

                        }

                        ;
//                        try {
//                            airlock.wait();
//                            PMO_SystemOutRedirect.println("wait in run");
//                        } catch (InterruptedException e) {
////                            e.printStackTrace();
//                        }
                    }
                }
            });
//            synchronized (airthread) {
//                airthread.put(airlock, th);
//            }
            th.start();
//            PMO_SystemOutRedirect.println("ac: " + th.getState());

        }

    }


    @Override
     public void cargoTransfer(CargoInterface cargo, Direction direction) {
        AirlockInterface airlock = null;
        AirlockInterface minAirlock = null;
//        PMO_SystemOutRedirect.println("ac: " + airlocks);
//        Map<Integer, ArrayList<AirlockInterface>> mm = this.airlocks.tailMap(cargo.getSize());
        Map<Integer, ArrayList<AirlockInterface>> mm = this.dummyairlocks.tailMap(cargo.getSize());

        for (Integer i : mm.keySet()) {
            for (AirlockInterface aa : mm.get(i)) {

                    if (ac.get(aa).isEmpty()) {
                        for (AirlockInterface as: airlocks) {
                            if(aa == as){
                                airlock = as;
                                break;
                            }

                        }
                        synchronized (airlock){
                            try {
                                ac.get(aa).put(cargo);
                                PMO_SystemOutRedirect.println("notify on: " + airlock);
                                PMO_SystemOutRedirect.println("notify value: " + ac.get(airlock).isEmpty());
                                PMO_SystemOutRedirect.println("activeCount: " + java.lang.Thread.activeCount());
                                airlock.notify();
//                                return;
                            } catch (InterruptedException e) {
                                continue;
                            }

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
//        synchronized (minAirlock) {
//            try {
//                ac.get(minAirlock).put(cargo);
////                PMO_SystemOutRedirect.println("------\\\\\\\\\\\\\\\\\\\\---"+ac.get(minAirlock).isEmpty());
//                minAirlock.notify();
//            } catch (InterruptedException e) {
////                e.printStackTrace();
//            }
//
//        }
        for (AirlockInterface as: airlocks) {
            if(minAirlock == as){
                airlock = as;
                break;
            }

        }
        synchronized (airlock){
            try {
                ac.get(minAirlock).put(cargo);
                PMO_SystemOutRedirect.println("notify on: " + airlock);
                PMO_SystemOutRedirect.println("notify value: " + ac.get(airlock).isEmpty());
                PMO_SystemOutRedirect.println("activeCount: " + java.lang.Thread.activeCount());
                airlock.notify();
//                return;
            } catch (InterruptedException e) {

            }

        }

    }


    private AirlockInterface.EventsListenerInterface eventListenerInside(CargoInterface cargo, AirlockInterface airlock) {
        return new AirlockInterface.EventsListenerInterface() {
            public void newAirlockEvent(AirlockInterface.Event event) {
                PMO_SystemOutRedirect.println("-------------------------------------------------------------new event");
                synchronized (airlock) {
                    airlock.notify();
                    boolean reaction = eventReaction(event, cargo, airlock);
                    if (!reaction) {
                        try {
                            airlock.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
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
