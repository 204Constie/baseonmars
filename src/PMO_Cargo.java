public class PMO_Cargo implements CargoInterface {

	private final int size;
	private final Direction direction;
	private final int id;
	private static int counter;
	private final String description;

	@Override
	public int getSize() {
		return size;
	}

	@Override
	public Direction getDirection() {
		return direction;
	}

	public PMO_Cargo(int size, Direction direction) {
		this.size = size;
		this.direction = direction;
		synchronized (PMO_Cargo.class) {
			this.id = ++counter;
		}
		description = "Ladunek ID:" + id + " rozmiar " + size + " kierunek " + direction;

		PMO_Log.log("Utworzony zostal ladunek " + description);
	}

	@Override
	public String toString() {
		return description;
	}

	@Override
	public int getID() {
		return id;
	}
}
