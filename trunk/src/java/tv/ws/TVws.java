/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tv.ws;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilterWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Scanner;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;

/**
 *
 * @author dida
 */
@WebService(serviceName = "WSSubmitJob")
public class TVws {

    /**
     * This is a sample web service operation
     */
    //Directories
    final String gridDir = "/biomed/user/l/louacheni";
    final String diracDir = "/home/dida/DIRAC/scripts";
    final String dockFile = "/home/dida/Desktop/DockLigFile";
    final String filesDir = "/home/dida/DIRAC";
    final String yii = "/var/www/yii-framework-php/framework/first_yii/protected/views";
    int j;
    String line = "";

    @WebMethod(operationName = "generateJDL")
    public String generateJDK(@WebParam(name = "file") String file, @WebParam(name = "gpf") String gpf, @WebParam(name = "dpf") String dpf) {

        //execute command shell line
        generateDockingJDL(file, gpf, dpf);
        String[] command = {diracDir + "/dirac-wms-job-submit", filesDir + "/" + file + "/dockingFile.jdl"};
        String output = executeShellCommand(command);

        return output;
    }

    private void generateDockingJDL(String file, String gpf, String dpf) {
        ArrayList<String> files = new ArrayList<String>();
        ArrayList<String> scriptF = new ArrayList<String>();

        File fileDir = new File(filesDir + "/" + file);
        //create file if it doesn't exist
        if (!fileDir.exists()) {
            //create didrectory
            boolean create = fileDir.mkdir(); //or fileDir.mkdir();
            if (!create) {
                System.out.println("Error creating directory" + fileDir);
            }
            String path = fileDir.getAbsolutePath();
            System.out.println("The path of the file is " + path);
        }
        //generate shell
        String scriptName = fileDir + "/dock.sh";
        try {
            //write to file        
            FileWriter fw = new FileWriter(scriptName);
            BufferedWriter bw = new BufferedWriter(fw);
            //job name
            bw.write("#!/bin/bash");
            bw.newLine();
            //
            bw.write("tar -xvzf" + " " + "fileVS.tar.gz");
            bw.newLine();
            //
            bw.write("/bin/bash autodock.sh autodocksuite-4.2.5.1-i86Linux2.tar.gz" + " " + yii+ "/" + gpf + " " + yii + "/" + dpf);
            bw.newLine();
            //
//            bw.write("autogrid4 -p " + yii + "/" + gpf+ " "+ "-l ${gpf%%.*}.glg");
//            bw.newLine();
//            //
//            bw.write("autodock4 -p " + yii + "/" + dpf+ " "+ "-l ${dpf%%.*}.dlg");
//            bw.newLine();
            //     
            bw.write("tar jcf file_dock_Dock.tar.bz2 *.*");
            bw.newLine();
            //
            bw.close();
            System.out.println("Done");

            //}
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        //open file name
        String fileName = fileDir + "/dockingFile.jdl";
        try {
            //write to file        
            FileWriter fw = new FileWriter(fileName);
            BufferedWriter bw = new BufferedWriter(fw);
            //job name
            bw.write("JobName = \"dockingFile-" + file + "\";");
            bw.newLine();
            //Argument
//            bw.write("Arguments = \"" + str + "\" ;");
//            //bw.write("Executable = \"" +filesDir + "/dock.sh\";");
//            bw.newLine();
            //executable
            bw.write("Executable = \"dock.sh\";");
            //bw.write("Executable = \"" +filesDir + "/dock.sh\";");
            bw.newLine();
            //output
            bw.write("StdOutput = \"stdOut\";");
            bw.newLine();
            //error
            bw.write("StdError = \"stdErr\";");
            bw.newLine();
            //inputsand box
            bw.write("InputSandbox = {\"" + fileDir + "/dock.sh\","
                    + "\"LFN:" + gridDir + "/fileVS.tar.gz\"};");
            bw.newLine();
            //outputSandbox
            bw.write("OutputSandbox = {\"stdOut\", \"stdErr\", \"file_dock_Dock.tar.bz2\"};");
            bw.newLine();
            //close file
            bw.close();
            System.out.println("Done");
            //}
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        files.add(fileName);

    }

    private String executeShellCommand(String[] command) {
        ArrayList<String> lineCommand = new ArrayList<String>();
        String stat = "";
        try {
            Runtime env = Runtime.getRuntime();
            Process process = env.exec(command);
            InputStreamReader isr = new InputStreamReader(process.getInputStream());
            BufferedReader input = new BufferedReader(isr);
            String line = "";
            while ((line = input.readLine()) != null) {
                lineCommand.add(line);
                System.out.println(lineCommand);
            }
            for (int i = 0; i <= lineCommand.size(); i++) {
                return lineCommand.get(i);
            }
            int exitVal = process.waitFor();
            System.out.println("Exited with error code " + exitVal);
            process.getInputStream().close();
            input.close();
            process.destroy();
        } catch (Exception e) {
            //e.printStackTrace();
            line = "Error: " + e.getMessage();
        }
        return line;

    }

    //get the status of the job
    @WebMethod(operationName = "getStatusJob")
    @SuppressWarnings("empty-statement")
    public String getStatusJob(@WebParam(name = "jobID") String jobID) {
        ArrayList<String> getStat = new ArrayList<String>();
        String jobStatOut = "";
        if (jobID.toLowerCase().startsWith("jobid")) {
            //extract the job id
            String idJob = jobID.substring(8, jobID.length());
            //execute command shell to get the status of the job
            String[] statCmd = {"/bin/bash", filesDir + "/getJobStat.sh", idJob};
            jobStatOut = executeShellCommand(statCmd);
            if (jobStatOut.toLowerCase().equals("failed")) {
                // then reschedule it
                String[] cmdres = {diracDir + "/dirac-wms-job-reschedule", idJob};
                jobStatOut = executeShellCommand(cmdres);
            } else {
                System.out.println("Status : " + jobStatOut);
                if (!jobStatOut.isEmpty()) {
                    getStat.add(jobStatOut);
                } else {
                    System.out.println("failed Job");
                }
            }

        } else {
            System.out.println("check job");
        }
        return jobStatOut;
    }

    //get job output  of the job
    @WebMethod(operationName = "getOutJob")
    @SuppressWarnings("empty-statement")
    public String getOutJob(@WebParam(name = "jobID") String jobID, @WebParam(name = "jobStat") String jobStat) {
        ArrayList<String> getStat = new ArrayList<String>();
        String getOutPath = "";
        boolean is_done = false;
        String jobStatOut = "";
        String idJob = "";
        if (jobStat.toLowerCase().equals("done")) {
            if (jobID.toLowerCase().startsWith("jobid")) {
                //extract the job id
                idJob = jobID.substring(8, jobID.length());
                //execute command shell to get the status of the job
                String[] statCmd = {"/bin/bash", filesDir + "/getJobPath.sh", idJob};
                jobStatOut = executeShellCommand(statCmd);
                if ((!jobStatOut.isEmpty())) {
                    is_done = true;
                    getOutPath = jobStatOut;
                } else {
                    is_done = false;
                }

            } else {
                System.out.println("failed Job");
            }
        } else {
            String[] cmdres = {diracDir + "/dirac-wms-job-reschedule", idJob};
            jobStatOut = executeShellCommand(cmdres);
        }
        return getOutPath;

    }

}
