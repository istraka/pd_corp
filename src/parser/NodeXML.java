/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package parser;

/**
 *
 * @author Ivan Straka
 */
public class NodeXML {
    public final String jarPath;
    public final String user;
    public final String hostname;
    public final int port;
    public final int ftpPort;
    public final int sshPort;
    public final boolean isMaster;
    public final boolean isComputing;
    public final String downloadDest;
    public final TaskXML task;
    public final String logFile;
     
    
    public NodeXML(String projPath, String user, String ip, String downloadDest, int nodePort, int ftpPort, int sshPort, TaskXML task, boolean isMaster, boolean compute, String logFile) {
        this.jarPath = projPath;
        this.user = user;
        this.downloadDest = downloadDest;
        this.hostname = ip;
        this.ftpPort = ftpPort;
        this.sshPort = sshPort;
        port = nodePort;
        this.task = task;
        this.isMaster = isMaster;
        this.isComputing = compute;
        this.logFile = logFile;
        
    }   
}
