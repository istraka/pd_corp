/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package local;

import execution.TaskManager;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import network.NetAddress;
import network.NodeDevice;

/**
 * Class provides information about node.
 * Typicaly used for {@link Master}.
 * @author Ivan Straka
 */
public class NodeInfo {
    
    private final static Logger logger = Logger.getLogger(NodeInfo.class.getName());
    private static final long ftpErrorFixedTimeMilis = 10*60*1000;
    
    private final String host;
    private final String user;
    private long lastHello;
    private final String jarPath;
    
    /**
     * Ssh port of destination.
     */
    public int sshPort;
    private int ftpPort;
    private int nodePort;
    
    private final int id;
    private double estimatedFinishTime;

    private final String downloadingDest;
    private boolean isEmpty;
    private boolean downloading;
    private NodeInfo downloadingFrom;
    private int filesLocked;
    private int uploadingToNodeNum;
    
    private boolean isQuitted;
    private boolean isReady;
    private final boolean isComputing;
    
    private final List<FileObject> filesToProcess;
    
    private final boolean isMasterNode;
    
    private final List<FileObject> filesWithMultipleAccess;
    
    
    NodeDevice device;
    private String task;
    private boolean isSingleFileTask;
    private String returnFolder;
    private String generalResultFile;
    private boolean notifyMasterAboutResult;
    private boolean generateResultConfig;
    private final String logFile;
    private long lastFtpError;
    private boolean suspected;
    
    public NodeInfo(int id, String user, String host, String projDest, int sshPort, String downloadingDest, boolean isMaster, boolean compute, String logFile){
        filesToProcess = new LinkedList<>();
        estimatedFinishTime = TaskManager.UNKNOWN;
        this.user = user;
        this.host = host;
        this.id = id;
        this.jarPath = projDest;
        this.sshPort = (sshPort == 0)?22:sshPort;
        ftpPort = 0;
        nodePort = 0;
        isMasterNode = isMaster;
        this.isComputing = compute;
        isReady = false;
        downloadingFrom = null;
        isEmpty= false;
        downloading = false;
        filesLocked = 0;
        uploadingToNodeNum = 0;
        lastFtpError = -1;
        
        filesWithMultipleAccess = new LinkedList<>();
        this.downloadingDest = downloadingDest;
        this.logFile = logFile;
        
    }
    
    public void ftpError(){
        lastFtpError = System.currentTimeMillis();
    }
    
    public boolean isFtpError(){
        return lastFtpError != -1 && System.currentTimeMillis() - lastFtpError < ftpErrorFixedTimeMilis;
    }
    
    /**
     * 
     * @return List of {@link FileObject} that can be accessed by 2 or more nodes
     */
    public List<FileObject> getFilesWithMultipleAccess(){
        return filesWithMultipleAccess;
    }
    
    /**
     * 
     * @return filenames that node processes
     */
    public List<String> getFilenamesToProcess(){
        List<String> strList = new ArrayList<>(filesToProcess.size());
        for(FileObject file: filesToProcess){
            strList.add(file.getNodeFilename(this));
        }
        return strList;
    }
    
    public boolean areFilesLocked(){
        return filesLocked > 0;
    }
    public void lockFiles(){
        filesLocked++;
    }
    public void unlockFiles(){
        if(filesLocked > 0) {
            filesLocked--;
        }
    }
    
    public void removeFile(FileObject file){
        filesToProcess.remove(file);
        if(file.getAllAccesses().size() > 1){
            filesWithMultipleAccess.remove(file);
        }
    }
    
    public FileObject getFileByName(String filename){
        for(int i=0; i<filesToProcess.size(); i++){
            FileObject file = filesToProcess.get(i);
            NodeInfo worker = file.getWorker();
            // test if this node is worker
            // just in case
            if(worker == null || !worker.equals(this)){
                filesToProcess.remove(i);
                if(file.getAllAccesses().size() > 1){
                    filesWithMultipleAccess.remove(file);
                }
                i--;
            }
            if(file.getWorkerFilename().equals(filename)){
                return file;
            }
        }
        return null;
    }
    
    public void unsetDownloading(){
        downloading = false;
        downloadingFrom = null;
    }
    public void setDownloading(NodeInfo node){
        downloading = true;
        downloadingFrom = node;
    }
    public void setUploading(boolean state){
        uploadingToNodeNum += state ? 1 : -1;
    }
    
    public boolean isUploading(){
        return uploadingToNodeNum == 1;
    }
    
    public boolean isDownloading(){
        return downloading;
    }
    
    public NodeInfo getDownloadingSrc(){
        return downloadingFrom;
    }
    
    /**
     * 
     * @return if estimated finish time is zero.
     */
    public boolean isFinished(){
        return (estimatedFinishTime <= 0);
    }
    
    public void quit(){
        isQuitted = true;
    }
    
    /**
     *
     * @return true if node has been isQuitted.
     */
    public boolean isQuitted(){
        return isQuitted;
    }
    
    /**
     * Set node isReady.
     */
    public void ready(){
        isReady = true;
    }
    
    /**
     * 
     * @return sum of queues's length (filesToProcess to process and to download).
     */
    public int queueLen(){
        return filesToProcess.size();
    }
    
    /**
     * Add file to process.
     */
    public void addFile(FileObject file){
        filesToProcess.add(file);
        if(file.getAllAccesses().size() > 1){
            filesWithMultipleAccess.add(file);
        }
    }
    
    /**
     * Set device for sending messages.
     * @see NodeDevice
     */
    public void setDevice(NodeDevice device){
        this.device = device;
    }
    
    /**
     * 
     * @return ftp adress 
     */
    public NetAddress getFtpAddress(){
        return NetAddress.getAddress(host,ftpPort);
    }
    
    /**
     * 
     * @return node's adress 
     */
    public NetAddress getNodeAddress(){
        return NetAddress.getAddress(host,nodePort);
    }
    
    /**
     * 
     * @return true if node is master
     */
    public boolean isMaster(){
        return isMasterNode;
    }
    
    /**
     * 
     * @return true if node is isReady
     */
    public boolean isReady(){
        return isReady;
    }
    
    /**
     * 
     * @return user for ssh
     */
    public String getUser(){
        return user;
    }
    
    
    public String getSshExecArgs(NetAddress masterDest, boolean returnResult){
        String line = "java -jar "+getSshJarPath();
        line += " -n "+id+" "+host+" "+masterDest+" "+nodePort+" "+ftpPort+" "+
                (isSingleFileTask?"s":"p") + " "+getSshTask()+" "+(returnResult?"t ":"f ")
                +(getSshReturnFolder().equals("")?"-":getSshReturnFolder())+" "+(generateResultConfig?"t ":"f ")
                +(getSshGeneralResultFile().equals("")?"-":getSshGeneralResultFile())+" "
                +(getSshLogFile().equals("")?"-":getSshLogFile())+" "+getSshDownloadingDest()+" -f ";
        // filesToProcess
        for(FileObject file: filesToProcess){
            line += file.getWorkerFilename()+" ";
        }
        
        line += "2>"+getSshLogFileWithExt(".err");
        return line;
    }
    
    @Override
    public boolean equals(Object o){
        if (o instanceof NodeInfo){
            NodeInfo tmp = (NodeInfo) o;
            return (this.id == tmp.id && this.host.equals(tmp.host));
        }
        else return false;
    }
    
    @Override
    public int hashCode(){
        return this.host.hashCode();
    }

    /**
     * 
     * @return node's id
     */
    public int getId() {
        return id;
    }

    /**
     * @see NodeDevice
     * @return node's device
     */
    public NodeDevice getDevice() {
        return device;
    }
    
    /**
     * Update node's info by hello message
     * @param estimatedFinish 
     */
    public void hello(double estimatedFinish){
        lastHello = System.currentTimeMillis();
        logger.log(Level.FINEST,"{0} estimated finish time {1}", new Object[]{host, Double.toString(estimatedFinish)});
        this.estimatedFinishTime = estimatedFinish;
    }
    
    /**
     * 
     * @return estimated finish time
     */
    public double getEstimatedFinishTimeSec(){
        return estimatedFinishTime;
    }
    
    /**
     * 
     * @return true if time from last hello has not exceeded {@link Master#nodeDeadTimeoutMilis}
     */
    public boolean isAlive(){
        return (this.isReady() && System.currentTimeMillis() - lastHello < Master.nodeDeadTimeoutMilis);
    }

    public void setEmpty(boolean b) {
        isEmpty = b;
    }
    
    public boolean isEmpty(){
        return isEmpty;
    }
    
    public void setReturnFileAddress(NetAddress a){
        returnFolder= a.dest;
        setFtpPort(a.port);
    }
    
    public NetAddress getReturnFileAddress(){
        return (returnFolder == null)?null:NetAddress.getAddress(returnFolder, host, ftpPort);
    }

    public String getHostname() {
        return host;
    }
    
    public InetAddress getAddress() {
        return device == null?null:device.getAddress();
    }
    

    /**
     * @param cmd
     * @param singleFileTask
     * @param returnFolder
     * @param generateResultConfig
     * @param resultFile
     */
    public void setTask(String cmd, boolean singleFileTask, String returnFolder, boolean generateResultConfig, String resultFile) {
        this.task = cmd;
        this.isSingleFileTask = singleFileTask;
        this.returnFolder = returnFolder;
        this.generalResultFile = resultFile;
        this.generateResultConfig = generateResultConfig;
    }

    public int getFtpPort() {
        return ftpPort;
    }

    public int getNodePort() {
        return nodePort;
    }

    public String getJarPath() {
        return jarPath;
    }

    public String getDownloadFolder() {
        return downloadingDest;
    }

    /**
     * @return the jarPath
     */
    private String getSshJarPath() {
        return '"'+jarPath+'"';
    }

    /**
     * @return the downloadingDest
     */
    private String getSshDownloadingDest() {
        return (downloadingDest.startsWith("\"")?"":"\"")+downloadingDest+(downloadingDest.endsWith("\"")?"":"\"");
    }

    /**
     * @return the task
     */
    private String getSshTask() {
        return (task.startsWith("\"")?"":"\"")+task+(task.endsWith("\"")?"":"\"");
    }

    /**
     * @return the returnFolder
     */
    private String getSshReturnFolder() {
        return (returnFolder.startsWith("\"")?"":"\"")+returnFolder+(returnFolder.endsWith("\"")?"":"\"");
    }

    /**
     * @return the generalResultFile
     */
    private String getSshGeneralResultFile() {
        return (generalResultFile.startsWith("\"")?"":"\"")+generalResultFile+(generalResultFile.endsWith("\"")?"":"\"");
    }

    /**
     * @return the logFile
     */
    private String getSshLogFile() {
        return (logFile.startsWith("\"")?"":"\"")+logFile+(logFile.endsWith("\"")?"":"\"");
    }
    private String getSshLogFileWithExt(String ext) {
        return (logFile.startsWith("\"")?"":"\"")+logFile+ext+(logFile.endsWith("\"")?"":"\"");
    }

    /**
     * @return the isComputing
     */
    public boolean isComputing() {
        return isComputing;
    }

    /**
     * @param ftpPort the ftpPort to set
     */
    public void setFtpPort(int ftpPort) {
        this.ftpPort = ftpPort;
    }

    /**
     * @param nodePort the nodePort to set
     */
    public void setNodePort(int nodePort) {
        this.nodePort = nodePort;
    }

    /**
     * Return true if has file that can be accessed by node with given filename.
     */ 
    public boolean hasFile(NodeInfo node, String nodeFilename) {
        for(FileObject file: filesToProcess){
            String filename = file.getNodeFilename(node);
            if(filename != null && filename.equals(nodeFilename)) {
                return true;
            }
        }
        return false;
    }

    public boolean isSuspicious() {
        return suspected;
    }
    
    public void setSusicious(boolean value){
        suspected = value;
    }
}
