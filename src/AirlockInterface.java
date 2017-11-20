/**
 * Created by constie on 14.11.2017.
 */
/**
 * Interfejs sterowania i monitoringu sluzy.
 *
 */
public interface AirlockInterface {

    /**
     * Typ wyliczeniowy Event zawiera zdarzenia, do ktorych moze dojsc w sluzie.
     *
     */
    enum Event {
        /**
         * Wewnetrzne drzwi sluzy zostaly zamkniete
         */
        INTERNAL_AIRTIGHT_DOORS_CLOSED,
        /**
         * Wewnetrzne drzwi sluzy zostaly otwarte
         */
        INTERNAL_AIRTIGHT_DOORS_OPENED,
        /**
         * Zewnetrzne drzwi sluzy zostaly zamkniete
         */
        EXTERNAL_AIRTIGHT_DOORS_CLOSED,
        /**
         * Zewnetrzne drzwi sluzy zostaly otwarte
         */
        EXTERNAL_AIRTIGHT_DOORS_OPENED,
        /**
         * Ladunek w sluzie
         */
        CARGO_INSIDE,
        /**
         * Sluza jest pusta
         */
        AIRLOCK_EMPTY,
        /**
         * Nic dobrego...
         */
        DISASTER
    }

    /**
     * Interfejs umozliwiajacy przekazanie informacji o zdarzeniu w sluzie.
     */
    interface EventsListenerInterface {
        void newAirlockEvent(Event event);
    }

    /**
     * Zlecenie otwarcia wewnetrznych drzwi sluzy.
     */
    void openInternalAirtightDoors();

    /**
     * Zlecenie zamkniecia wewnetrznych drzwi sluzy.
     */
    void closeInternalAirtightDoors();

    /**
     * Zlecenie otwarcia zewnetrznych drzwi sluzy
     */
    void openExternalAirtightDoors();

    /**
     * Zlecenie zamkniecia zewnetrznych drzwi sluzy
     */
    void closeExternalAirtightDoors();

    /**
     * Ladunek umieszczany jest w sluzie. Aby operacja mogla byc zrealizowana jedne
     * z drzwi musza byc otwarte i rozmiar sluzy musi byc nie mniejszy niz rozmiar
     * ladunku. Zlecenie umieszczenia w zamknietej lub zbyt malej sluzie ladunku
     * powoduje zniszczenie i ladunku i sluzy.
     *
     * @param cargo
     *            ladunek umieszczany w sluzie
     */
    void insertCargo(CargoInterface cargo);

    /**
     * Ladunek usuwany jest ze sluzy. Jedne z drzwi musza byc otwarte. Kierunek
     * ruchu zalezy od tego, ktore drzwi sa otwarte. Zlecenie operacji przy
     * zamknietych drzwiach prowadzi do zniszczenia ladunku i sluzy.
     */
    void ejectCargo();

    /**
     * Metoda zwraca rozmiar sluzy.
     * @return rozmiar sluzy
     */
    int getSize();

    /**
     * Metoda do ustawienia obiektu, ktory bedzie powiadamiany o zmianach w stanie
     * sluzy.
     *
     * @param eventsListener
     *            obiekt informowany o zmianach w stanie sluzy.
     */
    void setEventsListener(EventsListenerInterface eventsListener);
}