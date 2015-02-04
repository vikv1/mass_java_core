package edu.uw.bothell.css.dsl.MASS;

import java.util.Comparator;

public class AgentAsyncComparator implements Comparator<Agent> {

  @Override
  public int compare(Agent o1, Agent o2) {
    if (o1 != null && o2 != null) {
      // original agents
      if ((o1.getMyAsyncIndex() < Agents_base.STARTING_CHILD_ASYNC_INDEX
          && o2.getMyAsyncIndex() < Agents_base.STARTING_CHILD_ASYNC_INDEX) ||
          (o1.getMyAsyncIndex() >= Agents_base.STARTING_CHILD_ASYNC_INDEX
              && o2.getMyAsyncIndex() >= Agents_base.STARTING_CHILD_ASYNC_INDEX)){
        if (o1.getMyAsyncOriginalPid() != o2.getMyAsyncOriginalPid()) {
          return o1.getMyAsyncOriginalPid() - o2.getMyAsyncOriginalPid();
        } else {
          return o1.getMyAsyncIndex() - o2.getMyAsyncIndex();
        }
      }
      // spawned agents always greater than original agents
      else if (o1.getMyAsyncIndex() < Agents_base.STARTING_CHILD_ASYNC_INDEX) {
        // o2 is spawned agents
        return -1;
      }
      else {
        return 1;
      } 
    } else {
      if (o2 != null) {
        return -1;
      } else if (o1 != null) {
        return 1;
      } else {
        return 0;
      }
    }
  }

}
