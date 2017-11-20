/**
 * Created by constie on 18.11.2017.
 */
/**
 * Created by constie on 15.11.2017.
 */
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

public class MoonBase implements MoonBaseInterface {
    private SortedMap<Integer, ArrayList<AirlockInterface>> airlocks;
//    private List<Integer> airlocks;
    private HashMap<AirlockInterface, LinkedBlockingQueue<CargoInterface>> ac;
    private HashMap<AirlockInterface, Runnable> airthread;
    private List<CargoInterface> cargos = Collections.synchronizedList(new ArrayList<CargoInterface>());

    @Override
    public void setAirlocksConfiguration(List<AirlockInterface> airlocks) {
        System.out.println("airlocks: " + airlocks);
        for (AirlockInterface airlock: airlocks) {
            System.out.println("size: " + airlock.getSize());
            this.airlocks.putIfAbsent(airlock.getSize(), new ArrayList<>()).add(airlock);
            this.ac.put(airlock, new LinkedBlockingQueue<>());
            airthread.put(airlock,
                new Thread( new Runnable() {
                    public void run() {
                        while(!ac.get(airlock).isEmpty()){
//                            airlock.setEventsListener(eventListenerInside());
                            System.out.println("airlock not empty");
                        };
                        try {
                            airlock.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                })
                );
        }

    }


    @Override
    public void cargoTransfer(CargoInterface cargo, Direction direction) {

        AirlockInterface minAirlock = null;
        Map<Integer, ArrayList<AirlockInterface>> mm = this.airlocks.tailMap(cargo.getSize());
        for (Integer i: mm.keySet()) {
            for (AirlockInterface aa: mm.get(i)) {
                if(ac.get(aa).isEmpty()){
                    try {
                        ac.get(aa).put(cargo);
                        airthread.get(aa).notify();
                        return;
                    } catch (InterruptedException e) {
                        continue;
                    }
                } else {
                    if(minAirlock == null){
                        minAirlock = aa;
                    } else {
                        if(ac.get(aa).size() < ac.get(minAirlock).size()){
                            minAirlock = aa;
                        }
                    }
                }
            }
        }
        try {
            ac.get(minAirlock).put(cargo);

            airthread.get(minAirlock).notify();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


    }

//
//    private AirlockInterface.EventsListenerInterface eventListenerInside(){
//        return event -> {
//            if (cargo.getDirection() == Direction.INSIDE){
//                if (event == AirlockInterface.Event.EXTERNAL_AIRTIGHT_DOORS_OPENED) {
//                    airlock.insertCargo(cargo);
//                } else if (event == AirlockInterface.Event.CARGO_INSIDE) {
//                    airlock.closeExternalAirtightDoors();
//                } else if (event == AirlockInterface.Event.EXTERNAL_AIRTIGHT_DOORS_CLOSED) {
//                    airlock.openInternalAirtightDoors();
//                } else if (event == AirlockInterface.Event.INTERNAL_AIRTIGHT_DOORS_OPENED) {
//                    airlock.ejectCargo();
//                } else if (event == AirlockInterface.Event.AIRLOCK_EMPTY) {
//                    airlock.closeInternalAirtightDoors();
//                } else if (event == AirlockInterface.Event.INTERNAL_AIRTIGHT_DOORS_CLOSED) {
////                        synchronized (locks) {
////                            locks.notify();
////                        }
//                    synchronized (airlock) {
//                        airlock.notify();
//                    }
//                } else if (event == AirlockInterface.Event.INTERNAL_AIRTIGHT_DOORS_CLOSED) {
//                    throw new IllegalStateException();
//                }
//            } else {
//                if (event == AirlockInterface.Event.INTERNAL_AIRTIGHT_DOORS_OPENED) {
//                    airlock.insertCargo(cargo);
//                }
//                else if (event == AirlockInterface.Event.CARGO_INSIDE) {
//                    airlock.closeInternalAirtightDoors();
//                }
//                else if (event == AirlockInterface.Event.INTERNAL_AIRTIGHT_DOORS_CLOSED) {
//                    airlock.openExternalAirtightDoors();
//                }
//                else if (event == AirlockInterface.Event.EXTERNAL_AIRTIGHT_DOORS_OPENED) {
//                    airlock.ejectCargo();
//                }
//                else if (event == AirlockInterface.Event.AIRLOCK_EMPTY) {
//                    airlock.closeExternalAirtightDoors();
//                }
//                else if (event == AirlockInterface.Event.EXTERNAL_AIRTIGHT_DOORS_CLOSED) {
////                        synchronized (locks) {
////                            locks.notify();
////                        }
//                }
//                else if (event == AirlockInterface.Event.INTERNAL_AIRTIGHT_DOORS_CLOSED){
//                    throw new IllegalStateException();
//                }
//            }
//        };
//    }


    public static void main(String args[]) {

    }


}
