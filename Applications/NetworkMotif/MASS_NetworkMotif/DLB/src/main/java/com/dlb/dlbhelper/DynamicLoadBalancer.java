package main.java.com.dlb.dlbhelper;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimerTask;

import main.java.com.dlb.boundaryhelper.BoundaryHelper;
import main.java.com.dlb.impl.HistoryPolyRegression;
import main.java.com.dlb.impl.SlopeBasedAlgo;
import main.java.com.dlb.utils.DLBParams;


/**
 * 
 * @author Bhargav Mistry
 *
 */
public class DynamicLoadBalancer extends TimerTask {

	public static Map<Long, LinkedList<Long>> threadMap           = new HashMap<Long, LinkedList<Long>>();
	public static Map<Long, LinkedList<Double>> threadTimeDiff    = new HashMap<Long, LinkedList<Double>>();
	public static Map<Long, LinkedList<Double>> threadDiffPercent = new HashMap<Long, LinkedList<Double>>();
	public static int threadCounter                               = 0;
	public static Map<Long, LinkedList<Long>> threadTimeMap       = new HashMap<Long, LinkedList<Long>>();
		
	@Override
	public void run() {
		DLBParams.LB_FLAG = true;
		DLBParams.TIME_COUNTER += 5;
		System.out.println("Timer kicked in !");
	}
	
	public synchronized static void setThreadTimeNew(Long[][] threadTime) {
		long thId = 0;
		long thTime = 0;
		if (DLBParams.DEBUG) {
			System.out.println("*******************************************************");
		}
				
		for (int i = 0; i < threadTime.length; i++) {
		
			thId = threadTime[i][0];
			thTime = threadTime[i][1];
			
			if (!DynamicLoadBalancer.threadMap.containsKey(thId)) {
				LinkedList<Long> ll = new LinkedList<Long>();
				ll.addFirst(thTime);
				DynamicLoadBalancer.threadMap.put(thId, ll);
			} else {
				DynamicLoadBalancer.threadMap.get(thId).addLast(thTime);
			}
			
			if (DynamicLoadBalancer.threadMap.get(thId).size() >= 2 ) {
				if (!DynamicLoadBalancer.threadTimeDiff.containsKey(thId)) {
					LinkedList<Double> ll = new LinkedList<Double>();
					
					int size = DynamicLoadBalancer.threadMap.get(thId).size();
					ll.addFirst((double)(thTime - DynamicLoadBalancer.threadMap.get(thId).get(size-2)));
					
					DynamicLoadBalancer.threadTimeDiff.put(thId, ll);
					
				} else {
					int size = DynamicLoadBalancer.threadMap.get(thId).size();
					DynamicLoadBalancer.threadTimeDiff
						.get(thId)
							.addLast((double)(thTime - DynamicLoadBalancer.threadMap.get(thId).get(size-2)));
				}
			}// if ends here

		}// for loop ends here
		
		
		double highVal            = 0L;
		Long highThId             = 0L;
		double highPredictedVal   = 0.0;
		Long highThIdForPredicted = 0L;
		
		Iterator<Entry<Long, LinkedList<Double>>> itr = DynamicLoadBalancer.threadTimeDiff.entrySet().iterator();
		
		if (DLBParams.HISTORY_BASED || DLBParams.WINDOW_BASED) {
			/**
			 * Get the highest difference.
			 */
			while (itr.hasNext()) {
				Map.Entry<Long, LinkedList<Double>> pairs = (Entry<Long, LinkedList<Double>>) itr.next();
	
				if (highVal == 0.0) {
					highVal = pairs.getValue().getLast();
					highThId = pairs.getKey();
				} else {
					if (highVal < pairs.getValue().getLast()) {
						highVal = pairs.getValue().getLast();
						highThId = pairs.getKey();
					}
				}
			}//while ends here.
		}//main if ends here
		
		
		/********************************/
		itr = DynamicLoadBalancer.threadTimeDiff.entrySet().iterator();
		//Get the high
		double[] toDoubleArrX = null;
		Object[] objArr       = null;
		double predictedVal   = 0.0;
		
		while (itr.hasNext()) {
			
			Map.Entry<Long, LinkedList<Double>> pairs = (Entry<Long, LinkedList<Double>>) itr.next();
			
			Long key = pairs.getKey();
			
			LinkedList<Double> lnkLst = pairs.getValue();
			
			objArr = lnkLst.toArray();
			
			if (DLBParams.WINDOW_BASED) {
				
				double[] toDoubleArrY = new double[3];
				toDoubleArrX = new double[3];
				
				if (objArr.length > 2) {
					for (int i = (objArr.length - 3); i < toDoubleArrY.length; i++) {
						toDoubleArrY[i] = (Double)objArr[i];
						toDoubleArrX[i] = (i+1);
					}
					
					HistoryPolyRegression polyRegress = new HistoryPolyRegression(toDoubleArrX, toDoubleArrY, 4);
					predictedVal = polyRegress.predict(toDoubleArrX.length+1);
					
					if (highPredictedVal < predictedVal) {
						highPredictedVal = predictedVal;
						highThIdForPredicted = key;
					}
				}
			}//window based ends here
			
			if (DLBParams.HISTORY_BASED) {
				
				double[] toDoubleArrY = new double[objArr.length];
				toDoubleArrX = new double[objArr.length];
				for (int i = 0; i < toDoubleArrY.length; i++) {
					toDoubleArrY[i] = (Double)objArr[i];
					toDoubleArrX[i] = (i+1);
				}
			
				if (DLBParams.DEBUG) {
					System.out.println("array lengths : toDoubleArrX ["+toDoubleArrX.length+"] ["+toDoubleArrY.length+"]");
				}

				HistoryPolyRegression polyRegress = new HistoryPolyRegression(toDoubleArrX, toDoubleArrY, 4);
				predictedVal = polyRegress.predict(toDoubleArrX.length+1);
				
				if (DLBParams.DEBUG) {
					System.out.println("predicted val and th id : ["+predictedVal+"] ["+key+"]");
				}
				
				if (highPredictedVal < predictedVal) {
					highPredictedVal = predictedVal;
					highThIdForPredicted = key;
				}
			}// history based ends here.

			
			if (DLBParams.SLOPE_BASED) {
				double slope = 0.0;
				
				
				if (objArr.length > 1) {
					SlopeBasedAlgo dlbBase = new SlopeBasedAlgo();
					double x1 = objArr.length-1; 
					double y1 = (Double)objArr[objArr.length-2];
					double x2 = objArr.length;
					double y2 = (Double)objArr[(objArr.length-1)];
					
					slope = dlbBase.predict(x1, y1, x2, y2);
					
					if (DLBParams.DEBUG) {
					System.out.println("Slope calculated for thread id ["+pairs.getKey()+"] x1 ["+x1+"] y1 : ["+y1+"] x2 : ["+x2+"] y2 : ["+y2+"] slope : ["+slope+"]");
					}
					
					if (highVal < slope) {
						highVal = slope;
						highThId = pairs.getKey();
					}
					
					predictedVal = dlbBase.predictLoad( x2, y2, x2+1, slope);

					if (highPredictedVal < predictedVal) {
						highPredictedVal = predictedVal;
						highThIdForPredicted = pairs.getKey();
					}
				}
			}// slope based.
			
						
		}//main while ends here
		
		if (DLBParams.DEBUG) {
			System.out.println("highpredicted value : ["+highPredictedVal+"] hipredicted thread ["+highThIdForPredicted+"]");
		}
		
		/********************************/
		
		/**
		 * Adjust the boundaries for highest loaded thread.
		 */
		if (highVal != 0L) {
			if (DLBParams.DEBUG) {
				System.out.println("setThreadTimeNew highVal : ["+highVal+"] for threadId : ["+highThId+"]");
			}
			BoundaryHelper.calculateAndAdjustBoundariesNew(highThId, highVal);
		}
		
		/**
		 * Adjust again for the thread that is higher as per the algorithm prediction.
		 */
		if (highPredictedVal != 0) {
			if (objArr.length > 2) {
				if (DLBParams.DEBUG) {
					System.out.println("setThreadTimeNew predicted values : ["+
							highPredictedVal+"] for threadId : ["+highThIdForPredicted+"]");
				}
				BoundaryHelper.calculateAndAdjustBoundariesNew(highThIdForPredicted, highPredictedVal);
			}
		}
	}
	
//	public void printThreadTimeList() {
//		Iterator<Entry<Long, LinkedList<Long>>> itr = threadMap.entrySet().iterator();
//				
//		while (itr.hasNext()) {
//			Map.Entry<Long, LinkedList<Long>> pairs = (Map.Entry<Long, LinkedList<Long>>)itr.next();
//			System.out.println("Key: " + pairs.getKey());
//			LinkedList<Long> lst = pairs.getValue();
//			for (Object obj : lst.toArray()) {
//				System.out.println("Values : " + (Long)obj);
//			}
//			
//			doPercentageIncDec1(lst);
//		}
//	}
	
//	public double[] doPercentageIncDec(LinkedList<Long> list) {
//		double series[] = new double[list.size()];
//		int pos = 0;
//		double dd = 0.0;
//		
//		for (int i = 0; i < list.size(); i++) {
//			if ( i == 0 ){
//				continue;
//			}
//			
//			if (list.get(i-1) > list.get(i)) {
//				dd = ((double)(list.get(i-1) - list.get(i))/(double)list.get(i-1));
//				dd *= 100;
//				System.out.printf("i["+(i-1)+"]["+i+"] ["+list.get(i-1)+"]["+list.get(i)+"] = %.4f", dd);
//				//this is %decrease
//				System.out.println("");
//			} else {
//				dd = (double)(list.get(i) - list.get(i-1))/(double)list.get(i-1);
//				dd *= 100;
//				System.out.printf("i["+(i-1)+"]["+i+"] ["+list.get(i-1)+"]["+list.get(i)+"] = %.4f", 
//						dd);
//				System.out.println("");
//			}
//			series[pos++] = dd;
//		}
//		
//		return series;
//	}
	
//	public double[] doPercentageIncDec1(LinkedList<Long> list) {
//		double series[] = new double[list.size()-1];
//		long dd = 0;
//		int pos = 0;
//		double percentage[] = new double[list.size()-2];
//		
//		for (int i = 0; i < list.size(); i++) {
//			if ( i == 0 ){
//				continue;
//			}
//			
//			if (list.get(i-1) > list.get(i)) {
//				dd = Math.abs(list.get(i-1) - list.get(i));
//				
//				System.out.printf("i["+(i-1)+"]["+i+"] ["+list.get(i-1)+"]["+list.get(i)+"] = %d", dd);
//				//this is %decrease
//				System.out.println("");
//			} else {
//				dd = Math.abs(list.get(i) - list.get(i-1));
//				
//				System.out.printf("i["+(i-1)+"]["+i+"] ["+list.get(i-1)+"]["+list.get(i)+"] = %d", 
//						dd);
//				System.out.println("");
//			}
//			series[pos++] = dd;
//		}
//		
//		pos = 0;
//		
//		for (int i = 0; i < series.length; i++) {
//			if (i == 0) {
//				continue;
//			}
//			
//			percentage[pos++] = (series[i]/series[i-1])*100;
//			
//		}
//	
//		for (int k = 0; k < percentage.length; k++) {
//			System.out.println("percentage["+k+"]["+percentage[k]+"]");
//		}
//		
//		return percentage;
//	}

}
