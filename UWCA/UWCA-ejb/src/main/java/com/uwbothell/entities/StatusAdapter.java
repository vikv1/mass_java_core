package com.uwbothell.entities;

import java.util.*;

/**
 * Adapter to store and report status messages between analytics module and UI.
 *
 * @author Brett Yasutake (Primary)
 * @author Niko Simonson (Contributing)
 */
public class StatusAdapter {

    // Variables
    private List<String> statusList;//TODO keep status log/history
    private String message;         // Latest status message received from the
                                    // analytics module
    /**
     * Creates a new status adapter with initial status.
     */
    public StatusAdapter() {
        message = "Status adapter ready";
    }

    /**
     * Keep a history of all status messages received from analytics module
     * -- not implemented yet
     * 
     * @param statusName    the status message to log
     * @param results       
     */
    public void logStatus(String statusName, boolean[]... results) {
    }

    /**
     * Analytics module uses reportMessage to update the status adapter with its
     * latest status message
     * 
     * @param msg   new status message
     */
    public void reportMessage(String msg) {
        message = msg;
    }

    /**
     * Retrieves the latest status message.
     * 
     * @return  the current status message
     */
    public String getCurrentMessage() {
        return message;
    }
}