/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.uwbothell.web;

//import com.uwbothell.entities.JobManagerSingleton;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import uwca.JobManager;

/**
 *
 * @author jwoodrin
 */
public class UwcaServlet extends HttpServlet {
    // @Inject grabs a reference to the singleton jobMgr for us
  //  @Inject
  //  private JobManagerSingleton jobMgr;
    JobManager jobMgr;
 
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String method = request.getParameter("method");
        
        switch (method){
        
            case "getFile":
                String filename = request.getParameter("fileName");
		response.setContentType("text/plain");
		PrintWriter out = response.getWriter();
	//	 String filename = "File1.txt"; 
        //        String filename = "jobs\\1627\\toeMin.nc";
	//	  String filepath = "C:\\Program Files\\glassfish-4.1\\glassfish\\domains\\domain1\\config\\"; 
		  response.setContentType("APPLICATION/OCTET-STREAM"); 
		  response.setHeader("Content-Disposition","attachment; filename=\"" + filename + "\""); 

	//	  java.io.FileInputStream fileInputStream = new java.io.FileInputStream(filepath + filename);
                  java.io.FileInputStream fileInputStream = new java.io.FileInputStream(filename);
		  
		  int i; 
		  while ((i=fileInputStream.read()) != -1) {
		    out.write(i); 
		  } 
		  fileInputStream.close();
		  out.close(); 
                break;
            case "getStatusUpdates":
                jobMgr = JobManager.getInstance();
                String msg = jobMgr.getStatusUpdates();

                response.setContentType("text/plain");
                response.setCharacterEncoding("UTF-8");
                response.getWriter().write(msg);
                break;
            default:
                break;
        }
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        jobMgr = JobManager.getInstance();
        String var = request.getParameter("var");
        
        String[] params = null;
        
        switch(var){ /// tempthresh: tempThresh, tol: tolerance, toeyears:
            case "tmax":
                params = new String[3];
                params[0] = request.getParameter("tempthresh");
                params[1] = request.getParameter("tol");
                params[2] = request.getParameter("toeyears");
                break;
            default:
                break;
        }      
  
        String model = request.getParameter("model");
        jobMgr.submitJob(var, model, params);

        // send response message
        response.setContentType("text/plain");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("");
    }
}
