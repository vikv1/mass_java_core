package MASS;

//
//  MASS.Mthread.java
//  
//
//  Created by John Spiger on 6/22/10.
//  



/**
 * MASS.MASS thread
 */
public class Mthread extends Thread 
{

    // Vector<Long> ea_times;
    // Vector<Long> ca_times;

    /**
     * Constructor
     */
    public Mthread() {}

    /**
     * {@inheritDoc}
     */
    public void run() 
    {
	MASS.log( "Mthread created: " + Thread.currentThread( ) );
        boolean running = true;
        while (running) 
        {
            try 
            {
                synchronized (MASS.STATUS) 
                {
                    if (MASS.STATUS[0] == MASS.STATUS_READY) 
                    {
                        MASS.STATUS.wait();
                    }
                }
            } 
            catch (InterruptedException e) 
            {
                    e.printStackTrace();
                    running = false;
            } // end of catch
            if (MASS.STATUS[0] == MASS.STATUS_TERMINATE) 
            {
                running = false; // break out of while loop
            } 
            else if (MASS.STATUS[0] == MASS.STATUS_CALLALL) 
            {
                MASS.ca_callAll();
            } 
            else if (MASS.STATUS[0] == MASS.STATUS_EXCHANGE_ALL) 
            {
                MASS.ea_exchangeAll();
            } 
            else if (MASS.STATUS[0] == MASS.STATUS_AGENTS_CALL_ALL) 
            {
                MASS.agentsCallAllPerThread();
            } 
            else if (MASS.STATUS[0] == MASS.STATUS_AGENTS_MANAGE_ALL) 
            {
                MASS.agentsManageAllPerThread();
            } 
            else if (MASS.STATUS[0] == MASS.STATUS_AGENTS_SORT_ALL) 
            {
                MASS.agentsSortAllPerThread();
            } 
			else if (MASS.STATUS[0] == MASS.STATUS_EXCHANGE_BOUNDARY)
			{
				MASS.eb_exchangeBoundary();
				MASS.eb_update();
			}
            else 
            {
                if (running) 
                {
                    String msg = "ERROR: something is currupting MASS.MASS.STATUS and notifying MThreads.";
                    System.out.println(msg);
                }
            }
        }
        MASS.recordThreadExit(); // thread dies at end of run()
    }
}
