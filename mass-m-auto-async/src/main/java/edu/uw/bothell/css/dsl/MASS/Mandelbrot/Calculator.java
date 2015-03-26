package edu.uw.bothell.css.dsl.MASS.Mandelbrot;

import java.net.InetAddress;

import edu.uw.bothell.css.dsl.MASS.Agent;
import edu.uw.bothell.css.dsl.MASS.MASS;

public class Calculator extends Agent {

  private Object obj;

  public static final int CALCULATE_COLOR = 1;
  public static final int MIGRATE = 2;
  public static final int INIT_MIGRATE = 3;
  public static final int MIGRATE_HORIZON = 4;
  public static final int INIT_MIGRATE_HORIZON = 5;

  /**
   * This constructor will be called upon instantiation by MASS The Object
   * supplied MAY be the same object supplied when Places was created
   * 
   * @param obj
   */
  public Calculator(Object obj) {
    this.obj = obj;
  }

  /**
   * This method is called when "callAll" is invoked from the master node
   */
  public Object callMethod(int method, Object o) {

    switch (method) {

    case CALCULATE_COLOR:
      return calculateColor(o);

    case MIGRATE:
      return move(o);
    case INIT_MIGRATE:
      return initMigrate(o);
    case MIGRATE_HORIZON:
      return moveHorizon(o);
    case INIT_MIGRATE_HORIZON:
      return initMigrateHorizon(o);
    default:
      MASS.log("unknown method number " + method);
      return new String("Unknown Method Number: " + method);

    }

  }

  private Object initMigrate(Object o) {
    int yModifier = (Integer) o;
    //MASS.log("initMigrate [" + 0 + ", " + yModifier + "]");
    migrateAsync(0, yModifier);
    return o;
  }

  private Object initMigrateHorizon(Object o) {
    Point p = (Point)o;
  //  MASS.log("initMigrateReverse [" + xModifier + ", "
    //    + 0 + "]");
    migrateAsync(p.x, p.y);
    return o;
  }

  private Object moveHorizon(Object o) {
    int xModifier = this.getPlace().getIndex()[0];
    int yModifier = this.getPlace().getIndex()[1];
    ++yModifier;
    if(yModifier >= this.getPlace().getSize()[1]) {
      yModifier = 0;
      ++xModifier;
    }
  //  MASS.log("move from [" + xModifier + ", " + (yModifier - 1) + "] to ["
    //    + xModifier + ", " + yModifier + "]");
    migrateAsync(xModifier, yModifier);
    return o;
  }

  /**
   * Calculate the color of the current place in the Matrix
   * http://en.wikipedia.org/wiki/Mandelbrot_set#Computer_drawings
   * 
   * @param o
   * @return
   */
  public Object calculateColor(Object o) {
    int xModifier = this.getPlace().getIndex()[0];
    int yModifier = this.getPlace().getIndex()[1];    
    double x0 = -2.5 + ((double) yModifier / (double) this.getPlace().getSize()[1])
        * 3.5;
    double y0 = -1.0 + ((double) xModifier / (double) this.getPlace().getSize()[0])
        * 2.0;
    double x = 0.0, y = 0.0;
    int iteration = 0;
    //MASS.log("calculateColor for place[" + xModifier + "][" + yModifier
    //    + "].start x0 = " + x0 + ", y0 = " + y0);
    while (x * x + y * y < 4.0 && iteration < Program.MAX_ITERATION) {
      double xtemp = x * x - y * y + x0;
      y = 2 * x * y + y0;
      x = xtemp;
      iteration++;
    }
    /*if(yModifier < 5) {
    MASS.log("Calculate color for place[" + xModifier + "][" + yModifier
        + "] = " + iteration);
    }*/
    appendAsyncResult(iteration);
    return o;
  }

  /**
   * Move this Agent to the next position in the X-coordinate
   * 
   * @param o
   * @return
   */
  public Object move(Object o) {
    int xModifier = this.getPlace().getIndex()[0];
    int yModifier = this.getPlace().getIndex()[1];
    xModifier++;
   // MASS.log("move from [" + (xModifier - 1) + ", " + yModifier + "] to ["
    //    + xModifier + ", " + yModifier + "]");
    migrateAsync(xModifier, yModifier);
    return o;
  }

}
