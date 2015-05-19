package edu.uw.bothell.css.dsl.MASS.motif;

// Driver.java
//
// by Matt Kipps
// 12/12/14

import edu.uw.bothell.css.dsl.MASS.*;

public class Driver {
  private static final String JAR_FILE_NAME = "mass-motif-async-0.8.2-SNAPSHOT-jar-with-dependencies.jar";
    public static void main(String args[]) {
        // verify arguments
      if (args.length < 3) {
        System.out.println(
            "usage: Driver " +
            "threads_per_node data_file motif_size [--show-results]");
        System.exit(-1);
    }

    // read in the user's password
   /* String password = "";
    try {
        BufferedReader passReader = new BufferedReader(
            new FileReader(args[1]));
        password = passReader.readLine().trim();
        passReader.close();
    } catch (Exception e) {
        System.out.println("Unable to read user password.");
        e.printStackTrace();
        System.exit(-1);
    }*/

    // configure arguments for MASS
    //String[] massArgs = new String[5];
   // massArgs[0] = args[0];           // username
    //massArgs[1] = password;          // password
   // massArgs[2] = "nodes.xml"; // machine file
   // massArgs[3] = args[2];           // port UNUSED
   // massArgs[4] = args[3];           // additions to classpath

   // int nProcesses = Integer.parseInt(args[0]);
    int nThreads = Integer.parseInt(args[0]);

    boolean showResults = false;
    if (args.length == 4 && args[3].equals("--show-results")) {
        showResults = true;
    }

    MASS.addLibrary(JAR_FILE_NAME);
    MASS.setNodeFilePath("nodes.xml");
    MASS.setCommunicationPort(50951);
    MASS.setNumThreads(nThreads);

    // start MASS
    MASS.init();

    long start = System.currentTimeMillis();

    // run the program
    Main app = new Main(args[1], Integer.parseInt(args[2]), showResults);
        app.run();

        System.out.println(
            (System.currentTimeMillis() - start) + " milliseconds to " +
            "complete the program");

        // finish MASS
        MASS.finish();

    }
}
