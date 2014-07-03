package main.java.com.dlb.boundaryhelper;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import main.java.com.dlb.dlbhelper.DynamicLoadBalancer;
import main.java.com.dlb.utils.DLBParams;
import main.java.com.dlb.utils.Slice;

/**
 * 
 * @author Bhargav Mistry
 *
 */
public class BoundaryHelper {
	
	public static Map<Long, Integer> lowerMap = null;
    public static Map<Long, Integer> upperMap = null;
	private static BoundaryHelper helper = null;
	private static int totalThreads = 0;
	
	public static FileOutputStream logger = null;
	
	
	/**
	 * Set total threads.
	 */
	public static void setTotalThreads(int th) {
		totalThreads = th;
	}
	
	/**
	 * Get total threads.
	 */
	public static int getTotalThreads() {
		return totalThreads;
	}

	/**
	 * Get Boundary Helper instance.
	 */
	public static synchronized BoundaryHelper getInstance() {
		
		if (helper == null) {
			init();
		}
		
		return helper;
	}
	
	private static FileOutputStream getLogger() {
		if (logger == null) {
			try {
				logger = new FileOutputStream("DLBLogfile.txt");
			} catch (FileNotFoundException e) {
				log("Error while creating dlb logger");
			}
			return logger;
		} else {
			return logger;
		}
	}
	
	/**
	 * Initialize bound maps.
	 */
	private static void init() {
		helper = new BoundaryHelper();
		lowerMap = new HashMap<Long, Integer>();
		upperMap = new HashMap<Long, Integer>();
	}
	
	public static void log(String msg) {
		
		try {
			getLogger().write(msg.concat("\n").getBytes());
			getLogger().flush();
		} catch (IOException e) {
			try {
				getLogger().close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}
	
	
	public static void printAllThreadBounds() {
		Iterator<Entry<Long, Integer>> itr = lowerMap.entrySet().iterator();
		
		while (itr.hasNext()) {
			Map.Entry<Long, Integer> mapEntry = (Map.Entry<Long, Integer>)itr.next();
			log("lowermap key :["+mapEntry.getKey()+"] ["+mapEntry.getValue()+"]");
		}
		
		itr = upperMap.entrySet().iterator();
		
		while (itr.hasNext()) {
			Map.Entry<Long, Integer> mapEntry = (Map.Entry<Long, Integer>)itr.next();
			log("uppermap key :["+mapEntry.getKey()+"] ["+mapEntry.getValue()+"]");
		}
	}
	
	public static void printAllBounds() {
		Iterator<Entry<Long, Slice>> itr = DLBParams.boundaryMap.entrySet().iterator();
		
		while (itr.hasNext()) {
			Map.Entry<Long, Slice> entry = (Map.Entry<Long, Slice>)itr.next();
			log("Thread Id : " + entry.getKey() + " lowerbound : " + entry.getValue().getLowerBound() + 
					" upperbound : " + entry.getValue().getUpperBound());
		}
	}
	
	public static void calculateAndAdjustBoundaries(Long threadId, double highVal) {
		log("************ calculateAndAdjustBoundaries() method called start*****************");
		log("ThreadId : ["+threadId+"] highVal ["+highVal+"]");
		log("************ calculateAndAdjustBoundaries() method called end*****************");
		
		int lowerBoundOfThread = lowerMap.get(threadId);
		int upperBoundOfThread = upperMap.get(threadId);
		 
		 if (lowerBoundOfThread != 0) {
			long peerThread = getHighPeerThread(lowerBoundOfThread - 1);
			int upperboundPeer = upperMap.get(peerThread);
			
			if (lowerBoundOfThread+1 != upperboundPeer) {
				lowerBoundOfThread += 1;
				lowerMap.put(threadId, lowerBoundOfThread);

				upperboundPeer += 1;
				upperMap.put(peerThread, upperboundPeer);
			}
			
		 } else {
			 long peerThread = getLowPeerThread(upperBoundOfThread + 1);
			 int lowerBound = lowerMap.get(peerThread);
			 
			 upperBoundOfThread -= 1;
			 upperMap.remove(threadId);
			 upperMap.put(threadId, upperBoundOfThread);
			 
			 //adjust peer thread
			 
			 lowerBound -= 1;
			 lowerMap.remove(peerThread);
			 lowerMap.put(peerThread, lowerBound);
		 }
		 
		 printAllThreadBounds();

	}
	

	
	public static void calculateAndAdjustBoundariesNew(Long threadId, double highVal) {
		if (DLBParams.DEBUG_SP) {
			log("********* calculateAndAdjustBoundaries() method called start **************");
			printAllBounds();
			log("highest thread : " + threadId);
		}
		
		
		int lowerBoundOfCurrentThread = DLBParams.boundaryMap.get(threadId).getLowerBound();
		int upperBoundOfCurrentThread = DLBParams.boundaryMap.get(threadId).getUpperBound();
	
		log("lowerBoundOfCurrentThread : " + lowerBoundOfCurrentThread);
		log("upperBoundOfCurrentThread : " + upperBoundOfCurrentThread);
		
		if (DLBParams.DEBUG_SP) {
			log("current thread lowerBound ["+lowerBoundOfCurrentThread+"] upperBoundOfCurrentThread ["+
			upperBoundOfCurrentThread+"]");
		}
		
		if (lowerBoundOfCurrentThread == 0) {
			if (DLBParams.DEBUG_SP) {
				log("lowerBound is 0");
			}
			Long thId = getRightPeerThread(upperBoundOfCurrentThread+1);
			
			if ( lowerBoundOfCurrentThread != (upperBoundOfCurrentThread - 2) ) {
				upperBoundOfCurrentThread -= 1;
				DLBParams.boundaryMap.get(thId).setLowerBound(DLBParams.boundaryMap.get(thId).getLowerBound() - 1);
				DLBParams.boundaryMap.get(threadId).setUpperBound(upperBoundOfCurrentThread);
			} else {
				if (DLBParams.DEBUG_SP) {
					log("Cannot adjust boundary since lowerbound == upper-2 ["+lowerBoundOfCurrentThread+"] ["
							+upperBoundOfCurrentThread+"]");
				}
			}
			
		} else if (upperBoundOfCurrentThread == DLBParams.MAX_SIM_SIZE) {
			if (DLBParams.DEBUG_SP) {
				log("upperBound is MAX_SIM_SIZE");
			}
			Long thId = getLeftPeerThread(lowerBoundOfCurrentThread-1);
			
			if (upperBoundOfCurrentThread != (lowerBoundOfCurrentThread + 2)) {
				lowerBoundOfCurrentThread += 1;
				DLBParams.boundaryMap.get(thId).setUpperBound(DLBParams.boundaryMap.get(thId).getUpperBound() + 1);
				DLBParams.boundaryMap.get(threadId).setLowerBound(lowerBoundOfCurrentThread);
			} else {
				if (DLBParams.DEBUG_SP) {
					log("Cannot adjust boundary since upperbound == lower+2 ["+lowerBoundOfCurrentThread+"] ["
							+upperBoundOfCurrentThread+"]");
				}
			}
		} else {
			Long leftThId = getLeftPeerThread(lowerBoundOfCurrentThread-1);
			Long rightThId = getRightPeerThread(upperBoundOfCurrentThread+1);
			if (DLBParams.DEBUG_SP) {
				log("else part : leftThId ["+leftThId+"] rightThId ["+rightThId+"]");
			}
			
			double leftThreadTime = DynamicLoadBalancer.threadTimeDiff.get(leftThId).getLast();
			double rightThreadTime = DynamicLoadBalancer.threadTimeDiff.get(rightThId).getLast();
			
			if (DLBParams.DEBUG_SP) {
				log("else part time : leftThreadTime ["+leftThreadTime+"] rightThreadTime ["+rightThreadTime+"]");
			}
			
			if (leftThreadTime < rightThreadTime) {
				if ((lowerBoundOfCurrentThread+2) != upperBoundOfCurrentThread) {
					lowerBoundOfCurrentThread += 1;
					DLBParams.boundaryMap.get(leftThId).setUpperBound(
							DLBParams.boundaryMap.get(leftThId).getUpperBound() + 1);
					DLBParams.boundaryMap.get(threadId).setLowerBound(lowerBoundOfCurrentThread);
				} else {
					if (DLBParams.DEBUG_SP) {
						log("cannot adjust lowerbound == upperbound + 2");
					}
				}
				
			} else if ( (rightThreadTime < leftThreadTime) || (leftThreadTime == rightThreadTime) ) {
				if (lowerBoundOfCurrentThread != (upperBoundOfCurrentThread - 2)) {
					upperBoundOfCurrentThread -= 1;
					DLBParams.boundaryMap.get(rightThId).setLowerBound(
							DLBParams.boundaryMap.get(rightThId).getLowerBound() - 1);
					DLBParams.boundaryMap.get(threadId).setUpperBound(upperBoundOfCurrentThread);
				} else {
					if (DLBParams.DEBUG_SP) {
						log("cannot adjust lowerbound == upperBound-2");
					}
				}
			}
			
			if (DLBParams.DEBUG_SP) {
				log("*********** After boundary adjust **************");
				printAllBounds();
				log("*********** After boundary adjust **************");
			}
		}
		
		if (DLBParams.DEBUG_SP) {
			log("********* calculateAndAdjustBoundaries() method called end **************");
		}
	}
	
	public static void calculateAndAdjustBoundaries1(Long threadId, double highVal) {
		log("************ calculateAndAdjustBoundaries() method called start*****************");
		log("ThreadId : ["+threadId+"] highVal ["+highVal+"]");
		log("************ calculateAndAdjustBoundaries() method called end*****************");
		
		int lowerBoundOfCurrentThread = DLBParams.boundaryMap.get(threadId).getLowerBound();
		int upperBoundOfCurrentThread = DLBParams.boundaryMap.get(threadId).getUpperBound();
		
		log("lowerboundofcurrentThread : [" + lowerBoundOfCurrentThread +"] upperBoundofCurrThread : ["
				+upperBoundOfCurrentThread+"]");
		log("Sugar.MAX_SIM_TIME : " + DLBParams.MAX_SIM_SIZE);
		
		Long lowerPeerThreadId = getLowerPeerThread(lowerBoundOfCurrentThread == 0 ? lowerBoundOfCurrentThread : (lowerBoundOfCurrentThread - 1));
		Long higherPeerThreadId = getHigherPeerThread(upperBoundOfCurrentThread == DLBParams.MAX_SIM_SIZE ? upperBoundOfCurrentThread : 
			(upperBoundOfCurrentThread + 1));
		
		log("higherPeerThreadId : ["+higherPeerThreadId+"] for : ["+(upperBoundOfCurrentThread + 1)+"]");
		
		if (lowerBoundOfCurrentThread == 0) {
			
			if ( (upperBoundOfCurrentThread - 1) != lowerBoundOfCurrentThread) {
				upperBoundOfCurrentThread -= 1;
				DLBParams.boundaryMap.get(threadId).setUpperBound(upperBoundOfCurrentThread);
				
				int higherPeerLowerBound = DLBParams.boundaryMap.get(higherPeerThreadId).getLowerBound();
				higherPeerLowerBound -= 1;
				DLBParams.boundaryMap.get(higherPeerThreadId).setLowerBound(higherPeerLowerBound);
			}
			
		} else {
			if (upperBoundOfCurrentThread != DLBParams.MAX_SIM_SIZE) {
				log("higherPeerThreadId is : ["+higherPeerThreadId+"]");
				int higherPeerLowerBound = DLBParams.boundaryMap.get(higherPeerThreadId).getLowerBound();
				higherPeerLowerBound -= 1 ;
				DLBParams.boundaryMap.get(higherPeerThreadId).setLowerBound(higherPeerLowerBound);
				
				upperBoundOfCurrentThread -= 1;
				DLBParams.boundaryMap.get(threadId).setUpperBound(upperBoundOfCurrentThread);
				
			} else {
				lowerBoundOfCurrentThread += 1;
				DLBParams.boundaryMap.get(threadId).setLowerBound(lowerBoundOfCurrentThread);
				
				int lowerPeerUpperBound = DLBParams.boundaryMap.get(lowerPeerThreadId).getUpperBound();
				lowerPeerUpperBound += 1;
				DLBParams.boundaryMap.get(lowerPeerThreadId).setUpperBound(lowerPeerUpperBound);
			}
		}
		
		 
//		 if (lowerBoundOfThread != 0) {
//			int lowerPeerUpperBound = BoundaryHelper.boundarySlice.get(lowerPeerThreadId).getUpperBound();
//			
//			if ( (lowerBoundOfThread + 1 != upperboundPeer) ) {
//				lowerBoundOfThread += 1;
//				BoundaryHelper.boundarySlice.get(threadId).setLowerBound(lowerBoundOfThread);
//
//				
//				//	upperboundPeer += 1;
//				
//				BoundaryHelper.boundarySlice.get(lowerPeerThreadId).setUpperBound(upperboundPeer);
//			}
//		 } else {
//			 
//			 int lowerBoundPeer = BoundaryHelper.boundarySlice.get(higherPeerThreadId).getLowerBound();
//			 
//			 upperBoundOfThread -= 1;
//			 upperMap.remove(threadId);
//			 upperMap.put(threadId, upperBoundOfThread);
//			 
//			 //adjust peer thread
//			 
//			 lowerBound -= 1;
//			 lowerMap.remove(peerThread);
//			 lowerMap.put(peerThread, lowerBound);
//		 }
		 
		 printAllBounds();

	}
	
	public static Long getLeftPeerThread(int higherBound) {
		Long id = 0L;
		
		Iterator<Entry<Long, Slice>> itr = 
				DLBParams.boundaryMap.entrySet().iterator();
		
		while (itr.hasNext()) {
			Map.Entry<Long, Slice> map = (Map.Entry<Long, Slice>)itr.next();
			if ( 0 == higherBound) {
				if (map.getValue().getLowerBound() == higherBound) {
					id = map.getKey();
					break;
				}
			} else {
				if (map.getValue().getUpperBound() == higherBound) {
					id = map.getKey();
					break;
				}
			}
		}
		
		return id;
	}
	
	public static Long getLowerPeerThread(int higherBound) {
		Long id = 0L;
		
		Iterator<Entry<Long, Slice>> itr = 
				DLBParams.boundaryMap.entrySet().iterator();
		
		while (itr.hasNext()) {
			Map.Entry<Long, Slice> map = (Map.Entry<Long, Slice>)itr.next();
			if ( 0 == higherBound) {
				if (map.getValue().getLowerBound() == higherBound) {
					id = map.getKey();
					break;
				}
			} else {
				if (map.getValue().getUpperBound() == higherBound) {
					id = map.getKey();
					break;
				}
			}
		}
		
		return id;
	}
	
	public static Long getRightPeerThread(int lowerBound) {
		Long id = 0L;
		
		Iterator<Entry<Long, Slice>> itr = 
				DLBParams.boundaryMap.entrySet().iterator();
		
		while (itr.hasNext()) {
			Map.Entry<Long, Slice> map = (Map.Entry<Long, Slice>)itr.next();
			if (DLBParams.MAX_SIM_SIZE == lowerBound) {
				if (map.getValue().getUpperBound() == lowerBound) {
					id = map.getKey();
					break;
				}
			} else {
				if (map.getValue().getLowerBound() == lowerBound) {
					id = map.getKey();
					break;
				}
			}
		}
		
		return id;
	}
	
	public static Long getHigherPeerThread(int lowerBound) {
		Long id = 0L;
		
		Iterator<Entry<Long, Slice>> itr = 
				DLBParams.boundaryMap.entrySet().iterator();
		
		while (itr.hasNext()) {
			Map.Entry<Long, Slice> map = (Map.Entry<Long, Slice>)itr.next();
			if (DLBParams.MAX_SIM_SIZE == lowerBound) {
				if (map.getValue().getUpperBound() == lowerBound) {
					id = map.getKey();
					break;
				}
			} else {
				if (map.getValue().getLowerBound() == lowerBound) {
					id = map.getKey();
					break;
				}
			}
		}
		
		return id;
	}

	
	/**
	 * Calculate and adjust boundaries.
	 * This method calculates the boundareis for eligible
	 * threads and adjusts the boundaries dynamically.
	 */
	public static void calculateAndAdjustBoundaries(long[][] time, 
		int adjPrio) {
		long avg = 0;
		 for (int i= 0; i < getTotalThreads(); i++) {
             avg += time[i][1];
		 }
		 
		 avg /= getTotalThreads();
		 
		 //Sort the time array on highest time.
		 sort(time);
		 
		/**
		 * Keep adjusting the boundaries of all eligible
		 * candidates from top of the sorted array.
		 */
		for (int prio = 0; prio < adjPrio; prio++) {
			long threadId = time[prio][0];
			//get the top ones as per the argument
			 int lowerBoundOfThread = lowerMap.get(threadId);
			 int upperBoundOfThread = upperMap.get(threadId);
			 
			 if (lowerBoundOfThread != 0) {
				long peerThread = getHighPeerThread(lowerBoundOfThread - 1);
				
				lowerBoundOfThread += 1;
				lowerMap.put(threadId, lowerBoundOfThread);
				
				int upperboundPeer = upperMap.get(peerThread);
				upperboundPeer += 1;
				upperMap.put(peerThread, upperboundPeer);
				
			 } else {
				 long peerThread = getLowPeerThread(upperBoundOfThread + 1);
				 
				 upperBoundOfThread -= 1;
				 upperMap.remove(threadId);
				 upperMap.put(threadId, upperBoundOfThread);
				 
				 //adjust peer thread
				 int lowerBound = lowerMap.get(peerThread);
				 lowerBound -= 1;
				 lowerMap.remove(peerThread);
				 lowerMap.put(peerThread, lowerBound);
			 }
		}//prio for ends here

	}
	
	/**
	 * Sort method, sorts the 2 dimensional array
	 * based on second element (time).
	 */
	private static void sort(long[][] time) {
		long compareVal = 0;
		int index = -1;
		long tempTh = 0;
		long tempVal = 0;
		for (int bb = 0; bb < 3; bb++) {
			compareVal = time[bb][1];
			for (int kk = bb + 1; kk < 4; kk++) {
				if (compareVal < time[kk][1]) {
					compareVal = time[kk][1];
					index = kk;
				}
			}
			
		if (index != -1) {
			
			tempTh = time[bb][0];
			tempVal = time[bb][1];
			
			time[bb][0] = time[index][0];
			time[bb][1] = time[index][1];
			
			time[index][0] = tempTh;
			time[index][1] = tempVal;
			}
			index = -1;
		}
	}

	/**
	 * The method gets the higher bound of 
	 * the thread id passed in.
	 */
	public static long getHighPeerThread(long upperBound) {
		long threadId = 0;
		 Iterator itr1 = upperMap.entrySet().iterator();
         Map.Entry entry1 = null;
              while (itr1.hasNext()) {
                      entry1 = (Map.Entry)itr1.next();
                      entry1.getValue();
                      if ((Integer)entry1.getValue() == upperBound) {
                              threadId = (Long)entry1.getKey();
                              break;
                      }
              }
		
		
		return threadId;
	}
	
	/**
	 * The method gets the lower bound of
	 * the thread id passed in.
	 */
	public static long getLowPeerThread(long lowerBound) {
		long threadId = 0;
		 Iterator itr1 = lowerMap.entrySet().iterator();
        Map.Entry entry1 = null;
             while (itr1.hasNext()) {
                     entry1 = (Map.Entry)itr1.next();
                     entry1.getValue();
                     if ((Integer)entry1.getValue() == lowerBound) {
                             threadId = (Long)entry1.getKey();
                             break;
                     }
             }
		return threadId;
	}

}//class ends here.
