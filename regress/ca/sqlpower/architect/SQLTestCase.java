package regress.ca.sqlpower.architect;

import java.io.File;
import java.io.IOException;

import junit.framework.*;
import ca.sqlpower.sql.*;

import ca.sqlpower.architect.*;
import ca.sqlpower.architect.swingui.ArchitectFrame;

/**
 * SQLTestCase is an abstract base class for test cases that require a
 * database connection.
 */
public abstract class SQLTestCase extends TestCase {

	/**
	 * This is the SQLDatabase object.  It will be set up according to
	 * some system properties in the <code>setup()</code> method.
	 *
	 * @see #setup()
	 */
	SQLDatabase db;

	public SQLTestCase(String name) throws Exception {
		super(name);
	}
	
	/**
	 * Looks up and returns an ArchitectDataSource that represents the testing
	 * database. Uses a PL.INI file located in the current working directory, 
	 * called "pl.regression.ini" and creates a connection to the database called
	 * "regression_test".
	 * 
	 * <p>FIXME: Need to parameterise this so that we can test each supported
	 * database platform!
	 */
	static ArchitectDataSource getDataSource() throws IOException {
		ArchitectFrame.getMainInstance();  // creates an ArchitectFrame, which loads settings
		//FIXME: a better approach would be to have an initialsation method
		// in the business model, which does not depend on the init routine in ArchitectFrame.
		PlDotIni plini = new PlDotIni();
		plini.read(new File("pl.regression.ini"));
		return plini.getDataSource("regression_test");
	}
	
	/**
	 * Sets up the instance variable <code>db</code> using the getDatabase() method.
	 */
	protected void setUp() throws Exception {
		db = new SQLDatabase(getDataSource());
	}
	
	protected void tearDown() throws Exception {
		// we don't disconnect: this slows down the test because it 
		// requires setUp() to reconnect rather than using a cached connection!
		db = null;
	}
}
