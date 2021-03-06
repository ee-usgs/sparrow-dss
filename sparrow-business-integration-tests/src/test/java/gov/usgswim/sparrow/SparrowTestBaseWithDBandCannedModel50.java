package gov.usgswim.sparrow;

/**
 * Sets up a db connection to the test db, but allows all model 50 predict data
 * to be loaded from the canned files.
 * 
 * 
 * @author eeverman
 *
 */
public abstract class SparrowTestBaseWithDBandCannedModel50 extends SparrowTestBaseWithDB {

	
	public SparrowTestBaseWithDBandCannedModel50() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * Override to for file loading for predict data.
	 */
	@Override
	public boolean loadModelDataFromFile() {
		return true;
	}

}
