package edu.uw.bothell.css.dsl.MASS.Mandelbrot;

import java.net.InetAddress;

import edu.uw.bothell.css.dsl.MASS.Agent;

public class Colorer extends Agent {

  private Object obj;

  public static final int CALCULATE_COLOR = 0;
  public static final int MIGRATE = 1;
  public static final int INIT_MIGRATE = 2;

  /**
   * This constructor will be called upon instantiation by MASS The Object
   * supplied MAY be the same object supplied when Places was created
   * 
   * @param obj
   */
  public Colorer(Object obj) {
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
    default:
      return new String("Unknown Method Number: " + method);

    }

  }

  private Object initMigrate(Object o) {
    int yModifier = (Integer)o;
    migrateAsync(0, yModifier);
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
    double x0 = -2.5 + ((double)yModifier / (double)Program.MATRIX_SIZE) * 3.5;
    double y0 = -1.0 + ((double)xModifier / (double)Program.MATRIX_SIZE) * 2.0;
    double x = 0.0, y = 0.0;
    int iteration = 0;
    while (x*x + y*y < 4.0 && iteration < Program.MAX_ITERATION) {
      double xtemp = x*x - y*y + x0;
      y = 2 * x * y + y0;
      x = xtemp;
      iteration++;
    }
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

    migrateAsync(xModifier, yModifier);
    return o;
  }

}
