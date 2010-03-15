package gov.usgswim.sparrow.action;

import static org.junit.Assert.*;
import gov.usgswim.datatable.ColumnData;
import gov.usgswim.datatable.DataTable;
import gov.usgswim.datatable.adjustment.SparseOverrideAdjustment;
import gov.usgswim.datatable.impl.SimpleDataTable;
import gov.usgswim.datatable.impl.SparseDoubleColumnData;
import gov.usgswim.datatable.impl.StandardDoubleColumnData;
import gov.usgswim.sparrow.DeliveryRunner;
import gov.usgswim.sparrow.LifecycleListener;
import gov.usgswim.sparrow.PredictData;
import gov.usgswim.sparrow.PredictDataImm;
import gov.usgswim.sparrow.SparrowDBTest;
import gov.usgswim.sparrow.TestHelper;
import gov.usgswim.sparrow.action.CalcDeliveryFraction;
import gov.usgswim.sparrow.cachefactory.PredictResultFactory;
import gov.usgswim.sparrow.datatable.DataTableCompare;
import gov.usgswim.sparrow.datatable.PredictResult;
import gov.usgswim.sparrow.parser.AdjustmentGroups;
import gov.usgswim.sparrow.parser.Analysis;
import gov.usgswim.sparrow.parser.AreaOfInterest;
import gov.usgswim.sparrow.parser.Comparison;
import gov.usgswim.sparrow.parser.DataColumn;
import gov.usgswim.sparrow.parser.PredictionContext;
import gov.usgswim.sparrow.parser.TerminalReaches;
import gov.usgswim.sparrow.service.SharedApplication;
import gov.usgswim.sparrow.util.TabDelimFileUtil;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Delivery seems to perform really slowly, so this test compares the time
 * to that of a standard analysis.
 * 
 * @author eeverman
 */
public class PerformanceTest extends SparrowDBTest {
	
	static LifecycleListener lifecycle = new LifecycleListener();
	
	static PredictionContext context;
	
	@BeforeClass
	public static void setUp() throws Exception {
		
		//Turns on detailed logging
		log.setLevel(Level.DEBUG);
		
		//Turn off logging for actions, which might affect performance
		log.getLogger(Action.class).setLevel(Level.ERROR);
		
		
		AdjustmentGroups emptyAdjustments = new AdjustmentGroups(TEST_MODEL_ID);
		context = new PredictionContext(TEST_MODEL_ID, emptyAdjustments, null,
				null, null, null);
		SharedApplication.getInstance().putPredictionContext(context);
		
	}

	
	@Test
	public void testComparison() throws Exception {

		long startTime = 0;
		long endTime = 0;
		//Number of times to loop
		final int ITERATION_COUNT = 100;
		
		
		//Force the predict data to be loaded
		startTime = System.currentTimeMillis();
		PredictData predictData = SharedApplication.getInstance().getPredictData(TEST_MODEL_ID);
		endTime = System.currentTimeMillis();
		report(endTime - startTime, "Initial Load of model data", 1);
		assertTrue("It should take less then 40 seconds to load model 50, even on a slow connection.", 
				(endTime - startTime) < 40000);
		
		
		startTime = System.currentTimeMillis();
		for (int i = 0; i < 100; i++) {
			predictData = SharedApplication.getInstance().getPredictData(TEST_MODEL_ID);
		}
		endTime = System.currentTimeMillis();
		report(endTime - startTime, "Reload model data from cache", 100);
		assertTrue("Predict data is now cached, it should be faster than 100ms for 100 'loads'.", 
				(endTime - startTime) < 50);
		
		
		PredictResultFactory prFactory = new PredictResultFactory();
		AdjustmentGroups emptyAdjustments = new AdjustmentGroups(TEST_MODEL_ID);
		PredictResult predictResults = null;
		NSDataSetBuilder nsDataSetBuilder = new NSDataSetBuilder();
		CalcDeliveryFraction delAction = new CalcDeliveryFraction();
		DataColumn dataColumn = null;
		
		//Run the prediction
		startTime = System.currentTimeMillis();
		for (int i=0; i< ITERATION_COUNT;  i++) {
			predictResults = prFactory.createEntry(emptyAdjustments);
		}
		endTime = System.currentTimeMillis();
		long predictTotalTime = endTime - startTime;
		report(predictTotalTime, "Run Prediction", ITERATION_COUNT);
		assertTrue("Run Prediction should be less than 5 seconds for 100 runs.", 
				predictTotalTime < 5000);
		
		
		//Copy the prediction results to an NSDataSet
		dataColumn =
			new DataColumn(predictResults, predictResults.getTotalCol(), context.getId());
		nsDataSetBuilder.setData(dataColumn);
		startTime = System.currentTimeMillis();
		for (int i=0; i< ITERATION_COUNT;  i++) {
			nsDataSetBuilder.run();
		}
		endTime = System.currentTimeMillis();
		long predictCopyTotalTime = endTime - startTime;
		report(predictCopyTotalTime, "Copy predict result to NSDataset", ITERATION_COUNT);
		assertTrue("Copying the data to the NSDataset should be less than 500ms for 100 iterations.", 
				predictCopyTotalTime < 500);
		
		
		//Run Delivery
		List<Long> targetList = new ArrayList<Long>();
		targetList.add(9682L);
		TerminalReaches targets = new TerminalReaches(TEST_MODEL_ID, targetList);
		delAction.setPredictData(predictData);
		delAction.setTargetReachIds(targets.asSet());
		ColumnData deliveryFrac = null;
		startTime = System.currentTimeMillis();
		for (int i=0; i< ITERATION_COUNT;  i++) {
			deliveryFrac = delAction.run();
		}
		endTime = System.currentTimeMillis();
		long deliveryTotalTime = endTime - startTime;
		report(deliveryTotalTime, "Run Delivery", ITERATION_COUNT);
		assertTrue("Delivery calc time should be less than 500ms for 100 iterations.", 
				deliveryTotalTime < 500);
		
		
		
		assertTrue("Topo table should be indexed", predictData.getTopo().isIndexed(PredictData.TNODE_COL));

	}
	
	protected void report(long time, String description, int iterationCount) {
		log.debug("Total time for '"
				+ description + "' (" + iterationCount + " times) "
				+ time + "ms");
	}
	
}
