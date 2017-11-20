import java.util.List;

/**
 * Interfejs bazy ksiezycowej w zakresie przesylania 
 * ladunkow przez sluzy.
 */
public interface MoonBaseInterface {
	/**
	 * Metoda przekazuje kolekcje sluz, ktore maja byc
	 * uzyte do przekazywania przez nie ladunkow z/do
	 * stacji.
	 * 
	 * @param airlocks lista sluz.
	 */
	void setAirlocksConfiguration( List<AirlockInterface> airlocks );
	
	/**
	 * Metoda umozliwiajaca przekazanie informacji o 
	 * ladunku do przeslania z/do bazy. Metoda ma dzialac w sposob
	 * nieblokujacy pracy jej uzytkownika. Metoda ma dzialac prawidlowo
	 * nawet w przypadku rownoczesnego uzycia przez wielu rownoczesnych 
	 * uzytkownikow (wiele watkow).
	 * 
	 * @param cargo ladunek do przeslania
	 * @param direction kierunek transferu ladunku z/do bazy
	 */
	void cargoTransfer( CargoInterface cargo, Direction direction );
}
