package gov.usgswim.sparrow.action;

import com.mockrunner.mock.jdbc.*;
import com.mockrunner.jdbc.*;
import static org.junit.Assert.*;

import gov.usgswim.sparrow.domain.DataSeriesType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import static junit.framework.Assert.assertEquals;
import org.junit.Before;

import org.junit.Test;

/**
 * Note that the JDBCTestCaseAdapter extends JUnit Test, so must follow old
 * JUnit test naming conventions.
 * @author eeverman
 */
public class ActionUnitTest extends JDBCTestCaseAdapter {
	MockConnection connection;
	
	public void setUp() {
		connection =
				getJDBCMockObjectFactory().getMockConnection();
	}
	

	/**
	 * IN clause type structures are created by passing in a collection as one of the
	 * params, resulting in one '?' per collection entry.
	 * @throws Exception 
	 */
	public void testSqlWithSimpleINClause() throws Exception {
		String testSqlSrc = "SELECT * FROM TABLE WHERE ID IN ($mylist$)";
		String expectSql = "SELECT * FROM TABLE WHERE ID IN (?, ?)";
		
		Map<String, Object> params = new HashMap<String, Object>();
		ArrayList<String> myList = new ArrayList<String>();
		myList.add("val1");
		myList.add("val2");
		params.put("mylist", myList);

		Action.SQLString result = Action.processSql(testSqlSrc, params);
		
		assertEquals(expectSql, result.sql.toString());
		assertEquals(1, result.variables.size());
		assertEquals("mylist", result.variables.get(0));
		
		MockPreparedStatement prepSatement = (MockPreparedStatement) connection.prepareStatement(result.sql.toString());
		
		Action.assignParameters(prepSatement, result, params);
		
		assertEquals("val1", prepSatement.getParameter(1).toString());
		assertEquals("val2", prepSatement.getParameter(2).toString());
		assertEquals(2, prepSatement.getParameterMap().size());
	}
	
	public void testSqlWithMultipleINClauses() throws Exception {
		String testSqlSrc = "SELECT * FROM TABLE WHERE ID IN ($myIds$) AND Name IN ($myNames$) AND Other = $other$";
		String expectSql =  "SELECT * FROM TABLE WHERE ID IN (?, ?) AND Name IN (?) AND Other = ?";
		
		Map<String, Object> params = new HashMap<String, Object>();
		ArrayList<String> myIds = new ArrayList<String>();
		myIds.add("val1");
		myIds.add("val2");
		params.put("myIds", myIds);
		
		HashSet<String> myNames = new HashSet<String>();	//Collection type shouldn't matter
		myNames.add("name1");
		params.put("myNames", myNames);
		params.put("myNames", myNames);
		
		params.put("other", "else");

		Action.SQLString result = Action.processSql(testSqlSrc, params);
		
		assertEquals(expectSql, result.sql.toString());
		assertEquals(3, result.variables.size());
		assertEquals("myIds", result.variables.get(0));
		assertEquals("myNames", result.variables.get(1));
		assertEquals("other", result.variables.get(2));
		
		MockPreparedStatement prepSatement = (MockPreparedStatement) connection.prepareStatement(result.sql.toString());
		
		Action.assignParameters(prepSatement, result, params);
		
		assertEquals("val1", prepSatement.getParameter(1).toString());
		assertEquals("val2", prepSatement.getParameter(2).toString());
		assertEquals("name1", prepSatement.getParameter(3).toString());
		assertEquals("else", prepSatement.getParameter(4).toString());
		assertEquals(4, prepSatement.getParameterMap().size());
	}
	
	
	public void testProcessSqlWithVanillaParams() {
		
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("SqlParam1", "SqlParamVal1");
		params.put("SqlParam2", "SqlParamVal2");
		params.put("NonSqlParam1", "NonSqlVal1");
		
		String testCase = "blah @NonSqlParam1@ xx@NonSqlParam1@xx aehawo $SqlParam1$ lhwoew whea fown $SqlParam2$ wlheown weoah $SqlParam3$";
		String expected = "blah NonSqlVal1 xxNonSqlVal1xx aehawo ? lhwoew whea fown ? wlheown weoah ?";
		Action.SQLString result = Action.processSql(testCase, params);
		
		assertEquals(expected, result.sql.toString());
		
		ArrayList<String> expectedVariables = new ArrayList<String>();
		expectedVariables.add("SqlParam1");
		expectedVariables.add("SqlParam2");
		expectedVariables.add("SqlParam3");
		for (int i = 0; i < result.variables.size(); i++) {
			assertEquals(expectedVariables.get(i), result.variables.get(i));
		}
	}
	

	public void testProcessSqlWithNulls() {
		
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("SqlParam1", "SqlParamVal1");
		params.put("SqlParam2", "SqlParamVal2");
		params.put("NonSqlParam1", null);
		params.put("DoesNotExist", null);
		
		String testCase = "blah @NonSqlParam1@ xx@NonSqlParam1@xx aehawo $SqlParam1$ lhwoew whea fown $SqlParam2$ wlheown weoah $SqlParam3$";
		String expected = "blah  xxxx aehawo ? lhwoew whea fown ? wlheown weoah ?";
		Action.SQLString result = Action.processSql(testCase, params);
		
		assertEquals(expected, result.sql.toString());
		
		ArrayList<String> expectedVariables = new ArrayList<String>();
		expectedVariables.add("SqlParam1");
		expectedVariables.add("SqlParam2");
		expectedVariables.add("SqlParam3");
		for (int i = 0; i < result.variables.size(); i++) {
			assertEquals(expectedVariables.get(i), result.variables.get(i));
		}
	}
	
	public void testCheckDataSeriesTypeProperties() throws Exception {
		Action act = new CalcConcentration();
		
		assertEquals("Total Load",
				act.getDataSeriesProperty(DataSeriesType.total.name(), false, "Not Found"));
		assertEquals("Total Load",
				act.getDataSeriesProperty(DataSeriesType.total, false, "Not Found"));
		assertEquals("Not Found",
				act.getDataSeriesProperty("does_not_exist", false, "Not Found"));
	}
	
	public void testSplitListIntoSubLists_10_2() {
		List<Long> sourceIds = new ArrayList<Long>();
		List<List<Long>> destIds = new ArrayList<List<Long>>();
		
		for (long ids = 0; ids < 10; ids++) {
			sourceIds.add(ids);
		}
		
		ActionWrapper action = new ActionWrapper();
		action.testSplitListIntoSubLists(destIds, sourceIds, 2);
		
		assertEquals(5, destIds.size());
		assertEquals(2, destIds.get(0).size());
		assertEquals(new Long(0), destIds.get(0).get(0));
		assertEquals(new Long(1), destIds.get(0).get(1));
		assertEquals(2, destIds.get(1).size());
		assertEquals(2, destIds.get(2).size());
		assertEquals(2, destIds.get(3).size());
		assertEquals(2, destIds.get(4).size());
		assertEquals(new Long(8), destIds.get(4).get(0));
		assertEquals(new Long(9), destIds.get(4).get(1));
	}
	
	public void testSplitListIntoSubLists_10_3() {
		List<Long> sourceIds = new ArrayList<Long>();
		List<List<Long>> destIds = new ArrayList<List<Long>>();
		
		for (long ids = 0; ids < 10; ids++) {
			sourceIds.add(ids);
		}
		
		ActionWrapper action = new ActionWrapper();
		action.testSplitListIntoSubLists(destIds, sourceIds, 3);
		
		assertEquals(4, destIds.size());
		assertEquals(3, destIds.get(0).size());
		assertEquals(new Long(0), destIds.get(0).get(0));
		assertEquals(new Long(1), destIds.get(0).get(1));
		assertEquals(new Long(2), destIds.get(0).get(2));
		assertEquals(3, destIds.get(1).size());
		assertEquals(3, destIds.get(2).size());
		assertEquals(1, destIds.get(3).size());
		assertEquals(new Long(9), destIds.get(3).get(0));
	}
	
	public void testSplitListIntoSubLists_10_4() {
		List<Long> sourceIds = new ArrayList<Long>();
		List<List<Long>> destIds = new ArrayList<List<Long>>();
		
		for (long ids = 0; ids < 10; ids++) {
			sourceIds.add(ids);
		}
		
		ActionWrapper action = new ActionWrapper();
		action.testSplitListIntoSubLists(destIds, sourceIds, 4);
		
		assertEquals(3, destIds.size());
		assertEquals(4, destIds.get(0).size());
		assertEquals(new Long(0), destIds.get(0).get(0));
		assertEquals(new Long(1), destIds.get(0).get(1));
		assertEquals(new Long(2), destIds.get(0).get(2));
		assertEquals(new Long(3), destIds.get(0).get(3));
		assertEquals(4, destIds.get(1).size());
		assertEquals(2, destIds.get(2).size());
		assertEquals(new Long(8), destIds.get(2).get(0));
		assertEquals(new Long(9), destIds.get(2).get(1));
	}
	
	public void testSplitListIntoSubLists_10_5() {
		List<Long> sourceIds = new ArrayList<Long>();
		List<List<Long>> destIds = new ArrayList<List<Long>>();
		
		for (long ids = 0; ids < 10; ids++) {
			sourceIds.add(ids);
		}
		
		ActionWrapper action = new ActionWrapper();
		action.testSplitListIntoSubLists(destIds, sourceIds, 5);
		
		assertEquals(2, destIds.size());
		assertEquals(5, destIds.get(0).size());
		assertEquals(new Long(0), destIds.get(0).get(0));
		assertEquals(new Long(1), destIds.get(0).get(1));
		assertEquals(new Long(2), destIds.get(0).get(2));
		assertEquals(new Long(3), destIds.get(0).get(3));
		assertEquals(new Long(4), destIds.get(0).get(4));
		assertEquals(5, destIds.get(1).size());
		assertEquals(new Long(5), destIds.get(1).get(0));
		assertEquals(new Long(9), destIds.get(1).get(4));
	}
	
	public void testSplitListIntoSubLists_11_5() {
		List<Long> sourceIds = new ArrayList<Long>();
		List<List<Long>> destIds = new ArrayList<List<Long>>();
		
		for (long ids = 0; ids < 11; ids++) {
			sourceIds.add(ids);
		}
		
		ActionWrapper action = new ActionWrapper();
		action.testSplitListIntoSubLists(destIds, sourceIds, 5);
		
		assertEquals(3, destIds.size());
		assertEquals(5, destIds.get(0).size());
		assertEquals(new Long(0), destIds.get(0).get(0));
		assertEquals(new Long(1), destIds.get(0).get(1));
		assertEquals(new Long(2), destIds.get(0).get(2));
		assertEquals(new Long(3), destIds.get(0).get(3));
		assertEquals(new Long(4), destIds.get(0).get(4));
		assertEquals(5, destIds.get(1).size());
		assertEquals(new Long(5), destIds.get(1).get(0));
		assertEquals(new Long(9), destIds.get(1).get(4));
		assertEquals(1, destIds.get(2).size());
		assertEquals(new Long(10), destIds.get(2).get(0));
	}
	
	public static class ActionWrapper extends Action {

		@Override
		public Object doAction() throws Exception {
			throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
		}
		
		public static <T> void testSplitListIntoSubLists(List<List<T>> destinationListOfLists,
			List<T> sourceList, int itemCountInSublists) {
			
			splitListIntoSubLists(destinationListOfLists, sourceList, itemCountInSublists);
		}
		
	}
}
