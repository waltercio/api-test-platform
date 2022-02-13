package apitestplatform.controllers;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.io.BufferedReader;
import org.springframework.web.bind.annotation.GetMapping;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;

@RestController
@RequestMapping("/")
public class UserController {
String line; 
String responseString = "";
int i = 0;
Process gatlingExecutionProcess;
Process generateRubyReportExecutionProcess;

@GetMapping("/executesingletest")
public String callExecuteSingleTest(@RequestParam String testName, @RequestParam String ENV,@RequestParam String logType) throws Exception{
    UserController userController = new UserController();
    String actualDateTime = userController.getDateTimeStamp();
    String gatlingFolderString = userController.getGatlingFolderString(actualDateTime);
    String jsessionFolderString = userController.getJsessionFolderString(actualDateTime);
    userController.createGatlingJsessionFolders(gatlingFolderString,jsessionFolderString);
    String response = userController.executeSingleTest(testName,ENV,gatlingFolderString,jsessionFolderString,logType);
    return response;

}

@GetMapping("/createfolders")
public void createFolders() throws Exception{
    UserController controller = new UserController();
    String actualDateTime = controller.getDateTimeStamp();
    String gatlingFolderString = controller.getGatlingFolderString(actualDateTime);
    String jsessionFolderString = controller.getJsessionFolderString(actualDateTime);
    controller.createGatlingJsessionFolders(gatlingFolderString,jsessionFolderString);
}

@GetMapping("/getdate")
public String returnDateTimeStamp(){
    String date = "";
    UserController userController = new UserController();
    date = userController.getDateTimeStamp();
    return date;

}

public String getJsessionFolderString(String dateTimeStamp){
    String jsessionFolderString = "jsession_" + dateTimeStamp;
    return jsessionFolderString;
}

public String getGatlingFolderString(String dateTimeStamp){
    String gatlingFolderString = "gatling_suite_" + dateTimeStamp;
    return gatlingFolderString;
}

public void createGatlingJsessionFolders(String gatlingFolderString, String jsessionFolderString){
    String resultsFolder = System.getenv("PWD");
    File gatlingFolder = new File(resultsFolder + "/tests/_results/" + gatlingFolderString);
    File jsessionFolder = new File(resultsFolder + "/tests/_results/" + jsessionFolderString);
    try{
        gatlingFolder.mkdir();
        jsessionFolder.mkdir();
    }catch (Exception e){
        e.printStackTrace();
    }
}

public String getDateTimeStamp(){
    LocalDate actualDate = LocalDate.now();
    LocalTime actualTime = LocalTime.now();
    String dateTimeStamp = Integer.toString(actualDate.getYear()) + "-"
                         + Integer.toString(actualDate.getMonthValue()) + "-"
                         + Integer.toString(actualDate.getDayOfMonth()) + "-"
                         + Integer.toString(actualTime.getHour()) + "-"
                         + Integer.toString(actualTime.getMinute()) + "-"
                         + Integer.toString(actualTime.getSecond()) + "-"
                         + Integer.toString(actualTime.getNano());
    return dateTimeStamp;
}

public String getGatlingTerminalLog(String apiAutomationFolder, String gatlingFolderString, String testName) throws FileNotFoundException, IOException{
    String line = null;
    String response = "";
    BufferedReader br = null;
    File file = null;
    try{
        file = new File(apiAutomationFolder + "/tests/_results/" + gatlingFolderString + "/" + testName +".log");
        br = new BufferedReader(new FileReader(file)); 
        while ((line = br.readLine()) != null) {
            if(response.equals("")){
                response = line;
            }else{
                if(line.contains("Basic ")){
                    response = response + "\n" + "***Line with password or token removed!!!***";
                }else if(line.contains("byteArraysBody=")){
                    response = response + "\n" + "***Line with password or token removed!!!***";
                }else{
                    response = response + "\n" + line;
                }  
            }   
        }
        
    }catch(Exception e){
        response = e.getStackTrace().toString();
    }
    //System.out.println("response is=" + response);

    return response;
}

public String getResultsJson(String apiAutomationFolder, String gatlingFolderString) throws FileNotFoundException, IOException{
    String line = null;
    String response = "";
    BufferedReader br = null;
    File file = null;
    try{
        file = new File(apiAutomationFolder + "/tests/_results/" + gatlingFolderString + "/results.json");
        br = new BufferedReader(new FileReader(file)); 
        while ((line = br.readLine()) != null) {
            if(response.equals("")){
                response = line;
            }else{
                response = response + "\n" + line;
            }
        }
        
    }catch(Exception e){
        response = e.getStackTrace().toString();
    }
    //System.out.println("response is=" + response);
    return response;
}

public String executeSingleTest(String testName,String ENV,String gatlingFolderString, String jsessionFolderString,String logType) throws Exception{
    //check ENV parameter
    if(!ENV.equals("DEV") && !ENV.equals("STG") && !ENV.equals("PRD") && !ENV.equals("KSA")){
        responseString = "ENV variable not valid. Please set a valid one: DEV,STG,PRD,KSA (EU not valid for now due to compliance issues)";
        return responseString;
    }

    //validate logType parameter
    if(!logType.equals("gatlingLog") && !logType.equals("resultsLog")){
        responseString = "logType variable not valid. Please set a valid one: gatlingLog,resultsLog";
        return responseString;
    }
    
    try {
        String apiAutomationFolder = System.getenv("PWD");
        ProcessBuilder gatlingExecution = new ProcessBuilder("cmd.exe","/c","cd %PWD% && gatling.bat -sf %PWD% -bf %PWD%/bin -s " + testName + " -rf %PWD%/tests/_results/" + gatlingFolderString);
        Map<String, String> env = gatlingExecution.environment();
        env.put("ENV",ENV);
        env.put("JSESSION_SUITE_FOLDER",apiAutomationFolder + "/tests/_results/" + jsessionFolderString);
        gatlingExecution.redirectOutput(new File(apiAutomationFolder + "/tests/_results/" + gatlingFolderString + "/" + testName +".log"));
        gatlingExecutionProcess = gatlingExecution.start();
        gatlingExecutionProcess.waitFor();
        ProcessBuilder generateRubyReport = new ProcessBuilder("cmd.exe","/c","cd %PWD%/tests && ruby gatling_suite_generate_report.rb " + gatlingFolderString + " %PWD%/tests/_results/" + jsessionFolderString + " STG YES");
        generateRubyReportExecutionProcess = generateRubyReport.start();
        generateRubyReportExecutionProcess.waitFor();
        if(logType.equals("gatlingLog")){
            responseString = this.getGatlingTerminalLog(apiAutomationFolder,gatlingFolderString,testName);
        }else if(logType.equals("resultsLog")){
            responseString = this.getResultsJson(apiAutomationFolder,gatlingFolderString);
        }
    }catch(Exception ioe) {
            throw ioe;
        }
    gatlingExecutionProcess.destroy();
    generateRubyReportExecutionProcess.destroy();
    return responseString;
     }

}
