package edu.uw.bothell.css.dsl.MASS;

import java.io.Serializable;

@SuppressWarnings("serial")
public class AgentMigrationResponse implements Serializable {
  
  private int numOfAgentReceived;
  private boolean chosenAsParentPid;
  
  public AgentMigrationResponse(int nOA, boolean chosen) {
    numOfAgentReceived = nOA;
    chosenAsParentPid = chosen;
  }
  
  public int getNumOfAgentReceived() {
    return numOfAgentReceived;
  }
  
  public boolean isChosenAsParentPid() {
    return this.chosenAsParentPid;
  }
}
