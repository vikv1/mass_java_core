/* 
 * FILE: index.js
 * CREATION DATE: 12sep14
 */
//*************************************************************************************************
// this happens on initial page load
// initialize event handlers here
$(document).ready(function(){
    
    //*********************************
    // job submission button click event
    $("#submitBtn").click(function(){
        var param = $("#mgtParams").val();
        var model = $("#inputParams").val();
        if(param == "tmax"){
            var tempThresh = $("#tempThreshold").val();
            var tolerance = $("#tolerance").val();
            var numOfToeYears = $("#numToeYears").val();   
            submitJob(param[0], model[0], tempThresh, tolerance, numOfToeYears);
        }
        submitJob(param[0], model[0]);
        getStatusUpdates();
    });
    //*********************************
    // status request button
    $("#statusBtn").click(function(){
        getStatusUpdates();        
    });
    //*********************************
    //*********************************
});
//*************************************************************************************************
function buildStatusTable(data){
    data = JSON.parse( data );
    $("#statusTableBody").empty();
    for (i = 0; i < data.length; i++) { 
        
        var toeVar = data[i][0];
        var climateModel = data[i][1];
        var numOfToeYears = data[i][2];
        var status = data[i][3];
        var provFile =  data[i][4];
        var toeRegFile = data[i][5];
        var toeMaxFile =  data[i][6];
        var toeMinFile =  data[i][7];
        // <a href="UwcaServlet?method=getFile">download the new file</a>
        var provFileLink = "<a href='UwcaServlet?method=getFile&fileName="+provFile+"'>prov file</a>";
        var regToeFileLink = "<a href='UwcaServlet?method=getFile&fileName="+toeRegFile+"'>reg toe</a>";
        var plsToeFileLink = "<a href='UwcaServlet?method=getFile&fileName="+toeMaxFile+"'>max toe</a>";
        var minToeFileLink = "<a href='UwcaServlet?method=getFile&fileName="+toeMinFile+"'>min toe</a>";
        
       $("#statusTableBody").append("<tr><td>"+toeVar+"</td><td>"+climateModel+"</td>\n\
       <td>"+numOfToeYears+"</td><td>"+status+"</td><td>"+provFileLink+"</td><td>"+regToeFileLink+"</td><td>"+
                plsToeFileLink+"</td><td>"+minToeFileLink+"</td></tr>");
    }  
}
//*************************************************************************************************
function getStatusUpdates(){
        $.ajax({
          type: "GET",
          async: false,
          cache: false,
          url: "UwcaServlet",
          data: {method : "getStatusUpdates" },
          success : function(data){
              buildStatusTable(data);
          }
    });
}
//*************************************************************************************************
function submitJob(param, model, tempThresh, tolerance, numOfToeYears){    
    
    $.ajax({
          type: "POST",
          async: false,
          cache: false,
          url: "UwcaServlet",
          data: { var : param , model: model, tempthresh: tempThresh, tol: tolerance, toeyears: numOfToeYears},
          success : function(data){           
              alert("Job Submitted");
          }
    });
}
//*************************************************************************************************

//*************************************************************************************************