import java.util.ArrayList;
import java.util.Collection;

public class PMO_Consts {
	public static final long DOOR_OPENING_DELAY = 300;
	public static final long DOOR_OPENING_DELAY_SHORT = 150;
	public static final long DOOR_CLOSING_DELAY = 300;
	public static final long DOOR_CLOSING_DELAY_SHORT = 150;
	public static final long CARGO_MOVE_DELAY = 200;
	public static final long CARGO_MOVE_DELAY_SHORT = 50;

	public static final long CARG_TRANSFER_EXECUTION_TIME_LIMIT = 5000;

	public static final long AIRLOCK_WORK_CYCLE = 2 * (DOOR_OPENING_DELAY + DOOR_CLOSING_DELAY + CARGO_MOVE_DELAY);

	public static final Collection<String> testClasses = new ArrayList<String>() {
		/**
		* 
		*/
		private static final long serialVersionUID = 5548751489907382940L;

		{

			add("PMO_Airlock");
		}
	};
}
