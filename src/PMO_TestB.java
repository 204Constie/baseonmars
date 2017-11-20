import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class PMO_TestB extends PMO_TestA {
	protected Set<Thread> suspiciousThreads;
	protected AtomicBoolean stopCommunication = new AtomicBoolean(false);

	public PMO_TestB(PMO_TestThreadsKeeper keeper) {
		super(keeper);
	}

	protected void threadsInspection() {
		PMO_Log.log("Test wykonuje threadsInspection");
		Set<Thread.State> requiredStates = new HashSet<>();
		requiredStates.add(Thread.State.TIMED_WAITING);
		requiredStates.add(Thread.State.RUNNABLE);
		suspiciousThreads = PMO_ThreadsHelper.eliminateThreadsByThread(PMO_ThreadsHelper.findThreads(requiredStates),
				threadsKeeper.getThreads());

		if (suspiciousThreads.size() > 0) {
			Set<Thread> filteredThreads = PMO_ThreadsHelper.eliminateThreadByClassName(suspiciousThreads,
					"sun.misc.Unsafe");
			if (filteredThreads.size() != suspiciousThreads.size()) {
				PMO_Log.log("Wsrod znalezionych watkow byly wykonujace sun.misc.Unsafe");
				PMO_SystemOutRedirect.println("Wsrod znalezionych watkow byly wykonujace sun.misc.Unsafe");
				suspiciousThreads = filteredThreads;
			}
		}
	}

	protected void setCommunicationBlockFlag() {
		airlocks.forEach(e -> e.setBlockCommunicationFlag(stopCommunication));
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

		PMO_TimeHelper.sleep(PMO_Consts.AIRLOCK_WORK_CYCLE);
		stopCommunication.set(true);
		// Watek testowy
		// powinien zostac wykryty i spowodowac blad testu
		// PMO_ThreadsHelper.startThread( () -> {
		// PMO_TimeHelper.sleep( 20000 );
		// });
		PMO_TimeHelper.sleep(PMO_Consts.AIRLOCK_WORK_CYCLE);
		threadsInspection();

		threadsKeeper.removeThread();
		runCompleted();
	}

	public boolean isOK() {
		try {
			if (suspiciousThreads.size() > 0) {
				PMO_CommonErrorLog.error("BLAD: znaleziono watki, ktore sa w stanie RUNNABLE lub TIMED_WAITING");
				suspiciousThreads.forEach(t -> {
					PMO_CommonErrorLog.error(PMO_ThreadsHelper.thread2String(t));
				});
				return false;
			} else {
				PMO_SystemOutRedirect.println("OK: Nie wykryto watkow, ktore sa w stanie RUNNABLE lub TIMED_WAITTING");
				PMO_Log.log("OK: Nie wykryto watkow, ktore sa w stanie RUNNABLE lub TIMED_WAITTING");
			}
			return true;
		} finally {
			// na wszelki wypadek wlaczamy ponownie komunikacje
			stopCommunication.set(false);
		}
	}
}
