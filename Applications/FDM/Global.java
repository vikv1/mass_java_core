/******************************************************************************
  Copyright(C) 2007 DeepDigital Co.,Ltd. All Rights Reserved.

  Parallelized with the MASS library, University of Washington Bothell
******************************************************************************/

package FDM;

/**
 * corresponds the 0th, 1st, and 2nd index to x, y, z or u, v, w of a three
 * dimensional array. The celle center is defined as the 3rd index of a
 * four dimensional array.
 */
public interface Global {

    public final int    _x=0, _y=1, _z=2, _cellCenter=3;
    public final int    _u=0, _v=1, _w=2;

    // added for communication with neighboring cubicles in MASS
    public final int east   = 0;  // x + 1                                                
    public final int west   = 1;  // x - 1                                                
    public final int north  = 2;  // y + 1                                                
    public final int south  = 3;  // y - 1                                                
    public final int top    = 4;  // z + 1                                                
    public final int bottom = 5;  // z - 1  
}
