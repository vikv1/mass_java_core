/* 
 * FILE: index.js
 * CREATION DATE: 12sep14
 */
//*************************************************************************************************
// this happens on initial page load
// initialize event handlers here
$(document).ready(function(){
    
    $("#paramsDiv").hide();
    
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
        
        getStatusUpdates();
    });
    //*********************************
    // status request button
    $("#statusBtn").click(function(){
        getStatusUpdates();        
    });
    //*********************************
    // change the input models based on what variable is selected
    $("#mgtParams").change(function(){
        $("#inputParams").empty();
        var climateVariable = $("#mgtParams option:selected").val();
        switch(climateVariable) {
            case "tmax":
                
              $("#inputParams").append("<option value='conus_c5'>conus_c5</option>");
              $("#paramsDiv").show();
                break;
      
            default:
                
        }        
    });
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
        var toeMinFile =  data[i][6];
        var toeMaxFile =  data[i][7];
        // <a href="UwcaServlet?method=getFile">download the new file</a>
        var provFileLink = "<a href='UwcaServlet?method=getFile&fileName="+provFile+"'>prov file</a>";
        var regToeFileLink = "<a href='UwcaServlet?method=getFile&fileName="+toeRegFile+"'>ToE</a>";
        var plsToeFileLink = "<a href='UwcaServlet?method=getFile&fileName="+toeMaxFile+"'>ToE Plus Confidence Int</a>";
        var minToeFileLink = "<a href='UwcaServlet?method=getFile&fileName="+toeMinFile+"'>ToE Minus Confidence Int</a>";
        
       $("#statusTableBody").append("<tr><td class='c1'>"+toeVar+"</td><td class='c2'>"+climateModel+"</td>\n\
       <td class='c3'>"+numOfToeYears+"</td><td class='c4'>"+status+"</td><td class='c5'>"+provFileLink+"</td><td class='c6'>"+regToeFileLink+"</td><td class='c7'>"+
                minToeFileLink+"</td><td class='c8'>"+plsToeFileLink+"</td></tr>");
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