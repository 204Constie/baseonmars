import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Klasa tworzy ladunki (obiekty) i przechowuje ich referencje do pozniejszego
 * porwnania z lista ladunkow <b>poprawnie</b> przekazanych przez sluzy.
 * 
 * @author oramus
 *
 */
public class PMO_CargoHolder implements PMO_Testable {
	private List<PMO_Cargo> cargoHolder = Collections.synchronizedList(new ArrayList<>());
	private Set<CargoInterface> correctTransfersHolder = Collections.synchronizedSet(new HashSet<>());

	public PMO_Cargo get(int size, Direction direction) {
		PMO_Cargo cargo = new PMO_Cargo(size, direction);

		cargoHolder.add(cargo);

		return cargo;
	}

	public Set<CargoInterface> getTransferesHolder() {
		return correctTransfersHolder;
	}

	public int getNumberOfCreatedCargos() {
		return cargoHolder.size();
	}

	@Override
	public boolean isOK() {
		boolean isOK = true;
		if (cargoHolder.size() != correctTransfersHolder.size()) {
			PMO_CommonErrorLog.error("BLAD: Oczekiwano przekazania przez sluzy innej liczby ladunkow");
			PMO_CommonErrorLog
					.error("BLAD: Oczekiwano " + cargoHolder.size() + " a przeslano " + correctTransfersHolder.size());
			isOK = false;
		} else {
			PMO_Log.log("OK: Liczba przekazanych ladunkow = liczbie zlecen = " + cargoHolder.size());
		}

		Set<Integer> oryginalIDs = new TreeSet<>();
		Set<Integer> transferedIDs = new TreeSet<>();

		synchronized (cargoHolder) {
			cargoHolder.forEach(c -> oryginalIDs.add(c.getID()));
		}
		synchronized (correctTransfersHolder) {
			try {
				correctTransfersHolder.forEach(c -> transferedIDs.add(((PMO_Cargo) c).getID()));
			} catch (Exception e) {
				PMO_CommonErrorLog
						.error("W trakcie przegladania zbioru odebranych ladunkow doszlo do wyjatku " + e.toString());
				return false;
			}
		}

		if (!(oryginalIDs.containsAll(transferedIDs) && transferedIDs.containsAll(oryginalIDs))) {
			PMO_CommonErrorLog
					.error("Zbiory identyfikatorow ladunkow do przeslania i rzeczywiscie przeslanych sa rozne");
			isOK = false;
		} else {
			PMO_Log.log("OK. Potwierdzono zgodnosc identyfikatorow cargo");
		}

		return isOK;
	}

}
