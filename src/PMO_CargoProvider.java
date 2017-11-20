import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicBoolean;

public class PMO_CargoProvider implements PMO_RunnableAndTestable {

	private final PMO_Cargo cargo2send;
	private final AtomicBoolean cargoProvided = new AtomicBoolean(false);
	private final MoonBaseInterface base;
	private CyclicBarrier barrier;
	// pomiar czasu wykonania dodania cargo
	private long cargoTransferExecutionTime;
	// lista wlasnych watkow
	private PMO_TestThreadsKeeper threadsKeeper;
	// blokada komunikacji
	private AtomicBoolean blockCommunication;

	public PMO_CargoProvider(MoonBaseInterface base, PMO_Cargo cargo2send, CyclicBarrier barrier) {
		this.cargo2send = cargo2send;
		this.base = base;
		this.barrier = barrier;
	}

	public void setBlockCommunicationFlag(AtomicBoolean blockCommunication) {
		this.blockCommunication = blockCommunication;
	}

	public void setThreadsKeeper(PMO_TestThreadsKeeper threadsKeeper) {
		this.threadsKeeper = threadsKeeper;
	}

	public void blockCommunicationIfNecessary() {
		if (blockCommunication == null)
			return;
		if (blockCommunication.get()) {
			PMO_ThreadsHelper.wait(blockCommunication);
		}
	}

	@Override
	public void run() {
		try {
			threadsKeeper.addThread();
			// czekamy na przygotowanie innych watkow
			PMO_ThreadsHelper.wait(barrier);

			blockCommunicationIfNecessary();

			PMO_Log.log("Za chwile wykonane zostanie wywolanie cargoTransfer z " + cargo2send);
			cargoTransferExecutionTime = PMO_TimeHelper.executionTimeOrException(() -> {
				base.cargoTransfer(cargo2send, cargo2send.getDirection());
			});
			PMO_Log.log("cargoTransfer " + cargo2send + " zakonczono z czasem " + cargoTransferExecutionTime);
			cargoProvided.set(true);
		} catch (Exception e) {
			PMO_CommonErrorLog.error("W trakcie pracy cargoTransfer pojawil sie wyjatek " + e.toString());
		} finally {
			threadsKeeper.removeThread();
		}
	}

	@Override
	public boolean isOK() {
		if (!cargoProvided.get()) {
			PMO_CommonErrorLog.error("BLAD - metoda cargoTransfer nie odebrala ladunku " + cargo2send);
			return false;
		} else {
			PMO_Log.log("OK - metoda cargoTransfer zostala wykonana - test czasu pracy ma sens");
		}

		if (cargoTransferExecutionTime > PMO_Consts.CARG_TRANSFER_EXECUTION_TIME_LIMIT) {
			PMO_CommonErrorLog.error("BLAD - przekroczono limit czasu na wykonanie metody cargoTransfer");
			PMO_CommonErrorLog.error("BLAD - limit na cargoTransfer to " + PMO_Consts.CARG_TRANSFER_EXECUTION_TIME_LIMIT
					+ "msec, a zmierzono " + cargoTransferExecutionTime);
			return false;
		}
		return true;
	}
}
