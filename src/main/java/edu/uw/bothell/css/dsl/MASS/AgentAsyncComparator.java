/*

 	MASS Java Software License
	© 2012-2015 University of Washington

	Permission is hereby granted, free of charge, to any person obtaining a copy
	of this software and associated documentation files (the "Software"), to deal
	in the Software without restriction, including without limitation the rights
	to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
	copies of the Software, and to permit persons to whom the Software is
	furnished to do so, subject to the following conditions:

	The above copyright notice and this permission notice shall be included in
	all copies or substantial portions of the Software.

	The following acknowledgment shall be used where appropriate in publications, presentations, etc.:      

	© 2012-2015 University of Washington. MASS was developed by Computing and Software Systems at University of 
	Washington Bothell.

	THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
	IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
	FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
	AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
	LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
	OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
	THE SOFTWARE.

*/

package edu.uw.bothell.css.dsl.MASS;

import java.util.Comparator;

public class AgentAsyncComparator implements Comparator<Agent> {

  @Override
  public int compare(Agent o1, Agent o2) {
    if (o1 != null && o2 != null) {
      // original agents
      if ((o1.getMyOriginalAsyncIndex() < Agents_base.STARTING_CHILD_ASYNC_INDEX
          && o2.getMyOriginalAsyncIndex() < Agents_base.STARTING_CHILD_ASYNC_INDEX) ||
          (o1.getMyOriginalAsyncIndex() >= Agents_base.STARTING_CHILD_ASYNC_INDEX
              && o2.getMyOriginalAsyncIndex() >= Agents_base.STARTING_CHILD_ASYNC_INDEX)){
        if (o1.getMyAsyncOriginalPid() != o2.getMyAsyncOriginalPid()) {
          return o1.getMyAsyncOriginalPid() - o2.getMyAsyncOriginalPid();
        } else {
          return o1.getMyOriginalAsyncIndex() - o2.getMyOriginalAsyncIndex();
        }
      }
      // spawned agents always greater than original agents
      else if (o1.getMyOriginalAsyncIndex() < Agents_base.STARTING_CHILD_ASYNC_INDEX) {
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
