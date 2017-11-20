import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class PMO_TestA implements PMO_TestInterface {

	protected CyclicBarrier barrier;
	protected List<PMO_Cargo> cargos = new ArrayList<>();
	protected List<PMO_CargoProvider> providers = new ArrayList<>();
	protected PMO_CargoHolder cargoHolder = new PMO_CargoHolder();
	protected int NUMBER_OF_AIRLOCKS = 20;
	protected int CARGOS_TO_AIRLOCKS = 4;
	protected int NUMBER_OF_CARGOS = CARGOS_TO_AIRLOCKS * NUMBER_OF_AIRLOCKS;
	protected MoonBaseInterface base;
	protected PMO_TestThreadsKeeper threadsKeeper;
	protected List<Thread> providersThreads;
	protected List<PMO_Airlock> airlocks = new ArrayList<>();
	// liczba poprawnie przekazanych cargo
	protected AtomicInteger transfersCounter = new AtomicInteger(0);
	protected AtomicBoolean runCompleted = new AtomicBoolean(false);

	{
		base = (MoonBaseInterface) PMO_GeneralPurposeFabric.fabric("MoonBase", "MoonBaseInterface");
	}

	public PMO_TestA( PMO_TestThreadsKeeper keeper ) {
		PMO_Log.log("Utworzono nowy obiekt testu");
		setThreadsKeeper(keeper);
	}
	
	public void setThreadsKeeper(PMO_TestThreadsKeeper keeper) {
		this.threadsKeeper = keeper;
	}

	protected void createCargo(int size, Direction direction) {
		cargos.add(cargoHolder.get(size, direction));
	}

	protected void createCargos() {
		for (int i = 0; i < NUMBER_OF_CARGOS / 2; i++) {
			createCargo(1, Direction.INSIDE);
			createCargo(1, Direction.OUTSIDE);
		}
		Collections.shuffle(cargos);
	}

	protected void createAirlock(String name, int size) {
		airlocks.add(new PMO_Airlock(name, size));
	}

	protected void createAirlocks() {
		for (int i = 0; i < NUMBER_OF_AIRLOCKS; i++) {
			createAirlock(String.format("airlock-%03d", i), 2);
		}
	}

	protected void createProviders() {
		cargos.forEach(c -> providers.add(new PMO_CargoProvider(base, c, barrier)));
	}

	protected void prepareProviders() {
		providers.forEach(p -> p.setThreadsKeeper(threadsKeeper));
	}

	protected void prepareAirlocks() {
		airlocks.forEach(p -> {
			p.setThreadsKeeper(threadsKeeper);
			p.setCorrectTransfersCounter(transfersCounter);
			p.setTransferedCargoHolder(cargoHolder.getTransferesHolder());
		});
	}

	protected void addAirlocksToMoonBase() {
		try {
			base.setAirlocksConfiguration(new ArrayList<>(airlocks));
		} catch (Exception e) {
			PMO_CommonErrorLog
					.error("W trakcie pracy metody setAirlocksConfiguration pojawil sie wyjatek " + e.toString());
		}
	}

	protected void startProviders() {
		providersThreads = PMO_ThreadsHelper.createAndStartThreads(providers, true);
	}

	protected void runCompleted() {
		runCompleted.set(true);
	}
	
	@Override
	public void run() {
		threadsKeeper.addThread();
		createAirlocks();
		addAirlocksToMoonBase();

		createCargos();
		createProviders();

		prepareProviders();
		prepareAirlocks();

		startProviders();

		threadsKeeper.removeThread();
		runCompleted();
	}

	@Override
	public long requiredTime() {
		return (CARGOS_TO_AIRLOCKS + 1) * PMO_Consts.AIRLOCK_WORK_CYCLE;
	}

	@Override
	public boolean isOK() {
		
		if ( runCompleted.get() ) {
			PMO_SystemOutRedirect.println( "Metoda run() test zakonczyla prace");
		} else {
			PMO_CommonErrorLog.error( "BLAD: Test nie ma sensu - metoda run() nie zakonczyla pracy");
			return false;
		}
		
		// czy providers dostarczyli przesylki?

		boolean result = PMO_TestHelper.runTests(providers);

		if (result) {
			PMO_Log.log("OK - cargo zostalo prawidlowo przekazane przez cargoTransfer");
			PMO_SystemOutRedirect.println("OK - cargo zostalo prawidlowo przekazane przez cargoTransfer");
		} else {
			PMO_CommonErrorLog.error("BLAD - blad na poziomie przekazywania ladunkow przez cargoTransfer");
			PMO_CommonErrorLog.error("BLAD - dalszy test nie ma sensu");
			return false;
		}

		result &= PMO_TestHelper.runTests(airlocks);
		
		if (result) {
			PMO_Log.log("OK - sluzy nie wykryly bledu uzycia");
			PMO_SystemOutRedirect.println("OK - sluzy nie wykryly bledu uzycia");
		} else {
			PMO_CommonErrorLog.error("BLAD uzycia sluzy");
		}
		
		int transfers = transfersCounter.get();

		if (transfers == cargoHolder.getNumberOfCreatedCargos() ) {
			PMO_Log.log("OK - dostarczona zostala prawidlowa liczba przesylek");
			PMO_SystemOutRedirect.println("OK - dostarczona zostala prawidlowa liczba przesylek");
		} else {
			PMO_CommonErrorLog.error("BLAD - dostarczono inna liczbe przesylek niz wyslano");
			PMO_CommonErrorLog.error("BLAD - system mial przekazac       " + NUMBER_OF_CARGOS + " przesylek");
			PMO_CommonErrorLog.error("BLAD - system przeslal przez sluzy " + transfers + " przesylek");
			return false;
		}

		// czy towar dotarl na miejsce
		result &= cargoHolder.isOK();

		return result;
	}

}
