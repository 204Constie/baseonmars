/**
 * Interfejs ladunku przekazywanego przez sluzy stacji.
 */
public interface CargoInterface {
	/**
	 * Metoda zwraca rozmiar ladunku.
	 * 
	 * @return rozmiar ladunku.
	 */
	int getSize();

	/**
	 * Metoda zwraca informacje o kierunku, w ktorym ladunek ma zostac przekazany
	 * 
	 * @return kierunek transferu ladunku.
	 */
	Direction getDirection();

	default int getID() {
		return 0;
	}
}
