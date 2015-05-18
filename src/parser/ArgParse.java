/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package parser;

import java.util.LinkedList;
import java.util.List;
import network.NetAddress;

/**
 * Parser for arguments.
 * @author Ivan Straka
 */
public class ArgParse {
    
    public String[] args;
    private boolean master;
    private boolean slave;
    private boolean returnProcessedDownloadedFiles;
    private boolean generatingResultConfig;
    
    // master
    private String config;
    private String rsa;
    private String knownHosts;
    private boolean help;
    private boolean generateConfig;
    private String generateConfigInput;
    private String generateConfigOutput;
    private String generateResultConfigFile;
    private boolean checksumMultiaccessFiles;
    
    // slave
    private int id;
    private String task;
    private int nodePort;
    private int ftpPort;
    private List<String> filesToProcess;
    private String downloadDest;
    private boolean singleFileTask;
    private String remotelyProcessedReturnFolder;
    private String localHostname;
    private String generalResult;
    private String logFile;
    private NetAddress masterAddress;
    
    public ArgParse(String[] args){
        this.args = args;
        master = false;
        slave = false;
        help = false;
        knownHosts = null;
        config = null;
        rsa = null;
        id = -1;
        task = null;
        nodePort = -1;
        ftpPort = -1;
        filesToProcess = new LinkedList<>();
        generateConfig = false;
        returnProcessedDownloadedFiles = false;
        singleFileTask = false;
        remotelyProcessedReturnFolder = null;
        String input = "";
        generatingResultConfig = false;
        generateResultConfigFile = null;
        String output = "";
        logFile = null;
    }
    
    public static final String printHelp(){
        String retVal = "Usage:  -m [ [-g input_file output_file] | [-c file] ] [-h] [-k file] [-r file] [-e] [-n output_file]\n";
        retVal += "-m --master: Required. Define that PC is master\n";
        retVal += "-g --generate infile outfile: generate xml config file from given file to output file. If this optin is used, progam will quit after file is created.\n";
        retVal += "-c --config file: Define path to xml config file\n";
        retVal += "-k --known_hosts file: Define path to known_hosts file\n";
        retVal += "-r --rsa file: Optional. Define path to private key. File.pub will be added automaticaly.\n";
        retVal += "-e --return: Optional. Set uploading processed files back to node where they were downloaded from.\n";
        retVal += "-n --new_config output_file: Optional. Set generate new config file of file results.\n";
        retVal += "-s --checksum: Optional. Verify, that files that more than one node may access to has the same md5 sum";
        return retVal;
    }
    
    /**
     * Parse arguments.
     * @throws parser.ParserException
     */
    public void parse() throws ParserException{
        if(args.length < 1){
            throw new ParserException("Arguments error");
        }
        
        switch(args[0]){
            case "-n":
                parseSlave();
                break;
            case "-m":
                parseMaster();
                break;
            default:
                throw new ParserException("Arguments error");
                
        }            
    }
    
    public boolean isMaster(){
        return master;
    }
    
    private void parseMaster() throws ParserException{
        master = true;
        for(int i=0; i<args.length; i++){
            
            switch(args[i]){
                
                // master arg...do nothing
                case "-m":
                case "--master":
                    break;
                    
                case "-h":
                case "--help":
                    help = true;
                    break;
                
                case "-c":
                case "--config":
                    if(i+1 >= args.length || args[i+1].startsWith("-")){
                        throw new ParserException("Error");
                    }
                    config = args[i+1];
                    i++;
                    break;
                
                case "-r":
                case "--rsa":
                    if(i+1 >= args.length || args[i+1].startsWith("-")){
                        throw new ParserException("Error");
                    }
                    rsa = args[i+1];
                    i++;
                    break;
                
                case "-e":
                case "--return":
                    returnProcessedDownloadedFiles = true;
                    break;
                    
                case "-s":
                case "--checksum":
                    checksumMultiaccessFiles = true;
                    break;
                  
                case "-k":
                case "--known_hosts":
                    if(i+1 >= args.length || args[i+1].startsWith("-")){
                        throw new ParserException("Error");
                    }
                    knownHosts = args[i+1];
                    i++;
                    break;
                    
                case "-n":
                case "--new_config":
                    if(i+1 >= args.length || args[i+1].startsWith("-")){
                        throw new ParserException("Error");
                    }
                    generatingResultConfig = true;
                    generateResultConfigFile = args[i+1];
                    i++;
                    break;
                    
                case "-g":
                case "-generate":
                    if(i+2 >= args.length || 
                            args[i+1].startsWith("-") ||
                            args[i+2].startsWith("-")){
                        
                        throw new ParserException("Error");
                    }
                    generateConfigInput = args[i+1];
                    generateConfigOutput = args[i+2];
                    generateConfig = true;
                    i += 2;
                    break;
                    
                default:
                    throw new ParserException("Arguments error");
                    
            }
        }
        if(!isHelp() && (getConfig() == null && isGenerateConfig() != true)) {
            throw new ParserException("Arguments error");
        }
    }
    
    /**
     * FSM to parse slave args.
     * This is composed by master node not by user therefore exceptions are not expected.
     */
    
    private void parseSlave(){
        slave = true;
        id = Integer.parseInt(args[1]);
        localHostname = args[2];
        masterAddress = NetAddress.getAddress(args[3]);
        nodePort = Integer.parseInt(args[4]);
        ftpPort = Integer.parseInt(args[5]);
        singleFileTask = args[6].equals("s");
        task = args[7];
        returnProcessedDownloadedFiles = args[8].equals("t");
        remotelyProcessedReturnFolder = args[9].equals("-")?"":args[9];
        generatingResultConfig = args[10].equals("t");
        generalResult = args[11].equals("-")?"":args[11];
        logFile = args[12]; 
        downloadDest = args[13];
        
        
        int pos;
        for(pos = 15; pos<args.length ; pos++){
            filesToProcess.add(args[pos]);
        }

    }

    /**
     * @return the slave
     */
    public boolean isSlave() {
        return slave;
    }

    /**
     * @return the returnProcessedDownloadedFiles
     */
    public boolean isReturnProcessedDownloadedFiles() {
        return returnProcessedDownloadedFiles;
    }

    /**
     * @return the generatingResultConfig
     */
    public boolean isGeneratingResultConfig() {
        return generatingResultConfig;
    }

    /**
     * @return the config
     */
    public String getConfig() throws ParserException {
        if (!isMaster())
            throw new ParserException("Unaccessible config file in "+ArgParse.class);
        return config;
    }

    /**
     * @return the rsa
     */
    public String getRsa() throws ParserException {
        if (!isMaster())
            throw new ParserException("Unaccessible rsa file in "+ArgParse.class);
        return rsa;
    }

    /**
     * @return the hosts
     */
    public String getKnownHosts() throws ParserException {
        if (!isMaster())
            throw new ParserException("Unaccessible known hosts in "+ArgParse.class);
        return knownHosts;
    }

    /**
     * @return the help
     */
    public boolean isHelp() throws ParserException {
        if (!isMaster())
            throw new ParserException("Unaccessible help in "+ArgParse.class);
        return help;
    }

    /**
     * @return the generateConfig
     */
    public boolean isGenerateConfig() throws ParserException {
        if (!isMaster())
            throw new ParserException("Unaccessible generate config in "+ArgParse.class);
        return generateConfig;
    }

    /**
     * @return the generateConfigInput
     */
    public String getGenerateConfigInput() throws ParserException {
        if (!isMaster())
            throw new ParserException("Unaccessible input for generating config in "+ArgParse.class);
        return generateConfigInput;
    }

    /**
     * @return the generateConfigOutput
     */
    public String getGenerateConfigOutput() throws ParserException {
        if (!isMaster())
            throw new ParserException("Unaccessible output for generating config in "+ArgParse.class);
        return generateConfigOutput;
    }

    /**
     * @return the generateResultConfigFile
     */
    public String getGenerateResultConfigFile() throws ParserException {
     if (!isMaster())
            throw new ParserException("Unaccessible generating result config in "+ArgParse.class);   
        return generateResultConfigFile;
    }

    /**
     * @return the checksumMultiaccessFiles
     */
    public boolean isChecksumMultiaccessFiles() throws ParserException {
        if (!isMaster())
            throw new ParserException("Unaccessible checksum for multiaccessible files in "+ArgParse.class);
        return checksumMultiaccessFiles;
    }

    /**
     * @return the id
     */
    public int getId() throws ParserException {
        if (!isSlave())
            throw new ParserException("Unaccessible id in "+ArgParse.class);
        return id;
    }

    /**
     * @return the task
     */
    public String getTask() throws ParserException {
        if (!isSlave())
            throw new ParserException("Unaccessible task in "+ArgParse.class);
        return task;
    }

    /**
     * @return the nodePort
     */
    public int getNodePort() throws ParserException {
        if (!isSlave())
            throw new ParserException("Unaccessible node port in "+ArgParse.class);
        return nodePort;
    }

    /**
     * @return the ftpPort
     */
    public int getFtpPort() throws ParserException {
        if (!isSlave())
            throw new ParserException("Unaccessible ftp port in "+ArgParse.class);
        return ftpPort;
    }

    /**
     * @return the filesToProcess
     */
    public List<String> getFilesToProcess() throws ParserException {
        if (!isSlave())
            throw new ParserException("Unaccessible files to process in "+ArgParse.class);
        return filesToProcess;
    }

    /**
     * @return the downloadDest
     */
    public String getDownloadDest() throws ParserException {
        if (!isSlave())
            throw new ParserException("Unaccessible downloading folder in "+ArgParse.class);
        return downloadDest;
    }

    /**
     * @return the singleFileTask
     */
    public boolean isSingleFileTask() throws ParserException {
        if (!isSlave())
            throw new ParserException("Unaccessible feeder in "+ArgParse.class);
        return singleFileTask;
    }

    /**
     * @return the processedUploadedFileFolder
     */
    public String getRemotelyProcessedReturnFolder() throws ParserException {
        if (!isSlave())
            throw new ParserException("Unaccessible id in "+ArgParse.class);
        return remotelyProcessedReturnFolder;
    }

    /**
     * @return the localHostname
     */
    public String getLocalHostname() throws ParserException {
        if (!isSlave())
            throw new ParserException("Unaccessible local hostname in "+ArgParse.class);
        return localHostname;
    }

    /**
     * @return the generalResult
     */
    public String getGeneralResult() throws ParserException {
        if (!isSlave())
            throw new ParserException("Unaccessible general result in "+ArgParse.class);
        return generalResult;
    }

    /**
     * @return the logFile
     */
    public String getLogFile() throws ParserException {
        if (!isSlave())
            throw new ParserException("Unaccessible log file in "+ArgParse.class);
        return logFile;
    }

    /**
     * @return the masterAddress
     */
    public NetAddress getMasterAddress() throws ParserException {
        if (!isSlave())
            throw new ParserException("Unaccessible master address in "+ArgParse.class);
        return masterAddress;
    }
    
}
