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
        
       $("#statusTableBody").append("<tr><td>"+data[i][0]+"</td><td>"+data[i][1]+"</td>\n\
       <td>"+data[i][2]+"</td><td>"+data[i][3]+"</td></tr>");
    }  
}
//*************************************************************************************************
function getStatusUpdates(){
        $.ajax({
          type: "GET",
          async: false,
          cache: false,
          url: "UwcaServlet",
          data: {},
          success : function(data){             
              buildStatusTable(data);
             
          }
    });
}
//*************************************************************************************************
function submitJob(param, model){    
    
    $.ajax({
          type: "POST",
          async: false,
          cache: false,
          url: "UwcaServlet",
          data: { var : param , model: model},
          success : function(data){           
              alert("Job Submitted");
          }
    });
}
//*************************************************************************************************

//*************************************************************************************************