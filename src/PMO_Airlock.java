import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class PMO_Airlock implements AirlockInterface, PMO_Testable {

	private AtomicBoolean inUse = new AtomicBoolean();
	private AtomicReference<DoorState> internalDoor = new AtomicReference<>();
	private AtomicReference<DoorState> externalDoor = new AtomicReference<>();
	private AtomicReference<CargoInterface> cargoSpace = new AtomicReference<>();
	private AtomicReference<CargoSpaceState> cargoSpaceState = new AtomicReference<>();
	private BlockingQueue<DelayObject> queue = new DelayQueue<>();
	private int size;
	private EventsListenerInterface evlis;
	private String airlockDescription;
	private AtomicInteger correctTransfersCounter;
	private Set<CargoInterface> transferedCargoHolder; // identyfikatory przekazanych ladunkow - globalnie
	private List<CargoInterface> cargoTransferHistory; // historia pracy sluzy
	private PMO_TestThreadsKeeper threadsKeeper; // lista wlasnych watkow
	private AtomicBoolean blockCommunication; // blokada komunikacji
	private Thread queueRunnerThread; // watek odpowiedzialny za obsluge kolejki
	private PMO_Cargo cargo2add; // cargo do dodania "w locie" (w trakcie wyprowadzania poprzedniego cargo)
	private AtomicBoolean cargoTransfered = new AtomicBoolean(false); // potwierdzenie dodania dodatkowej paczki
	private AtomicBoolean cargoTransfered2Test = new AtomicBoolean(false);
	private MoonBaseInterface base;
	private AtomicBoolean testOKFlag = new AtomicBoolean(true);

	{
		internalDoor.set(DoorState.CLOSED);
		externalDoor.set(DoorState.CLOSED);
		cargoSpaceState.set(CargoSpaceState.EMPTY);
		inUse.set(false);
		cargoSpace.set(null);
		cargoTransferHistory = Collections.synchronizedList(new ArrayList<>());

		queueRunnerThread = PMO_ThreadsHelper.startThread(new Runnable() {
			@Override
			public void run() {
				DelayObject dob = null;
				while (true) {
					try {
						PMO_Log.log("Oczekiwanie na obiekt w kolejce");
						dob = queue.take();
						PMO_Log.log("Z kolejki odebrano obiekt");
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					if (dob != null)
						dob.run();

				}
			}
		});

	}

	@Override
	public boolean isOK() {
		return testOKFlag.get();
	}

	private void addThreadToThreadsKeeper() {
		if (threadsKeeper != null) {
			threadsKeeper.addThread();
		}
	}

	private void removeThreadFromThreadsKeeper() {
		if (threadsKeeper != null)
			threadsKeeper.removeThread();
	}

	public void setThreadsKeeper(PMO_TestThreadsKeeper threadsKeeper) {
		this.threadsKeeper = threadsKeeper;
		threadsKeeper.addThread(queueRunnerThread);
	}

	public void setCorrectTransfersCounter(AtomicInteger correctTransfersCounter) {
		this.correctTransfersCounter = correctTransfersCounter;
	}

	public void setBlockCommunicationFlag(AtomicBoolean blockCommunication) {
		this.blockCommunication = blockCommunication;
	}

	public void setTransferedCargoHolder(Set<CargoInterface> set) {
		transferedCargoHolder = set;
	}

	public void blockCommunicationIfNecessary() {
		if (blockCommunication == null)
			return;
		if (blockCommunication.get()) {
			PMO_ThreadsHelper.wait(blockCommunication);
		}
	}

	public void setMoonBaseInterface(MoonBaseInterface base) {
		this.base = base;
	}

	public void setCargo2Add(PMO_Cargo cargo) {
		cargo2add = cargo;
	}

	public PMO_Airlock(String name, int size) {
		this.size = size;
		airlockDescription = "Sluza " + name + " size = " + size + " > ";
	}

	private void log(String txt) {
		PMO_Log.log(airlockDescription + txt);
	}

	private void error(String txt) {
		log(txt);
		PMO_CommonErrorLog.error(airlockDescription + txt);
		testOKFlag.set(false);
	}

	private void criticalError(String txt) {
		error(txt);
		PMO_CommonErrorLog.criticalMistake();
	}

	// stan drzwi
	private enum DoorState {
		CLOSED {
			@Override
			DoorState nextCorrectState() {
				return OPENED;
			}
		},
		OPENED {
			@Override
			DoorState nextCorrectState() {
				return CLOSED;
			}

		};

		abstract DoorState nextCorrectState();
	}

	enum CargoSpaceState {
		EMPTY, FULL;
	}

	private class DelayObject implements Delayed, Runnable {
		private long startAt;
		private Runnable code2run;
		AirlockInterface.Event event2send;

		public DelayObject(Runnable code2run, AirlockInterface.Event event2send, long delay) {
			startAt = System.currentTimeMillis() + delay;
			this.code2run = code2run;
			this.event2send = event2send;
		}

		@Override
		public void run() {
			if (code2run != null)
				code2run.run();

			unsetAirlockInUse();
			PMO_ThreadsHelper.startThread(() -> {
				addThreadToThreadsKeeper();
				if (evlis == null) {
					error("Nie zarejestrowano EventsListener-a");
				} else {
					blockCommunicationIfNecessary();
					log("Wysylamy do Listenera zdarzenie " + event2send );
					evlis.newAirlockEvent(event2send);
				}
				removeThreadFromThreadsKeeper();
			});
		}

		@Override
		public int compareTo(Delayed o) {
			if (this.startAt < ((DelayObject) o).startAt) {
				return -1;
			}

			if (this.startAt > ((DelayObject) o).startAt) {
				return 1;
			}

			return 0;
		}

		@Override
		public long getDelay(TimeUnit unit) {
			long diff = startAt - System.currentTimeMillis();
			return unit.convert(diff, TimeUnit.MILLISECONDS);
		}
	}

	/**
	 * Metoda generuje obiekt i umieszcza go w kolejce
	 * 
	 * @param code2run
	 *            kod do wykonania po uplywie opoznienia
	 * @param event2send
	 *            zdarzenie do przekazanie obiektowi nasluchujacemu
	 * @param delay
	 *            opoznienie obiektu
	 */
	private void delayObject2queue(Runnable code2run, AirlockInterface.Event event2send, long delay) {
		try {
			queue.put(new DelayObject(code2run, event2send, delay));
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	// sluza przestawiana w stan w-uzyciu
	private boolean setAirlockInUse() {
		boolean changed = inUse.compareAndSet(false, true);
		if (!changed) {
			log("Proba zmiany stanu na w-uzyciu, ale taki stan juz stwierdzono");
			criticalError("Blad: przed zakonczeniem poprzedniego zlecenia wyslano kolejne.");
		} else {
			log("Zmiana stanu sluzy na w-uzyciu");
		}
		return changed;
	}

	// sluza przestawiana w stan w-spoczynku
	private boolean unsetAirlockInUse() {
		boolean changed = inUse.compareAndSet(true, false);
		if (!changed) {
			log("Proba zmiany stanu na w-spoczynku, ale taki stan juz stwierdzono");
			criticalError("Blad: operacje na jednej sluzie nakladaja sie na siebie.");
		} else {
			log("Zmiana stanu sluzy na w-spoczynku");
		}
		return changed;
	}

	private void airlockUnsealed() {
		log("Sssssssssssssssssss... to syczy uciekajace powietrze...");
		criticalError("Wykryto jednoczesne otwarcie drzwi sluzy.");
	}

	private void bothDoorsOpenedTest() {
		boolean result = (internalDoor.get() == DoorState.OPENED) && (externalDoor.get() == DoorState.OPENED);
		if (result)
			airlockUnsealed();
	}

	private void otherDoorOpenedTest(AtomicReference<DoorState> door) {
		boolean result = door.get() == DoorState.OPENED;
		if (result)
			airlockUnsealed();
	}

	private String getDoorsName(AtomicReference<DoorState> door) {
		return (door == internalDoor) ? "internal" : "external";
	}

	private void changeDoorState(AtomicReference<DoorState> doors, DoorState from, DoorState to) {
		DoorState state = doors.get();
		String doorsName = getDoorsName(doors);
		if (!doors.compareAndSet(from, to)) {
			log("Zmiana stanu drzwi " + doorsName + " z " + from + " na " + to + " zakonczona niepowodzeniem.");
			criticalError("Drzwi " + doorsName + " powinny byc w stanie " + from
					+ " a nie sa. Odczytany przed momentem stan to " + state);
		} else {
			log("Zmiana stanu drzwi " + doorsName + " z " + from + " na " + to);
		}
	}

	@Override
	public void openInternalAirtightDoors() {
		log("Zlecono openInternalAirtightDoors");
		openAirtightDoor(internalDoor, externalDoor, AirlockInterface.Event.INTERNAL_AIRTIGHT_DOORS_OPENED);
	}

	@Override
	public void openExternalAirtightDoors() {
		log("Zlecono openExternalAirtightDoors");
		openAirtightDoor(externalDoor, internalDoor, AirlockInterface.Event.EXTERNAL_AIRTIGHT_DOORS_OPENED);
	}

	private void openAirtightDoor(AtomicReference<DoorState> door, AtomicReference<DoorState> otherDoor,
			AirlockInterface.Event event) {
		setAirlockInUse();
		otherDoorOpenedTest(otherDoor);

		DoorState state = door.get();
		String doorName = getDoorsName(door);

		if (state == DoorState.OPENED) {
			log("WARNING: zlecono otwarcie otwartych juz drzwi " + doorName);
			delayObject2queue(null, event, PMO_Consts.DOOR_OPENING_DELAY_SHORT);
		} else if (state == DoorState.CLOSED) {
			log("Stan drzwi " + doorName + " - CLOSED - OK - uruchamiam naped drzwi");
			delayObject2queue(new Runnable() {
				public void run() {
					addCargo2MoonBaseSystem();
					changeDoorState(door, DoorState.CLOSED, DoorState.OPENED);
					bothDoorsOpenedTest();
				}
			}, event, PMO_Consts.DOOR_OPENING_DELAY);
		}
	}

	@Override
	public void closeInternalAirtightDoors() {
		log("Zlecono closeInternalAirtightDoors");
		closeAirtightDoor(internalDoor, externalDoor, AirlockInterface.Event.INTERNAL_AIRTIGHT_DOORS_CLOSED);
	}

	@Override
	public void closeExternalAirtightDoors() {
		log("Zlecono closeExternalAirtightDoors");
		closeAirtightDoor(externalDoor, internalDoor, AirlockInterface.Event.EXTERNAL_AIRTIGHT_DOORS_CLOSED);
	}

	private void closeAirtightDoor(AtomicReference<DoorState> door, AtomicReference<DoorState> otherDoor,
			AirlockInterface.Event event) {
		setAirlockInUse();
		bothDoorsOpenedTest();

		DoorState state = door.get();

		if (state == DoorState.CLOSED) {
			log("WARNING: zlecono zamkniecie zamknietych juz drzwi");
			delayObject2queue(null, event, PMO_Consts.DOOR_CLOSING_DELAY_SHORT);
		} else if (state == DoorState.OPENED) {
			log("Stan drzwi - OPENED - OK - uruchamiam naped drzwi");
			delayObject2queue(new Runnable() {
				public void run() {
					otherDoorOpenedTest(otherDoor);
					changeDoorState(door, DoorState.OPENED, DoorState.CLOSED);
				}
			}, event, PMO_Consts.DOOR_CLOSING_DELAY);
		}
	}

	@Override
	public void insertCargo(CargoInterface cargo) {
		log("Zlecono insertCargo. " + cargo);
		setAirlockInUse();
		bothDoorsOpenedTest();

		if (cargo == null) {
			criticalError("Zamiast ladunku do sluzy dotarl null");
		}

		if (cargo.getDirection() == Direction.INSIDE) {
			if (externalDoor.get() != DoorState.OPENED) {
				criticalError(cargo + " jest na zewnatrz, ale drzwi zewnetrzne sa zamkniete");
			}
			if (internalDoor.get() != DoorState.CLOSED) {
				criticalError(cargo + " jest na zewnatrz, ale to drzwi wnetrzne sa otwarte");
			}
		} else {
			if (internalDoor.get() != DoorState.OPENED) {
				criticalError(cargo + " jest wewnatrz, ale drzwi wnetrzne sa zamkniete");
			}
			if (externalDoor.get() != DoorState.CLOSED) {
				criticalError(cargo + " jest wewnatrz, ale to drzwi zewnetrzne sa otwarte");
			}
		}

		if (cargo.getSize() > size) {
			criticalError("Zlecono umieszczenie w sluzie " + cargo + ", ale sluza jest zbyt mala");
		}

		if (cargoTransfered2Test.get()) {
			log("Sluza wczesniej dodala dodatkowy priorytetowy ladunek - sprawdzamy, czy to on teraz dotarl");
			if (cargo != cargo2add) {
				error("BLAD: system nie respektuje prirytetow. Do sluzy powinna dotrzec inna paczka");
				error("Oczekiwano: " + cargo2add + ", a dotarl " + cargo);
			} else {
				log("OK: Dotarl priorytetowy ladunek");
				PMO_SystemOutRedirect.println("OK: Dotarl priorytetowy ladunek");
			}
			cargoTransfered2Test.set(false); // kolejne sprawdzenie nie ma sensu
		}

		cargoTransferHistory.add(cargo);

		delayObject2queue(() -> {
			if (cargoSpaceState.get() != CargoSpaceState.EMPTY) {
				criticalError("Zlecono umieszczenie w sluzie " + cargo + ", ale sluza jest juz zajeta");
			}
			checkIfAlreadyTransfered(cargo);

			cargoSpace.set(cargo);
		}, AirlockInterface.Event.CARGO_INSIDE, PMO_Consts.CARGO_MOVE_DELAY);
	}

	private void checkIfAlreadyTransfered(CargoInterface cargo) {
		if (transferedCargoHolder == null)
			return;
		if (transferedCargoHolder.contains(cargo)) {
			criticalError(cargo + " juz wczesniej zostal przeslany");
		}
	}

	@Override
	public void ejectCargo() {
		log("Zlecono ejectCargo.");
		setAirlockInUse();

		CargoInterface cargo = cargoSpace.get();
		if (cargo == null) {
			error("WARINIG: Zlecono ejectCargo, ale w sluzie nie ma niczego...");
			delayObject2queue(null, AirlockInterface.Event.AIRLOCK_EMPTY, PMO_Consts.CARGO_MOVE_DELAY_SHORT);
		} else {
			log("Zlecono ejectCargo. W sluzie jest " + cargo);
		}

		boolean ok = false;
		if (cargo.getDirection() == Direction.INSIDE) {
			if (internalDoor.get() == DoorState.OPENED) {
				ok = true;
			}
		} else {
			if (externalDoor.get() == DoorState.OPENED) {
				ok = true;
			}
		}

		if (ok) {
			log(cargo + " jest przekazywany w dobrym kierunku");
			delayObject2queue(() -> {
				addCargoToRegister(cargo);
				int counter = correctTransfersCounter.incrementAndGet();
				log(cargo + " opuszcza sluze, jest to " + counter + " transfer cargo");

			}, AirlockInterface.Event.AIRLOCK_EMPTY, PMO_Consts.CARGO_MOVE_DELAY);
		} else {
			log("WARNING: " + cargo + " przekazywany jest w zlym kierunku");
			delayObject2queue(() -> {
				log(cargo + " opuszcza sluze, ale porusza sie w zla strone");
			}, AirlockInterface.Event.AIRLOCK_EMPTY, PMO_Consts.CARGO_MOVE_DELAY);
		}

	}

	private void addCargo2MoonBaseSystem() {
		if (cargo2add != null) {
			if (!cargoTransfered.get()) {
				// ladunku jeszcze nie nadano
				CyclicBarrier cb = new CyclicBarrier(2);

				PMO_ThreadsHelper.startThread(() -> {
					base.cargoTransfer(cargo2add, cargo2add.getDirection());
					log("Do systemu dodano dodatkowa paczke " + cargo2add);
					cargoTransfered.set(true);
					// ladunek przekazany - test moze byc wykonany
					cargoTransfered2Test.set(true);
					PMO_ThreadsHelper.wait(cb);
				});
				PMO_ThreadsHelper.wait(cb);
			}
		}
	}

	private void addCargoToRegister(CargoInterface cargo) {
		assert transferedCargoHolder != null : "zbior przechowujacy przekazane ladunki nie zostal ustawiony";
		if (transferedCargoHolder == null)
			return;
		transferedCargoHolder.add(cargo);
	}

	@Override
	public int getSize() {
		return size;
	}

	@Override
	public void setEventsListener(EventsListenerInterface eventsListener) {
		evlis = eventsListener;
	}
}
