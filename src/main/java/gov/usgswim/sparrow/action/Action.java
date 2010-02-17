package gov.usgswim.sparrow.action;

import gov.usgswim.sparrow.service.SharedApplication;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

/**
 * An Action is intended to be a single use, non-threadsafe instance that is
 * used to run a calculation, build, and or load data.
 * 
 * The minimum to implement is to override the abstract doAction() method.
 * State is handled by the implementing subclass, so you will likely need some
 * setState() methods.
 * 
 * If logging for this class is enables, auto timing will take place, along with
 * a unique run number used in the logs so its possible to match up the
 * start and end messages in the logging by the number.
 * 
 * @author eeverman
 *
 * @param <R>
 */
public abstract class Action<R extends Object> {
	protected static Logger log =
		Logger.getLogger(Action.class); //logging for this class
	
	/**
	 * A shared run count that increments when a new instance is created.
	 */
	private static int staticRunCount = 0;
	
	/**
	 * The run number of the current instance, based on staticRunCount.
	 */
	private int runNumber = 0;
	
	/** A connection that will be closed when the action completes */
	private Connection conn = null;
	
	private long startTime;		//Time the action starts
	
	public Action() {
		staticRunCount++;
		runNumber = staticRunCount;
	}
	
	/**
	 * Generic public run method, invokes pre, do, and post action.
	 * 
	 * Actions may decide to return null to indicate a fail, or may choose
	 * to throw an error.
	 * 
	 * @return R as specified by the subclass.
	 * @throws Exception If the action throws an exception, post is still called.
	 */
	public R run() throws Exception {
		preAction();
		
		R r = null;
		Exception e = null;
		
		try {
			r = doAction();
		} catch (Exception ee) {
			e = ee;
		}
		postAction(r != null, e);
		return r;
	}
	
	protected void preAction() {
		
		if (log.isDebugEnabled()) {
			startTime = System.currentTimeMillis();
			log.debug("Beginning action for " + this.getClass().getName() +
					".  Run Number: " + runNumber);
		}
	}
	
	protected abstract R doAction() throws Exception;
	
	/**
	 * An optional message to add to the log on completion.  Only called
	 * if the log level is debug or greater, or the doAction method returned null
	 * (a fail) or threw an exception.  Note that the runNumber is
	 * auto added to the end of the message.
	 * 
	 * @return
	 */
	protected String getPostMessage() {
		return null;
	}
	
	protected void postAction(boolean success, Exception error) {
		if (log.isDebugEnabled()) {
			long totalTime = System.currentTimeMillis() - startTime;
			
			String msg = getPostMessage();
			if (msg != null) {
				log.debug(msg + "  (Run Number: " + runNumber + ")");
			}
			
			if (success && error == null) {
				log.debug("Action completed for " + this.getClass().getName() +
						".  Total Time: " + totalTime + "ms, Run Number: " +
						runNumber);
			} else {
				if (error != null) {
					log.error("Action FAILED w/ an exception for " +
							this.getClass().getName() + ".  Total Time: " +
							totalTime + "ms, Run Number: " +
							runNumber, error);
				} else {
					log.debug("Action FAILED, returning null, for " +
							this.getClass().getName() + ".  Total Time: " +
							totalTime + "ms, Run Number: " +
							runNumber);
				}
			}
			
		}
		
		//Close the connection, if not null
		if (conn != null) {
			try {
				if (! conn.isClosed()) {
					try {
						conn.close();
					} catch (Exception ee) {
						//ignore
						log.error("Unable to close a connection", ee);
					}
				}
			} catch (SQLException e) {
				//ignore
				log.error("Good grief, I just tried to find out if the connection was open or closed...", e);
			} finally {
				conn = null;
			}
		}
	}
	
	protected Connection getConnection() throws SQLException {
		if (conn == null) {
			conn = SharedApplication.getInstance().getConnection();
		}
		
		return conn;
	}
	
	/**
	 * Taken From:
	 * Properly closing Connections and ResultSets without throwing exceptions, see the section
	 * "Here is an example of properly written code to use a db connection obtained from a connection pool"
	 * http://tomcat.apache.org/tomcat-6.0-doc/jndi-datasource-examples-howto.html
	 * @param conn
	 */
	protected static void close(Connection conn) {
		if (conn != null) {
			try {
				conn.close();
			} catch (SQLException e) {
				log.error("Unable to close a jdbc connection", e);
			}
			conn = null;
		}
	}
	
	/**
	 * Taken From:
	 * Properly closing Connections and ResultSets without throwing exceptions, see the section
	 * "Here is an example of properly written code to use a db connection obtained from a connection pool"
	 * http://tomcat.apache.org/tomcat-6.0-doc/jndi-datasource-examples-howto.html
	 * 
	 * Note that this method makes no attempt to close the parent connection
	 * or statement.
	 * @param statement
	 */
	protected static void close(Statement statement) {
		if (statement != null) {
			try {
				statement.close();
			} catch (SQLException e) {
				log.error("Unable to close a jdbc statement", e);
			}
			statement = null;
		}
	}
	
	/**
	 * Taken From:
	 * Properly closing Connections and ResultSets without throwing exceptions, see the section
	 * "Here is an example of properly written code to use a db connection obtained from a connection pool"
	 * http://tomcat.apache.org/tomcat-6.0-doc/jndi-datasource-examples-howto.html
	 * 
	 * Note that this method makes no attempt to close the parent connection
	 * or statement.
	 * @param rset
	 */
	protected static void close(ResultSet rset) {
		if (rset != null) {
			try {
				rset.close();
			} catch (SQLException e) {
				log.error("Unable to close a jdbc resultset", e);
			}
			rset = null;
		}
	}
	
	
	/**
	 * Loads the named text chunk from the properties file and inserts the named values passed in params.
	 *
	 * This method assumes that the properties are contained in properties file
	 * in the same package and with the same name as the class.  E.g: pkg.MyClass
	 * should have a properties file as: pkg/MyClass.properties.
	 * <br><br>
	 * params are passed in serial pairs as {"name1", "value1", "name2", "value2"}.
	 * toString is called on each item, so it is OK to pass in autobox numerics.
	 * See the DataLoader.properties file for the names of the parameters available
	 * for the requested query.
	 *
	 * @param name	Name of the query in the properties file
	 * @param params	An array of name and value objects to replace in the query.
	 * @return
	 * @throws IOException
	 */
	public String getTextWithParamSubstitution(String name, Object... params)
			throws IOException {
		return getTextWithParamSubstitution(name, this.getClass(), params);
	}

	/**
	 * Loads the named text chunk from the properties file in the same package
	 * and with the same name as the class. E.g: pkg.MyClass should have a
	 * properties file as: pkg/MyClass.properties.
	 * <br><br>
	 * params are passed in serial pairs as {"name1", "value1", "name2",
	 * "value2"}. toString is called on each item, so it is OK to pass in
	 * autobox numerics. See the DataLoader.properties file for the names of the
	 * parameters available for the requested query.
	 *
	 * @param name
	 *            Name of the query in the properties file
	 * @param clazz
	 * @param params
	 *            An array of name and value objects to replace in the query.
	 * @return
	 * @throws IOException
	 *
	 * TODO move this to a utils class of some sort
	 */
	public static String getTextWithParamSubstitution(String name,
			Class<?> clazz, Object... params) throws IOException {
		
		String query = getText(name, clazz);

		for (int i=0; i<params.length; i+=2) {
			String n = "$" + params[i].toString() + "$";
			String v = params[i+1].toString();

			query = StringUtils.replace(query, n, v);
		}

		return query;
	}

	/**
	 * Loads the named text chunk from the properties file.
	 *
	 * This method assumes that the properties are contained in properties file
	 * in the same package and with the same name as the class.  E.g: pkg.MyClass
	 * should have a properties file as: pkg/MyClass.properties.
	 *
	 * @param name
	 * @return
	 * @throws IOException
	 * @
	 * TODO move this to a utils class of some sort
	 */
	public String getText(String name) throws IOException {
		return getText(name, this.getClass());
	}

	/**
	 * Loads the named text chunk from a properties file located in the same
	 * package and with the same name as the {@code}clazz{@code} parameter
	 *
	 * @param name of text chunk
	 * @param clazz
	 * @return
	 * @throws IOException
	 */
	public static String getText(String name, Class<?> clazz) throws IOException {
		Properties props = new Properties();

		String path = clazz.getName().replace('.', '/') + ".properties";
		props.load(Thread.currentThread().getContextClassLoader().getResourceAsStream(path));

		return props.getProperty(name);
	}
}
