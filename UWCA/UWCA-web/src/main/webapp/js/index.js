/* 
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */


//******************************************************************************
$(document).ready(function(){
   /*
    $("#playerInputDiv").hide();
    
    $("#gamerTagSubmit").click(function(){
        if($("#gamerTag").val() == "" || $("#gamerTag").val() == null ){
            alert("Please enter a valid gamer tag NOOB!");
            return;
        }
        glb.player = $("#gamerTag").val();
        $("#playerInputDiv").hide();
        SyncScores();
    });
    */
   
    someFunction();
   
});


//******************************************************************************
function someFunction(){
    
    
    $.ajax({
          type: "POST",
          url: "UwcaServlet",
          data: { name : "Hello " , game: "World"},
          success : function(data){
              alert(data);
            
          }
    });
    
    /*
    if(glb.player == null){        
        $("#playerInputDiv").show();
        return;
    }

glb.score = $("#score").text();
        $.ajax({
          type: "POST",
          url: "GameServlet",
          data: { name : glb.player , game: glb.game, score: glb.score},
          success : function(data){
            //  alert(data);
            PopulateScoreList(data);
          }
        });
*/
}