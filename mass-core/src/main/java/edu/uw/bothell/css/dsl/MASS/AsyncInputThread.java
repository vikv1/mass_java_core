package edu.uw.bothell.css.dsl.MASS;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

/**
 * Handle async Migration req and other type of message
 * from other nodes
 * @author hohung
 *
 */
public class AsyncInputThread extends Thread {
  private int portNumber;
  private ServerSocket serverSocket;
  private boolean listening;

  public AsyncInputThread(int port) {
    portNumber = port;
  }

  public void run() {
    listening = true;
    try (ServerSocket serverSocket = new ServerSocket(portNumber)) {
      while (listening) {
        new AsyncInputChildThread(serverSocket.accept()).start();
      }
    } catch (IOException e) {
      // Only Unexpected exception need to be logged 
      if (listening || !(e instanceof SocketException)) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        MASS_base.log(e + ". Stacktrace: " + sw.toString());
      }
    }
  }

  public void finish() {
    listening = false;
    if (serverSocket != null) {
      try {
        serverSocket.close();
      } catch (IOException e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        MASS_base.log(e + ". Stacktrace: " + sw.toString());
      }
    }
  }
  
  private class AsyncInputChildThread extends Thread {
    private Socket socket = null;
 
    public AsyncInputChildThread(Socket socket) {
        super("AsyncCommunicationServerThread");
        this.socket = socket;
    }
     
    public void run() { 
        try {
          // TODO process migration request to this current Node
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
}
