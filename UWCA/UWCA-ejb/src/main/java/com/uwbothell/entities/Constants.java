package com.uwbothell.entities;

/**
 * Configuration and global constants
 * 
 * @author Brett Yasutake
 * @author Niko Simonson
 */
public class Constants {
    // Panoply results viewer path
    public static final String PANOPLYJARPATH = "/home/brett/PanoplyJ/jars/Panoply.jar";
    
    public static final String ANALYTICSFOLDER =
            "/home/brett/NetBeans Projects/pac-nw-climate-analysis/src/pnca/analytics"; //brett linux
    
//    public static final String ANALYTICSFOLDER =
//            "/data/pac-nw-climate-analysis/src/pnca/analytics"; //hercules

    public static final String ANALYTICSPACKAGE = "pnca.analytics.";
    
    // MASS arguments
    public static final String[] MASSARGS = {"dslab", "dslab-302",
        "machinefile.txt", "12345"};            // connection arguments
    public static final int NPROCESSES = 2;    // number of processes to employ
    public static final int NTHREADS = 2;      // number of threads to employ

    public static final String TEMP = "T2";
    public static final String XWIND = "U10";      // east-west windspeed at 10m above surface
    public static final String YWIND = "V10";      // north-south windspeed at 10m above surface
    public static final String MOISTURE = "Q2";    // moisture at 2m above surface
    public static final String XQ = "QU";   // x moisture
    public static final String YQ = "QV";   // y moisture
    public static final String RAINC = "RAINC";
    public static final String RAINNC = "RAINNC";
    
    // Northwest regional climate data sample
    public static final int XRANGE = 123;              // width of data set (# of grid cells)
    public static final int YRANGE = 162;              // length of data set (# of grid cells)

    public static final int METADATALENGTH = 7;        // # of metadata elements
    public static final double FREEZING = 273.15;   // 0 celcius in kelvin
    public static final double GROWING = FREEZING + 5.0; // candidate for start of growing season
    public static final int DISTANCE = 30;     // km distance between two geographic cells
   
    // time decoding
    public static final int MONTHDIVISOR = 100;
    public static final int YEARDIVISOR = 10000;
    
    // max iterations
    public static final int NONTIMEBASED = 100;
    public static final int TALLIES = 1460;
    
    // max provenance character array size
    public static final int MAXPROVENANCESIZE = 1024;
}
