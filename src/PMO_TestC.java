import java.util.stream.IntStream;

public class PMO_TestC extends PMO_TestA {
	public PMO_TestC(PMO_TestThreadsKeeper keeper) {
		super(keeper);
	}

	// airlock maja rozne rozmiary
	@Override
	protected void createAirlocks() {
		IntStream.range(2, 2 + NUMBER_OF_AIRLOCKS).forEach(i -> createAirlock(String.format("airlock-%03d", i - 2), i));
	}
	
	@Override
	protected void prepareAirlocks() {
		super.prepareAirlocks();
		// tworzymy dodatkowe cargo, ktore ma pasowac do konkretnej sluzy
		airlocks.forEach(a -> { 
			a.setCargo2Add(cargoHolder.get(a.getSize(), Direction.INSIDE));
			a.setMoonBaseInterface(base);
		} );
	}

	@Override
	public long requiredTime() {
		return super.requiredTime() + 2 * PMO_Consts.AIRLOCK_WORK_CYCLE;
	}

}
