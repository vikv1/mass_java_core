package edu.uw.bothell.css.dsl.MASS;

/**
 * To hold while waiting for async result in AsyncOutputThread
 * @author hohung
 *
 */
public class AsyncCommunicationLock {
  private boolean ready = false;
  private Object result;
  private int counter = 0;
  private Object secondResult;
  
  public boolean isReady() {
    return ready;
  }
  
  public void reset() {
    counter = 0;
    ready = false;
    result = null;
    secondResult = null;
  }
  
  public void set() {
    ready = true;
  }
  
  public Object getResult() {
    return result;
  }
  
  public void setResult(Object value) {
    result = value;
  }
  
  public Object getSecondResult() {
    return secondResult;
  }
  
  public void setSecondResult(Object value) {
    secondResult = value;
  }
  
  public int getCounter() {
    return counter;
  }
  
  public void incrementCounter() {
    ++counter;
  }
}
