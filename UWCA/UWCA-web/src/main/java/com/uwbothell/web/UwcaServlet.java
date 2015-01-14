/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.uwbothell.web;

//import com.uwbothell.entities.JobManagerSingleton;

import java.io.IOException;
import java.io.PrintWriter;
import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import uwca.JobManager;
import uwca.calculations.toe.Tasmax;

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
        
        jobMgr = JobManager.getInstance();
        String msg = jobMgr.getStatusUpdates();
        
        response.setContentType("text/plain");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(msg);
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
        
        switch(var){
            case "tmax":
                params = new String[5];
                params[0] = request.getParameter("param1");
                params[1] = request.getParameter("param2");
                params[2] = request.getParameter("param3");
                params[3] = request.getParameter("param4");
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
