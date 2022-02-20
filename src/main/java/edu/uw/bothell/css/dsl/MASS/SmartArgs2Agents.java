/*
        MASS Java Software License
        © 2012-2021 University of Washington

        Permission is hereby granted, free of charge, to any person obtaining a copy
        of this software and associated documentation files (the "Software"), to deal
        in the Software without restriction, including without limitation the rights
        to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
        copies of the Software, and to permit persons to whom the Software is
        furnished to do so, subject to the following conditions:

        The above copyright notice and this permission notice shall be included in
        all copies or substantial portions of the Software.

        The following acknowledgment shall be used where appropriate in publications, presentations, etc.:

        © 2012-2021 University of Washington. MASS was developed by Computing and Software Systems at University of
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
import java.io.*;

public class SmartArgs2Agents implements Serializable {

    public static final int BreadthFirstSearch_ = 0;
    public static final int TriangleCounting_ = 1;
    public static final int rangeSearch_ = 2;


    public SmartArgs2Agents( int nextnode, int prevnode )
    {
        nextNode = nextnode;
        prevNode = prevnode;
    }

    public SmartArgs2Agents( int nextnode )
    {
        nextNode = nextnode;
    }

    public SmartArgs2Agents( int[] itinerary, int nextnode )
    {
        this.itinerary = itinerary.clone();
        this.nextNode = nextnode;
    }

    public SmartArgs2Agents(int applicationId, Object argument, int nextNode, int prevNode) {

        switch (applicationId) {
            case rangeSearch_:
                this.searchRange = (int[]) argument;
                if (nextNode != -1) {this.nextNode = nextNode;}
                break;
            case TriangleCounting_:  //TO DO: Should update the application program to use this constructor
                this.itinerary = (int[]) argument;
                this.nextNode = nextNode;
                break;
            case BreadthFirstSearch_: //TO DO: Should update the application program to use this constructor
                this.nextNode = nextNode;
                this.prevNode = prevNode;
                break;
            default:
        }
    }

    public int[] itinerary = null;
    public int nextNode = -1;
    public int prevNode = -1;

    //Attributes useful for Range Search
    public int[] searchRange;



}
